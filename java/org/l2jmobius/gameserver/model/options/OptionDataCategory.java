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
package org.l2jmobius.gameserver.model.options;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;

/**
 * @author Pere, Mobius
 */
public class OptionDataCategory
{
	private final Map<Options, Double> _options;
	private final Set<Integer> _itemIds;
	private final double _chance;
	private final boolean _isEmpty;
	
	public OptionDataCategory(Map<Options, Double> options, Set<Integer> itemIds, double chance)
	{
		_options = options;
		_itemIds = itemIds;
		_chance = chance;
		_isEmpty = ((options == null) || options.isEmpty());
	}
	
	public boolean isEmptyCategory()
	{
		return _isEmpty;
	}
	
	Options getRandomOptions()
	{
		if (_isEmpty)
		{
			return null;
		}
		
		double random = Rnd.nextDouble() * 100.0;
		for (Entry<Options, Double> entry : _options.entrySet())
		{
			if (entry.getValue() >= random)
			{
				return entry.getKey();
			}
			random -= entry.getValue();
		}
		return null;
	}
	
	public Map<Options, Double> getOptions()
	{
		return _options;
	}
	
	public Set<Integer> getItemIds()
	{
		return _itemIds;
	}
	
	public double getChance()
	{
		return _chance;
	}
}
