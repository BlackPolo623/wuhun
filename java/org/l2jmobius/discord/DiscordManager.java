/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.discord;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.discord.listener.DiscordEventListener;
import org.l2jmobius.gameserver.config.custom.DiscordConfig;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

/**
 * Discord Bot 核心管理器（Singleton）。
 *
 * 職責：
 *   - 啟動 / 關閉 JDA 連線
 *   - 提供 sendPlayerDM() / sendDM() 給任意腳本使用
 *
 * 腳本使用範例：
 * <pre>
 *   import org.l2jmobius.discord.DiscordManager;
 *   DiscordManager.getInstance().sendPlayerDM(player.getObjectId(), "報告內容...");
 * </pre>
 *
 * @author Custom
 */
public class DiscordManager
{
	private static final Logger LOGGER = Logger.getLogger(DiscordManager.class.getName());

	/** JDA 實例，null 表示 Bot 未啟用或初始化失敗 */
	private JDA _jda = null;

	/** 標記是否已成功啟動 */
	private volatile boolean _running = false;

	// ── 生命週期 ─────────────────────────────────────────────────────────────

	/**
	 * 啟動 Discord Bot。
	 * 由 GameServer 啟動流程呼叫，若設定停用則直接跳過。
	 */
	public void start()
	{
		if (!DiscordConfig.DISCORD_ENABLED)
		{
			LOGGER.info("DiscordManager: Discord Bot 已停用，跳過啟動。");
			return;
		}

		try
		{
			LOGGER.info("DiscordManager: 正在連線 Discord...");

			_jda = JDABuilder.createDefault(DiscordConfig.DISCORD_BOT_TOKEN)
				// 啟用接收私訊、訊息內容以及伺服器成員事件所需的 Gateway Intents
				// 注意：GUILD_MEMBERS 為 Privileged Intent，需在 Discord Developer Portal 開啟 "Server Members Intent"
				.enableIntents(GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
				// 註冊事件監聽器（處理 !bind 等指令及新成員歡迎訊息）
				.addEventListeners(new DiscordEventListener())
				.build();

			// 等待 Bot 準備就緒（最多 30 秒）
			_jda.awaitReady();
			_running = true;

			LOGGER.info("DiscordManager: Discord Bot 連線成功！Bot 名稱：" + _jda.getSelfUser().getName());
			logStatus();

			// 每小時清理一次過期驗證碼
			scheduleCodeCleanup();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "DiscordManager: Discord Bot 啟動失敗！", e);
			_jda = null;
			_running = false;
		}
	}

	/**
	 * 關閉 Discord Bot，釋放 JDA 連線。
	 * 由 GameServer 關服流程呼叫。
	 */
	public void shutdown()
	{
		if (_jda != null)
		{
			LOGGER.info("DiscordManager: 正在關閉 Discord Bot...");
			try
			{
				_jda.shutdown();
				// 等待最多 5 秒讓 JDA 優雅關閉
				if (!_jda.awaitShutdown(5, TimeUnit.SECONDS))
				{
					_jda.shutdownNow();
				}
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				_jda.shutdownNow();
			}
			_jda = null;
			_running = false;
			LOGGER.info("DiscordManager: Discord Bot 已關閉。");
		}
	}

	/** 是否已成功啟動並連線 */
	public boolean isRunning()
	{
		return _running && (_jda != null);
	}

	// ── 訊息發送（腳本呼叫的核心功能）──────────────────────────────────────

	/**
	 * 根據遊戲角色 ObjectId 發送 Discord 私訊。
	 * 若玩家未綁定 Discord 則靜默跳過。
	 *
	 * @param playerId 玩家 ObjectId
	 * @param message  訊息內容（純文字或 Markdown）
	 */
	public void sendPlayerDM(int playerId, String message)
	{
		if (!isRunning())
		{
			return;
		}

		final String discordId = DiscordDAO.getDiscordId(playerId);
		if (discordId == null)
		{
			// 玩家未綁定，靜默跳過（正常情況）
			return;
		}

		sendDM(discordId, message);
	}

	/**
	 * 直接向指定 Discord 用戶 ID 發送私訊。
	 *
	 * @param discordId Discord 用戶 ID（18 位數字字串）
	 * @param message   訊息內容
	 */
	public void sendDM(String discordId, String message)
	{
		if (!isRunning())
		{
			return;
		}

		_jda.retrieveUserById(discordId).queue(
			user -> user.openPrivateChannel().queue(
				channel -> channel.sendMessage(message).queue(
					success -> {},
					error -> LOGGER.warning("DiscordManager: 訊息發送失敗（" + discordId + "）：" + error.getMessage())
				),
				error -> LOGGER.warning("DiscordManager: 無法開啟 DM 頻道（" + discordId + "）：" + error.getMessage())
			),
			error -> LOGGER.warning("DiscordManager: 找不到 Discord 用戶（" + discordId + "）：" + error.getMessage())
		);
	}

	/**
	 * 根據遊戲角色 ObjectId 發送 Discord Embed 私訊。
	 *
	 * @param playerId 玩家 ObjectId
	 * @param embed    MessageEmbed 物件
	 */
	public void sendPlayerEmbed(int playerId, MessageEmbed embed)
	{
		if (!isRunning())
		{
			return;
		}

		final String discordId = DiscordDAO.getDiscordId(playerId);
		if (discordId == null)
		{
			return;
		}

		sendEmbed(discordId, embed);
	}

