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
 * 魂環系統 - 轉生獲得魂環
 * @author 黑普羅
 */
public class Hunhuan implements IVoicedCommandHandler
{
	// ==================== 等級設定 ====================

	private static final int[][] LEVEL_REQUIREMENTS =
			{
					{99, 70},
					{199, 71},
					{299, 72},
					{399, 73},
					{499, 74},
					{599, 75},
					{699, 76},
					{799, 77},
					{899, 78},
					{999, 79},
					{Integer.MAX_VALUE, 80},
			};

	private static final int RESET_LEVEL = 40;

	// ==================== 會員自動轉生設定 ====================

	// 會員自動轉生是否需要消耗材料（true=需要，false=免費）
	private static final boolean PREMIUM_AUTO_COST = true;

	// ==================== 需求道具設定 ====================

	private static final int BASE_ADENA_REQUIRED = 10000;
	private static final int ADENA_INCREMENT_PER_RING = 10000;
	private static final int ADENA_ID = 57;

	// ==================== 獎勵設定 ====================

	private static final int[][] REWARD_ITEMS =
			{
					{105801, 1, 3},
			};

	// ==================== 訊息設定 ====================

	private static final String SUCCESS_MESSAGE = "恭喜！成功獲得魂環，等級已重置至 " + RESET_LEVEL + " 級";
	private static final String SOUL_RING_VAR = "魂環";
	private static final String SYSTEM_NAME = "魂環系統";

	// ==================== 命令列表 ====================

	private static final String[] VOICED_COMMANDS =
			{
					"獲得魂環",
					"hdhh",
			};

	// ==================== 輔助方法 ====================

	private int getRequiredLevel(int currentSoulRingLevel)
	{
		for (int[] requirement : LEVEL_REQUIREMENTS)
		{
			if (currentSoulRingLevel <= requirement[0])
			{
				return requirement[1];
			}
		}
		return LEVEL_REQUIREMENTS[LEVEL_REQUIREMENTS.length - 1][1];
	}

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
				processRebirth(activeChar, false);
				break;
			}
		}

		return true;
	}

	/**
	 * 處理轉生邏輯
	 * @param player 玩家
	 * @param isAuto 是否為自動轉生
	 */
	public void processRebirth(Player player, boolean isAuto)
	{
		int currentRingLevel = player.getVariables().getInt(SOUL_RING_VAR, 0);
		int requiredLevel = getRequiredLevel(currentRingLevel);

		// 檢查等級需求
		if (player.getLevel() < requiredLevel)
		{
			if (!isAuto)
			{
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME,
						"等級不足，需要達到 " + requiredLevel + " 級才能獲得魂環 (當前魂環等級: " + currentRingLevel + ")"));
			}
			return;
		}

		// 如果是自動轉生且需要消耗材料，或者是手動轉生，檢查材料
		boolean needCheckItems = !isAuto || PREMIUM_AUTO_COST;

		if (needCheckItems)
		{
			// 檢查道具需求
			if (!checkAndShowRequiredItems(player, currentRingLevel, isAuto))
			{
				return;
			}

			// 扣除所有需求道具
			consumeRequiredItems(player, currentRingLevel);
		}

		// 增加魂環等級
		player.getVariables().set(SOUL_RING_VAR, currentRingLevel + 1);

		// 重置玩家等級
		final long currentExp = player.getExp();
		final long targetExp = ExperienceData.getInstance().getExpForLevel(RESET_LEVEL);

		player.getStat().setLevel(RESET_LEVEL);

		if (currentExp > targetExp)
		{
			player.removeExpAndSp(currentExp - targetExp, 0);
		}

		// 恢復 HP/MP/CP
		player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());

		// 給予獎勵
		giveRewards(player, isAuto);

		// 更新玩家資訊
		player.broadcastUserInfo();
		player.checkItemRestriction();

		// 發送成功訊息
		String prefix = isAuto ? "[自動轉生] " : "";
		player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, prefix + SUCCESS_MESSAGE));
		player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME,
				"您當前的魂環等級: " + (currentRingLevel + 1) +
						"，下次轉生需要等級: " + getRequiredLevel(currentRingLevel + 1) +
						"，需要金幣: " + formatNumber(getRequiredAdena(currentRingLevel + 1))));
	}

	private boolean checkAndShowRequiredItems(Player player, int currentRingLevel, boolean isAuto)
	{
		int requiredAdena = getRequiredAdena(currentRingLevel);
		long currentAdena = player.getInventory().getInventoryItemCount(ADENA_ID, 0);

		ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(ADENA_ID);
		String itemName = itemTemplate != null ? itemTemplate.getName() : "金幣";

		if (currentAdena < requiredAdena)
		{
			if (!isAuto)
			{
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "========================================"));
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "道具不足！"));
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME,
						itemName + ": " + formatNumber((int)currentAdena) + " / " + formatNumber(requiredAdena)));
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME,
						"還需要: " + formatNumber(requiredAdena - (int)currentAdena) + " 金幣"));
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "========================================"));
			}
			else
			{
				// 自動轉生失敗時的提示
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME,
						"[自動轉生失敗] 金幣不足，需要 " + formatNumber(requiredAdena) + " 金幣"));
			}
			return false;
		}

		return true;
	}

	private void consumeRequiredItems(Player player, int currentRingLevel)
	{
		int requiredAdena = getRequiredAdena(currentRingLevel);
		player.destroyItemByItemId(ItemProcessType.NONE, ADENA_ID, requiredAdena, null, true);
	}

	private void giveRewards(Player player, boolean isAuto)
	{
		String prefix = isAuto ? "[自動轉生] " : "";
		player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "========================================"));
		player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, prefix + "轉生獎勵："));

		for (int[] reward : REWARD_ITEMS)
		{
			int itemId = reward[0];
			int minCount = reward[1];
			int maxCount = reward[2];
			int randomCount = Rnd.get(minCount, maxCount);

			player.addItem(ItemProcessType.NONE, itemId, randomCount, null, true);

			ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(itemId);
			String itemName = itemTemplate != null ? itemTemplate.getName() : "未知道具";

			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME,
					"獲得 " + itemName + " x" + formatNumber(randomCount)));
		}

		player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "========================================"));
	}

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