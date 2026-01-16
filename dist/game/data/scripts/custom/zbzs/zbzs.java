package custom.zbzs;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.Custom;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.Armor;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.variables.ItemVariables;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class zbzs extends Script
{

	private static final int NPC_ID = 900002;
	private static final int ITEMS_PER_PAGE = 10; // 每頁顯示道具的數量

	private static int REQUIRED_ITEM_ID = 105801; // 道具 ID
	private static long REQUIRED_ITEM_COUNT = 1; // 每次升級所需數量

	private static final int[] ALLOWED_SLOTS = {
			0, 1, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14  // UNDER, HEAD, NECK, RHAND, CHEST, LHAND, REAR, LEAR, GLOVES, LEGS, FEET, RFINGER, LFINGER
	};

	private static final int MODE_1 = 1;
	private static final int MODE_10 = 10;
	private static final int MODE_50 = 50;

	// 成功率設定陣列 [起始次數, 結束次數, 成功率%]
	private static final int[][] SUCCESS_RATES = {
			{0, 49, 70},
			{50, 99, 50},
			{100, 1000, 30},
			{1001, 1200, 20},
	};

	public zbzs()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((event == null) || (player == null))
		{
			return "";
		}

		if (event.equals("main"))
		{
			return onFirstTalk(npc, player);
		}
		else if (event.startsWith("zhuans"))
		{
			StringTokenizer st = new StringTokenizer(event.substring(7), " ");
			int page = Integer.parseInt(st.nextToken());
			showUpgradePage(player, page);
		}
		else if (event.startsWith("shuxin"))
		{
			StringTokenizer st = new StringTokenizer(event.substring(7), " ");
			int objectId = Integer.parseInt(st.nextToken());
			int page = Integer.parseInt(st.nextToken());
			upgradeItem(player, objectId, page);
		}
		else if (event.startsWith("mode_"))
		{
			// 切换模式
			StringTokenizer st = new StringTokenizer(event.substring(5), " ");
			int mode = Integer.parseInt(st.nextToken());  // 先获取模式数字
			int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;  // 再获取页码

			player.getVariables().set("zbzs_mode", mode);
			showUpgradePage(player, page);
		}

		return null;
	}

	private void upgradeItem(Player player, int objectId, int page)
	{
		Item item = player.getInventory().getItemByObjectId(objectId);
		if (item == null)
		{
			return;
		}

		// 获取当前模式
		int mode = player.getVariables().getInt("zbzs_mode", MODE_1);

		int currentUpgradeCount = item.getVariables().getInt(ItemVariables.zbzscsu, 0);

		// 检查是否超过最大值
		if (currentUpgradeCount >= Custom.zhuangbeizszdz)
		{
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "裝備轉生", "達到最大轉生次數" + Custom.zhuangbeizszdz));
			showUpgradePage(player, page);
			return;
		}

		// 计算实际可升级次数（不超过上限）
		int canUpgrade = Math.min(mode, Custom.zhuangbeizszdz - currentUpgradeCount);
		long requiredCount = REQUIRED_ITEM_COUNT * canUpgrade;

		// 检查材料
		if (player.getInventory().getInventoryItemCount(REQUIRED_ITEM_ID, 0) < requiredCount)
		{
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "裝備轉生", "道具不足: 需要" + requiredCount + "個"));
			showUpgradePage(player, page);
			return;
		}

		// 获取成功率（使用当前等级的成功率）
		int randomSuccessRate = getRandomSuccessRate(item);

		// 判断成功
		if (Rnd.get(100) <= randomSuccessRate)
		{
			player.destroyItemByItemId(null, REQUIRED_ITEM_ID, requiredCount, null, true);
			item.getVariables().set(ItemVariables.zbzscsu, currentUpgradeCount + canUpgrade);
			item.getVariables().storeMe();
			player.updateZscsCache();
			player.broadcastUserInfo();
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "裝備轉生", "成功轉生 +" + canUpgrade + " 次"));
		}
		else
		{
			player.destroyItemByItemId(null, REQUIRED_ITEM_ID, requiredCount, null, true);
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "裝備轉生", "轉生失敗，扣除材料"));
		}

		showUpgradePage(player, page);
	}

	private int getRandomSuccessRate(Item item)
	{
		int currentUpgradeCount = item.getVariables().getInt(ItemVariables.zbzscsu, 0);
		int randomSuccessRate = 10; // 默認成功率

		// 從陣列中查找對應的成功率
		for (int[] rateConfig : SUCCESS_RATES)
		{
			int minCount = rateConfig[0];
			int maxCount = rateConfig[1];
			int successRate = rateConfig[2];

			if ((currentUpgradeCount >= minCount) && (currentUpgradeCount <= maxCount))
			{
				randomSuccessRate = successRate;
				break;
			}
		}

		return randomSuccessRate;
	}

	private void showUpgradePage(Player player, int page)
	{
		List<Item> items = new ArrayList<>();

		for (int slot : ALLOWED_SLOTS)
		{
			Item item = player.getInventory().getPaperdollItem(slot);
			if (item != null)
			{
				items.add(item);
			}
		}

		int totalItems = items.size();
		int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);

		if (page < 1)
		{
			page = 1;
		}
		if (page > totalPages)
		{
			page = totalPages;
		}

		NpcHtmlMessage msg = new NpcHtmlMessage(0, 1);
		msg.setFile(player, "data/scripts/custom/zbzs/zbzsck.htm");
		msg.replace("%list%", generateItemTable(player, page, items));

		// 获取当前模式
		int currentMode = player.getVariables().getInt("zbzs_mode", MODE_1);

		// 生成模式按钮
		// 生成模式按钮
		StringBuilder modeButtons = new StringBuilder();
		modeButtons.append("<table width=\"280\"><tr>");

