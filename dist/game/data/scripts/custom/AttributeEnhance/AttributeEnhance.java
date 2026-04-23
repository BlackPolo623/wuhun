package custom.AttributeEnhance;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.item.enchant.attribute.AttributeHolder;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * 屬性強化系統
 * NPC ID 在下方 NPC_ID 常數修改
 * 其餘所有數值設定請修改 AttributeEnhance.xml
 */
public class AttributeEnhance extends Script
{
	// ── 修改這裡設定 NPC ID ──────────────────────────────
	private static final int    NPC_ID   = 900045;
	private static final String XML_PATH = "data/scripts/custom/AttributeEnhance/AttributeEnhance.xml";
	private static final String HTM_PATH = "data/scripts/custom/AttributeEnhance/";

	// ── 元素顯示 ─────────────────────────────────────────
	private static final AttributeType[] ALL_ELEMENTS =
	{
		AttributeType.FIRE, AttributeType.WATER, AttributeType.WIND,
		AttributeType.EARTH, AttributeType.HOLY, AttributeType.DARK
	};

	private static final String[] ELEMENT_COLOR =
	{
		"FF4400", "4488FF", "88FF44", "AA6600", "FFFF00", "AA00FF"
	};

	// 武器上的屬性 = 攻擊屬性（直接）
	private static final String[] WEAPON_DESC =
	{
		"火攻擊", "水攻擊", "風攻擊", "地攻擊", "聖攻擊", "暗攻擊"
	};

	// 防具/飾品上的屬性 = 防禦「對立」屬性攻擊
	// Fire 屬性防具 → 防禦 Water 攻擊，依此類推
	private static final String[] ARMOR_DESC =
	{
		"火抗性", "水抗性", "風抗性", "地抗性", "聖抗性", "暗抗性"
	};

	// ── 部位對應 ──────────────────────────────────────────
	private static final Map<String, Integer> SLOT_MAP = new HashMap<>();
	private static final Map<String, String>  SLOT_ZH  = new HashMap<>();
	static
	{
		SLOT_MAP.put("head",    Inventory.PAPERDOLL_HEAD);
		SLOT_MAP.put("chest",   Inventory.PAPERDOLL_CHEST);
		SLOT_MAP.put("legs",    Inventory.PAPERDOLL_LEGS);
		SLOT_MAP.put("gloves",  Inventory.PAPERDOLL_GLOVES);
		SLOT_MAP.put("boots",   Inventory.PAPERDOLL_FEET);
		SLOT_MAP.put("weapon",  Inventory.PAPERDOLL_RHAND);
		SLOT_MAP.put("neck",    Inventory.PAPERDOLL_NECK);
		SLOT_MAP.put("rear",    Inventory.PAPERDOLL_REAR);
		SLOT_MAP.put("lear",    Inventory.PAPERDOLL_LEAR);
		SLOT_MAP.put("rfinger", Inventory.PAPERDOLL_RFINGER);
		SLOT_MAP.put("lfinger", Inventory.PAPERDOLL_LFINGER);

		SLOT_ZH.put("head",    "頭盔");
		SLOT_ZH.put("chest",   "胸甲");
		SLOT_ZH.put("legs",    "腿甲");
		SLOT_ZH.put("gloves",  "手套");
		SLOT_ZH.put("boots",   "鞋子");
		SLOT_ZH.put("weapon",  "主武器");
		SLOT_ZH.put("neck",    "項鍊");
		SLOT_ZH.put("rear",    "右耳環");
		SLOT_ZH.put("lear",    "左耳環");
		SLOT_ZH.put("rfinger", "右戒指");
		SLOT_ZH.put("lfinger", "左戒指");
	}

	private static final String[] ARMOR_SLOTS   = { "head", "chest", "legs", "gloves", "boots" };
	private static final String[] WEAPON_SLOTS  = { "weapon" };
	private static final String[] JEWELRY_SLOTS = { "neck", "rear", "lear", "rfinger", "lfinger" };

	// ── 設定資料 ──────────────────────────────────────────
	/** [slot][elementOrdinal] → StoneConfig */
	private static final Map<String, Map<Integer, StoneConfig>> ARMOR_STONES  = new HashMap<>();
	private static final Map<String, Map<Integer, StoneConfig>> WEAPON_STONES = new HashMap<>();

	// 預載 HTM 內容，避免每次請求時從磁碟讀取
	private static String MAIN_HTM    = "";
	private static String ARMOR_HTM   = "";
	private static String WEAPON_HTM  = "";
	private static String JEWELRY_HTM = "";
	private static String STATUS_HTM  = "";

	// 以下值由 loadConfig() 從 XML 載入，此處初始值僅為 XML 載入失敗時的備用預設
	private static int    JEWELRY_ITEM_ID   = 57;
	private static int    JEWELRY_MIN_SLOTS = 1;
	private static int    JEWELRY_MAX_SLOTS = 1;
	private static int    JEWELRY_MIN_VALUE = 0;
	private static int    JEWELRY_MAX_VALUE = 0;

	// ── 設定結構 ──────────────────────────────────────────
	private static class StoneConfig
	{
		final AttributeType element;
		final int           itemId;
		final int           minGain;
		final int           maxGain;
		final int           maxValue;
		final double        successRate;

		StoneConfig(AttributeType element, int itemId, int minGain, int maxGain, int maxValue, double successRate)
		{
			this.element     = element;
			this.itemId      = itemId;
			this.minGain     = minGain;
			this.maxGain     = maxGain;
			this.maxValue    = maxValue;
			this.successRate = successRate;
		}
	}

	// ── 建構子 ────────────────────────────────────────────
	public AttributeEnhance()
	{
		loadConfig();
		addStartNpc(NPC_ID);
		addFirstTalkId(NPC_ID);
		addTalkId(NPC_ID);
	}

