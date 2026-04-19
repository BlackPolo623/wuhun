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
import java.util.Map;
import java.util.TreeMap;

import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;

/**
 * 單個變身的頂層數據持有者。
 *
 * 對應 XML 中一個 {@code <morph>} 元素，包含：
 *   - 唯一 ID、名稱
 *   - NPC 外觀 ID（npcId）：整個變身共用同一外觀
 *   - 激活道具 ID（itemId）：使用該道具激活此變身
 *   - 碰撞尺寸（collisionRadius / collisionHeight）
 *   - 視覺特效列表（abnormalEffects）
 *   - 多個能力階級（grade level 1 ~ N），每個階級只含屬性加成
 *
 * @author Custom
 */
public class MorphHolder
{
	private final int _morphId;
	private final String _name;

	/** 變身外觀對應的 NPC ID */
	private final int _npcId;

	/** 激活此變身所需消耗的道具 ID */
	private final int _itemId;

	/** 碰撞半徑 */
	private final double _collisionRadius;

	/** 碰撞高度 */
	private final double _collisionHeight;

	/**
	 * 變身激活時附加的視覺特效列表。
	 * 對應 XML 屬性 {@code abnormalEffects="DOT_FIRE,SEIZURE1"}
	 */
	private final List<AbnormalVisualEffect> _abnormalEffects = new ArrayList<>();

	/** level → MorphGradeHolder，TreeMap 保證按階級升序 */
	private final Map<Integer, MorphGradeHolder> _grades = new TreeMap<>();

	// ── 構造器 ────────────────────────────────────────────────────────────

	public MorphHolder(int morphId, String name, int npcId, int itemId, double collisionRadius, double collisionHeight)
	{
		_morphId = morphId;
		_name = name;
		_npcId = npcId;
		_itemId = itemId;
		_collisionRadius = collisionRadius;
		_collisionHeight = collisionHeight;
	}

	// ── 視覺特效管理 ──────────────────────────────────────────────────────

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

	// ── 階級管理 ──────────────────────────────────────────────────────────

	public void addGrade(MorphGradeHolder grade)
	{
		if (grade != null)
		{
			_grades.put(grade.getLevel(), grade);
		}
	}

	public MorphGradeHolder getGrade(int level)
	{
		return _grades.get(level);
	}

	public List<MorphGradeHolder> getGrades()
	{
		return Collections.unmodifiableList(new ArrayList<>(_grades.values()));
	}

	public int getMaxLevel()
	{
		return _grades.isEmpty() ? 0 : ((TreeMap<Integer, MorphGradeHolder>) _grades).lastKey();
	}

	public int getMinLevel()
	{
		return _grades.isEmpty() ? 0 : ((TreeMap<Integer, MorphGradeHolder>) _grades).firstKey();
	}

	public boolean hasGrades()
	{
		return !_grades.isEmpty();
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
}
