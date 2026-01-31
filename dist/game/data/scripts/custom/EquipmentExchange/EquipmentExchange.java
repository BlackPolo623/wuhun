package custom.EquipmentExchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.variables.ItemVariables;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 裝備互換與轉生清洗系統
 * @author 黑普羅
 */
public class EquipmentExchange extends Script
{
	private static final Logger LOGGER = Logger.getLogger(EquipmentExchange.class.getName());

	// NPC ID
	private static final int EXCHANGE_NPC = 900033;

	// HTML路徑
	private static final String HTML_PATH = "data/scripts/custom/EquipmentExchange/";

	// 配置
	private static final boolean _enabled = true;
	private static final boolean _enableRebirthCleaning = true;

	// 互換消耗道具配置
	private static final int EXCHANGE_ITEM_ID = 57;      // 消耗道具ID (預設: 金幣)
	private static final long EXCHANGE_ITEM_COUNT = 100000000; // 單次互換消耗數量

	// 裝備系列配置 - 直接在這裡定義
	// 格式: {"系列名稱", 物品ID1, 物品ID2, 物品ID3, ...}
	private static final Object[][] EQUIPMENT_SERIES_CONFIG = {
		{"冰凍武器", 95725,95726,95727,95728,95729,95730,95731,95732,95733,95734,95735,95736,95737}
		// 在這裡添加更多系列...
	};

	// 裝備系列數據 <系列名稱, 物品ID列表>
	private static final Map<String, List<Integer>> _equipmentSeries = new LinkedHashMap<>();

	// 系列名稱列表 (按順序)
	private static final List<String> _seriesNameList = new ArrayList<>();

	public EquipmentExchange()
	{
		loadConfig();
		addStartNpc(EXCHANGE_NPC);
		addTalkId(EXCHANGE_NPC);
		addFirstTalkId(EXCHANGE_NPC);

		LOGGER.info("裝備互換系統已載入 - 系列數量: " + _equipmentSeries.size());
	}

	/**
	 * 載入配置
	 */
	private void loadConfig()
	{
		_equipmentSeries.clear();
		_seriesNameList.clear();

		// 從陣列載入裝備系列
		for (Object[] seriesData : EQUIPMENT_SERIES_CONFIG)
		{
			if (seriesData.length < 2)
			{
				continue; // 至少需要系列名稱和一個物品ID
			}

			String seriesName = (String) seriesData[0];
			List<Integer> items = new ArrayList<>();

			// 從索引1開始讀取物品ID
			for (int i = 1; i < seriesData.length; i++)
			{
				items.add((Integer) seriesData[i]);
			}

			_equipmentSeries.put(seriesName, items);
			_seriesNameList.add(seriesName);
		}

		LOGGER.info("裝備互換系統配置已載入 - 系列數量: " + _equipmentSeries.size());
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return getMainPage(player);
	}

