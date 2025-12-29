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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.enums.SoulType;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class EtcStatusUpdate extends ServerPacket
{
	private final Player _player;
	private int _mask;
	
	public EtcStatusUpdate(Player player)
	{
		_player = player;
		_mask = _player.getMessageRefusal() || _player.isChatBanned() || _player.isSilenceMode() ? 1 : 0;
		_mask |= _player.isInsideZone(ZoneId.DANGER_AREA) ? 2 : 0;
		_mask |= _player.hasCharmOfCourage() ? 4 : 0;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.ETC_STATUS_UPDATE.writeId(this, buffer);
		buffer.writeByte(_player.getCharges()); // 1-7 increase force, level
		buffer.writeInt(_player.getWeightPenalty()); // 1-4 weight penalty, level (1=50%, 2=66.6%, 3=80%, 4=100%)
		buffer.writeByte(0); // Weapon Grade Penalty [1-4]
		buffer.writeByte(0); // Armor Grade Penalty [1-4]
		buffer.writeByte(0); // Death Penalty [1-15, 0 = disabled)], not used anymore in Ertheia
		buffer.writeByte(0); // Old count for charged souls.
		buffer.writeByte(_mask);
		buffer.writeByte(_player.getChargedSouls(SoulType.SHADOW)); // Shadow souls
		buffer.writeByte(_player.getChargedSouls(SoulType.LIGHT)); // Light souls
	}
}
