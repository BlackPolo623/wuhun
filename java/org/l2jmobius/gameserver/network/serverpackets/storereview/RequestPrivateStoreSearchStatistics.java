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
package org.l2jmobius.gameserver.network.serverpackets.storereview;

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.PrivateStoreHistoryManager;
import org.l2jmobius.gameserver.model.ItemInfo;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.AbstractItemPacket;

/**
 * @author Mobius
 */
public class RequestPrivateStoreSearchStatistics extends AbstractItemPacket
{
	private final List<PrivateStoreHistoryManager.ItemHistoryTransaction> _mostItems;
	private final List<PrivateStoreHistoryManager.ItemHistoryTransaction> _highestItems;
	
	public RequestPrivateStoreSearchStatistics()
	{
		final PrivateStoreHistoryManager manager = PrivateStoreHistoryManager.getInstance();
		_mostItems = manager.getTopMostItem();
		_highestItems = manager.getTopHighestItem();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PRIVATE_STORE_SEARCH_STATISTICS.writeId(this, buffer);
		
		buffer.writeInt(Math.min(_mostItems.size(), 5));
		for (int i = 0; i < Math.min(_mostItems.size(), 5); i++)
		{
			buffer.writeInt((int) _mostItems.get(i).getCount());
			final ItemInfo itemInfo = new ItemInfo(new Item(_mostItems.get(i).getItemId()));
			buffer.writeInt(calculatePacketSize(itemInfo /* , mostItems.get(i).getCount() */));
			writeItem(itemInfo, _mostItems.get(i).getCount(), buffer);
		}
		
		buffer.writeInt(Math.min(_highestItems.size(), 5));
		for (int i = 0; i < Math.min(_highestItems.size(), 5); i++)
		{
			buffer.writeLong(_highestItems.get(i).getPrice());
			final ItemInfo itemInfo = new ItemInfo(new Item(_highestItems.get(i).getItemId()));
			buffer.writeInt(calculatePacketSize(itemInfo /* , highestItems.get(i).getCount() */));
			writeItem(itemInfo, _highestItems.get(i).getCount(), buffer);
		}
	}
}
