package custom.RefineSystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.RefineSystemData;
import org.l2jmobius.gameserver.managers.RefineSystemManager;
import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
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
	private static final String FILTER_VAR_PREFIX = "refine_filter_";
	private static final int MAX_FILTERS = 8;
	private static final int AUTO_REFINE_DELAY_MS = 500;

	// 正在自動精煉的玩家 objectId → 排程任務（用於防止重複啟動）
	private static final Map<Integer, ScheduledFuture<?>> _autoRefineTask = new ConcurrentHashMap<>();

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

		if (event.startsWith("do_premium_refine_"))
		{
			doPremiumRefine(npc, player, event.substring(18));
			return null;
		}

		if (event.startsWith("show_reset_confirm_"))
		{
			showResetConfirm(npc, player, event.substring(19));
			return null;
		}

		if (event.startsWith("do_reset_charges_"))
		{
			doResetCharges(npc, player, event.substring(17));
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

		if ("show_filter".equals(event))
		{
			showFilter(npc, player);
			return null;
		}

		if (event.startsWith("filter_pick_"))
		{
			final int page;
			try { page = Integer.parseInt(event.substring(12)); }
			catch (NumberFormatException e) { return null; }
			showFilterPick(npc, player, page);
			return null;
		}

		if (event.startsWith("filter_input_"))
		{
			showFilterInput(npc, player, event.substring(13).trim());
			return null;
		}

		if (event.startsWith("filter_add_"))
		{
			// format: filter_add_<seriesname> <minval>
			final String rest = event.substring(11);
			final int sp = rest.indexOf(' ');
			if (sp < 0) return null;
			final String seriesName = rest.substring(0, sp);
			final String minValStr = rest.substring(sp + 1).trim();
			doFilterAdd(npc, player, seriesName, minValStr);
			return null;
		}

		if (event.startsWith("filter_remove_"))
		{
			final int idx;
			try { idx = Integer.parseInt(event.substring(14)); }
			catch (NumberFormatException e) { return null; }
			doFilterRemove(npc, player, idx);
			return null;
		}

		if (event.startsWith("adv_show_"))
		{
			showAdvanced(npc, player, event.substring(9));
			return null;
		}

		if (event.startsWith("adv_pick_"))
		{
			// format: adv_pick_<slot>_<page>
			final String rest = event.substring(9);
			final int lastUs = rest.lastIndexOf('_');
			if (lastUs < 0) return null;
			final String slot = rest.substring(0, lastUs);
			final int page;
			try { page = Integer.parseInt(rest.substring(lastUs + 1)); }
			catch (NumberFormatException e) { return null; }
			showAdvancedPick(npc, player, slot, page);
			return null;
		}

		if (event.startsWith("adv_input_"))
		{
			// format: adv_input_<slot> <seriesname>
			final String rest = event.substring(10);
			final int sp = rest.indexOf(' ');
			if (sp < 0) return null;
			final String slot = rest.substring(0, sp);
			final String seriesName = rest.substring(sp + 1).trim();
			showAdvancedInput(npc, player, slot, seriesName);
			return null;
		}

		if (event.startsWith("adv_add_"))
		{
			// format: adv_add_<slot> <seriesname> <minval>
			final String rest = event.substring(8);
			final int sp1 = rest.indexOf(' ');
			if (sp1 < 0) return null;
			final String slot = rest.substring(0, sp1);
			final String args = rest.substring(sp1 + 1);
			final int sp2 = args.indexOf(' ');
			if (sp2 < 0) return null;
			final String seriesName = args.substring(0, sp2);
			final String minValStr = args.substring(sp2 + 1).trim();
			doAdvancedAdd(npc, player, slot, seriesName, minValStr);
			return null;
		}

		if (event.startsWith("adv_remove_"))
		{
			// format: adv_remove_<slot>_<index>
			final String rest = event.substring(11);
			final int lastUs = rest.lastIndexOf('_');
			if (lastUs < 0) return null;
			final String slot = rest.substring(0, lastUs);
			final int idx;
			try { idx = Integer.parseInt(rest.substring(lastUs + 1)); }
			catch (NumberFormatException e) { return null; }
			doAdvancedRemove(npc, player, slot, idx);
			return null;
		}

		if (event.startsWith("adv_refine_"))
		{
			doAdvancedRefine(npc, player, event.substring(11));
			return null;
		}

		if (event.startsWith("adv_premium_refine_"))
		{
			doAdvancedPremiumRefine(npc, player, event.substring(19));
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
		for (int i = 0; i < 4; i++)
		{
			sb.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr>");
			if (opts[i] > 0)
			{
				final int tier = RefineSystemData.getInstance().getTier(opts[i]);
				final String tierColor = RefineSystemData.getInstance().getTierColor(tier);
				final String tierLabel = tier > 0 ? "T" + tier : "T?";
				sb.append("<td width=28 align=center><font color=\"").append(tierColor).append("\"><b>").append(tierLabel).append("</b></font></td>");
				sb.append("<td width=4></td>");
				sb.append("<td width=136><font color=\"").append(tierColor).append("\">").append(RefineSystemData.getInstance().getSeriesName(opts[i])).append("</font></td>");
				sb.append("<td align=right><font color=\"88FF88\">+").append(RefineSystemData.getInstance().getValueDisplay(opts[i])).append("</font></td>");
			}
			else
			{
				sb.append("<td width=28 align=center><font color=\"444444\">—</font></td>");
				sb.append("<td width=4></td>");
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
			sb.append(" width=200 height=31 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			sb.append("</td></tr></table>");

			// 高級精煉按鈕（若有設定高級道具才顯示）
			if (RefineSystemData.getInstance().hasPremiumItem())
			{
				sb.append("<table border=0 width=270><tr><td height=3></td></tr></table>");
				sb.append("<table width=270 border=0><tr><td align=center>");
				sb.append("<font color=\"888888\" size=\"1\">消耗 ").append(RefineSystemData.getInstance().getPremiumItemCount()).append(" 個高級精煉石　只出 T").append(RefineSystemData.getInstance().getPremiumMinTier()).append("~T").append(RefineSystemData.getInstance().getPremiumMaxTier()).append("</font>");
				sb.append("</td></tr></table>");
				sb.append("<table width=270 border=0><tr><td align=center>");
				sb.append("<button value=\"高級精煉 ").append(SLOT_ZH.get(curSlot)).append("\" action=\"bypass -h Quest RefineSystem do_premium_refine_").append(curSlot).append("\"");
				sb.append(" width=200 height=31 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				sb.append("</td></tr></table>");
			}
		}
		else
		{
			sb.append("<table width=270 border=0><tr><td align=center height=28>");
			sb.append("<font color=\"FF6060\">此裝備精煉次數已耗盡</font>");
			sb.append("</td></tr></table>");
			if (RefineSystemData.getInstance().hasResetCost())
			{
				sb.append("<table border=0 width=270><tr><td height=3></td></tr></table>");
				sb.append("<table width=270 border=0><tr><td align=center>");
				sb.append("<button value=\"重置精煉次數\" action=\"bypass -h Quest RefineSystem show_reset_confirm_").append(curSlot).append("\"");
				sb.append(" width=200 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>");
				sb.append("</td></tr></table>");
			}
		}

		sb.append("<table border=0 width=270><tr><td height=3></td></tr></table>");

		sb.append("<table width=270 border=0><tr><td align=center>");
		sb.append("<button value=\"進階自動精煉\" action=\"bypass -h Quest RefineSystem adv_show_").append(curSlot).append("\"");
		sb.append(" width=200 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>");
		sb.append("</td></tr></table>");

		return sb.toString();
	}

	// ── 過濾器獨立頁面（從主選單進入）────────────────────────────────────────

	private void showFilter(Npc npc, Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(player, HTM_PATH + "filter.htm"));
		html.replace("%filter_rows%", buildFilterRows(player, null));
		player.sendPacket(html);
	}

	private void showFilterPick(Npc npc, Player player, int page)
	{
		final RefineSystemData data = RefineSystemData.getInstance();
		final List<RefineSystemData.SeriesEntry> allSeries = data.getAllSeries();
		final List<String[]> filters = loadFilters(player);

		final Set<String> usedNames = new HashSet<>();
		for (String[] f : filters) usedNames.add(f[0]);

		final StringBuilder buttons = new StringBuilder();
		int i = 0;
		while (i < allSeries.size())
		{
			final RefineSystemData.SeriesEntry se = allSeries.get(i);
			final boolean isShort = se.name.length() <= 6;
			if (isShort && i + 1 < allSeries.size() && allSeries.get(i + 1).name.length() <= 6)
			{
				final RefineSystemData.SeriesEntry se2 = allSeries.get(i + 1);
				buttons.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr>");
				buttons.append("<td width=2></td>");
				buttons.append("<td width=130 align=center>").append(makeFilterPickButton(se, usedNames, 126)).append("</td>");
				buttons.append("<td width=6></td>");
				buttons.append("<td width=130 align=center>").append(makeFilterPickButton(se2, usedNames, 126)).append("</td>");
				buttons.append("<td width=2></td>");
				buttons.append("</tr></table>");
				buttons.append("<table border=0 width=270><tr><td height=2></td></tr></table>");
				i += 2;
			}
			else
			{
				buttons.append("<table width=270 border=0><tr><td align=center>");
				buttons.append(makeFilterPickButton(se, usedNames, 220));
				buttons.append("</td></tr></table>");
				buttons.append("<table border=0 width=270><tr><td height=2></td></tr></table>");
				i++;
			}
		}

		// 複用 advanced_pick.htm，slot 欄位填空
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(player, HTM_PATH + "advanced_pick.htm"));
		html.replace("%slot_name%", "過濾器設定");
		html.replace("%series_buttons%", buttons.toString());
		html.replace("%slot%", "_filter");
		html.replace("adv_show__filter", "show_filter");
		player.sendPacket(html);
	}

	private String makeFilterPickButton(RefineSystemData.SeriesEntry se, Set<String> usedNames, int width)
	{
		final boolean used = usedNames.contains(se.name);
		if (used)
		{
			return "<button value=\"" + se.name + " [V]\""
				+ " action=\"\""
				+ " width=" + width + " height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>";
		}
		return "<button value=\"" + se.name + "\""
			+ " action=\"bypass -h Quest RefineSystem filter_input_" + se.name + "\""
			+ " width=" + width + " height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>";
	}

	private void showFilterInput(Npc npc, Player player, String seriesName)
	{
		final RefineSystemData data = RefineSystemData.getInstance();
		RefineSystemData.SeriesEntry se = null;
		for (RefineSystemData.SeriesEntry s : data.getAllSeries())
		{
			if (s.name.equals(seriesName)) { se = s; break; }
		}
		if (se == null)
		{
			showFilter(npc, player);
			return;
		}

		final int minRaw = se.ranges.get(0).from;
		final int maxRaw = se.ranges.get(se.ranges.size() - 1).to;
		final String rangeHint;
		final String inputHint;
		if (se.valueType == RefineSystemData.ValueType.FLAT)
		{
			rangeHint = "數值範圍：" + minRaw + " ~ " + maxRaw + "（可輸入超過上限，加總計算）";
			inputHint = "輸入整數，例如：" + maxRaw + "（即 " + maxRaw + " 點）";
		}
		else
		{
			rangeHint = String.format("數值範圍：%.2f%% ~ %.2f%%（可輸入超過上限，加總計算）", minRaw / 100.0, maxRaw / 100.0);
			inputHint = String.format("輸入整數，例如：%d（即 %.2f%%）", maxRaw, maxRaw / 100.0);
		}

		// 複用 advanced_input.htm，bypass 改成 filter_add_
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		String htm = HtmCache.getInstance().getHtm(player, HTM_PATH + "advanced_input.htm");
		// 替換確認按鈕的 bypass action 和返回按鈕
		htm = htm.replace(
			"bypass -h Quest RefineSystem adv_add_%slot% %series_name% $minval",
			"bypass -h Quest RefineSystem filter_add_" + seriesName + " $minval"
		);
		htm = htm.replace(
			"bypass -h Quest RefineSystem adv_pick_%slot%_0",
			"bypass -h Quest RefineSystem filter_pick_0"
		);
		html.setHtml(htm);
		html.replace("%slot_name%", "過濾器設定");
		html.replace("%series_name%", seriesName);
		html.replace("%range_hint%", rangeHint);
		html.replace("%input_hint%", inputHint);
		html.replace("%slot%", "_filter");
		player.sendPacket(html);
	}

	private void doFilterAdd(Npc npc, Player player, String seriesName, String minValStr)
	{
		final RefineSystemData data = RefineSystemData.getInstance();
		boolean valid = false;
		for (RefineSystemData.SeriesEntry se : data.getAllSeries())
		{
			if (se.name.equals(seriesName)) { valid = true; break; }
		}
		if (!valid) { showFilter(npc, player); return; }

		final List<String[]> filters = loadFilters(player);
		for (String[] f : filters)
		{
			if (f[0].equals(seriesName)) { showFilter(npc, player); return; }
		}
		if (filters.size() >= MAX_FILTERS) { showFilter(npc, player); return; }

		int minVal;
		try { minVal = Integer.parseInt(minValStr.trim()); }
		catch (NumberFormatException e) { minVal = 0; }
		if (minVal < 0) minVal = 0;

		filters.add(new String[]{ seriesName, String.valueOf(minVal) });
		saveFilters(player, filters);
		showFilter(npc, player);
	}

	private void doFilterRemove(Npc npc, Player player, int index)
	{
		final List<String[]> filters = loadFilters(player);
		if (index >= 0 && index < filters.size())
		{
			filters.remove(index);
			saveFilters(player, filters);
		}
		showFilter(npc, player);
	}

	// ── 進階自動精煉 ──────────────────────────────────────────────────────────

	private void showAdvanced(Npc npc, Player player, String slot)
	{
		final Item item = getEquipped(player, slot);
		final int charges = item != null ? RefineSystemManager.getInstance().getCharges(item) : 0;
		final int maxCharges = item != null ? getMaxCharges(item) : 0;

		final RefineSystemData data = RefineSystemData.getInstance();
		final StringBuilder seriesList = new StringBuilder();
		for (RefineSystemData.SeriesEntry se : data.getAllSeries())
		{
			if (seriesList.length() > 0) seriesList.append(';');
			seriesList.append(se.name);
		}

		final String backEvent = getPrefix(slot) + "slot_" + slot;

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(player, HTM_PATH + "advanced.htm"));
		html.replace("%slot_name%", SLOT_ZH.getOrDefault(slot, slot));
		html.replace("%charges%", String.valueOf(charges));
		html.replace("%max_charges%", String.valueOf(maxCharges));
		html.replace("%charges_color%", charges > 0 ? "AAAAAA" : "FF6060");
		html.replace("%series_list%", seriesList.toString());
		html.replace("%slot%", slot);
		html.replace("%filter_rows%", buildFilterRows(player, slot));
		html.replace("%back_event%", backEvent);

		// 高級自動精煉按鈕（只在有設定高級道具時顯示）
		final String premiumButton;
		if (RefineSystemData.getInstance().hasPremiumItem())
		{
			final StringBuilder pb = new StringBuilder();
			pb.append("<table border=0 width=270><tr><td height=3></td></tr></table>");
			pb.append("<table width=270 border=0><tr><td align=center>");
			pb.append("<font color=\"888888\" size=\"1\">高級自動精煉：只出 T")
				.append(RefineSystemData.getInstance().getPremiumMinTier())
				.append("~T").append(RefineSystemData.getInstance().getPremiumMaxTier())
				.append("，消耗高級精煉石</font>");
			pb.append("</td></tr></table>");
			pb.append("<table width=270 border=0><tr><td align=center>");
			pb.append("<button value=\"開始自動高級精煉\" action=\"bypass -h Quest RefineSystem adv_premium_refine_").append(slot).append("\"");
			pb.append(" width=180 height=28 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>");
			pb.append("</td></tr></table>");
			premiumButton = pb.toString();
		}
		else
		{
			premiumButton = "";
		}
		html.replace("%premium_button%", premiumButton);

		player.sendPacket(html);
	}

	private void showAdvancedPick(Npc npc, Player player, String slot, int page)
	{
		final RefineSystemData data = RefineSystemData.getInstance();
		final List<RefineSystemData.SeriesEntry> allSeries = data.getAllSeries();
		final List<String[]> filters = loadFilters(player);

		final Set<String> usedNames = new HashSet<>();
		for (String[] f : filters) usedNames.add(f[0]);

		// 短詞（≤6字）兩個一行，長詞（>6字）一個一行
		final StringBuilder buttons = new StringBuilder();
		int i = 0;
		while (i < allSeries.size())
		{
			final RefineSystemData.SeriesEntry se = allSeries.get(i);
			final boolean isShort = se.name.length() <= 6;

			// 嘗試配對：下一個也是短詞才並排
			if (isShort && i + 1 < allSeries.size() && allSeries.get(i + 1).name.length() <= 6)
			{
				final RefineSystemData.SeriesEntry se2 = allSeries.get(i + 1);
				buttons.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr>");
				buttons.append("<td width=2></td>");
				buttons.append("<td width=130 align=center>").append(makePickButton(se, slot, usedNames, 126)).append("</td>");
				buttons.append("<td width=6></td>");
				buttons.append("<td width=130 align=center>").append(makePickButton(se2, slot, usedNames, 126)).append("</td>");
				buttons.append("<td width=2></td>");
				buttons.append("</tr></table>");
				buttons.append("<table border=0 width=270><tr><td height=2></td></tr></table>");
				i += 2;
			}
			else
			{
				buttons.append("<table width=270 border=0><tr><td align=center>");
				buttons.append(makePickButton(se, slot, usedNames, 220));
				buttons.append("</td></tr></table>");
				buttons.append("<table border=0 width=270><tr><td height=2></td></tr></table>");
				i++;
			}
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(player, HTM_PATH + "advanced_pick.htm"));
		html.replace("%slot_name%", SLOT_ZH.getOrDefault(slot, slot));
		html.replace("%series_buttons%", buttons.toString());
		html.replace("%slot%", slot);
		player.sendPacket(html);
	}

	private String makePickButton(RefineSystemData.SeriesEntry se, String slot, Set<String> usedNames, int width)
	{
		final boolean used = usedNames.contains(se.name);
		if (used)
		{
			return "<button value=\"" + se.name + " [已選]\""
				+ " action=\"\""
				+ " width=" + width + " height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>";
		}
		return "<button value=\"" + se.name + "\""
			+ " action=\"bypass -h Quest RefineSystem adv_input_" + slot + " " + se.name + "\""
			+ " width=" + width + " height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>";
	}

	private void showAdvancedInput(Npc npc, Player player, String slot, String seriesName)
	{
		final RefineSystemData data = RefineSystemData.getInstance();
		RefineSystemData.SeriesEntry se = null;
		for (RefineSystemData.SeriesEntry s : data.getAllSeries())
		{
			if (s.name.equals(seriesName)) { se = s; break; }
		}
		if (se == null)
		{
			showAdvancedPick(npc, player, slot, 0);
			return;
		}

		final int minRaw = se.ranges.get(0).from;
		final int maxRaw = se.ranges.get(se.ranges.size() - 1).to;
		final String rangeHint;
		final String inputHint;
		if (se.valueType == RefineSystemData.ValueType.FLAT)
		{
			rangeHint = "數值範圍：" + minRaw + " ~ " + maxRaw + "（可輸入超過上限，加總計算）";
			inputHint = "輸入整數，例如：" + maxRaw + "（即 " + maxRaw + " 點）";
		}
		else
		{
			rangeHint = String.format("數值範圍：%.2f%% ~ %.2f%%（可輸入超過上限，加總計算）", minRaw / 100.0, maxRaw / 100.0);
			inputHint = String.format("輸入整數，例如：%d（即 %.2f%%）", maxRaw, maxRaw / 100.0);
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(player, HTM_PATH + "advanced_input.htm"));
		html.replace("%slot_name%", SLOT_ZH.getOrDefault(slot, slot));
		html.replace("%series_name%", seriesName);
		html.replace("%range_hint%", rangeHint);
		html.replace("%input_hint%", inputHint);
		html.replace("%slot%", slot);
		player.sendPacket(html);
	}


	private void doAdvancedAdd(Npc npc, Player player, String slot, String seriesName, String minValStr)
	{
		final RefineSystemData data = RefineSystemData.getInstance();
		RefineSystemData.SeriesEntry target = null;
		for (RefineSystemData.SeriesEntry se : data.getAllSeries())
		{
			if (se.name.equals(seriesName))
			{
				target = se;
				break;
			}
		}
		if (target == null)
		{
			showAdvanced(npc, player, slot);
			return;
		}

		final List<String[]> filters = loadFilters(player);

		// 重複詞條檢查
		for (String[] f : filters)
		{
			if (f[0].equals(seriesName))
			{
				showAdvanced(npc, player, slot);
				return;
			}
		}

		if (filters.size() >= MAX_FILTERS)
		{
			showAdvanced(npc, player, slot);
			return;
		}

		int minVal;
		try { minVal = Integer.parseInt(minValStr.trim()); }
		catch (NumberFormatException e) { minVal = 0; }
		if (minVal < 0) minVal = 0;

		filters.add(new String[]{ seriesName, String.valueOf(minVal) });
		saveFilters(player, filters);
		showAdvanced(npc, player, slot);
	}

	private void doAdvancedRemove(Npc npc, Player player, String slot, int index)
	{
		final List<String[]> filters = loadFilters(player);
		if (index >= 0 && index < filters.size())
		{
			filters.remove(index);
			saveFilters(player, filters);
		}
		showAdvanced(npc, player, slot);
	}

	private void doAdvancedRefine(Npc npc, Player player, String slot)
	{
		final int playerId = player.getObjectId();

		// 防止重複啟動
		if (_autoRefineTask.containsKey(playerId))
		{
			player.sendMessage("[精煉] 自動精煉已在執行中。");
			return;
		}

		// 啟動前先做一次前置檢查
		final Item item = getEquipped(player, slot);
		if (item == null)
		{
			showAdvancedResult(npc, player, slot, "stop_no_item", null);
			return;
		}
		if (RefineSystemManager.getInstance().getCharges(item) <= 0)
		{
			showAdvancedResult(npc, player, slot, "stop_no_charges", null);
			return;
		}
		if (player.getInventory().getInventoryItemCount(RefineSystemData.getInstance().getRefineItemId(), -1) < RefineSystemData.getInstance().getRefineItemCount())
		{
			showAdvancedResult(npc, player, slot, "stop_no_material", null);
			return;
		}

		final List<String[]> filters = loadFilters(player);
		if (filters.isEmpty())
		{
			player.sendMessage("[精煉] 請先設定過濾條件，否則精煉次數將全部消耗完。");
			showAdvanced(npc, player, slot);
			return;
		}

		player.sendMessage("[精煉] 開始自動精煉 " + SLOT_ZH.getOrDefault(slot, slot) + "...");

		scheduleNextRefine(npc, player, slot, 0);
	}

	private void doAdvancedPremiumRefine(Npc npc, Player player, String slot)
	{
		final RefineSystemData data = RefineSystemData.getInstance();

		if (!data.hasPremiumItem())
		{
			player.sendMessage("[精煉] 高級精煉道具尚未設定。");
			return;
		}

		final int playerId = player.getObjectId();

		if (_autoRefineTask.containsKey(playerId))
		{
			player.sendMessage("[精煉] 自動精煉已在執行中。");
			return;
		}

		final Item item = getEquipped(player, slot);
		if (item == null)
		{
			showAdvancedResult(npc, player, slot, "stop_no_item", null);
			return;
		}
		if (RefineSystemManager.getInstance().getCharges(item) <= 0)
		{
			showAdvancedResult(npc, player, slot, "stop_no_charges", null);
			return;
		}
		if (player.getInventory().getInventoryItemCount(data.getPremiumItemId(), -1) < data.getPremiumItemCount())
		{
			showAdvancedResult(npc, player, slot, "stop_no_material", null);
			return;
		}

		final List<String[]> premiumFilters = loadFilters(player);
		if (premiumFilters.isEmpty())
		{
			player.sendMessage("[精煉] 請先設定過濾條件，否則精煉次數將全部消耗完。");
			showAdvanced(npc, player, slot);
			return;
		}

		player.sendMessage("[精煉] 開始自動高級精煉 " + SLOT_ZH.getOrDefault(slot, slot) + "...");

		scheduleNextPremiumRefine(npc, player, slot, 0);
	}

	private void scheduleNextRefine(Npc npc, Player player, String slot, int count)	{
		final int playerId = player.getObjectId();
		final ScheduledFuture<?> task = ThreadPool.schedule(() ->
		{
			_autoRefineTask.remove(playerId);

			// 每次執行前重新檢查狀態
			final Item item = getEquipped(player, slot);
			if (item == null)
			{
				player.sendMessage("[精煉] 裝備已卸下，自動精煉停止。");
				showAdvancedResult(npc, player, slot, "stop_no_item", null);
				return;
			}
			if (RefineSystemManager.getInstance().getCharges(item) <= 0)
			{
				player.sendMessage("[精煉] 精煉次數耗盡，自動精煉停止。共精煉 " + count + " 次。");
				showAdvancedResult(npc, player, slot, "stop_no_charges", null);
				return;
			}
			if (player.getInventory().getInventoryItemCount(RefineSystemData.getInstance().getRefineItemId(), -1) < RefineSystemData.getInstance().getRefineItemCount())
			{
				player.sendMessage("[精煉] 精煉石不足，自動精煉停止。共精煉 " + count + " 次。");
				showAdvancedResult(npc, player, slot, "stop_no_material", null);
				return;
			}

			// 執行一次精煉
			final int tierCount = RefineSystemData.getInstance().rollTierCount();
			final int op1 = RefineSystemData.getInstance().rollRefineId();
			final int op2 = tierCount >= 2 ? RefineSystemData.getInstance().rollRefineId() : 0;
			final int op3 = tierCount >= 3 ? RefineSystemData.getInstance().rollRefineId() : 0;
			final int op4 = tierCount >= 4 ? RefineSystemData.getInstance().rollRefineId() : 0;

			item.setAugmentation(VariationInstance.ofRaw(RefineSystemData.getInstance().getRefineItemId(), op1, op2, op3, op4), true);
			player.destroyItemByItemId(ItemProcessType.NONE, RefineSystemData.getInstance().getRefineItemId(), RefineSystemData.getInstance().getRefineItemCount(), player, true);
			RefineSystemManager.getInstance().consumeCharge(item);

			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(item);
			player.sendInventoryUpdate(iu);

			final int newCount = count + 1;
			final int remaining = RefineSystemManager.getInstance().getCharges(item);
			player.sendMessage("[精煉] 第 " + newCount + " 次，剩餘次數：" + remaining);

			// 檢查過濾條件
			final List<String[]> filters = loadFilters(player);
			final int[] newOpts = { op1, op2, op3, op4 };
			if (!filters.isEmpty() && checkFilters(item, filters))
			{
				player.sendMessage("[精煉] 達成目標條件！共精煉 " + newCount + " 次。");
				showAdvancedResult(npc, player, slot, "stop_matched", newOpts);
				return;
			}

			// 繼續下一次
			scheduleNextRefine(npc, player, slot, newCount);

		}, AUTO_REFINE_DELAY_MS);

		_autoRefineTask.put(playerId, task);
	}

	private void scheduleNextPremiumRefine(Npc npc, Player player, String slot, int count)
	{
		final RefineSystemData data = RefineSystemData.getInstance();
		final int playerId = player.getObjectId();
		final ScheduledFuture<?> task = ThreadPool.schedule(() ->
		{
			_autoRefineTask.remove(playerId);

			final Item item = getEquipped(player, slot);
			if (item == null)
			{
				player.sendMessage("[精煉] 裝備已卸下，自動高級精煉停止。");
				showAdvancedResult(npc, player, slot, "stop_no_item", null);
				return;
			}
			if (RefineSystemManager.getInstance().getCharges(item) <= 0)
			{
				player.sendMessage("[精煉] 精煉次數耗盡，自動高級精煉停止。共精煉 " + count + " 次。");
				showAdvancedResult(npc, player, slot, "stop_no_charges", null);
				return;
			}
			if (player.getInventory().getInventoryItemCount(data.getPremiumItemId(), -1) < data.getPremiumItemCount())
			{
				player.sendMessage("[精煉] 高級精煉石不足，自動高級精煉停止。共精煉 " + count + " 次。");
				showAdvancedResult(npc, player, slot, "stop_no_material", null);
				return;
			}

			final int minTier = data.getPremiumMinTier();
			final int maxTier = data.getPremiumMaxTier();
			final int tierCount = data.rollTierCount();
			final int op1 = data.rollRefineIdForTier(minTier, maxTier);
			final int op2 = tierCount >= 2 ? data.rollRefineIdForTier(minTier, maxTier) : 0;
			final int op3 = tierCount >= 3 ? data.rollRefineIdForTier(minTier, maxTier) : 0;
			final int op4 = tierCount >= 4 ? data.rollRefineIdForTier(minTier, maxTier) : 0;

			item.setAugmentation(VariationInstance.ofRaw(data.getPremiumItemId(), op1, op2, op3, op4), true);
			player.destroyItemByItemId(ItemProcessType.NONE, data.getPremiumItemId(), data.getPremiumItemCount(), player, true);
			RefineSystemManager.getInstance().consumeCharge(item);

			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(item);
			player.sendInventoryUpdate(iu);

			final int newCount = count + 1;
			final int remaining = RefineSystemManager.getInstance().getCharges(item);
			player.sendMessage("[精煉] 高級第 " + newCount + " 次，剩餘次數：" + remaining);

			final List<String[]> filters = loadFilters(player);
			final int[] newOpts = { op1, op2, op3, op4 };
			if (!filters.isEmpty() && checkFilters(item, filters))
			{
				player.sendMessage("[精煉] 達成目標條件！共高級精煉 " + newCount + " 次。");
				showAdvancedResult(npc, player, slot, "stop_matched", newOpts);
				return;
			}

			scheduleNextPremiumRefine(npc, player, slot, newCount);

		}, AUTO_REFINE_DELAY_MS);

		_autoRefineTask.put(playerId, task);
	}

	private boolean checkFilters(Item item, List<String[]> filters)
	{
		final VariationInstance aug = item.getAugmentation();
		if (aug == null) return false;
		final int[] opts = { aug.getOption1Id(), aug.getOption2Id(), aug.getOption3Id(), aug.getOption4Id() };

		for (String[] filter : filters)
		{
			final String seriesName = filter[0];
			final int minVal;
			try { minVal = Integer.parseInt(filter[1]); }
			catch (NumberFormatException e) { continue; }

			int total = 0;
			for (int optionId : opts)
			{
				if (optionId <= 0) continue;
				final int base = (optionId / 10000) * 10000;
				final RefineSystemData.SeriesEntry se = RefineSystemData.getInstance().getSeriesByBase(base);
				if (se != null && se.name.equals(seriesName))
				{
					total += optionId % 10000;
				}
			}
			if (total >= minVal) return true;
		}
		return false;
	}

	private void showAdvancedResult(Npc npc, Player player, String slot, String reason, int[] opts)
	{
		final Item item = getEquipped(player, slot);
		final int charges = item != null ? RefineSystemManager.getInstance().getCharges(item) : 0;
		final int maxCharges = item != null ? getMaxCharges(item) : 0;

		final String resultColor;
		final String resultTitle;
		final String resultDesc;

		switch (reason)
		{
			case "stop_matched":
				resultColor = "88FF88";
				resultTitle = "達成目標條件！";
				resultDesc = "已達成過濾條件，自動精煉停止。";
				break;
			case "stop_no_charges":
				resultColor = "FF6060";
				resultTitle = "精煉次數耗盡";
				resultDesc = "此裝備已無剩餘精煉次數。";
				break;
			case "stop_no_material":
				resultColor = "FF6060";
				resultTitle = "精煉石不足";
				resultDesc = "背包中精煉石數量不足，無法繼續。";
				break;
			default: // stop_no_item
				resultColor = "FF6060";
				resultTitle = "裝備未穿戴";
				resultDesc = "該部位裝備已卸下，無法繼續精煉。";
				break;
		}

		final StringBuilder resultRows = new StringBuilder();
		if (opts != null)
		{
			for (int i = 0; i < 4; i++)
			{
				resultRows.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr>");
				if (opts[i] > 0)
				{
					final int tier = RefineSystemData.getInstance().getTier(opts[i]);
					final String tierColor = RefineSystemData.getInstance().getTierColor(tier);
					final String tierLabel = tier > 0 ? "T" + tier : "T?";
					resultRows.append("<td width=28 align=center><font color=\"").append(tierColor).append("\"><b>").append(tierLabel).append("</b></font></td>");
					resultRows.append("<td width=4></td>");
					resultRows.append("<td width=136><font color=\"").append(tierColor).append("\">").append(RefineSystemData.getInstance().getSeriesName(opts[i])).append("</font></td>");
					resultRows.append("<td align=right><font color=\"88FF88\">+").append(RefineSystemData.getInstance().getValueDisplay(opts[i])).append("</font></td>");
				}
				else
				{
					resultRows.append("<td width=28 align=center><font color=\"444444\">—</font></td>");
					resultRows.append("<td width=4></td>");
					resultRows.append("<td colspan=2><font color=\"666666\">—</font></td>");
				}
				resultRows.append("</tr></table>");
			}
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(player, HTM_PATH + "advanced_result.htm"));
		html.replace("%slot_name%", SLOT_ZH.getOrDefault(slot, slot));
		html.replace("%charges%", String.valueOf(charges));
		html.replace("%max_charges%", String.valueOf(maxCharges));
		html.replace("%charges_color%", charges > 0 ? "AAAAAA" : "FF6060");
		html.replace("%result_color%", resultColor);
		html.replace("%result_title%", resultTitle);
		html.replace("%result_desc%", resultDesc);
		html.replace("%result_rows%", resultRows.toString());
		html.replace("%slot%", slot);
		player.sendPacket(html);
	}

	private String buildFilterRows(Player player, String slot)
	{
		final List<String[]> filters = loadFilters(player);
		if (filters.isEmpty())
		{
			return "<table width=270 border=0><tr><td align=center height=24>" +
				"<font color=\"666666\">尚未設定任何條件</font>" +
				"</td></tr></table>";
		}

		final RefineSystemData data = RefineSystemData.getInstance();
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < filters.size(); i++)
		{
			final String seriesName = filters.get(i)[0];
			final int minVal;
			try { minVal = Integer.parseInt(filters.get(i)[1]); }
			catch (NumberFormatException e) { continue; }

			RefineSystemData.SeriesEntry se = null;
			for (RefineSystemData.SeriesEntry s : data.getAllSeries())
			{
				if (s.name.equals(seriesName)) { se = s; break; }
			}

			final String display;
			if (se != null && se.valueType == RefineSystemData.ValueType.FLAT)
			{
				display = String.valueOf(minVal);
			}
			else
			{
				display = String.format("%.2f%%", minVal / 100.0);
			}

			sb.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr>");
			sb.append("<td width=4></td>");
			sb.append("<td width=120><font color=\"FFE066\">").append(seriesName).append("</font></td>");
			sb.append("<td width=70><font color=\"88FF88\">≥ ").append(display).append("</font></td>");
			sb.append("<td width=60 align=right>");
			final String removeAction = slot != null
				? "bypass -h Quest RefineSystem adv_remove_" + slot + "_" + i
				: "bypass -h Quest RefineSystem filter_remove_" + i;
			sb.append("<a action=\"").append(removeAction).append("\">");
			sb.append("<font color=\"FF6060\">[移除]</font></a>");
			sb.append("</td>");
			sb.append("<td width=4></td>");
			sb.append("</tr></table>");
			sb.append("<table border=0 width=270><tr><td height=2></td></tr></table>");
		}
		return sb.toString();
	}

	private List<String[]> loadFilters(Player player)
	{
		final List<String[]> list = new ArrayList<>();
		for (int i = 0; i < MAX_FILTERS; i++)
		{
			final String val = player.getVariables().getString(FILTER_VAR_PREFIX + i, null);
			if (val == null) break;
			final int sep = val.indexOf('|');
			if (sep < 0) break;
			list.add(new String[]{ val.substring(0, sep), val.substring(sep + 1) });
		}
		return list;
	}

	private void saveFilters(Player player, List<String[]> filters)
	{
		for (int i = 0; i < MAX_FILTERS; i++)
		{
			if (i < filters.size())
			{
				player.getVariables().set(FILTER_VAR_PREFIX + i, filters.get(i)[0] + "|" + filters.get(i)[1]);
			}
			else
			{
				player.getVariables().remove(FILTER_VAR_PREFIX + i);
			}
		}
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

	private void doPremiumRefine(Npc npc, Player player, String slot)
	{
		final RefineSystemData data = RefineSystemData.getInstance();

		if (!data.hasPremiumItem())
		{
			sendMsg(npc, player, "<font color=\"FF0000\">高級精煉道具尚未設定。</font>", slot);
			return;
		}

		final Item item = getEquipped(player, slot);
		if (item == null)
		{
			sendMsg(npc, player, "<font color=\"FF0000\">" + SLOT_ZH.get(slot) + " 尚未裝備，無法精煉。</font>", slot);
			return;
		}

		final int charges = RefineSystemManager.getInstance().getCharges(item);
		if (charges <= 0)
		{
			sendMsg(npc, player, "<font color=\"FF0000\">此裝備精煉次數已耗盡，無法再次精煉。</font>", slot);
			return;
		}

		final long itemCount = player.getInventory().getInventoryItemCount(data.getPremiumItemId(), -1);
		if (itemCount < data.getPremiumItemCount())
		{
			sendMsg(npc, player, "<font color=\"FF0000\">高級精煉石不足，需要 " + data.getPremiumItemCount() + " 個。</font>", slot);
			return;
		}

		final int minTier = data.getPremiumMinTier();
		final int maxTier = data.getPremiumMaxTier();

		final int tierCount = data.rollTierCount();
		final int op1 = data.rollRefineIdForTier(minTier, maxTier);
		final int op2 = tierCount >= 2 ? data.rollRefineIdForTier(minTier, maxTier) : 0;
		final int op3 = tierCount >= 3 ? data.rollRefineIdForTier(minTier, maxTier) : 0;
		final int op4 = tierCount >= 4 ? data.rollRefineIdForTier(minTier, maxTier) : 0;

		item.setAugmentation(VariationInstance.ofRaw(data.getPremiumItemId(), op1, op2, op3, op4), true);

		player.destroyItemByItemId(ItemProcessType.NONE, data.getPremiumItemId(), data.getPremiumItemCount(), player, true);

		RefineSystemManager.getInstance().consumeCharge(item);

		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(item);
		player.sendInventoryUpdate(iu);

		refreshSlotPage(npc, player, slot);
	}

	private void showResetConfirm(Npc npc, Player player, String slot)
	{
		final Item item = getEquipped(player, slot);
		if (item == null)
		{
			sendMsg(npc, player, "<font color=\"FF0000\">" + SLOT_ZH.getOrDefault(slot, slot) + " 尚未裝備。</font>", slot);
			return;
		}

		final RefineSystemData data = RefineSystemData.getInstance();
		final int maxCharges = getMaxCharges(item);

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><head><title>重置精煉次數</title></head><body scroll=\"no\">");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=292 height=358>");
		sb.append("<tr><td valign=\"top\" align=\"center\">");

		sb.append("<table border=0 width=270><tr><td height=10></td></tr></table>");
		sb.append("<table width=270 border=0><tr><td align=center>");
		sb.append("<font color=\"CC7700\" size=\"4\"><b>重置精煉次數</b></font>");
		sb.append("</td></tr></table>");
		sb.append("<table border=0 width=270><tr><td height=4></td></tr></table>");
		sb.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr><td bgcolor=CC7700 height=1></td></tr></table>");
		sb.append("<table border=0 width=270><tr><td height=8></td></tr></table>");

		// 裝備名稱 + 重置後次數
		sb.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr>");
		sb.append("<td width=4></td>");
		sb.append("<td><font color=\"FFCC44\">").append(item.getName()).append("</font></td>");
		sb.append("<td align=right><font color=\"AAAAAA\">→ ").append(maxCharges).append(" 次</font></td>");
		sb.append("<td width=4></td>");
		sb.append("</tr></table>");

		sb.append("<table border=0 width=270><tr><td height=8></td></tr></table>");
		sb.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr><td bgcolor=444444 height=1></td></tr></table>");
		sb.append("<table border=0 width=270><tr><td height=6></td></tr></table>");

		// 消耗道具列表
		sb.append("<table width=270 border=0><tr><td width=4></td><td>");
		sb.append("<font color=\"CC7700\">需消耗道具</font>");
		sb.append("</td></tr></table>");
		sb.append("<table border=0 width=270><tr><td height=4></td></tr></table>");

		for (RefineSystemData.ResetCostEntry cost : data.getResetCostList())
		{
			final ItemTemplate tpl = ItemData.getInstance().getTemplate(cost.itemId);
			final String itemName = tpl != null ? tpl.getName() : "道具 #" + cost.itemId;
			final long owned = player.getInventory().getInventoryItemCount(cost.itemId, -1);
			final boolean enough = owned >= cost.count;
			final String color = enough ? "88FF88" : "FF6060";

			sb.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr>");
			sb.append("<td width=8></td>");
			sb.append("<td><font color=\"").append(color).append("\">").append(itemName).append("</font></td>");
			sb.append("<td align=right><font color=\"").append(color).append("\">× ").append(cost.count).append("</font></td>");
			sb.append("<td width=8></td>");
			sb.append("</tr></table>");
			sb.append("<table border=0 width=270><tr><td height=3></td></tr></table>");
		}

		sb.append("<table border=0 width=270><tr><td height=4></td></tr></table>");
		sb.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr><td bgcolor=CC7700 height=1></td></tr></table>");
		sb.append("<table border=0 width=270><tr><td height=8></td></tr></table>");

		// 確認 / 取消
		sb.append("<table width=270 border=0><tr>");
		sb.append("<td align=center>");
		sb.append("<button value=\"確認重置\" action=\"bypass -h Quest RefineSystem do_reset_charges_").append(slot).append("\"");
		sb.append(" width=120 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>");
		sb.append("</td>");
		sb.append("<td align=center>");
		sb.append("<button value=\"取消\" action=\"bypass -h Quest RefineSystem ").append(getPrefix(slot)).append("slot_").append(slot).append("\"");
		sb.append(" width=120 height=26 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>");
		sb.append("</td>");
		sb.append("</tr></table>");

		sb.append("</td></tr></table></body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	private void doResetCharges(Npc npc, Player player, String slot)
	{
		final Item item = getEquipped(player, slot);
		if (item == null)
		{
			sendMsg(npc, player, "<font color=\"FF0000\">" + SLOT_ZH.getOrDefault(slot, slot) + " 尚未裝備，無法重置。</font>", slot);
			return;
		}

		final RefineSystemData data = RefineSystemData.getInstance();

		// 檢查所有消耗道具是否足夠
		for (RefineSystemData.ResetCostEntry cost : data.getResetCostList())
		{
			final long owned = player.getInventory().getInventoryItemCount(cost.itemId, -1);
			if (owned < cost.count)
			{
				showResetConfirm(npc, player, slot);
				return;
			}
		}

		// 扣除道具
		for (RefineSystemData.ResetCostEntry cost : data.getResetCostList())
		{
			player.destroyItemByItemId(ItemProcessType.NONE, cost.itemId, cost.count, player, true);
		}

		// 重置次數
		RefineSystemManager.getInstance().resetCharges(item);

		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(item);
		player.sendInventoryUpdate(iu);

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
		sb.append(" width=120 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"/>");
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
