/**
 * 會員購買系統 - 超簡化版
 * NPC ID: 900017
 * 消耗道具: 105804
 */
package custom.PremiumShop;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 會員購買NPC
 * @author 黑普羅
 */
public class PremiumShop extends Script
{
	// ==================== 配置常數 ====================
	private static final int NPC_ID = 900017;
	private static final int CURRENCY_ITEM_ID = 105804; // 消耗道具ID
	private static final String HTML_PATH = "data/scripts/custom/PremiumShop/";

	// 會員套餐配置 [天數, 價格, 折扣百分比]
	// 折扣百分比用於顯示，例如：90表示9折
	private static final Map<Integer, PremiumPackage> PREMIUM_PACKAGES = new LinkedHashMap<>();

	static
	{
		// 配置各種會員套餐
		PREMIUM_PACKAGES.put(1, new PremiumPackage(7, 210, 100));      // 7天，1000個道具，無折扣
		PREMIUM_PACKAGES.put(2, new PremiumPackage(15, 400, 95));      // 15天，1900個道具，95折
		PREMIUM_PACKAGES.put(3, new PremiumPackage(30, 750, 88));      // 30天，3600個道具，9折
	}

	// ==================== 套餐數據類 ====================
	private static class PremiumPackage
	{
		final int days;           // 天數
		final long price;         // 價格
		final int discountPercent; // 折扣百分比（100=原價，90=9折）

		PremiumPackage(int days, long price, int discountPercent)
		{
			this.days = days;
			this.price = price;
			this.discountPercent = discountPercent;
		}

		boolean hasDiscount()
		{
			return discountPercent < 100;
		}

		String getDiscountText()
		{
			if (discountPercent >= 100)
			{
				return "";
			}
			return discountPercent + "折優惠";
		}
	}

	// ==================== 初始化 ====================
	public PremiumShop()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	// ==================== 事件處理 ====================
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainPage(player);
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((event == null) || (player == null))
		{
			return null;
		}

		if (event.equals("main"))
		{
			showMainPage(player);
		}
		else if (event.startsWith("buy_"))
		{
			try
			{
				int packageId = Integer.parseInt(event.substring(4));
				processPurchase(player, packageId);
			}
			catch (NumberFormatException e)
			{
				player.sendMessage("無效的套餐編號！");
				showMainPage(player);
			}
		}

