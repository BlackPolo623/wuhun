package handlers.voicedcommandhandlers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import java.awt.Color;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.discord.DiscordDAO;
import org.l2jmobius.discord.DiscordManager;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.handler.VoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDamageDealt;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;

public class DamageAnalyzer implements IVoicedCommandHandler
{
	private static final int DEFAULT_DURATION = 60;
	private static final int MAX_DURATION = 300;

	// ★ 允許記錄的目標怪物 ID 白名單，空集合 = 允許所有目標
	private static final Set<Integer> ALLOWED_NPC_IDS = Set.of(
			70000,
			70001,
			61003,
			61004,
			61005,
			61006,
			61007
	);

	private static final String[] COMMANDS =
	{
		"dmg", "傷害",
		"dmgstart", "傷害開始",
		"dmgstop", "傷害停止",
		"dmgstatus", "傷害狀態",
	};

	private static final Map<Integer, Session> SESSIONS = new ConcurrentHashMap<>();

	// ── Inner classes ────────────────────────────────────────────────────────

	private static class SkillStats
	{
		final String name;
		long hitCount = 0;
		long critCount = 0;
		double total = 0;
		double totalCritDamage = 0;
		double maxHit = 0;

		SkillStats(String name)
		{
			this.name = name;
		}

		void record(double dmg, boolean crit)
		{
			hitCount++;
			total += dmg;
			if (dmg > maxHit)
			{
				maxHit = dmg;
			}
			if (crit)
			{
				critCount++;
				totalCritDamage += dmg;
			}
		}

		double avgCrit()
		{
			return critCount > 0 ? totalCritDamage / critCount : 0;
		}

		double critRate()
		{
			return hitCount > 0 ? critCount * 100.0 / hitCount : 0;
		}
	}

	private static class Session
	{
		final int playerId;
		final long startTime;
		final int durationSec;
		final Map<String, SkillStats> skills = new ConcurrentHashMap<>();
		ConsumerEventListener listener;
		ScheduledFuture<?> expireTask;

		long totalHits = 0;
		long totalCrits = 0;
		double totalDamage = 0;
		double totalCritDamage = 0;
		double maxSingleHit = 0;
		String maxSingleHitSkill = "";
		double maxCritHit = 0;
		String maxCritHitSkill = "";

		Session(int playerId, int durationSec)
		{
			this.playerId = playerId;
			this.startTime = System.currentTimeMillis();
			this.durationSec = durationSec;
		}

		void record(double dmg, boolean crit, String skillName)
		{
			totalHits++;
			totalDamage += dmg;
			if (dmg > maxSingleHit)
			{
				maxSingleHit = dmg;
				maxSingleHitSkill = skillName;
			}
			if (crit)
			{
				totalCrits++;
				totalCritDamage += dmg;
				if (dmg > maxCritHit)
				{
					maxCritHit = dmg;
					maxCritHitSkill = skillName;
				}
			}
			skills.computeIfAbsent(skillName, SkillStats::new).record(dmg, crit);
		}

		long elapsedSec()
		{
			return (System.currentTimeMillis() - startTime) / 1000;
		}
	}

	// ── Constructor ──────────────────────────────────────────────────────────

	public DamageAnalyzer()
	{
	}

	// ── IVoicedCommandHandler ────────────────────────────────────────────────

	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		// Discord Bot 已啟用時，必須先完成綁定才可使用
		if (DiscordManager.getInstance().isRunning() && (DiscordDAO.getDiscordId(player.getObjectId()) == null))
		{
			player.sendMessage("您尚未跟 Discord Bot 進行綁定，請前往 Discord 綁定 NPC 完成綁定後再使用。");
			return true;
		}

