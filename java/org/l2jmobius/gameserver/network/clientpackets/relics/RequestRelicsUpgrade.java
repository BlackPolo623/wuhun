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
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerRelicData;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsCollectionUpdate;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsList;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsUpdateList;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsUpgrade;

/**
 * @author CostyKiller, Brado
 */
public class RequestRelicsUpgrade extends ClientPacket
{
	private int _relicId;
	private int _relicLevel;
	private final List<Integer> _ingredients = new ArrayList<>();
	private int _chance = 0;
	
	@Override
	protected void readImpl()
	{
		_relicId = readInt();
		_relicLevel = readInt();
		
		final int ingredientCount = readInt();
		for (int i = 0; i < ingredientCount; i++)
		{
			_ingredients.add(readInt());
		}
		
		_chance = RelicData.getInstance().getEnchantRateByIngredientCount(ingredientCount);
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final ItemHolder itemFee = RelicData.getInstance().getEnchantFee();
		if (!player.destroyItemByItemId(ItemProcessType.FEE, itemFee.getId(), itemFee.getCount(), player, true))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ADENA));
			return;
		}
		
		final boolean success = Rnd.get(100) < _chance;
		final Collection<PlayerRelicData> storedRelics = player.getRelics();
		PlayerRelicData existingRelic = null;
		for (PlayerRelicData relic : storedRelics)
		{
			if ((relic.getRelicId() == _relicId) && (relic.getRelicIndex() < 300))
			{
				existingRelic = relic;
				break;
			}
		}
		
		if ((existingRelic != null) && (existingRelic.getRelicLevel() < 4))
		{
			existingRelic.setRelicLevel(success ? existingRelic.getRelicLevel() + 1 : existingRelic.getRelicLevel());
			_relicLevel = existingRelic.getRelicLevel();
			if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				player.sendMessage("Relic Id: " + existingRelic.getRelicId() + " " + (success ? "Upgrade successful! Relic is now level: " : "Upgrade failed! Relic is still level: ") + _relicLevel);
			}
			
			player.sendPacket(new ExRelicsUpdateList(1, existingRelic.getRelicId(), existingRelic.getRelicLevel(), existingRelic.getRelicCount())); // Update confirmed relic list with new relic.
			if (!player.isRelicRegistered(existingRelic.getRelicId(), existingRelic.getRelicLevel()))
			{
				player.sendPacket(new ExRelicsCollectionUpdate(player, existingRelic.getRelicId(), existingRelic.getRelicLevel())); // Update collection list.
			}
		}
		
		for (int ingredient : _ingredients)
		{
			PlayerRelicData ingredientRelic = null;
			for (PlayerRelicData relic : storedRelics)
			{
				if (relic.getRelicId() == ingredient)
				{
					ingredientRelic = relic;
					break;
				}
			}
			
			if ((ingredientRelic != null) && (ingredientRelic.getRelicCount() > 0))
			{
				ingredientRelic.setRelicCount(ingredientRelic.getRelicCount() - 1);
				if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
				{
					player.sendMessage("Ingredient Relic data updated, ID: " + ingredientRelic + ", Count: " + ingredientRelic.getRelicCount());
				}
			}
		}
		
		player.storeRelics();
		
		final RelicDataHolder relicHolder = RelicData.getInstance().getRelic(_relicId);
		player.giveRelicSkill(relicHolder, _relicLevel);
		player.sendPacket(new ExRelicsList(player)); // Update confirmed relic list relics count.
		player.sendPacket(new ExRelicsUpgrade(success, _relicId, _relicLevel));
	}
}
