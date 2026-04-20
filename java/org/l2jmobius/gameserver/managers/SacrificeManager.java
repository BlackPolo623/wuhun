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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.SacrificeData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.model.morph.MorphStatEntry;
import org.l2jmobius.gameserver.model.sacrifice.SacrificeAltarEntry;
import org.l2jmobius.gameserver.model.sacrifice.SacrificeMaterialEntry;

/**
 * 祭祀系統管理器 SacrificeManager 功能： 1. loadPlayer — 登入時從 DB 加載玩家各祭壇等級 2. unloadPlayer — 下線時清除緩存 3. performSacrifice — 執行一次祭祀（消耗材料、骰子判定、升級） 4. pumpSacrificeStats — 在 PlayerStat.resetStats() 中注入當前祭祀屬性 DB 表（首次啟動前手動建表）：
 * 
 * <pre>
 * CREATE TABLE IF NOT EXISTS `player_sacrifice` (
 *   `char_id`  INT NOT NULL,
 *   `altar_id` INT NOT NULL,
 *   `level`    INT NOT NULL DEFAULT 0,
 *   PRIMARY KEY (`char_id`, `altar_id`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 * </pre>
 *
 * @author Custom
 */
public class SacrificeManager
{
	private static final Logger LOGGER = Logger.getLogger(SacrificeManager.class.getName());
	
	// ── SQL ──────────────────────────────────────────────────────────────
	
	private static final String SQL_LOAD = "SELECT altar_id, level FROM player_sacrifice WHERE char_id = ?";
	private static final String SQL_UPSERT = "INSERT INTO player_sacrifice (char_id, altar_id, level) VALUES (?,?,?) " + "ON DUPLICATE KEY UPDATE level = VALUES(level)";
	
	// ── 緩存：charId → (altarId → level) ─────────────────────────────────
	
	private final Map<Integer, Map<Integer, Integer>> _cache = new ConcurrentHashMap<>();
	
	// ── 構造器 ────────────────────────────────────────────────────────────
	
	protected SacrificeManager()
	{
	}
	
	// ── 玩家加載 / 卸載 ───────────────────────────────────────────────────
	
