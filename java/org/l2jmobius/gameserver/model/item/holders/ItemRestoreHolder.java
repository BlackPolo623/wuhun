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

public class ItemRestoreHolder
{
	private final int _ownerId;
	private final int _destroyedItemId;
	private int _repairItemId;
	private final short _enchantLevel;
	private final long _destroyDate;
	private boolean _needToStore;
	
	public ItemRestoreHolder(int ownerId, int destroyedItemId, short enchantLevel, long destroyDate, boolean needToStore)
	{
		_ownerId = ownerId;
		_destroyedItemId = destroyedItemId;
		_repairItemId = destroyedItemId;
		_enchantLevel = enchantLevel;
		_destroyDate = destroyDate;
		_needToStore = needToStore;
	}
	
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	public int getDestroyedItemId()
	{
		return _destroyedItemId;
	}
	
	public void setRepairItemId(int itemId)
	{
		_repairItemId = itemId;
	}
	
	public int getRepairItemId()
	{
		return _repairItemId;
	}
	
	public short getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	public long getDestroyDate()
	{
		return _destroyDate;
	}
	
	public boolean needToStore()
	{
		return _needToStore;
	}
	
	public void setNeedToStore(boolean needToStore)
	{
		_needToStore = needToStore;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		
		if ((obj == null) || (getClass() != obj.getClass()))
		{
			return false;
		}
		
		final ItemRestoreHolder other = (ItemRestoreHolder) obj;
		return (_ownerId == other._ownerId) && (_destroyedItemId == other._destroyedItemId) && (_repairItemId == other._repairItemId) && (_enchantLevel == other._enchantLevel) && (_destroyDate == other._destroyDate);
	}
}
