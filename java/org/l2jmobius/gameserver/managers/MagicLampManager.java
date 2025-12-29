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
package org.l2jmobius.gameserver.managers;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.MagicLampConfig;
import org.l2jmobius.gameserver.data.enums.LampType;
import org.l2jmobius.gameserver.data.holders.MagicLampDataHolder;
import org.l2jmobius.gameserver.data.holders.MagicLampHolder;
import org.l2jmobius.gameserver.data.xml.MagicLampData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.serverpackets.magiclamp.ExMagicLampInfo;
import org.l2jmobius.gameserver.network.serverpackets.magiclamp.ExMagicLampResult;

/**
 * @author Serenitty
 */
public class MagicLampManager
{
	private static final List<MagicLampDataHolder> REWARDS = MagicLampData.getInstance().getLamps();
	private static final int REWARD_COUNT = 1;
	
	public MagicLampManager()
	{
	}
	
	public void useMagicLamp(Player player)
	{
		if (REWARDS.isEmpty())
		{
			return;
		}
		
		final Map<LampType, MagicLampHolder> rewards = new EnumMap<>(LampType.class);
		int count = 0;
		while (count == 0) // There should be at least one Magic Lamp reward.
		{
			for (MagicLampDataHolder lamp : REWARDS)
			{
				if ((lamp.getFromLevel() <= player.getLevel()) && (player.getLevel() <= lamp.getToLevel()) && (Rnd.get(100d) < lamp.getChance()))
				{
					rewards.computeIfAbsent(lamp.getType(), _ -> new MagicLampHolder(lamp)).inc();
					if (++count >= REWARD_COUNT)
					{
						break;
					}
				}
			}
		}
		
		rewards.values().forEach(lamp ->
		{
			final int exp = (int) lamp.getExp();
			final int sp = (int) lamp.getSp();
			player.addExpAndSp(exp, sp);
			
			final LampType lampType = lamp.getType();
			player.sendPacket(new ExMagicLampResult(exp, lampType.getGrade()));
			player.sendPacket(new ExMagicLampInfo(player));
			manageSkill(player, lampType);
		});
	}
	
	public void addLampExp(Player player, double exp, int mobLevel, boolean rateModifiers)
	{
		if (!MagicLampConfig.ENABLE_MAGIC_LAMP)
		{
			return;
		}
		
		final int playerLevel = player.getLevel();
		final int levelDifference = mobLevel - playerLevel;
		final double baseContributionPercentage;
		if (playerLevel < 20)
		{
			baseContributionPercentage = 0.4; // 4% for players below level 20.
		}
		else if (playerLevel < 40)
		{
			baseContributionPercentage = 0.3; // 3% for players between 20 and 39.
		}
		else if (playerLevel < 60)
		{
			baseContributionPercentage = 0.2; // 2% for players between 40 and 59.
		}
		else if (playerLevel <= 70)
		{
			baseContributionPercentage = 0.15; // 1.5% for players between 60 and 70.
		}
		else if (playerLevel <= 85)
		{
			baseContributionPercentage = 0.1; // 1% for players between 71 and 85.
		}
		else
		{
			baseContributionPercentage = 0.08; // 0.8% for players above level 85.
		}
		
		double levelMultiplier = 1.0;
		if (levelDifference > 0)
		{
			levelMultiplier += levelDifference * 0.1;
		}
		else if (levelDifference < 0)
		{
			int absoluteDifference = Math.abs(levelDifference);
			double penalty = absoluteDifference * 0.2;
			levelMultiplier = Math.max(0.0, levelMultiplier - penalty);
		}
		
		final long baseLampExp = (long) (MagicLampConfig.MAGIC_LAMP_MAX_LEVEL_EXP * baseContributionPercentage);
		final long lampExp = (long) ((Math.max(exp, baseLampExp) * levelMultiplier * player.getStat().getExpBonusMultiplier()) * (rateModifiers ? (MagicLampConfig.MAGIC_LAMP_CHARGE_RATE * player.getStat().getMul(Stat.MAGIC_LAMP_EXP_RATE, 1)) : 1));
		long totalLampExp = lampExp + player.getLampExp();
		if (totalLampExp >= MagicLampConfig.MAGIC_LAMP_MAX_LEVEL_EXP)
		{
			totalLampExp %= MagicLampConfig.MAGIC_LAMP_MAX_LEVEL_EXP;
			useMagicLamp(player);
		}
		
		// Update the player's lamp XP and send packet.
		player.setLampExp((int) totalLampExp);
		player.sendPacket(new ExMagicLampInfo(player));
	}
	
	private void manageSkill(Player player, LampType lampType)
	{
		final Skill lampSkill;
		
		switch (lampType)
		{
			case RED:
			{
				lampSkill = CommonSkill.RED_LAMP.getSkill();
				break;
			}
			case PURPLE:
			{
				lampSkill = CommonSkill.PURPLE_LAMP.getSkill();
				break;
			}
			case BLUE:
			{
				lampSkill = CommonSkill.BLUE_LAMP.getSkill();
				break;
			}
			case GREEN:
			{
				lampSkill = CommonSkill.GREEN_LAMP.getSkill();
				break;
			}
			default:
			{
				lampSkill = null;
				break;
			}
		}
		
		if (lampSkill != null)
		{
			player.breakAttack(); // *TODO Stop Autohunt only for cast a skill?, nope.
			player.breakCast();
			
			player.doCast(lampSkill);
		}
	}
	
	public static MagicLampManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MagicLampManager INSTANCE = new MagicLampManager();
	}
}
