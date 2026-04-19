/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.MorphData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.model.morph.MorphEntry;
import org.l2jmobius.gameserver.model.morph.MorphStatEntry;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;

/**
 * 變身系統管理器 MorphManager
 * 流程設計：
 *   1. 卡牌激活（activateByItem）→ 僅收藏，寫 DB，不改外觀/屬性
 *   2. 變身捲軸 UI → 選中已收藏變身 → applyVisualMorph() 應用外觀+屬性，持續 30 分鐘
 *   3. 30 分鐘后或主動取消 → removeVisualMorph() 恢復原貌，移除屬性
 *   4. 自動續變身：啟用后，到期時消耗 1 張卷軸重新應用同一變身
 *
 * DB 表：player_morph (char_id INT, morph_id INT, grade_level INT, PRIMARY KEY (char_id, morph_id))
 * @author Custom
 */
public class MorphManager
{
	private static final Logger LOGGER = Logger.getLogger(MorphManager.class.getName());

	/** 變身外觀持續時間（毫秒） */
	private static final long VISUAL_DURATION_MS = 30L * 60L * 1000L; // 30 分鐘

	/** 激活/自動續 所需道具 ID（變身卷軸） */
	public static final int MORPH_SCROLL_ITEM_ID = 109006;

	/** 階級名稱（index 0 = grade 1） */
	private static final String[] GRADE_NAMES =
	{
		"一般",
		"高級",
		"稀有",
		"英雄",
		"傳說",
		"神話"
	};

	// ── SQL ──────────────────────────────────────────────────────────────

	private static final String SQL_LOAD = "SELECT morph_id, grade_level FROM player_morph WHERE char_id = ?";
	private static final String SQL_UPSERT = "INSERT INTO player_morph (char_id, morph_id, grade_level) VALUES (?,?,?) " + "ON DUPLICATE KEY UPDATE grade_level=VALUES(grade_level)";

	// ── 內存緩存 ──────────────────────────────────────────────────────────

	/** charId → (morphId → collectedGradeLevel) */
	private final Map<Integer, Map<Integer, Integer>> _cache = new ConcurrentHashMap<>();

	/** charId → 當前激活外觀的 morphId（不在 Map 中 = 無激活） */
	private final Map<Integer, Integer> _activeVisualMorph = new ConcurrentHashMap<>();

	/** charId → 30 分鐘倒計時任務 */
	private final Map<Integer, ScheduledFuture<?>> _activeVisualTimer = new ConcurrentHashMap<>();

	/** 已開啟自動續變身的 charId 集合 */
	private final Set<Integer> _autoRenewPlayers = ConcurrentHashMap.newKeySet();

	/** charId → 變身到期時間戳（毫秒），供 UI 顯示剩餘時間 */
	private final Map<Integer, Long> _morphEndTime = new ConcurrentHashMap<>();

	// ── 構造器 ────────────────────────────────────────────────────────────

	protected MorphManager()
	{
	}

	// ── 階級名稱工具 ──────────────────────────────────────────────────────

	/**
	 * 返回指定 grade（1-based）對應的中文階級名稱。
	 */
	public static String getGradeName(int grade)
	{
		if ((grade >= 1) && (grade <= GRADE_NAMES.length))
		{
			return GRADE_NAMES[grade - 1];
		}
		return "第" + grade + "階";
	}

	// ── DB 加載 / 卸載 ────────────────────────────────────────────────────

