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
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.actor.enums.player.RankingOlympiadCategory;
import org.l2jmobius.gameserver.model.actor.enums.player.RankingOlympiadScope;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Berezkin Nikolay, Mobius
 */
public class ExOlympiadRankingInfo extends ServerPacket
{
	private final Player _player;
	private final int _tabId;
	private final int _rankingType;
	private final int _unk;
	private final int _classId;
	private final int _serverId;
	private final Map<Integer, StatSet> _playerList;
	private final Map<Integer, StatSet> _snapshotList;
	
	public ExOlympiadRankingInfo(Player player, int tabId, int rankingType, int unk, int classId, int serverId)
	{
		_player = player;
		_tabId = tabId;
		_rankingType = rankingType;
		_unk = unk;
		_classId = classId;
		_serverId = serverId;
		_playerList = RankManager.getInstance().getOlyRankList();
		_snapshotList = RankManager.getInstance().getSnapshotOlyList();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_OLYMPIAD_RANKING_INFO.writeId(this, buffer);
		buffer.writeByte(_tabId); // Tab id
		buffer.writeByte(_rankingType); // ranking type
		buffer.writeByte(_unk); // unk, shows 1 all time
		buffer.writeInt(_classId); // class id (default 148) or caller class id for personal rank
		buffer.writeInt(_serverId); // 0 - all servers, server id - for caller server
		buffer.writeInt(933); // unk, 933 all time
		buffer.writeInt(OlympiadConfig.OLYMPIAD_MIN_MATCHES); // ElectionMatchCount (474)
		if (!_playerList.isEmpty())
		{
			final RankingOlympiadCategory category = RankingOlympiadCategory.values()[_tabId];
			writeFilteredRankingData(category, category.getScopeByGroup(_rankingType), PlayerClass.getPlayerClass(_classId), buffer);
		}
	}
	
	private void writeFilteredRankingData(RankingOlympiadCategory category, RankingOlympiadScope scope, PlayerClass playerClass, WritableBuffer buffer)
	{
		switch (category)
		{
			case SERVER:
			{
				writeScopeData(scope, new ArrayList<>(_playerList.entrySet()), new ArrayList<>(_snapshotList.entrySet()), buffer);
				break;
			}
			case CLASS:
			{
				writeScopeData(scope, _playerList.entrySet().stream().filter(it -> it.getValue().getInt("classId") == playerClass.getId()).collect(Collectors.toList()), _snapshotList.entrySet().stream().filter(it -> it.getValue().getInt("classId") == playerClass.getId()).collect(Collectors.toList()), buffer);
				break;
			}
		}
	}
	
	private void writeScopeData(RankingOlympiadScope scope, List<Entry<Integer, StatSet>> list, List<Entry<Integer, StatSet>> snapshot, WritableBuffer buffer)
	{
		Entry<Integer, StatSet> playerData = list.stream().filter(it -> it.getValue().getInt("charId", 0) == _player.getObjectId()).findFirst().orElse(null);
		final int indexOf = list.indexOf(playerData);
		final List<Entry<Integer, StatSet>> limited;
		switch (scope)
		{
			case TOP_100:
			{
				limited = list.stream().limit(100).collect(Collectors.toList());
				break;
			}
			case ALL:
			{
				limited = list;
				break;
			}
			case TOP_50:
			{
				limited = list.stream().limit(50).collect(Collectors.toList());
				break;
			}
			case SELF:
			{
				limited = playerData == null ? Collections.emptyList() : list.subList(Math.max(0, indexOf - 10), Math.min(list.size(), indexOf + 10));
				break;
			}
			default:
			{
				limited = Collections.emptyList();
			}
		}
		
		buffer.writeInt(limited.size());
		int rank = 1;
		for (Entry<Integer, StatSet> data : limited.stream().sorted(Entry.comparingByKey()).collect(Collectors.toList()))
		{
			int curRank = rank++;
			final StatSet player = data.getValue();
			buffer.writeSizedString(player.getString("name")); // name
			buffer.writeSizedString(player.getString("clanName")); // clan name
			buffer.writeInt(scope == RankingOlympiadScope.SELF ? data.getKey() : curRank); // rank
			if (!snapshot.isEmpty())
			{
				int snapshotRank = 1;
				for (Entry<Integer, StatSet> ssData : snapshot.stream().sorted(Entry.comparingByKey()).collect(Collectors.toList()))
				{
					final StatSet snapshotData = ssData.getValue();
					if (player.getInt("charId") == snapshotData.getInt("charId"))
					{
						buffer.writeInt(scope == RankingOlympiadScope.SELF ? ssData.getKey() : snapshotRank++); // previous rank
					}
				}
			}
			else
			{
				buffer.writeInt(scope == RankingOlympiadScope.SELF ? data.getKey() : curRank);
			}
			
			buffer.writeInt(OlympiadConfig.OLYMPIAD_MIN_MATCHES); // ElectionRank (474)
			buffer.writeInt(ServerConfig.SERVER_ID); // server id
			buffer.writeInt(player.getInt("level")); // level
			buffer.writeInt(player.getInt("classId")); // class id
			buffer.writeInt(player.getInt("clanLevel")); // clan level
			buffer.writeInt(player.getInt("competitions_won")); // win count
			buffer.writeInt(player.getInt("competitions_lost")); // lose count
			buffer.writeInt(player.getInt("competitions_drawn")); // DrawCount (474)
			buffer.writeInt(player.getInt("olympiad_points")); // points
			buffer.writeInt(player.getInt("legend_count")); // legend count
			buffer.writeInt(player.getInt("count")); // hero count
		}
	}
}
