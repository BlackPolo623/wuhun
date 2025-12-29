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
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.clientpackets.storereview.ExRequestPrivateStoreSearchList;
import org.l2jmobius.gameserver.network.clientpackets.storereview.ExRequestPrivateStoreSearchList.ShopItem;
import org.l2jmobius.gameserver.network.serverpackets.AbstractItemPacket;

/**
 * @author Mobius
 */
public class ExPrivateStoreSearchItem extends AbstractItemPacket
{
	private final int _page;
	private final int _maxPage;
	private final int _nSize;
	private final List<ShopItem> _items;
	
	public ExPrivateStoreSearchItem(int page, int maxPage, int nSize, List<ShopItem> items)
	{
		_page = page;
		_maxPage = maxPage;
		_nSize = nSize;
		_items = items;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PRIVATE_STORE_SEARCH_ITEM.writeId(this, buffer);
		buffer.writeByte(_page); // cPage
		buffer.writeByte(_maxPage); // cMaxPage
		buffer.writeInt(_nSize); // nSize
		if (_nSize > 0)
		{
			for (int itemIndex = (_page - 1) * ExRequestPrivateStoreSearchList.MAX_ITEM_PER_PAGE; (itemIndex < (_page * ExRequestPrivateStoreSearchList.MAX_ITEM_PER_PAGE)) && (itemIndex < _items.size()); itemIndex++)
			{
				final ShopItem shopItem = _items.get(itemIndex);
				buffer.writeSizedString(shopItem.getOwner().getName()); // Vendor name
				buffer.writeInt(shopItem.getOwner().getObjectId());
				buffer.writeByte(shopItem.getStoreType() == PrivateStoreType.PACKAGE_SELL ? 2 : shopItem.getStoreType() == PrivateStoreType.SELL ? 0 : 1); // store type (maybe "sold"/buy/Package (translated as Total Score...))
				buffer.writeLong(shopItem.getPrice()); // Price
				buffer.writeInt(shopItem.getOwner().getX()); // X
				buffer.writeInt(shopItem.getOwner().getY()); // Y
				buffer.writeInt(shopItem.getOwner().getZ()); // Z
				buffer.writeInt(calculatePacketSize(shopItem.getItemInfo() /* , shopItem.getCount() */)); // size
				writeItem(shopItem.getItemInfo(), shopItem.getCount(), buffer); // itemAssemble
			}
		}
	}
}
