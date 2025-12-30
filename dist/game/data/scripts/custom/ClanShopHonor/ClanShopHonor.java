/**
 * 血盟專屬商店系統 - 名譽鑄幣版本 (每日重置)
 * 使用名譽鑄幣 (Honor Coins) 購買
 */
package custom.ClanShopHonor;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class ClanShopHonor extends Script
{
	// ============================================
	// NPC 設定區
	// ============================================
	private static final int NPC_ID = 900007;

	// ============================================
	// 商品配置區
	// 格式: {血盟等級, 道具ID, 數量, 名譽鑄幣價格, 每日限購次數(0=無限)}
	// ============================================
	private static final int[][] SHOP_ITEMS =
			{
					{1, 101909, 1, 30, 5},

					{3, 101806, 1, 500, 1},

					{5, 9589, 1, 80, 2},
					{5, 90888, 1, 80, 2},
					{5, 90311, 1, 80, 2},

					{9, 92031, 1, 150, 2},
					{9, 91161, 1, 150, 2},
					{9, 91304, 1, 150, 2},

					{10, 105801, 1, 50, 10},
					{10, 103999, 1, 2000, 1},
					{10, 103828, 1, 2000, 1},
					{10, 94042, 1, 2000, 1},

					{12, 103069, 1, 1000, 1},

					{15, 94042, 1, 2000, 1}, //夏日海洋
			};

	// ============================================
	// 每日重置設定
	// ============================================
	private static final String LAST_PURCHASE_DATE_VAR = "ClanShopHonor_LastPurchaseDate";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	public ClanShopHonor()
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
			showMainMenu(player, npc);
		}
		else if (event.equals("check_honor"))
		{
			long honorCoins = player.getHonorCoins();
			player.sendMessage("您當前擁有 " + honorCoins + " 個名譽鑄幣。");
			showMainMenu(player, npc);
		}
		else if (event.equals("shop"))
		{
			showShopList(player, npc, 0); // 顯示第一頁
		}
		else if (event.startsWith("shop_page "))
		{
			try
			{
				int page = Integer.parseInt(event.substring(10));
				showShopList(player, npc, page);
			}
			catch (NumberFormatException e)
			{
				showShopList(player, npc, 0);
			}
		}
		else if (event.startsWith("buy_"))
		{
			try
			{
				int itemIndex = Integer.parseInt(event.substring(4));
				purchaseItem(player, npc, itemIndex);
			}
			catch (NumberFormatException e)
			{
				player.sendMessage("購買失敗:無效的商品編號。");
				showShopList(player, npc);
			}
		}

		return null;
	}

	/**
	 * 取得今日該商品已購買次數
	 */
	private int getTodayPurchaseCount(Player player, int itemIndex)
	{
		String today = DATE_FORMAT.format(new Date());
		String lastDateVar = LAST_PURCHASE_DATE_VAR + "_date_" + itemIndex;
		String countVar = LAST_PURCHASE_DATE_VAR + "_count_" + itemIndex;

		String lastPurchaseDate = player.getVariables().getString(lastDateVar, "");

		// 如果日期不同，重置計數
		if (!today.equals(lastPurchaseDate))
		{
			return 0;
		}

		return player.getVariables().getInt(countVar, 0);
	}

	/**
	 * 檢查是否可以購買（是否達到限購上限）
	 */
	private boolean canPurchaseToday(Player player, int itemIndex)
	{
		int dailyLimit = SHOP_ITEMS[itemIndex][4]; // 每日限購次數

		// 0 表示無限購買
		if (dailyLimit == 0)
		{
			return true;
		}

		int todayCount = getTodayPurchaseCount(player, itemIndex);
		return todayCount < dailyLimit;
	}

	/**
	 * 顯示商品列表 (參考wphs的liebiao方法)
	 */
	private void showShopList(Player player, Npc npc)
	{
		showShopList(player, npc, 0); // 預設第一頁
	}

	private void showShopList(Player player, Npc npc, int page)
	{
		Clan clan = player.getClan();

		// 檢查是否有血盟
		if (clan == null)
		{
			player.sendMessage("您必須加入血盟才能使用血盟商店!");
			showMainMenu(player, npc);
			return;
		}

		int clanLevel = clan.getLevel();
		long honorCoins = player.getHonorCoins();

		// 分頁設定
		int maxListPerPage = 4; // 每頁顯示5個商品
		int maxPages = SHOP_ITEMS.length / maxListPerPage;
		if (SHOP_ITEMS.length > (maxListPerPage * maxPages))
		{
			maxPages++;
		}
		if (page > maxPages)
		{
			page = maxPages;
		}
		int listStart = maxListPerPage * page;
		int listEnd = SHOP_ITEMS.length;
		if ((listEnd - listStart) > maxListPerPage)
		{
			listEnd = listStart + maxListPerPage;
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
		NpcHtmlMessage adminReply = new NpcHtmlMessage();
		StringBuilder htmltext = new StringBuilder();

		htmltext.append("<html><body>");
		htmltext.append("<center>");

		// 標題部分
		htmltext.append("<table width=290 border=0 cellpadding=0 cellspacing=0>");
		htmltext.append("<tr>");
		htmltext.append("<td align=center>");
		htmltext.append("<img src=L2UI.SquareWhite width=290 height=1>");
		htmltext.append("</td>");
		htmltext.append("</tr>");
		htmltext.append("<tr><td height=\"15\"></td></tr>");
		htmltext.append("<tr>");
		htmltext.append("<td align=center>");
		htmltext.append("<font color=b09979>血 盟 專 屬 商 店</font>");
		htmltext.append("</td>");
		htmltext.append("</tr>");
		htmltext.append("<tr><td height=\"5\"></td></tr>");
		htmltext.append("<tr>");
		htmltext.append("<td align=center>");
		htmltext.append("<img src=L2UI.SquareWhite width=290 height=1>");
		htmltext.append("</td>");
		htmltext.append("</tr>");
		htmltext.append("<tr><td height=\"10\"></td></tr>");
		htmltext.append("</table>");

		// 分隔線
		htmltext.append("<img src=L2UI.SquareGray width=290 height=1><br>");

		// 當前狀態顯示（只顯示名譽鑄幣）
		htmltext.append("<table width=290 border=0 cellpadding=2 bgcolor=333333>");
		htmltext.append("<tr>");
		htmltext.append("<td width=290 align=center><font color=FFD700>名譽鑄幣: " + honorCoins + "</font></td>");
		htmltext.append("</tr>");
		htmltext.append("</table><br>");

		// 提示文字
		htmltext.append("<table width=290 bgcolor=333333>");
		htmltext.append("<tr>");
		htmltext.append("<td align=center><font color=b09979>請選擇您要兌換的商品</font></td>");
		htmltext.append("</tr>");
		htmltext.append("</table>");

		// 分頁顯示
		if (SHOP_ITEMS.length != 0)
		{
			htmltext.append("<table width=290>");
			htmltext.append("<tr>");
			for (int x = 0; x < maxPages; x++)
			{
				int pagenr = x + 1;
				if (x == page)
				{
					htmltext.append("<td align=center><font color=FFFF00>第" + pagenr + "頁</font></td>");
				}
				else
				{
					htmltext.append("<td align=center><a action=\"bypass -h Quest ClanShopHonor shop_page " + x + "\">第" + pagenr + "頁</a></td>");
				}
			}
			htmltext.append("</tr>");
			htmltext.append("</table><br>");
		}

		// 商品列表
		for (int i = listStart; i < listEnd; i++)
		{
			int requiredLevel = SHOP_ITEMS[i][0];
			int itemId = SHOP_ITEMS[i][1];
			int itemCount = SHOP_ITEMS[i][2];
			int honorPrice = SHOP_ITEMS[i][3];

			ItemTemplate it = ItemData.getInstance().getTemplate(itemId);

			// 如果找不到物品，跳過
			if (it == null)
			{
				continue;
			}

			// 判斷是否可購買（每個商品單獨檢查）
			int dailyLimit = SHOP_ITEMS[i][4]; // 每日限購次數
			int todayPurchased = getTodayPurchaseCount(player, i);
			boolean canPurchase = canPurchaseToday(player, i);

			boolean levelOk = clanLevel >= requiredLevel;
			boolean honorOk = honorCoins >= honorPrice;
			boolean canBuy = levelOk && honorOk && canPurchase;

			// 物品展示框
			htmltext.append("<table width=290 bgcolor=111111>");
			htmltext.append("<tr>");
			htmltext.append("<td width=200>");

			// 左側資訊
			htmltext.append("<table width=200>");

			// 道具名稱 x 數量
			String nameColor = levelOk ? "LEVEL" : "808080";
			htmltext.append("<tr><td><font color=" + nameColor + ">" + it.getName() + " x" + itemCount + "</font></td></tr>");

			// 需要：XXXXX 名譽鑄幣 (現有數量) - 整行根據是否足夠顯示顏色
			String honorColor = honorOk ? "00FF00" : "FF0000";
			htmltext.append("<tr><td><font color=" + honorColor + ">需要: " + honorPrice + " 名譽幣 (現有: " + honorCoins + ")</font></td></tr>");

			// 需求血盟等級
			if (levelOk)
			{
				htmltext.append("<tr><td><font color=00FF00>需求血盟等級: Lv." + requiredLevel + " ✓</font></td></tr>");
			}
			else
			{
				htmltext.append("<tr><td><font color=FF0000>需求血盟等級: Lv." + requiredLevel + " (未達成)</font></td></tr>");
			}

			// 顯示限購信息
			if (dailyLimit > 0)
			{
				int remaining = dailyLimit - todayPurchased;
				if (remaining > 0)
				{
					htmltext.append("<tr><td><font color=00FFFF>今日剩餘: " + remaining + "/" + dailyLimit + "</font></td></tr>");
				}
				else
				{
					htmltext.append("<tr><td><font color=FF6600>今日已達購買上限</font></td></tr>");
				}
			}
			else
			{
				htmltext.append("<tr><td><font color=00FFFF>無限購買</font></td></tr>");
			}

			htmltext.append("</table>");
			htmltext.append("</td>");

			// 右側按鈕
			htmltext.append("<td width=90 align=center valign=middle>");

			if (!levelOk)
			{
				// 等級不足 - 顯示未開放（紅色字）
				htmltext.append("<font color=FF0000>未開放</font>");
			}
			else if (!canPurchase)
			{
				// 已達限購上限
				htmltext.append("<font color=FF6600>已達上限</font>");
			}
			else if (!honorOk)
			{
				// 名譽幣不足
				htmltext.append("<font color=FF0000>名譽幣不足</font>");
			}
			else
			{
				// 可以購買
				htmltext.append("<button value=\"購買\" action=\"bypass -h Quest ClanShopHonor buy_" + i + "\" width=85 height=20 back=L2UI_CT1.Button_DF fore=L2UI_CT1.Button_DF>");
			}

			htmltext.append("</td>");
			htmltext.append("</tr>");
			htmltext.append("</table>");

			// 添加間隔
			htmltext.append("<img src=L2UI.SquareGray width=290 height=1><br1>");
		}

		// 底部按鈕
		htmltext.append("<br>");
		htmltext.append("<button value=\"返回首頁\" action=\"bypass -h Quest ClanShopHonor main\" width=90 height=20 back=L2UI_ct1.button_df fore=L2UI_ct1.button_df>");

		htmltext.append("</center>");
		htmltext.append("</body></html>");

		adminReply.setHtml(htmltext.toString());
		player.sendPacket(adminReply);
	}

	/**
	 * 處理購買邏輯
	 */
	private void purchaseItem(Player player, Npc npc, int itemIndex)
	{
		Clan clan = player.getClan();
		if (clan == null)
		{
			player.sendMessage("您必須加入血盟才能使用血盟商店!");
			showMainMenu(player, npc);
			return;
		}

		if (itemIndex < 0 || itemIndex >= SHOP_ITEMS.length)
		{
			player.sendMessage("無效的商品!");
			showShopList(player, npc);
			return;
		}

		int[] item = SHOP_ITEMS[itemIndex];
		int requiredLevel = item[0];
		int itemId = item[1];
		int itemCount = item[2];
		int honorPrice = item[3];

		// 檢查血盟等級
		if (clan.getLevel() < requiredLevel)
		{
			player.sendMessage("您的血盟等級不足! 需要等級 " + requiredLevel + " (目前等級: " + clan.getLevel() + ")");
			showShopList(player, npc);
			return;
		}

		// 檢查是否達到限購上限
		int dailyLimit = item[4];
		if (!canPurchaseToday(player, itemIndex))
		{
			int todayCount = getTodayPurchaseCount(player, itemIndex);
			player.sendMessage("您今日已達此商品購買上限! (" + todayCount + "/" + dailyLimit + ")");
			showShopList(player, npc, 0);
			return;
		}

		// 檢查名譽鑄幣是否足夠
		long currentHonor = player.getHonorCoins();
		if (currentHonor < honorPrice)
		{
			player.sendMessage("您的名譽鑄幣不足! 需要 " + honorPrice + " 名譽鑄幣 (目前: " + currentHonor + ")");
			showShopList(player, npc);
			return;
		}

		// 檢查背包空間
		if (!player.getInventory().validateCapacity(1))
		{
			player.sendMessage("背包空間不足!");
			showShopList(player, npc);
			return;
		}

		// 扣除名譽鑄幣
		player.setHonorCoins(currentHonor - honorPrice);

		// 給予道具
		player.addItem(ItemProcessType.BUY, itemId, itemCount, npc, true);

		// 記錄該商品的購買次數
		String today = DATE_FORMAT.format(new Date());
		String lastDateVar = LAST_PURCHASE_DATE_VAR + "_date_" + itemIndex;
		String countVar = LAST_PURCHASE_DATE_VAR + "_count_" + itemIndex;

		int currentCount = getTodayPurchaseCount(player, itemIndex);
		player.getVariables().set(lastDateVar, today);
		player.getVariables().set(countVar, currentCount + 1);
		player.getVariables().storeMe();

		// 取得道具名稱
		ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(itemId);
		String itemName = itemTemplate != null ? itemTemplate.getName() : "未知道具";

		// 發送成功訊息
		player.sendMessage("成功購買 " + itemName + " x" + itemCount + "!");
		player.sendMessage("消耗名譽鑄幣: " + honorPrice + " (剩餘: " + (currentHonor - honorPrice) + ")");
		player.sendMessage("明日00:00後可再次購買!");

		// 顯示成功頁面
		showSuccessPage(player, npc, itemName, itemCount, honorPrice, currentHonor - honorPrice);
	}

	/**
	 * 顯示購買成功頁面
	 */
	private void showSuccessPage(Player player, Npc npc, String itemName, int itemCount, int honorPrice, long remainingHonor)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		NpcHtmlMessage adminReply = new NpcHtmlMessage();
		StringBuilder htmltext = new StringBuilder();

		htmltext.append("<html><body>");
		htmltext.append("<center>");

		// 標題部分
		htmltext.append("<table width=290 border=0 cellpadding=0 cellspacing=0>");
		htmltext.append("<tr>");
		htmltext.append("<td align=center>");
		htmltext.append("<img src=L2UI.SquareWhite width=290 height=1>");
		htmltext.append("</td>");
		htmltext.append("</tr>");
		htmltext.append("<tr><td height=\"15\"></td></tr>");
		htmltext.append("<tr>");
		htmltext.append("<td align=center>");
		htmltext.append("<font color=b09979>購 買 成 功</font>");
		htmltext.append("</td>");
		htmltext.append("</tr>");
		htmltext.append("<tr><td height=\"5\"></td></tr>");
		htmltext.append("<tr>");
		htmltext.append("<td align=center>");
		htmltext.append("<img src=L2UI.SquareWhite width=290 height=1>");
		htmltext.append("</td>");
		htmltext.append("</tr>");
		htmltext.append("<tr><td height=\"10\"></td></tr>");
		htmltext.append("</table>");

		// 分隔線
		htmltext.append("<img src=L2UI.SquareGray width=290 height=1><br>");

		// 成功圖示
		htmltext.append("<table width=290>");
		htmltext.append("<tr><td align=center>");
		htmltext.append("<img src=L2UI_CH3.joypad_shortcut width=32 height=32>");
		htmltext.append("</td></tr>");
		htmltext.append("<tr><td height=\"10\"></td></tr>");
		htmltext.append("<tr><td align=center><font color=00FF00>恭喜您購買成功!</font></td></tr>");
		htmltext.append("</table><br>");

		// 購買詳情
		htmltext.append("<table width=290 border=0 cellpadding=5 bgcolor=333333>");
		htmltext.append("<tr><td align=center><font color=LEVEL>━━━━━━━━━━━━━━━</font></td></tr>");
		htmltext.append("<tr><td align=center height=20><font color=LEVEL>購買商品</font></td></tr>");
		htmltext.append("<tr><td align=center height=22><font color=FFFFFF>" + itemName + " x" + itemCount + "</font></td></tr>");
		htmltext.append("<tr><td align=center><font color=LEVEL>━━━━━━━━━━━━━━━</font></td></tr>");
		htmltext.append("<tr><td align=center height=20><font color=LEVEL>消耗名譽鑄幣: <font color=FFD700>" + honorPrice + "</font></font></td></tr>");
		htmltext.append("<tr><td align=center height=20><font color=LEVEL>剩餘名譽鑄幣: <font color=FFD700>" + remainingHonor + "</font></font></td></tr>");
		htmltext.append("<tr><td align=center><font color=LEVEL>━━━━━━━━━━━━━━━</font></td></tr>");
		htmltext.append("</table><br>");

		// 底部按鈕
		htmltext.append("<button value=\"繼續購買\" action=\"bypass -h Quest ClanShopHonor shop\" width=90 height=20 back=L2UI_ct1.button_df fore=L2UI_ct1.button_df>");
		htmltext.append("<button value=\"返回首頁\" action=\"bypass -h Quest ClanShopHonor main\" width=90 height=20 back=L2UI_ct1.button_df fore=L2UI_ct1.button_df>");
		htmltext.append("<br><br>");
		htmltext.append("<font color=LEVEL>※ 明日00:00後可再次購買</font>");

		htmltext.append("</center>");
		htmltext.append("</body></html>");

		adminReply.setHtml(htmltext.toString());
		player.sendPacket(adminReply);
	}

	/**
	 * 顯示主選單
	 */
	private void showMainMenu(Player player, Npc npc)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/ClanShopHonor/main.htm");

		Clan clan = player.getClan();
		long honorCoins = player.getHonorCoins();

		html.replace("%clan_name%", clan != null ? clan.getName() : "無");
		html.replace("%clan_level%", clan != null ? String.valueOf(clan.getLevel()) : "0");
		html.replace("%honor_coins%", String.valueOf(honorCoins));

		player.sendPacket(html);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainMenu(player, npc);
		return null;
	}

	public static void main(String[] args)
	{
		new ClanShopHonor();
		System.out.println("【系統】血盟名譽商店系統載入完畢!");
		System.out.println("【系統】每日00:00重置購買次數");
	}
}