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
package instances.TimedHunting;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.AttackableAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.TimedHuntingZoneData;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeClientEffectInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.huntingzones.TimedHuntingZoneExit;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * @author Berezkin Nikolay, Mobius
 */
public class TimedHunting extends InstanceScript
{
	// NPCs (on official server random pick of NPC's)
	private static final int KATE = 34120; // Dragon Valley 80-99
	private static final int DEEKHIN = 34121; // Cemetery 50-59
	private static final int BUNCH = 34122; // Giant's Cave 90+
	private static final int AYAN = 34123; // Sel Mahum Base 85-99
	private static final int JOON = 34124; // Sea of Spores 40-49
	private static final int PANJI = 34125; // Plains of Glory 60-69
	private static final int DEBBIE = 34126; // War-Torn Plains 70-79
	private static final int[] GUARDIANS =
	{
		22292,
		22331,
		22332,
		22333,
		22334,
		22335,
		22424
	};
	
	// Skill
	private static final int BUFF = 45197;
	private static final int BUFF_FOR_KAMAEL = 45198;
	private static final int BUFF_FOR_WARG = 52050;
	private static final int WARG_ANCIENT_POWER = 88460;
	
	// Rewards
	private static final int SPIRIT_ORE_ID = 3031;
	private static final int SOULSHOT_TICKET_ID = 90907;
	private static final int SAYHAS_COOKIE_ID = 93274;
	
	// Misc
	private static final Location MANAGER_SPAWN_LOCATION = new Location(-86105, -113905, -10614, 41760);
	private static final ExChangeClientEffectInfo[] ZONE_EFFECTS =
	{
		ExChangeClientEffectInfo.TRANSCEDENT_GIRAN,
		ExChangeClientEffectInfo.TRANSCEDENT_ORC,
		ExChangeClientEffectInfo.TRANSCEDENT_DWARF,
		ExChangeClientEffectInfo.TRANSCEDENT_DARK_ELF,
		ExChangeClientEffectInfo.TRANSCEDENT_GRACIA_AIRSTRIP,
		ExChangeClientEffectInfo.TRANSCEDENT_ELF,
		ExChangeClientEffectInfo.TRANSCEDENT_ERTHEIA,
		ExChangeClientEffectInfo.TRANSCEDENT_HIGH_ELF,
		ExChangeClientEffectInfo.TRANSCEDENT_SKY_TOWER
	};
	private static final int[] TEMPLATES =
	{
		229, // Transcendent Instance Zone
	};
	private static final Map<Integer, Integer> SKILL_REPLACEMENTS = new HashMap<>();
	static
	{
		SKILL_REPLACEMENTS.put(3, 45199); // Power Strike
		SKILL_REPLACEMENTS.put(16, 45200); // Mortal Blow
		SKILL_REPLACEMENTS.put(56, 45201); // Power Shot
		SKILL_REPLACEMENTS.put(29, 45202); // Iron Punch
		SKILL_REPLACEMENTS.put(261, 45204); // Triple Slash
		SKILL_REPLACEMENTS.put(19, 45205); // Double Shot
		SKILL_REPLACEMENTS.put(190, 45206); // Fatal Strike
		SKILL_REPLACEMENTS.put(263, 45207); // Deadly Blow
		SKILL_REPLACEMENTS.put(280, 45208); // Burning Fist
		SKILL_REPLACEMENTS.put(284, 45209); // Hurricane Assault
		SKILL_REPLACEMENTS.put(343, 45210); // Lethal Shot
		SKILL_REPLACEMENTS.put(344, 45211); // Lethal Blow
		SKILL_REPLACEMENTS.put(400, 45212); // Aqua Strike
		SKILL_REPLACEMENTS.put(401, 45213); // Abyss Strike
		SKILL_REPLACEMENTS.put(984, 45215); // Shield Strike
		SKILL_REPLACEMENTS.put(1632, 45216); // Deadly Strike
		SKILL_REPLACEMENTS.put(45187, 45217); // Guard Crush
		SKILL_REPLACEMENTS.put(1230, 45218); // Prominence
		SKILL_REPLACEMENTS.put(1235, 45219); // Hydro Blast
		SKILL_REPLACEMENTS.put(1239, 45220); // Hurricane
		SKILL_REPLACEMENTS.put(1220, 45221); // Blaze
		SKILL_REPLACEMENTS.put(1175, 45222); // Aqua Swirl
		SKILL_REPLACEMENTS.put(1178, 45223); // Twister
		SKILL_REPLACEMENTS.put(1028, 45224); // Might of Heaven
		SKILL_REPLACEMENTS.put(1245, 45225); // Steal Essence
		SKILL_REPLACEMENTS.put(45155, 45227); // Soul Impulse
		SKILL_REPLACEMENTS.put(45161, 45228); // Soul Piercing
		SKILL_REPLACEMENTS.put(45163, 45229); // Soul Spark
		SKILL_REPLACEMENTS.put(45168, 45230); // Twin Shot
		SKILL_REPLACEMENTS.put(1148, 45231); // Death Spike
		SKILL_REPLACEMENTS.put(1031, 45261); // Divine Strike
		SKILL_REPLACEMENTS.put(45241, 45262); // Divine Beam
		SKILL_REPLACEMENTS.put(1090, 45265); // Life Drain
		SKILL_REPLACEMENTS.put(777, 45266); // Demolition Impact
		SKILL_REPLACEMENTS.put(45249, 45267); // Earth Tremor
		SKILL_REPLACEMENTS.put(348, 45268); // Spoil Crush
		SKILL_REPLACEMENTS.put(45303, 45360); // Wipeout
		SKILL_REPLACEMENTS.put(36, 45386); // Vortex
		SKILL_REPLACEMENTS.put(45402, 45397); // Frantic Pace
		SKILL_REPLACEMENTS.put(47011, 47015); // Freezing Wound
		SKILL_REPLACEMENTS.put(47002, 47095); // Dual Blow
		SKILL_REPLACEMENTS.put(47005, 47095); // Triple Blow
		SKILL_REPLACEMENTS.put(47279, 47434); // Knight's Assault
		SKILL_REPLACEMENTS.put(45377, 47435); // Ethereal Strike
		SKILL_REPLACEMENTS.put(45378, 47436); // Flame Explosion
		SKILL_REPLACEMENTS.put(45379, 47437); // Water Explosion
		SKILL_REPLACEMENTS.put(45380, 47438); // Thunder Explosion
		SKILL_REPLACEMENTS.put(45381, 47439); // Void Explosion
		SKILL_REPLACEMENTS.put(45301, 47470); // Punishment
		SKILL_REPLACEMENTS.put(47801, 47891); // Piercing
		SKILL_REPLACEMENTS.put(47802, 47892); // Amazing Piercing
		SKILL_REPLACEMENTS.put(47805, 47893); // Wild Scratch
		SKILL_REPLACEMENTS.put(47806, 47893); // Shadow Scratch
		SKILL_REPLACEMENTS.put(921, 47998); // Spike Thrust
		SKILL_REPLACEMENTS.put(87006, 87018); // Fatal Crush
		SKILL_REPLACEMENTS.put(87301, 87316); // Blow
		SKILL_REPLACEMENTS.put(87310, 87316); // Enhanced Blow
		SKILL_REPLACEMENTS.put(87801, 87831); // Sacral Strike
		SKILL_REPLACEMENTS.put(87701, 87776); // Fire Sphere
		SKILL_REPLACEMENTS.put(87702, 87777); // Ice Sphere
		SKILL_REPLACEMENTS.put(87705, 87778); // Bright Dance
		SKILL_REPLACEMENTS.put(87951, 87831); // Noblesse Sacral Strike
		SKILL_REPLACEMENTS.put(87954, 87834); // Noblesse Purify
	}
	
