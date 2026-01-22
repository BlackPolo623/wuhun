package custom.PlayerBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;

public class PlayerBaseDAO
{
	private static final Logger LOGGER = Logger.getLogger(PlayerBaseDAO.class.getName());

	// ==================== 基地主表操作 ====================

	public static boolean hasBase(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement("SELECT player_id FROM player_base WHERE player_id = ?"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				return rs.next();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "檢查基地失敗: " + e.getMessage(), e);
		}
		return false;
	}

	public static boolean createBase(int playerId, String playerName, int instanceId, int templateId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "INSERT INTO player_base (player_id, player_name, instance_id, template_id, max_monster_count, created_time) VALUES (?, ?, ?, ?, 50, ?)"))
		{
			ps.setInt(1, playerId);
			ps.setString(2, playerName);
			ps.setInt(3, instanceId);
			ps.setInt(4, templateId);
			ps.setLong(5, System.currentTimeMillis());
			ps.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "創建基地失敗: " + e.getMessage(), e);
		}
		return false;
	}

	public static Map<String, Object> getBaseInfo(int playerId)
	{
		Map<String, Object> info = new HashMap<>();
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement("SELECT * FROM player_base WHERE player_id = ?"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					info.put("instance_id", rs.getInt("instance_id"));
					info.put("template_id", rs.getInt("template_id"));
					info.put("max_monster_count", rs.getInt("max_monster_count"));
					info.put("player_name", rs.getString("player_name"));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "獲取基地信息失敗: " + e.getMessage(), e);
		}
		return info;
	}

	public static List<Map<String, Object>> getVisitableBases(int visitorId)
	{
		List<Map<String, Object>> bases = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "SELECT pb.player_id, pb.player_name, pb.instance_id, pb.template_id " +
							 "FROM player_base pb " +
							 "INNER JOIN player_base_visitors pv ON pb.player_id = pv.base_owner_id " +
							 "WHERE pv.visitor_id = ?"))
		{
			ps.setInt(1, visitorId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					Map<String, Object> base = new HashMap<>();
					base.put("player_id", rs.getInt("player_id"));
					base.put("player_name", rs.getString("player_name"));
					base.put("instance_id", rs.getInt("instance_id"));
					base.put("template_id", rs.getInt("template_id"));
					bases.add(base);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "獲取可訪問基地列表失敗: " + e.getMessage(), e);
		}
		return bases;
	}

	// ==================== 訪客管理 ====================

	public static boolean addVisitor(int ownerId, int visitorId, String visitorName)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "INSERT INTO player_base_visitors (base_owner_id, visitor_id, visitor_name, added_time) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE visitor_name = ?, added_time = ?"))
		{
			long time = System.currentTimeMillis();
			ps.setInt(1, ownerId);
			ps.setInt(2, visitorId);
			ps.setString(3, visitorName);
			ps.setLong(4, time);
			ps.setString(5, visitorName);
			ps.setLong(6, time);
			ps.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "添加訪客失敗: " + e.getMessage(), e);
		}
		return false;
	}

	public static boolean removeVisitor(int ownerId, int visitorId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement("DELETE FROM player_base_visitors WHERE base_owner_id = ? AND visitor_id = ?"))
		{
			ps.setInt(1, ownerId);
			ps.setInt(2, visitorId);
			ps.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "移除訪客失敗: " + e.getMessage(), e);
		}
		return false;
	}

	public static List<Map<String, Object>> getVisitors(int ownerId)
	{
		List<Map<String, Object>> visitors = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement("SELECT * FROM player_base_visitors WHERE base_owner_id = ?"))
		{
			ps.setInt(1, ownerId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					Map<String, Object> visitor = new HashMap<>();
					visitor.put("visitor_id", rs.getInt("visitor_id"));
					visitor.put("visitor_name", rs.getString("visitor_name"));
					visitor.put("added_time", rs.getLong("added_time"));
					visitors.add(visitor);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "獲取訪客列表失敗: " + e.getMessage(), e);
		}
		return visitors;
	}

	public static boolean canVisit(int ownerId, int visitorId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement("SELECT visitor_id FROM player_base_visitors WHERE base_owner_id = ? AND visitor_id = ?"))
		{
			ps.setInt(1, ownerId);
			ps.setInt(2, visitorId);
			try (ResultSet rs = ps.executeQuery())
			{
				return rs.next();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "檢查訪問權限失敗: " + e.getMessage(), e);
		}
		return false;
	}

	// ==================== 怪物配置管理（簡化版） ====================

	/**
	 * 添加怪物配置（累加數量）
	 */
	public static boolean addMonsterConfig(int ownerId, int monsterId, int count)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "INSERT INTO player_base_monsters (base_owner_id, monster_id, monster_count) " +
							 "VALUES (?, ?, ?) " +
							 "ON DUPLICATE KEY UPDATE monster_count = monster_count + ?"))
		{
			ps.setInt(1, ownerId);
			ps.setInt(2, monsterId);
			ps.setInt(3, count);
			ps.setInt(4, count);
			ps.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "添加怪物配置失敗: " + e.getMessage(), e);
		}
		return false;
	}

	/**
	 * 移除怪物配置（刪除整個配置）
	 */
	public static boolean removeMonsterConfig(int ownerId, int monsterId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "DELETE FROM player_base_monsters WHERE base_owner_id = ? AND monster_id = ?"))
		{
			ps.setInt(1, ownerId);
			ps.setInt(2, monsterId);
			ps.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "移除怪物配置失敗: " + e.getMessage(), e);
		}
		return false;
	}

	/**
	 * 獲取所有怪物配置（按怪物ID分組）
	 */
	public static List<Map<String, Integer>> getAllMonsterConfigs(int ownerId)
	{
		List<Map<String, Integer>> configs = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "SELECT monster_id, SUM(monster_count) as total " +
							 "FROM player_base_monsters " +
							 "WHERE base_owner_id = ? " +
							 "GROUP BY monster_id"))
		{
			ps.setInt(1, ownerId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					Map<String, Integer> config = new HashMap<>();
					config.put("monster_id", rs.getInt("monster_id"));
					config.put("monster_count", rs.getInt("total"));
					configs.add(config);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "獲取怪物配置失敗: " + e.getMessage(), e);
		}
		return configs;
	}

	/**
	 * 獲取當前怪物總數
	 */
	public static int getCurrentMonsterCount(int ownerId)
	{
		int count = 0;
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "SELECT SUM(monster_count) as total " +
							 "FROM player_base_monsters " +
							 "WHERE base_owner_id = ?"))
		{
			ps.setInt(1, ownerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					count = rs.getInt("total");
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "獲取當前怪物數量失敗: " + e.getMessage(), e);
		}
		return count;
	}

	// ==================== 拜訪系統管理 ====================

	/**
	 * 檢查今日是否已經拜訪過
	 */
	public static boolean hasDailyVisitedToday(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "SELECT COUNT(*) as count FROM player_base_daily_visits " +
							 "WHERE visitor_id = ? AND DATE(FROM_UNIXTIME(visit_time/1000)) = CURDATE()"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt("count") > 0;
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "檢查今日拜訪次數失敗: " + e.getMessage(), e);
		}
		return false;
	}

	/**
	 * 獲取今日已拜訪次數
	 */
	public static int getTodayVisitCount(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "SELECT COUNT(*) as count FROM player_base_daily_visits " +
							 "WHERE visitor_id = ? AND DATE(FROM_UNIXTIME(visit_time/1000)) = CURDATE()"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt("count");
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "獲取今日拜訪次數失敗: " + e.getMessage(), e);
		}
		return 0;
	}

	/**
	 * 獲取隨機一個有基地的玩家（排除自己和今天已拜訪過的）
	 */
	public static Map<String, Object> getRandomBaseForVisit(int visitorId)
	{
		Map<String, Object> baseInfo = new HashMap<>();
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "SELECT pb.player_id, pb.player_name, pb.instance_id, pb.template_id " +
							 "FROM player_base pb " +
							 "WHERE pb.player_id != ? " +
							 "AND pb.player_id NOT IN ( " +
							 "  SELECT base_owner_id FROM player_base_daily_visits " +
							 "  WHERE visitor_id = ? AND DATE(FROM_UNIXTIME(visit_time/1000)) = CURDATE() " +
							 ") " +
							 "ORDER BY RAND() LIMIT 1"))
		{
			ps.setInt(1, visitorId);
			ps.setInt(2, visitorId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					baseInfo.put("player_id", rs.getInt("player_id"));
					baseInfo.put("player_name", rs.getString("player_name"));
					baseInfo.put("instance_id", rs.getInt("instance_id"));
					baseInfo.put("template_id", rs.getInt("template_id"));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "獲取隨機基地失敗: " + e.getMessage(), e);
		}
		return baseInfo;
	}

	/**
	 * 記錄拜訪
	 */
	public static boolean recordDailyVisit(int visitorId, String visitorName, int baseOwnerId, String baseOwnerName)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "INSERT INTO player_base_daily_visits (visitor_id, visitor_name, base_owner_id, base_owner_name, visit_time) " +
							 "VALUES (?, ?, ?, ?, ?)"))
		{
			ps.setInt(1, visitorId);
			ps.setString(2, visitorName);
			ps.setInt(3, baseOwnerId);
			ps.setString(4, baseOwnerName);
			ps.setLong(5, System.currentTimeMillis());
			ps.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "記錄拜訪失敗: " + e.getMessage(), e);
		}
		return false;
	}

	/**
	 * 獲取所有有基地的玩家數量（排除自己）
	 */
	public static int getTotalBasesCount(int excludePlayerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "SELECT COUNT(*) as count FROM player_base WHERE player_id != ?"))
		{
			ps.setInt(1, excludePlayerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt("count");
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "獲取基地總數失敗: " + e.getMessage(), e);
		}
		return 0;
	}
}