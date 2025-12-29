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

import org.l2jmobius.gameserver.model.ItemInfo;
import org.l2jmobius.gameserver.model.item.enums.WorldExchangeItemStatusType;
import org.l2jmobius.gameserver.model.item.enums.WorldExchangeItemSubType;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * @author Index
 */
public class WorldExchangeHolder
{
	private final long _worldExchangeId;
	private final Item _itemInstance;
	private final ItemInfo _itemInfo;
	private final long _price;
	private final int _oldOwnerId;
	private WorldExchangeItemStatusType _storeType;
	private final WorldExchangeItemSubType _category;
	private final long _startTime;
	private long _endTime;
	private boolean _hasChanges;
	
	public WorldExchangeHolder(long worldExchangeId, Item itemInstance, ItemInfo itemInfo, long price, int oldOwnerId, WorldExchangeItemStatusType storeType, WorldExchangeItemSubType category, long startTime, long endTime, boolean hasChanges)
	{
		_worldExchangeId = worldExchangeId;
		_itemInstance = itemInstance;
		_itemInfo = itemInfo;
		_price = price;
		_oldOwnerId = oldOwnerId;
		_storeType = storeType;
		_category = category;
		_startTime = startTime;
		_endTime = endTime;
		_hasChanges = hasChanges;
	}
	
	public long getWorldExchangeId()
	{
		return _worldExchangeId;
	}
	
	public Item getItemInstance()
	{
		return _itemInstance;
	}
	
	public ItemInfo getItemInfo()
	{
		return _itemInfo;
	}
	
	public long getPrice()
	{
		return _price;
	}
	
	public int getOldOwnerId()
	{
		return _oldOwnerId;
	}
	
	public WorldExchangeItemStatusType getStoreType()
	{
		return _storeType;
	}
	
	public void setStoreType(WorldExchangeItemStatusType storeType)
	{
		_storeType = storeType;
	}
	
	public WorldExchangeItemSubType getCategory()
	{
		return _category;
	}
	
	public long getStartTime()
	{
		return _startTime;
	}
	
	public long getEndTime()
	{
		return _endTime;
	}
	
	public void setEndTime(long endTime)
	{
		_endTime = endTime;
	}
	
	public boolean hasChanges()
	{
		if (_hasChanges) // TODO: Fix logic.
		{
			_hasChanges = false;
			return true;
		}
		
		return false;
	}
	
	public void setHasChanges(boolean hasChanges)
	{
		_hasChanges = hasChanges;
	}
}
