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
package ai.areas.LandsOfShadow;

import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.spawns.SpawnGroup;
import org.l2jmobius.gameserver.model.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.model.zone.ZoneType;

/**
 * @author Liamxroy
 */
public class LandsOfShadow extends Script
{
	private static final SpawnTemplate SPAWN_MOTHER_TREE_1 = SpawnData.getInstance().getSpawnByName("MotherTree1");
	private static final SpawnTemplate SPAWN_SAPLING_1 = SpawnData.getInstance().getSpawnByName("Sapling1");
	private static final SpawnTemplate SPAWN_MOTHER_TREE_2 = SpawnData.getInstance().getSpawnByName("MotherTree2");
	private static final SpawnTemplate SPAWN_SAPLING_2 = SpawnData.getInstance().getSpawnByName("Sapling2");
	private static final SpawnTemplate SPAWN_MOTHER_TREE_3 = SpawnData.getInstance().getSpawnByName("MotherTree3");
	private static final SpawnTemplate SPAWN_SAPLING_3 = SpawnData.getInstance().getSpawnByName("Sapling3");
	private static final SpawnTemplate SPAWN_MOTHER_TREE_4 = SpawnData.getInstance().getSpawnByName("MotherTree4");
	private static final SpawnTemplate SPAWN_SAPLING_4 = SpawnData.getInstance().getSpawnByName("Sapling4");
	private static final SpawnTemplate SPAWN_MOTHER_TREE_5 = SpawnData.getInstance().getSpawnByName("MotherTree5");
	private static final SpawnTemplate SPAWN_SAPLING_5 = SpawnData.getInstance().getSpawnByName("Sapling5");
	
	private static final AtomicInteger KILL_COUNTER_1 = new AtomicInteger();
	private static final AtomicInteger KILL_COUNTER_2 = new AtomicInteger();
	private static final AtomicInteger KILL_COUNTER_3 = new AtomicInteger();
	private static final AtomicInteger KILL_COUNTER_4 = new AtomicInteger();
	private static final AtomicInteger KILL_COUNTER_5 = new AtomicInteger();
	
	protected static final ZoneType ZONE_1 = ZoneManager.getInstance().getZoneByName("LandsOfShadow_1");
	protected static final ZoneType ZONE_2 = ZoneManager.getInstance().getZoneByName("LandsOfShadow_2");
	protected static final ZoneType ZONE_3 = ZoneManager.getInstance().getZoneByName("LandsOfShadow_3");
	protected static final ZoneType ZONE_4 = ZoneManager.getInstance().getZoneByName("LandsOfShadow_4");
	protected static final ZoneType ZONE_5 = ZoneManager.getInstance().getZoneByName("LandsOfShadow_5");
	
	protected static final ZoneType BUFF_ZONE_1 = ZoneManager.getInstance().getZoneByName("LandsOfShadow_Effect_1");
	protected static final ZoneType BUFF_ZONE_2 = ZoneManager.getInstance().getZoneByName("LandsOfShadow_Effect_2");
	protected static final ZoneType BUFF_ZONE_3 = ZoneManager.getInstance().getZoneByName("LandsOfShadow_Effect_3");
	protected static final ZoneType BUFF_ZONE_4 = ZoneManager.getInstance().getZoneByName("LandsOfShadow_Effect_4");
	protected static final ZoneType BUFF_ZONE_5 = ZoneManager.getInstance().getZoneByName("LandsOfShadow_Effect_5");
	
	private static final int[] MONSTERS =
	{
		22913,
		22914,
		22915,
		22916,
	};
	
