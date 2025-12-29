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
package org.l2jmobius.gameserver.network.clientpackets.newhenna;

import org.l2jmobius.gameserver.data.xml.HennaPatternPotentialData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.henna.DyePotentialFee;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.newhenna.NewHennaList;
import org.l2jmobius.gameserver.network.serverpackets.newhenna.NewHennaPotenEnchantReset;

/**
 * @author Serenitty
 */
public class ExRequestNewHennaEnchantReset extends ClientPacket
{
	private int _resetItemId;
	
	@Override
	protected void readImpl()
	{
		_resetItemId = readInt(); // nCostItemId
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final int dailyReset = player.getDyePotentialDailyEnchantReset();
		final ItemHolder enchant;
		try
		{
			enchant = HennaPatternPotentialData.getInstance().getEnchantReset().stream().filter(item -> item.getId() == _resetItemId).findFirst().get();
		}
		catch (Exception e)
		{
			return;
		}
		
		if (player.destroyItemByItemId(ItemProcessType.FEE, enchant.getId(), enchant.getCount(), player, true))
		{
			final DyePotentialFee newFee = HennaPatternPotentialData.getInstance().getFee(1 /* daily step */);
			player.setDyePotentialDailyCount(newFee.getDailyCount());
			player.setDyePotentialDailyEnchantReset(dailyReset + 1);
			player.sendPacket(new NewHennaPotenEnchantReset(true));
			player.sendPacket(new NewHennaList(player, 1));
		}
		else
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_MONEY_TO_USE_THE_FUNCTION));
		}
	}
}
