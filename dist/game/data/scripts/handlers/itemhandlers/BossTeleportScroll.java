package handlers.itemhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class BossTeleportScroll implements IItemHandler
{
	private static final int BOSSES_PER_PAGE = 10;

	// [自定義修改] BOSS清單定義（包含固定傳送座標）
	// 座標直接從 RaidbossSpawns.xml 中的重生點座標設定
	private static final int[][] BOSS_LIST =
	{
		{50001, 98194, 7095, -3696},      // 怪獸一號
		{50002, 15740, 123435, -3680},    // 怪獸二號
		{50003, 77203, 8040, -3384},      // 怪獸三號
		{50004, 83007, 61463, -3496},     // 怪獸四號
		{50005, 90776, 69111, -3480},     // 怪獸五號
		{50006, 91450, 110356, -3800},    // 怪獸六號
		{50007, 96618, 110316, -3720},    // 怪獸七號
		{50008, 95713, 28333, -3644},     // 怪獸八號
		{50009, 108481, 27188, -3480},    // 怪獸九號
		{50010, 95668, 20366, -3200},     // 怪獸十號
		{50011, 101758, 26798, -3384},    // 怪獸十一號
		{50012, -47333, 141058, -2936},   // 怪獸十二號
		{50013, -51520, 135521, -2944},   // 怪獸十三號
		{50014, -90595, 106072, -3672},   // 怪獸十四號
		{50015, -95162, 110310, -3816},   // 怪獸十五號
		{50016, 125787, 126452, -3939},   // 怪獸十六號
		{50017, 185059, -9610, -5488},    // 怪獸十七號
		{50018, -6675, 18505, -5488},     // 怪獸十八號
		{50019, 49626, 220167, -3592},    // 怪獸十九號
		{50020, -53267, 128461, -3088},   // 怪獸二十號
		{50021, -56716, 118924, -3032},   // 怪獸二十一號
		{50022, 130974, 114453, -3728},   // 怪獸二十二號
		{50023, 26634, 183290, -3368},    // 怪獸二十三號
		{50024, 190414, 18382, -3720},    // 怪獸二十四號
		{50025, 190014, 22378, -3720},    // 怪獸二十五號
		{50026, 192414, 18382, -3720},    // 怪獸二十六號
		{50027, 187489, 20482, -3600},    // 怪獸二十七號
		{50028, 168559, -50182, -3480},   // 怪獸二十八號
		{50029, 169039, -43177, -3488},   // 怪獸二十九號
		{50030, 109310, -152650, -1664},  // 怪獸三十號
		{50031, 116196, -155476, -1512},  // 怪獸三十一號
		{50032, 120536, -159395, -1536},  // 怪獸三十二號
		{50033, -44269, 172740, -3536},   // 怪獸三十三號
		{50034, -40724, 177799, -3976},   // 怪獸三十四號
		{50035, -42758, 175758, -3712}    // 怪獸三十五號
	};

	private static final String[] BOSS_NAMES =
	{
		"怪獸一號", "怪獸二號", "怪獸三號", "怪獸四號", "怪獸五號",
		"怪獸六號", "怪獸七號", "怪獸八號", "怪獸九號", "怪獸十號",
		"怪獸十一號", "怪獸十二號", "怪獸十三號", "怪獸十四號", "怪獸十五號",
		"怪獸十六號", "怪獸十七號", "怪獸十八號", "怪獸十九號", "怪獸二十號",
		"怪獸二十一號", "怪獸二十二號", "怪獸二十三號", "怪獸二十四號", "怪獸二十五號",
		"怪獸二十六號", "怪獸二十七號", "怪獸二十八號", "怪獸二十九號", "怪獸三十號",
		"怪獸三十一號", "怪獸三十二號", "怪獸三十三號", "怪獸三十四號", "怪獸三十五號"
	};

	/**
	 * BOSS 狀態資訊（從資料庫讀取重生時間，座標使用固定值）
	 */
	private static class BossInfo
	{
		public final int npcId;
		public final Location location;     // 固定重生點座標
		public final long respawnTime;      // DB 中的重生時間戳
		public final boolean existsInDb;    // 資料庫中是否有記錄
		public final boolean existsInWorld; // 世界中是否存在活著的實體

		/** 從固定座標和資料庫資訊建立 */
		public BossInfo(int npcId, int x, int y, int z, long respawnTime, boolean existsInWorld)
		{
			this.npcId = npcId;
			this.location = new Location(x, y, z);
			this.respawnTime = respawnTime;
			this.existsInDb = true;
			this.existsInWorld = existsInWorld;
		}

		/** 資料庫中無此BOSS記錄 */
		public BossInfo(int npcId, int x, int y, int z)
		{
			this.npcId = npcId;
			this.location = new Location(x, y, z);
			this.respawnTime = 0;
			this.existsInDb = false;
			this.existsInWorld = false;
		}

		/**
		 * 判斷BOSS是否存活
		 * 最可靠的方式：世界中是否真的有這個NPC實體
		 */
		public boolean isAlive()
		{
			return existsInWorld;
		}

		/**
		 * 取得剩餘復活時間的格式化字串
		 */
		public String getRemainingTimeString()
		{
			if (isAlive())
			{
				return "存活中";
			}

			long remaining = respawnTime - System.currentTimeMillis();
			if (remaining <= 0)
			{
				return "即將復活";
			}

			long totalSeconds = remaining / 1000;
			long hours = totalSeconds / 3600;
			long minutes = (totalSeconds % 3600) / 60;
			long seconds = totalSeconds % 60;

			if (hours > 0)
			{
				return hours + "時" + minutes + "分";
			}
			else if (minutes > 0)
			{
				return minutes + "分" + seconds + "秒";
			}
			else
			{
				return seconds + "秒";
			}
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

		int totalBosses = BOSS_LIST.length;
		int totalPages = Math.max(1, (totalBosses + BOSSES_PER_PAGE - 1) / BOSSES_PER_PAGE);

		if (page > totalPages)
		{
			page = totalPages;
		}

		int startIndex = (page - 1) * BOSSES_PER_PAGE;
		int endIndex = Math.min(startIndex + BOSSES_PER_PAGE, totalBosses);

		sb.append("<html><head><title>實驗體首領傳送卷軸</title></head><body><center>");
		sb.append("<font color=\"FFFF00\">選擇傳送將消耗一張卷軸</font><br><br>");

		sb.append("<table width=\"300\" border=\"0\" cellspacing=\"1\" cellpadding=\"2\" bgcolor=\"111111\">");
		sb.append("<tr bgcolor=\"333333\">");
		sb.append("<td width=\"130\" align=\"center\"><font color=\"FFCC33\">BOSS名稱</font></td>");
		sb.append("<td width=\"50\" align=\"center\"><font color=\"FFCC33\">狀態</font></td>");
		sb.append("<td width=\"60\" align=\"center\"><font color=\"FFCC33\">復活</font></td>");
		sb.append("<td width=\"60\" align=\"center\"><font color=\"FFCC33\">操作</font></td>");
		sb.append("</tr>");

		for (int i = startIndex; i < endIndex; i++)
		{
			int npcId = BOSS_LIST[i][0];
			int fixedX = BOSS_LIST[i][1];
			int fixedY = BOSS_LIST[i][2];
			int fixedZ = BOSS_LIST[i][3];
			String bossName = BOSS_NAMES[i];
			int rowNum = (i - startIndex) + 1;
			String rowColor = (rowNum % 2 == 0) ? "222222" : "111111";

			// [自定義修改] 使用固定座標，只從資料庫讀取重生時間和存活狀態
			BossInfo info = loadBossInfo(npcId, fixedX, fixedY, fixedZ);

			// 獲取 NPC 等級
			int level = 1;
			try
			{
				level = NpcData.getInstance().getTemplate(npcId).getLevel();
			}
			catch (Exception e)
			{
				// 如果取得模板失敗,使用預設等級
			}

			sb.append("<tr bgcolor=\"").append(rowColor).append("\">");
			sb.append("<td width=\"130\"><font color=\"00CCFF\">").append(bossName).append(" Lv").append(level).append("</font></td>");

			if (!info.existsInDb)
			{
				// 資料庫無記錄（尚未被系統生成過）
				sb.append("<td width=\"50\" align=\"center\"><font color=\"FFFF00\">未知</font></td>");
				sb.append("<td width=\"60\" align=\"center\"><font color=\"777777\">--</font></td>");
				sb.append("<td width=\"60\" align=\"center\"><font color=\"777777\">--</font></td>");
			}
			else if (info.isAlive())
			{
				sb.append("<td width=\"50\" align=\"center\"><font color=\"00FF00\">存活</font></td>");
				sb.append("<td width=\"60\" align=\"center\"><font color=\"00FF00\">--</font></td>");
				sb.append("<td width=\"60\" align=\"center\">");
				sb.append("<button value=\"傳送\" action=\"bypass voice .bosstele ").append(npcId).append("\" width=\"50\" height=\"20\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				sb.append("</td>");
			}
			else
			{
				sb.append("<td width=\"50\" align=\"center\"><font color=\"FF0000\">死亡</font></td>");
				sb.append("<td width=\"60\" align=\"center\"><font color=\"FF8800\">").append(info.getRemainingTimeString()).append("</font></td>");
				sb.append("<td width=\"60\" align=\"center\">");
				sb.append("<button value=\"傳送\" action=\"bypass voice .bosstele ").append(npcId).append("\" width=\"50\" height=\"20\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				sb.append("</td>");
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
	 * [自定義修改] 載入BOSS資訊
	 * 使用固定座標，只從資料庫讀取重生時間，從世界檢查存活狀態
	 * @param npcId BOSS ID
	 * @param fixedX 固定重生點 X 座標
	 * @param fixedY 固定重生點 Y 座標
	 * @param fixedZ 固定重生點 Z 座標
	 * @return BossInfo 包含固定座標、重生時間、存活狀態
	 */
	private static BossInfo loadBossInfo(int npcId, int fixedX, int fixedY, int fixedZ)
	{
		// 從資料庫讀取重生時間
		long dbRespawnTime = 0;
		boolean existsInDb = false;

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement statement = con.prepareStatement("SELECT respawnTime FROM npc_respawns WHERE id = ?"))
		{
			statement.setInt(1, npcId);

			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					dbRespawnTime = rset.getLong("respawnTime");
					existsInDb = true;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// 從世界中檢查是否存活
		boolean existsInWorld = false;
		Npc worldNpc = World.getInstance().getNpc(npcId);
		if ((worldNpc != null) && !worldNpc.isDead() && (worldNpc.getInstanceId() == 0))
		{
			existsInWorld = true;
		}

		if (!existsInDb)
		{
			// 資料庫無記錄，使用固定座標
			return new BossInfo(npcId, fixedX, fixedY, fixedZ);
		}

		// 使用固定座標 + 資料庫重生時間 + 世界存活狀態
		return new BossInfo(npcId, fixedX, fixedY, fixedZ, dbRespawnTime, existsInWorld);
	}

	/**
	 * [自定義修改] 取得BOSS傳送座標（供 BossTeleportHandler 使用）
	 * 使用固定重生點座標，不再追蹤 BOSS 即時位置
	 * @param npcId BOSS ID
	 * @return 固定傳送位置
	 */
	public static Location getBossLocation(int npcId)
	{
		// 從 BOSS_LIST 中查找固定座標
		for (int[] boss : BOSS_LIST)
		{
			if (boss[0] == npcId)
			{
				return new Location(boss[1], boss[2], boss[3]);
			}
		}
		return null;
	}

	/**
	 * [自定義修改] 檢查BOSS是否存活（供 BossTeleportHandler 使用）
	 * @param npcId BOSS ID
	 * @return true 如果世界中存在活著的實體
	 */
	public static boolean isBossAlive(int npcId)
	{
		Npc worldNpc = World.getInstance().getNpc(npcId);
		return (worldNpc != null) && !worldNpc.isDead() && (worldNpc.getInstanceId() == 0);
	}
}
