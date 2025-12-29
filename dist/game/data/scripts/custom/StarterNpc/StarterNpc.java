/**
 * 新手獎勵系統
 */

package custom.StarterNpc;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class StarterNpc extends Script
{
	// NPC ID
	private final static int NPC_ID = 900000;

	// 新手獎勵配置
	private final static int[][] STARTER_REWARD_ITEMS = {
			{57, 5000000},    // 金幣
			{91663, 1000},    // 其他物品
	};

	// 魂環獎勵配置 - 1轉
	private final static int SOUL_RING_REWARD_1_REQUIRED = 1;
	private final static int[][] SOUL_RING_REWARD_1_ITEMS = {
			{57, 20000000},   // 1000萬金幣
			{91663, 1000},    // 物品
			{105801, 100},    // 物品
	};

	// 魂環獎勵配置 - 10轉
	private final static int SOUL_RING_REWARD_10_REQUIRED = 10;
	private final static int[][] SOUL_RING_REWARD_10_ITEMS = {
			{57, 50000000},   // 5000萬金幣
			{91663, 2000},    // 物品
			{105801, 200},    // 物品
	};

	// 魂環獎勵配置 - 50轉
	private final static int SOUL_RING_REWARD_50_REQUIRED = 50;
	private final static int[][] SOUL_RING_REWARD_50_ITEMS = {
			{57, 100000000},  // 2億金幣
			{91663, 3000},    // 物品
			{105801, 300},    // 物品
	};

	// 魂環獎勵配置 - 100轉循環獎勵
	private final static int SOUL_RING_REWARD_100_INTERVAL = 100;  // 每100轉一次
	private final static int[][] SOUL_RING_REWARD_100_ITEMS = {
			{57, 50000000},
			{91663, 5000},
			{105801, 500},    // 魂魄
	};

	private StarterNpc()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("claim_starter"))
		{
			claimStarterReward(player, npc);
		}
		else if (event.equals("claim_soul_1"))
		{
			claimSoulRingReward(player, npc, 1);
		}
		else if (event.equals("claim_soul_10"))
		{
			claimSoulRingReward(player, npc, 10);
		}
		else if (event.equals("claim_soul_50"))
		{
			claimSoulRingReward(player, npc, 50);
		}
		else if (event.equals("claim_soul_100"))
		{
			claimSoulRingReward100(player, npc);
		}
		else if (event.equals("back"))
		{
			showMainMenu(player, npc);
		}

		return null;
	}

	private void claimStarterReward(Player player, Npc npc)
	{
		if (player.getVariables().getBoolean("StarterRewardClaimed", false))
		{
			player.sendMessage("您已經領取過初入武魂獎勵了！");
			return;
		}

		if (!player.getInventory().validateCapacity(STARTER_REWARD_ITEMS.length))
		{
			player.sendMessage("背包空間不足，請清理後再來領取。");
			return;
		}

		for (int[] reward : STARTER_REWARD_ITEMS)
		{
			player.addItem(ItemProcessType.NONE, reward[0], reward[1], player, true);
		}

		player.getVariables().set("StarterRewardClaimed", true);
		player.getVariables().storeMe();

		player.sendMessage("成功領取初入武魂獎勵！");
		showMainMenu(player, npc);
	}

	private void claimSoulRingReward(Player player, Npc npc, int tier)
	{
		int currentSoulRings = player.getVariables().getInt("魂環", 0);
		String varName = "SoulRingReward" + tier + "Claimed";
		int requiredRings;
		int[][] rewards;

		// 根據階段設定需求和獎勵
		switch (tier)
		{
			case 1:
				requiredRings = SOUL_RING_REWARD_1_REQUIRED;
				rewards = SOUL_RING_REWARD_1_ITEMS;
				break;
			case 10:
				requiredRings = SOUL_RING_REWARD_10_REQUIRED;
				rewards = SOUL_RING_REWARD_10_ITEMS;
				break;
			case 50:
				requiredRings = SOUL_RING_REWARD_50_REQUIRED;
				rewards = SOUL_RING_REWARD_50_ITEMS;
				break;
			default:
				return;
		}

		// 檢查是否已領取
		if (player.getVariables().getBoolean(varName, false))
		{
			player.sendMessage("您已經領取過" + tier + "轉魂環獎勵了！");
			return;
		}

		// 檢查魂環數量
		if (currentSoulRings < requiredRings)
		{
			player.sendMessage("您的魂環數量不足，需要" + requiredRings + "個魂環才能領取。");
			return;
		}

		// 檢查背包空間
		if (!player.getInventory().validateCapacity(rewards.length))
		{
			player.sendMessage("背包空間不足，請清理後再來領取。");
			return;
		}

		// 發放獎勵
		for (int[] reward : rewards)
		{
			player.addItem(ItemProcessType.NONE, reward[0], reward[1], player, true);
		}

		// 記錄領取狀態
		player.getVariables().set(varName, true);
		player.getVariables().storeMe();

		player.sendMessage("成功領取" + tier + "轉魂環獎勵！");
		showMainMenu(player, npc);
	}

	/**
	 * 處理100轉循環獎勵領取
	 */
	private void claimSoulRingReward100(Player player, Npc npc)
	{
		int currentSoulRings = player.getVariables().getInt("魂環", 0);
		int lastClaimedMultiple = player.getVariables().getInt("SoulRingReward100LastClaimed", 0);

		// 計算下一個可領取的倍數
		int nextClaimableMultiple = lastClaimedMultiple + SOUL_RING_REWARD_100_INTERVAL;

		// 檢查是否達到下一個倍數
		if (currentSoulRings < nextClaimableMultiple)
		{
			player.sendMessage("您的魂環數量不足，需要" + nextClaimableMultiple + "個魂環才能領取下一次獎勵。");
			return;
		}

		// 檢查背包空間
		if (!player.getInventory().validateCapacity(SOUL_RING_REWARD_100_ITEMS.length))
		{
			player.sendMessage("背包空間不足，請清理後再來領取。");
			return;
		}

		// 發放獎勵
		for (int[] reward : SOUL_RING_REWARD_100_ITEMS)
		{
			player.addItem(ItemProcessType.NONE, reward[0], reward[1], player, true);
		}

		// 更新已領取的倍數
		player.getVariables().set("SoulRingReward100LastClaimed", nextClaimableMultiple);
		player.getVariables().storeMe();

		player.sendMessage("成功領取" + nextClaimableMultiple + "轉魂環獎勵！");
		showMainMenu(player, npc);
	}

	private void showMainMenu(Player player, Npc npc)
	{
		int currentSoulRings = player.getVariables().getInt("魂環", 0);
		boolean starterClaimed = player.getVariables().getBoolean("StarterRewardClaimed", false);
		boolean soul1Claimed = player.getVariables().getBoolean("SoulRingReward1Claimed", false);
		boolean soul10Claimed = player.getVariables().getBoolean("SoulRingReward10Claimed", false);
		boolean soul50Claimed = player.getVariables().getBoolean("SoulRingReward50Claimed", false);

		// 100轉獎勵狀態
		int lastClaimed100 = player.getVariables().getInt("SoulRingReward100LastClaimed", 0);
		int nextClaim100 = lastClaimed100 + SOUL_RING_REWARD_100_INTERVAL;

		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/StarterNpc/start.htm");
		html.replace("%soul_rings%", String.valueOf(currentSoulRings));
		html.replace("%starter_status%", starterClaimed ? "已領取" : "可領取");
		html.replace("%soul1_status%", getSoulRewardStatus(currentSoulRings, SOUL_RING_REWARD_1_REQUIRED, soul1Claimed));
		html.replace("%soul10_status%", getSoulRewardStatus(currentSoulRings, SOUL_RING_REWARD_10_REQUIRED, soul10Claimed));
		html.replace("%soul50_status%", getSoulRewardStatus(currentSoulRings, SOUL_RING_REWARD_50_REQUIRED, soul50Claimed));
		html.replace("%soul100_status%", getSoul100RewardStatus(currentSoulRings, nextClaim100));
		html.replace("%starter_button%", getButtonHtml(starterClaimed, "claim_starter", "領取初入武魂獎勵"));
		html.replace("%soul1_button%", getSoulButtonHtml(currentSoulRings, SOUL_RING_REWARD_1_REQUIRED, soul1Claimed, "claim_soul_1", "領取1轉魂環獎勵"));
		html.replace("%soul10_button%", getSoulButtonHtml(currentSoulRings, SOUL_RING_REWARD_10_REQUIRED, soul10Claimed, "claim_soul_10", "領取10轉魂環獎勵"));
		html.replace("%soul50_button%", getSoulButtonHtml(currentSoulRings, SOUL_RING_REWARD_50_REQUIRED, soul50Claimed, "claim_soul_50", "領取50轉魂環獎勵"));
		html.replace("%soul100_button%", getSoul100ButtonHtml(currentSoulRings, nextClaim100));

		player.sendPacket(html);
	}

	private String getSoulRewardStatus(int current, int required, boolean claimed)
	{
		if (claimed)
		{
			return "已領取";
		}
		else if (current >= required)
		{
			return "可領取";
		}
		else
		{
			return "未達成(" + current + "/" + required + ")";
		}
	}

	/**
	 * 獲取100轉獎勵狀態
	 */
	private String getSoul100RewardStatus(int current, int nextRequired)
	{
		if (current >= nextRequired)
		{
			return "可領取(下次:" + nextRequired + "轉)";
		}
		else
		{
			return "未達成(" + current + "/" + nextRequired + ")";
		}
	}

	private String getButtonHtml(boolean claimed, String event, String text)
	{
		if (claimed)
		{
			return "<font color=\"808080\">" + text + " (已領取)</font>";
		}
		else
		{
			return "<button action=\"bypass -h Quest StarterNpc " + event + "\" value=\"" + text + "\" width=\"250\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">";
		}
	}

	private String getSoulButtonHtml(int current, int required, boolean claimed, String event, String text)
	{
		if (claimed)
		{
			return "<font color=\"808080\">" + text + " (已領取)</font>";
		}
		else if (current >= required)
		{
			return "<button action=\"bypass -h Quest StarterNpc " + event + "\" value=\"" + text + "\" width=\"250\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">";
		}
		else
		{
			return "<font color=\"808080\">" + text + " (魂環不足)</font>";
		}
	}

	/**
	 * 獲取100轉獎勵按鈕HTML
	 */
	private String getSoul100ButtonHtml(int current, int nextRequired)
	{
		if (current >= nextRequired)
		{
			return "<button action=\"bypass -h Quest StarterNpc claim_soul_100\" value=\"領取" + nextRequired + "轉魂環獎勵\" width=\"250\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">";
		}
		else
		{
			return "<font color=\"808080\">領取" + nextRequired + "轉魂環獎勵 (魂環不足)</font>";
		}
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainMenu(player, npc);
		return null;
	}

	public static void main(String[] args)
	{
		new StarterNpc();
		System.out.println("【系統】新手獎勵系統載入完畢！");
	}
}