	public LandsOfShadow()
	{
		addKillId(MONSTERS);
		addEnterZoneId(ZONE_1.getId());
		addEnterZoneId(ZONE_2.getId());
		addEnterZoneId(ZONE_3.getId());
		addEnterZoneId(ZONE_4.getId());
		addEnterZoneId(ZONE_5.getId());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "spawn1":
			{
				SPAWN_MOTHER_TREE_1.getGroups().forEach(SpawnGroup::spawnAll);
				SPAWN_SAPLING_1.getGroups().forEach(SpawnGroup::despawnAll);
				enableZones(BUFF_ZONE_1);
				startQuestTimer("despawn1", 1800000, null, null);
				break;
			}
			case "despawn1":
			{
				KILL_COUNTER_1.set(0);
				disableZones(BUFF_ZONE_1);
				SPAWN_MOTHER_TREE_1.getGroups().forEach(SpawnGroup::despawnAll);
				SPAWN_SAPLING_1.getGroups().forEach(SpawnGroup::spawnAll);
				break;
			}
			case "spawn2":
			{
				SPAWN_MOTHER_TREE_2.getGroups().forEach(SpawnGroup::spawnAll);
				SPAWN_SAPLING_2.getGroups().forEach(SpawnGroup::despawnAll);
				enableZones(BUFF_ZONE_2);
				startQuestTimer("despawn2", 1800000, null, null);
				break;
			}
			case "despawn2":
			{
				KILL_COUNTER_2.set(0);
				disableZones(BUFF_ZONE_2);
				SPAWN_MOTHER_TREE_2.getGroups().forEach(SpawnGroup::despawnAll);
				SPAWN_SAPLING_2.getGroups().forEach(SpawnGroup::spawnAll);
				break;
			}
			case "spawn3":
			{
				SPAWN_MOTHER_TREE_3.getGroups().forEach(SpawnGroup::spawnAll);
				SPAWN_SAPLING_3.getGroups().forEach(SpawnGroup::despawnAll);
				enableZones(BUFF_ZONE_3);
				startQuestTimer("despawn3", 1800000, null, null);
				break;
			}
			case "despawn3":
			{
				KILL_COUNTER_3.set(0);
				disableZones(BUFF_ZONE_3);
				SPAWN_MOTHER_TREE_3.getGroups().forEach(SpawnGroup::despawnAll);
				SPAWN_SAPLING_3.getGroups().forEach(SpawnGroup::spawnAll);
				break;
			}
			case "spawn4":
			{
				SPAWN_MOTHER_TREE_4.getGroups().forEach(SpawnGroup::spawnAll);
				SPAWN_SAPLING_4.getGroups().forEach(SpawnGroup::despawnAll);
				enableZones(BUFF_ZONE_4);
				startQuestTimer("despawn4", 1800000, null, null);
				break;
			}
			case "despawn4":
			{
				KILL_COUNTER_4.set(0);
				disableZones(BUFF_ZONE_4);
				SPAWN_MOTHER_TREE_4.getGroups().forEach(SpawnGroup::despawnAll);
				SPAWN_SAPLING_4.getGroups().forEach(SpawnGroup::spawnAll);
				break;
			}
			case "spawn5":
			{
				SPAWN_MOTHER_TREE_5.getGroups().forEach(SpawnGroup::spawnAll);
				SPAWN_SAPLING_5.getGroups().forEach(SpawnGroup::despawnAll);
				enableZones(BUFF_ZONE_5);
				startQuestTimer("despawn5", 1800000, null, null);
				break;
			}
			case "despawn5":
			{
				KILL_COUNTER_5.set(0);
				disableZones(BUFF_ZONE_5);
				SPAWN_MOTHER_TREE_5.getGroups().forEach(SpawnGroup::despawnAll);
				SPAWN_SAPLING_5.getGroups().forEach(SpawnGroup::spawnAll);
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (killer.isInCategory(CategoryType.HIGH_ELF_ALL_CLASS))
		{
			if (ZONE_1.isCharacterInZone(killer))
			{
				if (!BUFF_ZONE_1.isEnabled())
				{
					final int count = KILL_COUNTER_1.incrementAndGet();
					if (count >= 50)
					{
						startQuestTimer("spawn1", 1000, null, null);
					}
				}
			}
			else if (ZONE_2.isCharacterInZone(killer))
			{
				if (!BUFF_ZONE_2.isEnabled())
				{
					final int count = KILL_COUNTER_2.incrementAndGet();
					if (count >= 50)
					{
						startQuestTimer("spawn2", 1000, null, null);
					}
				}
			}
			else if (ZONE_3.isCharacterInZone(killer))
			{
				if (!BUFF_ZONE_3.isEnabled())
				{
					final int count = KILL_COUNTER_3.incrementAndGet();
					if (count >= 50)
					{
						startQuestTimer("spawn3", 1000, null, null);
					}
				}
			}
			else if (ZONE_4.isCharacterInZone(killer))
			{
				if (!BUFF_ZONE_4.isEnabled())
				{
					final int count = KILL_COUNTER_4.incrementAndGet();
					if (count >= 50)
					{
						startQuestTimer("spawn4", 1000, null, null);
					}
				}
			}
			else if (ZONE_5.isCharacterInZone(killer))
			{
				if (!BUFF_ZONE_5.isEnabled())
				{
					final int count = KILL_COUNTER_5.incrementAndGet();
					if (count >= 50)
					{
						startQuestTimer("spawn5", 1000, null, null);
					}
				}
			}
		}
	}
	
	@Override
	public void onEnterZone(Creature character, ZoneType zone)
	{
	}
	
	private void enableZones(ZoneType zone)
	{
		zone.setEnabled(true);
	}
	
	private void disableZones(ZoneType zone)
	{
		zone.setEnabled(false);
	}
	
	public static void main(String[] args)
	{
		new LandsOfShadow();
	}
}
