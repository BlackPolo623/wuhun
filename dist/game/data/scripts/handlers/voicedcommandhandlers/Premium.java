package handlers.voicedcommandhandlers;

import java.text.SimpleDateFormat;

import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Premium 會員系統語音命令處理器
 * 支援指令: .premium, .會員
 * @author 黑普羅
 */
public class Premium implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
			{
					"premium",
					"會員"
			};

	// 自動轉生變數名稱（需與 Hunhuan.java 一致）
	private static final String AUTO_REBIRTH_VAR = "AutoSoulRing";

	@Override
	public boolean onCommand(String command, Player player, String target)
	{
		if (!PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED)
		{
			player.sendMessage("會員系統目前未開啟。");
			return false;
		}

		if (command.equals("premium") || command.equals("會員"))
		{
			// 檢查是否有參數（用於開關自動轉生）
			if (target != null && !target.isEmpty())
			{
				if (target.equals("toggle_auto"))
				{
					toggleAutoRebirth(player);
					return true;
				}
			}

			showPremiumInfo(player);
			return true;
		}

		return false;
	}

	/**
	 * 切換自動轉生開關
	 */
	private void toggleAutoRebirth(Player player)
	{
		// 檢查是否為會員
		long endDate = PremiumManager.getInstance().getPremiumExpiration(player.getAccountName());
		if (endDate <= 0)
		{
			player.sendMessage("只有會員才能使用自動轉生功能！");
			showPremiumInfo(player);
			return;
		}

		// 切換開關
		boolean currentStatus = player.getVariables().getBoolean(AUTO_REBIRTH_VAR, false);
		boolean newStatus = !currentStatus;

		player.getVariables().set(AUTO_REBIRTH_VAR, newStatus);

		String statusText = newStatus ? "已開啟" : "已關閉";
		player.sendMessage("========================================");
		player.sendMessage("自動轉生功能：" + statusText);
		if (newStatus)
		{
			player.sendMessage("達到80級時將自動轉生獲得魂環");
		}
		player.sendMessage("========================================");

		// 刷新界面
		showPremiumInfo(player);
	}

	/**
 * 顯示會員資訊界面
 */
