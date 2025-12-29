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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.enums.player.RelicGrade;

/**
 * @author Brado
 */
public class RelicCouponHolder
{
	private final int _itemId;
	private final int _relicId;
	private final int _summonCount;
	private final Set<Integer> _disabledIds = new HashSet<>();
	private final Map<RelicGrade, Integer> _grades = new HashMap<>();
	private final Map<Integer, Integer> _chanceRolls = new HashMap<>();
	
	public RelicCouponHolder(int itemId, int relicId, int summonCount)
	{
		_itemId = itemId;
		_relicId = relicId;
		_summonCount = summonCount;
	}
	
	public RelicCouponHolder(int itemId, int summonCount, Map<RelicGrade, Integer> grades, Set<Integer> disabledIds)
	{
		_itemId = itemId;
		_relicId = 0;
		_summonCount = summonCount;
		_grades.putAll(grades);
		_disabledIds.addAll(disabledIds);
	}
	
	public RelicCouponHolder(int itemId, int summonCount, Map<Integer, Integer> chanceRolls)
	{
		_itemId = itemId;
		_relicId = 0;
		_summonCount = summonCount;
		_chanceRolls.putAll(chanceRolls);
	}
	
	public int getItemId()
	{
		return _itemId;
	}
	
	public int getRelicId()
	{
		return _relicId;
	}
	
	public int getRelicSummonCount()
	{
		return _summonCount;
	}
	
	public Map<RelicGrade, Integer> getCouponRelicGrades()
	{
		return _grades;
	}
	
	public Collection<Integer> getDisabledIds()
	{
		return _disabledIds;
	}
	
	public Map<Integer, Integer> getChanceRolls()
	{
		return _chanceRolls;
	}
}
