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
package org.l2jmobius.gameserver.network.serverpackets.blackcoupon;

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.events.BlackCouponManager;
import org.l2jmobius.gameserver.model.item.enums.BlackCouponRestoreCategory;
import org.l2jmobius.gameserver.model.item.holders.ItemRestoreHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class ItemRestoreList extends ServerPacket
{
	private final BlackCouponRestoreCategory _category;
	private final List<ItemRestoreHolder> _restoreItems;
	
	public ItemRestoreList()
	{
		_category = BlackCouponRestoreCategory.WEAPON;
		_restoreItems = BlackCouponManager.getInstance().getRestoreItems(0, _category);
	}
	
	public ItemRestoreList(int playerObjectId)
	{
		_category = BlackCouponRestoreCategory.WEAPON;
		_restoreItems = BlackCouponManager.getInstance().getRestoreItems(playerObjectId, _category);
	}
	
	public ItemRestoreList(int playerObjectId, BlackCouponRestoreCategory category)
	{
		_category = category;
		_restoreItems = BlackCouponManager.getInstance().getRestoreItems(playerObjectId, _category);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_ITEM_RESTORE_LIST.writeId(this, buffer);
		buffer.writeByte(_category.ordinal());
		buffer.writeInt(_restoreItems.size());
		for (ItemRestoreHolder holder : _restoreItems)
		{
			buffer.writeInt(holder.getDestroyedItemId());
			buffer.writeInt(holder.getRepairItemId());
			buffer.writeByte(holder.getEnchantLevel());
			buffer.writeByte(_restoreItems.lastIndexOf(holder));
		}
	}
}
