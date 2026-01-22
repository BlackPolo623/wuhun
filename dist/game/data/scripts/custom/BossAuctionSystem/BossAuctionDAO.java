package custom.BossAuctionSystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;

/**
 * Database Access Object for Boss Auction System
 * @author 黑普羅
 */
public class BossAuctionDAO
{
	private static final Logger LOGGER = Logger.getLogger(BossAuctionDAO.class.getName());

	// ==================== 配置相關 ====================
	// 注意：配置已移至 BossAuction.ini 文件
	// 由 BossAuctionConfig.java 負責讀取
	// 不再使用資料庫 boss_auction_config 表

	// ==================== 競標會話相關 ====================

	/**
	 * 創建新的競標會話
	 */
	public static int createAuctionSession(int bossNpcId, String bossName, long killTime, long endTime)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"INSERT INTO boss_auction_sessions (boss_npc_id, boss_name, kill_time, end_time, status) VALUES (?, ?, ?, ?, 'ACTIVE')",
				Statement.RETURN_GENERATED_KEYS))
		{
			ps.setInt(1, bossNpcId);
			ps.setString(2, bossName);
			ps.setLong(3, killTime);
			ps.setLong(4, endTime);
			ps.executeUpdate();

			try (ResultSet rs = ps.getGeneratedKeys())
			{
				if (rs.next())
				{
					return rs.getInt(1);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "創建競標會話失敗", e);
		}
		return -1;
	}

	/**
	 * 更新會話狀態
	 */
	public static boolean updateSessionStatus(int sessionId, String status)
	{
		// 如果是設置為 PROCESSING，需要確保只從 ACTIVE 狀態才能更新
		String sql;
		if ("PROCESSING".equals(status))
		{
			sql = "UPDATE boss_auction_sessions SET status = ? WHERE session_id = ? AND status = 'ACTIVE'";
		}
		else
		{
			sql = "UPDATE boss_auction_sessions SET status = ? WHERE session_id = ?";
		}

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setString(1, status);
			ps.setInt(2, sessionId);
			return ps.executeUpdate() > 0;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "更新會話狀態失敗", e);
		}
		return false;
	}

	/**
	 * 【新增】更新會話結束時間（用於延長競標時間）
	 */
	public static boolean updateSessionEndTime(int sessionId, long newEndTime)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE boss_auction_sessions SET end_time = ? WHERE session_id = ?"))
		{
			ps.setLong(1, newEndTime);
			ps.setInt(2, sessionId);
			return ps.executeUpdate() > 0;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "更新會話結束時間失敗", e);
		}
		return false;
	}

	/**
	 * 獲取進行中的會話列表
	 */
	public static List<AuctionSession> getActiveSessions()
	{
		List<AuctionSession> sessions = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM boss_auction_sessions WHERE status = 'ACTIVE' ORDER BY end_time ASC"))
		{
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					AuctionSession session = new AuctionSession();
					session.sessionId = rs.getInt("session_id");
					session.bossNpcId = rs.getInt("boss_npc_id");
					session.bossName = rs.getString("boss_name");
					session.killTime = rs.getLong("kill_time");
					session.endTime = rs.getLong("end_time");
					session.status = rs.getString("status");
					sessions.add(session);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "獲取進行中會話失敗", e);
		}
		return sessions;
	}

	/**
	 * 檢查會話是否過期
	 */
	public static List<Integer> getExpiredSessions()
	{
		List<Integer> expiredIds = new ArrayList<>();
		long currentTime = System.currentTimeMillis();

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT session_id FROM boss_auction_sessions WHERE status = 'ACTIVE' AND end_time <= ?"))
		{
			ps.setLong(1, currentTime);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					expiredIds.add(rs.getInt("session_id"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "檢查過期會話失敗", e);
		}
		return expiredIds;
	}

	/**
	 * 獲取指定會話
	 */
	public static AuctionSession getSession(int sessionId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM boss_auction_sessions WHERE session_id = ?"))
		{
			ps.setInt(1, sessionId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					AuctionSession session = new AuctionSession();
					session.sessionId = rs.getInt("session_id");
					session.bossNpcId = rs.getInt("boss_npc_id");
					session.bossName = rs.getString("boss_name");
					session.killTime = rs.getLong("kill_time");
					session.endTime = rs.getLong("end_time");
					session.status = rs.getString("status");
					return session;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "獲取會話失敗", e);
		}
		return null;
	}

	// ==================== 競標物品相關 ====================

	/**
	 * 添加競標物品
	 */
	public static int addAuctionItem(int sessionId, int itemId, long itemCount, int enchantLevel, String itemData)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"INSERT INTO boss_auction_items (session_id, item_id, item_count, enchant_level, item_data, status) VALUES (?, ?, ?, ?, ?, 'PENDING')",
				Statement.RETURN_GENERATED_KEYS))
		{
			ps.setInt(1, sessionId);
			ps.setInt(2, itemId);
			ps.setLong(3, itemCount);
			ps.setInt(4, enchantLevel);
			ps.setString(5, itemData);
			ps.executeUpdate();

			try (ResultSet rs = ps.getGeneratedKeys())
			{
				if (rs.next())
				{
					return rs.getInt(1);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "添加競標物品失敗", e);
		}
		return -1;
	}

	/**
	 * 獲取會話的所有物品
	 */
	public static List<AuctionItem> getSessionItems(int sessionId)
	{
		List<AuctionItem> items = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM boss_auction_items WHERE session_id = ? ORDER BY auction_item_id ASC"))
		{
			ps.setInt(1, sessionId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					AuctionItem item = new AuctionItem();
					item.auctionItemId = rs.getInt("auction_item_id");
					item.sessionId = rs.getInt("session_id");
					item.itemId = rs.getInt("item_id");
					item.itemCount = rs.getLong("item_count");
					item.enchantLevel = rs.getInt("enchant_level");
					item.itemData = rs.getString("item_data");
					item.currentBid = rs.getLong("current_bid");
					item.currentBidderId = rs.getInt("current_bidder_id");
					item.currentBidderName = rs.getString("current_bidder_name");
					item.bidCount = rs.getInt("bid_count");
					item.status = rs.getString("status");
					items.add(item);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "獲取會話物品失敗", e);
		}
		return items;
	}

	/**
	 * 獲取單個競標物品
	 */
	public static AuctionItem getAuctionItem(int auctionItemId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM boss_auction_items WHERE auction_item_id = ?"))
		{
			ps.setInt(1, auctionItemId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					AuctionItem item = new AuctionItem();
					item.auctionItemId = rs.getInt("auction_item_id");
					item.sessionId = rs.getInt("session_id");
					item.itemId = rs.getInt("item_id");
					item.itemCount = rs.getLong("item_count");
					item.enchantLevel = rs.getInt("enchant_level");
					item.currentBid = rs.getLong("current_bid");
					item.currentBidderId = rs.getInt("current_bidder_id");
					item.currentBidderName = rs.getString("current_bidder_name");
					item.bidCount = rs.getInt("bid_count");
					item.status = rs.getString("status");
					return item;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "獲取競標物品失敗", e);
		}
		return null;
	}

	/**
	 * 更新物品出價
	 */
	public static boolean updateItemBid(int auctionItemId, long bidAmount, int bidderId, String bidderName)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"UPDATE boss_auction_items SET current_bid = ?, current_bidder_id = ?, current_bidder_name = ?, bid_count = bid_count + 1 WHERE auction_item_id = ?"))
		{
			ps.setLong(1, bidAmount);
			ps.setInt(2, bidderId);
			ps.setString(3, bidderName);
			ps.setInt(4, auctionItemId);
			return ps.executeUpdate() > 0;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "更新物品出價失敗", e);
		}
		return false;
	}

	/**
	 * 更新物品狀態
	 */
	public static boolean updateItemStatus(int auctionItemId, String status)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE boss_auction_items SET status = ? WHERE auction_item_id = ?"))
		{
			ps.setString(1, status);
			ps.setInt(2, auctionItemId);
			return ps.executeUpdate() > 0;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "更新物品狀態失敗", e);
		}
		return false;
	}

	// ==================== 出價記錄相關 ====================

	/**
	 * 記錄出價
	 */
	public static boolean recordBid(int auctionItemId, int sessionId, int playerId, String playerName, long bidAmount, long bidTime)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			// 先將該物品的所有出價設為非當前贏家
			try (PreparedStatement ps = con.prepareStatement("UPDATE boss_auction_bids SET is_current_winner = 0 WHERE auction_item_id = ?"))
			{
				ps.setInt(1, auctionItemId);
				ps.executeUpdate();
			}

			// 添加新出價
			try (PreparedStatement ps = con.prepareStatement(
				"INSERT INTO boss_auction_bids (auction_item_id, session_id, player_id, player_name, bid_amount, bid_time, is_current_winner) VALUES (?, ?, ?, ?, ?, ?, 1)"))
			{
				ps.setInt(1, auctionItemId);
				ps.setInt(2, sessionId);
				ps.setInt(3, playerId);
				ps.setString(4, playerName);
				ps.setLong(5, bidAmount);
				ps.setLong(6, bidTime);
				return ps.executeUpdate() > 0;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "記錄出價失敗", e);
		}
		return false;
	}

	/**
	 * 獲取物品的出價歷史
	 */
	public static List<BidRecord> getItemBidHistory(int auctionItemId)
	{
		List<BidRecord> bids = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM boss_auction_bids WHERE auction_item_id = ? ORDER BY bid_time DESC"))
		{
			ps.setInt(1, auctionItemId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					BidRecord bid = new BidRecord();
					bid.bidId = rs.getInt("bid_id");
					bid.auctionItemId = rs.getInt("auction_item_id");
					bid.playerId = rs.getInt("player_id");
					bid.playerName = rs.getString("player_name");
					bid.bidAmount = rs.getLong("bid_amount");
					bid.bidTime = rs.getLong("bid_time");
					bid.isCurrentWinner = rs.getBoolean("is_current_winner");
					bids.add(bid);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "獲取出價歷史失敗", e);
		}
		return bids;
	}

	// ==================== 參與者相關 ====================

	/**
	 * 記錄BOSS擊殺參與者
	 */
	public static boolean recordParticipant(int sessionId, int playerId, String playerName, long damageDealt)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"INSERT INTO boss_kill_participants (session_id, player_id, player_name, damage_dealt) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE damage_dealt = damage_dealt + ?"))
		{
			ps.setInt(1, sessionId);
			ps.setInt(2, playerId);
			ps.setString(3, playerName);
			ps.setLong(4, damageDealt);
			ps.setLong(5, damageDealt);
			return ps.executeUpdate() > 0;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "記錄參與者失敗", e);
		}
		return false;
	}

	/**
	 * 獲取合格的參與者（傷害>=最低要求）
	 */
	public static List<Participant> getQualifiedParticipants(int sessionId, long minDamage)
	{
		List<Participant> participants = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"SELECT * FROM boss_kill_participants WHERE session_id = ? AND damage_dealt >= ? AND reward_received = 0"))
		{
			ps.setInt(1, sessionId);
			ps.setLong(2, minDamage);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					Participant p = new Participant();
					p.participantId = rs.getInt("participant_id");
					p.sessionId = rs.getInt("session_id");
					p.playerId = rs.getInt("player_id");
					p.playerName = rs.getString("player_name");
					p.damageDealt = rs.getLong("damage_dealt");
					p.rewardReceived = rs.getBoolean("reward_received");
					participants.add(p);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "獲取合格參與者失敗", e);
		}
		return participants;
	}

	/**
	 * 標記參與者已領取獎勵
	 */
	public static boolean markParticipantRewarded(int participantId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE boss_kill_participants SET reward_received = 1 WHERE participant_id = ?"))
		{
			ps.setInt(1, participantId);
			return ps.executeUpdate() > 0;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "標記參與者獎勵失敗", e);
		}
		return false;
	}

	// ==================== 資料類 ====================

	public static class AuctionSession
	{
		public int sessionId;
		public int bossNpcId;
		public String bossName;
		public long killTime;
		public long endTime;
		public String status;
	}

	public static class AuctionItem
	{
		public int auctionItemId;
		public int sessionId;
		public int itemId;
		public long itemCount;
		public int enchantLevel;
		public String itemData;
		public long currentBid;
		public int currentBidderId;
		public String currentBidderName;
		public int bidCount;
		public String status;
	}

	public static class BidRecord
	{
		public int bidId;
		public int auctionItemId;
		public int playerId;
		public String playerName;
		public long bidAmount;
		public long bidTime;
		public boolean isCurrentWinner;
	}

	public static class Participant
	{
		public int participantId;
		public int sessionId;
		public int playerId;
		public String playerName;
		public long damageDealt;
		public boolean rewardReceived;
	}

	// ==================== 待領取獎勵相關 ====================

	/**
	 * 添加待領取獎勵
	 */
	public static int addPendingReward(int playerId, String playerName, int sessionId, int bossId, String bossName,
		String rewardType, int itemId, long itemCount, String itemData, long bidAmount, long damageDealt)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"INSERT INTO boss_auction_pending_rewards " +
				"(player_id, player_name, session_id, boss_id, boss_name, reward_type, item_id, item_count, item_data, bid_amount, damage_dealt, create_time, status) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')",
				Statement.RETURN_GENERATED_KEYS))
		{
			ps.setInt(1, playerId);
			ps.setString(2, playerName);
			ps.setInt(3, sessionId);
			ps.setInt(4, bossId);
			ps.setString(5, bossName);
			ps.setString(6, rewardType);
			ps.setInt(7, itemId);
			ps.setLong(8, itemCount);
			ps.setString(9, itemData);
			ps.setLong(10, bidAmount);
			ps.setLong(11, damageDealt);
			ps.setLong(12, System.currentTimeMillis());

			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys())
			{
				if (rs.next())
				{
					return rs.getInt(1);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "添加待領取獎勵失敗", e);
		}
		return -1;
	}

	/**
	 * 獲取玩家待領取的獎勵列表
	 */
	public static List<PendingReward> getPlayerPendingRewards(int playerId)
	{
		List<PendingReward> rewards = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"SELECT * FROM boss_auction_pending_rewards WHERE player_id = ? AND status = 'PENDING' ORDER BY create_time DESC"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					PendingReward reward = new PendingReward();
					reward.rewardId = rs.getInt("reward_id");
					reward.playerId = rs.getInt("player_id");
					reward.playerName = rs.getString("player_name");
					reward.sessionId = rs.getInt("session_id");
					reward.bossId = rs.getInt("boss_id");
					reward.bossName = rs.getString("boss_name");
					reward.rewardType = rs.getString("reward_type");
					reward.itemId = rs.getInt("item_id");
					reward.itemCount = rs.getLong("item_count");
					reward.itemData = rs.getString("item_data");
					reward.bidAmount = rs.getLong("bid_amount");
					reward.damageDealt = rs.getLong("damage_dealt");
					reward.createTime = rs.getLong("create_time");
					reward.status = rs.getString("status");
					rewards.add(reward);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "獲取待領取獎勵失敗", e);
		}
		return rewards;
	}

	/**
	 * 標記獎勵為已領取
	 */
	public static boolean claimReward(int rewardId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"UPDATE boss_auction_pending_rewards SET status = 'CLAIMED', claim_time = ? WHERE reward_id = ? AND status = 'PENDING'"))
		{
			ps.setLong(1, System.currentTimeMillis());
			ps.setInt(2, rewardId);
			return ps.executeUpdate() > 0;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "標記獎勵為已領取失敗", e);
		}
		return false;
	}

	/**
	 * 獲取玩家待領取獎勵數量
	 */
	public static int getPlayerPendingRewardCount(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"SELECT COUNT(*) FROM boss_auction_pending_rewards WHERE player_id = ? AND status = 'PENDING'"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt(1);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "獲取待領取獎勵數量失敗", e);
		}
		return 0;
	}

	/**
	 * 待領取獎勵數據結構
	 */
	public static class PendingReward
	{
		public int rewardId;
		public int playerId;
		public String playerName;
		public int sessionId;
		public int bossId;
		public String bossName;
		public String rewardType; // BID_WIN 或 REVENUE_SHARE
		public int itemId;
		public long itemCount;
		public String itemData;
		public long bidAmount;
		public long damageDealt;
		public long createTime;
		public String status;
	}
}
