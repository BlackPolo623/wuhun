package custom.WebsiteLottery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 網站抽獎系統 NPC
 * @author 黑普羅
 */
public class WebsiteLottery extends Script
{
	// NPC ID
	private static final int NPC_ID = 900034;

	// HTML路徑
	private static final String HTML_PATH = "data/scripts/custom/WebsiteLottery/";

	// 積分兌換配置
	private static final int POINT_ITEM_ID = 91663;           // 兌換積分的道具ID (預設: 金幣)
	private static final long POINT_ITEM_COUNT = 1;  // 單位數量 (100萬金幣)
	private static final int POINTS_PER_UNIT = 1;          // 每單位兌換的積分數

	public WebsiteLottery()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("main"))
		{
			return showMainPage(player);
		}
		else if (event.equals("receive_rewards"))
		{
			return showReceiveRewardsPage(player);
		}
		else if (event.equals("view_history"))
		{
			return showHistoryPage(player, 1);
		}
		else if (event.startsWith("history_page_"))
		{
			int page = Integer.parseInt(event.substring("history_page_".length()));
			return showHistoryPage(player, page);
		}
		else if (event.startsWith("receive_"))
		{
			int rewardId = Integer.parseInt(event.substring("receive_".length()));
			return receiveReward(player, rewardId);
		}
		else if (event.equals("receive_all"))
		{
			return receiveAllRewards(player);
		}
		else if (event.equals("point_exchange"))
		{
			return showPointExchangePage(player);
		}
		else if (event.startsWith("exchange_to_web "))
		{
			String amountStr = event.substring("exchange_to_web ".length()).trim();
			try
			{
				int amount = Integer.parseInt(amountStr);
				return exchangeItemToPoints(player, amount);
			}
			catch (NumberFormatException e)
			{
				return showHtmlFile(player, "error.htm", createMap("message", "輸入格式錯誤"));
			}
		}
		else if (event.startsWith("exchange_to_game "))
		{
			String amountStr = event.substring("exchange_to_game ".length()).trim();
			try
			{
				int amount = Integer.parseInt(amountStr);
				return exchangePointsToItem(player, amount);
			}
			catch (NumberFormatException e)
			{
				return showHtmlFile(player, "error.htm", createMap("message", "輸入格式錯誤"));
			}
		}

		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return showMainPage(player);
	}

	/**
	 * 顯示主頁面
	 */
	private String showMainPage(Player player)
	{
		Map<String, String> replacements = new HashMap<>();
		replacements.put("char_id", String.valueOf(player.getObjectId()));
		replacements.put("char_name", player.getName());

		// 檢查綁定狀態和積分
		String boundUsername = getBoundUsername(player);
		int webPoints = 0;
		if (boundUsername != null)
		{
			replacements.put("bind_status", "<font color=\"00FF00\">已綁定</font>");
			replacements.put("bind_info", "<font color=\"LEVEL\">綁定的帳號:</font> <font color=\"FFFF00\">" + boundUsername + "</font>");
			webPoints = getWebPoints(player);
		}
		else
		{
			replacements.put("bind_status", "<font color=\"FF6666\">未綁定</font>");
			replacements.put("bind_info", "<font color=\"FF6666\">請先到網站使用上方的角色專屬ID進行綁定</font>");
		}
		replacements.put("web_points", String.valueOf(webPoints));

		// 檢查待領取獎勵數量
		int pendingCount = getPendingRewardsCount(player);
		replacements.put("pending_count", String.valueOf(pendingCount));

		if (pendingCount > 0)
		{
			replacements.put("reward_notice", "<font color=\"FFFF00\">您有 " + pendingCount + " 個待領取的獎勵！</font>");
		}
		else
		{
			replacements.put("reward_notice", "<font color=\"LEVEL\">目前沒有待領取的獎勵</font>");
		}

		return showHtmlFile(player, "main.htm", replacements);
	}

	/**
	 * 獲取綁定的網站帳號
	 */
	private String getBoundUsername(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT username FROM bound_accounts WHERE char_id = ?"))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getString("username");
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 獲取網站積分
	 */
	private int getWebPoints(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT points FROM bound_accounts WHERE char_id = ?"))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt("points");
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 顯示積分兌換頁面
	 */
	private String showPointExchangePage(Player player)
	{
		// 檢查是否已綁定
		String boundUsername = getBoundUsername(player);
		if (boundUsername == null)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "您尚未綁定網站帳號<br>請先綁定後再使用積分功能"));
		}

		Map<String, String> replacements = new HashMap<>();

		// 獲取網站積分
		int webPoints = getWebPoints(player);
		replacements.put("web_points", String.valueOf(webPoints));

		// 獲取遊戲內道具數量
		long gameItemCount = player.getInventory().getInventoryItemCount(POINT_ITEM_ID, -1);
		replacements.put("game_item_count", formatNumber(gameItemCount));

		// 獲取道具名稱
		ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(POINT_ITEM_ID);
		String itemName = itemTemplate != null ? itemTemplate.getName() : "道具";
		replacements.put("item_name", itemName);

		// 兌換比率資訊
		replacements.put("item_per_unit", formatNumber(POINT_ITEM_COUNT));
		replacements.put("points_per_unit", String.valueOf(POINTS_PER_UNIT));

		return showHtmlFile(player, "point_exchange.htm", replacements);
	}

	/**
	 * 將遊戲道具兌換成網站積分
	 */
	private String exchangeItemToPoints(Player player, int units)
	{
		if (units <= 0)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "請輸入正確的數量"));
		}

		// 檢查是否已綁定
		String boundUsername = getBoundUsername(player);
		if (boundUsername == null)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "您尚未綁定網站帳號"));
		}

		// 計算需要的道具數量
		long totalItemCount = POINT_ITEM_COUNT * units;
		long playerItemCount = player.getInventory().getInventoryItemCount(POINT_ITEM_ID, -1);

		if (playerItemCount < totalItemCount)
		{
			ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(POINT_ITEM_ID);
			String itemName = itemTemplate != null ? itemTemplate.getName() : "道具";
			return showHtmlFile(player, "error.htm", createMap("message",
				"道具數量不足<br>需要: " + formatNumber(totalItemCount) + " " + itemName + "<br>擁有: " + formatNumber(playerItemCount)));
		}

		// 計算獲得的積分
		int pointsToAdd = POINTS_PER_UNIT * units;

		try (Connection con = DatabaseFactory.getConnection())
		{
			// 扣除道具
			if (!player.destroyItemByItemId(ItemProcessType.NONE, POINT_ITEM_ID, totalItemCount, player, true))
			{
				return showHtmlFile(player, "error.htm", createMap("message", "扣除道具失敗"));
			}

			// 增加網站積分
			try (PreparedStatement ps = con.prepareStatement("UPDATE bound_accounts SET points = points + ? WHERE char_id = ?"))
			{
				ps.setInt(1, pointsToAdd);
				ps.setInt(2, player.getObjectId());
				ps.executeUpdate();
			}

			ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(POINT_ITEM_ID);
			String itemName = itemTemplate != null ? itemTemplate.getName() : "道具";

			Map<String, String> replacements = new HashMap<>();
			replacements.put("title", "兌換成功！");
			replacements.put("line1", "消耗 " + formatNumber(totalItemCount) + " " + itemName);
			replacements.put("line2", "獲得 " + pointsToAdd + " 網站積分");

			return showHtmlFile(player, "success.htm", replacements);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			// 如果更新失敗，返還道具
			player.addItem(ItemProcessType.NONE, POINT_ITEM_ID, totalItemCount, player, true);
			return showHtmlFile(player, "error.htm", createMap("message", "資料庫錯誤，請聯繫管理員"));
		}
	}

	/**
	 * 將網站積分兌換成遊戲道具
	 */
	private String exchangePointsToItem(Player player, int units)
	{
		if (units <= 0)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "請輸入正確的數量"));
		}

		// 檢查是否已綁定
		String boundUsername = getBoundUsername(player);
		if (boundUsername == null)
		{
			return showHtmlFile(player, "error.htm", createMap("message", "您尚未綁定網站帳號"));
		}

		// 計算需要的積分
		int pointsNeeded = POINTS_PER_UNIT * units;
		int currentPoints = getWebPoints(player);

		if (currentPoints < pointsNeeded)
		{
			return showHtmlFile(player, "error.htm", createMap("message",
				"積分不足<br>需要: " + pointsNeeded + " 積分<br>擁有: " + currentPoints + " 積分"));
		}

		// 計算獲得的道具數量
		long itemsToGive = POINT_ITEM_COUNT * units;

		try (Connection con = DatabaseFactory.getConnection())
		{
			// 扣除網站積分
			try (PreparedStatement ps = con.prepareStatement("UPDATE bound_accounts SET points = points - ? WHERE char_id = ? AND points >= ?"))
			{
				ps.setInt(1, pointsNeeded);
				ps.setInt(2, player.getObjectId());
				ps.setInt(3, pointsNeeded);
				int updated = ps.executeUpdate();

				if (updated == 0)
				{
					return showHtmlFile(player, "error.htm", createMap("message", "積分不足或扣除失敗"));
				}
			}

			// 給予道具
			player.addItem(ItemProcessType.NONE, POINT_ITEM_ID, itemsToGive, player, true);

			ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(POINT_ITEM_ID);
			String itemName = itemTemplate != null ? itemTemplate.getName() : "道具";

			Map<String, String> replacements = new HashMap<>();
			replacements.put("title", "兌換成功！");
			replacements.put("line1", "消耗 " + pointsNeeded + " 網站積分");
			replacements.put("line2", "獲得 " + formatNumber(itemsToGive) + " " + itemName);

			return showHtmlFile(player, "success.htm", replacements);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return showHtmlFile(player, "error.htm", createMap("message", "資料庫錯誤，請聯繫管理員"));
		}
	}

	/**
	 * 顯示待領取獎勵頁面
	 */
	private String showReceiveRewardsPage(Player player)
	{
		List<RewardData> rewards = getPendingRewards(player);

		StringBuilder rewardList = new StringBuilder();

		if (rewards.isEmpty())
		{
			rewardList.append("<font color=\"LEVEL\">目前沒有待領取的獎勵</font><br>");
		}
		else
		{
			rewardList.append("<table width=280>");
			rewardList.append("<tr><td width=180><font color=\"LEVEL\">獎品名稱</font></td><td width=50><font color=\"LEVEL\">數量</font></td><td width=50></td></tr>");

			for (RewardData reward : rewards)
			{
				rewardList.append("<tr>");
				rewardList.append("<td width=180><font color=\"FFFF00\">").append(reward.itemName).append("</font></td>");
				rewardList.append("<td width=50><font color=\"LEVEL\">").append(reward.itemCount).append("</font></td>");
				rewardList.append("<td width=50><button value=\"領取\" action=\"bypass -h Quest WebsiteLottery receive_").append(reward.id).append("\" width=45 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				rewardList.append("</tr>");
			}

			rewardList.append("</table><br>");

			if (rewards.size() > 1)
			{
				rewardList.append("<button value=\"一鍵領取全部\" action=\"bypass -h Quest WebsiteLottery receive_all\" width=120 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>");
			}
		}

		Map<String, String> replacements = new HashMap<>();
		replacements.put("reward_list", rewardList.toString());
		replacements.put("total_count", String.valueOf(rewards.size()));

		return showHtmlFile(player, "receive_rewards.htm", replacements);
	}

	/**
	 * 顯示歷史記錄頁面
	 */
	private String showHistoryPage(Player player, int page)
	{
		int pageSize = 10;
		int offset = (page - 1) * pageSize;

		List<HistoryData> history = getLotteryHistory(player, offset, pageSize);
		int totalCount = getLotteryHistoryCount(player);
		int totalPages = (int) Math.ceil((double) totalCount / pageSize);

		StringBuilder historyList = new StringBuilder();

		if (history.isEmpty())
		{
			historyList.append("<font color=\"LEVEL\">目前沒有抽獎記錄</font><br>");
		}
		else
		{
			historyList.append("<table width=280>");
			historyList.append("<tr><td width=180><font color=\"LEVEL\">獎品名稱</font></td><td width=100><font color=\"LEVEL\">時間</font></td></tr>");

			for (HistoryData record : history)
			{
				historyList.append("<tr>");
				historyList.append("<td width=180><font color=\"FFFF00\">").append(record.prizeName).append("</font></td>");
				historyList.append("<td width=100><font color=\"LEVEL\">").append(formatDateTime(record.createdAt)).append("</font></td>");
				historyList.append("</tr>");
			}

			historyList.append("</table><br>");

			// 分頁按鈕
			if (totalPages > 1)
			{
				historyList.append("<table width=280><tr>");

				if (page > 1)
				{
					historyList.append("<td width=70><button value=\"上一頁\" action=\"bypass -h Quest WebsiteLottery history_page_").append(page - 1).append("\" width=65 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				}
				else
				{
					historyList.append("<td width=70></td>");
				}

				historyList.append("<td width=140 align=center><font color=\"LEVEL\">第 ").append(page).append(" / ").append(totalPages).append(" 頁</font></td>");

				if (page < totalPages)
				{
					historyList.append("<td width=70><button value=\"下一頁\" action=\"bypass -h Quest WebsiteLottery history_page_").append(page + 1).append("\" width=65 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				}
				else
				{
					historyList.append("<td width=70></td>");
				}

				historyList.append("</tr></table>");
			}
		}

		Map<String, String> replacements = new HashMap<>();
		replacements.put("history_list", historyList.toString());
		replacements.put("total_count", String.valueOf(totalCount));

		return showHtmlFile(player, "history.htm", replacements);
	}

	/**
	 * 領取單個獎勵
	 */
	private String receiveReward(Player player, int rewardId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT item_id, item_name, item_count FROM lottery_rewards WHERE id = ? AND char_id = ? AND received = 0"))
		{
			ps.setInt(1, rewardId);
			ps.setInt(2, player.getObjectId());

			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					int itemId = rs.getInt("item_id");
					String itemName = rs.getString("item_name");
					int itemCount = rs.getInt("item_count");

					// 給予物品
					player.addItem(ItemProcessType.NONE, itemId, itemCount, player, true);

					// 更新資料庫標記為已領取
					try (PreparedStatement updatePs = con.prepareStatement("UPDATE lottery_rewards SET received = 1, received_at = NOW() WHERE id = ?"))
					{
						updatePs.setInt(1, rewardId);
						updatePs.executeUpdate();
					}

					Map<String, String> replacements = new HashMap<>();
					replacements.put("title", "領取成功！");
					replacements.put("line1", "獎品: " + itemName);
					replacements.put("line2", "數量: " + itemCount);

					return showHtmlFile(player, "success.htm", replacements);
				}
				else
				{
					return showHtmlFile(player, "error.htm", createMap("message", "找不到該獎勵或已領取"));
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return showHtmlFile(player, "error.htm", createMap("message", "資料庫錯誤，請聯繫管理員"));
		}
	}

	/**
	 * 一鍵領取所有獎勵
	 */
	private String receiveAllRewards(Player player)
	{
		List<RewardData> rewards = getPendingRewards(player);

		if (rewards.isEmpty())
		{
			return showHtmlFile(player, "error.htm", createMap("message", "沒有可領取的獎勵"));
		}

		int successCount = 0;
		StringBuilder itemList = new StringBuilder();

		try (Connection con = DatabaseFactory.getConnection())
		{
			for (RewardData reward : rewards)
			{
				// 給予物品
				player.addItem(ItemProcessType.NONE, reward.itemId, reward.itemCount, player, false);

				// 更新資料庫
				try (PreparedStatement ps = con.prepareStatement("UPDATE lottery_rewards SET received = 1, received_at = NOW() WHERE id = ?"))
				{
					ps.setInt(1, reward.id);
					ps.executeUpdate();
				}

				successCount++;
				itemList.append(reward.itemName).append(" x").append(reward.itemCount).append("<br>");
			}

			player.sendItemList();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return showHtmlFile(player, "error.htm", createMap("message", "領取過程中發生錯誤"));
		}

		Map<String, String> replacements = new HashMap<>();
		replacements.put("title", "批量領取成功！");
		replacements.put("line1", "成功領取 " + successCount + " 個獎勵");
		replacements.put("line2", itemList.toString());

		return showHtmlFile(player, "success.htm", replacements);
	}

	/**
	 * 獲取待領取獎勵數量
	 */
	private int getPendingRewardsCount(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM lottery_rewards WHERE char_id = ? AND received = 0"))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt(1);
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 獲取待領取獎勵列表
	 */
	private List<RewardData> getPendingRewards(Player player)
	{
		List<RewardData> rewards = new ArrayList<>();

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT id, item_id, item_name, item_count FROM lottery_rewards WHERE char_id = ? AND received = 0 ORDER BY created_at DESC"))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					RewardData reward = new RewardData();
					reward.id = rs.getInt("id");
					reward.itemId = rs.getInt("item_id");
					reward.itemName = rs.getString("item_name");
					reward.itemCount = rs.getInt("item_count");
					rewards.add(reward);
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return rewards;
	}

	/**
	 * 獲取抽獎歷史記錄總數
	 */
	private int getLotteryHistoryCount(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM lottery_log l INNER JOIN bound_accounts b ON l.user_id = b.id WHERE b.char_id = ?"))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return rs.getInt(1);
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * 獲取抽獎歷史記錄
	 */
	private List<HistoryData> getLotteryHistory(Player player, int offset, int limit)
	{
		List<HistoryData> history = new ArrayList<>();

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT l.prize_name, l.created_at FROM lottery_log l INNER JOIN bound_accounts b ON l.user_id = b.id WHERE b.char_id = ? ORDER BY l.created_at DESC LIMIT ? OFFSET ?"))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, limit);
			ps.setInt(3, offset);

			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					HistoryData record = new HistoryData();
					record.prizeName = rs.getString("prize_name");
					record.createdAt = rs.getString("created_at");
					history.add(record);
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		return history;
	}

	/**
	 * 格式化日期時間
	 */
	private String formatDateTime(String datetime)
	{
		if (datetime == null || datetime.length() < 16)
		{
			return datetime;
		}
		// 從 "2024-01-28 10:30:45" 截取為 "01-28 10:30"
		return datetime.substring(5, 16);
	}

	/**
	 * 創建單一鍵值對的 Map
	 */
	private Map<String, String> createMap(String key, String value)
	{
		Map<String, String> map = new HashMap<>();
		map.put(key, value);
		return map;
	}

	/**
	 * 顯示 HTML 文件
	 */
	private String showHtmlFile(Player player, String fileName, Map<String, String> replacements)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player, HTML_PATH + fileName);

		if (replacements != null && !replacements.isEmpty())
		{
			for (Map.Entry<String, String> entry : replacements.entrySet())
			{
				String key = entry.getKey();
				String value = entry.getValue();
				if (key != null && value != null)
				{
					html.replace("%" + key + "%", value);
				}
			}
		}

		player.sendPacket(html);
		return null;
	}

	/**
	 * 獎勵數據類
	 */
	private static class RewardData
	{
		int id;
		int itemId;
		String itemName;
		int itemCount;
	}

	/**
	 * 歷史記錄數據類
	 */
	private static class HistoryData
	{
		String prizeName;
		String createdAt;
	}

	/**
	 * 格式化數字 (加入千分位逗號)
	 */
	private String formatNumber(long number)
	{
		return String.format("%,d", number);
	}

	public static void main(String[] args)
	{
		new WebsiteLottery();
	}
}
