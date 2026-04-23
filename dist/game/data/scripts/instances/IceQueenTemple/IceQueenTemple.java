package instances.IceQueenTemple;

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
 * 冰凍女王副本 (Ice Queen Temple)
 * 三階段 BOSS：18933 → 18934 → 18935
 * 寶箱依階段：920002 / 920003 / 920004
 * 副本 ID：232
 */
public class IceQueenTemple extends InstanceScript
{
	// ========================================
	// Instance ID
	// ========================================
	public static final int ICE_QUEEN_TEMPLE_INSTANCE_ID = 232;

	// ========================================
	// NPC IDs
	// ========================================
	private static final int ICE_QUEEN_1 = 18933;
	private static final int ICE_QUEEN_2 = 18934;
	private static final int ICE_QUEEN_3 = 18935;
	private static final int REWARD_CHEST_1 = 920002;
	private static final int REWARD_CHEST_2 = 920003;
	private static final int REWARD_CHEST_3 = 920004;

	// ========================================
	// Instance Status
	// ========================================
	private static final int PHASE1_ALIVE  = 1;
	private static final int PHASE2_ALIVE  = 2;
	private static final int PHASE3_ALIVE  = 3;
	private static final int FINISH_CHEST1 = 4;
	private static final int FINISH_CHEST2 = 5;
	private static final int FINISH_CHEST3 = 6;

	// ========================================
	// Phase Upgrade Chances (0-100)
	// ========================================
	/** 打死第一階段 BOSS 後，進階到第二階段的機率 */
	public static final int PHASE1_UPGRADE_CHANCE = 50;
	/** 打死第二階段 BOSS 後，進階到第三階段的機率 */
	public static final int PHASE2_UPGRADE_CHANCE = 30;

	// ========================================
	// Player Variable Keys
	// ========================================
	private static final String PLAYER_REWARDED_VAR   = "ICE_QUEEN_TEMPLE_REWARDED";
	private static final String PLAYER_PITY_COUNT_VAR = "ICE_QUEEN_TEMPLE_PITY";
	/** 是否曾通關第三階段（解鎖掃蕩資格） */
	public static final String PLAYER_ICE_CLEARED_VAR = "ICE_QUEEN_TEMPLE_CLEARED";

	// ========================================
	// Reward Configuration
	// ========================================
	public static final int REWARD_ITEMS_COUNT       = 6;
	public static final int SWEEP_REWARD_ITEMS_COUNT = 4;
	public static final int PITY_THRESHOLD           = 7;

	// ---------- Tier 1 ----------
	public static final int[][] REWARD_ITEMS_1 =
	{
		{98614, 1,  100},
		{98614, 3,   80},
		{98614, 5,   60},
		{98614, 10,  40},
		{98614, 15,  20},
		{98614, 20,  10},
		{98614, 30,   5},
		{98614, 50,   1},
		{120000, 1,   1},
		{120001, 1,   1},
		{120002, 1,   1},
		{120003, 1,   1},
		{120004, 1,   1},
		{120005, 1,   1},
		{120006, 1,   1},
		{120007, 1,   1},
		{120008, 1,   1},
		{120009, 1,   1},
		{120010, 1,   1},
		{120011, 1,   1},
		{120012, 1,   1},
	};
	// ---------- Tier 2 ----------
	public static final int[][] REWARD_ITEMS_2 =
	{
		{98614, 2,  100},
		{98614, 6,   80},
		{98614, 10,   60},
		{98614, 20,  40},
		{98614, 30,  20},
		{98614, 40,  10},
		{98614, 60,   5},
		{98614, 100,   1},
		{120013, 1,   1},
		{120014, 1,   1},
		{120015, 1,   1},
		{120016, 1,   1},
		{120017, 1,   1},
		{120018, 1,   1},
		{120019, 1,   1},
		{120020, 1,   1},
		{120021, 1,   1},
		{120022, 1,   1},
		{120023, 1,   1},
		{120024, 1,   1},
		{120025, 1,   1},
	};

	// ---------- Tier 3 ----------
	public static final int[][] REWARD_ITEMS_3 =
	{
		{98614, 3,  100},
		{98614, 9,   80},
		{98614, 15,   60},
		{98614, 30,  40},
		{98614, 45,  20},
		{98614, 60,  10},
		{98614, 90,   5},
		{98614, 150,   1},
		{120026, 1,   1},
		{120027, 1,   1},
		{120028, 1,   1},
		{120029, 1,   1},
		{120030, 1,   1},
		{120031, 1,   1},
		{120032, 1,   1},
		{120033, 1,   1},
		{120034, 1,   1},
		{120035, 1,   1},
		{120036, 1,   1},
		{120037, 1,   1},
		{120038, 1,   1},
	};


