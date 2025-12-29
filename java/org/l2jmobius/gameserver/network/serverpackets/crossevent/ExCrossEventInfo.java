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
package org.l2jmobius.gameserver.network.serverpackets.crossevent;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.events.CrossEventManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.CrossEventHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Smoto
 */
public class ExCrossEventInfo extends ServerPacket
{
	private final Player _player;
	private final int _availableCoupons;
	private final int _dailyResets;
	private final int _endTime;
	private final int _advancedRewards;
	private final int _resetCount;
	
	public ExCrossEventInfo(Player player)
	{
		_player = player;
		_advancedRewards = player.getCrossRewardsCount();
		_resetCount = player.getPlayerResetCount();
		
		final CrossEventManager manager = CrossEventManager.getInstance();
		_availableCoupons = manager.getGameTickets(player);
		_dailyResets = manager.getDailyResets();
		_endTime = manager.getEndTime();
	}
	
	@Override
	protected void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_CROSS_EVENT_INFO.writeId(this, buffer);
		buffer.writeByte(1); // on / off ??
		buffer.writeInt(16); // Cells size.
		
		for (int i = 0; i < 16; i++)
		{
			buffer.writeLong(1);
			buffer.writeByte(checkCell(i));
		}
		
		buffer.writeInt(_availableCoupons); // Count available coupons.
		buffer.writeInt(_advancedRewards); // Advanced rewards available.
		buffer.writeInt(_resetCount); // Resets available.
		buffer.writeInt(_dailyResets); // Maximum daily resets.
		buffer.writeLong(_endTime); // Remaining time.
	}
	
	private boolean checkCell(int cellNumber)
	{
		for (CrossEventHolder cells : _player.getCrossEventCells())
		{
			if (cells.cellId() == cellNumber)
			{
				return true;
			}
		}
		
		return false;
	}
}
