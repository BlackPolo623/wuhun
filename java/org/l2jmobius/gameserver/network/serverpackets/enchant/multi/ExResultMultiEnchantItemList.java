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
package org.l2jmobius.gameserver.network.serverpackets.enchant.multi;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.EnchantItemRequest;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Index
 */
public class ExResultMultiEnchantItemList extends ServerPacket
{
	public static final int SUCCESS = 0;
	public static final int FAIL = 1;
	public static final int ERROR = 2;
	
	private final Player _player;
	private boolean _error;
	private boolean _isResult;
	private Map<Integer, int[]> _successEnchant = new HashMap<>();
	private Map<Integer, Integer> _failureEnchant = new HashMap<>();
	private Map<Integer, ItemHolder> _failureReward = new HashMap<>();
	private final Map<Integer, Integer> _failChallengePointInfoList;
	
	public ExResultMultiEnchantItemList(Player player, boolean error)
	{
		_player = player;
		_error = error;
		_failChallengePointInfoList = new HashMap<>();
	}
	
	public ExResultMultiEnchantItemList(Player player, Map<Integer, ItemHolder> failureReward)
	{
		_player = player;
		_failureReward = failureReward;
		_failChallengePointInfoList = new HashMap<>();
	}
	
	public ExResultMultiEnchantItemList(Player player, Map<Integer, int[]> successEnchant, Map<Integer, Integer> failureEnchant)
	{
		_player = player;
		_successEnchant = successEnchant;
		_failureEnchant = failureEnchant;
		_failChallengePointInfoList = new HashMap<>();
	}
	
	public ExResultMultiEnchantItemList(Player player, Map<Integer, int[]> successEnchant, Map<Integer, Integer> failureEnchant, boolean isResult)
	{
		_player = player;
		_successEnchant = successEnchant;
		_failureEnchant = failureEnchant;
		_isResult = isResult;
		_failChallengePointInfoList = new HashMap<>();
	}
	
	public ExResultMultiEnchantItemList(Player player, Map<Integer, int[]> successEnchant, Map<Integer, Integer> failureEnchant, Map<Integer, Integer> failChallengePointInfoList, boolean isResult)
	{
		_player = player;
		_successEnchant = successEnchant;
		_failureEnchant = failureEnchant;
		_isResult = isResult;
		_failChallengePointInfoList = failChallengePointInfoList;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		final EnchantItemRequest request = _player.getRequest(EnchantItemRequest.class);
		if (request == null)
		{
			return;
		}
		
		ServerPackets.EX_RES_MULTI_ENCHANT_ITEM_LIST.writeId(this, buffer);
		
		if (_error)
		{
			buffer.writeByte(0);
			return;
		}
		
		buffer.writeByte(1);
		
		/* EnchantSuccessItem */
		if (_failureReward.isEmpty())
		{
			buffer.writeInt(_successEnchant.size());
			if (!_successEnchant.isEmpty())
			{
				for (int[] success : _successEnchant.values())
				{
					buffer.writeInt(success[0]);
					buffer.writeInt(success[1]);
				}
			}
		}
		else
		{
			buffer.writeInt(0);
		}
		
		/* EnchantFailItem */
		buffer.writeInt(_failureEnchant.size());
		if (!_failureEnchant.isEmpty())
		{
			for (int failure : _failureEnchant.values())
			{
				buffer.writeInt(failure);
				buffer.writeInt(0);
			}
		}
		else
		{
			buffer.writeInt(0);
		}
		
		/* EnchantFailRewardItem */
		if ((_successEnchant.isEmpty() && (request.getMultiFailItemsCount() != 0)) || (_isResult && (request.getMultiFailItemsCount() != 0)))
		{
			buffer.writeInt(request.getMultiFailItemsCount());
			_failureReward = request.getMultiEnchantFailItems();
			for (ItemHolder failure : _failureReward.values())
			{
				buffer.writeInt(failure.getId());
				buffer.writeInt((int) failure.getCount());
			}
			
			if (_isResult)
			{
				request.clearMultiSuccessEnchantList();
				request.clearMultiFailureEnchantList();
			}
			
			request.clearMultiFailReward();
		}
		else
		{
			buffer.writeInt(0);
		}
		
		/* EnchantFailChallengePointInfo */
		buffer.writeInt(_failChallengePointInfoList.size());
		for (Entry<Integer, Integer> item : _failChallengePointInfoList.entrySet())
		{
			buffer.writeInt(item.getKey());
			buffer.writeInt(item.getValue());
		}
	}
}
