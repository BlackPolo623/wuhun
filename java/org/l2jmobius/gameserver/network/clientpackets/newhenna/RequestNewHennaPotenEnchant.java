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

import java.util.Map.Entry;
import java.util.Optional;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.HennaPatternPotentialData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerHennaEnchant;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.henna.DyePotentialFee;
import org.l2jmobius.gameserver.model.item.henna.HennaPoten;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.newhenna.NewHennaPotenEnchant;

/**
 * @author Index, Serenitty
 */
public class RequestNewHennaPotenEnchant extends ClientPacket
{
	private int _slotId;
	private int _costItemId;
	
	@Override
	protected void readImpl()
	{
		_slotId = readByte();
		_costItemId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		int dailyStep = player.getDyePotentialDailyStep();
		final DyePotentialFee currentFee = HennaPatternPotentialData.getInstance().getFee(dailyStep);
		int dailyCount = player.getDyePotentialDailyCount();
		if ((_slotId < 1) || (_slotId > 4))
		{
			return;
		}
		
		final HennaPoten hennaPattern = player.getHennaPoten(_slotId);
		int enchantExp = hennaPattern.getEnchantExp();
		final int fullExpNeeded = HennaPatternPotentialData.getInstance().getExpForLevel(hennaPattern.getEnchantLevel());
		if ((enchantExp >= fullExpNeeded) && (hennaPattern.getEnchantLevel() == 30))
		{
			player.sendPacket(new NewHennaPotenEnchant(_slotId, hennaPattern.getEnchantLevel(), hennaPattern.getEnchantExp(), dailyStep, dailyCount, hennaPattern.getActiveStep(), true));
			return;
		}
		
		if ((currentFee == null) || (dailyCount <= 0))
		{
			return;
		}
		
		final Optional<ItemHolder> itemFee = currentFee.getItems().stream().filter(ih -> ih.getId() == _costItemId).findAny();
		if (itemFee.isEmpty() || !player.destroyItemByItemId(ItemProcessType.FEE, itemFee.get().getId(), itemFee.get().getCount(), player, true))
		{
			return;
		}
		
		final long adenaFee = currentFee.getAdenaFee();
		if ((adenaFee > 0) && !player.destroyItemByItemId(ItemProcessType.FEE, Inventory.ADENA_ID, adenaFee, player, true))
		{
			return;
		}
		
		dailyCount -= 1;
		if ((dailyCount <= 0) && (dailyStep != HennaPatternPotentialData.getInstance().getMaxPotenEnchantStep()))
		{
			dailyStep += 1;
			final DyePotentialFee newFee = HennaPatternPotentialData.getInstance().getFee(dailyStep);
			if (newFee != null)
			{
				dailyCount = 0;
			}
			
			player.setDyePotentialDailyCount(dailyCount);
			// player.setDyePotentialDailyStep(dailyStep);
		}
		else
		{
			player.setDyePotentialDailyCount(dailyCount);
		}
		
		double totalChance = 0;
		double random = Rnd.nextDouble() * 100;
		for (Entry<Integer, Double> entry : currentFee.getEnchantExp().entrySet())
		{
			totalChance += entry.getValue();
			if (random <= totalChance)
			{
				
				final int increase = entry.getKey();
				int newEnchantExp = hennaPattern.getEnchantExp() + increase;
				final int PatternExpNeeded = HennaPatternPotentialData.getInstance().getExpForLevel(hennaPattern.getEnchantLevel());
				if ((newEnchantExp >= PatternExpNeeded) && (hennaPattern.getEnchantLevel() < 30))
				{
					newEnchantExp -= PatternExpNeeded;
					if (hennaPattern.getEnchantLevel() < HennaPatternPotentialData.getInstance().getMaxPotenLevel())
					{
						hennaPattern.setEnchantLevel(hennaPattern.getEnchantLevel() + 1);
						player.applyDyePotenSkills();
					}
				}
				
				hennaPattern.setEnchantExp(newEnchantExp);
				hennaPattern.setSlotPosition(_slotId);
				player.sendPacket(new NewHennaPotenEnchant(_slotId, hennaPattern.getEnchantLevel(), hennaPattern.getEnchantExp(), dailyStep, dailyCount, hennaPattern.getActiveStep(), true));
				
				// Notify to scripts.
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_HENNA_ENCHANT, player))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerHennaEnchant(player), player);
				}
				return;
			}
		}
	}
}
