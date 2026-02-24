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
package instances.LeonasDungeon;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.managers.events.LeonasDungeonManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

/**
 * @author Serenitty, Mobius
 */
public class LeonasDungeon extends InstanceScript {
	// NPC
	private static final int LEONA = 34357;

	// Skills
	private final SkillHolder FORCE_BUFF = new SkillHolder(4647, 4);
	private final SkillHolder FORCE_DEBUFF = new SkillHolder(48403, 2);
	// Misc
	private static final int EVENT_TRIGGER_LEONAS_AREA = 18108866;
	private static final int TEMPLATE_ID = 236;

	// Monster IDs
	private static final int[] MOBS_DIFFICULTY_1 = {22535, 22536, 22537}; // Lith Guard, Lith Medium, Lith Prefect
	private static final int[] MOBS_DIFFICULTY_2 = {22529, 22530, 22531}; // Nephilim Guardsman, Nephilim Cardinal, Nephilim Battlemaster
	private static final int[] MOBS_DIFFICULTY_3 = {22538, 22539, 22540}; // Lilim's Guardian, Lilim's Assassin, Lilim's Soldier

	// Scoring Configuration
	private static final int POINTS_PER_KILL = 5; // 擊殺怪物基礎分數
	private static final int POINTS_PER_MINUTE_LOW = 10; // 低難度每分鐘分數
	private static final int POINTS_PER_MINUTE_MED = 20; // 中難度每分鐘分數
	private static final int POINTS_PER_MINUTE_HIGH = 30; // 高難度每分鐘分數
	private static final int POINTS_DEDUCT_PER_DEATH = 50; // 每次死亡扣分
	private static final int POINTS_DEDUCT_HP_THRESHOLD = 50; // HP低於此%時扣分
	private static final int POINTS_DEDUCT_LOW_HP = 200; // HP過低時扣分
	private static final double MOB_POWER_INCREASE = 1.05; // 怪物每次重生增強5%

	// Tiered Milestone System (無上限階梯式獎勵)
	// 1000-1999: 每100隻 +5分
	// 2000-2999: 每100隻 +7分
	// 3000-3999: 每100隻 +9分
	// 以此類推，每1000隻增加2分獎勵

	// Random Boss Configuration (隨機BOSS配置)
	private static final double BOSS_SPAWN_CHANCE = 0.005; // 千分之五機率 (0.5%)
	private static final int POINTS_PER_RANDOM_BOSS = 500; // 每擊殺一隻隨機BOSS獎勵500分

	// Boss IDs (四個隨機BOSS之一)
	private static final int[] RANDOM_BOSS_IDS = {
			50041, // BOSS ID 1 (請修改為實際BOSS ID)
			50042, // BOSS ID 2
			50043, // BOSS ID 3
			50044  // BOSS ID 4
	};

	// Boss Spawn Locations (BOSS出生座標 - X, Y, Z, Heading)
	private static final int[][] BOSS_SPAWN_LOCATIONS = {
			{-46752, 243871, -7856, 0}, // BOSS 1 出生點
			{-49402, 242191, -7856, 0}, // BOSS 2 出生點
			{-51764, 245745, -7856, 0}, // BOSS 3 出生點
			{-49396, 249977, -7856, 0}  // BOSS 4 出生點
	};

