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
package org.l2jmobius.gameserver.network.serverpackets.ranking;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Berezkin Nikolay
 */
public class ExPledgeRankingMyInfo extends ServerPacket
{
	private final Player _player;
	
	public ExPledgeRankingMyInfo(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PLEDGE_RANKING_MY_INFO.writeId(this, buffer);
		final Clan clan = _player.getClan();
		buffer.writeInt(clan != null ? RankManager.getInstance().getClanRankList().entrySet().stream().anyMatch(it -> it.getValue().getInt("clan_id") == _player.getClanId()) ? RankManager.getInstance().getClanRankList().entrySet().stream().filter(it -> it.getValue().getInt("clan_id") == _player.getClanId()).findFirst().orElse(null).getKey() : 0 : 0); // rank
		buffer.writeInt(clan != null ? RankManager.getInstance().getSnapshotClanRankList().entrySet().stream().anyMatch(it -> it.getValue().getInt("clan_id") == _player.getClanId()) ? RankManager.getInstance().getSnapshotClanRankList().entrySet().stream().filter(it -> it.getValue().getInt("clan_id") == _player.getClanId()).findFirst().orElse(null).getKey() : 0 : 0); // snapshot
		buffer.writeInt(clan != null ? clan.getExp() : 0); // exp
	}
}
