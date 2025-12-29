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
package org.l2jmobius.gameserver.network.serverpackets.balok;

import java.util.concurrent.TimeUnit;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius, Serenitty
 */
public class BalrogWarHud extends ServerPacket
{
	private final int _state;
	private final int _stage;
	private final int _time;
	
	public BalrogWarHud(int state, int stage)
	{
		_state = state;
		_stage = stage;
		
		final long remainTime = GlobalVariablesManager.getInstance().getLong(GlobalVariablesManager.BALOK_REMAIN_TIME, 0);
		final long currentTime = System.currentTimeMillis();
		_time = (int) TimeUnit.MILLISECONDS.toSeconds(remainTime - currentTime);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_BALROGWAR_HUD.writeId(this, buffer);
		buffer.writeInt(_state); // State
		buffer.writeInt(_stage); // Progress Step
		buffer.writeInt(_time); // Time (in seconds)
	}
}
