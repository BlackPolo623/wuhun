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

import java.util.Collection;
import java.util.List;

/**
 * @author CostyKiller
 */
public class RelicCollectionDataHolder
{
	private final int _relicCollectionId;
	private final int _optionId;
	private final int _category;
	private final int _completeCount;
	private final int _combatPower;
	private final List<RelicCollectionInfoHolder> _relics;
	
	public RelicCollectionDataHolder(int relicCollectionId, int optionId, int category, int completeCount, int combatPower, List<RelicCollectionInfoHolder> relics)
	{
		_relicCollectionId = relicCollectionId;
		_optionId = optionId;
		_category = category;
		_completeCount = completeCount;
		_combatPower = combatPower;
		_relics = relics;
	}
	
	public int getCollectionId()
	{
		return _relicCollectionId;
	}
	
	public int getOptionId()
	{
		return _optionId;
	}
	
	public int getCategory()
	{
		return _category;
	}
	
	public int getCompleteCount()
	{
		return _completeCount;
	}
	
	public int getCombatPower()
	{
		return _combatPower;
	}
	
	public Collection<RelicCollectionInfoHolder> getRelics()
	{
		return _relics;
	}
	
	public RelicCollectionInfoHolder getRelic(int index)
	{
		return _relics.get(index);
	}
}
