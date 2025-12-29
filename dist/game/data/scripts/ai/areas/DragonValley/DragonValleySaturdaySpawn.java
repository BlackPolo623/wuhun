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
package ai.areas.DragonValley;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.model.script.Script;

public class DragonValleySaturdaySpawn extends Script
{
	private boolean _isScheduling = false;
	
	private DragonValleySaturdaySpawn()
	{
		scheduleDragonValleySpawns();
	}
	
	private void changeDragonValleySpawns(boolean spawnDragons)
	{
		SpawnData.getInstance().getSpawns().stream().filter(spawnTemplate -> (spawnTemplate.getName() != null) && spawnTemplate.getName().equals("Dragon Valley No Antharas")).forEach(spawnTemplate -> spawnTemplate.getGroups().forEach(holder -> holder.despawnAll()));
		SpawnData.getInstance().getSpawns().stream().filter(spawnTemplate -> (spawnTemplate.getName() != null) && spawnTemplate.getName().equals("Dragon Valley Antharas")).forEach(spawnTemplate -> spawnTemplate.getGroups().forEach(holder -> holder.despawnAll()));
		if (spawnDragons)
		{
			SpawnData.getInstance().getSpawns().stream().filter(spawnTemplate -> (spawnTemplate.getName() != null) && spawnTemplate.getName().equals("Dragon Valley Antharas")).forEach(spawnTemplate -> spawnTemplate.getGroups().forEach(holder -> holder.spawnAll()));
		}
		else
		{
			SpawnData.getInstance().getSpawns().stream().filter(spawnTemplate -> (spawnTemplate.getName() != null) && spawnTemplate.getName().equals("Dragon Valley No Antharas")).forEach(spawnTemplate -> spawnTemplate.getGroups().forEach(holder -> holder.spawnAll()));
		}
	}
	
	private void scheduleDragonValleySpawns()
	{
		if (_isScheduling)
		{
			return;
		}
		
		_isScheduling = true;
		
		final long currentMillis = System.currentTimeMillis();
		final Calendar saturday = Calendar.getInstance();
		saturday.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		saturday.set(Calendar.HOUR_OF_DAY, 0);
		saturday.set(Calendar.MINUTE, 0);
		saturday.set(Calendar.SECOND, 0);
		saturday.set(Calendar.MILLISECOND, 0);
		
		final Calendar today = Calendar.getInstance();
		if ((saturday.getTimeInMillis() <= System.currentTimeMillis()) && (today.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)) // Is Saturday (and not Sunday).
		{
			changeDragonValleySpawns(true);
			
			final LocalDateTime start = LocalDateTime.now();
			final LocalDateTime end = start.plusDays(1).truncatedTo(ChronoUnit.DAYS);
			final long millis = Duration.between(start, end).toMillis() + 10000; // Sunday +10 sec to make sure it does not happen at 23:59 saturday.
			ThreadPool.schedule(() ->
			{
				scheduleDragonValleySpawns();
				_isScheduling = false;
			}, millis);
		}
		else // Not saturday.
		{
			changeDragonValleySpawns(false);
			ThreadPool.schedule(() ->
			{
				scheduleDragonValleySpawns();
				_isScheduling = false;
			}, Math.max(0, (saturday.getTimeInMillis() - currentMillis) + 10000)); // Time remaining till saturday (10s delay just in case?).
		}
	}
	
	public static void main(String[] args)
	{
		new DragonValleySaturdaySpawn();
	}
}
