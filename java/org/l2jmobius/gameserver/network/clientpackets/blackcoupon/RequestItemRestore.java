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
package org.l2jmobius.gameserver.network.clientpackets.blackcoupon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.l2jmobius.gameserver.managers.events.BlackCouponManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.BlackCouponRestoreCategory;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemRestoreHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.blackcoupon.ItemRestoreResult;

public class RequestItemRestore extends ClientPacket
{
	private int _brokenItemId;
	private short _enchantLevel;
	
	@Override
	public void readImpl()
	{
		_brokenItemId = readInt();
		_enchantLevel = readByte();
	}
	
	@Override
	public void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
		{
			return;
		}
		
		final BlackCouponManager manager = BlackCouponManager.getInstance();
		if (player.getInventory().getInventoryItemCount(manager.getBlackCouponId(), -1) < 1L)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.sendPacket(ItemRestoreResult.FAIL);
			return;
		}
		
		final BlackCouponRestoreCategory category = manager.getCategoryByItemId(_brokenItemId);
		final List<ItemRestoreHolder> holders = manager.getRestoreItems(player.getObjectId(), category);
		final List<ItemRestoreHolder> filter = new ArrayList<>();
		holders.stream().filter(destroyedItemHolder -> destroyedItemHolder.getDestroyedItemId() == _brokenItemId).forEach(filter::add);
		if (filter.isEmpty())
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.sendPacket(ItemRestoreResult.FAIL);
			return;
		}
		
		final ItemRestoreHolder restoreHolder = filter.stream().filter(holder -> holder.getEnchantLevel() == _enchantLevel).min(Comparator.comparing(ItemRestoreHolder::getDestroyDate)).orElse(null);
		if (restoreHolder == null)
		{
			return;
		}
		
		player.destroyItemByItemId(ItemProcessType.FEE, manager.getBlackCouponId(), manager.getBlackCouponCount(), player, true);
		final Item item = player.addItem(ItemProcessType.REWARD, restoreHolder.getRepairItemId(), manager.getBlackCouponCount(), restoreHolder.getEnchantLevel(), player, true);
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(item);
		player.sendInventoryUpdate(iu);
		manager.addToDelete(category, restoreHolder);
		player.sendPacket(ItemRestoreResult.SUCCESS);
	}
}
