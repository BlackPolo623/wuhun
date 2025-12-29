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
package org.l2jmobius.gameserver.managers.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Index
 */
public class LetterCollectorManager
{
	protected static final Logger LOGGER = Logger.getLogger(LetterCollectorManager.class.getName());
	
	private final Map<Integer, LetterCollectorRewardHolder> _rewards = new HashMap<>();
	private final Map<Integer, List<ItemHolder>> _words = new HashMap<>();
	private final Map<String, Integer> _letter = new HashMap<>();
	private final Map<Integer, Boolean> _needToSumAllChance = new HashMap<>();
	
	private int _minLevel = 1;
	private int _maxLevel = PlayerConfig.PLAYER_MAXIMUM_LEVEL;
	
	protected LetterCollectorManager()
	{
	}
	
	public void init()
	{
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _rewards.size() + " words.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _letter.size() + " letters.");
	}
	
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	public void setMinLevel(int minLevel)
	{
		_minLevel = minLevel;
	}
	
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	public void setMaxLevel(int maxLevel)
	{
		if (maxLevel < 1)
		{
			_maxLevel = PlayerConfig.PLAYER_MAXIMUM_LEVEL;
		}
		else
		{
			_maxLevel = maxLevel;
		}
	}
	
	public LetterCollectorRewardHolder getRewards(int id)
	{
		return _rewards.get(id);
	}
	
	public List<ItemHolder> getWord(int id)
	{
		return _words.get(id);
	}
	
	public void setRewards(Map<Integer, LetterCollectorRewardHolder> rewards)
	{
		_rewards.putAll(rewards);
	}
	
	public void setWords(Map<Integer, List<ItemHolder>> words)
	{
		_words.putAll(words);
	}
	
	public void addRewards(int id, LetterCollectorRewardHolder rewards)
	{
		_rewards.put(id, rewards);
	}
	
	public void addWords(int id, List<ItemHolder> words)
	{
		_words.put(id, words);
	}
	
	public void resetField()
	{
		_minLevel = 1;
		_rewards.clear();
		_words.clear();
		_needToSumAllChance.clear();
	}
	
	public void setLetters(Map<String, Integer> letters)
	{
		_letter.putAll(letters);
	}
	
	public Map<String, Integer> getLetters()
	{
		return _letter;
	}
	
	public void setNeedToSumAllChance(int id, boolean needToSumAllChance)
	{
		_needToSumAllChance.put(id, needToSumAllChance);
	}
	
	public boolean getNeedToSumAllChance(int id)
	{
		return _needToSumAllChance.get(id);
	}
	
	public static class LetterCollectorRewardHolder
	{
		final List<ItemChanceHolder> _rewards;
		final double _chance;
		
		public LetterCollectorRewardHolder(List<ItemChanceHolder> rewards, double chance)
		{
			_rewards = rewards;
			_chance = chance;
		}
		
		public List<ItemChanceHolder> getRewards()
		{
			return _rewards;
		}
		
		public double getChance()
		{
			return _chance;
		}
	}
	
	public static LetterCollectorManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final LetterCollectorManager INSTANCE = new LetterCollectorManager();
	}
}
