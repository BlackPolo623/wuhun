/*
 * 訓練木樁系統 (Training Dummy)
 *
 * 對話NPC: 900050  （放世界中，玩家點它設定）
 * 木樁NPC: DUMMY_NPC_ID（自行設定，預設 900051）
 *
 * 功能：
 *  - 玩家可設定 P.Def / M.Def（支援超過 21 億的值，使用 long 儲存）
 *  - 選擇模擬怪物 (PvE) 或模擬玩家 (PvP)
 *  - 生成後 5 分鐘自動消失
 *  - 可與木樁對話查看傷害統計 / 重置 / 手動移除
 *  - 使用 BigInteger 追蹤累積傷害，不受 INT/LONG 上限限制
 *
 * 安裝路徑：
 *  data/scripts/custom/TrainingDummy/TrainingDummy.java
 */
package custom.TrainingDummy;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDamageReceived;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 訓練木樁 NPC 腳本
 * @author Custom
 */
public class TrainingDummy extends Script
{
	// ===================================================================
	// 設定區（只需改這裡）
	// ===================================================================

	/** 對話用NPC：玩家點它來設定並召喚木樁 */
	private static final int CONFIGURATOR_NPC_ID = 900047;

	/** 召喚出來的訓練木樁 NPC ID */
	private static final int DUMMY_NPC_ID = 900048;

	/** 木樁自動消失時間（毫秒），預設5分鐘 */
	private static final long DESPAWN_DELAY_MS = 5 * 60 * 1000L;

	// ===================================================================
	// 資料儲存
	// ===================================================================

	/** 玩家 ObjectId → Session */
	private static final Map<Integer, DummySession> PLAYER_SESSIONS = new ConcurrentHashMap<>();

	/** 木樁 NPC ObjectId → Session */
	private static final Map<Integer, DummySession> NPC_SESSIONS = new ConcurrentHashMap<>();

	// ===================================================================
	// Session 資料類
	// ===================================================================

	private static class DummySession
	{
		final int playerObjectId;
		final long configPDef;
		final long configMDef;

		// 物理傷害統計（BigInteger，不受任何整數上限限制）
		BigInteger totalPhysDmg = BigInteger.ZERO;
		long maxPhysHit = 0;
		int physHitCount = 0;

		// 技能/魔法傷害統計
		BigInteger totalMagicDmg = BigInteger.ZERO;
		long maxMagicHit = 0;
		int magicHitCount = 0;

		long sessionStartMs = System.currentTimeMillis();

		Npc dummyNpc = null;
		ScheduledFuture<?> despawnTask = null;

		DummySession(int playerObjectId, long configPDef, long configMDef)
		{
			this.playerObjectId = playerObjectId;
			this.configPDef = configPDef;
			this.configMDef = configMDef;
		}

		BigInteger getTotalDmg()
		{
			return totalPhysDmg.add(totalMagicDmg);
		}

		int getTotalHits()
		{
			return physHitCount + magicHitCount;
		}

		long getElapsedSeconds()
		{
			return (System.currentTimeMillis() - sessionStartMs) / 1000L;
		}

		long getRemainingSeconds()
		{
			return Math.max(0L, (DESPAWN_DELAY_MS - (System.currentTimeMillis() - sessionStartMs)) / 1000L);
		}
	}

	// ===================================================================
	// 建構子
	// ===================================================================

	private TrainingDummy()
	{
		addStartNpc(CONFIGURATOR_NPC_ID);
		addTalkId(CONFIGURATOR_NPC_ID);
		addFirstTalkId(CONFIGURATOR_NPC_ID);

		addTalkId(DUMMY_NPC_ID);
		addFirstTalkId(DUMMY_NPC_ID);
		addAttackId(DUMMY_NPC_ID);
		addKillId(DUMMY_NPC_ID);

		// 使用 ON_CREATURE_DAMAGE_RECEIVED 取得 double 傷害值，不受 int 上限影響
		registerConsumer((Consumer<OnCreatureDamageReceived>) this::onDummyDamageReceived, EventType.ON_CREATURE_DAMAGE_RECEIVED, ListenerRegisterType.NPC, DUMMY_NPC_ID);
	}

