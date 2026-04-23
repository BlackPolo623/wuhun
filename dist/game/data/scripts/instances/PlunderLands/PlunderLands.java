/*
 * 掠奪之地 (Plunder Lands) — 公共共享副本
 *
 * 副本模板 ID : 1000（需與 掠奪之地.xml 的 id 屬性一致）
 * 固定世界 ID : 900（共享世界使用固定 runtime ID，保證全服只有一個世界）
 * 入口 NPC ID : 900039
 * 排行道具 ID : 57
 *
 * 特性：
 *  - 所有玩家共用同一個世界，可以互相看到
 *  - 整個副本為 PVP 區域（進入時設定 ZoneId.PVP，離開時移除）
 *  - 排行榜透過外部 ranking.htm + html.replace() 顯示，支援分頁
 *  - 擊殺計數達 90% 門檻後每 2% 向全服廣播進度
 *  - 達到 BOSS_KILL_THRESHOLD 後進入 60 秒倒數，倒數期間每隔一段時間全服公告
 *  - 倒數結束後生成 XML 中 boss 群組指定的 Boss
 *  - Boss 死亡後重置狀態；整個機制每 8 小時只能觸發一次
 */
package instances.PlunderLands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * 掠奪之地 (Plunder Lands) Instance Script
 * @author Custom
 */
public class PlunderLands extends InstanceScript
{
	// ==================== 副本設定 ====================

	/** 副本模板 ID，需與 掠奪之地.xml id 屬性一致 */
	private static final int TEMPLATE_ID = 1000;

	/**
	 * 固定共享世界 ID。
	 * 全服只會有一個掠奪之地世界，runtime ID 固定為此值。
	 * 使用 createInstance(TEMPLATE_ID, FIXED_WORLD_ID, player) 建立，
	 * 之後用 getInstance(FIXED_WORLD_ID) 取得。
	 */
	private static final int FIXED_WORLD_ID = 999;

	/** 入口 NPC ID（世界中放置，玩家點擊進入） */
	private static final int NPC_ID = 900039;

	/** 排行榜依此道具 ID 的持有數量排名 */
	private static final int RANK_ITEM_ID = 103991;

	/** 排行榜每頁顯示人數 */
	private static final int PLAYERS_PER_PAGE = 12;

	/** 副本入口傳送座標 */
	private static final Location ENTER_LOCATION   = new Location(16752, 214188, -15174);  // 掠奪之地大門
	private static final Location ENTER_LOCATION_A = new Location(21315, 220777, -14921);  // 進入點A
	private static final Location ENTER_LOCATION_B = new Location(18067,224995,-14791);  // 進入點B
	private static final Location ENTER_LOCATION_C = new Location(12116,220820,-14916);  // 進入點C

	// ==================== Boss 觸發設定 ====================

	/**
	 * 副本內怪物累積擊殺達此數量後，開始 60 秒倒數並生成 Boss。
	 * Boss 死亡後計數歸零，且需等待 8 小時冷卻才能再次觸發。
	 */
	private static final int BOSS_KILL_THRESHOLD = 20000;

	/**
	 * 擊殺計數達到門檻的幾 % 後開始向全服公告。
	 * 90% 時開始，之後每 WARN_STEP 次公告一次。
	 */
	private static final int WARN_START = (int) (BOSS_KILL_THRESHOLD * 0.9); // 90

	/**
	 * 90% 後每達到此擊殺間隔就公告一次（2% × 門檻）。
	 * 門檻 100 → 每 2 次公告（90, 92, 94, 96, 98, 100）。
	 */
	private static final int WARN_STEP = Math.max(1, (int) (BOSS_KILL_THRESHOLD * 0.02)); // 2

	/** Boss 出現前的倒數秒數 */
	private static final int BOSS_COUNTDOWN_SEC = 60;

	/**
	 * 觸發 Boss 機制的冷卻時間（毫秒）。
	 * 每 8 小時才能觸發一次（達到門檻開始倒數起算）。
	 */
	private static final long COOLDOWN_MS = 8L * 60 * 60 * 1000; // 8 小時

	/**
	 * Boss 所在的 XML spawnlist group 名稱。
	 * 需與 掠奪之地.xml 中的 <group name="..."> 一致。
	 */
	private static final String BOSS_GROUP_NAME = "boss";

	/**
	 * Boss NPC ID（用於偵測 Boss 被擊殺，以重置狀態）。
	 * ★ 請修改為 XML boss group 內 npc 的實際 ID ★
	 */
	private static final int BOSS_NPC_ID = 61001; // ← 改成你的 Boss NPC ID

	/**
	 * 副本內被計數的怪物 ID 列表（Harid + Zenta 兩組）。
	 * 擊殺這些 NPC 才會累積到 BOSS_KILL_THRESHOLD。
	 */
	private static final int[] MONSTER_IDS =
	{
			61002, // Harid Lizardman Berserker
	};

	// ==================== 血盟佔領設定 ====================

	/**
	 * 結算後第一名血盟成員按貢獻比例可領取的獎勵。
	 * 格式：{道具ID, 總數量}，例如 {57, 500_000_000} 表示 5億 Adena 依比例分配。
	 * 可設定多筆道具，每筆獨立計算比例。
	 */
	private static final int[][] OCCUPATION_REWARDS =
	{
			{91663, 300000},     // Adena 5億（按貢獻比例）
			{103991, 10000},
			{109012, 15000},
			{109013, 15000},
			// {itemId, totalAmount}, // 可繼續新增
	};

