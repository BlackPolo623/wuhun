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
import org.l2jmobius.gameserver.data.xml.RelicCollectionData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerRelicCollectionData;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author CostyKiller
 */
public class ExRelicsCollectionInfo extends ServerPacket
{
	private final Player _player;
	private final List<Integer> _relicCollectionIds = new ArrayList<>();
	
	public ExRelicsCollectionInfo(Player player)
	{
		_player = player;
		_relicCollectionIds.clear();
		for (PlayerRelicCollectionData relicCollection : player.getRelicCollections())
		{
			if (!_relicCollectionIds.contains(relicCollection.getRelicCollectionId()))
			{
				_relicCollectionIds.add(relicCollection.getRelicCollectionId());
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_COLLECTION_INFO.writeId(this, buffer);
		
		buffer.writeInt(1); // Page index.
		buffer.writeInt(82); // Max page.
		buffer.writeInt(_relicCollectionIds.size());
		
		for (int i = 1; i <= (_relicCollectionIds.size()); i++)
		{
			buffer.writeInt(_relicCollectionIds.get(i - 1)); // Collection array position.
			
			int completeCount = 0;
			for (PlayerRelicCollectionData coll : _player.getRelicCollections())
			{
				if (coll.getRelicCollectionId() == _relicCollectionIds.get(i - 1))
				{
					completeCount++;
				}
			}
			
			final int completedRelicsCount = RelicCollectionData.getInstance().getRelicCollection(_relicCollectionIds.get(i - 1)).getCompleteCount();
			buffer.writeByte(completeCount == completedRelicsCount);
			buffer.writeInt(completeCount);
			
			for (PlayerRelicCollectionData collection : _player.getRelicCollections())
			{
				if (collection.getRelicCollectionId() == _relicCollectionIds.get(i - 1))
				{
					buffer.writeInt(collection.getRelicId()); // Relic id.
					buffer.writeInt(collection.getRelicLevel()); // Relic level.
				}
			}
		}
	}
}
