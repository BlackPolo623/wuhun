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

import java.util.Calendar;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.TaxType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Serenitty, Mobius
 */
public class MercenaryCastleWarCastleInfo extends ServerPacket
{
	private final Castle _castle;
	
	public MercenaryCastleWarCastleInfo(int castleId)
	{
		_castle = CastleManager.getInstance().getCastleById(castleId);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_MERCENARY_CASTLEWAR_CASTLE_INFO.writeId(this, buffer);
		
		if (_castle == null)
		{
			buffer.writeInt(_castle.getResidenceId());
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeSizedString("");
			buffer.writeSizedString("");
			buffer.writeInt(0);
			buffer.writeLong(0);
			buffer.writeLong(0);
			buffer.writeInt(0);
			return;
		}
		
		final Clan clan = _castle.getOwner();
		buffer.writeInt(_castle.getResidenceId());
		buffer.writeInt(clan != null ? clan.getCrestId() : 0); // CastleOwnerPledgeSID
		buffer.writeInt(clan != null ? clan.getCrestLargeId() : 0); // CastleOwnerPledgeCrestDBID
		buffer.writeSizedString(clan != null ? clan.getName() : "-"); // CastleOwnerPledgeName
		buffer.writeSizedString(clan != null ? clan.getLeaderName() : "-"); // CastleOwnerPledgeMasterName
		buffer.writeInt(_castle.getTaxPercent(TaxType.BUY)); // CastleTaxRate
		buffer.writeLong(_castle.getTempTreasury());
		
		final long treasury = _castle.getTreasury();
		buffer.writeLong((long) (treasury + (treasury * _castle.getTaxRate(TaxType.BUY)))); // TotalIncome
		
		final Calendar siegeDate = _castle.getSiegeDate();
		buffer.writeInt(siegeDate != null ? (int) (siegeDate.getTimeInMillis() / 1000) : 0); // NextSiegeTime
	}
}
