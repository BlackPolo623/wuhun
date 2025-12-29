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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Call Skill effect implementation.
 * @author NosBit, Liamxroy
 */
public class CallSkill extends AbstractEffect
{
	private final SkillHolder _skill;
	private final int _skillLevelScaleTo;
	private final int _chance;
	
	private final Set<Integer> _targetCheckSkillIds = new HashSet<>();
	private final int _targetCheckSkillLevel;
	private final Set<Integer> _isAffectedBySkillId = new HashSet<>();
	private final Set<Integer> _isNotAffectedBySkillId = new HashSet<>();
	private final AbnormalType _checkAbnormalType;
	
	public CallSkill(StatSet params)
	{
		_skill = new SkillHolder(params.getInt("skillId"), params.getInt("skillLevel", 1), params.getInt("skillSubLevel", 0));
		_skillLevelScaleTo = params.getInt("skillLevelScaleTo", 0);
		_chance = params.getInt("chance", 100);
		
		for (String skill : params.getString("targetCheckSkillId", "").split(","))
		{
			if (!skill.isEmpty())
			{
				_targetCheckSkillIds.add(Integer.parseInt(skill));
			}
		}
		
		_targetCheckSkillLevel = params.getInt("targetCheckSkillLevel", 1);
		for (String afftectedSkill : params.getString("isAffectedBySkillId", "").split(","))
		{
			if (!afftectedSkill.isEmpty())
			{
				_isAffectedBySkillId.add(Integer.parseInt(afftectedSkill));
			}
		}
		
		for (String notAfftectedSkill : params.getString("isNotAffectedBySkillId", "").split(","))
		{
			if (!notAfftectedSkill.isEmpty())
			{
				_isNotAffectedBySkillId.add(Integer.parseInt(notAfftectedSkill));
			}
		}
		
		final String checkAbnormalType = params.getString("checkAbnormalType", null);
		_checkAbnormalType = checkAbnormalType != null ? AbnormalType.getAbnormalType(checkAbnormalType) : null;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((_checkAbnormalType != null) && !effected.hasAbnormalType(_checkAbnormalType))
		{
			return;
		}
		
		if ((_chance < 100) && (Rnd.get(100) > _chance))
		{
			return;
		}
		
		if (!_targetCheckSkillIds.isEmpty())
		{
			for (Integer skillId : _targetCheckSkillIds)
			{
				final BuffInfo checkSkill = effected.getEffectList().getBuffInfoBySkillId(skillId);
				if ((checkSkill != null) && (_targetCheckSkillLevel >= checkSkill.getSkill().getLevel()))
				{
					return;
				}
			}
		}
		
		if (!_isAffectedBySkillId.isEmpty())
		{
			for (Integer affectedSkillId : _isAffectedBySkillId)
			{
				final Boolean isAffectedBySkillId = effected.getEffectList().isAffectedBySkill(affectedSkillId);
				if (!isAffectedBySkillId)
				{
					return;
				}
			}
		}
		
		if (!_isNotAffectedBySkillId.isEmpty())
		{
			for (Integer notAfftectedSkill : _isAffectedBySkillId)
			{
				final Boolean notAffectedBySkillId = effected.getEffectList().isAffectedBySkill(notAfftectedSkill);
				if (notAffectedBySkillId)
				{
					return;
				}
			}
		}
		
		final Skill triggerSkill;
		if (_skillLevelScaleTo <= 0)
		{
			// Mobius: Use 0 to trigger max effector learned skill level.
			if (_skill.getSkillLevel() == 0)
			{
				final int knownLevel = effector.getSkillLevel(_skill.getSkillId());
				if (knownLevel > 0)
				{
					triggerSkill = SkillData.getInstance().getSkill(_skill.getSkillId(), knownLevel, _skill.getSkillSubLevel());
				}
				else
				{
					return;
				}
			}
			else
			{
				triggerSkill = _skill.getSkill();
			}
		}
		else
		{
			final BuffInfo buffInfo = effected.getEffectList().getBuffInfoBySkillId(_skill.getSkillId());
			if (buffInfo != null)
			{
				triggerSkill = SkillData.getInstance().getSkill(_skill.getSkillId(), Math.min(_skillLevelScaleTo, buffInfo.getSkill().getLevel() + 1));
			}
			else
			{
				triggerSkill = _skill.getSkill();
			}
		}
		
		if (triggerSkill != null)
		{
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
}
