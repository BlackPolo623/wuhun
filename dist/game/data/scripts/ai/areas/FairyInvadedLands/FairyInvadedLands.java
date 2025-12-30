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
package ai.areas.FairyInvadedLands;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.spawns.SpawnGroup;
import org.l2jmobius.gameserver.model.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * 精靈入侵領地事件
 * 定時檢查擊殺數量，達標則生成世界Boss
 * @author 黑普羅
 */
public class FairyInvadedLands extends Script
{
	private static final Logger LOGGER = Logger.getLogger(FairyInvadedLands.class.getName());

	// ==================== 配置區域 ====================
	// Boss設定
	private static final int BOSS_1_ID = 22800; // 第一個Boss ID (Lycaria)
	private static final int BOSS_2_ID = 22802; // 第二個Boss ID (Lyansus)
	private static final String BOSS_1_SPAWN_NAME = "里卡里亞"; // Boss1 Spawn名稱
	private static final String BOSS_2_SPAWN_NAME = "里安甚司"; // Boss2 Spawn名稱

	// 怪物列表 - 擊殺這些怪物會累積計數
	private static final int[] TARGET_MONSTERS =
			{
					22789, // 精靈戰士
					22790, // 精靈弓手
					22791, // 精靈法師
					22792, // 精靈騎士
					22793, // 精靈守衛
					22794, // 精靈長老
					22795, // 精靈祭司
					22796, // 精靈精銳
					22797, // 精靈隊長
					22798, // 精靈將軍
					22799, // 精靈王子
					22801  // 精靈公主
			};

	// 【重要】Boss生成時間點設定 (24小時制，可設定任意數量)
	// 格式：{小時, 分鐘} - 例如 {14, 0} 代表14:00，{19, 30} 代表19:30
	private static final int[][] SPAWN_TIMES =
			{
					{6, 0},  // 10:00
					{12, 0},  // 14:00
					{18, 0},  // 18:00
					{0, 0}
			};

	// 區域限制設定 (如果需要限制在特定區域內擊殺才計數)
	private static final boolean ENABLE_ZONE_CHECK = false; // 是否啟用區域檢查
	private static final int MIN_X = -50000; // 區域最小X座標
	private static final int MAX_X = 50000;  // 區域最大X座標
	private static final int MIN_Y = -50000; // 區域最小Y座標
	private static final int MAX_Y = 50000;  // 區域最大Y座標
	private static final int MIN_Z = -5000;  // 區域最小Z座標
	private static final int MAX_Z = 5000;   // 區域最大Z座標

	// 事件設定
	private static final int REQUIRED_KILLS = 500; // 需要擊殺的怪物數量
	private static final int BOSS_DESPAWN_TIME = 3600000; // Boss存在時間(毫秒) 1小時
	private static final int BOSS_2_DELAY = 15000; // 第二個Boss延遲生成時間(毫秒)
	private static final long CHECK_INTERVAL = 60000; // 檢查間隔(毫秒) 每1分鐘檢查一次

	// 訊息設定
	private static final String MSG_KILL_COUNT = "擊殺計數: %d /%d (下次檢查時間: %s)"; // 擊殺計數訊息
	private static final String MSG_BOSS_1_SPAWN = "精靈的魔法吸引了  里卡里亞  現身！"; // Boss1生成訊息
	private static final String MSG_BOSS_2_SPAWN = "精靈的魔法吸引了  里安甚司  現身！"; // Boss2生成訊息
	private static final String MSG_BOSS_DEFEATED = "%s 擊敗了入侵領地的守護者！"; // Boss被擊敗訊息
	private static final String MSG_OUT_OF_ZONE = "你不在事件區域內，擊殺不計數！"; // 區域外擊殺訊息
	private static final String MSG_TIME_CHECK = "定時檢查: 當前 %02d:%02d，擊殺數 %d/%d"; // 定時檢查訊息
	private static final String MSG_SPAWN_TRIGGERED = "達到擊殺數要求！準備生成Boss..."; // 觸發生成訊息
	private static final String MSG_KILLS_NOT_ENOUGH = "擊殺數不足！目前: %d/%d"; // 擊殺數不足訊息
	private static final String ANNOUNCER_NAME = "精靈侵略公告"; // 公告者名稱

	// ==================== 系統變數 ====================
	private static final SpawnTemplate BOSS_1_SPAWN = SpawnData.getInstance().getSpawnByName("Lycaria");
	private static final SpawnTemplate BOSS_2_SPAWN = SpawnData.getInstance().getSpawnByName("Lyansus");
	private static final AtomicInteger KILL_COUNTER = new AtomicInteger();
	private static boolean BOSS_1_SPAWNED = false;
	private static boolean BOSS_2_SPAWNED = false;
	private static boolean EVENT_ACTIVE = true; // 事件是否啟用
	private int _lastCheckedMinute = -1; // 記錄上次檢查的分鐘，避免重複觸發

