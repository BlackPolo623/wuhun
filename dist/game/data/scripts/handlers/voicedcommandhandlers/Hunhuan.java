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
package handlers.voicedcommandhandlers;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * 魂環系統 - 轉生獲得魂環（動態等級需求版）
 * @author 黑普羅
 */
public class Hunhuan implements IVoicedCommandHandler
{
	// ==================== 等級設定 ====================

	// 等級需求階段配置
	// 格式：{魂環等級上限, 需求等級}
	// 魂環等級 0-99 需要 60 級，100-199 需要 61 級，以此類推
	private static final int[][] LEVEL_REQUIREMENTS =
			{
					{99, 70},      // 0-99 轉：需要 60 級
					{199, 71},     // 100-199 轉：需要 61 級
					{299, 72},     // 200-299 轉：需要 62 級
					{399, 73},     // 300-399 轉：需要 63 級
					{499, 74},     // 400-499 轉：需要 64 級
					{599, 75},     // 500-599 轉：需要 65 級
					{699, 76},     // 600-699 轉：需要 66 級
					{799, 77},     // 700-799 轉：需要 67 級
					{899, 78},     // 800-899 轉：需要 68 級
					{999, 79},     // 900-999 轉：需要 69 級
					{Integer.MAX_VALUE, 80},  // 1000 轉以上：需要 70 級
			};

	// 獲得魂環後重置的等級
	private static final int RESET_LEVEL = 40;

	// ==================== 需求道具設定 ====================

	// 基礎需求金幣數量（第一轉）
	private static final int BASE_ADENA_REQUIRED = 10000;

	// 每轉增加的金幣數量
	private static final int ADENA_INCREMENT_PER_RING = 10000;

	// 金幣道具ID
	private static final int ADENA_ID = 57;

	// ==================== 獎勵設定 ====================
	// 格式: {道具ID, 最小數量, 最大數量}
	// 轉生成功後，會從這個列表中隨機給予獎勵

	private static final int[][] REWARD_ITEMS =
			{
					{105801, 1, 3},     // 範例獎勵1: 1~3個
			};

	// ==================== 訊息設定 ====================

	private static final String SUCCESS_MESSAGE = "恭喜！成功獲得魂環，等級已重置至 " + RESET_LEVEL + " 級";

	// 魂環變數名稱
	private static final String SOUL_RING_VAR = "魂環";

	// 系統名稱
	private static final String SYSTEM_NAME = "魂環系統";

	// ==================== 命令列表 ====================

	private static final String[] VOICED_COMMANDS =
			{
					"獲得魂環",
					"hdhh",
			};

	// ==================== 輔助方法 ====================

	/**
	 * 根據當前魂環等級獲取轉生所需等級
	 */
	private int getRequiredLevel(int currentSoulRingLevel)
	{
		for (int[] requirement : LEVEL_REQUIREMENTS)
		{
			if (currentSoulRingLevel <= requirement[0])
			{
				return requirement[1];
			}
		}
		// 如果超過所有配置，返回最後一個等級
		return LEVEL_REQUIREMENTS[LEVEL_REQUIREMENTS.length - 1][1];
	}

	/**
	 * 根據當前魂環等級計算需求金幣數量
	 * 公式：基礎金幣 + (當前魂環等級 × 每轉增加量)
	 */
	private int getRequiredAdena(int currentSoulRingLevel)
	{
		return BASE_ADENA_REQUIRED + (currentSoulRingLevel * ADENA_INCREMENT_PER_RING);
	}

	// ==================== 主要邏輯 ====================

	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		switch (command)
		{
			case "獲得魂環":
			case "hdhh":
			{
				// 獲取當前魂環等級
				int currentRingLevel = activeChar.getVariables().getInt(SOUL_RING_VAR, 0);

				// 獲取需求等級
				int requiredLevel = getRequiredLevel(currentRingLevel);

				// 檢查等級需求
				if (activeChar.getLevel() < requiredLevel)
				{
					activeChar.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "等級不足，需要達到 " + requiredLevel + " 級才能獲得魂環 (當前魂環等級: " + currentRingLevel + ")"));
					break;
				}

