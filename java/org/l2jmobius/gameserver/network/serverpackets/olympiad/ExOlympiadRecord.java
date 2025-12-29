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
package org.l2jmobius.gameserver.network.serverpackets.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class ExOlympiadRecord extends ServerPacket
{
	private static final String GET_PREVIOUS_CYCLE_DATA = "SELECT charId, class_id, olympiad_points, competitions_won, competitions_lost, " + "(SELECT COUNT(*) FROM olympiad_nobles_eom WHERE olympiad_points > t.olympiad_points) AS previousPlaceTotal " + "FROM olympiad_nobles_eom t WHERE class_id = ? ORDER BY olympiad_points DESC LIMIT " + RankManager.PLAYER_LIMIT;
	
	private final int _gameRuleType;
	private final int _type;
	private final int _noblePoints;
	private final int _competitionWon;
	private final int _competitionLost;
	private final int _remainingWeeklyMatches;
	private final int _previousPlace;
	private final int _previousWins;
	private final int _previousLoses;
	private final int _previousPoints;
	private final int _previousClass;
	private final int _previousPlaceTotal;
	private final boolean _inCompPeriod;
	private final int _currentCycle;
	
	public ExOlympiadRecord(Player player, int gameRuleType)
	{
		_gameRuleType = gameRuleType;
		_type = OlympiadManager.getInstance().isRegistered(player) ? 1 : 0;
		
		final Olympiad olympiad = Olympiad.getInstance();
		_noblePoints = olympiad.getNoblePoints(player);
		_competitionWon = olympiad.getCompetitionWon(player.getObjectId());
		_competitionLost = olympiad.getCompetitionLost(player.getObjectId());
		_remainingWeeklyMatches = olympiad.getRemainingWeeklyMatches(player.getObjectId());
		_inCompPeriod = olympiad.inCompPeriod();
		_currentCycle = olympiad.getCurrentCycle();
		
		// Initialize previous cycle data.
		int previousPlace = 0;
		int previousWins = 0;
		int previousLoses = 0;
		int previousPoints = 0;
		int previousClass = 0;
		int previousPlaceTotal = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(GET_PREVIOUS_CYCLE_DATA))
		{
			statement.setInt(1, player.getBaseClass());
			
			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					previousPlace = rset.getRow();
					previousWins = rset.getInt("competitions_won");
					previousLoses = rset.getInt("competitions_lost");
					previousPoints = rset.getInt("olympiad_points");
					previousClass = rset.getInt("class_id");
					previousPlaceTotal = rset.getInt("previousPlaceTotal") + 1;
				}
			}
		}
		catch (Exception e)
		{
			PacketLogger.warning("ExOlympiadRecord: Could not load data: " + e.getMessage());
		}
		
		_previousPlace = previousPlace;
		_previousWins = previousWins;
		_previousLoses = previousLoses;
		_previousPoints = previousPoints;
		_previousClass = previousClass;
		_previousPlaceTotal = previousPlaceTotal;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_OLYMPIAD_RECORD.writeId(this, buffer);
		buffer.writeInt(_noblePoints); // nPoint
		buffer.writeInt(_competitionWon); // nWinCount
		buffer.writeInt(_competitionLost); // nLoseCount
		buffer.writeInt(_remainingWeeklyMatches); // nMatchCount
		
		// Previous Cycle.
		buffer.writeInt(_previousClass); // nPrevClassType
		buffer.writeInt(_previousPlaceTotal); // nPrevRank in all servers
		buffer.writeInt(2); // nPrevRankCount number of participants with 25+ matches
		buffer.writeInt(_previousPlace); // nPrevClassRank in all servers
		buffer.writeInt(4); // nPrevClassRankCount number of participants with 25+ matches
		buffer.writeInt(5); // nPrevClassRankByServer in current server
		buffer.writeInt(6); // nPrevClassRankByServerCount number of participants with 25+ matches
		buffer.writeInt(_previousPoints); // nPrevPoint
		buffer.writeInt(_previousWins); // nPrevWinCount
		buffer.writeInt(_previousLoses); // nPrevLoseCount
		buffer.writeInt(_previousPlace); // nPrevGrade
		buffer.writeInt(Calendar.getInstance().get(Calendar.YEAR)); // nSeasonYear
		buffer.writeInt(Calendar.getInstance().get(Calendar.MONTH) + 1); // nSeasonMonth
		buffer.writeByte(_inCompPeriod); // bMatchOpen
		buffer.writeInt(_currentCycle); // nSeason
		buffer.writeByte(_type); // bRegistered
		buffer.writeInt(_gameRuleType); // cGameRuleType
	}
}
