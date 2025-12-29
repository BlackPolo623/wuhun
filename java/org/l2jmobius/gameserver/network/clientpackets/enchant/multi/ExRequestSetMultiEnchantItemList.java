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
package org.l2jmobius.gameserver.network.clientpackets.enchant.multi;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.EnchantItemRequest;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.enchant.multi.ExResultSetMultiEnchantItemList;
import org.l2jmobius.gameserver.network.serverpackets.enchant.single.ChangedEnchantTargetItemProbabilityList;

/**
 * @author Index
 */
public class ExRequestSetMultiEnchantItemList extends ClientPacket
{
	private int _slotId;
	private final Map<Integer, Integer> _itemObjectId = new HashMap<>();
	
	@Override
	protected void readImpl()
	{
		_slotId = readInt();
		for (int i = 1; remaining() != 0; i++)
		{
			_itemObjectId.put(i, readInt());
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final EnchantItemRequest request = player.getRequest(EnchantItemRequest.class);
		if (request == null)
		{
			player.sendPacket(new ExResultSetMultiEnchantItemList(player, 1));
			return;
		}
		
		if (request.getMultiEnchantingItemsBySlot(_slotId) != -1)
		{
			request.clearMultiEnchantingItemsBySlot();
			for (int i = 1; i <= _slotId; i++)
			{
				request.addMultiEnchantingItems(i, _itemObjectId.get(i));
			}
		}
		else
		{
			request.addMultiEnchantingItems(_slotId, _itemObjectId.get(_slotId));
		}
		
		_itemObjectId.clear();
		player.sendPacket(new ExResultSetMultiEnchantItemList(player, 0));
		player.sendPacket(new ChangedEnchantTargetItemProbabilityList(player, true));
	}
}
