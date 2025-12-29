package custom.chenghao;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.l2jmobius.commons.util.Rnd;
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

public class chenghao extends Script
{
	// ==================== 配置常數 ====================
	private static final int NPC_ID = 900003;
	private static final String VAR_TITLE = "稱號融合系統";
	private static final String HTML_PATH = "data/scripts/custom/chenghao/";

	// 稱號配置
	private static final int SKILL_LEVEL = 1;
	private static final int TITLES_PER_PAGE = 5;

	// 融合配置
	private static final String FUSION_TITLE = "終焉武魂";
	private static final int FUSION_SKILL_ID = 100000;
	private static final int MAX_FUSION_LEVEL = 10;
	private static final int FUSION_ITEM_ID = 57;
	private static final int FUSION_ITEM_COUNT = 5000000;

	// ==================== 融合成功率配置 ====================

	// 失敗懲罰類型
	private static final int FAILURE_PENALTY_TYPE = 1;
	// 1 = 扣材料，保留等級，消耗稱號
	// 2 = 扣材料，降1級，消耗稱號（標準模式）
	// 3 = 扣材料，歸零，消耗稱號（硬核模式）

	private static final Map<String, Integer> TITLE_SKILL_MAP = new LinkedHashMap<>();
	private static final Map<String, Integer> TITLE_BOSS_REQUIREMENT = new LinkedHashMap<>();
	private static final Map<String, String> TITLE_BOSS_NAME = new LinkedHashMap<>();
	private static final Map<Integer, Integer> FUSION_SUCCESS_RATES = new LinkedHashMap<>();

