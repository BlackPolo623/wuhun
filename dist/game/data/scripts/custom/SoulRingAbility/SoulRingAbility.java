/**
 * 魂環能力分配系統
 */
package custom.SoulRingAbility;

import java.util.List;

import org.l2jmobius.gameserver.data.xml.SoulRingAbilityData;
import org.l2jmobius.gameserver.data.xml.SoulRingAbilityData.CategoryConfig;
import org.l2jmobius.gameserver.data.xml.SoulRingAbilityData.StatConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 魂環能力分配系統
 * 能力設定全部來自 SoulRingAbilityData.xml，不再硬編碼。
 * @author 黑普羅
 */
public class SoulRingAbility extends Script
{
	private static final String SOUL_RING_VAR = "魂環";
	private static final String SYSTEM_NAME = "魂環能力";

	private SoulRingAbility()
	{
		// NPC ID 直接使用，若日後需要從 XML 讀取可自行擴充
		addStartNpc(900013);
		addTalkId(900013);
		addFirstTalkId(900013);
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
		switch (event)
		{
			case "index":
				showIndexPage(player, npc);
				break;
			case "show_pve_normal":
				showCategoryPage(player, npc, "pve_normal");
				break;
			case "show_pve_raid":
				showCategoryPage(player, npc, "pve_raid");
				break;
			case "show_pvp":
				showCategoryPage(player, npc, "pvp");
				break;
			case "show_special":
				showCategoryPage(player, npc, "special");
				break;
			case "reset":
				showResetConfirm(player, npc);
				break;
			case "reset_confirm":
				resetAllPoints(player);
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "已重置所有能力點數"));
				showIndexPage(player, npc);
				break;
			default:
				if (event.startsWith("addN_") || event.startsWith("subN_"))
				{
					handleAddSub(event, npc, player);
				}
				break;
		}
		return null;
	}

	// ==================== 事件處理 ====================

	private void handleAddSub(String event, Npc npc, Player player)
	{
		final boolean isAdd = event.startsWith("addN_");
		// 格式：addN_VARNAME $amtX（空格分隔）
		final String[] spaceParts = event.split(" ", 2);
		final String varName = spaceParts[0].substring(5); // 去掉 "addN_" 或 "subN_"

		int amount = 0;
		if ((spaceParts.length >= 2) && !spaceParts[1].trim().isEmpty())
		{
			try
			{
				amount = Integer.parseInt(spaceParts[1].trim());
			}
			catch (NumberFormatException e)
			{
				player.sendMessage("請輸入有效的整數數字");
				showCategoryPageByVar(player, npc, varName);
				return;
			}
		}

		if (amount <= 0)
		{
			player.sendMessage("請先輸入要加點的數字");
			showCategoryPageByVar(player, npc, varName);
			return;
		}

		final StatConfig sc = SoulRingAbilityData.getInstance().getStatByVar(varName);
		if (sc == null)
		{
			player.sendMessage("系統錯誤：未知的能力類型");
			showIndexPage(player, npc);
			return;
		}

		if (isAdd)
		{
			addPoints(player, sc, amount);
		}
		else
		{
			removePoints(player, sc, amount);
		}

		showCategoryPageByVar(player, npc, varName);
	}

	// ==================== 頁面顯示 ====================

	private void showIndexPage(Player player, Npc npc)
	{
		final int soulRingLevel = player.getVariables().getInt(SOUL_RING_VAR, 0);
		final int usedPoints = getTotalUsedPoints(player);

		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/SoulRingAbility/index.htm");
		html.replace("%soul_ring_level%", String.valueOf(soulRingLevel));
		html.replace("%used_points%", String.valueOf(usedPoints));
		html.replace("%total_points%", String.valueOf(soulRingLevel));
		html.replace("%remaining_points%", String.valueOf(soulRingLevel - usedPoints));
		player.sendPacket(html);
	}

	private void showCategoryPage(Player player, Npc npc, String categoryId)
	{
		final CategoryConfig cat = SoulRingAbilityData.getInstance().getCategory(categoryId);
		if (cat == null)
		{
			showIndexPage(player, npc);
			return;
		}

		final int soulRingLevel = player.getVariables().getInt(SOUL_RING_VAR, 0);
		final int usedPoints = getTotalUsedPoints(player);

		final String htmFile;
		switch (categoryId)
		{
			case "pve_normal": htmFile = "data/scripts/custom/SoulRingAbility/pve_normal.htm"; break;
			case "pve_raid":   htmFile = "data/scripts/custom/SoulRingAbility/pve_raid.htm";   break;
			case "pvp":        htmFile = "data/scripts/custom/SoulRingAbility/pvp.htm";        break;
			case "special":    htmFile = "data/scripts/custom/SoulRingAbility/special.htm";    break;
			default:
				showIndexPage(player, npc);
				return;
		}

		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, htmFile);
		html.replace("%soul_ring_level%", String.valueOf(soulRingLevel));
		html.replace("%used_points%", String.valueOf(usedPoints));
		html.replace("%total_points%", String.valueOf(soulRingLevel));
		html.replace("%remaining_points%", String.valueOf(soulRingLevel - usedPoints));

		// 特殊 placeholder 對應
		final String listPlaceholder;
		switch (categoryId)
		{
			case "pve_normal": listPlaceholder = "%pve_normal_list%"; break;
			case "pve_raid":   listPlaceholder = "%pve_raid_list%";   break;
			case "pvp":        listPlaceholder = "%pvp_list%";        break;
			case "special":    listPlaceholder = "%special_list%";    break;
			default:           listPlaceholder = "%list%";            break;
		}

		html.replace(listPlaceholder, buildStatList(player, cat.stats, soulRingLevel, usedPoints));
		player.sendPacket(html);
	}

	/** 依 varName 判斷應回哪個分類頁 */
	private void showCategoryPageByVar(Player player, Npc npc, String varName)
	{
		for (CategoryConfig cat : SoulRingAbilityData.getInstance().getCategories())
		{
			for (StatConfig sc : cat.stats)
			{
				if (sc.varName.equals(varName))
				{
					showCategoryPage(player, npc, cat.id);
					return;
				}
			}
		}
		showIndexPage(player, npc);
	}

	private void showResetConfirm(Player player, Npc npc)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/SoulRingAbility/reset.htm");
		player.sendPacket(html);
	}

	// ==================== 建立能力列表 HTML ====================

	private String buildStatList(Player player, List<StatConfig> stats, int soulRingLevel, int totalUsed)
	{
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < stats.size(); i++)
		{
			final StatConfig sc = stats.get(i);
			final int points = player.getVariables().getInt(sc.varName, 0);
			final double bonus = points * sc.percentPerPoint;
			final String varInput = "amt" + i;

			// 名稱 + 當前數值行
			sb.append("<table width=\"280\" border=\"0\"><tr>");
			sb.append("<td align=\"center\" height=\"22\">");
			sb.append("<font color=\"LEVEL\">").append(sc.name).append("</font>  ");
			sb.append("<font color=\"00FF00\">").append(points).append("</font>點");
			sb.append("  (<font color=\"FFFF00\">+").append(String.format("%.1f", bonus)).append("%</font>)");
			if (sc.pointCost > 1)
			{
				sb.append("  <font color=\"AAAAAA\">每點消耗").append(sc.pointCost).append("魂環點</font>");
			}
			sb.append("</td>");
			sb.append("</tr></table>");

			// [ - ] 輸入框 [ + ] 行
			sb.append("<table width=\"280\" border=\"0\"><tr height=\"28\">");

			// 減少按鈕
			sb.append("<td width=\"55\" align=\"center\">");
			if (points > 0)
			{
				sb.append("<button value=\"-\" action=\"bypass -h Quest SoulRingAbility subN_")
					.append(sc.varName).append(" $").append(varInput)
					.append("\" width=\"40\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			}
			else
			{
				sb.append("<button value=\"-\" action=\"\" width=\"40\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF_Grayed\">");
			}
			sb.append("</td>");

			// 輸入框
			sb.append("<td width=\"170\" align=\"center\">");
			sb.append("<edit var=\"").append(varInput).append("\" width=\"140\" height=\"16\" maxlen=\"5\">");
			sb.append("</td>");

			// 增加按鈕（需有足夠點數且未達上限）
			sb.append("<td width=\"55\" align=\"center\">");
			final boolean canAdd = (points < sc.maxPoints) && ((soulRingLevel - totalUsed) >= sc.pointCost);
			if (canAdd)
			{
				sb.append("<button value=\"+\" action=\"bypass -h Quest SoulRingAbility addN_")
					.append(sc.varName).append(" $").append(varInput)
					.append("\" width=\"40\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			}
			else
			{
				sb.append("<button value=\"+\" action=\"\" width=\"40\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF_Grayed\">");
			}
			sb.append("</td>");

			sb.append("</tr></table>");
			sb.append("<img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		}
		return sb.toString();
	}

	// ==================== 加減點邏輯 ====================

	private void addPoints(Player player, StatConfig sc, int amount)
	{
		final int soulRingLevel = player.getVariables().getInt(SOUL_RING_VAR, 0);
		final int totalUsed = getTotalUsedPoints(player);
		final int currentPoints = player.getVariables().getInt(sc.varName, 0);
		final int remaining = soulRingLevel - totalUsed;

		// 剩餘點數能買幾點
		final int canAddByTotal = remaining / sc.pointCost;
		// 距離上限還能加幾點
		final int canAddByCap = sc.maxPoints - currentPoints;
		final int canAdd = Math.min(canAddByTotal, canAddByCap);

		if (canAdd <= 0)
		{
			if (remaining < sc.pointCost)
			{
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "魂環點數不足（需要 " + sc.pointCost + " 點才能加 1 點能力）"));
			}
			else
			{
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "該能力已達到最大值"));
			}
			return;
		}

		final int actualAdd = Math.min(amount, canAdd);
		player.getVariables().set(sc.varName, currentPoints + actualAdd);

		if (actualAdd < amount)
		{
			player.sendMessage("已加 " + actualAdd + " 點（受點數上限或能力上限限制，已自動調整）");
		}
	}

	private void removePoints(Player player, StatConfig sc, int amount)
	{
		final int currentPoints = player.getVariables().getInt(sc.varName, 0);

		if (currentPoints <= 0)
		{
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "該能力點數已為0"));
			return;
		}

		final int actualRemove = Math.min(amount, currentPoints);
		player.getVariables().set(sc.varName, currentPoints - actualRemove);

		if (actualRemove < amount)
		{
			player.sendMessage("已扣 " + actualRemove + " 點（已到達0，無法再扣）");
		}
	}

	private void resetAllPoints(Player player)
	{
		for (CategoryConfig cat : SoulRingAbilityData.getInstance().getCategories())
		{
			for (StatConfig sc : cat.stats)
			{
				player.getVariables().remove(sc.varName);
			}
		}
	}

	/**
	 * 計算玩家已消耗的總魂環點數。
	 * 每個能力實際消耗 = rawPoints × pointCost。
	 */
	private int getTotalUsedPoints(Player player)
	{
		int total = 0;
		for (CategoryConfig cat : SoulRingAbilityData.getInstance().getCategories())
		{
			for (StatConfig sc : cat.stats)
			{
				final int pts = player.getVariables().getInt(sc.varName, 0);
				total += pts * sc.pointCost;
			}
		}
		return total;
	}

	public static void main(String[] args)
	{
		new SoulRingAbility();
		System.out.println("【系統】魂環能力分配系統載入完畢！");
	}
}
