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
package org.l2jmobius.gameserver.network.clientpackets.ranking;

import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.RankingPowerManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ranking.ExRankingBuffZoneNpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.ranking.ExRankingBuffZoneNpcPosition;

/**
 * @author Serenitty
 */
public class RequestExRankingCharSpawnBuffzoneNpc extends ClientPacket
{
	private static final int COST = 20000000;
	
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
		
		if (GlobalVariablesManager.getInstance().getLong(GlobalVariablesManager.RANKING_POWER_COOLDOWN, 0) > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.LEADER_POWER_COOLDOWN);
			return;
		}
		
		if (!player.isInsideZone(ZoneId.PEACE) || player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_LEADER_S_POWER_HERE);
			return;
		}
		
		if (player.getAdena() < COST)
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_MONEY_TO_USE_THE_FUNCTION);
			return;
		}
		
		player.destroyItemByItemId(ItemProcessType.FEE, 57, COST, player, true);
		RankingPowerManager.getInstance().activatePower(player);
		player.sendPacket(new ExRankingBuffZoneNpcPosition());
		player.sendPacket(new ExRankingBuffZoneNpcInfo());
	}
}
