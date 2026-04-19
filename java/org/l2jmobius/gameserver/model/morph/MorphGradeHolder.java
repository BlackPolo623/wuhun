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

/**
 * 变身系统 — 某一阶级的容器。
 *
 * 以 grade level 为主导，内含该阶级下所有变身的 {@link MorphEntry}。
 * 对应 XML {@code <grade level="N">} 元素。
 *
 * @author Custom
 */
public class MorphGradeHolder
{
	/** 阶级等级，从 1 开始 */
	private final int _level;

	/** morphId → MorphEntry，TreeMap 保证有序 */
	private final Map<Integer, MorphEntry> _entries = new TreeMap<>();

	// ── 构造器 ────────────────────────────────────────────────────────────

	public MorphGradeHolder(int level)
	{
		_level = level;
	}

	// ── 条目管理 ──────────────────────────────────────────────────────────

	public void addEntry(MorphEntry entry)
	{
		if (entry != null)
		{
			_entries.put(entry.getMorphId(), entry);
		}
	}

	/**
	 * 按 morphId 获取该阶级内的变身条目。
	 */
	public MorphEntry getEntry(int morphId)
	{
		return _entries.get(morphId);
	}

	/**
	 * 返回该阶级内所有变身条目（按 morphId 升序，不可修改）。
	 */
	public List<MorphEntry> getEntries()
	{
		return Collections.unmodifiableList(new ArrayList<>(_entries.values()));
	}

	public boolean hasEntries()
	{
		return !_entries.isEmpty();
	}

	// ── Getter ────────────────────────────────────────────────────────────

	public int getLevel()
	{
		return _level;
	}
}
