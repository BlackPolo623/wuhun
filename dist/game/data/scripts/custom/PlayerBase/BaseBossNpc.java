package custom.PlayerBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class BaseBossNpc extends Script
{
	private static final int BOSS_MANAGER_NPC = 900028;
	private static final int SUMMON_TICKET_ID = 105807;

	private static final int[][] CRAFT_MATERIALS = {
			{57, 50000000}
	};

	private static final Object[][] BOSS_LIST = {
			{"冰凍君主-初級", 29136, 2},
			{"冰凍君主-中級", 29137, 3},
			{"冰凍君主-高級", 29138, 36},
			{"冰凍君主-頂級", 29138, 54},
			{"實驗體首領一號", 50001, 1},
			{"實驗體首領二號", 50002, 1},
			{"實驗體首領三號", 50003, 1},
			{"實驗體首領四號", 50004, 1},
			{"實驗體首領五號", 50005, 1},
			{"實驗體首領六號", 50006, 1},
			{"實驗體首領七號", 50007, 1},
			{"實驗體首領八號", 50008, 1},
			{"實驗體首領九號", 50009, 1},
			{"實驗體首領十號", 50010, 1},
			{"實驗體首領十一號", 50011, 1},
			{"實驗體首領十二號", 50012, 1},
			{"實驗體首領十三號", 50013, 1},
			{"實驗體首領十四號", 50014, 1},
			{"實驗體首領十五號", 50015, 1},
			{"實驗體首領十六號", 50016, 1},
			{"實驗體首領十七號", 50017, 1},
			{"實驗體首領十八號", 50018, 1},
			{"實驗體首領十九號", 50019, 1},
			{"實驗體首領二十號", 50020, 1},
			{"實驗體首領二十一號", 50021, 1},
			{"實驗體首領二十二號", 50022, 1},
			{"實驗體首領二十三號", 50023, 1},
			{"實驗體首領二十四號", 50024, 1},
			{"實驗體首領二十五號", 50025, 1},
			{"實驗體首領二十六號", 50026, 1},
			{"實驗體首領二十七號", 50027, 1},
			{"實驗體首領二十八號", 50028, 1},
			{"實驗體首領二十九號", 50029, 1},
			{"實驗體首領三十號", 50030, 1},
			{"實驗體首領三十一號", 50031, 1},
			{"實驗體首領三十二號", 50032, 1},
			{"實驗體首領三十三號", 50033, 1},
			{"實驗體首領三十四號", 50034, 1},
			{"實驗體首領三十五號", 50035, 1}
	};


	public BaseBossNpc()
	{
		addStartNpc(BOSS_MANAGER_NPC);
		addTalkId(BOSS_MANAGER_NPC);
		addFirstTalkId(BOSS_MANAGER_NPC);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		// ========== 檢查是否為基地主人 ==========
		if (!isBaseOwner(player))
		{
			player.sendMessage("========================================");
			player.sendMessage("只有基地主人才能使用BOSS召喚功能！");
			player.sendMessage("訪客無法召喚BOSS");
			player.sendMessage("========================================");
			return showLockedPage(player);
		}

		// 檢查BOSS權限（管理員開通的權限）
		if (!hasBossPermission(player))
		{
			player.sendMessage("您沒有BOSS召喚權限！");
			player.sendMessage("請聯繫管理員開通此功能。");
			return showLockedPage(player);
		}

		if (event.equals("craft_ticket"))
		{
			return handleCraftTicket(player);
		}
		else if (event.startsWith("summon_boss "))
		{
			int bossIndex = Integer.parseInt(event.substring(12));
			return handleSummonBoss(player, bossIndex);
		}

		return null;
	}

	/**
	 * 檢查玩家是否為當前基地的主人
	 */
	private boolean isBaseOwner(Player player)
	{
		Instance instance = player.getInstanceWorld();
		if (instance == null)
		{
			return false;
		}

		Map<String, Object> baseInfo = PlayerBaseDAO.getBaseInfo(player.getObjectId());
		return !baseInfo.isEmpty() && (int) baseInfo.get("instance_id") == instance.getId();
	}

	/**
	 * 檢查玩家是否有BOSS召喚權限（數據庫配置）
	 */
	private boolean hasBossPermission(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "SELECT can_summon_boss FROM player_base WHERE player_id = ?"))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getBoolean("can_summon_boss");
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	private String handleCraftTicket(Player player)
	{
		// 再次確認權限（雙重保險）
		if (!isBaseOwner(player))
		{
			player.sendMessage("只有基地主人才能製作BOSS召喚券！");
			return null;
		}

		for (int[] material : CRAFT_MATERIALS)
		{
			if (player.getInventory().getInventoryItemCount(material[0], 0) < material[1])
			{
				player.sendMessage("材料不足！");
				return showMainPage(player);
			}
		}

		for (int[] material : CRAFT_MATERIALS)
		{
			takeItems(player, material[0], material[1]);
		}

		giveItems(player, SUMMON_TICKET_ID, 1);

		player.sendMessage("========================================");
		player.sendMessage("成功製作BOSS召喚券！");
		player.sendMessage("========================================");

		return showMainPage(player);
	}

	private String handleSummonBoss(Player player, int bossIndex)
	{
		// 再次確認權限（雙重保險）
		if (!isBaseOwner(player))
		{
			player.sendMessage("只有基地主人才能召喚BOSS！");
			return null;
		}

		if (bossIndex < 0 || bossIndex >= BOSS_LIST.length)
		{
			player.sendMessage("無效的BOSS！");
			return null;
		}

		Instance instance = player.getInstanceWorld();
		if (instance == null)
		{
			player.sendMessage("您不在基地中！");
			return null;
		}

		Object[] bossInfo = BOSS_LIST[bossIndex];
		String bossName = (String) bossInfo[0];
		int bossId = (int) bossInfo[1];
		int ticketCost = (int) bossInfo[2];

		if (player.getInventory().getInventoryItemCount(SUMMON_TICKET_ID, 0) < ticketCost)
		{
			player.sendMessage("召喚券不足！需要 " + ticketCost + " 張");
			return null;
		}

		takeItems(player, SUMMON_TICKET_ID, ticketCost);

		// ========== 修改：使用玩家當前位置召喚BOSS ==========
		int playerX = player.getX();
		int playerY = player.getY();
		int playerZ = player.getZ();

		// 在玩家前方100單位召喚BOSS（可選）
		// 根據玩家朝向計算前方位置
		int heading = player.getHeading();
		double radian = Math.toRadians(heading / 182.044444444);
		int offsetX = (int) (100 * Math.cos(radian));
		int offsetY = (int) (100 * Math.sin(radian));

		// 召喚在玩家前方
		addSpawn(bossId, playerX + offsetX, playerY + offsetY, playerZ, 0, false, 0, false, instance.getId());

		// 如果要直接召喚在玩家位置，用這個：
		// addSpawn(bossId, playerX, playerY, playerZ, 0, false, 0, false, instance.getId());

		player.sendMessage("========================================");
		player.sendMessage("成功召喚BOSS：" + bossName);
		player.sendMessage("BOSS已出現在您的前方！");
		player.sendMessage("========================================");

		return showMainPage(player);
	}

	/**
	 * 顯示權限鎖定頁面（非基地主人）
	 */
	private String showLockedPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/PlayerBase/boss_manager_locked.htm");
		player.sendPacket(html);
		return null;
	}

	private String showMainPage(Player player)
	{
		// 檢查是否為基地主人
		if (!isBaseOwner(player))
		{
			return showLockedPage(player);
		}

		// 檢查BOSS權限
		if (!hasBossPermission(player))
		{
			return showLockedPage(player);
		}

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/PlayerBase/boss_manager.htm");

		long ticketCount = player.getInventory().getInventoryItemCount(SUMMON_TICKET_ID, 0);
		html.replace("%ticket_count%", String.valueOf(ticketCount));

		StringBuilder bossList = new StringBuilder();
		for (int i = 0; i < BOSS_LIST.length; i++)
		{
			Object[] bossInfo = BOSS_LIST[i];
			String name = (String) bossInfo[0];
			int cost = (int) bossInfo[2];

			bossList.append("<tr bgcolor=\"222222\">");
			bossList.append("<td width=150 align=center><font color=\"FFCC33\">").append(name).append("</font></td>");
			bossList.append("<td width=60 align=center>").append(cost).append(" 張</td>");
			bossList.append("<td width=70 align=center>");
			bossList.append("<button value=\"召喚\" action=\"bypass -h Quest BaseBossNpc summon_boss ").append(i);
			bossList.append("\" width=60 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			bossList.append("</td></tr>");
		}
		html.replace("%boss_list%", bossList.toString());

		StringBuilder materialList = new StringBuilder();
		for (int[] material : CRAFT_MATERIALS)
		{
			materialList.append("道具ID ").append(material[0]).append(" x").append(material[1]).append("<br1>");
		}
		html.replace("%material_list%", materialList.toString());

		player.sendPacket(html);
		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return showMainPage(player);
	}

	public static void main(String[] args)
	{
		new BaseBossNpc();
		System.out.println("【系統】基地BOSS管理系統載入完畢！");
	}
}