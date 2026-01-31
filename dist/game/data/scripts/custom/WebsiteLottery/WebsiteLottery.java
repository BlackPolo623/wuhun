package custom.WebsiteLottery;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 網站積分兌換系統 NPC
 * @author 黑普羅
 *
 * 積分兌換採用本地佇列機制，避免跨地域延遲：
 * 1. 玩家兌換時，先在本地遊戲DB寫入佇列
 * 2. WebsiteExchangeProcessor 腳本會定時檢查佇列並處理
 */
public class WebsiteLottery extends Script
{
	private static final Logger LOGGER = Logger.getLogger(WebsiteLottery.class.getName());

	// NPC ID
	private static final int NPC_ID = 900034;

	// HTML路徑
	private static final String HTML_PATH = "data/scripts/custom/WebsiteLottery/";

	// 積分兌換配置
	private static final int POINT_ITEM_ID = 91663;           // 兌換積分的道具ID
	private static final long POINT_ITEM_COUNT = 1;           // 單位數量
	private static final int POINTS_PER_UNIT = 1;             // 每單位兌換的積分數

	// 網站資料庫連線設定
	private static final String WEB_DB_URL = "jdbc:mysql://et9w7o.stackhero-network.com:5063/wuhun_web?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC";
	private static final String WEB_DB_USER = "root";
	private static final String WEB_DB_PASS = "5LYOR0g3oh1WJ7Ds73YtahZjDOgciznj";