	private LeonasDungeon() {
		super(TEMPLATE_ID);

		addFirstTalkId(LEONA);
		addTalkId(LEONA);
		addEnterZoneId(EVENT_TRIGGER_LEONAS_AREA);
		addInstanceEnterId(TEMPLATE_ID);
		addInstanceLeaveId(TEMPLATE_ID);
		addInstanceDestroyId(TEMPLATE_ID);

		// Register kill and spawn events for all mob types
		addKillId(MOBS_DIFFICULTY_1);
		addKillId(MOBS_DIFFICULTY_2);
		addKillId(MOBS_DIFFICULTY_3);
		addSpawnId(MOBS_DIFFICULTY_1);
		addSpawnId(MOBS_DIFFICULTY_2);
		addSpawnId(MOBS_DIFFICULTY_3);

		// Register kill events for random bosses
		addKillId(RANDOM_BOSS_IDS);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player) {
		final String html;
		switch (event) {
			case "enterInstance": {
				if (!LeonasDungeonManager.getInstance().canEnterDungeon(player)) {
					final int remaining = LeonasDungeonManager.getInstance().getRemainingEntries(player);
					player.sendMessage("你本週的進入次數已用完。剩餘次數: " + remaining + "/" + LeonasDungeonManager.MAX_WEEKLY_ENTRIES);
					return null;
				}

				if (player.isInParty()) {
					final Party party = player.getParty();
					final List<Player> members = party.getMembers();
					for (Player member : members) {
						if (!member.isInsideRadius3D(npc, 1000)) {
							player.sendMessage("玩家 " + member.getName() + " 必須靠近一些。");
						}

						if (LeonasDungeonManager.getInstance().canEnterDungeon(member)) {
							enterInstance(member, npc, TEMPLATE_ID);
							LeonasDungeonManager.getInstance().incrementEntryCount(member);
						} else {
							member.sendMessage("你本週的進入次數已用完。");
						}
					}
				} else if (player.isGM()) {
					enterInstance(player, npc, TEMPLATE_ID);
					player.sendMessage("系統：你以 GM/管理員身份進入了蕾歐娜地下城。");
				} else {
					player.sendMessage("你必須組隊才能進入。");
				}
				break;
			}
			case "setDifficulty": {
				final Instance instance = player.getInstanceWorld();
				if (instance == null) {
					return null;
				}

				// 只有隊長可以切換難度
				if (player.isInParty() && !player.getParty().isLeader(player)) {
					player.sendMessage("只有隊長可以切換難度。");
					return null;
				}

				// 難度已選定後不可變更
				final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 0);
				if (difficulty != 0) {
					player.sendMessage("難度已選定，無法變更。");
					return null;
				}

				return "setDifficulty.htm";
			}
			case "viewScoreFormula": {
				return "scoreFormula.htm";
			}
			case "showRanking": {
				showRankingPage(player);
				return null;
			}
			case "setDifficulty1": {
				final Instance instance = player.getInstanceWorld();
				if (instance == null) {
					return null;
				}

				// 只有隊長可以切換難度
				if (player.isInParty() && !player.getParty().isLeader(player)) {
					player.sendMessage("只有隊長可以切換難度。");
					return null;
				}

				// 難度已選定後不可變更
				final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 0);
				if (difficulty != 0) {
					player.sendMessage("難度已選定，無法變更。");
					return null;
				}

				instance.getParameters().set("INSTANCE_DIFFICULTY", 1);

				instance.spawnGroup("MOBS_1");
				if (!instance.getParameters().getBoolean("PlayerEnter", false)) {
					instance.setParameter("PlayerEnter", true);
					instance.setDuration(30);
					startEvent(npc, player);
				}
				break;
			}
			case "setDifficulty2": {
				final Instance instance = player.getInstanceWorld();
				if (instance == null) {
					return null;
				}

				// 只有隊長可以切換難度
				if (player.isInParty() && !player.getParty().isLeader(player)) {
					player.sendMessage("只有隊長可以切換難度。");
					return null;
				}

				// 難度已選定後不可變更
				final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 0);
				if (difficulty != 0) {
					player.sendMessage("難度已選定，無法變更。");
					return null;
				}

				instance.getParameters().set("INSTANCE_DIFFICULTY", 2);

