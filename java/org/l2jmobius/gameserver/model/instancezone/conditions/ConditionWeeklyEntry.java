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
package org.l2jmobius.gameserver.model.instancezone.conditions;

import org.l2jmobius.gameserver.managers.events.InstanceEntryManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.InstanceTemplate;

/**
 * 副本每週進入次數檢查條件
 * 用於檢查玩家是否還有剩餘的每週進入次數
 *
 * 重要：所有副本統一在每週一凌晨 00:00 重置次數
 *
 * XML 參數說明：
 * - maxEntries: 每週最大進入次數（預設100）
 *
 * 使用範例：
 * <condition type="WeeklyEntry">
 *     <param name="maxEntries" value="3" />
 * </condition>
 *
 * @author Custom
 */
public class ConditionWeeklyEntry extends Condition
{
	private final int _instanceId;
	private boolean _registered = false;

	public ConditionWeeklyEntry(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, onlyLeader, showMessageAndHtml);
		_instanceId = template.getId();

		// 從 XML 讀取配置參數（如果沒有設定則使用預設值）
		final int maxEntries = parameters.getInt("maxEntries", 100);

		// 註冊副本的次數管理配置（統一週一 00:00 重置）
		InstanceEntryManager.getInstance().registerInstance(_instanceId, maxEntries);
		_registered = true;
	}

	@Override
	protected boolean test(Player player, Npc npc)
	{
		if (!_registered)
		{
			return true; // 如果註冊失敗，不限制進入
		}

		if (!InstanceEntryManager.getInstance().canEnter(_instanceId, player))
		{
			final int remaining = InstanceEntryManager.getInstance().getRemainingEntries(_instanceId, player);
			final int max = InstanceEntryManager.getInstance().getMaxEntries(_instanceId);
			player.sendMessage(player.getName() + " 本週的進入次數已用完。剩餘次數: " + remaining + "/" + max);
			return false;
		}
		return true;
	}

	@Override
	protected void onSuccess(Player player)
	{
		if (_registered)
		{
			// 所有條件通過後，扣除玩家的每週進入次數
			InstanceEntryManager.getInstance().incrementEntryCount(_instanceId, player);
		}
	}
}
