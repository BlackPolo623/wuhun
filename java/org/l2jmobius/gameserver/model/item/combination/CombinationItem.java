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
package org.l2jmobius.gameserver.model.item.combination;

import java.util.EnumMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.StatSet;

/**
 * @author UnAfraid, Mobius
 */
public class CombinationItem
{
	private final int _itemOne;
	private final int _enchantOne;
	private final int _itemTwo;
	private final int _enchantTwo;
	private final long _commission;
	private final float _chance;
	private final boolean _announce;
	private final Map<CombinationItemType, CombinationItemReward> _rewards = new EnumMap<>(CombinationItemType.class);
	
	public CombinationItem(StatSet set)
	{
		_itemOne = set.getInt("one");
		_enchantOne = set.getInt("enchantOne", 0);
		_itemTwo = set.getInt("two");
		_enchantTwo = set.getInt("enchantTwo", 0);
		_commission = set.getLong("commission", 0);
		_chance = set.getFloat("chance", 33);
		_announce = set.getBoolean("announce", false);
	}
	
	public int getItemOne()
	{
		return _itemOne;
	}
	
	public int getEnchantOne()
	{
		return _enchantOne;
	}
	
	public int getItemTwo()
	{
		return _itemTwo;
	}
	
	public int getEnchantTwo()
	{
		return _enchantTwo;
	}
	
	public long getCommission()
	{
		return _commission;
	}
	
	public float getChance()
	{
		return _chance;
	}
	
	public boolean isAnnounce()
	{
		return _announce;
	}
	
	public void addReward(CombinationItemReward item)
	{
		_rewards.put(item.getType(), item);
	}
	
	public CombinationItemReward getReward(CombinationItemType type)
	{
		return _rewards.get(type);
	}
}
