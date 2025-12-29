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
package org.l2jmobius.gameserver.network.clientpackets.teleports;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.data.xml.TeleportListData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * @author Mobius
 */
public class ExRequestTeleportFavoritesAddDel extends ClientPacket
{
	private boolean _enable;
	private int _teleportId;
	
	@Override
	protected void readImpl()
	{
		_enable = readByte() == 1;
		_teleportId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (TeleportListData.getInstance().getTeleport(_teleportId) == null)
		{
			PacketLogger.warning("No registered teleport location for id: " + _teleportId);
			return;
		}
		
		final List<Integer> favorites = new ArrayList<>();
		for (int id : player.getVariables().getIntegerList(PlayerVariables.FAVORITE_TELEPORTS))
		{
			if (TeleportListData.getInstance().getTeleport(_teleportId) == null)
			{
				PacketLogger.warning("No registered teleport location for id: " + _teleportId);
			}
			else
			{
				favorites.add(id);
			}
		}
		
		if (_enable)
		{
			if (!favorites.contains(_teleportId))
			{
				favorites.add(_teleportId);
			}
		}
		else
		{
			favorites.remove((Integer) _teleportId);
		}
		
		player.getVariables().setIntegerList(PlayerVariables.FAVORITE_TELEPORTS, favorites);
	}
}
