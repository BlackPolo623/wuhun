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
import java.util.HashMap;
import java.util.Map;
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
 * 定時檢查擊殺數量,達標則生成世界Boss
 * 支援每個時間點配置生成次數限制
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

	// 【重要】Boss生成時間點設定 (24小時制,可設定任意數量)
	// 格式:{小時, 分鐘} - 例如 {14, 0} 代表14:00,{19, 30} 代表19:30
	private static final int[][] SPAWN_TIMES =
			{
					{6, 0},   // 6:00
					{12, 0},  // 12:00
					{18, 0},  // 18:00
					{0, 0}    // 0:00
			};

	// ========== 【新增】每個時間點最多生成次數 ==========
	// 設定為1表示每個時間點只生成一次,設定為2表示可以生成兩次,依此類推
	// 設定為0或負數表示無限制(不推薦)
	private static final int MAX_SPAWNS_PER_TIMEPOINT = 1;

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
	private static final String MSG_KILL_COUNT = "擊殺計數: %d/%d (下次檢查: %s)";
	private static final String MSG_BOSS_1_SPAWN = "精靈的魔法吸引了  里卡里亞  現身!";
	private static final String MSG_BOSS_2_SPAWN = "精靈的魔法吸引了  里安甚司  現身!";
	private static final String MSG_BOSS_DEFEATED = "%s 擊敗了入侵領地的守護者!";
	private static final String MSG_OUT_OF_ZONE = "你不在事件區域內,擊殺不計數!";
	private static final String MSG_TIME_CHECK = "定時檢查: 當前 %02d:%02d,擊殺數 %d/%d";
	private static final String MSG_SPAWN_TRIGGERED = "達到擊殺數要求!準備生成Boss... (本時段第%d/%d次)";
	private static final String MSG_KILLS_NOT_ENOUGH = "擊殺數不足!目前: %d/%d";
	private static final String MSG_MAX_SPAWNS_REACHED = "本時段Boss生成次數已達上限 (%d/%d次),請等待下個時間點";
	private static final String ANNOUNCER_NAME = "精靈侵略公告";

	// ==================== 系統變數 ====================
	private static final SpawnTemplate BOSS_1_SPAWN = SpawnData.getInstance().getSpawnByName("Lycaria");
	private static final SpawnTemplate BOSS_2_SPAWN = SpawnData.getInstance().getSpawnByName("Lyansus");
	private static final AtomicInteger KILL_COUNTER = new AtomicInteger();
	private static boolean BOSS_1_SPAWNED = false;
	private static boolean BOSS_2_SPAWNED = false;
	private static boolean EVENT_ACTIVE = true;

	// ========== 【新增】記錄每個時間點的生成次數 ==========
	private static final Map<String, Integer> _spawnCountToday = new HashMap<>();
	private static int _lastResetDay = -1;
	private static String _currentTimeSlot = null; // 當前時間段標記

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

				final Calendar calendar = Calendar.getInstance();
				final int currentDay = calendar.get(Calendar.DAY_OF_YEAR);
				final int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
				final int currentMinute = calendar.get(Calendar.MINUTE);

				// 每天重置記錄
				if (_lastResetDay != currentDay)
				{
					_spawnCountToday.clear();
					_currentTimeSlot = null;
					_lastResetDay = currentDay;
					KILL_COUNTER.set(0);
					LOGGER.info("精靈入侵領地: 新的一天開始,重置所有記錄");
				}

				// 檢查是否為設定的時間點
				for (int[] spawnTime : SPAWN_TIMES)
				{
					final int targetHour = spawnTime[0];
					final int targetMinute = spawnTime[1];

					if (currentHour == targetHour && currentMinute == targetMinute)
					{
						final String timeKey = String.format("%02d:%02d", targetHour, targetMinute);

						// 檢查是否仍在當前時間段
						if (!timeKey.equals(_currentTimeSlot))
						{
							// 進入新時間段
							_currentTimeSlot = timeKey;
							LOGGER.info("精靈入侵領地: 進入時間段 " + timeKey);
						}

						final int currentKills = KILL_COUNTER.get();
						final int currentSpawnCount = _spawnCountToday.getOrDefault(timeKey, 0);

						LOGGER.info(String.format(MSG_TIME_CHECK, currentHour, currentMinute, currentKills, REQUIRED_KILLS));
						LOGGER.info("精靈入侵領地: 時段 " + timeKey + " 已生成次數: " + currentSpawnCount + "/" + MAX_SPAWNS_PER_TIMEPOINT);

						// ========== 【核心邏輯】檢查是否可以生成Boss ==========
						boolean canSpawn = true;
						String failReason = "";

						// 1. 檢查Boss是否已經存在
						if (BOSS_1_SPAWNED || BOSS_2_SPAWNED)
						{
							canSpawn = false;
							failReason = "Boss已經生成中";
						}
						// 2. 檢查擊殺數是否達標
						else if (currentKills < REQUIRED_KILLS)
						{
							canSpawn = false;
							failReason = "擊殺數不足 (" + currentKills + "/" + REQUIRED_KILLS + ")";
						}
						// 3. 檢查本時段生成次數是否達到上限
						else if (MAX_SPAWNS_PER_TIMEPOINT > 0 && currentSpawnCount >= MAX_SPAWNS_PER_TIMEPOINT)
						{
							canSpawn = false;
							failReason = "本時段生成次數已達上限 (" + currentSpawnCount + "/" + MAX_SPAWNS_PER_TIMEPOINT + ")";
						}

						if (canSpawn)
						{
							// 可以生成Boss
							final int newSpawnCount = currentSpawnCount + 1;
							_spawnCountToday.put(timeKey, newSpawnCount);

							LOGGER.info("精靈入侵領地: " + MSG_SPAWN_TRIGGERED);
							Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, ANNOUNCER_NAME,
									String.format(MSG_SPAWN_TRIGGERED, newSpawnCount, MAX_SPAWNS_PER_TIMEPOINT)));

							// 設置Boss生成標誌
							BOSS_1_SPAWNED = true;
							BOSS_2_SPAWNED = true;

							// 延遲3秒後生成Boss
							startQuestTimer("spawn_bosses", 3000, null, null);
						}
						else
						{
							// 不能生成,記錄原因
							LOGGER.info("精靈入侵領地: 無法生成Boss - " + failReason);

							// 如果是次數達到上限,發送公告
							if (MAX_SPAWNS_PER_TIMEPOINT > 0 && currentSpawnCount >= MAX_SPAWNS_PER_TIMEPOINT)
							{
								Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, ANNOUNCER_NAME,
										String.format(MSG_MAX_SPAWNS_REACHED, currentSpawnCount, MAX_SPAWNS_PER_TIMEPOINT)));
							}
							else if (currentKills < REQUIRED_KILLS && currentSpawnCount < MAX_SPAWNS_PER_TIMEPOINT)
							{
								// 擊殺數不足且還有生成機會
								Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, ANNOUNCER_NAME,
										String.format(MSG_KILLS_NOT_ENOUGH, currentKills, REQUIRED_KILLS)));
							}
						}

						break;
					}
				}

				// ========== 檢查是否離開時間段 ==========
				boolean inTimeSlot = false;
				for (int[] spawnTime : SPAWN_TIMES)
				{
					if (currentHour == spawnTime[0] && currentMinute == spawnTime[1])
					{
						inTimeSlot = true;
						break;
					}
				}

				if (!inTimeSlot && _currentTimeSlot != null)
				{
					LOGGER.info("精靈入侵領地: 離開時間段 " + _currentTimeSlot);
					_currentTimeSlot = null;
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
				LOGGER.info("精靈入侵領地: 開始生成Boss");

				// 生成第一個Boss
				if (BOSS_1_SPAWNED && BOSS_1_SPAWN != null)
				{
					LOGGER.info("精靈入侵領地: 生成 " + BOSS_1_SPAWN_NAME);
					BOSS_1_SPAWN.getGroups().forEach(SpawnGroup::spawnAll);
					startQuestTimer("announce_boss_1", 1000, null, null);
					startQuestTimer("despawn_boss_1", BOSS_DESPAWN_TIME, null, null);
				}
				else if (BOSS_1_SPAWNED)
				{
					LOGGER.warning("精靈入侵領地: 找不到 " + BOSS_1_SPAWN_NAME + " 的Spawn設定!");
					BOSS_1_SPAWNED = false;
				}

				// 生成第二個Boss
				if (BOSS_2_SPAWNED && BOSS_2_SPAWN != null)
				{
					LOGGER.info("精靈入侵領地: 生成 " + BOSS_2_SPAWN_NAME);
					startQuestTimer("announce_boss_2", BOSS_2_DELAY, null, null);
					BOSS_2_SPAWN.getGroups().forEach(SpawnGroup::spawnAll);
					startQuestTimer("despawn_boss_2", BOSS_DESPAWN_TIME, null, null);
				}
				else if (BOSS_2_SPAWNED)
				{
					LOGGER.warning("精靈入侵領地: 找不到 " + BOSS_2_SPAWN_NAME + " 的Spawn設定!");
					BOSS_2_SPAWNED = false;
				}

				// 重置計數器
				KILL_COUNTER.set(0);
				LOGGER.info("精靈入侵領地: Boss生成完成,擊殺計數器已重置");
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
			Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, ANNOUNCER_NAME,
					String.format(MSG_BOSS_DEFEATED, killer.getName())));
			startQuestTimer("despawn_boss_1", 1000, null, null);
			return;
		}
		else if (npcId == BOSS_2_ID)
		{
			Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, ANNOUNCER_NAME,
					String.format(MSG_BOSS_DEFEATED, killer.getName())));
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

		// ========== 【新增】顯示當前時段的生成狀態 ==========
		String statusMsg = String.format(MSG_KILL_COUNT, count, REQUIRED_KILLS, nextCheckTime);

		if (_currentTimeSlot != null)
		{
			final int currentSpawnCount = _spawnCountToday.getOrDefault(_currentTimeSlot, 0);
			if (MAX_SPAWNS_PER_TIMEPOINT > 0)
			{
				statusMsg += String.format(" | 本時段: %d/%d次", currentSpawnCount, MAX_SPAWNS_PER_TIMEPOINT);
			}
		}

		killer.sendMessage(statusMsg);
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
		LOGGER.info("精靈入侵領地: 每個時間點最多生成 " + MAX_SPAWNS_PER_TIMEPOINT + " 次Boss");
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

			if (targetTotalMinutes > currentTotalMinutes)
			{
				if (targetTotalMinutes < nearestTotalMinutes)
				{
					nearestTotalMinutes = targetTotalMinutes;
				}
			}
		}

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