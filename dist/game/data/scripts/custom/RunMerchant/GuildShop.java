/**
 * 商會商店系統 - 使用商會點數兌換
 * 參考 ClanShopHonor 界面設計
 */
package custom.RunMerchant;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 商會商店 - 使用商會點數兌換物品
 */
public class GuildShop extends Script
{
	// ============================================
	// NPC 設定區
	// ============================================
	private static final int SHOP_NPC_ID = 900006;

	// ============================================
	// PlayerVariables 設定（與 RunMerchant 保持一致）
	// ============================================
	private static final String PV_PREFIX = "RunMerchant_";

	// ============================================
	// 商品配置區
	// 格式: {物品ID, 所需點數, 數量}
	// ============================================
	private static final int[][] SHOP_ITEMS =
			{
					{105801, 99999, 1},
			};
	// ============================================
	// 優惠道具配置
	// ============================================
	private static final int DISCOUNT_ITEM_ID = 105803; // 持有此道具可享九折優惠
	private static final double DISCOUNT_RATE = 0.9; // 折扣比例 (0.9 = 九折)

	public GuildShop()
	{
		addStartNpc(SHOP_NPC_ID);
		addTalkId(SHOP_NPC_ID);
		addFirstTalkId(SHOP_NPC_ID);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("main"))
		{
			showMainMenu(player, npc);
		}
		else if (event.equals("check_points"))
		{
			long guildPoints = player.getVariables().getLong(PV_PREFIX + "total_guild_points", 0L);
			player.sendMessage("您當前擁有 " + formatNumber(guildPoints) + " 商會點數。");
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
	 * 顯示商品列表
	 */
	private void showShopList(Player player, Npc npc)
	{
		showShopList(player, npc, 0);
	}

	private void showShopList(Player player, Npc npc, int page)
	{
		PlayerVariables pv = player.getVariables();
		long guildPoints = pv.getLong(PV_PREFIX + "total_guild_points", 0L);

		// 分頁設定
		int maxListPerPage = 4; // 每頁顯示4個商品
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
		htmltext.append("<font color=b09979>商 會 商 店</font>");
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

		// 當前商會點數顯示
		htmltext.append("<table width=290 border=0 cellpadding=2 bgcolor=333333>");
		htmltext.append("<tr>");
		htmltext.append("<td width=290 align=center><font color=FFD700>商會點數: " + formatNumber(guildPoints) + "</font></td>");
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
					htmltext.append("<td align=center><a action=\"bypass -h Quest GuildShop shop_page " + x + "\">第" + pagenr + "頁</a></td>");
				}
			}
			htmltext.append("</tr>");
			htmltext.append("</table><br>");
		}

		// 商品列表
		for (int i = listStart; i < listEnd; i++)
		{
			int itemId = SHOP_ITEMS[i][0];
			int originalPrice = SHOP_ITEMS[i][1];
			boolean hasDiscount = hasDiscountItem(player);
			int pointsCost = calculatePrice(originalPrice, hasDiscount);
			int itemCount = SHOP_ITEMS[i][2];

			ItemTemplate it = ItemData.getInstance().getTemplate(itemId);

			// 如果找不到物品，跳過
			if (it == null)
			{
				continue;
			}

			// 判斷是否可購買
			boolean pointsOk = guildPoints >= pointsCost;
			boolean canBuy = pointsOk;

			// 物品展示框
			htmltext.append("<table width=290 bgcolor=111111>");
			htmltext.append("<tr>");
			htmltext.append("<td width=230>");

			// 左側資訊
			htmltext.append("<table width=230>");

			// 道具名稱 x 數量
			String nameColor = "LEVEL";
			htmltext.append("<tr><td><font color=" + nameColor + ">" + it.getName() + " x" + formatNumber(itemCount) + "</font></td></tr>");

			// 需要：XXXXX 商會點數 (現有數量) - 整行根據是否足夠顯示顏色
			String pointsColor = pointsOk ? "00FF00" : "FF0000";
			if (hasDiscount && originalPrice != pointsCost)
			{
				// 有折扣時顯示原價和折扣價
				htmltext.append("<tr><td><font color=" + pointsColor + ">需要: <font color=888888><s>" + formatNumber(originalPrice) + "</s></font> <font color=FFD700>" + formatNumber(pointsCost) + "</font> 商會點數</font></td></tr>");
			}
			else
			{
				// 無折扣時正常顯示
				htmltext.append("<tr><td><font color=" + pointsColor + ">需要: " + formatNumber(pointsCost) + " 商會點數</font></td></tr>");
			}
			htmltext.append("</table>");
			htmltext.append("</td>");

			// 右側按鈕
			htmltext.append("<td width=60 align=center valign=middle>");

			if (!pointsOk)
			{
				// 點數不足
				htmltext.append("<font color=FF0000>點數不足</font>");
			}
			else
			{
				// 可以購買
				htmltext.append("<button value=\"兌換\" action=\"bypass -h Quest GuildShop buy_" + i + "\" width=85 height=20 back=L2UI_CT1.Button_DF fore=L2UI_CT1.Button_DF>");
			}

			htmltext.append("</td>");
			htmltext.append("</tr>");
			htmltext.append("</table>");

			// 添加間隔
			htmltext.append("<img src=L2UI.SquareGray width=290 height=1><br1>");
		}

		// 底部按鈕
		htmltext.append("<br>");
		htmltext.append("<button value=\"返回首頁\" action=\"bypass -h Quest GuildShop main\" width=90 height=20 back=L2UI_ct1.button_df fore=L2UI_ct1.button_df>");

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
		if (itemIndex < 0 || itemIndex >= SHOP_ITEMS.length)
		{
			player.sendMessage("無效的商品!");
			showShopList(player, npc);
			return;
		}

		int[] item = SHOP_ITEMS[itemIndex];
		int itemId = item[0];
		int originalPrice = item[1];
		int itemCount = item[2];

		// 計算折扣後的價格
		boolean hasDiscount = hasDiscountItem(player);
		int pointsCost = calculatePrice(originalPrice, hasDiscount);

		PlayerVariables pv = player.getVariables();
		long currentPoints = pv.getLong(PV_PREFIX + "total_guild_points", 0L);

		// 檢查商會點數是否足夠
		if (currentPoints < pointsCost)
		{
			player.sendMessage("您的商會點數不足! 需要 " + formatNumber(pointsCost) + " 商會點數 (目前: " + formatNumber(currentPoints) + ")");
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

		// 扣除商會點數
		long newPoints = currentPoints - pointsCost;
		pv.set(PV_PREFIX + "total_guild_points", newPoints);

		// 給予道具
		player.addItem(ItemProcessType.BUY, itemId, itemCount, npc, true);

		// 取得道具名稱
		ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(itemId);
		String itemName = itemTemplate != null ? itemTemplate.getName() : "未知道具";

		// 發送成功訊息
		player.sendMessage("成功兌換 " + itemName + " x" + formatNumber(itemCount) + "!");
		if (hasDiscount && originalPrice != pointsCost)
		{
			player.sendMessage("消耗商會點數: " + formatNumber(pointsCost) + " (持有道具折扣後: " + formatNumber(originalPrice) + ") (剩餘: " + formatNumber(newPoints) + ")");
		}
		else
		{
			player.sendMessage("消耗商會點數: " + formatNumber(pointsCost) + " (剩餘: " + formatNumber(newPoints) + ")");
		}
		// 顯示成功頁面
		showSuccessPage(player, npc, itemName, itemCount, pointsCost, newPoints);
	}

	/**
	 * 顯示購買成功頁面
	 */
	private void showSuccessPage(Player player, Npc npc, String itemName, int itemCount, int pointsCost, long remainingPoints)
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
		htmltext.append("<font color=b09979>兌 換 成 功</font>");
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
		htmltext.append("<tr><td align=center><font color=00FF00>恭喜您兌換成功!</font></td></tr>");
		htmltext.append("</table><br>");

		// 兌換詳情
		htmltext.append("<table width=290 border=0 cellpadding=5 bgcolor=333333>");
		htmltext.append("<tr><td align=center><font color=LEVEL>━━━━━━━━━━━━━━━</font></td></tr>");
		htmltext.append("<tr><td align=center height=20><font color=LEVEL>兌換商品</font></td></tr>");
		htmltext.append("<tr><td align=center height=22><font color=FFFFFF>" + itemName + " x" + formatNumber(itemCount) + "</font></td></tr>");
		htmltext.append("<tr><td align=center><font color=LEVEL>━━━━━━━━━━━━━━━</font></td></tr>");
		htmltext.append("<tr><td align=center height=20><font color=LEVEL>消耗商會點數: <font color=FFD700>" + formatNumber(pointsCost) + "</font></font></td></tr>");
		htmltext.append("<tr><td align=center height=20><font color=LEVEL>剩餘商會點數: <font color=FFD700>" + formatNumber(remainingPoints) + "</font></font></td></tr>");
		htmltext.append("<tr><td align=center><font color=LEVEL>━━━━━━━━━━━━━━━</font></td></tr>");
		htmltext.append("</table><br>");

		// 底部按鈕
		htmltext.append("<button value=\"繼續兌換\" action=\"bypass -h Quest GuildShop shop\" width=90 height=20 back=L2UI_ct1.button_df fore=L2UI_ct1.button_df>");
		htmltext.append("<button value=\"返回首頁\" action=\"bypass -h Quest GuildShop main\" width=90 height=20 back=L2UI_ct1.button_df fore=L2UI_ct1.button_df>");
		htmltext.append("<br><br>");
		htmltext.append("<font color=LEVEL>※ 商會點數永久累積</font>");

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
		html.setFile(player, "data/scripts/custom/RunMerchant/GuildShop_main.htm");

		PlayerVariables pv = player.getVariables();
		long guildPoints = pv.getLong(PV_PREFIX + "total_guild_points", 0L);

		html.replace("%guild_points%", formatNumber(guildPoints));

		player.sendPacket(html);
	}

	/**
	 * 檢查玩家是否持有優惠道具
	 */
	private boolean hasDiscountItem(Player player)
	{
		return player.getInventory().getItemByItemId(DISCOUNT_ITEM_ID) != null;
	}

	/**
	 * 計算折扣後的價格
	 */
	private int calculatePrice(int originalPrice, boolean hasDiscount)
	{
		if (hasDiscount)
		{
			return (int) (originalPrice * DISCOUNT_RATE);
		}
		return originalPrice;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainMenu(player, npc);
		return null;
	}

	/**
	 * 格式化數字（加入千分位逗號）
	 */
	private String formatNumber(long number)
	{
		return String.format("%,d", number);
	}

	public static void main(String[] args)
	{
		new GuildShop();
		System.out.println("【系統】商會商店系統載入完畢!");
		System.out.println("【系統】使用商會點數兌換商品");
	}
}