	public WebsiteLottery()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);

		LOGGER.info("[WebsiteLottery] 積分兌換 NPC 已載入（即時兌換模式）");
	}

	// ==================== NPC 事件處理 ====================

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("main"))
		{
			return showMainPage(player);
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

	// ==================== 頁面顯示 ====================

	/**
	 * 顯示主頁面
	 */
	private String showMainPage(Player player)
	{
		Map<String, String> replacements = new HashMap<>();
		replacements.put("char_id", String.valueOf(player.getObjectId()));
		replacements.put("char_name", player.getName());

		// 檢查綁定狀態
		String boundUsername = getBoundUsername(player);

		if (boundUsername != null)
		{
			// 已綁定，顯示兌換頁面
			return showExchangePage(player, boundUsername);
		}
		else
		{
			// 未綁定，顯示提示頁面
			replacements.put("bind_status", "<font color=\"FF6666\">未綁定</font>");
			return showHtmlFile(player, "not_bound.htm", replacements);
		}
	}

	/**
	 * 顯示兌換頁面（已綁定玩家）
	 */
	private String showExchangePage(Player player, String boundUsername)
	{
		Map<String, String> replacements = new HashMap<>();
		replacements.put("char_id", String.valueOf(player.getObjectId()));
		replacements.put("char_name", player.getName());
		replacements.put("bind_status", "<font color=\"00FF00\">已綁定</font>");
		replacements.put("bound_username", boundUsername);

		// 獲取遊戲內道具數量
		long gameItemCount = player.getInventory().getInventoryItemCount(POINT_ITEM_ID, -1);
		replacements.put("game_item_count", formatNumber(gameItemCount));

		// 獲取道具名稱
		ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(POINT_ITEM_ID);
		String itemName = itemTemplate != null ? itemTemplate.getName() : "道具";
		replacements.put("item_name", itemName);

		// 獲取網站積分
		int webPoints = getWebPoints(player);
		replacements.put("web_points", String.valueOf(webPoints));

		return showHtmlFile(player, "main.htm", replacements);
	}

	/**
	 * 獲取網站資料庫連線
	 */
	private Connection getWebDatabaseConnection() throws SQLException
	{
		return DriverManager.getConnection(WEB_DB_URL, WEB_DB_USER, WEB_DB_PASS);
	}

	/**
	 * 獲取綁定的網站帳號
	 */
	private String getBoundUsername(Player player)
	{
		try (Connection con = getWebDatabaseConnection();
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
			LOGGER.warning("[WebsiteLottery] 查詢綁定帳號失敗: " + e.getMessage());
		}
		return null;
	}

	/**
	 * 獲取網站積分
	 */
	private int getWebPoints(Player player)
	{
		try (Connection con = getWebDatabaseConnection();
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
			LOGGER.warning("[WebsiteLottery] 查詢網站積分失敗: " + e.getMessage());
		}
		return 0;
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

		// 先扣除道具
		if (!player.destroyItemByItemId(ItemProcessType.NONE, POINT_ITEM_ID, totalItemCount, player, true))
		{
			return showHtmlFile(player, "error.htm", createMap("message", "扣除道具失敗"));
		}

		// 直接更新網站資料庫積分
		try (Connection con = getWebDatabaseConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE bound_accounts SET points = points + ? WHERE char_id = ?"))
		{
			ps.setInt(1, pointsToAdd);
			ps.setInt(2, player.getObjectId());
			int updated = ps.executeUpdate();

			if (updated > 0)
			{
				LOGGER.info("[WebsiteLottery] 玩家 " + player.getName() + " 兌換道具->積分成功，增加積分: " + pointsToAdd);
			}
			else
			{
				// 更新失敗，返還道具
				player.addItem(ItemProcessType.NONE, POINT_ITEM_ID, totalItemCount, player, true);
				return showHtmlFile(player, "error.htm", createMap("message", "綁定資料異常，請聯繫管理員"));
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("[WebsiteLottery] 更新網站積分失敗: " + e.getMessage());
			// 更新失敗，返還道具
			player.addItem(ItemProcessType.NONE, POINT_ITEM_ID, totalItemCount, player, true);
			return showHtmlFile(player, "error.htm", createMap("message", "系統繁忙，請稍後再試"));
		}

		ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(POINT_ITEM_ID);
		String itemName = itemTemplate != null ? itemTemplate.getName() : "道具";

		Map<String, String> replacements = new HashMap<>();
		replacements.put("title", "兌換成功！");
		replacements.put("line1", "消耗 " + formatNumber(totalItemCount) + " " + itemName);
		replacements.put("line2", "獲得 " + pointsToAdd + " 網站積分");

		return showHtmlFile(player, "success.htm", replacements);
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

		// 計算獲得的道具數量
		long itemsToGive = POINT_ITEM_COUNT * units;

		// 檢查網站積分是否足夠
		int currentPoints = getWebPoints(player);
		if (currentPoints < pointsNeeded)
		{
			return showHtmlFile(player, "error.htm", createMap("message",
				"積分不足<br>需要: " + pointsNeeded + " 積分<br>目前: " + currentPoints + " 積分"));
		}

		// 先扣除網站積分
		try (Connection con = getWebDatabaseConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE bound_accounts SET points = points - ? WHERE char_id = ? AND points >= ?"))
		{
			ps.setInt(1, pointsNeeded);
			ps.setInt(2, player.getObjectId());
			ps.setInt(3, pointsNeeded);
			int updated = ps.executeUpdate();

			if (updated == 0)
			{
				return showHtmlFile(player, "error.htm", createMap("message", "積分不足或綁定資料異常"));
			}

			LOGGER.info("[WebsiteLottery] 玩家 " + player.getName() + " 兌換積分->道具成功，扣除積分: " + pointsNeeded);
		}
		catch (SQLException e)
		{
			LOGGER.warning("[WebsiteLottery] 扣除網站積分失敗: " + e.getMessage());
			return showHtmlFile(player, "error.htm", createMap("message", "系統繁忙，請稍後再試"));
		}

		// 給予遊戲道具
		player.addItem(ItemProcessType.NONE, POINT_ITEM_ID, itemsToGive, player, true);

		ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(POINT_ITEM_ID);
		String itemName = itemTemplate != null ? itemTemplate.getName() : "道具";

		Map<String, String> replacements = new HashMap<>();
		replacements.put("title", "兌換成功！");
		replacements.put("line1", "消耗 " + pointsNeeded + " 網站積分");
		replacements.put("line2", "獲得 " + formatNumber(itemsToGive) + " " + itemName);

		return showHtmlFile(player, "success.htm", replacements);
	}


	// ==================== 工具方法 ====================

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