	private enum KeeperType
	{
		SPIRIT_ORE(NpcStringId.ENHANCED_WITH_SPIRIT_ORE),
		SOULSHOT(NpcStringId.ENHANCED_WITH_SPIRIT),
		GRACE(NpcStringId.ENHANCED_WITH_GRACE),
		SUPPLY(NpcStringId.ENHANCED_WITH_SUPPLIES);
		
		private final NpcStringId _npcStringId;
		
		KeeperType(NpcStringId title)
		{
			_npcStringId = title;
		}
		
		public NpcStringId getTitle()
		{
			return _npcStringId;
		}
	}
	
	public TimedHunting()
	{
		super(TEMPLATES);
		addFirstTalkId(KATE, DEEKHIN, BUNCH, AYAN, JOON, PANJI, DEBBIE);
		addKillId(GUARDIANS);
		addInstanceLeaveId(TEMPLATES);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("ENTER"))
		{
			final int zoneId = Integer.parseInt(event.split(" ")[1]);
			final TimedHuntingZoneHolder huntingZone = TimedHuntingZoneData.getInstance().getHuntingZone(zoneId);
			if (huntingZone == null)
			{
				return null;
			}
			
			if (huntingZone.isSoloInstance())
			{
				enterInstance(player, npc, huntingZone.getInstanceId());
			}
			else
			{
				Instance world = null;
				for (Instance instance : InstanceManager.getInstance().getInstances())
				{
					if (instance.getTemplateId() == huntingZone.getInstanceId())
					{
						world = instance;
						break;
					}
				}
				
				if (world == null)
				{
					world = InstanceManager.getInstance().createInstance(huntingZone.getInstanceId(), player);
				}
				
				player.teleToLocation(huntingZone.getEnterLocation(), world);
			}
		}
		else if (event.startsWith("FINISH"))
		{
			final Instance world = player.getInstanceWorld();
			if ((world != null) && ArrayUtil.contains(TEMPLATES, world.getTemplateId()))
			{
				world.setReenterTime();
				world.destroy();
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = player.getInstanceWorld();
		if (world.getParameters().getBoolean("TimedHuntingTaskFinished", false))
		{
			return npc.getId() + "-finished.html";
		}
		
		if (!world.getParameters().getBoolean("PlayerEnter", false))
		{
			world.setParameter("PlayerEnter", true);
			world.setDuration(10);
			
			player.sendPacket(getRandomEntry(ZONE_EFFECTS));
			replaceNormalSkills(player);
			startEvent(player);
		}
		
		npc.setTarget(player);
		if (player.getRace() == Race.KAMAEL)
		{
			if (!player.getEffectList().isAffectedBySkill(BUFF_FOR_KAMAEL))
			{
				npc.doCast(new SkillHolder(BUFF_FOR_KAMAEL, 1).getSkill());
			}
		}
		else if (player.isWarg())
		{
			if (!player.getEffectList().isAffectedBySkill(BUFF_FOR_WARG))
			{
				if (player.getEffectList().isAffectedBySkill(WARG_ANCIENT_POWER))
				{
					npc.doCast(new SkillHolder(BUFF_FOR_WARG, 2).getSkill());
				}
				else
				{
					npc.doCast(new SkillHolder(BUFF_FOR_WARG, 1).getSkill());
				}
			}
		}
		else if (!player.getEffectList().isAffectedBySkill(BUFF))
		{
			npc.doCast(new SkillHolder(BUFF, 1).getSkill());
		}
		
		String content = HtmCache.getInstance().getHtm(player, "data/scripts/instances/TimedHunting/" + npc.getId() + ".html");
		content = content.replace("%playerClass%", ClassListData.getInstance().getClass(player.getPlayerClass()).getClassName());
		content = content.replace("%replacedSkill%", getReplacedSkillNames(player));
		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setHtml(content);
		player.sendPacket(msg);
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		if ((instance == null) || !instance.getParameters().contains("KeeperType"))
		{
			return;
		}
		
		giveGuardianReward(killer, KeeperType.valueOf(instance.getParameters().getString("KeeperType", "")));
	}
	
	/**
	 * Gives a guardian reward to the specified player. Each reward type can be obtained only once.
	 * @param killer the player who killed the guardian
	 * @param type the type of guardian reward to give
	 */
	private void giveGuardianReward(Player killer, KeeperType type)
	{
		final Instance instance = killer.getInstanceWorld();
		final Map<Integer, Integer> rewardMap = instance.getParameters().getIntegerMap("GuardianReward");
		if (rewardMap.containsKey(type.ordinal()))
		{
			return;
		}
		
		switch (type)
		{
			case SPIRIT_ORE:
			{
				killer.addItem(ItemProcessType.REWARD, SPIRIT_ORE_ID, getRandomBoolean() ? getRandomBoolean() ? 3000 : 2000 : 1500, null, true);
				break;
			}
			case SOULSHOT:
			{
				killer.addItem(ItemProcessType.REWARD, SOULSHOT_TICKET_ID, getRandomBoolean() ? getRandomBoolean() ? 50 : 35 : 30, null, true);
				break;
			}
			case GRACE:
			{
				killer.addItem(ItemProcessType.REWARD, SAYHAS_COOKIE_ID, getRandomBoolean() ? getRandomBoolean() ? 300 : 230 : 180, null, true);
				break;
			}
			case SUPPLY:
			{
				killer.addItem(ItemProcessType.REWARD, SPIRIT_ORE_ID, 650, null, true);
				killer.addItem(ItemProcessType.REWARD, SOULSHOT_TICKET_ID, 12, null, true);
				killer.addItem(ItemProcessType.REWARD, SAYHAS_COOKIE_ID, 75, null, true);
				break;
			}
		}
		
		rewardMap.putIfAbsent(type.ordinal(), 1);
		instance.getParameters().setIntegerMap("GuardianReward", rewardMap);
	}
	
	@Override
	protected void onEnter(Player player, Instance instance, boolean firstEnter)
	{
		super.onEnter(player, instance, firstEnter);
		
		instance.setParameter("PlayerIsOut", false);
		if (!firstEnter)
		{
			if (instance.getTemplateId() != 228 /* Training Zone */)
			{
				replaceNormalSkills(player);
				startEvent(player);
			}
		}
		else if (instance.getTemplateId() == 229 /* Transcendent Instance Zone */) // Spawn manager.
		{
			addSpawn(getRandom(34120, 34126), MANAGER_SPAWN_LOCATION, false, 0, false, instance.getId());
		}
	}
	
	private void replaceNormalSkills(Player player)
	{
		if (player.getInstanceWorld().getTemplateId() == 228) // Training Zone.
		{
			return;
		}
		
		// Replace normal skills.
		for (Entry<Integer, Integer> entry : SKILL_REPLACEMENTS.entrySet())
		{
			final Integer normalSkillId = entry.getKey();
			final Skill knownSkill = player.getKnownSkill(normalSkillId);
			if (knownSkill == null)
			{
				continue;
			}
			
			final Integer transcendentSkillId = entry.getValue();
			player.addSkill(SkillData.getInstance().getSkill(transcendentSkillId, knownSkill.getLevel(), knownSkill.getSubLevel()), false);
			player.addReplacedSkill(normalSkillId, transcendentSkillId);
		}
	}
	
	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		if (instance.getParameters().getBoolean("TimedHuntingTaskFinished", false))
		{
			instance.setParameter("TimedHuntingTaskFinished", false);
		}
		
		player.sendPacket(new ExSendUIEvent(player, true, false, 600, 0, NpcStringId.TIME_LEFT));
		player.sendPacket(new TimedHuntingZoneExit(player.getVariables().getInt(PlayerVariables.LAST_HUNTING_ZONE_ID, 0)));
		
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, BUFF);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, BUFF_FOR_KAMAEL);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, BUFF_FOR_WARG);
		instance.setParameter("PlayerIsOut", true);
		
		// Restore normal skills.
		for (Entry<Integer, Integer> entry : SKILL_REPLACEMENTS.entrySet())
		{
			final Integer transcendentSkillId = entry.getValue();
			final Skill knownSkill = player.getKnownSkill(transcendentSkillId);
			if (knownSkill == null)
			{
				continue;
			}
			
			final Integer normalSkillId = entry.getKey();
			player.addSkill(SkillData.getInstance().getSkill(normalSkillId, knownSkill.getLevel(), knownSkill.getSubLevel()), false);
			player.removeReplacedSkill(normalSkillId);
		}

	}
	
	private String getReplacedSkillNames(Player player)
	{
		int count = 0;
		final StringBuilder sb = new StringBuilder();
		for (int transcendentSkillId : SKILL_REPLACEMENTS.values())
		{
			final Skill knownSkill = player.getKnownSkill(transcendentSkillId);
			if (knownSkill == null)
			{
				continue;
			}
			
			if (count > 0)
			{
				sb.append(", ");
			}
			
			count++;
			
			sb.append(knownSkill.getName());
		}
		
		if (count > 1)
		{
			sb.append(".");
		}
		
		return sb.toString();
	}
	
	private void startEvent(Player player)
	{
		// Start instance tasks.
		if (!player.getInstanceWorld().getParameters().getBoolean("TimedHuntingTaskFinished", false))
		{
			final Instance instance = player.getInstanceWorld();
			player.sendPacket(new ExSendUIEvent(player, false, false, Math.min(600, (int) (instance.getRemainingTime() / 1000)), 0, NpcStringId.TIME_LEFT));
			showSkillMessage(player);
			
			final ScheduledFuture<?> spawnTask = ThreadPool.scheduleAtFixedRate(() ->
			{
				if (!instance.getParameters().getBoolean("PlayerIsOut", false) && (instance.getAliveNpcCount() == 1))
				{
					if (getRandom(5) == 0)
					{
						spawnTreasures(player);
					}
					else
					{
						if (getRandom(3) == 0)
						{
							spawnTreasures(player);
						}
						
						if (getRandom(7) == 0)
						{
							spawnGuardian(player);
						}
						
						spawnMonsters(player);
					}
				}
			}, 10000, 5000); // Initial delay 10 seconds. Repeat every 0 to 5 seconds.

			long cleanupDelay = instance.getRemainingTime() - 30000;
			if (cleanupDelay > 0)
			{
				ThreadPool.schedule(() ->
				{
					instance.getNpcs().stream().filter(WorldObject::isAttackable).forEach(Npc::deleteMe);
					instance.getParameters().set("TimedHuntingTaskFinished", true);
					if (spawnTask != null)
					{
						spawnTask.cancel(false);
					}
				}, cleanupDelay);
			}

			long finishDelay = instance.getRemainingTime();
			if (finishDelay > 0)
			{
				ThreadPool.schedule(instance::finishInstance, finishDelay);
			}
		}
	}
	
	protected void showSkillMessage(Player player)
	{
		final NpcStringId npcStringId;
		switch (player.getPlayerClass())
		{
			case GLADIATOR:
			{
				npcStringId = NpcStringId.GLADIATOR_TRANSCENDENT_SKILL_SLASHING_BLADE;
				break;
			}
			case WARLORD:
			{
				npcStringId = NpcStringId.WARLORD_TRANSCENDENT_SKILL_FATAL_STRIKE;
				break;
			}
			case PALADIN:
			{
				npcStringId = NpcStringId.PALADIN_TRANSCENDENT_SKILL_SHIELD_STRIKE;
				break;
			}
			case DARK_AVENGER:
			{
				npcStringId = NpcStringId.DARK_AVENGER_TRANSCENDENT_SKILL_SHIELD_STRIKE;
				break;
			}
			case TREASURE_HUNTER:
			{
				npcStringId = NpcStringId.TREASURE_HUNTER_TRANSCENDENT_SKILL_DEADLY_BLOW;
				break;
			}
			case HAWKEYE:
			{
				npcStringId = NpcStringId.ARCHER_TRANSCENDENT_SKILL_DOUBLE_SHOT;
				break;
			}
			case SORCERER:
			{
				npcStringId = NpcStringId.SORCERER_TRANSCENDENT_SKILL_PROMINENCE;
				break;
			}
			case NECROMANCER:
			{
				npcStringId = NpcStringId.NECROMANCER_TRANSCENDENT_SKILL_DEATH_SPIKE;
				break;
			}
			case WARLOCK:
			{
				npcStringId = NpcStringId.WARLOCK_TRANSCENDENT_SKILL_BLAZE;
				break;
			}
			case BISHOP:
			{
				npcStringId = NpcStringId.BISHOP_TRANSCENDENT_SKILL_MIGHT_OF_HEAVEN;
				break;
			}
			case PROPHET:
			{
				npcStringId = NpcStringId.PROPHET_TRANSCENDENT_SKILL_MIGHT_OF_HEAVEN;
				break;
			}
			case TEMPLE_KNIGHT:
			{
				npcStringId = NpcStringId.TEMPLE_KNIGHT_TRANSCENDENT_SKILL_AQUA_STRIKE;
				break;
			}
			case SWORDSINGER:
			{
				npcStringId = NpcStringId.SWORDSINGER_TRANSCENDENT_SKILL_GUARD_CRUSH;
				break;
			}
			case PLAINS_WALKER:
			{
				npcStringId = NpcStringId.PLAINS_WALKER_TRANSCENDENT_SKILL_DEADLY_BLOW;
				break;
			}
			case SILVER_RANGER:
			{
				npcStringId = NpcStringId.SILVER_RANGER_TRANSCENDENT_SKILL_DOUBLE_SHOT;
				break;
			}
			case SPELLSINGER:
			{
				npcStringId = NpcStringId.SPELLSINGER_TRANSCENDENT_SKILL_HYDRO_BLAST;
				break;
			}
			case ELEMENTAL_SUMMONER:
			{
				npcStringId = NpcStringId.ELEMENTAL_SUMMONER_TRANSCENDENT_SKILL_ELEMENTAL_DISCHARGE;
				break;
			}
			case ELDER:
			{
				npcStringId = NpcStringId.ELVEN_ELDER_TRANSCENDENT_SKILL_MIGHT_OF_HEAVEN;
				break;
			}
			case SHILLIEN_KNIGHT:
			{
				npcStringId = NpcStringId.SHILLIEN_KNIGHT_TRANSCENDENT_SKILL_ABYSS_STRIKE;
				break;
			}
			case BLADEDANCER:
			{
				npcStringId = NpcStringId.BLADEDANCER_TRANSCENDENT_SKILL_GUARD_CRUSH;
				break;
			}
			case ABYSS_WALKER:
			{
				npcStringId = NpcStringId.ABYSS_WALKER_TRANSCENDENT_SKILL_DEADLY_BLOW;
				break;
			}
			case PHANTOM_RANGER:
			{
				npcStringId = NpcStringId.PHANTOM_RANGER_TRANSCENDENT_SKILL_DOUBLE_SHOT;
				break;
			}
			case SPELLHOWLER:
			{
				npcStringId = NpcStringId.SPELLHOWLER_TRANSCENDENT_SKILL_HURRICANE;
				break;
			}
			case PHANTOM_SUMMONER:
			{
				npcStringId = NpcStringId.PHANTOM_SUMMONER_TRANSCENDENT_SKILL_TWISTER;
				break;
			}
			case SHILLIEN_ELDER:
			{
				npcStringId = NpcStringId.SHILLIEN_ELDER_TRANSCENDENT_SKILL_MIGHT_OF_HEAVEN;
				break;
			}
			case DESTROYER:
			{
				npcStringId = NpcStringId.DESTROYER_TRANSCENDENT_SKILL_FATAL_STRIKE;
				break;
			}
			case TYRANT:
			{
				npcStringId = NpcStringId.TYRANT_TRANSCENDENT_SKILL_HURRICANE_ASSAULT;
				break;
			}
			case OVERLORD:
			{
				npcStringId = NpcStringId.OVERLORD_TRANSCENDENT_SKILLS_FATAL_STRIKE_VORTEX_OF_FIRE;
				break;
			}
			case WARCRYER:
			{
				npcStringId = NpcStringId.WARCRYER_TRANSCENDENT_SKILLS_FATAL_STRIKE_VORTEX_OF_FIRE;
				break;
			}
			case BOUNTY_HUNTER:
			{
				npcStringId = NpcStringId.BOUNTY_HUNTER_TRANSCENDENT_SKILL_FATAL_STRIKE;
				break;
			}
			case WARSMITH:
			{
				npcStringId = NpcStringId.WARSMITH_TRANSCENDENT_SKILL_FATAL_STRIKE;
				break;
			}
			case BERSERKER:
			{
				npcStringId = NpcStringId.BERSERKER_TRANSCENDENT_SKILL_SOUL_IMPULSE;
				break;
			}
			case SOUL_RANGER:
			{
				npcStringId = NpcStringId.SOUL_RANGER_TRANSCENDENT_SKILL_TWIN_SHOT;
				break;
			}
			case SOUL_BREAKER:
			{
				npcStringId = NpcStringId.SOUL_BREAKER_TRANSCENDENT_SKILLS_SOUL_PIERCING_SOUL_SPARK;
				break;
			}
			case DUELIST:
			{
				npcStringId = NpcStringId.DUELIST_TRANSCENDENT_SKILL_SLASHING_BLADE;
				break;
			}
			case DREADNOUGHT:
			{
				npcStringId = NpcStringId.DREADNOUGHT_TRANSCENDENT_SKILLS_FATAL_STRIKE_SPIKE_THRUST;
				break;
			}
			case PHOENIX_KNIGHT:
			{
				npcStringId = NpcStringId.PHOENIX_KNIGHT_TRANSCENDENT_SKILLS_SHIELD_STRIKE_KNIGHT_S_ASSAULT;
				break;
			}
			case HELL_KNIGHT:
			{
				npcStringId = NpcStringId.HELL_KNIGHT_TRANSCENDENT_SKILLS_SHIELD_STRIKE_KNIGHT_S_ASSAULT;
				break;
			}
			case ADVENTURER:
			{
				npcStringId = NpcStringId.ADVENTURER_TRANSCENDENT_SKILLS_LETHAL_BLOW_DARK_BLOW;
				break;
			}
			case SAGITTARIUS:
			{
				npcStringId = NpcStringId.SAGITTARIUS_TRANSCENDENT_SKILLS_DOUBLE_SHOT_LETHAL_SHOT_TRIPLE_SHOT;
				break;
			}
			case ARCHMAGE:
			{
				npcStringId = NpcStringId.ARCHMAGE_TRANSCENDENT_SKILLS_PROMINENCE_FLAME_EXPLOSION;
				break;
			}
			case SOULTAKER:
			{
				npcStringId = NpcStringId.SOULTAKER_TRANSCENDENT_SKILLS_DEATH_SPIKE_VOID_EXPLOSION;
				break;
			}
			case ARCANA_LORD:
			{
				npcStringId = NpcStringId.ARCANA_LORD_TRANSCENDENT_SKILLS_BLAZE_ETHEREAL_STRIKE;
				break;
			}
			case CARDINAL:
			{
				npcStringId = NpcStringId.CARDINAL_TRANSCENDENT_SKILLS_MIGHT_OF_HEAVEN_SHINING_BEAM;
				break;
			}
			case HIEROPHANT:
			{
				npcStringId = NpcStringId.HIEROPHANT_TRANSCENDENT_SKILLS_MIGHT_OF_HEAVEN_DIVINE_BEAM_FATAL_CRUSH;
				break;
			}
			case EVA_TEMPLAR:
			{
				npcStringId = NpcStringId.EVA_S_TEMPLAR_TRANSCENDENT_SKILLS_AQUA_STRIKE_TEMPLAR_S_ASSAULT;
				break;
			}
			case SWORD_MUSE:
			{
				npcStringId = NpcStringId.SWORD_MUSE_TRANSCENDENT_SKILLS_DEADLY_STRIKE_FRANTIC_PACE;
				break;
			}
			case WIND_RIDER:
			{
				npcStringId = NpcStringId.WIND_RIDER_TRANSCENDENT_SKILLS_LETHAL_BLOW_DARK_BLOW;
				break;
			}
			case MOONLIGHT_SENTINEL:
			{
				npcStringId = NpcStringId.MOONLIGHT_SENTINEL_TRANSCENDENT_SKILLS_DOUBLE_SHOT_LETHAL_SHOT_TRIPLE_SHOT;
				break;
			}
			case MYSTIC_MUSE:
			{
				npcStringId = NpcStringId.MYSTIC_MUSE_TRANSCENDENT_SKILLS_HYDRO_BLAST_MYSTIC_EXPLOSION;
				break;
			}
			case ELEMENTAL_MASTER:
			{
				npcStringId = NpcStringId.ELEMENTAL_MASTER_TRANSCENDENT_SKILLS_ELEMENTAL_DISCHARGE_ELEMENTAL_STRIKE;
				break;
			}
			case EVA_SAINT:
			{
				npcStringId = NpcStringId.EVA_S_SAINT_TRANSCENDENT_SKILLS_MIGHT_OF_HEAVEN_DIVINE_BEAM;
				break;
			}
			case SHILLIEN_TEMPLAR:
			{
				npcStringId = NpcStringId.SHILLIEN_TEMPLAR_TRANSCENDENT_SKILLS_ABYSS_STRIKE_TEMPLAR_S_ASSAULT;
				break;
			}
			case SPECTRAL_DANCER:
			{
				npcStringId = NpcStringId.SPECTRAL_DANCER_TRANSCENDENT_SKILLS_DEADLY_STRIKE_FRANTIC_PACE;
				break;
			}
			case GHOST_HUNTER:
			{
				npcStringId = NpcStringId.GHOST_HUNTER_TRANSCENDENT_SKILLS_LETHAL_BLOW_DARK_BLOW;
				break;
			}
			case GHOST_SENTINEL:
			{
				npcStringId = NpcStringId.GHOST_SENTINEL_TRANSCENDENT_SKILLS_DOUBLE_SHOT_LETHAL_SHOT_TRIPLE_SHOT;
				break;
			}
			case STORM_SCREAMER:
			{
				npcStringId = NpcStringId.STORM_SCREAMER_TRANSCENDENT_SKILLS_HURRICANE_THUNDER_EXPLOSION;
				break;
			}
			case SPECTRAL_MASTER:
			{
				npcStringId = NpcStringId.SPECTRAL_MASTER_TRANSCENDENT_SKILLS_TWISTER_ETHEREAL_STRIKE;
				break;
			}
			case SHILLIEN_SAINT:
			{
				npcStringId = NpcStringId.SHILLIEN_SAINT_TRANSCENDENT_SKILLS_MIGHT_OF_HEAVEN_DIVINE_BEAM;
				break;
			}
			case TITAN:
			{
				npcStringId = NpcStringId.TITAN_TRANSCENDENT_SKILLS_FATAL_STRIKE_DEMOLITION_IMPACT;
				break;
			}
			case GRAND_KHAVATARI:
			{
				npcStringId = NpcStringId.GRAND_KHAVATARI_TRANSCENDENT_SKILLS_HURRICANE_ASSAULT_DOUBLE_HURRICANE_ASSAULT_BURNING_ASSAULT;
				break;
			}
			case DOMINATOR:
			{
				npcStringId = NpcStringId.DOMINATOR_TRANSCENDENT_SKILLS_FATAL_STRIKE_VORTEX_OF_FIRE_BLOODY_STRIKE;
				break;
			}
			case DOOMCRYER:
			{
				npcStringId = NpcStringId.DOOMCRYER_TRANSCENDENT_SKILLS_FATAL_STRIKE_VORTEX_OF_FIRE_BLOODY_STRIKE;
				break;
			}
			case FORTUNE_SEEKER:
			{
				npcStringId = NpcStringId.FORTUNE_SEEKER_TRANSCENDENT_SKILLS_FATAL_STRIKE_SPOIL_CRUSH;
				break;
			}
			case MAESTRO:
			{
				npcStringId = NpcStringId.MAESTRO_TRANSCENDENT_SKILLS_FATAL_STRIKE_EARTH_TREMOR_HERO_S_ATTACK;
				break;
			}
			case DOOMBRINGER:
			{
				npcStringId = NpcStringId.DOOMBRINGER_TRANSCENDENT_SKILLS_SOUL_IMPULSE_SOUL_SMASH;
				break;
			}
			case TRICKSTER:
			{
				npcStringId = NpcStringId.TRICKSTER_TRANSCENDENT_SKILLS_TWIN_SHOT_MULTISHOT;
				break;
			}
			case SOUL_HOUND:
			{
				npcStringId = NpcStringId.SOUL_HOUND_TRANSCENDENT_SKILLS_SOUL_PIERCING_SOUL_SPARK;
				break;
			}
			case DEATH_MESSENGER_HUMAN:
			case DEATH_MESSENGER_ELF:
			case DEATH_MESSENGER_DARK_ELF:
			{
				npcStringId = NpcStringId.DEATH_MESSENGER_TRANSCENDENT_SKILLS_WIPEOUT_PUNISHMENT;
				break;
			}
			case DEATH_KIGHT_HUMAN:
			case DEATH_KIGHT_ELF:
			case DEATH_KIGHT_DARK_ELF:
			{
				npcStringId = NpcStringId.DEATH_KNIGHT_TRANSCENDENT_SKILLS_WIPEOUT_PUNISHMENT;
				break;
			}
			case WIND_SNIPER:
			{
				npcStringId = NpcStringId.WIND_SNIPER_TRANSCENDENT_SKILLS_DUAL_BLOW_FREEZING_WOUND;
				break;
			}
			case STORM_BLASTER:
			{
				npcStringId = NpcStringId.STORM_BLASTER_TRANSCENDENT_SKILLS_TRIPLE_BLOW_FREEZING_WOUND_GREATER_WIND_SHOT;
				break;
			}
			case DRAGOON:
			{
				npcStringId = NpcStringId.DRAGOON_TRANSCENDENT_SKILLS_PIERCING_WILD_SCRATCH;
				break;
			}
			case VANGUARD_RIDER:
			{
				npcStringId = NpcStringId.VANGUARD_RIDER_TRANSCENDENT_SKILLS_AMAZING_PIERCING_SHADOW_SCRATCH;
				break;
			}
			case ASSASSIN_FEMALE_2:
			case ASSASSIN_MALE_2:
			{
				npcStringId = NpcStringId.ASSASSIN_TRANSCENDENT_SKILLS_BLOW_RELENTLESS_ATTACK;
				break;
			}
			case ASSASSIN_FEMALE_3:
			case ASSASSIN_MALE_3:
			{
				npcStringId = NpcStringId.ASSASSIN_TRANSCENDENT_SKILLS_BLOW_RELENTLESS_ATTACK_2;
				break;
			}
			case ELEMENT_WEAVER_2:
			{
				npcStringId = NpcStringId.ELEMENT_WEAVER_TRANSCENDENT_SKILLS_FIRE_SPHERE_ICE_SPHERE;
				break;
			}
			case ELEMENT_WEAVER_3:
			{
				npcStringId = NpcStringId.ELEMENT_WEAVER_TRANSCENDENT_SKILLS_FIRE_SPHERE_ICE_SPHERE_2;
				break;
			}
			case DIVINE_TEMPLAR_2:
			{
				npcStringId = NpcStringId.DIVINE_TEMPLAR_TRANSCENDENT_SKILLS_SACRAL_STRIKE_PURIFY;
				break;
			}
			case DIVINE_TEMPLAR_3:
			{
				npcStringId = NpcStringId.DIVINE_TEMPLAR_TRANSCENDENT_SKILLS_SACRAL_STRIKE_PURIFY_2;
				break;
			}
			case WARG_2:
			{
				npcStringId = NpcStringId.WARG_TRANSCENDENT_SKILLS_DOUBLE_CLAW_STRIKE_TENACIOUS_CLAWS_VORTEX_OF_CLAWS_CLAW_SWEEP_LUNAR_INSANITY_ENORMOUS_WOLF_IMMINENT_PIERCING_ARTFUL_DISARM;
				break;
			}
			case WARG_3:
			{
				npcStringId = NpcStringId.WARG_TRANSCENDENT_SKILLS_DOUBLE_CLAW_STRIKE_TENACIOUS_CLAWS_VORTEX_OF_CLAWS_CLAW_SWEEP_LUNAR_INSANITY_ENORMOUS_WOLF_IMMINENT_PIERCING_ARTFUL_DISARM_2;
				break;
			}
			default:
			{
				npcStringId = null;
				break;
			}
		}
		
		if (npcStringId != null)
		{
			showOnScreenMsg(player, npcStringId, ExShowScreenMessage.TOP_CENTER, 5000);
		}
	}
	
	protected void spawnMonsters(Player player)
	{
		final Instance world = player.getInstanceWorld();
		if (world == null)
		{
			return;
		}
		
		List<Npc> npcs = Collections.emptyList();
		if (world.getTemplateId() == 229 /* Transcendent Instance Zone */)
		{
			final int level = player.getLevel();
			if (level < 50) // Sea of Spores (40-49)
			{
				npcs = world.spawnGroup("monsters_40_49");
			}
			else if (level < 60) // Cemetery (50-59)
			{
				npcs = world.spawnGroup("monsters_50_59");
			}
			else if (level < 70) // Plains of Glory (60-69)
			{
				npcs = world.spawnGroup("monsters_60_69");
			}
			else if (level < 80) // War-Torn Plains (70-79)
			{
				npcs = world.spawnGroup("monsters_70_79");
			}
			else if (level < 85) // Dragon Valley (80-99)
			{
				npcs = world.spawnGroup("monsters_80_84");
			}
			else if (level < 90) // Sel Mahum Base (85-99)
			{
				npcs = world.spawnGroup("monsters_85_89");
			}
			else // Giant's Cave (90+)
			{
				npcs = world.spawnGroup("monsters_90_99");
			}
		}
		else
		{
			npcs = world.spawnGroup("monsters");
		}
		
		for (Npc npc : npcs)
		{
			if (npc.isAttackable())
			{
				((AttackableAI) npc.getAI()).setGlobalAggro(0);
				npc.asAttackable().addDamageHate(player, 0, 9999);
				npc.getAI().setIntention(Intention.ATTACK);
			}
		}
	}
	
	protected void spawnTreasures(Player player)
	{
		final Instance world = player.getInstanceWorld();
		if (world == null)
		{
			return;
		}
		
		List<Npc> npcs = Collections.emptyList();
		if (world.getTemplateId() == 229 /* Transcendent Instance Zone */)
		{
			final int level = player.getLevel();
			if (level < 50) // Sea of Spores (40-49)
			{
				npcs = world.spawnGroup("treasures_40_49");
			}
			else if (level < 60) // Cemetery (50-59)
			{
				npcs = world.spawnGroup("treasures_50_59");
			}
			else if (level < 70) // Plains of Glory (60-69)
			{
				npcs = world.spawnGroup("treasures_60_69");
			}
			else if (level < 80) // War-Torn Plains (70-79)
			{
				npcs = world.spawnGroup("treasures_70_79");
			}
			else if (level < 85) // Dragon Valley (80-99)
			{
				npcs = world.spawnGroup("treasures_80_84");
			}
			else if (level < 90) // Sel Mahum Base (85-99)
			{
				npcs = world.spawnGroup("treasures_85_89");
			}
			else // Giant's Cave (90+)
			{
				npcs = world.spawnGroup("treasures_90_99");
			}
		}
		else
		{
			npcs = world.spawnGroup("treasures");
		}
		
		for (Npc npc : npcs)
		{
			if (npc.isAttackable())
			{
				((AttackableAI) npc.getAI()).setGlobalAggro(0);
				npc.asAttackable().addDamageHate(player, 0, 9999);
				npc.getAI().setIntention(Intention.ATTACK);
			}
		}
	}
	
	protected void spawnGuardian(Player player)
	{
		final Instance world = player.getInstanceWorld();
		if (world == null)
		{
			return;
		}
		
		List<Npc> npcs = Collections.emptyList();
		if (world.getTemplateId() == 229 /* Transcendent Instance Zone */)
		{
			final int level = player.getLevel();
			if (level < 50) // Sea of Spores (40-49)
			{
				npcs = world.spawnGroup("guardian_40_49");
			}
			else if (level < 60) // Cemetery (50-59)
			{
				npcs = world.spawnGroup("guardian_50_59");
			}
			else if (level < 70) // Plains of Glory (60-69)
			{
				npcs = world.spawnGroup("guardian_60_69");
			}
			else if (level < 80) // War-Torn Plains (70-79)
			{
				npcs = world.spawnGroup("guardian_70_79");
			}
			else if (level < 85) // Dragon Valley (80-99)
			{
				npcs = world.spawnGroup("guardian_80_84");
			}
			else if (level < 90) // Sel Mahum Base (85-99)
			{
				npcs = world.spawnGroup("guardian_85_89");
			}
			else // Giant's Cave (90+)
			{
				npcs = world.spawnGroup("guardian_90_99");
			}
		}
		else
		{
			npcs = world.spawnGroup("guardian");
		}
		
		if (!npcs.isEmpty())
		{
			final Npc guardianNpc = npcs.get(0);
			final KeeperType type = getRandomEntry(KeeperType.values());
			if (type == null)
			{
				LOGGER.warning(String.format("[%s]: No KeeperType found for instance %d", getClass().getSimpleName(), world.getId()));
				return;
			}
			
			world.setParameter("KeeperType", type.name());
			guardianNpc.setTitleString(type.getTitle());
			guardianNpc.broadcastInfo();
			
			if (guardianNpc.isAttackable())
			{
				((AttackableAI) guardianNpc.getAI()).setGlobalAggro(0);
				guardianNpc.asAttackable().addDamageHate(player, 0, 9999);
				guardianNpc.getAI().setIntention(Intention.ATTACK);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new TimedHunting();
	}
}
