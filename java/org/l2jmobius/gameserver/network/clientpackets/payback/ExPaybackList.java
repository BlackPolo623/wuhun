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
package org.l2jmobius.gameserver.network.clientpackets.payback;

import java.util.Map;

import org.l2jmobius.gameserver.managers.events.PaybackManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.payback.PaybackList;

public class ExPaybackList extends ClientPacket
{
	private byte _eventId;
	
	@Override
	protected void readImpl()
	{
		_eventId = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final PaybackManager manager = PaybackManager.getInstance();
		final Map<String, String> local = manager.getLocalString(player.getLang() == null ? "en" : player.getLang());
		if (player.getLevel() < manager.getMinLevel())
		{
			player.sendPacket(new ExShowScreenMessage(local.get("minLevel"), 2000));
			return;
		}
		else if ((manager.getMaxLevel() != -1) && (player.getLevel() > manager.getMaxLevel()))
		{
			player.sendPacket(new ExShowScreenMessage(local.get("maxLevel"), 2000));
			return;
		}
		
		player.sendPacket(new PaybackList(player, _eventId));
	}
}