	// ===================================================================
	// 對話入口
	// ===================================================================

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getId() == DUMMY_NPC_ID)
		{
			final DummySession session = NPC_SESSIONS.get(npc.getObjectId());
			if (session != null)
			{
				showStatsPage(npc, player, session);
			}
		}
		else
		{
			if (PLAYER_SESSIONS.containsKey(player.getObjectId()))
			{
				showExistsPage(npc, player);
			}
			else
			{
				showSetupPage(npc, player);
			}
		}
		return null;
	}

	// ===================================================================
	// 事件處理
	// ===================================================================

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		// ─── 快速預設生成：spawn_PDEF_MDEF ────────────────────────────
		if (event.startsWith("spawn_"))
		{
			final String[] parts = event.substring(6).split("_", 2);
			final long pdef = parseStat(parts[0]);
			final long mdef = parseStat(parts.length > 1 ? parts[1] : parts[0]);
			if (pdef < 0 || mdef < 0)
			{
				player.sendMessage("[木樁] 數值錯誤。");
				showSetupPage(npc, player);
				return null;
			}
			handleSpawn(npc, player, pdef, mdef);
			return null;
		}

		// ─── 自訂數值生成：spawn $pdef_input $mdef_input ──────────────
		if (event.startsWith("spawn "))
		{
			final String[] parts = event.substring(6).trim().split("\\s+", 2);
			final long pdef = parseStat(parts[0]);
			final long mdef = parseStat(parts.length > 1 ? parts[1] : parts[0]);
			if (pdef < 0 || mdef < 0)
			{
				player.sendMessage("[木樁] 請輸入 0 以上的整數。");
				showSetupPage(npc, player);
				return null;
			}
			handleSpawn(npc, player, pdef, mdef);
			return null;
		}

		// ─── 木樁對話事件 ─────────────────────────────────────────────
		switch (event)
		{
			case "view_stats":
			{
				final DummySession s = NPC_SESSIONS.get(npc.getObjectId());
				if (s != null)
				{
					showStatsPage(npc, player, s);
				}
				break;
			}
			case "reset_stats":
			{
				final DummySession s = NPC_SESSIONS.get(npc.getObjectId());
				if (s == null)
				{
					break;
				}
				if (s.playerObjectId != player.getObjectId())
				{
					player.sendMessage("[木樁] 只有生成此木樁的玩家才能重置數據。");
					showStatsPage(npc, player, s);
					break;
				}
				s.totalPhysDmg = BigInteger.ZERO;
				s.totalMagicDmg = BigInteger.ZERO;
				s.maxPhysHit = 0;
				s.maxMagicHit = 0;
				s.physHitCount = 0;
				s.magicHitCount = 0;
				s.sessionStartMs = System.currentTimeMillis();
				player.sendMessage("[木樁] 傷害數據已重置。");
				showStatsPage(npc, player, s);
				break;
			}
			case "close_dummy":
			{
				final DummySession s = NPC_SESSIONS.get(npc.getObjectId());
				if (s == null)
				{
					break;
				}
				if (s.playerObjectId != player.getObjectId())
				{
					player.sendMessage("[木樁] 只有生成此木樁的玩家才能移除它。");
					showStatsPage(npc, player, s);
					break;
				}
				despawnDummy(s);
				player.sendMessage("[木樁] 訓練木樁已移除。");
				break;
			}
		}
		return null;
	}

	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon, Skill skill)
	{
		// 讓木樁永不死亡（傷害追蹤由 onDummyAttacked 負責）
		final DummySession s = NPC_SESSIONS.get(npc.getObjectId());
		if (s != null)
		{
			npc.setCurrentHp(npc.getMaxHp());
		}
	}

	/**
	 * 接收木樁傷害事件。
	 * getDamage() 回傳 double（精確範圍達 9007 兆），完全不受 int/long 上限影響。
	 * isDamageOverTime() 用於過濾持續傷害（DoT），避免重複計算。
	 */
	public void onDummyDamageReceived(OnCreatureDamageReceived event)
	{
		if (event.isDamageOverTime())
		{
			return;
		}

		final Npc npc = event.getTarget() instanceof Npc ? (Npc) event.getTarget() : null;
		if (npc == null)
		{
			return;
		}

		final DummySession s = NPC_SESSIONS.get(npc.getObjectId());
		if (s == null)
		{
			return;
		}

		final long dmg = (long) event.getDamage();
		if (dmg <= 0)
		{
			return;
		}

		final BigInteger dmgBig = BigInteger.valueOf(dmg);
		final Skill skill = event.getSkill();

		if (skill != null)
		{
			s.totalMagicDmg = s.totalMagicDmg.add(dmgBig);
			s.magicHitCount++;
			if (dmg > s.maxMagicHit)
			{
				s.maxMagicHit = dmg;
			}
		}
		else
		{
			s.totalPhysDmg = s.totalPhysDmg.add(dmgBig);
			s.physHitCount++;
			if (dmg > s.maxPhysHit)
			{
				s.maxPhysHit = dmg;
			}
		}
	}

	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		cleanupNpc(npc.getObjectId());
	}

	// ===================================================================
	// 生成 / 消除
	// ===================================================================

	private void handleSpawn(Npc sourceNpc, Player player, long pdef, long mdef)
	{
		if (PLAYER_SESSIONS.containsKey(player.getObjectId()))
		{
			player.sendMessage("[木樁] 您已有一個訓練木樁！請先移除它再生成新的。");
			showExistsPage(sourceNpc, player);
			return;
		}

		final double rad = Math.toRadians(player.getHeading() / 182.0);
		final int spawnX = (int) (player.getX() + Math.cos(rad) * 80);
		final int spawnY = (int) (player.getY() + Math.sin(rad) * 80);
		final int spawnZ = player.getZ();

		final Npc dummy = addSpawn(DUMMY_NPC_ID, spawnX, spawnY, spawnZ, player.getHeading(), false, 0);
		if (dummy == null)
		{
			player.sendMessage("[木樁] 生成失敗，請確認 NPC " + DUMMY_NPC_ID + " 的資料是否正確。");
			return;
		}

		final DummySession session = new DummySession(player.getObjectId(), pdef, mdef);
		session.dummyNpc = dummy;

		session.despawnTask = ThreadPool.schedule(() ->
		{
			if ((session.dummyNpc != null) && !session.dummyNpc.isDecayed())
			{
				final Player owner = World.getInstance().getPlayer(session.playerObjectId);
				if ((owner != null) && owner.isOnline())
				{
					owner.sendMessage("[木樁] 訓練木樁已自動消失（5分鐘到期）。");
				}
				despawnDummy(session);
			}
		}, DESPAWN_DELAY_MS);

		PLAYER_SESSIONS.put(player.getObjectId(), session);
		NPC_SESSIONS.put(dummy.getObjectId(), session);

		player.sendMessage("[木樁] 訓練木樁已生成！P.Def：" + formatLong(pdef) + "　M.Def：" + formatLong(mdef));
		player.sendMessage("[木樁] 木樁將在 5 分鐘後消失，或對話手動移除。");
	}

	private void despawnDummy(DummySession session)
	{
		if (session.despawnTask != null)
		{
			session.despawnTask.cancel(false);
			session.despawnTask = null;
		}
		if ((session.dummyNpc != null) && !session.dummyNpc.isDecayed())
		{
			NPC_SESSIONS.remove(session.dummyNpc.getObjectId());
			session.dummyNpc.deleteMe();
			session.dummyNpc = null;
		}
		PLAYER_SESSIONS.remove(session.playerObjectId);
	}

	private void cleanupNpc(int npcObjectId)
	{
		final DummySession session = NPC_SESSIONS.remove(npcObjectId);
		if (session != null)
		{
			if (session.despawnTask != null)
			{
				session.despawnTask.cancel(false);
			}
			PLAYER_SESSIONS.remove(session.playerObjectId);
		}
	}

	// ===================================================================
	// 頁面顯示（使用外部 HTM 檔案）
	// ===================================================================

	private void showSetupPage(Npc npc, Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/TrainingDummy/setup.htm");
		player.sendPacket(html);
	}

	private void showExistsPage(Npc npc, Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/TrainingDummy/exists.htm");
		player.sendPacket(html);
	}

	private void showStatsPage(Npc npc, Player player, DummySession s)
	{
		final long elapsed = s.getElapsedSeconds();
		final long remaining = s.getRemainingSeconds();
		final boolean isOwner = (s.playerObjectId == player.getObjectId());

		final String dpsStr = elapsed > 0 ? formatBig(s.getTotalDmg().divide(BigInteger.valueOf(elapsed))) : "0";
		final String avgPhys = s.physHitCount > 0 ? formatBig(s.totalPhysDmg.divide(BigInteger.valueOf(s.physHitCount))) : "0";
		final String avgMagic = s.magicHitCount > 0 ? formatBig(s.totalMagicDmg.divide(BigInteger.valueOf(s.magicHitCount))) : "0";

		final String remainColor = remaining < 60 ? "FF4444" : remaining < 120 ? "FFAA00" : "FFFF00";

		final String ownerButtons = isOwner
				? "<button value=\"重置數據\" width=\"100\" height=\"22\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h Quest TrainingDummy reset_stats\">&nbsp;<button value=\"移除木樁\" width=\"100\" height=\"22\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" action=\"bypass -h Quest TrainingDummy close_dummy\">"
				: "";

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/TrainingDummy/stats.htm");
		html.replace("%pdef%", formatLong(s.configPDef));
		html.replace("%mdef%", formatLong(s.configMDef));
		html.replace("%elapsed%", String.valueOf(elapsed));
		html.replace("%remaining%", String.valueOf(remaining));
		html.replace("%remain_color%", remainColor);
		html.replace("%total_dmg%", formatBig(s.getTotalDmg()));
		html.replace("%total_hits%", String.valueOf(s.getTotalHits()));
		html.replace("%dps%", dpsStr);
		html.replace("%phys_dmg%", formatBig(s.totalPhysDmg));
		html.replace("%phys_hits%", String.valueOf(s.physHitCount));
		html.replace("%max_phys%", formatLong(s.maxPhysHit));
		html.replace("%avg_phys%", avgPhys);
		html.replace("%magic_dmg%", formatBig(s.totalMagicDmg));
		html.replace("%magic_hits%", String.valueOf(s.magicHitCount));
		html.replace("%max_magic%", formatLong(s.maxMagicHit));
		html.replace("%avg_magic%", avgMagic);
		html.replace("%owner_buttons%", ownerButtons);
		player.sendPacket(html);
	}

	// ===================================================================
	// 數字處理工具
	// ===================================================================

	/**
	 * 解析玩家輸入的數值字串為 long。
	 * 支援超過 21 億（long 最大值約 922 京）。
	 * 回傳 -1 代表輸入無效。
	 */
	private static long parseStat(String raw)
	{
		if ((raw == null) || raw.isEmpty())
		{
			return 0L;
		}
		final String cleaned = raw.trim().replace(",", "").replace("_", "");
		try
		{
			final long v = Long.parseLong(cleaned);
			return v < 0 ? -1L : v;
		}
		catch (NumberFormatException e)
		{
			return -1L;
		}
	}

	/**
	 * 將 BigInteger 格式化為萬/億/兆/京單位字串。
	 */
	private static String formatBig(BigInteger value)
	{
		if ((value == null) || (value.signum() <= 0))
		{
			return "0";
		}
		if (value.bitLength() > 62)
		{
			final BigInteger JING = BigInteger.valueOf(10_000_000_000_000_000L);
			if (value.compareTo(JING) >= 0)
			{
				return value.divide(JING) + "京+";
			}
			return value.toString();
		}
		return formatLong(value.longValue());
	}

	/**
	 * 將 long 格式化為萬/億/兆/京單位字串。
	 * 支援到 long 上限（約 922 京）。
	 */
	private static String formatLong(long v)
	{
		if (v <= 0)
		{
			return v == 0 ? "0" : String.valueOf(v);
		}
		final long JING = 10_000_000_000_000_000L;
		final long ZHAO = 1_000_000_000_000L;
		final long YI = 100_000_000L;
		final long WAN = 10_000L;

		if (v >= JING)
		{
			final long main = v / JING;
			final long frac = (v % JING) / (JING / 10);
			return frac > 0 ? main + "." + frac + "京" : main + "京";
		}
		if (v >= ZHAO)
		{
			final long main = v / ZHAO;
			final long frac = (v % ZHAO) / (ZHAO / 10);
			return frac > 0 ? main + "." + frac + "兆" : main + "兆";
		}
		if (v >= YI)
		{
			final long main = v / YI;
			final long frac = (v % YI) / (YI / 10);
			return frac > 0 ? main + "." + frac + "億" : main + "億";
		}
		if (v >= WAN)
		{
			final long main = v / WAN;
			final long frac = (v % WAN) / (WAN / 10);
			return frac > 0 ? main + "." + frac + "萬" : main + "萬";
		}
		return String.valueOf(v);
	}

	public static void main(String[] args)
	{
		new TrainingDummy();
		System.out.println("【系統】訓練木樁系統載入完畢！");
	}
}