	/**
	 * 獲取主頁面
	 */
	private String getMainPage(Player player)
	{
		// 生成系列按鈕
		StringBuilder seriesButtons = new StringBuilder();
		if (_seriesNameList.isEmpty())
		{
			seriesButtons.append("<font color=\"FF6666\">目前沒有開放的互換系列</font><br>");
		}
		else
		{
			for (int i = 0; i < _seriesNameList.size(); i++)
			{
				String seriesName = _seriesNameList.get(i);
				seriesButtons.append("<button value=\"").append(seriesName).append("\" action=\"bypass -h Quest EquipmentExchange series_").append(i).append("\" width=250 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>");
			}
		}

		// 生成轉生清洗區域
		StringBuilder rebirthSection = new StringBuilder();
		if (_enableRebirthCleaning)
		{
			rebirthSection.append("<table width=280 border=0 bgcolor=\"333333\">");
			rebirthSection.append("<tr><td align=center height=25><font color=\"FFAA00\">轉生清洗</font></td></tr>");
			rebirthSection.append("</table>");
			rebirthSection.append("<br>");
			rebirthSection.append("<font color=\"888888\">清除裝備的轉生等級<br>(不給予任何補償)</font><br><br>");
			rebirthSection.append("<button value=\"轉生清洗\" action=\"bypass -h Quest EquipmentExchange rebirth_clean\" width=250 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}

		Map<String, String> replacements = new HashMap<>();
		replacements.put("series_buttons", seriesButtons.toString());
		replacements.put("rebirth_section", rebirthSection.toString());
		return showHtmlFile(player, "main.htm", replacements);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		// 系列選擇
		if (event.startsWith("series_"))
		{
			int seriesIndex = Integer.parseInt(event.substring("series_".length()));
			if (seriesIndex >= 0 && seriesIndex < _seriesNameList.size())
			{
				String seriesName = _seriesNameList.get(seriesIndex);
				return getSeriesPage(player, seriesName);
			}
		}

		// 單件互換
		else if (event.startsWith("exchange_single_"))
		{
			String[] parts = event.substring("exchange_single_".length()).split("_");
			if (parts.length == 2)
			{
				int seriesIndex = Integer.parseInt(parts[0]);
				int sourceObjectId = Integer.parseInt(parts[1]);
				if (seriesIndex >= 0 && seriesIndex < _seriesNameList.size())
				{
					String seriesName = _seriesNameList.get(seriesIndex);
					return getExchangeTargetPage(player, seriesName, sourceObjectId, false);
				}
			}
		}

		// 全部互換
		else if (event.startsWith("exchange_all_"))
		{
			int seriesIndex = Integer.parseInt(event.substring("exchange_all_".length()));
			if (seriesIndex >= 0 && seriesIndex < _seriesNameList.size())
			{
				String seriesName = _seriesNameList.get(seriesIndex);
				return getExchangeAllPage(player, seriesName);
			}
		}

		// 執行全部互換 (先檢查更具體的條件)
		else if (event.startsWith("do_exchange_all_"))
		{
			String[] parts = event.substring("do_exchange_all_".length()).split("_");
			if (parts.length == 2)
			{
				int seriesIndex = Integer.parseInt(parts[0]);
				int targetItemId = Integer.parseInt(parts[1]);
				if (seriesIndex >= 0 && seriesIndex < _seriesNameList.size())
				{
					String seriesName = _seriesNameList.get(seriesIndex);
					return doExchangeAll(player, seriesName, targetItemId);
				}
			}
		}

		// 執行單件互換
		else if (event.startsWith("do_exchange_"))
		{
			String[] parts = event.substring("do_exchange_".length()).split("_");
			if (parts.length == 3)
			{
				int sourceItemId = Integer.parseInt(parts[0]);
				int targetItemId = Integer.parseInt(parts[1]);
				int objectId = Integer.parseInt(parts[2]);
				return doSingleExchange(player, sourceItemId, targetItemId, objectId);
			}
		}

		// 轉生清洗
		else if (event.equals("rebirth_clean"))
		{
			return getRebirthCleanPage(player);
		}

		// 執行轉生清洗
		else if (event.startsWith("do_clean_"))
		{
			int objectId = Integer.parseInt(event.substring("do_clean_".length()));
			return doRebirthClean(player, objectId);
		}

		// 返回主頁
		else if (event.equals("back"))
		{
			return getMainPage(player);
		}

		return null;
	}

	/**
	 * 獲取系列選擇頁面
	 */
	private String getSeriesPage(Player player, String seriesName)
	{
		List<Integer> seriesItems = _equipmentSeries.get(seriesName);
		if (seriesItems == null)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "系列不存在: " + seriesName));
		}

		// 檢查玩家背包中該系列的裝備
		List<Item> playerItems = getPlayerSeriesItems(player, seriesItems);

