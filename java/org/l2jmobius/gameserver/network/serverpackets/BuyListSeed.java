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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.model.SeedProduction;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class BuyListSeed extends ServerPacket
{
	private final int _manorId;
	private final long _money;
	private final List<SeedProduction> _list = new ArrayList<>();
	
	public BuyListSeed(long currentMoney, int castleId)
	{
		_money = currentMoney;
		_manorId = castleId;
		for (SeedProduction s : CastleManorManager.getInstance().getSeedProduction(castleId, false))
		{
			if ((s.getAmount() > 0) && (s.getPrice() > 0))
			{
				_list.add(s);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.BUY_LIST_SEED.writeId(this, buffer);
		buffer.writeLong(_money); // current money
		buffer.writeInt(0); // TODO: Find me!
		buffer.writeInt(_manorId); // manor id
		if (!_list.isEmpty())
		{
			buffer.writeShort(_list.size()); // list length
			for (SeedProduction s : _list)
			{
				buffer.writeByte(0); // mask item 0 to print minimum item information
				buffer.writeInt(s.getId()); // ObjectId
				buffer.writeInt(s.getId()); // ItemId
				buffer.writeByte(0xFF); // T1
				buffer.writeLong(s.getAmount()); // Quantity
				buffer.writeByte(5); // Item Type 2 : 00-weapon, 01-shield/armor, 02-ring/earring/necklace, 03-questitem, 04-adena, 05-item
				buffer.writeByte(0); // Filler (always 0)
				buffer.writeShort(0); // Equipped : 00-No, 01-yes
				buffer.writeLong(0); // Slot : 0006-lr.ear, 0008-neck, 0030-lr.finger, 0040-head, 0100-l.hand, 0200-gloves, 0400-chest, 0800-pants, 1000-feet, 4000-r.hand, 8000-r.hand
				buffer.writeShort(0); // Enchant level (pet level shown in control item)
				buffer.writeInt(-1);
				buffer.writeInt(-9999);
				buffer.writeByte(1); // GOD Item enabled = 1 disabled (red) = 0
				buffer.writeLong(s.getPrice()); // price
			}
			
			_list.clear();
		}
		else
		{
			buffer.writeShort(0);
		}
	}
}
