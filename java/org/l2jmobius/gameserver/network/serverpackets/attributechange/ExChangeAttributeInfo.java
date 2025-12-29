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
package org.l2jmobius.gameserver.network.serverpackets.attributechange;

import java.util.EnumMap;
import java.util.Map;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExChangeAttributeInfo extends ServerPacket
{
	private static final Map<AttributeType, Byte> ATTRIBUTE_MASKS = new EnumMap<>(AttributeType.class);
	static
	{
		ATTRIBUTE_MASKS.put(AttributeType.FIRE, (byte) 1);
		ATTRIBUTE_MASKS.put(AttributeType.WATER, (byte) 2);
		ATTRIBUTE_MASKS.put(AttributeType.WIND, (byte) 4);
		ATTRIBUTE_MASKS.put(AttributeType.EARTH, (byte) 8);
		ATTRIBUTE_MASKS.put(AttributeType.HOLY, (byte) 16);
		ATTRIBUTE_MASKS.put(AttributeType.DARK, (byte) 32);
	}
	
	private final int _crystalItemId;
	private int _attributes;
	private int _itemObjId;
	
	public ExChangeAttributeInfo(int crystalItemId, Item item)
	{
		_crystalItemId = crystalItemId;
		_attributes = 0;
		for (AttributeType e : AttributeType.ATTRIBUTE_TYPES)
		{
			if (e != item.getAttackAttributeType())
			{
				_attributes |= ATTRIBUTE_MASKS.get(e);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_CHANGE_ATTRIBUTE_INFO.writeId(this, buffer);
		buffer.writeInt(_crystalItemId);
		buffer.writeInt(_attributes);
		buffer.writeInt(_itemObjId);
	}
}
