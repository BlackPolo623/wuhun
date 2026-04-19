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
package org.l2jmobius.gameserver.model.morph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;

/**
 * 变身系统 — 单个变身在某一阶级的完整数据条目。
 *
 * 每个 MorphEntry 对应 XML 中 {@code <grade>} 内的一个 {@code <morph>}，包含：
 *   - morphId：变身唯一 ID
 *   - name：变身名称（中文）
 *   - npcId：该阶级对应的外观 NPC ID
 *   - itemId：激活该变身该阶级所需消耗的道具 ID
 *   - collisionRadius / collisionHeight：碰撞尺寸
 *   - abnormalEffects：视觉特效列表
 *   - stats：属性加成条目列表
 *
 * @author Custom
 */
public class MorphEntry
{
	private final int _morphId;
	private final String _name;
	private final int _npcId;
	/** 客户端 transform_data 中对应的 transform_id，用于 ExUserInfoAbnormalVisualEffect */
	private final int _transformId;
	private final int _itemId;
	private final double _collisionRadius;
	private final double _collisionHeight;

	private final List<AbnormalVisualEffect> _abnormalEffects = new ArrayList<>();
	private final List<MorphStatEntry> _stats = new ArrayList<>();

	// ── 构造器 ────────────────────────────────────────────────────────────

	public MorphEntry(int morphId, String name, int npcId, int transformId, int itemId, double collisionRadius, double collisionHeight)
	{
		_morphId = morphId;
		_name = name;
		_npcId = npcId;
		_transformId = transformId;
		_itemId = itemId;
		_collisionRadius = collisionRadius;
		_collisionHeight = collisionHeight;
	}

	// ── 视觉特效 ──────────────────────────────────────────────────────────

	public void addAbnormalEffect(AbnormalVisualEffect ave)
	{
		if (ave != null)
		{
			_abnormalEffects.add(ave);
		}
	}

	public List<AbnormalVisualEffect> getAbnormalEffects()
	{
		return Collections.unmodifiableList(_abnormalEffects);
	}

	public boolean hasAbnormalEffects()
	{
		return !_abnormalEffects.isEmpty();
	}

	// ── 属性条目 ──────────────────────────────────────────────────────────

	public void addStat(MorphStatEntry entry)
	{
		if (entry != null)
		{
			_stats.add(entry);
		}
	}

	public List<MorphStatEntry> getStats()
	{
		return Collections.unmodifiableList(_stats);
	}

	public boolean hasStats()
	{
		return !_stats.isEmpty();
	}

	// ── Getters ──────────────────────────────────────────────────────────

	public int getMorphId()
	{
		return _morphId;
	}

	public String getName()
	{
		return _name;
	}

	public int getNpcId()
	{
		return _npcId;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public double getCollisionRadius()
	{
		return _collisionRadius;
	}

	public double getCollisionHeight()
	{
		return _collisionHeight;
	}

	/**
	 * 返回此变身对应的客户端 transform_data 中的 transform_id。
	 * 此值发送至 ExUserInfoAbnormalVisualEffect，客户端据此在 transform_data 中查找对应外观。
	 */
	public int getTransformId()
	{
		return _transformId;
	}
}
