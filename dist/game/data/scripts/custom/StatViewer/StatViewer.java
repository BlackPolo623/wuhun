package custom.StatViewer;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.xml.SoulRingAbilityData;
import org.l2jmobius.gameserver.data.xml.SoulRingAbilityData.CategoryConfig;
import org.l2jmobius.gameserver.data.xml.SoulRingAbilityData.StatConfig;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * StatViewer — 屬性查看 NPC
 */
public class StatViewer extends Script
{
	private static final String SELF     = "_self";
	private static final String HTM_PATH = "data/scripts/custom/StatViewer/";

	private StatViewer()
	{
		addStartNpc(StatViewerConfig.NPC_ID);
		addTalkId(StatViewerConfig.NPC_ID);
		addFirstTalkId(StatViewerConfig.NPC_ID);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "main.htm";
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "main":        return "main.htm";
			case "other_form":  return "other_form.htm";
			case "self":
			{
				showStat(npc, player, player, true);
				return null;
			}
		}

		if (event.startsWith("search "))
		{
			return handleSearch(npc, player, event.substring(7).trim());
		}
		if (event.startsWith("view_stat_"))
		{
			return handleView(npc, player, event.substring(10), "stat");
		}
		if (event.startsWith("view_soul_"))
		{
			return handleView(npc, player, event.substring(10), "soul");
		}

