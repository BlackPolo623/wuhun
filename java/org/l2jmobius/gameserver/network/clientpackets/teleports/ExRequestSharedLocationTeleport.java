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

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.SharedTeleportHolder;
import org.l2jmobius.gameserver.managers.SharedTeleportManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * @author NasSeKa
 */
public class ExRequestSharedLocationTeleport extends ClientPacket
{
	private int _id;
	
	@Override
	protected void readImpl()
	{
		_id = (readInt() - 1) / 256;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final SharedTeleportHolder teleport = SharedTeleportManager.getInstance().getTeleport(_id);
		if ((teleport == null) || (teleport.getCount() == 0))
		{
			player.sendPacket(SystemMessageId.TELEPORTATION_LIMIT_FOR_THE_COORDINATES_RECEIVED_IS_REACHED);
			return;
		}
		
		if (player.getName().equals(teleport.getName()))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_TO_YOURSELF);
			return;
		}
		
		if (player.getInventory().getInventoryItemCount(Inventory.LCOIN_ID, -1) < GeneralConfig.TELEPORT_SHARE_LOCATION_COST)
		{
			player.sendPacket(SystemMessageId.THERE_ARE_NOT_ENOUGH_L_COINS);
			return;
		}
		
		if ((player.getMovieHolder() != null) || player.isFishing() || player.isInInstance() || player.isOnEvent() || player.isInOlympiadMode() || player.inObserverMode() || player.isInTraingCamp() || player.isInTimedHuntingZone() || player.isInsideZone(ZoneId.SIEGE))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_RIGHT_NOW);
			return;
		}
		
		if (player.destroyItemByItemId(ItemProcessType.FEE, Inventory.LCOIN_ID, GeneralConfig.TELEPORT_SHARE_LOCATION_COST, player, true))
		{
			teleport.decrementCount();
			player.abortCast();
			player.stopMove(null);
			player.teleToLocation(teleport.getLocation());
		}
	}
}
