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
package instances.LeonasDungeon;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.events.LeonasDungeonManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

/**
 * @author Serenitty, Mobius
 */
public class LeonasDungeon extends InstanceScript
{
	// NPC
	private static final int LEONA = 34357;

	// Skills
	private final SkillHolder FORCE_BUFF = new SkillHolder(4647, 4);
	private final SkillHolder FORCE_DEBUFF = new SkillHolder(48403, 2);
	private final SkillHolder[] LEONA_BUFFS =
	{
		new SkillHolder(48640, 4), // Leona's Blessing - Focus
		new SkillHolder(48641, 4), // Leona's Blessing - Death Whisper
		new SkillHolder(48643, 3), // Leona's Blessing - Haste
		new SkillHolder(48645, 4), // Leona's Blessing - Might
		new SkillHolder(48647, 4), // Leona's Blessing - Shield
		new SkillHolder(48649, 3), // Leona's Blessing - Wind Walk
		new SkillHolder(48651, 3), // Leona's Blessing - Berserker Spirit
		new SkillHolder(48652, 2), // Leona's Blessing - HP Recovery
		new SkillHolder(48653, 2), // Leona's Blessing - MP Recovery
		new SkillHolder(48836, 1), // Leona's XP Blessing
	};
	// Misc
	private static final int EVENT_TRIGGER_LEONAS_AREA = 18108866;
	private static final int TEMPLATE_ID = 236;

	// Monster IDs
	private static final int[] MOBS_DIFFICULTY_1 = {22535, 22536, 22537}; // Lith Guard, Lith Medium, Lith Prefect
	private static final int[] MOBS_DIFFICULTY_2 = {22529, 22530, 22531}; // Nephilim Guardsman, Nephilim Cardinal, Nephilim Battlemaster
	private static final int[] MOBS_DIFFICULTY_3 = {22538, 22539, 22540}; // Lilim's Guardian, Lilim's Assassin, Lilim's Soldier

	// Scoring Configuration
	private static final int POINTS_PER_KILL = 5; // 擊殺怪物基礎分數
	private static final int POINTS_PER_MINUTE_LOW = 10; // 低難度每分鐘分數
	private static final int POINTS_PER_MINUTE_MED = 20; // 中難度每分鐘分數
	private static final int POINTS_PER_MINUTE_HIGH = 30; // 高難度每分鐘分數
	private static final int POINTS_DEDUCT_PER_DEATH = 50; // 每次死亡扣分
	private static final int POINTS_DEDUCT_HP_THRESHOLD = 50; // HP低於此%時扣分
	private static final int POINTS_DEDUCT_LOW_HP = 200; // HP過低時扣分
	private static final double MOB_POWER_INCREASE = 1.3; // 怪物每次重生增強5%

	// Tiered Milestone System (無上限階梯式獎勵)
	// 1000-1999: 每100隻 +5分
	// 2000-2999: 每100隻 +7分
	// 3000-3999: 每100隻 +9分
	// 以此類推，每1000隻增加2分獎勵
	
