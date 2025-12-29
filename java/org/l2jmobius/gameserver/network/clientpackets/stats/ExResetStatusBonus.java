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
package org.l2jmobius.gameserver.network.clientpackets.stats;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * @author Mobius
 */
public class ExResetStatusBonus extends ClientPacket
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
		
		final PlayerVariables vars = player.getVariables();
		int points = vars.getInt(PlayerVariables.STAT_STR.toString(), 0) + vars.getInt(PlayerVariables.STAT_DEX.toString(), 0) + vars.getInt(PlayerVariables.STAT_CON.toString(), 0) + vars.getInt(PlayerVariables.STAT_INT.toString(), 0) + vars.getInt(PlayerVariables.STAT_WIT.toString(), 0) + vars.getInt(PlayerVariables.STAT_MEN.toString(), 0);
		int adenaCost;
		int lcoinCost;
		
		if (points < 6)
		{
			lcoinCost = 200;
			adenaCost = 200_000;
		}
		else if (points < 11)
		{
			lcoinCost = 300;
			adenaCost = 500_000;
		}
		else if (points < 16)
		{
			lcoinCost = 400;
			adenaCost = 1_000_000;
		}
		else if (points < 21)
		{
			lcoinCost = 500;
			adenaCost = 2_000_000;
		}
		else if (points < 26)
		{
			lcoinCost = 600;
			adenaCost = 5_000_000;
		}
		else
		{
			lcoinCost = 700;
			adenaCost = 10_000_000;
		}
		
		long adena = player.getAdena();
		long lcoin = player.getInventory().getItemByItemId(Inventory.LCOIN_ID) == null ? 0 : player.getInventory().getItemByItemId(Inventory.LCOIN_ID).getCount();
		
		if ((adena < adenaCost) || (lcoin < lcoinCost))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_MONEY_TO_USE_THE_FUNCTION);
			return;
		}
		
		if (player.reduceAdena(ItemProcessType.FEE, adenaCost, player, true) && player.destroyItemByItemId(ItemProcessType.FEE, Inventory.LCOIN_ID, lcoinCost, player, true))
		{
			player.getVariables().remove(PlayerVariables.STAT_POINTS);
			player.getVariables().remove(PlayerVariables.STAT_STR);
			player.getVariables().remove(PlayerVariables.STAT_DEX);
			player.getVariables().remove(PlayerVariables.STAT_CON);
			player.getVariables().remove(PlayerVariables.STAT_INT);
			player.getVariables().remove(PlayerVariables.STAT_WIT);
			player.getVariables().remove(PlayerVariables.STAT_MEN);
			player.getVariables().set(PlayerVariables.ELIXIRS_AVAILABLE, player.getVariables().getInt(PlayerVariables.ELIXIRS_AVAILABLE, 0));
			
			player.getStat().recalculateStats(true);
			
			// Calculate stat increase skills.
			player.calculateStatIncreaseSkills();
			player.updateUserInfo();
		}
	}
}
