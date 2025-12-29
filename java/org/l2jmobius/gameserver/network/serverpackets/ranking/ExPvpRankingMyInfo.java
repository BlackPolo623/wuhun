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
package org.l2jmobius.gameserver.network.serverpackets.ranking;

import java.util.Map;
import java.util.Optional;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Berezkin Nikolay
 */
public class ExPvpRankingMyInfo extends ServerPacket
{
	private final Player _player;
	private final Map<Integer, StatSet> _playerList;
	private final Map<Integer, StatSet> _snapshotList;
	
	public ExPvpRankingMyInfo(Player player)
	{
		_player = player;
		_playerList = RankManager.getInstance().getPvpRankList();
		_snapshotList = RankManager.getInstance().getSnapshotPvpRankList();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PVP_RANKING_MY_INFO.writeId(this, buffer);
		if (!_playerList.isEmpty())
		{
			boolean found = false;
			for (Integer id : _playerList.keySet())
			{
				final StatSet ss = _playerList.get(id);
				if (ss.getInt("charId") == _player.getObjectId())
				{
					final Optional<Map.Entry<Integer, StatSet>> snapshotValue = _snapshotList.entrySet().stream().filter(it -> it.getValue().getInt("charId") == _player.getObjectId()).findFirst();
					found = true;
					buffer.writeLong(ss.getInt("points")); // pvp points
					buffer.writeInt(id); // current rank
					buffer.writeInt(snapshotValue.isPresent() ? snapshotValue.get().getKey() : id); // ingame shown change in rank as this value - current rank value.
					buffer.writeInt(ss.getInt("kills")); // kills
					buffer.writeInt(ss.getInt("deaths")); // deaths
				}
			}
			
			if (!found)
			{
				buffer.writeLong(0); // pvp points
				buffer.writeInt(0); // current rank
				buffer.writeInt(0); // ingame shown change in rank as this value - current rank value.
				buffer.writeInt(0); // kills
				buffer.writeInt(0); // deaths
			}
		}
		else
		{
			buffer.writeLong(0); // pvp points
			buffer.writeInt(0); // current rank
			buffer.writeInt(0); // ingame shown change in rank as this value - current rank value.
			buffer.writeInt(0); // kills
			buffer.writeInt(0); // deaths
		}
	}
}
