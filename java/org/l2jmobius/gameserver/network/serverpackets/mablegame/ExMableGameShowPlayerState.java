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
package org.l2jmobius.gameserver.network.serverpackets.mablegame;

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.MableGameData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class ExMableGameShowPlayerState extends ServerPacket
{
	private final int _commonDiceLimit;
	private final int _dailyAvailableRounds;
	private final int _highestCellId;
	private final ItemHolder _finishReward;
	private final List<ItemHolder> _resetItems;
	private final MableGameData.MableGamePlayerState _playerState;
	
	public ExMableGameShowPlayerState(Player player)
	{
		final MableGameData data = MableGameData.getInstance();
		_commonDiceLimit = data.getCommonDiceLimit();
		_dailyAvailableRounds = data.getDailyAvailableRounds();
		_highestCellId = data.getHighestCellId();
		_finishReward = data.getRoundReward();
		_resetItems = data.getResetItems();
		_playerState = data.getPlayerState(player.getAccountName());
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_MABLE_GAME_SHOW_PLAYER_STATE.writeId(this, buffer);
		buffer.writeInt(_playerState.getRound());
		buffer.writeInt(_playerState.getCurrentCellId());
		buffer.writeInt(_playerState.getRemainCommonDice());
		buffer.writeInt(_commonDiceLimit);
		buffer.writeByte(_playerState.getCurrentCellId() == _highestCellId ? (_dailyAvailableRounds == _playerState.getRound() ? 6 : 5) : 0); // (cCurrentState // 0-3 unk / 4 = just finished / buttoncCurrentState / 5 = reset / 6 = all rounds completed)
		buffer.writeInt(_dailyAvailableRounds); // FinishRewards
		for (int i = 1; i <= _dailyAvailableRounds; i++)
		{
			buffer.writeInt(i); // PlayCount
			buffer.writeInt(_finishReward.getId());
			buffer.writeLong(_finishReward.getCount());
		}
		
		buffer.writeInt(_resetItems.size()); // ResetItems
		for (ItemHolder item : _resetItems)
		{
			buffer.writeInt(item.getId());
			buffer.writeLong(item.getCount());
		}
	}
}
