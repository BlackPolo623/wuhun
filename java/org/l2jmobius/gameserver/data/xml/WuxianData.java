package org.l2jmobius.gameserver.data.xml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.holders.WuxianDataHolder;

public class WuxianData
{
	private static final Logger LOGGER = Logger.getLogger(WuxianData.class.getName());

	/** 緩存所有無線數據 */
	private final List<WuxianDataHolder> _dataList = new ArrayList<>();

	/** 玩家ID索引 - 加速查詢 */
	private final Map<Integer, List<WuxianDataHolder>> _dataByPlayerId = new ConcurrentHashMap<>();

	private WuxianData()
	{
		load();
	}

	public synchronized void load()
	{
		_dataList.clear();
		_dataByPlayerId.clear();
		loadFromDatabase();
		LOGGER.info("無限成長系統: 已載入 " + _dataList.size() + " 筆人物加成數據.");
	}

	private void loadFromDatabase()
	{
		final String query = "SELECT id, shuzhi, stat, level FROM wxchar";
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(query);
			 ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final int id = rs.getInt("id");
				final int value = rs.getInt("shuzhi");
				final String stat = rs.getString("stat");
				final int level = rs.getInt("level");

				WuxianDataHolder holder = new WuxianDataHolder(id, value, stat, level);
				_dataList.add(holder);

				// 建立索引
				_dataByPlayerId.computeIfAbsent(id, k -> new ArrayList<>()).add(holder);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "無限成長系統: 讀取人物數據出錯", e);
		}
	}

	/**
	 *  優化: O(1) 查詢
	 */
	public List<WuxianDataHolder> getByPlayerId(int id)
	{
		List<WuxianDataHolder> result = _dataByPlayerId.get(id);
		return result != null ? new ArrayList<>(result) : new ArrayList<>();
	}

	public synchronized List<WuxianDataHolder> getAll()
	{
		return new ArrayList<>(_dataList);
	}

	/**
	 *  優化: 先寫數據庫再更新內存
	 */
	public synchronized void add(int id, int value, String stat, int level)
	{
		final WuxianDataHolder holder = new WuxianDataHolder(id, value, stat, level);

		if (insertToDatabase(holder))
		{
			_dataList.add(holder);
			_dataByPlayerId.computeIfAbsent(id, k -> new ArrayList<>()).add(holder);
		}
	}

	public synchronized void remove(int id, int value, String stat, int level)
	{
		// 先刪數據庫
		if (deleteFromDatabase(id, value, stat))
		{
			// 成功後刪內存
			final Iterator<WuxianDataHolder> it = _dataList.iterator();
			while (it.hasNext())
			{
				final WuxianDataHolder holder = it.next();
				if ((holder.getId() == id) && (holder.getshuzhi() == value) &&
						holder.getstat().equalsIgnoreCase(stat) && (holder.getlevel() == level))
				{
					it.remove();

					// 更新索引
					List<WuxianDataHolder> list = _dataByPlayerId.get(id);
					if (list != null)
					{
						list.remove(holder);
					}
					break;
				}
			}
		}
	}

	/**
	 *  優化: 添加 synchronized
	 */
	public synchronized void updateItemValue(int id, int value, String stat, int level)
	{
		for (WuxianDataHolder holder : _dataList)
		{
			if ((holder.getId() == id) && holder.getstat().equalsIgnoreCase(stat) &&
					(holder.getlevel() == level))
			{
				// 先更新數據庫
				if (updateInDatabase(id, value, stat, level))
				{
					// 成功後才更新內存
					holder.setshuzhi(value);
				}
				return;
			}
		}
	}

	/**
	 *  返回 boolean 表示成功/失敗
	 */
	private boolean insertToDatabase(WuxianDataHolder holder)
	{
		final String sql = "INSERT INTO wxchar (id,shuzhi,stat,level) VALUES (?,?,?,?)";
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, holder.getId());
			ps.setInt(2, holder.getshuzhi());
			ps.setString(3, holder.getstat());
			ps.setInt(4, holder.getlevel());
			ps.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "無限成長系統: 寫入出錯 playerId=" + holder.getId(), e);
			return false;
		}
	}

	private boolean updateInDatabase(int id, int value, String stat, int level)
	{
		final String sql = "UPDATE wxchar SET shuzhi = ? WHERE id = ? AND stat = ? AND level = ?";
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, value);
			ps.setInt(2, id);
			ps.setString(3, stat);
			ps.setInt(4, level);
			ps.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "無限成長系統: 更新出錯", e);
			return false;
		}
	}

	private boolean deleteFromDatabase(int id, int value, String stat)
	{
		final String sql = "DELETE FROM wxchar WHERE id = ? AND shuzhi = ? AND stat = ?";
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(sql))
		{
			ps.setInt(1, id);
			ps.setInt(2, value);
			ps.setString(3, stat);
			ps.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "無限成長系統: 刪除出錯", e);
			return false;
		}
	}

	public static WuxianData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		private static final WuxianData INSTANCE = new WuxianData();
	}
}