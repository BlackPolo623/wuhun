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
package org.l2jmobius.gameserver.model.item.enchant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;

/**
 * @author CostyKiller
 */
public class RewardItemsOnFailure
{
	private final Map<Integer, Map<Integer, List<ItemChanceHolder>>> _rewards = new HashMap<>();
	
	public void addItemToHolder(int destroyedItemId, int enchantLevel, int rewardId, long count, double chance)
	{
		final ItemChanceHolder item = new ItemChanceHolder(rewardId, chance, count);
		_rewards.computeIfAbsent(destroyedItemId, k -> new HashMap<>()).computeIfAbsent(enchantLevel, k -> new ArrayList<>()).add(item);
	}
	
	public List<ItemChanceHolder> getRewardItems(int destroyedItemId, int enchantLevel)
	{
		return _rewards.getOrDefault(destroyedItemId, Collections.emptyMap()).getOrDefault(enchantLevel, Collections.emptyList());
	}
	
	public int size()
	{
		int count = 0;
		for (Map<Integer, List<ItemChanceHolder>> rewardsByEnchant : _rewards.values())
		{
			for (List<ItemChanceHolder> rewards : rewardsByEnchant.values())
			{
				count += rewards.size();
			}
		}
		
		return count;
	}
}