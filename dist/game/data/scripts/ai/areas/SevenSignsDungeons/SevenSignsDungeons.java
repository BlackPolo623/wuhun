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
package ai.areas.SevenSignsDungeons;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.data.xml.TeleportListData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.spawns.SpawnGroup;
import org.l2jmobius.gameserver.model.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.PopupEventHud;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Mobius
 * @URL https://eu.4game.com/patchnotes/lineage2essence/440/
 */
public class SevenSignsDungeons extends Script
{
	// NPCs
	private static final Map<Integer, Location> ZIGGURAT_SPAWNS = new HashMap<>();
	static
	{
		ZIGGURAT_SPAWNS.put(31075, new Location(104192, 84064, -4376));
		ZIGGURAT_SPAWNS.put(31076, new Location(-20208, -250800, -8160));
		ZIGGURAT_SPAWNS.put(31077, new Location(-22224, 77376, -5168));
		ZIGGURAT_SPAWNS.put(31078, new Location(-21392, 77376, -5168));
		ZIGGURAT_SPAWNS.put(31079, new Location(140032, 79680, -5424));
		ZIGGURAT_SPAWNS.put(31080, new Location(140784, 79680, -5424));
	}
	
	// Teleports
	private static final Map<Integer, Location> ENTER_TELEPORTS = new HashMap<>();
	static
	{
		ENTER_TELEPORTS.put(31075, new Location(-20208, -250800, -8160));
		ENTER_TELEPORTS.put(31077, new Location(-21666, 77376, -5168));
		ENTER_TELEPORTS.put(31079, new Location(140613, 79692, -5424));
	}
	private static final Map<Integer, Location> EXIT_TELEPORTS = new HashMap<>();
	static
	{
		EXIT_TELEPORTS.put(31076, new Location(104192, 84064, -4376));
		EXIT_TELEPORTS.put(31078, new Location(-22076, 77375, -5168));
		EXIT_TELEPORTS.put(31080, new Location(139938, 79727, -5424));
	}
	private static final Location GIRAN_TELEPORT = TeleportListData.getInstance().getTeleport(25).getLocation();
	private static final Location ADEN_TELEPORT = TeleportListData.getInstance().getTeleport(321).getLocation();
	
	// Misc
	private static final SpawnTemplate PATRIOT_SPAWNS = SpawnData.getInstance().getSpawnByName("NecropolisOfThePatriots");
	private static final SpawnTemplate FORBIDDEN_SPAWNS = SpawnData.getInstance().getSpawnByName("CatacombOfTheForbiddenPath");
	private static final SpawnTemplate WITCHES_SPAWNS = SpawnData.getInstance().getSpawnByName("CatacombOfTheWitch");
	
	private static boolean _active = false;
	
	private SevenSignsDungeons()
	{
		addStartNpc(ZIGGURAT_SPAWNS.keySet());
		addFirstTalkId(ZIGGURAT_SPAWNS.keySet());
		addTalkId(ZIGGURAT_SPAWNS.keySet());
		
		for (Entry<Integer, Location> spawn : ZIGGURAT_SPAWNS.entrySet())
		{
			addSpawn(spawn.getKey(), spawn.getValue());
		}
		
		scheduleNext();
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "unavailable.html":
			{
				return event;
			}
			case "ENTER":
			{
				final Location location = ENTER_TELEPORTS.get(npc.getId());
				if ((location != null) && _active)
				{
					player.teleToLocation(location);
				}
				break;
			}
			case "EXIT":
			{
				final Location location = EXIT_TELEPORTS.get(npc.getId());
				if (location != null)
				{
					player.teleToLocation(location);
				}
				break;
			}
			case "GIRAN":
			{
				if (ENTER_TELEPORTS.containsKey(npc.getId()))
				{
					player.teleToLocation(GIRAN_TELEPORT);
				}
				break;
			}
			case "ADEN":
			{
				if (ENTER_TELEPORTS.containsKey(npc.getId()))
				{
					player.teleToLocation(ADEN_TELEPORT);
				}
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		switch (npc.getId())
		{
			case 31075:
			case 31077:
			case 31079:
			{
				return _active ? "active.html" : "disabled.html";
			}
			default:
			{
				return "exit.html";
			}
		}
	}
	
	private void scheduleNext()
	{
		// Schedule daily at 21:55.
		final long currentTime = System.currentTimeMillis();
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 21);
		calendar.set(Calendar.MINUTE, 55);
		calendar.set(Calendar.SECOND, 0);
		if (calendar.getTimeInMillis() < currentTime)
		{
			calendar.add(Calendar.DAY_OF_YEAR, 1);
		}
		
		final long calendarTime = calendar.getTimeInMillis();
		final long startDelay = Math.max(0, calendarTime - currentTime);
		ThreadPool.schedule(() -> announce(), startDelay);
	}
	
	private void announce()
	{
		Broadcast.toAllOnlinePlayers(new PopupEventHud(PopupEventHud.CATACOMBS, true));
		Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(NpcStringId.IN_5_MINUTES_MONSTERS_WILL_APPEAR_IN_A_SEVEN_SIGNS_DUNGEON_YOU_HAVE_30_MINUTES_TO_KILL_THEM, ExShowScreenMessage.TOP_CENTER, 10000, true));
		ThreadPool.schedule(() -> Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(NpcStringId.USE_TELEPORT_TO_GO_TO_A_SEVEN_SIGNS_DUNGEON, ExShowScreenMessage.TOP_CENTER, 10000, true)), 16000);
		ThreadPool.schedule(() -> enableDungeons(), 300000); // 5 minutes.
	}
	
	private void enableDungeons()
	{
		_active = true;
		
		PATRIOT_SPAWNS.getGroups().forEach(SpawnGroup::spawnAll);
		FORBIDDEN_SPAWNS.getGroups().forEach(SpawnGroup::spawnAll);
		WITCHES_SPAWNS.getGroups().forEach(SpawnGroup::spawnAll);
		
		// Disable dungeons.
		ThreadPool.schedule(() ->
		{
			_active = false;
			
			Broadcast.toAllOnlinePlayers(new PopupEventHud(PopupEventHud.CATACOMBS, false));
			
			PATRIOT_SPAWNS.getGroups().forEach(SpawnGroup::despawnAll);
			FORBIDDEN_SPAWNS.getGroups().forEach(SpawnGroup::despawnAll);
			WITCHES_SPAWNS.getGroups().forEach(SpawnGroup::despawnAll);
			
			scheduleNext();
		}, 7200000); // 2 hours.
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		if (!_active)
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		player.sendPacket(new PopupEventHud(PopupEventHud.CATACOMBS, true));
	}
	
	public static void main(String[] args)
	{
		new SevenSignsDungeons();
	}
}
