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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;

public class PaybackManager
{
	private static final Logger LOGGER = Logger.getLogger(PaybackManager.class.getName());
	
	private int _coinID;
	private int _minLevel;
	private int _maxLevel;
	private final List<Long> _multisells = new ArrayList<>();
	private long _endTime;
	private final ConcurrentHashMap<Integer, PaybackManagerHolder> _rewards = new ConcurrentHashMap<>();
	private final Map<String, Map<String, String>> _local = new HashMap<>();
	private final Map<Integer, StatSet> _playerProgress = new HashMap<>();
	private static final String GOLDEN_WHEEL_VAR = "GOLDEN_WHEEL_VAR";
	
	protected PaybackManager()
	{
	}
	
	public void init()
	{
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _rewards.size() + " rewards.");
	}
	
	public int getCoinID()
	{
		return _coinID;
	}
	
	public void setCoinID(int coinID)
	{
		_coinID = coinID;
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
		_maxLevel = maxLevel;
	}
	
	public ConcurrentHashMap<Integer, PaybackManagerHolder> getRewards()
	{
		return _rewards;
	}
	
	public PaybackManagerHolder getRewardsById(int id)
	{
		return _rewards.get(id);
	}
	
	public void addRewardsToHolder(int id, long count, List<ItemChanceHolder> rewards)
	{
		_rewards.put(id, new PaybackManagerHolder(rewards, count));
	}
	
	public List<Long> getMultisells()
	{
		return _multisells;
	}
	
	public void addToMultisells(List<Long> ids)
	{
		_multisells.addAll(ids);
	}
	
	public long getEndTime()
	{
		return _endTime;
	}
	
	public void setEndTime(long endTime)
	{
		_endTime = endTime;
	}
	
	public void resetField()
	{
		_coinID = 94834;
		_minLevel = 1;
		_maxLevel = -1;
		_multisells.clear();
		_rewards.clear();
	}
	
	public void addLocalString(String lang, String type, String message)
	{
		_local.putIfAbsent(lang, null);
		final Map<String, String> strings = _local.get(lang) == null ? new HashMap<>() : _local.get(lang);
		strings.put(type, message);
		_local.replace(lang, strings);
	}
	
	public Map<String, String> getLocalString(String lang)
	{
		if (!_local.isEmpty() && !_local.containsKey(lang))
		{
			return null;
		}
		
		return _local.get(lang);
	}
	
	public long getPlayerConsumedProgress(int objectID)
	{
		return _playerProgress.getOrDefault(objectID, null).getInt("CONSUMED_COINS");
	}
	
	public void changePlayerConsumedProgress(int objectID, long newValue)
	{
		if (_playerProgress.getOrDefault(objectID, null) == null)
		{
			return;
		}
		
		final StatSet set = _playerProgress.getOrDefault(objectID, null);
		set.remove("CONSUMED_COINS");
		set.set("CONSUMED_COINS", newValue);
		_playerProgress.replace(objectID, set);
	}
	
	public List<Integer> getPlayerMissionProgress(int objectID)
	{
		return _playerProgress.get(objectID).getIntegerList("MISSION_PROGRESS");
	}
	
	public void changeMissionProgress(int objectID, int missionID, int status)
	{
		if (_playerProgress.getOrDefault(objectID, null) == null)
		{
			return;
		}
		
		final List<Integer> currentProgress = _playerProgress.get(objectID).getIntegerList("MISSION_PROGRESS");
		currentProgress.set(missionID, status);
		_playerProgress.get(objectID).setIntegerList("MISSION_PROGRESS", currentProgress);
	}
	
	public void storePlayerProgress(Player player)
	{
		player.getVariables().set(GOLDEN_WHEEL_VAR, getStringVariable(_playerProgress.get(player.getObjectId())));
	}
	
	private String getStringVariable(StatSet progress)
	{
		final StringBuilder returnString = new StringBuilder();
		returnString.append("MISSION_PROGRESS").append("=").append("[").append(progress.getString("MISSION_PROGRESS")).append("]");
		returnString.append(":");
		returnString.append("CONSUMED_COINS").append("=").append(progress.getLong("CONSUMED_COINS"));
		return returnString.toString();
	}
	
	private StatSet getStatSetVariable(String variable)
	{
		final String[] splitsVariable = variable.split(":");
		final List<Integer> missionProgress = new ArrayList<>();
		for (String temp : splitsVariable[0].split("=")[1].split(","))
		{
			missionProgress.add(Integer.parseInt(temp.replace("[", "").replace("]", "").replace(" ", "")));
		}
		
		final Long consumed = Long.parseLong(splitsVariable[1].split("=")[1]);
		StatSet returnSet = new StatSet();
		returnSet.set("CONSUMED_COINS", consumed);
		returnSet.setIntegerList("MISSION_PROGRESS", missionProgress);
		return returnSet;
	}
	
	public void addPlayerToList(Player player)
	{
		final String variable = player.getVariables().getString(GOLDEN_WHEEL_VAR, null);
		final StatSet progress;
		if (variable == null)
		{
			progress = new StatSet();
			progress.set("CONSUMED_COINS", 0);
			List<Integer> temp = new ArrayList<>();
			_rewards.keySet().forEach(_ -> temp.add(0));
			progress.setIntegerList("MISSION_PROGRESS", temp);
		}
		else
		{
			progress = getStatSetVariable(variable);
		}
		
		_playerProgress.put(player.getObjectId(), progress);
	}
	
	public void removePlayerFromList(int objectID)
	{
		_playerProgress.remove(objectID);
	}
	
	public static class PaybackManagerHolder
	{
		final List<ItemChanceHolder> _rewards;
		final long _count;
		
		public PaybackManagerHolder(List<ItemChanceHolder> rewards, long count)
		{
			_rewards = rewards;
			_count = count;
		}
		
		public List<ItemChanceHolder> getRewards()
		{
			return _rewards;
		}
		
		public long getCount()
		{
			return _count;
		}
	}
	
	public static PaybackManager getInstance()
	{
		return PaybackManager.SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PaybackManager INSTANCE = new PaybackManager();
	}
}