	// ==================== 融合稱號名稱配置（Lv.1-10）====================
	private static final Map<Integer, String> FUSION_TITLE_NAMES = new LinkedHashMap<>();
	static
	{
		// ==================== 融合稱號名稱（可隨意修改）====================
		FUSION_TITLE_NAMES.put(1, "武魂壹♦覺醒");
		FUSION_TITLE_NAMES.put(2, "武魂貳♦蛻變");
		FUSION_TITLE_NAMES.put(3, "武魂參♦進化");
		FUSION_TITLE_NAMES.put(4, "武魂肆♦突破");
		FUSION_TITLE_NAMES.put(5, "武魂伍♦超越");
		FUSION_TITLE_NAMES.put(6, "武魂陸♦完美");
		FUSION_TITLE_NAMES.put(7, "武魂柒♦究極");
		FUSION_TITLE_NAMES.put(8, "武魂捌♦至尊");
		FUSION_TITLE_NAMES.put(9, "武魂玖♦神化");
		FUSION_TITLE_NAMES.put(10, "終焉武魂");

		// ==================== 成功率配置 ====================
		FUSION_SUCCESS_RATES.put(1, 90);  // Lv.1 → Lv.2: 90%
		FUSION_SUCCESS_RATES.put(2, 80);  // Lv.2 → Lv.3: 80%
		FUSION_SUCCESS_RATES.put(3, 70);  // Lv.3 → Lv.4: 70%
		FUSION_SUCCESS_RATES.put(4, 60);  // Lv.4 → Lv.5: 60%
		FUSION_SUCCESS_RATES.put(5, 50);  // Lv.5 → Lv.6: 50%
		FUSION_SUCCESS_RATES.put(6, 40);  // Lv.6 → Lv.7: 40%
		FUSION_SUCCESS_RATES.put(7, 30);  // Lv.7 → Lv.8: 30%
		FUSION_SUCCESS_RATES.put(8, 20);  // Lv.8 → Lv.9: 20%
		FUSION_SUCCESS_RATES.put(9, 10);  // Lv.9 → Lv.10: 15%

		// 首次融合（從0→1）100%成功
		FUSION_SUCCESS_RATES.put(0, 100);

		// ==================== 稱號技能配置 ====================
		TITLE_SKILL_MAP.put("實驗體一號", 100001);
		TITLE_SKILL_MAP.put("實驗體二號", 100002);
		TITLE_SKILL_MAP.put("實驗體三號", 100003);
		TITLE_SKILL_MAP.put("實驗體四號", 100004);
		TITLE_SKILL_MAP.put("實驗體五號", 100005);
		TITLE_SKILL_MAP.put("實驗體六號", 100006);
		TITLE_SKILL_MAP.put("實驗體七號", 100007);
		TITLE_SKILL_MAP.put("實驗體八號", 100008);
		TITLE_SKILL_MAP.put("實驗體九號", 100009);
		TITLE_SKILL_MAP.put("實驗體十號", 100010);
		TITLE_SKILL_MAP.put("實驗體十一號", 100011);
		TITLE_SKILL_MAP.put("實驗體十二號", 100012);
		TITLE_SKILL_MAP.put("實驗體十三號", 100013);
		TITLE_SKILL_MAP.put("實驗體十四號", 100014);
		TITLE_SKILL_MAP.put("實驗體十五號", 100015);
		TITLE_SKILL_MAP.put("實驗體十六號", 100016);
		TITLE_SKILL_MAP.put("實驗體十七號", 100017);
		TITLE_SKILL_MAP.put("實驗體十八號", 100018);
		TITLE_SKILL_MAP.put("實驗體十九號", 100019);
		TITLE_SKILL_MAP.put("實驗體二十號", 100020);
		TITLE_SKILL_MAP.put("實驗體二十一號", 100021);
		TITLE_SKILL_MAP.put("實驗體二十二號", 100022);
		TITLE_SKILL_MAP.put("實驗體二十三號", 100023);
		TITLE_SKILL_MAP.put("實驗體二十四號", 100024);
		TITLE_SKILL_MAP.put("實驗體二十五號", 100025);
		TITLE_SKILL_MAP.put("實驗體二十六號", 100026);
		TITLE_SKILL_MAP.put("實驗體二十七號", 100027);
		TITLE_SKILL_MAP.put("實驗體二十八號", 100028);
		TITLE_SKILL_MAP.put("實驗體二十九號", 100029);
		TITLE_SKILL_MAP.put("實驗體三十號", 100030);
		TITLE_SKILL_MAP.put("實驗體三十一號", 100031);
		TITLE_SKILL_MAP.put("實驗體三十二號", 100032);
		TITLE_SKILL_MAP.put("實驗體三十三號", 100033);
		TITLE_SKILL_MAP.put("實驗體三十四號", 100034);
		TITLE_SKILL_MAP.put("實驗體三十五號", 100035);

// 稱號解鎖條件（boss npcId）
		TITLE_BOSS_REQUIREMENT.put("實驗體一號", 50001);
		TITLE_BOSS_REQUIREMENT.put("實驗體二號", 50002);
		TITLE_BOSS_REQUIREMENT.put("實驗體三號", 50003);
		TITLE_BOSS_REQUIREMENT.put("實驗體四號", 50004);
		TITLE_BOSS_REQUIREMENT.put("實驗體五號", 50005);
		TITLE_BOSS_REQUIREMENT.put("實驗體六號", 50006);
		TITLE_BOSS_REQUIREMENT.put("實驗體七號", 50007);
		TITLE_BOSS_REQUIREMENT.put("實驗體八號", 50008);
		TITLE_BOSS_REQUIREMENT.put("實驗體九號", 50009);
		TITLE_BOSS_REQUIREMENT.put("實驗體十號", 50010);
		TITLE_BOSS_REQUIREMENT.put("實驗體十一號", 50011);
		TITLE_BOSS_REQUIREMENT.put("實驗體十二號", 50012);
		TITLE_BOSS_REQUIREMENT.put("實驗體十三號", 50013);
		TITLE_BOSS_REQUIREMENT.put("實驗體十四號", 50014);
		TITLE_BOSS_REQUIREMENT.put("實驗體十五號", 50015);
		TITLE_BOSS_REQUIREMENT.put("實驗體十六號", 50016);
		TITLE_BOSS_REQUIREMENT.put("實驗體十七號", 50017);
		TITLE_BOSS_REQUIREMENT.put("實驗體十八號", 50018);
		TITLE_BOSS_REQUIREMENT.put("實驗體十九號", 50019);
		TITLE_BOSS_REQUIREMENT.put("實驗體二十號", 50020);
		TITLE_BOSS_REQUIREMENT.put("實驗體二十一號", 50021);
		TITLE_BOSS_REQUIREMENT.put("實驗體二十二號", 50022);
		TITLE_BOSS_REQUIREMENT.put("實驗體二十三號", 50023);
		TITLE_BOSS_REQUIREMENT.put("實驗體二十四號", 50024);
		TITLE_BOSS_REQUIREMENT.put("實驗體二十五號", 50025);
		TITLE_BOSS_REQUIREMENT.put("實驗體二十六號", 50026);
		TITLE_BOSS_REQUIREMENT.put("實驗體二十七號", 50027);
		TITLE_BOSS_REQUIREMENT.put("實驗體二十八號", 50028);
		TITLE_BOSS_REQUIREMENT.put("實驗體二十九號", 50029);
		TITLE_BOSS_REQUIREMENT.put("實驗體三十號", 50030);
		TITLE_BOSS_REQUIREMENT.put("實驗體三十一號", 50031);
		TITLE_BOSS_REQUIREMENT.put("實驗體三十二號", 50032);
		TITLE_BOSS_REQUIREMENT.put("實驗體三十三號", 50033);
		TITLE_BOSS_REQUIREMENT.put("實驗體三十四號", 50034);
		TITLE_BOSS_REQUIREMENT.put("實驗體三十五號", 50035);
	}

