package custom.chenghao;

import java.util.List;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SkillList;
import org.l2jmobius.gameserver.util.Broadcast;

import custom.chenghao.TitleSystem.SmallTitle;
import custom.chenghao.TitleSystem.TitleSeries;

/**
 * 稱號系統 - 主腳本
 * 重構版：支持多系列、只有最終稱號可配戴、系列全解永久啟用
 */
public class chenghao extends Script
{
	private static final String HTML_PATH = "data/scripts/custom/chenghao/";

	public chenghao()
	{
		addStartNpc(TitleSystem.NPC_ID);
		addFirstTalkId(TitleSystem.NPC_ID);
		addTalkId(TitleSystem.NPC_ID);

		// 註冊所有系列的BOSS擊殺事件
		for (TitleSeries series : TitleSystem.getAllSeries())
		{
			for (SmallTitle title : series.getSmallTitles())
			{
				addKillId(title.getBossNpcId());
			}
		}
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		checkAndReapplySkills(player);
		showAllSeriesPage(player, 0);
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event == null || player == null)
		{
			return null;
		}

		switch (event)
		{
			case "main":
				showAllSeriesPage(player, 0);
				break;
			case "showAllSeries":
				showAllSeriesPage(player, 0);
				break;
			default:
				if (event.startsWith("showAllSeries_"))
				{
					try
					{
						int page = Integer.parseInt(event.substring(14));
						showAllSeriesPage(player, page);
					}
					catch (NumberFormatException e)
					{
						showAllSeriesPage(player, 0);
					}
				}
				else if (event.startsWith("showSeries_"))
				{
					String seriesId = event.substring(11);
					showSeriesDetailPage(player, seriesId);
				}
				else if (event.startsWith("fuseSeries_"))
				{
					String seriesId = event.substring(11);
					processFusion(player, seriesId);
				}
				else if (event.startsWith("showEquipSelect_"))
				{
					String seriesId = event.substring(16);
					showEquipSelectPage(player, seriesId);
				}
				else if (event.startsWith("equipSeriesLevel_"))
				{
					// 格式: equipSeriesLevel_seriesId_level
					String remainder = event.substring(17);
					int lastUnderscore = remainder.lastIndexOf('_');
					if (lastUnderscore > 0)
					{
						String sId = remainder.substring(0, lastUnderscore);
						try
						{
							int level = Integer.parseInt(remainder.substring(lastUnderscore + 1));
							equipFinalTitleAtLevel(player, sId, level);
						}
						catch (NumberFormatException e)
						{
							showSeriesDetailPage(player, sId);
						}
					}
				}
				else if (event.startsWith("equipSeries_"))
				{
					String seriesId = event.substring(12);
					equipFinalTitle(player, seriesId);
				}
				else if (event.startsWith("unequipSeries_"))
				{
					String seriesId = event.substring(14);
					unequipFinalTitle(player, seriesId);
				}
				break;
		}

