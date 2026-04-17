package instances.FireDragonTemple;

import java.util.LinkedHashMap;
import java.util.Map;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * 火龍副本 (Fire Dragon Temple)
 * 簡化版：進入 → 擊殺火龍 → 寶箱出現 → 領獎離開
 *
 * 副本 ID：231
 * Boss NPC：25967（火龍）
 * 寶箱 NPC：920001
 * 傳送 NPC：另見 FireDragonTeleport.java
 */
public class FireDragonTemple extends InstanceScript
{
	// ========================================
	// Instance ID
	// ========================================
	public static final int FIRE_DRAGON_TEMPLE_INSTANCE_ID = 231;

	// ========================================
	// NPC IDs
	// ========================================
	private static final int FIRE_DRAGON = 29191;
	private static final int REWARD_CHEST_NPC_ID = 920001;

	// ========================================
	// Instance Status
	// ========================================
	private static final int BOSS_ALIVE = 1;
	public static final int FINISH_INSTANCE = 2;

	// ========================================
	// Player Variable Keys
	// ========================================
	/** 玩家是否已通關此副本（用於掃蕩條件判斷） */
	public static final String PLAYER_CLEARED_VAR = "FIRE_DRAGON_TEMPLE_CLEARED";
	/** 玩家是否已從寶箱領取獎勵（每次副本重置） */
	private static final String PLAYER_REWARDED_VAR = "FIRE_DRAGON_TEMPLE_REWARDED";
	/** 保底計數器 */
	private static final String PLAYER_PITY_COUNT_VAR = "FIRE_DRAGON_TEMPLE_PITY_COUNT";

	// ========================================
	// Reward Configuration
	// ========================================
	public static final int REWARD_ITEMS_COUNT = 8;
	public static final int SWEEP_REWARD_ITEMS_COUNT = 6;

	/**
	 * 獎勵表：{ itemId, count, weight }
	 * 與 ValakasTemple 完全相同，如需修改請調整此處。
	 */
	public static final int[][] REWARD_ITEMS =
	{
		{130000, 1, 50},
		{130000, 3, 50},
		{130000, 5, 50},
		{130000, 10, 50},
		{130000, 15, 10},
		{130000, 20, 5},
		{57, 100000000, 30},
		{57, 500000000, 20},
		{57, 1000000000, 10},
		{91663, 10000, 10},
		{91663, 20000, 5},
		{91663, 40000, 3},
		{91663, 80000, 1},
		{108000, 100, 15},
		{108001, 100, 15},
		{108002, 100, 15},
		{108003, 100, 15},
		{108004, 100, 15},
		{108005, 100, 15},
		{108006, 100, 15},
		{108007, 100, 15},
		{108008, 100, 15},
		{105503, 1, 8},
		{105503, 2, 7},
		{105503, 3, 6},
		{130001, 1, 3},
		{130002, 1, 3},
		{130009, 1, 2},
		{130010, 1, 2},
		{130011, 1, 2},
		{130012, 1, 2},
		{130013, 1, 2},
		{130014, 1, 2},
	};

	// ========================================
	// Pity System
	// ========================================
	public static final int PITY_THRESHOLD = 5;

	public static final int[][] PITY_REWARD_ITEMS =
	{
		{108000, 200, 1},
		{108001, 200, 1},
		{108002, 200, 1},
		{108003, 200, 1},
		{108004, 200, 1},
		{108005, 200, 1},
		{108006, 200, 1},
		{108007, 200, 1},
		{108008, 200, 1},
		{130009, 1, 1},
		{130010, 1, 1},
		{130011, 1, 1},
		{130012, 1, 1},
		{130013, 1, 1},
		{130014, 1, 1},
	};

	// Pre-calculated total chances
	private static final int TOTAL_REWARD_CHANCE;
	private static final int TOTAL_PITY_CHANCE;
	static
	{
		int total = 0;
		for (int[] reward : REWARD_ITEMS)
		{
			total += reward[2];
		}
		TOTAL_REWARD_CHANCE = total;

		total = 0;
		for (int[] reward : PITY_REWARD_ITEMS)
		{
			total += reward[2];
		}
		TOTAL_PITY_CHANCE = total;
	}

	// ========================================
	// Constructor
	// ========================================

	private FireDragonTemple()
	{
		super(FIRE_DRAGON_TEMPLE_INSTANCE_ID);
		addInstanceCreatedId(FIRE_DRAGON_TEMPLE_INSTANCE_ID);
		addKillId(FIRE_DRAGON);
		addFirstTalkId(REWARD_CHEST_NPC_ID);
		addTalkId(REWARD_CHEST_NPC_ID);
	}

	// ========================================
	// Instance Lifecycle
	// ========================================

	@Override
	public void onInstanceCreated(Instance world, Player player)
	{
		// 進入副本時清除已領取旗標，確保每場都能正常領取
		player.getVariables().remove(PLAYER_REWARDED_VAR);
		if (player.isInParty())
		{
			for (Player member : player.getParty().getMembers())
			{
				member.getVariables().remove(PLAYER_REWARDED_VAR);
			}
		}

		world.setStatus(BOSS_ALIVE);
		super.onInstanceCreated(world, player);
	}