	/**
	 * 直接向指定 Discord 用戶 ID 發送 Embed 私訊。
	 *
	 * @param discordId Discord 用戶 ID
	 * @param embed     MessageEmbed 物件
	 */
	public void sendEmbed(String discordId, MessageEmbed embed)
	{
		if (!isRunning())
		{
			return;
		}

		_jda.retrieveUserById(discordId).queue(
			user -> user.openPrivateChannel().queue(
				channel -> channel.sendMessageEmbeds(embed).queue(
					success -> {},
					error -> LOGGER.warning("DiscordManager: Embed 發送失敗（" + discordId + "）：" + error.getMessage())
				),
				error -> LOGGER.warning("DiscordManager: 無法開啟 DM 頻道（" + discordId + "）：" + error.getMessage())
			),
			error -> LOGGER.warning("DiscordManager: 找不到 Discord 用戶（" + discordId + "）：" + error.getMessage())
		);
	}

	/**
	 * 向指定的 Discord 文字頻道發送訊息（用於 PVP/PK 公告等）。
	 *
	 * @param channelId Discord 文字頻道 ID
	 * @param message   訊息內容（支援 Markdown / @mention）
	 */
	public void sendChannelMessage(String channelId, String message)
	{
		if (!isRunning() || (channelId == null) || channelId.isEmpty())
		{
			return;
		}

		final TextChannel channel = _jda.getTextChannelById(channelId);
		if (channel == null)
		{
			LOGGER.warning("DiscordManager: 找不到文字頻道（ID：" + channelId + "），請確認 Bot 已加入該伺服器且頻道 ID 正確。");
			return;
		}

		channel.sendMessage(message).queue(
			success -> {},
			error -> LOGGER.warning("DiscordManager: 頻道訊息發送失敗（" + channelId + "）：" + error.getMessage())
		);
	}

	// ── 啟動狀態摘要 ──────────────────────────────────────────────────────────

	private void logStatus()
	{
		LOGGER.info("DiscordManager: --------- Discord Bot 功能狀態 ---------");

		// 玩家綁定系統
		LOGGER.info("DiscordManager: [玩家綁定] 啟用 | 驗證碼有效期：" + DiscordConfig.DISCORD_BIND_CODE_EXPIRE_MINUTES + " 分鐘");

		// 傷害分析報告
		LOGGER.info("DiscordManager: [傷害報告] 啟用 | 分析結束後自動發送至玩家 Discord DM");

		// PVP/PK 頻道公告
		final boolean pvpEnabled = DiscordConfig.DISCORD_PVP_ANNOUNCE_ENABLED && !DiscordConfig.DISCORD_PVP_CHANNEL_ID.isEmpty();
		if (pvpEnabled)
		{
			final String channelId = DiscordConfig.DISCORD_PVP_CHANNEL_ID;
			final net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = _jda.getTextChannelById(channelId);
			final String channelDesc = channel != null ? "#" + channel.getName() : "頻道不可訪問，請確認 Bot 已加入伺服器";
			LOGGER.info("DiscordManager: [PVP公告] 啟用 | 頻道：" + channelDesc + " (" + channelId + ")");
			if (channel == null)
			{
				LOGGER.warning("DiscordManager: [PVP公告] 警告：找不到頻道，PVP/PK 公告將無法發送！");
			}
		}
		else if (DiscordConfig.DISCORD_PVP_ANNOUNCE_ENABLED)
		{
			LOGGER.warning("DiscordManager: [PVP公告] 警告：已啟用但未設定 DiscordPvpChannelId，公告停用。");
		}
		else
		{
			LOGGER.info("DiscordManager: [PVP公告] 停用");
		}

		// 新成員歡迎訊息
		if (DiscordConfig.DISCORD_WELCOME_ENABLED)
		{
			final boolean hasContent = (DiscordConfig.DISCORD_WELCOME_MESSAGE != null) && !DiscordConfig.DISCORD_WELCOME_MESSAGE.isEmpty();
			if (hasContent)
			{
				LOGGER.info("DiscordManager: [歡迎訊息] 啟用 | 新成員加入 Discord 伺服器後自動發送私訊");
			}
			else
			{
				LOGGER.warning("DiscordManager: [歡迎訊息] 警告：已啟用但 DiscordWelcome.txt 未找到或內容為空，歡迎訊息將不會發送！");
			}
		}
		else
		{
			LOGGER.info("DiscordManager: [歡迎訊息] 停用");
		}

		LOGGER.info("DiscordManager: ----------------------------------------");
	}

	// ── 定期清理 ─────────────────────────────────────────────────────────────

	private void scheduleCodeCleanup()
	{
		// 每小時清理過期驗證碼，避免資料表無限增長（L2J ThreadPool 單位為毫秒）
		org.l2jmobius.commons.threads.ThreadPool.scheduleAtFixedRate(
			DiscordDAO::cleanupExpiredCodes,
			60 * 60 * 1000L,  // 初始延遲 1 小時
			60 * 60 * 1000L   // 每 1 小時執行一次
		);
	}

	// ── Singleton ────────────────────────────────────────────────────────────

	public static DiscordManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final DiscordManager INSTANCE = new DiscordManager();
	}

	protected DiscordManager()
	{
	}
}