	private LeonasDungeon()
	{
		super(TEMPLATE_ID);

		addFirstTalkId(LEONA);
		addTalkId(LEONA);
		addEnterZoneId(EVENT_TRIGGER_LEONAS_AREA);
		addInstanceEnterId(TEMPLATE_ID);
		addInstanceLeaveId(TEMPLATE_ID);
		addInstanceDestroyId(TEMPLATE_ID);

		// Register kill and spawn events for all mob types
		addKillId(MOBS_DIFFICULTY_1);
		addKillId(MOBS_DIFFICULTY_2);
		addKillId(MOBS_DIFFICULTY_3);
		addSpawnId(MOBS_DIFFICULTY_1);
		addSpawnId(MOBS_DIFFICULTY_2);
		addSpawnId(MOBS_DIFFICULTY_3);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String html;
		switch (event)
		{
			case "enterInstance":
			{
				if (!LeonasDungeonManager.getInstance().canEnterDungeon(player))
				{
					final int remaining = LeonasDungeonManager.getInstance().getRemainingEntries(player);
					player.sendMessage("你本週的進入次數已用完。剩餘次數: " + remaining + "/" + LeonasDungeonManager.MAX_WEEKLY_ENTRIES);
					return null;
				}

				if (player.isInParty())
				{
					final Party party = player.getParty();
					final List<Player> members = party.getMembers();
					for (Player member : members)
					{
						if (!member.isInsideRadius3D(npc, 1000))
						{
							player.sendMessage("玩家 " + member.getName() + " 必須靠近一些。");
						}

						if (LeonasDungeonManager.getInstance().canEnterDungeon(member))
						{
							enterInstance(member, npc, TEMPLATE_ID);
							LeonasDungeonManager.getInstance().incrementEntryCount(member);
						}
						else
						{
							member.sendMessage("你本週的進入次數已用完。");
						}
					}
				}
				else if (player.isGM())
				{
					enterInstance(player, npc, TEMPLATE_ID);
					player.sendMessage("系統：你以 GM/管理員身份進入了蕾歐娜地下城。");
				}
				else
				{
					player.sendMessage("你必須組隊才能進入。");
				}
				break;
			}
			case "setDifficulty":
			{
				final Instance instance = player.getInstanceWorld();
				final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 0);
				if (difficulty == 0)
				{
					return "setDifficulty.htm";
				}
				
				final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
				packet.setHtml(getHtm(player, "setDifficulty.htm"));
				switch (difficulty)
				{
					case 1:
					{
						packet.replace("<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h Quest LeonasDungeon setDifficulty1\">Difficulty - Low</Button>", "");
						break;
					}
					case 2:
					{
						packet.replace("<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h Quest LeonasDungeon setDifficulty2\">Difficulty - Medium</Button>", "");
						break;
					}
					case 3:
					{
						packet.replace("<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h Quest LeonasDungeon setDifficulty3\">Difficulty - High</Button>", "");
						break;
					}
				}
				
				player.sendPacket(packet);
				return null;
			}
			case "getBuff":
			{
				final Instance instance = player.getInstanceWorld();
				if (instance != null)
				{
					if ((player.getInventory().getItemByItemId(57) != null) && (player.getInventory().getItemByItemId(57).getCount() >= 50000))
					{
						if (player.destroyItemByItemId(ItemProcessType.FEE, 57, 50000, player, true))
						{
							for (SkillHolder holder : LEONA_BUFFS)
							{
								if (holder.getSkillId() == 48836) // Leona's XP Blessing
								{
									final Skill randomXpBlessing = SkillData.getInstance().getSkill(48836, Rnd.get(1, 4));
									if (randomXpBlessing != null)
									{
										randomXpBlessing.applyEffects(npc, player);
									}
								}
								else
								{
									holder.getSkill().applyEffects(npc, player);
								}
							}
						}
					}
					else
					{
						player.sendMessage("你沒有足夠的金幣。");
					}
				}
				break;
			}
			case "viewScoreFormula":
			{
				return "scoreFormula.htm";
			}
			case "setDifficulty1":
			{
				final Instance instance = player.getInstanceWorld();
				final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 0);
				instance.getParameters().set("INSTANCE_DIFFICULTY", 1);
				instance.getParameters().set("INSTANCE_DIFFICULTY_LOCK_TIME", System.currentTimeMillis() + 60000);
				instance.despawnGroup("Mobs_" + difficulty);
				
				instance.spawnGroup("MOBS_1");
				if (!instance.getParameters().getBoolean("PlayerEnter", false))
				{
					instance.setParameter("PlayerEnter", true);
					instance.setDuration(30);
					startEvent(npc, player);
				}
				break;
			}
			case "setDifficulty2":
			{
				final Instance instance = player.getInstanceWorld();
				final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 0);
				instance.getParameters().set("INSTANCE_DIFFICULTY", 2);
				instance.getParameters().set("INSTANCE_DIFFICULTY_LOCK_TIME", System.currentTimeMillis() + 60000);
				instance.despawnGroup("Mobs_" + difficulty);
				
