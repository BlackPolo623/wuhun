/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.effecthandlers;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * Dispel By Slot Probability effect implementation.
 * @author Adry_85, Zoey76
 */
public class DispelBySlotProbability extends AbstractEffect
{
	private final Set<AbnormalType> _dispelAbnormals;
	private final int _rate;
	
	public DispelBySlotProbability(StatSet params)
	{
		final String[] dispelEffects = params.getString("dispel").split(";");
		_rate = params.getInt("rate", 100);
		_dispelAbnormals = new HashSet<>(dispelEffects.length);
		for (String slot : dispelEffects)
		{
			final AbnormalType abnormalType = Enum.valueOf(AbnormalType.class, slot);
			if (abnormalType.equals(AbnormalType.STAT_SKILL) //
				|| abnormalType.equals(AbnormalType.POWER_STAT) //
				|| abnormalType.equals(AbnormalType.STAT_SKILL_COMBAT) //
				|| abnormalType.equals(AbnormalType.STAT_SKILL_PDEF) //
				|| abnormalType.equals(AbnormalType.STAT_SKILL_MDEF) //
				|| abnormalType.equals(AbnormalType.STAT_SKILL_PATK) //
				|| abnormalType.equals(AbnormalType.STAT_SKILL_MATK) //
				|| abnormalType.equals(AbnormalType.STAT_SKILL_STR) //
				|| abnormalType.equals(AbnormalType.STAT_SKILL_DEX) //
				|| abnormalType.equals(AbnormalType.STAT_SKILL_INT) //
				|| abnormalType.equals(AbnormalType.STAT_SKILL_WIT) //
				|| abnormalType.equals(AbnormalType.STAT_SKILL_CON))
			{
				continue;
			}
			
			_dispelAbnormals.add(abnormalType);
		}
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.DISPEL;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((effected == null) || effected.isRaid())
		{
			return;
		}
		
		// The effectlist should already check if it has buff with this abnormal type or not.
		effected.getEffectList().stopEffects(info -> !info.getSkill().isIrreplaceableBuff() && (Rnd.get(100) < _rate) && _dispelAbnormals.contains(info.getSkill().getAbnormalType()), true, true);
	}
}
