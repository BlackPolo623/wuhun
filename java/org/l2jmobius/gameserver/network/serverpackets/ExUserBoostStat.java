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
import org.l2jmobius.gameserver.model.actor.enums.player.BonusExpType;
import org.l2jmobius.gameserver.model.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class ExUserBoostStat extends ServerPacket
{
	private final Player _player;
	private final BonusExpType _type;
	
	public ExUserBoostStat(Player player, BonusExpType type)
	{
		_player = player;
		_type = type;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_USER_BOOST_STAT.writeId(this, buffer);
		int count = 0;
		int bonus = 0;
		final PlayerStat stat = _player.getStat();
		switch (_type)
		{
			case VITALITY:
			{
				final int vitalityBonus = (int) (stat.getVitalityExpBonus() * 100);
				if (vitalityBonus > 0)
				{
					count = 1;
					bonus = vitalityBonus;
				}
				break;
			}
			case BUFFS:
			{
				count = (int) stat.getValue(Stat.BONUS_EXP_BUFFS, 0);
				bonus = (int) (stat.getValue(Stat.ACTIVE_BONUS_EXP, 0) * 10);
				break;
			}
			case PASSIVE:
			{
				count = (int) stat.getValue(Stat.BONUS_EXP_PASSIVES, 0);
				bonus = (int) ((stat.getValue(Stat.BONUS_EXP, 0) - stat.getValue(Stat.ACTIVE_BONUS_EXP, 0)) * 10);
				break;
			}
		}
		
		buffer.writeByte(_type.getId());
		buffer.writeByte(count);
		buffer.writeShort(bonus);
	}
}
