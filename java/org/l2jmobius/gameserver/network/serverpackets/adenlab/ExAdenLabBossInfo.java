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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author SaltyMike
 */
public class ExAdenLabBossInfo extends ServerPacket
{
	private final int _bossId;
	private final int _currentUnlockedSlot;
	private final int _transcendEnchant;
	private final int _normalGameSaleDailyCount;
	private final int _normalGameDailyCount;
	private final Map<Byte, Map<Byte, Integer>> _specialSlots = new HashMap<>(); // <pageIndex, <optionIndex, stageLevel>>
	
	public ExAdenLabBossInfo(byte bossId, Player player)
	{
		_bossId = bossId;
		_currentUnlockedSlot = player.getAdenLabCurrentlyUnlockedPage(bossId);
		_transcendEnchant = player.getAdenLabCurrentTranscendLevel(bossId);
		_normalGameSaleDailyCount = 0; // UNK use-case
		_normalGameDailyCount = 0; // UNK use-case
		_specialSlots.putAll(player.getAdenLabSpecialGameStagesConfirmedOptions().isEmpty() ? new HashMap<>() : player.getAdenLabSpecialGameStagesConfirmedOptions().get(bossId)); // slotId (up to 4) + optionGrades (which is the stage)
	}
	
	@Override
	protected void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_ADENLAB_BOSS_INFO.writeId(this, buffer);
		buffer.writeInt(_bossId);
		buffer.writeInt(_currentUnlockedSlot);
		buffer.writeInt(_transcendEnchant);
		buffer.writeInt(_normalGameSaleDailyCount);
		buffer.writeInt(_normalGameDailyCount);
		
		buffer.writeInt(_specialSlots.size());
		for (Entry<Byte, Map<Byte, Integer>> slot : _specialSlots.entrySet())
		{
			final Map<Byte, Integer> values = slot.getValue();
			buffer.writeInt(slot.getKey());
			buffer.writeInt(values.size());
			for (Integer value : values.values())
			{
				buffer.writeInt(value);
			}
		}
	}
}