		return null;
	}

	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (killer == null)
		{
			return;
		}

		for (TitleSeries series : TitleSystem.getAllSeries())
		{
			for (SmallTitle title : series.getSmallTitles())
			{
				if (npc.getId() == title.getBossNpcId())
				{
					String varKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_" + title.getTitleName();
					if (!killer.getVariables().getBoolean(varKey, false))
					{
						killer.getVariables().set(varKey, true);
						killer.sendMessage("========================================");
						killer.sendMessage("恭喜！你已解鎖【" + series.getSeriesName() + "】");
						killer.sendMessage("稱號：" + title.getTitleName());
						killer.sendMessage("前往稱號NPC查看進度！");
						killer.sendMessage("========================================");
					}
					return;
				}
			}
		}
	}

	@Override
	public void onEnterWorld(Player player)
	{
		String equippedSeriesId = player.getVariables().getString(TitleSystem.VAR_PREFIX + "equipped", null);
		if (equippedSeriesId != null)
		{
			TitleSeries series = TitleSystem.getSeriesById(equippedSeriesId);
			if (series != null && isSeriesFused(player, series))
			{
				String levelKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_level";
				int fusionLevel = player.getVariables().getInt(levelKey, 1);
				// 使用玩家選擇的配戴等級，若無記錄則用融合等級
				int equippedLevel = player.getVariables().getInt(TitleSystem.VAR_PREFIX + "equipped_level", fusionLevel);

				player.setTitle(series.getFinalTitleName());
				Skill skill = SkillData.getInstance().getSkill(series.getFinalSkillId(), equippedLevel);
				if (skill != null)
				{
					player.addSkill(skill, true);
					player.sendPacket(new SkillList());
				}
			}
		}

		activatePermanentSkills(player);
	}

	private void showMainPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, HTML_PATH + "main.htm");

		String equippedSeriesId = player.getVariables().getString(TitleSystem.VAR_PREFIX + "equipped", null);
		if (equippedSeriesId != null)
		{
			TitleSeries series = TitleSystem.getSeriesById(equippedSeriesId);
			if (series != null)
			{
				// 獲取當前等級
				String levelKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_level";
				int currentLevel = player.getVariables().getInt(levelKey, 1);
				html.replace("%current_title%", "<font color=\"FFFF00\">" + series.getFinalTitleName() + " Lv." + currentLevel + "</font>");
			}
			else
			{
				html.replace("%current_title%", "<font color=\"808080\">無</font>");
			}
		}
		else
		{
			html.replace("%current_title%", "<font color=\"808080\">無</font>");
		}

		int totalSeries = TitleSystem.getAllSeries().size();
		int completedSeries = 0;
		for (TitleSeries series : TitleSystem.getAllSeries())
		{
			if (isSeriesCompleted(player, series))
			{
				completedSeries++;
			}
		}

		html.replace("%completed_series%", String.valueOf(completedSeries));
		html.replace("%total_series%", String.valueOf(totalSeries));

		player.sendPacket(html);
	}

	private void showAllSeriesPage(Player player, int page)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, HTML_PATH + "series_list.htm");

		List<TitleSeries> allSeries = TitleSystem.getAllSeries();
		int totalSeries = allSeries.size();
		int totalPages = Math.max(1, (totalSeries + 4) / 5);
		if (page < 0) page = 0;
		if (page >= totalPages) page = totalPages - 1;

		int start = page * 5;
		int end = Math.min(start + 5, totalSeries);

		StringBuilder seriesList = new StringBuilder();
		for (int i = start; i < end; i++)
		{
			TitleSeries series = allSeries.get(i);
			int unlockedCount = getUnlockedCount(player, series);
			int totalCount = series.getTotalCount();
			boolean completed = isSeriesCompleted(player, series);
			boolean fused = isSeriesFused(player, series);

			seriesList.append("<tr bgcolor=\"222222\" height=\"40\">");
			seriesList.append("<td width=\"120\" align=\"center\">");

			if (completed)
			{
				seriesList.append("<font color=\"00FF66\">").append(series.getSeriesName()).append("</font>");
			}
			else
			{
				seriesList.append("<font color=\"FFAA00\">").append(series.getSeriesName()).append("</font>");
			}

			seriesList.append("</td>");
			seriesList.append("<td width=\"50\" align=\"center\">");
			seriesList.append(unlockedCount).append(" / ").append(totalCount);
			seriesList.append("</td>");
			seriesList.append("<td width=\"60\" align=\"center\">");

			if (fused)
			{
				String fusionLevelKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_level";
				int fusionLevel = player.getVariables().getInt(fusionLevelKey, 0);
				String lvColor = fusionLevel >= series.getMaxLevel() ? "00FF66" : "FFAA00";
				seriesList.append("<font color=\"" + lvColor + "\">Lv." + fusionLevel + "/" + series.getMaxLevel() + "</font>");
			}
			else if (completed)
			{
				seriesList.append("<font color=\"FFFF00\">可融合</font>");
			}
			else
			{
				seriesList.append("<font color=\"808080\">未完成</font>");
			}

			seriesList.append("</td>");
			seriesList.append("<td width=\"60\" align=\"center\">");
			seriesList.append("<button value=\"查看\" action=\"bypass -h Quest chenghao showSeries_").append(series.getSeriesId());
			seriesList.append("\" width=\"55\" height=\"22\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			seriesList.append("</td>");
			seriesList.append("</tr>");
		}

		html.replace("%series_list%", seriesList.toString());

		// 分頁導航
		StringBuilder pageNav = new StringBuilder();
		pageNav.append("<table width=\"290\"><tr>");
		pageNav.append("<td width=\"90\" align=\"center\">");
		if (page > 0)
		{
			pageNav.append("<button value=\"上一頁\" action=\"bypass -h Quest chenghao showAllSeries_").append(page - 1);
			pageNav.append("\" width=\"80\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		}
		pageNav.append("</td>");
		pageNav.append("<td width=\"110\" align=\"center\">");
		pageNav.append("<font color=\"AAAAAA\">").append(page + 1).append(" / ").append(totalPages).append("</font>");
		pageNav.append("</td>");
		pageNav.append("<td width=\"90\" align=\"center\">");
		if (page < totalPages - 1)
		{
			pageNav.append("<button value=\"下一頁\" action=\"bypass -h Quest chenghao showAllSeries_").append(page + 1);
			pageNav.append("\" width=\"80\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		}
		pageNav.append("</td>");
		pageNav.append("</tr></table>");

		html.replace("%page_nav%", pageNav.toString());
		player.sendPacket(html);
	}

	private void showSeriesDetailPage(Player player, String seriesId)
	{
		TitleSeries series = TitleSystem.getSeriesById(seriesId);
		if (series == null)
		{
			player.sendMessage("無效的系列！");
			return;
		}

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, HTML_PATH + "series_detail.htm");

		html.replace("%series_name%", series.getSeriesName());
		html.replace("%final_title%", series.getFinalTitleName());

		int unlockedCount = getUnlockedCount(player, series);
		int totalCount = series.getTotalCount();
		boolean completed = isSeriesCompleted(player, series);
		boolean fused = isSeriesFused(player, series);
		boolean equipped = isSeriesEquipped(player, series);

		// 獲取當前等級
		String levelKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_level";
		int currentLevel = player.getVariables().getInt(levelKey, 0);

		html.replace("%unlocked_count%", String.valueOf(unlockedCount));
		html.replace("%total_count%", String.valueOf(totalCount));

		int progress = (int) ((double) unlockedCount / totalCount * 100);
		html.replace("%progress%", String.valueOf(progress));

		StringBuilder titleList = new StringBuilder();
		for (SmallTitle title : series.getSmallTitles())
		{
			String varKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_" + title.getTitleName();
			boolean unlocked = player.getVariables().getBoolean(varKey, false);

			titleList.append("<tr bgcolor=\"222222\" height=\"25\">");
			titleList.append("<td width=\"150\" align=\"left\">");

			if (unlocked)
			{
				titleList.append("<font color=\"00FF66\">已完成 -- ").append(title.getTitleName()).append("</font>");
			}
			else
			{
				titleList.append("<font color=\"808080\">未完成 -- ").append(title.getTitleName()).append("</font>");
			}

			titleList.append("</td>");
			titleList.append("<td width=\"100\" align=\"center\">");

			if (!unlocked)
			{
				NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(title.getBossNpcId());
				String bossName = npcTemplate != null ? npcTemplate.getName() : "未知";
				titleList.append("<font color=\"666666\" size=\"1\">").append(bossName).append("</font>");
			}

			titleList.append("</td>");
			titleList.append("</tr>");
		}

		html.replace("%title_list%", titleList.toString());

		StringBuilder fusionButton = new StringBuilder();
		if (currentLevel >= series.getMaxLevel())
		{
			fusionButton.append("<table width=\"280\"><tr><td align=\"center\"><font color=\"00FF66\" size=\"3\">✓ 已達最高等級 Lv.").append(currentLevel).append("</font></td></tr></table>");
		}
		else if (completed)
		{
			long itemCount = player.getInventory().getInventoryItemCount(TitleSystem.FUSION_ITEM_ID, 0);
			if (itemCount >= TitleSystem.FUSION_ITEM_COUNT)
			{
				String buttonText = fused ? "升級稱號 (Lv." + currentLevel + " → Lv." + (currentLevel + 1) + ")" : "融合稱號";
				fusionButton.append("<button value=\"").append(buttonText).append("\" action=\"bypass -h Quest chenghao fuseSeries_").append(seriesId);
				fusionButton.append("\" width=\"200\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");

				// 顯示成功率
				int targetLevel = currentLevel + 1;
				Double successRate = series.getFusionSuccessRate(targetLevel);
				if (successRate != null && successRate < 1.0)
				{
					int percentage = (int) (successRate * 100);
					String color = percentage >= 70 ? "00FF66" : (percentage >= 40 ? "FFAA00" : "FF3333");
					fusionButton.append("<table width=\"280\"><tr><td align=\"center\">");
					fusionButton.append("<font color=\"").append(color).append("\" size=\"1\">成功率: ").append(percentage).append("%</font>");
					fusionButton.append("</td></tr></table>");
				}
			}
			else
			{
				fusionButton.append("<table width=\"280\"><tr><td align=\"center\"><font color=\"FF3333\">材料不足</font></td></tr>");
				fusionButton.append("<tr><td align=\"center\"><font color=\"808080\" size=\"1\">需要 ").append(TitleSystem.FUSION_ITEM_COUNT).append(" 金幣</font></td></tr></table>");
			}
		}
		else
		{
			fusionButton.append("<table width=\"280\"><tr><td align=\"center\"><font color=\"808080\">請先完成全部稱號解鎖</font></td></tr></table>");
		}

		html.replace("%fusion_button%", fusionButton.toString());

		// 合成進度
		if (fused)
		{
			String progressColor = currentLevel >= series.getMaxLevel() ? "00FF66" : "FFAA00";
			html.replace("%fusion_progress%", "<font color=\"" + progressColor + "\">" + currentLevel + " / " + series.getMaxLevel() + "</font>");
		}
		else
		{
			html.replace("%fusion_progress%", "<font color=\"808080\">尚未融合</font>");
		}

		// 配戴選擇按鈕（顯示在合成進度旁）
		if (!fused)
		{
			html.replace("%equip_select_button%", "");
		}
		else
		{
			int eLevel = equipped ? player.getVariables().getInt(TitleSystem.VAR_PREFIX + "equipped_level", currentLevel) : 0;
			String btnLabel = equipped ? "配戴中 Lv." + eLevel : "選擇配戴";
			String btnStyle = "L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF";
			html.replace("%equip_select_button%", "<button value=\"" + btnLabel + "\" action=\"bypass -h Quest chenghao showEquipSelect_" + seriesId + "\" width=\"130\" height=\"22\" back=\"" + btnStyle + "\">");
		}

		// 檢查是否達到最高等級
		if (currentLevel >= series.getMaxLevel())
		{
			html.replace("%permanent_status%", "<font color=\"00FF66\">✓ 已永久啟用技能 Lv." + currentLevel + "</font>");
		}
		else if (fused)
		{
			html.replace("%permanent_status%", "<font color=\"FFAA00\">升至最高等級後永久啟用</font>");
		}
		else
		{
			html.replace("%permanent_status%", "<font color=\"808080\">完成全部解鎖後永久啟用</font>");
		}

		int seriesPage = getSeriesPage(series.getSeriesId());
		String backButton = "<button value=\"返回列表\" action=\"bypass -h Quest chenghao showAllSeries_" + seriesPage + "\" width=\"100\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">";
		html.replace("%back_button%", backButton);

		player.sendPacket(html);
	}

	private void processFusion(Player player, String seriesId)
	{
		TitleSeries series = TitleSystem.getSeriesById(seriesId);
		if (series == null)
		{
			player.sendMessage("無效的系列！");
			return;
		}

		// 獲取當前等級
		String levelKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_level";
		int currentLevel = player.getVariables().getInt(levelKey, 0);

		// 檢查是否已達最高等級
		if (currentLevel >= series.getMaxLevel())
		{
			player.sendMessage("此系列已達到最高等級！");
			showSeriesDetailPage(player, seriesId);
			return;
		}

		// 檢查是否完成全部解鎖
		if (!isSeriesCompleted(player, series))
		{
			player.sendMessage("請先完成全部稱號解鎖！");
			showSeriesDetailPage(player, seriesId);
			return;
		}

		// 檢查材料
		long itemCount = player.getInventory().getInventoryItemCount(TitleSystem.FUSION_ITEM_ID, 0);
		if (itemCount < TitleSystem.FUSION_ITEM_COUNT)
		{
			player.sendMessage("材料不足！需要 " + TitleSystem.FUSION_ITEM_COUNT + " 金幣");
			showSeriesDetailPage(player, seriesId);
			return;
		}

		// 扣除材料
		player.destroyItemByItemId(null, TitleSystem.FUSION_ITEM_ID, TitleSystem.FUSION_ITEM_COUNT, null, true);

		// 計算目標等級
		int newLevel = currentLevel + 1;

		// 檢查成功率
		Double successRate = series.getFusionSuccessRate(newLevel);
		boolean success = true;

		if (successRate != null && successRate < 1.0)
		{
			// 有設定成功率，進行判定
			double random = Math.random();
			success = random < successRate;

			// 顯示成功率資訊
			int percentage = (int) (successRate * 100);
			player.sendMessage("合成成功率: " + percentage + "%");
		}

		// 合成失敗
		if (!success)
		{
			// 移除所有小稱號解鎖狀態（消耗掉）
			for (SmallTitle title : series.getSmallTitles())
			{
				String varKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_" + title.getTitleName();
				player.getVariables().remove(varKey);
			}

			// 失敗訊息
			player.sendMessage("========================================");
			player.sendMessage("很遺憾，合成失敗了...");
			player.sendMessage("材料和稱號已消耗，請重新收集");
			player.sendMessage("========================================");

			showSeriesDetailPage(player, seriesId);
			return;
		}

		// 合成成功 - 升級
		player.getVariables().set(levelKey, newLevel);

		// 標記為已融合（首次融合時）
		if (currentLevel == 0)
		{
			String fusedKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_fused";
			player.getVariables().set(fusedKey, true);
		}

		// 移除所有小稱號解鎖狀態（消耗掉）
		for (SmallTitle title : series.getSmallTitles())
		{
			String varKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_" + title.getTitleName();
			player.getVariables().remove(varKey);
		}

		// 更新技能等級
		String equippedSeriesId = player.getVariables().getString(TitleSystem.VAR_PREFIX + "equipped", null);
		boolean isEquipped = series.getSeriesId().equals(equippedSeriesId);

		// 如果配戴中,更新技能等級
		if (isEquipped)
		{
			Skill skill = SkillData.getInstance().getSkill(series.getFinalSkillId(), newLevel);
			if (skill != null)
			{
				player.addSkill(skill, true);
				player.sendPacket(new SkillList());
			}
		}
		// 如果達到最高等級,永久啟用技能(即使未配戴)
		else if (newLevel >= series.getMaxLevel())
		{
			Skill skill = SkillData.getInstance().getSkill(series.getFinalSkillId(), newLevel);
			if (skill != null)
			{
				player.addSkill(skill, true);
				player.sendPacket(new SkillList());
			}
		}

		// 訊息提示
		player.sendMessage("========================================");
		if (currentLevel == 0)
		{
			player.sendMessage("恭喜！成功融合【" + series.getSeriesName() + "】");
			player.sendMessage("獲得稱號：" + series.getFinalTitleName() + " Lv." + newLevel);
		}
		else
		{
			player.sendMessage("恭喜！稱號升級成功！");
			player.sendMessage(series.getFinalTitleName() + " Lv." + currentLevel + " → Lv." + newLevel);
		}
		player.sendMessage("========================================");

		// 全服公告
		if (currentLevel == 0 || newLevel >= 5)
		{
			Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, "系統公告",
				"恭喜玩家 " + player.getName() + " 將【" + series.getFinalTitleName() + "】升級至 Lv." + newLevel + "！"));
		}

		showSeriesDetailPage(player, seriesId);
	}

	private void equipFinalTitle(Player player, String seriesId)
	{
		TitleSeries series = TitleSystem.getSeriesById(seriesId);
		if (series == null)
		{
			player.sendMessage("無效的系列！");
			return;
		}

		if (!isSeriesFused(player, series))
		{
			player.sendMessage("你尚未融合此系列！");
			return;
		}

		String oldEquippedId = player.getVariables().getString(TitleSystem.VAR_PREFIX + "equipped", null);
		if (oldEquippedId != null)
		{
			TitleSeries oldSeries = TitleSystem.getSeriesById(oldEquippedId);
			if (oldSeries != null)
			{
				// 檢查舊系列是否達到最高等級
				String oldLevelKey = TitleSystem.VAR_PREFIX + oldSeries.getSeriesId() + "_level";
				int oldLevel = player.getVariables().getInt(oldLevelKey, 0);

				// 只有未達最高等級才移除技能
				if (oldLevel < oldSeries.getMaxLevel())
				{
					player.removeSkill(oldSeries.getFinalSkillId());
				}
			}
		}

		// 獲取當前等級
		String levelKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_level";
		int currentLevel = player.getVariables().getInt(levelKey, 1);

		player.setTitle(series.getFinalTitleName());
		player.broadcastTitleInfo();
		player.getVariables().set(TitleSystem.VAR_PREFIX + "equipped", seriesId);

		Skill skill = SkillData.getInstance().getSkill(series.getFinalSkillId(), currentLevel);
		if (skill != null)
		{
			player.addSkill(skill, true);
			player.sendPacket(new SkillList());
		}

		player.sendMessage("已配戴稱號：" + series.getFinalTitleName() + " Lv." + currentLevel);
		showSeriesDetailPage(player, seriesId);
	}

	private void unequipFinalTitle(Player player, String seriesId)
	{
		TitleSeries series = TitleSystem.getSeriesById(seriesId);
		if (series == null)
		{
			player.sendMessage("無效的系列！");
			return;
		}

		player.setTitle("");
		player.broadcastTitleInfo();
		player.getVariables().remove(TitleSystem.VAR_PREFIX + "equipped");
		player.getVariables().remove(TitleSystem.VAR_PREFIX + "equipped_level");

		// 檢查是否達到最高等級
		String levelKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_level";
		int currentLevel = player.getVariables().getInt(levelKey, 0);

		// 只有達到最高等級才是永久技能,否則卸下時移除
		if (currentLevel < series.getMaxLevel())
		{
			player.removeSkill(series.getFinalSkillId());
			player.sendPacket(new SkillList());
		}

		player.sendMessage("已卸下稱號");
		showSeriesDetailPage(player, seriesId);
	}

	private int getUnlockedCount(Player player, TitleSeries series)
	{
		int count = 0;
		for (SmallTitle title : series.getSmallTitles())
		{
			String varKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_" + title.getTitleName();
			if (player.getVariables().getBoolean(varKey, false))
			{
				count++;
			}
		}
		return count;
	}

	private boolean isSeriesCompleted(Player player, TitleSeries series)
	{
		return getUnlockedCount(player, series) == series.getTotalCount();
	}

	private boolean isSeriesFused(Player player, TitleSeries series)
	{
		String fusedKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_fused";
		return player.getVariables().getBoolean(fusedKey, false);
	}

	private boolean isSeriesEquipped(Player player, TitleSeries series)
	{
		String equippedId = player.getVariables().getString(TitleSystem.VAR_PREFIX + "equipped", null);
		return series.getSeriesId().equals(equippedId);
	}

	private void equipFinalTitleAtLevel(Player player, String seriesId, int level)
	{
		TitleSeries series = TitleSystem.getSeriesById(seriesId);
		if (series == null)
		{
			player.sendMessage("無效的系列！");
			return;
		}

		if (!isSeriesFused(player, series))
		{
			player.sendMessage("你尚未融合此系列！");
			return;
		}

		String levelKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_level";
		int currentLevel = player.getVariables().getInt(levelKey, 0);

		if (level < 1 || level > currentLevel)
		{
			player.sendMessage("無效的等級！");
			return;
		}

		// 移除其他尚未升滿（非永久）系列的技能
		for (TitleSeries s : TitleSystem.getAllSeries())
		{
			if (s.getSeriesId().equals(seriesId)) continue;
			String sLevelKey = TitleSystem.VAR_PREFIX + s.getSeriesId() + "_level";
			int sLevel = player.getVariables().getInt(sLevelKey, 0);
			if (sLevel < s.getMaxLevel()) // 未升滿，非永久技能
			{
				player.removeSkill(s.getFinalSkillId());
			}
		}

		player.setTitle(series.getFinalTitleName());
		player.broadcastTitleInfo();
		player.getVariables().set(TitleSystem.VAR_PREFIX + "equipped", seriesId);
		player.getVariables().set(TitleSystem.VAR_PREFIX + "equipped_level", level);

		Skill skill = SkillData.getInstance().getSkill(series.getFinalSkillId(), level);
		if (skill != null)
		{
			player.addSkill(skill, true);
			player.sendPacket(new SkillList());
		}

		player.sendMessage("已配戴稱號：" + series.getFinalTitleName() + " Lv." + level);
		showSeriesDetailPage(player, seriesId);
	}

	private void showEquipSelectPage(Player player, String seriesId)
	{
		TitleSeries series = TitleSystem.getSeriesById(seriesId);
		if (series == null) return;

		if (!isSeriesFused(player, series))
		{
			player.sendMessage("你尚未融合此系列！");
			return;
		}

		String levelKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_level";
		int currentLevel = player.getVariables().getInt(levelKey, 0);
		boolean equipped = isSeriesEquipped(player, series);
		int equippedLevel = equipped ? player.getVariables().getInt(TitleSystem.VAR_PREFIX + "equipped_level", currentLevel) : 0;

		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><title>選擇配戴稱號</title><center>");
		sb.append("<br><table width=\"290\" bgcolor=\"000000\">");
		sb.append("<tr><td align=\"center\"><font color=\"FFAA00\" size=\"3\">").append(series.getSeriesName()).append("</font></td></tr>");
		sb.append("</table><br>");
		sb.append("<table width=\"290\" bgcolor=\"222222\">");
		sb.append("<tr><td height=\"5\"></td></tr>");
		sb.append("<tr><td align=\"center\"><font color=\"AAAAAA\" size=\"1\">選擇要配戴的稱號等級</font></td></tr>");
		if (equipped)
		{
			sb.append("<tr><td align=\"center\"><font color=\"00FF66\" size=\"1\">目前配戴: Lv.").append(equippedLevel).append("</font></td></tr>");
		}
		sb.append("<tr><td height=\"5\"></td></tr>");
		sb.append("</table><br>");

		// 等級選擇按鈕，每行 5 個
		sb.append("<table width=\"290\">");
		int col = 0;
		for (int lv = 1; lv <= currentLevel; lv++)
		{
			if (col == 0) sb.append("<tr>");
			sb.append("<td align=\"center\">");
			if (equipped && equippedLevel == lv)
			{
				sb.append("<button value=\"Lv.").append(lv).append("\" action=\"bypass -h Quest chenghao unequipSeries_").append(seriesId);
				sb.append("\" width=\"50\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			}
			else
			{
				sb.append("<button value=\"Lv.").append(lv).append("\" action=\"bypass -h Quest chenghao equipSeriesLevel_").append(seriesId).append("_").append(lv);
				sb.append("\" width=\"50\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			}
			sb.append("</td>");
			col++;
			if (col == 5 || lv == currentLevel)
			{
				sb.append("</tr>");
				col = 0;
			}
		}
		sb.append("</table><br>");
		sb.append("<button value=\"返回\" action=\"bypass -h Quest chenghao showSeries_").append(seriesId);
		sb.append("\" width=\"100\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</center></body></html>");

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	private int getSeriesPage(String seriesId)
	{
		int index = 0;
		for (TitleSeries series : TitleSystem.getAllSeries())
		{
			if (series.getSeriesId().equals(seriesId))
			{
				return index / 5;
			}
			index++;
		}
		return 0;
	}

	private void removeOldSkills(Player player)
	{
		boolean removed = false;
		// 移除舊版實驗體技能 (100001~100035)
		for (int skillId = TitleSystem.OLD_SKILL_ID_START; skillId <= TitleSystem.OLD_SKILL_ID_END; skillId++)
		{
			if (player.getKnownSkill(skillId) != null)
			{
				player.removeSkill(skillId);
				removed = true;
			}
		}

		if (removed)
		{
			player.sendPacket(new SkillList());
			player.sendMessage("已移除舊版稱號技能");
		}
	}

	private void activatePermanentSkills(Player player)
	{
		for (TitleSeries series : TitleSystem.getAllSeries())
		{
			// 只有達到最高等級才永久啟用技能
			if (isSeriesFused(player, series))
			{
				String levelKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_level";
				int currentLevel = player.getVariables().getInt(levelKey, 0);

				// 只有達到最高等級才永久啟用
				if (currentLevel >= series.getMaxLevel())
				{
					Skill skill = SkillData.getInstance().getSkill(series.getFinalSkillId(), currentLevel);
					if (skill != null && player.getKnownSkill(series.getFinalSkillId()) == null)
					{
						player.addSkill(skill, true);
					}
				}
			}
		}
		player.sendPacket(new SkillList());
	}

	/**
	 * 檢查並重新套用玩家所有稱號技能，確保等級正確。
	 * 規則：
	 *   - 已融合且達到最高等級 → 永久啟用（技能等級 = 當前融合等級）
	 *   - 已融合、配戴中、但尚未滿級 → 啟用配戴等級
	 *   - 其餘情況（未融合 / 未配戴且未滿級）→ 不應有此技能
	 * 檢查完畢後透過 sendMessage 報告結果。
	 */
	private void checkAndReapplySkills(Player player)
	{
		final String equippedSeriesId = player.getVariables().getString(TitleSystem.VAR_PREFIX + "equipped", null);
		final int equippedLevelVar = player.getVariables().getInt(TitleSystem.VAR_PREFIX + "equipped_level", 0);

		int fixedCount = 0;
		final StringBuilder fixLog = new StringBuilder();
		boolean hasFusedSeries = false;

		for (TitleSeries series : TitleSystem.getAllSeries())
		{
			final boolean fused = isSeriesFused(player, series);
			final String levelKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_level";
			final int currentLevel = fused ? player.getVariables().getInt(levelKey, 0) : 0;

			if (fused)
			{
				hasFusedSeries = true;
			}

			// 決定此系列應該持有的技能等級
			final int expectedLevel;
			final String reason;

			if (fused && (currentLevel >= series.getMaxLevel()))
			{
				// 已滿級 → 永久技能，無論是否配戴都應存在
				expectedLevel = currentLevel;
				reason = "永久（已滿級 Lv." + currentLevel + "）";
			}
			else if (fused && series.getSeriesId().equals(equippedSeriesId))
			{
				// 配戴中但未滿級 → 使用玩家選擇的配戴等級
				int eLevel = equippedLevelVar > 0 ? equippedLevelVar : currentLevel;
				if (eLevel > currentLevel)
				{
					eLevel = currentLevel;
				}
				expectedLevel = eLevel;
				reason = "配戴中（Lv." + eLevel + "）";
			}
			else
			{
				// 未融合 / 未滿級且未配戴 → 不應擁有技能
				expectedLevel = 0;
				reason = fused ? "未配戴且未滿級" : "尚未融合";
			}

			// 取得玩家目前實際擁有的技能等級
			final Skill currentSkill = player.getKnownSkill(series.getFinalSkillId());
			final int actualLevel = currentSkill != null ? currentSkill.getLevel() : 0;

			if (actualLevel == expectedLevel)
			{
				continue; // 狀態正確，不需修正
			}

			// 需要修正
			fixedCount++;
			if (expectedLevel == 0)
			{
				// 移除多餘技能
				player.removeSkill(series.getFinalSkillId());
				fixLog.append("【").append(series.getSeriesName()).append("】移除多餘技能（").append(reason).append("）\n");
			}
			else
			{
				// 補上或修正技能等級
				final Skill skill = SkillData.getInstance().getSkill(series.getFinalSkillId(), expectedLevel);
				if (skill != null)
				{
					player.addSkill(skill, true);
					if (actualLevel == 0)
					{
						fixLog.append("【").append(series.getSeriesName()).append("】補上缺失技能 Lv.").append(expectedLevel).append("（").append(reason).append("）\n");
					}
					else
					{
						fixLog.append("【").append(series.getSeriesName()).append("】修正 Lv.").append(actualLevel).append(" → Lv.").append(expectedLevel).append("（").append(reason).append("）\n");
					}
				}
			}
		}

		// 有異常才需要重新整理技能欄
		if (fixedCount > 0)
		{
			player.sendPacket(new SkillList());
		}

		// 玩家尚未融合任何系列 → 不顯示訊息
		if (!hasFusedSeries)
		{
			return;
		}

		// ===== 報告 =====
		player.sendMessage("========== 稱號技能檢查 ==========");
		if (fixedCount == 0)
		{
			player.sendMessage("所有稱號技能狀態正常。");
		}
		else
		{
			player.sendMessage("發現並修正 " + fixedCount + " 個異常：");
			for (String line : fixLog.toString().split("\n"))
			{
				if (!line.isEmpty())
				{
					player.sendMessage(line);
				}
			}
		}

		// 顯示各系列目前狀態
		player.sendMessage("--- 稱號技能現況 ---");
		for (TitleSeries series : TitleSystem.getAllSeries())
		{
			if (!isSeriesFused(player, series))
			{
				continue;
			}
			final String levelKey = TitleSystem.VAR_PREFIX + series.getSeriesId() + "_level";
			final int level = player.getVariables().getInt(levelKey, 0);
			final Skill sk = player.getKnownSkill(series.getFinalSkillId());
			final String status;
			if (level >= series.getMaxLevel())
			{
				status = "永久 Lv." + level;
			}
			else if (series.getSeriesId().equals(equippedSeriesId))
			{
				status = "配戴中 Lv." + (sk != null ? sk.getLevel() : 0);
			}
			else
			{
				status = "未啟用（Lv." + level + " 可配戴）";
			}
			player.sendMessage(series.getSeriesName() + "：" + status);
		}
		player.sendMessage("===================================");
	}

	public static void main(String[] args)
	{
		System.out.println("【系統】稱號系統載入完畢！");
		new chenghao();
	}
}
