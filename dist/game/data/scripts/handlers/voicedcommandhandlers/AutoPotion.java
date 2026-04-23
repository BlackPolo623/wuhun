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
package handlers.voicedcommandhandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.config.custom.AutoPotionsConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.taskmanagers.AutoPotionTaskManager;

/**
 * 高級自動喝水系統
 * 指令格式：bypass -h voice .COMMAND params
 *
 * 快速開關：.開水 / .apon　　.關水 / .apoff
 * 開啟介面：.自動喝水 / .ap
 * 個別開關：.hp開 .hpon　.hp關 .hpoff　.mp開 .mpon　.mp關 .mpoff　.cp開 .cpon　.cp關 .cpoff
 */
public class AutoPotion implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		// 快速指令
		"開水", "apon",
		"關水", "apoff",
		"自動喝水", "ap",
		"hp開", "hpon", "hp關", "hpoff",
		"mp開", "mpon", "mp關", "mpoff",
		"cp開", "cpon", "cp關", "cpoff",
		// 介面頁面（從 bypass -h voice .XXX 進入）
		"ap_main",
		"ap_on", "ap_off",
		"ap_toggle_hp", "ap_toggle_mp", "ap_toggle_cp",
		"ap_hp", "ap_mp", "ap_cp",
		"ap_save_hp", "ap_save_mp", "ap_save_cp",
		"ap_select_hp", "ap_select_mp", "ap_select_cp",
		"ap_pick_hp", "ap_pick_mp", "ap_pick_cp",
		"ap_remove_hp", "ap_remove_mp", "ap_remove_cp",
	};

	private static final String PATH = "data/scripts/handlers/voicedcommandhandlers/AutoPotion/";

	private static final int FREE_MAX_SLOTS = 1;
	private static final int VIP_MAX_SLOTS = 5;
	private static final int FREE_INTERVAL = 5000;
	private static final int MIN_INTERVAL = 1000;
	private static final int MAX_INTERVAL = 20000;

	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		if (!AutoPotionsConfig.AUTO_POTIONS_ENABLED || (player == null))
		{
			return false;
		}

		// params 可能為 null
		final String p = (params != null) ? params.trim() : "";

		// ── 快速指令 ──────────────────────────────────────────────────────────
		switch (command)
		{
			case "開水":
			case "apon":
				setEnabled(player, true);
				player.sendMessage("[自動喝水] 已開啟。");
				return true;
			case "關水":
			case "apoff":
				setEnabled(player, false);
				player.sendMessage("[自動喝水] 已關閉。");
				return true;
			case "hp開": case "hpon":
				player.getVariables().set(PlayerVariables.AP_HP_ENABLED, true);
				player.sendMessage("[自動喝水] HP 自動回復已開啟。");
				return true;
			case "hp關": case "hpoff":
				player.getVariables().set(PlayerVariables.AP_HP_ENABLED, false);
				player.sendMessage("[自動喝水] HP 自動回復已關閉。");
				return true;
			case "mp開": case "mpon":
				player.getVariables().set(PlayerVariables.AP_MP_ENABLED, true);
				player.sendMessage("[自動喝水] MP 自動回復已開啟。");
				return true;
			case "mp關": case "mpoff":
				player.getVariables().set(PlayerVariables.AP_MP_ENABLED, false);
				player.sendMessage("[自動喝水] MP 自動回復已關閉。");
				return true;
			case "cp開": case "cpon":
				player.getVariables().set(PlayerVariables.AP_CP_ENABLED, true);
				player.sendMessage("[自動喝水] CP 自動回復已開啟。");
				return true;
			case "cp關": case "cpoff":
				player.getVariables().set(PlayerVariables.AP_CP_ENABLED, false);
				player.sendMessage("[自動喝水] CP 自動回復已關閉。");
				return true;
		}

		// ── 介面事件 ──────────────────────────────────────────────────────────
		switch (command)
		{
			case "自動喝水": case "ap": case "ap_main":
				showMain(player);
				return true;
			case "ap_on":
				setEnabled(player, true);
				showMain(player);
				return true;
			case "ap_off":
				setEnabled(player, false);
				showMain(player);
				return true;
			case "ap_toggle_hp":
			{
				final boolean cur = player.getVariables().getBoolean(PlayerVariables.AP_HP_ENABLED, false);
				player.getVariables().set(PlayerVariables.AP_HP_ENABLED, !cur);
				showMain(player);
				return true;
			}
			case "ap_toggle_mp":
			{
				final boolean cur = player.getVariables().getBoolean(PlayerVariables.AP_MP_ENABLED, false);
				player.getVariables().set(PlayerVariables.AP_MP_ENABLED, !cur);
				showMain(player);
				return true;
			}
			case "ap_toggle_cp":
			{
				final boolean cur = player.getVariables().getBoolean(PlayerVariables.AP_CP_ENABLED, false);
				player.getVariables().set(PlayerVariables.AP_CP_ENABLED, !cur);
				showMain(player);
				return true;
			}
			case "ap_hp":
				showType(player, "hp");
				return true;
			case "ap_mp":
				showType(player, "mp");
				return true;
			case "ap_cp":
				showType(player, "cp");
				return true;
		}

		// ── 帶參數事件 ────────────────────────────────────────────────────────
		// command = "ap_save_hp", params = "75 2000"
		if (command.startsWith("ap_save_"))
		{
			handleSave(player, command, p);
			return true;
		}
		if (command.startsWith("ap_select_"))
		{
			handleSelect(player, command, p);
			return true;
		}
		if (command.startsWith("ap_pick_"))
		{
			handlePick(player, command, p);
			return true;
		}
		if (command.startsWith("ap_remove_"))
		{
			handleRemove(player, command, p);
			return true;
		}

		return true;
	}

	// ── 主頁 ─────────────────────────────────────────────────────────────────

	private void showMain(Player player)
	{
		final boolean isPremium = isPremium(player);
		final PlayerVariables vars = player.getVariables();
		final boolean enabled = vars.getBoolean(PlayerVariables.AP_ENABLED, false);
		final boolean hpEnabled = vars.getBoolean(PlayerVariables.AP_HP_ENABLED, false);
		final boolean mpEnabled = vars.getBoolean(PlayerVariables.AP_MP_ENABLED, false);
		final boolean cpEnabled = vars.getBoolean(PlayerVariables.AP_CP_ENABLED, false);

		final String statusColor = enabled ? "00FF00" : "FF3333";
		final String statusText = enabled ? "● 運行中" : "○ 已停止";
		final String premiumText = isPremium ? "<font color=\"FFCC00\">★ 會員</font>" : "<font color=\"777777\">一般玩家</font>";

		final double hpPct = (player.getStatus().getCurrentHp() / player.getMaxHp()) * 100.0;
		final double mpPct = (player.getStatus().getCurrentMp() / player.getMaxMp()) * 100.0;
		final double cpPct = (player.getStatus().getCurrentCp() / player.getMaxCp()) * 100.0;
		final int hpThresh = isPremium ? vars.getInt(PlayerVariables.AP_HP_PERCENT, AutoPotionsConfig.AUTO_HP_PERCENTAGE) : AutoPotionsConfig.AUTO_HP_PERCENTAGE;
		final int mpThresh = isPremium ? vars.getInt(PlayerVariables.AP_MP_PERCENT, AutoPotionsConfig.AUTO_MP_PERCENTAGE) : AutoPotionsConfig.AUTO_MP_PERCENTAGE;
		final int cpThresh = isPremium ? vars.getInt(PlayerVariables.AP_CP_PERCENT, AutoPotionsConfig.AUTO_CP_PERCENTAGE) : AutoPotionsConfig.AUTO_CP_PERCENTAGE;

		// 快捷開關按鈕文字與顏色
		final String hpSwColor = hpEnabled ? "00EE00" : "EE3333";
		final String mpSwColor = mpEnabled ? "00EE00" : "EE3333";
		final String cpSwColor = cpEnabled ? "00EE00" : "EE3333";
		final String hpSwText = hpEnabled ? "HP ●" : "HP ○";
		final String mpSwText = mpEnabled ? "MP ●" : "MP ○";
		final String cpSwText = cpEnabled ? "CP ●" : "CP ○";
		// 狀態文字
		final String hpStatusColor = hpEnabled ? "00EE00" : "EE3333";
		final String mpStatusColor = mpEnabled ? "00EE00" : "EE3333";
		final String cpStatusColor = cpEnabled ? "00EE00" : "EE3333";
		final String hpStatusText = hpEnabled ? "開" : "關";
		final String mpStatusText = mpEnabled ? "開" : "關";
		final String cpStatusText = cpEnabled ? "開" : "關";
		// 底部總開關狀態說明
		final String apStatusSummary = enabled
			? "<font color=\"00FF00\">自動喝水功能目前：運行中</font>"
			: "<font color=\"FF3333\">自動喝水功能目前：已停止</font>";

		final NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
		msg.setFile(player, PATH + "ap_main.htm");
		msg.replace("%premiumText%", premiumText);
		msg.replace("%statusColor%", statusColor);
		msg.replace("%statusText%", statusText);
		msg.replace("%hpSwColor%", hpSwColor);
		msg.replace("%hpSwText%", hpSwText);
		msg.replace("%mpSwColor%", mpSwColor);
		msg.replace("%mpSwText%", mpSwText);
		msg.replace("%cpSwColor%", cpSwColor);
		msg.replace("%cpSwText%", cpSwText);
		msg.replace("%hpStatusColor%", hpStatusColor);
		msg.replace("%hpStatusText%", hpStatusText);
		msg.replace("%mpStatusColor%", mpStatusColor);
		msg.replace("%mpStatusText%", mpStatusText);
		msg.replace("%cpStatusColor%", cpStatusColor);
		msg.replace("%cpStatusText%", cpStatusText);
		msg.replace("%apStatusSummary%", apStatusSummary);
		msg.replace("%hpGauge%", buildGauge(hpPct, "CC2222"));
		msg.replace("%hpPct%", String.format("%.0f", hpPct));
		msg.replace("%hpThresh%", String.valueOf(hpThresh));
		msg.replace("%mpGauge%", buildGauge(mpPct, "2255CC"));
		msg.replace("%mpPct%", String.format("%.0f", mpPct));
		msg.replace("%mpThresh%", String.valueOf(mpThresh));
		msg.replace("%cpGauge%", buildGauge(cpPct, "CC8800"));
		msg.replace("%cpPct%", String.format("%.0f", cpPct));
		msg.replace("%cpThresh%", String.valueOf(cpThresh));
		player.sendPacket(msg);
	}

	// ── HP / MP / CP 設定頁 ───────────────────────────────────────────────────

	private void showType(Player player, String type)
	{
		final boolean isPremium = isPremium(player);
		final PlayerVariables vars = player.getVariables();

		final String enabledKey = enabledKey(type);
		final String percentKey = percentKey(type);
		final String intervalKey = intervalKey(type);
		final String itemsKey = itemsKey(type);
		final int defaultPercent = defaultPercent(type);
		final Set<Integer> whitelist = getWhitelist(type);
		final String typeCN = typeCN(type);
		final String gaugeColor = gaugeColor(type);
		final double currentPct = currentPct(player, type);

		final boolean typeEnabled = vars.getBoolean(enabledKey, false);
		final int percent = isPremium ? vars.getInt(percentKey, defaultPercent) : defaultPercent;
		final int interval = isPremium ? vars.getInt(intervalKey, FREE_INTERVAL) : FREE_INTERVAL;
		final int maxSlots = isPremium ? VIP_MAX_SLOTS : FREE_MAX_SLOTS;
		final List<Integer> itemIds = parseItems(vars.getString(itemsKey, ""));

		// ── 閾值 / 間隔區塊 ──────────────────────────────────────────────────
		final StringBuilder threshBlock = new StringBuilder();
		if (isPremium)
		{
			// 兩列等寬：標籤100 | 輸入框80 | 目前值100
			threshBlock.append("<table width=280 bgcolor=1A1A1A><tr>");
			threshBlock.append("<td width=100><font color=\"CCCCCC\">觸發閾值（%）</font></td>");
			threshBlock.append("<td width=80><edit var=\"ap_pct\" width=60 height=15></td>");
			threshBlock.append("<td width=100><font color=\"FFFF44\">目前：").append(percent).append("%</font></td>");
			threshBlock.append("</tr></table>");
			threshBlock.append("<table width=280 bgcolor=141414><tr>");
			threshBlock.append("<td width=100><font color=\"CCCCCC\">觸發間隔（ms）</font></td>");
			threshBlock.append("<td width=80><edit var=\"ap_ivl\" width=60 height=15></td>");
			threshBlock.append("<td width=100><font color=\"FFFF44\">目前：").append(interval).append("</font></td>");
			threshBlock.append("</tr></table>");
			threshBlock.append("<table width=280><tr><td height=18><font color=\"555555\">※ 1000=1秒　5000=5秒　20000=20秒　範圍：1000～20000</font></td></tr></table>");
		}
		else
		{
			threshBlock.append("<table width=280 bgcolor=1A1A1A><tr>");
			threshBlock.append("<td width=100><font color=\"CCCCCC\">觸發閾值</font></td>");
			threshBlock.append("<td width=80></td>");
			threshBlock.append("<td width=100><font color=\"FFFF44\">").append(percent).append("%（固定）</font></td>");
			threshBlock.append("</tr></table>");
			threshBlock.append("<table width=280 bgcolor=141414><tr>");
			threshBlock.append("<td width=100><font color=\"CCCCCC\">觸發間隔</font></td>");
			threshBlock.append("<td width=80></td>");
			threshBlock.append("<td width=100><font color=\"FFFF44\">5000ms（固定）</font></td>");
			threshBlock.append("</tr></table>");
			threshBlock.append("<table width=280><tr><td height=18><font color=\"444444\">※ 升級為會員可自訂閾值與間隔</font></td></tr></table>");
		}

		// ── 藥水槽位 ──────────────────────────────────────────────────────────
		final StringBuilder slotsBlock = new StringBuilder();
		for (int i = 0; i < maxSlots; i++)
		{
			final String bg = (i % 2 == 0) ? "1A1A1A" : "141414";
			slotsBlock.append("<table width=280 bgcolor=").append(bg).append("><tr>");
			slotsBlock.append("<td width=25 align=center><font color=\"888888\">#").append(i + 1).append("</font></td>");
			if (i < itemIds.size())
			{
				final int itemId = itemIds.get(i);
				final long owned = player.getInventory().getInventoryItemCount(itemId, -1);
				slotsBlock.append("<td width=140><font color=\"CCCCCC\">").append(getItemName(itemId)).append("</font></td>");
				slotsBlock.append("<td width=55 align=center><font color=\"").append(owned > 0 ? "00FF88" : "FF6666").append("\">×").append(owned).append("</font></td>");
				slotsBlock.append("<td width=60 align=center>");
				slotsBlock.append("<button value=\"移除\" action=\"bypass -h voice .ap_remove_").append(type).append(" ").append(i);
				slotsBlock.append("\" width=50 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				slotsBlock.append("</td>");
			}
			else
			{
				slotsBlock.append("<td width=160><font color=\"555555\">─ 未設定 ─</font></td>");
				slotsBlock.append("<td width=95 align=center>");
				slotsBlock.append("<button value=\"+ 選擇\" action=\"bypass -h voice .ap_select_").append(type).append(" ").append(i);
				slotsBlock.append("\" width=60 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				slotsBlock.append("</td>");
			}
			slotsBlock.append("</tr></table>");
		}
		if (!isPremium)
		{
			slotsBlock.append("<table width=280><tr><td><font color=\"444444\">↳ 升級為會員可設定最多 ").append(VIP_MAX_SLOTS).append(" 個優先道具</font></td></tr></table>");
		}

		// 開關按鈕文字
		final String toggleVal = typeEnabled ? ("關閉 " + typeCN + " 回復") : ("開啟 " + typeCN + " 回復");
		final String toggleBypass = typeEnabled ? ("ap_save_" + type + " disable") : ("ap_save_" + type + " enable");
		// 儲存 bypass（VIP 讀輸入框；非會員 keep keep）
		final String saveBypass = isPremium ? ("ap_save_" + type + " $ap_pct $ap_ivl") : ("ap_save_" + type + " keep keep");

		final NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
		msg.setFile(player, PATH + "ap_type.htm");
		msg.replace("%typeCN%", typeCN);
		msg.replace("%typeKey%", type);
		msg.replace("%gauge%", buildGauge(currentPct, gaugeColor));
		msg.replace("%currentPct%", String.format("%.0f", currentPct));
		msg.replace("%threshBlock%", threshBlock.toString());
		msg.replace("%slotsBlock%", slotsBlock.toString());
		msg.replace("%toggleVal%", toggleVal);
		msg.replace("%toggleBypass%", toggleBypass);
		msg.replace("%saveBypass%", saveBypass);
		player.sendPacket(msg);
	}

	// ── 藥水選擇頁 ────────────────────────────────────────────────────────────

	private void handleSelect(Player player, String command, String params)
	{
		// command = "ap_select_hp", params = "0"
		final String type = command.substring("ap_select_".length()); // "hp"
		final int slot = safeInt(params, 0);
		final Set<Integer> whitelist = getWhitelist(type);
		final String typeCN = typeCN(type);

		final StringBuilder rows = new StringBuilder();
		boolean odd = true;
		boolean any = false;
		for (int itemId : whitelist)
		{
			if (itemId <= 0) continue;
			any = true;
			final String bg = odd ? "1A1A1A" : "141414";
			odd = !odd;
			final long owned = player.getInventory().getInventoryItemCount(itemId, -1);
			rows.append("<table width=280 bgcolor=").append(bg).append("><tr>");
			rows.append("<td width=145><font color=\"CCCCCC\">").append(getItemName(itemId)).append("</font></td>");
			rows.append("<td width=55 align=center><font color=\"").append(owned > 0 ? "00FF88" : "666666").append("\">×").append(owned).append("</font></td>");
			rows.append("<td width=80 align=center>");
			rows.append("<button value=\"選擇\" action=\"bypass -h voice .ap_pick_").append(type).append(" ").append(slot).append(" ").append(itemId);
			rows.append("\" width=55 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			rows.append("</td>");
			rows.append("</tr></table>");
		}
		if (!any)
		{
			rows.append("<table width=280><tr><td align=center><font color=\"555555\">（ini 中無設定可用道具）</font></td></tr></table>");
		}

		final NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
		msg.setFile(player, PATH + "ap_select.htm");
		msg.replace("%typeCN%", typeCN);
		msg.replace("%typeKey%", type);
		msg.replace("%slot%", String.valueOf(slot + 1));
		msg.replace("%rows%", rows.toString());
		player.sendPacket(msg);
	}

	// ── 選取道具後寫入槽位 ────────────────────────────────────────────────────

	private void handlePick(Player player, String command, String params)
	{
		// command = "ap_pick_hp", params = "0 49663"
		final String type = command.substring("ap_pick_".length());
		final String[] parts = params.split(" ");
		if (parts.length < 2) return;
		final int slot = safeInt(parts[0], 0);
		final int itemId = safeInt(parts[1], 0);
		if (itemId <= 0) return;

		if (!getWhitelist(type).contains(itemId))
		{
			player.sendMessage("[自動喝水] 該道具不在允許清單中。");
			return;
		}

		final boolean isPremium = isPremium(player);
		final int maxSlots = isPremium ? VIP_MAX_SLOTS : FREE_MAX_SLOTS;
		if (slot >= maxSlots) return;

		final String itemsKey = itemsKey(type);
		final PlayerVariables vars = player.getVariables();
		final List<Integer> ids = parseItems(vars.getString(itemsKey, ""));

		// 去除重複
		ids.remove(Integer.valueOf(itemId));
		// 補齊至 slot
		while (ids.size() <= slot) ids.add(0);
		ids.set(slot, itemId);

		vars.set(itemsKey, joinItems(ids));
		showType(player, type);
	}

	// ── 移除槽位 ──────────────────────────────────────────────────────────────

	private void handleRemove(Player player, String command, String params)
	{
		// command = "ap_remove_hp", params = "1"
		final String type = command.substring("ap_remove_".length());
		final int slot = safeInt(params, -1);
		if (slot < 0) return;

		final String itemsKey = itemsKey(type);
		final PlayerVariables vars = player.getVariables();
		final List<Integer> ids = parseItems(vars.getString(itemsKey, ""));
		if (slot < ids.size()) ids.remove(slot);
		vars.set(itemsKey, joinItems(ids));
		showType(player, type);
	}

	// ── 儲存閾值 / 間隔 / 開關 ───────────────────────────────────────────────

	private void handleSave(Player player, String command, String params)
	{
		// command = "ap_save_hp"
		// params = "enable" / "disable" / "75 2000" / "keep keep"
		final String type = command.substring("ap_save_".length());
		final String enabledKey = enabledKey(type);
		final PlayerVariables vars = player.getVariables();

		if (params.equals("enable"))
		{
			vars.set(enabledKey, true);
			player.sendMessage("[自動喝水] " + typeCN(type) + " 自動回復已開啟。");
			showType(player, type);
			return;
		}
		if (params.equals("disable"))
		{
			vars.set(enabledKey, false);
			player.sendMessage("[自動喝水] " + typeCN(type) + " 自動回復已關閉。");
			showType(player, type);
			return;
		}

		// VIP 儲存數值：各欄位獨立驗證，空值保留現有設定
		if (isPremium(player) && !params.startsWith("keep"))
		{
			final String[] parts = params.split(" ", 2);
			boolean saved = false;

			// 閾值（第一欄）
			if (parts.length >= 1 && !parts[0].trim().isEmpty())
			{
				final int pct = safeInt(parts[0].trim(), -1);
				if (pct >= 1 && pct <= 99)
				{
					vars.set(percentKey(type), pct);
					saved = true;
				}
				else if (pct != -1)
				{
					player.sendMessage("[自動喝水] 閾值需在 1～99 之間，已略過。");
				}
			}

			// 間隔（第二欄）
			if (parts.length >= 2 && !parts[1].trim().isEmpty())
			{
				final int ivl = safeInt(parts[1].trim(), -1);
				if (ivl >= MIN_INTERVAL && ivl <= MAX_INTERVAL)
				{
					vars.set(intervalKey(type), ivl);
					saved = true;
				}
				else if (ivl != -1)
				{
					player.sendMessage("[自動喝水] 間隔需在 1000～20000 ms 之間，已略過。");
				}
			}

			if (saved)
			{
				final int curPct = vars.getInt(percentKey(type), defaultPercent(type));
				final int curIvl = vars.getInt(intervalKey(type), FREE_INTERVAL);
				player.sendMessage("[自動喝水] " + typeCN(type) + " 已更新（閾值：" + curPct + "% / 間隔：" + curIvl + "ms）。");
			}
		}
		showType(player, type);
	}

	// ── 總開關 ────────────────────────────────────────────────────────────────

	private void setEnabled(Player player, boolean enable)
	{
		player.getVariables().set(PlayerVariables.AP_ENABLED, enable);
		if (enable) AutoPotionTaskManager.getInstance().add(player);
		else AutoPotionTaskManager.getInstance().remove(player);
	}

	// ── 進度條 ────────────────────────────────────────────────────────────────

	private String buildGauge(double percent, String color)
	{
		final int filled = (int) Math.round(Math.min(100, Math.max(0, percent)) / 10.0);
		final StringBuilder sb = new StringBuilder("<table cellspacing=1><tr>");
		for (int i = 0; i < 10; i++)
		{
			sb.append("<td width=17 height=7 bgcolor=").append(i < filled ? color : "333333").append("></td>");
		}
		sb.append("</tr></table>");
		return sb.toString();
	}

	// ── 工具 ─────────────────────────────────────────────────────────────────

	private boolean isPremium(Player player)
	{
		return PremiumManager.getInstance().getPremiumExpiration(player.getAccountName()) > 0;
	}

	private String getItemName(int itemId)
	{
		final ItemTemplate tpl = ItemData.getInstance().getTemplate(itemId);
		return (tpl != null) ? tpl.getName() : "道具 #" + itemId;
	}

	private List<Integer> parseItems(String s)
	{
		final List<Integer> list = new ArrayList<>();
		if ((s == null) || s.isEmpty()) return list;
		for (String tok : s.split(","))
		{
			tok = tok.trim();
			if (tok.isEmpty()) continue;
			try { list.add(Integer.parseInt(tok)); } catch (NumberFormatException ignored) {}
		}
		return list;
	}

	private String joinItems(List<Integer> ids)
	{
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ids.size(); i++)
		{
			if (ids.get(i) == 0) continue;
			if (sb.length() > 0) sb.append(",");
			sb.append(ids.get(i));
		}
		return sb.toString();
	}

	private int safeInt(String s, int def)
	{
		if ((s == null) || s.isEmpty()) return def;
		try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
	}

	private Set<Integer> getWhitelist(String type)
	{
		switch (type)
		{
			case "hp": return AutoPotionsConfig.AUTO_HP_ITEM_IDS;
			case "mp": return AutoPotionsConfig.AUTO_MP_ITEM_IDS;
			default:   return AutoPotionsConfig.AUTO_CP_ITEM_IDS;
		}
	}

	private String itemsKey(String type)
	{
		switch (type)
		{
			case "hp": return PlayerVariables.AP_HP_ITEMS;
			case "mp": return PlayerVariables.AP_MP_ITEMS;
			default:   return PlayerVariables.AP_CP_ITEMS;
		}
	}

	private String enabledKey(String type)
	{
		switch (type)
		{
			case "hp": return PlayerVariables.AP_HP_ENABLED;
			case "mp": return PlayerVariables.AP_MP_ENABLED;
			default:   return PlayerVariables.AP_CP_ENABLED;
		}
	}

	private String percentKey(String type)
	{
		switch (type)
		{
			case "hp": return PlayerVariables.AP_HP_PERCENT;
			case "mp": return PlayerVariables.AP_MP_PERCENT;
			default:   return PlayerVariables.AP_CP_PERCENT;
		}
	}

	private String intervalKey(String type)
	{
		switch (type)
		{
			case "hp": return PlayerVariables.AP_HP_INTERVAL;
			case "mp": return PlayerVariables.AP_MP_INTERVAL;
			default:   return PlayerVariables.AP_CP_INTERVAL;
		}
	}

	private int defaultPercent(String type)
	{
		switch (type)
		{
			case "hp": return AutoPotionsConfig.AUTO_HP_PERCENTAGE;
			case "mp": return AutoPotionsConfig.AUTO_MP_PERCENTAGE;
			default:   return AutoPotionsConfig.AUTO_CP_PERCENTAGE;
		}
	}

	private String typeCN(String type)
	{
		switch (type)
		{
			case "hp": return "HP";
			case "mp": return "MP";
			default:   return "CP";
		}
	}

	private String gaugeColor(String type)
	{
		switch (type)
		{
			case "hp": return "AA3333";
			case "mp": return "3366AA";
			default:   return "AA7700";
		}
	}

	private double currentPct(Player player, String type)
	{
		switch (type)
		{
			case "hp": return (player.getStatus().getCurrentHp() / player.getMaxHp()) * 100.0;
			case "mp": return (player.getStatus().getCurrentMp() / player.getMaxMp()) * 100.0;
			default:   return (player.getStatus().getCurrentCp() / player.getMaxCp()) * 100.0;
		}
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
