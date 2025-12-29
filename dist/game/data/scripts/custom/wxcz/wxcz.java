/**
 * 商業化專用腳本 - 無限成長系統（全新優化版）
 */

package custom.wxcz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.l2jmobius.commons.util.Rnd;
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

	// 基礎材料ID和數量  目前不開放製作
	private static final int BASE_MATERIAL_ID = 57; // 金幣作為基礎材料
	private static final long BASE_MATERIAL_COUNT = 1000000; // 製作專用材料所需數量

	// 8種屬性對應的專用材料ID
	private static final Map<String, Integer> STAT_MATERIAL_MAP = new HashMap<>();
	private static final Map<String, String> STAT_NAME_MAP = new HashMap<>();

	static
	{
		// 屬性名稱對應
		STAT_NAME_MAP.put("patk", "物理攻擊");
		STAT_NAME_MAP.put("matk", "魔法攻擊");
		STAT_NAME_MAP.put("pdef", "物理防禦");
		STAT_NAME_MAP.put("mdef", "魔法防禦");
		STAT_NAME_MAP.put("maxhp", "最大HP值");
		STAT_NAME_MAP.put("maxmp", "最大MP值");
		STAT_NAME_MAP.put("mcatk", "魔法致命傷害");
		STAT_NAME_MAP.put("catk", "物理致命傷害");
		STAT_NAME_MAP.put("skillcatk", "物理技能致命傷害");

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
		else if (event.equals("craft_materials"))
		{
			return showCraftPage(npc, player);
		}
		else if (event.equals("upgrade_all"))
		{
			processUpgradeAll(player);
			return showUpgradePage(npc, player);
		}
		else if (event.startsWith("upgrade_"))
		{
			String stat = event.substring(8);
			processUpgrade(player, stat);
			return showUpgradePage(npc, player);
		}
		else if (event.equals("craft_random"))
		{
			processCraftMaterial(player);
			return showCraftPage(npc, player);
		}
		else if (event.equals("craft_all"))
		{
			processCraftAllMaterials(player);
			return showCraftPage(npc, player);
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

		// 替換材料數量
		html.replace("%patk_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("patk"), -1)));
		html.replace("%matk_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("matk"), -1)));
		html.replace("%pdef_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("pdef"), -1)));
		html.replace("%mdef_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("mdef"), -1)));
		html.replace("%maxhp_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("maxhp"), -1)));
		html.replace("%maxmp_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("maxmp"), -1)));
		html.replace("%mcatk_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("mcatk"), -1)));
		html.replace("%catk_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("catk"), -1)));
		html.replace("%skillcatk_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("skillcatk"), -1)));
		player.sendPacket(html);
		return null;
	}

	private String showCraftPage(Npc npc, Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/wxcz/wxczzz.htm");

		// 替換當前金幣數量
		long currentGold = player.getInventory().getInventoryItemCount(BASE_MATERIAL_ID, -1);
		html.replace("%current_gold%", String.valueOf(currentGold));
		html.replace("%required_gold%", String.valueOf(BASE_MATERIAL_COUNT));

		// 替換精華庫存
		html.replace("%patk_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("patk"), -1)));
		html.replace("%matk_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("matk"), -1)));
		html.replace("%pdef_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("pdef"), -1)));
		html.replace("%mdef_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("mdef"), -1)));
		html.replace("%maxhp_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("maxhp"), -1)));
		html.replace("%maxmp_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("maxmp"), -1)));
		html.replace("%mcatk_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("mcatk"), -1)));
		html.replace("%catk_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("catk"), -1)));
		html.replace("%skillcatk_count%", String.valueOf(player.getInventory().getInventoryItemCount(STAT_MATERIAL_MAP.get("skillcatk"), -1)));
		// 根據金幣數量決定是否顯示製作按鈕
		if (currentGold >= BASE_MATERIAL_COUNT)
		{
			html.replace("%craft_button%", "<button value=\"製作隨機精華\" action=\"bypass -h Quest wxcz craft_random\" width=\"180\" height=\"30\" back=\"L2UI_CT1.ListCTRL_DF_Title\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">");

			// 計算可以製作的總數量
			long maxCraftCount = currentGold / BASE_MATERIAL_COUNT;
			if (maxCraftCount > 1)
			{
				html.replace("%craft_all_button%", "<button value=\"一鍵全部製作(" + maxCraftCount + "個)\" action=\"bypass -h Quest wxcz craft_all\" width=\"180\" height=\"30\" back=\"L2UI_CT1.ListCTRL_DF_Title\" fore=\"L2UI_CT1.ListCTRL_DF_Title\">");
			}
			else
			{
				html.replace("%craft_all_button%", "");
			}
		}
		else
		{
			html.replace("%craft_button%", "<font color=\"FF0000\">金幣不足，無法製作</font>");
			html.replace("%craft_all_button%", "");
		}

		player.sendPacket(html);
		return null;
	}
	private void processCraftAllMaterials(Player player)
	{
		long goldCount = player.getInventory().getInventoryItemCount(BASE_MATERIAL_ID, -1);
		long maxCraftCount = goldCount / BASE_MATERIAL_COUNT;

		if (maxCraftCount < 1)
		{
			player.sendPacket(new ExShowScreenMessage("金幣不足：需要" + BASE_MATERIAL_COUNT + "個", 3000));
			return;
		}

		// 計算總消耗金幣
		long totalGoldCost = maxCraftCount * BASE_MATERIAL_COUNT;

		// 消耗所有可用的金幣
		player.destroyItemByItemId(null, BASE_MATERIAL_ID, totalGoldCost, null, true);

		// 統計製作結果
		Map<String, Integer> craftResults = new HashMap<>();
		String[] stats = STAT_MATERIAL_MAP.keySet().toArray(new String[0]);

		// 製作所有精華
		for (int i = 0; i < maxCraftCount; i++)
		{
			String randomStat = stats[Rnd.get(stats.length)];
			int materialId = STAT_MATERIAL_MAP.get(randomStat);

			// 給予隨機精華
			player.addItem(null, materialId, 1, null, false); // 不顯示每次獲得的消息

			// 統計結果
			craftResults.put(randomStat, craftResults.getOrDefault(randomStat, 0) + 1);
		}

		// 顯示製作結果
		StringBuilder resultMessage = new StringBuilder("一鍵製作完成！共製作了" + maxCraftCount + "個精華：");
		for (Map.Entry<String, Integer> entry : craftResults.entrySet())
		{
			String statName = STAT_NAME_MAP.get(entry.getKey());
			int count = entry.getValue();
			resultMessage.append(" ").append(statName).append("精華×").append(count);
		}

		player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "材料製作", resultMessage.toString()));
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

		if (materialCount < 1)
		{
			player.sendPacket(new ExShowScreenMessage("材料不足：需要1個" + STAT_NAME_MAP.get(stat) + "精華", 3000));
			return;
		}

		// 檢查是否已達上限
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

		if (enchant != null && enchant.getshuzhi() >= Custom.WUXIANXIANDING)
		{
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "無限成長", "目前達到成長最高值，請聯繫管理員開放更高的限制"));
			return;
		}

		// 消耗材料並升級
		player.destroyItemByItemId(null, materialId, 1, null, true);

		if (isStatNull && enchant != null)
		{
			WuxianData.getInstance().updateItemValue(enchant.getId(), enchant.getshuzhi() + 1, stat, enchant.getlevel());
		}
		else
		{
			WuxianData.getInstance().add(player.getObjectId(), 1, stat, 1);
		}

		player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "無限成長", "成功提升" + STAT_NAME_MAP.get(stat) + "！"));
	}

	private void processCraftMaterial(Player player)
	{
		long goldCount = player.getInventory().getInventoryItemCount(BASE_MATERIAL_ID, -1);

		if (goldCount < BASE_MATERIAL_COUNT)
		{
			player.sendPacket(new ExShowScreenMessage("金幣不足：需要" + BASE_MATERIAL_COUNT + "個", 3000));
			return;
		}

		// 消耗金幣
		player.destroyItemByItemId(null, BASE_MATERIAL_ID, BASE_MATERIAL_COUNT, null, true);

		// 隨機選擇一種精華
		String[] stats = STAT_MATERIAL_MAP.keySet().toArray(new String[0]);
		String randomStat = stats[Rnd.get(stats.length)];
		int materialId = STAT_MATERIAL_MAP.get(randomStat);

		// 給予隨機精華
		player.addItem(null, materialId, 1, null, true);

		player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "材料製作", "成功製作出" + STAT_NAME_MAP.get(randomStat) + "精華！"));
	}

	private void processUpgradeAll(Player player)
	{
		int totalUpgraded = 0;
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

			// 計算可升級次數
			long canUpgrade = materialCount;
			if (enchant != null)
			{
				long remaining = Custom.WUXIANXIANDING - enchant.getshuzhi();
				canUpgrade = Math.min(materialCount, remaining);
			}

			if (canUpgrade > 0)
			{
				player.destroyItemByItemId(null, materialId, canUpgrade, null, false);

				if (enchant != null)
				{
					WuxianData.getInstance().updateItemValue(enchant.getId(), enchant.getshuzhi() + (int)canUpgrade, stat, enchant.getlevel());
				}
				else
				{
					WuxianData.getInstance().add(player.getObjectId(), (int)canUpgrade, stat, 1);
				}

				upgradeResults.put(stat, (int)canUpgrade);
				totalUpgraded += canUpgrade;
			}
		}

		if (totalUpgraded > 0)
		{
			StringBuilder msg = new StringBuilder("一鍵加成完成！共提升" + totalUpgraded + "次：");
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