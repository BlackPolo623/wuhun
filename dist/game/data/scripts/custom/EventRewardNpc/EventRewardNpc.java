/**
 * 多活動獎勵整合系統
 */
package custom.EventRewardNpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class EventRewardNpc extends Script
{
	// NPC ID
	private final static int NPC_ID = 900011;

	// ========== 活動配置區 ==========
	// 每次新增活動，只需在下方列表中添加新的EventConfig即可

	private static final List<EventConfig> EVENTS = new ArrayList<>();

	static
	{
		// 活動1：聖誕狂歡
		EVENTS.add(new EventConfig(
				"20251221",           // 活動ID（唯一標識）
				"20251221推王活動",              // 活動名稱
				new int[][] {                // 獎勵物品
						{57, 17500000},
						{108000, 10},
						{108001, 10},
						{108002, 10},
						{108003, 10},
						{108004, 10},
						{108005, 10},
						{108006, 10},
						{108007, 10},
						{108008, 10},
						{107005, 2},
						{107006, 2},
						{94871, 5},
				},
				new HashSet<>(Arrays.asList( // 獲獎名單
						"遊戲管理員",
						"辣手摧花",
						"DEX",
						"奇",
						"好",
						"小風時雨",
						"lol",
						"苏瑀",
						"月神",
						"愛的歸宿",
						"老王老王",
						"掃地機器人",
						"愛的小手",
						"水之月",
						"精靈哇哇",
						"狗今生",
						"玄璃月",
						"公司招聘",
						"麥當勞",
						"影"
				))
		));

		EVENTS.add(new EventConfig(
				"20260103",           // 活動ID(唯一標識)
				"20260103推王活動",              // 活動名稱
				new int[][] {                // 獎勵物品
						{57, 438000000},
						{92314, 64},
						{108000, 21},
						{108001, 21},
						{108002, 21},
						{108003, 21},
						{108004, 21},
						{108005, 21},
						{108006, 21},
						{108007, 21},
						{108008, 21},
						{101214, 4},
						{103461, 13},
						{57, 1331262},
						{105801, 362},
				},
				new HashSet<>(Arrays.asList( // 獲獎名單
						"遊戲管理員",
						"茗玥",
						"麥當勞",
						"三眼怪",
						"史努比",
						"糊塗塌客",
						"愛的小手",
						"撒枯拉",
						"出來吧鐵支",
						"加西雅",
						"熱炒涼麵",
						"99",
						"影",
						"弓狐無忌",
						"紅髮",
						"玄璃月",
						"亞希米勒",
						"歐拉夫",
						"女殺手",
						"阿修羅",
						"三眼仔",
						"四處逛逛",
						"橘右京",
						"清木真罩",
						"宣兒",
						"閻浮堤",
						"暗夜飛",
						"雷亞利斯",
						"奇",
						"買茶葉蛋刷卡分期",
						"史安琪",
						"Witch",
						"奶茶",
						"佐藤楓",
						"快射了",
						"梁山伯與豬硬來",
						"精靈哇哇",
						"塵世無仙",
						"櫻花乂甜不辣"
				))
		));

		// ========== 在此處繼續添加新活動 ==========
		// EVENTS.add(new EventConfig(...));
	}

	// ========== 活動配置類 ==========
	private static class EventConfig
	{
		String eventId;          // 活動唯一ID
		String eventName;        // 活動名稱
		int[][] rewards;         // 獎勵物品 [itemId, count]
		Set<String> playerList;  // 獲獎玩家名單

		public EventConfig(String eventId, String eventName, int[][] rewards, Set<String> playerList)
		{
			this.eventId = eventId;
			this.eventName = eventName;
			this.rewards = rewards;
			this.playerList = playerList;
		}

		public boolean canClaim(Player player)
		{
			// 檢查是否在名單中且未領取
			if (!playerList.contains(player.getName()))
			{
				return false;
			}

			String varName = "EventReward_" + eventId;
			return !player.getVariables().getBoolean(varName, false);
		}
	}

	// ========== 系統代碼 ==========

	private EventRewardNpc()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("claim_"))
		{
			String eventId = event.substring(6); // 移除 "claim_" 前綴
			claimEventReward(player, npc, eventId);
		}

		return null;
	}

	private void claimEventReward(Player player, Npc npc, String eventId)
	{
		// 尋找對應的活動
		EventConfig targetEvent = null;
		for (EventConfig config : EVENTS)
		{
			if (config.eventId.equals(eventId))
			{
				targetEvent = config;
				break;
			}
		}

		if (targetEvent == null)
		{
			player.sendMessage("找不到該活動配置。");
			return;
		}

		String varName = "EventReward_" + eventId;

		// 檢查是否在名單中
		if (!targetEvent.playerList.contains(player.getName()))
		{
			player.sendMessage("您不在【" + targetEvent.eventName + "】的獲獎名單中。");
			showMainMenu(player, npc);
			return;
		}

		// 檢查是否已領取
		if (player.getVariables().getBoolean(varName, false))
		{
			player.sendMessage("您已經領取過【" + targetEvent.eventName + "】的獎勵了！");
			showMainMenu(player, npc);
			return;
		}

		// 檢查背包空間
		if (!player.getInventory().validateCapacity(targetEvent.rewards.length))
		{
			player.sendMessage("背包空間不足，請清理後再來領取。");
			showMainMenu(player, npc);
			return;
		}

		// 發放獎勵
		for (int[] reward : targetEvent.rewards)
		{
			player.addItem(ItemProcessType.NONE, reward[0], reward[1], player, true);
		}

		// 記錄領取狀態
		player.getVariables().set(varName, true);
		player.getVariables().storeMe();

		player.sendMessage("恭喜！成功領取【" + targetEvent.eventName + "】獎勵！");
		showMainMenu(player, npc);
	}

	private void showMainMenu(Player player, Npc npc)
	{
		// 過濾出該玩家可以領取的活動
		List<EventConfig> availableEvents = new ArrayList<>();
		for (EventConfig config : EVENTS)
		{
			if (config.canClaim(player))
			{
				availableEvents.add(config);
			}
		}

		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/EventRewardNpc/main.htm");
		html.replace("%player_name%", player.getName());
		html.replace("%available_count%", String.valueOf(availableEvents.size()));
		html.replace("%event_list%", buildEventList(availableEvents));

		player.sendPacket(html);
	}

	private String buildEventList(List<EventConfig> availableEvents)
	{
		if (availableEvents.isEmpty())
		{
			return "<tr><td align=\"center\" height=\"100\"><font color=\"808080\">目前沒有可領取的活動獎勵</font></td></tr>";
		}

		StringBuilder sb = new StringBuilder();
		for (EventConfig config : availableEvents)
		{
			sb.append("<tr><td align=\"center\" height=\"35\">");
			sb.append("<button action=\"bypass -h Quest EventRewardNpc claim_").append(config.eventId).append("\" ");
			sb.append("value=\"").append(config.eventName).append("\" ");
			sb.append("width=\"250\" height=\"31\" ");
			sb.append("back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" ");
			sb.append("fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
			sb.append("</td></tr>");
			sb.append("<tr><td height=\"5\"></td></tr>");
		}

		return sb.toString();
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainMenu(player, npc);
		return null;
	}

	public static void main(String[] args)
	{
		new EventRewardNpc();
		System.out.println("【系統】多活動獎勵系統載入完畢！當前活動數量：" + EVENTS.size());
	}
}