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
package org.l2jmobius.gameserver.network.serverpackets.secretshop;

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.events.SecretShopEventManager.SecretShopRewardHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Serenitty
 */
public class ExFestivalBmTopItemInfo extends ServerPacket
{
	private final long _endTime;
	private final boolean _isUseFestivalBm;
	private final List<SecretShopRewardHolder> _activeRewards;
	
	public ExFestivalBmTopItemInfo(long endTime, boolean isUseFestivalBm, List<SecretShopRewardHolder> activeRewards)
	{
		_endTime = endTime;
		_isUseFestivalBm = isUseFestivalBm;
		_activeRewards = activeRewards;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_FESTIVAL_BM_TOP_ITEM_INFO.writeId(this, buffer);
		buffer.writeByte(_isUseFestivalBm ? 1 : 2);
		buffer.writeInt(20);
		buffer.writeInt((int) _endTime / 1000);
		buffer.writeInt(3);
		int written = 0;
		if (_activeRewards != null)
		{
			for (SecretShopRewardHolder reward : _activeRewards)
			{
				if (reward.isTopGrade())
				{
					written++;
					buffer.writeByte(reward.getGrade());
					buffer.writeInt(reward.getId());
					buffer.writeInt((int) reward.getCurrentAmount());
					buffer.writeInt((int) reward.getTotalAmount());
				}
			}
		}
		
		for (; written < 3; written++)
		{
			buffer.writeByte(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
	}
}