	/**
	 * 上週佔領血盟可施放的全盟 BUFF 清單。
	 * 格式：{技能ID, 技能等級, 消耗道具ID, 消耗道具數量}
	 * 可設定多筆，每次點擊按鈕依序全部施放並扣除所有消耗。
	 */
	private static final int[][] CLAN_BUFFS =
	{
			{48003, 1, 103991, 1000},
			{48004, 1, 103991, 1000},
			{48005, 1, 103991, 3000},
			// {skillId, skillLevel, costItemId, costAmount},
	};

	/** 血盟排行榜每頁顯示筆數 */
	private static final int CLAN_PER_PAGE   = 10;
	/** 血盟詳情每頁顯示成員數 */
	private static final int MEMBER_PER_PAGE = 12;

	// ==================== 血盟佔領狀態 ====================

	/** 本週積分（in-memory）：clanId → playerId → AtomicInteger */
	private static final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, AtomicInteger>> WEEK_SCORES = new ConcurrentHashMap<>();
	/** 名稱快取（最後一次更新） */
	private static final ConcurrentHashMap<Integer, String> CLAN_NAMES   = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Integer, String> PLAYER_NAMES = new ConcurrentHashMap<>();

	/** 上週結算資訊（從 DB 載入，結算後更新） */
	private static volatile int    LAST_WINNER_CLAN_ID    = 0;
	private static volatile String LAST_WINNER_CLAN_NAME  = "";
	private static volatile long   LAST_WINNER_WEEK_START = 0;
	private static volatile int    LAST_WINNER_TOTAL      = 0;

	// ==================== 執行時狀態（靜態，對應單一共享世界）====================

	/**
	 * 當前副本世界的怪物擊殺累積計數。
	 * AtomicInteger 確保多執行緒安全。
	 */
	private static final AtomicInteger KILL_COUNTER = new AtomicInteger(0);

	/**
	 * 是否有 Boss 正在副本內存活，或正在倒數中。
	 * 使用 AtomicBoolean + compareAndSet 確保多執行緒下只有一條執行緒能觸發倒數。
	 */
	private static final AtomicBoolean BOSS_ACTIVE = new AtomicBoolean(false);

	/**
	 * 下次可以觸發 Boss 機制的最早時間（System.currentTimeMillis）。
	 * 初始為 0，表示尚未使用過冷卻。
	 */
	private static volatile long NEXT_TRIGGER_TIME = 0;

	// ==================== 建構子 ====================

	private PlunderLands()
	{
		super(TEMPLATE_ID);
		addStartNpc(NPC_ID);
		addFirstTalkId(NPC_ID);
		addTalkId(NPC_ID);
		addInstanceLeaveId(TEMPLATE_ID);
		addInstanceCreatedId(TEMPLATE_ID);

		// 偵測副本內怪物死亡（累積計數）
		addKillId(MONSTER_IDS);
		// 偵測 Boss 死亡（重置 BOSS_ACTIVE，允許下次觸發）
		addKillId(BOSS_NPC_ID);

		// 血盟佔領：載入上週結算與本週積分，排程週結算與DB flush
		final PlunderLandsDAO dao = PlunderLandsDAO.getInstance();
		final PlunderLandsDAO.WinnerRecord lastWinner = dao.loadLastWinner();
		if (lastWinner != null)
		{
			LAST_WINNER_CLAN_ID    = lastWinner.clanId;
			LAST_WINNER_CLAN_NAME  = lastWinner.clanName;
			LAST_WINNER_WEEK_START = lastWinner.weekStart;
			LAST_WINNER_TOTAL      = lastWinner.totalScore;
		}
		dao.loadScores(WEEK_SCORES, CLAN_NAMES, PLAYER_NAMES);
		ThreadPool.scheduleAtFixedRate(PlunderLands::flushScoresToDB, 60_000, 60_000);
		scheduleNextSettlement();
	}