		StringBuilder itemList = new StringBuilder();
		if (playerItems.isEmpty())
		{
			itemList.append("<font color=\"FF6666\">您的背包中沒有此系列的裝備</font><br><br>");
		}
		else
		{
			itemList.append("<font color=\"LEVEL\">您擁有的裝備:</font><br>");
			itemList.append("<table width=280>");

			for (Item item : playerItems)
			{
				String displayName = getItemDisplayName(item);
				itemList.append("<tr>");
				itemList.append("<td width=180><font color=\"LEVEL\">").append(displayName).append("</font></td>");
				itemList.append("<td width=100 align=right><button value=\"互換\" action=\"bypass -h Quest EquipmentExchange exchange_single_").append(getSeriesIndex(seriesName)).append("_").append(item.getObjectId()).append("\" width=80 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				itemList.append("</tr>");
			}

			itemList.append("</table><br>");

			// 全部互換按鈕
			if (playerItems.size() > 1)
			{
				itemList.append("<button value=\"全部互換成同一種\" action=\"bypass -h Quest EquipmentExchange exchange_all_").append(getSeriesIndex(seriesName)).append("\" width=250 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>");
			}
		}

		Map<String, String> replacements = new HashMap<>();
		replacements.put("series_name", seriesName);
		replacements.put("item_list", itemList.toString());
		return showHtmlFile(player, "series_page.htm", replacements);
	}

