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

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.FlyType;
import org.l2jmobius.gameserver.model.stats.Formulas;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;

/**
 * @author fruit
 */
public class PullToTarget extends AbstractEffect
{
	private final int _speed;
	private final int _delay;
	private final int _animationSpeed;
	private final FlyType _type;
	private final int _radius;
	
	public PullToTarget(StatSet params)
	{
		_speed = params.getInt("speed", 0);
		_delay = params.getInt("delay", _speed);
		_animationSpeed = params.getInt("animationSpeed", 0);
		_type = params.getEnum("type", FlyType.class, FlyType.WARP_FORWARD); // type 9
		_radius = params.getInt("radius", 100);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcProbability(100, effector, effected, skill);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		// Check if the skill target is valid.
		if (effected == null)
		{
			return;
		}
		
		World.getInstance().forEachVisibleObjectInRange(effected, Creature.class, _radius, nearby ->
		{
			if ((nearby != effected) && (nearby != effector))
			{
				flyToTarget(nearby, effected);
			}
		});
	}
	
	private void flyToTarget(Creature creature, Creature flyToTarget)
	{
		// Prevent pulling raids and town NPCs.
		if (creature.isRaid() || (creature.isNpc() && !creature.isAttackable()))
		{
			return;
		}
		
		// Prevent pulling debuff blocked characters.
		if (creature.isDebuffBlocked())
		{
			return;
		}
		
		// In retail, you get debuff, but you are not even moved if there is obstacle. You are still disabled from using skills and moving though.
		if (GeoEngine.getInstance().canMoveToTarget(creature.getX(), creature.getY(), creature.getZ(), flyToTarget.getX(), flyToTarget.getY(), flyToTarget.getZ(), flyToTarget.getInstanceWorld()))
		{
			creature.broadcastPacket(new FlyToLocation(creature, flyToTarget, _type, _speed, _delay, _animationSpeed));
			creature.setXYZ(flyToTarget.getX(), flyToTarget.getY(), GeoEngine.getInstance().getHeight(flyToTarget.getX(), flyToTarget.getY(), flyToTarget.getZ()) + 20);
			creature.broadcastPacket(new ValidateLocation(creature), false);
			creature.revalidateZone(true);
		}
	}
}
