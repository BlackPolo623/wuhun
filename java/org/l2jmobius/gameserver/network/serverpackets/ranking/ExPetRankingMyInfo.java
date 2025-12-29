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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Berezkin Nikolay
 */
public class ExPetRankingMyInfo extends ServerPacket
{
	private final int _petId;
	private final Player _player;
	private final Optional<Entry<Integer, StatSet>> _ranking;
	private final Optional<Entry<Integer, StatSet>> _snapshotRanking;
	private final Map<Integer, StatSet> _rankingList;
	private final Map<Integer, StatSet> _snapshotList;
	
	public ExPetRankingMyInfo(Player player, int petId)
	{
		_player = player;
		_petId = petId;
		_ranking = RankManager.getInstance().getPetRankList().entrySet().stream().filter(it -> it.getValue().getInt("controlledItemObjId") == petId).findFirst();
		_snapshotRanking = RankManager.getInstance().getSnapshotPetRankList().entrySet().stream().filter(it -> it.getValue().getInt("controlledItemObjId") == petId).findFirst();
		_rankingList = RankManager.getInstance().getPetRankList();
		_snapshotList = RankManager.getInstance().getSnapshotPetRankList();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PET_RANKING_MY_INFO.writeId(this, buffer);
		buffer.writeInt(_petId);
		buffer.writeShort(1);
		buffer.writeInt(-1);
		buffer.writeInt(0);
		buffer.writeInt(_ranking.isPresent() ? _ranking.get().getKey() : 0); // server rank
		buffer.writeInt(_snapshotRanking.isPresent() ? _snapshotRanking.get().getKey() : 0); // snapshot server rank
		if (_petId > 0)
		{
			int typeRank = 1;
			boolean found = false;
			for (StatSet ss : _rankingList.values())
			{
				if (ss.getInt("petType", -1) == PetDataTable.getInstance().getTypeByIndex(_player.getPetEvolve(_petId).getIndex()))
				{
					if (ss.getInt("controlledItemObjId", -1) == _petId)
					{
						found = true;
						buffer.writeInt(typeRank);
						break;
					}
					
					typeRank++;
				}
			}
			
			if (!found)
			{
				buffer.writeInt(0);
			}
			
			int snapshotTypeRank = 1;
			boolean snapshotFound = false;
			for (StatSet ss : _snapshotList.values())
			{
				if (ss.getInt("petType", -1) == PetDataTable.getInstance().getTypeByIndex(_player.getPetEvolve(_petId).getIndex()))
				{
					if (ss.getInt("controlledItemObjId", -1) == _petId)
					{
						snapshotFound = true;
						buffer.writeInt(snapshotTypeRank);
						break;
					}
					
					snapshotTypeRank++;
				}
			}
			
			if (!snapshotFound)
			{
				buffer.writeInt(0);
			}
		}
		else
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
	}
}
