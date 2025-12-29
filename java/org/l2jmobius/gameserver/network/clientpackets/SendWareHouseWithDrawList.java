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

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.ClanAccess;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.ClanWarehouse;
import org.l2jmobius.gameserver.model.itemcontainer.ItemContainer;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerWarehouse;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * @version $Revision: 1.2.2.1.2.4 $ $Date: 2005/03/29 23:15:16 $
 */
public class SendWareHouseWithDrawList extends ClientPacket
{
	private static final int BATCH_LENGTH = 12; // length of the one item
	
	private ItemHolder[] _items = null;
	
	@Override
	protected void readImpl()
	{
		final int count = readInt();
		if ((count <= 0) || (count > PlayerConfig.MAX_ITEM_IN_PACKET) || ((count * BATCH_LENGTH) != remaining()))
		{
			return;
		}
		
		_items = new ItemHolder[count];
		for (int i = 0; i < count; i++)
		{
			final int objId = readInt();
			final long cnt = readLong();
			if ((objId < 1) || (cnt < 0))
			{
				_items = null;
				return;
			}
			
			_items[i] = new ItemHolder(objId, cnt);
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_items == null)
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
			player.sendMessage("You are withdrawing items too fast.");
			return;
		}
		
		final ItemContainer warehouse = player.getActiveWarehouse();
		if (warehouse == null)
		{
			return;
		}
		
		if (!(warehouse instanceof PlayerWarehouse) && !player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your Access Level.");
			return;
		}
		
		// Alt game - Karma punishment
		if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_WAREHOUSE && (player.getReputation() < 0))
		{
			return;
		}
		
		if (PlayerConfig.ALT_MEMBERS_CAN_WITHDRAW_FROM_CLANWH)
		{
			if ((warehouse instanceof ClanWarehouse) && !player.hasAccess(ClanAccess.ACCESS_WAREHOUSE))
			{
				return;
			}
		}
		else if ((warehouse instanceof ClanWarehouse) && !player.isClanLeader())
		{
			player.sendPacket(SystemMessageId.TO_STORE_AN_ITEM_AT_THE_CLAN_WAREHOUSE_YOU_HAVE_TO_PAY_A_FEE_IN_L_COINS_ONLY_THE_CLAN_LEADER_CAN_WITHDRAW_ITEMS_FROM_THERE_CONTINUE);
			return;
		}
		
		int weight = 0;
		int slots = 0;
		for (ItemHolder i : _items)
		{
			// Calculate needed slots
			final Item item = warehouse.getItemByObjectId(i.getId());
			if ((item == null) || (item.getCount() < i.getCount()))
			{
				PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to withdraw non-existent item from warehouse.", GeneralConfig.DEFAULT_PUNISH);
				return;
			}
			
			weight += i.getCount() * item.getTemplate().getWeight();
			if (!item.isStackable())
			{
				slots += i.getCount();
			}
			else if (player.getInventory().getItemByItemId(item.getId()) == null)
			{
				slots++;
			}
		}
		
		// Item Max Limit Check
		if (!player.getInventory().validateCapacity(slots))
		{
			player.sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
			return;
		}
		
		// Weight limit Check
		if (!player.getInventory().validateWeight(weight))
		{
			player.sendPacket(SystemMessageId.WEIGHT_LIMIT_IS_EXCEEDED);
			return;
		}
		
		// Proceed to the transfer
		for (ItemHolder i : _items)
		{
			final Item oldItem = warehouse.getItemByObjectId(i.getId());
			if ((oldItem == null) || (oldItem.getCount() < i.getCount()))
			{
				PacketLogger.warning("Error withdrawing a warehouse object for char " + player.getName() + " (olditem == null)");
				return;
			}
			
			final Item newItem = warehouse.transferItem(ItemProcessType.TRANSFER, i.getId(), i.getCount(), player.getInventory(), player, player.getLastFolkNPC());
			if (newItem == null)
			{
				PacketLogger.warning("Error withdrawing a warehouse object for char " + player.getName() + " (newitem == null)");
				return;
			}
		}
		
		// Send updated item list to the player
		player.sendItemList();
	}
}