		return null;
	}

	private String handleSearch(Npc npc, Player player, String targetName)
	{
		if (StatViewerConfig.REQUIRE_GM_FOR_OTHERS && !player.isGM())
		{
			sendMsg(npc, player, "<font color=\"FF0000\">您沒有權限查詢其他玩家的屬性。</font>",
				"bypass -h Quest StatViewer main", "返回主選單");
			return null;
		}
		if (targetName.isEmpty())
		{
			sendMsg(npc, player, "<font color=\"FF0000\">請輸入玩家名稱。</font>",
				"bypass -h Quest StatViewer other_form", "返回");
			return null;
		}
		final Player target = World.getInstance().getPlayer(targetName);
		if (target == null)
		{
			sendMsg(npc, player, "<font color=\"FF0000\">找不到玩家「" + targetName + "」，或該玩家目前不在線上。</font>",
				"bypass -h Quest StatViewer other_form", "重新查詢");
			return null;
		}
		showStat(npc, player, target, false);
		return null;
	}

	private String handleView(Npc npc, Player player, String key, String tab)
	{
		final boolean isSelf = SELF.equals(key);
		final Player target = isSelf ? player : World.getInstance().getPlayer(key);
		if (target == null)
		{
			sendMsg(npc, player, "<font color=\"FF0000\">玩家已離線，無法繼續查詢。</font>",
				"bypass -h Quest StatViewer other_form", "重新查詢");
			return null;
		}
		if ("soul".equals(tab))
		{
			showSoul(npc, player, target, isSelf);
		}
		else
		{
			showStat(npc, player, target, isSelf);
		}
		return null;
	}

	// ── 分頁標籤列 ──────────────────────────────────────────────────────────

	private String buildTabBar(String curTab, String targetKey)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<table width=270 border=0 cellpadding=0 cellspacing=0><tr>");
		if ("stat".equals(curTab))
		{
			sb.append("<td width=135 align=center><font color=\"LEVEL\"><b>能力</b></font></td>");
		}
		else
		{
			sb.append("<td width=135 align=center><a action=\"bypass -h Quest StatViewer view_stat_").append(targetKey).append("\">能力</a></td>");
		}
		if ("soul".equals(curTab))
		{
			sb.append("<td width=135 align=center><font color=\"LEVEL\"><b>魂環加成</b></font></td>");
		}
		else
		{
			sb.append("<td width=135 align=center><a action=\"bypass -h Quest StatViewer view_soul_").append(targetKey).append("\">魂環加成</a></td>");
		}
		sb.append("</tr></table>");
		return sb.toString();
	}

	// ── 能力頁 ───────────────────────────────────────────────────────────────

	private void showStat(Npc npc, Player viewer, Player target, boolean isSelf)
	{
		final String targetKey = isSelf ? SELF : target.getName();

		// %target_name_row%
		final String nameRow = isSelf ? "" :
			"<tr><td height=3></td></tr><tr><td align=center><font color=\"AAAAAA\">" + target.getName() + "</font></td></tr>";

		// %data_table%
		final StringBuilder tbl = new StringBuilder();
		if (isSelf)
		{
			tbl.append("<table width=270 bgcolor=111111 border=0 cellspacing=1 cellpadding=3>");
			tbl.append("<tr bgcolor=1A1A2A>");
			tbl.append("<td width=160><font color=\"FFCC33\">屬性名稱</font></td>");
			tbl.append("<td width=100><font color=\"FFCC33\">數值</font></td>");
			tbl.append("</tr>");
		}
		else
		{
			tbl.append("<table width=270 bgcolor=111111 border=0 cellspacing=1 cellpadding=3>");
			tbl.append("<tr bgcolor=1A1A2A>");
			tbl.append("<td width=120><font color=\"FFCC33\">屬性名稱</font></td>");
			tbl.append("<td width=70><font color=\"AAFFAA\">自己</font></td>");
			tbl.append("<td width=70><font color=\"FFCC33\">").append(target.getName()).append("</font></td>");
			tbl.append("</tr>");
		}
		for (StatViewerConfig.StatEntry entry : StatViewerConfig.STATS)
		{
			try
			{
				final Stat stat = Stat.valueOf(entry.statName);
				final double tVal = target.getStat().getValue(stat);
				final String tDisp = entry.isPercent ? String.format("%.2f%%", tVal * 100) : String.format("%.4f", tVal);
				tbl.append("<tr bgcolor=222222>");
				tbl.append("<td><font color=\"FFFFFF\">").append(entry.displayName).append("</font></td>");
				if (isSelf)
				{
					tbl.append("<td align=right><font color=\"00FF66\">").append(tDisp).append("</font></td>");
				}
				else
				{
					final double sVal = viewer.getStat().getValue(stat);
					final String sDisp = entry.isPercent ? String.format("%.2f%%", sVal * 100) : String.format("%.4f", sVal);
					tbl.append("<td align=right><font color=\"AAFFAA\">").append(sDisp).append("</font></td>");
					tbl.append("<td align=right><font color=\"00FF66\">").append(tDisp).append("</font></td>");
				}
				tbl.append("</tr>");
			}
			catch (Exception e)
			{
				tbl.append("<tr bgcolor=222222>");
				tbl.append("<td><font color=\"FFFFFF\">").append(entry.displayName).append("</font></td>");
				tbl.append("<td align=right><font color=\"FF6060\">-</font></td>");
				if (!isSelf)
				{
					tbl.append("<td align=right><font color=\"FF6060\">-</font></td>");
				}
				tbl.append("</tr>");
			}
		}
		tbl.append("</table>");

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(viewer, HTM_PATH + "stat.htm"));
		html.replace("%target_name_row%", nameRow);
		html.replace("%tab_bar%",         buildTabBar("stat", targetKey));
		html.replace("%data_table%",      tbl.toString());
		viewer.sendPacket(html);
	}

	// ── 魂環頁 ───────────────────────────────────────────────────────────────

	private void showSoul(Npc npc, Player viewer, Player target, boolean isSelf)
	{
		final String targetKey = isSelf ? SELF : target.getName();

		final String nameRow = isSelf ? "" :
			"<tr><td height=3></td></tr><tr><td align=center><font color=\"AAAAAA\">" + target.getName() + "</font></td></tr>";

		final StringBuilder tables = new StringBuilder();
		for (CategoryConfig cat : SoulRingAbilityData.getInstance().getCategories())
		{
			if (isSelf)
			{
				tables.append("<table width=270 bgcolor=111111 border=0 cellspacing=1 cellpadding=3>");
				tables.append("<tr bgcolor=1A1A2A>");
				tables.append("<td width=160><font color=\"FFCC33\">").append(cat.name).append("</font></td>");
				tables.append("<td width=100><font color=\"FFCC33\">點數</font></td>");
				tables.append("</tr>");
			}
			else
			{
				tables.append("<table width=270 bgcolor=111111 border=0 cellspacing=1 cellpadding=3>");
				tables.append("<tr bgcolor=1A1A2A>");
				tables.append("<td width=150><font color=\"FFCC33\">").append(cat.name).append("</font></td>");
				tables.append("<td width=60><font color=\"AAFFAA\">自己</font></td>");
				tables.append("<td width=60><font color=\"FFCC33\">").append(target.getName()).append("</font></td>");
				tables.append("</tr>");
			}
			for (StatConfig sc : cat.stats)
			{
				final int tPts = target.getVariables().getInt(sc.varName, 0);
				tables.append("<tr bgcolor=222222>");
				tables.append("<td><font color=\"FFFFFF\">").append(sc.name).append("</font></td>");
				if (isSelf)
				{
					tables.append("<td align=right><font color=\"00FF66\">").append(tPts).append("</font></td>");
				}
				else
				{
					final int sPts = viewer.getVariables().getInt(sc.varName, 0);
					tables.append("<td align=right><font color=\"AAFFAA\">").append(sPts).append("</font></td>");
					tables.append("<td align=right><font color=\"00FF66\">").append(tPts).append("</font></td>");
				}
				tables.append("</tr>");
			}
			tables.append("</table>");
			tables.append("<table border=0><tr><td height=4></td></tr></table>");
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(viewer, HTM_PATH + "soul.htm"));
		html.replace("%target_name_row%", nameRow);
		html.replace("%tab_bar%",         buildTabBar("soul", targetKey));
		html.replace("%data_tables%",     tables.toString());
		viewer.sendPacket(html);
	}

	// ── 訊息頁 ───────────────────────────────────────────────────────────────

	private void sendMsg(Npc npc, Player player, String content, String backAction, String backLabel)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(HtmCache.getInstance().getHtm(player, HTM_PATH + "msg.htm"));
		html.replace("%content%",     content);
		html.replace("%back_action%", backAction);
		html.replace("%back_label%",  backLabel);
		player.sendPacket(html);
	}

	public static void main(String[] args)
	{
		new StatViewer();
	}
}
