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

import java.util.Random;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.custom.DiscordConfig;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * Discord 綁定流程管理器。
 *
 * 綁定流程：
 *   1. 玩家在遊戲輸入 .discord bind
 *   2. {@link #generateCode(Player)} 產生 8 位驗證碼並儲存至 DB
 *   3. 玩家在 Discord 對 Bot 發送 !bind <驗證碼>
 *   4. {@link #confirmBind(String, String)} 驗證碼 → 查玩家 → 寫入綁定 → 刪除驗證碼
 *
 * @author Custom
 */
public class DiscordBindingManager
{
	private static final Logger LOGGER = Logger.getLogger(DiscordBindingManager.class.getName());

	private static final int CODE_LENGTH = 6;
	private static final Random RANDOM = new Random();

	// ── 公開方法 ─────────────────────────────────────────────────────────────

	/**
	 * 為玩家產生並儲存綁定驗證碼。
	 * 若玩家已有綁定則不產生，直接回傳提示。
	 *
	 * @param player 發出 .discord bind 的玩家
	 * @return 產生的驗證碼（8 位英數），若已綁定則回傳 null
	 */
	public static String generateCode(Player player)
	{
		final int playerId = player.getObjectId();

		// 已有綁定，不需要再產生驗證碼
		if (DiscordDAO.getDiscordId(playerId) != null)
		{
			return null;
		}

		final String code = buildRandomCode();
		DiscordDAO.savePendingCode(playerId, code, DiscordConfig.DISCORD_BIND_CODE_EXPIRE_MINUTES);
		LOGGER.info("DiscordBinding: 玩家 " + player.getName() + " 產生驗證碼 " + code);
		return code;
	}

	/**
	 * 玩家在 Discord 輸入 !bind <code> 後，由 Bot 呼叫此方法完成綁定。
	 *
	 * @param code      玩家輸入的 8 位驗證碼
	 * @param discordId 發送命令的 Discord 用戶 ID
	 * @return {@link BindResult} 描述結果的枚舉
	 */
	public static BindResult confirmBind(String code, String discordId)
	{
		// 此 Discord 帳號是否已綁定過其他角色
		if (DiscordDAO.isDiscordIdBound(discordId))
		{
			return BindResult.DISCORD_ALREADY_BOUND;
		}

		// 根據驗證碼查玩家（同時檢查有效期）
		final int playerId = DiscordDAO.getPlayerByCode(code);
		if (playerId == -1)
		{
			return BindResult.CODE_INVALID;
		}

		// 儲存綁定：從 World 取玩家名稱（若在線），否則從 DB 取
		final String playerName = getPlayerName(playerId);
		final boolean saved = DiscordDAO.saveBinding(playerId, playerName, discordId);
		if (!saved)
		{
			return BindResult.DB_ERROR;
		}

		// 清除驗證碼（防止重複使用）
		DiscordDAO.deletePendingCodeByPlayer(playerId);

		LOGGER.info("DiscordBinding: 玩家 " + playerName + "(" + playerId + ") 已綁定 Discord ID " + discordId);
		return BindResult.SUCCESS;
	}

	/**
	 * 解除玩家的 Discord 綁定（由 !unbind 或遊戲內 .discord unbind 觸發）。
	 *
	 * @param playerId 玩家 ObjectId
	 * @return true 表示解除成功
	 */
	public static boolean unbind(int playerId)
	{
		return DiscordDAO.deleteBinding(playerId);
	}

	// ── 輔助方法 ─────────────────────────────────────────────────────────────

	private static String buildRandomCode()
	{
		// 產生 100000–999999 的 6 位數字驗證碼
		return String.valueOf(100000 + RANDOM.nextInt(900000));
	}

	private static String getPlayerName(int playerId)
	{
		// 優先從線上玩家取得最新名稱
		final org.l2jmobius.gameserver.model.World world = org.l2jmobius.gameserver.model.World.getInstance();
		final Player onlinePlayer = world.getPlayer(playerId);
		if (onlinePlayer != null)
		{
			return onlinePlayer.getName();
		}
		// 玩家不在線：回傳占位符（DB 已有名稱的情況由 ON DUPLICATE KEY UPDATE 維持）
		return "Unknown";
	}

	// ── 綁定結果枚舉 ─────────────────────────────────────────────────────────

	public enum BindResult
	{
		/** 綁定成功 */
		SUCCESS,
		/** 驗證碼無效或已過期 */
		CODE_INVALID,
		/** 此 Discord 帳號已綁定其他角色 */
		DISCORD_ALREADY_BOUND,
		/** 資料庫儲存失敗 */
		DB_ERROR
	}
}
