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
package org.l2jmobius.gameserver.network.clientpackets;

import static org.l2jmobius.gameserver.model.itemcontainer.Inventory.ADENA_ID;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExChangePostState;
import org.l2jmobius.gameserver.network.serverpackets.ExShowReceivedPostList;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Migi, DS, Mobius
 */
public class RequestPostAttachment extends ClientPacket
{
	private int _msgId;
	
	@Override
	protected void readImpl()
	{
		_msgId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		if (!GeneralConfig.ALLOW_MAIL || !GeneralConfig.ALLOW_ATTACHMENTS)
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().canPerformTransaction())
		{
			return;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your Access Level");
			return;
		}
		
		if (player.isInCombat())
		{
			player.sendPacket(SystemMessageId.NOT_AVAILABLE_IN_COMBAT);
			return;
		}
		
		if (player.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_DEAD_AND_CANNOT_PERFORM_THIS_ACTION);
			return;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_DURING_AN_EXCHANGE);
			return;
		}
		
		if (player.hasItemRequest())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_ANYTHING_WHILE_ENCHANTING_IMBUING_WITH_ATTRIBUTES_OR_COMPOUNDING_WITH_JEWELS);
			return;
		}
		
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_BECAUSE_THE_PRIVATE_STORE_OR_WORKSHOP_IS_IN_PROGRESS);
			return;
		}
		
		final Message msg = MailManager.getInstance().getMessage(_msgId);
		if (msg == null)
		{
			return;
		}
		
		if (msg.getReceiverId() != player.getObjectId())
		{
			PunishmentManager.handleIllegalPlayerAction(player, player + " tried to get not own attachment!", GeneralConfig.DEFAULT_PUNISH);
			return;
		}
		
		if (!msg.hasAttachments())
		{
			return;
		}
		
		final ItemContainer attachments = msg.getAttachments();
		if (attachments == null)
		{
			return;
		}
		
		int weight = 0;
		int slots = 0;
		for (Item item : attachments.getItems())
		{
			if (item == null)
			{
				continue;
			}
			
			// Calculate needed slots
			if (item.getOwnerId() != msg.getSenderId())
			{
				PunishmentManager.handleIllegalPlayerAction(player, player + " tried to get wrong item (ownerId != senderId) from attachment!", GeneralConfig.DEFAULT_PUNISH);
				return;
			}
			
			if (item.getItemLocation() != ItemLocation.MAIL)
			{
				PunishmentManager.handleIllegalPlayerAction(player, player + " tried to get wrong item (Location != MAIL) from attachment!", GeneralConfig.DEFAULT_PUNISH);
				return;
			}
			
			if (item.getLocationSlot() != msg.getId())
			{
				PunishmentManager.handleIllegalPlayerAction(player, player + " tried to get items from different attachment!", GeneralConfig.DEFAULT_PUNISH);
				return;
			}
			
			weight += item.getCount() * item.getTemplate().getWeight();
			if (!item.isStackable())
			{
				slots += item.getCount();
			}
			else if (player.getInventory().getItemByItemId(item.getId()) == null)
			{
				slots++;
			}
		}
		
		// Item Max Limit Check
		if (!player.getInventory().validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.YOU_COULD_NOT_RECEIVE_BECAUSE_YOUR_INVENTORY_IS_FULL);
			return;
		}
		
		// Weight limit Check
		if (!player.getInventory().validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.YOU_COULD_NOT_RECEIVE_BECAUSE_YOUR_INVENTORY_IS_FULL);
			return;
		}
		
		final long adena = msg.getReqAdena();
		if ((adena > 0) && !player.reduceAdena(ItemProcessType.FEE, adena, null, true))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RECEIVE_BECAUSE_YOU_DON_T_HAVE_ENOUGH_ADENA);
			return;
		}
		
		// Proceed to the transfer
		final InventoryUpdate playerIU = new InventoryUpdate();
		for (Item item : attachments.getItems())
		{
			if (item == null)
			{
				continue;
			}
			
			if (item.getOwnerId() != msg.getSenderId())
			{
				PunishmentManager.handleIllegalPlayerAction(player, player + " tried to get items with owner != sender !", GeneralConfig.DEFAULT_PUNISH);
				return;
			}
			
			final long count = item.getCount();
			final Item newItem = attachments.transferItem(ItemProcessType.TRANSFER, item.getObjectId(), item.getCount(), player.getInventory(), player, null);
			if (newItem == null)
			{
				return;
			}
			
			if (newItem.isStackable() && (newItem.getCount() > count))
			{
				playerIU.addModifiedItem(newItem);
			}
			else
			{
				playerIU.addNewItem(newItem);
			}
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
			sm.addItemName(item.getId());
			sm.addLong(count);
			player.sendPacket(sm);
		}
		
		// Send updated item list to the player
		player.sendInventoryUpdate(playerIU);
		
		// Send full list to avoid duplicates.
		player.sendItemList();
		
		msg.removeAttachments();
		
		SystemMessage sm;
		final Player sender = World.getInstance().getPlayer(msg.getSenderId());
		if (adena > 0)
		{
			if (sender != null)
			{
				sender.addAdena(ItemProcessType.TRANSFER, adena, player, false);
				sm = new SystemMessage(SystemMessageId.S2_COMPLETED_THE_PAYMENT_AND_YOU_RECEIVE_S1_ADENA);
				sm.addLong(adena);
				sm.addString(player.getName());
				sender.sendPacket(sm);
			}
			else
			{
				final Item paidAdena = ItemManager.createItem(ItemProcessType.FEE, ADENA_ID, adena, player, null);
				paidAdena.setOwnerId(msg.getSenderId());
				paidAdena.setItemLocation(ItemLocation.INVENTORY);
				paidAdena.updateDatabase(true);
				World.getInstance().removeObject(paidAdena);
			}
		}
		else if (sender != null)
		{
			sm = new SystemMessage(SystemMessageId.S1_HAS_OBTAINED_THE_ITEM_ATTACHED_TO_THE_MAIL);
			sm.addString(player.getName());
			sender.sendPacket(sm);
		}
		
		player.sendPacket(new ExChangePostState(true, _msgId, Message.READED));
		player.sendPacket(SystemMessageId.THE_ITEM_IS_RECEIVED);
		player.sendPacket(new ExShowReceivedPostList(player.getObjectId()));
	}
}
