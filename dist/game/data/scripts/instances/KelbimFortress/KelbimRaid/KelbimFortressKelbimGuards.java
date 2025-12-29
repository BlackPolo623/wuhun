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
package instances.KelbimFortress.KelbimRaid;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

import instances.KelbimFortress.KelbimFortressManager;

/**
 * @author Index
 */
public class KelbimFortressKelbimGuards extends Script
{
	private static final SkillHolder[] SKILL_LIST = new SkillHolder[]
	{
		new SkillHolder(48664, 1), // 0
		new SkillHolder(48665, 1), // 1
		new SkillHolder(48666, 1), // 2
		new SkillHolder(48667, 1), // 3
		new SkillHolder(48668, 1), // 4
		new SkillHolder(48665, 1), // 5
		new SkillHolder(48666, 1), // 6
		new SkillHolder(48667, 1), // 7
		new SkillHolder(48668, 1), // 8
	};
	
	public static final int[] GUARD_IDS =
	{
		18853,
		18854,
		18855,
		18856,
		18857,
		18858,
		18859
	};
	
	public KelbimFortressKelbimGuards()
	{
		addAttackId(GUARD_IDS);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final Instance world = (attacker == null) || (npc == null) ? null : attacker.getInstanceWorld();
		if ((npc == null) || (world == null) || (world.getTemplateId() != KelbimFortressManager.INSTANCE_TEMPLATE_ID))
		{
			super.onAttack(npc, attacker, damage, isSummon, skill);
		}
		
		thinkAction(world, npc);
	}
	
	public void thinkAction(Instance world, Npc npc)
	{
		if (!npc.isCastingNow() && !npc.isMovementDisabled())
		{
			final int status = world.getParameters().getInt(KelbimFortressManager.VARIABLE_KELBIM_GUARD_STATUS, 0);
			final SkillHolder attackSkill = SKILL_LIST.length <= status ? SKILL_LIST[0] : SKILL_LIST[status];
			final Skill skill = SkillData.getInstance().getSkill(attackSkill.getSkillId(), attackSkill.getSkillLevel());
			if (skill != null)
			{
				npc.doCast(skill);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new KelbimFortressKelbimGuards();
	}
}
