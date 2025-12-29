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
package org.l2jmobius.gameserver.network.serverpackets.dailymission;

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.MissionLevel;
import org.l2jmobius.gameserver.model.MissionLevelHolder;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.MissionLevelPlayerDataHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Index
 */
public class ExMissionLevelRewardList extends ServerPacket
{
	private final int _year;
	private final int _month;
	private final int _maxNormalLevel;
	private final MissionLevelHolder _holder;
	private final MissionLevelPlayerDataHolder _info;
	private final List<Integer> _collectedNormalRewards;
	private final List<Integer> _collectedKeyRewards;
	private final List<Integer> _collectedBonusRewards;
	
	public ExMissionLevelRewardList(Player player)
	{
		final MissionLevel missionData = MissionLevel.getInstance();
		final int currentSeason = missionData.getCurrentSeason();
		final String currentSeasonString = String.valueOf(currentSeason);
		_year = Integer.parseInt(currentSeasonString.substring(0, 4));
		_month = Integer.parseInt(currentSeasonString.substring(4, 6));
		_holder = missionData.getMissionBySeason(currentSeason);
		_maxNormalLevel = _holder.getBonusLevel(); // After normal rewards there will be bonus.
		_info = player.getMissionLevelProgress();
		_collectedNormalRewards = _info.getCollectedNormalRewards();
		_collectedKeyRewards = _info.getCollectedKeyRewards();
		_collectedBonusRewards = _info.getListOfCollectedBonusRewards();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_MISSION_LEVEL_REWARD_LIST.writeId(this, buffer);
		if (_info.getCurrentLevel() == 0)
		{
			buffer.writeInt(1); // 0 -> does not work, -1 -> game crushed
			buffer.writeInt(3); // Type
			buffer.writeInt(-1); // Level
			buffer.writeInt(0); // State
		}
		else
		{
			sendAvailableRewardsList(buffer, _info);
		}
		
		buffer.writeInt(_info.getCurrentLevel()); // Level
		buffer.writeInt(getPercent(_info)); // PointPercent
		buffer.writeInt(_year); // SeasonYear
		buffer.writeInt(_month); // SeasonMonth
		buffer.writeInt(getAvailableRewards(_info)); // TotalRewardsAvailable
		if (_holder.getBonusRewardIsAvailable() && _holder.getBonusRewardByLevelUp())
		{
			boolean check = false;
			for (int level = _maxNormalLevel; level <= _holder.getMaxLevel(); level++)
			{
				if ((level <= _info.getCurrentLevel()) && !_collectedBonusRewards.contains(level))
				{
					check = true;
					break;
				}
			}
			
			buffer.writeInt(check); // ExtraRewardsAvailable
		}
		else
		{
			if (_holder.getBonusRewardIsAvailable() && _info.getCollectedSpecialReward() && !_info.getCollectedBonusReward())
			{
				buffer.writeInt(1); // ExtraRewardsAvailable
			}
			else
			{
				buffer.writeInt(0); // ExtraRewardsAvailable
			}
		}
		
		buffer.writeInt(0); // RemainSeasonTime / does not work? / not used?
		buffer.writeByte(0); // 464 - AccountLevelJumped
	}
	
	private int getAvailableRewards(MissionLevelPlayerDataHolder info)
	{
		int availableRewards = 0;
		for (int level : _holder.getNormalRewards().keySet())
		{
			if ((level <= info.getCurrentLevel()) && !_collectedNormalRewards.contains(level))
			{
				availableRewards++;
			}
		}
		
		for (int level : _holder.getKeyRewards().keySet())
		{
			if ((level <= info.getCurrentLevel()) && !_collectedKeyRewards.contains(level))
			{
				availableRewards++;
			}
		}
		
		if (_holder.getBonusRewardIsAvailable() && _holder.getBonusRewardByLevelUp() && info.getCollectedSpecialReward())
		{
			final List<Integer> collectedBonusRewards = info.getListOfCollectedBonusRewards();
			for (int level = _maxNormalLevel; level <= _holder.getMaxLevel(); level++)
			{
				if ((level <= info.getCurrentLevel()) && !collectedBonusRewards.contains(level))
				{
					availableRewards++;
					break;
				}
			}
		}
		else if (_holder.getBonusRewardIsAvailable() && _holder.getBonusRewardByLevelUp() && (info.getCurrentLevel() >= _maxNormalLevel))
		{
			availableRewards++;
		}
		else if (_holder.getBonusRewardIsAvailable() && (info.getCurrentLevel() >= _holder.getMaxLevel()) && !info.getCollectedBonusReward() && info.getCollectedSpecialReward())
		{
			availableRewards++;
		}
		else if ((info.getCurrentLevel() >= _holder.getMaxLevel()) && !info.getCollectedBonusReward())
		{
			availableRewards++;
		}
		
		return availableRewards;
	}
	
