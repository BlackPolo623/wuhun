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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDamageReceived;
import org.l2jmobius.gameserver.model.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.model.events.returns.DamageReturn;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.skill.enums.StatModifierType;

/**
 * @author Sdw, Mobius
 */
public class AbsorbDamage extends AbstractEffect
{
	private static final Map<Integer, Double> DIFF_DAMAGE_HOLDER = new ConcurrentHashMap<>();
	private static final Map<Integer, Double> PER_DAMAGE_HOLDER = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> HITS_HOLDER = new ConcurrentHashMap<>();
	
	private final double _damage;
	private final StatModifierType _mode;
	private final int _hits;
	private final double _casterHpMod;
	
	public AbsorbDamage(StatSet params)
	{
		_damage = params.getDouble("damage", 0);
		_mode = params.getEnum("mode", StatModifierType.class, StatModifierType.DIFF);
		_hits = params.getInt("hits", -1);
		_casterHpMod = params.getDouble("casterHpMod", 0); // % from caster Max HP to _damage
	}
	
	private DamageReturn onDamageReceivedDiffEvent(OnCreatureDamageReceived event, Creature effected, Skill skill)
	{
		// DOT effects are not taken into account.
		if (event.isDamageOverTime())
		{
			return null;
		}
		
		final int objectId = event.getTarget().getObjectId();
		
		final double damageLeft = DIFF_DAMAGE_HOLDER.getOrDefault(objectId, 0d);
		final double newDamageLeft = Math.max(damageLeft - event.getDamage(), 0);
		final double newDamage = Math.max(event.getDamage() - damageLeft, 0);
		
		int hitsUsed = HITS_HOLDER.getOrDefault(objectId, 0);
		if (_hits != -1)
		{
			hitsUsed++;
			if (hitsUsed > _hits)
			{
				effected.stopSkillEffects(skill);
			}
			else
			{
				HITS_HOLDER.put(objectId, hitsUsed);
			}
		}
		
		if (newDamageLeft > 0)
		{
			DIFF_DAMAGE_HOLDER.put(objectId, newDamageLeft);
		}
		else
		{
			effected.stopSkillEffects(skill);
		}
		
		return new DamageReturn(false, true, false, newDamage);
	}
	
	private DamageReturn onDamageReceivedPerEvent(OnCreatureDamageReceived event)
	{
		// DOT effects are not taken into account.
		if (event.isDamageOverTime())
		{
			return null;
		}
		
		final int objectId = event.getTarget().getObjectId();
		
		final double damagePercent = PER_DAMAGE_HOLDER.getOrDefault(objectId, 0d);
		final double currentDamage = event.getDamage();
		final double newDamage = currentDamage - ((currentDamage / 100) * damagePercent);
		
		return new DamageReturn(false, true, false, newDamage);
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_DAMAGE_RECEIVED, listener -> listener.getOwner() == this);
		if (_mode == StatModifierType.DIFF)
		{
			DIFF_DAMAGE_HOLDER.remove(effected.getObjectId());
		}
		else
		{
			PER_DAMAGE_HOLDER.remove(effected.getObjectId());
		}
		
		HITS_HOLDER.remove(effected.getObjectId());
		
		// Stop other effects when shield is removed.
		if (skill != null)
		{
			effected.stopSkillEffects(SkillFinishType.SILENT, skill.getId());
		}
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (_mode == StatModifierType.DIFF)
		{
			DIFF_DAMAGE_HOLDER.put(effected.getObjectId(), _damage + ((effector.getMaxHp() * _casterHpMod) / 100));
			effected.addListener(new FunctionEventListener(effected, EventType.ON_CREATURE_DAMAGE_RECEIVED, (OnCreatureDamageReceived event) -> onDamageReceivedDiffEvent(event, effected, skill), this));
		}
		else
		{
			PER_DAMAGE_HOLDER.put(effected.getObjectId(), _damage + ((effector.getMaxHp() * _casterHpMod) / 100));
			effected.addListener(new FunctionEventListener(effected, EventType.ON_CREATURE_DAMAGE_RECEIVED, (OnCreatureDamageReceived event) -> onDamageReceivedPerEvent(event), this));
		}
		
		HITS_HOLDER.put(effected.getObjectId(), 0);
	}
}
