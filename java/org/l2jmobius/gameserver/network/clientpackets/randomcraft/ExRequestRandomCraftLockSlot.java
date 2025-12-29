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
package org.l2jmobius.gameserver.network.clientpackets.randomcraft;

import org.l2jmobius.gameserver.config.RandomCraftConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerRandomCraft;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.randomcraft.ExCraftRandomInfo;
import org.l2jmobius.gameserver.network.serverpackets.randomcraft.ExCraftRandomLockSlot;

/**
 * @author Mode
 */
public class ExRequestRandomCraftLockSlot extends ClientPacket
{
	private static final int[] LOCK_PRICE =
	{
		100,
		500,
		1000
	};
	
	private int _id;
	
	@Override
	protected void readImpl()
	{
		_id = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		if (!RandomCraftConfig.ENABLE_RANDOM_CRAFT)
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((_id >= 0) && (_id < 5))
		{
			final PlayerRandomCraft rc = player.getRandomCraft();
			int lockedItemCount = rc.getLockedSlotCount();
			if (((rc.getRewards().size() - 1) >= _id) && (lockedItemCount < 3))
			{
				int price = LOCK_PRICE[Math.min(lockedItemCount, 2)];
				Item lcoin = player.getInventory().getItemByItemId(Inventory.LCOIN_ID);
				if ((lcoin != null) && (lcoin.getCount() >= price))
				{
					player.destroyItem(ItemProcessType.FEE, lcoin, price, player, true);
					rc.getRewards().get(_id).lock();
					player.sendPacket(new ExCraftRandomLockSlot());
					player.sendPacket(new ExCraftRandomInfo(player));
				}
			}
		}
	}
}