	// ==================== NPC 對話 ====================

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainPage(npc, player);
		return null;
	}

	// ==================== 事件處理 ====================

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enter":
			{
				handleEnter(npc, player, ENTER_LOCATION);
				break;
			}
			case "enterA":
			{
				handleEnter(npc, player, ENTER_LOCATION_A);
				break;
			}
			case "enterB":
			{
				handleEnter(npc, player, ENTER_LOCATION_B);
				break;
			}
			case "enterC":
			{
				handleEnter(npc, player, ENTER_LOCATION_C);
				break;
			}
			case "mainPage":
			{
				showMainPage(npc, player);
				break;
			}
			case "shop":
			{
				MultisellData.getInstance().separateAndSend(90003901, player, npc, false);
				break;
			}
			case "clanOccupy":
			{
				showClanOccupy(npc, player);
				break;
			}
			case "claimReward":
			{
				handleClaimReward(npc, player);
				break;
			}
			default:
			{
				if (event.startsWith("clanBuff_"))
				{
					try
					{
						final int idx = Integer.parseInt(event.substring("clanBuff_".length()));
						handleClanBuff(npc, player, idx);
					}
					catch (NumberFormatException ignored)
					{
					}
				}
				else if (event.startsWith("showRank_"))
				{
					int page = 0;
					try
					{
						page = Integer.parseInt(event.substring("showRank_".length()));
					}
					catch (NumberFormatException ignored)
					{
					}
					showRankPage(npc, player, page);
				}
				else if (event.startsWith("showClanRank_"))
				{
					int page = 0;
					try
					{
						page = Integer.parseInt(event.substring("showClanRank_".length()));
					}
					catch (NumberFormatException ignored)
					{
					}
					showClanRank(npc, player, page);
				}
				else if (event.startsWith("clanDetail_"))
				{
					// format: clanDetail_<clanId>_<page>
					final String suffix = event.substring("clanDetail_".length());
					final int sep = suffix.lastIndexOf('_');
					if (sep > 0)
					{
						try
						{
							final int clanId = Integer.parseInt(suffix.substring(0, sep));
							final int page   = Integer.parseInt(suffix.substring(sep + 1));
							showClanDetail(npc, player, clanId, page);
						}
						catch (NumberFormatException ignored)
						{
						}
					}
				}
				break;
			}
		}
		return null;
	}

	// ==================== 副本建立：重置狀態 ====================

	/**
	 * 每次副本世界被建立時，清除擊殺計數與 Boss 狀態。
	 * 注意：NEXT_TRIGGER_TIME（8 小時冷卻）不在此重置，跨世界重建仍有效。
	 */
	@Override
	public void onInstanceCreated(Instance world, Player player)
	{
		KILL_COUNTER.set(0);
		BOSS_ACTIVE.set(false);
		super.onInstanceCreated(world, player);
	}

	// ==================== 擊殺事件：計數 + Boss 觸發 ====================

	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		// 只處理掠奪之地副本內的擊殺
		final Instance world = (killer != null) ? killer.getInstanceWorld() : null;
		if ((world == null) || (world.getTemplateId() != TEMPLATE_ID))
		{
			return;
		}

		final int npcId = npc.getId();

		// ── Boss 被擊殺：重置狀態，公告下次可觸發時間 ───────────────────────
		if (npcId == BOSS_NPC_ID)
		{
			BOSS_ACTIVE.set(false);
			KILL_COUNTER.set(0);

			// 計算下次可觸發的時間（HH:mm）
			final long nextTime = NEXT_TRIGGER_TIME;
			final java.time.Instant instant = java.time.Instant.ofEpochMilli(nextTime);
			final java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
			final String nextTimeStr = String.format("%02d:%02d", ldt.getHour(), ldt.getMinute());

			final String killMsg =  killer.getName() + " 擊敗了掠奪之地的守護者！";
			final String cooldownMsg = "下次擊殺累積任務最快於 " + nextTimeStr + " 開放。";
			world.broadcastPacket(new ExShowScreenMessage(killMsg, 8000));
			broadcastAnnounce(killMsg);
			broadcastAnnounce(cooldownMsg);
			return;
		}

		// ── 普通怪物被擊殺：確認是計數目標 ─────────────────────────────────
		boolean isTarget = false;
		for (int id : MONSTER_IDS)
		{
			if (npcId == id)
			{
				isTarget = true;
				break;
			}
		}
		if (!isTarget)
		{
			return;
		}

		// ── 血盟積分累積 ──────────────────────────────────────────────────────────
		final Clan clan = killer.getClan();
		if (clan != null)
		{
			final int clanId   = clan.getId();
			final int playerId = killer.getObjectId();
			CLAN_NAMES.put(clanId, clan.getName());
			PLAYER_NAMES.put(playerId, killer.getName());
			WEEK_SCORES.computeIfAbsent(clanId, k -> new ConcurrentHashMap<>())
			           .computeIfAbsent(playerId, k -> new AtomicInteger(0))
			           .incrementAndGet();
		}

		// 若 Boss 正在場上（或倒數中），不再累積計數
		if (BOSS_ACTIVE.get())
		{
			return;
		}

		// 8 小時冷卻期間完全不累積（boss 死亡後需等到冷卻結束才重新開始）
		if (System.currentTimeMillis() < NEXT_TRIGGER_TIME)
		{
			return;
		}

		final int count = KILL_COUNTER.incrementAndGet();

		// ── 90% 後每 2% 向全服公告進度 ────────────────────────────────────────
		if ((count >= WARN_START) && ((count - WARN_START) % WARN_STEP == 0) && (count < BOSS_KILL_THRESHOLD))
		{
			final int pct = count * 100 / BOSS_KILL_THRESHOLD;
			final String warnMsg = "守護者即將甦醒！擊殺進度已達 " + pct + "% (" + count + "/" + BOSS_KILL_THRESHOLD + ")！";
			broadcastAnnounce(warnMsg);
			world.broadcastPacket(new ExShowScreenMessage(warnMsg, 5000));
		}

		// ── 達到門檻：檢查冷卻，啟動倒數 ────────────────────────────────────
		if (count >= BOSS_KILL_THRESHOLD)
		{
			final long now = System.currentTimeMillis();

			// 8 小時冷卻尚未結束
			if (now < NEXT_TRIGGER_TIME)
			{
				KILL_COUNTER.set(0);
				final long remainMin = (NEXT_TRIGGER_TIME - now) / 60000;
				world.broadcastPacket(new ExShowScreenMessage(
					"守護者仍在沉睡，還需等待約 " + remainMin + " 分鐘才能再次召喚。", 6000));
				return;
			}

			// 鎖定狀態，防止重複觸發（compareAndSet 確保多執行緒只有一條能通過）
			if (!BOSS_ACTIVE.compareAndSet(false, true))
			{
				return;
			}
			KILL_COUNTER.set(0);
			NEXT_TRIGGER_TIME = now + COOLDOWN_MS;

			// 全服宣告倒數開始
			final String triggerMsg = "擊殺累積達標！掠奪之地的守護者將在 " + BOSS_COUNTDOWN_SEC + " 秒後降臨！";
			broadcastAnnounce(triggerMsg);
			world.broadcastPacket(new ExShowScreenMessage(triggerMsg, 8000));

			// 啟動倒數計時
			scheduleBossCountdown();
		}
	}

	// ==================== Boss 倒數計時 ====================

	/** 倒數過程中要公告的剩餘秒數（從大到小）。 */
	private static final int[] COUNTDOWN_ANNOUNCE_SECS = { 60, 30, 10, 5, 3, 2, 1 };

	/**
	 * 預排固定幾個公告任務 + 最後一個生成任務。
	 * 任務數固定（公告點數 + 1），無遞迴，overhead 最低。
	 */
	private static void scheduleBossCountdown()
	{
		for (int sec : COUNTDOWN_ANNOUNCE_SECS)
		{
			final int remaining = sec;
			final long delay = (long) (BOSS_COUNTDOWN_SEC - sec) * 1000;
			ThreadPool.schedule(() ->
			{
				final String msg = "守護者將在 " + remaining + " 秒後降臨！";
				broadcastAnnounce(msg);
				final Instance w = InstanceManager.getInstance().getInstance(FIXED_WORLD_ID);
				if (w != null)
				{
					w.broadcastPacket(new ExShowScreenMessage(msg, 4000));
				}
			}, delay);
		}
		ThreadPool.schedule(PlunderLands::doSpawnBoss, BOSS_COUNTDOWN_SEC * 1000L);
	}

	/**
	 * 倒數結束後實際生成 Boss。
	 * 取最新 world 引用，避免 60 秒間世界被銷毀的問題。
	 */
	private static void doSpawnBoss()
	{
		final Instance world = InstanceManager.getInstance().getInstance(FIXED_WORLD_ID);
		if (world == null)
		{
			BOSS_ACTIVE.set(false);
			return;
		}

		final List<Npc> spawned = world.spawnGroup(BOSS_GROUP_NAME);
		if ((spawned != null) && !spawned.isEmpty())
		{
			final String msg = "守護者已現身！英雄們快去挑戰！";
			broadcastAnnounce(msg);
			world.broadcastPacket(new ExShowScreenMessage(msg, 10000));
		}
		else
		{
			// spawnGroup 找不到群組，回退狀態避免永久鎖定
			BOSS_ACTIVE.set(false);
		}
	}

	// ==================== 全服雙重公告工具 ====================

	/**
	 * 同時發送螢幕訊息（ExShowScreenMessage）和世界頻道文字（CreatureSay WORLD）給全體線上玩家。
	 *
	 * @param msg 公告文字
	 */
	private static void broadcastAnnounce(final String msg)
	{
		Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(msg, 6000));
		Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, "掠奪之地", msg));
	}

	// ==================== 進入副本 ====================

	private void handleEnter(Npc npc, Player player, Location enterLoc)
	{
		// 已在副本內則拒絕重複進入
		final Instance current = player.getInstanceWorld();
		if ((current != null) && (current.getTemplateId() == TEMPLATE_ID))
		{
			player.sendMessage("[掠奪之地] 你已經在副本內了！");
			return;
		}
		if (player.getSoulringCount() <= 300)
		{
			player.sendMessage("魂環數量不足，無法進入該副本");
			return;
		}

		// 取得或建立唯一的共享世界
		Instance world = InstanceManager.getInstance().getInstance(FIXED_WORLD_ID);
		if (world == null)
		{
			world = InstanceManager.getInstance().createInstance(TEMPLATE_ID, FIXED_WORLD_ID, player);
		}

		if (world == null)
		{
			player.sendMessage("[掠奪之地] 副本建立失敗，請稍後再試或通知管理員。");
			return;
		}

		// 傳送進入並標記 PVP 區域
		player.teleToLocation(enterLoc, world);
		player.setInsideZone(ZoneId.PVP, true);
		player.sendPacket(new ExShowScreenMessage("進入掠奪之地！此為 PVP 區域，死亡將掉落指定道具！", 6000));
	}

	// ==================== 副本離開 ====================

	/**
	 * 玩家離開副本時（傳送出、死亡被傳出、自然登出）自動移除 PVP 旗標。
	 */
	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		player.setInsideZone(ZoneId.PVP, false);
	}

	// ==================== 主頁面 ====================

	private void showMainPage(Npc npc, Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/PlunderLands/main.htm");

		// ── 擊殺進度 ──────────────────────────────────────────────────────────
		final String progressStr;
		if (BOSS_ACTIVE.get())
		{
			progressStr = "<font color=\"FF4444\">守護者已降臨！</font>";
		}
		else
		{
			final int count = KILL_COUNTER.get();
			final int pct = BOSS_KILL_THRESHOLD > 0 ? count * 100 / BOSS_KILL_THRESHOLD : 0;
			progressStr = count + " / " + BOSS_KILL_THRESHOLD + "　(" + pct + "%)";
		}
		html.replace("%kill_progress%", progressStr);

		// ── 下次開放時間 ───────────────────────────────────────────────────────
		final long now = System.currentTimeMillis();
		final String nextTimeStr;
		if (BOSS_ACTIVE.get())
		{
			nextTimeStr = "<font color=\"FF8800\">倒數或戰鬥中</font>";
		}
		else if (NEXT_TRIGGER_TIME == 0 || now >= NEXT_TRIGGER_TIME)
		{
			nextTimeStr = "<font color=\"00CC44\">現在可累積</font>";
		}
		else
		{
			final java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(
				java.time.Instant.ofEpochMilli(NEXT_TRIGGER_TIME), java.time.ZoneId.systemDefault());
			final long remainMin = (NEXT_TRIGGER_TIME - now) / 60000;
			nextTimeStr = String.format("%02d:%02d　（剩約 %d 分鐘）", ldt.getHour(), ldt.getMinute(), remainMin);
		}
		html.replace("%next_time%", nextTimeStr);

		// 血盟佔領欄位
		html.replace("%last_winner%", LAST_WINNER_CLAN_NAME.isEmpty() ? "尚無記錄" : LAST_WINNER_CLAN_NAME);
		html.replace("%current_top%", getCurrentTopClan());

		player.sendPacket(html);
	}

	// ==================== 排行榜（外部 HTM + replace）====================

	private void showRankPage(Npc npc, Player player, int page)
	{
		// 取得共享世界的所有玩家，依持有道具數量排序
		final List<Player> players = new ArrayList<>();
		final Instance world = InstanceManager.getInstance().getInstance(FIXED_WORLD_ID);
		if (world != null)
		{
			players.addAll(world.getPlayers());
		}
		players.sort(Comparator.comparingLong((Player p) -> p.getInventory().getInventoryItemCount(RANK_ITEM_ID, -1)).reversed());

		// 分頁計算
		final int total = players.size();
		final int totalPages = Math.max(1, (int) Math.ceil((double) total / PLAYERS_PER_PAGE));
		page = Math.max(0, Math.min(page, totalPages - 1));

		final int start = page * PLAYERS_PER_PAGE;
		final int end = Math.min(start + PLAYERS_PER_PAGE, total);

		// 產生排行列
		final StringBuilder rows = new StringBuilder();
		if (total == 0)
		{
			rows.append("<tr><td colspan=\"3\"><center><font color=\"888888\">無</font></center></td></tr>");
		}
		else
		{
			for (int i = start; i < end; i++)
			{
				final Player p = players.get(i);
				final int rank = i + 1;
				final long itemCount = p.getInventory().getInventoryItemCount(RANK_ITEM_ID, -1);
				final String color = (rank == 1) ? "FFDD00" : (rank == 2) ? "C0C0C0" : (rank == 3) ? "CD7F32" : "FFFFFF";
				rows.append("<tr>")
					.append("<td width=\"25\"><font color=\"").append(color).append("\">").append(rank).append(".</font></td>")
					.append("<td><font color=\"").append(color).append("\">").append(p.getName()).append("</font></td>")
					.append("<td ><font color=\"").append(color).append("\">").append(itemCount).append("</font></td>")
					.append("</tr>");
			}
		}

		// 翻頁按鈕
		final String prevBtn = (page > 0)
			? "<button action=\"bypass -h Quest PlunderLands showRank_" + (page - 1) + "\" value=\"上頁\" width=60 height=22 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>"
			: "";

		final String nextBtn = (page < totalPages - 1)
			? "<button action=\"bypass -h Quest PlunderLands showRank_" + (page + 1) + "\" value=\"下頁\" width=60 height=22 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>"
			: "";

		final String pageInfo = "第 " + (page + 1) + " / " + totalPages + " 頁　共 " + total + " 人";

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/PlunderLands/ranking.htm");
		html.replace("%rank_rows%", rows.toString());
		html.replace("%page_info%", pageInfo);
		html.replace("%prev_button%", prevBtn);
		html.replace("%next_button%", nextBtn);
		player.sendPacket(html);
	}

	// ==================== 血盟佔領：頁面 ====================

	private void showClanRank(Npc npc, Player player, int page)
	{
		// 從 in-memory 計算各血盟總積分
		final List<int[]> clanTotals = new ArrayList<>();
		for (Map.Entry<Integer, ConcurrentHashMap<Integer, AtomicInteger>> entry : WEEK_SCORES.entrySet())
		{
			final int total = entry.getValue().values().stream().mapToInt(AtomicInteger::get).sum();
			clanTotals.add(new int[]{ entry.getKey(), total });
		}
		clanTotals.sort((a, b) -> b[1] - a[1]);

		final int total      = clanTotals.size();
		final int totalPages = Math.max(1, (int) Math.ceil((double) total / CLAN_PER_PAGE));
		page = Math.max(0, Math.min(page, totalPages - 1));
		final int start = page * CLAN_PER_PAGE;
		final int end   = Math.min(start + CLAN_PER_PAGE, total);

		final StringBuilder rows = new StringBuilder();
		if (total == 0)
		{
			rows.append("<tr><td colspan=4 align=center><font color=888888>無</font></td></tr>");
		}
		else
		{
			for (int i = start; i < end; i++)
			{
				final int    rank      = i + 1;
				final int    clanId    = clanTotals.get(i)[0];
				final int    clanScore = clanTotals.get(i)[1];
				final String clanName  = CLAN_NAMES.getOrDefault(clanId, "未知血盟");
				final String color     = rank == 1 ? "FFDD00" : rank == 2 ? "C0C0C0" : rank == 3 ? "CD7F32" : "FFFFFF";
				rows.append("<tr bgcolor=1A1A1A>")
				    .append("<td width=25 align=center><font color=").append(color).append(">").append(rank).append("</font></td>")
				    .append("<td><font color=").append(color).append(">").append(clanName).append("</font></td>")
				    .append("<td width=70 align=right><font color=").append(color).append(">").append(clanScore).append("</font></td>")
				    .append("<td width=55 align=center>")
				    .append("<button value=詳情 action=\"bypass -h Quest PlunderLands clanDetail_").append(clanId).append("_0\"")
				    .append(" width=48 height=20 back=L2UI_CT1.Button_DF fore=L2UI_CT1.Button_DF>")
				    .append("</td></tr>");
			}
		}

		final String prevBtn = (page > 0)
			? "<button value=上頁 action=\"bypass -h Quest PlunderLands showClanRank_" + (page - 1) + "\" width=60 height=25 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>"
			: "";
		final String nextBtn = (page < totalPages - 1)
			? "<button value=下頁 action=\"bypass -h Quest PlunderLands showClanRank_" + (page + 1) + "\" width=60 height=25 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>"
			: "";

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/PlunderLands/clan_rank.htm");
		html.replace("%rank_rows%", rows.toString());
		html.replace("%page_info%", "第 " + (page + 1) + " / " + totalPages + " 頁　共 " + total + " 盟");
		html.replace("%prev_button%", prevBtn);
		html.replace("%next_button%", nextBtn);
		player.sendPacket(html);
	}

	private void showClanDetail(Npc npc, Player player, int clanId, int page)
	{
		final ConcurrentHashMap<Integer, AtomicInteger> clanPlayers = WEEK_SCORES.get(clanId);
		final List<int[]> members = new ArrayList<>();
		if (clanPlayers != null)
		{
			for (Map.Entry<Integer, AtomicInteger> e : clanPlayers.entrySet())
			{
				members.add(new int[]{ e.getKey(), e.getValue().get() });
			}
		}
		members.sort((a, b) -> b[1] - a[1]);

		final int totalMembers = members.size();
		final int totalPages   = Math.max(1, (int) Math.ceil((double) totalMembers / MEMBER_PER_PAGE));
		page = Math.max(0, Math.min(page, totalPages - 1));
		final int start = page * MEMBER_PER_PAGE;
		final int end   = Math.min(start + MEMBER_PER_PAGE, totalMembers);

		final int    clanTotal = members.stream().mapToInt(m -> m[1]).sum();
		final String clanName  = CLAN_NAMES.getOrDefault(clanId, "未知血盟");

		final StringBuilder rows = new StringBuilder();
		if (totalMembers == 0)
		{
			rows.append("<tr><td colspan=4 align=center><font color=888888>無</font></td></tr>");
		}
		else
		{
			for (int i = start; i < end; i++)
			{
				final int    rank       = i + 1;
				final int    playerId   = members.get(i)[0];
				final int    score      = members.get(i)[1];
				final String playerName = PLAYER_NAMES.getOrDefault(playerId, "未知玩家");
				final int    pct        = clanTotal > 0 ? score * 100 / clanTotal : 0;
				final String color      = rank == 1 ? "FFDD00" : rank == 2 ? "C0C0C0" : rank == 3 ? "CD7F32" : "FFFFFF";
				rows.append("<tr bgcolor=1A1A1A>")
				    .append("<td width=25 align=center><font color=").append(color).append(">").append(rank).append("</font></td>")
				    .append("<td><font color=").append(color).append(">").append(playerName).append("</font></td>")
				    .append("<td width=65 align=right><font color=").append(color).append(">").append(score).append("</font></td>")
				    .append("<td width=45 align=right><font color=FFCC33>").append(pct).append("%</font></td>")
				    .append("</tr>");
			}
		}

		final String prevBtn = (page > 0)
			? "<button value=上頁 action=\"bypass -h Quest PlunderLands clanDetail_" + clanId + "_" + (page - 1) + "\" width=60 height=22 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>"
			: "";
		final String nextBtn = (page < totalPages - 1)
			? "<button value=下頁 action=\"bypass -h Quest PlunderLands clanDetail_" + clanId + "_" + (page + 1) + "\" width=60 height=22 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>"
			: "";

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/PlunderLands/clan_detail.htm");
		html.replace("%clan_name%", clanName);
		html.replace("%clan_total%", String.valueOf(clanTotal));
		html.replace("%member_rows%", rows.toString());
		html.replace("%page_info%", "第 " + (page + 1) + " / " + totalPages + " 頁　共 " + totalMembers + " 人");
		html.replace("%prev_button%", prevBtn);
		html.replace("%next_button%", nextBtn);
		player.sendPacket(html);
	}

	private void showClanOccupy(Npc npc, Player player)
	{
		final Clan clan     = player.getClan();
		final boolean isWinner = (clan != null) && (clan.getId() == LAST_WINNER_CLAN_ID) && (LAST_WINNER_CLAN_ID != 0);

		// 獎勵說明
		final StringBuilder rewardDesc = new StringBuilder();
		for (int[] r : OCCUPATION_REWARDS)
		{
			final String itemName = ItemData.getInstance().getTemplate(r[0]).getName();
			rewardDesc.append(itemName).append(" x").append(r[1]).append("<br1>");
		}

		// BUFF 每技能一行：技能名 | 消耗 | 按鈕（非勝者顯示無法使用）
		final StringBuilder buffRows = new StringBuilder();
		for (int i = 0; i < CLAN_BUFFS.length; i++)
		{
			final int[] b = CLAN_BUFFS[i];
			final Skill buffSkill    = SkillData.getInstance().getSkill(b[0], b[1]);
			final String skillName   = (buffSkill != null ? buffSkill.getName() : "技能 ID:" + b[0]) + " Lv" + b[1];
			final String costName    = ItemData.getInstance().getTemplate(b[2]).getName() + " x" + b[3];
			final String actionCell  = isWinner
				? "<button value=施放 action=\"bypass -h Quest PlunderLands clanBuff_" + i + "\" width=60 height=20 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>"
				: "<font color=888888>禁用</font>";
			buffRows.append("<tr bgcolor=1A1A1A>")
			        .append("<td><font color=AAAAAA size=1>").append(skillName).append("</font></td>")
			        .append("<td><font color=AAAAAA size=1>").append(costName).append("</font></td>")
			        .append("<td align=center>").append(actionCell).append("</td>")
			        .append("</tr>");
		}

		// 是否有待領獎勵
		final boolean hasPending = isWinner && PlunderLandsDAO.getInstance().hasPendingRewards(player.getObjectId(), LAST_WINNER_WEEK_START);

		final String claimBtn = isWinner && hasPending
			? "<button value=\"領取獎勵\" action=\"bypass -h Quest PlunderLands claimReward\" width=200 height=26 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>"
			: "<font color=888888>" + (isWinner ? "已領取或無獎勵" : "僅限上週第一名血盟") + "</font>";

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/PlunderLands/clan_occupy.htm");
		html.replace("%last_winner_name%", LAST_WINNER_CLAN_NAME.isEmpty() ? "<font color=888888>尚無記錄</font>" : "<font color=FFFF44>" + LAST_WINNER_CLAN_NAME + "</font>");
		html.replace("%is_winner%", isWinner ? "<font color=00FF88>是（您的血盟）</font>" : "<font color=FF4444>否</font>");
		html.replace("%reward_desc%", rewardDesc.length() > 0 ? rewardDesc.toString() : "無獎勵設定");
		html.replace("%buff_rows%", buffRows.toString());
		html.replace("%claim_btn%", claimBtn);
		player.sendPacket(html);
	}

	// ==================== 血盟佔領：操作 ====================

	private void handleClaimReward(Npc npc, Player player)
	{
		final Clan clan = player.getClan();
		if ((clan == null) || (clan.getId() != LAST_WINNER_CLAN_ID) || (LAST_WINNER_CLAN_ID == 0))
		{
			player.sendMessage("只有上週第一名血盟的成員才能領取獎勵！");
			showClanOccupy(npc, player);
			return;
		}

		final PlunderLandsDAO dao = PlunderLandsDAO.getInstance();
		final List<long[]> rewards = dao.getPendingRewards(player.getObjectId(), LAST_WINNER_WEEK_START);

		if (rewards.isEmpty())
		{
			player.sendMessage("您目前沒有可領取的獎勵。");
			showClanOccupy(npc, player);
			return;
		}

		for (long[] r : rewards)
		{
			player.addItem(ItemProcessType.NONE, (int) r[0], r[1], npc, true);
		}

		dao.markRewardsClaimed(player.getObjectId(), LAST_WINNER_WEEK_START);

		player.sendMessage("========================================");
		player.sendMessage("已成功領取掠奪之地佔領獎勵！");
		player.sendMessage("========================================");

		showClanOccupy(npc, player);
	}

	private void handleClanBuff(Npc npc, Player player, int idx)
	{
		final Clan clan = player.getClan();
		if ((clan == null) || (clan.getId() != LAST_WINNER_CLAN_ID) || (LAST_WINNER_CLAN_ID == 0))
		{
			player.sendMessage("只有上週第一名血盟的成員才能使用此功能！");
			showClanOccupy(npc, player);
			return;
		}

		if ((idx < 0) || (idx >= CLAN_BUFFS.length))
		{
			showClanOccupy(npc, player);
			return;
		}

		final int[] b    = CLAN_BUFFS[idx];
		final Skill skill = SkillData.getInstance().getSkill(b[0], b[1]);
		if (skill == null)
		{
			player.sendMessage("技能 ID:" + b[0] + " 不存在，請通知管理員。");
			showClanOccupy(npc, player);
			return;
		}

		if (player.getInventory().getInventoryItemCount(b[2], -1) < b[3])
		{
			final String costName = ItemData.getInstance().getTemplate(b[2]).getName();
			player.sendMessage("消耗材料不足！需要 " + costName + " x" + b[3]);
			showClanOccupy(npc, player);
			return;
		}

		player.destroyItemByItemId(ItemProcessType.NONE, b[2], b[3], npc, true);

		int buffedCount = 0;
		for (ClanMember member : clan.getMembers())
		{
			if (member.isOnline())
			{
				final Player memberPlayer = member.getPlayer();
				if (memberPlayer != null)
				{
					skill.applyEffects(player, memberPlayer);
					memberPlayer.broadcastPacket(new MagicSkillUse(player, memberPlayer, b[0], b[1], 0, 0));
					buffedCount++;
				}
			}
		}

		player.sendMessage("========================================");
		player.sendMessage("已對 " + buffedCount + " 位在線血盟成員施放【" + skill.getName() + "】！");
		player.sendMessage("========================================");

		showClanOccupy(npc, player);
	}

	// ==================== 血盟佔領：DB 與結算 ====================

	/** 將 in-memory 積分批次寫入 DB（每60秒執行，及結算前執行） */
	private static void flushScoresToDB()
	{
		PlunderLandsDAO.getInstance().flushScores(WEEK_SCORES, CLAN_NAMES, PLAYER_NAMES);
	}

	/** 計算到下次週一 00:00:05 的延遲並排程結算 */
	private static void scheduleNextSettlement()
	{
		final java.time.LocalDateTime now = java.time.LocalDateTime.now();
		java.time.LocalDateTime next = now
			.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY))
			.withHour(0).withMinute(0).withSecond(5).withNano(0);
		if (!next.isAfter(now))
		{
			next = next.plusWeeks(1);
		}
		final long delayMs = java.time.Duration.between(now, next).toMillis();
		ThreadPool.schedule(PlunderLands::doWeeklySettlement, delayMs);
		LOGGER.info("PlunderLands: 血盟佔領下次結算排程於 " + next);
	}

	/** 每周一 00:00 結算：找勝者 → 寫DB → 發放獎勵 → 清空積分 → 公告 */
	private static void doWeeklySettlement()
	{
		LOGGER.info("PlunderLands: 血盟佔領週結算開始...");
		flushScoresToDB();

		// 從 in-memory 找本週勝者
		int    winnerClanId    = 0;
		String winnerClanName  = "";
		int    winnerTotal     = 0;
		for (Map.Entry<Integer, ConcurrentHashMap<Integer, AtomicInteger>> entry : WEEK_SCORES.entrySet())
		{
			final int t = entry.getValue().values().stream().mapToInt(AtomicInteger::get).sum();
			if (t > winnerTotal)
			{
				winnerTotal    = t;
				winnerClanId   = entry.getKey();
				winnerClanName = CLAN_NAMES.getOrDefault(winnerClanId, "未知血盟");
			}
		}

		final long weekStart = getWeekStart();
		final PlunderLandsDAO dao = PlunderLandsDAO.getInstance();

		if (winnerClanId > 0)
		{
			dao.saveWinner(weekStart, winnerClanId, winnerClanName, winnerTotal);
			dao.saveRewards(weekStart, WEEK_SCORES.get(winnerClanId), winnerTotal, OCCUPATION_REWARDS);

			// 更新 in-memory 勝者資訊
			LAST_WINNER_CLAN_ID    = winnerClanId;
			LAST_WINNER_CLAN_NAME  = winnerClanName;
			LAST_WINNER_WEEK_START = weekStart;
			LAST_WINNER_TOTAL      = winnerTotal;

			final String msg = "【掠奪之地】本週血盟佔領結算！第一名：" + winnerClanName + "（" + winnerTotal + " 積分），請前往 NPC 領取獎勵！";
			Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(msg, 12000));
			Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, "掠奪之地", msg));
		}

		dao.expireUnclaimedRewards(weekStart);
		dao.clearScores();
		WEEK_SCORES.clear();
		scheduleNextSettlement();
		LOGGER.info("PlunderLands: 血盟佔領週結算完成，勝者：" + (winnerClanName.isEmpty() ? "無" : winnerClanName));
	}

	/** 取本週週一 00:00 的毫秒時間戳 */
	private static long getWeekStart()
	{
		return java.time.LocalDateTime.now()
			.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
			.withHour(0).withMinute(0).withSecond(0).withNano(0)
			.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	/** 取 in-memory 中當前積分第一名血盟的顯示字串 */
	private static String getCurrentTopClan()
	{
		String topName  = "";
		int    topScore = 0;
		for (Map.Entry<Integer, ConcurrentHashMap<Integer, AtomicInteger>> entry : WEEK_SCORES.entrySet())
		{
			final int t = entry.getValue().values().stream().mapToInt(AtomicInteger::get).sum();
			if (t > topScore)
			{
				topScore = t;
				topName  = CLAN_NAMES.getOrDefault(entry.getKey(), "未知血盟");
			}
		}
		return topScore > 0 ? topName + "（" + topScore + " 積分）" : "尚無記錄";
	}

	// ==================== 載入入口 ====================

	public static void main(String[] args)
	{
		new PlunderLands();
		System.out.println("【副本】掠奪之地 (PlunderLands) 載入完畢！Template=" + TEMPLATE_ID + " WorldID=" + FIXED_WORLD_ID + " BossThreshold=" + BOSS_KILL_THRESHOLD + " Cooldown=8h");
	}
}
