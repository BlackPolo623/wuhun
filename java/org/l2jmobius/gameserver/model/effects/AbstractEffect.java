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
package org.l2jmobius.gameserver.model.effects;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * Abstract effect implementation.<br>
 * Instant effects should not override {@link #onExit(Creature, Creature, Skill)}.<br>
 * Instant effects should not override {@link #canStart(Creature, Creature, Skill)}, all checks should be done {@link #onStart(Creature, Creature, Skill, Item)}.<br>
 * Do not call super class methods {@link #onStart(Creature, Creature, Skill, Item)} nor {@link #onExit(Creature, Creature, Skill)}.
 * @author Zoey76, Mobius
 */
public abstract class AbstractEffect
{
	protected static final Logger LOGGER = Logger.getLogger(AbstractEffect.class.getName());
	
	private int _ticks;
	
	/**
	 * Gets the effect ticks
	 * @return the ticks
	 */
	public int getTicks()
	{
		return _ticks;
	}
	
	/**
	 * Sets the effect ticks
	 * @param ticks the ticks
	 */
	protected void setTicks(int ticks)
	{
		_ticks = ticks;
	}
	
	public double getTicksMultiplier()
	{
		return (getTicks() * PlayerConfig.EFFECT_TICK_RATIO) / 1000f;
	}
	
	/**
	 * Calculates whether this effects land or not.<br>
	 * If it lands will be scheduled and added to the character effect list.<br>
	 * Override in effect implementation to change behavior.<br>
	 * <b>Warning:</b> Must be used only for instant effects continuous effects will not call this they have their success handled by activate_rate.
	 * @param effector
	 * @param effected
	 * @param skill
	 * @return {@code true} if this effect land, {@code false} otherwise
	 */
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return true;
	}
	
	/**
	 * Verify if the buff can start.<br>
	 * Used for continuous effects.
	 * @param effector
	 * @param effected
	 * @param skill
	 * @return {@code true} if all the start conditions are meet, {@code false} otherwise
	 */
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return true;
	}
	
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
	}
	
	public void continuousInstant(Creature effector, Creature effected, Skill skill, Item item)
	{
	}
	
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
	}
	
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
	}
	
	/**
	 * Called on each tick.<br>
	 * If the abnormal time is lesser than zero it will last forever.
	 * @param effector
	 * @param effected
	 * @param skill
	 * @param item
	 * @return if {@code true} this effect will continue forever, if {@code false} it will stop after abnormal time has passed
	 */
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		return false;
	}
	
	/**
	 * Get the effect flags.
	 * @return bit flag for current effect
	 */
	public long getEffectFlags()
	{
		return EffectFlag.NONE.getMask();
	}
	
	public boolean checkCondition(Object obj)
	{
		return true;
	}
	
	/**
	 * Verify if this effect is an instant effect.
	 * @return {@code true} if this effect is instant, {@code false} otherwise
	 */
	public boolean isInstant()
	{
		return false;
	}
	
	/**
	 * @param effector
	 * @param effected
	 * @param skill
	 * @return {@code true} if pump can be invoked, {@code false} otherwise
	 */
	public boolean canPump(Creature effector, Creature effected, Skill skill)
	{
		return true;
	}
	
	/**
	 * @param effected
	 * @param skill
	 */
	public void pump(Creature effected, Skill skill)
	{
	}
	
	/**
	 * @return if pumped at a later time.
	 */
	public boolean delayPump()
	{
		return false;
	}
	
	/**
	 * Get this effect's type.<br>
	 * TODO: Remove.
	 * @return the effect type
	 */
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}
	
	@Override
	public String toString()
	{
		return "Effect " + getClass().getSimpleName();
	}
}
