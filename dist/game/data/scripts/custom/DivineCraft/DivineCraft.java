package custom.DivineCraft;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 神匠強化系統
 * 讀取 DivineCraft.xml 配置，支援多系列裝備強化
 */
public class DivineCraft extends Script
{
	private static final int NPC_ID = 900046;
	private static final String XML_PATH = "data/scripts/custom/DivineCraft/DivineCraft.xml";

	// PAPERDOLL slot 對應
	private static final Map<String, Integer> SLOT_MAP = new LinkedHashMap<>();
	static
	{
		SLOT_MAP.put("HEAD",    Inventory.PAPERDOLL_HEAD);
		SLOT_MAP.put("CHEST",   Inventory.PAPERDOLL_CHEST);
		SLOT_MAP.put("LEGS",    Inventory.PAPERDOLL_LEGS);
		SLOT_MAP.put("GLOVES",  Inventory.PAPERDOLL_GLOVES);
		SLOT_MAP.put("FEET",    Inventory.PAPERDOLL_FEET);
		SLOT_MAP.put("BACK",    Inventory.PAPERDOLL_CLOAK);
		SLOT_MAP.put("BELT",    Inventory.PAPERDOLL_BELT);
		SLOT_MAP.put("NECK",    Inventory.PAPERDOLL_NECK);
		SLOT_MAP.put("RFINGER", Inventory.PAPERDOLL_RFINGER);
		SLOT_MAP.put("LFINGER", Inventory.PAPERDOLL_LFINGER);
		SLOT_MAP.put("REAR",    Inventory.PAPERDOLL_REAR);
		SLOT_MAP.put("LEAR",    Inventory.PAPERDOLL_LEAR);
	}

	// 系列資料結構
	private static class MaterialOption
	{
		int itemId;
		long count;
	}

	private static class SuccessRateRange
	{
		int min;
		int max;
		int rate;
	}

	private static class SlotConfig
	{
		String slotType;
		int itemIdMin;
		int itemIdMax;
		List<MaterialOption> materials = new ArrayList<>();
	}

	private static class SeriesConfig
	{
		String name;
		String id;
		int maxEnchant;
		int destroyChance;
		int protectionItemId;
		List<SuccessRateRange> successRates = new ArrayList<>();
		List<SlotConfig> slots = new ArrayList<>();

		// 根據當前強化等級獲取成功率
		int getSuccessRate(int currentEnchant)
		{
			int nextLevel = currentEnchant + 1;
			for (SuccessRateRange range : successRates)
			{
				if (nextLevel >= range.min && nextLevel <= range.max)
				{
					return range.rate;
				}
			}
			return 5; // 默認成功率
		}
	}

	private static final List<SeriesConfig> SERIES_LIST = new ArrayList<>();

	// 預載 HTM 內容，避免每次請求時從磁碟讀取
	private static String MAIN_HTM    = "";
	private static String SERIES_HTM  = "";
	private static String ENCHANT_HTM = "";

	private DivineCraft()
	{
		addFirstTalkId(NPC_ID);
		addTalkId(NPC_ID);
		addStartNpc(NPC_ID);
		loadConfig();
	}

