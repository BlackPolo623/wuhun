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
package org.l2jmobius.gameserver.managers.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * 通用副本進入次數管理器
 * 支援多個副本各自獨立的次數限制和重置時間
 *
 * 重要：所有副本統一在每週一凌晨 00:00 重置次數
 *
 * @author Custom
 */
public class InstanceEntryManager
{
	private static final Logger LOGGER = Logger.getLogger(InstanceEntryManager.class.getName());

	// 資料庫操作
	private static final String INSERT_ENTRY = "REPLACE INTO instance_entries (charId, instance_id, weekly_entries) VALUES (?, ?, ?)";
	private static final String SELECT_ENTRIES = "SELECT charId, instance_id, weekly_entries FROM instance_entries";
	private static final String DELETE_INSTANCE_ENTRIES = "DELETE FROM instance_entries WHERE instance_id = ?";
	private static final String DELETE_ALL_ENTRIES = "DELETE FROM instance_entries";

	// ========================================
	// 統一重置時間設定（所有副本共用）
	// ========================================
	/** 重置日：週一 */
	private static final int RESET_DAY_OF_WEEK = Calendar.MONDAY;
	/** 重置時間：凌晨 0 點 */
	private static final int RESET_HOUR = 0;
	/** 重置時間：0 分 */
	private static final int RESET_MINUTE = 0;
	// ========================================

	// 儲存格式：instanceId -> (playerId -> entryCount)
	private final Map<Integer, Map<Integer, AtomicInteger>> _instanceEntries = new ConcurrentHashMap<>();

	// 副本配置：instanceId -> maxWeeklyEntries
	private final Map<Integer, Integer> _instanceMaxEntries = new ConcurrentHashMap<>();

	// 重置任務（全局唯一，所有副本共用）
	private ScheduledFuture<?> _weeklyResetTask = null;

	// 日期格式化器
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public InstanceEntryManager()
	{
		restoreFromDatabase();
		scheduleWeeklyReset();
	}

	/**
	 * 註冊副本的次數配置
	 * 注意：重置時間統一為週一 00:00，此處只需設定最大次數
	 *
	 * @param instanceId 副本模板ID
	 * @param maxWeeklyEntries 每週最大進入次數
	 */
	public void registerInstance(int instanceId, int maxWeeklyEntries)
	{
		_instanceMaxEntries.put(instanceId, maxWeeklyEntries);
		_instanceEntries.putIfAbsent(instanceId, new ConcurrentHashMap<>());
		LOGGER.info(getClass().getSimpleName() + ": 註冊副本 " + instanceId + " 的次數管理（每週 " + maxWeeklyEntries + " 次，統一於週一 00:00 重置）");
	}

	/**
	 * 檢查玩家是否可以進入副本
	 */
	public boolean canEnter(int instanceId, Player player)
	{
		final Integer maxEntries = _instanceMaxEntries.get(instanceId);
		if (maxEntries == null)
		{
			return true; // 未註冊的副本不限制次數
		}

		final Map<Integer, AtomicInteger> instanceData = _instanceEntries.get(instanceId);
		if (instanceData == null)
		{
			return true;
		}

		final int entries = instanceData.getOrDefault(player.getObjectId(), new AtomicInteger(0)).get();
		return entries < maxEntries;
	}

	/**
	 * 取得玩家剩餘進入次數
	 */
	public int getRemainingEntries(int instanceId, Player player)
	{
		final Integer maxEntries = _instanceMaxEntries.get(instanceId);
		if (maxEntries == null)
		{
			return 999; // 未註冊的副本顯示無限次數
		}

		final Map<Integer, AtomicInteger> instanceData = _instanceEntries.get(instanceId);
		if (instanceData == null)
		{
			return maxEntries;
		}

		final int entries = instanceData.getOrDefault(player.getObjectId(), new AtomicInteger(0)).get();
		return Math.max(0, maxEntries - entries);
	}

	/**
	 * 取得玩家最大進入次數
	 */
	public int getMaxEntries(int instanceId)
	{
		final Integer maxEntries = _instanceMaxEntries.get(instanceId);
		return (maxEntries != null) ? maxEntries : 999;
	}

	/**
	 * 增加玩家的進入次數
	 */
	public void incrementEntryCount(int instanceId, Player player)
	{
		final Map<Integer, AtomicInteger> instanceData = _instanceEntries.computeIfAbsent(instanceId, k -> new ConcurrentHashMap<>());
		instanceData.computeIfAbsent(player.getObjectId(), k -> new AtomicInteger()).incrementAndGet();
		saveToDatabase(instanceId);
	}

	/**
	 * 取得下次重置時間字串
	 */
	public String getNextResetString(int instanceId)
	{
		final Calendar nextReset = getNextResetTime();
		final long remaining = nextReset.getTimeInMillis() - System.currentTimeMillis();
		final long days = remaining / (24 * 60 * 60 * 1000);
		final long hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
		final long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);

