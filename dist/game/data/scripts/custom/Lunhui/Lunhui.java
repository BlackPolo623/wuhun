package custom.Lunhui;

import java.util.Collection;

import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class Lunhui extends Script
{
	// ============ 配置區 ============
	private static final int NPC_ID = 900014;

	// 第一次習得雙職業配置 - {道具ID, 數量}
	private static final int[][] FIRST_LEARN_ITEMS = {
			{105805, 12},
			{105801, 300},
			{91481, 500},
			{57, 100000000},
	};
	private static final int FIRST_LEARN_MIN_LEVEL = 95;
	private static final int FIRST_LEARN_MIN_CLASS = 88;

	// 更換雙職業配置 - {道具ID, 數量}
	private static final int[][] CHANGE_ITEMS = {
			{105805, 5},
			{105801, 500},
			{91481, 1000},
			{57, 200000000},
	};

	// ============ 構造函數 ============
	private Lunhui()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	// ============ 事件處理 ============
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = "";

		if (event.equals("start"))
		{
			return showMainPage(player);
		}
		else if (event.equals("firstlearn"))
		{
			return showFirstLearnPage(player);
		}
		else if (event.equals("change"))
		{
			return showChangePage(player);
		}
		else if (event.startsWith("showFirstConfirm_"))
		{
			String className = event.substring(17);
			int classId = getClassIdByName(className);
			return showFirstConfirmPage(player, classId);
		}
		else if (event.startsWith("showChangeConfirm_"))
		{
			String className = event.substring(18);
			int classId = getClassIdByName(className);
			return showChangeConfirmPage(player, classId);
		}
		else if (event.startsWith("confirmFirstLearn_"))
		{
			String classIdStr = event.substring(18);
			int classId = Integer.parseInt(classIdStr);
			handleFirstLearn(player, classId);
		}
		else if (event.startsWith("confirmChange_"))
		{
			String classIdStr = event.substring(14);
			int classId = Integer.parseInt(classIdStr);
			handleChange(player, classId);
		}

		return htmltext;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return showMainPage(player);
	}

	// ============ 主要功能方法 ============

	private String showMainPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/Lunhui/start.htm");

		int currentDualClass = player.getVariables().getInt("雙職業", 0);
		String currentClassName = "尚未習得";
		if (currentDualClass > 0)
		{
			currentClassName = ClassListData.getInstance().getClass(currentDualClass).getClassName();
		}

		html.replace("%current_dual_class%", currentClassName);
		player.sendPacket(html);
		return "";
	}

	private String showFirstLearnPage(Player player)
	{
		int currentDualClass = player.getVariables().getInt("雙職業", 0);
		if (currentDualClass > 0)
		{
			player.sendPacket(new ExShowScreenMessage("您已經習得雙職業，請使用更換職業功能", 3000));
			return "";
		}

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/Lunhui/firstlearn.htm");
		html.replace("%min_level%", String.valueOf(FIRST_LEARN_MIN_LEVEL));
		player.sendPacket(html);
		return "";
	}

	private String showChangePage(Player player)
	{
		int currentDualClass = player.getVariables().getInt("雙職業", 0);
		if (currentDualClass == 0)
		{
			player.sendPacket(new ExShowScreenMessage("您尚未習得雙職業，請先習得雙職業", 3000));
			return "";
		}

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/Lunhui/change.htm");
		player.sendPacket(html);
		return "";
	}

	private String showFirstConfirmPage(Player player, int classId)
	{
		if (classId == 0)
		{
			player.sendPacket(new ExShowScreenMessage("錯誤：職業ID無效", 3000));
			return "";
		}

		PlayerClass targetClass = PlayerClass.getPlayerClass(classId);
		if (targetClass == null)
		{
			player.sendPacket(new ExShowScreenMessage("錯誤：職業不存在", 3000));
			return "";
		}

		int currentDualClass = player.getVariables().getInt("雙職業", 0);
		if (currentDualClass > 0)
		{
			player.sendPacket(new ExShowScreenMessage("您已經習得雙職業", 3000));
			return "";
		}

		if (player.getLevel() < FIRST_LEARN_MIN_LEVEL)
		{
			player.sendPacket(new ExShowScreenMessage("等級不足，需要達到 " + FIRST_LEARN_MIN_LEVEL + " 級", 3000));
			return "";
		}

		if (player.getPlayerClass().getId() < FIRST_LEARN_MIN_CLASS)
		{
			player.sendPacket(new ExShowScreenMessage("您的職業尚未三轉", 3000));
			return "";
		}

		if (player.getPlayerClass().getId() == classId)
		{
			player.sendPacket(new ExShowScreenMessage("不能設定為與主職業相同", 3000));
			return "";
		}

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/Lunhui/confirm_first.htm");

		String className = ClassListData.getInstance().getClass(classId).getClassName();
		html.replace("%class_name%", className);
		html.replace("%class_id%", String.valueOf(classId));

		StringBuilder itemList = new StringBuilder();
		boolean hasAllItems = true;

		for (int[] itemData : FIRST_LEARN_ITEMS)
		{
			int itemId = itemData[0];
			int needCount = itemData[1];
			long hasCount = player.getInventory().getInventoryItemCount(itemId, 0);
			ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
			String itemName = item != null ? item.getName() : "未知道具";

			String color = hasCount >= needCount ? "00FF00" : "FF0000";
			itemList.append("<tr><td align=center><font color=\"").append(color).append("\">")
					.append(itemName).append("</font></td>");
			itemList.append("<td align=center><font color=\"").append(color).append("\">")
					.append(hasCount).append(" / ").append(needCount).append("</font></td></tr>");

			if (hasCount < needCount)
			{
				hasAllItems = false;
			}
		}

		html.replace("%item_list%", itemList.toString());
		html.replace("%can_learn%", hasAllItems ? "yes" : "no");

		player.sendPacket(html);
		return "";
	}

	private String showChangeConfirmPage(Player player, int classId)
	{
		if (classId == 0)
		{
			player.sendPacket(new ExShowScreenMessage("錯誤：職業ID無效", 3000));
			return "";
		}

		PlayerClass targetClass = PlayerClass.getPlayerClass(classId);
		if (targetClass == null)
		{
			player.sendPacket(new ExShowScreenMessage("錯誤：職業不存在", 3000));
			return "";
		}

		int currentDualClass = player.getVariables().getInt("雙職業", 0);
		if (currentDualClass == 0)
		{
			player.sendPacket(new ExShowScreenMessage("您尚未習得雙職業", 3000));
			return "";
		}

		if (currentDualClass == classId)
		{
			player.sendPacket(new ExShowScreenMessage("您已經是這個雙職業了", 3000));
			return "";
		}

		if (player.getPlayerClass().getId() == classId)
		{
			player.sendPacket(new ExShowScreenMessage("不能設定為與主職業相同", 3000));
			return "";
		}

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/Lunhui/confirm_change.htm");

		String className = ClassListData.getInstance().getClass(classId).getClassName();
		html.replace("%class_name%", className);
		html.replace("%class_id%", String.valueOf(classId));

		StringBuilder itemList = new StringBuilder();
		boolean hasAllItems = true;

		for (int[] itemData : CHANGE_ITEMS)
		{
			int itemId = itemData[0];
			int needCount = itemData[1];
			long hasCount = player.getInventory().getInventoryItemCount(itemId, 0);
			ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
			String itemName = item != null ? item.getName() : "未知道具";

			String color = hasCount >= needCount ? "00FF00" : "FF0000";
			itemList.append("<tr><td align=center><font color=\"").append(color).append("\">")
					.append(itemName).append("</font></td>");
			itemList.append("<td align=center><font color=\"").append(color).append("\">")
					.append(hasCount).append(" / ").append(needCount).append("</font></td></tr>");

			if (hasCount < needCount)
			{
				hasAllItems = false;
			}
		}

		html.replace("%item_list%", itemList.toString());
		html.replace("%can_change%", hasAllItems ? "yes" : "no");

		player.sendPacket(html);
		return "";
	}

	private void handleFirstLearn(Player player, int classId)
	{
		PlayerClass targetClass = PlayerClass.getPlayerClass(classId);
		if (targetClass == null)
		{
			player.sendPacket(new ExShowScreenMessage("錯誤：職業不存在", 3000));
			return;
		}

		int currentDualClass = player.getVariables().getInt("雙職業", 0);
		if (currentDualClass > 0)
		{
			player.sendPacket(new ExShowScreenMessage("您已經習得雙職業", 3000));
			return;
		}

		if (player.getLevel() < FIRST_LEARN_MIN_LEVEL)
		{
			player.sendPacket(new ExShowScreenMessage("等級不足", 3000));
			return;
		}

		if (player.getPlayerClass().getId() < FIRST_LEARN_MIN_CLASS)
		{
			player.sendPacket(new ExShowScreenMessage("您的職業尚未三轉", 3000));
			return;
		}

		if (player.getPlayerClass().getId() == classId)
		{
			player.sendPacket(new ExShowScreenMessage("不能設定為與主職業相同", 3000));
			return;
		}

		for (int[] itemData : FIRST_LEARN_ITEMS)
		{
			int itemId = itemData[0];
			int needCount = itemData[1];
			if (player.getInventory().getInventoryItemCount(itemId, 0) < needCount)
			{
				player.sendPacket(new ExShowScreenMessage("道具不足", 3000));
				return;
			}
		}

		for (int[] itemData : FIRST_LEARN_ITEMS)
		{
			player.destroyItemByItemId(ItemProcessType.NONE, itemData[0], itemData[1], player, true);
		}

		player.getVariables().set("雙職業", classId);
		player.addtwoclass();

		String targetClassName = ClassListData.getInstance().getClass(classId).getClassName();
		player.sendPacket(new ExShowScreenMessage("成功習得雙職業：" + targetClassName + "，請重新登錄", 5000));
	}

	private void handleChange(Player player, int classId)
	{
		PlayerClass targetClass = PlayerClass.getPlayerClass(classId);
		if (targetClass == null)
		{
			player.sendPacket(new ExShowScreenMessage("錯誤：職業不存在", 3000));
			return;
		}

		int currentDualClass = player.getVariables().getInt("雙職業", 0);
		if (currentDualClass == 0)
		{
			player.sendPacket(new ExShowScreenMessage("您尚未習得雙職業", 3000));
			return;
		}

		if (currentDualClass == classId)
		{
			player.sendPacket(new ExShowScreenMessage("您已經是這個雙職業了", 3000));
			return;
		}

		if (player.getPlayerClass().getId() == classId)
		{
			player.sendPacket(new ExShowScreenMessage("不能設定為與主職業相同", 3000));
			return;
		}

		for (int[] itemData : CHANGE_ITEMS)
		{
			int itemId = itemData[0];
			int needCount = itemData[1];
			if (player.getInventory().getInventoryItemCount(itemId, 0) < needCount)
			{
				player.sendPacket(new ExShowScreenMessage("道具不足", 3000));
				return;
			}
		}

		for (int[] itemData : CHANGE_ITEMS)
		{
			player.destroyItemByItemId(ItemProcessType.NONE, itemData[0], itemData[1], player, true);
		}

		final Collection<Skill> skills = SkillTreeData.getInstance().getAllAvailableSkills(player, PlayerClass.getPlayerClass(currentDualClass), true, true, true);
		for (Skill skill : skills)
		{
			player.stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
			player.removeSkill(skill);
		}

		player.getVariables().set("雙職業", classId);
		player.addtwoclass();

		String targetClassName = ClassListData.getInstance().getClass(classId).getClassName();
		player.sendPacket(new ExShowScreenMessage("成功更換雙職業為：" + targetClassName + "，請重新登錄", 5000));
	}

	// ============ 輔助方法 ============

	private int getClassIdByName(String className)
	{
		switch (className)
		{
			case "決鬥者": return 88;
			case "猛將": return 89;
			case "聖凰騎士": return 90;
			case "煉獄騎士": return 91;
			case "人馬": return 92;
			case "冒險英豪": return 93;
			case "大魔導士": return 94;
			case "魂狩術士": return 95;
			case "秘儀召主": return 96;
			case "樞機主教": return 97;
			case "昭聖者": return 98;
			case "伊娃神殿騎士": return 99;
			case "伊娃吟遊詩人": return 100;
			case "疾風浪人": return 101;
			case "月光箭靈": return 102;
			case "伊娃秘術詩人": return 103;
			case "元素支配者": return 104;
			case "伊娃聖者": return 105;
			case "席琳冥殿騎士": return 106;
			case "幽冥舞者": return 107;
			case "魅影獵者": return 108;
			case "幽冥箭靈": return 109;
			case "暴風狂嘯者": return 110;
			case "闇影支配者": return 111;
			case "席琳聖者": return 112;
			case "泰坦": return 113;
			case "卡巴塔里宗師": return 114;
			case "君主": return 115;
			case "末日戰狂": return 116;
			case "財富獵人": return 117;
			case "巨匠": return 118;
			case "末日使者": return 131;
			case "魔彈射手": return 134;
			case "追魂者": return 195;
			case "死亡騎士人類": return 199;
			case "死亡騎士精靈": return 203;
			case "死亡騎士黑暗精靈": return 207;
			case "暴風爆破者": return 211;
			case "先鋒": return 220;
			case "人類暗殺者": return 224;
			case "黑暗精靈暗殺者": return 228;
			case "伊娃神諭者": return 239;
			case "伊娃聖堂武士": return 243;
			case "狼人": return 250;
			case "血薔薇": return 254;
			case "闇鴉武士": return 263;
			default: return 0;
		}
	}

	public static void main(String[] args)
	{
		new Lunhui();
		System.out.println("雙職業系統載入成功");
	}
}