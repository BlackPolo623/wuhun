/*
 * This file is part of the L2J Mobius project.
 *
 * 掠奪之地 (Plunder Lands) — 公共共享副本
 * 所有玩家共用同一個世界，可以互相看到。
 * 玩家在副本內死亡時，指定道具全數掉落（由 Player.java 處理）。
 * NPC 提供排行榜功能：依副本內玩家持有的指定道具數量排名。
 *
 * Template ID : 900  (需與 PlunderLands.xml 的 id 屬性一致)
 * NPC ID      : 900099
 * 排行道具 ID : 57 (需與 Player.java 的 DROP_ITEM_ID 一致)
 */
package instances.PlunderLands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 掠奪之地 (Plunder Lands) Instance Script
 * @author Custom
 */
public class PlunderLands extends InstanceScript
{
	// ==================== 常數設定 ====================

	/** 副本模板 ID，需與 PlunderLands.xml id 屬性一致 */
	private static final int TEMPLATE_ID = 900;

	/** 入口 NPC ID */
	private static final int NPC_ID = 900039;

	/** 排行榜道具 ID（同時也是 Player.java 死亡掉落的道具 ID） */
	private static final int RANK_ITEM_ID = 57;

	/** 排行榜每頁顯示人數 */
	private static final int PLAYERS_PER_PAGE = 12;

	/** 副本入口傳送座標 */
	private static final Location ENTER_LOCATION = new Location(19057, 225854, -14769);

	// ==================== 建構子 ====================

	public PlunderLands()
	{
		super(TEMPLATE_ID);
		addStartNpc(NPC_ID);
		addFirstTalkId(NPC_ID);
		addTalkId(NPC_ID);
		addInstanceLeaveId(TEMPLATE_ID);
	}