	public void loadPlayer(Player player)
	{
		final int charId = player.getObjectId();
		final Map<Integer, Integer> map = new ConcurrentHashMap<>();

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_LOAD))
		{
			ps.setInt(1, charId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final int morphId = rs.getInt("morph_id");
					final int gradeLevel = rs.getInt("grade_level");
					if (gradeLevel > 0)
					{
						map.put(morphId, gradeLevel);
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "MorphManager: Cannot load morph for charId=" + charId, e);
		}

		_cache.put(charId, map);
	}

	public void unloadPlayer(int charId)
	{
		// 取消 30 分鐘倒計時（玩家下線時變身自動結束）
		final ScheduledFuture<?> timer = _activeVisualTimer.remove(charId);
		if ((timer != null) && !timer.isDone())
		{
			timer.cancel(false);
		}
		_activeVisualMorph.remove(charId);
		_autoRenewPlayers.remove(charId);
		_morphEndTime.remove(charId);
		_cache.remove(charId);
	}

	// ── 卡牌激活（僅收藏，不應用外觀）────────────────────────────────────

	/**
	 * 通過道具 ID 激活變身卡（僅將變身收入圖鑑，不改變外觀/屬性）。
	 * 如果當前階級 >= 目標階級則拒絕。
	 * @return true = 成功收藏
	 */
	public boolean activateByItem(Player player, int itemId)
	{
		final MorphEntry targetEntry = MorphData.getInstance().getEntryByItemId(itemId);
		if (targetEntry == null)
		{
			return false;
		}

		final int morphId = targetEntry.getMorphId();

		// 確定目標階級
		int entryGradeLevel = 0;
		outer: for (org.l2jmobius.gameserver.model.morph.MorphGradeHolder grade : MorphData.getInstance().getGradeList())
		{
			for (MorphEntry e : grade.getEntries())
			{
				if (e.getItemId() == itemId)
				{
					entryGradeLevel = grade.getLevel();
					break outer;
				}
			}
		}

		if (entryGradeLevel == 0)
		{
			LOGGER.warning("MorphManager: Cannot determine grade level for itemId=" + itemId);
			return false;
		}

		final int currentLevel = getActivatedGradeLevel(player, morphId);
		if (currentLevel >= entryGradeLevel)
		{
			player.sendMessage("變身「" + targetEntry.getName() + "」【" + getGradeName(currentLevel) + "】已擁有無需重複激活。");
			return false;
		}

		// 消耗道具
		if (!player.destroyItemByItemId(null, itemId, 1, player, true))
		{
			player.sendMessage("激活失敗：道具數量不足。");
			return false;
		}

		// 更新緩存 + DB（僅收藏記錄，不改外觀）
		getOrCreateCache(player).put(morphId, entryGradeLevel);
		savePlayerMorph(player.getObjectId(), morphId, entryGradeLevel);

		player.sendMessage("========================================");
		player.sendMessage("已收藏變身「" + targetEntry.getName() + "」【" + getGradeName(entryGradeLevel) + "】！");
		player.sendMessage("使用變身捲軸可查看並應用已收藏的變身外觀。");
		player.sendMessage("========================================");
		return true;
	}

	// ── 應用視覺變身（外觀 + 屬性，持續 30 分鐘）─────────────────────────

	/**
	 * 將指定 morphId 的外觀應用到玩家身上，持續 30 分鐘。
	 * 使用玩家已收藏的最高階級數據（外觀 NPC + 屬性 + AVE）。
	 * 若已有激活變身則先移除。
	 */
	public void applyVisualMorph(Player player, int morphId)
	{
		final int collectedGrade = getActivatedGradeLevel(player, morphId);
		if (collectedGrade <= 0)
		{
			player.sendMessage("你尚未收藏該變身，請先使用變身卡激活。");
			return;
		}

		final MorphEntry entry = MorphData.getInstance().getEntry(morphId, collectedGrade);
		if (entry == null)
		{
			player.sendMessage("找不到變身數據（morphId=" + morphId + " grade=" + collectedGrade + "）。");
			return;
		}

		// 若已有激活變身，先靜默清除舊緩存（不調用 stopTransformation，避免觸發原生邏輯）
		final int charId = player.getObjectId();
		cancelVisualTimerSilently(charId);

		// 記錄當前激活（必須在 updateEffectList 之前設置，
		// 這樣 pumpMorphStats 和 getPersistentAbnormalVisualEffects 才能讀到正確值）
		_activeVisualMorph.put(charId, morphId);

		// 應用變身時自動開啟自動續身
		_autoRenewPlayers.add(charId);

		// 記錄到期時間（供 UI 顯示剩餘時間）
		_morphEndTime.put(charId, System.currentTimeMillis() + VISUAL_DURATION_MS);

		// updateEffectList() 一步完成：
		//   1. 重建 AVE 集合（含 getPersistentAbnormalVisualEffects → morph AVE）
		//   2. recalculateStats(true) → pumpMorphStats 注入變身屬性
		//   3. _abnormalVisualEffects 更新后自動調 updateAbnormalVisualEffects()
		//      → 發 ExUserInfoAbnormalVisualEffect（transformId + AVE）給自己
		//      → broadcastCharInfo（transformationDisplayId）給周圍玩家
		player.getEffectList().updateEffectList();

		// 安排 30 分鐘后自動移除
		final ScheduledFuture<?> timer = ThreadPool.schedule(() -> removeVisualMorph(player), VISUAL_DURATION_MS);
		_activeVisualTimer.put(charId, timer);

		player.sendMessage("========================================");
		player.sendMessage("已變身為「" + entry.getName() + "」【" + getGradeName(collectedGrade) + "】，持續 30 分鐘。");
		player.sendMessage("========================================");
	}

	/**
	 * 玩家手動取消變身（點擊「取消變身」按鈕）。
	 * 強制關閉自動續，然後移除變身，不會觸發自動續邏輯。
	 */
	public void cancelMorphManually(Player player)
	{
		_autoRenewPlayers.remove(player.getObjectId()); // 先關閉自動續，防止 removeVisualMorph 觸發
		removeVisualMorph(player);
	}

	/**
	 * 移除當前激活的視覺變身（恢復原貌，清除屬性加成）。
	 * 若自動續變身已啟用，優先嘗試消耗 1 張卷軸重新應用。
	 */
	public void removeVisualMorph(Player player)
	{
		final int charId = player.getObjectId();
		if (!_activeVisualMorph.containsKey(charId))
		{
			return; // 沒有激活變身，無需處理
		}

		final int morphId = _activeVisualMorph.get(charId);

		// 自動續變身：到期后嘗試消耗卷軸重新應用
		if (_autoRenewPlayers.contains(charId))
		{
			if (player.destroyItemByItemId(null, MORPH_SCROLL_ITEM_ID, 1, player, true))
			{
				// 消耗成功 → 重新應用（applyVisualMorph 會重置計時器、endTime、發送訊息）
				applyVisualMorph(player, morphId);
				return;
			}
			// 卷軸不足 → 關閉自動續，繼續正常移除流程
			_autoRenewPlayers.remove(charId);
			player.sendMessage("卷軸不足，自動續變身已自動關閉。");
		}

		cancelVisualTimerSilently(charId);
		_activeVisualMorph.remove(charId);
		_morphEndTime.remove(charId);

		// updateEffectList() 一步完成：
		//   1. getPersistentAbnormalVisualEffects 返回空集（無激活變身）→ AVE 被移除
		//   2. recalculateStats(true) → pumpMorphStats 不注入任何屬性（變身已清除）
		//   3. _abnormalVisualEffects 變化 → updateAbnormalVisualEffects()
		//      → 發 transformId=0 + 空 AVE 列表給客戶端 → 客戶端恢復原貌
		player.getEffectList().updateEffectList();

		player.sendMessage("變身已結束，已恢復原始外觀。");
	}

	// ── 自動續變身 ────────────────────────────────────────────────────────

	/**
	 * 切換自動續變身狀態（啟用↔關閉）。
	 */
	public void toggleAutoRenew(Player player)
	{
		final int charId = player.getObjectId();
		if (_autoRenewPlayers.contains(charId))
		{
			_autoRenewPlayers.remove(charId);
			player.sendMessage("自動續變身已關閉。");
		}
		else
		{
			_autoRenewPlayers.add(charId);
			player.sendMessage("自動續變身已開啟，到期時將消耗 1 張卷軸自動續身。");
		}
	}

	/**
	 * 查詢自動續變身是否已啟用。
	 */
	public boolean isAutoRenewEnabled(Player player)
	{
		return _autoRenewPlayers.contains(player.getObjectId());
	}

	/**
	 * 返回當前變身剩餘時間（毫秒），若無激活變身返回 0。
	 */
	public long getRemainingTimeMs(Player player)
	{
		final Long endTime = _morphEndTime.get(player.getObjectId());
		if (endTime == null)
		{
			return 0L;
		}
		return Math.max(0L, endTime - System.currentTimeMillis());
	}

	// ── 屬性注入（僅注入當前激活外觀的屬性）─────────────────────────────

	/**
	 * 在 PlayerStat.resetStats() 中被調用，注入當前激活視覺變身的屬性。
	 * 若無激活變身則不注入任何屬性。
	 */
	public void pumpMorphStats(Player player, CreatureStat stat)
	{
		final int charId = player.getObjectId();
		final Integer morphId = _activeVisualMorph.get(charId);
		if (morphId == null)
		{
			return; // 無激活變身
		}

		final int gradeLevel = getActivatedGradeLevel(player, morphId);
		if (gradeLevel <= 0)
		{
			return;
		}

		final MorphEntry entry = MorphData.getInstance().getEntry(morphId, gradeLevel);
		if ((entry == null) || !entry.hasStats())
		{
			return;
		}

		for (MorphStatEntry morphStat : entry.getStats())
		{
			if (morphStat.isMultiply())
			{
				stat.mergeMul(morphStat.getStat(), morphStat.getMulFactor());
			}
			else
			{
				stat.mergeAdd(morphStat.getStat(), morphStat.getValue());
			}
		}
	}

	// ── AVE 持久注入（僅注入當前激活外觀的 AVE）──────────────────────────

	/**
	 * 返回當前激活視覺變身的 AbnormalVisualEffect 集合，供 EffectList 重建時持久注入。
	 */
	public java.util.Set<AbnormalVisualEffect> getActiveMorphAves(Player player)
	{
		final Integer morphId = _activeVisualMorph.get(player.getObjectId());
		if (morphId == null)
		{
			return java.util.Collections.emptySet();
		}

		final int gradeLevel = getActivatedGradeLevel(player, morphId);
		if (gradeLevel <= 0)
		{
			return java.util.Collections.emptySet();
		}

		final MorphEntry entry = MorphData.getInstance().getEntry(morphId, gradeLevel);
		if ((entry == null) || !entry.hasAbnormalEffects())
		{
			return java.util.Collections.emptySet();
		}

		final java.util.Set<AbnormalVisualEffect> result = java.util.EnumSet.noneOf(AbnormalVisualEffect.class);
		result.addAll(entry.getAbnormalEffects());
		return result;
	}

	// ── 查詢 ─────────────────────────────────────────────────────────────

	public int getActivatedGradeLevel(Player player, int morphId)
	{
		final Map<Integer, Integer> map = _cache.get(player.getObjectId());
		return (map == null) ? 0 : map.getOrDefault(morphId, 0);
	}

	public Map<Integer, Integer> getPlayerMorphSnapshot(Player player)
	{
		final Map<Integer, Integer> map = _cache.get(player.getObjectId());
		return (map == null) ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(map));
	}

	public boolean hasActivatedMorph(Player player, int morphId)
	{
		return getActivatedGradeLevel(player, morphId) > 0;
	}

	/**
	 * 返回當前激活外觀的 morphId，若無則返回 -1。
	 */
	public int getActiveVisualMorphId(Player player)
	{
		return _activeVisualMorph.getOrDefault(player.getObjectId(), -1);
	}

	/**
	 * 是否正在使用變身外觀。
	 */
	public boolean hasActiveVisualMorph(Player player)
	{
		return _activeVisualMorph.containsKey(player.getObjectId());
	}

	/**
	 * 返回當前激活外觀在客戶端 transform_data 中的 transform_id。
	 * 用於 ExUserInfoAbnormalVisualEffect（自己看自己的變身外觀）。
	 * @return transformId，若未配置或無激活變身則返回 0
	 */
	public int getActiveVisualTransformId(Player player)
	{
		final Integer morphId = _activeVisualMorph.get(player.getObjectId());
		if (morphId == null)
		{
			return 0;
		}
		final int grade = getActivatedGradeLevel(player, morphId);
		if (grade <= 0)
		{
			return 0;
		}
		final MorphEntry entry = MorphData.getInstance().getEntry(morphId, grade);
		return (entry != null) ? entry.getTransformId() : 0;
	}

	/**
	 * 返回當前激活外觀的 NPC ID（用於 CharInfo displayId，別人看到你的外觀）。
	 * @return npcId，若無激活變身則返回 0
	 */
	public int getActiveVisualNpcId(Player player)
	{
		final Integer morphId = _activeVisualMorph.get(player.getObjectId());
		if (morphId == null)
		{
			return 0;
		}
		final int grade = getActivatedGradeLevel(player, morphId);
		if (grade <= 0)
		{
			return 0;
		}
		final MorphEntry entry = MorphData.getInstance().getEntry(morphId, grade);
		return (entry != null) ? entry.getNpcId() : 0;
	}

	/**
	 * 當前激活外觀的碰撞半徑（供 Player.getCollisionRadius 使用）。
	 */
	public float getVisualCollisionRadius(Player player, float defaultRadius)
	{
		final Integer morphId = _activeVisualMorph.get(player.getObjectId());
		if (morphId == null)
		{
			return defaultRadius;
		}
		final int grade = getActivatedGradeLevel(player, morphId);
		if (grade <= 0)
		{
			return defaultRadius;
		}
		final MorphEntry entry = MorphData.getInstance().getEntry(morphId, grade);
		return (entry != null) ? (float) entry.getCollisionRadius() : defaultRadius;
	}

	/**
	 * 當前激活外觀的碰撞身高（供 Player.getCollisionHeight 使用）。
	 */
	public float getVisualCollisionHeight(Player player, float defaultHeight)
	{
		final Integer morphId = _activeVisualMorph.get(player.getObjectId());
		if (morphId == null)
		{
			return defaultHeight;
		}
		final int grade = getActivatedGradeLevel(player, morphId);
		if (grade <= 0)
		{
			return defaultHeight;
		}
		final MorphEntry entry = MorphData.getInstance().getEntry(morphId, grade);
		return (entry != null) ? (float) entry.getCollisionHeight() : defaultHeight;
	}

	// ── 內部工具 ─────────────────────────────────────────────────────────

	private void cancelVisualTimerSilently(int charId)
	{
		final ScheduledFuture<?> timer = _activeVisualTimer.remove(charId);
		if ((timer != null) && !timer.isDone())
		{
			timer.cancel(false);
		}
	}

	private Map<Integer, Integer> getOrCreateCache(Player player)
	{
		return _cache.computeIfAbsent(player.getObjectId(), k -> new ConcurrentHashMap<>());
	}

	private void savePlayerMorph(int charId, int morphId, int gradeLevel)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_UPSERT))
		{
			ps.setInt(1, charId);
			ps.setInt(2, morphId);
			ps.setInt(3, gradeLevel);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "MorphManager: Cannot save morph charId=" + charId + " morphId=" + morphId + " grade=" + gradeLevel, e);
		}
	}

	// ── Singleton ─────────────────────────────────────────────────────────

	public static MorphManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final MorphManager INSTANCE = new MorphManager();
	}
}