				instance.spawnGroup("MOBS_2");
				if (!instance.getParameters().getBoolean("PlayerEnter", false))
				{
					instance.setParameter("PlayerEnter", true);
					instance.setDuration(30);
					startEvent(npc, player);
				}
				break;
			}
			case "setDifficulty3":
			{
				final Instance instance = player.getInstanceWorld();
				final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 0);
				instance.getParameters().set("INSTANCE_DIFFICULTY", 3);
				instance.getParameters().set("INSTANCE_DIFFICULTY_LOCK_TIME", System.currentTimeMillis() + 60000);
				instance.despawnGroup("Mobs_" + difficulty);
				
				instance.spawnGroup("MOBS_3");
				if (!instance.getParameters().getBoolean("PlayerEnter", false))
				{
					instance.setParameter("PlayerEnter", true);
					instance.setDuration(30);
					startEvent(npc, player);
				}
				break;
			}
			case "exitInstance":
			{
				final Instance instance = player.getInstanceWorld();
				if (instance != null)
				{
					instance.ejectPlayer(player);
				}
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance == null)
		{
			return null;
		}
		
		final long lockTime = instance.getParameters().getLong("INSTANCE_DIFFICULTY_LOCK_TIME", 0);
		if (lockTime > System.currentTimeMillis())
		{
			return "34357-03.htm";
		}
		
		final boolean instanceStarted = instance.getParameters().getBoolean("PlayerEnter", false);
		return instanceStarted ? "34357-02.htm" : "34357-01.htm";
	}
	
	private void startEvent(Npc npc, Player player)
	{
		final Instance instance = player.getInstanceWorld();
		instance.setParameter("Leona_Running", true);
		instance.setParameter("StartTime", System.currentTimeMillis());
		player.getInstanceWorld().broadcastPacket(new ExSendUIEvent(player, false, false, (int) (instance.getRemainingTime() / 1000), 0, NpcStringId.TIME_LEFT));
		instance.broadcastPacket(new OnEventTrigger(EVENT_TRIGGER_LEONAS_AREA, true));
		if (npc.getId() == LEONA)
		{
			npc.setDisplayEffect(2);
		}

		// Initialize score tracking for all players
		final Collection<Player> members = instance.getPlayers();
		for (Player member : members)
		{
			instance.getParameters().set("Player_" + member.getObjectId() + "_Score", 0);
			instance.getParameters().set("Player_" + member.getObjectId() + "_Kills", 0);
			instance.getParameters().set("Player_" + member.getObjectId() + "_Deaths", 0);
		}

		// Schedule periodic HP check and death tracking
		final ScheduledFuture<?> scheduledTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			for (Player member : instance.getPlayers())
			{
				// Track if player died
				if (member.isDead())
				{
					final int deaths = instance.getParameters().getInt("Player_" + member.getObjectId() + "_Deaths", 0);
					instance.getParameters().set("Player_" + member.getObjectId() + "_Deaths", deaths + 1);
				}
			}
		}, 5000, 5000);

		instance.setParameter("RankingPointTask", scheduledTask);
	}
	
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		final Instance instance = creature.getInstanceWorld();
		if ((instance != null) && creature.isPlayer())
		{
			FORCE_BUFF.getSkill().applyEffects(creature, creature);
		}
		else
		{
			FORCE_DEBUFF.getSkill().applyEffects(creature, creature);
		}
	}
	
	@Override
	public void onInstanceEnter(Player player, Instance instance)
	{
		final boolean running = instance.getParameters().getBoolean("Leona_Running", false);
		if ((instance.getRemainingTime() > 0) && running)
		{
			player.sendPacket(new ExSendUIEvent(player, false, false, (int) (instance.getRemainingTime() / 1000), 0, NpcStringId.TIME_LEFT));
		}
	}
	
	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		player.sendPacket(new ExSendUIEvent(player, false, false, 0, 0, NpcStringId.TIME_LEFT));

		// 清除所有效果（保留BUFF清除功能）
		player.stopAllEffects();
	}
	
	@Override
	public void onInstanceDestroy(Instance instance)
	{
		final ScheduledFuture<?> task = instance.getParameters().getObject("RankingPointTask", ScheduledFuture.class);
		if ((task != null) && !task.isDone())
		{
			task.cancel(true);
		}

		// Calculate final score for all players
		final Collection<Player> players = instance.getPlayers();
		for (Player member : players)
		{
			final int finalScore = calculateFinalScore(member, instance);
			LeonasDungeonManager.getInstance().updateBestScore(member, finalScore);
			member.sendMessage("你的最終得分: " + finalScore + " 分");
		}

		instance.setParameter("RankingPointTask", null);
	}

	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance != null)
		{
			// Increment kill count for the killer
			final int kills = instance.getParameters().getInt("Player_" + killer.getObjectId() + "_Kills", 0) + 1;
			instance.getParameters().set("Player_" + killer.getObjectId() + "_Kills", kills);

			// Add points for kill
			addScoreToPlayer(killer, instance, POINTS_PER_KILL);

			// Check tiered milestone bonuses (階梯式獎勵)
			if (kills >= 1000)
			{
				// 檢查是否剛好達到100的整數倍
				if ((kills % 100) == 0)
				{
					final int tier = kills / 1000; // 1000-1999=1, 2000-2999=2, etc.
					final int bonusPoints = 5 + (tier - 1) * 2; // 1000檔:5分, 2000檔:7分, 3000檔:9分
					addScoreToPlayer(killer, instance, bonusPoints);
					killer.sendMessage("擊殺里程碑！擊殺 " + kills + " 隻怪物，獲得 " + bonusPoints + " 分獎勵！");
				}
			}

			// Track mob for respawn with increased power
			final int mobRespawnCount = instance.getParameters().getInt("Mob_" + npc.getId() + "_RespawnCount", 0);
			instance.getParameters().set("Mob_" + npc.getId() + "_RespawnCount", mobRespawnCount + 1);
		}
	}

	@Override
	public void onSpawn(Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance != null)
		{
			// Apply power increase based on respawn count
			final int respawnCount = instance.getParameters().getInt("Mob_" + npc.getId() + "_RespawnCount", 0);
			if (respawnCount > 0)
			{
				final double powerMultiplier = Math.pow(MOB_POWER_INCREASE, respawnCount);

				// Apply HP and MP multiplier
				npc.setCurrentHp(npc.getMaxHp() * powerMultiplier);
				npc.setCurrentMp(npc.getMaxMp() * powerMultiplier);

				// Apply stat multipliers using new source code methods
				npc.setPhysicalAttackMultiplier(powerMultiplier);
				npc.setMagicalAttackMultiplier(powerMultiplier);
				npc.setPhysicalDefenseMultiplier(powerMultiplier);
				npc.setMagicalDefenseMultiplier(powerMultiplier);
				npc.setAttackSpeedMultiplier(powerMultiplier);
				npc.setCastSpeedMultiplier(powerMultiplier);
			}
		}
	}

	private void addScoreToPlayer(Player player, Instance instance, int points)
	{
		final int currentScore = instance.getParameters().getInt("Player_" + player.getObjectId() + "_Score", 0);
		instance.getParameters().set("Player_" + player.getObjectId() + "_Score", currentScore + points);
	}

	private int calculateFinalScore(Player player, Instance instance)
	{
		int finalScore = instance.getParameters().getInt("Player_" + player.getObjectId() + "_Score", 0);

		// Add time-based points based on difficulty
		final int difficulty = instance.getParameters().getInt("INSTANCE_DIFFICULTY", 1);
		final long timeInMinutes = (30 * 60000 - instance.getRemainingTime()) / 60000;
		int pointsPerMinute = POINTS_PER_MINUTE_LOW;
		if (difficulty == 2)
		{
			pointsPerMinute = POINTS_PER_MINUTE_MED;
		}
		else if (difficulty == 3)
		{
			pointsPerMinute = POINTS_PER_MINUTE_HIGH;
		}
		finalScore += (int) (timeInMinutes * pointsPerMinute);

		// Deduct points for deaths
		final int deaths = instance.getParameters().getInt("Player_" + player.getObjectId() + "_Deaths", 0);
		finalScore -= deaths * POINTS_DEDUCT_PER_DEATH;

		// Deduct points if HP is too low at the end
		final double hpPercent = (player.getCurrentHp() / player.getMaxHp()) * 100;
		if (hpPercent < POINTS_DEDUCT_HP_THRESHOLD)
		{
			finalScore -= POINTS_DEDUCT_LOW_HP;
		}

		return Math.max(0, finalScore);
	}
	
	public static void main(String[] args)
	{
		new LeonasDungeon();
	}
}
