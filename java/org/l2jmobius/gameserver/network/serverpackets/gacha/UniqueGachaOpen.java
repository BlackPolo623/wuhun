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
package org.l2jmobius.gameserver.network.serverpackets.gacha;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.events.UniqueGachaManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.GachaItemHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class UniqueGachaOpen extends ServerPacket
{
	private static final int SHORT_PACKET_INFO = 2 + 1 + 4 + 8;
	
	private final int _fullInfo;
	private final int _openMode;
	private final long _currencyCount;
	private final int _stepsToGuaranteedReward;
	private final int _remainingTime;
	private final boolean _isShowProbability;
	private final Set<GachaItemHolder> _visibleItems;
	private final int _totalRewardCount;
	private final Collection<Set<GachaItemHolder>> _rewardItems;
	private final int _currencyItemId;
	private final Map<Integer, Long> _gameCosts;
	
	public UniqueGachaOpen(Player player, int fullInfo, int openMode)
	{
		_fullInfo = fullInfo;
		_openMode = openMode;
		
		final UniqueGachaManager manager = UniqueGachaManager.getInstance();
		_currencyCount = manager.getCurrencyCount(player);
		_stepsToGuaranteedReward = manager.getStepsToGuaranteedReward(player);
		_remainingTime = (int) ((manager.getActiveUntilPeriod() - System.currentTimeMillis()) / 1000L);
		_isShowProbability = manager.isShowProbability();
		if (_fullInfo == 1)
		{
			_visibleItems = manager.getVisibleItems();
			_totalRewardCount = manager.getTotalRewardCount();
			_rewardItems = manager.getRewardItems().values();
			_currencyItemId = manager.getCurrencyItemId();
			_gameCosts = manager.getGameCosts();
		}
		else
		{
			_visibleItems = Collections.emptySet();
			_totalRewardCount = 0;
			_rewardItems = Collections.emptySet();
			_currencyItemId = 0;
			_gameCosts = Collections.emptyMap();
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_UNIQUE_GACHA_OPEN.writeId(this, buffer);
		buffer.writeByte(_openMode); // open mode 1 = const GACHA_TITLE_FORM = 1; = will open main window?
		buffer.writeByte(0); // result // char // not used?
		buffer.writeLong(_currencyCount); // my cost item amount // long (493) // current item count for roll?
		buffer.writeInt(_stepsToGuaranteedReward); // my confirm count // int // left to guarant
		buffer.writeInt(_remainingTime); // remain time in seconds // int
		buffer.writeByte(_fullInfo); // full info // char
		buffer.writeByte(_isShowProbability); // show probability // char
		if (_fullInfo == 1)
		{
			writeFullInfo(buffer);
		}
		else
		{
			writeDummyInfo(buffer);
		}
	}
	
	private void writeFullInfo(WritableBuffer buffer)
	{
		buffer.writeInt(_visibleItems.size()); // show items size // int
		for (GachaItemHolder item : _visibleItems)
		{
			buffer.writeShort(SHORT_PACKET_INFO); // current size // short
			buffer.writeByte(item.getRank().getClientId()); // rank type // char
			buffer.writeInt(item.getId()); // item type // int
			buffer.writeLong(item.getCount()); // amount // long
			buffer.writeDouble(getChance(item.getItemChance())); // probability // double
		}
		
		buffer.writeInt(_totalRewardCount); // reward item size // int
		for (Set<GachaItemHolder> items : _rewardItems)
		{
			for (GachaItemHolder item : items)
			{
				buffer.writeShort(SHORT_PACKET_INFO); // current size // short
				buffer.writeByte(item.getRank().getClientId()); // rank type // char
				buffer.writeInt(item.getId()); // item type // int
				buffer.writeLong(item.getCount()); // amount // long
				buffer.writeDouble(getChance(item.getItemChance())); // probability // double
			}
		}
		
		buffer.writeByte(1); // cost type // char // bool?
		buffer.writeInt(_currencyItemId); // cost item type // int // item id
		buffer.writeInt(_gameCosts.size()); // cost amount item info // int
		for (Entry<Integer, Long> entry : _gameCosts.entrySet())
		{
			buffer.writeInt(entry.getKey()); // game count // int
			buffer.writeLong(entry.getValue()); // amount // long
		}
	}
	
	private void writeDummyInfo(WritableBuffer buffer)
	{
		buffer.writeInt(0); // show items size // int
		buffer.writeInt(0); // reward item size // int
		buffer.writeByte(0); // cost type // char // bool?
		buffer.writeInt(0); // cost item type // int // item id
		buffer.writeInt(0); // cost amount item info // int
	}
	
	private double getChance(int itemChance)
	{
		return _isShowProbability ? (double) ((double) itemChance / (double) UniqueGachaManager.MINIMUM_CHANCE) : 0;
	}
}
