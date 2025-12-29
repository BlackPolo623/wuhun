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
package org.l2jmobius.gameserver.network.clientpackets.commission;

import org.l2jmobius.gameserver.managers.ItemCommissionManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExCloseCommission;

/**
 * @author NosBit, Ren
 */
public class RequestCommissionRegister extends ClientPacket
{
	private int _itemObjectId;
	private long _pricePerUnit;
	private long _itemCount;
	private int _durationType; // -1 = None, 0 = 1 Day, 1 = 3 Days, 2 = 5 Days, 3 = 7 Days, 4 = 15 Days, 5 = 30 Days;
	private int _feeDiscountType; // 0 = none, 1 = 30% discount, 2 = 100% discount;
	
	@Override
	protected void readImpl()
	{
		_itemObjectId = readInt();
		readString(); // Item Name they use it for search we will use server side available names.
		_pricePerUnit = readLong();
		_itemCount = readLong();
		_durationType = readInt();
		_feeDiscountType = readShort();
		
		// readShort(); // Unknown IDS;
		// readInt(); // Unknown
		// readInt(); // Unknown
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((_feeDiscountType < 0) || (_feeDiscountType > 2))
		{
			PacketLogger.warning(player + " sent incorrect commission discount type: " + _feeDiscountType + ".");
			return;
		}
		
		if ((_feeDiscountType == 1) && (player.getInventory().getItemByItemId(22351) == null))
		{
			PacketLogger.warning(player + ": Auction House Fee 30% Voucher not found in inventory.");
			return;
		}
		else if ((_feeDiscountType == 2) && (player.getInventory().getItemByItemId(22352) == null))
		{
			PacketLogger.warning(player + ": Auction House Fee 100% Voucher not found in inventory.");
			return;
		}
		
		if ((_durationType < 0) || (_durationType > 5))
		{
			PacketLogger.warning(player + " sent incorrect commission duration type: " + _durationType + ".");
			return;
		}
		
		if ((_durationType == 4) && (player.getInventory().getItemByItemId(22353) == null))
		{
			PacketLogger.warning(player + ": Auction House (15-day) Extension not found in inventory.");
			return;
		}
		else if ((_durationType == 5) && (player.getInventory().getItemByItemId(22354) == null))
		{
			PacketLogger.warning(player + ": Auction House (30-day) Extension not found in inventory.");
			return;
		}
		
		if (!ItemCommissionManager.isPlayerAllowedToInteract(player))
		{
			player.sendPacket(ExCloseCommission.STATIC_PACKET);
			return;
		}
		
		ItemCommissionManager.getInstance().registerItem(player, _itemObjectId, _itemCount, _pricePerUnit, _durationType, (byte) Math.min((_feeDiscountType * 30) * _feeDiscountType, 100));
	}
}
