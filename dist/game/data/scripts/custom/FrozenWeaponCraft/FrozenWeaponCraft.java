package custom.FrozenWeaponCraft;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.TimedHuntingZoneData;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.huntingzones.TimedHuntingZoneEnter;
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

	// 冰凍君主之城 Zone ID
	private static final int FROZEN_ZONE_ID = 18;

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
	private static final int BASE_SUCCESS_RATE = 10;  // 基礎成功率 (10%)

	// ==================== 成功率提升道具配置 ====================
	private static final int BOOST_ITEM_ID = 95781;         // 提升成功率的道具ID
	private static final int BOOST_PER_ITEM = 1;           // 每個道具增加的成功率 (1%)
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
		showIndexPage(player, npc);
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
			case "index":
				showIndexPage(player, npc);
				break;
			case "showCraft":
				showCraftPage(player, npc);
				break;
			case "craft":
				processCraft(player, npc);
				break;
			case "enterZone":
				enterFrozenZone(player, npc);
				break;
		}

		return null;
	}

	// ==================== 頁面顯示 ====================

	/**
	 * 顯示首頁 - 兩個選項
	 */
	private void showIndexPage(Player player, Npc npc)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, HTML_PATH + "index.htm");

		final TimedHuntingZoneHolder holder = TimedHuntingZoneData.getInstance().getHuntingZone(FROZEN_ZONE_ID);

		// 獲取上次進入時間
		final long lastEntryTime = player.getVariables().getLong(PlayerVariables.HUNTING_ZONE_ENTRY + FROZEN_ZONE_ID, 0);
		final long currentTime = System.currentTimeMillis();

		// 判斷是否已重置（從未進入或超過重置時間）
		// 注意：getResetDelay() 返回的是毫秒！
		boolean isReset = (lastEntryTime == 0) || ((lastEntryTime + holder.getResetDelay()) < currentTime);

		String timeDisplay;
		String statusDisplay;

		if (isReset)
		{
			// 從未進入或已重置 - 顯示初始時間
			// 注意：getInitialTime() 返回的是毫秒！
			long initialMs = holder.getInitialTime();
			long hours = initialMs / (60 * 60 * 1000);  // 毫秒轉小時
			long minutes = (initialMs % (60 * 60 * 1000)) / (60 * 1000);  // 毫秒轉分鐘
			timeDisplay = "<font color=\"00FF00\">" + hours + " 小時 " + minutes + " 分鐘</font>";
			statusDisplay = "<font color=\"00FF00\">可進入</font>";
		}
		else
		{
			// 已進入且未重置 - 顯示剩餘時間
			long remainingMs = player.getTimedHuntingZoneRemainingTime(FROZEN_ZONE_ID);

			if (remainingMs > 0)
			{
				long hours = remainingMs / (60 * 60 * 1000);
				long minutes = (remainingMs % (60 * 60 * 1000)) / (60 * 1000);
				timeDisplay = "<font color=\"FFFF00\">" + hours + " 小時 " + minutes + " 分鐘</font>";
				statusDisplay = "<font color=\"00FF00\">可進入</font>";
			}
			else
			{
				timeDisplay = "<font color=\"FF3333\">時間已用完</font>";
				statusDisplay = "<font color=\"FF3333\">無法進入</font>";
			}
		}

		html.replace("%remaining_time%", timeDisplay);
		html.replace("%entry_status%", statusDisplay);

		player.sendPacket(html);
	}

	/**
	 * 顯示武器製作頁面
	 */
	private void showCraftPage(Player player, Npc npc)
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

	// ==================== 進入冰凍君主之城 ====================

	/**
	 * 進入冰凍君主之城（限時獵場）
	 */
	private void enterFrozenZone(Player player, Npc npc)
	{
		if (!GlobalVariablesManager.getInstance().getBoolean("AvailableFrostLord", false))
		{
			player.sendMessage("冰凍君主城堡目前未開放！");
			player.sendMessage("開放時間：每週二～六 當日18:00 ~ 隔日15:00");
			return;
		}
		// 檢查各種條件（參考 ExTimedHuntingZoneEnter）
		if (player.isInCombat())
		{
			player.sendMessage("戰鬥中無法進入限時獵場。");
			return;
		}

		if (player.getReputation() < 0)
		{
			player.sendMessage("聲望為負時無法進入限時獵場。");
			return;
		}

		if (player.isMounted())
		{
			player.sendMessage("騎乘狀態無法進入限時獵場。");
			return;
		}

		if (player.isInDuel())
		{
			player.sendMessage("決鬥中無法進入限時獵場。");
			return;
		}

		if (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendMessage("奧林匹亞競技場期間無法進入限時獵場。");
			return;
		}

		if (player.isRegisteredOnEvent())
		{
			player.sendMessage("活動期間無法進入限時獵場。");
			return;
		}

		if (player.isInInstance())
		{
			player.sendMessage("副本中無法進入限時獵場。");
			return;
		}

		if (player.isPrisoner())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_FUNCTION_IN_THE_UNDERGROUND_LABYRINTH);
			return;
		}

		// 獲取獵場數據
		final TimedHuntingZoneHolder holder = TimedHuntingZoneData.getInstance().getHuntingZone(FROZEN_ZONE_ID);
		if (holder == null)
		{
			player.sendMessage("找不到冰凍君主之城的配置數據。");
			return;
		}

		// 檢查等級
		if ((player.getLevel() < holder.getMinLevel()) || (player.getLevel() > holder.getMaxLevel()))
		{
			player.sendMessage("您的等級不符合進入條件。需要等級：" + holder.getMinLevel() + "-" + holder.getMaxLevel());
			return;
		}

		// 處理時間和冷卻
		final long currentTime = System.currentTimeMillis();
		final int instanceId = holder.getInstanceId();

		// 檢查副本冷卻
		if ((instanceId > 0) && holder.isSoloInstance())
		{
			if (InstanceManager.getInstance().getInstanceTime(player, instanceId) > currentTime)
			{
				player.sendMessage("冰凍君主之城尚未重置。");
				return;
			}
		}

		//  計算結束時間
		long endTime = currentTime + player.getTimedHuntingZoneRemainingTime(FROZEN_ZONE_ID);
		final long lastEntryTime = player.getVariables().getLong(PlayerVariables.HUNTING_ZONE_ENTRY + FROZEN_ZONE_ID, 0);

		//  如果已重置，給予新的時間
		if ((lastEntryTime + holder.getResetDelay()) < currentTime)
		{
			if (endTime == currentTime)
			{
				endTime += holder.getInitialTime();  // 已經是毫秒
			}
			player.getVariables().set(PlayerVariables.HUNTING_ZONE_ENTRY + FROZEN_ZONE_ID, currentTime);
		}

		//  關鍵：這裡改成 > 而不是 <=
		if (endTime > currentTime)
		{
			// 扣除入場費
			if (holder.getEntryItemId() == Inventory.ADENA_ID)
			{
				if (player.getAdena() >= holder.getEntryFee())
				{
					player.reduceAdena(ItemProcessType.FEE, holder.getEntryFee(), player, true);
				}
				else
				{
					player.sendMessage("金幣不足。需要：" + formatNumber(holder.getEntryFee()) + " 金幣。");
					return;
				}
			}
			else if (!player.destroyItemByItemId(ItemProcessType.FEE, holder.getEntryItemId(), holder.getEntryFee(), player, true))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS);
				return;
			}

			// 設置變量
			player.getVariables().set(PlayerVariables.LAST_HUNTING_ZONE_ID, FROZEN_ZONE_ID);
			player.getVariables().set(PlayerVariables.HUNTING_ZONE_TIME + FROZEN_ZONE_ID, endTime - currentTime);

			// 傳送玩家
			if (instanceId == 0)
			{
				player.teleToLocation(holder.getEnterLocation());
				player.sendPacket(new TimedHuntingZoneEnter(player, FROZEN_ZONE_ID));
				player.sendMessage("已進入冰凍君主之城！");
			}
			else
			{
				// 副本獵場處理（如果需要）
				player.teleToLocation(holder.getEnterLocation());
				player.sendPacket(new TimedHuntingZoneEnter(player, FROZEN_ZONE_ID));
				player.sendMessage("已進入冰凍君主之城副本！");
			}
		}
		else
		{
			player.sendMessage("您沒有足夠的時間進入獵場。");
		}
	}

	// ==================== 武器製作邏輯 ====================

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
				showCraftPage(player, npc);
				return;
			}
		}

		// 檢查背包空間
		if (!player.getInventory().validateCapacity(1))
		{
			player.sendMessage("背包空間不足！");
			showCraftPage(player, npc);
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
			showCraftPage(player, npc);
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

		showCraftPage(player, npc);
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