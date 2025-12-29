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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.StatModifierType;

/**
 * @author Mobius
 */
public class AbnormalTimeChangeBySkillId extends AbstractEffect
{
	private final double _time;
	private final StatModifierType _mode;
	private final Set<Integer> _skillIds = new HashSet<>();
	
	public AbnormalTimeChangeBySkillId(StatSet params)
	{
		_time = params.getDouble("time", -1);
		_mode = params.getEnum("mode", StatModifierType.class, StatModifierType.PER);
		final String skillIds = params.getString("ids", null);
		if ((skillIds != null) && !skillIds.isEmpty())
		{
			for (String id : skillIds.split(","))
			{
				_skillIds.add(Integer.valueOf(id));
			}
		}
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		for (int skillId : _skillIds)
		{
			if (_mode == StatModifierType.PER)
			{
				effected.addMultipliedAbnormalTime(skillId, _time);
			}
			else // DIFF
			{
				effected.addAddedAbnormalTime(skillId, (int) _time);
			}
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		for (int skillId : _skillIds)
		{
			if (_mode == StatModifierType.PER)
			{
				effected.removeMultipliedAbnormalTime(skillId, _time);
			}
			else // DIFF
			{
				effected.removeAddedAbnormalTime(skillId, (int) _time);
			}
		}
	}
}
