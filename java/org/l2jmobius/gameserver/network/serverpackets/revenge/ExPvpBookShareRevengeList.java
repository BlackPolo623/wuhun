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
package org.l2jmobius.gameserver.network.serverpackets.revenge;

import java.util.Collection;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.RevengeHistoryManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.RevengeHistoryHolder;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExPvpBookShareRevengeList extends ServerPacket
{
	private final Collection<RevengeHistoryHolder> _history;
	
	public ExPvpBookShareRevengeList(Player player)
	{
		_history = RevengeHistoryManager.getInstance().getHistory(player);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PVPBOOK_SHARE_REVENGE_LIST.writeId(this, buffer);
		if (_history == null)
		{
			buffer.writeByte(1); // CurrentPage
			buffer.writeByte(1); // MaxPage
			buffer.writeInt(0);
		}
		else
		{
			buffer.writeByte(1); // CurrentPage
			buffer.writeByte(1); // MaxPage
			buffer.writeInt(_history.size());
			for (RevengeHistoryHolder holder : _history)
			{
				buffer.writeInt(holder.getType().ordinal()); // ShareType (2 - help request, 1 - revenge, 0 - both)
				buffer.writeInt((int) (holder.getKillTime() / 1000)); // KilledTime
				buffer.writeInt(holder.getShowLocationRemaining()); // ShowKillerCount
				buffer.writeInt(holder.getTeleportRemaining()); // TeleportKillerCount
				buffer.writeInt(holder.getSharedTeleportRemaining()); // SharedTeleportKillerCount
				buffer.writeInt(0); // KilledUserDBID
				buffer.writeSizedString(holder.getVictimName()); // KilledUserName
				buffer.writeSizedString(holder.getVictimClanName()); // KilledUserPledgeName
				buffer.writeInt(holder.getVictimLevel()); // KilledUserLevel
				buffer.writeInt(holder.getVictimRaceId()); // KilledUserRace
				buffer.writeInt(holder.getVictimClassId()); // KilledUserClass
				buffer.writeInt(0); // KillUserDBID
				buffer.writeSizedString(holder.getKillerName()); // KillUserName
				buffer.writeSizedString(holder.getKillerClanName()); // KillUserPledgeName
				buffer.writeInt(holder.getKillerLevel()); // KillUserLevel
				buffer.writeInt(holder.getKillerRaceId()); // KillUserRace
				buffer.writeInt(holder.getKillerClassId()); // KillUserClass
				Player killer = World.getInstance().getPlayer(holder.getKillerName());
				buffer.writeInt((killer != null) && killer.isOnline() ? 2 : 0); // KillUserOnline (2 - online, 0 - offline)
				buffer.writeInt(0); // KillUserKarma
				buffer.writeInt((int) (holder.getShareTime() / 1000)); // nSharedTime
			}
		}
	}
}
