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
package org.l2jmobius.gameserver.network.serverpackets.castlewar;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class MercenaryCastleWarCastleSiegeHudInfo extends ServerPacket
{
	private final Castle _castle;
	
	public MercenaryCastleWarCastleSiegeHudInfo(int castleId)
	{
		_castle = CastleManager.getInstance().getCastleById(castleId);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		if (_castle == null)
		{
			return;
		}
		
		ServerPackets.EX_MERCENARY_CASTLEWAR_CASTLE_SIEGE_HUD_INFO.writeId(this, buffer);
		buffer.writeInt(_castle.getResidenceId());
		if (_castle.getSiege().isInProgress())
		{
			buffer.writeInt(1);
			buffer.writeInt(0);
			buffer.writeInt((int) (((_castle.getSiegeDate().getTimeInMillis() + (SiegeManager.getInstance().getSiegeLength() * 60000)) - System.currentTimeMillis()) / 1000));
		}
		else
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt((int) ((_castle.getSiegeDate().getTimeInMillis() - System.currentTimeMillis()) / 1000));
		}
	}
}
