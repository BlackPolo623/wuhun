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
package org.l2jmobius.gameserver.network.serverpackets;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

public class PvpBookList extends ServerPacket
{
	public PvpBookList()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PVPBOOK_LIST.writeId(this, buffer);
		final int size = 1;
		buffer.writeInt(4); // show killer's location count
		buffer.writeInt(5); // teleport count
		buffer.writeInt(size); // killer count
		for (int i = 0; i < size; i++)
		{
			buffer.writeSizedString("killer" + i); // killer name
			buffer.writeSizedString("clanKiller" + i); // killer clan name
			buffer.writeInt(15); // killer level
			buffer.writeInt(2); // killer race
			buffer.writeInt(10); // killer class
			buffer.writeInt((int) LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()); // kill time
			buffer.writeByte(1); // is online
		}
	}
}
