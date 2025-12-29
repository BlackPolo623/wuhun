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
import java.util.concurrent.Future;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author Norvox
 */
public class Ryuminir extends Script
{
	// NPC
	private static final int RYUMINIR = 25936;
	
	// Locations
	private static final Location[] SPAWNS =
	{
		new Location(20970, 256781, -1350),
		new Location(21537, 251284, -1458),
	};
	
	// Misc
	private static final int ALIVE_MILLISECONDS = 3600000;
	private static final String RYUMINIR_RESPAWN_PATTERN = "0 12 * * 6";
	private static SchedulingPattern _respawnPattern = null;
	private static Future<?> _deleteTask;
	private static Future<?> _startTask;
	
	private Ryuminir()
	{
		addKillId(RYUMINIR);
		_respawnPattern = new SchedulingPattern(RYUMINIR_RESPAWN_PATTERN);
		_startTask = ThreadPool.schedule(new ScheduleAiTask(), (getNextRespawn() - System.currentTimeMillis()));
	}
	
	public static Script provider()
	{
		return new Ryuminir();
	}
	
	public class ScheduleAiTask implements Runnable
	{
		private Npc _npc;
		
		public ScheduleAiTask()
		{
		}
		
		@Override
		public void run()
		{
			final Calendar calendar = Calendar.getInstance();
			if (calendar.get(Calendar.DAY_OF_WEEK) == 7)
			{
				if (_npc != null)
				{
					_npc.deleteMe();
				}
				
				try
				{
					final Location loc = getRandomEntry(SPAWNS);
					_npc = addSpawn(RYUMINIR, loc.getX(), loc.getY(), loc.getZ(), 49151, false, 0, true);
					_deleteTask = ThreadPool.schedule(() -> deleteMe(_npc), ALIVE_MILLISECONDS);
				}
				catch (Exception e)
				{
				}
			}
			else
			{
				if (_startTask != null)
				{
					_startTask.cancel(true);
				}
				
				_startTask = ThreadPool.schedule(new ScheduleAiTask(), (getNextRespawn() - System.currentTimeMillis()));
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc != null)
		{
			if (_startTask != null)
			{
				_startTask.cancel(true);
			}
			
			final long millsecondsToNextRespawn = (getNextRespawn() - System.currentTimeMillis());
			_startTask = ThreadPool.schedule(new ScheduleAiTask(), millsecondsToNextRespawn);
		}
		
		if (_deleteTask != null)
		{
			_deleteTask.cancel(true);
		}
	}
	
	public void deleteMe(Npc npc)
	{
		if (_startTask != null)
		{
			_startTask.cancel(true);
		}
		
		final long millsecondsToNextRespawn = (getNextRespawn() - System.currentTimeMillis());
		_startTask = ThreadPool.schedule(new ScheduleAiTask(), millsecondsToNextRespawn);
		
		if (npc != null)
		{
			npc.deleteMe();
		}
	}
	
	public long getNextRespawn()
	{
		final long respawnTime = _respawnPattern.next(System.currentTimeMillis());
		// LOGGER.info(getClass().getSimpleName() + ": New " + "Ryuminir" + " respawn time to " + Util.formatDate(new Date(respawnTime), "dd.MM.yyyy HH:mm"));
		return respawnTime;
	}
	
	public static void main(String[] args)
	{
		new Ryuminir();
	}
}
