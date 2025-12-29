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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureKilled;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.skill.targets.TargetType;

/**
 * @author Liamxroy
 */
public class CallSkillByKill extends AbstractEffect
{
	private final List<InstanceType> _victimType;
	private final TargetType _targetType;
	private final int _chance;
	private final SkillHolder _skill;
	
	public CallSkillByKill(StatSet params)
	{
		_victimType = params.getEnumList("victimType", InstanceType.class);
		_targetType = params.getEnum("targetType", TargetType.class, TargetType.ENEMY);
		_chance = params.getInt("chance", 100);
		_skill = new SkillHolder(params.getInt("skillId", 0), params.getInt("skillLevel", 0));
	}
	
	private void onCreatureKilled(OnCreatureKilled event, Creature target)
	{
		final Creature attacker = event.getAttacker();
		final Creature eventTarget = event.getTarget();
		if ((_chance == 0) || ((_skill.getSkillId() == 0) || (_skill.getSkillLevel() == 0)))
		{
			return;
		}
		
		if (Rnd.get(100) > _chance)
		{
			return;
		}
		
		final Skill triggerSkill = _skill.getSkill();
		if (triggerSkill == null)
		{
			LOGGER.warning("Player " + event.getAttacker() + " called unknown skill " + _skill.getSkillId() + " triggered by CallSkillByKill.");
			return;
		}
		
		if ((((_victimType != null) && !_victimType.isEmpty()) && _victimType.contains(target.getInstanceType()) && (event.getAttacker() == target)))
		{
			if ((_targetType != null))
			{
				final ITargetTypeHandler handler = TargetHandler.getInstance().getHandler(_targetType);
				if (handler != null)
				{
					final WorldObject handlerTarget = handler.getTarget(attacker, eventTarget, triggerSkill, false, false, true);
					if (handlerTarget instanceof Creature)
					{
						SkillCaster.triggerCast(attacker, handlerTarget.asCreature(), triggerSkill);
					}
				}
			}
			else
			{
				SkillCaster.triggerCast(attacker, eventTarget, triggerSkill);
			}
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_KILLED, listener -> listener.getOwner() == this);
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_KILLED, (OnCreatureKilled event) -> onCreatureKilled(event, effected), this));
	}
}
