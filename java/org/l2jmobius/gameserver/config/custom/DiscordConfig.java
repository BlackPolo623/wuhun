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
package org.l2jmobius.gameserver.config.custom;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * Discord Bot 設定讀取器
 * 設定檔位置：config/Custom/Discord.ini
 * @author Custom
 */
public class DiscordConfig
{
	private static final Logger LOGGER = Logger.getLogger(DiscordConfig.class.getName());

	// 設定檔路徑
	private static final String DISCORD_CONFIG_FILE = "./config/Custom/Discord.ini";
	private static final String DISCORD_WELCOME_FILE = "./config/Custom/DiscordWelcome.txt";

	// ── 設定值 ──────────────────────────────────────────────────────────────

	/** 是否啟用 Discord Bot */
	public static boolean DISCORD_ENABLED;

	/** Discord Bot Token */
	public static String DISCORD_BOT_TOKEN;

	/** 綁定驗證碼有效時間（分鐘） */
	public static int DISCORD_BIND_CODE_EXPIRE_MINUTES;

	/** 是否啟用 PVP/PK Discord 頻道公告 */
	public static boolean DISCORD_PVP_ANNOUNCE_ENABLED;

	/** PVP/PK 公告目標頻道 ID */
	public static String DISCORD_PVP_CHANNEL_ID;

	/** 是否啟用新成員加入 Discord 伺服器時的歡迎私訊功能 */
	public static boolean DISCORD_WELCOME_ENABLED;

	/** 新成員加入 Discord 伺服器時發送的歡迎訊息（從 DiscordWelcome.txt 載入） */
	public static String DISCORD_WELCOME_MESSAGE;

	// ── 讀取方法 ─────────────────────────────────────────────────────────────

	public static void load()
	{
		final ConfigReader config = new ConfigReader(DISCORD_CONFIG_FILE);

		DISCORD_ENABLED = config.getBoolean("DiscordBotEnabled", false);
		DISCORD_BOT_TOKEN = config.getString("BotToken", "").trim();
		DISCORD_BIND_CODE_EXPIRE_MINUTES = config.getInt("BindCodeExpireMinutes", 5);
		DISCORD_PVP_ANNOUNCE_ENABLED = config.getBoolean("DiscordPvpAnnounceEnabled", false);
		DISCORD_PVP_CHANNEL_ID = config.getString("DiscordPvpChannelId", "").trim();
		DISCORD_WELCOME_ENABLED = config.getBoolean("DiscordWelcomeEnabled", false);

		// 載入歡迎訊息
		try
		{
			DISCORD_WELCOME_MESSAGE = Files.readString(Paths.get(DISCORD_WELCOME_FILE), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			DISCORD_WELCOME_MESSAGE = "";
			LOGGER.warning("DiscordConfig: 找不到歡迎訊息檔案 " + DISCORD_WELCOME_FILE + "，歡迎訊息功能將停用。");
		}

		// Token 為空時自動停用
		if (DISCORD_ENABLED && DISCORD_BOT_TOKEN.isEmpty())
		{
			LOGGER.warning("DiscordConfig: 已啟用 Discord Bot 但 BotToken 為空！自動停用。");
			DISCORD_ENABLED = false;
		}

		if (DISCORD_ENABLED)
		{
			LOGGER.info("DiscordConfig: Discord Bot 已啟用，驗證碼有效期 " + DISCORD_BIND_CODE_EXPIRE_MINUTES + " 分鐘。");
		}
	}
}