	private int getTotalRewards(MissionLevelPlayerDataHolder info)
	{
		int totalRewards = 0;
		for (int level : _holder.getNormalRewards().keySet())
		{
			if (level <= info.getCurrentLevel())
			{
				totalRewards++;
			}
		}
		
		for (int level : _holder.getKeyRewards().keySet())
		{
			if (level <= info.getCurrentLevel())
			{
				totalRewards++;
			}
		}
		
		if (_holder.getBonusRewardByLevelUp() && info.getCollectedSpecialReward() && _holder.getBonusRewardIsAvailable() && (_maxNormalLevel <= info.getCurrentLevel()))
		{
			for (int level = _maxNormalLevel; level <= _holder.getMaxLevel(); level++)
			{
				if (level <= info.getCurrentLevel())
				{
					totalRewards++;
					break;
				}
			}
		}
		else if (info.getCollectedSpecialReward() && _holder.getBonusRewardIsAvailable() && (_maxNormalLevel <= info.getCurrentLevel()))
		{
			totalRewards++;
		}
		else if (_maxNormalLevel <= info.getCurrentLevel())
		{
			totalRewards++;
		}
		
		return totalRewards;
	}
	
	private int getPercent(MissionLevelPlayerDataHolder info)
	{
		if (info.getCurrentLevel() >= _holder.getMaxLevel())
		{
			return 100;
		}
		
		return (int) Math.floor(((double) info.getCurrentEXP() / (double) _holder.getXPForSpecifiedLevel(info.getCurrentLevel())) * 100.0);
	}
	
	private void sendAvailableRewardsList(WritableBuffer buffer, MissionLevelPlayerDataHolder info)
	{
		buffer.writeInt(getTotalRewards(info)); // PkMissionLevelReward
		for (int level : _holder.getNormalRewards().keySet())
		{
			if (level <= info.getCurrentLevel())
			{
				buffer.writeInt(1); // Type
				buffer.writeInt(level); // Level
				buffer.writeInt(_collectedNormalRewards.contains(level) ? 2 : 1); // State
			}
		}
		
		for (int level : _holder.getKeyRewards().keySet())
		{
			if (level <= info.getCurrentLevel())
			{
				buffer.writeInt(2); // Type
				buffer.writeInt(level); // Level
				buffer.writeInt(_collectedKeyRewards.contains(level) ? 2 : 1); // State
			}
		}
		
		if (_holder.getBonusRewardByLevelUp() && info.getCollectedSpecialReward() && _holder.getBonusRewardIsAvailable() && (_maxNormalLevel <= info.getCurrentLevel()))
		{
			buffer.writeInt(3); // Type
			int sendLevel = 0;
			for (int level = _maxNormalLevel; level <= _holder.getMaxLevel(); level++)
			{
				if ((level <= info.getCurrentLevel()) && !_collectedBonusRewards.contains(level))
				{
					sendLevel = level;
					break;
				}
			}
			
			buffer.writeInt(sendLevel == 0 ? _holder.getMaxLevel() : sendLevel); // Level
			buffer.writeInt(2); // State
		}
		else if (info.getCollectedSpecialReward() && _holder.getBonusRewardIsAvailable() && (_maxNormalLevel <= info.getCurrentLevel()))
		{
			buffer.writeInt(3); // Type
			buffer.writeInt(_holder.getMaxLevel()); // Level
			buffer.writeInt(2); // State
		}
		else if (_maxNormalLevel <= info.getCurrentLevel())
		{
			buffer.writeInt(3); // Type
			buffer.writeInt(_holder.getMaxLevel()); // Level
			buffer.writeInt(!info.getCollectedSpecialReward()); // State
		}
	}
}
