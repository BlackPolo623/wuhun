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
package org.l2jmobius.gameserver.network.clientpackets.newhenna;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.HennaData;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.henna.Henna;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.newhenna.NewHennaEquip;

/**
 * @author Index, Serenitty
 */
public class RequestNewHennaEquip extends ClientPacket
{
	private int _slotId;
	private int _symbolId;
	private int _otherItemId;
	
	@Override
	protected void readImpl()
	{
		_slotId = readByte();
		_symbolId = readInt();
		_otherItemId = readInt(); // CostItemId
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().canPerformTransaction())
		{
			return;
		}
		
		if (player.getHennaEmptySlots() == 0)
		{
			PacketLogger.warning(player + ": Invalid Henna error 0 Id " + _symbolId + " " + _slotId);
			player.sendPacket(SystemMessageId.YOU_CANNOT_MAKE_A_PATTERN);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(_symbolId);
		if (item == null)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.sendPacket(new NewHennaEquip(_slotId, 0, false));
			return;
		}
		
		final Henna henna = HennaData.getInstance().getHennaByItemId(item.getId());
		if (henna == null)
		{
			PacketLogger.warning(player + ": Invalid Henna SymbolId " + _symbolId + " " + _slotId + " " + item.getTemplate());
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.sendPacket(SystemMessageId.YOU_CANNOT_MAKE_A_PATTERN);
			return;
		}
		
		final long _count = player.getInventory().getInventoryItemCount(henna.getDyeItemId(), -1);
		if (henna.isAllowedClass(player) && (_count >= henna.getWearCount()) && ((player.getAdena() >= henna.getWearFee()) || (player.getInventory().getItemByItemId(91663).getCount() >= henna.getL2CoinFee())) && player.addHenna(_slotId, henna))
		
		{
			int feeType = 0;
			
			if (_otherItemId == 57)
			{
				feeType = henna.getWearFee();
			}
			
			if (_otherItemId == 91663)
			{
				feeType = henna.getL2CoinFee();
			}
			
			player.destroyItemByItemId(ItemProcessType.FEE, henna.getDyeItemId(), henna.getWearCount(), player, true);
			player.destroyItemByItemId(ItemProcessType.FEE, _otherItemId, feeType, player, true);
			if (player.getAdena() > 0)
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(player.getInventory().getAdenaInstance());
				player.sendInventoryUpdate(iu);
			}
			
			player.sendPacket(new NewHennaEquip(_slotId, henna.getDyeId(), true));
			player.getStat().recalculateStats(true);
			player.sendPacket(new UserInfo(player));
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_MAKE_A_PATTERN);
			if (!player.isGM() && !henna.isAllowedClass(player))
			{
				PunishmentManager.handleIllegalPlayerAction(player, "Exploit attempt: Character " + player.getName() + " of account " + player.getAccountName() + " tryed to add a forbidden henna.", GeneralConfig.DEFAULT_PUNISH);
			}
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.sendPacket(new NewHennaEquip(_slotId, henna.getDyeId(), false));
		}
	}
}
