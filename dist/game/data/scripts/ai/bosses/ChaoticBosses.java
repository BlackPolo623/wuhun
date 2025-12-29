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
package ai.bosses;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.managers.DatabaseSpawnManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.spawns.SpawnGroup;
import org.l2jmobius.gameserver.model.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeClientEffectInfo;

/**
 * @URL https://l2central.info/essence/articles/23.html?lang=en
 * @URL https://l2central.info/essence/updates/2882.html?lang=en#19010
 * @author NasSeKa, Mobius
 */
public final class ChaoticBosses extends Script
{
	// NPCs
	private static final int CHAOTIC_CORE = 29170;
	private static final int CHAOTIC_ORFEN = 29171;
	private static final int CHAOTIC_QUEEN_ANT = 29172;
	private static final int CHAOTIC_ZAKEN = 29173;
	private static final int[] RAID_BOSSES =
	{
		CHAOTIC_CORE,
		CHAOTIC_ORFEN,
		CHAOTIC_QUEEN_ANT,
		CHAOTIC_ZAKEN,
	};
	
	// Zone
	private static final ZoneType SEAL_OF_SHILLIEN_ZONE = ZoneManager.getInstance().getZoneByName("aden_2518_001");
	private static final ZoneType SEAL_OF_SHILLIEN_TELEPORT_ZONE = ZoneManager.getInstance().getZoneByName("Seal of Shillien Teleport");
	private static final Map<Integer, ExChangeClientEffectInfo> ZONE_EFFECTS = new HashMap<>();
	static
	{
		ZONE_EFFECTS.put(CHAOTIC_CORE, ExChangeClientEffectInfo.SEAL_OF_SHILLEN_CORE);
		ZONE_EFFECTS.put(CHAOTIC_ORFEN, ExChangeClientEffectInfo.SEAL_OF_SHILLEN_ORFEN);
		ZONE_EFFECTS.put(CHAOTIC_QUEEN_ANT, ExChangeClientEffectInfo.SEAL_OF_SHILLEN_QUEEN_ANT);
		ZONE_EFFECTS.put(CHAOTIC_ZAKEN, ExChangeClientEffectInfo.SEAL_OF_SHILLEN_ZAKEN);
	}
	
	// Spawn
	private static final Map<Integer, SpawnTemplate> SPAWNS = new HashMap<>();
	static
	{
		SPAWNS.put(CHAOTIC_CORE, SpawnData.getInstance().getSpawnByName("Seal of Shillien Core Monsters"));
		SPAWNS.put(CHAOTIC_ORFEN, SpawnData.getInstance().getSpawnByName("Seal of Shillien Orfen Monsters"));
		SPAWNS.put(CHAOTIC_QUEEN_ANT, SpawnData.getInstance().getSpawnByName("Seal of Shillien Queen Ant Monsters"));
		SPAWNS.put(CHAOTIC_ZAKEN, SpawnData.getInstance().getSpawnByName("Seal of Shillien Zaken Monsters"));
	}
	
	// Misc
	private static final int RESPAWN_DELAY = 86400000; // 24 hours.
	private static final Location SPAWN_LOCATION = new Location(191512, 21855, -3680);
	private static ExChangeClientEffectInfo _zoneEffect = ExChangeClientEffectInfo.SEAL_OF_SHILLEN_DEFAULT;
	
	private ChaoticBosses()
	{
		addKillId(RAID_BOSSES);
		addSpawnId(RAID_BOSSES);
		addEnterZoneId(SEAL_OF_SHILLIEN_ZONE.getId(), SEAL_OF_SHILLIEN_TELEPORT_ZONE.getId());
		
		// Schedule reset everyday at 20:00.
		final Calendar nextReset = TimeUtil.getNextTime(20, 0);
		final long startDelay = nextReset.getTimeInMillis() - System.currentTimeMillis();
		
		// Daily reset task.
		ThreadPool.scheduleAtFixedRate(this::onSpawn, startDelay, RESPAWN_DELAY);
		
		// Check boss respawn.
		for (Entry<Integer, SpawnTemplate> entry : SPAWNS.entrySet())
		{
			final int chaosBossId = entry.getKey();
			if (World.getInstance().getNpc(chaosBossId) != null)
			{
				_zoneEffect = ZONE_EFFECTS.get(chaosBossId);
				entry.getValue().getGroups().forEach(SpawnGroup::spawnAll);
				return;
			}
		}
		
		// No boss respawn.
		final int chaosBossId = getRandom(CHAOTIC_CORE, CHAOTIC_ZAKEN);
		_zoneEffect = ZONE_EFFECTS.get(chaosBossId);
		SPAWNS.get(chaosBossId).getGroups().forEach(SpawnGroup::spawnAll);
	}
	
	private void onSpawn()
	{
		for (int npcId : RAID_BOSSES)
		{
			for (Spawn spawn : SpawnTable.getInstance().getSpawns(npcId))
			{
				for (Npc monster : spawn.getSpawnedNpcs())
				{
					if (!monster.isAlikeDead())
					{
						spawn.stopRespawn();
						DatabaseSpawnManager.getInstance().deleteSpawn(spawn, true);
						monster.deleteMe();
					}
				}
			}
		}
		
		final int randomIndex = getRandom(4);
		final int chaosBossId = RAID_BOSSES[randomIndex];
		final NpcTemplate template = NpcData.getInstance().getTemplate(chaosBossId);
		if (template != null)
		{
			_zoneEffect = ZONE_EFFECTS.get(chaosBossId);
			SEAL_OF_SHILLIEN_ZONE.broadcastPacket(_zoneEffect);
			
			try
			{
				final Spawn spawn = new Spawn(template);
				spawn.setXYZ(SPAWN_LOCATION);
				spawn.setRespawnDelay(RESPAWN_DELAY);
				spawn.startRespawn();
				DatabaseSpawnManager.getInstance().addNewSpawn(spawn, true);
			}
			catch (Exception e)
			{
				LOGGER.warning("ChaoticBosses: Could not spawn " + chaosBossId);
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		for (SpawnTemplate spawns : SPAWNS.values())
		{
			spawns.getGroups().forEach(SpawnGroup::despawnAll);
		}
		
		SPAWNS.get(npc.getId()).getGroups().forEach(SpawnGroup::spawnAll);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		for (int npcId : RAID_BOSSES)
		{
			for (Spawn spawn : SpawnTable.getInstance().getSpawns(npcId))
			{
				for (Npc monster : spawn.getSpawnedNpcs())
				{
					if (!monster.isDead())
					{
						spawn.stopRespawn();
						DatabaseSpawnManager.getInstance().deleteSpawn(spawn, true);
						monster.deleteMe();
					}
				}
			}
		}
	}
	
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer())
		{
			creature.sendPacket(_zoneEffect);
		}
	}
	
	public static void main(String[] args)
	{
		new ChaoticBosses();
	}
}
