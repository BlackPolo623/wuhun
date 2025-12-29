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
package ai.bosses.TowerOfInsolence;

import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author gugaf
 */
public class Phiriel extends Script
{
	// NPC
	private static final int PHIRIEL = 29210;
	
	// Location
	private static final Location PHIRIEL_LOC = new Location(115213, 16623, 10080);
	
	// Misc
	private static final String PHIRIEL_RESPAWN_PATTERN = "30 22 * * 4";
	private SchedulingPattern _respawnPattern = null;
	
	public Phiriel()
	{
		addKillId(PHIRIEL);
		
		_respawnPattern = new SchedulingPattern(PHIRIEL_RESPAWN_PATTERN);
		
		final long nextRespawnTime = getNextRespawnTime();
		if (getNextRespawnTime() > 0)
		{
			startQuestTimer("respawn_phiriel", nextRespawnTime, null, null);
		}
		else
		{
			addSpawn(PHIRIEL, PHIRIEL_LOC, false, getDespawnTime());
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("respawn_phiriel"))
		{
			addSpawn(PHIRIEL, PHIRIEL_LOC, false, getDespawnTime());
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final long nextRespawnTime = getNextRespawnTime();
		if (nextRespawnTime > 0)
		{
			startQuestTimer("respawn_phiriel", nextRespawnTime, null, null);
		}
		else
		{
			addSpawn(PHIRIEL, PHIRIEL_LOC, false, getDespawnTime());
		}
	}
	
	private long getDespawnTime()
	{
		final long currentTime = System.currentTimeMillis();
		return _respawnPattern.next(currentTime) - currentTime - 60000 /* 1 minute less */;
	}
	
	private long getNextRespawnTime()
	{
		final long currentTime = System.currentTimeMillis();
		return _respawnPattern.next(currentTime) - currentTime;
	}
	
	public static void main(String[] args)
	{
		new Phiriel();
	}
}
