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
package org.l2jmobius.gameserver.model.script.newquestdata;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Magik
 */
public class NewQuest
{
	private static final Logger LOGGER = Logger.getLogger(NewQuest.class.getName());
	
	private final int _id;
	private int _questType;
	private final String _name;
	private final int _startNpcId;
	private final int _endNpcId;
	private final int _startItemId;
	private final NewQuestLocation _location;
	private final NewQuestCondition _conditions;
	private final NewQuestGoal _goal;
	private final NewQuestReward _rewards;
	
	public NewQuest(StatSet set)
	{
		_id = set.getInt("id", -1);
		_questType = set.getInt("type", -1);
		_name = set.getString("name", "");
		_startNpcId = set.getInt("startNpcId", -1);
		_endNpcId = set.getInt("endNpcId", -1);
		_startItemId = set.getInt("startItemId", -1);
		_location = new NewQuestLocation(set.getInt("startLocationId", 0), set.getInt("endLocationId", 0), set.getInt("questLocationId", 0));
		
		final String classIds = set.getString("classIds", "");
		final List<PlayerClass> classRestriction = classIds.isEmpty() ? Collections.emptyList() : Arrays.stream(classIds.split(";")).map(it -> PlayerClass.getPlayerClass(Integer.parseInt(it))).collect(Collectors.toList());
		final String preQuestId = set.getString("preQuestId", "");
		final List<Integer> preQuestIds = preQuestId.isEmpty() ? Collections.emptyList() : Arrays.stream(preQuestId.split(";")).map(it -> Integer.parseInt(it)).collect(Collectors.toList());
		_conditions = new NewQuestCondition(set.getInt("minLevel", -1), set.getInt("maxLevel", ExperienceData.getInstance().getMaxLevel()), preQuestIds, classRestriction, set.getBoolean("oneOfPreQuests", false), set.getBoolean("specificStart", false));
		
		final int goalItemId = set.getInt("goalItemId", -1);
		final int goalCount = set.getInt("goalCount", -1);
		if (goalItemId > 0)
		{
			final ItemTemplate template = ItemData.getInstance().getTemplate(goalItemId);
			if (template == null)
			{
				LOGGER.warning(getClass().getSimpleName() + _id + ": Could not find goal item template with id " + goalItemId);
			}
			else if (goalCount > 1)
			{
				if (!template.isStackable())
				{
					LOGGER.warning(getClass().getSimpleName() + _id + ": Item template with id " + goalItemId + " should be stackable.");
				}
				
				if (!template.isQuestItem())
				{
					LOGGER.warning(getClass().getSimpleName() + _id + ": Item template with id " + goalItemId + " should be quest item.");
				}
			}
		}
		
		_goal = new NewQuestGoal(goalItemId, goalCount, set.getString("goalString", ""));
		
		_rewards = new NewQuestReward(set.getLong("rewardExp", -1), set.getLong("rewardSp", -1), set.getInt("rewardLevel", -1), set.getList("rewardItems", ItemHolder.class));
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getQuestType()
	{
		return _questType;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public int getStartNpcId()
	{
		return _startNpcId;
	}
	
	public int getEndNpcId()
	{
		return _endNpcId;
	}
	
	public int getStartItemId()
	{
		return _startItemId;
	}
	
	public NewQuestLocation getLocation()
	{
		return _location;
	}
	
	public NewQuestCondition getConditions()
	{
		return _conditions;
	}
	
	public NewQuestGoal getGoal()
	{
		return _goal;
	}
	
	public NewQuestReward getRewards()
	{
		return _rewards;
	}
	
	public void setType(int type)
	{
		_questType = type;
	}
}