	/**
	 * 玩家登入時調用，從 DB 加載其所有祭壇等級。
	 */
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
					final int altarId = rs.getInt("altar_id");
					final int level = rs.getInt("level");
					if (level > 0)
					{
						map.put(altarId, level);
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "SacrificeManager: Cannot load data for charId=" + charId, e);
		}
		
		_cache.put(charId, map);
	}
	
	/**
	 * 玩家下線時調用，清除緩存。
	 */
	public void unloadPlayer(int charId)
	{
		_cache.remove(charId);
	}
	
	// ── 執行祭祀 ─────────────────────────────────────────────────────────
	
	/**
	 * 執行一次祭祀嘗試。
	 * <ol>
	 * <li>驗證祭壇存在且未達最高等級</li>
	 * <li>驗證並消耗所有材料</li>
	 * <li>骰子判定成功概率</li>
	 * <li>成功 → 等級 +1，更新緩存與 DB，重算屬性</li>
	 * <li>失敗 → 材料已消耗，等級不變</li>
	 * </ol>
	 * @param player 執行祭祀的玩家
	 * @param altarId 祭壇 ID
	 * @return {@code true} = 祭祀成功升級；{@code false} = 失敗（但材料已消耗）
	 */
	public boolean performSacrifice(Player player, int altarId)
	{
		final SacrificeAltarEntry altar = SacrificeData.getInstance().getAltar(altarId);
		if (altar == null)
		{
			player.sendMessage("找不到該祭壇資料（id=" + altarId + "）。");
			return false;
		}
		
		final int charId = player.getObjectId();
		final int currentLevel = getPlayerLevel(player, altarId);
		
		// 已達最高等級
		if (currentLevel >= altar.getMaxLevel())
		{
			player.sendMessage("「" + altar.getName() + "」已達最高等級（Lv." + altar.getMaxLevel() + "），無法繼續祭祀。");
			return false;
		}
		
		// 驗證材料是否足夠
		for (SacrificeMaterialEntry mat : altar.getMaterials())
		{
			if (player.getInventory().getInventoryItemCount(mat.getItemId(), -1) < mat.getCount())
			{
				player.sendMessage("材料不足，祭祀失敗。（道具 " + mat.getItemId() + " 需要 " + mat.getCount() + " 個）");
				return false;
			}
		}
		
		// 消耗材料
		for (SacrificeMaterialEntry mat : altar.getMaterials())
		{
			if (!player.destroyItemByItemId(null, mat.getItemId(), mat.getCount(), player, true))
			{
				player.sendMessage("材料消耗異常，請稍後重試。");
				return false;
			}
		}
		
		// 骰子判定
		final boolean success = (int) (Math.random() * 100) < altar.getChancePercent();
		
		if (success)
		{
			final int newLevel = currentLevel + 1;
			getOrCreateCache(player).put(altarId, newLevel);
			saveLevel(charId, altarId, newLevel);
			
			// 重算屬性（讓新等級的加成立即生效）
			player.getStat().recalculateStats(true);
			
			player.sendMessage("========================================");
			player.sendMessage("「" + altar.getName() + "」祭祀成功！");
			player.sendMessage("等級提升：Lv." + currentLevel + " → Lv." + newLevel + "（上限 Lv." + altar.getMaxLevel() + "）");
			if (newLevel < altar.getMaxLevel())
			{
				player.sendMessage("下次祭祀成功率：" + altar.getChancePercent() + "%");
			}
			else
			{
				player.sendMessage("已達最高等級，祭壇封印完成！");
			}
			player.sendMessage("========================================");
			return true;
		}
		
		// 失敗
		player.sendMessage("========================================");
		player.sendMessage("「" + altar.getName() + "」祭祀失敗，材料已消耗。");
		player.sendMessage("當前等級：Lv." + currentLevel + "，成功率：" + altar.getChancePercent() + "%，繼續嘗試！");
		player.sendMessage("========================================");
		return false;
	}
	
	// ── 屬性注入（在 PlayerStat.resetStats 中調用）────────────────────────
	
	/**
	 * 注入玩家所有已祭祀祭壇的屬性加成到 {@link CreatureStat}。 在 {@code PlayerStat.resetStats()} 中調用。
	 * <p>
	 * 升級公式：{@code value(Lv.N) = base × (1 + upgradeRate × (N − 1))}
	 * </p>
	 */
	public void pumpSacrificeStats(Player player, CreatureStat stat)
	{
		final Map<Integer, Integer> map = _cache.get(player.getObjectId());
		if ((map == null) || map.isEmpty())
		{
			return;
		}
		
		for (Map.Entry<Integer, Integer> entry : map.entrySet())
		{
			final int altarId = entry.getKey();
			final int level = entry.getValue();
			if (level <= 0)
			{
				continue;
			}
			
			final SacrificeAltarEntry altar = SacrificeData.getInstance().getAltar(altarId);
			if ((altar == null) || !altar.hasStats())
			{
				continue;
			}
			
			for (MorphStatEntry morphStat : altar.getStats())
			{
				final double scaledValue = altar.getScaledValue(morphStat.getValue(), level);
				
				if (morphStat.isMultiply())
				{
					// scaledValue 為百分比，轉為乘算因子：value=10 @ Lv.3 upgradeRate=0.10
					// → scaledValue = 10×(1+0.10×2) = 12 → factor = 1.12
					stat.mergeMul(morphStat.getStat(), 1.0 + (scaledValue / 100.0));
				}
				else
				{
					stat.mergeAdd(morphStat.getStat(), scaledValue);
				}
			}
		}
	}
	
	// ── 查詢 ─────────────────────────────────────────────────────────────
	
	/**
	 * 返回玩家在指定祭壇的當前等級（0 = 未祭祀）。
	 */
	public int getPlayerLevel(Player player, int altarId)
	{
		final Map<Integer, Integer> map = _cache.get(player.getObjectId());
		return (map == null) ? 0 : map.getOrDefault(altarId, 0);
	}
	
	/**
	 * 返回玩家所有祭壇等級的快照（不可修改）。
	 */
	public Map<Integer, Integer> getPlayerSnapshot(Player player)
	{
		final Map<Integer, Integer> map = _cache.get(player.getObjectId());
		return (map == null) ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(map));
	}
	
	// ── 內部工具 ─────────────────────────────────────────────────────────
	
	private Map<Integer, Integer> getOrCreateCache(Player player)
	{
		return _cache.computeIfAbsent(player.getObjectId(), k -> new ConcurrentHashMap<>());
	}
	
	private void saveLevel(int charId, int altarId, int level)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_UPSERT))
		{
			ps.setInt(1, charId);
			ps.setInt(2, altarId);
			ps.setInt(3, level);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "SacrificeManager: Cannot save level charId=" + charId + " altarId=" + altarId, e);
		}
	}
	
	// ── Singleton ─────────────────────────────────────────────────────────
	
	public static SacrificeManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SacrificeManager INSTANCE = new SacrificeManager();
	}
}
