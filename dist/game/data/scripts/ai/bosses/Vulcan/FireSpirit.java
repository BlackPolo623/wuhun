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
package ai.bosses.Vulcan;

import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * @author Mobius
 */
public class FireSpirit extends Script
{
	// NPCs
	private static final int VALATION = 29246;
	private static final int VULCAN = 29247;
	private static final int ENRAGED_VULCAN = 29252;
	private static final int FIRE_SPIRIT = 34445;
	
	// Zone
	private static final ZoneType ENTRANCE_ZONE = ZoneManager.getInstance().getZoneByName("ancient_ruins_entrance_peace");
	private static final ZoneType VULKAN_FURNACE_ZONE = ZoneManager.getInstance().getZoneByName("vulcans_furnace_thz");
	private static final ZoneType VULKAN_LAIR_ZONE = ZoneManager.getInstance().getZoneByName("vulcans_lair_thz");
	
	// Locations
	private static final Location VULKAN_FURNACE = new Location(216686, -75268, -14594, 59899);
	private static final Location VULKAN_FURNACE_DEPTHS = new Location(217591, -86305, -14698, 24982);
	private static final Location VULKAN_LAIR = new Location(215941, -115111, -6641, 33324);
	private static final Location VULKAN_SPAWN = new Location(212586, -115234, -6631, 64342);
	
	// Other
	private static final int VULKAN_FURNACE_COST = 50000;
	private static final int VULKAN_FURNACE_DEPTHS_COST = 100000;
	private static final int FIRE_SPIRIT_DESPAWN_DELAY = 1800000; // 30 minutes.
	private static final int ENRAGED_SPAWN_CHANCE = 10;
	
	private FireSpirit()
	{
		addFirstTalkId(FIRE_SPIRIT);
		addKillId(VALATION, VULCAN, ENRAGED_VULCAN);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((npc == null) || (npc.getId() != FIRE_SPIRIT))
		{
			return null;
		}
		
		switch (event)
		{
			case "ENTER_FURNACE":
			{
				if (player.getAdena() >= VULKAN_FURNACE_COST)
				{
					player.reduceAdena(ItemProcessType.FEE, VULKAN_FURNACE_COST, npc, true);
					player.teleToLocation(VULKAN_FURNACE);
				}
				else
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA_2);
				}
				break;
			}
			case "ENTER_FURNACE_DEPTHS":
			{
				if (player.getAdena() >= VULKAN_FURNACE_DEPTHS_COST)
				{
					player.reduceAdena(ItemProcessType.FEE, VULKAN_FURNACE_DEPTHS_COST, npc, true);
					player.teleToLocation(VULKAN_FURNACE_DEPTHS);
				}
				else
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA_2);
				}
				break;
			}
			case "FIGHT_VULCAN":
			{
				if (VULKAN_FURNACE_ZONE.isCharacterInZone(player))
				{
					player.teleToLocation(VULKAN_LAIR);
				}
				break;
			}
			case "EXIT_VULCAN":
			{
				if (VULKAN_LAIR_ZONE.isCharacterInZone(player))
				{
					player.teleToLocation(VULKAN_FURNACE);
				}
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (ENTRANCE_ZONE.isCharacterInZone(player))
		{
			return "34445-01.htm";
		}
		
		if (VULKAN_LAIR_ZONE.isCharacterInZone(player))
		{
			return "34445-03.htm";
		}
		
		return "34445-02.htm";
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if ((npc.getId() == VALATION) && (World.getInstance().getNpc(VULCAN) == null) && (World.getInstance().getNpc(ENRAGED_VULCAN) == null))
		{
			addSpawn(getRandom(100) < ENRAGED_SPAWN_CHANCE ? ENRAGED_VULCAN : VULCAN, VULKAN_SPAWN);
		}
		
		addSpawn(FIRE_SPIRIT, npc, true, FIRE_SPIRIT_DESPAWN_DELAY);
	}
	
	public static void main(String[] args)
	{
		new FireSpirit();
	}
}
