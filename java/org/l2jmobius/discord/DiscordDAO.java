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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.discord.model.DiscordBinding;

/**
 * Discord 綁定系統的資料庫操作類別。
 *
 * 對應資料表：
 *   discord_bindings      — 角色 ↔ Discord 帳號永久綁定
 *   discord_pending_codes — 待確認的一次性驗證碼（短暫存在）
 *
 * @author Custom
 */
public class DiscordDAO
{
	private static final Logger LOGGER = Logger.getLogger(DiscordDAO.class.getName());

	// ── SQL 語句 ─────────────────────────────────────────────────────────────

	private static final String GET_DISCORD_ID =
		"SELECT discord_id FROM discord_bindings WHERE player_id = ?";

	private static final String GET_BINDING_BY_PLAYER =
		"SELECT player_id, player_name, discord_id FROM discord_bindings WHERE player_id = ?";

	private static final String INSERT_BINDING =
		"INSERT INTO discord_bindings (player_id, player_name, discord_id) VALUES (?, ?, ?) " +
		"ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), discord_id = VALUES(discord_id)";

	private static final String DELETE_BINDING =
		"DELETE FROM discord_bindings WHERE player_id = ?";

	private static final String IS_DISCORD_BOUND =
		"SELECT COUNT(*) FROM discord_bindings WHERE discord_id = ?";

	private static final String INSERT_PENDING_CODE =
		"INSERT INTO discord_pending_codes (player_id, code, expires_at) VALUES (?, ?, ?) " +
		"ON DUPLICATE KEY UPDATE code = VALUES(code), expires_at = VALUES(expires_at)";

	private static final String GET_PLAYER_BY_CODE =
		"SELECT player_id FROM discord_pending_codes WHERE code = ? AND expires_at > NOW()";

	private static final String DELETE_PENDING_CODE_BY_PLAYER =
		"DELETE FROM discord_pending_codes WHERE player_id = ?";

	private static final String DELETE_PENDING_CODE_BY_CODE =
		"DELETE FROM discord_pending_codes WHERE code = ?";

	private static final String GET_PLAYER_BY_DISCORD_ID =
		"SELECT player_id FROM discord_bindings WHERE discord_id = ?";

	private static final String CLEANUP_EXPIRED_CODES =
		"DELETE FROM discord_pending_codes WHERE expires_at <= NOW()";

	// ── 綁定查詢 ─────────────────────────────────────────────────────────────

	/**
	 * 取得指定玩家綁定的 Discord ID。
	 * @param playerId 玩家 ObjectId
	 * @return Discord 用戶 ID 字串，若未綁定則回傳 null
	 */
	public static String getDiscordId(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(GET_DISCORD_ID))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getString("discord_id");
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DiscordDAO: 無法取得玩家 " + playerId + " 的 Discord ID", e);
		}
		return null;
	}

	/**
	 * 取得指定玩家的完整綁定資料。
	 * @param playerId 玩家 ObjectId
	 * @return DiscordBinding 物件，若未綁定則回傳 null
	 */
	public static DiscordBinding getBinding(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(GET_BINDING_BY_PLAYER))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return new DiscordBinding(rs.getInt("player_id"), rs.getString("player_name"), rs.getString("discord_id"));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DiscordDAO: 無法取得玩家 " + playerId + " 的綁定資料", e);
		}
		return null;
	}

	/**
	 * 建立或更新角色 ↔ Discord 帳號的綁定。
	 * @param playerId   玩家 ObjectId
	 * @param playerName 玩家角色名稱
	 * @param discordId  Discord 用戶 ID
	 * @return true 表示儲存成功
	 */
	public static boolean saveBinding(int playerId, String playerName, String discordId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_BINDING))
		{
			ps.setInt(1, playerId);
			ps.setString(2, playerName);
			ps.setString(3, discordId);
			ps.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DiscordDAO: 無法儲存綁定 playerId=" + playerId + " discordId=" + discordId, e);
		}
		return false;
	}

	/**
	 * 解除指定玩家的 Discord 綁定。
	 * @param playerId 玩家 ObjectId
	 * @return true 表示刪除成功（或本來就沒有綁定）
	 */
	public static boolean deleteBinding(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_BINDING))
		{
			ps.setInt(1, playerId);
			ps.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DiscordDAO: 無法刪除玩家 " + playerId + " 的綁定", e);
		}
		return false;
	}

	/**
	 * 檢查指定 Discord ID 是否已與某角色綁定。
	 * @param discordId Discord 用戶 ID
	 * @return true 表示已有綁定
	 */
	public static boolean isDiscordIdBound(String discordId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(IS_DISCORD_BOUND))
		{
			ps.setString(1, discordId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt(1) > 0;
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DiscordDAO: 無法查詢 Discord ID " + discordId + " 的綁定狀態", e);
		}
		return false;
	}

	// ── 驗證碼操作 ───────────────────────────────────────────────────────────

	/**
	 * 儲存待確認的綁定驗證碼。
	 * @param playerId     玩家 ObjectId
	 * @param code         8 位驗證碼
	 * @param expireMinutes 有效時間（分鐘）
	 */
	public static void savePendingCode(int playerId, String code, int expireMinutes)
	{
		final Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + (expireMinutes * 60L * 1000L));
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_PENDING_CODE))
		{
			ps.setInt(1, playerId);
			ps.setString(2, code);
			ps.setTimestamp(3, expiresAt);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DiscordDAO: 無法儲存驗證碼 playerId=" + playerId, e);
		}
	}

	/**
	 * 根據驗證碼查詢對應的玩家 ID（同時驗證有效期）。
	 * @param code 驗證碼
	 * @return 玩家 ObjectId，若無效或已過期則回傳 -1
	 */
	public static int getPlayerByCode(String code)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(GET_PLAYER_BY_CODE))
		{
			ps.setString(1, code);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt("player_id");
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DiscordDAO: 無法查詢驗證碼 " + code, e);
		}
		return -1;
	}

	/**
	 * 根據玩家 ID 刪除驗證碼（綁定完成後清理）。
	 */
	public static void deletePendingCodeByPlayer(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_PENDING_CODE_BY_PLAYER))
		{
			ps.setInt(1, playerId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DiscordDAO: 無法刪除玩家 " + playerId + " 的驗證碼", e);
		}
	}

	/**
	 * 根據驗證碼字串刪除（避免重複使用）。
	 */
	public static void deletePendingCodeByCode(String code)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_PENDING_CODE_BY_CODE))
		{
			ps.setString(1, code);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DiscordDAO: 無法刪除驗證碼 " + code, e);
		}
	}

	/**
	 * 根據 Discord ID 查詢綁定的玩家 ObjectId。
	 * @param discordId Discord 用戶 ID
	 * @return 玩家 ObjectId，若未綁定則回傳 -1
	 */
	public static int getPlayerByDiscordId(String discordId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(GET_PLAYER_BY_DISCORD_ID))
		{
			ps.setString(1, discordId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt("player_id");
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DiscordDAO: 無法根據 Discord ID " + discordId + " 查詢玩家", e);
		}
		return -1;
	}

	/**
	 * 清除所有已過期的驗證碼（可定期呼叫）。
	 */
	public static void cleanupExpiredCodes()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(CLEANUP_EXPIRED_CODES))
		{
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "DiscordDAO: 清除過期驗證碼失敗", e);
		}
	}
}
