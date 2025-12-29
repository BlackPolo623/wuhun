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
package org.l2jmobius.gameserver.network.clientpackets.crossevent;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.managers.events.CrossEventManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.CrossEventAdvancedRewardHolder;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.crossevent.ExCrossEventInfo;
import org.l2jmobius.gameserver.network.serverpackets.crossevent.ExCrossEventRareReward;

/**
 * @author Smoto
 */
public class RequestCrossEventRareReward extends ClientPacket
{
	@Override
	protected void readImpl()
	{
		// readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final CrossEventAdvancedRewardHolder item = getRareReward();
		if (item == null)
		{
			return;
		}
		
		final ItemHolder reward = new ItemHolder(item.getItemId(), item.getCount());
		player.sendPacket(new ExCrossEventRareReward(true, reward.getId()));
		
		player.setCrossAdvancedRewardCount(-1);
		player.addItem(ItemProcessType.REWARD, reward, player, true);
		player.sendPacket(new ExCrossEventInfo(player));
	}
	
	private CrossEventAdvancedRewardHolder getRareReward()
	{
		final List<CrossEventAdvancedRewardHolder> tempList = new ArrayList<>();
		for (CrossEventAdvancedRewardHolder reward : CrossEventManager.getInstance().getAdvancedRewardList())
		{
			if (Rnd.get(100000) <= reward.getChance())
			{
				tempList.add(reward);
			}
		}
		
		// Prevent tempList to be empty.
		if (tempList.isEmpty())
		{
			return getRareReward();
		}
		
		return tempList.get(Rnd.get(0, tempList.size() - 1));
	}
}
