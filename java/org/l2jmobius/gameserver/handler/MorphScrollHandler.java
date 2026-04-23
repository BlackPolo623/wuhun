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

import org.l2jmobius.gameserver.data.xml.MorphData;
import org.l2jmobius.gameserver.managers.MorphManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.morph.MorphEntry;
import org.l2jmobius.gameserver.model.morph.MorphGradeHolder;
import org.l2jmobius.gameserver.model.morph.MorphStatEntry;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 變身收藏系統 UI
 *
 * 頁面邏輯（使用卷軸時）：
 *   有激活變身 → 顯示「當前變身狀態」頁（含剩餘時間、屬性、自動續身開關、取消按鈕）
 *   無激活變身 → 顯示「變身收藏列表」頁（等級標籤 + 列表 + 應用按鈕）
 *
 * 卷軸消耗規則：
 *   - 使用卷軸打開 UI → 不消耗
 *   - 點擊「應用」變身 → 消耗 1 張卷軸；無卷軸時拒絕
 *   - 自動續變身到期 → 消耗 1 張卷軸；無卷軸時關閉自動續
 *
 * Bypass 命令：
 *   morph_show_list [grade]     — 顯示收藏列表（grade 默認 1）
 *   morph_apply {morphId} {grade} — 消耗卷軸並應用變身
 *   morph_cancel                — 取消當前變身，返回列表
 *   morph_cancel {grade}        — 取消當前變身，返回指定等級列表
 *   morph_toggle_autorenew      — 切換自動續變身開關
 *   morph_active_page           — 刷新當前變身狀態頁
 *   morph_view {morphId}        — 變身各階級概覽
 *   morph_grade {morphId} {lv}  — 指定階級屬性詳情
 *   morph_activate {itemId}     — 卡牌激活（收藏）
 *
 * @author Custom
 */
public class MorphScrollHandler implements IBypassHandler, IItemHandler
{
	/** 變身卷軸道具 ID（與 MorphManager.MORPH_SCROLL_ITEM_ID 保持一致） */
	private static final int MORPH_SCROLL_ITEM_ID = MorphManager.MORPH_SCROLL_ITEM_ID;

	/** 最大等級（與 XML grade 最大值對應） */
	private static final int MAX_GRADE = 6;

	/** 等級名稱（index 0 = grade 1） */
	private static final String[] GRADE_NAMES =
	{
		"一般",
		"高級",
		"稀有",
		"英雄",
		"傳說",
		"神話"
	};

	/** 等級顏色 */
	private static final String[] GRADE_COLORS =
	{
		"AAAAAA",
		"88CCFF",
		"AAFFAA",
		"FFAA44",
		"FF88FF",
		"FF6666"
	};

	private static final String[] COMMANDS =
	{
		"morph_show_list",
		"morph_view",
		"morph_grade",
		"morph_apply",
		"morph_cancel",
		"morph_activate",
		"morph_toggle_autorenew",
		"morph_active_page"
	};

	// ── IBypassHandler ────────────────────────────────────────────────────

	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if ((player == null) || !command.startsWith("morph_"))
		{
			return false;
		}

