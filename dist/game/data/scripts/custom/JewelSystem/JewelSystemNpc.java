/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package custom.JewelSystem;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.jewel.JewelSystemConfig;
import org.l2jmobius.gameserver.model.jewel.JewelSystemManager;
import org.l2jmobius.gameserver.model.jewel.PlayerJewelData;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 寶玉系統 NPC 腳本
 * @author YourName
 */
public class JewelSystemNpc extends Script
{
	private static final int NPC_ID = 900036;

	public JewelSystemNpc()
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
		else if (event.equals("activate"))
		{
			JewelSystemManager.getInstance().activateSystem(player);
			return showMainPage(player);
		}
		else if (event.equals("reveal"))
		{
			JewelSystemManager.getInstance().revealValue(player);
			return showMainPage(player);
		}
		else if (event.equals("breakthrough"))
		{
			JewelSystemManager.getInstance().breakthrough(player);
			return showMainPage(player);
		}
		else if (event.equals("reset"))
		{
			JewelSystemManager.getInstance().resetCurrentTier(player);
			return showMainPage(player);
		}
		else if (event.equals("confirm_reset"))
		{
			return showResetConfirmPage(player);
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
		final PlayerJewelData data = JewelSystemManager.getInstance().getPlayerData(player);
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);

		if (!data.isActivated())
		{
			// 未啟用頁面
			html.setFile(player, "data/scripts/custom/JewelSystem/not_activated.htm");

			final String activationItemName = ItemData.getInstance().getTemplate(JewelSystemConfig.ACTIVATION_ITEM_ID).getName();
			final long hasCount = player.getInventory().getInventoryItemCount(JewelSystemConfig.ACTIVATION_ITEM_ID, -1);

			html.replace("%activation_item%", activationItemName);
			html.replace("%has_count%", String.valueOf(hasCount));
		}
		else
		{
			// 已啟用頁面
			html.setFile(player, "data/scripts/custom/JewelSystem/main.htm");

			// 總加成
			html.replace("%total_bonus%", formatNumber(data.calculateTotalBonus()));

			// 構建階段行
			final StringBuilder stageRows = buildStageRows(player, data);
			html.replace("%stage_rows%", stageRows.toString());

			// 初始化按鈕
			if (data.canReset())
			{
				html.replace("%reset_button%", "<button value=\"初始化\" action=\"bypass -h Quest JewelSystemNpc confirm_reset\" width=\"95\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			}
			else
			{
				html.replace("%reset_button%", "");
			}
		}

		player.sendPacket(html);
		return null;
	}

	/**
	 * 顯示初始化確認頁面
	 */
	private String showResetConfirmPage(Player player)
	{
		final PlayerJewelData data = JewelSystemManager.getInstance().getPlayerData(player);
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/JewelSystem/confirm_reset.htm");

		final int currentStage = data.getCurrentValueStage();

		// 計算總損失
		long totalLoss = 0;
		for (int i = 1; i <= currentStage; i++)
		{
			totalLoss += data.getStageValue(i);
		}

		html.replace("%current_stage%", String.valueOf(currentStage));
		html.replace("%total_loss%", formatNumber(totalLoss));

		player.sendPacket(html);
		return null;
	}

	/**
	 * 構建階段行 (數值 | 階段 | 加成%)
	 */
	private StringBuilder buildStageRows(Player player, PlayerJewelData data)
	{
		final StringBuilder sb = new StringBuilder();
		final int currentValueStage = data.getCurrentValueStage();
		final int currentBonusStage = data.getCurrentBonusStage();

		for (int i = 1; i <= 20; i++)
		{
			final long value = data.getStageValue(i);
			final int tier = ((i - 1) / 5) + 1;

			// 區間背景色
			String rowColor;
			switch (tier)
			{
				case 1:
					rowColor = "222222";
					break;
				case 2:
					rowColor = "282828";
					break;
				case 3:
					rowColor = "2e2e2e";
					break;
				default:
					rowColor = "343434";
					break;
			}

			sb.append("<tr bgcolor=\"").append(rowColor).append("\" height=18>");

			// 左欄: 數值
			sb.append("<td width=94>");
			if (i <= currentValueStage && value > 0)
			{
				sb.append("<font color=\"00FF00\">").append(formatNumber(value)).append("</font>");
			}
			else if (i == currentValueStage + 1)
			{
				sb.append("<font color=\"FFFF00\">待揭露</font>");
			}
			else
			{
				sb.append("<font color=\"666666\">-</font>");
			}
			sb.append("</td>");

			// 中欄: 階段 (用固定格式讓寬度一致)
			String stageNum;
			if (i < 10)
			{
				stageNum = "  " + i + "  ";
			}
			else
			{
				final int tens = i / 10;
				final int ones = i % 10;
				stageNum = " " + tens + " " + ones + " ";
			}
			sb.append("<td width=95><font color=\"FFFFFF\">＜第").append(stageNum).append("階＞</font></td>");

			// 右欄: 加成% (顯示該階段的配置加成百分比)
			sb.append("<td width=94>");
			final int bonusPercent = JewelSystemConfig.getBonusPercent(i);
			if (i <= currentBonusStage)
			{
				// 已突破 - 青色
				sb.append("<font color=\"00FFFF\">").append(bonusPercent).append("%(已突破)</font>");
			}
			else if (i == currentBonusStage + 1)
			{
				// 待突破 - 橙色
				sb.append("<font color=\"FFAA00\">").append(bonusPercent).append("%(當前)</font>");
			}
			else
			{
				// 未到達 - 灰色
				sb.append("<font color=\"666666\">").append(bonusPercent).append("%(待突破)</font>");
			}
			sb.append("</td>");

			sb.append("</tr>");
		}

		return sb;
	}

	/**
	 * 格式化數字
	 */
	private String formatNumber(long number)
	{
		if (number >= 100000000)
		{
			return String.format("%.2f億", number / 100000000.0);
		}
		else if (number >= 10000)
		{
			return String.format("%.2f萬", number / 10000.0);
		}
		return String.valueOf(number);
	}

	public static void main(String[] args)
	{
		new JewelSystemNpc();
		System.out.println("【寶玉系統】NPC腳本載入完成");
	}
}
