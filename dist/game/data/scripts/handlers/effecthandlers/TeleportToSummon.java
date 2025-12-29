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

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.FlyType;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * @author Liamxroy
 */
public class TeleportToSummon extends AbstractEffect
{
	private final int _summonId;
	private final double _maxDistance;
	
	public TeleportToSummon(StatSet params)
	{
		_summonId = params.getInt("summonId", 0);
		_maxDistance = params.getDouble("distance", -1);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.TELEPORT_TO_TARGET;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return effected.hasServitors();
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Creature spawnedCreature = effected.asPlayer().getRecallCreature();
		if (spawnedCreature != null)
		{
			teleportToSummon(effector, effected, spawnedCreature);
		}
		else
		{
			final Summon summon = effected.asPlayer().getFirstServitor();
			if (_summonId > 0)
			{
				for (Summon servitor : effected.getServitors().values())
				{
					if ((servitor != null) && (_summonId == servitor.getId()) && !servitor.isDisabled())
					{
						teleportToSummon(effector, effected, servitor);
					}
				}
			}
			else
			{
				teleportToSummon(effector, effected, summon);
			}
		}
	}
	
	public void teleportToSummon(Creature effector, Creature effected, Creature summon)
	{
		if ((_maxDistance > 0) && (effector.calculateDistance2D(summon) >= _maxDistance))
		{
			return;
		}
		
		final int px = summon.getX();
		final int py = summon.getY();
		double ph = LocationUtil.convertHeadingToDegree(summon.getHeading());
		
		ph += 180;
		if (ph > 360)
		{
			ph -= 360;
		}
		
		ph = (Math.PI * ph) / 180;
		final int x = (int) (px + (25 * Math.cos(ph)));
		final int y = (int) (py + (25 * Math.sin(ph)));
		final int z = summon.getZ();
		
		final Location loc = GeoEngine.getInstance().getValidLocation(effector.getX(), effector.getY(), effector.getZ(), x, y, z, effector.getInstanceWorld());
		
		effector.getAI().setIntention(Intention.IDLE);
		effector.broadcastPacket(new FlyToLocation(effector, loc.getX(), loc.getY(), loc.getZ(), FlyType.DUMMY));
		effector.abortAttack();
		effector.abortCast();
		effector.setXYZ(loc);
		effector.broadcastPacket(new ValidateLocation(effector));
		effected.revalidateZone(true);
	}
}
