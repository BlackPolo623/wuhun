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
package org.l2jmobius.discord.listener;

import java.util.logging.Logger;

import org.l2jmobius.discord.DiscordBindingManager;
import org.l2jmobius.discord.DiscordBindingManager.BindResult;
import org.l2jmobius.discord.DiscordDAO;
import org.l2jmobius.discord.model.DiscordBinding;
import org.l2jmobius.gameserver.config.custom.DiscordConfig;

import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Discord Bot 訊息監聽器。
 *
 * 處理玩家在 Discord 發送的指令（僅限私訊 DM）：
 *   !bind  <code>  — 使用驗證碼完成角色綁定
 *   !unbind        — 解除角色與 Discord 帳號的綁定
 *   !status        — 查看目前綁定狀態
 *   !help          — 顯示使用說明
 *
 * @author Custom
 */
public class DiscordEventListener extends ListenerAdapter
{
	private static final Logger LOGGER = Logger.getLogger(DiscordEventListener.class.getName());

	@Override
	public void onMessageReceived(MessageReceivedEvent event)
	{
		// 忽略 Bot 本身的訊息
		if (event.getAuthor().isBot())
		{
			return;
		}

		// 只處理私訊（DM），不處理伺服器頻道訊息
		if (event.isFromGuild())
		{
			return;
		}

		final String content = event.getMessage().getContentRaw().trim();
		if (!content.startsWith("!"))
		{
			return;
		}

		final String discordId = event.getAuthor().getId();
		final String[] parts = content.substring(1).split("\\s+", 2);
		final String command = parts[0].toLowerCase();
		final String args = parts.length > 1 ? parts[1].trim() : "";

		switch (command)
		{
			case "bind":
			{
				handleBind(event, discordId, args);
				break;
			}
			case "unbind":
			{
				handleUnbind(event, discordId);
				break;
			}
			case "status":
			{
				handleStatus(event, discordId);
				break;
			}
			case "help":
			default:
			{
				handleHelp(event);
				break;
			}
		}
	}

	// ── 指令處理 ─────────────────────────────────────────────────────────────

	private void handleBind(MessageReceivedEvent event, String discordId, String code)
	{
		if (code.isEmpty())
		{
			reply(event, "❌ 請輸入驗證碼。用法：`!bind <驗證碼>`\n在遊戲內找到Discord小助手取得驗證碼。");
			return;
		}

		final BindResult result = DiscordBindingManager.confirmBind(code, discordId);
		switch (result)
		{
			case SUCCESS:
			{
				reply(event, "✅ **綁定成功！**\n您的 Discord 帳號已與遊戲角色連結。\n今後由我為你服務。");
				LOGGER.info("DiscordEventListener: Discord ID " + discordId + " 綁定成功，驗證碼：" + code);
				break;
			}
			case CODE_INVALID:
			{
				reply(event, "❌ 驗證碼無效或已過期。\n請在遊戲內重新輸入 `.discord bind` 取得新的驗證碼。");
				break;
			}
			case DISCORD_ALREADY_BOUND:
			{
				reply(event, "⚠️ 您的 Discord 帳號已綁定其他角色。\n如需換綁，請先輸入 `!unbind` 解除後再重新綁定。");
				break;
			}
			case DB_ERROR:
			{
				reply(event, "❌ 伺服器發生錯誤，請稍後再試。");
				break;
			}
		}
	}

	private void handleUnbind(MessageReceivedEvent event, String discordId)
	{
		// 找出綁定此 Discord 帳號的玩家（透過 DB 查詢）
		final int playerId = DiscordDAO.getPlayerByDiscordId(discordId);
		if (playerId == -1)
		{
			reply(event, "⚠️ 您的帳號目前沒有任何綁定。");
			return;
		}

		final boolean success = DiscordBindingManager.unbind(playerId);
		if (success)
		{
			reply(event, "✅ 已成功解除綁定。您可以重新執行 `!bind` 來綁定其他角色。");
			LOGGER.info("DiscordEventListener: Discord ID " + discordId + " 已解除綁定（playerId=" + playerId + "）");
		}
		else
		{
			reply(event, "❌ 解除綁定時發生錯誤，請稍後再試。");
		}
	}

	private void handleStatus(MessageReceivedEvent event, String discordId)
	{
		final int playerId = DiscordDAO.getPlayerByDiscordId(discordId);
		if (playerId == -1)
		{
			reply(event, "📋 **綁定狀態：未綁定**\n輸入 `!help` 了解如何綁定遊戲角色。");
			return;
		}

		final DiscordBinding binding = DiscordDAO.getBinding(playerId);
		if (binding != null)
		{
			reply(event, "📋 **綁定狀態：已綁定**\n🎮 角色名稱：**" + binding.getPlayerName() + "**");
		}
		else
		{
			reply(event, "📋 **綁定狀態：未綁定**");
		}
	}

	private void handleHelp(MessageReceivedEvent event)
	{
		reply(event,
			"🤖 **武魂天堂2 Discord Bot 使用說明**\n\n" +
			"**綁定遊戲角色：**\n" +
			"1. 在遊戲內找到Discord小助手取得驗證碼\n" +
			"2. 取得驗證碼後，在此輸入 `!bind <驗證碼>`\n\n" +
			"**指令列表：**\n" +
			"`!bind <驗證碼>` — 綁定遊戲角色\n" +
			"`!unbind` — 解除綁定\n" +
			"`!status` — 查看綁定狀態\n" +
			"`!help` — 顯示此說明\n\n" +
			"綁定後即可享有未來開發的新功能的使用權限。"
		);
	}

	// ── 新成員加入 ───────────────────────────────────────────────────────────

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event)
	{
		// Bot 自己加入時不處理
		if (event.getUser().isBot())
		{
			return;
		}

		// 功能未啟用則跳過
		if (!DiscordConfig.DISCORD_WELCOME_ENABLED)
		{
			return;
		}

		// 歡迎訊息為空則跳過
		final String welcomeMsg = DiscordConfig.DISCORD_WELCOME_MESSAGE;
		if (welcomeMsg == null || welcomeMsg.isEmpty())
		{
			return;
		}

		// 發送私訊歡迎訊息
		event.getUser().openPrivateChannel().queue(
			channel -> channel.sendMessage(welcomeMsg).queue(
				success -> LOGGER.info("DiscordEventListener: 已發送歡迎訊息給新成員 " + event.getUser().getName()),
				error -> LOGGER.warning("DiscordEventListener: 無法發送歡迎訊息給 " + event.getUser().getName() + "：" + error.getMessage())
			),
			error -> LOGGER.warning("DiscordEventListener: 無法開啟 DM 給新成員 " + event.getUser().getName() + "：" + error.getMessage())
		);
	}

	// ── 工具方法 ─────────────────────────────────────────────────────────────

	private void reply(MessageReceivedEvent event, String message)
	{
		event.getChannel().sendMessage(message).queue(
			success -> {},
			error -> LOGGER.warning("DiscordEventListener: 無法發送回覆訊息：" + error.getMessage())
		);
	}
}
