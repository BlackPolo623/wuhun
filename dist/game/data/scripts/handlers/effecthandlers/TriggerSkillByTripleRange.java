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

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * Trigger Skill By Triple Range effect implementation.
 * @author Mobius, fruit
 */
public class TriggerSkillByTripleRange extends AbstractEffect
{
	private final SkillHolder _closeSkill;
	private final SkillHolder _midSkill;
	private final SkillHolder _rangeSkill;
	private final int _closeDistance;
	private final int _midDistance;
	private final boolean _adjustLevel;
	
	public TriggerSkillByTripleRange(StatSet params)
	{
		// Just use closeSkill, midSkill and rangeSkill parameters.
		_closeSkill = new SkillHolder(params.getInt("closeSkill"), params.getInt("closeSkillLevel", 1));
		_midSkill = new SkillHolder(params.getInt("midSkill"), params.getInt("midSkillLevel", 1));
		_rangeSkill = new SkillHolder(params.getInt("rangeSkill"), params.getInt("rangeSkillLevel", 1));
		_closeDistance = params.getInt("closeDistance", 120);
		_midDistance = params.getInt("rangeDistance", 360);
		_adjustLevel = params.getBoolean("adjustLevel", true);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.DUAL_RANGE;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((effected == null) || !effector.isPlayer())
		{
			return;
		}
		
		final double distance = effector.calculateDistance3D(effected);
		final SkillHolder skillHolder;
		if (distance < _closeDistance)
		{
			skillHolder = _closeSkill;
		}
		else if (distance < _midDistance)
		{
			skillHolder = _midSkill;
		}
		else
		{
			skillHolder = _rangeSkill;
		}
		
		final Skill triggerSkill = _adjustLevel ? SkillData.getInstance().getSkill(skillHolder.getSkillId(), skill.getLevel(), skill.getSubLevel()) : skillHolder.getSkill();
		if (triggerSkill == null)
		{
			return;
		}
		
		final Player caster = effector.asPlayer();
		if ((effected != effector) && effected.isPlayable() && !effected.isAutoAttackable(caster))
		{
			caster.updatePvPStatus();
		}
		
		caster.useMagic(triggerSkill, null, true, false);
	}
}