	public FairyInvadedLands()
	{
		addKillId(BOSS_1_ID, BOSS_2_ID);
		addKillId(TARGET_MONSTERS);

		// 啟動定時檢查
		startQuestTimer("time_check", CHECK_INTERVAL, null, null, true);

		// 輸出設定的時間點
		logSpawnTimes();
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "time_check":
			{
				if (!EVENT_ACTIVE)
				{
					return null;
				}

				// 檢查當前時間是否符合設定的時間點
				final Calendar calendar = Calendar.getInstance();
				final int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
				final int currentMinute = calendar.get(Calendar.MINUTE);

				// 避免在同一分鐘內重複檢查
				final int currentTimeKey = currentHour * 100 + currentMinute;
				if (_lastCheckedMinute == currentTimeKey)
				{
					return null;
				}

				// 檢查是否為設定的時間點
				for (int[] spawnTime : SPAWN_TIMES)
				{
					final int targetHour = spawnTime[0];
					final int targetMinute = spawnTime[1];

					if (currentHour == targetHour && currentMinute == targetMinute)
					{
						_lastCheckedMinute = currentTimeKey;

						final int currentKills = KILL_COUNTER.get();
						LOGGER.info(String.format(MSG_TIME_CHECK, currentHour, currentMinute, currentKills, REQUIRED_KILLS));

						// 檢查擊殺數是否達標
						if (currentKills >= REQUIRED_KILLS && !BOSS_1_SPAWNED && !BOSS_2_SPAWNED)
						{
							LOGGER.info(MSG_SPAWN_TRIGGERED);
							Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, ANNOUNCER_NAME, MSG_SPAWN_TRIGGERED));
							startQuestTimer("spawn_bosses", 3000, null, null);
						}
						else if (BOSS_1_SPAWNED || BOSS_2_SPAWNED)
						{
							LOGGER.info("定時檢查: Boss已經生成中，跳過本次檢查");
						}
						else
						{
							LOGGER.info("定時檢查: 擊殺數不足，需要 " + REQUIRED_KILLS + " 但目前只有 " + currentKills);
							Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, ANNOUNCER_NAME, String.format(MSG_KILLS_NOT_ENOUGH, currentKills, REQUIRED_KILLS)));
						}

