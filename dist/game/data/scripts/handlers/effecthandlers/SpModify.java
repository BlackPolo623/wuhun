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
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * @author Sdw
 */
public class SpModify extends AbstractStatAddEffect
{
	private final int _minLevel;
	private final int _maxLevel;
	
	public SpModify(StatSet params)
	{
		super(params, Stat.BONUS_SP);
		_minLevel = params.getInt("minLevel", -1);
		_maxLevel = params.getInt("maxLevel", -1);
	}
	
	@Override
	public void pump(Creature effected, Skill skill)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		// Check level restriction.
		if ((_minLevel > 0) && (_maxLevel > 0))
		{
			final int level = player.getLevel();
			if ((level < _minLevel) || (level > _maxLevel))
			{
				return;
			}
		}
		
		effected.getStat().mergeAdd(Stat.BONUS_SP, _amount);
		
		player.sendUserBoostStat();
	}
}