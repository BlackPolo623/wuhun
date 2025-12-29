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

import org.l2jmobius.gameserver.model.item.enums.UniqueGachaRank;

public class GachaItemTimeStampHolder extends GachaItemHolder
{
	private final long _timeStamp;
	private boolean _storedInDatabase;
	
	public GachaItemTimeStampHolder(int itemId, long itemCount, int enchantLevel, UniqueGachaRank rank, long timeStamp, boolean stored)
	{
		super(itemId, itemCount, 0, enchantLevel, rank);
		_timeStamp = timeStamp;
		_storedInDatabase = stored;
	}
	
	public long getTimeStamp()
	{
		return _timeStamp;
	}
	
	public int getTimeStampFromNow()
	{
		final long timeOfReceive = _timeStamp / 1000L;
		final long currentTime = System.currentTimeMillis() / 1000L;
		return (int) (currentTime - timeOfReceive);
	}
	
	public boolean getStoredStatus()
	{
		if (_storedInDatabase)
		{
			return true;
		}
		
		_storedInDatabase = true;
		return false;
	}
}
