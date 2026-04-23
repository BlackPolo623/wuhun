package custom.PlayerBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.ItemData;
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
			{57, 100000000},
			{92476, 10000}
	};

	private static final Object[][] BOSS_LIST = {
			// { 名稱, npcId, 票數, 分組 }
			{"初階女王芙蕾雅",       18933, 100, "特殊"},
			{"中階女王芙蕾雅",       18934, 150, "特殊"},
			{"高階女王芙蕾雅",       18935, 200, "特殊"},
			{"火龍巴拉卡斯",       29191, 100, "特殊"},
			{"蕾歐娜-一號",          50041,  10, "蕾歐娜"},
			{"蕾歐娜-二號",          50042,  10, "蕾歐娜"},
			{"蕾歐娜-三號",          50043,  10, "蕾歐娜"},
			{"蕾歐娜-四號",          50044,  10, "蕾歐娜"},
			{"冰凍君主-初級",        29136,   2, "冰凍君主"},
			{"冰凍君主-中級",        29137,   3, "冰凍君主"},
			{"冰凍君主-高級",        29138,  36, "冰凍君主"},
			{"冰凍君主-頂級",        29139,  54, "冰凍君主"},
			{"實驗體一號",        50001,   1, "實驗體"},
			{"實驗體二號",        50002,   1, "實驗體"},
			{"實驗體三號",        50003,   1, "實驗體"},
			{"實驗體四號",        50004,   1, "實驗體"},
			{"實驗體五號",        50005,   1, "實驗體"},
			{"實驗體六號",        50006,   1, "實驗體"},
			{"實驗體七號",        50007,   1, "實驗體"},
			{"實驗體八號",        50008,   1, "實驗體"},
			{"實驗體九號",        50009,   1, "實驗體"},
			{"實驗體十號",        50010,   1, "實驗體"},
			{"實驗體十一號",      50011,   1, "實驗體"},
			{"實驗體十二號",      50012,   1, "實驗體"},
			{"實驗體十三號",      50013,   1, "實驗體"},
			{"實驗體十四號",      50014,   1, "實驗體"},
			{"實驗體十五號",      50015,   1, "實驗體"},
			{"實驗體十六號",      50016,   1, "實驗體"},
			{"實驗體十七號",      50017,   1, "實驗體"},
			{"實驗體十八號",      50018,   1, "實驗體"},
			{"實驗體十九號",      50019,   1, "實驗體"},
			{"實驗體二十號",      50020,   1, "實驗體"},
			{"實驗體二十一號",    50021,   1, "實驗體"},
			{"實驗體二十二號",    50022,   1, "實驗體"},
			{"實驗體二十三號",    50023,   1, "實驗體"},
			{"實驗體二十四號",    50024,   1, "實驗體"},
			{"實驗體二十五號",    50025,   1, "實驗體"},
			{"實驗體二十六號",    50026,   1, "實驗體"},
			{"實驗體二十七號",    50027,   1, "實驗體"},
			{"實驗體二十八號",    50028,   1, "實驗體"},
			{"實驗體二十九號",    50029,   1, "實驗體"},
			{"實驗體三十號",      50030,   1, "實驗體"},
			{"實驗體三十一號",    50031,   1, "實驗體"},
			{"實驗體三十二號",    50032,   1, "實驗體"},
			{"實驗體三十三號",    50033,   1, "實驗體"},
			{"實驗體三十四號",    50034,   1, "實驗體"},
			{"實驗體三十五號",    50035,   1, "實驗體"},
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

		if (event.equals("main_page"))
		{
			return showMainPage(player);
		}
		else if (event.equals("show_boss_list"))
		{
			// Open boss page on the first category
			return showBossPage(player, (String) BOSS_LIST[0][3]);
		}
		else if (event.startsWith("boss_cat_"))
		{
			return showBossPage(player, event.substring("boss_cat_".length()));
		}
		else if (event.startsWith("craft_ticket_confirm "))
		{
			try
			{
				int amount = Integer.parseInt(event.substring(21).trim());
				return handleCraftTicketConfirm(player, amount);
			}
			catch (NumberFormatException e)
			{
				player.sendMessage("請輸入有效的數量！");
				return showMainPage(player);
			}
		}
		else if (event.startsWith("summon_boss "))
		{
			// format: "summon_boss <category> <index>"
			final String[] parts = event.substring("summon_boss ".length()).split(" ", 2);
			if (parts.length == 2)
			{
				try
				{
					final String retCat = parts[0];
					final int bossIndex = Integer.parseInt(parts[1]);
					return handleSummonBoss(player, bossIndex, retCat);
				}
				catch (NumberFormatException ignored)
				{
				}
			}
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

	private int calcMaxCraft(Player player)
	{
		int maxCraft = Integer.MAX_VALUE;
		for (int[] material : CRAFT_MATERIALS)
		{
			long playerCount = player.getInventory().getInventoryItemCount(material[0], 0);
			int canCraft = (int) (playerCount / material[1]);
			maxCraft = Math.min(maxCraft, canCraft);
		}
		return (maxCraft == Integer.MAX_VALUE) ? 0 : maxCraft;
	}

	private String handleCraftTicketConfirm(Player player, int amount)
	{
		// 再次確認權限（雙重保險）
		if (!isBaseOwner(player))
		{
			player.sendMessage("只有基地主人才能製作BOSS召喚券！");
			return null;
		}

		if (amount <= 0)
		{
			player.sendMessage("製作數量必須大於 0！");
			return showMainPage(player);
		}

		int maxCraft = calcMaxCraft(player);

		if (maxCraft <= 0)
		{
			player.sendMessage("材料不足，無法製作！");
			return showMainPage(player);
		}

		if (amount > maxCraft)
		{
			player.sendMessage("材料不足！最多可製作 " + maxCraft + " 張，您輸入了 " + amount + " 張。");
			return showMainPage(player);
		}

		for (int[] material : CRAFT_MATERIALS)
		{
			takeItems(player, material[0], (long) material[1] * amount);
		}

		giveItems(player, SUMMON_TICKET_ID, amount);

		player.sendMessage("========================================");
		player.sendMessage("成功製作BOSS召喚券 x" + amount + " 張！");
		player.sendMessage("========================================");

		return showMainPage(player);
	}

	private String handleSummonBoss(Player player, int bossIndex, String retCategory)
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

		return showBossPage(player, retCategory);
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
		html.replace("%max_craft%", String.valueOf(calcMaxCraft(player)));

		StringBuilder materialList = new StringBuilder();
		for (int[] material : CRAFT_MATERIALS)
		{
			String itemName = ItemData.getInstance().getTemplate(material[0]).getName();
			materialList.append(itemName).append(" x").append(material[1]).append("<br1>");
		}
		html.replace("%material_list%", materialList.toString());

		player.sendPacket(html);
		return null;
	}

	private String showBossPage(Player player, String activeCategory)
	{
		if (!isBaseOwner(player))
		{
			return showLockedPage(player);
		}
		if (!hasBossPermission(player))
		{
			return showLockedPage(player);
		}

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/PlayerBase/boss_summon.htm");

		long ticketCount = player.getInventory().getInventoryItemCount(SUMMON_TICKET_ID, 0);
		html.replace("%ticket_count%", String.valueOf(ticketCount));

		// ── Collect ordered unique categories ──────────────────────────────────
		final java.util.List<String> categories = new java.util.ArrayList<>();
		for (Object[] boss : BOSS_LIST)
		{
			final String cat = (String) boss[3];
			if (!categories.contains(cat))
			{
				categories.add(cat);
			}
		}

		// Validate activeCategory falls back to first if unknown
		if (!categories.contains(activeCategory))
		{
			activeCategory = categories.get(0);
		}

		final StringBuilder content = new StringBuilder();

		// ── Tab row (same pattern as AttributeEnhance slot tabs) ───────────────
		content.append("<table width=280 cellpadding=2 cellspacing=0><tr>");
		for (String cat : categories)
		{
			if (cat.equals(activeCategory))
			{
				content.append("<td align=center><font color=\"LEVEL\">").append(cat).append("</font></td>");
			}
			else
			{
				content.append("<td align=center><a action=\"bypass -h Quest BaseBossNpc boss_cat_")
				       .append(cat).append("\">").append(cat).append("</a></td>");
			}
		}
		content.append("</tr></table>");

		content.append("<table border=0><tr><td height=4></td></tr></table>");

		// ── Boss buttons for active category ───────────────────────────────────
		final boolean twoCol = activeCategory.equals("實驗體");

		if (twoCol)
		{
			// 2-column layout: button text = "召喚 NAME"
			content.append("<table width=280 border=0 cellpadding=2 cellspacing=1>");
			int col = 0;
			for (int i = 0; i < BOSS_LIST.length; i++)
			{
				final Object[] boss = BOSS_LIST[i];
				if (!boss[3].equals(activeCategory))
				{
					continue;
				}
				final String name = (String) boss[0];
				if (col == 0)
				{
					content.append("<tr>");
				}
				content.append("<td width=138 align=center>")
				       .append("<button value=\"").append(name)
				       .append("\" action=\"bypass -h Quest BaseBossNpc summon_boss ").append(activeCategory).append(" ").append(i)
				       .append("\" width=128 height=22 back=L2UI_CT1.Button_DF fore=L2UI_CT1.Button_DF>")
				       .append("</td>");
				col++;
				if (col == 2)
				{
					content.append("</tr>");
					col = 0;
				}
			}
			if (col == 1)
			{
				content.append("<td width=138></td></tr>");
			}
			content.append("</table>");
		}
		else
		{
			// Single-column layout: button text = "召喚 NAME (X張)"
			content.append("<table width=280 border=0 cellpadding=2 cellspacing=1>");
			for (int i = 0; i < BOSS_LIST.length; i++)
			{
				final Object[] boss = BOSS_LIST[i];
				if (!boss[3].equals(activeCategory))
				{
					continue;
				}
				final String name = (String) boss[0];
				final int cost = (int) boss[2];
				content.append("<tr><td align=center>")
				       .append("<button value=\"").append(name).append("（").append(cost).append("張）")
				       .append("\" action=\"bypass -h Quest BaseBossNpc summon_boss ").append(activeCategory).append(" ").append(i)
				       .append("\" width=260 height=22 back=L2UI_CT1.Button_DF fore=L2UI_CT1.Button_DF>")
				       .append("</td></tr>");
			}
			content.append("</table>");
		}

		html.replace("%boss_content%", content.toString());
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