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

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.ElementalSpirit;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.ElementalSpiritType;
import org.l2jmobius.gameserver.model.item.holders.ElementalSpiritAbsorbItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author JoeAlisson
 */
public class ElementalSpiritAbsorbInfo extends ServerPacket
{
	private final Player _player;
	private final byte _type;
	
	public ElementalSpiritAbsorbInfo(Player player, byte type)
	{
		_player = player;
		_type = type;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_ELEMENTAL_SPIRIT_ABSORB_INFO.writeId(this, buffer);
		final ElementalSpirit spirit = _player.getElementalSpirit(ElementalSpiritType.of(_type));
		if (spirit == null)
		{
			buffer.writeByte(0);
			buffer.writeByte(0);
			return;
		}
		
		buffer.writeByte(1);
		buffer.writeByte(_type);
		buffer.writeByte(spirit.getStage());
		buffer.writeLong(spirit.getExperience());
		buffer.writeLong(spirit.getExperienceToNextLevel()); // NextExp
		buffer.writeLong(spirit.getExperienceToNextLevel()); // MaxExp
		buffer.writeInt(spirit.getLevel());
		buffer.writeInt(spirit.getMaxLevel());
		
		final List<ElementalSpiritAbsorbItemHolder> absorbItems = spirit.getAbsorbItems();
		buffer.writeInt(absorbItems.size());
		for (ElementalSpiritAbsorbItemHolder absorbItem : absorbItems)
		{
			buffer.writeInt(absorbItem.getId());
			
			final Item item = _player.getInventory().getItemByItemId(absorbItem.getId());
			final int itemCount = item != null ? (int) item.getCount() : 0;
			buffer.writeInt(itemCount);
			
			buffer.writeInt(absorbItem.getExperience());
		}
	}
}
