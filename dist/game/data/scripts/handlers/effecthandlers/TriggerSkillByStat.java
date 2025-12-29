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
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * @author Mobius
 */
public class TriggerSkillByStat extends AbstractEffect
{
	private final Stat _stat;
	private final int _skillId;
	private final int _skillLevel;
	private final int _skillSubLevel;
	private final int _min;
	private final int _max;
	
	public TriggerSkillByStat(StatSet params)
	{
		_stat = params.getEnum("stat", Stat.class);
		_skillId = params.getInt("skillId", 0);
		_skillLevel = params.getInt("skillLevel", 1);
		_skillSubLevel = params.getInt("skillSubLevel", 0);
		_min = params.getInt("min", 1);
		_max = params.getInt("max", 9999999);
		
		if (_min < 1)
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " minimum should be a positive number.");
		}
	}
	
	@Override
	public boolean delayPump()
	{
		return true;
	}
	
	@Override
	public void pump(Creature effected, Skill skill)
	{
		if (effected == null)
		{
			return;
		}
		
		final int currentValue = (int) effected.getStat().getValue(_stat);
		if ((currentValue < _min) || (currentValue > _max))
		{
			return;
		}
		
		final int level = effected.getAffectedSkillLevel(_skillId);
		if (level == _skillLevel)
		{
			return;
		}
		
		// if (level > 0)
		// {
		// effected.getEffectList().stopSkillEffects(SkillFinishType.SILENT, _skillId);
		// }
		
		ThreadPool.execute(() -> SkillCaster.triggerCast(effected, effected, SkillData.getInstance().getSkill(_skillId, _skillLevel, _skillSubLevel)));
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, _skillId);
	}
}
