package custom.RefineSystem;

import java.util.LinkedHashMap;
import java.util.Map;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.xml.RefineSystemData;
import org.l2jmobius.gameserver.managers.RefineSystemManager;
import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;


/**
 * RefineSystem — 裝備精煉 NPC（ID 900052）
 */
public class RefineSystem extends Script
{
	private static final int NPC_ID   = 900052;
	private static final String HTM_PATH = "data/scripts/custom/RefineSystem/";

	// 部位名稱 → Paperdoll 槽位
	private static final Map<String, Integer> SLOT_MAP = new LinkedHashMap<>();
	// 部位名稱 → 中文
	private static final Map<String, String>  SLOT_ZH  = new LinkedHashMap<>();

	// 防具部位
	private static final String[] ARMOR_SLOTS   = { "head", "chest", "legs", "gloves", "boots" };
	// 武器部位
	private static final String[] WEAPON_SLOTS  = { "weapon" };
	// 飾品部位
	private static final String[] JEWELRY_SLOTS = { "neck", "rear", "lear", "rfinger", "lfinger" };

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

	private RefineSystem()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "main.htm";
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("main".equals(event))
		{
			return "main.htm";
		}

		if (event.startsWith("weapon_slot_"))
		{
			showWeapon(npc, player, event.substring(12));
			return null;
		}
		if (event.startsWith("armor_slot_"))
		{
			showArmor(npc, player, event.substring(11));
			return null;
		}
		if (event.startsWith("jewelry_slot_"))
		{
			showJewelry(npc, player, event.substring(13));
			return null;
		}
		if (event.startsWith("do_refine_"))
		{
			doRefine(npc, player, event.substring(10));
			return null;
		}

		if ("show_summary".equals(event))
		{
			showSummary(npc, player);
			return null;
		}

		if ("show_info".equals(event))
		{
			showRefineInfo(npc, player);
			return null;
		}