	private static void loadConfig()
	{
		SERIES_LIST.clear();
		try
		{
			File file = new File("./" + XML_PATH);
			if (!file.exists())
			{
				LOGGER.warning("DivineCraft: 找不到配置文件 " + XML_PATH);
				return;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(file);
			doc.getDocumentElement().normalize();

			NodeList seriesNodes = doc.getElementsByTagName("series");
			for (int i = 0; i < seriesNodes.getLength(); i++)
			{
				Element seriesEl = (Element) seriesNodes.item(i);
				SeriesConfig series = new SeriesConfig();
				series.name = seriesEl.getAttribute("name");
				series.id = seriesEl.getAttribute("id");
				series.maxEnchant = Integer.parseInt(seriesEl.getAttribute("maxEnchant"));
				series.destroyChance = Integer.parseInt(seriesEl.getAttribute("destroyChance"));

				String protectionItemIdStr = seriesEl.getAttribute("protectionItemId");
				series.protectionItemId = protectionItemIdStr.isEmpty() ? 0 : Integer.parseInt(protectionItemIdStr);

				// 讀取成功率區間
				NodeList ratesNodes = seriesEl.getElementsByTagName("successRates");
				if (ratesNodes.getLength() > 0)
				{
					Element ratesEl = (Element) ratesNodes.item(0);
					NodeList rangeNodes = ratesEl.getElementsByTagName("range");
					for (int j = 0; j < rangeNodes.getLength(); j++)
					{
						Element rangeEl = (Element) rangeNodes.item(j);
						SuccessRateRange range = new SuccessRateRange();
						range.min = Integer.parseInt(rangeEl.getAttribute("min"));
						range.max = Integer.parseInt(rangeEl.getAttribute("max"));
						range.rate = Integer.parseInt(rangeEl.getAttribute("rate"));
						series.successRates.add(range);
					}
				}

				NodeList slotNodes = seriesEl.getElementsByTagName("slot");
				for (int j = 0; j < slotNodes.getLength(); j++)
				{
					Element slotEl = (Element) slotNodes.item(j);
					SlotConfig slot = new SlotConfig();
					slot.slotType = slotEl.getAttribute("type");
					slot.itemIdMin = Integer.parseInt(slotEl.getAttribute("itemIdMin"));
					slot.itemIdMax = Integer.parseInt(slotEl.getAttribute("itemIdMax"));

					NodeList materialNodes = slotEl.getElementsByTagName("material");
					for (int k = 0; k < materialNodes.getLength(); k++)
					{
						Element matEl = (Element) materialNodes.item(k);
						MaterialOption mat = new MaterialOption();
						mat.itemId = Integer.parseInt(matEl.getAttribute("itemId"));
						mat.count = Long.parseLong(matEl.getAttribute("count"));
						slot.materials.add(mat);
					}

					series.slots.add(slot);
				}
				SERIES_LIST.add(series);
			}
			LOGGER.info("DivineCraft: 載入 " + SERIES_LIST.size() + " 個系列配置");
		}
		catch (Exception e)
		{
			LOGGER.warning("DivineCraft: 載入配置失敗: " + e.getMessage());
		}

		// 預載 HTM 檔案到記憶體，避免玩家請求時觸發磁碟讀取
		final String basePath = "data/scripts/custom/DivineCraft/";
		MAIN_HTM    = HtmCache.getInstance().getHtm(null, basePath + "main.htm");
		SERIES_HTM  = HtmCache.getInstance().getHtm(null, basePath + "series.htm");
		ENCHANT_HTM = HtmCache.getInstance().getHtm(null, basePath + "enchant.htm");
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return showMainMenu(player);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("main"))
		{
			return showMainMenu(player);
		}
		else if (event.startsWith("series "))
		{
			String seriesId = event.substring(7);
			return showSeriesPage(player, seriesId);
		}
		else if (event.startsWith("enchant_page "))
		{
			// enchant_page seriesId slotType
			String[] parts = event.split(" ");
			if (parts.length >= 3)
			{
				return showEnchantPage(player, parts[1], parts[2]);
			}
		}
		else if (event.startsWith("do_enchant "))
		{
			// do_enchant seriesId slotType
			String[] parts = event.split(" ");
			if (parts.length >= 3)
			{
				return doEnchant(player, parts[1], parts[2]);
			}
		}
		return null;
	}

