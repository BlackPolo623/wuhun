package custom.StatViewer;

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
	private StatViewer()
	{
		addStartNpc(StatViewerConfig.NPC_ID);
		addTalkId(StatViewerConfig.NPC_ID);
		addFirstTalkId(StatViewerConfig.NPC_ID);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMain(npc, player);
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "main":
			{
				showMain(npc, player);
				return null;
			}
			case "self":
			{
				showStats(npc, player, player);
				return null;
			}
			case "other_form":
			{
				showOtherForm(npc, player);
				return null;
			}
		}

		if (event.startsWith("search "))
		{
			final String targetName = event.substring(7).trim();

			if (StatViewerConfig.REQUIRE_GM_FOR_OTHERS && !player.isGM())
			{
				sendSimpleMsg(npc, player,
					"<font color=\"FF0000\">您沒有權限查詢其他玩家的屬性。</font>",
					"bypass -h Quest StatViewer main", "返回主選單");
				return null;
			}

			if (targetName.isEmpty())
			{
				sendSimpleMsg(npc, player,
					"<font color=\"FF0000\">請輸入玩家名稱。</font>",
					"bypass -h Quest StatViewer other_form", "返回");
				return null;
			}

			final Player target = World.getInstance().getPlayer(targetName);
			if (target == null)
			{
				sendSimpleMsg(npc, player,
					"<font color=\"FF0000\">找不到玩家「" + targetName + "」，或該玩家目前不在線上。</font>",
					"bypass -h Quest StatViewer other_form", "重新查詢");
				return null;
			}

			showStats(npc, player, target);
			return null;
		}

		return null;
	}

	private void showMain(Npc npc, Player player)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><head><title>屬性查詢系統</title></head><body scroll=\"no\">");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=292 height=358>");
		sb.append("<tr><td valign=\"top\" align=\"center\">");
		sb.append("<table border=0><tr><td height=15></td></tr></table>");
		sb.append("<table border=0 cellpadding=0 cellspacing=0>");
		sb.append("<tr><td align=center><font color=\"LEVEL\" size=\"4\"><b>屬性查詢系統</b></font></td></tr>");
		sb.append("<tr><td height=5></td></tr>");
		sb.append("<tr><td align=center><font color=\"AAAAAA\">查看角色的各項能力數值</font></td></tr>");
		sb.append("<tr><td height=8></td></tr></table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		sb.append("<table border=0><tr><td height=15></td></tr></table>");
		sb.append("<table width=270 border=0>");
		sb.append("<tr><td align=center>");
		sb.append("<button action=\"bypass -h Quest StatViewer self\" value=\"查看自己的屬性\"");
		sb.append(" width=\"200\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
		sb.append("</td></tr>");
		sb.append("<tr><td height=10></td></tr>");
		sb.append("<tr><td align=center>");
		sb.append("<button action=\"bypass -h Quest StatViewer other_form\" value=\"查詢其他玩家屬性\"");
		sb.append(" width=\"200\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
		sb.append("</td></tr></table>");
		sb.append("<table border=0><tr><td height=15></td></tr></table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\">");
		sb.append("</td></tr></table></body></html>");

		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(sb.toString());
		player.sendPacket(msg);
	}

	private void showOtherForm(Npc npc, Player player)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><head><title>查詢其他玩家</title></head><body scroll=\"no\">");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=292 height=358>");
		sb.append("<tr><td valign=\"top\" align=\"center\">");
		sb.append("<table border=0><tr><td height=15></td></tr></table>");
		sb.append("<table border=0 cellpadding=0 cellspacing=0>");
		sb.append("<tr><td align=center><font color=\"LEVEL\" size=\"4\"><b>查詢其他玩家</b></font></td></tr>");
		sb.append("<tr><td height=5></td></tr>");
		sb.append("<tr><td align=center><font color=\"AAAAAA\">輸入玩家名稱（須在線上）</font></td></tr>");
		sb.append("<tr><td height=8></td></tr></table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		sb.append("<table border=0><tr><td height=20></td></tr></table>");
		sb.append("<table width=270 bgcolor=111111 border=0 cellspacing=1 cellpadding=6>");
		sb.append("<tr bgcolor=222222><td align=center>");
		sb.append("<font color=\"FFCC33\">玩家名稱</font><br>");
		sb.append("<table border=0><tr><td height=5></td></tr></table>");
		sb.append("<edit var=\"pname\" width=180 height=15>");
		sb.append("</td></tr></table>");
		sb.append("<table border=0><tr><td height=12></td></tr></table>");
		sb.append("<table width=270 border=0><tr><td align=center>");
		sb.append("<button action=\"bypass -h Quest StatViewer search $pname\" value=\"開始查詢\"");
		sb.append(" width=\"200\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
		sb.append("</td></tr></table>");
		sb.append("<table border=0><tr><td height=15></td></tr></table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		sb.append("<table width=270 cellpadding=3 cellspacing=1><tr><td align=center>");
		sb.append("<button value=\"返回主選單\" action=\"bypass -h Quest StatViewer main\"");
		sb.append(" width=130 height=22 back=\"L2UI_CT1.Button_DF_Small_Down\" fore=\"L2UI_CT1.Button_DF_Small\"/>");
		sb.append("</td></tr></table>");
		sb.append("</td></tr></table></body></html>");

		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(sb.toString());
		player.sendPacket(msg);
	}

	private void showStats(Npc npc, Player viewer, Player target)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><head><title>屬性查詢</title></head><body scroll=\"no\">");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=292 height=358>");
		sb.append("<tr><td valign=\"top\" align=\"center\">");
		sb.append("<table border=0><tr><td height=15></td></tr></table>");

		// 標題
		sb.append("<table border=0 cellpadding=0 cellspacing=0>");
		sb.append("<tr><td align=center><font color=\"LEVEL\" size=\"4\"><b>屬性查詢結果</b></font></td></tr>");
		sb.append("<tr><td height=5></td></tr>");
		sb.append("<tr><td align=center><font color=\"AAAAAA\">").append(target.getName()).append("</font></td></tr>");
		sb.append("<tr><td height=8></td></tr>");
		sb.append("</table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		sb.append("<table border=0><tr><td height=8></td></tr></table>");

		// 屬性資料表
		sb.append("<table width=270 bgcolor=111111 border=0 cellspacing=1 cellpadding=4>");
		sb.append("<tr bgcolor=1A1A2A>");
		sb.append("<td width=160><font color=\"FFCC33\">屬性名稱</font></td>");
		sb.append("<td width=100 align=right><font color=\"FFCC33\">數值</font></td>");
		sb.append("</tr>");

		for (StatViewerConfig.StatEntry entry : StatViewerConfig.STATS)
		{
			try
			{
				final Stat stat = Stat.valueOf(entry.statName);
				final double value = target.getStat().getValue(stat);
				final String display = entry.isPercent
					? String.format("%.2f%%", value * 100)
					: String.format("%.4f", value);

				sb.append("<tr bgcolor=222222>");
				sb.append("<td><font color=\"FFFFFF\">").append(entry.displayName).append("</font></td>");
				sb.append("<td align=right><font color=\"00FF66\">").append(display).append("</font></td>");
				sb.append("</tr>");
			}
			catch (Exception e)
			{
				sb.append("<tr bgcolor=222222>");
				sb.append("<td><font color=\"FFFFFF\">").append(entry.displayName).append("</font></td>");
				sb.append("<td align=right><font color=\"FF6060\">讀取失敗</font></td>");
				sb.append("</tr>");
			}
		}
		sb.append("</table>");

		// 底部按鈕
		sb.append("<table border=0><tr><td height=10></td></tr></table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		sb.append("<table width=270 cellpadding=3 cellspacing=1><tr>");
		sb.append("<td align=center>");
		sb.append("<button value=\"返回主選單\" action=\"bypass -h Quest StatViewer main\"");
		sb.append(" width=130 height=22 back=\"L2UI_CT1.Button_DF_Small_Down\" fore=\"L2UI_CT1.Button_DF_Small\"/>");
		sb.append("</td></tr></table>");

		sb.append("</td></tr></table>");
		sb.append("</body></html>");

		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(sb.toString());
		viewer.sendPacket(msg);
	}

	private void sendSimpleMsg(Npc npc, Player player, String content, String backAction, String backLabel)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><head><title>屬性查詢</title></head><body scroll=\"no\">");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=292 height=358>");
		sb.append("<tr><td valign=\"top\" align=\"center\">");
		sb.append("<table border=0><tr><td height=15></td></tr></table>");
		sb.append("<table border=0 cellpadding=0 cellspacing=0>");
		sb.append("<tr><td align=center><font color=\"LEVEL\" size=\"4\"><b>屬性查詢系統</b></font></td></tr>");
		sb.append("<tr><td height=8></td></tr></table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		sb.append("<table border=0><tr><td height=20></td></tr></table>");
		sb.append("<table width=270 bgcolor=111111 border=0 cellspacing=1 cellpadding=8>");
		sb.append("<tr bgcolor=222222><td align=center>").append(content).append("</td></tr>");
		sb.append("</table>");
		sb.append("<table border=0><tr><td height=15></td></tr></table>");
		sb.append("<img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"><br>");
		sb.append("<table width=270 cellpadding=3 cellspacing=1><tr><td align=center>");
		sb.append("<button value=\"").append(backLabel).append("\" action=\"").append(backAction).append("\"");
		sb.append(" width=130 height=22 back=\"L2UI_CT1.Button_DF_Small_Down\" fore=\"L2UI_CT1.Button_DF_Small\"/>");
		sb.append("</td></tr></table>");
		sb.append("</td></tr></table></body></html>");

		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(sb.toString());
		player.sendPacket(msg);
	}

	public static void main(String[] args)
	{
		new StatViewer();
	}
}