	public chenghao()
	{
		addStartNpc(NPC_ID);
		addFirstTalkId(NPC_ID);
		addTalkId(NPC_ID);

		for (int bossId : TITLE_BOSS_REQUIREMENT.values())
		{
			addKillId(bossId);
		}

		// 初始化BOSS名稱緩存
		initBossNames();
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
		else if (event.startsWith("showTitleList"))
		{
			String[] parts = event.split(" ");
			int page = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
			showTitleListPage(player, page);
		}
		else if (event.equals("showFusion"))
		{
			showFusionPage(player);
		}
		// 新增：顯示融合稱號佩戴頁面
		else if (event.equals("showFusionTitle"))
		{
			showFusionTitlePage(player);
		}
		else if (event.startsWith("selectTitle_"))
		{
			String titleName = event.substring(12);
			selectTitle(player, titleName);
		}
		else if (event.equals("doFusion"))
		{
			processFusion(player);
		}
		// 新增：佩戴融合稱號
		else if (event.equals("equipFusionTitle"))
		{
			selectTitle(player, FUSION_TITLE);
			showFusionTitlePage(player); // 刷新頁面
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

		for (Map.Entry<String, Integer> entry : TITLE_BOSS_REQUIREMENT.entrySet())
		{
			String title = entry.getKey();
			int requiredNpcId = entry.getValue();

			if (npc.getId() == requiredNpcId)
			{
				if (!killer.getVariables().getBoolean("title_unlocked_" + title, false))
				{
					killer.getVariables().set("title_unlocked_" + title, true);
					killer.sendMessage("恭喜！你已解鎖稱號：" + title);
					killer.sendMessage("前往稱號NPC即可佩戴該稱號！");
				}
			}
		}
	}

	@Override
	public void onEnterWorld(Player player)
	{
		String currentTitle = player.getVariables().getString(VAR_TITLE, null);
		if (currentTitle == null)
		{
			return;
		}

		// 處理融合稱號
		if (currentTitle.equals(FUSION_TITLE))
		{
			int fusionLevel = player.getVariables().getInt("fusion_level", 0);
			if (fusionLevel > 0)
			{
				Skill fusionSkill = SkillData.getInstance().getSkill(FUSION_SKILL_ID, fusionLevel);
				if (fusionSkill != null)
				{
					player.addSkill(fusionSkill, true);
				}
				// 修改：使用等級對應的稱號名稱
				player.setTitle(getFusionTitleByLevel(fusionLevel));
				player.sendPacket(new SkillList());
				//Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, "公告", "歡迎融合稱號擁有者 " + player.getName() + " 進入天堂2世界"));
			}
		}
		// 處理普通稱號
		else if (TITLE_SKILL_MAP.containsKey(currentTitle))
		{
			int skillId = TITLE_SKILL_MAP.get(currentTitle);
			Skill skill = SkillData.getInstance().getSkill(skillId, SKILL_LEVEL);
			if (skill != null)
			{
				player.addSkill(skill, true);
				player.sendPacket(new SkillList());
			}
			player.setTitle(currentTitle);
			Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, "公告",
					"歡迎【" + currentTitle + "】稱號擁有者 " + player.getName() + " 進入天堂2世界"));
		}
	}

	// ==================== 頁面顯示 ====================

	private void showMainPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, HTML_PATH + "chenghao.htm");

		String currentTitle = player.getVariables().getString(VAR_TITLE, null);
		if (currentTitle == null)
		{
			html.replace("%current_title%", "<font color=\"808080\">無</font>");
		}
		else
		{
			int fusionLevel = player.getVariables().getInt("fusion_level", 0);
			if (currentTitle.equals(FUSION_TITLE) && fusionLevel > 0)
			{
				// 修改：顯示對應等級的稱號名稱
				String displayTitle = getFusionTitleByLevel(fusionLevel);
				html.replace("%current_title%", displayTitle);
			}
			else
			{
				html.replace("%current_title%", currentTitle);
			}
		}

		player.sendPacket(html);
	}

	private void showTitleListPage(Player player, int page)
	{
		int unlockedCount = getUnlockedTitleCount(player);
		int totalCount = TITLE_SKILL_MAP.size();
		int totalPages = (int) Math.ceil((double) totalCount / TITLES_PER_PAGE);

		// 確保頁碼有效
		page = Math.max(1, Math.min(page, totalPages));

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, HTML_PATH + "chenghaolist.htm");

		html.replace("%unlocked_count%", String.valueOf(unlockedCount));
		html.replace("%total_count%", String.valueOf(totalCount));
		html.replace("%list%", generateTitleList(player, page));

		// 分頁按鈕
		StringBuilder prevButton = new StringBuilder();
		StringBuilder nextButton = new StringBuilder();

		if (page > 1)
		{
			prevButton.append("<button value=\"上一頁\" action=\"bypass -h Quest chenghao showTitleList ")
					.append(page - 1).append("\" width=\"80\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		}

		if (page < totalPages)
		{
			nextButton.append("<button value=\"下一頁\" action=\"bypass -h Quest chenghao showTitleList ")
					.append(page + 1).append("\" width=\"80\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		}

		html.replace("%prev_page_button%", prevButton.toString());
		html.replace("%next_page_button%", nextButton.toString());

		player.sendPacket(html);
	}

	private void showFusionPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, HTML_PATH + "chenghaofusion.htm");

		int fusionLevel = player.getVariables().getInt("fusion_level", 0);
		int unlockedCount = getUnlockedTitleCount(player);
		int totalCount = TITLE_SKILL_MAP.size();
		long currentItemCount = player.getInventory().getInventoryItemCount(FUSION_ITEM_ID, 0);

		// 獲取當前等級的成功率
		int successRate = FUSION_SUCCESS_RATES.getOrDefault(fusionLevel, 0);

		// 修改：顯示對應等級的稱號名稱
		String currentFusionTitle = fusionLevel > 0 ? getFusionTitleByLevel(fusionLevel) : "未融合";
		html.replace("%fusion_title%", currentFusionTitle);
		html.replace("%fusion_level%", String.valueOf(fusionLevel));
		html.replace("%max_level%", String.valueOf(MAX_FUSION_LEVEL));
		html.replace("%unlocked_count%", String.valueOf(unlockedCount));
		html.replace("%total_count%", String.valueOf(totalCount));
		html.replace("%required_count%", String.valueOf(FUSION_ITEM_COUNT));
		html.replace("%current_count%", String.valueOf(currentItemCount));
		html.replace("%success_rate%", String.valueOf(successRate));

		// 成功率顏色
		String rateColor = "00FF00"; // 綠色
		if (successRate < 50)
		{
			rateColor = "FF0000"; // 紅色
		}
		else if (successRate < 80)
		{
			rateColor = "FFFF00"; // 黃色
		}
		html.replace("%rate_color%", rateColor);

		// 判斷融合條件
		StringBuilder requirementText = new StringBuilder();
		StringBuilder fusionButton = new StringBuilder();

		boolean hasAllTitles = hasAllTitles(player);
		boolean hasEnoughItems = currentItemCount >= FUSION_ITEM_COUNT;
		boolean canFusion = false;

		if (fusionLevel >= MAX_FUSION_LEVEL)
		{
			requirementText.append("<font color=\"FF0000\">融合稱號已達到最高等級！</font>");
		}
		else if (fusionLevel == 0)
		{
			// 首次融合
			if (!hasAllTitles)
			{
				requirementText.append("<font color=\"FF3333\">需解鎖全部14個基礎稱號</font>");
			}
			else
			{
				requirementText.append("<font color=\"00FF66\">✓ 已解鎖全部稱號</font>");
				canFusion = hasEnoughItems;
			}
		}
		else
		{
			// 升級融合
			if (!hasAllTitles)
			{
				requirementText.append("<font color=\"FF3333\">需重新解鎖全部基礎稱號</font>");
			}
			else
			{
				requirementText.append("<font color=\"00FF66\">✓ 已解鎖全部稱號</font>");
				canFusion = hasEnoughItems;
			}
		}

		html.replace("%fusion_requirement%", requirementText.toString());

		// 融合按鈕
		if (canFusion)
		{
			fusionButton.append("<button action=\"bypass -h Quest chenghao doFusion\" value=\"");
			fusionButton.append(fusionLevel == 0 ? "融合稱號" : "升級稱號");
			fusionButton.append("\" width=\"200\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
		}
		else
		{
			fusionButton.append("<table width=\"200\"><tr><td align=center><font color=\"808080\">條件未滿足</font></td></tr></table>");
		}

		html.replace("%fusion_button%", fusionButton.toString());

		player.sendPacket(html);
	}

	// ==================== 業務邏輯 ====================

	private String generateTitleList(Player player, int page)
	{
		StringBuilder sb = new StringBuilder();
		String currentTitle = player.getVariables().getString(VAR_TITLE, null);

		int startIdx = (page - 1) * TITLES_PER_PAGE;
		int endIdx = Math.min(startIdx + TITLES_PER_PAGE, TITLE_SKILL_MAP.size());

		int index = 0;
		for (Map.Entry<String, Integer> entry : TITLE_SKILL_MAP.entrySet())
		{
			if ((index >= startIdx) && (index < endIdx))
			{
				String titleName = entry.getKey();
				boolean unlocked = player.getVariables().getBoolean("title_unlocked_" + titleName, false);

				sb.append("<tr bgcolor=\"222222\" height=\"30\">");

				if (unlocked)
				{
					// 已解鎖的稱號
					String titleColor = titleName.equals(currentTitle) ? "FFFF00" : "00FF00";
					sb.append("<td align=\"left\" width=\"180\"><font color=\"").append(titleColor).append("\">")
							.append(titleName).append("</font></td>");

					sb.append("<td align=\"center\" width=\"100\">");
					if (titleName.equals(currentTitle))
					{
						sb.append("<font color=\"00FF66\">使用中</font>");
					}
					else
					{
						sb.append("<button value=\"佩戴\" action=\"bypass -h Quest chenghao selectTitle_")
								.append(titleName).append("\" width=\"70\" height=\"22\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
					}
					sb.append("</td>");
				}
				else
				{
					// 未解鎖的稱號
					String bossName = TITLE_BOSS_NAME.getOrDefault(titleName, "未知BOSS");
					sb.append("<td align=\"left\" width=\"180\"><font color=\"808080\">").append(titleName).append("</font></td>");
					sb.append("<td align=\"center\" width=\"100\"><font color=\"FF3333\" size=\"1\">未解鎖</font></td>");
					sb.append("</tr>");
					sb.append("<tr bgcolor=\"222222\">");
					sb.append("<td colspan=\"2\" align=\"center\" height=\"20\"><font color=\"666666\" size=\"1\">需擊殺：")
							.append(bossName).append("</font></td>");
				}

				sb.append("</tr>");
			}
			index++;
		}

		if (sb.length() == 0)
		{
			return "<tr bgcolor=\"222222\"><td colspan=\"2\" align=\"center\" height=\"30\"><font color=\"808080\">無可用稱號</font></td></tr>";
		}

		return sb.toString();
	}

	private void selectTitle(Player player, String titleName)
	{
		// 處理融合稱號選擇
		if (titleName.equals(FUSION_TITLE))
		{
			int fusionLevel = player.getVariables().getInt("fusion_level", 0);
			if (fusionLevel <= 0)
			{
				player.sendMessage("你尚未融合稱號，無法佩戴！");
				return;
			}

			String oldTitle = player.getVariables().getString(VAR_TITLE, null);
			removeOldTitleSkill(player, oldTitle);

			// 修改：使用等級對應的稱號名稱
			player.setTitle(getFusionTitleByLevel(fusionLevel));
			player.broadcastTitleInfo();
			player.getVariables().set(VAR_TITLE, FUSION_TITLE);

			Skill fusionSkill = SkillData.getInstance().getSkill(FUSION_SKILL_ID, fusionLevel);
			if (fusionSkill != null)
			{
				player.addSkill(fusionSkill, true);
				player.sendPacket(new SkillList());
			}

			player.sendMessage("你已佩戴融合稱號：" + getFusionTitleByLevel(fusionLevel));
			return;
		}

		// 處理普通稱號選擇
		if (!TITLE_SKILL_MAP.containsKey(titleName))
		{
			player.sendMessage("無效的稱號！");
			return;
		}

		boolean unlocked = player.getVariables().getBoolean("title_unlocked_" + titleName, false);
		if (!unlocked)
		{
			player.sendMessage("你尚未解鎖該稱號！");
			return;
		}

		String oldTitle = player.getVariables().getString(VAR_TITLE, null);
		removeOldTitleSkill(player, oldTitle);

		player.setTitle(titleName);
		player.broadcastTitleInfo();
		player.getVariables().set(VAR_TITLE, titleName);

		int skillId = TITLE_SKILL_MAP.get(titleName);
		Skill skill = SkillData.getInstance().getSkill(skillId, SKILL_LEVEL);
		if (skill != null)
		{
			player.addSkill(skill, true);
			player.sendPacket(new SkillList());
		}

		player.sendMessage("你已佩戴稱號：" + titleName);
	}

	private void processFusion(Player player)
	{
		int currentLevel = player.getVariables().getInt("fusion_level", 0);

		// 檢查是否已達最高等級
		if (currentLevel >= MAX_FUSION_LEVEL)
		{
			player.sendMessage("融合稱號技能已達到最高等級！");
			showFusionPage(player);
			return;
		}

		// 檢查是否有全部稱號
		if (!hasAllTitles(player))
		{
			if (currentLevel == 0)
			{
				player.sendMessage("你尚未解鎖所有基礎稱號，無法首次融合！");
			}
			else
			{
				player.sendMessage("要升級融合稱號，請重新解鎖全部14個基礎稱號！");
			}
			showFusionPage(player);
			return;
		}

		// 檢查材料
		long itemCount = player.getInventory().getInventoryItemCount(FUSION_ITEM_ID, 0);
		if (itemCount < FUSION_ITEM_COUNT)
		{
			player.sendMessage("所需道具不足，需要 " + FUSION_ITEM_COUNT + " 個金幣！");
			showFusionPage(player);
			return;
		}

		// ==================== 機率判定 ====================
		int successRate = FUSION_SUCCESS_RATES.getOrDefault(currentLevel, 0);
		boolean success = Rnd.get(100) < successRate;

		// 消耗材料（無論成功失敗都扣）
		player.destroyItemByItemId(null, FUSION_ITEM_ID, FUSION_ITEM_COUNT, null, true);

		// 移除基礎稱號技能及解鎖狀態（無論成功失敗都移除）
		for (String baseTitle : TITLE_SKILL_MAP.keySet())
		{
			if (player.getVariables().getBoolean("title_unlocked_" + baseTitle, false))
			{
				int skillId = TITLE_SKILL_MAP.get(baseTitle);
				player.removeSkill(skillId);
				player.getVariables().remove("title_unlocked_" + baseTitle);
			}
		}

		// 移除當前稱號技能
		String oldTitle = player.getVariables().getString(VAR_TITLE, null);
		removeOldTitleSkill(player, oldTitle);

		if (success)
		{
			// ========== 成功邏輯 ==========
			int newLevel = currentLevel + 1;
			player.getVariables().set("fusion_level", newLevel);

			// 修改：使用等級對應的稱號名稱
			player.setTitle(getFusionTitleByLevel(newLevel));
			player.getVariables().set(VAR_TITLE, FUSION_TITLE);

			// 添加融合技能
			Skill fusionSkill = SkillData.getInstance().getSkill(FUSION_SKILL_ID, newLevel);
			if (fusionSkill != null)
			{
				player.addSkill(fusionSkill, true);
				player.sendPacket(new SkillList());
			}

			player.broadcastTitleInfo();

			String newTitleName = getFusionTitleByLevel(newLevel);

			if (currentLevel == 0)
			{
				player.sendMessage("========================================");
				player.sendMessage("恭喜！你成功融合稱號！");
				player.sendMessage("獲得：[" + newTitleName + "] Lv." + newLevel + " 技能");
				player.sendMessage("成功率：" + successRate + "%");
				player.sendMessage("========================================");

				// 全服公告
				Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, "系統公告",
						"恭喜玩家 " + player.getName() + " 成功融合稱號【" + newTitleName + "】！"));
			}
			else
			{
				String oldTitleName = getFusionTitleByLevel(currentLevel);
				player.sendMessage("========================================");
				player.sendMessage("恭喜！融合稱號升級成功！");
				player.sendMessage("[" + oldTitleName + "] → [" + newTitleName + "]");
				player.sendMessage("成功率：" + successRate + "%");
				player.sendMessage("========================================");

				// 高等級升級成功時全服公告
				if (newLevel >= 5)
				{
					Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, "系統公告",
							"恭喜玩家 " + player.getName() + " 將融合稱號升級至【" + newTitleName + "】！"));
				}
			}
		}
		else
		{
			// ========== 失敗邏輯 ==========
			int newLevel = currentLevel;

			switch (FAILURE_PENALTY_TYPE)
			{
				case 1: // 保留等級
					newLevel = currentLevel;
					break;
				case 2: // 降1級
					newLevel = Math.max(0, currentLevel - 1);
					break;
				case 3: // 歸零
					newLevel = 0;
					break;
			}

			player.getVariables().set("fusion_level", newLevel);

			// 如果還有等級，保留融合稱號
			if (newLevel > 0)
			{
				// 修改：使用等級對應的稱號名稱
				player.setTitle(getFusionTitleByLevel(newLevel));
				player.getVariables().set(VAR_TITLE, FUSION_TITLE);

				Skill fusionSkill = SkillData.getInstance().getSkill(FUSION_SKILL_ID, newLevel);
				if (fusionSkill != null)
				{
					player.addSkill(fusionSkill, true);
					player.sendPacket(new SkillList());
				}
			}
			else
			{
				// 等級歸零，移除稱號
				player.setTitle("");
				player.getVariables().remove(VAR_TITLE);
			}

			player.broadcastTitleInfo();

			player.sendMessage("========================================");
			player.sendMessage("很遺憾，稱號融合失敗了...");
			player.sendMessage("成功率：" + successRate + "%");

			if (FAILURE_PENALTY_TYPE == 1)
			{
				player.sendMessage("融合等級保持：Lv." + newLevel);
			}
			else if (FAILURE_PENALTY_TYPE == 2)
			{
				if (currentLevel > 0)
				{
					String oldTitleName = getFusionTitleByLevel(currentLevel);
					String newTitleName = newLevel > 0 ? getFusionTitleByLevel(newLevel) : "無";
					player.sendMessage("融合等級降低：[" + oldTitleName + "] → [" + newTitleName + "]");
				}
				else
				{
					player.sendMessage("首次融合失敗，請重新收集稱號再試。");
				}
			}
			else if (FAILURE_PENALTY_TYPE == 3)
			{
				player.sendMessage("融合等級歸零！需重新開始。");
			}

			player.sendMessage("已消耗材料和全部基礎稱號。");
			player.sendMessage("========================================");
		}

		showFusionPage(player);
	}

	// ==================== 輔助方法 ====================

	private void initBossNames()
	{
		for (Map.Entry<String, Integer> entry : TITLE_BOSS_REQUIREMENT.entrySet())
		{
			String titleName = entry.getKey();
			int bossId = entry.getValue();
			String bossName = getNpcName(bossId);
			TITLE_BOSS_NAME.put(titleName, bossName);
		}
	}

	private String getNpcName(int npcId)
	{
		NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		return template != null ? template.getName() : "未知BOSS";
	}

	private int getUnlockedTitleCount(Player player)
	{
		int count = 0;
		for (String title : TITLE_SKILL_MAP.keySet())
		{
			if (player.getVariables().getBoolean("title_unlocked_" + title, false))
			{
				count++;
			}
		}
		return count;
	}

	private boolean hasAllTitles(Player player)
	{
		for (String title : TITLE_SKILL_MAP.keySet())
		{
			if (!player.getVariables().getBoolean("title_unlocked_" + title, false))
			{
				return false;
			}
		}
		return true;
	}

	private void removeOldTitleSkill(Player player, String oldTitle)
	{
		if (oldTitle == null)
		{
			return;
		}

		if (TITLE_SKILL_MAP.containsKey(oldTitle))
		{
			player.removeSkill(TITLE_SKILL_MAP.get(oldTitle));
		}
		else if (oldTitle.equals(FUSION_TITLE))
		{
			int fusionLevel = player.getVariables().getInt("fusion_level", 0);
			if (fusionLevel > 0)
			{
				player.removeSkill(FUSION_SKILL_ID);
			}
		}
	}
	// 新增方法：顯示融合稱號佩戴頁面
	private void showFusionTitlePage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, HTML_PATH + "chenghaofusiontitle.htm");

		int fusionLevel = player.getVariables().getInt("fusion_level", 0);
		String currentTitle = player.getVariables().getString(VAR_TITLE, null);

		if (fusionLevel <= 0)
		{
			// 尚未融合
			html.replace("%fusion_title_name%", "<font color=\"808080\">尚未融合</font>");
			html.replace("%fusion_level%", "0");
			html.replace("%equipped_status%", "<font color=\"808080\">-</font>");
			html.replace("%equip_button%", "<table width=\"280\"><tr><td align=center><font color=\"FF3333\">您還沒有融合稱號</font></td></tr><tr><td align=center><font color=\"808080\">請先完成稱號融合</font></td></tr></table>");
		}
		else
		{
			// 已融合
			String fusionTitleName = getFusionTitleByLevel(fusionLevel);
			html.replace("%fusion_title_name%", fusionTitleName);
			html.replace("%fusion_level%", String.valueOf(fusionLevel));

			boolean isEquipped = FUSION_TITLE.equals(currentTitle);

			if (isEquipped)
			{
				html.replace("%equipped_status%", "<font color=\"00FF66\">✓ 已佩戴</font>");
				html.replace("%equip_button%", "<table width=\"280\"><tr><td align=center><font color=\"00FF66\" size=\"3\">當前正在使用此稱號</font></td></tr></table>");
			}
			else
			{
				html.replace("%equipped_status%", "<font color=\"808080\">未佩戴</font>");
				html.replace("%equip_button%", "<button action=\"bypass -h Quest chenghao equipFusionTitle\" value=\"佩戴融合稱號\" width=\"200\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
			}
		}

		player.sendPacket(html);
	}

	// ==================== 根據等級獲取稱號名稱 ====================
	private String getFusionTitleByLevel(int level)
	{
		return FUSION_TITLE_NAMES.getOrDefault(level, "未知稱號");
	}

	public static void main(String[] args)
	{
		System.out.println("【系統】稱號融合系統載入完畢！");
		new chenghao();
	}
}