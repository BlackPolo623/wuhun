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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * Give physical attack when attack speed exceeding limits by %.
 * @author SaltyMike
 */
public class ExceedingPhysicalAttackSpeed extends AbstractEffect
{
	private final int _percent;
	
	public ExceedingPhysicalAttackSpeed(StatSet params)
	{
		_percent = params.getInt("percent", 100);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final double currentPAtkSpd = effector.getPAtkSpd();
		final double bonusSpeed = (currentPAtkSpd * _percent) / 100.0;
		final double newPAtkSpd = currentPAtkSpd + bonusSpeed;
		if (newPAtkSpd > PlayerConfig.MAX_PATK_SPEED)
		{
			final double bonusSpeedToCap = PlayerConfig.MAX_PATK_SPEED - currentPAtkSpd;
			final double excessBonusSpeed = bonusSpeed - bonusSpeedToCap;
			effector.getStat().mergeAdd(Stat.PHYSICAL_ATTACK_SPEED, bonusSpeedToCap);
			
			final double currentPAtk = effector.getPAtk();
			final double bonusPAtk = currentPAtk + excessBonusSpeed;
			effector.getStat().mergeMul(Stat.PHYSICAL_ATTACK, bonusPAtk);
		}
		else
		{
			effector.getStat().mergeAdd(Stat.PHYSICAL_ATTACK_SPEED, bonusSpeed);
		}
	}
}
