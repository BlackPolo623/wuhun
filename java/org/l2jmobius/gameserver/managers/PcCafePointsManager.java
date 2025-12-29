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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class PcCafePointsManager
{
	public void run(Player player)
	{
		// PC-points only premium accounts
		if (!PremiumSystemConfig.PC_CAFE_ENABLED || !PremiumSystemConfig.PC_CAFE_RETAIL_LIKE || (!player.hasEnteredWorld()))
		{
			return;
		}
		
		ThreadPool.scheduleAtFixedRate(() -> giveRetailPcCafePont(player), PremiumSystemConfig.PC_CAFE_REWARD_TIME, PremiumSystemConfig.PC_CAFE_REWARD_TIME);
	}
	
	public void giveRetailPcCafePont(Player player)
	{
		if (!PremiumSystemConfig.PC_CAFE_ENABLED || !PremiumSystemConfig.PC_CAFE_RETAIL_LIKE || (player.isOnlineInt() == 0) || (!player.hasPremiumStatus() && PremiumSystemConfig.PC_CAFE_ONLY_PREMIUM) || player.isInOfflineMode())
		{
			return;
		}
		
		int points = PremiumSystemConfig.ACQUISITION_PC_CAFE_RETAIL_LIKE_POINTS;
		
		if (points >= PremiumSystemConfig.PC_CAFE_MAX_POINTS)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EARNED_THE_MAXIMUM_NUMBER_OF_ABUNDANCE_POINTS);
			return;
		}
		
		if (PremiumSystemConfig.PC_CAFE_RANDOM_POINT)
		{
			points = Rnd.get(points / 2, points);
		}
		
		SystemMessage message = null;
		if (PremiumSystemConfig.PC_CAFE_ENABLE_DOUBLE_POINTS && (Rnd.get(100) < PremiumSystemConfig.PC_CAFE_DOUBLE_POINTS_CHANCE))
		{
			points *= 2;
			message = new SystemMessage(SystemMessageId.DOUBLE_POINTS_YOU_HAVE_EARNED_ABUNDANCE_POINTS_X_S1);
		}
		else
		{
			message = new SystemMessage(SystemMessageId.YOU_HAVE_RECEIVED_S1_POINT_S_AS_A_DAILY_REWARD_FOR_USING_BLESSING_OF_ABUNDANCE);
		}
		
		if ((player.getPcCafePoints() + points) > PremiumSystemConfig.PC_CAFE_MAX_POINTS)
		{
			points = PremiumSystemConfig.PC_CAFE_MAX_POINTS - player.getPcCafePoints();
		}
		
		message.addLong(points);
		player.sendPacket(message);
		player.setPcCafePoints(player.getPcCafePoints() + points);
		player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), points, 1));
	}
	
	public void givePcCafePoint(Player player, double exp)
	{
		if (PremiumSystemConfig.PC_CAFE_RETAIL_LIKE || !PremiumSystemConfig.PC_CAFE_ENABLED || player.isInsideZone(ZoneId.PEACE) || player.isInsideZone(ZoneId.PVP) || player.isInsideZone(ZoneId.SIEGE) || (player.isOnlineInt() == 0) || player.isJailed())
		{
			return;
		}
		
		// PC-points only premium accounts
		if (PremiumSystemConfig.PC_CAFE_ONLY_PREMIUM && !player.hasPremiumStatus())
		{
			return;
		}
		
		if (PremiumSystemConfig.PC_CAFE_ONLY_VIP && (player.getVipTier() <= 0))
		{
			return;
		}
		
		if (player.getPcCafePoints() >= PremiumSystemConfig.PC_CAFE_MAX_POINTS)
		{
			final SystemMessage message = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_THE_MAXIMUM_NUMBER_OF_ABUNDANCE_POINTS);
			player.sendPacket(message);
			return;
		}
		
		int points = (int) (exp * 0.0001 * PremiumSystemConfig.PC_CAFE_POINT_RATE);
		if (PremiumSystemConfig.PC_CAFE_RANDOM_POINT)
		{
			points = Rnd.get(points / 2, points);
		}
		
		if ((points == 0) && (exp > 0) && PremiumSystemConfig.PC_CAFE_REWARD_LOW_EXP_KILLS && (Rnd.get(100) < PremiumSystemConfig.PC_CAFE_LOW_EXP_KILLS_CHANCE))
		{
			points = 1; // minimum points
		}
		
		if (points <= 0)
		{
			return;
		}
		
		SystemMessage message = null;
		if (PremiumSystemConfig.PC_CAFE_ENABLE_DOUBLE_POINTS && (Rnd.get(100) < PremiumSystemConfig.PC_CAFE_DOUBLE_POINTS_CHANCE))
		{
			points *= 2;
			message = new SystemMessage(SystemMessageId.DOUBLE_POINTS_YOU_HAVE_EARNED_ABUNDANCE_POINTS_X_S1);
		}
		else
		{
			message = new SystemMessage(SystemMessageId.DOUBLE_POINTS_YOU_HAVE_EARNED_ABUNDANCE_POINTS_X_S1);
		}
		
		if ((player.getPcCafePoints() + points) > PremiumSystemConfig.PC_CAFE_MAX_POINTS)
		{
			points = PremiumSystemConfig.PC_CAFE_MAX_POINTS - player.getPcCafePoints();
		}
		
		message.addLong(points);
		player.sendPacket(message);
		player.setPcCafePoints(player.getPcCafePoints() + points);
		player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), points, 1));
	}
	
	/**
	 * Gets the single instance of {@code PcCafePointsManager}.
	 * @return single instance of {@code PcCafePointsManager}
	 */
	public static PcCafePointsManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PcCafePointsManager INSTANCE = new PcCafePointsManager();
	}
}
