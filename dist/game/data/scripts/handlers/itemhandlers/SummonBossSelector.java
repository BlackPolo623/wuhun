package handlers.itemhandlers;

import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Broadcast;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * BOSS 選擇召喚道具處理器
 * 使用道具 72382 時會彈出選擇頁面，讓玩家選擇要召喚的 BOSS
 */
public class SummonBossSelector implements IItemHandler
{
	private static final Logger LOGGER = Logger.getLogger(SummonBossSelector.class.getName());

	// 召喚道具 ID
	private static final int SUMMON_ITEM_ID = 72382;

	// 召喚特效技能ID
	private static final int SUMMON_SKILL_ID = 1034;

	// 可召喚的 BOSS 列表（BOSS ID -> BOSS 名稱）
	// 使用 LinkedHashMap 保持順序
	private static final Map<Integer, String> AVAILABLE_BOSSES = new LinkedHashMap<>();

	static
	{
		// 在這裡配置可召喚的 BOSS
		// 格式：AVAILABLE_BOSSES.put(BOSS_ID, "BOSS顯示名稱");
		AVAILABLE_BOSSES.put(50001, "怪獸一號");
		AVAILABLE_BOSSES.put(50002, "怪獸二號");
		AVAILABLE_BOSSES.put(50003, "怪獸三號");
		AVAILABLE_BOSSES.put(50004, "怪獸四號");
		AVAILABLE_BOSSES.put(50005, "怪獸五號");
		AVAILABLE_BOSSES.put(50006, "怪獸六號");
		AVAILABLE_BOSSES.put(50007, "怪獸七號");
		AVAILABLE_BOSSES.put(50008, "怪獸八號");
		AVAILABLE_BOSSES.put(50009, "怪獸九號");
		AVAILABLE_BOSSES.put(50010, "怪獸十號");
		AVAILABLE_BOSSES.put(50011, "怪獸十一號");
		AVAILABLE_BOSSES.put(50012, "怪獸十二號");
		AVAILABLE_BOSSES.put(50013, "怪獸十三號");
		AVAILABLE_BOSSES.put(50014, "怪獸十四號");
		AVAILABLE_BOSSES.put(50015, "怪獸十五號");
		AVAILABLE_BOSSES.put(50016, "怪獸十六號");
		AVAILABLE_BOSSES.put(50017, "怪獸十七號");
		AVAILABLE_BOSSES.put(50018, "怪獸十八號");
		AVAILABLE_BOSSES.put(50019, "怪獸十九號");
		AVAILABLE_BOSSES.put(50020, "怪獸二十號");
		AVAILABLE_BOSSES.put(50021, "怪獸二十一號");
		AVAILABLE_BOSSES.put(50022, "怪獸二十二號");
		AVAILABLE_BOSSES.put(50023, "怪獸二十三號");
		AVAILABLE_BOSSES.put(50024, "怪獸二十四號");
		AVAILABLE_BOSSES.put(50025, "怪獸二十五號");
		AVAILABLE_BOSSES.put(50026, "怪獸二十六號");
		AVAILABLE_BOSSES.put(50027, "怪獸二十七號");
		AVAILABLE_BOSSES.put(50028, "怪獸二十八號");
		AVAILABLE_BOSSES.put(50029, "怪獸二十九號");
		AVAILABLE_BOSSES.put(50030, "怪獸三十號");
		AVAILABLE_BOSSES.put(50031, "怪獸三十一號");
		AVAILABLE_BOSSES.put(50032, "怪獸三十二號");
		AVAILABLE_BOSSES.put(50033, "怪獸三十三號");
		AVAILABLE_BOSSES.put(50034, "怪獸三十四號");
		AVAILABLE_BOSSES.put(50035, "怪獸三十五號");
	}

	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!(playable instanceof Player))
		{
			return false;
		}

		final Player player = (Player) playable;

		// 檢查是否為指定道具
		if (item.getId() != SUMMON_ITEM_ID)
		{
			return false;
		}

		// 檢查玩家是否在和平區域
		if (player.isInsideZone(org.l2jmobius.gameserver.model.zone.ZoneId.PEACE))
		{
			player.sendMessage("無法在和平區域召喚 BOSS。");
			return false;
		}

		// 顯示 BOSS 選擇頁面
		showBossSelectionPage(player);
		return true;
	}

	/**
	 * 顯示 BOSS 選擇頁面
	 */
	private void showBossSelectionPage(Player player)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><title>選擇召喚 BOSS</title><center>");
		sb.append("<br>");
		sb.append("<table width=\"290\" bgcolor=\"222222\">");
		sb.append("<tr><td height=\"5\"></td></tr>");
		sb.append("<tr><td align=\"center\"><font color=\"AAAAAA\" size=\"1\">請選擇要召喚的首領</font></td></tr>");
		sb.append("<tr><td height=\"5\"></td></tr>");
		sb.append("</table><br>");

		// BOSS 選擇按鈕，每行 2 個
		sb.append("<table width=\"290\">");
		int col = 0;
		for (Map.Entry<Integer, String> entry : AVAILABLE_BOSSES.entrySet())
		{
			if (col == 0) sb.append("<tr>");
			sb.append("<td align=\"center\">");
			sb.append("<button value=\"").append(entry.getValue()).append("\" action=\"bypass -h summon_boss ").append(entry.getKey());
			sb.append("\" width=\"130\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			sb.append("</td>");
			col++;
			if (col == 2)
			{
				sb.append("</tr>");
				col = 0;
			}
		}
		// 如果最後一行不滿，補齊
		if (col != 0)
		{
			sb.append("</tr>");
		}
		sb.append("</table><br>");
		sb.append("<button value=\"取消\" action=\"bypass -h close_window\" width=\"100\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</center></body></html>");

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	/**
	 * 處理 BOSS 召喚（由 bypass 觸發）
	 * 這個方法需要在 BypassHandler 中註冊
	 */
	public static boolean handleBossSummon(Player player, String command)
	{
		// 檢查命令格式
		if (!command.startsWith("summon_boss"))
		{
			return false;
		}

		try
		{
			// 解析 BOSS ID（格式: "summon_boss 50001"）
			int bossId = Integer.parseInt(command.substring(12).trim());

			// 檢查 BOSS 是否在可召喚列表中
			if (!AVAILABLE_BOSSES.containsKey(bossId))
			{
				player.sendMessage("無效的 BOSS 選擇。");
				return true;
			}

			// 檢查玩家是否擁有召喚道具
			if (player.getInventory().getInventoryItemCount(SUMMON_ITEM_ID, -1) < 1)
			{
				player.sendMessage("你沒有召喚道具。");
				return true;
			}

			// 檢查玩家是否在和平區域
			if (player.isInsideZone(org.l2jmobius.gameserver.model.zone.ZoneId.PEACE))
			{
				player.sendMessage("無法在和平區域召喚 BOSS。");
				return true;
			}

			// 檢查怪物模板是否存在
			final NpcTemplate template = NpcData.getInstance().getTemplate(bossId);
			if (template == null)
			{
				player.sendMessage("召喚失敗：BOSS 資料不存在！");
				LOGGER.warning("SummonBossSelector: Boss template not found for ID: " + bossId);
				return true;
			}

			// 計算召喚位置（玩家前方 100 距離）
			final int x = player.getX() + 100;
			final int y = player.getY() + 100;
			final int z = player.getZ();

			// 創建 Spawn
			final Spawn spawn = new Spawn(template);
			spawn.setXYZ(x, y, z);
			spawn.setAmount(1);
			spawn.setHeading(player.getHeading());
			spawn.setRespawnDelay(0);

			// 如果在副本中，設置副本ID
			if (player.isInInstance())
			{
				spawn.setInstanceId(player.getInstanceId());
			}

			// 添加到 SpawnTable（非永久）
			SpawnTable.getInstance().addSpawn(spawn);

			// 初始化 spawn
			spawn.init();

			// 停止重生機制（一次性召喚）
			spawn.stopRespawn();

			// 廣播怪物信息
			if (spawn.getLastSpawn() != null)
			{
				spawn.getLastSpawn().broadcastInfo();
			}

			// 播放召喚特效
			Broadcast.toSelfAndKnownPlayers(player, new MagicSkillUse(player, player, SUMMON_SKILL_ID, 1, 100, 0));

			// 發送成功訊息
			String bossName = AVAILABLE_BOSSES.get(bossId);
			player.sendMessage("成功召喚了 " + bossName + "！");

			// 消耗道具
			player.destroyItemByItemId(ItemProcessType.NONE, SUMMON_ITEM_ID, 1, player, true);

			return true;
		}
		catch (Exception e)
		{
			LOGGER.warning("SummonBossSelector: Error summoning boss - " + e.getMessage());
			e.printStackTrace();
			player.sendMessage("召喚 BOSS 時發生錯誤，請回報管理員。");
			return true;
		}
	}
}
