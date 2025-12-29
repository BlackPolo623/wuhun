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
package org.l2jmobius.gameserver.network.serverpackets.relics;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerRelicData;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller, Atronic
 */
public class ExRelicsUpdateList extends ServerPacket
{
	private final List<PlayerRelicData> _updatedRelics;
	
	public ExRelicsUpdateList(int relicListSize, int relicId, int relicLevel, int relicCount)
	{
		final PlayerRelicData relic = new PlayerRelicData(relicId, relicLevel, 0, 0, relicCount);
		_updatedRelics = new ArrayList<>(relicListSize);
		for (int i = 0; i < relicListSize; i++)
		{
			_updatedRelics.add(relic);
		}
	}
	
	public ExRelicsUpdateList(List<PlayerRelicData> updatedRelics)
	{
		_updatedRelics = updatedRelics;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_UPDATE_LIST.writeId(this, buffer);
		buffer.writeInt(_updatedRelics.size());
		for (PlayerRelicData relic : _updatedRelics)
		{
			buffer.writeInt(relic.getRelicId());
			buffer.writeInt(relic.getRelicLevel());
			buffer.writeInt(relic.getRelicCount());
		}
	}
}
