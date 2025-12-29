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
package org.l2jmobius.gameserver.network.serverpackets.subjugation;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.PurgeRankingManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Berezkin Nikolay
 */
public class ExSubjugationRanking extends ServerPacket
{
	private final Map<String, Integer> _ranking;
	private final int _category;
	private final SimpleEntry<Integer, Integer> _playerPoints;
	
	public ExSubjugationRanking(int category, int objectId)
	{
		_ranking = PurgeRankingManager.getInstance().getTop5(category);
		_category = category;
		_playerPoints = PurgeRankingManager.getInstance().getPlayerRating(_category, objectId);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SUBJUGATION_RANKING.writeId(this, buffer);
		buffer.writeInt(_ranking.entrySet().size());
		int counter = 1;
		for (Entry<String, Integer> data : _ranking.entrySet())
		{
			buffer.writeSizedString(data.getKey());
			buffer.writeInt(data.getValue());
			buffer.writeInt(counter++);
		}
		
		buffer.writeInt(_category);
		buffer.writeInt(_playerPoints.getValue());
		buffer.writeInt(_playerPoints.getKey());
	}
}