	// ========================================
	// Kill Handler
	// ========================================

	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = (killer == null) ? null : killer.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != FIRE_DRAGON_TEMPLE_INSTANCE_ID))
		{
			return;
		}

		if ((npc.getId() == FIRE_DRAGON) && (world.getStatus() == BOSS_ALIVE))
		{
			world.setStatus(FINISH_INSTANCE);
			onBossKilled(world);
		}
	}

	private static void onBossKilled(Instance world)
	{
		// 生成獎勵寶箱
		world.spawnGroup("reward_chest");

		// 標記全體玩家已通關
		world.getPlayers().forEach(player -> player.getVariables().set(PLAYER_CLEARED_VAR, true));

		// 通知畫面訊息
		world.getPlayers().forEach(player -> player.sendPacket(new ExShowScreenMessage("火龍已被擊敗！請領取獎勵，副本將在3分鐘後關閉。", 10000)));

		// 3 分鐘後強制傳送所有玩家離開並銷毀副本
		ThreadPool.schedule(() ->
		{
			world.getPlayers().forEach(player ->
			{
				if (player != null)
				{
					final Location exitLoc = world.getTemplateParameters().getLocation("exit");
					if (exitLoc != null)
					{
						player.teleToLocation(exitLoc, false);
					}
					else
					{
						player.teleToLocation(new Location(82507, 148619, -3488), false);
					}
					world.removeAllowed(player);
				}
			});
			world.destroy();
		}, 180_000);
	}

	// ========================================
	// Reward Chest Dialogue
	// ========================================

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = player.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != FIRE_DRAGON_TEMPLE_INSTANCE_ID))
		{
			return null;
		}

		if (world.getStatus() != FINISH_INSTANCE)
		{
			return "RewardChest/no-reward.htm";
		}

		if (player.getVariables().getBoolean(PLAYER_REWARDED_VAR, false))
		{
			return "RewardChest/already-rewarded.htm";
		}

		return "RewardChest/reward.htm";
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("claim_reward"))
		{
			final Instance world = player.getInstanceWorld();
			if ((world == null) || (world.getTemplateId() != FIRE_DRAGON_TEMPLE_INSTANCE_ID))
			{
				return null;
			}

			if (world.getStatus() != FINISH_INSTANCE)
			{
				return "RewardChest/no-reward.htm";
			}

			if (player.getVariables().getBoolean(PLAYER_REWARDED_VAR, false))
			{
				return "RewardChest/already-rewarded.htm";
			}

			// 先設旗標，防止重複領取
			player.getVariables().set(PLAYER_REWARDED_VAR, true);

			// 非同步給予道具
			ThreadPool.execute(() ->
			{
				giveRewards(player);
				checkAndApplyPity(player);
			});

			// 15 秒後傳送玩家離開
			ThreadPool.schedule(() ->
			{
				if (player.getInstanceWorld() == world)
				{
					final Location exitLoc = world.getTemplateParameters().getLocation("exit");
					if (exitLoc != null)
					{
						player.teleToLocation(exitLoc, false, null);
					}
					else
					{
						player.teleToLocation(new Location(82507, 148619, -3488), false, null);
					}
					world.removeAllowed(player);
				}
			}, 15_000);

			return "RewardChest/rewarded.htm";
		}

		return super.onEvent(event, npc, player);
	}

	// ========================================
	// Reward Logic
	// ========================================

	private void giveRewards(Player player)
	{
		final Map<Integer, Long> batchMap = new LinkedHashMap<>();
		for (int i = 0; i < REWARD_ITEMS_COUNT; i++)
		{
			final int random = getRandom(TOTAL_REWARD_CHANCE);
			int currentChance = 0;
			for (int[] reward : REWARD_ITEMS)
			{
				currentChance += reward[2];
				if (random < currentChance)
				{
					batchMap.merge(reward[0], (long) reward[1], Long::sum);
					break;
				}
			}
		}
		for (Map.Entry<Integer, Long> entry : batchMap.entrySet())
		{
			giveItems(player, entry.getKey(), entry.getValue());
		}
	}

	private void checkAndApplyPity(Player player)
	{
		final int count = player.getVariables().getInt(PLAYER_PITY_COUNT_VAR, 0) + 1;
		if (count >= PITY_THRESHOLD)
		{
			final int random = getRandom(TOTAL_PITY_CHANCE);
			int currentChance = 0;
			for (int[] reward : PITY_REWARD_ITEMS)
			{
				currentChance += reward[2];
				if (random < currentChance)
				{
					giveItems(player, reward[0], reward[1]);
					break;
				}
			}
			player.getVariables().set(PLAYER_PITY_COUNT_VAR, 0);
			player.sendPacket(new ExShowScreenMessage("保底觸發！已獲得保底獎勵！", 6000));
		}
		else
		{
			player.getVariables().set(PLAYER_PITY_COUNT_VAR, count);
			player.sendPacket(new ExShowScreenMessage("累計通關 " + count + "/" + PITY_THRESHOLD + " 次，還差 " + (PITY_THRESHOLD - count) + " 次觸發保底。", 5000));
		}
	}

	// ========================================
	// Instance Destroy
	// ========================================

	@Override
	public void onInstanceDestroy(Instance instance)
	{
		cancelQuestTimers("CHECK_STATUS");
		super.onInstanceDestroy(instance);
	}

	public static void main(String[] args)
	{
		new FireDragonTemple();
	}
}
