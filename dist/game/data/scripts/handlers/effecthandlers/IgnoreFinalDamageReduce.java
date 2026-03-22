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

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * Ignore Final Damage Reduce effect implementation.
 * Reduces target's final damage reduction by a percentage.
 * Applied when calculating damage against targets with FinalDamageReduce.
 *
 * Example: Target has 99% final damage reduce, attacker has 10% ignore
 * - Target's effective reduce: 99% - 10% = 89%
 * - Original damage: 100
 * - After reduce: 100 × (1 - 89/100) = 11
 *
 * @author Custom
 */
public class IgnoreFinalDamageReduce extends AbstractStatEffect
{
	public IgnoreFinalDamageReduce(StatSet params)
	{
		super(params, Stat.IGNORE_FINAL_DAMAGE_REDUCE);
	}
}
