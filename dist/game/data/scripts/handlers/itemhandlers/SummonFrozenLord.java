package handlers.itemhandlers;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.util.Broadcast;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 冰凍君主召喚道具處理器
 * 使用單一道具,根據機率隨機召喚不同等級的冰凍君主BOSS
 *
 * @author 黑普羅
 */
public class SummonFrozenLord implements IItemHandler
{
	private static final Logger LOGGER = Logger.getLogger(SummonFrozenLord.class.getName());

	// ==================== 配置區域 ====================

	// 召喚道具ID
	private static final int SUMMON_ITEM_ID = 97786; // 改成你的道具ID

	// 召喚特效技能ID
	private static final int SUMMON_SKILL_ID = 1034;

	// 召喚距離（玩家前方多少距離）
	private static final int SUMMON_DISTANCE = 100;

	// 怪物召喚配置
	private static final List<MonsterConfig> MONSTER_POOL = new ArrayList<>();

	static
	{
		// 配置格式: new MonsterConfig(怪物ID, 權重, 怪物描述)
		// 權重越高,召喚機率越大
		// 實際機率 = 該怪物權重 / 所有怪物權重總和

		MONSTER_POOL.add(new MonsterConfig(29136, 96, "冰凍君主·初階"));  // 50/100 = 50%
		MONSTER_POOL.add(new MonsterConfig(29137, 96, "冰凍君主·中階"));  // 30/100 = 30%
		MONSTER_POOL.add(new MonsterConfig(29138, 4, "冰凍君主·高階"));  // 15/100 = 15%
		MONSTER_POOL.add(new MonsterConfig(29139, 4,  "冰凍君主·極難"));  // 4/100 = 4%
	}

	// ==================== 怪物配置類 ====================

	/**
	 * 怪物召喚配置
	 */
	private static class MonsterConfig
	{
		private final int monsterId;      // 怪物ID
		private final int weight;         // 召喚權重
		private final String description; // 怪物描述（用於訊息提示）

		public MonsterConfig(int monsterId, int weight, String description)
		{
			this.monsterId = monsterId;
			this.weight = weight;
			this.description = description;
		}
	}

	// ==================== 主要邏輯 ====================

	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!(playable instanceof Player))
		{
			return false;
		}

		final Player player = (Player) playable;

		// 檢查道具ID
		if (item.getId() != SUMMON_ITEM_ID)
		{
			return false;
		}

		// 檢查是否在和平區域
		if (player.isInsideZone(org.l2jmobius.gameserver.model.zone.ZoneId.PEACE))
		{
			player.sendMessage("無法在和平區域召喚冰凍君主。");
			return false;
		}

		// 根據機率選擇怪物
		final MonsterConfig selectedMonster = selectRandomMonster();
		if (selectedMonster == null)
		{
			player.sendMessage("召喚失敗：系統配置錯誤！");
			LOGGER.warning("SummonFrozenLord: No monster configured in pool!");
			return false;
		}

		// 檢查怪物模板是否存在
		final NpcTemplate template = NpcData.getInstance().getTemplate(selectedMonster.monsterId);
		if (template == null)
		{
			player.sendMessage("召喚失敗：怪物資料不存在！");
			LOGGER.warning("SummonFrozenLord: Monster template not found for ID: " + selectedMonster.monsterId);
			return false;
		}

		try
		{
			// 計算召喚位置（玩家前方）
			final double radian = Math.toRadians(player.getHeading() / 182.044444444);
			final int x = (int) (player.getX() + (SUMMON_DISTANCE * Math.cos(radian)));
			final int y = (int) (player.getY() + (SUMMON_DISTANCE * Math.sin(radian)));
			final int z = player.getZ();

			// 創建 Spawn
			final Spawn spawn = new Spawn(template);
			spawn.setXYZ(x, y, z);
			spawn.setAmount(1);
			spawn.setHeading(player.getHeading());
			spawn.setRespawnDelay(0);

			// 如果在副本中,設置副本ID
			if (player.isInInstance())
			{
				spawn.setInstanceId(player.getInstanceId());
			}

			// 添加到 SpawnTable（非永久）
			SpawnTable.getInstance().addSpawn(spawn);

			// 初始化並停止重生
			spawn.init();
			spawn.stopRespawn();

			// 廣播怪物信息
			if (spawn.getLastSpawn() != null)
			{
				spawn.getLastSpawn().broadcastInfo();
			}

			// 播放召喚特效
			Broadcast.toSelfAndKnownPlayers(player, new MagicSkillUse(player, player, SUMMON_SKILL_ID, 1, 100, 0));

			// 發送成功訊息
			player.sendMessage("========================================");
			player.sendMessage("成功召喚了【" + selectedMonster.description + "】！");
			player.sendMessage("怪物名稱：" + template.getName());
			player.sendMessage("========================================");

			// 全服公告（可選）
			if (selectedMonster.weight <= 5) // 稀有怪物才公告
			{
				Broadcast.toAllOnlinePlayers("【系統公告】玩家 " + player.getName() +
						" 召喚出了稀有的【" + selectedMonster.description + "】！");
			}

			// 消耗道具
			player.destroyItem(ItemProcessType.NONE, item, 1, null, true);

			return true;
		}
		catch (Exception e)
		{
			LOGGER.warning("SummonFrozenLord: Error summoning monster - " + e.getMessage());
			e.printStackTrace();
			player.sendMessage("召喚冰凍君主時發生錯誤,請回報管理員。");
			return false;
		}
	}

	/**
	 * 根據權重隨機選擇怪物
	 *
	 * @return 選中的怪物配置
	 */
	private MonsterConfig selectRandomMonster()
	{
		if (MONSTER_POOL.isEmpty())
		{
			return null;
		}

		// 計算總權重
		int totalWeight = 0;
		for (MonsterConfig config : MONSTER_POOL)
		{
			totalWeight += config.weight;
		}

		// 生成隨機數
		int random = Rnd.get(totalWeight);

		// 根據權重範圍選擇怪物
		int currentWeight = 0;
		for (MonsterConfig config : MONSTER_POOL)
		{
			currentWeight += config.weight;
			if (random < currentWeight)
			{
				return config;
			}
		}

		// 理論上不會到這裡,但作為保險返回第一個
		return MONSTER_POOL.get(0);
	}
}