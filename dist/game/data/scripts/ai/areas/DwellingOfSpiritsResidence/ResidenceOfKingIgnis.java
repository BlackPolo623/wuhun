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
package ai.areas.DwellingOfSpiritsResidence;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author RobikBobik
 */
public class ResidenceOfKingIgnis extends Script
{
	// NPCs
	private static final int IGNIS = 29105;
	
	// Skills
	private static final SkillHolder FIRE_RAG_1 = new SkillHolder(50050, 1);
	private static final SkillHolder FIRE_RAG_2 = new SkillHolder(50050, 2);
	private static final SkillHolder FIRE_RAG_3 = new SkillHolder(50050, 3);
	private static final SkillHolder FIRE_RAG_4 = new SkillHolder(50050, 4);
	private static final SkillHolder FIRE_RAG_5 = new SkillHolder(50050, 5);
	private static final SkillHolder FIRE_RAG_6 = new SkillHolder(50050, 6);
	private static final SkillHolder FIRE_RAG_7 = new SkillHolder(50050, 7);
	private static final SkillHolder FIRE_RAG_8 = new SkillHolder(50050, 8);
	private static final SkillHolder FIRE_RAG_9 = new SkillHolder(50050, 9);
	private static final SkillHolder FIRE_RAG_10 = new SkillHolder(50050, 10);
	
	public ResidenceOfKingIgnis()
	{
		addAttackId(IGNIS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "CAST_FIRE_RAGE_1":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_1.getSkill()))
				{
					npc.doCast(FIRE_RAG_1.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_2":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_2.getSkill()))
				{
					npc.doCast(FIRE_RAG_2.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_3":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_3.getSkill()))
				{
					npc.doCast(FIRE_RAG_3.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_4":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_4.getSkill()))
				{
					npc.doCast(FIRE_RAG_4.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_5":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_5.getSkill()))
				{
					npc.doCast(FIRE_RAG_5.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_6":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_6.getSkill()))
				{
					npc.doCast(FIRE_RAG_6.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_7":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_7.getSkill()))
				{
					npc.doCast(FIRE_RAG_7.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_8":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_8.getSkill()))
				{
					npc.doCast(FIRE_RAG_8.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_9":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_9.getSkill()))
				{
					npc.doCast(FIRE_RAG_9.getSkill());
				}
				break;
			}
			case "CAST_FIRE_RAGE_10":
			{
				if (SkillCaster.checkUseConditions(npc, FIRE_RAG_10.getSkill()))
				{
					npc.doCast(FIRE_RAG_10.getSkill());
				}
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.99)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.70)))
		{
			startQuestTimer("CAST_FIRE_RAGE_1", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.70)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.50)))
		{
			startQuestTimer("CAST_FIRE_RAGE_2", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.50)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.40)))
		{
			startQuestTimer("CAST_FIRE_RAGE_3", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.40)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.25)))
		{
			startQuestTimer("CAST_FIRE_RAGE_4", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.25)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.15)))
		{
			startQuestTimer("CAST_FIRE_RAGE_5", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.15)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.10)))
		{
			startQuestTimer("CAST_FIRE_RAGE_6", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.10)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.7)))
		{
			startQuestTimer("CAST_FIRE_RAGE_7", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.7)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.5)))
		{
			startQuestTimer("CAST_FIRE_RAGE_8", 1000, npc, null);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.5)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.3)))
		{
			startQuestTimer("CAST_FIRE_RAGE_9", 1000, npc, null);
		}
		else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.3))
		{
			startQuestTimer("CAST_FIRE_RAGE_10", 1000, npc, null);
		}
	}
	
	public static void main(String[] args)
	{
		new ResidenceOfKingIgnis();
	}
}
