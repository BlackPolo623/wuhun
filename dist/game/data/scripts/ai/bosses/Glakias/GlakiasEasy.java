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
package ai.bosses.Glakias;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Serenitty
 */
public class GlakiasEasy extends Script
{
	// NPCs
	private static final int GLAKIAS = 29136;
	private static final int GLAKIAS2 = 29137;
	
	// Skills
	private static final SkillHolder GLAKIAS_ENCHANCEMENT_LV_1 = new SkillHolder(48372, 1);
	
	// Skills Glakias (29136)
	private static final SkillHolder EMPEROR_THUNDER_LV_1 = new SkillHolder(48378, 1);
	private static final SkillHolder EMPEROR_THUNDER_LV_2 = new SkillHolder(48378, 2);
	private static final SkillHolder EMPEROR_SMASH_LV_1 = new SkillHolder(48377, 1);
	private static final SkillHolder EMPEROR_SMASH_LV_2 = new SkillHolder(48377, 2);
	
	// Skills Glakias (29137)
	private static final SkillHolder EMPEROR_TORNADO_LV_1 = new SkillHolder(48382, 1);
	private static final SkillHolder EMPEROR_TORNADO_LV_2 = new SkillHolder(48382, 2);
	private static final SkillHolder EMPEROR_CRYSTAL_LV_1 = new SkillHolder(48383, 1);
	private static final SkillHolder EMPEROR_CRYSTAL_LV_2 = new SkillHolder(48383, 2);
	
	public GlakiasEasy()
	{
		addKillId(GLAKIAS);
		addAttackId(GLAKIAS);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (npc.isAttackable())
		{
			if (npc.getId() == GLAKIAS)
			{
				manageGlakiasSkills(npc, attacker);
			}
			else if (npc.getId() == GLAKIAS2)
			{
				manageTwoGlakiasSkills(npc, attacker);
			}
		}
	}
	
	private void manageGlakiasSkills(Npc npc, Player player)
	{
		if (npc.isCastingNow(SkillCaster::isAnyNormalType) || npc.isCoreAIDisabled() || !npc.isInCombat())
		{
			return;
		}
		
		SkillHolder skillToCast = null;
		final double currentHpPercentage = npc.getCurrentHp() / npc.getMaxHp();
		if (currentHpPercentage > 45)
		{
			if (getRandom(100) < 40)
			{
				final int randomSkill = getRandom(4);
				switch (randomSkill)
				{
					case 1:
					{
						skillToCast = EMPEROR_THUNDER_LV_1;
						break;
					}
					case 2:
					{
						skillToCast = EMPEROR_SMASH_LV_1;
						break;
					}
				}
			}
		}
		else if ((currentHpPercentage < 45) && (getRandom(100) < 40))
		{
			final int randomSkill = getRandom(4);
			switch (randomSkill)
			{
				case 1:
				{
					skillToCast = EMPEROR_THUNDER_LV_2;
					break;
				}
				case 2:
				{
					skillToCast = EMPEROR_SMASH_LV_2;
					break;
				}
				case 3:
				{
					skillToCast = GLAKIAS_ENCHANCEMENT_LV_1;
					break;
				}
			}
		}
		
		if ((skillToCast != null) && SkillCaster.checkUseConditions(npc, skillToCast.getSkill()))
		{
			npc.setTarget(player);
			npc.doCast(skillToCast.getSkill());
		}
	}
	
	private void manageTwoGlakiasSkills(Npc npc, Player player)
	{
		if (npc.isCastingNow(SkillCaster::isAnyNormalType) || npc.isCoreAIDisabled() || !npc.isInCombat())
		{
			return;
		}
		
		SkillHolder skillToCast = null;
		final double currentHpPercentage = npc.getCurrentHp() / npc.getMaxHp();
		if (currentHpPercentage > 45)
		{
			if (getRandom(100) < 40)
			{
				final int randomSkill = getRandom(4);
				switch (randomSkill)
				{
					case 1:
					{
						skillToCast = EMPEROR_TORNADO_LV_1;
						break;
					}
					case 2:
					{
						skillToCast = EMPEROR_CRYSTAL_LV_1;
						break;
					}
				}
			}
		}
		else if ((currentHpPercentage < 45) && (getRandom(100) < 40))
		{
			final int randomSkill = getRandom(4);
			switch (randomSkill)
			{
				case 1:
				{
					skillToCast = EMPEROR_CRYSTAL_LV_2;
					break;
				}
				case 2:
				{
					skillToCast = EMPEROR_TORNADO_LV_2;
					break;
				}
				case 3:
				{
					skillToCast = GLAKIAS_ENCHANCEMENT_LV_1;
					break;
				}
			}
		}
		
		if ((skillToCast != null) && SkillCaster.checkUseConditions(npc, skillToCast.getSkill()))
		{
			npc.setTarget(player);
			npc.doCast(skillToCast.getSkill());
		}
	}
	
	public static void main(String[] args)
	{
		new GlakiasEasy();
	}
}
