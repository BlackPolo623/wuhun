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
package ai.bosses.Zaken;

import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * Zaken AI<br>
 * TODO: Skill cast?<br>
 * TODO: Day/Night spawn? TODO: Boss message broadcast.
 * @author Mobius
 */
public class Zaken extends Script
{
	// NPC
	private static final int ZAKEN = 29022;
	
	// Location
	private static final int ZAKEN_X = 42675;
	private static final int ZAKEN_Y = 207567;
	private static final int ZAKEN_Z = -3752;
	private static final int ZAKEN_HEADING = 7835;
	
	// Misc
	private static final byte ALIVE = 0;
	private static final byte DEAD = 1;
	
	private Zaken()
	{
		addKillId(ZAKEN);
		
		final StatSet info = GrandBossManager.getInstance().getStatSet(ZAKEN);
		final int status = GrandBossManager.getInstance().getStatus(ZAKEN);
		if (status == DEAD)
		{
			// load the unlock date and time from DB
			final long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			if (temp > 0)
			{
				startQuestTimer("zaken_unlock", temp, null, null);
			}
			else // the time has already expired while the server was offline
			{
				spawnBoss();
			}
		}
		else
		{
			spawnBoss();
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("zaken_unlock"))
		{
			spawnBoss();
		}
		
		return super.onEvent(event, npc, player);
	}
	
	private void spawnBoss()
	{
		final GrandBoss zaken = (GrandBoss) addSpawn(ZAKEN, ZAKEN_X, ZAKEN_Y, ZAKEN_Z, ZAKEN_HEADING, false, 0);
		GrandBossManager.getInstance().setStatus(ZAKEN, ALIVE);
		GrandBossManager.getInstance().addBoss(zaken);
		zaken.broadcastPacket(new PlaySound(1, "BS01_A", 1, zaken.getObjectId(), zaken.getX(), zaken.getY(), zaken.getZ()));
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		GrandBossManager.getInstance().setStatus(ZAKEN, DEAD);
		
		final long baseIntervalMillis = GrandBossConfig.ZAKEN_SPAWN_INTERVAL * 3600000;
		final long randomRangeMillis = GrandBossConfig.ZAKEN_SPAWN_RANDOM * 3600000;
		final long respawnTime = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
		startQuestTimer("zaken_unlock", respawnTime, null, null);
		
		// also save the respawn time so that the info is maintained past reboots
		final StatSet info = GrandBossManager.getInstance().getStatSet(ZAKEN);
		info.set("respawn_time", System.currentTimeMillis() + respawnTime);
		GrandBossManager.getInstance().setStatSet(ZAKEN, info);
	}
	
	public static void main(String[] args)
	{
		new Zaken();
	}
}
