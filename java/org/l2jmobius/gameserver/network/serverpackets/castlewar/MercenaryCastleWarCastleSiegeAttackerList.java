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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.SiegeClan;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class MercenaryCastleWarCastleSiegeAttackerList extends ServerPacket
{
	private final int _castleId;
	private final Castle _castle;
	private final List<Clan> _attackers = new ArrayList<>();
	
	public MercenaryCastleWarCastleSiegeAttackerList(int castleId)
	{
		_castleId = castleId;
		_castle = CastleManager.getInstance().getCastleById(castleId);
		for (SiegeClan siegeclan : _castle.getSiege().getAttackerClans())
		{
			final Clan clan = ClanTable.getInstance().getClan(siegeclan.getClanId());
			if (clan != null)
			{
				_attackers.add(clan);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_MERCENARY_CASTLEWAR_CASTLE_SIEGE_ATTACKER_LIST.writeId(this, buffer);
		
		buffer.writeInt(_castleId);
		buffer.writeInt(0);
		buffer.writeInt(1);
		buffer.writeInt(0);
		
		if (_castle == null)
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
		else
		{
			buffer.writeInt(_attackers.size());
			buffer.writeInt(_attackers.size());
			for (Clan clan : _attackers)
			{
				buffer.writeInt(clan.getId());
				buffer.writeString(clan.getName());
				buffer.writeString(clan.getLeaderName());
				buffer.writeInt(clan.getCrestId());
				buffer.writeInt(0); // time
				
				buffer.writeInt(clan.isRecruitMercenary());
				buffer.writeLong(clan.getRewardMercenary());
				buffer.writeInt(clan.getMapMercenary().size());
				buffer.writeLong(0);
				buffer.writeLong(0);
				if (clan.getAllyId() != 0)
				{
					buffer.writeInt(clan.getAllyId());
					buffer.writeString(clan.getAllyName());
					buffer.writeString("");
					buffer.writeInt(clan.getAllyCrestId());
				}
				else
				{
					buffer.writeInt(0);
					buffer.writeString("");
					buffer.writeString("");
					buffer.writeInt(0);
				}
			}
		}
	}
}
