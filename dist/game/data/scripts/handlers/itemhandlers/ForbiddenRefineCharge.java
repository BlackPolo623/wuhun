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

import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.RefineSystemData;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * 禁忌精煉補充卷處理器（item 105859）。
 * 玩家使用後 FORBIDDEN_BONUS_COUNT +1，無上限累積，
 * 該補充次數會由禁忌精煉系統在每日次數耗盡後優先扣除。
 *
 * 流程：
 *   1. 檢查使用者為玩家
 *   2. 檢查禁忌精煉系統啟用狀態
 *   3. 消耗補充卷 ×1
 *   4. 變數 forbidden_bonus_count +1
 *   5. 訊息 + 日誌
 */
public class ForbiddenRefineCharge implements IItemHandler
{
	private static final Logger LOGGER = Logger.getLogger(ForbiddenRefineCharge.class.getName());

	/** 玩家變數 key — 必須與 RefineSystem.java 中 FORBIDDEN_BONUS_COUNT 一致 */
	private static final String FORBIDDEN_BONUS_COUNT = "forbidden_bonus_count";

	/** 一張卷補的次數 */
	private static final int CHARGE_PER_USE = 1;

	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		// 1. 玩家檢查
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
			return false;
		}
		final Player player = playable.asPlayer();

		// 2. 系統啟用檢查
		if (!RefineSystemData.getInstance().isForbiddenEnabled())
		{
			player.sendMessage("❌ [禁忌精煉補充] 系統尚未啟用。");
			return false;
		}

		// 3. 消耗補充卷 ×1
		if (!player.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, player, false))
		{
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			return false;
		}

		// 4. 增加補充次數
		final int oldBonus = player.getVariables().getInt(FORBIDDEN_BONUS_COUNT, 0);
		final int newBonus = oldBonus + CHARGE_PER_USE;
		player.getVariables().set(FORBIDDEN_BONUS_COUNT, newBonus);

		// 5. 訊息與日誌
		player.sendMessage("✨ [禁忌精煉補充] 補充次數 +" + CHARGE_PER_USE + "（目前累計：" + newBonus + " 次）");
		LOGGER.info("[ForbiddenRefineCharge] " + player.getName() + "(objId=" + player.getObjectId() + ") used scroll, bonus " + oldBonus + " -> " + newBonus);

		return true;
	}
}