		return null;
	}

	// ==================== 頁面顯示 ====================

	/**
	 * 顯示主頁面
	 */
	private void showMainPage(Player player)
	{
		if (!PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED)
		{
			player.sendMessage("會員系統目前未開啟。");
			return;
		}

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, HTML_PATH + "main.htm");

		// 獲取玩家當前會員狀態
		long endDate = PremiumManager.getInstance().getPremiumExpiration(player.getAccountName());
		boolean isPremium = endDate > 0;

		// 替換當前狀態
		if (isPremium)
		{
			SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日");
			html.replace("%premium_status%", "<font color=\"LEVEL\">高級會員</font>");
			html.replace("%expire_date%", format.format(endDate));
			html.replace("%status_color%", "00FF66");
		}
		else
		{
			html.replace("%premium_status%", "<font color=\"808080\">普通帳號</font>");
			html.replace("%expire_date%", "<font color=\"808080\">未開通</font>");
			html.replace("%status_color%", "808080");
		}

		// 替換當前道具數量
		long currentCurrency = player.getInventory().getInventoryItemCount(CURRENCY_ITEM_ID, -1);
		html.replace("%current_currency%", formatNumber(currentCurrency));

		// 生成套餐列表
		html.replace("%package_list%", generatePackageList(player));

		player.sendPacket(html);
	}

	// ==================== 業務邏輯 ====================

	/**
	 * 生成套餐列表HTML
	 */
	private String generatePackageList(Player player)
	{
		StringBuilder sb = new StringBuilder();
		long currentCurrency = player.getInventory().getInventoryItemCount(CURRENCY_ITEM_ID, -1);

		for (Map.Entry<Integer, PremiumPackage> entry : PREMIUM_PACKAGES.entrySet())
		{
			int packageId = entry.getKey();
			PremiumPackage pkg = entry.getValue();

			boolean canAfford = currentCurrency >= pkg.price;

			// 套餐信息
			sb.append("<tr>");
			sb.append("<td align=center><font color=\"FFCC33\">");
			sb.append(pkg.days).append(" 天會員  價格: ");
			sb.append("<font color=\"FFD700\">").append(formatNumber(pkg.price)).append("</font>");
			if (pkg.hasDiscount())
			{
				sb.append(" <font color=\"FF6600\" size=\"1\">(").append(pkg.getDiscountText()).append(")</font>");
			}
			sb.append("</font></td>");
			sb.append("</tr>");

			// 購買按鈕
			sb.append("<tr>");
			sb.append("<td align=center>");
			if (canAfford)
			{
				sb.append("<button action=\"bypass -h Quest PremiumShop buy_").append(packageId)
						.append("\" value=\"購買 ").append(pkg.days).append(" 天會員\" ")
						.append("width=\"200\" height=\"31\" ")
						.append("back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" ")
						.append("fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
			}
			else
			{
				sb.append("<font color=\"808080\">購買 ").append(pkg.days).append(" 天會員 (貨幣不足)</font>");
			}
			sb.append("</td>");
			sb.append("</tr>");

			// 間隔
			sb.append("<tr>");
			sb.append("<td height=10></td>");
			sb.append("</tr>");
		}

		return sb.toString();
	}

	/**
	 * 處理購買請求
	 */
	private void processPurchase(Player player, int packageId)
	{
		// 檢查套餐是否存在
		PremiumPackage pkg = PREMIUM_PACKAGES.get(packageId);
		if (pkg == null)
		{
			player.sendMessage("無效的套餐！");
			showMainPage(player);
			return;
		}

		// 檢查會員系統是否開啟
		if (!PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED)
		{
			player.sendMessage("會員系統目前未開啟。");
			showMainPage(player);
			return;
		}

		// 檢查貨幣是否足夠
		long currentCurrency = player.getInventory().getInventoryItemCount(CURRENCY_ITEM_ID, -1);
		if (currentCurrency < pkg.price)
		{
			player.sendMessage("========================================");
			player.sendMessage("貨幣不足！");
			player.sendMessage("所需數量：" + formatNumber(pkg.price));
			player.sendMessage("當前擁有：" + formatNumber(currentCurrency));
			player.sendMessage("還需要：" + formatNumber(pkg.price - currentCurrency));
			player.sendMessage("========================================");
			showMainPage(player);
			return;
		}

		// 扣除貨幣
		player.destroyItemByItemId(ItemProcessType.FEE, CURRENCY_ITEM_ID, pkg.price, player, true);

		// 添加會員時間
		PremiumManager.getInstance().addPremiumTime(player.getAccountName(), pkg.days, TimeUnit.DAYS);

		// 如果啟用PC Cafe系統，運行它
		if (PremiumSystemConfig.PC_CAFE_RETAIL_LIKE)
		{
			PcCafePointsManager.getInstance().run(player);
		}

		// 發送成功消息
		SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
		long endDate = PremiumManager.getInstance().getPremiumExpiration(player.getAccountName());

		player.sendMessage("========================================");
		player.sendMessage("購買成功！");
		player.sendMessage("購買天數：" + pkg.days + " 天");
		player.sendMessage("消耗道具：" + formatNumber(pkg.price));
		player.sendMessage("會員到期：" + format.format(endDate));
		player.sendMessage("會員權益已即時生效，同帳號所有角色共享");
		player.sendMessage("========================================");

		// 刷新主頁面
		showMainPage(player);
	}

	// ==================== 輔助方法 ====================

	/**
	 * 格式化數字（添加千分位）
	 */
	private String formatNumber(long number)
	{
		return String.format("%,d", number);
	}

	public static void main(String[] args)
	{
		new PremiumShop();
		System.out.println("【系統】會員購買系統載入完畢！");
	}
}