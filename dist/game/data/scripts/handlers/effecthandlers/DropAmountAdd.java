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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * Adds flat bonuses to drop item min/max quantity after all rate multipliers are applied.
 * Supports multiple items per effect, configured via fromLevel/toLevel on the effect node.
 * @author Custom
 */
public class DropAmountAdd extends AbstractEffect
{
	private static final class DropBonus
	{
		final int itemId;
		final int minAdd;
		final int maxAdd;

		DropBonus(int itemId, int minAdd, int maxAdd)
		{
			this.itemId = itemId;
			this.minAdd = minAdd;
			this.maxAdd = maxAdd;
		}
	}

	private final List<DropBonus> _bonuses = new ArrayList<>();

	public DropAmountAdd(StatSet params)
	{
		for (StatSet item : params.getList("drops", StatSet.class))
		{
			final int itemId = item.getInt(".id");
			final int minAdd = Math.max(0, item.getInt(".minAdd", 0));
			final int maxAdd = Math.max(0, item.getInt(".maxAdd", 0));
			_bonuses.add(new DropBonus(itemId, minAdd, maxAdd));
		}
	}

	@Override
	public boolean canPump(Creature effector, Creature effected, Skill skill)
	{
		return effected.isPlayer();
	}

	@Override
	public void pump(Creature effected, Skill skill)
	{
		final Player player = effected.asPlayer();
		for (DropBonus bonus : _bonuses)
		{
			player.addDropAmountBonus(bonus.itemId, bonus.minAdd, bonus.maxAdd);
		}
	}

	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		for (DropBonus bonus : _bonuses)
		{
			player.removeDropAmountBonus(bonus.itemId, bonus.minAdd, bonus.maxAdd);
		}
	}
}
