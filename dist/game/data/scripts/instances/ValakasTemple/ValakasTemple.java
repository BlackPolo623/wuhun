/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package instances.ValakasTemple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.events.ValakasTempleManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureTeleported;
import org.l2jmobius.gameserver.model.events.holders.instance.OnInstanceStatusChange;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.spawns.NpcSpawnTemplate;
import org.l2jmobius.gameserver.model.spawns.SpawnGroup;
import org.l2jmobius.gameserver.model.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

/**
 * @author Index
 */
public class ValakasTemple extends InstanceScript
{
	// ========================================
	// Reward Chest Configuration
	// ========================================
	private static final int REWARD_CHEST_NPC_ID = 920001;
	public static final int REWARD_ITEMS_COUNT = 10;
	public static final int SWEEP_REWARD_ITEMS_COUNT = 5;
	public static final int[][] REWARD_ITEMS =
	{
		{
			130000,
			1,
			60
		},
		{
			130000,
			3,
			20
		},
		{
			130000,
			5,
			10
		},
		{
			130001,
			1,
			2
		},
		{
			130002,
			1,
			2
		},
		{
			130009,
			1,
			1
		},
		{
			1300010,
			1,
			1
		},
		{
			1300011,
			1,
			1
		},
		{
			1300012,
			1,
			1
		},
		{
			1300013,
			1,
			1
		},
		{
			1300014,
			1,
			1
		},
	};
	private static final String PLAYER_REWARDED_VAR = "VALAKAS_TEMPLE_REWARDED";
	public static final String PLAYER_CLEARED_VAR = "VALAKAS_TEMPLE_CLEARED";
	
	// ========================================
	// Pity System
	// ========================================
	public static final int PITY_THRESHOLD = 10;
	public static final int[][] PITY_REWARD_ITEMS =
	{
		{
			130009,
			1,
			15
		},
		{
			1300010,
			1,
			15
		},
		{
			1300011,
			1,
			15
		},
		{
			1300012,
			1,
			15
		},
		{
			1300013,
			1,
			15
		},
		{
			1300014,
			1,
			15
		},
	};
	private static final String PLAYER_PITY_COUNT_VAR = "VALAKAS_TEMPLE_PITY_COUNT";
	
	// ========================================
	// Instance Status Constants
	// ========================================
	private static final int INSTANCE_CREATE = 0;
	private static final int SAVE_HERMIT = 1;
	private static final int KILL_OBSERVATION_DEVICE_CENTER = 2;
	private static final int KILL_MONSTERS_CENTER = 3;
	private static final int KILL_IFRIT_CENTER = 4;
	private static final int KILL_LEFT_OR_RIGHT = 5;
	private static final int KILL_OBSERVATION_DEVICE_TOMB = 6;
	public static final int KILL_TOMB = 7;
	public static final int GOTO_DUMMY_IFRIT = 8;
	public static final int OPEN_GATE_TIMER = 9;
	private static final int KILL_LAST_IFRIT = 10;
	private static final int FINISH_INSTANCE = 11;
	
	private static final int LEFT_SPECTATOR = 1;
	private static final int RIGHT_SPECTATOR = 2;
	private static final int TOMB_SPECTATOR = 3;
	
	public static final int VALAKAS_TEMPLE_INSTANCE_ID = 230;
	public static final int EVENT_ID_PLAYER_CIRCLE = 24137770;
	private static final int EVENT_ID_BOSS_CIRCLE = 24138880;
	private static final int BOSS_DOOR_ID = 24130002;
	private static final String IS_REMOVED_EVENTS = "IS_REMOVED_EVENTS";
	
	// ========================================
	// NPC IDs
	// ========================================
	private static final int OBSERVATION_DEVICE = 18730;
	private static final int HUGE_IFRIT = 25964;
	private static final int DUMMY_IFRIT_NPC = 18727;
	private static final int LAST_IFRIT = 25966;
	private static final int[] MONSTER_IDs =
	{
		22490,
		22491,
		22492,
		22493,
		22494,
	};
	
