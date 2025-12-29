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
package org.l2jmobius.gameserver.model.commission;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.gameserver.model.ItemInfo;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * @author NosBit, Ren
 */
public class CommissionItem
{
	private final long _commissionId;
	private final Item _itemInstance;
	private final ItemInfo _itemInfo;
	private final long _pricePerUnit;
	private final Instant _startTime;
	private final byte _durationInDays;
	private final byte _discountInPercentage;
	private ScheduledFuture<?> _saleEndTask;
	
	public CommissionItem(long commissionId, Item itemInstance, long pricePerUnit, Instant startTime, byte durationInDays, byte discountInPercentage)
	{
		_commissionId = commissionId;
		_itemInstance = itemInstance;
		_itemInfo = new ItemInfo(_itemInstance);
		_pricePerUnit = pricePerUnit;
		_startTime = startTime;
		_durationInDays = durationInDays;
		_discountInPercentage = discountInPercentage;
	}
	
	/**
	 * Gets the commission id.
	 * @return the commission id
	 */
	public long getCommissionId()
	{
		return _commissionId;
	}
	
	/**
	 * Gets the item instance.
	 * @return the item instance
	 */
	public Item getItemInstance()
	{
		return _itemInstance;
	}
	
	/**
	 * Gets the item info.
	 * @return the item info
	 */
	public ItemInfo getItemInfo()
	{
		return _itemInfo;
	}
	
	/**
	 * Gets the price per unit.
	 * @return the price per unit
	 */
	public long getPricePerUnit()
	{
		return _pricePerUnit;
	}
	
	/**
	 * Gets the start time.
	 * @return the start time
	 */
	public Instant getStartTime()
	{
		return _startTime;
	}
	
	/**
	 * Gets the duration in days.
	 * @return the duration in days
	 */
	public byte getDurationInDays()
	{
		return _durationInDays;
	}
	
	/**
	 * Gets the discount in percentage
	 * @return the _discountInPercentage
	 */
	public byte getDiscountInPercentage()
	{
		return _discountInPercentage;
	}
	
	/**
	 * Gets the end time.
	 * @return the end time
	 */
	public Instant getEndTime()
	{
		return _startTime.plus(_durationInDays, ChronoUnit.DAYS);
	}
	
	/**
	 * Gets the sale end task.
	 * @return the sale end task
	 */
	public ScheduledFuture<?> getSaleEndTask()
	{
		return _saleEndTask;
	}
	
	/**
	 * Sets the sale end task.
	 * @param saleEndTask the sale end task
	 */
	public void setSaleEndTask(ScheduledFuture<?> saleEndTask)
	{
		_saleEndTask = saleEndTask;
	}
}
