package custom.BossAuctionSystem;

import java.util.List;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

import custom.BossAuctionSystem.BossAuctionDAO.*;
import custom.BossAuctionSystem.BossAuctionManager.*;

/**
 * Boss Auction System - NPC Script
 * 提供玩家與競標系統的交互介面
 * @author 黑普羅
 */
public class BossAuctionSystem extends Script
{
	// NPC ID (競標管理員)
	private static final int AUCTION_NPC_ID = 900029;

	public BossAuctionSystem()
	{
		addStartNpc(AUCTION_NPC_ID);
		addTalkId(AUCTION_NPC_ID);
		addFirstTalkId(AUCTION_NPC_ID);

		// 初始化管理器
		BossAuctionManager.getInstance();

		// ========== 【重要】手動載入傷害監聽器 ==========
		// 確保 BossKillListener 被載入，以便記錄玩家傷害
		new BossKillListener();
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("main"))
		{
			showMainPage(player);
			return null;
		}
		else if (event.startsWith("viewSession_"))
		{
			int sessionId = Integer.parseInt(event.substring(12));
			showSessionItems(player, sessionId);
			return null;
		}
		else if (event.startsWith("bidItem_"))
		{
			int auctionItemId = Integer.parseInt(event.substring(8));
			showBidPage(player, auctionItemId);
			return null;
		}
		else if (event.startsWith("bid_"))
		{
			String[] parts = event.substring(4).split("_");
			int auctionItemId = Integer.parseInt(parts[0]);
			long bidAmount = Long.parseLong(parts[1]);
			handleBid(player, auctionItemId, bidAmount);
			return null;
		}
		else if (event.startsWith("bidAdd_"))
		{
			// 處理快捷加價出價 bidAdd_auctionItemId_增量
			String[] parts = event.substring(7).split("_");
			if (parts.length >= 2)
			{
				try
				{
					int auctionItemId = Integer.parseInt(parts[0]);
					long increment = Long.parseLong(parts[1]);

					// 獲取當前物品信息
					AuctionItem item = BossAuctionDAO.getAuctionItem(auctionItemId);
					if (item != null)
					{
						long bidAmount = item.currentBid + increment;
						handleBid(player, auctionItemId, bidAmount);
					}
					else
					{
						player.sendMessage("物品不存在");
					}
				}
				catch (NumberFormatException e)
				{
					player.sendMessage("出價失敗");
				}
			}
			return null;
		}
		else if (event.startsWith("bidCustom_"))
		{
			// 處理自訂金額出價 bidCustom_金額
			String bidStr = event.substring(10);

			// 從 bidStr 中提取 auctionItemId 和 金額
			// 格式: "auctionItemId 金額"
			String[] parts = bidStr.split(" ");
			if (parts.length >= 2)
			{
				try
				{
					int auctionItemId = Integer.parseInt(parts[0]);
					long bidAmount = Long.parseLong(parts[1]);
					handleBid(player, auctionItemId, bidAmount);
				}
				catch (NumberFormatException e)
				{
					player.sendMessage("請輸入有效的數字");
					return null;
				}
			}
			else
			{
				player.sendMessage("請輸入出價金額");
			}
			return null;
		}

		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainPage(player);
		return null;
	}

	/**
	 * 顯示主頁面
	 */
	private void showMainPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/BossAuctionSystem/auction_main.htm");

		// 獲取活躍會話
		List<AuctionSession> sessions = BossAuctionManager.getInstance().getActiveSessions();

		StringBuilder sessionList = new StringBuilder();
		if (sessions.isEmpty())
		{
			sessionList.append("<tr><td bgcolor=\"222222\" align=center>目前沒有進行中的競標</td></tr>");
		}
		else
		{
			for (AuctionSession session : sessions)
			{
				long remainingTime = session.endTime - System.currentTimeMillis();
				String timeLeft = formatTime(remainingTime);

				sessionList.append("<tr><td bgcolor=\"222222\" align=center>");
				sessionList.append("<font color=\"00FF66\">").append(session.bossName).append("</font> - ");
				sessionList.append("<font color=\"FFFF00\">").append(timeLeft).append("</font>");
				sessionList.append("</td></tr>");
				sessionList.append("<tr><td bgcolor=\"222222\" align=center>");
				sessionList.append("<button action=\"bypass -h Quest BossAuctionSystem viewSession_").append(session.sessionId);
				sessionList.append("\" value=\"查看物品\" width=200 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
				sessionList.append("</td></tr>");
				sessionList.append("<tr><td height=3></td></tr>");
			}
		}

		html.replace("%session_list%", sessionList.toString());
		player.sendPacket(html);
	}

	/**
	 * 顯示會話物品列表
	 */
	private void showSessionItems(Player player, int sessionId)
	{
		AuctionSession session = BossAuctionManager.getInstance().getActiveSessions().stream()
			.filter(s -> s.sessionId == sessionId)
			.findFirst()
			.orElse(null);

		if (session == null)
		{
			player.sendMessage("競標會話不存在或已結束");
			showMainPage(player);
			return;
		}

		List<AuctionItem> items = BossAuctionManager.getInstance().getSessionItems(sessionId);

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/BossAuctionSystem/auction_items.htm");

		// BOSS 名稱和剩餘時間
		html.replace("%boss_name%", session.bossName);
		long remainingTime = session.endTime - System.currentTimeMillis();
		html.replace("%time_left%", formatTime(remainingTime));
		html.replace("%session_id%", String.valueOf(sessionId));

		// 物品列表
		StringBuilder itemList = new StringBuilder();
		if (items.isEmpty())
		{
			itemList.append("<tr><td bgcolor=\"222222\" align=center colspan=2>沒有物品</td></tr>");
		}
		else
		{
			for (AuctionItem item : items)
			{
				ItemTemplate template = ItemData.getInstance().getTemplate(item.itemId);
				String itemName = template != null ? template.getName() : "未知物品";

				if (item.enchantLevel > 0)
				{
					itemName = "+" + item.enchantLevel + " " + itemName;
				}

				if (item.itemCount > 1)
				{
					itemName += " x" + item.itemCount;
				}

				String priceText = item.currentBid > 0 ? formatNumber(item.currentBid) : "起標";

				itemList.append("<tr>");
				itemList.append("<td bgcolor=\"222222\" width=180>").append(itemName).append("</td>");
				itemList.append("<td bgcolor=\"222222\" width=80>").append(priceText).append("</td>");
				itemList.append("</tr>");

				if ("PENDING".equals(item.status))
				{
					itemList.append("<tr><td bgcolor=\"222222\" colspan=2 align=center>");
					itemList.append("<button action=\"bypass -h Quest BossAuctionSystem bidItem_").append(item.auctionItemId);
					itemList.append("\" value=\"出價\" width=120 height=22 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
					itemList.append("</td></tr>");
				}
				else
				{
					itemList.append("<tr><td bgcolor=\"222222\" colspan=2 align=center><font color=\"FF0000\">已結束</font></td></tr>");
				}

				itemList.append("<tr><td colspan=2 height=3></td></tr>");
			}
		}

		html.replace("%item_list%", itemList.toString());
		player.sendPacket(html);
	}

	/**
	 * 顯示出價頁面
	 */
	private void showBidPage(Player player, int auctionItemId)
	{
		AuctionItem item = BossAuctionDAO.getAuctionItem(auctionItemId);
		if (item == null)
		{
			player.sendMessage("物品不存在");
			showMainPage(player);
			return;
		}

		ItemTemplate template = ItemData.getInstance().getTemplate(item.itemId);
		String itemName = template != null ? template.getName() : "未知物品";

		if (item.enchantLevel > 0)
		{
			itemName = "+" + item.enchantLevel + " " + itemName;
		}

		if (item.itemCount > 1)
		{
			itemName += " x" + item.itemCount;
		}

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/BossAuctionSystem/auction_bid.htm");

		html.replace("%item_name%", itemName);
		html.replace("%current_bid%", item.currentBid > 0 ? formatNumber(item.currentBid) + " L Coin" : "無出價");
		html.replace("%current_bidder%", item.currentBidderName != null ? item.currentBidderName : "無");
		html.replace("%bid_count%", String.valueOf(item.bidCount));
		html.replace("%auction_item_id%", String.valueOf(auctionItemId));
		html.replace("%session_id%", String.valueOf(item.sessionId));

		long suggestedBid = item.currentBid > 0 ? item.currentBid + 1000 : 1000;
		html.replace("%suggested_bid%", String.valueOf(suggestedBid));

		// 【新增】計算並顯示剩餘時間
		AuctionSession session = BossAuctionDAO.getSession(item.sessionId);
		if (session != null)
		{
			long timeRemaining = session.endTime - System.currentTimeMillis();
			String timeRemainingStr = formatTime(timeRemaining);

			// 根據剩餘時間設定顏色
			String timeColor = "FFFFFF"; // 白色
			if (timeRemaining <= 300000) // 5分鐘
			{
				timeColor = "FF0000"; // 紅色
			}
			else if (timeRemaining <= 600000) // 10分鐘
			{
				timeColor = "FF9900"; // 橘色
			}

			html.replace("%time_remaining%", timeRemainingStr);
			html.replace("%time_color%", timeColor);

			// 【新增】顯示剩餘可延長次數
			int currentExtensions = BossAuctionManager.getInstance().getSessionExtensionCount(item.sessionId);
			int maxExtensions = BossAuctionConfig.getMaxExtensionCount();
			int extensionRemaining = maxExtensions - currentExtensions;
			html.replace("%extension_remaining%", extensionRemaining + " / " + maxExtensions);
		}
		else
		{
			html.replace("%time_remaining%", "已結束");
			html.replace("%time_color%", "808080");
			html.replace("%extension_remaining%", "0 / 0");
		}

		player.sendPacket(html);
	}

	/**
	 * 處理出價
	 */
	private void handleBid(Player player, int auctionItemId, long bidAmount)
	{
		BidResult result = BossAuctionManager.getInstance().placeBid(player, auctionItemId, bidAmount);

		player.sendMessage(result.message);

		// 返回出價頁面
		showBidPage(player, auctionItemId);
	}

	/**
	 * 格式化時間
	 */
	private String formatTime(long ms)
	{
		if (ms <= 0)
		{
			return "已結束";
		}

		long hours = ms / 3600000;
		long minutes = (ms % 3600000) / 60000;

		if (hours > 0)
		{
			return hours + "小時" + minutes + "分";
		}
		else
		{
			return minutes + "分鐘";
		}
	}

	/**
	 * 格式化數字
	 */
	private String formatNumber(long number)
	{
		return String.format("%,d", number);
	}

	public static void main(String[] args)
	{
		new BossAuctionSystem();
	}
}
