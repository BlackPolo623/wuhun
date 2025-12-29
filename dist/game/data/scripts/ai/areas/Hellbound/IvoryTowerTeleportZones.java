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
package ai.areas.Hellbound;

import java.util.Calendar;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.zone.ZoneType;

/**
 * @author Mobius
 */
public class IvoryTowerTeleportZones extends Script
{
	private static final int HOURS_24 = 86400000;
	
	private static final String[] ZONE_NAMES =
	{
		"hellbound_tp_1",
		"hellbound_tp_2",
		"hellbound_tp_3",
		"hellbound_tp_4"
	};
	
	private IvoryTowerTeleportZones()
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		// Current day check.
		if ((calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) && (calendar.get(Calendar.HOUR_OF_DAY) >= 10))
		{
			enableZones();
			calendar.add(Calendar.DAY_OF_WEEK, 7);
			calendar.set(Calendar.HOUR, 10);
			closeZones();
		}
		else
		{
			disableZones();
			calendar.add(Calendar.DAY_OF_WEEK, Calendar.SATURDAY - calendar.get(Calendar.DAY_OF_WEEK));
			calendar.set(Calendar.HOUR, 10);
		}
		
		// Schedule task to check if it is Saturday.
		ThreadPool.scheduleAtFixedRate(this::checkCondition, calendar.getTimeInMillis() - System.currentTimeMillis(), HOURS_24 * 7); // Check every week.
	}
	
	private void checkCondition()
	{
		final Calendar calendar = Calendar.getInstance();
		if ((calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) && (calendar.get(Calendar.HOUR_OF_DAY) >= 10))
		{
			enableZones();
			closeZones();
		}
	}
	
	private void closeZones()
	{
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_WEEK, 1);
		calendar.set(Calendar.HOUR, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		ThreadPool.schedule(this::disableZones, calendar.getTimeInMillis() - System.currentTimeMillis());
	}
	
	private void enableZones()
	{
		for (String name : ZONE_NAMES)
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByName(name);
			if (zone != null)
			{
				zone.setEnabled(true);
			}
		}
	}
	
	private void disableZones()
	{
		for (String name : ZONE_NAMES)
		{
			final ZoneType zone = ZoneManager.getInstance().getZoneByName(name);
			if (zone != null)
			{
				zone.setEnabled(false);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new IvoryTowerTeleportZones();
	}
}
