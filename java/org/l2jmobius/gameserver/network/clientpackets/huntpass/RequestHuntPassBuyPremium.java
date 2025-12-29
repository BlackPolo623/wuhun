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
package org.l2jmobius.gameserver.network.clientpackets.huntpass;

import java.util.Calendar;

import org.l2jmobius.gameserver.config.HuntPassConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.huntpass.HuntPassInfo;
import org.l2jmobius.gameserver.network.serverpackets.huntpass.HuntPassSayhasSupportInfo;

/**
 * @author Serenitty
 */
public class RequestHuntPassBuyPremium extends ClientPacket
{
	private int _huntPassType;
	
	@Override
	protected void readImpl()
	{
		_huntPassType = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Calendar calendar = Calendar.getInstance();
		if ((calendar.get(Calendar.DAY_OF_MONTH) == HuntPassConfig.HUNT_PASS_PERIOD) && (calendar.get(Calendar.HOUR_OF_DAY) == 6) && (calendar.get(Calendar.MINUTE) < 30))
		{
			player.sendPacket(SystemMessageId.CURRENTLY_UNAVAILABLE_FOR_PURCHASE_YOU_CAN_BUY_THE_SEASON_PASS_ADDITIONAL_REWARDS_ONLY_UNTIL_6_30_A_M_OF_THE_SEASON_S_LAST_DAY);
			return;
		}
		
		if (!player.destroyItemByItemId(ItemProcessType.FEE, HuntPassConfig.HUNT_PASS_PREMIUM_ITEM_ID, HuntPassConfig.HUNT_PASS_PREMIUM_ITEM_COUNT, player, true))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_MONEY_TO_USE_THE_FUNCTION);
			return;
		}
		
		player.getHuntPass().setPremium(true);
		player.sendPacket(new HuntPassSayhasSupportInfo(player));
		player.sendPacket(new HuntPassInfo(player, _huntPassType));
	}
}