	// ==================== NPC 對話 ====================

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/instances/PlunderLands/main.htm");
		player.sendPacket(html);
		return null;
	}

	// ==================== 事件處理 ====================

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enter":
			{
				// 尋找已存在的公共副本世界
				Instance world = null;
				for (Instance instance : InstanceManager.getInstance().getInstances())
				{
					if (instance.getTemplateId() == TEMPLATE_ID)
					{
						world = instance;
						break;
					}
				}

				// 若尚未建立，則創建新的共享世界
				if (world == null)
				{
					world = InstanceManager.getInstance().createInstance(TEMPLATE_ID, player);
				}

				if (world != null)
				{
					player.teleToLocation(ENTER_LOCATION, world);
				}
				break;
			}
			case "mainPage":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
				html.setFile(player, "data/scripts/instances/PlunderLands/main.htm");
				player.sendPacket(html);
				break;
			}
			default:
			{
				// 處理 showPlayers_頁碼 事件
				if (event.startsWith("showPlayers_"))
				{
					int page = 0;
					try
					{
						page = Integer.parseInt(event.substring("showPlayers_".length()));
					}
					catch (NumberFormatException ignored)
					{
						page = 0;
					}
					showPlayerList(player, page);
				}
				break;
			}
		}
		return null;
	}

	// ==================== 副本離開 ====================

	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		// 公共副本持續存在（empty="-1"），離開時無需特別處理
	}

	// ==================== 排行榜 ====================

	/**
	 * 建立並發送玩家排行榜頁面。
	 * 依副本內玩家持有 RANK_ITEM_ID 的數量由多到少排序，每頁顯示 PLAYERS_PER_PAGE 人。
	 * @param viewer 請求排行榜的玩家
	 * @param page   0-based 頁碼
	 */
	private void showPlayerList(Player viewer, int page)
	{
		// 取得公共副本世界
		Instance world = null;
		for (Instance instance : InstanceManager.getInstance().getInstances())
		{
			if (instance.getTemplateId() == TEMPLATE_ID)
			{
				world = instance;
				break;
			}
		}

		// 收集副本內所有玩家並依道具數量排序（由多到少）
		final List<Player> players = new ArrayList<>();
		if (world != null)
		{
			final Set<Player> worldPlayers = world.getPlayers();
			if (worldPlayers != null)
			{
				players.addAll(worldPlayers);
			}
		}
		players.sort(Comparator.comparingLong((Player p) -> p.getInventory().getInventoryItemCount(RANK_ITEM_ID, -1)).reversed());

		// 分頁計算
		final int totalPlayers = players.size();
		final int totalPages = Math.max(1, (int) Math.ceil((double) totalPlayers / PLAYERS_PER_PAGE));
		if (page < 0)
		{
			page = 0;
		}
		if (page >= totalPages)
		{
			page = totalPages - 1;
		}

		final int start = page * PLAYERS_PER_PAGE;
		final int end = Math.min(start + PLAYERS_PER_PAGE, totalPlayers);

		// 建立 HTML
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table border=0 cellspacing=0 cellpadding=0 width=292 height=358>");
		sb.append("<tr><td>");
		sb.append("<br>");
		sb.append("<center><font color=\"FFCC33\" name=\"hs12\">掠奪之地 — 玩家排行</font></center>");
		sb.append("<br>");

		// 排行表頭
		sb.append("<table border=0 cellspacing=3 cellpadding=0 width=270>");
		sb.append("<tr>");
		sb.append("<td width=25><font color=\"AAAAAA\">#</font></td>");
		sb.append("<td width=155><font color=\"AAAAAA\">玩家名稱</font></td>");
		sb.append("<td width=80><font color=\"AAAAAA\">持有數量</font></td>");
		sb.append("</tr>");

		if (totalPlayers == 0)
		{
			sb.append("<tr><td colspan=3><font color=\"888888\">目前副本內沒有玩家。</font></td></tr>");
		}
		else
		{
			for (int i = start; i < end; i++)
			{
				final Player p = players.get(i);
				final int rank = i + 1;
				final long count = p.getInventory().getInventoryItemCount(RANK_ITEM_ID, -1);

				// 前三名使用特殊顏色
				String nameColor;
				if (rank == 1)
				{
					nameColor = "FFDD00"; // 金
				}
				else if (rank == 2)
				{
					nameColor = "C0C0C0"; // 銀
				}
				else if (rank == 3)
				{
					nameColor = "CD7F32"; // 銅
				}
				else
				{
					nameColor = "FFFFFF";
				}

				sb.append("<tr>");
				sb.append("<td><font color=\"").append(nameColor).append("\">").append(rank).append("</font></td>");
				sb.append("<td><font color=\"").append(nameColor).append("\">").append(p.getName()).append("</font></td>");
				sb.append("<td><font color=\"").append(nameColor).append("\">").append(count).append("</font></td>");
				sb.append("</tr>");
			}
		}

		sb.append("</table>");
		sb.append("<br>");
		sb.append("<center>");

		// 上一頁按鈕
		if (page > 0)
		{
			sb.append("<button action=\"bypass -h Quest PlunderLands showPlayers_").append(page - 1).append("\"");
			sb.append(" value=\"&lt;&lt; 上頁\" width=100 height=26");
			sb.append(" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\"");
			sb.append(" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
		}

		// 下一頁按鈕
		if (page < totalPages - 1)
		{
			sb.append("<button action=\"bypass -h Quest PlunderLands showPlayers_").append(page + 1).append("\"");
			sb.append(" value=\"下頁 &gt;&gt;\" width=100 height=26");
			sb.append(" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\"");
			sb.append(" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
		}

		sb.append("<br>");
		sb.append("<font color=\"888888\">第 ").append(page + 1).append(" / ").append(totalPages).append(" 頁　共 ").append(totalPlayers).append(" 人</font>");
		sb.append("<br><br>");

		// 返回主頁按鈕
		sb.append("<button action=\"bypass -h Quest PlunderLands mainPage\"");
		sb.append(" value=\"返回\" width=110 height=26");
		sb.append(" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\"");
		sb.append(" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");

		sb.append("</center>");
		sb.append("</td></tr>");
		sb.append("</table>");
		sb.append("</body></html>");

		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setHtml(sb.toString());
		viewer.sendPacket(html);
	}

	// ==================== 載入入口 ====================

	public static void main(String[] args)
	{
		new PlunderLands();
		System.out.println("【副本】掠奪之地 (PlunderLands) 載入完畢！Template ID: " + TEMPLATE_ID + ", NPC ID: " + NPC_ID);
	}
}
