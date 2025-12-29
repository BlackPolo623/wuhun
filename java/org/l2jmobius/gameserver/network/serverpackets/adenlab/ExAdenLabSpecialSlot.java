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
package org.l2jmobius.gameserver.network.serverpackets.adenlab;

import java.util.Map;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author SaltyMike
 */
public class ExAdenLabSpecialSlot extends ServerPacket
{
	private final int _bossId;
	private final int _pageIndex;
	private final Map<Byte, Integer> _drawnOptionGrades;
	private final Map<Byte, Integer> _fixedOptionGrades;
	
	public ExAdenLabSpecialSlot(int bossId, int slotId, Map<Byte, Integer> drawnOptionGrades, Map<Byte, Integer> fixedOptionGrades)
	{
		_bossId = bossId;
		_pageIndex = slotId;
		_drawnOptionGrades = drawnOptionGrades;
		_fixedOptionGrades = fixedOptionGrades;
	}
	
	@Override
	protected void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_ADENLAB_SPECIAL_SLOT.writeId(this, buffer);
		buffer.writeInt(_bossId);
		buffer.writeInt(_pageIndex);
		
		buffer.writeInt(_drawnOptionGrades.size());
		for (int optionGrades : _drawnOptionGrades.values())
		{
			buffer.writeInt(optionGrades);
		}
		
		buffer.writeInt(_fixedOptionGrades.size());
		for (int optionGrades : _fixedOptionGrades.values())
		{
			buffer.writeInt(optionGrades);
		}
	}
}
