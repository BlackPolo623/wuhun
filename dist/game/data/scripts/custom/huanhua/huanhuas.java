package custom.huanhua;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class huanhuas extends Script
{
	// ==================== 配置區 ====================
	private static final int NPC_ID = 901008;
	private static final String HTML_PATH = "data/scripts/custom/huanhua/";

	// 幻化價格配置
	private static final int PRICE_ITEM_ID = 57;
	private static final int PRICE_ITEM_COUNT = 1000000;

	// 每頁顯示數量
	private static final int RACES_PER_PAGE = 6;

	// 種族配置 <種族ID, 顯示名稱>
	private static final Map<Integer, String> RACE_CONFIG = new LinkedHashMap<>();
	static
	{
		RACE_CONFIG.put(1, "人類戰士");
		RACE_CONFIG.put(2, "人類法師");
		RACE_CONFIG.put(3, "白精靈");
		RACE_CONFIG.put(4, "黑精靈");
		RACE_CONFIG.put(5, "獸人戰士");
		RACE_CONFIG.put(6, "獸人法師");
		RACE_CONFIG.put(7, "矮人");
		RACE_CONFIG.put(8, "鳥人");
		RACE_CONFIG.put(9, "死亡騎士人類");
		RACE_CONFIG.put(10, "死亡騎士精靈");
		RACE_CONFIG.put(11, "死亡騎士黑精靈");
		RACE_CONFIG.put(12, "獸人先鋒");
		RACE_CONFIG.put(15, "高等精靈");
		RACE_CONFIG.put(16, "狼人");
		RACE_CONFIG.put(17, "血薔薇");
		RACE_CONFIG.put(18, "暗鴉武士");
	}

	// ==================== 系統代碼 ====================

	public huanhuas()
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
			showMainPage(player, npc);
		}
		else if (event.startsWith("showList"))
		{
			StringTokenizer st = new StringTokenizer(event.substring(9), " ");
			int page = Integer.parseInt(st.nextToken());
			showRaceListPage(player, npc, page);
		}
		else if (event.startsWith("transform"))
		{
			StringTokenizer st = new StringTokenizer(event.substring(10), " ");
			int raceId = Integer.parseInt(st.nextToken());
			int page = Integer.parseInt(st.nextToken());
			transformRace(player, npc, raceId, page);
		}
		else if (event.equals("reset"))
		{
			resetTransform(player, npc);
		}

		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainPage(player, npc);
		return null;
	}

	private void showMainPage(Player player, Npc npc)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, HTML_PATH + "main.htm");

		int currentRace = player.getVariables().getInt("外形幻化", 0);
		String raceName = currentRace > 0 ? RACE_CONFIG.getOrDefault(currentRace, "未知") : "無";

		html.replace("%current_race%", raceName);
		html.replace("%price%", formatNumber(PRICE_ITEM_COUNT));

		player.sendPacket(html);
	}

	private void showRaceListPage(Player player, Npc npc, int page)
	{
		int totalRaces = RACE_CONFIG.size();
		int totalPages = (int) Math.ceil((double) totalRaces / RACES_PER_PAGE);
		page = Math.max(1, Math.min(page, totalPages));

		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, HTML_PATH + "list.htm");

		html.replace("%race_list%", generateRaceList(player, page));
		html.replace("%prev_button%", generatePrevButton(page));
		html.replace("%next_button%", generateNextButton(page, totalPages));

		player.sendPacket(html);
	}

	private String generateRaceList(Player player, int page)
	{
		StringBuilder sb = new StringBuilder();
		int currentRace = player.getVariables().getInt("外形幻化", 0);
		long playerGold = player.getInventory().getInventoryItemCount(PRICE_ITEM_ID, 0);

		int startIdx = (page - 1) * RACES_PER_PAGE;
		int endIdx = Math.min(startIdx + RACES_PER_PAGE, RACE_CONFIG.size());

		int index = 0;
		for (Map.Entry<Integer, String> entry : RACE_CONFIG.entrySet())
		{
			if (index >= startIdx && index < endIdx)
			{
				int raceId = entry.getKey();
				String raceName = entry.getValue();
				boolean isCurrent = raceId == currentRace;
				boolean canAfford = playerGold >= PRICE_ITEM_COUNT;

				sb.append("<tr bgcolor=\"222222\" height=\"35\">");
				sb.append("<td align=\"center\" width=\"180\">");
				sb.append("<font color=\"").append(isCurrent ? "FFFF00" : "00FF66").append("\">").append(raceName).append("</font>");
				sb.append("</td>");
				sb.append("<td align=\"center\" width=\"100\">");

				if (isCurrent)
				{
					sb.append("<font color=\"00FF66\">使用中</font>");
				}
				else if (!canAfford)
				{
					sb.append("<font color=\"FF3333\">金幣不足</font>");
				}
				else
				{
					sb.append("<button value=\"幻化\" action=\"bypass -h Quest huanhuas transform ").append(raceId).append(" ").append(page);
					sb.append("\" width=\"70\" height=\"22\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
				}

				sb.append("</td>");
				sb.append("</tr>");
			}
			index++;
		}

		return sb.toString();
	}

	private String generatePrevButton(int page)
	{
		if (page > 1)
		{
			return "<button value=\"上一頁\" action=\"bypass -h Quest huanhuas showList " + (page - 1) +
					"\" width=\"80\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">";
		}
		return "";
	}

	private String generateNextButton(int page, int totalPages)
	{
		if (page < totalPages)
		{
			return "<button value=\"下一頁\" action=\"bypass -h Quest huanhuas showList " + (page + 1) +
					"\" width=\"80\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">";
		}
		return "";
	}

	private void transformRace(Player player, Npc npc, int raceId, int page)
	{
		if (!RACE_CONFIG.containsKey(raceId))
		{
			player.sendMessage("無效的種族ID！");
			return;
		}

		long playerGold = player.getInventory().getInventoryItemCount(PRICE_ITEM_ID, 0);
		if (playerGold < PRICE_ITEM_COUNT)
		{
			player.sendMessage("金幣不足！需要 " + formatNumber(PRICE_ITEM_COUNT) + " 金幣");
			showRaceListPage(player, npc, page);
			return;
		}

		player.destroyItemByItemId(null, PRICE_ITEM_ID, PRICE_ITEM_COUNT, null, true);
		player.getVariables().set("外形幻化", raceId);

		player.sendMessage("========================================");
		player.sendMessage("成功幻化為：" + RACE_CONFIG.get(raceId));
		player.sendMessage("消耗：" + formatNumber(PRICE_ITEM_COUNT) + " 金幣");
		player.sendMessage("========================================");

		showRaceListPage(player, npc, page);
	}

	private void resetTransform(Player player, Npc npc)
	{
		player.getVariables().remove("外形幻化");
		player.sendMessage("已重置種族幻化！");
		showMainPage(player, npc);
	}

	private String formatNumber(long number)
	{
		return String.format("%,d", number);
	}

	public static void main(String[] args)
	{
		new huanhuas();
		System.out.println("【系統】種族幻化系統載入完畢！");
	}
}