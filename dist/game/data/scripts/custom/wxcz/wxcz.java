/**
 * 商業化專用腳本 - 無限成長系統（全新優化版）
 */

package custom.wxcz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.config.custom.Custom;
import org.l2jmobius.gameserver.data.holders.WuxianDataHolder;
import org.l2jmobius.gameserver.data.xml.WuxianData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class wxcz extends Script
{
	private static final int NPC_ID = 900001;

	// 8種屬性對應的專用材料ID
	private static final Map<String, Integer> STAT_MATERIAL_MAP = new HashMap<>();
	private static final Map<String, String> STAT_NAME_MAP = new HashMap<>();

	// 階段消耗配置 [階段門檻, 消耗數量]
	// 例如: [20000, 3] 表示超過20000時，每次消耗3個道具
	private static final int[][] STAGE_COST_CONFIG = {
			{0, 1},       // 0~19999: 消耗1個
			{20000, 5},   // 20000~39999: 消耗3個
			{40000, 10},   // 40000以上: 消耗5個
			{45000, 20},
			{50000, 30},
			{55000, 40},
	};

	static
	{
		// 屬性名稱對應
		STAT_NAME_MAP.put("patk", "物理攻擊");
		STAT_NAME_MAP.put("matk", "魔法攻擊");
		STAT_NAME_MAP.put("pdef", "物理防禦");
		STAT_NAME_MAP.put("mdef", "魔法防禦");
		STAT_NAME_MAP.put("maxhp", "最大HP值");
		STAT_NAME_MAP.put("maxmp", "最大MP值");
		STAT_NAME_MAP.put("mcatk", "魔法技能爆傷");
		STAT_NAME_MAP.put("catk", "物理攻擊爆傷");
		STAT_NAME_MAP.put("skillcatk", "物理技能爆傷");

		// 專用材料ID對應
		STAT_MATERIAL_MAP.put("patk", 108000);    // 力量精華
		STAT_MATERIAL_MAP.put("matk", 108001);    // 智慧精華
		STAT_MATERIAL_MAP.put("pdef", 108002);    // 防護精華
		STAT_MATERIAL_MAP.put("mdef", 108003);    // 抗魔精華
		STAT_MATERIAL_MAP.put("maxhp", 108004);   // 生命精華
		STAT_MATERIAL_MAP.put("maxmp", 108005);   // 法力精華
		STAT_MATERIAL_MAP.put("mcatk", 108006);   // 魔爆精華
		STAT_MATERIAL_MAP.put("catk", 108007);    // 破甲精華
		STAT_MATERIAL_MAP.put("skillcatk", 108008);    // 物理技能破甲精華
	}

	/**
	 * 根據當前數值取得消耗數量
	 * @param currentValue 當前數值
	 * @return 消耗數量
	 */
	private int getCostByValue(long currentValue)
	{
		int cost = 1;
		for (int i = STAGE_COST_CONFIG.length - 1; i >= 0; i--)
		{
			if (currentValue >= STAGE_COST_CONFIG[i][0])
			{
				cost = STAGE_COST_CONFIG[i][1];
				break;
			}
		}
		return cost;
	}

	private wxcz()
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
		else if (event.equals("view_stats"))
		{
			return showStatsPage(npc, player);
		}
		else if (event.equals("upgrade_stats"))
		{
			return showUpgradePage(npc, player);
		}
		else if (event.equals("upgrade_all"))
		{
			processUpgradeAll(player);
			return showUpgradePage(npc, player);
		}
		else if (event.startsWith("upgradeall_"))
		{
			String stat = event.substring(11);
			processUpgradeAllStat(player, stat);
			return showUpgradePage(npc, player);
		}
		else if (event.startsWith("upgrade_"))
		{
			String stat = event.substring(8);
			processUpgrade(player, stat);
			return showUpgradePage(npc, player);
		}

		return null;
	}

	private String showStatsPage(Npc npc, Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/wxcz/wxczck.htm");

		// 替換屬性數值
		html.replace("%patk%", format(player.getwxsz("patk") / 100.0));
		html.replace("%matk%", format(player.getwxsz("matk") / 100.0));
		html.replace("%pdef%", format(player.getwxsz("pdef") / 100.0));
		html.replace("%mdef%", format(player.getwxsz("mdef") / 100.0));
		html.replace("%maxhp%", format(player.getwxsz("maxhp") / 100.0));
		html.replace("%maxmp%", format(player.getwxsz("maxmp") / 100.0));
		html.replace("%mcatk%", format(player.getwxsz("mcatk") / 100.0));
		html.replace("%catk%", format(player.getwxsz("catk") / 100.0));
		html.replace("%skillcatk%", format(player.getwxsz("skillcatk") / 100.0));

		player.sendPacket(html);
		return null;
	}

	private String showUpgradePage(Npc npc, Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/wxcz/wxczjc.htm");

		for (String stat : new String[]{"patk", "matk", "pdef", "mdef", "maxhp", "maxmp", "mcatk", "catk", "skillcatk"})
		{
			int materialId = STAT_MATERIAL_MAP.get(stat);
			long materialCount = player.getInventory().getInventoryItemCount(materialId, -1);
			long currentValue = getPlayerStatValue(player, stat);
			int cost = getCostByValue(currentValue);
			boolean capped = currentValue >= Custom.WUXIANXIANDING;
			boolean sufficient = !capped && (materialCount >= cost);

			html.replace("%" + stat + "_current%", format(currentValue / 100.0));
			html.replace("%" + stat + "_count%", String.valueOf(materialCount));
			html.replace("%" + stat + "_cost%", String.valueOf(cost));
			html.replace("%" + stat + "_count_color%", (capped || sufficient) ? "FFFF00" : "FF4444");

			if (capped)
			{
				html.replace("%" + stat + "_btn%", "<font color=\"888888\" size=\"1\">已達上限</font>");
				html.replace("%" + stat + "_allbtn%", "");
			}
			else
			{
				html.replace("%" + stat + "_btn%", "<button value=\"" + STAT_NAME_MAP.get(stat) + "\" action=\"bypass -h Quest wxcz upgrade_" + stat + "\" width=83 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				html.replace("%" + stat + "_allbtn%", "<button value=\"全加\" action=\"bypass -h Quest wxcz upgradeall_" + stat + "\" width=36 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			}
		}

		player.sendPacket(html);
		return null;
	}

	/**
	 * 取得玩家指定屬性的當前數值
	 */
	private long getPlayerStatValue(Player player, String stat)
	{
		List<WuxianDataHolder> holders = WuxianData.getInstance().getByPlayerId(player.getObjectId());
		for (WuxianDataHolder holder : holders)
		{
			if (holder.getstat().equalsIgnoreCase(stat))
			{
				return holder.getshuzhi();
			}
		}
		return 0;
	}

	private void processUpgrade(Player player, String stat)
	{
		if (!STAT_MATERIAL_MAP.containsKey(stat))
		{
			player.sendMessage("無效的屬性類型！");
			return;
		}

		int materialId = STAT_MATERIAL_MAP.get(stat);
		long materialCount = player.getInventory().getInventoryItemCount(materialId, -1);

		// 檢查是否已達上限並取得當前數值
		boolean isStatNull = false;
		WuxianDataHolder enchant = null;
		List<WuxianDataHolder> holders = WuxianData.getInstance().getByPlayerId(player.getObjectId());

		for (WuxianDataHolder holder : holders)
		{
			if (holder.getstat().equalsIgnoreCase(stat))
			{
				enchant = holder;
				isStatNull = true;
				break;
			}
		}

		// 取得當前數值
		long currentValue = (enchant != null) ? enchant.getshuzhi() : 0;

		// 根據當前數值計算消耗
		int requiredCost = getCostByValue(currentValue);

		if (materialCount < requiredCost)
		{
			player.sendPacket(new ExShowScreenMessage("材料不足：需要" + requiredCost + "個" + STAT_NAME_MAP.get(stat) + "精華", 3000));
			return;
		}

		if (enchant != null && enchant.getshuzhi() >= Custom.WUXIANXIANDING)
		{
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "無限成長", "目前達到成長最高值，請聯繫管理員開放更高的限制"));
			return;
		}

		// 消耗材料並升級
		player.destroyItemByItemId(null, materialId, requiredCost, null, true);

		if (isStatNull && enchant != null)
		{
			WuxianData.getInstance().updateItemValue(enchant.getId(), enchant.getshuzhi() + 1, stat, enchant.getlevel());
		}
		else
		{
			WuxianData.getInstance().add(player.getObjectId(), 1, stat, 1);
		}

		player.updateWuxianCache();
		player.broadcastUserInfo();

		player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "無限成長", "成功提升" + STAT_NAME_MAP.get(stat) + "！(消耗" + requiredCost + "個精華)"));
	}

	private void processUpgradeAllStat(Player player, String stat)
	{
		if (!STAT_MATERIAL_MAP.containsKey(stat))
		{
			player.sendMessage("無效的屬性類型！");
			return;
		}

		int materialId = STAT_MATERIAL_MAP.get(stat);
		long materialCount = player.getInventory().getInventoryItemCount(materialId, -1);

		if (materialCount < 1)
		{
			player.sendPacket(new ExShowScreenMessage("沒有" + STAT_NAME_MAP.get(stat) + "精華", 3000));
			return;
		}

		WuxianDataHolder enchant = null;
		for (WuxianDataHolder holder : WuxianData.getInstance().getByPlayerId(player.getObjectId()))
		{
			if (holder.getstat().equalsIgnoreCase(stat))
			{
				enchant = holder;
				break;
			}
		}

		long currentValue = (enchant != null) ? enchant.getshuzhi() : 0;
		long remaining = Custom.WUXIANXIANDING - currentValue;

		if (remaining <= 0)
		{
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "無限成長", STAT_NAME_MAP.get(stat) + "已達成長最高值"));
			return;
		}

		int upgradeCount = 0;
		long materialsUsed = 0;
		long tempValue = currentValue;

		while (materialsUsed < materialCount && upgradeCount < remaining)
		{
			int cost = getCostByValue(tempValue);
			if (materialsUsed + cost > materialCount)
			{
				break;
			}
			materialsUsed += cost;
			upgradeCount++;
			tempValue++;
		}

		if (upgradeCount > 0)
		{
			player.destroyItemByItemId(null, materialId, materialsUsed, null, false);

			if (enchant != null)
			{
				WuxianData.getInstance().updateItemValue(enchant.getId(), enchant.getshuzhi() + upgradeCount, stat, enchant.getlevel());
			}
			else
			{
				WuxianData.getInstance().add(player.getObjectId(), upgradeCount, stat, 1);
			}

			player.updateWuxianCache();
			player.broadcastUserInfo();
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "無限成長", STAT_NAME_MAP.get(stat) + "提升" + upgradeCount + "次(消耗" + materialsUsed + "個精華)"));
		}
	}

	private void processUpgradeAll(Player player)
	{
		int totalUpgraded = 0;
		long totalMaterialUsed = 0;
		Map<String, Integer> upgradeResults = new HashMap<>();

		for (String stat : STAT_MATERIAL_MAP.keySet())
		{
			int materialId = STAT_MATERIAL_MAP.get(stat);
			long materialCount = player.getInventory().getInventoryItemCount(materialId, -1);

			if (materialCount < 1) continue;

			// 檢查上限
			WuxianDataHolder enchant = null;
			for (WuxianDataHolder holder : WuxianData.getInstance().getByPlayerId(player.getObjectId()))
			{
				if (holder.getstat().equalsIgnoreCase(stat))
				{
					enchant = holder;
					break;
				}
			}

			long currentValue = (enchant != null) ? enchant.getshuzhi() : 0;
			long remaining = Custom.WUXIANXIANDING - currentValue;

			if (remaining <= 0) continue;

			// 計算可升級次數 (考慮階段消耗)
			int upgradeCount = 0;
			long materialsUsed = 0;
			long tempValue = currentValue;

			while (materialsUsed < materialCount && upgradeCount < remaining)
			{
				int cost = getCostByValue(tempValue);
				if (materialsUsed + cost > materialCount)
				{
					break;
				}
				materialsUsed += cost;
				upgradeCount++;
				tempValue++;
			}

			if (upgradeCount > 0)
			{
				player.destroyItemByItemId(null, materialId, materialsUsed, null, false);

				if (enchant != null)
				{
					WuxianData.getInstance().updateItemValue(enchant.getId(), enchant.getshuzhi() + upgradeCount, stat, enchant.getlevel());
				}
				else
				{
					WuxianData.getInstance().add(player.getObjectId(), upgradeCount, stat, 1);
				}

				upgradeResults.put(stat, upgradeCount);
				totalUpgraded += upgradeCount;
				totalMaterialUsed += materialsUsed;
			}
		}

		if (totalUpgraded > 0)
		{
			player.updateWuxianCache();
			player.broadcastUserInfo();
			StringBuilder msg = new StringBuilder("一鍵加成完成！共提升" + totalUpgraded + "次(消耗" + totalMaterialUsed + "個精華)：");
			for (Map.Entry<String, Integer> entry : upgradeResults.entrySet())
			{
				msg.append(" ").append(STAT_NAME_MAP.get(entry.getKey())).append("×").append(entry.getValue());
			}
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "無限成長", msg.toString()));
		}
		else
		{
			player.sendPacket(new ExShowScreenMessage("沒有可用的精華材料", 3000));
		}
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/wxcz/wxcz.htm");
		player.sendPacket(html);
		return null;
	}

	// 保留兩位小數
	private String format(double value)
	{
		return String.format("%.2f", value);
	}

	public static void main(String[] args)
	{
		new wxcz();
		System.out.println("【系統】無限成長系統加載完畢！");
	}
}