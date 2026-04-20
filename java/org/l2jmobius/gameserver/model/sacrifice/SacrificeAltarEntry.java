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
package org.l2jmobius.gameserver.model.sacrifice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.gameserver.model.morph.MorphStatEntry;

/**
 * 祭祀系統 — 單座祭壇的完整定義。
 *
 * 對應 XML {@code <altar>} 元素，包含：
 * <ul>
 *   <li>id / name — 祭壇唯一識別與顯示名</li>
 *   <li>chancePercent — 每次祭祀成功概率（1~100）</li>
 *   <li>upgradeRate — 每級屬性增幅（0.10 = +10%/級）</li>
 *   <li>maxLevel — 可祭祀的最高等級</li>
 *   <li>materials — 每次祭祀消耗的材料列表</li>
 *   <li>stats — Lv.1 基礎屬性加成列表（升級後按 upgradeRate 遞增）</li>
 * </ul>
 *
 * 屬性升級公式：{@code value(Lv.N) = base × (1 + upgradeRate × (N − 1))}
 *
 * @author Custom
 */
public class SacrificeAltarEntry
{
	private final int _id;
	private final String _name;

	/** 每次祭祀成功概率（1~100） */
	private final int _chancePercent;

	/** 每級屬性增幅（例：0.10 = 每升一級屬性 +10%） */
	private final double _upgradeRate;

	/** 最高等級上限 */
	private final int _maxLevel;

	private final List<SacrificeMaterialEntry> _materials = new ArrayList<>();
	private final List<MorphStatEntry> _stats = new ArrayList<>();

	// ── 構造器 ────────────────────────────────────────────────────────────

	public SacrificeAltarEntry(int id, String name, int chancePercent, double upgradeRate, int maxLevel)
	{
		_id = id;
		_name = name;
		_chancePercent = chancePercent;
		_upgradeRate = upgradeRate;
		_maxLevel = maxLevel;
	}

	// ── 條目管理 ──────────────────────────────────────────────────────────

	public void addMaterial(SacrificeMaterialEntry material)
	{
		if (material != null)
		{
			_materials.add(material);
		}
	}

	public void addStat(MorphStatEntry stat)
	{
		if (stat != null)
		{
			_stats.add(stat);
		}
	}

	// ── 屬性計算 ──────────────────────────────────────────────────────────

	/**
	 * 計算指定等級下某基礎值的縮放結果。
	 * 公式：{@code base × (1 + upgradeRate × (level − 1))}
	 *
	 * @param baseValue Lv.1 基礎值
	 * @param level     當前等級（≥ 1）
	 * @return 該等級對應的屬性值
	 */
	public double getScaledValue(double baseValue, int level)
	{
		if (level <= 1)
		{
			return baseValue;
		}
		return baseValue * (1.0 + _upgradeRate * (level - 1));
	}

	// ── Getters ──────────────────────────────────────────────────────────

	public int getId()
	{
		return _id;
	}

	public String getName()
	{
		return _name;
	}

	public int getChancePercent()
	{
		return _chancePercent;
	}

	public double getUpgradeRate()
	{
		return _upgradeRate;
	}

	public int getMaxLevel()
	{
		return _maxLevel;
	}

	public List<SacrificeMaterialEntry> getMaterials()
	{
		return Collections.unmodifiableList(_materials);
	}

	public List<MorphStatEntry> getStats()
	{
		return Collections.unmodifiableList(_stats);
	}

	public boolean hasMaterials()
	{
		return !_materials.isEmpty();
	}

	public boolean hasStats()
	{
		return !_stats.isEmpty();
	}
}
