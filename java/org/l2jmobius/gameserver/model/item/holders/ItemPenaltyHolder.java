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
package org.l2jmobius.gameserver.model.item.holders;

import java.util.Date;

public class ItemPenaltyHolder
{
	private final int _itemObjectId;
	private final int _killerObjectId;
	private final Date _dateLost;
	
	public ItemPenaltyHolder(int itemObjectId, int killerObjectId, Date dateLost)
	{
		_itemObjectId = itemObjectId;
		_killerObjectId = killerObjectId;
		_dateLost = dateLost;
	}
	
	public int getItemObjectId()
	{
		return _itemObjectId;
	}
	
	public int getKillerObjectId()
	{
		return _killerObjectId;
	}
	
	public Date getDateLost()
	{
		return _dateLost;
	}
}