	private String showMainMenu(Player player)
	{
		StringBuilder sb = new StringBuilder();
		for (SeriesConfig series : SERIES_LIST)
		{
			sb.append("<table width=270 cellpadding=3 cellspacing=0>");
			sb.append("<tr><td align=center>");
			sb.append("<button value=\"").append(series.name).append("\"");
			sb.append(" action=\"bypass -h Quest DivineCraft series ").append(series.id).append("\"");
			sb.append(" width=200 height=28 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			sb.append("</td></tr></table>");
		}

		NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
		msg.setHtml(MAIN_HTM);
		msg.replace("%seriesButtons%", sb.toString());
		player.sendPacket(msg);
		return null;
	}

	private String showSeriesPage(Player player, String seriesId)
	{
		SeriesConfig series = findSeries(seriesId);
		if (series == null)
		{
			return showMainMenu(player);
		}

		StringBuilder slotListHtml = new StringBuilder();
		int rowIdx = 0;

		for (String slotType : SLOT_MAP.keySet())
		{
			SlotConfig slotCfg = findSlotInSeries(series, slotType);
			Item equipped = player.getInventory().getPaperdollItem(SLOT_MAP.get(slotType));

			String rowColor = (rowIdx % 2 == 0) ? "1A1A2A" : "1A2A1A";
			String slotName = getSlotName(slotType);
			String itemDisplay;
			String actionDisplay;

			if (slotCfg == null)
			{
				itemDisplay = "<font color=808080>（此系列無此部位）</font>";
				actionDisplay = "-";
			}
			else if (equipped == null || equipped.getId() < slotCfg.itemIdMin || equipped.getId() > slotCfg.itemIdMax)
			{
				itemDisplay = "<font color=FF4444>非" + series.name + "裝備</font>";
				actionDisplay = "-";
			}
			else
			{
				int enchant = equipped.getEnchantLevel();
				itemDisplay = "<font color=00FF88>" + equipped.getName() + (enchant > 0 ? " +" + enchant : "") + "</font>";

				if (enchant >= series.maxEnchant)
				{
					actionDisplay = "<font color=FFFF00>已滿</font>";
				}
				else
				{
					actionDisplay = "<button value=\"強化\" action=\"bypass -h Quest DivineCraft enchant_page " + seriesId + " " + slotType + "\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Small_Down\" fore=\"L2UI_CT1.Button_DF_Small\">";
				}
			}

			slotListHtml.append("<tr bgcolor=").append(rowColor).append(">")
				.append("<td align=center><font color=AAAAAA>").append(slotName).append("</font></td>")
				.append("<td>").append(itemDisplay).append("</td>")
				.append("<td align=center>").append(actionDisplay).append("</td>")
				.append("</tr>");
			rowIdx++;
		}

		NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
		msg.setHtml(SERIES_HTM);
		msg.replace("%seriesName%", series.name);
		msg.replace("%slotList%", slotListHtml.toString());
		player.sendPacket(msg);
		return null;
	}

	private String showEnchantPage(Player player, String seriesId, String slotType)
	{
		SeriesConfig series = findSeries(seriesId);
		SlotConfig slotCfg = series != null ? findSlotInSeries(series, slotType) : null;
		if (slotCfg == null) return showMainMenu(player);

		Item equipped = player.getInventory().getPaperdollItem(SLOT_MAP.get(slotType));
		if (equipped == null || equipped.getId() < slotCfg.itemIdMin || equipped.getId() > slotCfg.itemIdMax)
		{
			return showSeriesPage(player, seriesId);
		}

		int enchant = equipped.getEnchantLevel();
		int currentSuccessRate = series.getSuccessRate(enchant);

		// 一次性掃描背包，收集所有素材數量，避免重複遍歷
		final Map<Integer, Long> matCounts = new HashMap<>();
		for (MaterialOption mat : slotCfg.materials)
		{
			matCounts.put(mat.itemId, player.getInventory().getInventoryItemCount(mat.itemId, -1));
		}

		// 從快取結果找可用素材（不再掃背包）
		MaterialOption availableMat = null;
		for (MaterialOption mat : slotCfg.materials)
		{
			if (matCounts.get(mat.itemId) >= mat.count)
			{
				availableMat = mat;
				break;
			}
		}

		// 建立素材列表顯示（直接查快取，不再掃背包）
		StringBuilder matListHtml = new StringBuilder();
		matListHtml.append("<table width=270 bgcolor=111111 cellpadding=3 cellspacing=1>");
		matListHtml.append("<tr bgcolor=0A0A0A>")
			.append("<td width=110><font color=AAAAAA>素材名稱</font></td>")
			.append("<td width=50 align=center><font color=AAAAAA>需求</font></td>")
			.append("<td width=50 align=center><font color=AAAAAA>擁有</font></td>")
			.append("<td width=60 align=center><font color=AAAAAA>狀態</font></td>")
			.append("</tr>");

		int rowIdx = 0;
		for (MaterialOption mat : slotCfg.materials)
		{
			long playerMat = matCounts.get(mat.itemId);
			org.l2jmobius.gameserver.model.item.ItemTemplate matTemplate = org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(mat.itemId);
			String matName = matTemplate != null ? matTemplate.getName() : "道具ID " + mat.itemId;

			boolean enough = playerMat >= mat.count;
			String rowColor = (rowIdx % 2 == 0) ? "1A1A2A" : "1A2A1A";
			String statusColor = enough ? "00FF88" : "FF4444";
			String statusText = enough ? "足夠" : "不足";

			matListHtml.append("<tr bgcolor=").append(rowColor).append(">")
				.append("<td><font color=FFFFFF>").append(matName).append("</font></td>")
				.append("<td align=center><font color=FFCC33>").append(mat.count).append("</font></td>")
				.append("<td align=center><font color=AAAAAA>").append(playerMat).append("</font></td>")
				.append("<td align=center><font color=").append(statusColor).append(">").append(statusText).append("</font></td>")
				.append("</tr>");
			rowIdx++;
		}
		matListHtml.append("</table>");

		// 防爆道具資訊
		String protectionInfo = "";
		if (series.protectionItemId > 0)
		{
			long protectionCount = player.getInventory().getInventoryItemCount(series.protectionItemId, -1);
			org.l2jmobius.gameserver.model.item.ItemTemplate protTemplate = org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(series.protectionItemId);
			String protName = protTemplate != null ? protTemplate.getName() : "防爆道具";
			String protColor = protectionCount > 0 ? "00FF88" : "FF4444";

			protectionInfo = "<table width=270 bgcolor=2A0A0A cellpadding=3 cellspacing=1>";
			protectionInfo += "<tr><td align=center><font color=" + protColor + ">防爆道具: " + protName + " x" + protectionCount + "</font></td></tr>";
			protectionInfo += "<tr><td align=center><font color=FFAAAA size=1>失敗消失機率: " + series.destroyChance + "%（有防爆道具時不消失）</font></td></tr>";
			protectionInfo += "</table>";
		}
		else
		{
			protectionInfo = "<table width=270 bgcolor=2A0A0A cellpadding=3 cellspacing=1>";
			protectionInfo += "<tr><td align=center><font color=FFAAAA size=1>失敗消失機率: " + series.destroyChance + "%</font></td></tr>";
			protectionInfo += "</table>";
		}

		// 強化按鈕
		String enchantButton;
		if (enchant >= series.maxEnchant)
		{
			enchantButton = "<font color=FFFF00>此裝備已達最大強化值！</font>";
		}
		else if (availableMat == null)
		{
			enchantButton = "<font color=FF4444>素材不足，無法強化！</font>";
		}
		else
		{
			enchantButton = "<button value=\"確認強化\" action=\"bypass -h Quest DivineCraft do_enchant " + seriesId + " " + slotType + "\" width=150 height=30 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";
		}

		NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
		msg.setHtml(ENCHANT_HTM);
		msg.replace("%itemName%", equipped.getName() + (enchant > 0 ? " +" + enchant : ""));
		msg.replace("%currentEnchant%", String.valueOf(enchant));
		msg.replace("%maxEnchant%", String.valueOf(series.maxEnchant));
		msg.replace("%successRate%", String.valueOf(currentSuccessRate));
		msg.replace("%materialList%", matListHtml.toString());
		msg.replace("%protectionInfo%", protectionInfo);
		msg.replace("%enchantButton%", enchantButton);
		msg.replace("%seriesId%", seriesId);
		player.sendPacket(msg);
		return null;
	}

	private String doEnchant(Player player, String seriesId, String slotType)
	{
		SeriesConfig series = findSeries(seriesId);
		SlotConfig slotCfg = series != null ? findSlotInSeries(series, slotType) : null;
		if (slotCfg == null) return showMainMenu(player);

		Item equipped = player.getInventory().getPaperdollItem(SLOT_MAP.get(slotType));
		if (equipped == null || equipped.getId() < slotCfg.itemIdMin || equipped.getId() > slotCfg.itemIdMax)
		{
			return showSeriesPage(player, seriesId);
		}

		if (equipped.getEnchantLevel() >= series.maxEnchant)
		{
			player.sendMessage("此裝備已達最大強化值！");
			return showEnchantPage(player, seriesId, slotType);
		}

		// 尋找玩家擁有的第一個可用素材
		MaterialOption useMat = null;
		for (MaterialOption mat : slotCfg.materials)
		{
			long playerMat = player.getInventory().getInventoryItemCount(mat.itemId, -1);
			if (playerMat >= mat.count)
			{
				useMat = mat;
				break;
			}
		}

		if (useMat == null)
		{
			player.sendMessage("素材不足，無法強化！");
			return showEnchantPage(player, seriesId, slotType);
		}

		// 扣除素材
		player.destroyItemByItemId(org.l2jmobius.gameserver.model.item.enums.ItemProcessType.NONE, useMat.itemId, useMat.count, player, true);

		// 獲取當前強化等級的成功率
		int currentEnchant = equipped.getEnchantLevel();
		int successRate = series.getSuccessRate(currentEnchant);

		// 判斷成功失敗
		if (Rnd.get(100) < successRate)
		{
			equipped.setEnchantLevel(equipped.getEnchantLevel() + 1);
			equipped.updateDatabase();
			player.broadcastUserInfo();
			player.sendMessage("強化成功！" + equipped.getName() + " +" + equipped.getEnchantLevel());
		}
		else
		{
			player.sendMessage("強化失敗！素材已消耗。");

			// 判斷是否裝備消失
			if (Rnd.get(100) < series.destroyChance)
			{
				// 檢查是否有防爆道具
				boolean hasProtection = false;
				if (series.protectionItemId > 0)
				{
					long protectionCount = player.getInventory().getInventoryItemCount(series.protectionItemId, -1);
					if (protectionCount > 0)
					{
						// 扣除一個防爆道具
						player.destroyItemByItemId(org.l2jmobius.gameserver.model.item.enums.ItemProcessType.NONE, series.protectionItemId, 1, player, true);
						hasProtection = true;
						player.sendMessage("防爆道具已消耗，裝備未消失！");
					}
				}

				// 如果沒有防爆道具，裝備消失
				if (!hasProtection)
				{
					player.getInventory().unEquipItemInSlot(SLOT_MAP.get(slotType));
					player.destroyItem(org.l2jmobius.gameserver.model.item.enums.ItemProcessType.NONE, equipped, player, true);
					player.broadcastUserInfo();
					player.sendMessage("裝備已消失！");
					return showSeriesPage(player, seriesId);
				}
			}
		}

		return showEnchantPage(player, seriesId, slotType);
	}

	private SeriesConfig findSeries(String id)
	{
		for (SeriesConfig s : SERIES_LIST)
		{
			if (s.id.equals(id)) return s;
		}
		return null;
	}

	private SlotConfig findSlotInSeries(SeriesConfig series, String slotType)
	{
		for (SlotConfig slot : series.slots)
		{
			if (slot.slotType.equals(slotType)) return slot;
		}
		return null;
	}

	private String getSlotName(String slotType)
	{
		switch (slotType)
		{
			case "HEAD":    return "頭盔";
			case "CHEST":   return "胸甲";
			case "LEGS":    return "脛甲";
			case "GLOVES":  return "手套";
			case "FEET":    return "靴子";
			case "BACK":    return "披風";
			case "BELT":    return "腰帶";
			case "NECK":    return "項鍊";
			case "RFINGER": return "右戒指";
			case "LFINGER": return "左戒指";
			case "REAR":    return "右耳環";
			case "LEAR":    return "左耳環";
			default:        return slotType;
		}
	}

	public static void main(String[] args)
	{
		new DivineCraft();
	}
}
