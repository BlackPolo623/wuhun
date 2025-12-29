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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * @author Mobius
 */
public class CharSelected extends ServerPacket
{
	private final Player _player;
	private final int _sessionId;
	
	public CharSelected(Player player, int sessionId)
	{
		_player = player;
		_sessionId = sessionId;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.CHARACTER_SELECTED.writeId(this, buffer);
		buffer.writeString(_player.getName());
		buffer.writeInt(_player.getObjectId());
		buffer.writeString(_player.getTitle());
		buffer.writeInt(_sessionId);
		buffer.writeInt(_player.getClanId());
		buffer.writeInt(0); // ??
		buffer.writeInt(_player.getAppearance().isFemale());
		buffer.writeInt(_player.getRace().ordinal());
		
		if (_player.isSamurai())
		{
			buffer.writeInt(260); // Base Samurai class id.
		}
		else
		{
			buffer.writeInt(_player.getPlayerClass().getId());
		}
		
		buffer.writeInt(1); // active ??
		buffer.writeInt(_player.getX());
		buffer.writeInt(_player.getY());
		buffer.writeInt(_player.getZ());
		buffer.writeDouble(_player.getCurrentHp());
		buffer.writeDouble(_player.getCurrentMp());
		buffer.writeLong(_player.getSp());
		buffer.writeLong(_player.getExp());
		buffer.writeInt(_player.getLevel());
		buffer.writeInt(_player.getReputation());
		buffer.writeInt(_player.getPkKills());
		buffer.writeInt(GameTimeTaskManager.getInstance().getGameTime() % (24 * 60)); // "reset" on 24th hour
		buffer.writeInt(0);
		buffer.writeInt(_player.getPlayerClass().getId());
		buffer.writeBytes(new byte[16]);
		buffer.writeInt(0);
		buffer.writeInt(0);
		buffer.writeInt(0);
		buffer.writeInt(0);
		buffer.writeInt(0);
		buffer.writeInt(0);
		buffer.writeInt(0);
		buffer.writeInt(0);
		buffer.writeInt(0);
		buffer.writeBytes(new byte[28]);
		buffer.writeInt(0);
	}
}
