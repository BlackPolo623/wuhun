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
package instances.PaganTemple.EliNpc;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;

import instances.PaganTemple.PaganTempleManager;

/**
 * @author Index
 */
public class PaganTempleEliNpc extends Script
{
	private static final int ELI_NPC_ID = 34379;
	private static final int TRIOLS_REVALATION = 15993;
	
	private static final List<Entry<Long, Location>> TELEPORT_LOCATIONS = new ArrayList<>();
	static
	{
		TELEPORT_LOCATIONS.add(new SimpleEntry<>(0L, new Location(-16352, -43522, -10729)));
		TELEPORT_LOCATIONS.add(new SimpleEntry<>(500_000L, new Location(-16385, -49975, -10921)));
		TELEPORT_LOCATIONS.add(new SimpleEntry<>(Long.MAX_VALUE, null));
		TELEPORT_LOCATIONS.add(new SimpleEntry<>(1_500_000L, new Location(-16387, -52229, -10607)));
		TELEPORT_LOCATIONS.add(new SimpleEntry<>(1_000_000L, new Location(-18006, -50719, -11017)));
		TELEPORT_LOCATIONS.add(new SimpleEntry<>(1_000_000L, new Location(-14795, -50695, -11017)));
	}
	
	public PaganTempleEliNpc()
	{
		addFirstTalkId(ELI_NPC_ID);
		addTalkId(ELI_NPC_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final Instance world = (player == null) || (npc == null) ? null : player.getInstanceWorld();
		if ((event == null) || (npc == null) || (world == null) || (world.getTemplateId() != PaganTempleManager.INSTANCE_TEMPLATE_ID))
		{
			return super.onEvent(event, npc, player);
		}
		
		if (event.startsWith("TALK"))
		{
			return handleTalkAction(event, npc, player, world);
		}
		else if (!PaganTempleManager.isAvailableToEnter(player))
		{
			return ELI_NPC_ID + "-no.htm";
		}
		else if (event.startsWith("TELEPORT_ME_TO"))
		{
			return handleTeleportAction(event, npc, player, world);
		}
		else if (event.equalsIgnoreCase("ALTAR_REQUEST"))
		{
			return ELI_NPC_ID + (world.getStatus() >= PaganTempleManager.ANDREAS_BOSS ? "-altar-select" : "-altar-no-time") + ".htm";
		}
		
		return super.onEvent(event, npc, player);
	}
	
	private static String handleTalkAction(String event, Npc npc, Player player, Instance world)
	{
		final int index = event.length() <= "TALK".length() ? 0 : Integer.parseInt(event.substring("TALK".length() + 1));
		if (!PaganTempleManager.isAvailableToEnter(player))
		{
			return ELI_NPC_ID + "-no.htm";
		}
		else if (index == 1)
		{
			return ELI_NPC_ID + "-info.htm";
		}
		else
		{
			return ELI_NPC_ID + ".htm";
		}
	}
	
	private static String handleTeleportAction(String event, Npc npc, Player player, Instance world)
	{
		int index = event.length() == "TELEPORT_ME_TO".length() ? -1 : Integer.parseInt(event.substring("TELEPORT_ME_TO".length() + 1));
		index = (index == -1) || (TELEPORT_LOCATIONS.size() <= index) ? -1 : index;
		
		if ((index == -1) || (index == 2))
		{
			world.ejectPlayer(player);
			return null;
		}
		
		if ((index >= 3) && (world.getStatus() < PaganTempleManager.ANDREAS_BOSS))
		{
			return ELI_NPC_ID + "-altar-no-time" + ".htm";
		}
		
		final Entry<Long, Location> loc = TELEPORT_LOCATIONS.get(index);
		if ((loc == null) || ((loc.getValue() == null) && ((loc.getKey() != 0) && (player.getAdena() < loc.getKey()))))
		{
			return ELI_NPC_ID + "-no-adena" + ".htm";
		}
		
		if (index >= 3)
		{
			if (!checkAndDecreaseTriolsRevalation(world))
			{
				return ELI_NPC_ID + "-altar-no-time" + ".htm";
			}
			else if (!teleportById(world, player, npc, index))
			{
				return ELI_NPC_ID + "-no-adena" + ".htm";
			}
			
			addNewFighter(world, player);
		}
		else if (!teleportById(world, player, npc, index))
		{
			return ELI_NPC_ID + "-no-adena" + ".htm";
		}
		
		return null;
	}
	
	private static boolean teleportById(Instance world, Player player, Npc npc, int index)
	{
		final Entry<Long, Location> loc = TELEPORT_LOCATIONS.get(index);
		if ((loc.getKey() <= 0L) || ((player.getAdena() >= loc.getKey()) && player.getInventory().reduceAdena(ItemProcessType.FEE, loc.getKey(), player, npc)))
		{
			player.teleToLocation(loc.getValue(), false, world);
			return true;
		}
		
		return false;
	}
	
	private static boolean checkAndDecreaseTriolsRevalation(Instance world)
	{
		final Npc triolsRavalation = world.getNpc(TRIOLS_REVALATION);
		if (triolsRavalation == null)
		{
			return false;
		}
		
		if (world.getParameters().increaseInt(PaganTempleManager.VARIABLE_TRIOLS_REVALATION_USES, 0, 1) >= 10)
		{
			if (triolsRavalation.getScriptValue() > 0)
			{
				PaganTempleManager.deSpawnNpcGroup(world, "TRIOLS_REVALATION_" + triolsRavalation.getScriptValue());
			}
			else
			{
				triolsRavalation.deleteMe();
			}
			
			world.getParameters().set(PaganTempleManager.VARIABLE_TRIOLS_REVALATION_USES, 0);
		}
		
		return true;
	}
	
	private static boolean isFightBefore(Instance world, Player player)
	{
		if ((world == null) || (player == null))
		{
			return false;
		}
		
		if (world.getParameters().contains(PaganTempleManager.VARIABLE_PLAYERS_FIGHT_LIST))
		{
			final List<Integer> playerList = world.getParameters().getIntegerList(PaganTempleManager.VARIABLE_PLAYERS_FIGHT_LIST);
			if (playerList.contains(player.getObjectId()))
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static void addNewFighter(Instance world, Player player)
	{
		if ((world == null) || (player == null))
		{
			return;
		}
		
		final List<Integer> playerList = world.getParameters().contains(PaganTempleManager.VARIABLE_PLAYERS_FIGHT_LIST) ? world.getParameters().getIntegerList(PaganTempleManager.VARIABLE_PLAYERS_FIGHT_LIST) : new ArrayList<>();
		if (!playerList.contains(player.getObjectId()))
		{
			playerList.add(player.getObjectId());
		}
		
		world.getParameters().setIntegerList(PaganTempleManager.VARIABLE_PLAYERS_FIGHT_LIST, playerList);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = (player == null) || (npc == null) ? null : player.getInstanceWorld();
		if ((npc == null) || (world == null) || (world.getTemplateId() != PaganTempleManager.INSTANCE_TEMPLATE_ID))
		{
			return super.onFirstTalk(npc, player);
		}
		
		if (!PaganTempleManager.isAvailableToEnter(player))
		{
			return npc.getId() + "-no" + ".htm";
		}
		
		if (isFightBefore(world, player))
		{
			return ELI_NPC_ID + "-ex" + ".htm";
		}
		
		return ELI_NPC_ID + ".htm";
	}
	
	public static void main(String[] args)
	{
		new PaganTempleEliNpc();
	}
}
