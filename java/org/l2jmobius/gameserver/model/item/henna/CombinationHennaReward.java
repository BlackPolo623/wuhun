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

import org.l2jmobius.gameserver.model.item.combination.CombinationItemType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Index
 */
public class CombinationHennaReward extends ItemHolder
{
	private final int _hennaId;
	private final CombinationItemType _type;
	
	public CombinationHennaReward(int id, int count, CombinationItemType type)
	{
		super(id, count);
		_hennaId = 0;
		_type = type;
	}
	
	public CombinationHennaReward(int hennaId, int id, int count, CombinationItemType type)
	{
		super(id, count);
		_hennaId = hennaId;
		_type = type;
	}
	
	public int getHennaId()
	{
		return _hennaId;
	}
	
	public CombinationItemType getType()
	{
		return _type;
	}
}
