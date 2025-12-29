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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Brado
 */
public class ExRelicsCombination extends ServerPacket
{
	private final ArrayList<Integer> _successCompoundIds;
	private final ArrayList<Integer> _failCompoundIds;
	
	public ExRelicsCombination(Player player, ArrayList<Integer> successCompoundIds, ArrayList<Integer> failCompoundIds)
	{
		_successCompoundIds = successCompoundIds;
		_failCompoundIds = failCompoundIds;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RELICS_COMBINATION.writeId(this, buffer);
		buffer.writeByte(1); // If not true the compound result page is not showing up.
		buffer.writeInt(_successCompoundIds.size() + _failCompoundIds.size()); // Obtained relics count array size.
		for (int receivedRelicId : _successCompoundIds)
		{
			buffer.writeInt(receivedRelicId);
		}
		
		for (int receivedRelicId : _failCompoundIds)
		{
			buffer.writeInt(receivedRelicId);
		}
		
		buffer.writeInt(0); // Obtained items when failed array size.
		buffer.writeInt(0); // Item 1 id.
		buffer.writeLong(0); // Item 1 count.
	}
}
