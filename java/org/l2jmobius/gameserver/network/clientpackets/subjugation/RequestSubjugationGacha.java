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
package org.l2jmobius.gameserver.network.clientpackets.subjugation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SubjugationGacha;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerPurgeHolder;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.item.OnItemPurgeReward;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.subjugation.ExSubjugationGacha;
import org.l2jmobius.gameserver.network.serverpackets.subjugation.ExSubjugationGachaUI;

/**
 * @author Berezkin Nikolay
 */
public class RequestSubjugationGacha extends ClientPacket
{
	private int _category;
	private int _amount;
	
	@Override
	protected void readImpl()
	{
		_category = readInt();
		_amount = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		if ((_amount < 1) || ((_amount * 20000L) < 1))
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final PlayerPurgeHolder playerKeys = player.getPurgePoints().get(_category);
		final Map<Integer, Double> subjugationData = SubjugationGacha.getInstance().getSubjugation(_category);
		if ((playerKeys != null) && (playerKeys.getKeys() >= _amount) && (player.getInventory().getAdena() > (20000L * _amount)))
		{
			player.getInventory().reduceAdena(ItemProcessType.FEE, 20000L * _amount, player, null);
			final int curKeys = playerKeys.getKeys() - _amount;
			player.getPurgePoints().put(_category, new PlayerPurgeHolder(playerKeys.getPoints(), curKeys, 0));
			Map<Integer, Integer> rewards = new HashMap<>();
			for (int i = 0; i < _amount; i++)
			{
				double rate = 0;
				for (int index = 0; index < subjugationData.size(); index++)
				{
					final double[] chances = subjugationData.values().stream().mapToDouble(it -> it).toArray();
					final double maxBound = Arrays.stream(chances).sum();
					final double itemChance = chances[index];
					if (Rnd.get(maxBound - rate) < itemChance)
					{
						final int itemId = subjugationData.keySet().stream().mapToInt(it -> it).toArray()[index];
						rewards.put(itemId, rewards.getOrDefault(itemId, 0) + 1);
						final Item item = player.addItem(ItemProcessType.REWARD, itemId, 1, player, true);
						
						// Notify to scripts.
						if (EventDispatcher.getInstance().hasListener(EventType.ON_ITEM_PURGE_REWARD))
						{
							EventDispatcher.getInstance().notifyEventAsync(new OnItemPurgeReward(player, item));
						}
						break;
					}
					
					rate += itemChance;
				}
			}
			
			player.sendPacket(new ExSubjugationGachaUI(_category, curKeys));
			player.sendPacket(new ExSubjugationGacha(rewards));
		}
	}
}
