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

/**
 * 祭祀系統 — 單條材料消耗定義。
 * 對應 XML {@code <material itemId="..." count="..."/>} 元素。
 *
 * @author Custom
 */
public class SacrificeMaterialEntry
{
	private final int _itemId;
	private final long _count;

	public SacrificeMaterialEntry(int itemId, long count)
	{
		_itemId = itemId;
		_count = count;
	}

	/** 消耗道具 ID */
	public int getItemId()
	{
		return _itemId;
	}

	/** 每次祭祀消耗數量 */
	public long getCount()
	{
		return _count;
	}
}
