/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.handler;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.SacrificeData;
import org.l2jmobius.gameserver.managers.SacrificeManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.morph.MorphStatEntry;
import org.l2jmobius.gameserver.model.sacrifice.SacrificeAltarEntry;
import org.l2jmobius.gameserver.model.sacrifice.SacrificeMaterialEntry;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 祭祀系統 UI Bypass 處理器。
 *
 * 頁面結構：
 *   祭壇列表頁  — data/custom/sacrifice/sacrifice_list.htm
 *   祭壇詳情頁  — data/custom/sacrifice/sacrifice_view.htm
 *
 * Bypass 命令：
 *   sacrifice_list               — 顯示所有祭壇概覽
 *   sacrifice_view {altarId}     — 顯示指定祭壇詳情
 *   sacrifice_perform {altarId}  — 執行一次祭祀
 *
 * @author Custom
 */
public class SacrificeHandler implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"sacrifice_list",
		"sacrifice_view",
		"sacrifice_perform"
	};

	private static final String PATH_LIST = "data/custom/sacrifice/sacrifice_list.htm";
	private static final String PATH_VIEW = "data/custom/sacrifice/sacrifice_view.htm";

	// ── IBypassHandler ────────────────────────────────────────────────────

	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if ((player == null) || !command.startsWith("sacrifice_"))
		{
			return false;
		}

		try
		{
			if (command.equals("sacrifice_list"))
			{
				handleList(player);
				return true;
			}
			else if (command.startsWith("sacrifice_view "))
			{
				final int altarId = Integer.parseInt(command.substring(15).trim());
				handleView(player, altarId);
				return true;
			}
			else if (command.startsWith("sacrifice_perform "))
			{
				final int altarId = Integer.parseInt(command.substring(18).trim());
				SacrificeManager.getInstance().performSacrifice(player, altarId);
				handleView(player, altarId); // 祭祀後刷新詳情頁
				return true;
			}
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("命令參數格式錯誤。");
		}

		return false;
	}

	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}

	// ── 祭壇概覽列表頁 ────────────────────────────────────────────────────

	private void handleList(Player player)
	{
		final java.util.List<SacrificeAltarEntry> altars = SacrificeData.getInstance().getAltarList();

		final StringBuilder rows = new StringBuilder();

		if (altars.isEmpty())
		{
			rows.append("<table width=290><tr><td height=40 align=center>");
			rows.append("<font color=\"444444\">尚無可用祭壇。</font>");
			rows.append("</td></tr></table>");
		}
		else
		{
			boolean odd = true;
			for (SacrificeAltarEntry altar : altars)
			{
				final int level = SacrificeManager.getInstance().getPlayerLevel(player, altar.getId());
				final boolean maxed = level >= altar.getMaxLevel();
				final String bg = odd ? "1E1E1E" : "141414";
				odd = !odd;

				rows.append("<table width=290 bgcolor=").append(bg).append("><tr>");

				// 名稱
				rows.append("<td width=120 align=center>");
				if (maxed)
				{
					rows.append("<font color=\"FFAA00\">★ ").append(altar.getName()).append("</font>");
				}
				else if (level > 0)
				{
					rows.append("<font color=\"00FF88\">").append(altar.getName()).append("</font>");
				}
				else
				{
					rows.append("<font color=\"AAAAAA\">").append(altar.getName()).append("</font>");
				}
				rows.append("</td>");

				// 等級
				rows.append("<td width=80 align=center>");
				if (maxed)
				{
					rows.append("<font color=\"FFAA00\">Lv.").append(level).append(" MAX</font>");
				}
				else
				{
					rows.append("<font color=\"FFFFFF\">Lv.").append(level).append(" / ").append(altar.getMaxLevel()).append("</font>");
				}
				rows.append("</td>");

				// 操作
				rows.append("<td width=90 align=center>");
				rows.append("<button value=\"詳情\" action=\"bypass sacrifice_view ").append(altar.getId());
				rows.append("\" width=70 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				rows.append("</td>");

				rows.append("</tr></table>");
			}
		}

		final NpcHtmlMessage msg = new NpcHtmlMessage();
		msg.setFile(player, PATH_LIST);
		msg.replace("%altarRows%", rows.toString());
		player.sendPacket(msg);
	}

	// ── 祭壇詳情頁 ────────────────────────────────────────────────────────

	private void handleView(Player player, int altarId)
	{
		final SacrificeAltarEntry altar = SacrificeData.getInstance().getAltar(altarId);
		if (altar == null)
		{
			player.sendMessage("找不到祭壇（id=" + altarId + "）。");
			return;
		}

		final int currentLevel = SacrificeManager.getInstance().getPlayerLevel(player, altarId);
		final int nextLevel = currentLevel + 1;
		final boolean maxed = currentLevel >= altar.getMaxLevel();

		// ── %levelBanner% ──────────────────────────────────────────────────
		final StringBuilder levelBanner = new StringBuilder();
		if (maxed)
		{
			levelBanner.append("<table width=290 bgcolor=332200><tr><td height=24 align=center>");
			levelBanner.append("<font color=\"FFAA00\">★ 已達最高等級 Lv.").append(currentLevel).append(" MAX ★</font>");
			levelBanner.append("</td></tr></table>");
		}
		else
		{
			levelBanner.append("<table width=290 bgcolor=002233><tr>");
			levelBanner.append("<td width=145 align=center><font color=\"AAAAAA\">當前等級</font></td>");
			levelBanner.append("<td width=145 align=center><font color=\"00FFFF\">Lv.").append(currentLevel);
			levelBanner.append(" &nbsp;→&nbsp; <font color=\"FFFF00\">Lv.").append(nextLevel).append("</font></font></td>");
			levelBanner.append("</tr></table>");

			levelBanner.append("<table width=290 bgcolor=1A1A1A><tr>");
			levelBanner.append("<td width=145 align=center><font color=\"AAAAAA\">本次成功率</font></td>");
			levelBanner.append("<td width=145 align=center><font color=\"FF8844\">").append(altar.getChancePercent()).append("%</font></td>");
			levelBanner.append("</tr></table>");

			levelBanner.append("<table width=290 bgcolor=141414><tr>");
			levelBanner.append("<td width=145 align=center><font color=\"AAAAAA\">每級屬性增幅</font></td>");
			levelBanner.append("<td width=145 align=center><font color=\"88FF88\">+").append((int) (altar.getUpgradeRate() * 100)).append("% / 級</font></td>");
			levelBanner.append("</tr></table>");
		}

		// ── %materialRows% ─────────────────────────────────────────────────
		final StringBuilder matRows = new StringBuilder();
		if (!altar.hasMaterials())
		{
			matRows.append("<table width=290 bgcolor=1A1A1A><tr><td align=center height=20>");
			matRows.append("<font color=\"555555\">（無需材料）</font>");
			matRows.append("</td></tr></table>");
		}
		else
		{
			boolean matOdd = true;
			for (SacrificeMaterialEntry mat : altar.getMaterials())
			{
				final long owned = player.getInventory().getInventoryItemCount(mat.getItemId(), -1);
				final boolean enough = owned >= mat.getCount();
				final String bg = matOdd ? "1A1A1A" : "141414";
				matOdd = !matOdd;

				matRows.append("<table width=290 bgcolor=").append(bg).append("><tr>");
				matRows.append("<td width=10></td>");
				final ItemTemplate itemTpl = ItemData.getInstance().getTemplate(mat.getItemId());
				final String itemName = (itemTpl != null) ? itemTpl.getName() : "道具 " + mat.getItemId();
				matRows.append("<td width=100><font color=\"CCCCCC\">").append(itemName).append("</font></td>");
				matRows.append("<td width=100 align=right>");
				if (enough)
				{
					matRows.append("<font color=\"00FF00\">").append(formatCount(mat.getCount())).append("</font>");
				}
				else
				{
					matRows.append("<font color=\"FF4444\">").append(formatCount(mat.getCount())).append("</font>");
				}
				matRows.append("</td>");
				matRows.append("<td width=70 align=right>");
				matRows.append("<font color=\"888888\">持有：").append(formatCount(owned)).append("</font>");
				matRows.append("</td>");
				matRows.append("<td width=10></td>");
				matRows.append("</tr></table>");
			}
		}

		// ── %statSectionTitle% ─────────────────────────────────────────────
		final String statSectionTitle;
		if (maxed)
		{
			statSectionTitle = "當前屬性加成（Lv." + currentLevel + "）";
		}
		else
		{
			statSectionTitle = "屬性加成預覽";
		}

		// ── %statHeader% ───────────────────────────────────────────────────
		final StringBuilder statHeader = new StringBuilder();
		if (altar.hasStats())
		{
			statHeader.append("<table width=290 bgcolor=2A2A2A><tr>");
			statHeader.append("<td width=10></td>");
			statHeader.append("<td width=120><font color=\"AAAAAA\">屬性</font></td>");
			if (!maxed && (currentLevel > 0))
			{
				statHeader.append("<td width=70 align=center><font color=\"AAAAAA\">當前</font></td>");
				statHeader.append("<td width=80 align=center><font color=\"FFFF00\">升級後</font></td>");
			}
			else if (maxed)
			{
				statHeader.append("<td width=150 align=center><font color=\"FFAA00\">最終值</font></td>");
			}
			else
			{
				statHeader.append("<td width=150 align=center><font color=\"AAAAAA\">Lv.1 值</font></td>");
			}
			statHeader.append("<td width=10></td>");
			statHeader.append("</tr></table>");
		}

		// ── %statRows% ─────────────────────────────────────────────────────
		final StringBuilder statRows = new StringBuilder();
		if (!altar.hasStats())
		{
			statRows.append("<table width=290 bgcolor=1A1A1A><tr><td align=center height=20>");
			statRows.append("<font color=\"555555\">（無屬性加成）</font>");
			statRows.append("</td></tr></table>");
		}
		else
		{
			boolean odd = true;
			for (MorphStatEntry stat : altar.getStats())
			{
				final String bg = odd ? "1A1A1A" : "141414";
				odd = !odd;

				statRows.append("<table width=290 bgcolor=").append(bg).append("><tr>");
				statRows.append("<td width=10></td>");
				statRows.append("<td width=120><font color=\"CCCCCC\">").append(stat.getDisplayName()).append("</font></td>");

				if (!maxed && (currentLevel > 0))
				{
					final double curVal = altar.getScaledValue(stat.getValue(), currentLevel);
					statRows.append("<td width=70 align=center>");
					appendStatValue(statRows, stat, curVal, "AAFFAA");
					statRows.append("</td>");

					final double nextVal = altar.getScaledValue(stat.getValue(), nextLevel);
					statRows.append("<td width=80 align=center>");
					appendStatValue(statRows, stat, nextVal, "FFFF44");
					statRows.append("</td>");
				}
				else
				{
					final double val = altar.getScaledValue(stat.getValue(), Math.max(1, currentLevel));
					final String valColor = maxed ? "FFAA00" : "AAFFAA";
					statRows.append("<td width=150 align=center>");
					appendStatValue(statRows, stat, val, valColor);
					statRows.append("</td>");
				}

				statRows.append("<td width=10></td>");
				statRows.append("</tr></table>");
			}
		}

		// ── %actionButtons% ────────────────────────────────────────────────
		final StringBuilder buttons = new StringBuilder();
		buttons.append("<table width=290><tr>");
		buttons.append("<td width=100 align=center>");
		buttons.append("<button value=\"← 返回\" action=\"bypass sacrifice_list\" width=80 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		buttons.append("</td>");

		if (!maxed)
		{
			buttons.append("<td width=90 align=center>");
			buttons.append("<button value=\"⚡ 祭 祀\" action=\"bypass sacrifice_perform ").append(altarId);
			buttons.append("\" width=80 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			buttons.append("</td>");
		}

		buttons.append("<td width=100 align=center>");
		buttons.append("<button value=\"關 閉\" action=\"bypass -h close\" width=80 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		buttons.append("</td>");
		buttons.append("</tr></table>");

		// ── 組裝並發送 ─────────────────────────────────────────────────────
		final NpcHtmlMessage msg = new NpcHtmlMessage();
		msg.setFile(player, PATH_VIEW);
		msg.replace("%altarName%", altar.getName());
		msg.replace("%levelBanner%", levelBanner.toString());
		msg.replace("%materialRows%", matRows.toString());
		msg.replace("%statSectionTitle%", statSectionTitle);
		msg.replace("%statHeader%", statHeader.toString());
		msg.replace("%statRows%", statRows.toString());
		msg.replace("%actionButtons%", buttons.toString());
		player.sendPacket(msg);
	}

	// ── 工具 ─────────────────────────────────────────────────────────────

	/** 在 StringBuilder 中追加格式化屬性值（add 或 mul%） */
	private static void appendStatValue(StringBuilder sb, MorphStatEntry stat, double value, String color)
	{
		sb.append("<font color=\"").append(color).append("\">");
		if (stat.isMultiply())
		{
			sb.append("+").append(String.format("%.1f", value)).append("%");
		}
		else
		{
			sb.append("+").append((long) value);
		}
		sb.append("</font>");
	}

	/** 格式化大數字，超過萬時加 W 後綴 */
	private static String formatCount(long count)
	{
		if (count >= 10_000_000)
		{
			return String.format("%.0fW", count / 10000.0);
		}
		else if (count >= 10_000)
		{
			return String.format("%.1fW", count / 10000.0);
		}
		return String.valueOf(count);
	}

	// ── Singleton ─────────────────────────────────────────────────────────

	private SacrificeHandler()
	{
		BypassHandler.getInstance().registerHandler(this);
	}

	public static SacrificeHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final SacrificeHandler INSTANCE = new SacrificeHandler();
	}
}
