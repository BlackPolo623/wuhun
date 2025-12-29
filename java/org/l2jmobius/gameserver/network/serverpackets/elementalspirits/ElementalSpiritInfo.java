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
package org.l2jmobius.gameserver.network.serverpackets.elementalspirits;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.ElementalSpirit;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Magik
 */
public class ElementalSpiritInfo extends AbstractElementalSpiritPacket
{
	private final Player _player;
	private final byte _type;
	
	public ElementalSpiritInfo(Player player, byte packetType)
	{
		_player = player;
		_type = packetType;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_ELEMENTAL_SPIRIT_INFO.writeId(this, buffer);
		final ElementalSpirit[] spirits = _player.getSpirits();
		if (spirits == null)
		{
			buffer.writeByte(0);
			buffer.writeByte(0);
			buffer.writeByte(0);
			return;
		}
		
		buffer.writeByte(_type); // show spirit info window 1; Change type 2; Only update 0
		buffer.writeByte(spirits.length); // spirit count
		
		for (ElementalSpirit spirit : spirits)
		{
			buffer.writeByte(spirit.getType());
			buffer.writeByte(1);
			writeSpiritInfo(buffer, spirit);
		}
		
		buffer.writeInt(1); // Reset talent items count
		for (int i = 0; i < 1; i++)
		{
			buffer.writeInt(57);
			buffer.writeLong(50000);
		}
	}
}
