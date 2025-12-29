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
package ai.bosses.Vulcan;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.ZoneType;

/**
 * @author Tanatos
 */
public class Vulcan extends Script
{
	// NPCs
	private static final int VULCAN = 29247;
	private static final int ENRAGED_VULCAN = 29252;
	private static final int SOUL_1 = 34446;
	private static final int SOUL_2 = 34447;
	private static final int ENRAGED_SOUL_1 = 34449;
	private static final int ENRAGED_SOUL_2 = 34450;
	private static final Set<Integer> VULCAN_SOULS = new HashSet<>();
	static
	{
		VULCAN_SOULS.add(SOUL_1);
		VULCAN_SOULS.add(SOUL_2);
		VULCAN_SOULS.add(ENRAGED_SOUL_1);
		VULCAN_SOULS.add(ENRAGED_SOUL_2);
	}
	
	// Skills
	private static final SkillHolder VULCAN_RANGE = new SkillHolder(48901, 1); // Vulcan's Ranged Attack
	private static final SkillHolder VULCAN_FIRE = new SkillHolder(48902, 1); // Vulcan's Fire
	private static final SkillHolder VULCAN_LAVA = new SkillHolder(48949, 1); // Vulcan's Lava
	private static final SkillHolder ENRAGED_VULCAN_LAVA = new SkillHolder(48950, 1); // Vulcan's Lava
	private static final SkillHolder SOUL_AREA = new SkillHolder(48988, 1); // Vulcan's Soul Area
	private static final SkillHolder SOUL_SURVEIL_1 = new SkillHolder(48989, 1); // Vulcan's Surveillance
	private static final SkillHolder SOUL_SURVEIL_2 = new SkillHolder(48989, 2); // Vulcan's Surveillance
	private static final SkillHolder SOUL_SURVEIL_3 = new SkillHolder(48989, 3); // Vulcan's Surveillance
	private static final SkillHolder SOUL_SURVEIL_4 = new SkillHolder(48989, 4); // Vulcan's Surveillance
	private static final SkillHolder SOUL_SURVEIL_5 = new SkillHolder(48989, 5); // Vulcan's Surveillance
	private static final SkillHolder VULCAN_SOUL_1 = new SkillHolder(49084, 1); // Vulcan's Soul
	private static final SkillHolder VULCAN_SOUL_2 = new SkillHolder(49084, 2); // Vulcan's Soul
	private static final SkillHolder VULCAN_SOUL_3 = new SkillHolder(49084, 3); // Vulcan's Soul
	private static final SkillHolder[] VULCAN_SOULS_SKILLS =
	{
		VULCAN_SOUL_1,
		VULCAN_SOUL_2,
		VULCAN_SOUL_3
	};
	
	// Zone
	private static final ZoneType VULKAN_LAIR_ZONE = ZoneManager.getInstance().getZoneByName("vulcans_lair_thz");
	
	// Misc
	private static boolean _firstSoul = false;
	private static boolean _secondSoul = false;
	