	public static final int[][] PITY_REWARD_ITEMS =
			{
					{120000, 1,   1},
					{120001, 1,   1},
					{120002, 1,   1},
					{120003, 1,   1},
					{120004, 1,   1},
					{120005, 1,   1},
					{120006, 1,   1},
					{120007, 1,   1},
					{120008, 1,   1},
					{120009, 1,   1},
					{120010, 1,   1},
					{120011, 1,   1},
					{120012, 1,   1},
			};


	// Pre-calculated total chances
	public static final int TOTAL_REWARD_CHANCE_1;
	public static final int TOTAL_REWARD_CHANCE_2;
	public static final int TOTAL_REWARD_CHANCE_3;
	private static final int TOTAL_PITY_CHANCE;
	static
	{
		int t;
		t = 0; for (int[] r : REWARD_ITEMS_1)     { t += r[2]; } TOTAL_REWARD_CHANCE_1 = t;
		t = 0; for (int[] r : REWARD_ITEMS_2)     { t += r[2]; } TOTAL_REWARD_CHANCE_2 = t;
		t = 0; for (int[] r : REWARD_ITEMS_3)     { t += r[2]; } TOTAL_REWARD_CHANCE_3 = t;
		t = 0; for (int[] r : PITY_REWARD_ITEMS)  { t += r[2]; } TOTAL_PITY_CHANCE     = t;
	}

	// ========================================
	// Constructor
	// ========================================

	private IceQueenTemple()
	{
		super(ICE_QUEEN_TEMPLE_INSTANCE_ID);
		addInstanceCreatedId(ICE_QUEEN_TEMPLE_INSTANCE_ID);
		addKillId(ICE_QUEEN_1, ICE_QUEEN_2, ICE_QUEEN_3);
		addFirstTalkId(REWARD_CHEST_1, REWARD_CHEST_2, REWARD_CHEST_3);
		addTalkId(REWARD_CHEST_1, REWARD_CHEST_2, REWARD_CHEST_3);
	}

	// ========================================
	// Instance Lifecycle
	// ========================================

	@Override
	public void onInstanceCreated(Instance world, Player player)
	{
		player.getVariables().remove(PLAYER_REWARDED_VAR);
		if (player.isInParty())
		{
			for (Player member : player.getParty().getMembers())
			{
				member.getVariables().remove(PLAYER_REWARDED_VAR);
			}
		}

		world.setStatus(PHASE1_ALIVE);
		super.onInstanceCreated(world, player);
	}