				instance.spawnGroup("MOBS_2");
				if (!instance.getParameters().getBoolean("PlayerEnter", false)) {
					instance.setParameter("PlayerEnter", true);
					instance.setDuration(30);
					startEvent(npc, player);
				}
				break;
			}
			case "setDifficulty3": {
				final Instance instance = player.getInstanceWorld();
				if (instance == null) {
					return null;
				}

				// 只有隊長可以切換難度
				if (player.isInParty() && !player.getParty().isLeader(player)) {
					player.sendMessage("只有隊長可以切換難度。");
					return null;
				}

				// 難度已選定後不可變更
				final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 0);
				if (difficulty != 0) {
					player.sendMessage("難度已選定，無法變更。");
					return null;
				}

				instance.getParameters().set("INSTANCE_DIFFICULTY", 3);

				instance.spawnGroup("MOBS_3");
				if (!instance.getParameters().getBoolean("PlayerEnter", false)) {
					instance.setParameter("PlayerEnter", true);
					instance.setDuration(30);
					startEvent(npc, player);
				}
				break;
			}
			case "exitInstance": {
				final Instance instance = player.getInstanceWorld();
				if (instance == null) {
					break;
				}

				// 防止重複觸發離開倒數
				if (instance.getParameters().getBoolean("ExitCountdownStarted", false)) {
					player.sendMessage("已有隊員發起離開，請等待倒數結束。");
					break;
				}

				instance.setParameter("ExitCountdownStarted", true);

				// 通知全隊：5秒後離開
				instance.broadcastPacket(new ExShowScreenMessage(player.getName() + " 發起了離開副本，5秒後全隊將被傳送離開！", 5000));

				// 5秒後全隊踢出並銷毀副本
				ThreadPool.schedule(() ->
				{
					for (Player member : instance.getPlayers()) {
						instance.ejectPlayer(member);
					}
					instance.destroy();
				}, 5000);
				break;
			}
		}

		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player) {
		final Instance instance = npc.getInstanceWorld();
		if (instance == null) {
			// 主世界：顯示進入副本的頁面（含玩家資訊）
			final LeonasDungeonManager manager = LeonasDungeonManager.getInstance();
			final int bestScore = manager.getPlayerPoints(player);
			final int remaining = manager.getRemainingEntries(player);
			final String resetTime = getNextMondayResetString();

			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId(), 1);
			html.setFile(player, "data/scripts/instances/LeonasDungeon/34357-enter.htm");
			html.replace("%bestScore%", String.valueOf(bestScore));
			html.replace("%remaining%", String.valueOf(remaining));
			html.replace("%maxEntries%", String.valueOf(LeonasDungeonManager.MAX_WEEKLY_ENTRIES));
			html.replace("%resetTime%", resetTime);
			player.sendPacket(html);
			return null;
		}

		final boolean instanceStarted = instance.getParameters().getBoolean("PlayerEnter", false);
		return instanceStarted ? "34357-02.htm" : "34357-01.htm";
	}

	/**
	 * 計算下週一凌晨重置時間的格式化字串
	 */
	private String getNextMondayResetString() {
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
			calendar.add(Calendar.WEEK_OF_MONTH, 1);
		}

		long remaining = calendar.getTimeInMillis() - System.currentTimeMillis();
		long days = remaining / (24 * 60 * 60 * 1000);
		long hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
		long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);

		if (days > 0) {
			return days + "天" + hours + "時";
		} else if (hours > 0) {
			return hours + "時" + minutes + "分";
		} else {
			return minutes + "分";
		}
	}

	private void startEvent(Npc npc, Player player) {
		final Instance instance = player.getInstanceWorld();
		instance.setParameter("Leona_Running", true);
		instance.setParameter("StartTime", System.currentTimeMillis());
		player.getInstanceWorld().broadcastPacket(new ExSendUIEvent(player, false, false, (int) (instance.getRemainingTime() / 1000), 0, NpcStringId.TIME_LEFT));
		instance.broadcastPacket(new OnEventTrigger(EVENT_TRIGGER_LEONAS_AREA, true));
		if (npc.getId() == LEONA) {
			npc.setDisplayEffect(2);
		}

		// Initialize score tracking for all players
		final Collection<Player> members = instance.getPlayers();
		for (Player member : members) {
			instance.getParameters().set("Player_" + member.getObjectId() + "_Score", 0);
			instance.getParameters().set("Player_" + member.getObjectId() + "_Kills", 0);
			instance.getParameters().set("Player_" + member.getObjectId() + "_Deaths", 0);
		}

		// Schedule periodic HP check and death tracking
		final ScheduledFuture<?> scheduledTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			for (Player member : instance.getPlayers()) {
				final boolean wasDead = instance.getParameters().getBoolean("Player_" + member.getObjectId() + "_WasDead", false);
				if (member.isDead() && !wasDead) {
					// 首次偵測到死亡，計一次死亡並儲存分數快照
					instance.getParameters().set("Player_" + member.getObjectId() + "_WasDead", true);
					final int deaths = instance.getParameters().getInt("Player_" + member.getObjectId() + "_Deaths", 0) + 1;
					instance.getParameters().set("Player_" + member.getObjectId() + "_Deaths", deaths);
					saveDeathScoreSnapshot(member, instance, deaths);
				} else if (!member.isDead() && wasDead) {
					// 玩家復活，重置死亡旗標並清除死亡快照（復活後繼續計分）
					instance.getParameters().set("Player_" + member.getObjectId() + "_WasDead", false);
					instance.getParameters().set("Player_" + member.getObjectId() + "_DeathScore", -1);
				}
			}
		}, 5000, 5000);

		instance.setParameter("RankingPointTask", scheduledTask);

		// Schedule periodic score display every 1 minute
		final ScheduledFuture<?> scoreDisplayTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 1);
			final int[][] mobArrays = {MOBS_DIFFICULTY_1, MOBS_DIFFICULTY_2, MOBS_DIFFICULTY_3};
			final int[] currentMobs = mobArrays[Math.min(difficulty, 3) - 1];
			final int respawnCount = instance.getParameters().getInt("Mob_" + currentMobs[0] + "_RespawnCount", 0);
			final int strengthTier = respawnCount + 1; // 第1層 = 初始, 每次重生+1層

			for (Player member : instance.getPlayers()) {
				final int currentScore = instance.getParameters().getInt("Player_" + member.getObjectId() + "_Score", 0);
				final String message = "目前積分: " + currentScore + " | 怪物強度: 第 " + strengthTier + " 層";
				member.sendPacket(new ExShowScreenMessage(message, ExShowScreenMessage.TOP_CENTER, 6000));
			}
		}, 60000, 60000); // 每60秒顯示一次

		instance.setParameter("ScoreDisplayTask", scoreDisplayTask);
	}

	@Override
	public void onEnterZone(Creature creature, ZoneType zone) {
		final Instance instance = creature.getInstanceWorld();
		if (instance == null) {
			return;
		}

		if (creature.isPlayer()) {
			FORCE_BUFF.getSkill().applyEffects(creature, creature);
		} else {
			FORCE_DEBUFF.getSkill().applyEffects(creature, creature);
		}
	}

	@Override
	public void onInstanceEnter(Player player, Instance instance) {
		final boolean running = instance.getParameters().getBoolean("Leona_Running", false);
		if ((instance.getRemainingTime() > 0) && running) {
			player.sendPacket(new ExSendUIEvent(player, false, false, (int) (instance.getRemainingTime() / 1000), 0, NpcStringId.TIME_LEFT));

			// 初始化中途加入玩家的分數
			if (instance.getParameters().getInt("Player_" + player.getObjectId() + "_Score", -1) == -1) {
				instance.getParameters().set("Player_" + player.getObjectId() + "_Score", 0);
				instance.getParameters().set("Player_" + player.getObjectId() + "_Kills", 0);
				instance.getParameters().set("Player_" + player.getObjectId() + "_Deaths", 0);
			}
		}
	}

	@Override
	public void onInstanceLeave(Player player, Instance instance) {
		player.sendPacket(new ExSendUIEvent(player, false, false, 0, 0, NpcStringId.TIME_LEFT));

		// 結算該玩家的積分（如果副本已開始且尚未結算過）
		final boolean started = instance.getParameters().getBoolean("PlayerEnter", false);
		final boolean alreadySettled = instance.getParameters().getBoolean("Player_" + player.getObjectId() + "_Settled", false);
		if (started && !alreadySettled) {
			// 如果玩家死亡離開，但周期任務尚未偵測到此次死亡，立即補存快照
			if (player.isDead() && !instance.getParameters().getBoolean("Player_" + player.getObjectId() + "_WasDead", false)) {
				instance.getParameters().set("Player_" + player.getObjectId() + "_WasDead", true);
				final int deaths = instance.getParameters().getInt("Player_" + player.getObjectId() + "_Deaths", 0) + 1;
				instance.getParameters().set("Player_" + player.getObjectId() + "_Deaths", deaths);
				saveDeathScoreSnapshot(player, instance, deaths);
			}
			instance.getParameters().set("Player_" + player.getObjectId() + "_Settled", true);
			final int finalScore = calculateFinalScore(player, instance);
			LeonasDungeonManager.getInstance().updateBestScore(player, finalScore);
		}

		// 清除所有效果
		player.stopAllEffects();

		// 同進同出：只有活著的玩家主動離開才觸發
		if (started && !player.isDead() && !instance.getParameters().getBoolean("ExitCountdownStarted", false)) {
			final Collection<Player> remaining = instance.getPlayers();
			if (!remaining.isEmpty()) {
				instance.setParameter("ExitCountdownStarted", true);
				instance.broadcastPacket(new ExShowScreenMessage("隊員 " + player.getName() + " 已離開副本，5秒後全隊將被傳送離開！", 5000));

				ThreadPool.schedule(() ->
				{
					for (Player member : instance.getPlayers()) {
						instance.ejectPlayer(member);
					}
					instance.destroy();
				}, 5000);
			} else {
				instance.destroy();
			}
		}
		// 死亡離開：檢查是否全隊都已死亡
		else if (started && player.isDead() && !instance.getParameters().getBoolean("ExitCountdownStarted", false)) {
			final Collection<Player> remaining = instance.getPlayers();
			final boolean allDead = remaining.isEmpty() || remaining.stream().allMatch(Creature::isDead);
			if (allDead) {
				instance.setParameter("ExitCountdownStarted", true);
				instance.broadcastPacket(new ExShowScreenMessage("全隊陣亡！5秒後副本結束！", 5000));
				ThreadPool.schedule(() ->
				{
					for (Player member : instance.getPlayers()) {
						instance.ejectPlayer(member);
					}
					instance.destroy();
				}, 5000);
			}
		}
	}

	@Override
	public void onInstanceDestroy(Instance instance) {
		final ScheduledFuture<?> task = instance.getParameters().getObject("RankingPointTask", ScheduledFuture.class);
		if ((task != null) && !task.isDone()) {
			task.cancel(true);
		}

		// 取消積分顯示任務
		final ScheduledFuture<?> scoreDisplayTask = instance.getParameters().getObject("ScoreDisplayTask", ScheduledFuture.class);
		if ((scoreDisplayTask != null) && !scoreDisplayTask.isDone()) {
			scoreDisplayTask.cancel(true);
		}

		// Calculate final score for all players (skip already settled)
		final Collection<Player> players = instance.getPlayers();
		for (Player member : players) {
			final boolean alreadySettled = instance.getParameters().getBoolean("Player_" + member.getObjectId() + "_Settled", false);
			if (!alreadySettled) {
				instance.getParameters().set("Player_" + member.getObjectId() + "_Settled", true);
				final int finalScore = calculateFinalScore(member, instance);
				LeonasDungeonManager.getInstance().updateBestScore(member, finalScore);
			}
		}

		instance.setParameter("RankingPointTask", null);
		instance.setParameter("ScoreDisplayTask", null);
	}

	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon) {
		final Instance instance = npc.getInstanceWorld();
		if (instance != null) {
			// 檢查是否為隨機BOSS
			boolean isRandomBoss = false;
			for (int bossId : RANDOM_BOSS_IDS) {
				if (npc.getId() == bossId) {
					isRandomBoss = true;
					break;
				}
			}

			// 如果是隨機BOSS，記錄擊殺數但不計入一般擊殺數和分數
			if (isRandomBoss) {
				// 清除BOSS存在標記
				instance.setParameter("RandomBossAlive", false);
				instance.setParameter("CurrentRandomBoss", null);

				// 記錄擊殺者的隨機BOSS擊殺數
				final int bossKills = instance.getParameters().getInt("Player_" + killer.getObjectId() + "_BossKills", 0) + 1;
				instance.getParameters().set("Player_" + killer.getObjectId() + "_BossKills", bossKills);

				// 向副本內所有玩家發送BOSS被擊殺的訊息
				final String bossName = npc.getName();
				for (Player player : instance.getPlayers()) {
					player.sendPacket(new ExShowScreenMessage("隨機BOSS " + bossName + " 已被擊殺！擊殺者將在結算時獲得 " + POINTS_PER_RANDOM_BOSS + " 分獎勵！", 5000));
				}
				return;
			}

			// 一般怪物的處理
			// Increment kill count for the killer
			final int kills = instance.getParameters().getInt("Player_" + killer.getObjectId() + "_Kills", 0) + 1;
			instance.getParameters().set("Player_" + killer.getObjectId() + "_Kills", kills);

			// Add points for kill
			addScoreToPlayer(killer, instance, POINTS_PER_KILL);

			// Check tiered milestone bonuses (階梯式獎勵)
			if (kills >= 1000) {
				// 檢查是否剛好達到100的整數倍
				if ((kills % 100) == 0) {
					final int tier = kills / 1000; // 1000-1999=1, 2000-2999=2, etc.
					final int bonusPoints = 5 + (tier - 1) * 2; // 1000檔:5分, 2000檔:7分, 3000檔:9分
					addScoreToPlayer(killer, instance, bonusPoints);
					killer.sendMessage("擊殺里程碑！擊殺 " + kills + " 隻怪物，獲得 " + bonusPoints + " 分獎勵！");
				}
			}

			// Track mob for respawn with increased power
			final int mobRespawnCount = instance.getParameters().getInt("Mob_" + npc.getId() + "_RespawnCount", 0);
			instance.getParameters().set("Mob_" + npc.getId() + "_RespawnCount", mobRespawnCount + 1);

			// 隨機BOSS出現機制 (千分之五機率)
			if (Math.random() < BOSS_SPAWN_CHANCE) {
				spawnRandomBoss(instance);
			}
		}
	}

	/**
	 * 隨機出現BOSS (四選一)
	 */
	private void spawnRandomBoss(Instance instance) {
		// 檢查是否已有隨機BOSS存在
		if (instance.getParameters().getBoolean("RandomBossAlive", false)) {
			return; // 已有BOSS存在，不再生成
		}

		// 隨機選擇一個BOSS (0-3)
		final int randomIndex = (int) (Math.random() * RANDOM_BOSS_IDS.length);
		final int bossId = RANDOM_BOSS_IDS[randomIndex];
		final int[] spawnLoc = BOSS_SPAWN_LOCATIONS[randomIndex];

		// 生成BOSS (不受副本成長型能力影響，無重生時間)
		final Npc boss = addSpawn(bossId, spawnLoc[0], spawnLoc[1], spawnLoc[2], spawnLoc[3], false, 0, false, instance.getId());

		if (boss != null) {
			// 標記BOSS已存在
			instance.setParameter("RandomBossAlive", true);
			instance.setParameter("CurrentRandomBoss", boss.getObjectId());

			// 向副本內所有玩家發送屏幕公告
			final String bossName = boss.getName();
			for (Player player : instance.getPlayers()) {
				player.sendPacket(new ExShowScreenMessage("隨機BOSS " + bossName + " 已出現！", 5000));
			}
		}
	}

	@Override
	public void onSpawn(Npc npc) {
		final Instance instance = npc.getInstanceWorld();
		if (instance != null) {
			// 檢查是否為隨機BOSS，隨機BOSS不受成長型能力影響
			boolean isRandomBoss = false;
			for (int bossId : RANDOM_BOSS_IDS) {
				if (npc.getId() == bossId) {
					isRandomBoss = true;
					break;
				}
			}

			// 隨機BOSS不套用成長型能力
			if (isRandomBoss) {
				return;
			}

			// Apply power increase based on respawn count
			final int respawnCount = instance.getParameters().getInt("Mob_" + npc.getId() + "_RespawnCount", 0);
			if (respawnCount > 0) {
				// 計算成長倍率
				final double rawMultiplier = Math.pow(MOB_POWER_INCREASE, respawnCount);

				// HP/MP 上限 100 億
				final double hpCap = 10_000_000_000.0 / npc.getMaxHp();
				final double mpCap = 10_000_000_000.0 / npc.getMaxMp();
				final double hpMultiplier = Math.min(rawMultiplier, hpCap);
				final double mpMultiplier = Math.min(rawMultiplier, mpCap);

				// 攻擊/防禦/速度 上限 21 億 (Integer.MAX_VALUE)
				final double pAtkCap = 2_100_000_000.0 / npc.getPAtk();
				final double mAtkCap = 2_100_000_000.0 / npc.getMAtk();
				final double pDefCap = 2_100_000_000.0 / npc.getPDef();
				final double mDefCap = 2_100_000_000.0 / npc.getMDef();
				final double pAtkSpdCap = 2_100_000_000.0 / npc.getPAtkSpd();
				final double mAtkSpdCap = 2_100_000_000.0 / npc.getMAtkSpd();

				final double pAtkMultiplier = Math.min(rawMultiplier, pAtkCap);
				final double mAtkMultiplier = Math.min(rawMultiplier, mAtkCap);
				final double pDefMultiplier = Math.min(rawMultiplier, pDefCap);
				final double mDefMultiplier = Math.min(rawMultiplier, mDefCap);
				final double pAtkSpdMultiplier = Math.min(rawMultiplier, pAtkSpdCap);
				final double mAtkSpdMultiplier = Math.min(rawMultiplier, mAtkSpdCap);

				// Apply HP and MP multiplier using stat system
				npc.getStat().mergeMul(org.l2jmobius.gameserver.model.stats.Stat.MAX_HP, hpMultiplier);
				npc.getStat().mergeMul(org.l2jmobius.gameserver.model.stats.Stat.MAX_MP, mpMultiplier);
				npc.setCurrentHp(npc.getMaxHp());
				npc.setCurrentMp(npc.getMaxMp());

				// Apply stat multipliers with caps
				npc.setPhysicalAttackMultiplier(pAtkMultiplier);
				npc.setMagicalAttackMultiplier(mAtkMultiplier);
				npc.setPhysicalDefenseMultiplier(pDefMultiplier);
				npc.setMagicalDefenseMultiplier(mDefMultiplier);
				npc.setAttackSpeedMultiplier(pAtkSpdMultiplier);
				npc.setCastSpeedMultiplier(mAtkSpdMultiplier);
			}
		}
	}

	private void showRankingPage(Player player) {
		final Map<Integer, Integer> topPlayers = LeonasDungeonManager.getInstance().getTopPlayers(20);
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><head><title>蕾歐娜</title></head><body>");
		sb.append("<center><font color=\"FFCC33\" name=\"hs12\">蕾歐娜地下城 - 排行榜</font></center><br>");
		sb.append("<table width=\"280\" border=\"0\" cellspacing=\"1\" cellpadding=\"3\" bgcolor=\"111111\">");
		sb.append("<tr bgcolor=\"333333\">");
		sb.append("<td width=\"40\" align=\"center\"><font color=\"FFCC33\">排名</font></td>");
		sb.append("<td width=\"140\" align=\"center\"><font color=\"FFCC33\">角色名稱</font></td>");
		sb.append("<td width=\"100\" align=\"center\"><font color=\"FFCC33\">積分</font></td>");
		sb.append("</tr>");

		if (topPlayers.isEmpty()) {
			sb.append("<tr bgcolor=\"222222\"><td colspan=\"3\" align=\"center\"><font color=\"999999\">目前尚無排行資料</font></td></tr>");
		} else {
			int rank = 1;
			for (Map.Entry<Integer, Integer> entry : topPlayers.entrySet()) {
				final int charId = entry.getKey();
				final int points = entry.getValue();
				final String charName = CharInfoTable.getInstance().getNameById(charId);
				final String displayName = (charName != null) ? charName : "未知";
				final String rowColor = (rank % 2 == 0) ? "222222" : "111111";

				// 前三名用特殊顏色
				String rankColor = "AAAAAA";
				if (rank == 1) {
					rankColor = "FFD700"; // 金
				} else if (rank == 2) {
					rankColor = "C0C0C0"; // 銀
				} else if (rank == 3) {
					rankColor = "CD7F32"; // 銅
				}

				sb.append("<tr bgcolor=\"").append(rowColor).append("\">");
				sb.append("<td width=\"40\" align=\"center\"><font color=\"").append(rankColor).append("\">").append(rank).append("</font></td>");
				sb.append("<td width=\"140\" align=\"center\"><font color=\"00CCFF\">").append(displayName).append("</font></td>");
				sb.append("<td width=\"100\" align=\"center\"><font color=\"00FF00\">").append(points).append("</font></td>");
				sb.append("</tr>");
				rank++;
			}
		}

		sb.append("</table><br>");

		// 顯示玩家自己的排名
		final int myRank = LeonasDungeonManager.getInstance().getPlayerRank(player);
		final int myPoints = LeonasDungeonManager.getInstance().getPlayerPoints(player);
		sb.append("<center><font color=\"AAAAAA\">你的排名: </font><font color=\"FFFF00\">第 ").append(myRank).append(" 名</font>");
		sb.append(" <font color=\"AAAAAA\">| 積分: </font><font color=\"00FF00\">").append(myPoints).append("</font></center>");

		sb.append("</body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	private void addScoreToPlayer(Player player, Instance instance, int points) {
		final int currentScore = instance.getParameters().getInt("Player_" + player.getObjectId() + "_Score", 0);
		instance.getParameters().set("Player_" + player.getObjectId() + "_Score", currentScore + points);
	}

	private int calculateFinalScore(Player player, Instance instance) {
		// 如果有死亡時儲存的分數快照，直接使用（保留死亡當下的分數）
		final int deathScore = instance.getParameters().getInt("Player_" + player.getObjectId() + "_DeathScore", -1);
		if (deathScore >= 0) {
			player.sendPacket(new ExShowScreenMessage("結算完成！最終得分: " + deathScore + " 分", ExShowScreenMessage.TOP_CENTER, 8000));
			player.sendMessage("========== 副本結算明細 ==========");
			player.sendMessage("（死亡時結算）最終得分: " + deathScore + " 分");
			player.sendMessage("==================================");
			return deathScore;
		}

		final int killScore = instance.getParameters().getInt("Player_" + player.getObjectId() + "_Score", 0);
		final int kills = instance.getParameters().getInt("Player_" + player.getObjectId() + "_Kills", 0);
		final int deaths = instance.getParameters().getInt("Player_" + player.getObjectId() + "_Deaths", 0);
		final int bossKills = instance.getParameters().getInt("Player_" + player.getObjectId() + "_BossKills", 0);

		// Time-based points
		final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 1);
		final long timeInMinutes = (30 * 60000 - instance.getRemainingTime()) / 60000;
		int pointsPerMinute = POINTS_PER_MINUTE_LOW;
		String difficultyName = "低";
		if (difficulty == 2) {
			pointsPerMinute = POINTS_PER_MINUTE_MED;
			difficultyName = "中";
		} else if (difficulty == 3) {
			pointsPerMinute = POINTS_PER_MINUTE_HIGH;
			difficultyName = "高";
		}
		final int timeScore = (int) (timeInMinutes * pointsPerMinute);

		// Random Boss bonus
		final int bossBonus = bossKills * POINTS_PER_RANDOM_BOSS;

		// Death penalty
		final int deathPenalty = deaths * POINTS_DEDUCT_PER_DEATH;

		// HP penalty
		final double hpPercent = (player.getCurrentHp() / player.getMaxHp()) * 100;
		final int hpPenalty = (hpPercent < POINTS_DEDUCT_HP_THRESHOLD) ? POINTS_DEDUCT_LOW_HP : 0;

		// Final
		final int rawTotal = killScore + timeScore + bossBonus - deathPenalty - hpPenalty;
		final int finalScore = Math.max(0, rawTotal);

		// Send detailed breakdown to player
		player.sendPacket(new ExShowScreenMessage("結算完成！最終得分: " + finalScore + " 分", ExShowScreenMessage.TOP_CENTER, 8000));
		player.sendMessage("========== 副本結算明細 ==========");
		player.sendMessage("難度: " + difficultyName + " | 擊殺數: " + kills + " | 死亡數: " + deaths);
		player.sendMessage("擊殺積分: +" + killScore + " (含里程碑獎勵)");
		player.sendMessage("時間積分: +" + timeScore + " (" + timeInMinutes + "分鐘 x " + pointsPerMinute + "分/分鐘)");
		if (bossBonus > 0) {
			player.sendMessage("隨機BOSS獎勵: +" + bossBonus + " (" + bossKills + "隻 x " + POINTS_PER_RANDOM_BOSS + "分)");
		}
		if (deathPenalty > 0) {
			player.sendMessage("死亡扣分: -" + deathPenalty + " (" + deaths + "次 x " + POINTS_DEDUCT_PER_DEATH + "分)");
		}
		if (hpPenalty > 0) {
			player.sendMessage("血量過低扣分: -" + hpPenalty + " (HP " + (int) hpPercent + "% < " + POINTS_DEDUCT_HP_THRESHOLD + "%)");
		}
		player.sendMessage("最終得分: " + finalScore + " 分");
		player.sendMessage("==================================");

		return finalScore;
	}

	public static void main(String[] args) {
		new LeonasDungeon();
	}

	/**
	 * 儲存玩家死亡當下的分數快照（不含HP扣分）
	 */
	private void saveDeathScoreSnapshot(Player player, Instance instance, int deaths) {
		final int killScore = instance.getParameters().getInt("Player_" + player.getObjectId() + "_Score", 0);
		final int bossKills = instance.getParameters().getInt("Player_" + player.getObjectId() + "_BossKills", 0);
		final int bossBonus = bossKills * POINTS_PER_RANDOM_BOSS;
		final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 1);
		final long timeInMinutes = (30 * 60000 - instance.getRemainingTime()) / 60000;
		int pointsPerMinute = POINTS_PER_MINUTE_LOW;
		if (difficulty == 2) {
			pointsPerMinute = POINTS_PER_MINUTE_MED;
		} else if (difficulty == 3) {
			pointsPerMinute = POINTS_PER_MINUTE_HIGH;
		}
		final int timeScore = (int) (timeInMinutes * pointsPerMinute);
		final int deathPenalty = deaths * POINTS_DEDUCT_PER_DEATH;
		final int hpPenalty = POINTS_DEDUCT_LOW_HP; // 死亡時HP=0，必然觸發HP扣分
		final int rawScore = Math.max(0, killScore + timeScore + bossBonus - deathPenalty - hpPenalty);
		instance.getParameters().set("Player_" + player.getObjectId() + "_DeathScore", rawScore);
	}
}