	private Vulcan()
	{
		addSpawnId(VULCAN_SOULS);
		addAttackId(VULCAN, ENRAGED_VULCAN);
		addKillId(VULCAN, ENRAGED_VULCAN);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((npc == null))
		{
			return null;
		}
		
		switch (event)
		{
			case "summonFirstSoul":
			{
				addSpawn(npc.getId() == VULCAN ? SOUL_1 : ENRAGED_SOUL_1, npc.getX() + 275, npc.getY() + 225, npc.getZ() + 10, 0, false, 10000, false, 0);
				addSpawn(npc.getId() == VULCAN ? SOUL_1 : ENRAGED_SOUL_1, npc.getX() + 275, npc.getY() - 225, npc.getZ() + 10, 0, false, 10000, false, 0);
				addSpawn(npc.getId() == VULCAN ? SOUL_1 : ENRAGED_SOUL_1, npc.getX() - 275, npc.getY() + 225, npc.getZ() + 10, 0, false, 10000, false, 0);
				addSpawn(npc.getId() == VULCAN ? SOUL_1 : ENRAGED_SOUL_1, npc.getX() - 275, npc.getY() - 225, npc.getZ() + 10, 0, false, 10000, false, 0);
				addSpawn(npc.getId() == VULCAN ? SOUL_1 : ENRAGED_SOUL_1, npc.getX(), npc.getY() + 275, npc.getZ() + 10, 0, false, 10000, false, 0);
				addSpawn(npc.getId() == VULCAN ? SOUL_1 : ENRAGED_SOUL_1, npc.getX(), npc.getY() - 275, npc.getZ() + 10, 0, false, 10000, false, 0);
				startQuestTimer("summonFirstSoul", 9000, npc, null);
				break;
			}
			case "summonSecondSoul":
			{
				addSpawn(npc.getId() == VULCAN ? SOUL_2 : ENRAGED_SOUL_2, npc.getX() + 225, npc.getY() - 275, npc.getZ() + 10, 0, false, 10000, false, 0);
				addSpawn(npc.getId() == VULCAN ? SOUL_2 : ENRAGED_SOUL_2, npc.getX() - 225, npc.getY() - 275, npc.getZ() + 10, 0, false, 10000, false, 0);
				addSpawn(npc.getId() == VULCAN ? SOUL_2 : ENRAGED_SOUL_2, npc.getX(), npc.getY() + 225, npc.getZ() + 10, 0, false, 10000, false, 0);
				startQuestTimer("summonSecondSoul", 15000, npc, null);
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final List<Player> players = VULKAN_LAIR_ZONE.getPlayersInside();
		npc.doCast(SOUL_AREA.getSkill());
		switch (npc.getId())
		{
			case SOUL_1:
			case ENRAGED_SOUL_1:
			{
				for (Player p : players)
				{
					if (p.isInsideRadius2D(npc.getX(), npc.getY(), 0, 300))
					{
						if (p.getAffectedSkillLevel(SOUL_SURVEIL_5.getSkillId()) == 5)
						{
							return;
						}
						else if (p.getAffectedSkillLevel(SOUL_SURVEIL_4.getSkillId()) == 4)
						{
							SOUL_SURVEIL_5.getSkill().applyEffects(p, p);
						}
						else if (p.getAffectedSkillLevel(SOUL_SURVEIL_3.getSkillId()) == 3)
						{
							SOUL_SURVEIL_4.getSkill().applyEffects(p, p);
						}
						else if (p.getAffectedSkillLevel(SOUL_SURVEIL_2.getSkillId()) == 2)
						{
							SOUL_SURVEIL_3.getSkill().applyEffects(p, p);
						}
						else if (p.getAffectedSkillLevel(SOUL_SURVEIL_1.getSkillId()) == 1)
						{
							SOUL_SURVEIL_2.getSkill().applyEffects(p, p);
						}
						else
						{
							SOUL_SURVEIL_1.getSkill().applyEffects(p, p);
						}
					}
				}
				break;
			}
			case SOUL_2:
			case ENRAGED_SOUL_2:
			{
				for (Player p : players)
				{
					if (p.isInsideRadius2D(npc.getX(), npc.getY(), 0, 300))
					{
						VULCAN_SOULS_SKILLS[getRandom(2)].getSkill().applyEffects(p, p);
					}
				}
				break;
			}
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final Creature mostHated = npc.asAttackable().getMostHated();
		if (!_firstSoul)
		{
			_firstSoul = true;
			startQuestTimer("summonFirstSoul", 1000, npc, null);
		}
		else if (!_secondSoul && (npc.getCurrentHp() <= (npc.getMaxHp() * 0.5)))
		{
			_secondSoul = true;
			startQuestTimer("summonSecondSoul", 1000, npc, null);
		}
		
		if (getRandom(100) < 10)
		{
			if (npc.getId() == VULCAN)
			{
				VULCAN_LAVA.getSkill().applyEffects(attacker, attacker);
			}
			else
			{
				ENRAGED_VULCAN_LAVA.getSkill().applyEffects(attacker, attacker);
			}
		}
		else if (getRandom(100) < 60)
		{
			if (SkillCaster.checkUseConditions(npc, VULCAN_RANGE.getSkill()))
			{
				npc.setTarget(mostHated);
				npc.doCast(VULCAN_RANGE.getSkill());
			}
		}
		else
		{
			if (SkillCaster.checkUseConditions(npc, VULCAN_FIRE.getSkill()))
			{
				npc.setTarget(mostHated);
				npc.doCast(VULCAN_FIRE.getSkill());
			}
		}
	}
	
	public static void main(String[] args)
	{
		new Vulcan();
	}
}
