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
import org.l2jmobius.gameserver.config.HuntPassConfig;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Serenitty
 */
public class HuntPassData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(HuntPassData.class.getName());
	
	private final List<ItemHolder> _rewards = new ArrayList<>();
	private final List<ItemHolder> _premiumRewards = new ArrayList<>();
	private int _rewardCount = 0;
	private int _premiumRewardCount = 0;
	
	protected HuntPassData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		if (HuntPassConfig.ENABLE_HUNT_PASS)
		{
			_rewards.clear();
			parseDatapackFile("data/HuntPass.xml");
			_rewardCount = _rewards.size();
			_premiumRewardCount = _premiumRewards.size();
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _rewardCount + " HuntPass rewards.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": Disabled.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		// Traverse "list" elements and process each "item" node.
		forEach(document, "list", listNode -> forEach(listNode, "item", rewardNode ->
		{
			// Parse attributes of the "item" element into a StatSet.
			final StatSet set = new StatSet(parseAttributes(rewardNode));
			final int itemId = set.getInt("id");
			final int itemCount = set.getInt("count");
			final int premiumItemId = set.getInt("premiumId");
			final int premiumItemCount = set.getInt("premiumCount");
			
			// Check if item exists, log if not found, and add to rewards lists if found.
			if (ItemData.getInstance().getTemplate(itemId) == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Item with id " + itemId + " does not exist.");
			}
			else
			{
				_rewards.add(new ItemHolder(itemId, itemCount));
				_premiumRewards.add(new ItemHolder(premiumItemId, premiumItemCount));
			}
		}));
	}
	
	/**
	 * Retrieves the list of standard rewards.
	 * @return a {@link List} of {@link ItemHolder} objects representing the standard rewards
	 */
	public List<ItemHolder> getRewards()
	{
		return _rewards;
	}
	
	/**
	 * Retrieves the total count of standard rewards.
	 * @return an {@code int} representing the number of standard rewards
	 */
	public int getRewardsCount()
	{
		return _rewardCount;
	}
	
	/**
	 * Retrieves the list of premium rewards.
	 * @return a {@link List} of {@link ItemHolder} objects representing the premium rewards
	 */
	public List<ItemHolder> getPremiumRewards()
	{
		return _premiumRewards;
	}
	
	/**
	 * Retrieves the total count of premium rewards.
	 * @return an {@code int} representing the number of premium rewards
	 */
	public int getPremiumRewardsCount()
	{
		return _premiumRewardCount;
	}
	
	public static HuntPassData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HuntPassData INSTANCE = new HuntPassData();
	}
}
