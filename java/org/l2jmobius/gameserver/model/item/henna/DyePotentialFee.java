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
package org.l2jmobius.gameserver.model.item.henna;

import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Serenitty
 */
public class DyePotentialFee
{
	private final int _step;
	private final List<ItemHolder> _items;
	private final int _dailyCount;
	private final Map<Integer, Double> _enchantExp;
	private final long _adenaFee;
	
	public DyePotentialFee(int step, List<ItemHolder> items, long adenaFee, int dailyCount, Map<Integer, Double> enchantExp)
	{
		_step = step;
		_items = items;
		_adenaFee = adenaFee;
		_dailyCount = dailyCount;
		_enchantExp = enchantExp;
	}
	
	public int getStep()
	{
		return _step;
	}
	
	public List<ItemHolder> getItems()
	{
		return _items;
	}
	
	public long getAdenaFee()
	{
		return _adenaFee;
	}
	
	public int getDailyCount()
	{
		return _dailyCount;
	}
	
	public Map<Integer, Double> getEnchantExp()
	{
		return _enchantExp;
	}
}