	// ── XML 載入 ──────────────────────────────────────────
	private static void loadConfig()
	{
		ARMOR_STONES.clear();
		WEAPON_STONES.clear();

		try
		{
			final File file = new File("./" + XML_PATH);
			if (!file.exists())
			{
				LOGGER.warning("AttributeEnhance: 找不到設定檔 " + XML_PATH);
				return;
			}

			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder        builder = factory.newDocumentBuilder();
			final Document               doc     = builder.parse(file);
			doc.getDocumentElement().normalize();

			// 防具石
			final NodeList armorGroups = doc.getElementsByTagName("armorStones");
			if (armorGroups.getLength() > 0)
			{
				final NodeList stones = ((Element) armorGroups.item(0)).getElementsByTagName("stone");
				for (int i = 0; i < stones.getLength(); i++)
				{
					final Element     el  = (Element) stones.item(i);
					final String      slot = el.getAttribute("slot");
					final StoneConfig cfg  = parseStone(el);
					if (cfg != null)
					{
						ARMOR_STONES.computeIfAbsent(slot, k -> new HashMap<>()).put((int) cfg.element.getClientId(), cfg);
					}
				}
			}

			// 武器石
			final NodeList weaponGroups = doc.getElementsByTagName("weaponStones");
			if (weaponGroups.getLength() > 0)
			{
				final NodeList stones = ((Element) weaponGroups.item(0)).getElementsByTagName("stone");
				for (int i = 0; i < stones.getLength(); i++)
				{
					final Element     el   = (Element) stones.item(i);
					final String      slot = el.getAttribute("slot");
					final StoneConfig cfg  = parseStone(el);
					if (cfg != null)
					{
						WEAPON_STONES.computeIfAbsent(slot, k -> new HashMap<>()).put((int) cfg.element.getClientId(), cfg);
					}
				}
			}

			// 飾品石
			final NodeList jwNodes = doc.getElementsByTagName("jewelryStone");
			if (jwNodes.getLength() > 0)
			{
				final Element jw = (Element) jwNodes.item(0);
				JEWELRY_ITEM_ID   = Integer.parseInt(jw.getAttribute("itemId"));
				JEWELRY_MIN_SLOTS = Integer.parseInt(jw.getAttribute("minSlots"));
				JEWELRY_MAX_SLOTS = Integer.parseInt(jw.getAttribute("maxSlots"));
				JEWELRY_MIN_VALUE = Integer.parseInt(jw.getAttribute("minValue"));
				JEWELRY_MAX_VALUE = Integer.parseInt(jw.getAttribute("maxValue"));
			}

			LOGGER.info("AttributeEnhance: 設定載入完成。");
		}
		catch (Exception e)
		{
			LOGGER.warning("AttributeEnhance: 設定載入失敗！" + e.getMessage());
		}

		// 預載 HTM 檔案到記憶體，避免玩家請求時觸發磁碟讀取
		MAIN_HTM    = HtmCache.getInstance().getHtm(null, HTM_PATH + "main.htm");
		ARMOR_HTM   = HtmCache.getInstance().getHtm(null, HTM_PATH + "armor.htm");
		WEAPON_HTM  = HtmCache.getInstance().getHtm(null, HTM_PATH + "weapon.htm");
		JEWELRY_HTM = HtmCache.getInstance().getHtm(null, HTM_PATH + "jewelry.htm");
		STATUS_HTM  = HtmCache.getInstance().getHtm(null, HTM_PATH + "status.htm");
	}

	private static StoneConfig parseStone(Element el)
	{
		final AttributeType element = AttributeType.findByName(el.getAttribute("element"));
		if (element == null) return null;
		return new StoneConfig(
			element,
			Integer.parseInt(el.getAttribute("itemId")),
			Integer.parseInt(el.getAttribute("minGain")),
			Integer.parseInt(el.getAttribute("maxGain")),
			Integer.parseInt(el.getAttribute("maxValue")),
			Double.parseDouble(el.getAttribute("successRate"))
		);
	}

