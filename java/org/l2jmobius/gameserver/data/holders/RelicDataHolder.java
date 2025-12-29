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
package org.l2jmobius.gameserver.data.holders;

import java.util.List;

import org.l2jmobius.gameserver.model.actor.enums.player.RelicGrade;

/**
 * @author CostyKiller
 */
public class RelicDataHolder
{
	private final int _relicId;
	private final int _parentRelicId;
	private final RelicGrade _grade;
	private final List<RelicEnchantHolder> _enchantHolder;
	private final long _summonChance;
	private final float _compoundChanceModifier;
	private final float _compoundUpGradeChanceModifier;
	
	public RelicDataHolder(int relicId, int parentRelicId, RelicGrade grade, long summonChance, List<RelicEnchantHolder> enchantHolder, float compoundChanceModifier, float compoundUpGradeChanceModifier)
	{
		_relicId = relicId;
		_parentRelicId = parentRelicId;
		_grade = grade;
		_summonChance = summonChance;
		_enchantHolder = enchantHolder;
		_compoundChanceModifier = compoundChanceModifier;
		_compoundUpGradeChanceModifier = compoundUpGradeChanceModifier;
	}
	
	public int getRelicId()
	{
		return _relicId;
	}
	
	public int getParentRelicId()
	{
		return _parentRelicId;
	}
	
	public RelicGrade getGrade()
	{
		return _grade;
	}
	
	public int getGradeOrdinal()
	{
		return _grade.ordinal();
	}
	
	public long getSummonChance()
	{
		return _summonChance;
	}
	
	public List<RelicEnchantHolder> getEnchantHoldders()
	{
		return _enchantHolder;
	}
	
	public RelicEnchantHolder getEnchantHolderByEnchant(int enchant)
	{
		
		for (RelicEnchantHolder holder : _enchantHolder)
		{
			if (enchant == holder.getEnchantLevel())
			{
				return holder;
			}
		}
		
		return _enchantHolder.getFirst();
	}
	
	public float getCompoundChanceModifier()
	{
		return _compoundChanceModifier;
	}
	
	public float getCompoundUpGradeChanceModifier()
	{
		return _compoundUpGradeChanceModifier;
	}
}