	/**
	 * 獲取互換目標選擇頁面
	 */
	private String getExchangeTargetPage(Player player, String seriesName, int sourceObjectId, boolean isAll)
	{
		List<Integer> seriesItems = _equipmentSeries.get(seriesName);
		if (seriesItems == null)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "系列不存在"));
		}

		// 找出要互換的物品
		Item sourceItem = null;
		if (!isAll)
		{
			sourceItem = player.getInventory().getItemByObjectId(sourceObjectId);
			if (sourceItem == null)
			{
				return showHtmlFile(player, "error.htm", createMap("message", "找不到該裝備"));
			}

			// 檢查轉生等級必須為0
			int rebirthLevel = sourceItem.getVariables().getInt(ItemVariables.zbzscsu, 0);
			if (rebirthLevel > 0)
			{
				return showHtmlFile(player, "error.htm", createMap("message", "該裝備有轉生屬性 (+" + rebirthLevel + ")，無法互換<br>請先進行轉生清洗"));
			}
		}

		// 生成來源物品顯示
		StringBuilder sourceItemHtml = new StringBuilder();
		if (sourceItem != null)
		{
			sourceItemHtml.append("<font color=\"LEVEL\">來源裝備:</font><br>");
			sourceItemHtml.append("<font color=\"FFFFFF\">").append(getItemDisplayName(sourceItem)).append("</font><br><br>");
		}

		// 生成目標物品列表
		StringBuilder targetItems = new StringBuilder();
		for (int itemId : seriesItems)
		{
			// 如果是單件互換,排除來源物品
			if (!isAll && sourceItem != null && sourceItem.getId() == itemId)
			{
				continue;
			}

			ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
			if (template != null)
			{
				targetItems.append("<tr>");
				targetItems.append("<td width=180><font color=\"LEVEL\">").append(template.getName()).append("</font></td>");
				targetItems.append("<td width=100 align=right><button value=\"選擇\" action=\"bypass -h Quest EquipmentExchange do_exchange_").append(sourceItem != null ? sourceItem.getId() : 0).append("_").append(itemId).append("_").append(sourceItem != null ? sourceItem.getObjectId() : 0).append("\" width=80 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				targetItems.append("</tr>");
			}
		}

		Map<String, String> replacements = new HashMap<>();
		replacements.put("source_item", sourceItemHtml.toString());
		replacements.put("target_items", targetItems.toString());
		replacements.put("encoded_series", String.valueOf(getSeriesIndex(seriesName)));
		return showHtmlFile(player, "exchange_target.htm", replacements);
	}

	/**
	 * 獲取全部互換頁面
	 */
	private String getExchangeAllPage(Player player, String seriesName)
	{
		List<Integer> seriesItems = _equipmentSeries.get(seriesName);
		if (seriesItems == null)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "系列不存在"));
		}

		List<Item> playerItems = getPlayerSeriesItems(player, seriesItems);
		if (playerItems.size() < 2)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "您至少需要2件該系列裝備才能使用全部互換"));
		}

		// 生成裝備列表
		StringBuilder itemListHtml = new StringBuilder();
		for (Item item : playerItems)
		{
			itemListHtml.append("<tr><td><font color=\"FFFFFF\">").append(getItemDisplayName(item)).append("</font></td></tr>");
		}

		// 生成目標選擇列表
		StringBuilder targetItems = new StringBuilder();
		for (int itemId : seriesItems)
		{
			ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
			if (template != null)
			{
				targetItems.append("<tr>");
				targetItems.append("<td width=180><font color=\"LEVEL\">").append(template.getName()).append("</font></td>");
				targetItems.append("<td width=100 align=right><button value=\"全換成這個\" action=\"bypass -h Quest EquipmentExchange do_exchange_all_").append(getSeriesIndex(seriesName)).append("_").append(itemId).append("\" width=100 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				targetItems.append("</tr>");
			}
		}

		Map<String, String> replacements = new HashMap<>();
		replacements.put("item_count", String.valueOf(playerItems.size()));
		replacements.put("item_list", itemListHtml.toString());
		replacements.put("target_items", targetItems.toString());
		replacements.put("encoded_series", String.valueOf(getSeriesIndex(seriesName)));
		return showHtmlFile(player, "exchange_all.htm", replacements);
	}

	/**
	 * 執行單件互換
	 */
	private String doSingleExchange(Player player, int sourceItemId, int targetItemId, int objectId)
	{
		// 找出來源物品
		Item sourceItem = player.getInventory().getItemByObjectId(objectId);
		if (sourceItem == null)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "找不到來源裝備"));
		}

		// 檢查是否在身上
		if (sourceItem.isEquipped())
		{
			return showHtmlFile(player, "error.htm", createMap("message", "請先將裝備卸下再進行互換"));
		}

		// 檢查是否有足夠的消耗道具
		long playerItemCount = player.getInventory().getInventoryItemCount(EXCHANGE_ITEM_ID, -1);
		if (playerItemCount < EXCHANGE_ITEM_COUNT)
		{
			ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(EXCHANGE_ITEM_ID);
			String itemName = itemTemplate != null ? itemTemplate.getName() : "道具";
			return showHtmlFile(player, "error.htm", createMap("message",
				"互換需要消耗 " + formatNumber(EXCHANGE_ITEM_COUNT) + " 個 " + itemName + "<br>您目前擁有: " + formatNumber(playerItemCount)));
		}

		// 扣除消耗道具
		if (!player.destroyItemByItemId(ItemProcessType.NONE, EXCHANGE_ITEM_ID, EXCHANGE_ITEM_COUNT, player, true))
		{
			return showHtmlFile(player, "error.htm", createMap("message", "扣除消耗道具失敗"));
		}

		// 獲取強化等級
		int enchantLevel = sourceItem.getEnchantLevel();

		// 刪除舊物品
		if (!player.destroyItem(ItemProcessType.NONE, sourceItem, player, true))
		{
			return showHtmlFile(player, "error.htm", createMap("message", "移除舊裝備失敗"));
		}

		// 給予新物品
		Item newItem = player.addItem(ItemProcessType.NONE, targetItemId, 1, player, true);
		if (newItem == null)
		{
			// 如果失敗,返還舊物品
			player.addItem(ItemProcessType.NONE, sourceItemId, 1, player, true);
			return showHtmlFile(player, "error.htm", createMap("message", "給予新裝備失敗"));
		}

		// 設置強化等級
		if (enchantLevel > 0)
		{
			newItem.setEnchantLevel(enchantLevel);
			newItem.updateDatabase();
		}

		// 刷新角色背包跟資料
		player.broadcastUserInfo();
		player.sendItemList();
		player.updateZscsCache();

		// 顯示成功頁面
		ItemTemplate sourceTemplate = ItemData.getInstance().getTemplate(sourceItemId);
		ItemTemplate targetTemplate = ItemData.getInstance().getTemplate(targetItemId);

		Map<String, String> replacements = new HashMap<>();
		replacements.put("title", "互換成功！");
		replacements.put("line1", "原裝備: " + (sourceTemplate != null ? sourceTemplate.getName() : "未知"));
		replacements.put("line2", "新裝備: " + (targetTemplate != null ? targetTemplate.getName() : "未知") + (enchantLevel > 0 ? " (+" + enchantLevel + ")" : ""));
		return showHtmlFile(player, "success.htm", replacements);
	}

	/**
	 * 執行全部互換
	 */
	private String doExchangeAll(Player player, String seriesName, int targetItemId)
	{
		List<Integer> seriesItems = _equipmentSeries.get(seriesName);
		if (seriesItems == null)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "系列不存在"));
		}

		// 獲取玩家該系列的所有裝備
		List<Item> playerItems = getPlayerSeriesItems(player, seriesItems);
		if (playerItems.isEmpty())
		{
			return showHtmlFile(player, "error.htm", createMap("message", "您沒有該系列的裝備"));
		}

		// 計算需要互換的裝備數量 (排除已裝備的)
		int itemsToExchange = 0;
		for (Item item : playerItems)
		{
			if (!item.isEquipped())
			{
				itemsToExchange++;
			}
		}

		if (itemsToExchange == 0)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "沒有裝備可以互換(可能都已裝備)"));
		}

		// 計算所需消耗道具總量
		long totalCost = EXCHANGE_ITEM_COUNT * itemsToExchange;
		long playerItemCount = player.getInventory().getInventoryItemCount(EXCHANGE_ITEM_ID, -1);

		// 檢查是否有足夠的消耗道具
		if (playerItemCount < totalCost)
		{
			ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(EXCHANGE_ITEM_ID);
			String itemName = itemTemplate != null ? itemTemplate.getName() : "道具";
			return showHtmlFile(player, "error.htm", createMap("message",
				"批量互換需要消耗 " + formatNumber(totalCost) + " 個 " + itemName + "<br>您目前擁有: " + formatNumber(playerItemCount) + "<br>需要互換裝備數: " + itemsToExchange));
		}

		// 扣除消耗道具
		if (!player.destroyItemByItemId(ItemProcessType.NONE, EXCHANGE_ITEM_ID, totalCost, player, true))
		{
			return showHtmlFile(player, "error.htm", createMap("message", "扣除消耗道具失敗"));
		}

		int successCount = 0;

		// 逐一互換
		for (Item item : playerItems)
		{
			if (item.isEquipped())
			{
				continue; // 跳過已裝備的
			}

			int enchantLevel = item.getEnchantLevel();

			// 刪除舊物品
			if (player.destroyItem(ItemProcessType.NONE, item, player, false))
			{
				// 給予新物品
				Item newItem = player.addItem(ItemProcessType.NONE, targetItemId, 1, player, false);
				if (newItem != null)
				{
					// 設置強化等級
					if (enchantLevel > 0)
					{
						newItem.setEnchantLevel(enchantLevel);
						newItem.updateDatabase();
					}
					successCount++;
				}
			}
		}

		if (successCount == 0)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "沒有裝備可以互換(可能都已裝備)"));
		}

		// 刷新角色背包跟資料
		player.broadcastUserInfo();
		player.sendItemList();
		player.updateZscsCache();

		// 發送系統訊息
		player.sendMessage("成功互換 " + successCount + " 件裝備");

		ItemTemplate targetTemplate = ItemData.getInstance().getTemplate(targetItemId);
		Map<String, String> replacements = new HashMap<>();
		replacements.put("title", "批量互換成功！");
		replacements.put("line1", "成功互換: " + successCount + " 件");
		replacements.put("line2", "目標裝備: " + (targetTemplate != null ? targetTemplate.getName() : "未知"));
		return showHtmlFile(player, "success.htm", replacements);
	}

	/**
	 * 獲取轉生清洗頁面
	 */
	private String getRebirthCleanPage(Player player)
	{
		// 獲取所有有轉生的裝備 (包含已裝備的)
		List<Item> rebirthItems = new ArrayList<>();

		for (Item item : player.getInventory().getItems())
		{
			int rebirthLevel = item.getVariables().getInt(ItemVariables.zbzscsu, 0);
			if (rebirthLevel > 0)
			{
				rebirthItems.add(item);
			}
		}

		StringBuilder itemListHtml = new StringBuilder();
		if (rebirthItems.isEmpty())
		{
			itemListHtml.append("<font color=\"LEVEL\">背包中沒有可清洗的裝備</font><br>");
		}
		else
		{
			itemListHtml.append("<font color=\"LEVEL\">可清洗的裝備:</font><br>");
			itemListHtml.append("<table width=280>");

			for (Item item : rebirthItems)
			{
				itemListHtml.append("<tr>");
				itemListHtml.append("<td width=180><font color=\"LEVEL\">").append(getItemDisplayName(item)).append("</font></td>");
				itemListHtml.append("<td width=100 align=right><button value=\"清洗\" action=\"bypass -h Quest EquipmentExchange do_clean_").append(item.getObjectId()).append("\" width=80 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				itemListHtml.append("</tr>");
			}

			itemListHtml.append("</table>");
		}

		Map<String, String> replacements = new HashMap<>();
		replacements.put("item_list", itemListHtml.toString());
		return showHtmlFile(player, "rebirth_clean.htm", replacements);
	}

	/**
	 * 執行轉生清洗
	 */
	private String doRebirthClean(Player player, int objectId)
	{
		Item item = player.getInventory().getItemByObjectId(objectId);
		if (item == null)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "找不到該裝備"));
		}

		// 清除轉生數據
		int rebirthLevel = item.getVariables().getInt(ItemVariables.zbzscsu, 0);
		if (rebirthLevel > 0)
		{
			item.getVariables().set(ItemVariables.zbzscsu, 0);
			item.getVariables().storeMe();
			player.updateZscsCache();
			player.broadcastUserInfo();
			player.sendItemList();

			Map<String, String> replacements = new HashMap<>();
			replacements.put("title", "清洗成功！");
			replacements.put("line1", "裝備: " + getItemDisplayName(item));
			replacements.put("line2", "轉生等級已清除 (原等級: +" + rebirthLevel + ")");
			return showHtmlFile(player, "success.htm", replacements);
		}
		else
		{
			return showHtmlFile(player, "error.htm", createMap("message", "該裝備沒有轉生屬性"));
		}
	}

	/**
	 * 獲取玩家背包中該系列的裝備
	 */
	private List<Item> getPlayerSeriesItems(Player player, List<Integer> seriesItems)
	{
		List<Item> result = new ArrayList<>();

		for (Item item : player.getInventory().getItems())
		{
			// 只顯示符合系列、未裝備、且轉生等級為0的裝備
			int rebirthLevel = item.getVariables().getInt(ItemVariables.zbzscsu, 0);
			if (seriesItems.contains(item.getId()) && !item.isEquipped() && rebirthLevel == 0)
			{
				result.add(item);
			}
		}

		return result;
	}

	/**
	 * 獲取物品顯示名稱
	 */
	private String getItemDisplayName(Item item)
	{
		int enchant = item.getEnchantLevel();
		String name = item.getTemplate().getName();
		return enchant > 0 ? "+" + enchant + " " + name : name;
	}

	/**
	 * 獲取系列索引
	 */
	private int getSeriesIndex(String seriesName)
	{
		return _seriesNameList.indexOf(seriesName);
	}

	/**
	 * 創建單一鍵值對的 Map
	 */
	private Map<String, String> createMap(String key, String value)
	{
		Map<String, String> map = new HashMap<>();
		map.put(key, value);
		return map;
	}

	/**
	 * 顯示HTML文件並替換變數
	 */
	private String showHtmlFile(Player player, String fileName, Map<String, String> replacements)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, HTML_PATH + fileName);

		// 替換所有變數
		if (replacements != null && !replacements.isEmpty())
		{
			for (Map.Entry<String, String> entry : replacements.entrySet())
			{
				String key = entry.getKey();
				String value = entry.getValue();
				if (key != null && value != null)
				{
					html.replace("%" + key + "%", value);
				}
			}
		}

		player.sendPacket(html);
		return null;
	}

	/**
	 * 格式化數字 (加入千分位逗號)
	 */
	private String formatNumber(long number)
	{
		return String.format("%,d", number);
	}

	public static void main(String[] args)
	{
		new EquipmentExchange();
	}
}