						break;
					}
				}
				break;
			}
			case "announce_boss_1":
			{
				Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, ANNOUNCER_NAME, MSG_BOSS_1_SPAWN));
				break;
			}
			case "announce_boss_2":
			{
				Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, ANNOUNCER_NAME, MSG_BOSS_2_SPAWN));
				break;
			}
			case "spawn_bosses":
			{
				// 生成第一個Boss
				if (!BOSS_1_SPAWNED)
				{
					BOSS_1_SPAWNED = true;
					LOGGER.info("精靈入侵領地: 生成 " + BOSS_1_SPAWN_NAME);

					if (BOSS_1_SPAWN != null)
					{
						BOSS_1_SPAWN.getGroups().forEach(SpawnGroup::spawnAll);
						startQuestTimer("announce_boss_1", 1000, null, null);
						startQuestTimer("despawn_boss_1", BOSS_DESPAWN_TIME, null, null);
					}
					else
					{
						LOGGER.warning("精靈入侵領地: 找不到 " + BOSS_1_SPAWN_NAME + " 的Spawn設定！");
					}
				}

				// 生成第二個Boss
				if (!BOSS_2_SPAWNED)
				{
					BOSS_2_SPAWNED = true;
					LOGGER.info("精靈入侵領地: 生成 " + BOSS_2_SPAWN_NAME);

					if (BOSS_2_SPAWN != null)
					{
						startQuestTimer("announce_boss_2", BOSS_2_DELAY, null, null);
						BOSS_2_SPAWN.getGroups().forEach(SpawnGroup::spawnAll);
						startQuestTimer("despawn_boss_2", BOSS_DESPAWN_TIME, null, null);
					}
					else
					{
						LOGGER.warning("精靈入侵領地: 找不到 " + BOSS_2_SPAWN_NAME + " 的Spawn設定！");
					}
				}

				// 重置計數器
				KILL_COUNTER.set(0);
				break;
			}
			case "despawn_boss_1":
			{
				if (BOSS_1_SPAWN != null)
				{
					BOSS_1_SPAWN.getGroups().forEach(SpawnGroup::despawnAll);
					LOGGER.info("精靈入侵領地: " + BOSS_1_SPAWN_NAME + " 已消失");
				}
				BOSS_1_SPAWNED = false;
				break;
			}
			case "despawn_boss_2":
			{
				if (BOSS_2_SPAWN != null)
				{
					BOSS_2_SPAWN.getGroups().forEach(SpawnGroup::despawnAll);
					LOGGER.info("精靈入侵領地: " + BOSS_2_SPAWN_NAME + " 已消失");
				}
				BOSS_2_SPAWNED = false;
				break;
			}
		}

		return super.onEvent(event, npc, player);
	}

	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int npcId = npc.getId();

		// 檢查是否為Boss
		if (npcId == BOSS_1_ID)
		{
			Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, ANNOUNCER_NAME, String.format(MSG_BOSS_DEFEATED, killer.getName())));
			startQuestTimer("despawn_boss_1", 1000, null, null);
			return;
		}
		else if (npcId == BOSS_2_ID)
		{
			Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, ANNOUNCER_NAME, String.format(MSG_BOSS_DEFEATED, killer.getName())));
			startQuestTimer("despawn_boss_2", 1000, null, null);
			return;
		}

		// 檢查是否為目標怪物
		boolean isTargetMonster = false;
		for (int monsterId : TARGET_MONSTERS)
		{
			if (npcId == monsterId)
			{
				isTargetMonster = true;
				break;
			}
		}

		if (!isTargetMonster)
		{
			return;
		}

		// 區域檢查
		if (ENABLE_ZONE_CHECK && !isInEventZone(killer))
		{
			killer.sendMessage(MSG_OUT_OF_ZONE);
			return;
		}

		// 增加擊殺計數
		final int count = KILL_COUNTER.incrementAndGet();
		final String nextCheckTime = getNextCheckTime();
		killer.sendMessage(String.format(MSG_KILL_COUNT, count, REQUIRED_KILLS, nextCheckTime));
	}

	/**
	 * 輸出設定的時間點到Log
	 */
	private void logSpawnTimes()
	{
		final StringBuilder sb = new StringBuilder("精靈入侵領地: 設定的檢查時間點 - ");
		for (int i = 0; i < SPAWN_TIMES.length; i++)
		{
			sb.append(String.format("%02d:%02d", SPAWN_TIMES[i][0], SPAWN_TIMES[i][1]));
			if (i < SPAWN_TIMES.length - 1)
			{
				sb.append(", ");
			}
		}
		LOGGER.info(sb.toString());
		LOGGER.info("精靈入侵領地: 每 " + (CHECK_INTERVAL / 1000) + " 秒檢查一次時間");
	}

	/**
	 * 獲取下一個檢查時間點
	 */
	private String getNextCheckTime()
	{
		final Calendar calendar = Calendar.getInstance();
		final int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
		final int currentMinute = calendar.get(Calendar.MINUTE);
		final int currentTotalMinutes = currentHour * 60 + currentMinute;

		int nearestTotalMinutes = Integer.MAX_VALUE;

		for (int[] spawnTime : SPAWN_TIMES)
		{
			final int targetTotalMinutes = spawnTime[0] * 60 + spawnTime[1];

			// 找出最近的未來時間點
			if (targetTotalMinutes > currentTotalMinutes)
			{
				if (targetTotalMinutes < nearestTotalMinutes)
				{
					nearestTotalMinutes = targetTotalMinutes;
				}
			}
		}

		// 如果今天沒有更多時間點，就找明天第一個時間點
		if (nearestTotalMinutes == Integer.MAX_VALUE)
		{
			nearestTotalMinutes = SPAWN_TIMES[0][0] * 60 + SPAWN_TIMES[0][1];
			return String.format("明天 %02d:%02d", SPAWN_TIMES[0][0], SPAWN_TIMES[0][1]);
		}

		final int nextHour = nearestTotalMinutes / 60;
		final int nextMinute = nearestTotalMinutes % 60;
		return String.format("今天 %02d:%02d", nextHour, nextMinute);
	}

	/**
	 * 檢查玩家是否在事件區域內
	 */
	private boolean isInEventZone(Player player)
	{
		final int x = player.getX();
		final int y = player.getY();
		final int z = player.getZ();

		return (x >= MIN_X && x <= MAX_X) &&
				(y >= MIN_Y && y <= MAX_Y) &&
				(z >= MIN_Z && z <= MAX_Z);
	}

	public static void main(String[] args)
	{
		new FairyInvadedLands();
	}
}