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
package org.l2jmobius.gameserver.network.clientpackets.huntpass;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.data.xml.HuntPassData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.HuntPass;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.RewardRequest;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.huntpass.HuntPassInfo;
import org.l2jmobius.gameserver.network.serverpackets.huntpass.HuntPassSayhasSupportInfo;
import org.l2jmobius.gameserver.network.serverpackets.huntpass.HuntPassSimpleInfo;

/**
 * @author Serenitty, Mobius, Fakee
 */
public class RequestHuntPassRewardAll extends ClientPacket
{
	private int _huntPassType;
	
	@Override
	protected void readImpl()
	{
		_huntPassType = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!OfflineTradeConfig.OFFLINE_DISCONNECT_SAME_ACCOUNT || !OfflinePlayConfig.OFFLINE_PLAY_DISCONNECT_SAME_ACCOUNT)
		{
			int sameAccountPlayers = 0;
			for (Player worldPlayer : World.getInstance().getPlayers())
			{
				if (worldPlayer.getAccountName().equals(player.getAccountName()))
				{
					sameAccountPlayers++;
				}
			}
			
			if (sameAccountPlayers > 1)
			{
				player.sendMessage("Hunting rewards are shared across the account and cannot be received if more than one character is online simultaneously.");
				return;
			}
		}
		
		if (player.hasRequest(RewardRequest.class))
		{
			return;
		}
		
		player.addRequest(new RewardRequest(player));
		
		int rewardIndex;
		int premiumRewardIndex;
		boolean inventoryLimitReached = false;
		final HuntPass huntPass = player.getHuntPass();
		REWARD_LOOP: while (true)
		{
			rewardIndex = huntPass.getRewardStep();
			premiumRewardIndex = huntPass.getPremiumRewardStep();
			if ((rewardIndex >= HuntPassData.getInstance().getRewardsCount()) && (premiumRewardIndex >= HuntPassData.getInstance().getPremiumRewardsCount()))
			{
				break REWARD_LOOP;
			}
			
			ItemHolder reward = null;
			if (!huntPass.isPremium())
			{
				if (rewardIndex >= huntPass.getCurrentStep())
				{
					break REWARD_LOOP;
				}
				
				reward = HuntPassData.getInstance().getRewards().get(rewardIndex);
			}
			else
			{
				if (premiumRewardIndex >= huntPass.getCurrentStep())
				{
					break REWARD_LOOP;
				}
				
				if (rewardIndex < HuntPassData.getInstance().getRewardsCount())
				{
					reward = HuntPassData.getInstance().getRewards().get(rewardIndex);
				}
				else if (premiumRewardIndex < HuntPassData.getInstance().getPremiumRewardsCount())
				{
					reward = HuntPassData.getInstance().getPremiumRewards().get(premiumRewardIndex);
				}
			}
			
			if (reward == null)
			{
				break REWARD_LOOP;
			}
			
			final ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(reward.getId());
			final long weight = itemTemplate.getWeight() * reward.getCount();
			final long slots = itemTemplate.isStackable() ? 1 : reward.getCount();
			if (!player.getInventory().validateWeight(weight) || !player.getInventory().validateCapacity(slots))
			{
				player.sendPacket(SystemMessageId.YOUR_INVENTORY_S_WEIGHT_SLOT_LIMIT_HAS_BEEN_EXCEEDED_SO_YOU_CANNOT_RECEIVE_THE_REWARD_PLEASE_FREE_UP_SOME_SPACE_AND_TRY_AGAIN);
				inventoryLimitReached = true;
				break REWARD_LOOP;
			}
			
			normalReward(player);
			premiumReward(player);
			huntPass.setRewardStep(rewardIndex + 1);
		}
		
		if (!inventoryLimitReached)
		{
			huntPass.setRewardAlert(false);
		}
		
		huntPass.store();
		
		player.sendPacket(new HuntPassInfo(player, _huntPassType));
		player.sendPacket(new HuntPassSayhasSupportInfo(player));
		player.sendPacket(new HuntPassSimpleInfo(player));
		
		ThreadPool.schedule(() -> player.removeRequest(RewardRequest.class), 300);
	}
	
	private void rewardItem(Player player, ItemHolder reward)
	{
		if (reward.getId() == 72286) // Sayha's Grace Sustention Points
		{
			final int count = (int) reward.getCount();
			player.getHuntPass().addSayhaTime(count);
			
			final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_VE_GOT_S1_SAYHA_S_GRACE_SUSTENTION_POINT_S);
			msg.addInt(count);
			player.sendPacket(msg);
		}
		else
		{
			player.addItem(ItemProcessType.REWARD, reward, player, true);
		}
	}
	
	private void premiumReward(Player player)
	{
		final HuntPass huntPass = player.getHuntPass();
		final int premiumRewardIndex = huntPass.getPremiumRewardStep();
		if (premiumRewardIndex >= HuntPassData.getInstance().getPremiumRewardsCount())
		{
			return;
		}
		
		if (!huntPass.isPremium())
		{
			return;
		}
		
		rewardItem(player, HuntPassData.getInstance().getPremiumRewards().get(premiumRewardIndex));
		huntPass.setPremiumRewardStep(premiumRewardIndex + 1);
	}
	
	private void normalReward(Player player)
	{
		final HuntPass huntPass = player.getHuntPass();
		final int rewardIndex = huntPass.getRewardStep();
		if (rewardIndex >= HuntPassData.getInstance().getRewardsCount())
		{
			return;
		}
		
		if (huntPass.isPremium() && ((huntPass.getPremiumRewardStep() < rewardIndex) || (huntPass.getPremiumRewardStep() >= HuntPassData.getInstance().getPremiumRewardsCount())))
		{
			return;
		}
		
		rewardItem(player, HuntPassData.getInstance().getRewards().get(rewardIndex));
	}
}
