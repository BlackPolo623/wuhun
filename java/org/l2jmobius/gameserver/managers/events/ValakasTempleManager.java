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
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * 巴拉卡斯神殿 - 每週進入次數管理器
 */
public class ValakasTempleManager
{
	private static final Logger LOGGER = Logger.getLogger(ValakasTempleManager.class.getName());

	private static final String INSERT_ENTRY = "REPLACE INTO valakas_temple_entries (charId, weekly_entries) VALUES (?, ?)";
	private static final String SELECT_ENTRIES = "SELECT charId, weekly_entries FROM valakas_temple_entries";
	private static final String DELETE_ALL_ENTRIES = "DELETE FROM valakas_temple_entries";

	private final ConcurrentHashMap<Integer, AtomicInteger> _playerWeeklyEntries = new ConcurrentHashMap<>();

	// ========================================
	// 設定區：可自行調整
	// ========================================
	/** 每週最大進入次數 */
	public static final int MAX_WEEKLY_ENTRIES = 1;
	/**
	 * 重置日（輸入數字）：
	 * 1 = 週一, 2 = 週二, 3 = 週三, 4 = 週四
	 * 5 = 週五, 6 = 週六, 7 = 週日
	 */
	private static final int RESET_DAY = 1; // 週三
	/** 重置時間（小時，24小時制） */
	private static final int RESET_HOUR = 0;
	/** 重置時間（分鐘） */
	private static final int RESET_MINUTE = 0;
	// ========================================

	/** 將自訂數字（1-7）轉換為 Calendar.DAY_OF_WEEK 常數 */
	private static int toCalendarDay(int day)
	{
		// Calendar: 1=日, 2=一, 3=二, 4=三, 5=四, 6=五, 7=六
		// 我們的輸入: 1=一, 2=二, 3=三, 4=四, 5=五, 6=六, 7=日
		final int[] map = { Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY };
		final int index = Math.max(0, Math.min(6, day - 1));
		return map[index];
	}

	public ValakasTempleManager()
	{
		restoreFromDatabase();
		scheduleWeeklyReset();
	}

	private void scheduleWeeklyReset()
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_WEEK, toCalendarDay(RESET_DAY));
		calendar.set(Calendar.HOUR_OF_DAY, RESET_HOUR);
		calendar.set(Calendar.MINUTE, RESET_MINUTE);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		if (calendar.getTimeInMillis() <= System.currentTimeMillis())
		{
			calendar.add(Calendar.WEEK_OF_YEAR, 1);
		}

		ThreadPool.schedule(() ->
		{
			resetAllEntries();
			LOGGER.info(getClass().getSimpleName() + ": 巴拉卡斯神殿每週次數已重置。");
			scheduleWeeklyReset();
		}, calendar.getTimeInMillis() - System.currentTimeMillis());
	}

	public boolean canEnter(Player player)
	{
		final int entries = _playerWeeklyEntries.getOrDefault(player.getObjectId(), new AtomicInteger(0)).get();
		return entries < MAX_WEEKLY_ENTRIES;
	}

	public int getRemainingEntries(Player player)
	{
		final int entries = _playerWeeklyEntries.getOrDefault(player.getObjectId(), new AtomicInteger(0)).get();
		return Math.max(0, MAX_WEEKLY_ENTRIES - entries);
	}

	public void incrementEntryCount(Player player)
	{
		_playerWeeklyEntries.computeIfAbsent(player.getObjectId(), k -> new AtomicInteger()).incrementAndGet();
		saveToDatabase();
	}

	public String getNextResetString()
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_WEEK, toCalendarDay(RESET_DAY));
		calendar.set(Calendar.HOUR_OF_DAY, RESET_HOUR);
		calendar.set(Calendar.MINUTE, RESET_MINUTE);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		if (calendar.getTimeInMillis() <= System.currentTimeMillis())
		{
			calendar.add(Calendar.WEEK_OF_YEAR, 1);
		}

		final long remaining = calendar.getTimeInMillis() - System.currentTimeMillis();
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

	private void resetAllEntries()
	{
		_playerWeeklyEntries.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_ALL_ENTRIES))
		{
			statement.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.severe(getClass().getSimpleName() + ": 重置次數時發生錯誤: " + e.getMessage());
		}
	}

	private void saveToDatabase()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_ENTRY))
		{
			_playerWeeklyEntries.forEach((playerId, entries) ->
			{
				try
				{
					statement.setInt(1, playerId);
					statement.setInt(2, entries.get());
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

	private void restoreFromDatabase()
	{
		_playerWeeklyEntries.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_ENTRIES);
			ResultSet result = statement.executeQuery())
		{
			while (result.next())
			{
				final int playerId = result.getInt("charId");
				final int weeklyEntries = result.getInt("weekly_entries");
				_playerWeeklyEntries.put(playerId, new AtomicInteger(weeklyEntries));
			}
		}
		catch (SQLException e)
		{
			LOGGER.severe(getClass().getSimpleName() + ": 從資料庫讀取次數時發生錯誤: " + e.getMessage());
		}
	}

	public static ValakasTempleManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final ValakasTempleManager INSTANCE = new ValakasTempleManager();
	}
}