		try
		{
			// ── 收藏列表頁 ───────────────────────────────────────────────
			if (command.equals("morph_show_list"))
			{
				handleShowList(player, 1);
				return true;
			}
			else if (command.startsWith("morph_show_list "))
			{
				final int grade = Integer.parseInt(command.substring(16).trim());
				handleShowList(player, Math.max(1, Math.min(grade, MAX_GRADE)));
				return true;
			}
			// ── 刷新當前變身狀態頁 ────────────────────────────────────────
			else if (command.equals("morph_active_page"))
			{
				handleActiveMorphPage(player);
				return true;
			}
			// ── 切換自動續變身 ────────────────────────────────────────────
			else if (command.equals("morph_toggle_autorenew"))
			{
				MorphManager.getInstance().toggleAutoRenew(player);
				// 重新顯示當前變身頁，讓玩家看到最新狀態
				handleActiveMorphPage(player);
				return true;
			}
			// ── 應用變身（消耗卷軸）──────────────────────────────────────
			else if (command.startsWith("morph_apply "))
			{
				final String[] p = command.substring(12).trim().split(" ");
				final int morphId = Integer.parseInt(p[0]);
				final int grade = (p.length >= 2) ? Integer.parseInt(p[1]) : 1;

				// 消耗 1 張卷軸
				if (!player.destroyItemByItemId(null, MORPH_SCROLL_ITEM_ID, 1, player, true))
				{
					player.sendMessage("卷軸不足，無法應用變身，請先獲取變身卷軸。");
					return true;
				}

				MorphManager.getInstance().applyVisualMorph(player, morphId);
				// 應用后顯示當前變身狀態頁
				handleActiveMorphPage(player);
				return true;
			}
			// ── 取消變身 ─────────────────────────────────────────────────
			else if (command.equals("morph_cancel"))
			{
				MorphManager.getInstance().cancelMorphManually(player);
				handleShowList(player, 1);
				return true;
			}
			else if (command.startsWith("morph_cancel "))
			{
				final int grade = Integer.parseInt(command.substring(13).trim());
				MorphManager.getInstance().cancelMorphManually(player);
				handleShowList(player, Math.max(1, Math.min(grade, MAX_GRADE)));
				return true;
			}
			// ── 概覽頁 ────────────────────────────────────────────────────
			else if (command.startsWith("morph_view "))
			{
				handleViewMorph(player, Integer.parseInt(command.substring(11).trim()));
				return true;
			}
			// ── 階級詳細屬性頁 ────────────────────────────────────────────
			else if (command.startsWith("morph_grade "))
			{
				final String[] p = command.substring(12).trim().split(" ");
				if (p.length >= 2)
				{
					handleViewGrade(player, Integer.parseInt(p[0]), Integer.parseInt(p[1]));
					return true;
				}
			}
			// ── 卡牌激活 ──────────────────────────────────────────────────
			else if (command.startsWith("morph_activate "))
			{
				MorphManager.getInstance().activateByItem(player, Integer.parseInt(command.substring(15).trim()));
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

	// ── 當前變身狀態頁（有激活變身時顯示）────────────────────────────────

	private void handleActiveMorphPage(Player player)
	{
		final int morphId = MorphManager.getInstance().getActiveVisualMorphId(player);
		if (morphId < 0)
		{
			// 沒有激活變身，跳轉到收藏列表
			handleShowList(player, 1);
			return;
		}

		final int grade = MorphManager.getInstance().getActivatedGradeLevel(player, morphId);
		final MorphEntry entry = MorphData.getInstance().getEntry(morphId, grade);
		if (entry == null)
		{
			handleShowList(player, 1);
			return;
		}

		// 剩餘時間格式化
		final long remainMs = MorphManager.getInstance().getRemainingTimeMs(player);
		final long remainSec = remainMs / 1000L;
		final long mins = remainSec / 60L;
		final long secs = remainSec % 60L;
		final String timeStr = String.format("%02d:%02d", mins, secs);

		final String gradeName = GRADE_NAMES[Math.min(grade - 1, GRADE_NAMES.length - 1)];
		final String gradeColor = GRADE_COLORS[Math.min(grade - 1, GRADE_COLORS.length - 1)];
		final boolean autoRenew = MorphManager.getInstance().isAutoRenewEnabled(player);

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");

		// 標題
		sb.append("<table width=290><tr><td align=center>");
		sb.append("<font color=\"LEVEL\">★ 當前變身狀態 ★</font>");
		sb.append("</td></tr></table>");

		// 變身名稱 + 階級
		sb.append("<table width=290 bgcolor=003300><tr><td height=26 align=center>");
		sb.append("<font color=\"00FF00\">").append(entry.getName()).append("</font>");
		sb.append("&nbsp;<font color=\"").append(gradeColor).append("\">【").append(gradeName).append("】</font>");
		sb.append("</td></tr></table>");

		// 剩餘時間
		sb.append("<table width=290 bgcolor=1A1A2A><tr>");
		sb.append("<td width=145 align=center><font color=\"AAAAAA\">剩餘時間</font></td>");
		sb.append("<td width=145 align=center><font color=\"FFFF00\">").append(timeStr).append("</font></td>");
		sb.append("</tr></table>");

		// 自動續身開關
		sb.append("<table width=290 bgcolor=1A1A1A><tr>");
		sb.append("<td width=145 align=center><font color=\"AAAAAA\">自動續身</font></td>");
		sb.append("<td width=145 align=center>");
		if (autoRenew)
		{
			sb.append("<font color=\"00FF00\">已開啟</font>&nbsp;");
			sb.append("<button value=\"關閉\" action=\"bypass morph_toggle_autorenew\" width=50 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		else
		{
			sb.append("<font color=\"FF4444\">已關閉</font>&nbsp;");
			sb.append("<button value=\"開啟\" action=\"bypass morph_toggle_autorenew\" width=50 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		sb.append("</td>");
		sb.append("</tr></table>");

		// 分隔線
		sb.append("<table width=290 bgcolor=555555><tr><td height=1></td></tr></table>");

		// 視覺特效
		if (entry.hasAbnormalEffects())
		{
			sb.append("<table width=290 bgcolor=222233><tr><td height=18><font color=\"FFFF00\">視覺特效</font></td></tr></table>");
			sb.append("<table width=290 bgcolor=1A1A1A><tr><td align=center height=20>");
			boolean first = true;
			for (var ave : entry.getAbnormalEffects())
			{
				if (!first)
				{
					sb.append("&nbsp; ");
				}
				sb.append("<font color=\"88FFFF\">").append(ave.name()).append("</font>");
				first = false;
			}
			sb.append("</td></tr></table>");
		}

		// 屬性加成
		sb.append("<table width=290 bgcolor=222233><tr><td height=18><font color=\"FFFF00\">變身屬性加成</font></td></tr></table>");
		if (entry.hasStats())
		{
			sb.append("<table width=290 bgcolor=2A2A2A><tr>");
			sb.append("<td width=10></td>");
			sb.append("<td width=150><font color=\"AAAAAA\">屬性</font></td>");
			sb.append("<td width=120 align=right><font color=\"AAAAAA\">加成</font></td>");
			sb.append("<td width=10></td>");
			sb.append("</tr></table>");
			boolean odd = true;
			for (MorphStatEntry stat : entry.getStats())
			{
				final String bg = odd ? "1A1A1A" : "141414";
				odd = !odd;
				sb.append("<table width=290 bgcolor=").append(bg).append("><tr>");
				sb.append("<td width=10></td>");
				sb.append("<td width=150><font color=\"CCCCCC\">").append(stat.getDisplayName()).append("</font></td>");
				sb.append("<td width=120 align=right>");
				if (stat.isMultiply())
				{
					sb.append("<font color=\"AAFFAA\">+").append((int) stat.getValue()).append("%</font>");
				}
				else
				{
					final double v = stat.getValue();
					if ((v > 0) && (v < 1.0))
					{
						sb.append("<font color=\"AAFFAA\">+").append(String.format("%.2f", v)).append("</font>");
					}
					else
					{
						sb.append("<font color=\"AAFFAA\">+").append((long) v).append("</font>");
					}
				}
				sb.append("</td>");
				sb.append("<td width=10></td>");
				sb.append("</tr></table>");
			}
		}
		else
		{
			sb.append("<table width=290 bgcolor=1A1A1A><tr><td align=center height=20>");
			sb.append("<font color=\"555555\">（無屬性加成）</font>");
			sb.append("</td></tr></table>");
		}

		// 底部按鈕：取消變身 + 關閉
		sb.append("<table width=290><tr>");
		sb.append("<td width=145 align=center>");
		sb.append("<button value=\"取消變身\" action=\"bypass morph_cancel\" width=100 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</td>");
		sb.append("<td width=145 align=center>");
		sb.append("<button value=\"關 閉\" action=\"bypass -h close\" width=80 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</td>");
		sb.append("</tr></table>");

		sb.append("</body></html>");
		sendHtml(player, sb.toString());
	}

	// ── 主收藏列表頁（寵物收藏風格）──────────────────────────────────────

	private void handleShowList(Player player, int selectedGrade)
	{
		// 全域統計
		int totalMorphs = 0;
		int totalCollected = 0;
		for (int morphId : MorphData.getInstance().getAllMorphIds())
		{
			totalMorphs++;
			if (MorphManager.getInstance().getActivatedGradeLevel(player, morphId) > 0)
			{
				totalCollected++;
			}
		}

		final int activeId = MorphManager.getInstance().getActiveVisualMorphId(player);
		final StringBuilder sb = new StringBuilder();

		sb.append("<html><body>");

		// 已收藏數量
		sb.append("<table width=290 bgcolor=1A1A2A><tr>");
		sb.append("<td width=145 align=center><font color=\"AAAAAA\">已收藏數量</font></td>");
		sb.append("<td width=145 align=center><font color=\"00FF00\">").append(totalCollected).append(" / ").append(totalMorphs).append("</font></td>");
		sb.append("</tr></table>");

		// 當前激活變身橫幅
		if (activeId >= 0)
		{
			final int grade = MorphManager.getInstance().getActivatedGradeLevel(player, activeId);
			final MorphEntry cur = MorphData.getInstance().getEntry(activeId, grade);
			final String curName = (cur != null) ? cur.getName() : ("變身#" + activeId);
			sb.append("<table width=290 bgcolor=003300><tr><td height=22 align=center>");
			sb.append("<font color=\"00FF00\">★ 當前變身：").append(curName).append("&nbsp;&nbsp;</font>");
			sb.append("<button value=\"查看\" action=\"bypass morph_active_page\" width=50 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			sb.append("</td></tr></table>");
		}

		// 等級標籤欄
		sb.append("<table width=290><tr>");
		for (int g = 1; g <= MAX_GRADE; g++)
		{
			final String gName = GRADE_NAMES[g - 1];
			final String gColor = GRADE_COLORS[g - 1];
			final MorphGradeHolder holder = MorphData.getInstance().getGrade(g);
			if (g == selectedGrade)
			{
				sb.append("<td align=center><font color=\"LEVEL\"><b>").append(gName).append("</b></font></td>");
			}
			else if ((holder != null) && holder.hasEntries())
			{
				sb.append("<td align=center><a action=\"bypass morph_show_list ").append(g).append("\">");
				sb.append("<font color=\"").append(gColor).append("\">").append(gName).append("</font></a></td>");
			}
			else
			{
				sb.append("<td align=center><font color=\"444444\">").append(gName).append("</font></td>");
			}
		}
		sb.append("</tr></table>");
		sb.append("<table width=290 bgcolor=555555><tr><td height=1></td></tr></table>");

		// 表頭
		sb.append("<table width=290 bgcolor=2A2A2A><tr>");
		sb.append("<td width=40 align=center><font color=\"AAAAAA\">編號</font></td>");
		sb.append("<td width=120 align=center><font color=\"AAAAAA\">變身名稱</font></td>");
		sb.append("<td width=70 align=center><font color=\"AAAAAA\">狀態</font></td>");
		sb.append("<td width=60 align=center><font color=\"AAAAAA\">操作</font></td>");
		sb.append("</tr></table>");

		// 該等級的變身列表
		final MorphGradeHolder gradeHolder = MorphData.getInstance().getGrade(selectedGrade);
		if ((gradeHolder == null) || !gradeHolder.hasEntries())
		{
			sb.append("<table width=290><tr><td height=40 align=center>");
			sb.append("<font color=\"444444\">此等級尚無變身資料。</font>");
			sb.append("</td></tr></table>");
		}
		else
		{
			int index = 1;
			for (MorphEntry entry : gradeHolder.getEntries())
			{
				final int morphId = entry.getMorphId();
				final int collectedGrade = MorphManager.getInstance().getActivatedGradeLevel(player, morphId);
				final boolean isCollected = collectedGrade > 0;
				final boolean isActive = (activeId == morphId);
				final String bg = ((index % 2) == 0) ? "141414" : "1E1E1E";

				sb.append("<table width=290 bgcolor=").append(bg).append("><tr>");

				// 編號
				sb.append("<td width=40 align=center><font color=\"888888\">").append(index).append("</font></td>");

				// 變身名稱
				sb.append("<td width=120 align=center>");
				if (isActive)
				{
					sb.append("<font color=\"FFFF00\">").append(entry.getName()).append("</font>");
				}
				else if (isCollected)
				{
					sb.append("<font color=\"").append(GRADE_COLORS[selectedGrade - 1]).append("\">").append(entry.getName()).append("</font>");
				}
				else
				{
					sb.append("<font color=\"555555\">").append(entry.getName()).append("</font>");
				}
				sb.append("</td>");

				// 狀態
				sb.append("<td width=70 align=center>");
				if (isActive)
				{
					sb.append("<font color=\"00FFFF\">使用中</font>");
				}
				else if (isCollected)
				{
					sb.append("<font color=\"00FF00\">已收藏</font>");
				}
				else
				{
					sb.append("<font color=\"FF4444\">未收藏</font>");
				}
				sb.append("</td>");

				// 操作
				sb.append("<td width=60 align=center>");
				if (isActive)
				{
					sb.append("<button value=\"查看\" action=\"bypass morph_active_page\" width=50 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				}
				else if (isCollected)
				{
					sb.append("<button value=\"應用\" action=\"bypass morph_apply ").append(morphId).append(" ").append(selectedGrade).append("\" width=50 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				}
				else
				{
					sb.append("<font color=\"444444\">-</font>");
				}
				sb.append("</td>");

				sb.append("</tr></table>");
				index++;
			}
		}

		// 底部按鈕
		sb.append("<table width=290><tr><td align=center>");
		sb.append("<button value=\"關 閉\" action=\"bypass -h close\" width=80 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</td></tr></table>");

		sb.append("</body></html>");
		sendHtml(player, sb.toString());
	}

	// ── 變身各階級概覽頁 ──────────────────────────────────────────────────

	private void handleViewMorph(Player player, int morphId)
	{
		final java.util.List<MorphEntry> entries = MorphData.getInstance().getEntriesByMorphId(morphId);
		if (entries.isEmpty())
		{
			player.sendMessage("找不到變身 ID = " + morphId);
			return;
		}

		final String name = entries.get(0).getName();
		final int curLv = MorphManager.getInstance().getActivatedGradeLevel(player, morphId);
		final boolean isActive = MorphManager.getInstance().getActiveVisualMorphId(player) == morphId;

		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");

		sb.append("<table width=270><tr><td align=center>");
		sb.append("<font color=\"LEVEL\">【").append(name).append("】 階級一覽</font>");
		sb.append("</td></tr></table>");

		if (curLv > 0)
		{
			if (isActive)
			{
				sb.append("<table width=270 bgcolor=003300><tr><td height=22 align=center>");
				sb.append("<font color=\"00FF00\">★ 變身中（").append(MorphManager.getGradeName(curLv)).append("）&nbsp;</font>");
				sb.append("<button value=\"取消變身\" action=\"bypass morph_cancel\" width=70 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				sb.append("</td></tr></table>");
			}
			else
			{
				sb.append("<table width=270 bgcolor=002244><tr><td height=22 align=center>");
				sb.append("<font color=\"88AAFF\">已收藏【").append(MorphManager.getGradeName(curLv)).append("】&nbsp;&nbsp;</font>");
				sb.append("<button value=\"應用外觀\" action=\"bypass morph_apply ").append(morphId).append(" 1\" width=80 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				sb.append("</td></tr></table>");
			}
		}

		sb.append("<table width=270 bgcolor=333333><tr>");
		sb.append("<td width=60 align=center><font color=\"AAAAAA\">階級</font></td>");
		sb.append("<td width=80 align=center><font color=\"AAAAAA\">狀態</font></td>");
		sb.append("<td width=70 align=center><font color=\"AAAAAA\">激活道具</font></td>");
		sb.append("<td width=60 align=center><font color=\"AAAAAA\">詳情</font></td>");
		sb.append("</tr></table>");

		boolean odd = true;
		for (MorphEntry e : entries)
		{
			final int lv = MorphData.getInstance().getGradeList().stream().filter(g -> g.getEntry(morphId) == e).mapToInt(MorphGradeHolder::getLevel).findFirst().orElse(0);
			final String gName = MorphManager.getGradeName(lv);
			final String gColor = (lv >= 1 && lv <= GRADE_COLORS.length) ? GRADE_COLORS[lv - 1] : "AAAAAA";
			final String bg = odd ? "1A1A1A" : "141414";
			odd = !odd;

			sb.append("<table width=270 bgcolor=").append(bg).append("><tr>");
			sb.append("<td width=60 align=center><font color=\"").append(gColor).append("\">").append(gName).append("</font></td>");
			sb.append("<td width=80 align=center>");
			if (lv == curLv)
			{
				sb.append("<font color=\"00FF00\">★ 當前</font>");
			}
			else if (lv < curLv)
			{
				sb.append("<font color=\"666666\">已超越</font>");
			}
			else
			{
				sb.append("<font color=\"FF6600\">未激活</font>");
			}
			sb.append("</td>");
			sb.append("<td width=70 align=center><font color=\"AAAAAA\">").append(e.getItemId()).append("</font></td>");
			sb.append("<td width=60 align=center>");
			sb.append("<button value=\"屬性\" action=\"bypass morph_grade ").append(morphId).append(" ").append(lv);
			sb.append("\" width=50 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			sb.append("</td>");
			sb.append("</tr></table>");
		}

		sb.append("<table width=270><tr>");
		sb.append("<td width=135 align=center>");
		sb.append("<button value=\"← 返回列表\" action=\"bypass morph_show_list 1\" width=110 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</td><td width=135 align=center>");
		sb.append("<button value=\"關 閉\" action=\"bypass -h close\" width=80 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</td></tr></table>");
		sb.append("</body></html>");
		sendHtml(player, sb.toString());
	}

	// ── 階級詳細屬性頁 ────────────────────────────────────────────────────

	private void handleViewGrade(Player player, int morphId, int level)
	{
		final MorphEntry entry = MorphData.getInstance().getEntry(morphId, level);
		if (entry == null)
		{
			player.sendMessage("找不到變身 " + morphId + " 階級 " + level + " 的資料。");
			return;
		}

		final int curLv = MorphManager.getInstance().getActivatedGradeLevel(player, morphId);
		final String gradeName = MorphManager.getGradeName(level);
		final StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<table width=270><tr><td align=center>");
		sb.append("<font color=\"LEVEL\">").append(entry.getName()).append(" &nbsp;·&nbsp; 【").append(gradeName).append("】</font>");
		sb.append("</td></tr></table>");

		if (level == curLv)
		{
			sb.append("<table width=270 bgcolor=003300><tr><td height=20 align=center>");
			sb.append("<font color=\"00FF00\">★ 當前已收藏此階級 ★</font>");
			sb.append("</td></tr></table>");
		}
		else if (level < curLv)
		{
			sb.append("<table width=270 bgcolor=222200><tr><td height=20 align=center>");
			sb.append("<font color=\"888888\">此階級已被超越（當前【").append(MorphManager.getGradeName(curLv)).append("】）</font>");
			sb.append("</td></tr></table>");
		}
		else
		{
			sb.append("<table width=270 bgcolor=330000><tr><td height=20 align=center>");
			sb.append("<font color=\"FF6600\">激活需消耗道具 ID：<font color=\"FFFF00\">").append(entry.getItemId()).append("</font></font>");
			sb.append("</td></tr></table>");
		}
		sb.append("<br>");

		sb.append("<table width=270 bgcolor=222233><tr><td height=18><font color=\"FFFF00\">基本信息</font></td></tr></table>");
		sb.append("<table width=270><tr>");
		sb.append("<td width=10></td>");
		sb.append("<td width=120><font color=\"AAAAAA\">外觀 NPC ID</font></td>");
		sb.append("<td width=140><font color=\"FFFFFF\">").append(entry.getNpcId()).append("</font></td>");
		sb.append("</tr><tr>");
		sb.append("<td width=10></td>");
		sb.append("<td width=120><font color=\"AAAAAA\">碰撞半徑 / 身高</font></td>");
		sb.append("<td width=140><font color=\"FFFFFF\">").append(entry.getCollisionRadius()).append(" / ").append(entry.getCollisionHeight()).append("</font></td>");
		sb.append("</tr></table>");
		sb.append("<br>");

		if (entry.hasAbnormalEffects())
		{
			sb.append("<table width=270 bgcolor=222233><tr><td height=18><font color=\"FFFF00\">視覺特效</font></td></tr></table>");
			sb.append("<table width=270><tr><td width=10></td><td>");
			boolean first = true;
			for (var ave : entry.getAbnormalEffects())
			{
				if (!first)
				{
					sb.append(" &nbsp; ");
				}
				sb.append("<font color=\"88FFFF\">").append(ave.name()).append("</font>");
				first = false;
			}
			sb.append("</td></tr></table>");
			sb.append("<br>");
		}

		sb.append("<table width=270 bgcolor=222233><tr><td height=18><font color=\"FFFF00\">屬性加成</font></td></tr></table>");
		if (entry.hasStats())
		{
			sb.append("<table width=270 bgcolor=2A2A2A><tr>");
			sb.append("<td width=10></td>");
			sb.append("<td width=140><font color=\"AAAAAA\">屬性</font></td>");
			sb.append("<td width=110 align=right><font color=\"AAAAAA\">加成</font></td>");
			sb.append("<td width=10></td>");
			sb.append("</tr></table>");
			boolean odd = true;
			for (MorphStatEntry stat : entry.getStats())
			{
				final String bg = odd ? "1A1A1A" : "141414";
				odd = !odd;
				sb.append("<table width=270 bgcolor=").append(bg).append("><tr>");
				sb.append("<td width=10></td>");
				sb.append("<td width=140><font color=\"CCCCCC\">").append(stat.getDisplayName()).append("</font></td>");
				sb.append("<td width=110 align=right>");
				if (stat.isMultiply())
				{
					sb.append("<font color=\"AAFFAA\">+").append((int) stat.getValue()).append("%</font>");
				}
				else
				{
					final double v = stat.getValue();
					if ((v > 0) && (v < 1.0))
					{
						sb.append("<font color=\"AAFFAA\">+").append(String.format("%.2f", v)).append("</font>");
					}
					else
					{
						sb.append("<font color=\"AAFFAA\">+").append((long) v).append("</font>");
					}
				}
				sb.append("</td>");
				sb.append("<td width=10></td>");
				sb.append("</tr></table>");
			}
		}
		else
		{
			sb.append("<table width=270><tr><td align=center>");
			sb.append("<font color=\"555555\">（此階級無屬性加成）</font>");
			sb.append("</td></tr></table>");
		}

		sb.append("<table width=270><tr>");
		sb.append("<td width=135 align=center>");
		sb.append("<button value=\"← 返回\" action=\"bypass morph_view ").append(morphId);
		sb.append("\" width=100 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</td><td width=135 align=center>");
		sb.append("<button value=\"關 閉\" action=\"bypass -h close\" width=80 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</td></tr></table>");
		sb.append("</body></html>");
		sendHtml(player, sb.toString());
	}

	// ── IItemHandler ──────────────────────────────────────────────────────

	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			return false;
		}
		final Player player = playable.asPlayer();

		if (item.getId() == MORPH_SCROLL_ITEM_ID)
		{
			// 使用卷軸不消耗——根據是否有激活變身決定顯示哪個頁面
			if (MorphManager.getInstance().hasActiveVisualMorph(player))
			{
				handleActiveMorphPage(player);
			}
			else
			{
				handleShowList(player, 1);
			}
			return true;
		}

		// 其他道具視為變身卡牌
		return MorphManager.getInstance().activateByItem(player, item.getId());
	}

	// ── 工具 ─────────────────────────────────────────────────────────────

	private static void sendHtml(Player player, String html)
	{
		final NpcHtmlMessage msg = new NpcHtmlMessage();
		msg.setHtml(html);
		player.sendPacket(msg);
	}

	// ── Singleton ─────────────────────────────────────────────────────────

	private MorphScrollHandler()
	{
		BypassHandler.getInstance().registerHandler(this);
		ItemHandler.getInstance().registerHandler(this);
	}

	public static MorphScrollHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final MorphScrollHandler INSTANCE = new MorphScrollHandler();
	}
}
