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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author NviX
 */
public class ExRankingCharInfo extends ServerPacket
{
	private final Player _player;
	private final Map<Integer, StatSet> _playerList;
	private final Map<Integer, StatSet> _snapshotList;
	
	public ExRankingCharInfo(Player player)
	{
		_player = player;
		_playerList = RankManager.getInstance().getRankList();
		_snapshotList = RankManager.getInstance().getSnapshotList();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RANKING_CHAR_INFO.writeId(this, buffer);
		if (!_playerList.isEmpty())
		{
			for (Integer id : _playerList.keySet())
			{
				final StatSet player = _playerList.get(id);
				if (player.getInt("charId") == _player.getObjectId())
				{
					buffer.writeInt(id); // server rank
					buffer.writeInt(player.getInt("raceRank")); // race rank
					buffer.writeInt(player.getInt("classRank")); // class rank
					for (Integer id2 : _snapshotList.keySet())
					{
						final StatSet snapshot = _snapshotList.get(id2);
						if (player.getInt("charId") == snapshot.getInt("charId"))
						{
							buffer.writeInt(id2); // server rank snapshot
							buffer.writeInt(snapshot.getInt("classRank")); // class rank snapshot
							buffer.writeInt(player.getInt("classRank")); // class rank snapshot
							buffer.writeInt(0);
							buffer.writeInt(0);
							buffer.writeInt(0);
							return;
						}
					}
				}
			}
			
			buffer.writeInt(0); // server rank
			buffer.writeInt(0); // race rank
			buffer.writeInt(0); // server rank snapshot
			buffer.writeInt(0); // race rank snapshot
			buffer.writeInt(0); // nClassRank
			buffer.writeInt(0); // nClassRank_Snapshot snapshot
		}
		else
		{
			buffer.writeInt(0); // server rank
			buffer.writeInt(0); // race rank
			buffer.writeInt(0); // server rank snapshot
			buffer.writeInt(0); // race rank snapshot
			buffer.writeInt(0); // nClassRank
			buffer.writeInt(0); // nClassRank_Snapshot snapshot
		}
	}
}
