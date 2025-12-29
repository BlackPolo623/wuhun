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
import org.l2jmobius.gameserver.model.item.henna.Henna;
import org.l2jmobius.gameserver.model.stats.BaseStat;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class HennaItemRemoveInfo extends ServerPacket
{
	private final Player _player;
	private final Henna _henna;
	
	public HennaItemRemoveInfo(Henna henna, Player player)
	{
		_henna = henna;
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.HENNA_UNEQUIP_INFO.writeId(this, buffer);
		buffer.writeInt(_henna.getDyeId()); // symbol Id
		buffer.writeInt(_henna.getDyeItemId()); // item id of dye
		buffer.writeLong(_henna.getCancelCount()); // total amount of dye require
		buffer.writeLong(_henna.getCancelFee()); // total amount of Adena require to remove symbol
		buffer.writeInt(_henna.isAllowedClass(_player)); // able to remove or not
		buffer.writeLong(_player.getAdena());
		buffer.writeInt(_player.getINT()); // current INT
		buffer.writeShort(_player.getINT() - _henna.getBaseStats(BaseStat.INT)); // equip INT
		buffer.writeInt(_player.getSTR()); // current STR
		buffer.writeShort(_player.getSTR() - _henna.getBaseStats(BaseStat.STR)); // equip STR
		buffer.writeInt(_player.getCON()); // current CON
		buffer.writeShort(_player.getCON() - _henna.getBaseStats(BaseStat.CON)); // equip CON
		buffer.writeInt(_player.getMEN()); // current MEN
		buffer.writeShort(_player.getMEN() - _henna.getBaseStats(BaseStat.MEN)); // equip MEN
		buffer.writeInt(_player.getDEX()); // current DEX
		buffer.writeShort(_player.getDEX() - _henna.getBaseStats(BaseStat.DEX)); // equip DEX
		buffer.writeInt(_player.getWIT()); // current WIT
		buffer.writeShort(_player.getWIT() - _henna.getBaseStats(BaseStat.WIT)); // equip WIT
		buffer.writeInt(0); // current LUC
		buffer.writeShort(0); // equip LUC
		buffer.writeInt(0); // current CHA
		buffer.writeShort(0); // equip CHA
		buffer.writeInt(_henna.getDuration() * 60000);
	}
}
