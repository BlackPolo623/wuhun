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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.MissionLevelHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Index
 */
public class MissionLevel implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(MissionLevel.class.getName());
	
	private final Map<Integer, MissionLevelHolder> _template = new HashMap<>();
	private int _currentSeason;
	
	protected MissionLevel()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_template.clear();
		parseDatapackFile("data/MissionLevel.xml");
		if (_currentSeason > 0)
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _template.size() + " seasons.");
		}
		else
		{
			_template.clear();
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "current", current -> _currentSeason = parseInteger(current.getAttributes(), "season"));
			forEach(listNode, "missionLevel", missionNode ->
			{
				final StatSet missionSet = new StatSet(parseAttributes(missionNode));
				final AtomicInteger season = new AtomicInteger(missionSet.getInt("season"));
				final AtomicInteger maxLevel = new AtomicInteger(missionSet.getInt("maxLevel"));
				final AtomicBoolean bonusRewardIsAvailable = new AtomicBoolean(missionSet.getBoolean("bonusRewardIsAvailable"));
				final AtomicBoolean bonusRewardByLevelUp = new AtomicBoolean(missionSet.getBoolean("bonusRewardByLevelUP"));
				final AtomicReference<Map<Integer, ItemHolder>> keyReward = new AtomicReference<>(new HashMap<>());
				final AtomicReference<Map<Integer, ItemHolder>> normalReward = new AtomicReference<>(new HashMap<>());
				final AtomicReference<Map<Integer, Integer>> xpForLevel = new AtomicReference<>(new HashMap<>());
				final AtomicReference<ItemHolder> specialReward = new AtomicReference<>();
				final AtomicReference<ItemHolder> bonusReward = new AtomicReference<>();
				forEach(missionNode, "expTable", expListNode -> forEach(expListNode, "exp", expNode ->
				{
					final StatSet expSet = new StatSet(parseAttributes(expNode));
					xpForLevel.get().put(expSet.getInt("level"), expSet.getInt("amount"));
				}));
				forEach(missionNode, "baseRewards", baseRewardsNode -> forEach(baseRewardsNode, "baseReward", rewards ->
				{
					final StatSet rewardsSet = new StatSet(parseAttributes(rewards));
					normalReward.get().put(rewardsSet.getInt("level"), new ItemHolder(rewardsSet.getInt("itemId"), rewardsSet.getLong("itemCount")));
				}));
				forEach(missionNode, "keyRewards", keyRewardsNode -> forEach(keyRewardsNode, "keyReward", rewards ->
				{
					final StatSet rewardsSet = new StatSet(parseAttributes(rewards));
					keyReward.get().put(rewardsSet.getInt("level"), new ItemHolder(rewardsSet.getInt("itemId"), rewardsSet.getLong("itemCount")));
				}));
				forEach(missionNode, "specialReward", specialRewardNode ->
				{
					final StatSet specialRewardSet = new StatSet(parseAttributes(specialRewardNode));
					specialReward.set(new ItemHolder(specialRewardSet.getInt("itemId"), specialRewardSet.getLong("itemCount")));
				});
				forEach(missionNode, "bonusReward", bonusRewardNode ->
				{
					final StatSet bonusRewardSet = new StatSet(parseAttributes(bonusRewardNode));
					bonusReward.set(new ItemHolder(bonusRewardSet.getInt("itemId"), bonusRewardSet.getLong("itemCount")));
				});
				int bonusLevel = normalReward.get().keySet().stream().max(Integer::compare).orElse(maxLevel.get());
				if (bonusLevel == maxLevel.get())
				{
					bonusLevel = bonusLevel - 1;
				}
				
				_template.put(season.get(), new MissionLevelHolder(maxLevel.get(), bonusLevel + 1, xpForLevel.get(), normalReward.get(), keyReward.get(), specialReward.get(), bonusReward.get(), bonusRewardByLevelUp.get(), bonusRewardIsAvailable.get()));
			});
		});
	}
	
	public int getCurrentSeason()
	{
		return _currentSeason;
	}
	
	public MissionLevelHolder getMissionBySeason(int season)
	{
		return _template.getOrDefault(season, null);
	}
	
	public static MissionLevel getInstance()
	{
		return MissionLevel.SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MissionLevel INSTANCE = new MissionLevel();
	}
}