	private ValakasTemple()
	{
		super(VALAKAS_TEMPLE_INSTANCE_ID);
		setInstanceStatusChangeId(this::onInstanceStatusChange, VALAKAS_TEMPLE_INSTANCE_ID);
		addInstanceCreatedId(VALAKAS_TEMPLE_INSTANCE_ID);
		addKillId(OBSERVATION_DEVICE);
		addKillId(HUGE_IFRIT);
		addKillId(DUMMY_IFRIT_NPC);
		addKillId(LAST_IFRIT);
		addKillId(MONSTER_IDs);
		addFirstTalkId(REWARD_CHEST_NPC_ID);
		addTalkId(REWARD_CHEST_NPC_ID);
	}
	
	public void onInstanceStatusChange(OnInstanceStatusChange event)
	{
		final Instance world = event.getWorld();
		final int status = event.getStatus();
		switch (status)
		{
			case INSTANCE_CREATE:
			{
				break;
			}
			case KILL_OBSERVATION_DEVICE_CENTER:
			{
				setSecondStatusForInstance(world);
				break;
			}
			case KILL_MONSTERS_CENTER:
			{
				setThirdStatusForInstance(world);
				break;
			}
			case KILL_IFRIT_CENTER:
			{
				setFourthStatusForInstance(world);
				break;
			}
			case KILL_LEFT_OR_RIGHT:
			{
				// 25964 死後：直接刷左右兩隻 18730（完全同原版）
				setFifthStatusForInstance(world);
				break;
			}
			case KILL_OBSERVATION_DEVICE_TOMB:
			{
				setSixthStatusForInstance(world);
				break;
			}
			case KILL_TOMB:
			{
				// 墓碑區 18730 死後：解除 18727 無敵
				setSeventhStatusForInstance(world);
				break;
			}
			case GOTO_DUMMY_IFRIT:
			{
				setEightStatusForInstance(world);
				break;
			}
			case OPEN_GATE_TIMER:
			{
				setNineStatusForInstance(world);
				break;
			}
			case KILL_LAST_IFRIT:
			{
				setTenStatusForInstance(world);
				break;
			}
			case FINISH_INSTANCE:
			{
				setElevenStatusForInstance(world);
				break;
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = (killer == null) || (npc == null) ? null : killer.getInstanceWorld();
		if ((npc == null) || (world == null) || (world.getTemplateId() != VALAKAS_TEMPLE_INSTANCE_ID))
		{
			return;
		}
		
		switch (npc.getId())
		{
			case OBSERVATION_DEVICE:
			{
				if (world.getStatus() == KILL_OBSERVATION_DEVICE_CENTER)
				{
					world.setStatus(KILL_MONSTERS_CENTER);
				}
				else if ((world.getStatus() == KILL_OBSERVATION_DEVICE_TOMB) && (npc.getScriptValue() == TOMB_SPECTATOR))
				{
					// 墓碑區看守 18730 死亡 → 解除 18727 無敵
					world.setStatus(KILL_TOMB);
				}
				else
				{
					// 擊殺左或右的 18730 → 刷對應側小怪（原版邏輯）
					spawnMonsterLeftOrRight(world, npc.getScriptValue());
				}
				break;
			}
			case HUGE_IFRIT:
			{
				if (world.getStatus() == KILL_IFRIT_CENTER)
				{
					// 擊殺 25964 → 觸發 setFifthStatusForInstance，刷左右兩隻 18730
					world.setStatus(KILL_LEFT_OR_RIGHT);
				}
				break;
			}
			case DUMMY_IFRIT_NPC:
			{
				// 18727 無敵解除後被擊殺，不觸發副本狀態變化
				break;
			}
			case LAST_IFRIT:
			{
				if (world.getStatus() == KILL_LAST_IFRIT)
				{
					world.setStatus(FINISH_INSTANCE);
				}
				break;
			}
			default:
			{
				if ((world.getStatus() == SAVE_HERMIT) && world.getAliveNpcs(MONSTER_IDs).isEmpty())
				{
					world.setStatus(KILL_OBSERVATION_DEVICE_CENTER);
				}
				else if ((world.getStatus() == KILL_MONSTERS_CENTER) && world.getAliveNpcs(MONSTER_IDs).isEmpty())
				{
					world.setStatus(KILL_IFRIT_CENTER);
				}
				else if (world.getStatus() == KILL_LEFT_OR_RIGHT)
				{
					// 左右小怪全死 → 解鎖墓碑區
					if (world.getAliveNpcs(MONSTER_IDs).isEmpty())
					{
						world.setStatus(KILL_OBSERVATION_DEVICE_TOMB);
					}
				}
				break;
			}
		}
	}
	
	@Override
	public void onInstanceCreated(Instance world, Player player)
	{
		final InstanceTemplate template = InstanceManager.getInstance().getInstanceTemplate(VALAKAS_TEMPLE_INSTANCE_ID);
		for (SpawnTemplate spawn : template.getSpawns())
		{
			final List<Location> locations = new ArrayList<>(7);
			for (SpawnGroup group : spawn.getGroupsByName("clones"))
			{
				for (NpcSpawnTemplate clone : group.getSpawns())
				{
					locations.add(clone.getSpawnLocation());
				}
			}
			world.setParameter("TELEPORT_CLONES", locations);
		}
		
		world.setStatus(SAVE_HERMIT);
		
		// 刷出 tomb group（18727 封印石碑 + 18728 封印裝置）
		// 18727 立刻設為無敵，防止玩家跳過流程直接擊殺
		final List<Npc> tombNpcs = world.spawnGroup("tomb");
		if (tombNpcs != null)
		{
			for (Npc tombNpc : tombNpcs)
			{
				if (tombNpc.getId() == DUMMY_IFRIT_NPC)
				{
					tombNpc.setInvul(true);
				}
			}
		}
		
		final List<Npc> monstersNearHermit = world.getNpcsOfGroup("hermit_attackers_01");
		final Npc hermitInInstance = world.getNpcOfGroup("hermit_01", Objects::nonNull);
		if ((monstersNearHermit == null) || monstersNearHermit.isEmpty())
		{
			return;
		}
		
		for (Npc monster : monstersNearHermit)
		{
			final Attackable attackable = monster.asAttackable();
			attackable.addDamageHate(hermitInInstance, 0, 1);
			addAttackDesire(monster, hermitInInstance);
		}
		
		ThreadPool.schedule(() -> sendMessageOnScreen(world, NpcStringId.OVER_HERE_PLEASE_HELP_ME), 3000);
		
		super.onInstanceCreated(world, player);
	}
	
	// Status 2: 中央 18730 死 → 刷門+18730
	private static void setSecondStatusForInstance(Instance world)
	{
		world.spawnGroup("gate_of_legion_center");
		world.spawnGroup("spectator_center");
	}
	
	// Status 3: 中央 18730 死 → 刷中央小怪
	private static void setThirdStatusForInstance(Instance world)
	{
		ThreadPool.schedule(() -> world.spawnGroup("monsters_from_gate_center"), 6_000);
		ThreadPool.schedule(() -> world.spawnGroup("monsters_from_gate_center_02"), 6_000);
	}
	
	// Status 4: 中央小怪死 → 刷 25964
	private static void setFourthStatusForInstance(Instance world)
	{
		world.spawnGroup("raid_boss_center");
		sendMessageOnScreen(world, NpcStringId.WHO_YOU_ARE_HOW_DARE_YOU_INVADE_THE_TEMPLE_OF_THE_GREAT_VALAKAS);
	}
	
	// Status 5: 25964 死 → 刷左右兩隻 18730（完全還原原版邏輯）
	private static void setFifthStatusForInstance(Instance world)
	{
		world.spawnGroup("gate_of_legion_left");
		final Npc npcLeft = world.spawnGroup("spectator_left").get(0);
		npcLeft.setScriptValue(LEFT_SPECTATOR);
		world.spawnGroup("gate_of_legion_right");
		final Npc npcRight = world.spawnGroup("spectator_right").get(0);
		npcRight.setScriptValue(RIGHT_SPECTATOR);
	}
	
	// Status 6: 左右小怪全死 → 刷墓碑區 18730
	private static void setSixthStatusForInstance(Instance world)
	{
		final Npc tombSpectator = world.spawnGroup("spectator_tomb").get(0);
		tombSpectator.setScriptValue(TOMB_SPECTATOR);
		world.spawnGroup("gate_of_legion_tomb");
	}
	
	// Status 7: 墓碑區 18730 死 → 解除 18727 無敵 + 刷墓碑小怪
	private static void setSeventhStatusForInstance(Instance world)
	{
		for (Npc npc : world.getNpcs())
		{
			if (npc.getId() == DUMMY_IFRIT_NPC)
			{
				npc.setInvul(false);
				npc.broadcastInfo();
			}
		}
		ThreadPool.schedule(() -> world.spawnGroup("monsters_from_gate_tomb"), 6_000);
	}
	
	// Status 8: GOTO_DUMMY_IFRIT
	private static void setEightStatusForInstance(Instance world)
	{
		final Npc dummyIfrit = world.spawnGroup("dummy_ifrit").get(0);
		dummyIfrit.setImmobilized(true);
		world.spawnGroup("hermit_02");
	}
	
	// Status 9: OPEN_GATE_TIMER
	private static void setNineStatusForInstance(Instance world)
	{
		world.getPlayers().forEach(player -> player.sendPacket(new OnEventTrigger(ValakasTemple.EVENT_ID_PLAYER_CIRCLE, true)));
		ThreadPool.schedule(() -> world.setStatus(KILL_LAST_IFRIT), 15_000);
	}
	
	// Status 10: KILL_LAST_IFRIT
	private static void setTenStatusForInstance(Instance world)
	{
		ThreadPool.schedule(() -> removeEventsFromInstance(world), 15_000);
		world.getPlayers().forEach(player -> player.sendPacket(new OnEventTrigger(ValakasTemple.EVENT_ID_BOSS_CIRCLE, true)));
		
		final List<Player> players = new ArrayList<>(world.getPlayers());
		Collections.shuffle(players);
		final List<Location> locations = world.getParameters().getList("TELEPORT_CLONES", Location.class);
		for (Player player : players)
		{
			player.teleToLocation(getRandomEntry(locations), false);
		}
		
		final List<Npc> cloneNpcs = world.spawnGroup("clones");
		int counter = 0;
		for (Npc clone : cloneNpcs)
		{
			if ((counter >= players.size()) || (counter > 7))
			{
				break;
			}
			clone.setCloneObjId(players.get(counter).getObjectId());
			clone.setName(players.get(counter).getAppearance().getVisibleName());
			clone.broadcastInfo();
			counter += 1;
		}
	}
	
	// Status 11: FINISH_INSTANCE
	private static void setElevenStatusForInstance(Instance world)
	{
		world.spawnGroup("reward_chest");
		world.getPlayers().forEach(player -> player.sendPacket(new ExShowScreenMessage("副本將在5分鐘後關閉，請盡快領取獎勵！", 10000)));
		world.getPlayers().forEach(player -> player.getVariables().set(PLAYER_CLEARED_VAR, true));
		
		ThreadPool.schedule(() ->
		{
			final Location exitLoc = world.getTemplateParameters().getLocation("exit");
			world.getPlayers().forEach(player ->
			{
				if (player != null)
				{
					if (exitLoc != null)
					{
						player.teleToLocation(exitLoc, false);
					}
					else
					{
						player.teleToLocation(new Location(82507, 148619, -3488), false);
					}
				}
			});
			world.destroy();
		}, 300_000);
	}
	
	private static void removeEventsFromInstance(Instance world)
	{
		world.despawnGroup("dummy_ifrit");
		runRemoveEventsFromInstance(world);
		ThreadPool.schedule(() -> spawnBoss(world), 15000);
	}
	
	private static void runRemoveEventsFromInstance(Instance world)
	{
		world.getParameters().set(IS_REMOVED_EVENTS, true);
		world.getPlayers().forEach(player -> player.sendPacket(new OnEventTrigger(ValakasTemple.EVENT_ID_PLAYER_CIRCLE, false)));
		world.getPlayers().forEach(player -> player.sendPacket(new OnEventTrigger(ValakasTemple.EVENT_ID_BOSS_CIRCLE, false)));
	}
	
	private static void spawnBoss(Instance world)
	{
		world.openCloseDoor(BOSS_DOOR_ID, true);
		world.spawnGroup("raid_boss_last");
		ThreadPool.schedule(() -> world.spawnGroup("monsters_from_gate_last"), 15000);
	}
	
	// 擊殺左或右的 18730 後刷對應側小怪（完全同原版）
	private static void spawnMonsterLeftOrRight(Instance world, int side)
	{
		final String spawnGroupName = side == LEFT_SPECTATOR ? "monsters_from_gate_left" : "monsters_from_gate_right";
		ThreadPool.schedule(() -> world.spawnGroup(spawnGroupName), 6000);
	}
	
	private static void sendMessageOnScreen(Instance world, NpcStringId npcId)
	{
		final ExShowScreenMessage screenMessage = new ExShowScreenMessage(npcId, ExShowScreenMessage.TOP_CENTER, 10000, true);
		world.getPlayers().forEach(player -> player.sendPacket(screenMessage));
	}
	
	@RegisterEvent(EventType.ON_CREATURE_TELEPORTED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onCreatureTeleported(OnCreatureTeleported event)
	{
		if (!event.getCreature().isPlayer())
		{
			return;
		}
		
		final Player player = event.getCreature().asPlayer();
		final Instance world = player.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != VALAKAS_TEMPLE_INSTANCE_ID))
		{
			return;
		}
		
		if ((world.getStatus() == KILL_LAST_IFRIT) && !world.getParameters().getBoolean(IS_REMOVED_EVENTS, false))
		{
			player.sendPacket(new OnEventTrigger(ValakasTemple.EVENT_ID_PLAYER_CIRCLE, true));
			player.sendPacket(new OnEventTrigger(ValakasTemple.EVENT_ID_BOSS_CIRCLE, true));
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = player.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != VALAKAS_TEMPLE_INSTANCE_ID))
		{
			return null;
		}
		
		if (world.getStatus() != FINISH_INSTANCE)
		{
			return "RewardChest/no-reward.htm";
		}
		
		final String instanceRewardedKey = PLAYER_REWARDED_VAR + "_" + world.getId();
		if (player.getVariables().getBoolean(instanceRewardedKey, false))
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
			if ((world == null) || (world.getTemplateId() != VALAKAS_TEMPLE_INSTANCE_ID))
			{
				return null;
			}
			
			if (world.getStatus() != FINISH_INSTANCE)
			{
				return "RewardChest/no-reward.htm";
			}
			
			final String instanceRewardedKey = PLAYER_REWARDED_VAR + "_" + world.getId();
			if (player.getVariables().getBoolean(instanceRewardedKey, false))
			{
				return "RewardChest/already-rewarded.htm";
			}
			
			// 扣取本週次數
			ValakasTempleManager.getInstance().incrementEntryCount(player);
			
			giveRewards(player);
			checkAndApplyPity(player);
			player.getVariables().set(instanceRewardedKey, true);
			
			ThreadPool.schedule(() ->
			{
				if ((player != null) && (player.getInstanceWorld() == world))
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
				}
			}, 15_000);
			
			return "RewardChest/rewarded.htm";
		}
		
		return super.onEvent(event, npc, player);
	}
	
	private void giveRewards(Player player)
	{
		for (int i = 0; i < REWARD_ITEMS_COUNT; i++)
		{
			int totalChance = 0;
			for (int[] reward : REWARD_ITEMS)
			{
				totalChance += reward[2];
			}
			
			final int random = getRandom(totalChance);
			int currentChance = 0;
			
			for (int[] reward : REWARD_ITEMS)
			{
				currentChance += reward[2];
				if (random < currentChance)
				{
					giveItems(player, reward[0], reward[1]);
					break;
				}
			}
		}
	}
	
	private void checkAndApplyPity(Player player)
	{
		final int count = player.getVariables().getInt(PLAYER_PITY_COUNT_VAR, 0) + 1;
		if (count >= PITY_THRESHOLD)
		{
			int totalChance = 0;
			for (int[] reward : PITY_REWARD_ITEMS)
			{
				totalChance += reward[2];
			}
			final int random = getRandom(totalChance);
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
	
	public static void main(String[] args)
	{
		new ValakasTemple();
	}
}
