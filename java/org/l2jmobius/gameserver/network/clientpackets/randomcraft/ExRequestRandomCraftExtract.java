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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.config.RandomCraftConfig;
import org.l2jmobius.gameserver.data.xml.RandomCraftData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.RandomCraftRequest;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.randomcraft.ExCraftExtract;
import org.l2jmobius.gameserver.network.serverpackets.randomcraft.ExCraftInfo;

/**
 * @author Mode
 */
public class ExRequestRandomCraftExtract extends ClientPacket
{
	private final Map<Integer, Long> _items = new HashMap<>();
	
	@Override
	protected void readImpl()
	{
		final int size = readInt();
		for (int i = 0; i < size; i++)
		{
			final int objId = readInt();
			final long count = readLong();
			if (count > 0)
			{
				_items.put(objId, count);
			}
		}
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
		
		if (player.hasItemRequest() || player.hasRequest(RandomCraftRequest.class))
		{
			return;
		}
		
		player.addRequest(new RandomCraftRequest(player));
		
		int points = 0;
		int fee = 0;
		Map<Integer, Long> toDestroy = new HashMap<>();
		for (Entry<Integer, Long> e : _items.entrySet())
		{
			final int objId = e.getKey();
			final long count = e.getValue();
			if (count < 1)
			{
				player.removeRequest(RandomCraftRequest.class);
				return;
			}
			
			final Item item = player.getInventory().getItemByObjectId(objId);
			if (item != null)
			{
				if (count > item.getCount())
				{
					continue;
				}
				
				toDestroy.put(objId, count);
				points += RandomCraftData.getInstance().getPoints(item.getId()) * count;
				fee += RandomCraftData.getInstance().getFee(item.getId()) * count;
			}
			else
			{
				player.sendPacket(new ExCraftExtract());
			}
		}
		
		if ((points < 1) || (fee < 0))
		{
			player.removeRequest(RandomCraftRequest.class);
			return;
		}
		
		if (player.reduceAdena(ItemProcessType.FEE, fee, player, true))
		{
			for (Entry<Integer, Long> e : toDestroy.entrySet())
			{
				player.destroyItem(ItemProcessType.FEE, e.getKey(), e.getValue(), player, true);
			}
			
			player.getRandomCraft().addCraftPoints(points);
		}
		
		player.sendPacket(new ExCraftInfo(player));
		player.sendPacket(new ExCraftExtract());
		player.removeRequest(RandomCraftRequest.class);
	}
}
