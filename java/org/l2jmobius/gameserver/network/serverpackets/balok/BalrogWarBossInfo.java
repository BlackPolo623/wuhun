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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.BattleWithBalokManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Serenitty, NasSeKa
 */
public class BalrogWarBossInfo extends ServerPacket
{
	private final int _bossState1;
	private final int _bossState2;
	private final int _bossState3;
	private final int _bossState4;
	private final int _bossState5;
	private final int _finalBossId;
	private final int _finalState;
	private final long _globalpoints;
	private final int _globalstage;
	
	public BalrogWarBossInfo(int balokid, int balokstatus, int boss1, int boss2, int boss3, int boss4, int boss5)
	{
		_finalBossId = balokid + 1000000;
		_finalState = balokstatus;
		_bossState1 = boss1;
		_bossState2 = boss2;
		_bossState3 = boss3;
		_bossState4 = boss4;
		_bossState5 = boss5;
		_globalpoints = BattleWithBalokManager.getInstance().getGlobalPoints();
		_globalstage = BattleWithBalokManager.getInstance().getGlobalStage();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_BALROGWAR_BOSSINFO.writeId(this, buffer);
		if ((_globalpoints < 320000) && (_globalstage <= 2))
		{
			buffer.writeInt(1);
			buffer.writeInt(1);
			buffer.writeInt(1);
			buffer.writeInt(1);
			buffer.writeInt(1);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
		else
		{
			final int bossId1 = 25956 + 1000000;
			final int bossId2 = 25957 + 1000000;
			final int bossId3 = 25958 + 1000000;
			int bossId4 = 0;
			int bossId5 = 0;
			if ((_globalpoints >= 800000) && (_globalstage >= 3))
			{
				bossId4 = 25959 + 1000000;
				bossId5 = 25960 + 1000000;
			}
			
			buffer.writeInt(bossId1);
			buffer.writeInt(bossId2);
			buffer.writeInt(bossId3);
			buffer.writeInt(bossId4);
			buffer.writeInt(bossId5);
			buffer.writeInt(_bossState1);
			buffer.writeInt(_bossState2);
			buffer.writeInt(_bossState3);
			buffer.writeInt(_bossState4);
			buffer.writeInt(_bossState5);
			buffer.writeInt(_finalBossId);
			buffer.writeInt(_finalState);
		}
	}
}
