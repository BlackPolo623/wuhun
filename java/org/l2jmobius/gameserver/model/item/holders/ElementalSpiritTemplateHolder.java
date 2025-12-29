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
package org.l2jmobius.gameserver.model.item.holders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JoeAlisson
 */
public class ElementalSpiritTemplateHolder
{
	private final byte _type;
	private final byte _stage;
	private final int _npcId;
	private final int _maxCharacteristics;
	private final int _extractItem;
	
	private final Map<Integer, SpiritLevel> _levels;
	private List<ItemHolder> _itemsToEvolve;
	private List<ElementalSpiritAbsorbItemHolder> _absorbItems;
	
	public ElementalSpiritTemplateHolder(byte type, byte stage, int npcId, int extractItem, int maxCharacteristics)
	{
		_type = type;
		_stage = stage;
		_npcId = npcId;
		_extractItem = extractItem;
		_maxCharacteristics = maxCharacteristics;
		_levels = new HashMap<>(10);
	}
	
	public void addLevelInfo(int level, int attack, int defense, int criticalRate, int criticalDamage, long maxExperience)
	{
		final SpiritLevel spiritLevel = new SpiritLevel();
		spiritLevel.attack = attack;
		spiritLevel.defense = defense;
		spiritLevel.criticalRate = criticalRate;
		spiritLevel.criticalDamage = criticalDamage;
		spiritLevel.maxExperience = maxExperience;
		_levels.put(level, spiritLevel);
	}
	
	public void addItemToEvolve(Integer itemId, Integer count)
	{
		if (_itemsToEvolve == null)
		{
			_itemsToEvolve = new ArrayList<>(2);
		}
		
		_itemsToEvolve.add(new ItemHolder(itemId, count));
	}
	
	public byte getType()
	{
		return _type;
	}
	
	public byte getStage()
	{
		return _stage;
	}
	
	public int getNpcId()
	{
		return _npcId;
	}
	
	public long getMaxExperienceAtLevel(int level)
	{
		final SpiritLevel spiritLevel = _levels.get(level);
		return spiritLevel == null ? 0 : spiritLevel.maxExperience;
	}
	
	public int getMaxLevel()
	{
		return _levels.size();
	}
	
	public int getAttackAtLevel(int level)
	{
		return _levels.get(level).attack;
	}
	
	public int getDefenseAtLevel(int level)
	{
		return _levels.get(level).defense;
	}
	
	public int getCriticalRateAtLevel(int level)
	{
		return _levels.get(level).criticalRate;
	}
	
	public int getCriticalDamageAtLevel(int level)
	{
		return _levels.get(level).criticalDamage;
	}
	
	public int getMaxCharacteristics()
	{
		return _maxCharacteristics;
	}
	
	public List<ItemHolder> getItemsToEvolve()
	{
		return _itemsToEvolve == null ? Collections.emptyList() : _itemsToEvolve;
	}
	
	public void addAbsorbItem(Integer itemId, Integer experience)
	{
		if (_absorbItems == null)
		{
			_absorbItems = new ArrayList<>();
		}
		
		_absorbItems.add(new ElementalSpiritAbsorbItemHolder(itemId, experience));
	}
	
	public List<ElementalSpiritAbsorbItemHolder> getAbsorbItems()
	{
		return _absorbItems == null ? Collections.emptyList() : _absorbItems;
	}
	
	public int getExtractItem()
	{
		return _extractItem;
	}
	
	private static class SpiritLevel
	{
		public SpiritLevel()
		{
		}
		
		long maxExperience;
		int criticalDamage;
		int criticalRate;
		int defense;
		int attack;
	}
}
