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
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.AchievementBoxHolder;
import org.l2jmobius.gameserver.model.actor.holders.player.AchievementBoxInfoHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Serenitty
 */
public class ExSteadyOneBoxUpdate extends ServerPacket
{
	private final AchievementBoxHolder _achievementBox;
	private final int _slotId;
	
	public ExSteadyOneBoxUpdate(Player player, int slotId)
	{
		_achievementBox = player.getAchievementBox();
		_slotId = slotId;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_STEADY_ONE_BOX_UPDATE.writeId(this, buffer);
		buffer.writeInt(_achievementBox.getMonsterPoints());
		buffer.writeInt(_achievementBox.getPvpPoints());
		
		final AchievementBoxInfoHolder boxholder = _achievementBox.getAchievementBox().get(_slotId - 1);
		buffer.writeInt(_slotId);
		buffer.writeInt(boxholder.getState().ordinal());
		buffer.writeInt(boxholder.getType().ordinal());
		
		final int rewardTimeStage = ((int) ((_achievementBox.getBoxOpenTime() - System.currentTimeMillis()) / 1000));
		buffer.writeInt(rewardTimeStage > 0 ? rewardTimeStage : 0);
	}
}
