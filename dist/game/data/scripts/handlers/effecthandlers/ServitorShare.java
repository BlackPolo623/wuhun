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

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * ServitorShare effect — 寵物能力共享給主人
 *
 * 作用：玩家持有此被動技能時，召喚的寵物（Pet / Servitor）的指定能力值
 *       會以「固定百分比」直接加算到玩家身上。
 *
 * 特性：共享的數值使用 mergeFinalAdd()，在所有加乘計算完成後才加入，
 *       不受玩家本身任何 buff / 裝備加成的影響。
 *
 * @author Mobius (original), Custom (pet-to-player rewrite)
 */
public class ServitorShare extends AbstractEffect
{
	/** stat → 共享百分比（0.0 ~ 1.0），從 XML 值 / 100 換算而來 */
	private final Map<Stat, Float> _sharedStats = new EnumMap<>(Stat.class);

	public ServitorShare(StatSet params)
	{
		if (params.isEmpty())
		{
			return;
		}

		for (Entry<String, Object> param : params.getSet().entrySet())
		{
			_sharedStats.put(Stat.valueOf(param.getKey()), Float.parseFloat((String) param.getValue()) / 100f);
		}
	}

	@Override
	public boolean delayPump()
	{
		// 延遲執行：等所有其他 pump 完成後（含召喚獸的 pump）再計算共享值
		return true;
	}

	/**
	 * 只對玩家（主人）本身觸發，不對召喚獸觸發。
	 */
	@Override
	public boolean canPump(Creature effector, Creature effected, Skill skill)
	{
		return effected.isPlayer();
	}

	@Override
	public void pump(Creature effected, Skill skill)
	{
		if (!effected.isPlayer())
		{
			return;
		}

		final Player player = effected.asPlayer();
		final CreatureStat playerStatObj = player.getStat();
		boolean applied = false;

		// === 處理 Pet（實體寵物，如孵化系統的寵物）===
		if (player.hasPet())
		{
			final Summon pet = player.getPet();
			final CreatureStat petStat = pet.getStat();
			for (Entry<Stat, Float> entry : _sharedStats.entrySet())
			{
				final Stat stat = entry.getKey();
				final double petValue = petStat.getValue(stat);
				if (petValue > 0)
				{
					// 使用 mergeFinalAdd：加在所有乘算之後，不被任何加成再次放大
					playerStatObj.mergeFinalAdd(stat, petValue * entry.getValue());
				}
			}
			applied = true;
		}

		// === 處理 Servitor（法術召喚獸）===
		if (player.hasServitors())
		{
			for (Summon summon : player.getServitors().values())
			{
				final CreatureStat summonStat = summon.getStat();
				for (Entry<Stat, Float> entry : _sharedStats.entrySet())
				{
					final Stat stat = entry.getKey();
					final double summonValue = summonStat.getValue(stat);
					if (summonValue > 0)
					{
						playerStatObj.mergeFinalAdd(stat, summonValue * entry.getValue());
					}
				}
				applied = true;
			}
		}

		// 若沒有任何召喚物，什麼都不加（等同技能效果為 0）
	}

	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		// 技能失效時重新計算玩家 stats，清除已加入的 _statsFinalAdd
		if (effected.isPlayer())
		{
			effected.getStat().recalculateStats(true);
		}
	}
}