private void showPremiumInfo(Player player)
{
	final SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
	final long endDate = PremiumManager.getInstance().getPremiumExpiration(player.getAccountName());
	final boolean isPremium = endDate > 0;

	final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
	final StringBuilder sb = new StringBuilder();

	sb.append("<html><head><title>會員系統</title></head>");
	sb.append("<body scroll=\"no\">");
	sb.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"292\" height=\"358\">");
	sb.append("<tr><td valign=\"top\" align=\"center\">");

	// 標題區域
	sb.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
	sb.append("<tr><td align=\"center\" width=\"290\" height=\"90\" background=\"L2UI_EPIC.HtmlWnd.HtmlWnd_PremiumManagerWnd_Store_IMG\"></td></tr>");
	sb.append("</table>");

	sb.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
	sb.append("<tr><td height=\"20\"></td></tr>");
	sb.append("<tr><td align=\"center\"><font color=\"LEVEL\" size=\"6\"><b>會員系統</b></font></td></tr>");
	sb.append("<tr><td height=\"15\"></td></tr>");

	// 帳號狀態
	if (isPremium)
	{
		sb.append("<tr><td align=\"center\"><font color=\"00FF66\">帳號狀態：</font><font color=\"LEVEL\" size=\"4\"><b>高級會員</b></font></td></tr>");
	}
	else
	{
		sb.append("<tr><td align=\"center\"><font color=\"00FF66\">帳號狀態：</font><font color=\"808080\">普通帳號</font></td></tr>");
	}
	sb.append("<tr><td height=\"10\"></td></tr>");

	sb.append("<tr><td align=\"center\"><img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"></td></tr>");
	sb.append("<tr><td height=\"10\"></td></tr>");

	// 當前倍率顯示
	sb.append("<tr><td align=\"center\"><font color=\"FFCC33\" size=\"3\"><b>當前倍率</b></font></td></tr>");
	sb.append("<tr><td height=\"8\"></td></tr>");
	sb.append("</table>");  // 關閉第一個內容表格

	// 倍率表格
	sb.append("<table width=\"270\" bgcolor=\"111111\" border=\"0\" cellspacing=\"1\" cellpadding=\"3\">");
	sb.append("<tr bgcolor=\"333333\">");
	sb.append("<td width=\"135\" align=\"center\"><font color=\"FFCC33\">項目</font></td>");
	sb.append("<td width=\"135\" align=\"center\"><font color=\"FFCC33\">倍率</font></td>");
	sb.append("</tr>");

	// 根據是否為會員顯示不同倍率
	if (isPremium)
	{
		addRateRow(sb, "經驗倍率", RatesConfig.RATE_XP * PremiumSystemConfig.PREMIUM_RATE_XP);
		addRateRow(sb, "技能點倍率", RatesConfig.RATE_SP * PremiumSystemConfig.PREMIUM_RATE_SP);
		addRateRow(sb, "掉落機率", RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER * PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE);
		addRateRow(sb, "掉落數量", RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER * PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT);
	}
	else
	{
		addRateRow(sb, "經驗倍率", RatesConfig.RATE_XP);
		addRateRow(sb, "技能點倍率", RatesConfig.RATE_SP);
		addRateRow(sb, "掉落機率", RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER);
		addRateRow(sb, "掉落數量", RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER);
	}

	sb.append("</table>");  // 關閉倍率表格

	// 開始新的內容表格
	sb.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
	sb.append("<tr><td height=\"10\"></td></tr>");
	sb.append("<tr><td align=\"center\"><img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"></td></tr>");
	sb.append("<tr><td height=\"10\"></td></tr>");

	// 會員到期時間或升級說明
	if (isPremium)
	{
		// 到期時間區域
		sb.append("<tr><td align=\"center\"><font color=\"FFCC33\" size=\"3\"><b>到期時間</b></font></td></tr>");
		sb.append("<tr><td height=\"5\"></td></tr>");
		sb.append("<tr><td align=\"center\"><font color=\"FFFF00\" size=\"3\">").append(format.format(endDate)).append("</font></td></tr>");
		sb.append("<tr><td height=\"3\"></td></tr>");
		sb.append("<tr><td align=\"center\"><font color=\"808080\" size=\"1\">當前時間：").append(format.format(System.currentTimeMillis())).append("</font></td></tr>");

		sb.append("<tr><td height=\"10\"></td></tr>");
		sb.append("<tr><td align=\"center\"><img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"></td></tr>");
		sb.append("<tr><td height=\"10\"></td></tr>");

		// 會員特權標題
		sb.append("<tr><td align=\"center\"><font color=\"FFCC33\" size=\"3\"><b>會員特權</b></font></td></tr>");
		sb.append("<tr><td height=\"8\"></td></tr>");
		sb.append("</table>");  // 關閉內容表格

		// 自動轉生狀態表格
		boolean autoRebirthEnabled = player.getVariables().getBoolean(AUTO_REBIRTH_VAR, false);
		String statusText = autoRebirthEnabled ? "已開啟" : "已關閉";
		String statusColor = autoRebirthEnabled ? "00FF66" : "808080";
		String buttonText = autoRebirthEnabled ? "關閉自動轉生" : "開啟自動轉生";

		sb.append("<table width=\"270\" bgcolor=\"111111\" border=\"0\" cellspacing=\"1\" cellpadding=\"3\">");
		sb.append("<tr bgcolor=\"222222\">");
		sb.append("<td align=\"center\" width=\"135\"><font color=\"00FF66\">自動轉生</font></td>");
		sb.append("<td align=\"center\" width=\"135\"><font color=\"").append(statusColor).append("\">").append(statusText).append("</font></td>");
		sb.append("</tr>");
		sb.append("</table>");  // 關閉狀態表格

		// 按鈕區域
		sb.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
		sb.append("<tr><td height=\"10\"></td></tr>");
		sb.append("<tr><td align=\"center\">");
		sb.append("<button action=\"bypass -h voice .會員 toggle_auto\" value=\"").append(buttonText).append("\" width=\"180\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</td></tr>");
		sb.append("<tr><td height=\"5\"></td></tr>");
		sb.append("<tr><td align=\"center\"><font color=\"808080\" size=\"1\">※ 達到80級時自動轉生獲得魂環</font></td></tr>");
	}
	else
	{
		// 非會員升級說明
		sb.append("<tr><td align=\"center\"><font color=\"FFCC33\" size=\"3\"><b>升級為高級會員可享受</b></font></td></tr>");
		sb.append("<tr><td height=\"8\"></td></tr>");
		sb.append("<tr><td align=\"center\"><font color=\"00FF66\">• 多項倍率</font><font color=\"LEVEL\"> x3</font></td></tr>");
		sb.append("<tr><td align=\"center\"><font color=\"00FF66\">• 80級自動轉生特權</font></td></tr>");
		sb.append("<tr><td align=\"center\"><font color=\"00FF66\">• 未來還有其他功能</font></td></tr>");
	}

	sb.append("<tr><td height=\"10\"></td></tr>");
	sb.append("<tr><td align=\"center\"><img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"></td></tr>");
	sb.append("<tr><td height=\"10\"></td></tr>");

	// 會員規則說明
	sb.append("<tr><td align=\"center\"><font color=\"FFCC33\">會員規則</font></td></tr>");
	sb.append("<tr><td height=\"5\"></td></tr>");
	sb.append("<tr><td align=\"center\"><font color=\"70FFCA\" size=\"1\">• 會員權益無法轉移</font></td></tr>");
	sb.append("<tr><td align=\"center\"><font color=\"70FFCA\" size=\"1\">• 會員權益不影響隊友</font></td></tr>");
	sb.append("<tr><td align=\"center\"><font color=\"70FFCA\" size=\"1\">• 會員權益對同帳號所有角色生效</font></td></tr>");

	if (isPremium)
	{
		sb.append("<tr><td height=\"10\"></td></tr>");
		sb.append("<tr><td align=\"center\"><font color=\"LEVEL\">感謝您對伺服器的支持！</font></td></tr>");
	}

	sb.append("<tr><td height=\"15\"></td></tr>");
	sb.append("</table>");  // 關閉最後的內容表格

	sb.append("</td></tr></table>");  // 關閉最外層表格
	sb.append("</body></html>");

	html.setHtml(sb.toString());
	player.sendPacket(html);
}
	/**
	 * 添加倍率行到表格
	 */
	private void addRateRow(StringBuilder sb, String name, double rate)
	{
		sb.append("<tr bgcolor=\"222222\">");
		sb.append("<td align=\"center\"><font color=\"00FF66\">").append(name).append("</font></td>");
		sb.append("<td align=\"center\"><font color=\"FFFF00\" size=\"3\">x").append(String.format("%.1f", rate)).append("</font></td>");
		sb.append("</tr>");
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}