				// 檢查道具需求（如果不足，會顯示詳細訊息）
				if (!checkAndShowRequiredItems(activeChar, currentRingLevel))
				{
					break;
				}

				// 扣除所有需求道具
				consumeRequiredItems(activeChar, currentRingLevel);

				// 增加魂環等級
				activeChar.getVariables().set(SOUL_RING_VAR, currentRingLevel + 1);

				// 重置玩家等級
				final long currentExp = activeChar.getExp();
				final long targetExp = ExperienceData.getInstance().getExpForLevel(RESET_LEVEL);

				// 先設置等級
				activeChar.getStat().setLevel(RESET_LEVEL);

				// 移除多餘的經驗值
				if (currentExp > targetExp)
				{
					activeChar.removeExpAndSp(currentExp - targetExp, 0);
				}

				// 恢復 HP/MP/CP
				activeChar.setCurrentHpMp(activeChar.getMaxHp(), activeChar.getMaxMp());
				activeChar.setCurrentCp(activeChar.getMaxCp());

				// 給予獎勵
				giveRewards(activeChar);

				// 更新玩家資訊
				activeChar.broadcastUserInfo();
				activeChar.checkItemRestriction();

				// 發送成功訊息
				activeChar.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, SUCCESS_MESSAGE));
				activeChar.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "您當前的魂環等級: " + (currentRingLevel + 1) + "，下次轉生需要等級: " + getRequiredLevel(currentRingLevel + 1) + "，需要金幣: " + formatNumber(getRequiredAdena(currentRingLevel + 1))));

				break;
			}
		}

		return true;
	}

	/**
	 * 檢查並顯示需求道具
	 * @return 如果擁有所有需求道具返回 true，否則返回 false
	 */
	private boolean checkAndShowRequiredItems(Player player, int currentRingLevel)
	{
		// 計算需求金幣數量
		int requiredAdena = getRequiredAdena(currentRingLevel);
		long currentAdena = player.getInventory().getInventoryItemCount(ADENA_ID, 0);

		// 取得道具名稱
		ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(ADENA_ID);
		String itemName = itemTemplate != null ? itemTemplate.getName() : "金幣";

		// 如果金幣不足
		if (currentAdena < requiredAdena)
		{
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "========================================"));
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "道具不足！"));
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, itemName + ": " + formatNumber((int)currentAdena) + " / " + formatNumber(requiredAdena)));
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "還需要: " + formatNumber(requiredAdena - (int)currentAdena) + " 金幣"));
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "========================================"));
			return false;
		}

		return true;
	}

	/**
	 * 扣除所有需求道具
	 */
	private void consumeRequiredItems(Player player, int currentRingLevel)
	{
		// 計算需求金幣數量
		int requiredAdena = getRequiredAdena(currentRingLevel);

		// 扣除金幣
		player.destroyItemByItemId(ItemProcessType.NONE, ADENA_ID, requiredAdena, null, true);
	}

	/**
	 * 給予轉生獎勵（隨機數量）
	 */
	private void giveRewards(Player player)
	{
		player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "========================================"));
		player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "轉生獎勵："));

		for (int[] reward : REWARD_ITEMS)
		{
			int itemId = reward[0];
			int minCount = reward[1];
			int maxCount = reward[2];

			// 隨機數量（包含最小和最大值）
			int randomCount = Rnd.get(minCount, maxCount);

			// 給予道具
			player.addItem(ItemProcessType.NONE, itemId, randomCount, null, true);

			// 取得道具名稱
			ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(itemId);
			String itemName = itemTemplate != null ? itemTemplate.getName() : "未知道具";

			// 發送獎勵訊息
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "獲得 " + itemName + " x" + formatNumber(randomCount)));
		}

		player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "========================================"));
	}

	/**
	 * 格式化數字（添加千分位逗號）
	 */
	private String formatNumber(int number)
	{
		return String.format("%,d", number);
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}