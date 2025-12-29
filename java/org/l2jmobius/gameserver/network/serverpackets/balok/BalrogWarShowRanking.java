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
package org.l2jmobius.gameserver.network.serverpackets.balok;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.managers.BattleWithBalokManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class BalrogWarShowRanking extends ServerPacket
{
	private final List<RankHolder> _ranking = new LinkedList<>();
	
	public BalrogWarShowRanking()
	{
		int rank = 1;
		for (Entry<Integer, Integer> entry : BattleWithBalokManager.getInstance().getTopPlayers(150).entrySet())
		{
			_ranking.add(new RankHolder(rank++, CharInfoTable.getInstance().getNameById(entry.getKey()), entry.getValue()));
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_BALROGWAR_SHOW_RANKING.writeId(this, buffer);
		buffer.writeInt(_ranking.size());
		for (RankHolder holder : _ranking)
		{
			buffer.writeInt(holder.getRank());
			buffer.writeSizedString(holder.getName());
			buffer.writeInt(holder.getScore());
		}
	}
	
	private class RankHolder
	{
		private final int _score;
		private final String _name;
		private final int _rank;
		
		public RankHolder(int score, String name, int rank)
		{
			_score = score;
			_name = name;
			_rank = rank;
		}
		
		public int getScore()
		{
			return _score;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public int getRank()
		{
			return _rank;
		}
	}
}
