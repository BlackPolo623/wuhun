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
package org.l2jmobius.gameserver.network.serverpackets.huntpass;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.HuntPass;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Serenitty
 */
public class HuntPassInfo extends ServerPacket
{
	private final int _interfaceType;
	private final HuntPass _huntPass;
	private final int _timeEnd;
	private final boolean _isPremium;
	private final int _points;
	private final int _step;
	private final int _rewardStep;
	private final int _premiumRewardStep;
	
	public HuntPassInfo(Player player, int interfaceType)
	{
		_interfaceType = interfaceType;
		_huntPass = player.getHuntPass();
		_timeEnd = _huntPass.getHuntPassDayEnd();
		_isPremium = _huntPass.isPremium();
		_points = _huntPass.getPoints();
		_step = _huntPass.getCurrentStep();
		_rewardStep = _huntPass.getRewardStep();
		_premiumRewardStep = _huntPass.getPremiumRewardStep();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_L2PASS_INFO.writeId(this, buffer);
		buffer.writeByte(_interfaceType);
		buffer.writeInt(_timeEnd); // LeftTime
		buffer.writeByte(_isPremium); // Premium
		buffer.writeInt(_points); // Points
		buffer.writeInt(_step); // CurrentStep
		buffer.writeInt(_rewardStep); // Reward
		buffer.writeInt(_premiumRewardStep); // PremiumReward
	}
}
