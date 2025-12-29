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
package org.l2jmobius.gameserver.network.serverpackets.randomcraft;

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.holders.RandomCraftRewardItemHolder;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mode
 */
public class ExCraftRandomInfo extends ServerPacket
{
	private final List<RandomCraftRewardItemHolder> _rewards;
	
	public ExCraftRandomInfo(Player player)
	{
		_rewards = player.getRandomCraft().getRewards();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_CRAFT_RANDOM_INFO.writeId(this, buffer);
		int size = 5;
		buffer.writeInt(size); // size
		for (int i = 0; i < _rewards.size(); i++)
		{
			final RandomCraftRewardItemHolder holder = _rewards.get(i);
			if ((holder != null) && (holder.getItemId() != 0))
			{
				buffer.writeByte(holder.isLocked()); // Locked
				buffer.writeInt(holder.getLockLeft()); // Rolls it will stay locked
				buffer.writeInt(holder.getItemId()); // Item id
				buffer.writeLong(holder.getItemCount()); // Item count
			}
			else
			{
				buffer.writeByte(0);
				buffer.writeInt(0);
				buffer.writeInt(0);
				buffer.writeLong(0);
			}
			
			size--;
		}
		
		// Write missing
		for (int i = size; i > 0; i--)
		{
			buffer.writeByte(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeLong(0);
		}
	}
}
