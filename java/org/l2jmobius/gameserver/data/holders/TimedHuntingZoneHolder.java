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

import org.l2jmobius.gameserver.model.Location;

/**
 * @author Mobius
 */
public class TimedHuntingZoneHolder
{
	private final int _id;
	private final String _name;
	private final int _initialTime;
	private final int _maximumAddedTime;
	private final int _resetDelay;
	private final int _entryItemId;
	private final int _entryFee;
	private final int _minLevel;
	private final int _maxLevel;
	private final int _remainRefillTime;
	private final int _refillTimeMax;
	private final boolean _pvpZone;
	private final boolean _noPvpZone;
	private final int _instanceId;
	private final boolean _soloInstance;
	private final boolean _weekly;
	private final boolean _useWorldPrefix;
	private final boolean _zonePremiumUserOnly;
	private final Location _enterLocation;
	private final Location _exitLocation;
	private final boolean _isEvenWeek;
	private final boolean _isSwapWeek;
	
	public TimedHuntingZoneHolder(int id, String name, int initialTime, int maximumAddedTime, int resetDelay, int entryItemId, int entryFee, int minLevel, int maxLevel, int remainRefillTime, int refillTimeMax, boolean pvpZone, boolean noPvpZone, int instanceId, boolean soloInstance, boolean weekly, boolean useWorldPrefix, boolean zonePremiumUserOnly, Location enterLocation, Location exitLocation, boolean isEvenWeek, boolean isSwapWeek)
	{
		_id = id;
		_name = name;
		_initialTime = initialTime;
		_maximumAddedTime = maximumAddedTime;
		_resetDelay = resetDelay;
		_entryItemId = entryItemId;
		_entryFee = entryFee;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_remainRefillTime = remainRefillTime;
		_refillTimeMax = refillTimeMax;
		_pvpZone = pvpZone;
		_noPvpZone = noPvpZone;
		_instanceId = instanceId;
		_soloInstance = soloInstance;
		_weekly = weekly;
		_useWorldPrefix = useWorldPrefix;
		_zonePremiumUserOnly = zonePremiumUserOnly;
		_enterLocation = enterLocation;
		_exitLocation = exitLocation;
		_isEvenWeek = isEvenWeek;
		_isSwapWeek = isSwapWeek;
	}
	
	public int getZoneId()
	{
		return _id;
	}
	
	public String getZoneName()
	{
		return _name;
	}
	
	public int getInitialTime()
	{
		return _initialTime;
	}
	
	public int getMaximumAddedTime()
	{
		return _maximumAddedTime;
	}
	
	public int getResetDelay()
	{
		return _resetDelay;
	}
	
	public int getEntryItemId()
	{
		return _entryItemId;
	}
	
	public int getEntryFee()
	{
		return _entryFee;
	}
	
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	public int getRemainRefillTime()
	{
		return _remainRefillTime;
	}
	
	public int getRefillTimeMax()
	{
		return _refillTimeMax;
	}
	
	public boolean isPvpZone()
	{
		return _pvpZone;
	}
	
	public boolean isNoPvpZone()
	{
		return _noPvpZone;
	}
	
	public int getInstanceId()
	{
		return _instanceId;
	}
	
	public boolean isSoloInstance()
	{
		return _soloInstance;
	}
	
	public boolean isWeekly()
	{
		return _weekly;
	}
	
	public boolean useWorldPrefix()
	{
		return _useWorldPrefix;
	}
	
	public boolean zonePremiumUserOnly()
	{
		return _zonePremiumUserOnly;
	}
	
	public Location getEnterLocation()
	{
		return _enterLocation;
	}
	
	public Location getExitLocation()
	{
		return _exitLocation;
	}
	
	public boolean isEvenWeek()
	{
		return _isEvenWeek;
	}
	
	public boolean isSwapWeek()
	{
		return _isSwapWeek;
	}
}
