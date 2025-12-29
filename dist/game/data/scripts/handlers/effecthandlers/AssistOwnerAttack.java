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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureAttack;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author Liamxroy
 */
public class AssistOwnerAttack extends AbstractEffect
{
	private final Set<Integer> _summonIds;
	
	public AssistOwnerAttack(StatSet params)
	{
		String summonIdString = params.getString("summonId");
		_summonIds = Arrays.stream(summonIdString.split(";")).map(Integer::parseInt).collect(Collectors.toSet());
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_ATTACK, (OnCreatureAttack event) -> onAttackEvent(event), this));
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
	}
	
	private void handleAttackEvent(Creature caster, WorldObject target)
	{
		processSummonActions(caster, target);
	}
	
	private void processSummonActions(Creature caster, WorldObject target)
	{
		if (caster.getServitors() != null)
		{
			for (Summon servitor : caster.getServitors().values())
			{
				if ((servitor != null) && _summonIds.contains(servitor.getId()) && !servitor.isDisabled())
				{
					servitor.setTarget(target);
					servitor.doAttack(target);
				}
			}
		}
		
		if (caster.getSummonedNpcCount() != 0)
		{
			caster.getSummonedNpcs().forEach(summon ->
			{
				if (_summonIds.contains(summon.getId()) && !summon.isDisabled())
				{
					summon.setTarget(target);
					summon.moveToLocation(target.getX(), target.getY(), target.getZ(), summon.getPhysicalAttackRange());
					summon.doAutoAttack(target.asCreature());
				}
			});
		}
	}
	
	private void onAttackEvent(OnCreatureAttack event)
	{
		final Creature caster = event.getAttacker();
		if (((caster.getSummonedNpcCount() == 0) && (caster.getServitors() == null)) || !caster.isPlayer())
		{
			return;
		}
		
		final WorldObject target = event.getTarget();
		if ((target == null) || !target.isCreature())
		{
			return;
		}
		
		handleAttackEvent(caster, target);
	}
}
