package custom.FrozenWeaponCraft;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * 冰凍君主武器製作系統
 * @author 黑普羅
 */
public class FrozenWeaponCraft extends Script
{
	// ==================== 基本配置 ====================

	private static final int NPC_ID = 900015;
	private static final String HTML_PATH = "data/scripts/custom/FrozenWeaponCraft/";

	// ==================== 材料需求 ====================
	private static final int[][] REQUIRED_MATERIALS = {
			{91663, 500},
			{72478, 1},
			{95782, 1000},
	};

	// ==================== 武器ID列表 ====================
	private static final int[] WEAPON_IDS = {
			95725, 95726, 95727, 95728, 95729, 95730, 95731,
			95732, 95733, 95734, 95735, 95736, 95737
	};

	// ==================== 強化值配置 ====================
	private static final int MIN_ENCHANT = 1;         // 最小強化值
	private static final int MAX_ENCHANT = 30;        // 最大強化值
	private static final int ANNOUNCE_THRESHOLD = 22; // 公告閾值

	// ==================== 成功率配置 ====================
	private static final int BASE_SUCCESS_RATE = 10;  // 基礎成功率 (80%)

	// ==================== 成功率提升道具配置 ====================
	private static final int BOOST_ITEM_ID = 95781;         // 提升成功率的道具ID
	private static final int BOOST_PER_ITEM = 1;           // 每個道具增加的成功率 (10%)
	private static final int MAX_BOOST_RATE = 30;           // 最大增加成功率 (30%)
	private static final boolean CONSUME_BOOST_ITEM = true; // 是否消耗提升道具


