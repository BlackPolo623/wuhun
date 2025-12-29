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
package org.l2jmobius.gameserver.network.clientpackets.olympiad;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadInfo;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMatchMakingResult;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadRecord;

public class OlympiadMatchMakingCancel extends ClientPacket
{
	private byte _cGameRuleType;
	
	@Override
	protected void readImpl()
	{
		_cGameRuleType = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_cGameRuleType == 1)
		{
			OlympiadManager.getInstance().unRegisterNoble(player);
		}
		
		player.sendPacket(new ExOlympiadMatchMakingResult(_cGameRuleType, 0));
		
		if (OlympiadConfig.OLYMPIAD_ENABLED && Olympiad.getInstance().inCompPeriod())
		{
			player.sendPacket(new ExOlympiadInfo(1, Olympiad.getInstance().getRemainingTime()));
		}
		else
		{
			player.sendPacket(new ExOlympiadInfo(0, Olympiad.getInstance().getRemainingTime()));
		}
		
		player.sendPacket(new ExOlympiadRecord(player, _cGameRuleType));
	}
}
