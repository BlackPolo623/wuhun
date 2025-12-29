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
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.enums.CharacterStyleCategoryType;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.matching.MatchingRoom;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Atronic
 */
public class ExPartyRoomAnnounce extends ServerPacket
{
	private final MatchingRoom _room;
	
	public ExPartyRoomAnnounce(Player player)
	{
		_room = player.getMatchingRoom();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PARTY_ROOM_ANNOUNCE.writeId(this, buffer);
		
		if (_room == null)
		{
			return;
		}
		
		final Player leader = _room.getLeader();
		final int minLevel = _room.getMinLevel();
		final int maxLevel = _room.getMaxLevel();
		final int memberCount = _room.getMembersCount();
		
		buffer.writeSizedString(leader.getName()); // sName
		buffer.writeSizedString(_room.getTitle()); // sTitle
		buffer.writeInt(ServerConfig.SERVER_ID); // nWorldID
		buffer.writeInt(ServerConfig.SERVER_ID); // nMasterServerID
		buffer.writeInt(_room.getId()); // nPartyRoomID
		buffer.writeInt(minLevel); // nLowerLevelLimit
		buffer.writeInt(maxLevel); // nUpperLevelLimit
		buffer.writeInt(memberCount); // nPartyMemberCount
		buffer.writeInt(leader.getPledgeClass()); // cCharRankGrade
		
		int castleId = 0;
		if (leader.getClan() != null)
		{
			castleId = leader.getClan().getCastleId();
		}
		
		buffer.writeInt(castleId); // cPledgeCastleDBID
		buffer.writeInt(0); // cEventEmblemID
		buffer.writeInt(leader.getVariables().getInt(PlayerVariables.ACTIVE_CHARACTER_STYLE + CharacterStyleCategoryType.CHAT_BACKGROUND, 0));
	}
}
