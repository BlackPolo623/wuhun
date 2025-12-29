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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureSkillFinishCast;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureSkillUse;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Mobius
 */
public class AssistOwnerCast extends AbstractEffect
{
	private final int _summonId;
	private final int _castSkillId;
	private final List<SkillHolder> _skills = new ArrayList<>();
	private final boolean _onFinishCast;
	
	public AssistOwnerCast(StatSet params)
	{
		_summonId = params.getInt("summonId"); // Npc id
		_castSkillId = params.getInt("castSkillId");
		_onFinishCast = params.getBoolean("onFinishCast", false);
		
		final String skillIds = params.getString("skillId", "");
		final String skillLevels = params.getString("skillLevel", "");
		
		final String[] skillIdArray = skillIds.split(";");
		final String[] skillLevelArray = skillLevels.split(";");
		
		if (skillIdArray.length != skillLevelArray.length)
		{
			throw new IllegalArgumentException("Mismatch between number of skillIds and skillLevels!");
		}
		
		for (int i = 0; i < skillIdArray.length; i++)
		{
			final int skillId = Integer.parseInt(skillIdArray[i].trim());
			final int skillLevel = Integer.parseInt(skillLevelArray[i].trim());
			_skills.add(new SkillHolder(skillId, skillLevel));
		}
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (_skills.isEmpty() || (_castSkillId == 0))
		{
			return;
		}
		
		if (_onFinishCast)
		{
			effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_SKILL_FINISH_CAST, (OnCreatureSkillFinishCast event) -> onSkillUseEvent(event), this));
		}
		else
		{
			effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_SKILL_USE, (OnCreatureSkillUse event) -> onSkillUseEvent(event), this));
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (_onFinishCast)
		{
			effected.removeListenerIf(EventType.ON_CREATURE_SKILL_FINISH_CAST, listener -> listener.getOwner() == this);
		}
		else
		{
			effected.removeListenerIf(EventType.ON_CREATURE_SKILL_USE, listener -> listener.getOwner() == this);
		}
	}
	
	private void processSummonActions(Creature caster, WorldObject target)
	{
		if (caster.getServitors() != null)
		{
			for (Summon servitor : caster.getServitors().values())
			{
				if ((servitor != null) && (_summonId == servitor.getId()) && !servitor.isDisabled())
				{
					servitor.setTarget(target);
					castSkills(servitor, target);
				}
			}
		}
		
		if (caster.getSummonedNpcCount() != 0)
		{
			caster.getSummonedNpcs().forEach(summon ->
			{
				if ((_summonId == summon.getId()) && !summon.isDisabled())
				{
					summon.setTarget(target);
					castSkills(summon, target);
				}
			});
		}
	}
	
	private void castSkills(Creature summon, WorldObject target)
	{
		for (int i = 0; i < _skills.size(); i++)
		{
			final Skill skill = _skills.get(i).getSkill();
			if (skill != null)
			{
				ThreadPool.schedule(() ->
				{
					if (skill.hasNegativeEffect())
					{
						summon.getAI().setIntention(Intention.ATTACK, target);
					}
					
					summon.doCast(skill);
				}, (1250 * i) + 50); // Delay between skills.
			}
		}
	}
	
	private void handleSkillUseEvent(Creature caster, WorldObject target)
	{
		processSummonActions(caster, target);
	}
	
	private void onSkillUseEvent(OnCreatureSkillFinishCast event)
	{
		final Creature caster = event.getCaster();
		if (((caster.getSummonedNpcCount() == 0) && (caster.getServitors() == null)) || (_castSkillId != event.getSkill().getId()) || !caster.isPlayer())
		{
			return;
		}
		
		final WorldObject target = event.getTarget();
		if ((target == null) || !target.isCreature())
		{
			return;
		}
		
		handleSkillUseEvent(caster, target);
	}
	
	private void onSkillUseEvent(OnCreatureSkillUse event)
	{
		final Creature caster = event.getCaster();
		if (((caster.getSummonedNpcCount() == 0) && (caster.getServitors() == null)) || (_castSkillId != event.getSkill().getId()) || !caster.isPlayer())
		{
			return;
		}
		
		final WorldObject target = caster.getTarget();
		if ((target == null) || !target.isCreature())
		{
			return;
		}
		
		handleSkillUseEvent(caster, target);
	}
}