	// ========================================
	// Kill Handler
	// ========================================

	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = (killer == null) ? null : killer.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != ICE_QUEEN_TEMPLE_INSTANCE_ID))
		{
			return;
		}

		final int npcId = npc.getId();

		if ((npcId == ICE_QUEEN_1) && (world.getStatus() == PHASE1_ALIVE))
		{
			if (getRandom(100) < PHASE1_UPGRADE_CHANCE)
			{
				world.setStatus(PHASE2_ALIVE);
				world.spawnGroup("ice_queen_2");
				broadcast(world, "冰凍女王實力提升！第二形態出現！", 8000);
			}
			else
			{
				world.setStatus(FINISH_CHEST1);
				world.spawnGroup("reward_chest_1");
				scheduleClose(world, "冰凍女王已被擊敗！請領取一階獎勵，副本將在3分鐘後關閉。");
			}
		}
		else if ((npcId == ICE_QUEEN_2) && (world.getStatus() == PHASE2_ALIVE))
		{
			if (getRandom(100) < PHASE2_UPGRADE_CHANCE)
			{
				world.setStatus(PHASE3_ALIVE);
				world.spawnGroup("ice_queen_3");
				broadcast(world, "冰凍女王再次進化！最終形態出現！", 8000);
			}
			else
			{
				world.setStatus(FINISH_CHEST2);
				world.spawnGroup("reward_chest_2");
				scheduleClose(world, "冰凍女王已被擊敗！請領取二階獎勵，副本將在3分鐘後關閉。");
			}
		}
		else if ((npcId == ICE_QUEEN_3) && (world.getStatus() == PHASE3_ALIVE))
		{
			world.setStatus(FINISH_CHEST3);
			world.spawnGroup("reward_chest_3");
			// 記錄所有隊員已通關第三階段（解鎖掃蕩）
			world.getPlayers().forEach(p -> p.getVariables().set(PLAYER_ICE_CLEARED_VAR, true));
			scheduleClose(world, "最終形態冰凍女王已被擊敗！請領取最高獎勵，副本將在3分鐘後關閉。");
		}
	}

	private static void broadcast(Instance world, String msg, int duration)
	{
		world.getPlayers().forEach(p -> p.sendPacket(new ExShowScreenMessage(msg, duration)));
	}

	private static void scheduleClose(Instance world, String msg)
	{
		broadcast(world, msg, 10000);
		ThreadPool.schedule(() ->
		{
			world.getPlayers().forEach(p ->
			{
				if (p != null)
				{
					p.teleToLocation(new Location(82507, 148619, -3488), false);
					world.removeAllowed(p);
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
		if ((world == null) || (world.getTemplateId() != ICE_QUEEN_TEMPLE_INSTANCE_ID))
		{
			return null;
		}

		final int tier = getTier(npc.getId());
		final int requiredStatus = FINISH_CHEST1 + (tier - 1);

		if (world.getStatus() != requiredStatus)
		{
			return "RewardChest/no-reward.htm";
		}

		if (player.getVariables().getBoolean(PLAYER_REWARDED_VAR, false))
		{
			return "RewardChest/already-rewarded.htm";
		}

		return "RewardChest/reward_" + tier + ".htm";
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("claim_reward"))
		{
			final Instance world = player.getInstanceWorld();
			if ((world == null) || (world.getTemplateId() != ICE_QUEEN_TEMPLE_INSTANCE_ID))
			{
				return null;
			}

			final int tier = getTier(npc.getId());
			final int requiredStatus = FINISH_CHEST1 + (tier - 1);

			if (world.getStatus() != requiredStatus)
			{
				return "RewardChest/no-reward.htm";
			}

			if (player.getVariables().getBoolean(PLAYER_REWARDED_VAR, false))
			{
				return "RewardChest/already-rewarded.htm";
			}

			player.getVariables().set(PLAYER_REWARDED_VAR, true);

			final int finalTier = tier;
			ThreadPool.execute(() ->
			{
				giveRewards(player, finalTier);
				checkAndApplyPity(player, finalTier);
			});

			ThreadPool.schedule(() ->
			{
				if (player.getInstanceWorld() == world)
				{
					player.teleToLocation(new Location(82507, 148619, -3488), false, null);
					world.removeAllowed(player);
				}
			}, 15_000);

			return "RewardChest/rewarded.htm";
		}

		return super.onEvent(event, npc, player);
	}

	// ========================================
	// Helpers
	// ========================================

	/** Returns chest tier (1/2/3) based on NPC ID. */
	private static int getTier(int npcId)
	{
		switch (npcId)
		{
			case REWARD_CHEST_2: return 2;
			case REWARD_CHEST_3: return 3;
			default:             return 1;
		}
	}

	// ========================================
	// Reward Logic
	// ========================================

	private void giveRewards(Player player, int tier)
	{
		final int[][] table = (tier == 3) ? REWARD_ITEMS_3 : (tier == 2) ? REWARD_ITEMS_2 : REWARD_ITEMS_1;
		final int totalChance = (tier == 3) ? TOTAL_REWARD_CHANCE_3 : (tier == 2) ? TOTAL_REWARD_CHANCE_2 : TOTAL_REWARD_CHANCE_1;

		final Map<Integer, Long> batchMap = new LinkedHashMap<>();
		for (int i = 0; i < REWARD_ITEMS_COUNT; i++)
		{
			final int random = getRandom(totalChance);
			int currentChance = 0;
			for (int[] reward : table)
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

	private void checkAndApplyPity(Player player, int tier)
	{
		final int[][] pityTable = PITY_REWARD_ITEMS;

		final int count = player.getVariables().getInt(PLAYER_PITY_COUNT_VAR, 0) + 1;
		if (count >= PITY_THRESHOLD)
		{
			final int random = getRandom(TOTAL_PITY_CHANCE);
			int currentChance = 0;
			for (int[] reward : pityTable)
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
		new IceQueenTemple();
	}
}
