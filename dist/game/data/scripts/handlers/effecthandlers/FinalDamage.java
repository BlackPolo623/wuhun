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
 * Final Damage effect implementation.
 * Increases final damage dealt by a percentage.
 * This is applied as the last multiplier in damage calculation.
 *
 * Example: 30% final damage bonus
 * - Original damage: 100
 * - Final damage: 100 × (1 + 30/100) = 130
 *
 * @author 黑普羅
 */
public class FinalDamage extends AbstractStatEffect
{
	public FinalDamage(StatSet params)
	{
		super(params, Stat.FINAL_DAMAGE_RATE);
	}
}