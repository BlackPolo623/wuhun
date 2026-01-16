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
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.data.xml.TeleportListData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.variables.GlobalVariables;
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
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.PopupEventHud;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Mobius
 * @author 黑普羅 (改進版 - 添加重載狀態恢復)
 * @URL https://eu.4game.com/patchnotes/lineage2essence/440/
 */
public class SevenSignsDungeons extends Script
{
	private static final Logger LOGGER = Logger.getLogger(SevenSignsDungeons.class.getName());

	// ==================== 活動時間配置 ====================
	private static final int ANNOUNCE_HOUR = 20;           // 公告時間：20點
	private static final int ANNOUNCE_MINUTE = 55;         // 公告時間：55分
	private static final int ANNOUNCE_DELAY_MS = 300000;   // 預告時間：5分鐘後開放 (毫秒)
	private static final int DUNGEON_DURATION_MS = 82800000; // 地下城開放時長：23小時 (毫秒)

	// ==================== 提示訊息配置 ====================
	private static final int SECOND_MESSAGE_DELAY_MS = 16000; // 第二條公告延遲：16秒 (毫秒)
	private static final int SCREEN_MESSAGE_DURATION_MS = 10000; // 屏幕訊息顯示時長：10秒 (毫秒)

	// ==================== NPC 刷新位置配置 ====================
	private static final Map<Integer, Location> ZIGGURAT_SPAWNS = new HashMap<>();
	static
	{
		ZIGGURAT_SPAWNS.put(31075, new Location(148453, 211885, -2182));
		ZIGGURAT_SPAWNS.put(31076, new Location(12941, -248481, -9555));

		ZIGGURAT_SPAWNS.put(31077, new Location(148236, 211948, -2181));
		ZIGGURAT_SPAWNS.put(31078, new Location(-21392, 77376, -5168));

		ZIGGURAT_SPAWNS.put(31079, new Location(148675, 212016, -2175));
		ZIGGURAT_SPAWNS.put(31080, new Location(140784, 79680, -5424));
	}

	// ==================== 傳送點配置 ====================
	private static final Map<Integer, Location> ENTER_TELEPORTS = new HashMap<>();
	static
	{
		ENTER_TELEPORTS.put(31075, new Location(12941, -248481, -9555));
		ENTER_TELEPORTS.put(31077, new Location(-21666, 77376, -5168));
		ENTER_TELEPORTS.put(31079, new Location(140613, 79692, -5424));
	}

	private static final Map<Integer, Location> EXIT_TELEPORTS = new HashMap<>();
	static
	{
		EXIT_TELEPORTS.put(31076, new Location(148453, 211885, -2182));
		EXIT_TELEPORTS.put(31078, new Location(148236, 211948, -2181));
		EXIT_TELEPORTS.put(31080, new Location(148675, 212016, -2175));
	}

	private static final Location GIRAN_TELEPORT = TeleportListData.getInstance().getTeleport(25).getLocation();
	private static final Location ADEN_TELEPORT = TeleportListData.getInstance().getTeleport(321).getLocation();

	// ==================== 地下城刷怪配置 ====================
	private static final SpawnTemplate PATRIOT_SPAWNS = SpawnData.getInstance().getSpawnByName("NecropolisOfThePatriots");
	private static final SpawnTemplate FORBIDDEN_SPAWNS = SpawnData.getInstance().getSpawnByName("CatacombOfTheForbiddenPath");
	private static final SpawnTemplate WITCHES_SPAWNS = SpawnData.getInstance().getSpawnByName("CatacombOfTheWitch");

	// ==================== GlobalVariables 鍵名 ====================
	private static final String VAR_ACTIVE = "SevenSignsDungeons_Active";
	private static final String VAR_END_TIME = "SevenSignsDungeons_EndTime";

	// ==================== 運行狀態 ====================
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

