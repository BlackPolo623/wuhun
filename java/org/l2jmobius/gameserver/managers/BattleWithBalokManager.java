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
package org.l2jmobius.gameserver.managers;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author Serenitty, Index
 */
public class BattleWithBalokManager
{
	private final Map<Integer, Integer> _playerPoints = new ConcurrentHashMap<>();
	private boolean _inBattle = false;
	private int _reward = 0;
	private int _globalPoints = 0;
	private int _globalStage = 0;
	private int _globalStatus = 0;
	
	public BattleWithBalokManager()
	{
	}
	
	public void addPointsForPlayer(Player player, boolean isScorpion)
	{
		final int pointsToAdd = isScorpion ? GrandBossConfig.BALOK_POINTS_PER_MONSTER * 10 : GrandBossConfig.BALOK_POINTS_PER_MONSTER;
		final int currentPoints = _playerPoints.computeIfAbsent(player.getObjectId(), _ -> 0);
		int sum = pointsToAdd + currentPoints;
		_playerPoints.put(player.getObjectId(), sum);
	}
	
	public Map<Integer, Integer> getTopPlayers(int count)
	{
		return _playerPoints.entrySet().stream().sorted(Entry.comparingByValue(Comparator.reverseOrder())).limit(count).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, _) -> e1, LinkedHashMap::new));
	}
	
	public int getPlayerRank(Player player)
	{
		if (!_playerPoints.containsKey(player.getObjectId()))
		{
			return 0;
		}
		
		final Map<Integer, Integer> sorted = _playerPoints.entrySet().stream().sorted(Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, _) -> e1, LinkedHashMap::new));
		return sorted.keySet().stream().toList().indexOf(player.getObjectId()) + 1;
	}
	
	public int getMonsterPoints(Player player)
	{
		return _playerPoints.computeIfAbsent(player.getObjectId(), _ -> 0);
	}
	
	public int getReward()
	{
		return _reward;
	}
	
	public void setReward(int value)
	{
		_reward = value;
	}
	
	public boolean getInBattle()
	{
		return _inBattle;
	}
	
	public void setInBattle(boolean value)
	{
		_inBattle = value;
	}
	
	public int getGlobalPoints()
	{
		return _globalPoints;
	}
	
	public void setGlobalPoints(int value)
	{
		_globalPoints = value;
	}
	
	public int getGlobalStage()
	{
		return _globalStage;
	}
	
	public void setGlobalStage(int value)
	{
		_globalStage = value;
	}
	
	public int getGlobalStatus()
	{
		return _globalStatus;
	}
	
	public void setGlobalStatus(int value)
	{
		_globalStatus = value;
	}
	
	public static BattleWithBalokManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BattleWithBalokManager INSTANCE = new BattleWithBalokManager();
	}
}
