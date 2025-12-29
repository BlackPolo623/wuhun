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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Serenitty
 */
public class LeonasDungeonManager
{
	private static final Logger LOGGER = Logger.getLogger(LeonasDungeonManager.class.getName());
	
	private static final String INSERT_DUNGEON_RANKING = "REPLACE INTO leonas_dungeon_ranking (charId, points) VALUES (?, ?)";
	private static final String SELECT_DUNGEON_RANKING = "SELECT charId, points FROM leonas_dungeon_ranking";
	private static final String DELETE_DUNGEON_RANKING = "DELETE FROM leonas_dungeon_ranking WHERE charId=?";
	private static final String DELETE_ALL_DUNGEON_RANKING = "DELETE FROM leonas_dungeon_ranking";
	
	private final ConcurrentHashMap<Integer, AtomicInteger> _playerPoints = new ConcurrentHashMap<>();
	private final List<Integer> _rewardedPlayers = new ArrayList<>();
	
	public LeonasDungeonManager()
	{
		restoreDungeonRankingFromDatabase();
		scheduleMondayReset();
	}
	
	private void scheduleMondayReset()
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		if (calendar.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
		{
			calendar.add(Calendar.WEEK_OF_MONTH, 1);
		}
		
		ThreadPool.schedule(() ->
		{
			LeonasDungeonManager.getInstance().rewardTopPlayersOnMonday();
			LeonasDungeonManager.getInstance().deleteAllDungeonRankingData();
			LOGGER.info(getClass().getSimpleName() + ": Leona Dungeon reset");
			scheduleMondayReset();
		}, calendar.getTimeInMillis() - System.currentTimeMillis());
	}
	
	public void addPointsForPlayer(Player player, int val)
	{
		_playerPoints.computeIfAbsent(player.getObjectId(), k -> new AtomicInteger()).addAndGet(val);
		saveDungeonRankingToDatabase();
	}
	
	public long getTotalPoints()
	{
		return _playerPoints.values().stream().mapToLong(AtomicInteger::get).sum();
	}
	
	public Map<Integer, Integer> getTopPlayers(int count)
	{
		return _playerPoints.entrySet().stream().sorted(Entry.<Integer, AtomicInteger> comparingByValue(Comparator.comparingInt(AtomicInteger::get).reversed()).thenComparing(Entry.comparingByKey())).limit(count).collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().get(), (e1, e2) -> e1, LinkedHashMap::new));
	}
	
	public int getPlayerRank(Player player)
	{
		final int playerPoints = _playerPoints.getOrDefault(player.getObjectId(), new AtomicInteger(0)).get();
		return (int) _playerPoints.values().stream().filter(points -> points.get() > playerPoints).count() + 1;
	}
	
	public int getPlayerPoints(Player player)
	{
		return _playerPoints.getOrDefault(player.getObjectId(), new AtomicInteger(0)).get();
	}
	
	public void rewardTopPlayersOnMonday()
	{
		int rank = 1;
		final Map<Integer, Integer> topPlayers = getTopPlayers(150);
		for (Entry<Integer, Integer> entry : topPlayers.entrySet())
		{
			final int playerId = entry.getKey();
			int rewardsId = 99845;
			int rewards = 1;
			if (rank == 1)
			{
				rewards = 10;
				rewardsId = 97366; // Dawn Lady's Pack (Time-limited) Sealed x1
			}
			else if ((rank == 2) || (rank == 3))
			{
				rewards = 1;
				rewardsId = 99849; // Dawn Prince's Pack (Time-limited) Sealed x1
			}
			else if ((rank >= 4) && (rank <= 10))
			{
				rewards = 1;
				rewardsId = 99848; // Dawn Bishop's Pack (Time-limited) Sealed x1
			}
			else if ((rank >= 11) && (rank <= 50))
			{
				rewards = 1;
				rewardsId = 99847; // Dawn Aristocrat's Pack (Time-limited) Sealed x1
			}
			else if ((rank >= 51) && (rank <= 100))
			{
				rewards = 1;
				rewardsId = 99846; // Dawn General's Pack (Time-limited) Sealed x1
			}
			else if ((rank >= 101) && (rank <= 150))
			{
				rewards = 1;
				rewardsId = 99845; // Dawn General's Pack (Time-limited) Sealed x1
			}
			
			giveRewardToPlayer(playerId, rewardsId, rewards);
			rank++;
		}
	}
	
	private void giveRewardToPlayer(int playerId, int item, int rewards)
	{
		final ItemHolder holder = new ItemHolder(item, rewards);
		final Message message = new Message(-1, playerId, false, "Weekly Leona Dungeon Reward", "Congratulations! Here are your rewards.", 0);
		message.createAttachments();
		message.getAttachments().addItem(ItemProcessType.REWARD, holder.getId(), holder.getCount(), null, null);
		MailManager.getInstance().sendMessage(message);
	}
	
	public void clear()
	{
		_playerPoints.clear();
		_rewardedPlayers.clear();
	}
	
	public void saveDungeonRankingToDatabase()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_DUNGEON_RANKING))
		{
			_playerPoints.forEach((playerId, points) ->
			{
				try
				{
					statement.setInt(1, playerId);
					statement.setInt(2, points.get());
					statement.addBatch();
				}
				catch (SQLException e)
				{
					LOGGER.severe(getClass().getSimpleName() + ": Error preparing batch statement: " + e.getMessage());
				}
			});
			
			statement.executeBatch();
			// No need to call con.commit() as HikariCP autocommit is true.
		}
		catch (SQLException e)
		{
			LOGGER.severe(getClass().getSimpleName() + ": Error during batch database operation: " + e.getMessage());
		}
	}
	
	public void restoreDungeonRankingFromDatabase()
	{
		_playerPoints.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_DUNGEON_RANKING);
			ResultSet result = statement.executeQuery())
		{
			while (result.next())
			{
				final int playerId = result.getInt("charId");
				final int points = result.getInt("points");
				_playerPoints.put(playerId, new AtomicInteger(points));
			}
		}
		catch (SQLException e)
		{
			LOGGER.severe(getClass().getSimpleName() + ": Error restoring Leona Dungeon data from database: " + e.getMessage());
		}
	}
	
	public void deletePlayerFromDungeonRanking(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_DUNGEON_RANKING))
		{
			statement.setInt(1, playerId);
			statement.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.severe(getClass().getSimpleName() + ": Error deleting Leona Dungeon player from database: " + e.getMessage());
		}
		
		_playerPoints.remove(playerId);
	}
	
	public void deleteAllDungeonRankingData()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(DELETE_ALL_DUNGEON_RANKING))
		{
			statement.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.severe(getClass().getSimpleName() + ": Error deleting all Leona dungeon ranking data from database: " + e.getMessage());
		}
		
		clear();
	}
	
	public static LeonasDungeonManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final LeonasDungeonManager INSTANCE = new LeonasDungeonManager();
	}
}
