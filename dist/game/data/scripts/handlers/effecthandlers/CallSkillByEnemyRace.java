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
package handlers.effecthandlers;

import java.util.List;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;

public class CallSkillByEnemyRace extends AbstractEffect
{
	private final int humanSkillId;
	private final int elfSkillId;
	private final int dElfSkillId;
	private final int orcSkillId;
	private final int dwarfSkillId;
	private final int kamaelSkillId;
	private final int sylphSkillId;
	private final int hElfSkillId;
	private final int skillLevel;
	
	public CallSkillByEnemyRace(StatSet params)
	{
		humanSkillId = params.getInt("humanSkillId", 0);
		elfSkillId = params.getInt("elfSkillId", 0);
		dElfSkillId = params.getInt("dElfSkillId", 0);
		orcSkillId = params.getInt("orcSkillId", 0);
		dwarfSkillId = params.getInt("dwarfSkillId", 0);
		kamaelSkillId = params.getInt("kamaelSkillId", 0);
		sylphSkillId = params.getInt("sylphSkillId", 0);
		hElfSkillId = params.getInt("hElfSkillId", 0);
		skillLevel = params.getInt("skillLevel", 1);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((effector == null) || (effected == null))
		{
			return;
		}
		
		final int skillIdByRace = getSkillIdByRace(effected);
		if (skillIdByRace > 0)
		{
			final Skill skillByRace = SkillData.getInstance().getSkill(skillIdByRace, skillLevel);
			if (skillByRace != null)
			{
				final List<WorldObject> targetsAffected = skillByRace.getTargetsAffected(effector, effected);
				SkillCaster.callSkill(effector, effected, targetsAffected, skillByRace, item);
			}
		}
	}
	
	private int getSkillIdByRace(Creature effected)
	{
		if (effected == null)
		{
			return 0;
		}
		
		int skillId = 0;
		
		switch (effected.getRace())
		{
			case HUMAN:
			{
				skillId = humanSkillId;
				break;
			}
			case ELF:
			{
				skillId = elfSkillId;
				break;
			}
			case DARK_ELF:
			{
				skillId = dElfSkillId;
				break;
			}
			case DWARF:
			{
				skillId = dwarfSkillId;
				break;
			}
			case ORC:
			{
				skillId = orcSkillId;
				break;
			}
			case KAMAEL:
			{
				skillId = kamaelSkillId;
				break;
			}
			case SYLPH:
			{
				skillId = sylphSkillId;
				break;
			}
			case HIGH_ELF:
			{
				skillId = hElfSkillId;
				break;
			}
		}
		
		return skillId;
	}
}
