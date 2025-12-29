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
package org.l2jmobius.gameserver.network.serverpackets.attendance;

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.holders.AttendanceItemHolder;
import org.l2jmobius.gameserver.data.xml.AttendanceRewardData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.AttendanceInfoHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius, Serenitty, Lonely
 */
public class ExVipAttendanceList extends ServerPacket
{
	private final int _index;
	private final int _delayreward;
	private final boolean _available;
	private final List<AttendanceItemHolder> _rewardItems;
	
	public ExVipAttendanceList(Player player)
	{
		final AttendanceInfoHolder attendanceInfo = player.getAttendanceInfo();
		_index = attendanceInfo.getRewardIndex();
		_delayreward = player.getAttendanceDelay();
		_available = attendanceInfo.isRewardAvailable();
		_rewardItems = AttendanceRewardData.getInstance().getRewards();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_VIP_ATTENDANCE_LIST.writeId(this, buffer);
		
		buffer.writeInt(_rewardItems.size());
		for (AttendanceItemHolder reward : _rewardItems)
		{
			buffer.writeInt(reward.getItemId());
			buffer.writeLong(reward.getItemCount());
			buffer.writeByte(reward.getHighlight());
		}
		
		buffer.writeInt(1); // MinimumLevel
		buffer.writeInt(_delayreward); // RemainCheckTime
		if (_available)
		{
			buffer.writeByte(_index + 1); // RollBookDay
			if ((_delayreward == 0) && (_available))
			{
				buffer.writeByte(_index + 1); // AttendanceDay
			}
			else
			{
				buffer.writeByte(_index); // AttendanceDay
			}
			
			buffer.writeByte(_index); // RewardDay
			buffer.writeByte(0); // FollowBaseDay
			// buffer.writeByte(_available);
			buffer.writeByte(0); // FollowBaseDay
		}
		else
		{
			buffer.writeByte(_index); // RollBookDay
			if ((_delayreward == 0) && (_available))
			{
				buffer.writeByte(_index + 1); // AttendanceDay
			}
			else
			{
				buffer.writeByte(_index); // AttendanceDay
			}
			
			buffer.writeByte(_index); // RewardDay
			buffer.writeByte(0); // FollowBaseDay
			// buffer.writeByte(_available);
			buffer.writeByte(1); // FollowBaseDay
		}
	}
}