		// ========== 新增：檢查並恢復狀態 ==========
		checkAndRestoreState();
	}

	/**
	 * 檢查並恢復當前應有的狀態
	 * 這樣重載腳本后不會丟失狀態
	 */
	private void checkAndRestoreState()
	{
		final long currentTime = System.currentTimeMillis();

		// 從 GlobalVariables 讀取狀態（用 int 代替 boolean：1=true, 0=false）
		final int wasActiveInt = GlobalVariables.getInt(VAR_ACTIVE, 0);
		final boolean wasActive = (wasActiveInt == 1);
		final long endTime = GlobalVariables.getLong(VAR_END_TIME, 0);

		// 如果之前是活動狀態，且結束時間還未到
		if (wasActive && (endTime > 0) && (currentTime < endTime))
		{
			// 說明應該處於開啟狀態
			LOGGER.info("[SevenSignsDungeons] 檢測到應該處於活動狀態，正在恢復...");
			_active = true;

			// 恢復怪物刷新
			PATRIOT_SPAWNS.getGroups().forEach(SpawnGroup::spawnAll);
			FORBIDDEN_SPAWNS.getGroups().forEach(SpawnGroup::spawnAll);
			WITCHES_SPAWNS.getGroups().forEach(SpawnGroup::spawnAll);

			// 發送HUD給所有在線玩家
			Broadcast.toAllOnlinePlayers(new PopupEventHud(PopupEventHud.CATACOMBS, true));

			// 計算剩餘時間並設置關閉定時器
			long remainingTime = endTime - currentTime;
			LOGGER.info("[SevenSignsDungeons] 活動狀態已恢復，剩餘時間: " + (remainingTime / 1000 / 60) + " 分鐘");

			ThreadPool.schedule(() ->
			{
				_active = false;
				// 清除 GlobalVariables 中的狀態
				GlobalVariables.set(VAR_ACTIVE, 0);  // 0 = false
				GlobalVariables.set(VAR_END_TIME, 0L);

				Broadcast.toAllOnlinePlayers(new PopupEventHud(PopupEventHud.CATACOMBS, false));
				PATRIOT_SPAWNS.getGroups().forEach(SpawnGroup::despawnAll);
				FORBIDDEN_SPAWNS.getGroups().forEach(SpawnGroup::despawnAll);
				WITCHES_SPAWNS.getGroups().forEach(SpawnGroup::despawnAll);
				scheduleNext();
			}, remainingTime);
		}
		else
		{
			// 不在活動時間內，清除可能存在的舊狀態
			if (wasActive)
			{
				LOGGER.info("[SevenSignsDungeons] 清除過期的活動狀態");
				GlobalVariables.set(VAR_ACTIVE, 0);  // 0 = false
				GlobalVariables.set(VAR_END_TIME, 0L);
			}

			// 正常調度下次活動
			LOGGER.info("[SevenSignsDungeons] 當前不在活動時間，正在調度下次活動...");
			scheduleNext();
		}
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
		final Calendar calendar = Calendar.getInstance();
		final int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
		final int currentMinute = calendar.get(Calendar.MINUTE);
		final int currentTimeMinutes = (currentHour * 60) + currentMinute;

		// 活动时间：21:00 - 次日 20:00
		final int activityStartMinutes = 21 * 60;  // 21:00 = 1260分钟
		final int activityEndMinutes = 20 * 60;    // 20:00 = 1200分钟

		// 检查当前是否在活动时间内 (21:00 - 23:59 或 00:00 - 20:00)
		boolean inActivityTime = (currentTimeMinutes >= activityStartMinutes) || (currentTimeMinutes < activityEndMinutes);

		if (inActivityTime)
		{
			// 当前在活动时间内，立即开启
			LOGGER.info("[SevenSignsDungeons] 检测到当前在活动时间内，立即开启活动");
			enableDungeons();
			return;
		}

		// 不在活动时间内，调度下次公告
		calendar.set(Calendar.HOUR_OF_DAY, 20);
		calendar.set(Calendar.MINUTE, 55);
		calendar.set(Calendar.SECOND, 0);

		if (calendar.getTimeInMillis() < System.currentTimeMillis())
		{
			calendar.add(Calendar.DAY_OF_YEAR, 1);
		}

		final long initialDelay = calendar.getTimeInMillis() - System.currentTimeMillis();
		LOGGER.info("[SevenSignsDungeons] 下次公告時間: " + calendar.getTime());
		LOGGER.info("[SevenSignsDungeons] 距離下次公告還有: " + (initialDelay / 1000 / 60) + " 分鐘");

		ThreadPool.schedule(() ->
		{
			Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.ANNOUNCEMENT, "系統公告", "七印地下城將在5分鐘後開啟！"));
			ThreadPool.schedule(this::enableDungeons, 5 * 60 * 1000);
		}, initialDelay);
	}

	private void announce()
	{
		LOGGER.info("[SevenSignsDungeons] 發送活動預告公告");

		Broadcast.toAllOnlinePlayers(new PopupEventHud(PopupEventHud.CATACOMBS, true));
		Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(NpcStringId.IN_5_MINUTES_MONSTERS_WILL_APPEAR_IN_A_SEVEN_SIGNS_DUNGEON_YOU_HAVE_30_MINUTES_TO_KILL_THEM, ExShowScreenMessage.TOP_CENTER, SCREEN_MESSAGE_DURATION_MS, true));
		ThreadPool.schedule(() -> Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(NpcStringId.USE_TELEPORT_TO_GO_TO_A_SEVEN_SIGNS_DUNGEON, ExShowScreenMessage.TOP_CENTER, SCREEN_MESSAGE_DURATION_MS, true)), SECOND_MESSAGE_DELAY_MS);
		ThreadPool.schedule(() -> enableDungeons(), ANNOUNCE_DELAY_MS);
	}

	private void enableDungeons()
	{
		_active = true;
		final long endTime = System.currentTimeMillis() + DUNGEON_DURATION_MS;

		// 保存狀態到 GlobalVariables（持久化）
		// 使用 int：1 = true, 0 = false
		GlobalVariables.set(VAR_ACTIVE, 1);
		GlobalVariables.set(VAR_END_TIME, endTime);

		LOGGER.info("[SevenSignsDungeons] 地下城活動已開啟");
		LOGGER.info("[SevenSignsDungeons] 活動將持續: " + (DUNGEON_DURATION_MS / 1000 / 60 / 60) + " 小時");

		PATRIOT_SPAWNS.getGroups().forEach(SpawnGroup::spawnAll);
		FORBIDDEN_SPAWNS.getGroups().forEach(SpawnGroup::spawnAll);
		WITCHES_SPAWNS.getGroups().forEach(SpawnGroup::spawnAll);

		// Disable dungeons.
		ThreadPool.schedule(() ->
		{
			_active = false;

			// 清除 GlobalVariables 中的狀態
			GlobalVariables.set(VAR_ACTIVE, 0);  // 0 = false
			GlobalVariables.set(VAR_END_TIME, 0L);

			LOGGER.info("[SevenSignsDungeons] 地下城活動已關閉");

			Broadcast.toAllOnlinePlayers(new PopupEventHud(PopupEventHud.CATACOMBS, false));

			PATRIOT_SPAWNS.getGroups().forEach(SpawnGroup::despawnAll);
			FORBIDDEN_SPAWNS.getGroups().forEach(SpawnGroup::despawnAll);
			WITCHES_SPAWNS.getGroups().forEach(SpawnGroup::despawnAll);

			scheduleNext();
		}, DUNGEON_DURATION_MS);
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