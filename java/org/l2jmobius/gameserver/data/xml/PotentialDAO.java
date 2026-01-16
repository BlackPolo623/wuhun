package org.l2jmobius.gameserver.data.xml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;

public class PotentialDAO
{
	private static final Logger LOGGER = Logger.getLogger(PotentialDAO.class.getName());

	private static final String LOAD_QUERY = "SELECT slot_index, skill_id FROM player_potentials WHERE player_id = ?";
	private static final String INSERT_QUERY = "INSERT INTO player_potentials (player_id, slot_index, skill_id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE skill_id = ?";
	private static final String DELETE_QUERY = "DELETE FROM player_potentials WHERE player_id = ?";

	// 待選擇記錄相關SQL
	private static final String LOAD_PENDING_QUERY = "SELECT pending_type, old_data, new_data FROM player_potential_pending WHERE player_id = ?";
	private static final String INSERT_PENDING_QUERY = "INSERT INTO player_potential_pending (player_id, pending_type, old_data, new_data, created_time) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE pending_type = ?, old_data = ?, new_data = ?, created_time = ?";
	private static final String DELETE_PENDING_QUERY = "DELETE FROM player_potential_pending WHERE player_id = ?";

	public static Map<Integer, Integer> loadPotentials(int playerId)
	{
		Map<Integer, Integer> potentials = new HashMap<>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(LOAD_QUERY))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					int slotIndex = rs.getInt("slot_index");
					int skillId = rs.getInt("skill_id");
					potentials.put(slotIndex, skillId);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "無法載入玩家潛能: " + playerId, e);
		}

		return potentials;
	}

	public static void savePotential(int playerId, int slotIndex, int skillId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(INSERT_QUERY))
		{
			ps.setInt(1, playerId);
			ps.setInt(2, slotIndex);
			ps.setInt(3, skillId);
			ps.setInt(4, skillId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "無法保存玩家潛能: " + playerId, e);
		}
	}

	public static void deletePotentials(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(DELETE_QUERY))
		{
			ps.setInt(1, playerId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "無法刪除玩家潛能: " + playerId, e);
		}
	}

	/**
	 * 載入待選擇記錄
	 * @return [0]=type, [1]=oldData, [2]=newData; null if not exists
	 */
	public static String[] loadPendingChoice(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(LOAD_PENDING_QUERY))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					String[] result = new String[3];
					result[0] = String.valueOf(rs.getInt("pending_type"));
					result[1] = rs.getString("old_data");
					result[2] = rs.getString("new_data");
					return result;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "無法載入待選擇記錄: " + playerId, e);
		}

		return null;
	}

	/**
	 * 保存待選擇記錄
	 * @param pendingType 1=結合, 2=自由
	 */
	public static void savePendingChoice(int playerId, int pendingType, String oldData, String newData)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(INSERT_PENDING_QUERY))
		{
			long now = System.currentTimeMillis();
			ps.setInt(1, playerId);
			ps.setInt(2, pendingType);
			ps.setString(3, oldData);
			ps.setString(4, newData);
			ps.setLong(5, now);
			// ON DUPLICATE KEY UPDATE
			ps.setInt(6, pendingType);
			ps.setString(7, oldData);
			ps.setString(8, newData);
			ps.setLong(9, now);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "無法保存待選擇記錄: " + playerId, e);
		}
	}

	/**
	 * 刪除待選擇記錄
	 */
	public static void deletePendingChoice(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(DELETE_PENDING_QUERY))
		{
			ps.setInt(1, playerId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "無法刪除待選擇記錄: " + playerId, e);
		}
	}
}