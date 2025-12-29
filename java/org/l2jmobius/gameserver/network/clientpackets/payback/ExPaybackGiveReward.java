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
package org.l2jmobius.gameserver.network.clientpackets.payback;

import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.managers.events.PaybackManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.payback.PaybackGiveReward;

public class ExPaybackGiveReward extends ClientPacket
{
	private int _eventId;
	private int _index;
	
	@Override
	protected void readImpl()
	{
		_eventId = readByte();
		_index = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final PaybackManager manager = PaybackManager.getInstance();
		final long consumed = manager.getPlayerConsumedProgress(player.getObjectId());
		final List<Integer> rewardStatus = manager.getPlayerMissionProgress(player.getObjectId());
		if ((rewardStatus == null) || rewardStatus.get(_index - 1).equals(1) || (manager.getRewardsById(_index) == null))
		{
			player.sendPacket(new PaybackGiveReward(false, _eventId, _index));
			return;
		}
		
		final long count = manager.getRewardsById(_index).getCount();
		if (count > consumed)
		{
			player.sendPacket(new PaybackGiveReward(false, _eventId, _index));
			return;
		}
		
		for (ItemChanceHolder item : manager.getRewardsById(_index).getRewards())
		{
			if (item.getId() == -1)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.NOTHING_HAPPENED));
			}
			else if (Rnd.get(100) <= item.getChance())
			{
				player.addItem(ItemProcessType.REWARD, item.getId(), item.getCount(), item.getEnchantmentLevel(), player, true);
			}
		}
		
		manager.changeMissionProgress(player.getObjectId(), _index - 1, 1);
		manager.storePlayerProgress(player);
		player.sendPacket(new PaybackGiveReward(true, _eventId, _index));
	}
}
