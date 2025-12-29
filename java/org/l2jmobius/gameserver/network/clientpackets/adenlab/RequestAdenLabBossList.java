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
package org.l2jmobius.gameserver.network.clientpackets.adenlab;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.AdenLaboratoryConfig;
import org.l2jmobius.gameserver.managers.AdenLaboratoryManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.AdenLabRequest;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;

/**
 * @author SaltyMike
 */
public class RequestAdenLabBossList extends ClientPacket
{
	private static final Logger LOGGER = Logger.getLogger(RequestAdenLabBossList.class.getName());
	
	private final List<Integer> _bossList = new ArrayList<>();
	
	@Override
	protected void readImpl()
	{
		while (remaining() > 0)
		{
			_bossList.add(readInt());
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!AdenLaboratoryConfig.ADENLAB_ENABLED || player.hasRequest(AdenLabRequest.class))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.sendPacket(SystemMessageId.THE_FUNCTION_IS_UNAVAILABLE);
			LOGGER.warning("Player " + player.getName() + " [" + player.getObjectId() + "] tried to access the Aden Lab while it is disabled or has another active request.");
			return;
		}
		
		AdenLaboratoryManager.processRequestAdenLabBossList(player, _bossList);
	}
}
