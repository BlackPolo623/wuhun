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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.ElementalAttributeData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class ExChooseInventoryAttributeItem extends ServerPacket
{
	private final int _itemId;
	private final long _count;
	private final AttributeType _atribute;
	private final int _level;
	private final Set<Integer> _items = new HashSet<>();
	
	public ExChooseInventoryAttributeItem(Player player, Item stone)
	{
		_itemId = stone.getDisplayId();
		_count = stone.getCount();
		_atribute = ElementalAttributeData.getInstance().getItemElement(_itemId);
		if (_atribute == AttributeType.NONE)
		{
			throw new IllegalArgumentException("Undefined Atribute item: " + stone);
		}
		
		_level = ElementalAttributeData.getInstance().getMaxElementLevel(_itemId);
		
		// Register only items that can be put an attribute stone/crystal
		for (Item item : player.getInventory().getItems())
		{
			if (item.isElementable())
			{
				_items.add(item.getObjectId());
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_CHOOSE_INVENTORY_ATTRIBUTE_ITEM.writeId(this, buffer);
		buffer.writeInt(_itemId);
		buffer.writeLong(_count);
		buffer.writeInt(_atribute == AttributeType.FIRE); // Fire
		buffer.writeInt(_atribute == AttributeType.WATER); // Water
		buffer.writeInt(_atribute == AttributeType.WIND); // Wind
		buffer.writeInt(_atribute == AttributeType.EARTH); // Earth
		buffer.writeInt(_atribute == AttributeType.HOLY); // Holy
		buffer.writeInt(_atribute == AttributeType.DARK); // Unholy
		buffer.writeInt(_level); // Item max attribute level
		buffer.writeInt(_items.size());
		_items.forEach(buffer::writeInt);
	}
}
