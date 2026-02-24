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

	// [自定義修改] BOSS清單定義（只保留 npcId 和顯示名稱）
	// 座標和存活狀態全部從 npc_respawns 資料庫讀取
	private static final int[][] BOSS_LIST =
	{
		{50001}, {50002}, {50003}, {50004}, {50005},
		{50006}, {50007}, {50008}, {50009}, {50010},
		{50011}, {50012}, {50013}, {50014}, {50015},
		{50016}, {50017}, {50018}, {50019}, {50020},
		{50021}, {50022}, {50023}, {50024}, {50025},
		{50026}, {50027}, {50028}, {50029}, {50030},
		{50031}, {50032}, {50033}, {50034}, {50035}
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
	 * BOSS 狀態資訊（優先從世界實體取得，fallback 到資料庫）
	 */
	private static class BossInfo
	{
		public final int npcId;
		public final Location location;     // 即時座標（來自世界實體）或重生點座標（來自DB）
		public final long respawnTime;       // DB 中的重生時間戳
		public final boolean existsInDb;     // 資料庫中是否有記錄
		public final boolean existsInWorld;  // 世界中是否存在活著的實體

		/** 從世界實體建立（確定存活） */
		public BossInfo(int npcId, Npc npc, long respawnTime)
		{
			this.npcId = npcId;
			this.location = npc.getLocation();
			this.respawnTime = respawnTime;
			this.existsInDb = true;
			this.existsInWorld = true;
		}

		/** 從資料庫建立（世界中找不到實體） */
		public BossInfo(int npcId, int x, int y, int z, long respawnTime)
		{
			this.npcId = npcId;
			this.location = new Location(x, y, z);
			this.respawnTime = respawnTime;
			this.existsInDb = true;
			this.existsInWorld = false;
		}

		/** 資料庫中無此BOSS記錄 */
		public BossInfo(int npcId)
		{
			this.npcId = npcId;
			this.location = null;
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
			String bossName = BOSS_NAMES[i];
			int rowNum = (i - startIndex) + 1;
			String rowColor = (rowNum % 2 == 0) ? "222222" : "111111";

			// [自定義修改] 優先從世界實體偵測BOSS，fallback到資料庫
			BossInfo info = loadBossInfo(npcId);

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
	 * 優先從遊戲世界中尋找活著的NPC實體（最可靠），找不到才 fallback 到資料庫
	 * @param npcId BOSS ID
	 * @return BossInfo 包含即時座標、重生時間、存活狀態
	 */
	private static BossInfo loadBossInfo(int npcId)
	{
		// 先從資料庫讀取基本資訊（重生時間、重生點座標）
		long dbRespawnTime = 0;
		int dbX = 0, dbY = 0, dbZ = 0;
		boolean existsInDb = false;

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement statement = con.prepareStatement("SELECT x, y, z, respawnTime FROM npc_respawns WHERE id = ?"))
		{
			statement.setInt(1, npcId);

			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					dbX = rset.getInt("x");
					dbY = rset.getInt("y");
					dbZ = rset.getInt("z");
					dbRespawnTime = rset.getLong("respawnTime");
					existsInDb = true;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		if (!existsInDb)
		{
			return new BossInfo(npcId);
		}

		// 從世界中尋找活著的NPC實體
		Npc worldNpc = World.getInstance().getNpc(npcId);
		if ((worldNpc != null) && !worldNpc.isDead() && (worldNpc.getInstanceId() == 0))
		{
			// 世界中找到活著的實體（排除副本內的同ID怪物）
			return new BossInfo(npcId, worldNpc, dbRespawnTime);
		}

		// 世界中找不到，使用資料庫座標
		return new BossInfo(npcId, dbX, dbY, dbZ, dbRespawnTime);
	}

	/**
	 * [自定義修改] 取得BOSS傳送座標（供 BossTeleportHandler 使用）
	 * 優先取得世界中活著的NPC即時座標，fallback到資料庫重生點座標
	 * @param npcId BOSS ID
	 * @return 傳送位置,如果資料庫無記錄返回 null
	 */
	public static Location getBossLocation(int npcId)
	{
		BossInfo info = loadBossInfo(npcId);
		if (info.existsInDb)
		{
			return info.location;
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
		BossInfo info = loadBossInfo(npcId);
		return info.isAlive();
	}
}
