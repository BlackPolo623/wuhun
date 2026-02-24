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
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

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
		if ((event == null) || (npc == null) || (player == null))
		{
			return super.onEvent(event, npc, player);
		}

		// 主世界入口處理
		if (event.equalsIgnoreCase("enterPaganTemple"))
		{
			return handleEnterFromOutside(player);
		}

		final Instance world = player.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != PaganTempleManager.INSTANCE_TEMPLATE_ID))
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

		// 主世界 - 顯示入口資訊頁面
		if ((world == null) || (world.getTemplateId() != PaganTempleManager.INSTANCE_TEMPLATE_ID))
		{
			showEntrancePage(npc, player);
			return null;
		}

		// 副本內 - 原有邏輯
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

	// ==================== 主世界入口功能 ====================

	private void showEntrancePage(Npc npc, Player player)
	{
		String content = HtmCache.getInstance().getHtm(player, "data/scripts/instances/PaganTemple/34379-entrance.htm");
		if (content == null)
		{
			player.sendMessage("找不到對話頁面。");
			return;
		}

		final Instance paganInstance = findPaganInstance();
		final String statusText;
		final String statusColor;
		final String remainingTime;

		// 用 Calendar 判斷真實的開放狀態
		final Calendar now = Calendar.getInstance();
		final int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
		final int hour = now.get(Calendar.HOUR_OF_DAY);
		// 開放時間：週一 00:00 ~ 週六 00:00 (Calendar: SUNDAY=1, MONDAY=2, ..., SATURDAY=7)
		final boolean isOpenPeriod = (dayOfWeek >= Calendar.MONDAY) && (dayOfWeek <= Calendar.FRIDAY);

		if (!isOpenPeriod)
		{
			// 週六或週日 = 關閉期間
			statusText = "已關閉";
			statusColor = "FF0000";
			final Calendar nextMonday = (Calendar) now.clone();
			final int daysToMonday = (dayOfWeek == Calendar.SATURDAY) ? 2 : 1;
			nextMonday.add(Calendar.DAY_OF_MONTH, daysToMonday);
			nextMonday.set(Calendar.HOUR_OF_DAY, 0);
			nextMonday.set(Calendar.MINUTE, 0);
			nextMonday.set(Calendar.SECOND, 0);
			remainingTime = formatDuration(nextMonday.getTimeInMillis() - now.getTimeInMillis()) + " 後重新開放";
		}
		else
		{
			// 週一~週五 = 開放期間
			// 檢查是否有 BOSS
			if ((paganInstance != null) && (paganInstance.getParameters().getInt("INSTANCE_STATUS", PaganTempleManager.NORMAL) == PaganTempleManager.ANDREAS_BOSS))
			{
				statusText = "BOSS 出沒中";
				statusColor = "FF00FF";
			}
			else if ((dayOfWeek == Calendar.FRIDAY) && (hour >= 22))
			{
				statusText = "BOSS 出沒中";
				statusColor = "FF00FF";
			}
			else
			{
				statusText = "開放中";
				statusColor = "00FF00";
			}

			// 計算到週六 00:00 的剩餘時間
			final Calendar saturday = (Calendar) now.clone();
			final int daysToSaturday = Calendar.SATURDAY - dayOfWeek;
			saturday.add(Calendar.DAY_OF_MONTH, daysToSaturday);
			saturday.set(Calendar.HOUR_OF_DAY, 0);
			saturday.set(Calendar.MINUTE, 0);
			saturday.set(Calendar.SECOND, 0);
			remainingTime = formatDuration(saturday.getTimeInMillis() - now.getTimeInMillis());
		}

		final int playerCount = (paganInstance != null) ? paganInstance.getPlayers().size() : 0;

		content = content.replace("%status%", statusText);
		content = content.replace("%statusColor%", statusColor);
		content = content.replace("%remainingTime%", remainingTime);
		content = content.replace("%playerCount%", String.valueOf(playerCount));

		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(content);
		player.sendPacket(msg);
	}

	private String handleEnterFromOutside(Player player)
	{
		if ((player.getLevel() < 85) || (player.getLevel() > 99))
		{
			player.sendMessage("等級不符合要求（需要 85~99 級）。");
			return null;
		}

		// 用 Calendar 判斷是否在開放時段
		final Calendar now = Calendar.getInstance();
		final int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
		final boolean isOpenPeriod = (dayOfWeek >= Calendar.MONDAY) && (dayOfWeek <= Calendar.FRIDAY);
		if (!isOpenPeriod)
		{
			player.sendMessage("異教神殿目前已關閉，每週一 00:00 重新開放。");
			return null;
		}

		final Instance paganInstance = findPaganInstance();
		if (paganInstance == null)
		{
			player.sendMessage("異教神殿尚未開放，請稍後再試。");
			return null;
		}

		player.teleToLocation(-16364, -40790, -10702, paganInstance);
		return null;
	}

	private String formatDuration(long ms)
	{
		final long totalSeconds = ms / 1000;
		final long days = totalSeconds / 86400;
		final long hours = (totalSeconds % 86400) / 3600;
		final long minutes = (totalSeconds % 3600) / 60;
		final StringBuilder sb = new StringBuilder();
		if (days > 0)
		{
			sb.append(days).append(" 天 ");
		}
		if (hours > 0)
		{
			sb.append(hours).append(" 小時 ");
		}
		sb.append(minutes).append(" 分鐘");
		return sb.toString();
	}

	private Instance findPaganInstance()
	{
		for (Instance instance : InstanceManager.getInstance().getInstances())
		{
			if (instance.getTemplateId() == PaganTempleManager.INSTANCE_TEMPLATE_ID)
			{
				return instance;
			}
		}
		return null;
	}

	public static void main(String[] args)
	{
		new PaganTempleEliNpc();
	}
}
