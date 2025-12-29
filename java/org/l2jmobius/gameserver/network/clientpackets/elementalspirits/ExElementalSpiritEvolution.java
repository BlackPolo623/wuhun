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
package org.l2jmobius.gameserver.network.clientpackets.elementalspirits;

import java.util.stream.Collectors;

import org.l2jmobius.gameserver.model.ElementalSpirit;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.ElementalSpiritType;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.itemcontainer.InventoryBlockType;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.elementalspirits.ElementalSpiritEvolution;

/**
 * @author JoeAlisson
 */
public class ExElementalSpiritEvolution extends ClientPacket
{
	private byte _type;
	
	@Override
	protected void readImpl()
	{
		_type = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final ElementalSpirit spirit = player.getElementalSpirit(ElementalSpiritType.of(_type));
		if (spirit == null)
		{
			player.sendPacket(SystemMessageId.NO_SPIRITS_ARE_AVAILABLE);
			return;
		}
		
		final boolean canEvolve = checkConditions(player, spirit);
		if (canEvolve)
		{
			spirit.upgrade();
			player.sendPacket(new SystemMessage(SystemMessageId.S1_HAS_EVOLVED_TO_LV_S2).addElementalSpiritName(_type).addInt(spirit.getStage()));
			final UserInfo userInfo = new UserInfo(player);
			userInfo.addComponentType(UserInfoType.ATT_SPIRITS);
			player.sendPacket(userInfo);
		}
		
		player.sendPacket(new ElementalSpiritEvolution(player, _type, canEvolve));
	}
	
	private boolean checkConditions(Player player, ElementalSpirit spirit)
	{
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.CANNOT_EVOLVE_ABSORB_EXTRACT_WHILE_USING_THE_PRIVATE_STORE_WORKSHOP);
			return false;
		}
		
		if (player.isInBattle())
		{
			player.sendPacket(SystemMessageId.UNABLE_TO_EVOLVE_DURING_BATTLE);
			return false;
		}
		
		if (!spirit.canEvolve())
		{
			player.sendPacket(SystemMessageId.THIS_SPIRIT_CANNOT_EVOLVE);
			return false;
		}
		
		if (!consumeEvolveItems(player, spirit))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_INGREDIENTS_FOR_EVOLUTION);
			return false;
		}
		
		return true;
	}
	
	private boolean consumeEvolveItems(Player player, ElementalSpirit spirit)
	{
		final PlayerInventory inventory = player.getInventory();
		try
		{
			inventory.setInventoryBlock(spirit.getItemsToEvolve().stream().map(ItemHolder::getId).collect(Collectors.toList()), InventoryBlockType.BLACKLIST);
			for (ItemHolder itemHolder : spirit.getItemsToEvolve())
			{
				if (inventory.getInventoryItemCount(itemHolder.getId(), -1) < itemHolder.getCount())
				{
					return false;
				}
			}
			
			for (ItemHolder itemHolder : spirit.getItemsToEvolve())
			{
				player.destroyItemByItemId(ItemProcessType.FEE, itemHolder.getId(), itemHolder.getCount(), player, true);
			}
			
			return true;
		}
		finally
		{
			inventory.unblock();
		}
	}
}
