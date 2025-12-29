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
package org.l2jmobius.gameserver.data.holders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.data.enums.AdenLabGameType;

/**
 * @author SaltyMike
 */
public class AdenLabHolder
{
	private AdenLabGameType _gameType;
	private byte _bossId;
	private byte _pageIndex;
	private byte _cardCount;
	private float _gameSuccessRate; // Needs to be changed to `DOUBLE` if there are more than 7 digits after the decimal point.
	private final Map<Byte, Map<Byte, List<AdenLabStageHolder>>> _options = new HashMap<>(); // <optionIndex, <stageLevel, <holdersList>>>
	
	public AdenLabHolder()
	{
	}
	
	public void setBossId(byte bossId)
	{
		_bossId = bossId;
	}
	
	public void setGameType(AdenLabGameType gameType)
	{
		_gameType = gameType;
	}
	
	public void setPageIndex(byte pageIndex)
	{
		_pageIndex = pageIndex < 0 ? 0 : pageIndex;
	}
	
	public void setGameSuccessRate(float value)
	{
		_gameSuccessRate = value < 0 ? 0f : value;
	}
	
	public void setCardCount(byte cardCount)
	{
		_cardCount = cardCount < 0 ? 0 : cardCount;
	}
	
	public void addStage(byte optionIndex, byte stageLevel, AdenLabStageHolder stageHolder)
	{
		// Get or create the inner stage level map.
		final Map<Byte, List<AdenLabStageHolder>> levelMap = _options.computeIfAbsent(optionIndex, k -> new HashMap<>());
		
		// Get or create the list of stage holders.
		levelMap.computeIfAbsent(stageLevel, k -> new ArrayList<>()).add(stageHolder);
	}
	
	public void addStages(byte optionIndex, byte stageLevel, List<AdenLabStageHolder> stageMap)
	{
		// Get or initialize the optionIndex map.
		final Map<Byte, List<AdenLabStageHolder>> levelMap = _options.computeIfAbsent(optionIndex, k -> new HashMap<>());
		
		// Add or merge the stage list.
		levelMap.computeIfAbsent(stageLevel, k -> new ArrayList<>()).addAll(stageMap);
	}
	
	// getters
	public byte getBossId()
	{
		return _bossId;
	}
	
	public AdenLabGameType getGameType()
	{
		return _gameType;
	}
	
	public int getPageIndex()
	{
		return _pageIndex;
	}
	
	public float getGameSuccessRate()
	{
		return _gameSuccessRate;
	}
	
	public byte getCardCount()
	{
		return _cardCount;
	}
	
	/**
	 * @return [optionIndex, {stageLevel, HoldersList}]
	 */
	public Map<Byte, Map<Byte, List<AdenLabStageHolder>>> getOptions()
	{
		return _options;
	}
	
	public List<AdenLabStageHolder> getStageHolderListByLevel(byte optionIndex, int stageLevel)
	{
		// Get or initialize the map for the given optionIndex.
		final Map<Byte, List<AdenLabStageHolder>> stageMap = _options.computeIfAbsent(optionIndex, k -> new HashMap<>());
		
		// Get or initialize the list for the given stageLevel.
		return stageMap.computeIfAbsent((byte) stageLevel, k -> new ArrayList<>());
	}
}
