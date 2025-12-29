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
package org.l2jmobius.gameserver.network.serverpackets.pledgeV3;

import java.util.List;
import java.util.stream.Collectors;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanWar;
import org.l2jmobius.gameserver.model.clan.enums.ClanWarState;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Berezkin Nikolay
 */
public class ExPledgeEnemyInfoList extends ServerPacket
{
	private final Clan _playerClan;
	private final List<ClanWar> _warList;
	
	public ExPledgeEnemyInfoList(Clan playerClan)
	{
		_playerClan = playerClan;
		_warList = playerClan.getWarList().values().stream().filter(it -> (it.getClanWarState(playerClan) == ClanWarState.MUTUAL) || (it.getAttackerClanId() == playerClan.getId())).collect(Collectors.toList());
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PLEDGE_ENEMY_INFO_LIST.writeId(this, buffer);
		buffer.writeInt(_warList.size());
		for (ClanWar war : _warList)
		{
			final Clan clan = war.getOpposingClan(_playerClan);
			buffer.writeInt(clan.getRank());
			buffer.writeInt(clan.getId());
			buffer.writeSizedString(clan.getName());
			buffer.writeSizedString(clan.getLeaderName());
			buffer.writeInt((int) (war.getStartTime() / 1000)); // 430
		}
	}
}