		switch (command)
		{
			case "dmg":
			case "傷害":
			{
				handleHelp(player);
				break;
			}
			case "dmgstart":
			case "傷害開始":
			{
				handleStart(player, params);
				break;
			}
			case "dmgstop":
			case "傷害停止":
			{
				handleStop(player);
				break;
			}
			case "dmgstatus":
			case "傷害狀態":
			{
				handleStatus(player);
				break;
			}
		}
		return true;
	}

	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}

	// ── Command handlers ─────────────────────────────────────────────────────

	private void handleHelp(Player player)
	{
		player.sendMessage("━━━━ 傷害分析系統 ━━━━");
		player.sendMessage(".傷害開始 [秒數] / .dmgstart [秒] — 開始測試（預設60秒，上限300秒）");
		player.sendMessage(".傷害停止 / .dmgstop — 提前結束並輸出報告");
		player.sendMessage(".傷害狀態 / .dmgstatus — 查看目前即時數據");
		player.sendMessage("━━━━━━━━━━━━━━━━━━━━━");
	}

	private void handleStart(Player player, String params)
	{
		if (SESSIONS.containsKey(player.getObjectId()))
		{
			player.sendMessage("[傷害分析] 測試已在進行中，請先使用 .傷害停止 結束。");
			return;
		}

		int duration = DEFAULT_DURATION;
		if ((params != null) && !params.trim().isEmpty())
		{
			try
			{
				duration = Integer.parseInt(params.trim());
				if (duration < 1)
				{
					duration = DEFAULT_DURATION;
				}
				else if (duration > MAX_DURATION)
				{
					duration = MAX_DURATION;
					player.sendMessage("[傷害分析] 秒數上限為 " + MAX_DURATION + " 秒，已自動調整。");
				}
			}
			catch (NumberFormatException ignored)
			{
				player.sendMessage("[傷害分析] 秒數格式錯誤，使用預設 " + DEFAULT_DURATION + " 秒。");
			}
		}

		final Session session = new Session(player.getObjectId(), duration);

		final ConsumerEventListener listener = new ConsumerEventListener(player, EventType.ON_CREATURE_DAMAGE_DEALT, (OnCreatureDamageDealt event) ->
		{
			if (event.isDamageOverTime() || event.isReflect())
			{
				return;
			}
			// 白名單過濾：目標必須是 NPC 且 ID 在允許清單內
			if (!ALLOWED_NPC_IDS.isEmpty())
			{
				if (!event.getTarget().isNpc() || !ALLOWED_NPC_IDS.contains(event.getTarget().getId()))
				{
					return;
				}
			}
			double dmg = event.getDamage();
			// 補上武器屬性平傷（calcAttributeFlatBonus 在事件觸發後才套用）
			dmg += Formulas.calcAttributeFlatBonus(event.getAttacker(), event.getTarget(), event.getSkill());
			if (dmg < 0)
			{
				dmg = 0;
			}
			final String skillName = (event.getSkill() != null) ? event.getSkill().getName() : "普通攻擊";
			session.record(dmg, event.isCritical(), skillName);
		}, session);

		session.listener = listener;
		player.addListener(listener);

		final int finalDuration = duration;
		session.expireTask = ThreadPool.schedule(() -> finishSession(player, session), finalDuration * 1000L);

		SESSIONS.put(player.getObjectId(), session);
		player.sendMessage("[傷害分析] 測試開始！持續 " + duration + " 秒，對任何目標造成傷害即可記錄。");
		if (!ALLOWED_NPC_IDS.isEmpty())
		{
			player.sendMessage("[傷害分析] 僅記錄指定目標的傷害（NPC ID: " + ALLOWED_NPC_IDS + "）。");
		}
		player.sendMessage("[傷害分析] 使用 .傷害停止 可提前結束。");
	}

	private void handleStop(Player player)
	{
		final Session session = SESSIONS.get(player.getObjectId());
		if (session == null)
		{
			player.sendMessage("[傷害分析] 目前沒有進行中的測試。使用 .傷害開始 開始測試。");
			return;
		}
		if (session.expireTask != null)
		{
			session.expireTask.cancel(false);
		}
		finishSession(player, session);
	}

	private void handleStatus(Player player)
	{
		final Session session = SESSIONS.get(player.getObjectId());
		if (session == null)
		{
			player.sendMessage("[傷害分析] 目前沒有進行中的測試。使用 .傷害開始 開始測試。");
			return;
		}

		final long elapsed = session.elapsedSec();
		final long remaining = session.durationSec - elapsed;
		player.sendMessage("━━━━ 即時狀態 ━━━━");
		player.sendMessage("剩餘時間: " + remaining + " 秒");
		player.sendMessage("總傷害: " + fmt(session.totalDamage));
		if (elapsed > 0)
		{
			player.sendMessage("DPS: " + fmt(session.totalDamage / elapsed) + "/秒");
		}
		player.sendMessage("攻擊次數: " + session.totalHits + "  暴擊: " + session.totalCrits);
		player.sendMessage("━━━━━━━━━━━━━━━━━");
	}

	// ── Session finalization ─────────────────────────────────────────────────

	private void finishSession(Player player, Session session)
	{
		SESSIONS.remove(player.getObjectId());
		if (session.listener != null)
		{
			player.removeListenerIf(EventType.ON_CREATURE_DAMAGE_DEALT, l -> l.getOwner() == session);
		}

		final long elapsed = Math.max(session.elapsedSec(), 1);

		if (session.totalHits == 0)
		{
			player.sendMessage("[傷害分析] 測試結束，期間未記錄到任何傷害。");
			return;
		}

		final double dps = session.totalDamage / elapsed;
		final double critRate = session.totalHits > 0 ? (session.totalCrits * 100.0 / session.totalHits) : 0;

		player.sendMessage("[傷害分析] 測試結束！報告已發送至您的 Discord 私訊。");

		// ── 發送 Discord Embed 報告 ───────────────────────────────────────────
		sendDiscordReport(player, session, elapsed, dps, critRate);
	}

	private void sendDiscordReport(Player player, Session session, long elapsed, double dps, double critRate)
	{
		if (!DiscordManager.getInstance().isRunning())
		{
			return;
		}

		final double avgHit = session.totalHits > 0 ? session.totalDamage / session.totalHits : 0;
		final double avgCrit = session.totalCrits > 0 ? session.totalCritDamage / session.totalCrits : 0;
		final double critDmgPct = session.totalDamage > 0 ? session.totalCritDamage * 100.0 / session.totalDamage : 0;

		final EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("⚔️  傷害分析報告");
		eb.setDescription("🎮 角色：**" + player.getName() + "**　┃　⏱️ 測試時長：**" + elapsed + " 秒**");
		eb.setColor(new Color(0xF57C00)); // 橙色

		// ── 第一列：傷害總覽 ─────────────────────────────────────────────────
		eb.addField("🔥 總傷害", fmt(session.totalDamage), true);
		eb.addField("🎯 DPS", fmt(dps) + " / 秒", true);
		eb.addField("📈 平均每擊", fmt(avgHit), true);

		// ── 第二列：暴擊統計 ─────────────────────────────────────────────────
		eb.addField("⚔️ 攻擊次數", session.totalHits + " 次", true);
		eb.addField("💥 暴擊次數", session.totalCrits + " 次", true);
		eb.addField("📊 暴擊率", round1(critRate) + "%", true);

		// ── 第三列：暴擊深度（有暴擊時才顯示）──────────────────────────────
		if (session.totalCrits > 0)
		{
			eb.addField("🌟 平均暴擊", fmt(avgCrit), true);
			eb.addField("💢 暴擊傷害佔比", round1(critDmgPct) + "%", true);
			eb.addField("​", "​", true); // 空白補位
		}

		// ── 傷害來源（前10名）────────────────────────────────────────────────
		final StringBuilder skillsSb = new StringBuilder();
		final long[] rank = {1};

		session.skills.entrySet().stream()
			.sorted((a, b) -> Double.compare(b.getValue().total, a.getValue().total))
			.limit(10)
			.forEach(entry ->
			{
				final SkillStats s = entry.getValue();
				final double pct = session.totalDamage > 0 ? (s.total * 100.0 / session.totalDamage) : 0;
				final String medal = rank[0] == 1 ? "🥇" : rank[0] == 2 ? "🥈" : rank[0] == 3 ? "🥉" : "🔸";
				rank[0]++;

				skillsSb.append(medal).append(" **").append(s.name).append("**");
				skillsSb.append(" — `").append(fmt(s.total)).append("`");
				skillsSb.append("  **").append(round1(pct)).append("%**");
				skillsSb.append("  ×").append(s.hitCount).append("次");

				if (s.critCount > 0)
				{
					skillsSb.append("  💥 ").append(round1(s.critRate())).append("%");
					skillsSb.append("（均 ").append(fmt(s.avgCrit())).append("）");
				}
				skillsSb.append("\n");
			});

		eb.addField("🗡️ 傷害來源（前10名）", skillsSb.toString(), false);

		// ── 極值記錄 ─────────────────────────────────────────────────────────
		final StringBuilder peakSb = new StringBuilder();
		peakSb.append("🥇 最高單擊：**").append(fmt(session.maxSingleHit)).append("**（").append(session.maxSingleHitSkill).append("）\n");
		if (session.maxCritHit > 0)
		{
			peakSb.append("🏆 最高暴擊：**").append(fmt(session.maxCritHit)).append("**（").append(session.maxCritHitSkill).append("）");
		}
		eb.addField("🏅 極值記錄", peakSb.toString(), false);

		eb.setFooter("武魂天堂2 • 傷害分析系統");

		DiscordManager.getInstance().sendPlayerEmbed(player.getObjectId(), eb.build());
	}

	// ── Formatting helpers ───────────────────────────────────────────────────

	private static String fmt(double dmg)
	{
		if (dmg >= 1_000_000_000.0)
		{
			return round2(dmg / 1_000_000_000.0) + "B";
		}
		if (dmg >= 1_000_000.0)
		{
			return round2(dmg / 1_000_000.0) + "M";
		}
		if (dmg >= 1_000.0)
		{
			return round2(dmg / 1_000.0) + "K";
		}
		return String.valueOf((long) dmg);
	}

	private static String round2(double v)
	{
		return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String round1(double v)
	{
		return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).toPlainString();
	}
}
