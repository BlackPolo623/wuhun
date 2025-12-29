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
package org.l2jmobius.gameserver.network.serverpackets.autopeel;

import java.util.Collection;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExResultItemAutoPeel extends ServerPacket
{
	private final boolean _result;
	private final long _totalPeelCount;
	private final long _remainingPeelCount;
	private final Collection<ItemHolder> _itemList;
	
	public ExResultItemAutoPeel(boolean result, long totalPeelCount, long remainingPeelCount, Collection<ItemHolder> itemList)
	{
		_result = result;
		_totalPeelCount = totalPeelCount;
		_remainingPeelCount = remainingPeelCount;
		_itemList = itemList;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RESULT_ITEM_AUTO_PEEL.writeId(this, buffer);
		buffer.writeByte(_result);
		buffer.writeLong(_totalPeelCount);
		buffer.writeLong(_remainingPeelCount);
		buffer.writeInt(_itemList.size());
		for (ItemHolder holder : _itemList)
		{
			buffer.writeInt(holder.getId());
			buffer.writeLong(holder.getCount());
			buffer.writeInt(0); // Announce level.
			buffer.writeByte(0); // Enchanted.
			buffer.writeByte(0); // Grade color.
		}
	}
}
