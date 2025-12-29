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
package org.l2jmobius.gameserver.network.clientpackets.newskillenchant;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.newskillenchant.ExSpExtractItem;

/**
 * @author Serenitty
 */
public class RequestExSpExtractItem extends ClientPacket
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
		
		if ((player.getSp() >= 50000000L) && (player.getAdena() >= 3000000) && (player.getVariables().getInt(PlayerVariables.DAILY_EXTRACT_ITEM + Inventory.SP_POUCH, 5) > 0))
		{
			player.removeExpAndSp(0, 50000000L);
			player.broadcastUserInfo();
			player.reduceAdena(ItemProcessType.FEE, 3000000, null, true);
			player.addItem(ItemProcessType.REWARD, Inventory.SP_POUCH, 1, null, true);
			final int current = player.getVariables().getInt(PlayerVariables.DAILY_EXTRACT_ITEM + Inventory.SP_POUCH, 5);
			player.getVariables().set(PlayerVariables.DAILY_EXTRACT_ITEM + Inventory.SP_POUCH, current - 1);
			player.sendPacket(new ExSpExtractItem());
		}
	}
}
