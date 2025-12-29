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
package org.l2jmobius.gameserver.network.clientpackets.equipmentupgrade;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.holders.EquipmentUpgradeHolder;
import org.l2jmobius.gameserver.data.xml.EquipmentUpgradeData;
import org.l2jmobius.gameserver.model.ItemInfo;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.model.item.enchant.attribute.AttributeHolder;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.variables.ItemVariables;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExItemAnnounce;
import org.l2jmobius.gameserver.network.serverpackets.equipmentupgrade.ExUpgradeSystemResult;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Mobius
 */
public class RequestUpgradeSystemResult extends ClientPacket
{
	private int _objectId;
	private int _upgradeId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
		_upgradeId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item existingItem = player.getInventory().getItemByObjectId(_objectId);
		if (existingItem == null)
		{
			player.sendPacket(new ExUpgradeSystemResult(0, 0));
			return;
		}
		
		final EquipmentUpgradeHolder upgradeHolder = EquipmentUpgradeData.getInstance().getUpgrade(_upgradeId);
		if (upgradeHolder == null)
		{
			player.sendPacket(new ExUpgradeSystemResult(0, 0));
			return;
		}
		
		for (ItemHolder material : upgradeHolder.getMaterials())
		{
			if (player.getInventory().getInventoryItemCount(material.getId(), -1) < material.getCount())
			{
				player.sendPacket(new ExUpgradeSystemResult(0, 0));
				return;
			}
		}
		
		final long adena = upgradeHolder.getAdena();
		if ((adena > 0) && (player.getAdena() < adena))
		{
			player.sendPacket(new ExUpgradeSystemResult(0, 0));
			return;
		}
		
		if ((existingItem.getTemplate().getId() != upgradeHolder.getRequiredItemId()) || (existingItem.getEnchantLevel() != upgradeHolder.getRequiredItemEnchant()))
		{
			player.sendPacket(new ExUpgradeSystemResult(0, 0));
			return;
		}
		
		// Store old item enchantment info.
		final ItemInfo itemEnchantment = new ItemInfo(existingItem);
		
		// Get materials.
		player.destroyItem(ItemProcessType.FEE, _objectId, 1, player, true);
		for (ItemHolder material : upgradeHolder.getMaterials())
		{
			player.destroyItemByItemId(ItemProcessType.FEE, material.getId(), material.getCount(), player, true);
		}
		
		if (adena > 0)
		{
			player.reduceAdena(ItemProcessType.FEE, adena, player, true);
		}
		
		// Give item.
		final Item addedItem = player.addItem(ItemProcessType.REWARD, upgradeHolder.getResultItemId(), 1, player, true);
		if (upgradeHolder.isAnnounce())
		{
			Broadcast.toAllOnlinePlayers(new ExItemAnnounce(player, addedItem, ExItemAnnounce.UPGRADE));
		}
		
		// Transfer item enchantments.
		if (addedItem.isEquipable())
		{
			addedItem.setAugmentation(itemEnchantment.getAugmentation(), false);
			if (addedItem.isWeapon() && (addedItem.getTemplate().getAttributes() == null))
			{
				if (itemEnchantment.getAttackElementPower() > 0)
				{
					addedItem.setAttribute(new AttributeHolder(AttributeType.findByClientId(itemEnchantment.getAttackElementType()), itemEnchantment.getAttackElementPower()), false);
				}
			}
			else if (addedItem.getTemplate().getAttributes() == null)
			{
				if (itemEnchantment.getAttributeDefence(AttributeType.FIRE) > 0)
				{
					addedItem.setAttribute(new AttributeHolder(AttributeType.FIRE, itemEnchantment.getAttributeDefence(AttributeType.FIRE)), false);
				}
				
				if (itemEnchantment.getAttributeDefence(AttributeType.WATER) > 0)
				{
					addedItem.setAttribute(new AttributeHolder(AttributeType.WATER, itemEnchantment.getAttributeDefence(AttributeType.WATER)), false);
				}
				
				if (itemEnchantment.getAttributeDefence(AttributeType.WIND) > 0)
				{
					addedItem.setAttribute(new AttributeHolder(AttributeType.WIND, itemEnchantment.getAttributeDefence(AttributeType.WIND)), false);
				}
				
				if (itemEnchantment.getAttributeDefence(AttributeType.EARTH) > 0)
				{
					addedItem.setAttribute(new AttributeHolder(AttributeType.EARTH, itemEnchantment.getAttributeDefence(AttributeType.EARTH)), false);
				}
				
				if (itemEnchantment.getAttributeDefence(AttributeType.HOLY) > 0)
				{
					addedItem.setAttribute(new AttributeHolder(AttributeType.HOLY, itemEnchantment.getAttributeDefence(AttributeType.HOLY)), false);
				}
				
				if (itemEnchantment.getAttributeDefence(AttributeType.DARK) > 0)
				{
					addedItem.setAttribute(new AttributeHolder(AttributeType.DARK, itemEnchantment.getAttributeDefence(AttributeType.DARK)), false);
				}
			}
			
			if (itemEnchantment.getSoulCrystalOptions() != null)
			{
				int pos = -1;
				for (EnsoulOption ensoul : itemEnchantment.getSoulCrystalOptions())
				{
					pos++;
					addedItem.addSpecialAbility(ensoul, pos, 1, false);
				}
			}
			
			if (itemEnchantment.getSoulCrystalSpecialOptions() != null)
			{
				for (EnsoulOption ensoul : itemEnchantment.getSoulCrystalSpecialOptions())
				{
					addedItem.addSpecialAbility(ensoul, 0, 2, false);
				}
			}
			
			if (itemEnchantment.getVisualId() > 0)
			{
				final ItemVariables oldVars = existingItem.getVariables();
				final ItemVariables newVars = addedItem.getVariables();
				newVars.set(ItemVariables.VISUAL_ID, oldVars.getInt(ItemVariables.VISUAL_ID, 0));
				newVars.set(ItemVariables.VISUAL_APPEARANCE_STONE_ID, oldVars.getInt(ItemVariables.VISUAL_APPEARANCE_STONE_ID, 0));
				newVars.set(ItemVariables.VISUAL_APPEARANCE_LIFE_TIME, oldVars.getLong(ItemVariables.VISUAL_APPEARANCE_LIFE_TIME, 0));
				newVars.storeMe();
				addedItem.scheduleVisualLifeTime();
			}
		}
		
		// Apply update holder enchant.
		final int enchantLevel = upgradeHolder.getResultItemEnchant();
		if (enchantLevel > 0)
		{
			addedItem.setEnchantLevel(enchantLevel);
		}
		
		// Save item.
		addedItem.updateDatabase(true);
		
		// Send result packet.
		ThreadPool.schedule(() -> player.sendPacket(new ExUpgradeSystemResult(addedItem.getObjectId(), 1)), 500);
		player.sendItemList();
	}
}
