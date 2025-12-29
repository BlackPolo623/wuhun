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
package org.l2jmobius.gameserver.network.serverpackets.achievementbox;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.AchievementBoxConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Serenitty
 */
public class ExSteadyBoxUiInit extends ServerPacket
{
	private static final int[] OPEN_PRICE =
	{
		500,
		1000,
		1500
	};
	private static final int[] WAIT_TIME =
	{
		0,
		60,
		180,
		360,
		540
	};
	private static final int[] TIME_PRICE =
	{
		100,
		500,
		1000,
		1500,
		2000
	};
	
	private final Player _player;
	
	public ExSteadyBoxUiInit(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_STEADY_BOX_UI_INIT.writeId(this, buffer);
		
		buffer.writeInt(AchievementBoxConfig.ACHIEVEMENT_BOX_POINTS_FOR_REWARD);
		buffer.writeInt(AchievementBoxConfig.ACHIEVEMENT_BOX_PVP_POINTS_FOR_REWARD);
		if (AchievementBoxConfig.ENABLE_ACHIEVEMENT_PVP)
		{
			buffer.writeInt(2); // EventID Normal Point + Pvp Point Bar
		}
		else
		{
			buffer.writeInt(0); // EventID Normal Point + Pvp Point Bar
		}
		
		buffer.writeInt(0); // nEventStartTime time for limitkill
		buffer.writeInt(_player.getAchievementBox().pvpEndDate());
		
		buffer.writeInt(OPEN_PRICE.length);
		for (int i = 0; i < OPEN_PRICE.length; i++)
		{
			buffer.writeInt(i + 1);
			buffer.writeInt(Inventory.LCOIN_ID);
			buffer.writeLong(OPEN_PRICE[i]);
		}
		
		buffer.writeInt(TIME_PRICE.length);
		for (int i = 0; i < TIME_PRICE.length; i++)
		{
			buffer.writeInt(WAIT_TIME[i]);
			buffer.writeInt(Inventory.LCOIN_ID);
			buffer.writeLong(TIME_PRICE[i]);
		}
		
		final int rewardTimeStage = (int) (_player.getAchievementBox().getBoxOpenTime() - System.currentTimeMillis()) / 1000;
		buffer.writeInt(rewardTimeStage > 0 ? rewardTimeStage : 0);
	}
}
