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
package org.l2jmobius.gameserver.network.clientpackets.compound;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.CombinationItemsData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.CompoundRequest;
import org.l2jmobius.gameserver.model.item.combination.CombinationItem;
import org.l2jmobius.gameserver.model.item.combination.CombinationItemReward;
import org.l2jmobius.gameserver.model.item.combination.CombinationItemType;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExItemAnnounce;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.compound.ExEnchantFail;
import org.l2jmobius.gameserver.network.serverpackets.compound.ExEnchantOneFail;
import org.l2jmobius.gameserver.network.serverpackets.compound.ExEnchantSucess;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author NasSeKa
 */
public class RequestNewEnchantTry extends ClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_IN_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.sendPacket(ExEnchantOneFail.STATIC_PACKET);
			return;
		}
		
		if (player.isProcessingTransaction() || player.isProcessingRequest())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_SYSTEM_DURING_TRADING_PRIVATE_STORE_AND_WORKSHOP_SETUP);
			player.sendPacket(ExEnchantOneFail.STATIC_PACKET);
			return;
		}
		
		final CompoundRequest request = player.getRequest(CompoundRequest.class);
		if ((request == null) || request.isProcessing())
		{
			player.sendPacket(ExEnchantFail.STATIC_PACKET);
			return;
		}
		
		request.setProcessing(true);
		
		final Item itemOne = request.getItemOne();
		final Item itemTwo = request.getItemTwo();
		if ((itemOne == null) || (itemTwo == null))
		{
			player.sendPacket(ExEnchantFail.STATIC_PACKET);
			player.removeRequest(request.getClass());
			return;
		}
		
		// Lets prevent using same item twice. Also stackable item check.
		if ((itemOne.getObjectId() == itemTwo.getObjectId()) && (!itemOne.isStackable() || (player.getInventory().getInventoryItemCount(itemOne.getTemplate().getId(), -1) < 2)))
		{
			player.sendPacket(new ExEnchantFail(itemOne.getId(), itemTwo.getId()));
			player.removeRequest(request.getClass());
			return;
		}
		
		final CombinationItem combinationItem = CombinationItemsData.getInstance().getItemsBySlots(itemOne.getId(), itemOne.getEnchantLevel(), itemTwo.getId(), itemTwo.getEnchantLevel());
		
		// Not implemented or not able to merge!
		if (combinationItem == null)
		{
			player.sendPacket(new ExEnchantFail(itemOne.getId(), itemTwo.getId()));
			player.removeRequest(request.getClass());
			return;
		}
		
		if (combinationItem.getCommission() > player.getAdena())
		{
			player.sendPacket(new ExEnchantFail(itemOne.getId(), itemTwo.getId()));
			player.removeRequest(request.getClass());
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
			return;
		}
		
		// Calculate compound result.
		final double random = (Rnd.nextDouble() * 100);
		final boolean success = random <= combinationItem.getChance();
		final CombinationItemReward rewardItem = combinationItem.getReward(success ? CombinationItemType.ON_SUCCESS : CombinationItemType.ON_FAILURE);
		
		// Add item (early).
		final int itemId = rewardItem.getId();
		final Item item = itemId == 0 ? null : player.addItem(ItemProcessType.REWARD, itemId, rewardItem.getCount(), rewardItem.getEnchantLevel(), null, true);
		
		// Send success or fail.
		if (success)
		{
			player.sendPacket(new ExEnchantSucess(itemId, rewardItem.getEnchantLevel()));
			if (combinationItem.isAnnounce() && (item != null))
			{
				Broadcast.toAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.COMPOUND));
			}
		}
		else
		{
			player.sendPacket(new ExEnchantSucess(itemId, rewardItem.getEnchantLevel()));
		}
		
		// Take required items.
		if (player.destroyItem(ItemProcessType.FEE, itemOne, 1, null, true) && player.destroyItem(ItemProcessType.FEE, itemTwo, 1, null, true) && ((combinationItem.getCommission() <= 0) || player.reduceAdena(ItemProcessType.FEE, combinationItem.getCommission(), player, true)))
		{
			final InventoryUpdate iu = new InventoryUpdate();
			if (item != null)
			{
				iu.addModifiedItem(item);
			}
			
			if (itemOne.isStackable() && (itemOne.getCount() > 0))
			{
				iu.addModifiedItem(itemOne);
			}
			else
			{
				iu.addRemovedItem(itemOne);
			}
			
			if (itemTwo.isStackable() && (itemTwo.getCount() > 0))
			{
				iu.addModifiedItem(itemTwo);
			}
			else
			{
				iu.addRemovedItem(itemTwo);
			}
			
			player.sendInventoryUpdate(iu);
		}
		
		player.removeRequest(request.getClass());
	}
}
