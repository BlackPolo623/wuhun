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
package org.l2jmobius.gameserver.network.clientpackets.penaltyitemdrop;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemPenaltyHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.serverpackets.penaltyitemdrop.ExPenaltyItemInfo;
import org.l2jmobius.gameserver.network.serverpackets.penaltyitemdrop.ExPenaltyItemList;
import org.l2jmobius.gameserver.network.serverpackets.penaltyitemdrop.ExPenaltyItemRestore;

public class ExRequestPenaltyItemRestore extends ClientPacket
{
	private int _objectId;
	private boolean _isAdena;
	
	@Override
	public void readImpl()
	{
		_objectId = readInt();
		_isAdena = readBoolean();
	}
	
	@Override
	public void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		ItemPenaltyHolder itemPenalty = null;
		for (ItemPenaltyHolder itemPenaltyHolder : player.getItemPenaltyList())
		{
			if (itemPenaltyHolder.getItemObjectId() == _objectId)
			{
				itemPenalty = itemPenaltyHolder;
				break;
			}
		}
		
		if (itemPenalty == null)
		{
			return;
		}
		
		final int price = _isAdena ? FeatureConfig.ITEM_PENALTY_RESTORE_ADENA : FeatureConfig.ITEM_PENALTY_RESTORE_LCOIN;
		if (_isAdena)
		{
			if (!player.reduceAdena(ItemProcessType.RESTORE, price, null, true))
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
				return;
			}
		}
		else if (!player.destroyItemByItemId(ItemProcessType.TRANSFER, Inventory.LCOIN_ID, price, null, true))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH);
			return;
		}
		
		final Item item = player.getItemPenalty().getItemByObjectId(itemPenalty.getItemObjectId());
		player.getItemPenalty().transferItem(ItemProcessType.TRANSFER, item.getObjectId(), item.getCount(), player.getInventory(), player, null);
		player.sendItemList();
		
		if (itemPenalty.getKillerObjectId() > 0)
		{
			final int itemReward = _isAdena ? Inventory.ADENA_ID : Inventory.LCOIN_ID;
			final Item rewardItem = ItemManager.createItem(ItemProcessType.REWARD, itemReward, price, null);
			final Message msg = new Message(player.getObjectId(), itemPenalty.getKillerObjectId(), "Penalty Item Reward!", "You are reward from Item penalty!", MailType.PRIME_SHOP_GIFT);
			final Mail attachments = msg.createAttachments();
			attachments.addItem(ItemProcessType.REWARD, rewardItem, null, null);
			MailManager.getInstance().sendMessage(msg);
		}
		
		player.removePenaltyItem(itemPenalty);
		player.sendPacket(new ExPenaltyItemRestore());
		player.sendPacket(new ExPenaltyItemList(player));
		player.sendPacket(new ExPenaltyItemInfo(player));
	}
}
