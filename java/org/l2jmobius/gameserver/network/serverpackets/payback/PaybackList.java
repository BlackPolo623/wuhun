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
package org.l2jmobius.gameserver.network.serverpackets.payback;

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.events.PaybackManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class PaybackList extends ServerPacket
{
	private final Player _player;
	private final int _eventID;
	
	public PaybackList(Player player, int EventID)
	{
		_player = player;
		_eventID = EventID;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		final PaybackManager manager = PaybackManager.getInstance();
		final List<Integer> rewardStatus = manager.getPlayerMissionProgress(_player.getObjectId());
		if ((rewardStatus == null) || rewardStatus.isEmpty())
		{
			return;
		}
		
		ServerPackets.EX_PAYBACK_LIST.writeId(this, buffer);
		buffer.writeInt(manager.getRewards().size());
		for (int id : manager.getRewards().keySet())
		{
			buffer.writeInt(manager.getRewards().get(id).getRewards().size());
			for (ItemChanceHolder reward : manager.getRewards().get(id).getRewards())
			{
				buffer.writeInt(reward.getId());
				buffer.writeInt(Math.toIntExact(reward.getCount()));
			}
			
			buffer.writeByte(id);
			buffer.writeInt(Math.toIntExact(manager.getRewards().get(id).getCount()));
			buffer.writeByte(rewardStatus.get(id - 1));
		}
		
		buffer.writeByte(_eventID);
		buffer.writeInt(Math.toIntExact(manager.getEndTime() / 1000));
		buffer.writeInt(manager.getCoinID());
		buffer.writeInt((int) manager.getPlayerConsumedProgress(_player.getObjectId()));
	}
}
