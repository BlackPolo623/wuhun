/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.itemhandlers;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * 永久增加HP藥水處理器
 * Permanent HP Potion Handler
 * Item ID: 105808
 */
public class PermanentHpPotion implements IItemHandler
{
	// 可配置的最小和最大HP增加值
	private static final int MIN_HP_INCREASE = 10;
	private static final int MAX_HP_INCREASE = 50;

	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
			return false;
		}

		final Player player = playable.asPlayer();

		// 生成隨機HP增加值
		final int hpIncrease = Rnd.get(MIN_HP_INCREASE, MAX_HP_INCREASE);

		// 獲取當前已累積的HP加成
		final int currentBonusHp = player.getVariables().getInt(PlayerVariables.BONUS_HP_POTION, 0);

		// 更新累積的HP加成
		final int newBonusHp = currentBonusHp + hpIncrease;
		player.getVariables().set(PlayerVariables.BONUS_HP_POTION, newBonusHp);

		// 刷新玩家狀態以應用新的HP加成
		player.getStat().recalculateStats(true);
		player.broadcastUserInfo();

		// 發送系統訊息通知玩家
		player.sendMessage("你永久增加了 " + hpIncrease + " 點HP！目前累積增加: " + newBonusHp + " HP");

		// 消耗道具
		if (!player.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, player, false))
		{
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			return false;
		}

		return true;
	}
}
