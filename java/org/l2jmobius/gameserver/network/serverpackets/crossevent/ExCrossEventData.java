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

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.events.CrossEventManager;
import org.l2jmobius.gameserver.model.actor.holders.player.CrossEventAdvancedRewardHolder;
import org.l2jmobius.gameserver.model.actor.holders.player.CrossEventRegularRewardHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Smoto
 */
public class ExCrossEventData extends ServerPacket
{
	private final List<CrossEventRegularRewardHolder> _cellRewards;
	private final List<CrossEventAdvancedRewardHolder> _advancedRewards;
	private final long _resetCostAmount;
	private final int _displayId;
	
	public ExCrossEventData()
	{
		final CrossEventManager manager = CrossEventManager.getInstance();
		_cellRewards = manager.getRegularRewardsList();
		_advancedRewards = manager.getAdvancedRewardList();
		_resetCostAmount = manager.getResetCostAmount();
		_displayId = manager.getDisplayId();
	}
	
	@Override
	protected void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_CROSS_EVENT_DATA.writeId(this, buffer);
		
		// Normal Cell Rewards.
		buffer.writeInt(_cellRewards.size()); // 4 x 4 Cell
		for (CrossEventRegularRewardHolder item : _cellRewards)
		{
			buffer.writeInt(item.cellReward()); // item id
			buffer.writeLong(item.cellAmount()); // amount
		}
		
		// Advanced Rewards.
		buffer.writeInt(_advancedRewards.size()); // advanced rewards list
		for (CrossEventAdvancedRewardHolder reward : _advancedRewards)
		{
			buffer.writeInt(reward.getItemId()); // Item Id
			buffer.writeLong(reward.getCount()); // Amount
		}
		
		buffer.writeByte(0); // unk - 0
		buffer.writeByte(1); // unk - 1
		
		buffer.writeInt(57); // Reset Item Id
		buffer.writeLong(_resetCostAmount); // Reset Item Amount
		buffer.writeInt(_displayId); // Interface: 0 - Normal, 1 - Summer, 4 - Winter
	}
}