// +1 按钮
		if (currentMode == MODE_1)
		{
			modeButtons.append("<td width=\"93\" align=\"center\" height=\"25\"><font color=\"FFFF00\" size=\"3\">【+1】</font></td>");
		}
		else
		{
			modeButtons.append("<td width=\"93\" align=\"center\"><button value=\"+1\" action=\"bypass -h Quest zbzs mode_1 ").append(page).append("\" width=\"85\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td>");
		}

// +10 按钮
		if (currentMode == MODE_10)
		{
			modeButtons.append("<td width=\"94\" align=\"center\" height=\"25\"><font color=\"FFFF00\" size=\"3\">【+10】</font></td>");
		}
		else
		{
			modeButtons.append("<td width=\"94\" align=\"center\"><button value=\"+10\" action=\"bypass -h Quest zbzs mode_10 ").append(page).append("\" width=\"85\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td>");
		}

	// +100 按钮
		if (currentMode == MODE_50)
		{
			modeButtons.append("<td width=\"93\" align=\"center\" height=\"25\"><font color=\"FFFF00\" size=\"3\">【+50】</font></td>");
		}
		else
		{
			modeButtons.append("<td width=\"93\" align=\"center\"><button value=\"+50\" action=\"bypass -h Quest zbzs mode_50 ").append(page).append("\" width=\"85\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td>");
		}

		modeButtons.append("</tr></table>");

		msg.replace("%mode_buttons%", modeButtons.toString());

		StringBuilder prevButton = new StringBuilder();
		StringBuilder nextButton = new StringBuilder();

		if (page > 1)
		{
			prevButton.append("<button value=\"上一頁\" action=\"bypass -h Quest zbzs zhuans ").append(page - 1)
					.append("\" width=\"80\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		}

		if (page < totalPages)
		{
			nextButton.append("<button value=\"下一頁\" action=\"bypass -h Quest zbzs zhuans ").append(page + 1)
					.append("\" width=\"80\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		}

		msg.replace("%prev_page_button%", prevButton.toString());
		msg.replace("%next_page_button%", nextButton.toString());

		player.sendPacket(msg);
	}

	private String generateItemTable(Player player, int page, List<Item> items)
	{
		StringBuilder sb = new StringBuilder();

		int totalPages = (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE);
		int startIdx = (page - 1) * ITEMS_PER_PAGE;
		int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, items.size());

		// 防止訪問超出物品範圍
		if ((startIdx >= items.size()) || (items.size() == 0))
		{
			return "<tr bgcolor=\"222222\"><td colspan=\"3\" align=\"center\" height=\"30\"><font color=\"808080\">沒有已裝備的道具</font></td></tr>";
		}

		// 遍歷當前頁物品
		for (int i = startIdx; i < endIdx; i++)
		{
			Item item = items.get(i);
			int upgradeCount = item.getVariables().getInt(ItemVariables.zbzscsu, 0);
			int successRate = getRandomSuccessRate(item);

			// 根據轉生等級選擇顏色
			String countColor = "FFFF00"; // 黃色
			if (upgradeCount >= 80)
			{
				countColor = "FF00FF"; // 紫色
			}
			else if (upgradeCount >= 50)
			{
				countColor = "00FF00"; // 綠色
			}

			sb.append("<tr bgcolor=\"222222\" height=\"25\">");
			sb.append("<td align=\"left\" width=\"140\"><font color=\"00CCFF\">").append(item.getName()).append("</font></td>");
			sb.append("<td align=\"center\" width=\"60\"><font color=\"").append(countColor).append("\">+").append(upgradeCount).append("</font></td>");
			sb.append("<td align=\"center\" width=\"80\">");
			sb.append("<button value=\"轉生\" action=\"bypass -h Quest zbzs shuxin ").append(item.getObjectId()).append(" ").append(page)
					.append("\" width=\"60\" height=\"20\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			sb.append("</td>");
			sb.append("</tr>");

			// 添加成功率提示（小字體）
			sb.append("<tr bgcolor=\"222222\">");
			sb.append("<td colspan=\"3\" align=\"center\" height=\"15\"><font color=\"808080\" size=\"1\">成功率: ")
					.append(successRate).append("%</font></td>");
			sb.append("</tr>");
		}

		return sb.toString();
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/zbzs/zbzs.htm");
		player.sendPacket(html);
		return null;
	}

	public static void main(String[] args)
	{
		new zbzs();
		System.out.println("裝備轉生系統加載完畢！");
	}
}