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

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.ElementalItemHolder;
import org.l2jmobius.gameserver.data.xml.ElementalAttributeData;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.actor.request.EnchantItemAttributeRequest;
import org.l2jmobius.gameserver.model.item.enchant.attribute.AttributeHolder;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAttributeEnchantResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Mobius
 */
public class RequestExEnchantItemAttribute extends ClientPacket
{
	private int _objectId;
	private long _count;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
		_count = readLong();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final EnchantItemAttributeRequest request = player.getRequest(EnchantItemAttributeRequest.class);
		if (request == null)
		{
			return;
		}
		
		request.setProcessing(true);
		
		if (_objectId == 0xFFFFFFFF)
		{
			// Player canceled enchant
			player.removeRequest(request.getClass());
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			return;
		}
		
		if (!player.isOnline())
		{
			player.removeRequest(request.getClass());
			return;
		}
		
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ADD_ELEMENTAL_POWER_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.removeRequest(request.getClass());
			return;
		}
		
		// Restrict enchant during a trade (bug if enchant fails)
		if (player.getActiveRequester() != null)
		{
			// Cancel trade
			player.cancelActiveTrade();
			player.removeRequest(request.getClass());
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_TRADING);
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(_objectId);
		final Item stone = request.getEnchantingStone();
		if ((item == null) || (stone == null))
		{
			player.removeRequest(request.getClass());
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			return;
		}
		
		if (!item.isElementable())
		{
			player.sendPacket(SystemMessageId.ELEMENTAL_POWER_ENHANCER_USAGE_REQUIREMENT_IS_NOT_SUFFICIENT);
			player.removeRequest(request.getClass());
			return;
		}
		
		if (_count < 1)
		{
			player.removeRequest(request.getClass());
			return;
		}
		
		switch (item.getItemLocation())
		{
			case INVENTORY:
			case PAPERDOLL:
			{
				if (item.getOwnerId() != player.getObjectId())
				{
					player.removeRequest(request.getClass());
					return;
				}
				break;
			}
			default:
			{
				player.removeRequest(request.getClass());
				PunishmentManager.handleIllegalPlayerAction(player, player + " tried to use enchant Exploit!", GeneralConfig.DEFAULT_PUNISH);
				return;
			}
		}
		
		final int stoneId = stone.getId();
		final long count = Math.min(stone.getCount(), _count);
		AttributeType elementToAdd = ElementalAttributeData.getInstance().getItemElement(stoneId);
		
		// Armors have the opposite element
		if (item.isArmor())
		{
			elementToAdd = elementToAdd.getOpposite();
		}
		
		final AttributeType opositeElement = elementToAdd.getOpposite();
		final AttributeHolder oldElement = item.getAttribute(elementToAdd);
		final int elementValue = oldElement == null ? 0 : oldElement.getValue();
		final int limit = getLimit(item, stoneId);
		int powerToAdd = getPowerToAdd(stoneId, elementValue, item);
		if ((item.isWeapon() && (oldElement != null) && (oldElement.getType() != elementToAdd) && (oldElement.getType() != AttributeType.NONE)) || (item.isArmor() && (item.getAttribute(elementToAdd) == null) && (item.getAttributes() != null) && (item.getAttributes().size() >= 3)))
		{
			player.sendPacket(SystemMessageId.ANOTHER_ELEMENTAL_POWER_HAS_ALREADY_BEEN_ADDED_THIS_ELEMENTAL_POWER_CANNOT_BE_ADDED);
			player.removeRequest(request.getClass());
			return;
		}
		
		if (item.isArmor() && (item.getAttributes() != null))
		{
			// can't add opposite element
			for (AttributeHolder attribute : item.getAttributes())
			{
				if (attribute.getType() == opositeElement)
				{
					player.removeRequest(request.getClass());
					PunishmentManager.handleIllegalPlayerAction(player, player + " tried to add oposite attribute to item!", GeneralConfig.DEFAULT_PUNISH);
					return;
				}
			}
		}
		
		int newPower = elementValue + powerToAdd;
		if (newPower > limit)
		{
			newPower = limit;
			powerToAdd = limit - elementValue;
		}
		
		if (powerToAdd <= 0)
		{
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			player.removeRequest(request.getClass());
			return;
		}
		
		int usedStones = 0;
		int successfulAttempts = 0;
		int failedAttempts = 0;
		for (int i = 0; i < count; i++)
		{
			usedStones++;
			final int result = addElement(player, stone, item, elementToAdd);
			if (result == 1)
			{
				successfulAttempts++;
			}
			else if (result == 0)
			{
				failedAttempts++;
			}
			else
			{
				break;
			}
		}
		
		item.updateItemElementals();
		player.destroyItem(ItemProcessType.FEE, stone, usedStones, player, true);
		final AttributeHolder newElement = item.getAttribute(elementToAdd);
		final int newValue = newElement != null ? newElement.getValue() : 0;
		final AttributeType realElement = item.isArmor() ? opositeElement : elementToAdd;
		final InventoryUpdate iu = new InventoryUpdate();
		if (successfulAttempts > 0)
		{
			SystemMessage sm;
			if (item.getEnchantLevel() == 0)
			{
				if (item.isArmor())
				{
					sm = new SystemMessage(SystemMessageId.S1_ARE_NOW_IMBUED_WITH_S2_ATTRIBUTE_AND_YOUR_S3_RESISTANCE_HAS_INCREASED);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S2_ATTRIBUTE_HAS_BEEN_ADDED_TO_S1);
				}
				
				sm.addItemName(item);
				sm.addAttribute(realElement.getClientId());
				if (item.isArmor())
				{
					sm.addAttribute(realElement.getOpposite().getClientId());
				}
			}
			else
			{
				if (item.isArmor())
				{
					sm = new SystemMessage(SystemMessageId.S3_POWER_HAS_BEEN_ADDED_TO_S1_S2_S4_RESISTANCE_IS_INCREASED);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_S2_S3_ATTRIBUTE_POWER_IS_ENABLED);
				}
				
				sm.addInt(item.getEnchantLevel());
				sm.addItemName(item);
				sm.addAttribute(realElement.getClientId());
				if (item.isArmor())
				{
					sm.addAttribute(realElement.getOpposite().getClientId());
				}
			}
			
			player.sendPacket(sm);
			
			// send packets
			iu.addModifiedItem(item);
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_FAILED_TO_ADD_ELEMENTAL_POWER);
		}
		
		int result = 0;
		if (successfulAttempts == 0)
		{
			// Failed
			result = 2;
		}
		
		// Stone must be removed
		if (stone.getCount() == 0)
		{
			iu.addRemovedItem(stone);
		}
		else
		{
			iu.addModifiedItem(stone);
		}
		
		player.removeRequest(request.getClass());
		player.sendPacket(new ExAttributeEnchantResult(result, item.isWeapon(), elementToAdd, elementValue, newValue, successfulAttempts, failedAttempts));
		player.updateUserInfo();
		player.sendInventoryUpdate(iu);
	}
	
	private int addElement(Player player, Item stone, Item item, AttributeType elementToAdd)
	{
		final AttributeHolder oldElement = item.getAttribute(elementToAdd);
		final int elementValue = oldElement == null ? 0 : oldElement.getValue();
		final int limit = getLimit(item, stone.getId());
		int powerToAdd = getPowerToAdd(stone.getId(), elementValue, item);
		int newPower = elementValue + powerToAdd;
		if (newPower > limit)
		{
			newPower = limit;
			powerToAdd = limit - elementValue;
		}
		
		if (powerToAdd <= 0)
		{
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			player.removeRequest(EnchantItemAttributeRequest.class);
			return -1;
		}
		
		final boolean success = ElementalAttributeData.getInstance().isSuccess(item, stone.getId());
		if (success)
		{
			item.setAttribute(new AttributeHolder(elementToAdd, newPower), false);
		}
		
		return success ? 1 : 0;
	}
	
	public int getLimit(Item item, int sotneId)
	{
		final ElementalItemHolder elementItem = ElementalAttributeData.getInstance().getItemElemental(sotneId);
		if (elementItem == null)
		{
			return 0;
		}
		
		if (item.isWeapon())
		{
			return ElementalAttributeData.WEAPON_VALUES[elementItem.getType().getMaxLevel()];
		}
		
		return ElementalAttributeData.ARMOR_VALUES[elementItem.getType().getMaxLevel()];
	}
	
	public int getPowerToAdd(int stoneId, int oldValue, Item item)
	{
		if (ElementalAttributeData.getInstance().getItemElement(stoneId) != AttributeType.NONE)
		{
			if (ElementalAttributeData.getInstance().getItemElemental(stoneId).getPower() > 0)
			{
				return ElementalAttributeData.getInstance().getItemElemental(stoneId).getPower();
			}
			
			if (item.isWeapon())
			{
				if (oldValue == 0)
				{
					return ElementalAttributeData.FIRST_WEAPON_BONUS;
				}
				
				return ElementalAttributeData.NEXT_WEAPON_BONUS;
			}
			else if (item.isArmor())
			{
				return ElementalAttributeData.ARMOR_BONUS;
			}
		}
		
		return 0;
	}
}