	public FrozenWeaponCraft()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}


	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainPage(player, npc);
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((event == null) || (player == null))
		{
			return null;
		}

		switch (event)
		{
			case "main":
				showMainPage(player, npc);
				break;
			case "craft":
				processCraft(player, npc);
				break;
		}

		return null;
	}

	// ==================== 頁面顯示 ====================

	private void showMainPage(Player player, Npc npc)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, HTML_PATH + "main.htm");

		// 生成材料列表
		StringBuilder materialList = new StringBuilder();
		boolean hasAllMaterials = true;

		for (int[] material : REQUIRED_MATERIALS)
		{
			int itemId = material[0];
			long requiredCount = material[1];
			long currentCount = player.getInventory().getInventoryItemCount(itemId, -1);

			if (currentCount < requiredCount)
			{
				hasAllMaterials = false;
			}

			ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(itemId);
			String itemName = itemTemplate != null ? itemTemplate.getName() : "未知物品";
			String countColor = currentCount >= requiredCount ? "00FF00" : "FF0000";

			materialList.append("<tr bgcolor=\"222222\">");
			materialList.append("<td align=\"left\" width=\"180\"><font color=\"LEVEL\">").append(itemName).append("</font></td>");
			materialList.append("<td align=\"right\" width=\"100\"><font color=\"").append(countColor).append("\">")
					.append(formatNumber(currentCount)).append(" / ").append(formatNumber(requiredCount)).append("</font></td>");
			materialList.append("</tr>");
		}

		html.replace("%material_list%", materialList.toString());
		html.replace("%min_enchant%", String.valueOf(MIN_ENCHANT));
		html.replace("%max_enchant%", String.valueOf(MAX_ENCHANT));
		html.replace("%announce_threshold%", String.valueOf(ANNOUNCE_THRESHOLD));

		// 計算當前成功率
		long boostItemCount = player.getInventory().getInventoryItemCount(BOOST_ITEM_ID, -1);
		int extraRate = calculateExtraSuccessRate(boostItemCount);
		int totalRate = BASE_SUCCESS_RATE + extraRate;

		html.replace("%base_rate%", String.valueOf(BASE_SUCCESS_RATE));
		html.replace("%extra_rate%", extraRate > 0 ? "+" + extraRate : "0");
		html.replace("%total_rate%", String.valueOf(totalRate));
		html.replace("%boost_count%", String.valueOf(boostItemCount));

		// 製作按鈕
		StringBuilder craftButton = new StringBuilder();
		if (hasAllMaterials)
		{
			craftButton.append("<button action=\"bypass -h Quest FrozenWeaponCraft craft\" value=\"開始製作\" width=\"200\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
		}
		else
		{
			craftButton.append("<table width=\"200\"><tr><td align=center><font color=\"FF3333\">材料不足</font></td></tr></table>");
		}
		html.replace("%craft_button%", craftButton.toString());

		player.sendPacket(html);
	}

	// ==================== 製作邏輯 ====================

	private void processCraft(Player player, Npc npc)
	{
		// 檢查材料
		for (int[] material : REQUIRED_MATERIALS)
		{
			int itemId = material[0];
			long requiredCount = material[1];
			long currentCount = player.getInventory().getInventoryItemCount(itemId, -1);

			if (currentCount < requiredCount)
			{
				ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(itemId);
				String itemName = itemTemplate != null ? itemTemplate.getName() : "未知物品";
				player.sendMessage("材料不足：" + itemName);
				showMainPage(player, npc);
				return;
			}
		}

		// 檢查背包空間
		if (!player.getInventory().validateCapacity(1))
		{
			player.sendMessage("背包空間不足！");
			showMainPage(player, npc);
			return;
		}

		// 計算成功率加成
		long boostItemCount = player.getInventory().getInventoryItemCount(BOOST_ITEM_ID, -1);
		int extraRate = calculateExtraSuccessRate(boostItemCount);
		int finalSuccessRate = BASE_SUCCESS_RATE + extraRate;

		// 扣除基礎材料
		for (int[] material : REQUIRED_MATERIALS)
		{
			player.destroyItemByItemId(null, material[0], material[1], npc, true);
		}

		// 扣除提升道具（如果啟用消耗）
		int consumedBoostItems = 0;
		if (CONSUME_BOOST_ITEM && boostItemCount > 0)
		{
			// 計算實際使用的道具數量（最多到達上限即可）
			consumedBoostItems = (int) Math.min(boostItemCount, (MAX_BOOST_RATE / BOOST_PER_ITEM));
			player.destroyItemByItemId(null, BOOST_ITEM_ID, consumedBoostItems, npc, true);
		}

		// 判斷成功或失敗
		if (Rnd.get(100) >= finalSuccessRate)
		{
			player.sendMessage("製作失敗！材料已消耗。");
			showMainPage(player, npc);
			return;
		}

		// 成功 - 隨機選擇武器
		int weaponId = WEAPON_IDS[Rnd.get(WEAPON_IDS.length)];

		// 隨機強化值
		int enchantLevel = Rnd.get(MIN_ENCHANT, MAX_ENCHANT);

		// 創建武器
		Item weapon = player.addItem(null, weaponId, 1, npc, true);
		if (weapon != null && enchantLevel > 0)
		{
			weapon.setEnchantLevel(enchantLevel);
			weapon.updateDatabase();
		}

		// 獲取武器名稱
		ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(weaponId);
		String weaponName = itemTemplate != null ? itemTemplate.getName() : "冰凍君主武器";

		player.sendMessage("製作成功：+" + enchantLevel + " " + weaponName);

		// 如果強化值達到閾值，全服公告
		if (enchantLevel >= ANNOUNCE_THRESHOLD)
		{
			String announcement = "恭喜玩家 " + player.getName() + " 製作出 +" + enchantLevel + " " + weaponName + "！";
			Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, "武器製作", announcement));
		}

		showMainPage(player, npc);
	}

	// ==================== 輔助方法 ====================

	/**
	 * 計算額外成功率加成
	 * @param boostItemCount 提升道具數量
	 * @return 額外成功率
	 */
	private int calculateExtraSuccessRate(long boostItemCount)
	{
		if (boostItemCount <= 0)
		{
			return 0;
		}

		// 計算加成率，但不超過上限
		int extraRate = (int) (boostItemCount * BOOST_PER_ITEM);
		return Math.min(extraRate, MAX_BOOST_RATE);
	}

	/**
	 * 格式化數字（千分位）
	 */
	private String formatNumber(long number)
	{
		return String.format("%,d", number);
	}

	public static void main(String[] args)
	{
		new FrozenWeaponCraft();
		System.out.println("【系統】冰凍君主武器製作系統載入完畢！");
	}
}