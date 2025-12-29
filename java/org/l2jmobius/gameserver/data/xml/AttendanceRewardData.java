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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.AttendanceRewardsConfig;
import org.l2jmobius.gameserver.data.holders.AttendanceItemHolder;
import org.l2jmobius.gameserver.model.StatSet;

/**
 * @author Mobius
 */
public class AttendanceRewardData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(AttendanceRewardData.class.getName());
	private final List<AttendanceItemHolder> _rewards = new ArrayList<>();
	private int _rewardsCount = 0;
	
	protected AttendanceRewardData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		if (AttendanceRewardsConfig.ENABLE_ATTENDANCE_REWARDS)
		{
			_rewards.clear();
			parseDatapackFile("data/AttendanceRewards.xml");
			_rewardsCount = _rewards.size();
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _rewardsCount + " rewards.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": Disabled.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "item", rewardNode ->
		{
			// Parse item attributes into StatSet.
			final StatSet set = new StatSet(parseAttributes(rewardNode));
			
			// Extract item ID and count.
			final int itemId = set.getInt("id");
			final int itemCount = set.getInt("count");
			final int highlight = set.getInt("highlight", 0);
			
			// Validate item existence.
			if (ItemData.getInstance().getTemplate(itemId) == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Item with id " + itemId + " does not exist.");
			}
			else // Add valid item to rewards.
			{
				_rewards.add(new AttendanceItemHolder(itemId, itemCount, highlight));
			}
		}));
	}
	
	public List<AttendanceItemHolder> getRewards()
	{
		return _rewards;
	}
	
	public int getRewardsCount()
	{
		return _rewardsCount;
	}
	
	public static AttendanceRewardData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AttendanceRewardData INSTANCE = new AttendanceRewardData();
	}
}
