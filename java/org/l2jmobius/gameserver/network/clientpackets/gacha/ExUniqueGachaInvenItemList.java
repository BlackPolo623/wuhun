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
package org.l2jmobius.gameserver.network.clientpackets.gacha;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.managers.events.UniqueGachaManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.gacha.UniqueGachaInvenItemList;

public class ExUniqueGachaInvenItemList extends ClientPacket
{
	@Override
	protected void readImpl()
	{
		readByte(); // _inventoryType
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final List<Item> items = new ArrayList<>(UniqueGachaManager.getInstance().getTemporaryWarehouse(player));
		final int totalSize = items.size();
		final int perPage = 150;
		final int totalPages = totalSize / perPage;
		
		for (int i = 0; i <= totalPages; i++)
		{
			// Page on client should start from 1, not 0.
			// If page is set to 0 - nothing shows up at all.
			player.sendPacket(new UniqueGachaInvenItemList((i + 1), (totalPages + 1), items.subList(i * perPage, Math.min((i + 1) * perPage, totalSize))));
		}
	}
}