		if (days > 0)
		{
			return days + "天" + hours + "時";
		}
		else if (hours > 0)
		{
			return hours + "時" + minutes + "分";
		}
		return minutes + "分";
	}

	/**
	 * 計算下次重置時間
	 */
	private Calendar getNextResetTime()
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_WEEK, RESET_DAY_OF_WEEK);
		calendar.set(Calendar.HOUR_OF_DAY, RESET_HOUR);
		calendar.set(Calendar.MINUTE, RESET_MINUTE);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		// 如果計算出的時間已經過去，則加一週
		if (calendar.getTimeInMillis() <= System.currentTimeMillis())
		{
			calendar.add(Calendar.WEEK_OF_YEAR, 1);
		}

		return calendar;
	}

	/**
	 * 排程每週重置（全局唯一任務）
	 */
	private void scheduleWeeklyReset()
	{
		// 取消舊任務（如果存在）
		if (_weeklyResetTask != null)
		{
			_weeklyResetTask.cancel(false);
		}

		final Calendar nextReset = getNextResetTime();
		final long delay = nextReset.getTimeInMillis() - System.currentTimeMillis();

		LOGGER.info(getClass().getSimpleName() + ": 排程下次重置時間 - " + DATE_FORMAT.format(nextReset.getTime()) + " (距離現在 " + (delay / 1000 / 60) + " 分鐘)");

		_weeklyResetTask = ThreadPool.schedule(() ->
		{
			try
			{
				LOGGER.info(getClass().getSimpleName() + ": 開始執行每週次數重置...");
				resetAllEntries();
				LOGGER.info(getClass().getSimpleName() + ": 所有副本的每週次數已重置完成。");
			}
			catch (Exception e)
			{
				LOGGER.severe(getClass().getSimpleName() + ": 重置次數時發生嚴重錯誤: " + e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				// 重新排程下次重置
				scheduleWeeklyReset();
			}
		}, delay);
	}

	/**
	 * 重置所有副本的進入次數
	 */
	private void resetAllEntries()
	{
		// 清空記憶體中的所有次數記錄
		_instanceEntries.clear();

		// 重新初始化所有已註冊副本的 Map
		for (int instanceId : _instanceMaxEntries.keySet())
		{
			_instanceEntries.put(instanceId, new ConcurrentHashMap<>());
		}

		// 清空資料庫中的所有次數記錄
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_ALL_ENTRIES))
		{
			final int deleted = statement.executeUpdate();
			LOGGER.info(getClass().getSimpleName() + ": 已從資料庫清除 " + deleted + " 筆次數記錄。");
		}
		catch (SQLException e)
		{
			LOGGER.severe(getClass().getSimpleName() + ": 重置次數時發生資料庫錯誤: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 儲存到資料庫
	 */
	private void saveToDatabase(int instanceId)
	{
		final Map<Integer, AtomicInteger> instanceData = _instanceEntries.get(instanceId);
		if (instanceData == null)
		{
			return;
		}

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_ENTRY))
		{
			instanceData.forEach((playerId, entries) ->
			{
				try
				{
					statement.setInt(1, playerId);
					statement.setInt(2, instanceId);
					statement.setInt(3, entries.get());
					statement.addBatch();
				}
				catch (SQLException e)
				{
					LOGGER.severe(getClass().getSimpleName() + ": 儲存次數時發生錯誤: " + e.getMessage());
				}
			});
			statement.executeBatch();
		}
		catch (SQLException e)
		{
			LOGGER.severe(getClass().getSimpleName() + ": 批次儲存次數時發生錯誤: " + e.getMessage());
		}
	}

	/**
	 * 從資料庫讀取
	 */
	private void restoreFromDatabase()
	{
		_instanceEntries.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_ENTRIES);
			ResultSet result = statement.executeQuery())
		{
			int count = 0;
			while (result.next())
			{
				final int playerId = result.getInt("charId");
				final int instanceId = result.getInt("instance_id");
				final int weeklyEntries = result.getInt("weekly_entries");

				final Map<Integer, AtomicInteger> instanceData = _instanceEntries.computeIfAbsent(instanceId, k -> new ConcurrentHashMap<>());
				instanceData.put(playerId, new AtomicInteger(weeklyEntries));
				count++;
			}
			LOGGER.info(getClass().getSimpleName() + ": 從資料庫讀取了 " + count + " 筆次數記錄。");
		}
		catch (SQLException e)
		{
			LOGGER.severe(getClass().getSimpleName() + ": 從資料庫讀取次數時發生錯誤: " + e.getMessage());
		}
	}

	public static InstanceEntryManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final InstanceEntryManager INSTANCE = new InstanceEntryManager();
	}
}