	// ── NPC 事件 ──────────────────────────────────────────
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMain(player);
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "main":    showMain(player);              return null;
			case "armor":   showArmor(player, "head");     return null;
			case "weapon":  showWeapon(player, "weapon");  return null;
			case "jewelry": showJewelry(player, "neck");   return null;
			case "status":  showStatus(player);            return null;
		}

		if (event.startsWith("armor_slot_"))
		{
			showArmor(player, event.substring("armor_slot_".length()));
		}
		else if (event.startsWith("weapon_slot_"))
		{
			showWeapon(player, event.substring("weapon_slot_".length()));
		}
		else if (event.startsWith("jewelry_slot_"))
		{
			showJewelry(player, event.substring("jewelry_slot_".length()));
		}
		else if (event.startsWith("armor_enhance_"))
		{
			// armor_enhance_<slot>_<elemOrd> <count>
			final String[] parts = event.split(" ", 2);
			final String[] p = parts[0].split("_", 4);
			final int count = (parts.length >= 2 && !parts[1].trim().isEmpty()) ? Math.max(1, Math.min(safeParseInt(parts[1].trim()), 9999)) : 1;
			for (int i = 0; i < count; i++) doArmorEnhance(player, p[2], Integer.parseInt(p[3]));
			showArmor(player, p[2]);
		}
		else if (event.startsWith("armor_reset_"))
		{
			final String slot = event.substring("armor_reset_".length());
			doArmorReset(player, slot);
			showArmor(player, slot);
		}
		else if (event.startsWith("weapon_enhance_"))
		{
			// weapon_enhance_<slot>_<elemOrd> <count>
			final String[] parts = event.split(" ", 2);
			final String[] p = parts[0].split("_", 4);
			final int count = (parts.length >= 2 && !parts[1].trim().isEmpty()) ? Math.max(1, Math.min(safeParseInt(parts[1].trim()), 9999)) : 1;
			for (int i = 0; i < count; i++) doWeaponEnhance(player, p[2], Integer.parseInt(p[3]));
			showWeapon(player, p[2]);
		}
		else if (event.startsWith("jewelry_roll_"))
		{
			final String slot = event.substring("jewelry_roll_".length());
			doJewelryRoll(player, slot);
			showJewelry(player, slot);
		}
		else if (event.startsWith("weapon_reset_"))
		{
			final String slot = event.substring("weapon_reset_".length());
			doWeaponReset(player, slot);
			showWeapon(player, slot);
		}
		else if (event.startsWith("status_detail_"))
		{
			try
			{
				showStatusDetail(player, Integer.parseInt(event.substring("status_detail_".length())));
			}
			catch (NumberFormatException e)
			{
				showStatus(player);
			}
		}

		return null;
	}

	// ── 頁面：主選單 ──────────────────────────────────────
	private void showMain(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setHtml(MAIN_HTM);
		player.sendPacket(html);
	}

	// ── 頁面：防具 ────────────────────────────────────────
	private void showArmor(Player player, String curSlot)
	{
		final Item item = getEquipped(player, curSlot);
		final StringBuilder sb = new StringBuilder();

		// 部位分頁
		sb.append("<table width=270 cellpadding=2 cellspacing=0><tr>");
		for (String s : ARMOR_SLOTS)
		{
			if (s.equals(curSlot))
			{
				sb.append("<td align=center><font color=\"LEVEL\">").append(SLOT_ZH.get(s)).append("</font></td>");
			}
			else
			{
				sb.append("<td align=center><a action=\"bypass -h Quest AttributeEnhance armor_slot_").append(s).append("\">").append(SLOT_ZH.get(s)).append("</a></td>");
			}
		}
		sb.append("</tr></table><br>");

		// 當前屬性
		sb.append(buildCurrentAttrTable(item, curSlot, false));
		sb.append("<br>");

		// 強化按鈕（防具：只顯示已有屬性的強化選項）
		sb.append(buildArmorEnhanceButtons(player, item, ARMOR_STONES.get(curSlot), curSlot));

		// 清洗屬性按鈕
		sb.append("<table width=270 bgcolor=222233 cellpadding=3 cellspacing=0>");
		sb.append("<tr><td align=center><font color=LEVEL><b>清洗屬性</b></font></td></tr>");
		sb.append("</table>");
		sb.append("<table width=270 bgcolor=2A0A0A cellpadding=3 cellspacing=1>");
		sb.append("<tr><td align=center><font color=FFAAAA size=1>※ 清洗後將移除所有防禦屬性</font></td></tr>");
		sb.append("</table>");
		sb.append("<table width=270 cellpadding=3 cellspacing=0>");
		sb.append("<tr><td align=center>");
		sb.append("<button value=\"清洗屬性\" action=\"bypass -h Quest AttributeEnhance armor_reset_").append(curSlot).append("\"");
		sb.append(" width=120 height=24 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>");
		sb.append("</td></tr></table>");

		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setHtml(ARMOR_HTM);
		html.replace("%slot_zh%",   SLOT_ZH.get(curSlot));
		html.replace("%slot_tabs%", sb.toString());
		player.sendPacket(html);
	}

	// ── 頁面：武器 ────────────────────────────────────────
	private void showWeapon(Player player, String curSlot)
	{
		final Item item = getEquipped(player, curSlot);
		final StringBuilder sb = new StringBuilder();

		// 部位分頁
		sb.append("<table width=270 cellpadding=2 cellspacing=0><tr>");
		for (String s : WEAPON_SLOTS)
		{
			if (s.equals(curSlot))
			{
				sb.append("<td align=center><font color=\"LEVEL\">").append(SLOT_ZH.get(s)).append("</font></td>");
			}
			else
			{
				sb.append("<td align=center><a action=\"bypass -h Quest AttributeEnhance weapon_slot_").append(s).append("\">").append(SLOT_ZH.get(s)).append("</a></td>");
			}
		}
		sb.append("</tr></table><br>");

		// 當前屬性（武器只有一種）
		sb.append("<table width=270 bgcolor=222233 cellpadding=3 cellspacing=0>");
		sb.append("<tr><td align=center><font color=LEVEL><b>").append(SLOT_ZH.get(curSlot)).append(" 當前屬性</b></font></td></tr>");
		sb.append("</table>");
		sb.append("<table width=270 bgcolor=111111 cellpadding=3 cellspacing=1>");
		if (item == null)
		{
			sb.append("<tr><td align=center colspan=2><font color=FF4444>該部位未裝備</font></td></tr>");
		}
		else
		{
			sb.append("<tr bgcolor=1A1A2A><td width=60><font color=FFCC33>裝備名稱</font></td>")
			  .append("<td><font color=FFFFFF>").append(item.getTemplate().getName()).append("</font></td></tr>");
			final AttributeHolder atk = item.getAttackAttribute();
			if (atk != null && atk.getType() != AttributeType.NONE && atk.getValue() > 0)
			{
				final int ord = (int) atk.getType().getClientId();
				final int max = getMaxValue(WEAPON_STONES.get(curSlot), ord, 300);
				sb.append("<tr bgcolor=1A2A1A><td width=60><font color=").append(ELEMENT_COLOR[ord]).append(">")
				  .append(WEAPON_DESC[ord]).append("</font></td>")
				  .append("<td>").append(buildGauge(atk.getValue(), max)).append("</td></tr>");
			}
			else
			{
				sb.append("<tr><td align=center colspan=2><font color=888888>尚無攻擊屬性</font></td></tr>");
			}
		}
		sb.append("</table><br>");

		// 強化按鈕（武器：只顯示已有屬性）
		sb.append(buildWeaponEnhanceButtons(player, item, WEAPON_STONES.get(curSlot), curSlot));

		// 清洗屬性按鈕
		sb.append("<table width=270 bgcolor=222233 cellpadding=3 cellspacing=0>");
		sb.append("<tr><td align=center><font color=LEVEL><b>清洗屬性</b></font></td></tr>");
		sb.append("</table>");
		sb.append("<table width=270 bgcolor=2A0A0A cellpadding=3 cellspacing=1>");
		sb.append("<tr><td align=center><font color=FFAAAA size=1>※ 清洗後將移除所有攻擊屬性</font></td></tr>");
		sb.append("</table>");
		sb.append("<table width=270 cellpadding=3 cellspacing=0>");
		sb.append("<tr><td align=center>");
		sb.append("<button value=\"清洗屬性\" action=\"bypass -h Quest AttributeEnhance weapon_reset_").append(curSlot).append("\"");
		sb.append(" width=120 height=24 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>");
		sb.append("</td></tr></table>");

		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setHtml(WEAPON_HTM);
		html.replace("%slot_zh%",   SLOT_ZH.get(curSlot));
		html.replace("%slot_tabs%", sb.toString());
		player.sendPacket(html);
	}

	// ── 頁面：飾品 ────────────────────────────────────────
	private void showJewelry(Player player, String curSlot)
	{
		final Item item     = getEquipped(player, curSlot);
		final long hasStone = player.getInventory().getInventoryItemCount(JEWELRY_ITEM_ID, -1);
		final StringBuilder sb = new StringBuilder();

		// 部位分頁
		sb.append("<table width=270 cellpadding=2 cellspacing=0><tr>");
		for (String s : JEWELRY_SLOTS)
		{
			if (s.equals(curSlot))
			{
				sb.append("<td align=center><font color=\"LEVEL\">").append(SLOT_ZH.get(s)).append("</font></td>");
			}
			else
			{
				sb.append("<td align=center><a action=\"bypass -h Quest AttributeEnhance jewelry_slot_").append(s).append("\">").append(SLOT_ZH.get(s)).append("</a></td>");
			}
		}
		sb.append("</tr></table><br>");

		// 當前屬性
		sb.append(buildCurrentAttrTable(item, curSlot, true));
		sb.append("<br>");

		// 洗屬性區
		sb.append("<table width=270 bgcolor=222233 cellpadding=3 cellspacing=0>");
		sb.append("<tr><td align=center><font color=LEVEL><b>隨機洗屬性</b></font></td></tr>");
		sb.append("</table>");
		sb.append("<table width=270 bgcolor=111111 cellpadding=3 cellspacing=1>");
		sb.append("<tr bgcolor=1A1A2A><td width=120><font color=AAAAAA>消耗道具數量</font></td>")
		  .append("<td><font color=").append(hasStone > 0 ? "00FF88" : "FF4444").append(">")
		  .append(hasStone).append(" 個</font></td></tr>");
		sb.append("<tr bgcolor=1A2A1A><td><font color=AAAAAA>隨機屬性種數</font></td>")
		  .append("<td><font color=FFCC33>").append(JEWELRY_MIN_SLOTS).append(" ~ ").append(JEWELRY_MAX_SLOTS).append(" 種</font></td></tr>");
		sb.append("<tr bgcolor=1A1A2A><td><font color=AAAAAA>每種屬性值</font></td>")
		  .append("<td><font color=FFCC33>").append(JEWELRY_MIN_VALUE).append(" ~ ").append(JEWELRY_MAX_VALUE).append("</font></td></tr>");
		sb.append("</table>");
		sb.append("<table width=270 bgcolor=2A0A0A cellpadding=3 cellspacing=1>");
		sb.append("<tr><td align=center><font color=FFAAAA size=1>※ 洗屬性會覆蓋原有全部屬性</font></td></tr>");
		sb.append("</table>");
		sb.append("<table width=270 cellpadding=3 cellspacing=0>");
		sb.append("<tr><td align=center>")
		  .append("<button value=\"隨機洗屬性\" action=\"bypass -h Quest AttributeEnhance jewelry_roll_").append(curSlot).append("\"")
		  .append(" width=150 height=24 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>")
		  .append("</td></tr>");
		sb.append("</table>");

		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setHtml(JEWELRY_HTM);
		html.replace("%slot_zh%",   SLOT_ZH.get(curSlot));
		html.replace("%slot_tabs%", sb.toString());
		player.sendPacket(html);
	}

	// ── 頁面：屬性總覽 ────────────────────────────────────
	private void showStatus(Player player)
	{
		final StringBuilder sb = new StringBuilder();

		// 攻擊屬性
		sb.append("<table width=270 bgcolor=111111 cellpadding=3 cellspacing=1>");
		sb.append("<tr bgcolor=332200><td align=center colspan=3><font color=FFCC33><b>武器攻擊屬性</b></font></td></tr>");
		sb.append("<tr bgcolor=0A0A0A><td width=70><font color=AAAAAA>部位</font></td>")
		  .append("<td width=70><font color=AAAAAA>屬性</font></td>")
		  .append("<td><font color=AAAAAA>數值</font></td></tr>");

		for (String s : WEAPON_SLOTS)
		{
			final Item it = getEquipped(player, s);
			if (it == null) continue;
			final AttributeHolder atk = it.getAttackAttribute();
			if (atk == null || atk.getType() == AttributeType.NONE || atk.getValue() <= 0) continue;
			final int ord = (int) atk.getType().getClientId();
			sb.append("<tr bgcolor=1A1A0A>")
			  .append("<td><font color=FFCC33>").append(SLOT_ZH.get(s)).append("</font></td>")
			  .append("<td><font color=").append(ELEMENT_COLOR[ord]).append(">").append(WEAPON_DESC[ord]).append("</font></td>")
			  .append("<td><font color=FFFFFF>").append(atk.getValue()).append("</font></td></tr>");
		}
		sb.append("</table><br>");

		// 防禦屬性（防具 + 飾品）按元素加總
		sb.append("<table width=270 bgcolor=111111 cellpadding=3 cellspacing=1>");
		sb.append("<tr bgcolor=002233><td align=center colspan=3><font color=88CCFF><b>防具/飾品防禦屬性</b></font></td></tr>");
		sb.append("<tr bgcolor=0A0A0A><td width=90><font color=AAAAAA>屬性</font></td>")
		  .append("<td width=100><font color=AAAAAA>總數值</font></td>")
		  .append("<td width=80></td></tr>");

		final String[] defSlots = { "head", "chest", "legs", "gloves", "boots", "neck", "rear", "lear", "rfinger", "lfinger" };
		boolean anyDef = false;
		for (AttributeType elem : ALL_ELEMENTS)
		{
			final int ord = (int) elem.getClientId();
			int total = 0;
			for (String s : defSlots)
			{
				final Item it = getEquipped(player, s);
				if (it == null) continue;
				final AttributeHolder ah = it.getAttribute(elem);
				if (ah != null) total += ah.getValue();
			}
			if (total <= 0) continue;
			anyDef = true;
			sb.append("<tr bgcolor=0A1A2A>")
			  .append("<td><font color=").append(ELEMENT_COLOR[ord]).append(">").append(ARMOR_DESC[ord]).append("</font></td>")
			  .append("<td><font color=FFFFFF>").append(total).append("</font></td>")
			  .append("<td align=center><button value=\"詳情\" action=\"bypass -h Quest AttributeEnhance status_detail_").append(ord).append("\"")
			  .append(" width=55 height=20 back=\"L2UI_CT1.Button_DF_Small_Down\" fore=\"L2UI_CT1.Button_DF_Small\"/></td>")
			  .append("</tr>");
		}
		if (!anyDef)
		{
			sb.append("<tr><td align=center colspan=3><font color=888888>無防禦屬性</font></td></tr>");
		}
		sb.append("</table>");

		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setHtml(STATUS_HTM);
		html.replace("%status_table%", sb.toString());
		player.sendPacket(html);
	}

	// ── 頁面：防禦屬性詳情 ───────────────────────────────
	private void showStatusDetail(Player player, int elemOrd)
	{
		if (elemOrd < 0 || elemOrd >= ALL_ELEMENTS.length) return;

		final AttributeType elem = ALL_ELEMENTS[elemOrd];
		final String[] defSlots = { "head", "chest", "legs", "gloves", "boots", "neck", "rear", "lear", "rfinger", "lfinger" };

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body><title>防禦屬性詳情</title><center>");

		sb.append("<table width=270 bgcolor=002233 cellpadding=3 cellspacing=0>");
		sb.append("<tr><td align=center><font color=").append(ELEMENT_COLOR[elemOrd]).append("><b>")
		  .append(ARMOR_DESC[elemOrd]).append(" 詳情</b></font></td></tr>");
		sb.append("</table>");

		sb.append("<table width=270 bgcolor=111111 cellpadding=3 cellspacing=1>");
		sb.append("<tr bgcolor=0A0A0A>")
		  .append("<td width=90><font color=AAAAAA>部位</font></td>")
		  .append("<td><font color=AAAAAA>數值</font></td></tr>");

		int total = 0;
		for (String s : defSlots)
		{
			final Item it = getEquipped(player, s);
			if (it == null) continue;
			final AttributeHolder ah = it.getAttribute(elem);
			if (ah == null || ah.getValue() <= 0) continue;
			total += ah.getValue();
			sb.append("<tr bgcolor=0A1A2A>")
			  .append("<td><font color=FFCC33>").append(SLOT_ZH.get(s)).append("</font></td>")
			  .append("<td><font color=FFFFFF>").append(ah.getValue()).append("</font></td></tr>");
		}

		sb.append("<tr bgcolor=002233>")
		  .append("<td><font color=88CCFF><b>合計</b></font></td>")
		  .append("<td><font color=FFFFFF><b>").append(total).append("</b></font></td></tr>");
		sb.append("</table><br>");

		sb.append("<button value=\"返回總覽\" action=\"bypass -h Quest AttributeEnhance status\"");
		sb.append(" width=100 height=24 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>");
		sb.append("</center></body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	// ── 強化邏輯：防具 ────────────────────────────────────
	private void doArmorEnhance(Player player, String slot, int elemOrd)
	{
		final Map<Integer, StoneConfig> stones = ARMOR_STONES.get(slot);
		if (stones == null) return;
		final StoneConfig cfg = stones.get(elemOrd);
		if (cfg == null) return;

		final Item item = getEquipped(player, slot);
		if (item == null) { player.sendMessage("該部位未裝備任何道具。"); return; }

		if (player.getInventory().getInventoryItemCount(cfg.itemId, -1) < 1)
		{
			player.sendMessage("材料不足！需要屬性石 x1。");
			return;
		}

		// 防具最多三種屬性
		int attrCount = 0;
		boolean alreadyHas = false;
		for (AttributeType elem : ALL_ELEMENTS)
		{
			final AttributeHolder ah = item.getAttribute(elem);
			if (ah != null && ah.getValue() > 0)
			{
				attrCount++;
				if ((int) elem.getClientId() == elemOrd) alreadyHas = true;
			}
		}
		if (!alreadyHas && attrCount >= 3)
		{
			player.sendMessage("防具最多只能有三種屬性！");
			return;
		}

		final AttributeHolder cur    = item.getAttribute(ALL_ELEMENTS[elemOrd]);
		final int             curVal = (cur != null) ? cur.getValue() : 0;
		if (curVal >= cfg.maxValue)
		{
			player.sendMessage(ARMOR_DESC[elemOrd] + " 已達上限 " + cfg.maxValue + "！");
			return;
		}

		// 消耗道具
		player.destroyItemByItemId(ItemProcessType.NONE, cfg.itemId, 1, player, true);

		if (Rnd.nextDouble() >= cfg.successRate)
		{
			player.sendMessage("強化失敗！");
			return;
		}

		final int newVal = Math.min(curVal + Rnd.get(cfg.minGain, cfg.maxGain), cfg.maxValue);
		applyAttribute(player, slot, ALL_ELEMENTS[elemOrd], newVal);
		player.sendMessage("強化成功！" + SLOT_ZH.get(slot) + " " + ARMOR_DESC[elemOrd] + " " + curVal + " → " + newVal);
	}

	// ── 強化邏輯：武器 ────────────────────────────────────
	private void doWeaponEnhance(Player player, String slot, int elemOrd)
	{
		final Map<Integer, StoneConfig> stones = WEAPON_STONES.get(slot);
		if (stones == null) return;
		final StoneConfig cfg = stones.get(elemOrd);
		if (cfg == null) return;

		final Item item = getEquipped(player, slot);
		if (item == null) { player.sendMessage("該部位未裝備任何道具。"); return; }

		if (player.getInventory().getInventoryItemCount(cfg.itemId, -1) < 1)
		{
			player.sendMessage("材料不足！需要屬性石 x1。");
			return;
		}

		final AttributeHolder atk    = item.getAttackAttribute();
		final int             curVal = (atk != null && (int) atk.getType().getClientId() == elemOrd) ? atk.getValue() : 0;

		if (curVal >= cfg.maxValue)
		{
			player.sendMessage(WEAPON_DESC[elemOrd] + " 已達上限 " + cfg.maxValue + "！");
			return;
		}

		// 消耗道具
		player.destroyItemByItemId(ItemProcessType.NONE, cfg.itemId, 1, player, true);

		if (Rnd.nextDouble() >= cfg.successRate)
		{
			player.sendMessage("強化失敗！");
			return;
		}

		final int newVal = Math.min(curVal + Rnd.get(cfg.minGain, cfg.maxGain), cfg.maxValue);
		applyAttribute(player, slot, ALL_ELEMENTS[elemOrd], newVal);
		player.sendMessage("強化成功！" + SLOT_ZH.get(slot) + " " + WEAPON_DESC[elemOrd] + " " + curVal + " → " + newVal);
	}

	// ── 強化邏輯：飾品隨機洗 ─────────────────────────────
	private void doJewelryRoll(Player player, String slot)
	{
		final Item item = getEquipped(player, slot);
		if (item == null) { player.sendMessage("該部位未裝備任何道具。"); return; }

		if (player.getInventory().getInventoryItemCount(JEWELRY_ITEM_ID, -1) < 1)
		{
			player.sendMessage("需要隨機屬性石 x1。");
			return;
		}

		player.destroyItemByItemId(ItemProcessType.NONE, JEWELRY_ITEM_ID, 1, player, true);

		// 隨機挑選屬性
		final int slotCount = Rnd.get(JEWELRY_MIN_SLOTS, JEWELRY_MAX_SLOTS);
		final List<Integer> pool = new ArrayList<>();
		for (int i = 0; i < 6; i++) pool.add(i);
		Collections.shuffle(pool);

		// 清除舊屬性並套上新屬性
		final int paperdoll = SLOT_MAP.get(slot);
		player.getInventory().unEquipItemInSlot(paperdoll);
		item.clearAllAttributes();

		final StringBuilder result = new StringBuilder(SLOT_ZH.get(slot) + " 洗出：");
		for (int i = 0; i < slotCount; i++)
		{
			final int ord = pool.get(i);
			final int val = Rnd.get(JEWELRY_MIN_VALUE, JEWELRY_MAX_VALUE);
			item.setAttribute(new AttributeHolder(ALL_ELEMENTS[ord], val), true);
			if (i > 0) result.append("、");
			result.append(ARMOR_DESC[ord]).append(val);
		}

		player.getInventory().equipItem(item);
		sendInventoryUpdate(player, item);
		player.sendMessage(result.toString());
	}

	// ── 清洗邏輯：防具屬性重置 ───────────────────────────
	private void doArmorReset(Player player, String slot)
	{
		final Item item = getEquipped(player, slot);
		if (item == null) { player.sendMessage("該部位未裝備任何道具。"); return; }

		// 檢查是否有任何防禦屬性
		boolean hasDefAttr = false;
		for (AttributeType elem : ALL_ELEMENTS)
		{
			final AttributeHolder ah = item.getAttribute(elem);
			if (ah != null && ah.getValue() > 0)
			{
				hasDefAttr = true;
				break;
			}
		}

		if (!hasDefAttr)
		{
			player.sendMessage("該防具沒有防禦屬性，無需清洗。");
			return;
		}

		// 清除所有防禦屬性
		final int paperdoll = SLOT_MAP.get(slot);
		player.getInventory().unEquipItemInSlot(paperdoll);
		item.clearAllAttributes();
		player.getInventory().equipItem(item);
		sendInventoryUpdate(player, item);
		player.sendMessage(SLOT_ZH.get(slot) + " 的防禦屬性已清洗完成。");
	}

	// ── 清洗邏輯：武器屬性重置 ───────────────────────────
	private void doWeaponReset(Player player, String slot)
	{
		final Item item = getEquipped(player, slot);
		if (item == null) { player.sendMessage("該部位未裝備任何道具。"); return; }

		final AttributeHolder atk = item.getAttackAttribute();
		if (atk == null || atk.getType() == AttributeType.NONE || atk.getValue() <= 0)
		{
			player.sendMessage("該武器沒有攻擊屬性，無需清洗。");
			return;
		}

		// 清除攻擊屬性
		final int paperdoll = SLOT_MAP.get(slot);
		player.getInventory().unEquipItemInSlot(paperdoll);
		item.clearAttackAttribute();
		player.getInventory().equipItem(item);
		sendInventoryUpdate(player, item);
		player.sendMessage(SLOT_ZH.get(slot) + " 的攻擊屬性已清洗完成。");
	}

	// ── 工具方法 ──────────────────────────────────────────
	private Item getEquipped(Player player, String slot)
	{
		final Integer paperdoll = SLOT_MAP.get(slot);
		return paperdoll == null ? null : player.getInventory().getPaperdollItem(paperdoll);
	}

	private void applyAttribute(Player player, String slot, AttributeType elem, int value)
	{
		final int paperdoll = SLOT_MAP.get(slot);
		final Item item     = player.getInventory().getPaperdollItem(paperdoll);
		if (item == null) return;
		player.getInventory().unEquipItemInSlot(paperdoll);
		item.setAttribute(new AttributeHolder(elem, value), true);
		player.getInventory().equipItem(item);
		sendInventoryUpdate(player, item);
	}

	private void sendInventoryUpdate(Player player, Item item)
	{
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(item);
		player.sendInventoryUpdate(iu);
	}

	/** 當前屬性表格（防具/飾品共用） */
	private String buildCurrentAttrTable(Item item, String slot, boolean isJewelry)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<table width=270 bgcolor=222233 cellpadding=3 cellspacing=0>");
		sb.append("<tr><td align=center><font color=LEVEL><b>")
		  .append(SLOT_ZH.get(slot)).append(" 當前屬性</b></font></td></tr>");
		sb.append("</table>");
		sb.append("<table width=270 bgcolor=111111 cellpadding=3 cellspacing=1>");
		if (item == null)
		{
			sb.append("<tr><td align=center colspan=2><font color=FF4444>該部位未裝備</font></td></tr>");
		}
		else
		{
			sb.append("<tr bgcolor=1A1A2A><td width=60><font color=FFCC33>裝備名稱</font></td>")
			  .append("<td><font color=FFFFFF>").append(item.getTemplate().getName()).append("</font></td></tr>");
			boolean any = false;
			for (AttributeType elem : ALL_ELEMENTS)
			{
				final AttributeHolder ah = item.getAttribute(elem);
				if (ah == null || ah.getValue() <= 0) continue;
				final int ord = (int) elem.getClientId();
				final int max = isJewelry ? JEWELRY_MAX_VALUE : getMaxValue(ARMOR_STONES.get(slot), ord, 200);
				sb.append("<tr bgcolor=1A2A1A><td width=60><font color=").append(ELEMENT_COLOR[ord]).append(">")
				  .append(ARMOR_DESC[ord]).append("</font></td>")
				  .append("<td>").append(buildGauge(ah.getValue(), max)).append("</td></tr>");
				any = true;
			}
			if (!any) sb.append("<tr><td align=center colspan=2><font color=888888>尚無屬性</font></td></tr>");
		}
		sb.append("</table>");
		return sb.toString();
	}

	/** 強化按鈕表格（防具專用：只顯示已有屬性） */
	private String buildArmorEnhanceButtons(Player player, Item item, Map<Integer, StoneConfig> stones, String slot)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<table width=270 bgcolor=222233 cellpadding=3 cellspacing=0>");
		sb.append("<tr><td align=center><font color=LEVEL><b>選擇要強化的屬性</b></font></td></tr>");
		sb.append("</table>");

		if (item == null)
		{
			sb.append("<table width=270 bgcolor=111111 cellpadding=3 cellspacing=1>");
			sb.append("<tr><td align=center><font color=888888>請先裝備防具</font></td></tr>");
			sb.append("</table>");
			return sb.toString();
		}

		// 收集已有屬性
		final java.util.List<Integer> hasAttrs = new java.util.ArrayList<>();
		for (AttributeType elem : ALL_ELEMENTS)
		{
			final AttributeHolder ah = item.getAttribute(elem);
			if (ah != null && ah.getValue() > 0)
			{
				hasAttrs.add((int) elem.getClientId());
			}
		}

		// 如果已有 3 種屬性，只顯示這 3 種
		// 如果少於 3 種，顯示所有 6 種（讓玩家可以選擇新屬性）
		final java.util.List<Integer> displayAttrs = new java.util.ArrayList<>();
		if (hasAttrs.size() >= 3)
		{
			displayAttrs.addAll(hasAttrs);
		}
		else
		{
			for (int i = 0; i < 6; i++) displayAttrs.add(i);
		}

		sb.append("<table width=270 bgcolor=111111 cellpadding=3 cellspacing=1>");
		sb.append("<tr bgcolor=0A0A0A>")
		  .append("<td width=70><font color=AAAAAA>屬性</font></td>")
		  .append("<td width=55><font color=AAAAAA>擁有</font></td>")
		  .append("<td width=50><font color=AAAAAA>成功率</font></td>")
		  .append("<td width=60><font color=AAAAAA>次數</font></td>")
		  .append("<td width=35></td></tr>");

		int rowIndex = 0;
		for (int ord : displayAttrs)
		{
			final StoneConfig cfg = (stones != null) ? stones.get(ord) : null;
			if (cfg == null) continue;
			final long    has      = player.getInventory().getInventoryItemCount(cfg.itemId, -1);
			final String  rowColor = (rowIndex % 2 == 0) ? "1A1A2A" : "1A2A1A";
			sb.append("<tr bgcolor=").append(rowColor).append(">")
			  .append("<td><font color=").append(ELEMENT_COLOR[ord]).append(">").append(ARMOR_DESC[ord]).append("</font></td>")
			  .append("<td align=center><font color=").append(has > 0 ? "00FF88" : "FF4444").append(">").append(has).append("個</font></td>")
			  .append("<td align=center><font color=FFFF00>").append((int) (cfg.successRate * 100)).append("%</font></td>")
			  .append("<td align=center><edit var=\"acnt_").append(slot).append("_").append(ord).append("\" width=55 height=15></td>")
			  .append("<td align=center><button value=\"強化\" action=\"bypass -h Quest AttributeEnhance armor_enhance_")
			  .append(slot).append("_").append(ord).append(" $acnt_").append(slot).append("_").append(ord).append("\"")
			  .append(" width=32 height=20 back=\"L2UI_CT1.Button_DF_Small_Down\" fore=\"L2UI_CT1.Button_DF_Small\"/></td>")
			  .append("</tr>");
			rowIndex++;
		}
		sb.append("</table>");
		return sb.toString();
	}

	/** 強化按鈕表格（武器專用：只顯示已有屬性） */
	private String buildWeaponEnhanceButtons(Player player, Item item, Map<Integer, StoneConfig> stones, String slot)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<table width=270 bgcolor=222233 cellpadding=3 cellspacing=0>");
		sb.append("<tr><td align=center><font color=LEVEL><b>選擇要強化的屬性</b></font></td></tr>");
		sb.append("</table>");

		if (item == null)
		{
			sb.append("<table width=270 bgcolor=111111 cellpadding=3 cellspacing=1>");
			sb.append("<tr><td align=center><font color=888888>請先裝備武器</font></td></tr>");
			sb.append("</table>");
			return sb.toString();
		}

		// 檢查武器是否已有攻擊屬性
		final AttributeHolder atk = item.getAttackAttribute();
		final boolean hasAttr = (atk != null && atk.getType() != AttributeType.NONE && atk.getValue() > 0);

		// 如果已有屬性，只顯示該屬性；否則顯示全部 6 種
		final java.util.List<Integer> displayAttrs = new java.util.ArrayList<>();
		if (hasAttr)
		{
			displayAttrs.add((int) atk.getType().getClientId());
		}
		else
		{
			for (int i = 0; i < 6; i++) displayAttrs.add(i);
		}

		sb.append("<table width=270 bgcolor=111111 cellpadding=3 cellspacing=1>");
		sb.append("<tr bgcolor=0A0A0A>")
		  .append("<td width=70><font color=AAAAAA>屬性</font></td>")
		  .append("<td width=55><font color=AAAAAA>擁有</font></td>")
		  .append("<td width=50><font color=AAAAAA>成功率</font></td>")
		  .append("<td width=60><font color=AAAAAA>次數</font></td>")
		  .append("<td width=35></td></tr>");

		int rowIndex = 0;
		for (int ord : displayAttrs)
		{
			final StoneConfig cfg = (stones != null) ? stones.get(ord) : null;
			if (cfg == null) continue;
			final long    has      = player.getInventory().getInventoryItemCount(cfg.itemId, -1);
			final String  rowColor = (rowIndex % 2 == 0) ? "1A1A2A" : "1A2A1A";
			sb.append("<tr bgcolor=").append(rowColor).append(">")
			  .append("<td><font color=").append(ELEMENT_COLOR[ord]).append(">").append(WEAPON_DESC[ord]).append("</font></td>")
			  .append("<td align=center><font color=").append(has > 0 ? "00FF88" : "FF4444").append(">").append(has).append("個</font></td>")
			  .append("<td align=center><font color=FFFF00>").append((int) (cfg.successRate * 100)).append("%</font></td>")
			  .append("<td align=center><edit var=\"wcnt_").append(slot).append("_").append(ord).append("\" width=55 height=15></td>")
			  .append("<td align=center><button value=\"強化\" action=\"bypass -h Quest AttributeEnhance weapon_enhance_")
			  .append(slot).append("_").append(ord).append(" $wcnt_").append(slot).append("_").append(ord).append("\"")
			  .append(" width=32 height=20 back=\"L2UI_CT1.Button_DF_Small_Down\" fore=\"L2UI_CT1.Button_DF_Small\"/></td>")
			  .append("</tr>");
			rowIndex++;
		}
		sb.append("</table>");
		return sb.toString();
	}

	/** 強化按鈕表格（防具與武器共用） */
	private String buildEnhanceButtons(Player player, Map<Integer, StoneConfig> stones, String slot, String actionPrefix, String[] descs)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<table width=270 bgcolor=222233 cellpadding=3 cellspacing=0>");
		sb.append("<tr><td align=center><font color=LEVEL><b>選擇要強化的屬性</b></font></td></tr>");
		sb.append("</table>");
		sb.append("<table width=270 bgcolor=111111 cellpadding=3 cellspacing=1>");
		sb.append("<tr bgcolor=0A0A0A>")
		  .append("<td width=70><font color=AAAAAA>屬性</font></td>")
		  .append("<td width=55><font color=AAAAAA>擁有</font></td>")
		  .append("<td width=50><font color=AAAAAA>成功率</font></td>")
		  .append("<td width=60><font color=AAAAAA>次數</font></td>")
		  .append("<td width=35></td></tr>");

		for (int i = 0; i < 6; i++)
		{
			final StoneConfig cfg = (stones != null) ? stones.get(i) : null;
			if (cfg == null) continue;
			final long    has      = player.getInventory().getInventoryItemCount(cfg.itemId, -1);
			final String  rowColor = (i % 2 == 0) ? "1A1A2A" : "1A2A1A";
			final String  varName  = actionPrefix + "_" + slot + "_" + i;
			sb.append("<tr bgcolor=").append(rowColor).append(">")
			  .append("<td><font color=").append(ELEMENT_COLOR[i]).append(">").append(descs[i]).append("</font></td>")
			  .append("<td align=center><font color=").append(has > 0 ? "00FF88" : "FF4444").append(">").append(has).append("個</font></td>")
			  .append("<td align=center><font color=FFFF00>").append((int) (cfg.successRate * 100)).append("%</font></td>")
			  .append("<td align=center><edit var=\"").append(varName).append("\" width=55 height=15></td>")
			  .append("<td align=center><button value=\"強化\" action=\"bypass -h Quest AttributeEnhance ")
			  .append(actionPrefix).append("_").append(slot).append("_").append(i).append(" $").append(varName).append("\"")
			  .append(" width=32 height=20 back=\"L2UI_CT1.Button_DF_Small_Down\" fore=\"L2UI_CT1.Button_DF_Small\"/></td>")
			  .append("</tr>");
		}
		sb.append("</table>");
		return sb.toString();
	}

	/** 屬性進度條 */
	private String buildGauge(int current, int max)
	{
		return "<table cellpadding=0 cellspacing=0><tr>"
			+ "<td>" + HtmlUtil.getMpGauge(130, current, max, false) + "</td>"
			+ "<td width=6></td>"
			+ "<td width=70><font color=FFFFFF>" + current + "/" + max + "</font></td>"
			+ "</tr></table>";
	}

	/** 安全解析整數，失敗回傳 1 */
	private static int safeParseInt(String s)
	{
		try { return Integer.parseInt(s.trim()); }
		catch (NumberFormatException e) { return 1; }
	}

	/** 從設定取最大值，若無設定用預設值 */
	private int getMaxValue(Map<Integer, StoneConfig> stones, int elemOrd, int defaultMax)
	{
		if (stones == null) return defaultMax;
		final StoneConfig c = stones.get(elemOrd);
		return c == null ? defaultMax : c.maxValue;
	}

	public static void main(String[] args)
	{
		new AttributeEnhance();
	}
}
