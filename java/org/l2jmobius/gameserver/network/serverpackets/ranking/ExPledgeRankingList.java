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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Berezkin Nikolay
 */
public class ExPledgeRankingList extends ServerPacket
{
	private final Player _player;
	private final int _category;
	private final Map<Integer, StatSet> _rankingClanList;
	private final Map<Integer, StatSet> _snapshotClanList;
	
	public ExPledgeRankingList(Player player, int category)
	{
		_player = player;
		_category = category;
		_rankingClanList = RankManager.getInstance().getClanRankList();
		_snapshotClanList = RankManager.getInstance().getSnapshotClanRankList();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PLEDGE_RANKING_LIST.writeId(this, buffer);
		buffer.writeByte(_category);
		if (!_rankingClanList.isEmpty())
		{
			writeScopeData(buffer, _category == 0, new ArrayList<>(_rankingClanList.entrySet()), new ArrayList<>(_snapshotClanList.entrySet()));
		}
		else
		{
			buffer.writeInt(0);
		}
	}
	
	private void writeScopeData(WritableBuffer buffer, boolean isTop150, List<Entry<Integer, StatSet>> list, List<Entry<Integer, StatSet>> snapshot)
	{
		Entry<Integer, StatSet> playerData = list.stream().filter(it -> it.getValue().getInt("clan_id", 0) == _player.getClanId()).findFirst().orElse(null);
		final int indexOf = list.indexOf(playerData);
		final List<Entry<Integer, StatSet>> limited = isTop150 ? list.stream().limit(150).collect(Collectors.toList()) : playerData == null ? Collections.emptyList() : list.subList(Math.max(0, indexOf - 10), Math.min(list.size(), indexOf + 10));
		buffer.writeInt(limited.size());
		int rank = 1;
		for (Entry<Integer, StatSet> data : limited.stream().sorted(Entry.comparingByKey()).collect(Collectors.toList()))
		{
			int curRank = rank++;
			final StatSet player = data.getValue();
			buffer.writeInt(!isTop150 ? data.getKey() : curRank);
			for (Entry<Integer, StatSet> ssData : snapshot.stream().sorted(Entry.comparingByKey()).collect(Collectors.toList()))
			{
				final StatSet snapshotData = ssData.getValue();
				if (player.getInt("clan_id") == snapshotData.getInt("clan_id"))
				{
					buffer.writeInt(!isTop150 ? ssData.getKey() : curRank); // server rank snapshot
				}
			}
			
			buffer.writeSizedString(player.getString("clan_name"));
			buffer.writeInt(player.getInt("clan_level"));
			buffer.writeSizedString(player.getString("char_name"));
			buffer.writeInt(player.getInt("level"));
			buffer.writeInt(ClanTable.getInstance().getClan(player.getInt("clan_id")) != null ? ClanTable.getInstance().getClan(player.getInt("clan_id")).getMembersCount() : 0);
			buffer.writeInt((int) Math.min(Integer.MAX_VALUE, player.getLong("exp")));
		}
	}
}
