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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.CrossEventAdvancedRewardHolder;
import org.l2jmobius.gameserver.model.actor.holders.player.CrossEventHolder;
import org.l2jmobius.gameserver.model.actor.holders.player.CrossEventRegularRewardHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.crossevent.ExCrossEventInfo;

/**
 * @author Smoto
 */
public class CrossEventManager
{
	private final List<CrossEventRegularRewardHolder> _regularRewards = new ArrayList<>();
	private final List<CrossEventAdvancedRewardHolder> _advancedRewards = new ArrayList<>();
	private final Map<String, List<Integer>> _rewardsAvailable = new ConcurrentHashMap<>();
	private int _ticketId = 72458; // Spring Flower Coupon.
	private int _displayId = 1;
	private int _endTime;
	private int _dailyReset;
	private int _resetCostAmount;
	
	public void setTicketId(int ticketId)
	{
		_ticketId = ticketId;
	}
	
	public int getTicketId()
	{
		return _ticketId;
	}
	
	public void setDisplayId(int displayId)
	{
		_displayId = displayId;
	}
	
	public int getDisplayId()
	{
		return _displayId;
	}
	
	public void setEndTime(int endTime)
	{
		_endTime = endTime;
	}
	
	public int getEndTime()
	{
		return _endTime;
	}
	
	public List<CrossEventRegularRewardHolder> getRegularRewardsList()
	{
		return _regularRewards;
	}
	
	public List<CrossEventAdvancedRewardHolder> getAdvancedRewardList()
	{
		return _advancedRewards;
	}
	
	public void setDailyResets(int dailyReset)
	{
		_dailyReset = dailyReset;
	}
	
	public int getDailyResets()
	{
		return _dailyReset;
	}
	
	public void setResetCostAmount(int resetCostAmount)
	{
		_resetCostAmount = resetCostAmount;
	}
	
	public int getResetCostAmount()
	{
		return _resetCostAmount;
	}
	
	public int getGameTickets(Player player)
	{
		final Item item = player.getInventory().getItemByItemId(_ticketId);
		if (item != null)
		{
			return (int) item.getCount();
		}
		
		return 0;
	}
	
	public List<Integer> getPlayerRewardsAvailable(Player player)
	{
		final String accountName = player.getAccountName();
		if (!_rewardsAvailable.containsKey(accountName))
		{
			return Collections.emptyList();
		}
		
		return _rewardsAvailable.get(accountName);
	}
	
	public void addRewardsAvailable(Player player, int reward)
	{
		final String accountName = player.getAccountName();
		final List<Integer> rewards;
		if (_rewardsAvailable.containsKey(accountName))
		{
			rewards = _rewardsAvailable.get(accountName);
		}
		else
		{
			rewards = new ArrayList<>();
			_rewardsAvailable.put(player.getAccountName(), rewards);
		}
		
		rewards.add(reward);
	}
	
	public void checkAdvancedRewardAvailable(Player player)
	{
		final List<CrossEventHolder> cells = player.getCrossEventCells();
		if (!rewardExist(player, 1) && (cells.stream().filter(c -> c.getHorizontal() == 0).count() == 4))
		{
			addRewardsAvailable(player, 1);
			player.setCrossAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 2) && (cells.stream().filter(c -> c.getHorizontal() == 1).count() == 4))
		{
			addRewardsAvailable(player, 2);
			player.setCrossAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 3) && (cells.stream().filter(c -> c.getHorizontal() == 2).count() == 4))
		{
			addRewardsAvailable(player, 3);
			player.setCrossAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 4) && (cells.stream().filter(c -> c.getHorizontal() == 3).count() == 4))
		{
			addRewardsAvailable(player, 4);
			player.setCrossAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 5) && (cells.stream().filter(c -> c.getVertical() == 0).count() == 4))
		{
			addRewardsAvailable(player, 5);
			player.setCrossAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 6) && (cells.stream().filter(c -> c.getVertical() == 1).count() == 4))
		{
			addRewardsAvailable(player, 6);
			player.setCrossAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 7) && (cells.stream().filter(c -> c.getVertical() == 2).count() == 4))
		{
			addRewardsAvailable(player, 7);
			player.setCrossAdvancedRewardCount(1);
		}
		
		if (!rewardExist(player, 8) && (cells.stream().filter(c -> c.getVertical() == 3).count() == 4))
		{
			addRewardsAvailable(player, 8);
			player.setCrossAdvancedRewardCount(1);
		}
		
		player.sendPacket(new ExCrossEventInfo(player));
	}
	
	private boolean rewardExist(Player player, int reward)
	{
		final String accountName = player.getAccountName();
		if (!_rewardsAvailable.containsKey(accountName))
		{
			return false;
		}
		
		final List<Integer> list = _rewardsAvailable.get(accountName);
		return list.contains(reward);
	}
	
	public void resetAdvancedRewards(Player player)
	{
		_rewardsAvailable.remove(player.getAccountName());
	}
	
	public static CrossEventManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CrossEventManager INSTANCE = new CrossEventManager();
	}
}
