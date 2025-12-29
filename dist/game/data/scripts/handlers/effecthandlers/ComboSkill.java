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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Combo Skill effect implementation.
 * @author Prink
 */
public class ComboSkill extends AbstractEffect
{
	private final SkillHolder _skill;
	private final int _checkSkill;
	private final int _checkSkill2;
	private final String _mode;
	private final int _chance;
	
	public ComboSkill(StatSet params)
	{
		_skill = new SkillHolder(params.getInt("skillId"), params.getInt("skillLevel", 1), params.getInt("skillSubLevel", 0));
		_checkSkill = params.getInt("targetCheckSkillId", 0);
		_checkSkill2 = params.getInt("targetCheckSkillId2", 0);
		_mode = params.getString("mode", "DUAL");
		_chance = params.getInt("chance", 100);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Skill triggerSkill = _skill.getSkill();
		
		switch (_mode)
		{
			case "DUAL":
			{
				if (!effected.isAffectedBySkill(_checkSkill))
				{
					return;
				}
				break;
			}
			case "TRIPLE":
			{
				if (((!effected.isAffectedBySkill(_checkSkill) && !effected.isAffectedBySkill(_checkSkill2))) || (!effected.isAffectedBySkill(_checkSkill) && effected.isAffectedBySkill(_checkSkill2)) || (effected.isAffectedBySkill(_checkSkill) && !effected.isAffectedBySkill(_checkSkill2)))
				{
					return;
				}
				break;
			}
		}
		
		if ((_chance < 100) && (Rnd.get(100) >= _chance))
		{
			return;
		}
		
		if (triggerSkill == null)
		{
			return;
		}
		
		// Prevent infinite loop.
		if ((skill.getId() == triggerSkill.getId()) && (skill.getLevel() == triggerSkill.getLevel()))
		{
			return;
		}
		
		final int hitTime = triggerSkill.getHitTime();
		if (hitTime > 0)
		{
			if (effector.isSkillDisabled(triggerSkill))
			{
				return;
			}
			
			effector.broadcastSkillPacket(new MagicSkillUse(effector, effected, triggerSkill.getDisplayId(), triggerSkill.getLevel(), hitTime, 0), effected);
			ThreadPool.schedule(() -> SkillCaster.triggerCast(effector, effected, triggerSkill), hitTime);
		}
		else
		{
			SkillCaster.triggerCast(effector, effected, triggerSkill);
		}
	}
}
