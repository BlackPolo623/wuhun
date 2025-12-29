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
package org.l2jmobius.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.RankingHistoryDataHolder;

/**
 * @author Serenitty
 */
public class RankingHistory
{
	private static final Logger LOGGER = Logger.getLogger(RankingHistory.class.getName());
	
	private static final int NUM_HISTORY_DAYS = 7;
	
	private final Player _player;
	private final Collection<RankingHistoryDataHolder> _data = new ArrayList<>();
	private long _nextUpdate = 0;
	
	public RankingHistory(Player player)
	{
		_player = player;
	}
	
	public void store()
	{
		final int ranking = RankManager.getInstance().getPlayerGlobalRank(_player);
		final long exp = _player.getExp();
		final int today = (int) (System.currentTimeMillis() / 86400000L);
		final int oldestDay = (today - NUM_HISTORY_DAYS) + 1;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_ranking_history (charId, day, ranking, exp) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE ranking = ?, exp = ?");
			PreparedStatement deleteSt = con.prepareStatement("DELETE FROM character_ranking_history WHERE charId = ? AND day < ?"))
		{
			statement.setInt(1, _player.getObjectId());
			statement.setInt(2, today);
			statement.setInt(3, ranking);
			statement.setLong(4, exp);
			statement.setInt(5, ranking); // update
			statement.setLong(6, exp); // update
			statement.execute();
			
			// Delete old records
			deleteSt.setInt(1, _player.getObjectId());
			deleteSt.setInt(2, oldestDay);
			deleteSt.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not insert RankingCharHistory data: " + e.getMessage(), e);
		}
	}
	
	public Collection<RankingHistoryDataHolder> getData()
	{
		final long currentTime = System.currentTimeMillis();
		if (currentTime > _nextUpdate)
		{
			_data.clear();
			if (_nextUpdate == 0)
			{
				store(); // to update
			}
			
			_nextUpdate = currentTime + GeneralConfig.CHAR_DATA_STORE_INTERVAL;
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT * FROM character_ranking_history WHERE charId = ? ORDER BY day DESC"))
			{
				statement.setInt(1, _player.getObjectId());
				try (ResultSet rset = statement.executeQuery())
				{
					while (rset.next())
					{
						final int day = rset.getInt("day");
						final long timestamp = (day * 86400000L) + 86400000L;
						final int ranking = rset.getInt("ranking");
						final long exp = rset.getLong("exp");
						_data.add(new RankingHistoryDataHolder(timestamp / 1000, ranking, exp));
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not get RankingCharHistory data: " + e.getMessage(), e);
			}
		}
		
		return _data;
	}
}
