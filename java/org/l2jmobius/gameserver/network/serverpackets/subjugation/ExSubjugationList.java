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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerPurgeHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Berezkin Nikolay
 */
public class ExSubjugationList extends ServerPacket
{
	private final List<Entry<Integer, PlayerPurgeHolder>> _playerHolder;
	
	public ExSubjugationList(Map<Integer, PlayerPurgeHolder> playerHolder)
	{
		_playerHolder = playerHolder.entrySet().stream().filter(it -> it.getValue() != null).collect(Collectors.toList());
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SUBJUGATION_LIST.writeId(this, buffer);
		buffer.writeInt(_playerHolder.size());
		for (Entry<Integer, PlayerPurgeHolder> integerPlayerPurgeHolderEntry : _playerHolder)
		{
			buffer.writeInt(integerPlayerPurgeHolderEntry.getKey());
			buffer.writeInt(integerPlayerPurgeHolderEntry.getValue() != null ? integerPlayerPurgeHolderEntry.getValue().getPoints() : 0);
			buffer.writeInt(integerPlayerPurgeHolderEntry.getValue() != null ? integerPlayerPurgeHolderEntry.getValue().getKeys() : 0);
			buffer.writeInt(integerPlayerPurgeHolderEntry.getValue() != null ? integerPlayerPurgeHolderEntry.getValue().getRemainingKeys() : 40);
		}
	}
}