		return null;
	}

	// ── 頁面顯示 ─────────────────────────────────────────────────────────────

	private void showWeapon(Npc npc, Player player, String curSlot)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(player, HTM_PATH + "weapon.htm"));
		html.replace("%slot_tabs%", buildSlotContent(player, WEAPON_SLOTS, curSlot, "weapon_slot_"));
		player.sendPacket(html);
	}

	private void showArmor(Npc npc, Player player, String curSlot)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(player, HTM_PATH + "armor.htm"));
		html.replace("%slot_tabs%", buildSlotContent(player, ARMOR_SLOTS, curSlot, "armor_slot_"));
		player.sendPacket(html);
	}

	private void showJewelry(Npc npc, Player player, String curSlot)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(player, HTM_PATH + "jewelry.htm"));
		html.replace("%slot_tabs%", buildSlotContent(player, JEWELRY_SLOTS, curSlot, "jewelry_slot_"));
		player.sendPacket(html);
	}

	private void showSummary(Npc npc, Player player)
	{
		final RefineSystemData data = RefineSystemData.getInstance();

		final Map<String, Integer> sumMap = new LinkedHashMap<>();
		final Map<String, RefineSystemData.ValueType> typeMap = new LinkedHashMap<>();

		final int[] allSlots = {
			Inventory.PAPERDOLL_RHAND,
			Inventory.PAPERDOLL_HEAD, Inventory.PAPERDOLL_CHEST,
			Inventory.PAPERDOLL_LEGS, Inventory.PAPERDOLL_GLOVES, Inventory.PAPERDOLL_FEET,
			Inventory.PAPERDOLL_NECK,
			Inventory.PAPERDOLL_REAR, Inventory.PAPERDOLL_LEAR,
			Inventory.PAPERDOLL_RFINGER, Inventory.PAPERDOLL_LFINGER
		};

		for (int slot : allSlots)
		{
			final Item item = player.getInventory().getPaperdollItem(slot);
			if (item == null) continue;
			final VariationInstance aug = item.getAugmentation();
			if (aug == null) continue;
			final int[] opts = { aug.getOption1Id(), aug.getOption2Id(), aug.getOption3Id(), aug.getOption4Id() };
			for (int optionId : opts)
			{
				if (optionId <= 0) continue;
				final int base = (optionId / 10000) * 10000;
				final RefineSystemData.SeriesEntry se = data.getSeriesByBase(base);
				if (se == null) continue;
				sumMap.merge(se.name, optionId % 10000, Integer::sum);
				typeMap.putIfAbsent(se.name, se.valueType);
			}
		}

		final StringBuilder rows = new StringBuilder();
		if (sumMap.isEmpty())
		{
			rows.append("<table width=270 border=0><tr><td align=center height=30>");
			rows.append("<font color=\"888888\">目前全身無任何精煉詞條</font>");
			rows.append("</td></tr></table>");
		}
		else
		{
			for (Map.Entry<String, Integer> entry : sumMap.entrySet())
			{
				final String name = entry.getKey();
				final int totalRaw = entry.getValue();
				final RefineSystemData.ValueType vt = typeMap.get(name);
				final String display = vt == RefineSystemData.ValueType.FLAT
					? "+" + totalRaw
					: String.format("+%.2f%%", totalRaw / 100.0);
				rows.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr>");
				rows.append("<td width=4></td>");
				rows.append("<td width=150><font color=\"FFE066\">").append(name).append("</font></td>");
				rows.append("<td width=100><font color=\"88FF88\">").append(display).append("</font></td>");
				rows.append("<td width=4></td>");
				rows.append("</tr></table>");
				rows.append("<table border=0 width=270><tr><td height=2></td></tr></table>");
			}
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(player, HTM_PATH + "summary.htm"));
		html.replace("%summary_rows%", rows.toString());
		player.sendPacket(html);
	}

	private void showRefineInfo(Npc npc, Player player)
	{
		final RefineSystemData data = RefineSystemData.getInstance();

		final StringBuilder rows = new StringBuilder();
		for (RefineSystemData.SeriesEntry se : data.getAllSeries())
		{
			final int minRaw = se.ranges.get(0).from;
			final int maxRaw = se.ranges.get(se.ranges.size() - 1).to;
			final String minDisplay;
			final String maxDisplay;
			if (se.valueType == RefineSystemData.ValueType.FLAT)
			{
				minDisplay = String.valueOf(minRaw);
				maxDisplay = String.valueOf(maxRaw);
			}
			else
			{
				minDisplay = String.format("%.2f%%", minRaw / 100.0);
				maxDisplay = String.format("%.2f%%", maxRaw / 100.0);
			}
			rows.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr>");
			rows.append("<td width=4></td>");
			rows.append("<td width=140><font color=\"FFE066\">").append(se.name).append("</font></td>");
			rows.append("<td width=120><font color=\"88FF88\">+").append(minDisplay).append(" ~ +").append(maxDisplay).append("</font></td>");
			rows.append("<td width=4></td>");
			rows.append("</tr></table>");
			rows.append("<table border=0 width=270><tr><td height=2></td></tr></table>");
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(player, HTM_PATH + "info.htm"));
		html.replace("%info_rows%", rows.toString());
		player.sendPacket(html);
	}

	/**
	 * 建立部位分頁列 + 當前部位的精煉資訊 + 精煉按鈕
	 */
	private String buildSlotContent(Player player, String[] slots, String curSlot, String prefix)
	{
		final StringBuilder sb = new StringBuilder();

		// ── 分頁標籤列 ──
		sb.append("<table width=270 border=0 cellpadding=0 cellspacing=2><tr>");
		for (String s : slots)
		{
			if (s.equals(curSlot))
			{
				sb.append("<td align=center bgcolor=CC7700 height=22><font color=\"FFFFFF\"><b>").append(SLOT_ZH.get(s)).append("</b></font></td>");
			}
			else
			{
				sb.append("<td align=center bgcolor=333333 height=22><a action=\"bypass -h Quest RefineSystem ").append(prefix).append(s).append("\"><font color=\"CCCCCC\">").append(SLOT_ZH.get(s)).append("</font></a></td>");
			}
		}
		sb.append("</tr></table>");

		sb.append("<table border=0 width=270><tr><td height=4></td></tr></table>");

		// ── 當前部位資訊 ──
		final Item item = getEquipped(player, curSlot);
		if (item == null)
		{
			sb.append("<table width=270 border=0><tr><td align=center height=30>");
			sb.append("<font color=\"FF6060\">").append(SLOT_ZH.get(curSlot)).append(" 尚未裝備</font>");
			sb.append("</td></tr></table>");
			return sb.toString();
		}

		final int charges = RefineSystemManager.getInstance().getCharges(item);
		final int maxCharges = getMaxCharges(item);

		// 裝備名稱
		sb.append("<table width=270 border=0><tr>");
		sb.append("<td><font color=\"FFCC44\"><b>").append(item.getName()).append("</b></font></td>");
		sb.append("<td width=80 align=right><font color=\"").append(charges > 0 ? "AAAAAA" : "FF6060").append("\">").append(charges).append("/").append(maxCharges).append(" 次</font></td>");
		sb.append("</tr></table>");

		sb.append("<table border=0 width=270><tr><td height=3></td></tr></table>");

		// 分隔線
		sb.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr><td bgcolor=CC7700 height=1></td></tr></table>");

		sb.append("<table border=0 width=270><tr><td height=3></td></tr></table>");

		// 詞條標題
		sb.append("<table width=270 border=0><tr><td align=center>");
		sb.append("<font color=\"CC7700\">── 當前精煉詞條 ──</font>");
		sb.append("</td></tr></table>");

		sb.append("<table border=0 width=270><tr><td height=3></td></tr></table>");

		// 詞條列表
		final VariationInstance aug = item.getAugmentation();
		final int[] opts = aug != null
			? new int[]{ aug.getOption1Id(), aug.getOption2Id(), aug.getOption3Id(), aug.getOption4Id() }
			: new int[]{ 0, 0, 0, 0 };
		final String[] labels = { "①", "②", "③", "④" };
		for (int i = 0; i < 4; i++)
		{
			sb.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr>");
			sb.append("<td width=22 align=center><font color=\"00CCCC\">").append(labels[i]).append("</font></td>");
			sb.append("<td width=4></td>");
			if (opts[i] > 0)
			{
				sb.append("<td width=140><font color=\"FFE066\">").append(RefineSystemData.getInstance().getSeriesName(opts[i])).append("</font></td>");
				sb.append("<td align=right><font color=\"88FF88\">+").append(RefineSystemData.getInstance().getValueDisplay(opts[i])).append("</font></td>");
			}
			else
			{
				sb.append("<td colspan=2><font color=\"666666\">—</font></td>");
			}
			sb.append("</tr></table>");
		}

		sb.append("<table border=0 width=270><tr><td height=4></td></tr></table>");

		// 分隔線
		sb.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr><td bgcolor=CC7700 height=1></td></tr></table>");

		sb.append("<table border=0 width=270><tr><td height=5></td></tr></table>");

		// 精煉按鈕
		if (charges > 0)
		{
			sb.append("<table width=270 border=0><tr><td align=center>");
			sb.append("<font color=\"888888\" size=\"1\">消耗 ").append(RefineSystemData.getInstance().getRefineItemCount()).append(" 個精煉石</font>");
			sb.append("</td></tr></table>");
			sb.append("<table width=270 border=0><tr><td align=center>");
			sb.append("<button value=\"精煉 ").append(SLOT_ZH.get(curSlot)).append("\" action=\"bypass -h Quest RefineSystem do_refine_").append(curSlot).append("\"");
			sb.append(" width=200 height=31 back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
			sb.append("</td></tr></table>");
		}
		else
		{
			sb.append("<table width=270 border=0><tr><td align=center height=28>");
			sb.append("<font color=\"FF6060\">此裝備精煉次數已耗盡</font>");
			sb.append("</td></tr></table>");
		}

		return sb.toString();
	}

	// ── 精煉執行 ─────────────────────────────────────────────────────────────

	private void doRefine(Npc npc, Player player, String slot)
	{
		final Item item = getEquipped(player, slot);
		if (item == null)
		{
			sendMsg(npc, player, "<font color=\"FF0000\">" + SLOT_ZH.get(slot) + " 尚未裝備，無法精煉。</font>", slot);
			return;
		}

		// 檢查次數
		final int charges = RefineSystemManager.getInstance().getCharges(item);
		if (charges <= 0)
		{
			sendMsg(npc, player, "<font color=\"FF0000\">此裝備精煉次數已耗盡，無法再次精煉。</font>", slot);
			return;
		}

		// 檢查道具
		final long itemCount = player.getInventory().getInventoryItemCount(RefineSystemData.getInstance().getRefineItemId(), -1);
		if (itemCount < RefineSystemData.getInstance().getRefineItemCount())
		{
			sendMsg(npc, player, "<font color=\"FF0000\">精煉石不足，需要 " + RefineSystemData.getInstance().getRefineItemCount() + " 個。</font>", slot);
			return;
		}

		// 隨機生成詞條：第1條一定有，第2/3/4條依機率決定
		final int tierCount = RefineSystemData.getInstance().rollTierCount();
		final int op1 = RefineSystemData.getInstance().rollRefineId();
		final int op2 = tierCount >= 2 ? RefineSystemData.getInstance().rollRefineId() : 0;
		final int op3 = tierCount >= 3 ? RefineSystemData.getInstance().rollRefineId() : 0;
		final int op4 = tierCount >= 4 ? RefineSystemData.getInstance().rollRefineId() : 0;

		// 套用精煉（存入 item_variations，繞過 OptionData）
		item.setAugmentation(VariationInstance.ofRaw(RefineSystemData.getInstance().getRefineItemId(), op1, op2, op3, op4), true);

		// 扣除道具
		player.destroyItemByItemId(ItemProcessType.NONE, RefineSystemData.getInstance().getRefineItemId(), RefineSystemData.getInstance().getRefineItemCount(), player, true);

		// 扣除次數
		RefineSystemManager.getInstance().consumeCharge(item);

		// 更新背包封包
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(item);
		player.sendInventoryUpdate(iu);

		// 重新顯示頁面
		refreshSlotPage(npc, player, slot);
	}

	private void refreshSlotPage(Npc npc, Player player, String slot)
	{
		if (isArmorSlot(slot))
		{
			showArmor(npc, player, slot);
		}
		else if (isJewelrySlot(slot))
		{
			showJewelry(npc, player, slot);
		}
		else
		{
			showWeapon(npc, player, slot);
		}
	}

	private void sendMsg(Npc npc, Player player, String msg, String slot)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><head><title>裝備精煉</title></head><body scroll=\"no\">");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=292 height=358>");
		sb.append("<tr><td valign=\"top\" align=\"center\">");
		sb.append("<table border=0 width=270><tr><td height=10></td></tr></table>");
		sb.append("<table width=270 border=0><tr><td align=center>");
		sb.append("<font color=\"CC7700\" size=\"4\"><b>裝備精煉</b></font>");
		sb.append("</td></tr></table>");
		sb.append("<table border=0 width=270><tr><td height=4></td></tr></table>");
		sb.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr><td bgcolor=CC7700 height=1></td></tr></table>");
		sb.append("<table border=0 width=270><tr><td height=20></td></tr></table>");
		sb.append("<table width=270 border=0><tr><td align=center>").append(msg).append("</td></tr></table>");
		sb.append("<table border=0 width=270><tr><td height=15></td></tr></table>");
		sb.append("<table width=270 border=0><tr><td align=center>");
		sb.append("<button value=\"返回\" action=\"bypass -h Quest RefineSystem ").append(getPrefix(slot)).append("slot_").append(slot).append("\"");
		sb.append(" width=120 height=22 back=\"L2UI_CT1.Button_DF_Small_Down\" fore=\"L2UI_CT1.Button_DF_Small\"/>");
		sb.append("</td></tr></table>");
		sb.append("</td></tr></table></body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	// ── 工具方法 ─────────────────────────────────────────────────────────────

	private Item getEquipped(Player player, String slot)
	{
		final Integer paperdoll = SLOT_MAP.get(slot);
		return paperdoll == null ? null : player.getInventory().getPaperdollItem(paperdoll);
	}

	private int getMaxCharges(Item item)
	{
		return RefineSystemData.getInstance().getSpecialCharges(item.getId());
	}

	private boolean isArmorSlot(String slot)
	{
		for (String s : ARMOR_SLOTS)
		{
			if (s.equals(slot))
			{
				return true;
			}
		}
		return false;
	}

	private boolean isJewelrySlot(String slot)
	{
		for (String s : JEWELRY_SLOTS)
		{
			if (s.equals(slot))
			{
				return true;
			}
		}
		return false;
	}

	private String getPrefix(String slot)
	{
		if (isArmorSlot(slot))   return "armor_";
		if (isJewelrySlot(slot)) return "jewelry_";
		return "weapon_";
	}

	public static void main(String[] args)
	{
		new RefineSystem();
	}
}
