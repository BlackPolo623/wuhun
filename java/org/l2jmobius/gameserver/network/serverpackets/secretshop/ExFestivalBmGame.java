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
package org.l2jmobius.gameserver.network.serverpackets.secretshop;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Serenitty
 */
public class ExFestivalBmGame extends ServerPacket
{
	private final int _ticketId;
	private final int _amount;
	private final int _ticketPerGame;
	private final int _rewardGrade;
	private final int _rewardId;
	private final int _rewardCount;
	private final int _result;
	
	public ExFestivalBmGame(int ticketId, int amount, int ticketPerGame, int rewardGrade, int rewardId, int rewardCount, int result)
	{
		_ticketId = ticketId;
		_amount = amount;
		_ticketPerGame = ticketPerGame;
		_rewardGrade = rewardGrade;
		_rewardId = rewardId;
		_rewardCount = rewardCount;
		_result = result;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_FESTIVAL_BM_GAME.writeId(this, buffer);
		buffer.writeByte(_result);
		buffer.writeInt(_ticketId);
		buffer.writeLong(_amount);
		buffer.writeInt(_ticketPerGame);
		buffer.writeByte(_rewardGrade);
		buffer.writeInt(_rewardId);
		buffer.writeInt(_rewardCount);
	}
}
