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
	
	// [自定義修改] 優化獎勵抽選機制：
	// 1. 基數從 100000 改為 1000（配合 Holder 的 *10），減少無效重試
	// 2. 遞迴改為 while 迴圈，避免 StackOverflow
	// 3. 加上最大重試次數保護（1000次），防止無限迴圈
	private static final int RARE_REWARD_RANDOM_BASE = 1000;
	private static final int RARE_REWARD_MAX_RETRIES = 1000;

	private CrossEventAdvancedRewardHolder getRareReward()
	{
		for (int attempt = 0; attempt < RARE_REWARD_MAX_RETRIES; attempt++)
		{
			final List<CrossEventAdvancedRewardHolder> tempList = new ArrayList<>();
			for (CrossEventAdvancedRewardHolder reward : CrossEventManager.getInstance().getAdvancedRewardList())
			{
				if (Rnd.get(RARE_REWARD_RANDOM_BASE) <= reward.getChance())
				{
					tempList.add(reward);
				}
			}

			if (!tempList.isEmpty())
			{
				return tempList.get(Rnd.get(0, tempList.size() - 1));
			}
		}

		// 保底：重試耗盡時回傳第一個獎勵（正常不應該發生）
		return CrossEventManager.getInstance().getAdvancedRewardList().get(0);
	}
}
