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
package ai.areas.LairOfAntharas;

import java.util.Calendar;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.managers.DatabaseSpawnManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.MountType;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.Earthquake;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * Behemoth AI
 * @URL https://l2central.info/essence/locations/special_zones/antharas_lair/
 */
public class Behemoth extends Script
{
	// NPC
	private static final int BEHEMOTH = 25912;
	private static final int ULTIMATE_BEHEMOTH = 25935;
	
	// Location
	private static final ZoneType ZONE = ZoneManager.getInstance().getZoneByName("antharas_lair");
	private static final Location LOCATION = new Location(148386, 117952, -3715);
	
	// skills
	private static final SkillHolder ANTI_STRIDER = new SkillHolder(4258, 1); // Hinder Strider
	private static final SkillHolder BEHEMOTH_ATTACK = new SkillHolder(48081, 1);
	private static final int BEHEMOTH_ATTACK_SKILL_MIN_COOLTIME = 30000; // 30 seconds
	private static final SkillHolder BEHEMOTH_FEAR = new SkillHolder(48082, 1);
	private static final int BEHEMOTH_FEAR_SKILL_MIN_COOLTIME = 33000; // 33 seconds
	private long _lastBehemothAttackSkillTime;
	private long _lastBehemothFearSkillTime;
	
	private Behemoth()
	{
		addKillId(BEHEMOTH, ULTIMATE_BEHEMOTH);
		addAttackId(BEHEMOTH, ULTIMATE_BEHEMOTH);
		
		// Schedule reset everyday from 22:00 till 22:30
		final long currentTime = System.currentTimeMillis();
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 22);
		calendar.set(Calendar.MINUTE, getRandom(0, 0));
		calendar.set(Calendar.SECOND, getRandom(0, 0));
		if (calendar.getTimeInMillis() < currentTime)
		{
			calendar.add(Calendar.DAY_OF_YEAR, 1);
		}
		
		final long calendarTime = calendar.getTimeInMillis();
		final long startDelay = Math.max(0, calendarTime - currentTime);
		// LOGGER.info(getClass().getSimpleName() + ": Loaded: Behemoth will spawn at: " + startDelay);
		ThreadPool.scheduleAtFixedRate(this::onSpawn, startDelay, 86400000); // 86400000 = 1 day
	}
	
	private void onSpawn()
	{
		boolean spawnStatus = false;
		for (Spawn spawn : SpawnTable.getInstance().getSpawns(BEHEMOTH))
		{
			for (Npc monster : spawn.getSpawnedNpcs())
			{
				if (monster.isDead())
				{
					DatabaseSpawnManager.getInstance().deleteSpawn(spawn, true);
				}
				else
				{
					spawnStatus = true;
				}
			}
		}
		
		if (!spawnStatus)
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(BEHEMOTH);
			if (template != null)
			{
				try
				{
					final Spawn spawn = new Spawn(template);
					spawn.setXYZ(LOCATION);
					spawn.setRespawnDelay(86400000);
					DatabaseSpawnManager.getInstance().addNewSpawn(spawn, true);
					if (ZONE != null)
					{
						ZONE.broadcastPacket(new ExShowScreenMessage(NpcStringId.ANTHARAS_AVATAR_BEHEMOTH_APPEARED, 2, 5000));
						ZONE.broadcastPacket(new ExShowScreenMessage("Behemoth has spawned!", 5000));
						ZONE.broadcastPacket(new Earthquake(LOCATION, 40, 10));
						ZONE.broadcastPacket(new PlaySound("BS02_A"));
					}
					else
					{
						LOGGER.warning(getClass().getSimpleName() + ": Zone is not initialized properly!");
					}
					
					startQuestTimer("CHECK_ATTACK", 60000, null, null);
				}
				catch (Exception e)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Problem with onSpawn! " + e.getMessage());
				}
			}
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if ((npc.getId() == BEHEMOTH) || (npc.getId() == ULTIMATE_BEHEMOTH))
		{
			if ((attacker.getMountType() == MountType.STRIDER) && !attacker.isAffectedBySkill(ANTI_STRIDER.getSkillId()) && !npc.isSkillDisabled(ANTI_STRIDER.getSkill()))
			{
				npc.setTarget(attacker);
				npc.doCast(ANTI_STRIDER.getSkill());
			}
			
			manageSkills(npc);
		}
	}
	
	private void manageSkills(Npc npc)
	{
		if (npc.isCastingNow() || npc.isCoreAIDisabled() || !npc.isInCombat())
		{
			return;
		}
		
		SkillHolder skillToCast = null;
		if (npc.getId() == BEHEMOTH)
		{
			if (((_lastBehemothAttackSkillTime + BEHEMOTH_ATTACK_SKILL_MIN_COOLTIME) < System.currentTimeMillis()))
			{
				skillToCast = BEHEMOTH_ATTACK;
				_lastBehemothAttackSkillTime = System.currentTimeMillis();
			}
			else if (((_lastBehemothFearSkillTime + BEHEMOTH_FEAR_SKILL_MIN_COOLTIME) < System.currentTimeMillis()))
			{
				_lastBehemothFearSkillTime = System.currentTimeMillis();
				skillToCast = BEHEMOTH_FEAR;
			}
		}
		
		if ((skillToCast != null) && SkillCaster.checkUseConditions(npc, skillToCast.getSkill()))
		{
			npc.getAI().setIntention(Intention.CAST, skillToCast.getSkill(), npc);
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if ((npc.getId() == BEHEMOTH) && (getRandom(100) <= 15))
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(ULTIMATE_BEHEMOTH);
			if (template != null)
			{
				try
				{
					final Spawn spawn = new Spawn(template);
					Location locationWhereBehemothDie = npc.getLocation();
					spawn.setXYZ(locationWhereBehemothDie);
					spawn.setRespawnDelay(86400000);
					DatabaseSpawnManager.getInstance().addNewSpawn(spawn, true);
					if (ZONE != null)
					{
						ZONE.broadcastPacket(new ExShowScreenMessage(NpcStringId.ANTHARAS_AVATAR_ULTIMATE_BEHEMOTH_APPEARED, 2, 5000));
						ZONE.broadcastPacket(new ExShowScreenMessage("Ultimate Behemoth has spawned!", 5000));
						ZONE.broadcastPacket(new Earthquake(npc.getX(), npc.getY(), npc.getZ(), 40, 10));
						ZONE.broadcastPacket(new PlaySound("BS02_A"));
					}
					else
					{
						LOGGER.warning(getClass().getSimpleName() + ": Zone is not initialized properly!");
					}
				}
				catch (Exception e)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Problem with onSpawn! " + e.getMessage());
				}
			}
		}
		
		if (npc.getId() == ULTIMATE_BEHEMOTH)
		{
			for (Spawn spawn : SpawnTable.getInstance().getSpawns(ULTIMATE_BEHEMOTH))
			{
				for (Npc monster : spawn.getSpawnedNpcs())
				{
					if (monster.isDead())
					{
						DatabaseSpawnManager.getInstance().deleteSpawn(spawn, true);
					}
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new Behemoth();
	}
}
