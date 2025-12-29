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
import org.l2jmobius.gameserver.model.siege.SiegeClanType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class MercenaryCastleWarCastleSiegeDefenderList extends ServerPacket
{
	private final int _castleId;
	private final Castle _castle;
	private final Clan _owner;
	private final List<Clan> _defenders = new ArrayList<>();
	private final List<Clan> _defendersWaiting = new ArrayList<>();
	
	public MercenaryCastleWarCastleSiegeDefenderList(int castleId)
	{
		_castleId = castleId;
		_castle = CastleManager.getInstance().getCastleById(castleId);
		
		// Owner.
		_owner = _castle.getOwner();
		
		// Defenders.
		for (SiegeClan siegeClan : _castle.getSiege().getDefenderClans())
		{
			final Clan clan = ClanTable.getInstance().getClan(siegeClan.getClanId());
			if ((clan != null) && (clan != _castle.getOwner()))
			{
				_defenders.add(clan);
			}
		}
		
		// Defenders waiting.
		for (SiegeClan siegeClan : _castle.getSiege().getDefenderWaitingClans())
		{
			final Clan clan = ClanTable.getInstance().getClan(siegeClan.getClanId());
			if (clan != null)
			{
				_defendersWaiting.add(clan);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_MERCENARY_CASTLEWAR_CASTLE_SIEGE_DEFENDER_LIST.writeId(this, buffer);
		
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
			final int size = (_owner != null ? 1 : 0) + _defenders.size() + _defendersWaiting.size();
			buffer.writeInt(size);
			buffer.writeInt(size);
			
			// Owners.
			if (_owner != null)
			{
				buffer.writeInt(_owner.getId());
				buffer.writeString(_owner.getName());
				buffer.writeString(_owner.getLeaderName());
				buffer.writeInt(_owner.getCrestId());
				buffer.writeInt(0); // time (seconds)
				buffer.writeInt(SiegeClanType.OWNER.ordinal());
				
				buffer.writeInt(_owner.isRecruitMercenary());
				buffer.writeLong(_owner.getRewardMercenary());
				buffer.writeInt(_owner.getMapMercenary().size());
				buffer.writeLong(0);
				buffer.writeLong(0);
				if (_owner.getAllyId() != 0)
				{
					buffer.writeInt(_owner.getAllyId());
					buffer.writeString(_owner.getAllyName());
					buffer.writeString("");
					buffer.writeInt(_owner.getAllyCrestId());
				}
				else
				{
					buffer.writeInt(0);
					buffer.writeString("");
					buffer.writeString("");
					buffer.writeInt(0);
				}
			}
			
			// Defenders.
			for (Clan defender : _defenders)
			{
				buffer.writeInt(defender.getId());
				buffer.writeString(defender.getName());
				buffer.writeString(defender.getLeaderName());
				buffer.writeInt(defender.getCrestId());
				buffer.writeInt(0); // time (seconds)
				buffer.writeInt(SiegeClanType.DEFENDER.ordinal());
				
				buffer.writeInt(defender.isRecruitMercenary());
				buffer.writeLong(defender.getRewardMercenary());
				buffer.writeInt(defender.getMapMercenary().size());
				buffer.writeLong(0);
				buffer.writeLong(0);
				if (defender.getAllyId() != 0)
				{
					buffer.writeInt(defender.getAllyId());
					buffer.writeString(defender.getAllyName());
					buffer.writeString("");
					buffer.writeInt(defender.getAllyCrestId());
				}
				else
				{
					buffer.writeInt(0);
					buffer.writeString("");
					buffer.writeString("");
					buffer.writeInt(0);
				}
			}
			
			// Defenders waiting.
			for (Clan defender : _defendersWaiting)
			{
				buffer.writeInt(defender.getId());
				buffer.writeString(defender.getName());
				buffer.writeString(defender.getLeaderName());
				buffer.writeInt(defender.getCrestId());
				buffer.writeInt(0); // time (seconds)
				buffer.writeInt(SiegeClanType.DEFENDER_PENDING.ordinal());
				
				buffer.writeInt(defender.isRecruitMercenary());
				buffer.writeLong(defender.getRewardMercenary());
				buffer.writeInt(defender.getMapMercenary().size());
				buffer.writeLong(0);
				buffer.writeLong(0);
				if (defender.getAllyId() != 0)
				{
					buffer.writeInt(defender.getAllyId());
					buffer.writeString(defender.getAllyName());
					buffer.writeString("");
					buffer.writeInt(defender.getAllyCrestId());
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
