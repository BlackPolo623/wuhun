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

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.events.UniqueGachaManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.GachaItemHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class UniqueGachaGame extends ServerPacket
{
	public static final int FAILURE = 0;
	public static final int SUCCESS = 1;
	
	private final int _success;
	private final Player _player;
	private final List<GachaItemHolder> _rewards;
	private final boolean _rare;
	private final long _currencyCount;
	private final int _guaranteedReward;
	
	public UniqueGachaGame(int success, Player player, List<GachaItemHolder> rewards, boolean rare)
	{
		_success = success;
		_player = player;
		_rewards = rewards;
		_rare = rare;
		
		final UniqueGachaManager manager = UniqueGachaManager.getInstance();
		_currencyCount = manager.getCurrencyCount(_player);
		_guaranteedReward = manager.getStepsToGuaranteedReward(_player);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_UNIQUE_GACHA_GAME.writeId(this, buffer);
		buffer.writeByte(_success); // result // char
		buffer.writeLong(_currencyCount); // amount // long (493)
		buffer.writeInt(_guaranteedReward); // guaranty // int
		
		// 0 - yellow
		// 1 - purple
		buffer.writeByte(_rare ? 1 : 0); // rank // char
		buffer.writeInt(_rewards.size()); // size // int
		for (GachaItemHolder item : _rewards)
		{
			buffer.writeByte(item.getRank().getClientId()); // rank // char
			buffer.writeInt(item.getId()); // itemId // int
			buffer.writeLong(item.getCount()); // count // long
		}
	}
}
