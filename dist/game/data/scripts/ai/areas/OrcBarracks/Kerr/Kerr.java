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
package ai.areas.OrcBarracks.Kerr;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author Mobius
 */
public class Kerr extends Script
{
	// NPC
	private static final int KERR = 22140;
	
	// Locations
	private static final Location[] SPAWNS =
	{
		new Location(-89895, 108555, -3530),
		new Location(-88925, 112500, -3414),
		new Location(-92690, 112605, -3728),
		new Location(-95168, 110316, -3823),
		new Location(-95823, 114893, -3528),
		new Location(-93044, 117007, -3315),
		new Location(-96494, 119720, -3196),
		new Location(-96553, 106922, -3729),
		new Location(-93522, 105608, -3491),
		new Location(-96148, 102058, -3496),
		new Location(-93228, 100642, -3551),
		new Location(-91038, 102344, -3418),
		new Location(-89841, 100158, -3612),
		new Location(-88155, 103068, -3385),
	};
	
	// Misc
	private static final int SPAWN_COUNT = 3;
	private static final int RESPAWN_DELAY = 60000; // 1 minute.
	private static final Map<Npc, Location> KERR_SPAWN_LOCATIONS = new ConcurrentHashMap<>(SPAWN_COUNT);
	
	private Kerr()
	{
		addKillId(KERR);
		for (int i = 0; i < SPAWN_COUNT; i++)
		{
			spawnKerr();
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		ThreadPool.schedule(() ->
		{
			KERR_SPAWN_LOCATIONS.remove(npc);
			spawnKerr();
		}, RESPAWN_DELAY);
	}
	
	private void spawnKerr()
	{
		while (true)
		{
			final Location location = getRandomEntry(SPAWNS);
			if (KERR_SPAWN_LOCATIONS.containsValue(location))
			{
				continue;
			}
			
			KERR_SPAWN_LOCATIONS.put(addSpawn(KERR, location), location);
			break;
		}
	}
	
	public static void main(String[] args)
	{
		new Kerr();
	}
}
