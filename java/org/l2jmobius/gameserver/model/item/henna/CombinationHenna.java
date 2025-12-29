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
package org.l2jmobius.gameserver.model.item.henna;

import java.util.EnumMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.combination.CombinationItemType;

/**
 * @author Index
 */
public class CombinationHenna
{
	private final int _henna;
	private final int _itemOne;
	private final long _countOne;
	private final int _itemTwo;
	private final long _countTwo;
	private final long _commission;
	private final float _chance;
	private final Map<CombinationItemType, CombinationHennaReward> _rewards = new EnumMap<>(CombinationItemType.class);
	
	public CombinationHenna(StatSet set)
	{
		_henna = set.getInt("dyeId");
		_itemOne = set.getInt("itemOne", -1);
		_countOne = set.getLong("countOne", 1);
		_itemTwo = set.getInt("itemTwo", -1);
		_countTwo = set.getLong("countTwo", 1);
		_commission = set.getLong("commission", 0);
		_chance = set.getFloat("chance", 33);
	}
	
	public int getHenna()
	{
		return _henna;
	}
	
	public int getItemOne()
	{
		return _itemOne;
	}
	
	public long getCountOne()
	{
		return _countOne;
	}
	
	public int getItemTwo()
	{
		return _itemTwo;
	}
	
	public long getCountTwo()
	{
		return _countTwo;
	}
	
	public long getCommission()
	{
		return _commission;
	}
	
	public float getChance()
	{
		return _chance;
	}
	
	public void addReward(CombinationHennaReward item)
	{
		_rewards.put(item.getType(), item);
	}
	
	public CombinationHennaReward getReward(CombinationItemType type)
	{
		return _rewards.get(type);
	}
}
