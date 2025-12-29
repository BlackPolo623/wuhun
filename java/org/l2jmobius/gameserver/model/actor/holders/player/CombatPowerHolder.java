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
package org.l2jmobius.gameserver.model.actor.holders.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.gameserver.data.holders.RelicCollectionDataHolder;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.data.holders.RelicEnchantHolder;
import org.l2jmobius.gameserver.data.xml.RelicCollectionData;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * @author Brado
 */
public class CombatPowerHolder
{
	private final Player _owner;
	private final AtomicInteger _itemCP = new AtomicInteger();
	private final AtomicInteger _blessCP = new AtomicInteger();
	private final AtomicInteger _ensoulCP = new AtomicInteger();
	private final AtomicInteger _relicEffectCP = new AtomicInteger();
	private final AtomicInteger _relicCollectionCP = new AtomicInteger();
	private final AtomicInteger _adenLabCollectionCP = new AtomicInteger();
	private final AtomicInteger _skillCP = new AtomicInteger();
	
	public CombatPowerHolder(Player player)
	{
		_owner = player;
	}
	
	public int getItemCombatPower()
	{
		return _itemCP.get();
	}
	
	public int getRelicEffectCombatPower()
	{
		return _relicEffectCP.get();
	}
	
	public int getRelicCollectionCombatPower()
	{
		return _relicCollectionCP.get();
	}
	
	public int getAdenLabCollectionCP()
	{
		return _adenLabCollectionCP.get();
	}
	
	public int getBlessCP()
	{
		return _blessCP.get();
	}
	
	public int getEnsoulCP()
	{
		return _ensoulCP.get();
	}
	
	public int getSkillCombatPower()
	{
		return _skillCP.get();
	}
	
	public int getTotalCombatPower()
	{
		return _itemCP.get() + _blessCP.get() + _ensoulCP.get() + _relicEffectCP.get() + _relicCollectionCP.get() + _adenLabCollectionCP.get() + _skillCP.get();
	}
	
	public void addItemCombatPower(Item item)
	{
		if (item.isBlessed())
		{
			_blessCP.addAndGet(item.getTemplate().getGearScore() + (int) (Math.pow(1.2, item.getEnchantLevel())));
		}
		else if (!item.getSpecialAbilities().isEmpty() || !item.getAdditionalSpecialAbilities().isEmpty())
		{
			_ensoulCP.addAndGet(item.getTemplate().getGearScore() + (int) (Math.pow(1.2, item.getEnchantLevel())));
		}
		else
		{
			_itemCP.addAndGet(item.getTemplate().getGearScore() + (int) (Math.pow(1.2, item.getEnchantLevel())));
		}
		
		_owner.sendCombatPower();
	}
	
	public void removeItemCombatPower(Item item)
	{
		if (item.isBlessed())
		{
			_blessCP.addAndGet(-(item.getTemplate().getGearScore() + (int) (Math.pow(1.2, item.getEnchantLevel()))));
		}
		else if (!item.getSpecialAbilities().isEmpty() || !item.getAdditionalSpecialAbilities().isEmpty())
		{
			_ensoulCP.addAndGet(-(item.getTemplate().getGearScore() + (int) (Math.pow(1.2, item.getEnchantLevel()))));
		}
		else
		{
			_itemCP.addAndGet(-(item.getTemplate().getGearScore() + (int) (Math.pow(1.2, item.getEnchantLevel()))));
		}
		
		_owner.sendCombatPower();
	}
	
	public void updateRelicCombatPower(Player player)
	{
		_relicEffectCP.set(0);
		for (PlayerRelicData data : player.getRelics())
		{
			final RelicDataHolder rdh = RelicData.getInstance().getRelic(data.getRelicId());
			for (RelicEnchantHolder reh : rdh.getEnchantHoldders())
			{
				if (reh.getEnchantLevel() == data.getRelicLevel())
				{
					_relicEffectCP.addAndGet(reh.getCombatPower());
				}
			}
		}
	}
	
	public void updateRelicCollectionCombatPower()
	{
		// Reset combat power.
		_relicCollectionCP.set(0);
		
		// Map to track the count of each relic collection.
		final Map<Integer, Integer> relicCollectionCounts = new HashMap<>();
		for (PlayerRelicCollectionData relicCollection : _owner.getRelicCollections())
		{
			relicCollectionCounts.merge(relicCollection.getRelicCollectionId(), 1, Integer::sum);
		}
		
		// Calculate combat power from completed collections.
		for (Entry<Integer, Integer> entry : relicCollectionCounts.entrySet())
		{
			final int relicCollectionId = entry.getKey();
			final int count = entry.getValue();
			final RelicCollectionDataHolder relicCollection = RelicCollectionData.getInstance().getRelicCollection(relicCollectionId);
			if ((relicCollection != null) && (count >= relicCollection.getCompleteCount()))
			{
				_relicCollectionCP.addAndGet(relicCollection.getCombatPower());
			}
		}
	}
	
	public void setAdenLabCombatPower(int totalAmount)
	{
		_adenLabCollectionCP.set(totalAmount);
		_owner.sendCombatPower();
	}
	
	public void addAdenLabCombatPower(int additionalPower)
	{
		_adenLabCollectionCP.addAndGet(additionalPower);
		_owner.sendCombatPower();
	}
	
	public void setSkillCombatPower(int value)
	{
		_skillCP.set(value);
		_owner.sendCombatPower();
	}
	
	public void addSkillCombatPower(int value)
	{
		_skillCP.addAndGet(value);
		_owner.sendCombatPower();
	}
}
