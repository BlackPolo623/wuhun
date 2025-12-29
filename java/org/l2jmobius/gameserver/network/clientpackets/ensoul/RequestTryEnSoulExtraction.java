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
package org.l2jmobius.gameserver.network.clientpackets.ensoul;

import java.util.Collection;

import org.l2jmobius.gameserver.data.xml.EnsoulData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ensoul.ExEnSoulExtractionResult;

/**
 * @author Mobius
 */
public class RequestTryEnSoulExtraction extends ClientPacket
{
	private int _itemObjectId;
	private int _type;
	private int _position;
	
	@Override
	protected void readImpl()
	{
		_itemObjectId = readInt();
		_type = readByte();
		_position = readByte() - 1;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(_itemObjectId);
		if (item == null)
		{
			return;
		}
		
		EnsoulOption option = null;
		if (_type == 1)
		{
			option = item.getSpecialAbility(_position);
			
			// If position is invalid, check the other one.
			if ((option == null) && (_position == 0))
			{
				option = item.getSpecialAbility(1);
				if (option != null)
				{
					_position = 1;
				}
			}
		}
		
		if (_type == 2)
		{
			option = item.getAdditionalSpecialAbility(_position);
		}
		
		if (option == null)
		{
			return;
		}
		
		final int runeId = EnsoulData.getInstance().getStone(_type, option.getId());
		final Collection<ItemHolder> removalFee = EnsoulData.getInstance().getRemovalFee(runeId);
		if (removalFee.isEmpty())
		{
			return;
		}
		
		// Check if player has required items.
		for (ItemHolder itemHolder : removalFee)
		{
			if (player.getInventory().getInventoryItemCount(itemHolder.getId(), -1) < itemHolder.getCount())
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT);
				player.sendPacket(new ExEnSoulExtractionResult(false, item));
				return;
			}
		}
		
		// Take required items.
		for (ItemHolder itemHolder : removalFee)
		{
			player.destroyItemByItemId(ItemProcessType.FEE, itemHolder.getId(), itemHolder.getCount(), player, true);
		}
		
		// Remove equipped rune.
		item.removeSpecialAbility(_position, _type);
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(item);
		
		// Add rune in player inventory.
		if (runeId > 0)
		{
			iu.addItem(player.addItem(ItemProcessType.REWARD, runeId, 1, player, true));
		}
		
		player.sendInventoryUpdate(iu);
		player.sendItemList();
		
		player.sendPacket(new ExEnSoulExtractionResult(true, item));
	}
}
