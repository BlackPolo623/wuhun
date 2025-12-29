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
import org.l2jmobius.gameserver.network.enums.PartySmallWindowUpdateType;

/**
 * @author Mobius
 */
public class PartySmallWindowUpdate extends AbstractMaskPacket<PartySmallWindowUpdateType>
{
	private final Player _member;
	private int _flags = 0;
	
	public PartySmallWindowUpdate(Player member, boolean addAllFlags)
	{
		_member = member;
		if (addAllFlags)
		{
			for (PartySmallWindowUpdateType type : PartySmallWindowUpdateType.values())
			{
				addComponentType(type);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.PARTY_SMALL_WINDOW_UPDATE.writeId(this, buffer);
		buffer.writeInt(_member.getObjectId());
		buffer.writeShort(_flags);
		if (containsMask(PartySmallWindowUpdateType.CURRENT_CP))
		{
			buffer.writeInt((int) _member.getCurrentCp()); // c4
		}
		
		if (containsMask(PartySmallWindowUpdateType.MAX_CP))
		{
			buffer.writeInt(_member.getMaxCp()); // c4
		}
		
		if (containsMask(PartySmallWindowUpdateType.CURRENT_HP))
		{
			buffer.writeInt((int) _member.getCurrentHp());
		}
		
		if (containsMask(PartySmallWindowUpdateType.MAX_HP))
		{
			buffer.writeInt((int) _member.getMaxHp());
		}
		
		if (containsMask(PartySmallWindowUpdateType.CURRENT_MP))
		{
			buffer.writeInt((int) _member.getCurrentMp());
		}
		
		if (containsMask(PartySmallWindowUpdateType.MAX_MP))
		{
			buffer.writeInt(_member.getMaxMp());
		}
		
		if (containsMask(PartySmallWindowUpdateType.LEVEL))
		{
			buffer.writeByte(_member.getLevel());
		}
		
		if (containsMask(PartySmallWindowUpdateType.CLASS_ID))
		{
			buffer.writeShort(_member.getPlayerClass().getId());
		}
		
		if (containsMask(PartySmallWindowUpdateType.PARTY_SUBSTITUTE))
		{
			buffer.writeByte(0);
		}
		
		if (containsMask(PartySmallWindowUpdateType.VITALITY_POINTS))
		{
			buffer.writeInt(_member.getVitalityPoints());
		}
	}
	
	@Override
	protected void addMask(int mask)
	{
		_flags |= mask;
	}
	
	@Override
	public boolean containsMask(PartySmallWindowUpdateType component)
	{
		return containsMask(_flags, component);
	}
	
	@Override
	protected byte[] getMasks()
	{
		return new byte[0];
	}
}
