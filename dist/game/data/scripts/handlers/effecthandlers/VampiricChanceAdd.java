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
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * 吸血觸發機率加成效果。
 * amount 單位為 %：amount=5 表示增加 5% 觸發機率。
 * Creature.java 讀取 ABSORB_DAMAGE_CHANCE 時會除以 100 轉為小數。
 */
public class VampiricChanceAdd extends AbstractEffect
{
	private final double _amount;

	public VampiricChanceAdd(StatSet params)
	{
		_amount = params.getDouble("amount", 0);
	}

	@Override
	public void pump(Creature effected, Skill skill)
	{
		// amount 直接以 % 儲存，Creature.java 讀取時 /100 轉換
		effected.getStat().mergeAdd(Stat.ABSORB_DAMAGE_CHANCE, _amount);
	}
}
