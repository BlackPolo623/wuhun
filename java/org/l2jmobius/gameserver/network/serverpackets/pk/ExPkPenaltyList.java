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
package org.l2jmobius.gameserver.network.serverpackets.pk;

import java.util.LinkedList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExPkPenaltyList extends ServerPacket
{
	private final int _lastPkTime;
	private final List<PlayerHolder> _players = new LinkedList<>();
	
	public ExPkPenaltyList()
	{
		_lastPkTime = World.getInstance().getLastPkTime();
		for (Player player : World.getInstance().getPkPlayers())
		{
			_players.add(new PlayerHolder(player.getObjectId(), String.format("%1$-" + 23 + "s", player.getName()), player.getLevel(), player.getPlayerClass().getId()));
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PK_PENALTY_LIST.writeId(this, buffer);
		buffer.writeInt(_lastPkTime);
		buffer.writeInt(_players.size());
		for (PlayerHolder holder : _players)
		{
			buffer.writeInt(holder.getObjectId());
			buffer.writeString(holder.getName());
			buffer.writeInt(holder.getLevel());
			buffer.writeInt(holder.getClassId());
		}
	}
	
	private class PlayerHolder
	{
		private final int _objectId;
		private final String _name;
		private final int _level;
		private final int _classId;
		
		public PlayerHolder(int objectId, String name, int level, int classId)
		{
			_objectId = objectId;
			_name = name;
			_level = level;
			_classId = classId;
		}
		
		public int getObjectId()
		{
			return _objectId;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public int getLevel()
		{
			return _level;
		}
		
		public int getClassId()
		{
			return _classId;
		}
	}
}
