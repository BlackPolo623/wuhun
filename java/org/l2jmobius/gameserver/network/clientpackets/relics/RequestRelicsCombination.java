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
package org.l2jmobius.gameserver.network.clientpackets.relics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.data.holders.RelicCompoundFeeHolder;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerRelicData;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsCollectionUpdate;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsCombination;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsList;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsUpdateList;

/**
 * @author CostyKiller, Brado
 */
public class RequestRelicsCombination extends ClientPacket
{
	private int _relicsUsedGrade;
	private int _relicsUsedCount;
	private final List<Integer> _ingredientIds = new LinkedList<>();
	
	@Override
	protected void readImpl()
	{
		_relicsUsedGrade = readInt();
		_relicsUsedCount = readInt();
		while (remaining() > 0)
		{
			_ingredientIds.add(readInt());
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
		
		final Collection<PlayerRelicData> storedRelics = player.getRelics();
		final List<Integer> unconfirmedRelics = new ArrayList<>();
		final int compoundAttempts = _relicsUsedCount / 4;
		for (PlayerRelicData relic : storedRelics)
		{
			if ((relic.getRelicIndex() >= 300) && (relic.getRelicCount() == 1)) // Unconfirmed relics are set on summon to index 300.
			{
				unconfirmedRelics.add(relic.getRelicId());
			}
		}
		
		if (unconfirmedRelics.size() == RelicSystemConfig.RELIC_UNCONFIRMED_LIST_LIMIT) // If you have 100 relics in your confirmation list, restrictions are applied to the relic summoning and compounding functions.
		{
			player.sendPacket(SystemMessageId.SUMMON_COMPOUND_IS_UNAVAILABLE_AS_YOU_HAVE_MORE_THAN_100_UNCONFIRMED_DOLLS);
			return;
		}
		
		final RelicCompoundFeeHolder feeHolder = RelicData.getInstance().getCompoundFeeHolderByGrade(RelicGrade.values()[_relicsUsedGrade]);
		if (!player.destroyItemByItemId(ItemProcessType.FEE, feeHolder.getId(), feeHolder.getCount() * compoundAttempts, player, true))
		{
			player.sendPacket(SystemMessageId.AUTO_COMPOUNDING_IS_CANCELLED_NOT_ENOUGH_MONEY);
			return;
		}
		
		if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
		{
			player.sendMessage("Compound Ingredients: " + _relicsUsedCount);
		}
		
		int ingredientIndex = 0;
		for (int ingredientId : _ingredientIds)
		{
			ingredientIndex++;
			final PlayerRelicData ingredientRelic = storedRelics.stream().filter(relic -> (relic.getRelicId() == ingredientId) && (relic.getRelicIndex() < 300)).findFirst().orElse(null);
			if ((ingredientRelic != null) && (ingredientRelic.getRelicCount() > 0))
			{
				ingredientRelic.setRelicCount(ingredientRelic.getRelicCount() - 1);
				if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
				{
					player.sendMessage(String.format("Ingredient Relic %d data updated, ID: %d, Count: %d", ingredientIndex, ingredientRelic.getRelicId(), ingredientRelic.getRelicCount()));
				}
			}
		}
		
		final ArrayList<Integer> successCompoundIds = new ArrayList<>();
		final ArrayList<Integer> failCompoundIds = new ArrayList<>();
		for (int i = 0; i < compoundAttempts; i++)
		{
			final Entry<Boolean, Integer> result = RelicData.getInstance().getRelicByCompound(RelicGrade.values()[_relicsUsedGrade]);
			final int obtainedRelicId = result.getValue();
			if (result.getKey())
			{
				successCompoundIds.add(obtainedRelicId);
			}
			else
			{
				failCompoundIds.add(obtainedRelicId);
			}
			
			PlayerRelicData existingRelic = null;
			for (PlayerRelicData relic : storedRelics)
			{
				if (relic.getRelicId() == obtainedRelicId)
				{
					existingRelic = relic;
					break;
				}
			}
			
			final PlayerRelicData newRelic = new PlayerRelicData(obtainedRelicId, 0, 0, 0, 0);
			if (existingRelic != null)
			{
				existingRelic.setRelicCount(existingRelic.getRelicCount() + 1);
				player.sendPacket(new ExRelicsUpdateList(1, existingRelic.getRelicId(), 0, existingRelic.getRelicCount() + 1)); // Update confirmed relic list with new relic.
				if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
				{
					player.sendMessage("Existing relic id: " + obtainedRelicId + " count increased.");
				}
				
				if (existingRelic.getRelicIndex() == 0)
				{
					if (!player.isRelicRegistered(existingRelic.getRelicId(), existingRelic.getRelicLevel()))
					{
						player.sendPacket(new ExRelicsCollectionUpdate(player, existingRelic.getRelicId(), existingRelic.getRelicLevel())); // Update collection list.
					}
				}
			}
			else
			{
				newRelic.setRelicIndex(0);
				storedRelics.add(newRelic);
				player.sendPacket(new ExRelicsUpdateList(1, newRelic.getRelicId(), 0, 0)); // Update confirmed relic list with new relic.
				if (newRelic.getRelicIndex() == 0)
				{
					if (!player.isRelicRegistered(newRelic.getRelicId(), newRelic.getRelicLevel()))
					{
						player.sendPacket(new ExRelicsCollectionUpdate(player, newRelic.getRelicId(), newRelic.getRelicLevel())); // Update collection list.
					}
				}
			}
		}
		
		final int successCount = successCompoundIds.size();
		final int failCount = failCompoundIds.size();
		if (successCount > failCount)
		{
			player.sendPacket(new ExShowScreenMessage("Relics compound has failed.", ExShowScreenMessage.TOP_CENTER, 5000, 0, true, false));
		}
		
		player.sendMessage("You obtained through compounding: " + compoundAttempts + " relics.");
		player.sendMessage("Relics compound summary: " + successCompoundIds.size() + " succeded and " + failCompoundIds.size() + " failed.");
		player.sendPacket(new ExRelicsCombination(player, successCompoundIds, failCompoundIds));
		player.sendPacket(new ExRelicsList(player)); // Update confirmed relic list relics count.
		player.storeRelics();
	}
}
