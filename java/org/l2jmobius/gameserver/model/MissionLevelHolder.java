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
package org.l2jmobius.gameserver.model;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Index
 */
public class MissionLevelHolder
{
	private int _maxLevel;
	private final int _bonusLevel;
	private final Map<Integer, Integer> _xpForLevel = new HashMap<>();
	private final Map<Integer, ItemHolder> _normalReward = new HashMap<>();
	private final Map<Integer, ItemHolder> _keyReward = new HashMap<>();
	private final ItemHolder _specialReward;
	private final ItemHolder _bonusReward;
	private final boolean _bonusRewardIsAvailable;
	private final boolean _bonusRewardByLevelUp;
	
	public MissionLevelHolder(int maxLevel, int bonusLevel, Map<Integer, Integer> xpForLevel, Map<Integer, ItemHolder> normalReward, Map<Integer, ItemHolder> keyReward, ItemHolder specialReward, ItemHolder bonusReward, boolean bonusRewardByLevelUp, boolean bonusRewardIsAvailable)
	{
		_maxLevel = maxLevel;
		_bonusLevel = bonusLevel;
		_xpForLevel.putAll(xpForLevel);
		_normalReward.putAll(normalReward);
		_keyReward.putAll(keyReward);
		_specialReward = specialReward;
		_bonusReward = bonusReward;
		_bonusRewardByLevelUp = bonusRewardByLevelUp;
		_bonusRewardIsAvailable = bonusRewardIsAvailable;
	}
	
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	public void setMaxLevel(int maxLevel)
	{
		_maxLevel = maxLevel;
	}
	
	public int getBonusLevel()
	{
		return _bonusLevel;
	}
	
	public Map<Integer, Integer> getXPForLevel()
	{
		return _xpForLevel;
	}
	
	public int getXPForSpecifiedLevel(int level)
	{
		return _xpForLevel.get(level == 0 ? level + 1 : level);
	}
	
	public Map<Integer, ItemHolder> getNormalRewards()
	{
		return _normalReward;
	}
	
	public Map<Integer, ItemHolder> getKeyRewards()
	{
		return _keyReward;
	}
	
	public ItemHolder getSpecialReward()
	{
		return _specialReward;
	}
	
	public ItemHolder getBonusReward()
	{
		return _bonusReward;
	}
	
	public boolean getBonusRewardByLevelUp()
	{
		return _bonusRewardByLevelUp;
	}
	
	public boolean getBonusRewardIsAvailable()
	{
		return _bonusRewardIsAvailable;
	}
}
