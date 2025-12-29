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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * @author Serenitty
 */
public class ExRequestNewInvitePartyInquiry extends ServerPacket
{
	private final int _reqType;
	private final ChatType _sayType;
	private final int _charRankGrade;
	private final int _pledgeCastleDBID;
	private final int _userID;
	private final Player _player;
	
	public ExRequestNewInvitePartyInquiry(Player player, int reqType, ChatType sayType)
	{
		_player = player;
		_userID = _player.getObjectId();
		_reqType = reqType;
		_sayType = sayType;
		
		final int rank = RankManager.getInstance().getPlayerGlobalRank(player);
		_charRankGrade = (rank == 1) ? 1 : (rank <= 30) ? 2 : (rank <= 100) ? 3 : 0;
		
		int castle = 0;
		final Clan clan = _player.getClan();
		if (clan != null)
		{
			castle = clan.getCastleId();
		}
		
		_pledgeCastleDBID = castle;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_REQUEST_INVITE_PARTY.writeId(this, buffer);
		buffer.writeSizedString(_player.getName());
		buffer.writeByte(_reqType);
		buffer.writeByte(_sayType.ordinal());
		buffer.writeByte(_charRankGrade);
		buffer.writeByte(_pledgeCastleDBID);
		buffer.writeByte(_player.isInTimedHuntingZone() || _player.isInSiege() || _player.isRegisteredOnEvent());
		buffer.writeInt(0); // Chat background.
		buffer.writeInt(_userID);
	}
}
