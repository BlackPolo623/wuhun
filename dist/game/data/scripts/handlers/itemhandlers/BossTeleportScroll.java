package handlers.itemhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class BossTeleportScroll implements IItemHandler
{
	private static final int BOSSES_PER_PAGE = 10;
	private static final int MIN_BOSS_ID = 50001;
	private static final int MAX_BOSS_ID = 50035;

	// BOSS 重生位置配置表
	private static final BossLocation[] BOSS_LOCATIONS =
			{
					new BossLocation(50001, 98194, 7095, -3696, "怪獸一號"),
					new BossLocation(50002, 15740, 123435, -3680, "怪獸二號"),
					new BossLocation(50003, 77203, 8040, -3384, "怪獸三號"),
					new BossLocation(50004, 83007, 61463, -3496, "怪獸四號"),
					new BossLocation(50005, 90776, 69111, -3480, "怪獸五號"),
					new BossLocation(50006, 91450, 110356, -3800, "怪獸六號"),
					new BossLocation(50007, 96618, 110316, -3720, "怪獸七號"),
					new BossLocation(50008, 95713, 28333, -3644, "怪獸八號"),
					new BossLocation(50009, 108481, 27188, -3480, "怪獸九號"),
					new BossLocation(50010, 95668, 20366, -3200, "怪獸十號"),
					new BossLocation(50011, 101758, 26798, -3384, "怪獸十一號"),
					new BossLocation(50012, -47333, 141058, -2936, "怪獸十二號"),
					new BossLocation(50013, -51520, 135521, -2944, "怪獸十三號"),
					new BossLocation(50014, -90595, 106072, -3672, "怪獸十四號"),
					new BossLocation(50015, -95162, 110310, -3816, "怪獸十五號"),
					new BossLocation(50016, 125787, 126452, -3939, "怪獸十六號"),
					new BossLocation(50017, 185059, -9610, -5488, "怪獸十七號"),
					new BossLocation(50018, -6675, 18505, -5488, "怪獸十八號"),
					new BossLocation(50019, 49626, 220167, -3592, "怪獸十九號"),
					new BossLocation(50020, -53267, 128461, -3088, "怪獸二十號"),
					new BossLocation(50021, -56716, 118924, -3032, "怪獸二十一號"),
					new BossLocation(50022, 130974, 114453, -3728, "怪獸二十二號"),
					new BossLocation(50023, 26634, 183290, -3368, "怪獸二十三號"),
					new BossLocation(50024, 190414, 18382, -3720, "怪獸二十四號"),
					new BossLocation(50025, 190014, 22378, -3720, "怪獸二十五號"),
					new BossLocation(50026, 192414, 18382, -3720, "怪獸二十六號"),
					new BossLocation(50027, 187489, 20482, -3600, "怪獸二十七號"),
					new BossLocation(50028, 168559, -50182, -3480, "怪獸二十八號"),
					new BossLocation(50029, 169039, -43177, -3488, "怪獸二十九號"),
					new BossLocation(50030, 109310, -152650, -1664, "怪獸三十號"),
					new BossLocation(50031, 116196, -155476, -1512, "怪獸三十一號"),
					new BossLocation(50032, 120536, -159395, -1536, "怪獸三十二號"),
					new BossLocation(50033, -44269, 172740, -3536, "怪獸三十三號"),
					new BossLocation(50034, -40724, 177799, -3976, "怪獸三十四號"),
					new BossLocation(50035, -42758, 175758, -3712, "怪獸三十五號")
			};

	// BOSS 位置數據類
	private static class BossLocation
	{
		public final int npcId;
		public final int x;
		public final int y;
		public final int z;
		public final String name;

		public BossLocation(int npcId, int x, int y, int z, String name)
		{
			this.npcId = npcId;
			this.x = x;
			this.y = y;
			this.z = z;
			this.name = name;
		}

		public Location getLocation()
		{
			return new Location(x, y, z);
		}
	}

	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
			return false;
		}

		final Player player = playable.asPlayer();
		showBossList(player, 1);
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return true;
	}

	public static void showBossList(Player player, int page)
	{
		if (page < 1)
		{
			page = 1;
		}

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		StringBuilder sb = new StringBuilder();

		int totalBosses = BOSS_LOCATIONS.length;
		int totalPages = Math.max(1, (totalBosses + BOSSES_PER_PAGE - 1) / BOSSES_PER_PAGE);

		if (page > totalPages)
		{
			page = totalPages;
		}

		if (page < 1)
		{
			page = 1;
		}

		int startIndex = (page - 1) * BOSSES_PER_PAGE;
		int endIndex = Math.min(startIndex + BOSSES_PER_PAGE, totalBosses);

		sb.append("<html><head><title>實驗體首領傳送卷軸</title></head><body><center>");
		sb.append("<font color=\"FFFF00\">選擇傳送將消耗一張卷軸</font><br><br>");

		sb.append("<table width=\"280\" border=\"0\" cellspacing=\"1\" cellpadding=\"2\" bgcolor=\"111111\">");
		sb.append("<tr bgcolor=\"333333\">");
		sb.append("<td width=\"160\" align=\"center\"><font color=\"FFCC33\">BOSS名稱</font></td>");
		sb.append("<td width=\"60\" align=\"center\"><font color=\"FFCC33\">狀態</font></td>");
		sb.append("<td width=\"60\" align=\"center\"><font color=\"FFCC33\">操作</font></td>");
		sb.append("</tr>");

		for (int i = startIndex; i < endIndex; i++)
		{
			BossLocation boss = BOSS_LOCATIONS[i];
			int rowNum = (i - startIndex) + 1;
			String rowColor = (rowNum % 2 == 0) ? "222222" : "111111";

			// 從資料庫檢查 BOSS 是否存活
			boolean isAlive = isBossAlive(boss.npcId);

			// 獲取 NPC 等級
			int level = 1;
			try
			{
				level = NpcData.getInstance().getTemplate(boss.npcId).getLevel();
			}
			catch (Exception e)
			{
				// 如果取得模板失敗,使用預設等級
			}

			sb.append("<tr bgcolor=\"").append(rowColor).append("\">");
			sb.append("<td width=\"160\"><font color=\"00CCFF\">").append(boss.name).append(" Lv").append(level).append("</font></td>");

			if (isAlive)
			{
				sb.append("<td width=\"60\" align=\"center\"><font color=\"00FF00\">存活</font></td>");
				sb.append("<td width=\"60\" align=\"center\">");
				sb.append("<button value=\"傳送\" action=\"bypass voice .bosstele ").append(boss.npcId).append("\" width=\"50\" height=\"20\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				sb.append("</td>");
			}
			else
			{
				sb.append("<td width=\"60\" align=\"center\"><font color=\"FF0000\">死亡</font></td>");
				sb.append("<td width=\"60\" align=\"center\"><font color=\"777777\">--</font></td>");
			}

			sb.append("</tr>");
		}

		sb.append("</table><br>");

		sb.append("<table width=\"280\"><tr>");

		if (page > 1)
		{
			sb.append("<td width=\"100\" align=\"left\">");
			sb.append("<button value=\"上一頁\" action=\"bypass voice .bosspage ").append(page - 1).append("\" width=\"80\" height=\"20\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			sb.append("</td>");
		}
		else
		{
			sb.append("<td width=\"100\" align=\"left\"></td>");
		}

		sb.append("<td width=\"80\" align=\"center\"><font color=\"AAAAAA\">").append(page).append(" / ").append(totalPages).append("</font></td>");

		if (page < totalPages)
		{
			sb.append("<td width=\"100\" align=\"right\">");
			sb.append("<button value=\"下一頁\" action=\"bypass voice .bosspage ").append(page + 1).append("\" width=\"80\" height=\"20\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			sb.append("</td>");
		}
		else
		{
			sb.append("<td width=\"100\" align=\"right\"></td>");
		}

		sb.append("</tr></table>");
		sb.append("</center></body></html>");

		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	/**
	 * 從資料庫檢查 BOSS 是否存活
	 * @param npcId BOSS ID
	 * @return true 如果已重生(存活), false 如果還在重生倒計時(死亡)
	 */
	private static boolean isBossAlive(int npcId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement statement = con.prepareStatement("SELECT respawnTime FROM npc_respawns WHERE id = ?"))
		{
			statement.setInt(1, npcId);

			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					long respawnTime = rset.getLong("respawnTime");
					// 如果 respawnTime <= 當前時間,表示已經重生(存活)
					return respawnTime <= System.currentTimeMillis();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// 如果資料庫中沒有記錄,預設為存活
		return true;
	}

	/**
	 * 根據 BOSS ID 獲取傳送位置
	 * @param npcId BOSS ID
	 * @return 傳送位置,如果找不到返回 null
	 */
	public static Location getBossLocation(int npcId)
	{
		for (BossLocation boss : BOSS_LOCATIONS)
		{
			if (boss.npcId == npcId)
			{
				return boss.getLocation();
			}
		}
		return null;
	}
}