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
package org.l2jmobius.gameserver.network.clientpackets.dailymission;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.MissionLevel;
import org.l2jmobius.gameserver.model.MissionLevelHolder;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.MissionLevelPlayerDataHolder;
import org.l2jmobius.gameserver.model.actor.request.RewardRequest;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.dailymission.ExMissionLevelRewardList;

/**
 * @author Index
 */
public class RequestMissionLevelReceiveReward extends ClientPacket
{
	private final MissionLevelHolder _holder = MissionLevel.getInstance().getMissionBySeason(MissionLevel.getInstance().getCurrentSeason());
	private int _level;
	private int _rewardType;
	
	@Override
	protected void readImpl()
	{
		_level = readInt();
		_rewardType = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.hasRequest(RewardRequest.class))
		{
			return;
		}
		
		player.addRequest(new RewardRequest(player));
		
		final MissionLevelPlayerDataHolder info = player.getMissionLevelProgress();
		switch (_rewardType)
		{
			case 1:
			{
				if (!_holder.getNormalRewards().containsKey(_level) || info.getCollectedNormalRewards().contains(_level) || ((info.getCurrentLevel() != _level) && (info.getCurrentLevel() < _level)))
				{
					player.removeRequest(RewardRequest.class);
					return;
				}
				
				final ItemHolder reward = _holder.getNormalRewards().get(_level);
				player.addItem(ItemProcessType.REWARD, reward.getId(), reward.getCount(), null, true);
				info.addToCollectedNormalRewards(_level);
				info.storeInfoInVariable(player);
				break;
			}
			case 2:
			{
				if (!_holder.getKeyRewards().containsKey(_level) || info.getCollectedKeyRewards().contains(_level) || ((info.getCurrentLevel() != _level) && (info.getCurrentLevel() < _level)))
				{
					player.removeRequest(RewardRequest.class);
					return;
				}
				
				final ItemHolder reward = _holder.getKeyRewards().get(_level);
				player.addItem(ItemProcessType.REWARD, reward.getId(), reward.getCount(), null, true);
				info.addToCollectedKeyReward(_level);
				info.storeInfoInVariable(player);
				break;
			}
			case 3:
			{
				if ((_holder.getSpecialReward() == null) || info.getCollectedSpecialReward() || ((info.getCurrentLevel() != _level) && (info.getCurrentLevel() < _level)))
				{
					player.removeRequest(RewardRequest.class);
					return;
				}
				
				final ItemHolder reward = _holder.getSpecialReward();
				player.addItem(ItemProcessType.REWARD, reward.getId(), reward.getCount(), null, true);
				info.setCollectedSpecialReward(true);
				info.storeInfoInVariable(player);
				break;
			}
			case 4:
			{
				if (!_holder.getBonusRewardIsAvailable() || (_holder.getBonusReward() == null) || !info.getCollectedSpecialReward() || info.getCollectedBonusReward() || ((info.getCurrentLevel() != _level) && (info.getCurrentLevel() < _level)))
				{
					player.removeRequest(RewardRequest.class);
					return;
				}
				
				if (_holder.getBonusRewardByLevelUp())
				{
					int maxNormalLevel = _holder.getBonusLevel();
					int availableReward = -1;
					for (int level = maxNormalLevel; level <= _holder.getMaxLevel(); level++)
					{
						if ((level <= info.getCurrentLevel()) && !info.getListOfCollectedBonusRewards().contains(level))
						{
							availableReward = level;
							break;
						}
					}
					
					if (availableReward != -1)
					{
						info.addToListOfCollectedBonusRewards(availableReward);
					}
					else
					{
						player.removeRequest(RewardRequest.class);
						return;
					}
				}
				else
				{
					info.setCollectedBonusReward(true);
				}
				
				final ItemHolder reward = _holder.getBonusReward();
				player.addItem(ItemProcessType.REWARD, reward.getId(), reward.getCount(), null, true);
				info.storeInfoInVariable(player);
				break;
			}
		}
		
		player.sendPacket(new ExMissionLevelRewardList(player));
		
		ThreadPool.schedule(() -> player.removeRequest(RewardRequest.class), 300);
	}
}
