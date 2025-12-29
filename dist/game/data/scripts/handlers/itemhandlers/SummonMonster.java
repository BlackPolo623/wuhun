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
import org.l2jmobius.gameserver.util.Broadcast;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 召喚怪物道具處理器
 * 使用道具時會根據道具ID召喚對應的怪物（一次性，不重生）
 */
public class SummonMonster implements IItemHandler
{
	private static final Logger LOGGER = Logger.getLogger(SummonMonster.class.getName());

	// 召喚特效技能ID
	private static final int SUMMON_SKILL_ID = 1034;

	// 道具ID -> 怪物ID 的對應表
	private static final Map<Integer, Integer> ITEM_TO_MONSTER = new HashMap<>();

	static
	{
		ITEM_TO_MONSTER.put(107001, 60001);
		ITEM_TO_MONSTER.put(107002, 60002);
		ITEM_TO_MONSTER.put(107003, 60003);
		ITEM_TO_MONSTER.put(107004, 60004);
		ITEM_TO_MONSTER.put(107005, 60005);
		ITEM_TO_MONSTER.put(107006, 60006);
	}

	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!(playable instanceof Player))
		{
			return false;
		}

		final Player player = (Player) playable;
		final int itemId = item.getId();

		// 檢查道具ID是否在對應表中
		if (!ITEM_TO_MONSTER.containsKey(itemId))
		{
			player.sendMessage("此道具無法召喚怪物。");
			return false;
		}

		// 獲取對應的怪物ID
		final int monsterId = ITEM_TO_MONSTER.get(itemId);

		// 檢查怪物模板是否存在
		final NpcTemplate template = NpcData.getInstance().getTemplate(monsterId);
		if (template == null)
		{
			player.sendMessage("召喚失敗：怪物資料不存在！");
			LOGGER.warning("SummonMonster: Monster template not found for ID: " + monsterId);
			return false;
		}

		// 檢查玩家是否在和平區域
		if (player.isInsideZone(org.l2jmobius.gameserver.model.zone.ZoneId.PEACE))
		{
			player.sendMessage("無法在和平區域召喚怪物。");
			return false;
		}

		try
		{
			// 計算召喚位置（玩家前方 50 距離）
			final int x = player.getX() + 50;
			final int y = player.getY() + 50;
			final int z = player.getZ();

			// 參考 admin 指令的方式創建 Spawn
			final Spawn spawn = new Spawn(template);
			spawn.setXYZ(x, y, z);
			spawn.setAmount(1); // 召喚 1 隻
			spawn.setHeading(player.getHeading());
			spawn.setRespawnDelay(0); // 重生延遲設為 0

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
			player.sendMessage("成功召喚了 " + template.getName() + "！");

			// 消耗道具
			player.destroyItem(ItemProcessType.NONE, item, 1, null, true);

			return true;
		}
		catch (Exception e)
		{
			LOGGER.warning("SummonMonster: Error summoning monster ID " + monsterId + " - " + e.getMessage());
			e.printStackTrace();
			player.sendMessage("召喚怪物時發生錯誤，請回報管理員。");
			return false;
		}
	}
}