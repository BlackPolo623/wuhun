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
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
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
	private static final int RANK_ITEM_ID = 57;

	/** 排行榜每頁顯示人數 */
	private static final int PLAYERS_PER_PAGE = 12;

	/** 副本入口傳送座標 */
	private static final Location ENTER_LOCATION = new Location(16752, 214188, -15174);

	// ==================== Boss 觸發設定 ====================

	/**
	 * 副本內怪物累積擊殺達此數量後，開始 60 秒倒數並生成 Boss。
	 * Boss 死亡後計數歸零，且需等待 8 小時冷卻才能再次觸發。
	 */
	private static final int BOSS_KILL_THRESHOLD = 100;

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
		22740, // Harid Lizardman Berserker
		22741, // Harid Lizardman Warrior
		22742, // Harid Lizardman Shaman
		22743, // Harid Lizardman
		22744, // Tantar Warrior
		22745, // Tantar Herbalist
		22747, // Zenta Lizardman Warrior
		22748, // Zenta Lizardman Shaman
		22749, // Zenta Lizardman Archer
		22750, // Tantar Warrior (Zenta)
		22751, // Tantar Herbalist (Zenta)
	};

	// ==================== 執行時狀態（靜態，對應單一共享世界）====================

	/**
	 * 當前副本世界的怪物擊殺累積計數。
	 * AtomicInteger 確保多執行緒安全。
	 */
	private static final AtomicInteger KILL_COUNTER = new AtomicInteger(0);

	/**
	 * 是否有 Boss 正在副本內存活，或正在倒數中。
	 * 為 true 時不再因擊殺計數觸發新的倒數/Boss 生成。
	 */
	private static volatile boolean BOSS_ACTIVE = false;

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
				handleEnter(npc, player);
				break;
			}
			case "mainPage":
			{
				showMainPage(npc, player);
				break;
			}
			default:
			{
				if (event.startsWith("showRank_"))
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
		BOSS_ACTIVE = false;
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
			BOSS_ACTIVE = false;
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

		// 若 Boss 正在場上（或倒數中），不再累積計數
		if (BOSS_ACTIVE)
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

			// 鎖定狀態，防止重複觸發
			BOSS_ACTIVE = true;
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
			BOSS_ACTIVE = false;
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
			BOSS_ACTIVE = false;
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
		Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.WORLD, "掠奪之地", msg));
	}

	// ==================== 進入副本 ====================

	private void handleEnter(Npc npc, Player player)
	{
		// 已在副本內則拒絕重複進入
		final Instance current = player.getInstanceWorld();
		if ((current != null) && (current.getTemplateId() == TEMPLATE_ID))
		{
			player.sendMessage("[掠奪之地] 你已經在副本內了！");
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
		player.teleToLocation(ENTER_LOCATION, world);
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
		if (BOSS_ACTIVE)
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
		if (BOSS_ACTIVE)
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
			rows.append("<tr><td colspan=\"3\"><center><font color=\"888888\">目前副本內沒有玩家。</font></center></td></tr>");
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
					.append("<td align=\"right\"><font color=\"").append(color).append("\">").append(itemCount).append("</font></td>")
					.append("</tr>");
			}
		}

		// 翻頁按鈕
		final String prevBtn = (page > 0)
			? "<button action=\"bypass -h Quest PlunderLands showRank_" + (page - 1) + "\" value=\"&lt;&lt; 上頁\" width=\"90\" height=\"22\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">"
			: "";

		final String nextBtn = (page < totalPages - 1)
			? "<button action=\"bypass -h Quest PlunderLands showRank_" + (page + 1) + "\" value=\"下頁 &gt;&gt;\" width=\"90\" height=\"22\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">"
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

	// ==================== 載入入口 ====================

	public static void main(String[] args)
	{
		new PlunderLands();
		System.out.println("【副本】掠奪之地 (PlunderLands) 載入完畢！Template=" + TEMPLATE_ID + " WorldID=" + FIXED_WORLD_ID + " BossThreshold=" + BOSS_KILL_THRESHOLD + " Cooldown=8h");
	}
}
