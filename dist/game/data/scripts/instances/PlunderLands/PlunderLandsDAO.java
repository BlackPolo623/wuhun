package instances.PlunderLands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;

/**
 * PlunderLands 資料庫存取層。
 * 所有 SQL 語句集中於此，腳本本體不直接操作 Connection。
 */
public class PlunderLandsDAO
{
	private static final Logger LOGGER = Logger.getLogger(PlunderLandsDAO.class.getName());

	// ==================== 單例 ====================

	private static final PlunderLandsDAO INSTANCE = new PlunderLandsDAO();

	public static PlunderLandsDAO getInstance()
	{
		return INSTANCE;
	}

	private PlunderLandsDAO()
	{
	}

	// ==================== 資料模型 ====================

	/** 上週結算結果 */
	public static class WinnerRecord
	{
		public final int    clanId;
		public final String clanName;
		public final long   weekStart;
		public final int    totalScore;

		WinnerRecord(int clanId, String clanName, long weekStart, int totalScore)
		{
			this.clanId     = clanId;
			this.clanName   = clanName;
			this.weekStart  = weekStart;
			this.totalScore = totalScore;
		}
	}

	// ==================== 讀取 ====================

	/**
	 * 載入最新一筆上週結算結果。
	 * @return WinnerRecord，若無紀錄則回傳 null
	 */
	public WinnerRecord loadLastWinner()
	{
		final String sql = "SELECT clan_id, clan_name, week_start, total_score FROM plunder_last_winner ORDER BY week_start DESC LIMIT 1";
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				return new WinnerRecord(
					rs.getInt("clan_id"),
					rs.getString("clan_name"),
					rs.getLong("week_start"),
					rs.getInt("total_score"));
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("PlunderLandsDAO.loadLastWinner: " + e.getMessage());
		}
		return null;
	}

	/**
	 * 將 DB 中的本週積分恢復至 in-memory 結構。
	 * @param weekScores  目標積分 Map（clanId → playerId → AtomicInteger）
	 * @param clanNames   血盟名稱快取
	 * @param playerNames 玩家名稱快取
	 */
	public void loadScores(
		ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, AtomicInteger>> weekScores,
		ConcurrentHashMap<Integer, String> clanNames,
		ConcurrentHashMap<Integer, String> playerNames)
	{
		final String sql = "SELECT clan_id, player_id, clan_name, player_name, score FROM plunder_scores";
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final int clanId   = rs.getInt("clan_id");
				final int playerId = rs.getInt("player_id");
				clanNames.put(clanId, rs.getString("clan_name"));
				playerNames.put(playerId, rs.getString("player_name"));
				weekScores.computeIfAbsent(clanId, k -> new ConcurrentHashMap<>())
				          .computeIfAbsent(playerId, k -> new AtomicInteger(0))
				          .set(rs.getInt("score"));
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("PlunderLandsDAO.loadScores: " + e.getMessage());
		}
	}

	/**
	 * 查詢玩家是否有未領取的獎勵。
	 * @param playerId  玩家 ObjectId
	 * @param weekStart 結算週開始時間戳（毫秒）
	 */
	public boolean hasPendingRewards(int playerId, long weekStart)
	{
		final String sql = "SELECT COUNT(*) FROM plunder_rewards WHERE player_id=? AND week_start=? AND claimed=0";
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, playerId);
			ps.setLong(2, weekStart);
			try (ResultSet rs = ps.executeQuery())
			{
				return rs.next() && (rs.getInt(1) > 0);
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("PlunderLandsDAO.hasPendingRewards: " + e.getMessage());
		}
		return false;
	}

	/**
	 * 取得玩家所有未領取的獎勵列表。
	 * @return List of {itemId, amount}
	 */
	public List<long[]> getPendingRewards(int playerId, long weekStart)
	{
		final List<long[]> result = new ArrayList<>();
		final String sql = "SELECT item_id, amount FROM plunder_rewards WHERE player_id=? AND week_start=? AND claimed=0";
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, playerId);
			ps.setLong(2, weekStart);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					result.add(new long[]{ rs.getInt("item_id"), rs.getLong("amount") });
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("PlunderLandsDAO.getPendingRewards: " + e.getMessage());
		}
		return result;
	}

	// ==================== 寫入 ====================

	/**
	 * 將 in-memory 積分批次 upsert 至 DB。
	 * @param weekScores  積分 Map
	 * @param clanNames   血盟名稱快取
	 * @param playerNames 玩家名稱快取
	 */
	public void flushScores(
		ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, AtomicInteger>> weekScores,
		ConcurrentHashMap<Integer, String> clanNames,
		ConcurrentHashMap<Integer, String> playerNames)
	{
		if (weekScores.isEmpty())
		{
			return;
		}
		final String sql =
			"INSERT INTO plunder_scores (clan_id, player_id, clan_name, player_name, score) VALUES (?,?,?,?,?) " +
			"ON DUPLICATE KEY UPDATE clan_name=VALUES(clan_name), player_name=VALUES(player_name), score=VALUES(score)";
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(sql))
		{
			for (Map.Entry<Integer, ConcurrentHashMap<Integer, AtomicInteger>> clanEntry : weekScores.entrySet())
			{
				final int    clanId   = clanEntry.getKey();
				final String clanName = clanNames.getOrDefault(clanId, "");
				for (Map.Entry<Integer, AtomicInteger> pe : clanEntry.getValue().entrySet())
				{
					ps.setInt(1, clanId);
					ps.setInt(2, pe.getKey());
					ps.setString(3, clanName);
					ps.setString(4, playerNames.getOrDefault(pe.getKey(), ""));
					ps.setInt(5, pe.getValue().get());
					ps.addBatch();
				}
			}
			ps.executeBatch();
		}
		catch (SQLException e)
		{
			LOGGER.warning("PlunderLandsDAO.flushScores: " + e.getMessage());
		}
	}

	/**
	 * 寫入（或覆寫）本週勝者記錄。
	 */
	public void saveWinner(long weekStart, int clanId, String clanName, int totalScore)
	{
		final String sql =
			"INSERT INTO plunder_last_winner (week_start, clan_id, clan_name, total_score) VALUES (?,?,?,?) " +
			"ON DUPLICATE KEY UPDATE clan_id=VALUES(clan_id), clan_name=VALUES(clan_name), total_score=VALUES(total_score)";
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setLong(1, weekStart);
			ps.setInt(2, clanId);
			ps.setString(3, clanName);
			ps.setInt(4, totalScore);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.warning("PlunderLandsDAO.saveWinner: " + e.getMessage());
		}
	}

	/**
	 * 按貢獻比例計算並批次寫入各玩家待領獎勵。
	 * @param weekStart    結算週開始時間戳
	 * @param playerScores 勝者血盟的 playerId → AtomicInteger
	 * @param winnerTotal  該血盟本週總積分
	 * @param rewardConfig 獎勵設定 {{itemId, totalAmount}, ...}
	 */
	public void saveRewards(
		long weekStart,
		ConcurrentHashMap<Integer, AtomicInteger> playerScores,
		int winnerTotal,
		int[][] rewardConfig)
	{
		if ((playerScores == null) || playerScores.isEmpty() || (winnerTotal <= 0))
		{
			return;
		}
		final String sql =
			"INSERT INTO plunder_rewards (player_id, week_start, item_id, amount, claimed) VALUES (?,?,?,?,0) " +
			"ON DUPLICATE KEY UPDATE amount=VALUES(amount), claimed=0";
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(sql))
		{
			for (Map.Entry<Integer, AtomicInteger> me : playerScores.entrySet())
			{
				final int playerScore = me.getValue().get();
				for (int[] reward : rewardConfig)
				{
					final long amount = (long) reward[1] * playerScore / winnerTotal;
					if (amount <= 0)
					{
						continue;
					}
					ps.setInt(1, me.getKey());
					ps.setLong(2, weekStart);
					ps.setInt(3, reward[0]);
					ps.setLong(4, amount);
					ps.addBatch();
				}
			}
			ps.executeBatch();
		}
		catch (SQLException e)
		{
			LOGGER.warning("PlunderLandsDAO.saveRewards: " + e.getMessage());
		}
	}

	/**
	 * 清空本週所有積分（結算後呼叫）。
	 */
	public void clearScores()
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement("DELETE FROM plunder_scores"))
		{
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.warning("PlunderLandsDAO.clearScores: " + e.getMessage());
		}
	}

	/**
	 * 刪除指定週之前所有未領取的獎勵（本週結算時淘汰上週未領）。
	 * @param weekStart 本次結算的週起始時間戳；所有 week_start < 此值且 claimed=0 的記錄將被刪除
	 */
	public void expireUnclaimedRewards(long weekStart)
	{
		final String sql = "DELETE FROM plunder_rewards WHERE week_start < ? AND claimed = 0";
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setLong(1, weekStart);
			final int deleted = ps.executeUpdate();
			if (deleted > 0)
			{
				LOGGER.info("PlunderLandsDAO.expireUnclaimedRewards: 已淘汰 " + deleted + " 筆未領獎勵。");
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("PlunderLandsDAO.expireUnclaimedRewards: " + e.getMessage());
		}
	}

	/**
	 * 將玩家本週所有未領取的獎勵標記為已領取。
	 */
	public void markRewardsClaimed(int playerId, long weekStart)
	{
		final String sql = "UPDATE plunder_rewards SET claimed=1 WHERE player_id=? AND week_start=?";
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, playerId);
			ps.setLong(2, weekStart);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.warning("PlunderLandsDAO.markRewardsClaimed: " + e.getMessage());
		}
	}
}
