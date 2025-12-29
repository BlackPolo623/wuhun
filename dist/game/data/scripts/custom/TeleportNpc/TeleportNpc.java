package custom.TeleportNpc;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;


/**
 * 傳送NPC
 */
public class TeleportNpc extends Script
{
	// NPC ID配置
	private static final int TELEPORT_NPC = 900010; // 改成你的NPC ID

	// 傳送點配置 [名稱, X, Y, Z]
	private static final Object[][] TELEPORT_LOCATIONS =
			{
					{"新手村", new Location(-84176, 244573, -3729)},
					{"奇岩城", new Location(115113, -178212, -880)},
					{"狄恩城", new Location(15670, 142983, -2704)},
					{"古魯丁", new Location(-80826, 149775, -3043)},
					{"亞丁城", new Location(147450, 26741, -2204)},
					{"海音斯", new Location(111409, 219364, -3545)},
					{"歐瑞鎮", new Location(82698, 148638, -3464)},
					{"修練場", new Location(83400, 147943, -3404)},
			};

	private TeleportNpc()
	{
		addStartNpc(TELEPORT_NPC);
		addTalkId(TELEPORT_NPC);
		addFirstTalkId(TELEPORT_NPC);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/TeleportNpc/main.html");

		// 動態生成傳送點列表
		final StringBuilder list = new StringBuilder();
		for (int i = 0; i < TELEPORT_LOCATIONS.length; i++)
		{
			String name = (String) TELEPORT_LOCATIONS[i][0];
			list.append("<tr>");
			list.append("<td width=200 align=left><font color=\"LEVEL\">").append(name).append("</font></td>");
			list.append("<td width=92 align=right><button value=\"立即前往\" action=\"bypass -h Quest TeleportNpc teleport_").append(i).append("\" width=75 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			list.append("</tr>");
		}

		html.replace("%list%", list.toString());
		player.sendPacket(html);
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("teleport_"))
		{
			try
			{
				int index = Integer.parseInt(event.replace("teleport_", ""));
				if (index >= 0 && index < TELEPORT_LOCATIONS.length)
				{
					Location loc = (Location) TELEPORT_LOCATIONS[index][1];
					player.teleToLocation(loc, true);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}

	public static void main(String[] args)
	{
		System.out.println("傳送NPC已準備！");
		new TeleportNpc();
	}
}