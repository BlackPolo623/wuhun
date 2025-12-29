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
package ai.areas.GardenOfAuthority;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.Earthquake;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

/**
 * @autor Serenitty
 * @URL https://l2central.info/essence/notices/1645.html?lang=en
 */
public class GardenOfAuthority extends Script
{
	private static final long COMBAT_DURATION = 3600000; // 1 hour
	private static final long DESPAWN_TIME_PHON = 5 * 60 * 1000;
	private static final int PHON = 34405; // Teleporter
	private static final int BOSS = 25984;
	private static final Location PHON_LOCATION = new Location(-50394, 883279, -5131);
	private static final Location BOSS_LOCATION = new Location(-50584, 83224, -5133);
	
	// Teleport stage cubes.
	private static final int[] TELEPORT_STAGE_CUBES =
	{
		18202104,
		18201704,
		22250134,
		18201504,
		18201104,
		18201406,
		18201602,
		18200106,
		18201106,
		18200202,
		18202002,
		18202406,
		18202206,
		18201306
	};
	private static final Location PRIMEVAL_GARDEN = new Location(219172, 92596, 269);
	private static final Location GARDEN_OF_EVA = new Location(205204, 80084, -387);
	private static final String[] ZONE_NAMES =
	{
		"Garden Authority Cube1",
		"Garden Authority Cube2",
		"Garden Authority Cube3",
		"Garden Authority Cube4",
		"Garden Authority Cube5"
	};
	private static final ZoneType[] ZONES = new ZoneType[ZONE_NAMES.length];
	private static final ZoneType ZONE_GENERAL = ZoneManager.getInstance().getZoneByName("fg_authority_thz");
	
	private boolean _inProgress = false;
	private Npc _boss;
	private static ScheduledFuture<?> _despawnTask;
	
	private GardenOfAuthority()
	{
		addKillId(BOSS);
		addFirstTalkId(PHON);
		
		// Initialize zones.
		for (int i = 0; i < ZONE_NAMES.length; i++)
		{
			ZONES[i] = ZoneManager.getInstance().getZoneByName(ZONE_NAMES[i]);
			addEnterZoneId(ZONES[i].getId());
		}
		
		final long currentTime = System.currentTimeMillis();
		final Calendar startTime = Calendar.getInstance();
		startTime.set(Calendar.HOUR_OF_DAY, 23);
		startTime.set(Calendar.MINUTE, 00);
		startTime.set(Calendar.SECOND, 0);
		if (startTime.getTimeInMillis() < currentTime)
		{
			startTime.add(Calendar.DAY_OF_YEAR, 1);
		}
		
		ThreadPool.scheduleAtFixedRate(this::start, startTime.getTimeInMillis() - currentTime, 86400000); // 86400000 = 1 day
	}
	
	private void start()
	{
		_inProgress = true;
		handleEffects();
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "Garden_of_Eva_tele":
			{
				teleportCheck(player, 1);
				break;
			}
			case "Primeval_Garden_tele":
			{
				teleportCheck(player, 2);
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		final Player player = creature.asPlayer();
		if (player == null)
		{
			return;
		}
		
		if (creature.isPlayer() && (_inProgress))
		{
			if (zone == ZONE_GENERAL)
			{
				handleEffects();
			}
		}
		
		for (ZoneType cubeArea : ZONES)
		{
			if ((zone == cubeArea) && creature.isPlayer() && (_inProgress))
			{
				player.teleToLocation(-51348, 83629, -5089);
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return getHtm(player, "34405.htm");
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		_inProgress = false;
		lastHitRewardMonsters(killer);
		addSpawn(PHON, PHON_LOCATION, false, DESPAWN_TIME_PHON, true);
		
		for (int triggerId : TELEPORT_STAGE_CUBES)
		{
			ZONE_GENERAL.broadcastPacket(new OnEventTrigger(triggerId, false));
		}
	}
	
	private void sendEarthquake()
	{
		for (Player player : ZONE_GENERAL.getPlayersInside())
		{
			player.sendPacket(new Earthquake(player, 20, 7));
		}
	}
	
	private void spawnBoss()
	{
		_boss = addSpawn(BOSS, BOSS_LOCATION, false, 0, true);
	}
	
	private void despawnBoss()
	{
		_inProgress = false;
		deleteBoss();
		for (int triggerId : TELEPORT_STAGE_CUBES)
		{
			ZONE_GENERAL.broadcastPacket(new OnEventTrigger(triggerId, false));
		}
	}
	
	private void deleteBoss()
	{
		if (_boss != null)
		{
			_boss.deleteMe();
		}
		
		if (_despawnTask != null)
		{
			_despawnTask.cancel(true);
		}
	}
	
	private void lastHitRewardMonsters(Player player)
	{
		final int rnd = Rnd.get(100);
		int reward = 0;
		if (rnd < 5)
		{
			reward = 33809; // Improved Scroll: Enchant A-grade Weapon.
			ZONE_GENERAL.broadcastPacket(new ExShowScreenMessage(NpcStringId.S1_HAS_OBTAINED_SCROLL_ENCHANT_WEAPON, ExShowScreenMessage.BOTTOM_RIGHT, 10000, false, player.getName()));
		}
		else if (rnd < 15)
		{
			reward = 729; // Scroll: Enchant A-grade Weapon.
			ZONE_GENERAL.broadcastPacket(new ExShowScreenMessage(NpcStringId.S1_HAS_OBTAINED_SCROLL_ENCHANT_WEAPON, ExShowScreenMessage.BOTTOM_RIGHT, 10000, false, player.getName()));
		}
		else
		{
			reward = 730; // Scroll: Enchant A-grade Armor.
			ZONE_GENERAL.broadcastPacket(new ExShowScreenMessage(NpcStringId.S1_HAS_OBTAINED_SCROLL_ENCHANT_ARMOR, ExShowScreenMessage.BOTTOM_RIGHT, 10000, false, player.getName()));
		}
		
		player.addItem(ItemProcessType.QUEST, reward, 1, player, true);
	}
	
	private void handleEffects()
	{
		ZONE_GENERAL.broadcastPacket(new ExShowScreenMessage(NpcStringId.EMPRESS_OF_JUDGMENT_RADIN_HAS_APPEARED, ExShowScreenMessage.TOP_CENTER, 17000, true));
		for (int triggerId : TELEPORT_STAGE_CUBES)
		{
			ZONE_GENERAL.broadcastPacket(new OnEventTrigger(triggerId, true));
		}
		
		sendEarthquake();
		spawnBoss();
		
		_despawnTask = ThreadPool.schedule(this::despawnBoss, COMBAT_DURATION);
	}
	
	private void teleportCheck(Player player, int locationId)
	{
		int requiredMoney = 0;
		Location location = null;
		switch (locationId)
		{
			case 1:
			{
				requiredMoney = 1000000;
				location = PRIMEVAL_GARDEN;
				break;
			}
			case 2:
			{
				requiredMoney = 1000000;
				location = GARDEN_OF_EVA;
				break;
			}
		}
		
		if (!player.destroyItemByItemId(ItemProcessType.FEE, Inventory.ADENA_ID, requiredMoney, player, true))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_MONEY_TO_USE_THE_FUNCTION);
			return;
		}
		
		player.teleToLocation(location);
	}
	
	public static void main(String[] args)
	{
		new GardenOfAuthority();
	}
}
