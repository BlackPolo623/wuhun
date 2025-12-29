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

import java.util.List;

import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;

/**
 * @author Magik
 */
public class NewQuestCondition
{
	private final int _minLevel;
	private final int _maxLevel;
	private final List<Integer> _previousQuestIds;
	private final List<PlayerClass> _allowedClassIds;
	private final boolean _oneOfPreQuests;
	private final boolean _specificStart;
	
	public NewQuestCondition(int minLevel, int maxLevel, List<Integer> previousQuestIds, List<PlayerClass> allowedClassIds, boolean oneOfPreQuests, boolean specificStart)
	{
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_previousQuestIds = previousQuestIds;
		_allowedClassIds = allowedClassIds;
		_oneOfPreQuests = oneOfPreQuests;
		_specificStart = specificStart;
	}
	
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	public List<Integer> getPreviousQuestIds()
	{
		return _previousQuestIds;
	}
	
	public List<PlayerClass> getAllowedClassIds()
	{
		return _allowedClassIds;
	}
	
	public boolean getOneOfPreQuests()
	{
		return _oneOfPreQuests;
	}
	
	public boolean getSpecificStart()
	{
		return _specificStart;
	}
}
