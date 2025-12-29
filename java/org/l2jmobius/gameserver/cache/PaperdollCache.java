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
package org.l2jmobius.gameserver.cache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.xml.ArmorSetData;
import org.l2jmobius.gameserver.model.ArmorSet;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.stats.BaseStat;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * @author Sahar
 */
public class PaperdollCache
{
	private final Set<Item> _paperdollItems = ConcurrentHashMap.newKeySet();
	
	private final Map<BaseStat, Double> _baseStatValues = new ConcurrentHashMap<>();
	private final Map<Stat, Double> _statValues = new ConcurrentHashMap<>();
	private int _armorSetEnchant = -1;
	
	public Set<Item> getPaperdollItems()
	{
		return _paperdollItems;
	}
	
	public void clearCachedStats()
	{
		_baseStatValues.clear();
		_statValues.clear();
		
		clearArmorSetEnchant();
	}
	
	public void clearArmorSetEnchant()
	{
		_armorSetEnchant = -1;
	}
	
	public double getBaseStatValue(Player player, BaseStat stat)
	{
		final Double baseStatValue = _baseStatValues.get(stat);
		if (baseStatValue != null)
		{
			return baseStatValue.doubleValue();
		}
		
		final Set<ArmorSet> appliedSets = new HashSet<>(2);
		double value = 0;
		for (Item item : _paperdollItems)
		{
			for (ArmorSet set : ArmorSetData.getInstance().getSets(item.getId()))
			{
				if ((set.getPieceCount(player) >= set.getMinimumPieces()) && appliedSets.add(set))
				{
					value += set.getStatsBonus(stat);
				}
			}
		}
		
		_baseStatValues.put(stat, value);
		return value;
	}
	
	public int getArmorSetEnchant(Playable playable)
	{
		int armorSetEnchant = _armorSetEnchant;
		if (armorSetEnchant >= 0)
		{
			return armorSetEnchant;
		}
		
		armorSetEnchant = 0;
		for (Item item : _paperdollItems)
		{
			for (ArmorSet set : ArmorSetData.getInstance().getSets(item.getId()))
			{
				final int enchantEffect = set.getSetEnchant(playable);
				if (enchantEffect > armorSetEnchant)
				{
					armorSetEnchant = enchantEffect;
				}
			}
		}
		
		_armorSetEnchant = armorSetEnchant;
		return armorSetEnchant;
	}
	
	public double getStats(Stat stat)
	{
		final Double statValue = _statValues.get(stat);
		if (statValue != null)
		{
			return statValue.doubleValue();
		}
		
		double value = 0;
		for (Item item : _paperdollItems)
		{
			value += item.getTemplate().getStats(stat, 0);
		}
		
		_statValues.put(stat, value);
		return value;
	}
}
