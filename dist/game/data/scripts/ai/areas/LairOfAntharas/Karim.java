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
package ai.areas.LairOfAntharas;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * @author Mobius
 */
public class Karim extends Script
{
	// NPC
	private static final int KARIM = 25913;
	
	// Locations
	private static final Location[] LOCATIONS =
	{
		new Location(144801, 114629, -3717),
		new Location(147294, 116274, -3712),
		new Location(148694, 115718, -3724),
		new Location(147470, 112505, -3724)
	};
	
	// Zone
	private static final ZoneType ZONE = ZoneManager.getInstance().getZoneByName("antharas_lair");
	
	// Misc
	private static Npc _karim;
	
	private Karim()
	{
		addKillId(KARIM);
		
		// Schedule daily spawn from 18:50 till 19:20.
		final long currentTime = System.currentTimeMillis();
		final long startSpawnDelay = Math.max(0, TimeUtil.getNextTime(18, 50).getTimeInMillis() - currentTime);
		final long startDeSpawnDelay = Math.max(0, TimeUtil.getNextTime(19, 20).getTimeInMillis() - currentTime);
		ThreadPool.scheduleAtFixedRate(this::spawnKarim, startSpawnDelay, 86400000);
		ThreadPool.scheduleAtFixedRate(this::despawnKarim, startDeSpawnDelay, 86400000);
	}
	
	private void spawnKarim()
	{
		_karim = addSpawn(KARIM, getRandomEntry(LOCATIONS));
		if (ZONE != null)
		{
			ZONE.broadcastPacket(new ExShowScreenMessage(NpcStringId.ANTHARAS_ADEPT_KARIM_HAS_APPEARED, 2, 5000));
			ZONE.broadcastPacket(new ExShowScreenMessage("Karim has spawned!", 5000));
			ZONE.broadcastPacket(new PlaySound("BS02_A"));
		}
	}
	
	private void despawnKarim()
	{
		if ((_karim != null) && _karim.isSpawned())
		{
			_karim.deleteMe();
		}
	}
	
	public static void main(String[] args)
	{
		new Karim();
	}
}
