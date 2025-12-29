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
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Arrays;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.EnchantItemAttributeRequest;
import org.l2jmobius.gameserver.model.actor.request.EnchantItemRequest;
import org.l2jmobius.gameserver.model.item.Armor;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.VariationFee;
import org.l2jmobius.gameserver.model.skill.AbnormalType;
import org.l2jmobius.gameserver.network.SystemMessageId;

public abstract class AbstractRefinePacket extends ClientPacket
{
	/**
	 * Checks player, source item, lifestone and gemstone validity for augmentation process
	 * @param player
	 * @param item
	 * @param mineralItem
	 * @param feeItem
	 * @param fee
	 * @return
	 */
	protected static boolean isValid(Player player, Item item, Item mineralItem, Item feeItem, VariationFee fee)
	{
		if (fee == null)
		{
			return false;
		}
		
		if (!isValid(player, item, mineralItem))
		{
			return false;
		}
		
		if (feeItem != null)
		{
			// GemStones must belong to owner
			if (feeItem.getOwnerId() != player.getObjectId())
			{
				return false;
			}
			
			// .. and located in inventory
			if (feeItem.getItemLocation() != ItemLocation.INVENTORY)
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks player, source item and lifestone validity for augmentation process
	 * @param player
	 * @param item
	 * @param mineralItem
	 * @return
	 */
	protected static boolean isValid(Player player, Item item, Item mineralItem)
	{
		if (!isValid(player, item))
		{
			return false;
		}
		
		// Item must belong to owner
		if (mineralItem.getOwnerId() != player.getObjectId())
		{
			return false;
		}
		
		// Lifestone must be located in inventory
		if (mineralItem.getItemLocation() != ItemLocation.INVENTORY)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check both player and source item conditions for augmentation process
	 * @param player
	 * @param item
	 * @return
	 */
	protected static boolean isValid(Player player, Item item)
	{
		if (!isValid(player))
		{
			return false;
		}
		
		// Item must belong to owner
		if (item.getOwnerId() != player.getObjectId())
		{
			return false;
		}
		
		if (item.isHeroItem())
		{
			return false;
		}
		
		if (item.isShadowItem())
		{
			return false;
		}
		
		if (item.isCommonItem())
		{
			return false;
		}
		
		if (item.isEtcItem())
		{
			return false;
		}
		
		if (item.isTimeLimitedItem())
		{
			return false;
		}
		
		if (item.isPvp() && !PlayerConfig.ALT_ALLOW_AUGMENT_PVP_ITEMS)
		{
			return false;
		}
		
		// Source item can be equipped or in inventory
		switch (item.getItemLocation())
		{
			case INVENTORY:
			case PAPERDOLL:
			{
				break;
			}
			default:
			{
				return false;
			}
		}
		
		if (!(item.getTemplate() instanceof Weapon) && !(item.getTemplate() instanceof Armor))
		{
			return false; // neither weapon nor armor ?
		}
		
		// blacklist check
		if (Arrays.binarySearch(PlayerConfig.AUGMENTATION_BLACKLIST, item.getId()) >= 0)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check if player's conditions valid for augmentation process
	 * @param player
	 * @return
	 */
	protected static boolean isValid(Player player)
	{
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP_IS_IN_OPERATION);
			return false;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_ENGAGED_IN_TRADE_ACTIVITIES);
			return false;
		}
		
		if (player.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_DEAD);
			return false;
		}
		
		if (player.hasBlockActions() && player.hasAbnormalType(AbnormalType.PARALYZE))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_PARALYZED);
			return false;
		}
		
		if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_FISHING);
			return false;
		}
		
		if (player.isSitting())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_SITTING);
			return false;
		}
		
		if (player.isCursedWeaponEquipped())
		{
			return false;
		}
		
		if (player.hasRequest(EnchantItemRequest.class, EnchantItemAttributeRequest.class) || player.isProcessingTransaction())
		{
			return false;
		}
		
		return true;
	}
}
