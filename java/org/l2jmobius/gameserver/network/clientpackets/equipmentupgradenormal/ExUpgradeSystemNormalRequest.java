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
package org.l2jmobius.gameserver.network.clientpackets.equipmentupgradenormal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.holders.EquipmentUpgradeNormalHolder;
import org.l2jmobius.gameserver.data.xml.EquipmentUpgradeNormalData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.enums.UpgradeDataType;
import org.l2jmobius.gameserver.model.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.holders.UniqueItemEnchantHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.equipmentupgradenormal.ExUpgradeSystemNormalResult;

/**
 * @author Index
 */
public class ExUpgradeSystemNormalRequest extends ClientPacket
{
	private int _objectId;
	private int _typeId;
	private int _upgradeId;
	
	private final List<UniqueItemEnchantHolder> _resultItems = new ArrayList<>();
	private final List<UniqueItemEnchantHolder> _bonusItems = new ArrayList<>();
	private final Map<Integer, Long> _discount = new HashMap<>();
	private boolean isNeedToSendUpdate = false;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
		_typeId = readInt();
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
		
		final Item requestedItem = player.getInventory().getItemByObjectId(_objectId);
		if (requestedItem == null)
		{
			player.sendPacket(ExUpgradeSystemNormalResult.FAIL);
			return;
		}
		
		final EquipmentUpgradeNormalHolder upgradeHolder = EquipmentUpgradeNormalData.getInstance().getUpgrade(_upgradeId);
		if ((upgradeHolder == null) || (upgradeHolder.getType() != _typeId))
		{
			player.sendPacket(ExUpgradeSystemNormalResult.FAIL);
			return;
		}
		
		final Inventory inventory = player.getInventory();
		if ((inventory.getItemByItemId(upgradeHolder.getInitialItem().getId()) == null) || (inventory.getInventoryItemCount(upgradeHolder.getInitialItem().getId(), -1) < upgradeHolder.getInitialItem().getCount()))
		{
			player.sendPacket(ExUpgradeSystemNormalResult.FAIL);
			return;
		}
		
		if (upgradeHolder.isHasCategory(UpgradeDataType.MATERIAL))
		{
			for (ItemEnchantHolder material : upgradeHolder.getItems(UpgradeDataType.MATERIAL))
			{
				if (material.getCount() < 0)
				{
					player.sendPacket(ExUpgradeSystemNormalResult.FAIL);
					PacketLogger.warning(getClass().getSimpleName() + ": material -> item -> count in file EquipmentUpgradeNormalData.xml for upgrade id " + upgradeHolder.getId() + " cannot be less than 0! Aborting current request!");
					return;
				}
				
				if (inventory.getInventoryItemCount(material.getId(), material.getEnchantLevel()) < material.getCount())
				{
					player.sendPacket(ExUpgradeSystemNormalResult.FAIL);
					return;
				}
				
				for (ItemHolder discount : EquipmentUpgradeNormalData.getInstance().getDiscount())
				{
					if (discount.getId() == material.getId())
					{
						_discount.put(material.getId(), discount.getCount());
						break;
					}
				}
			}
		}
		
		final long adena = upgradeHolder.getCommission();
		if ((adena > 0) && (inventory.getAdena() < adena))
		{
			player.sendPacket(ExUpgradeSystemNormalResult.FAIL);
			return;
		}
		
		// Get materials.
		player.destroyItem(ItemProcessType.FEE, _objectId, 1, player, true);
		if (upgradeHolder.isHasCategory(UpgradeDataType.MATERIAL))
		{
			for (ItemHolder material : upgradeHolder.getItems(UpgradeDataType.MATERIAL))
			{
				player.destroyItemByItemId(ItemProcessType.FEE, material.getId(), material.getCount() - (_discount.isEmpty() ? 0 : _discount.get(material.getId())), player, true);
			}
		}
		
		if (adena > 0)
		{
			player.reduceAdena(ItemProcessType.FEE, adena, player, true);
		}
		
		if (Rnd.get(100d) < upgradeHolder.getChance())
		{
			for (ItemEnchantHolder successItem : upgradeHolder.getItems(UpgradeDataType.ON_SUCCESS))
			{
				final Item addedSuccessItem = player.addItem(ItemProcessType.REWARD, successItem.getId(), successItem.getCount(), player, true);
				if (successItem.getEnchantLevel() != 0)
				{
					isNeedToSendUpdate = true;
					addedSuccessItem.setEnchantLevel(successItem.getEnchantLevel());
				}
				
				addedSuccessItem.updateDatabase(true);
				_resultItems.add(new UniqueItemEnchantHolder(successItem, addedSuccessItem.getObjectId()));
			}
			
			if (upgradeHolder.isHasCategory(UpgradeDataType.BONUS_TYPE) && (Rnd.get(100d) < upgradeHolder.getChanceToReceiveBonusItems()))
			{
				for (ItemEnchantHolder bonusItem : upgradeHolder.getItems(UpgradeDataType.BONUS_TYPE))
				{
					final Item addedBonusItem = player.addItem(ItemProcessType.REWARD, bonusItem.getId(), bonusItem.getCount(), player, true);
					if (bonusItem.getEnchantLevel() != 0)
					{
						isNeedToSendUpdate = true;
						addedBonusItem.setEnchantLevel(bonusItem.getEnchantLevel());
					}
					
					addedBonusItem.updateDatabase(true);
					_bonusItems.add(new UniqueItemEnchantHolder(bonusItem, addedBonusItem.getObjectId()));
				}
			}
		}
		else
		{
			if (upgradeHolder.isHasCategory(UpgradeDataType.ON_FAILURE))
			{
				for (ItemEnchantHolder failureItem : upgradeHolder.getItems(UpgradeDataType.ON_FAILURE))
				{
					final Item addedFailureItem = player.addItem(ItemProcessType.COMPENSATE, failureItem.getId(), failureItem.getCount(), player, true);
					if (failureItem.getEnchantLevel() != 0)
					{
						isNeedToSendUpdate = true;
						addedFailureItem.setEnchantLevel(failureItem.getEnchantLevel());
					}
					
					addedFailureItem.updateDatabase(true);
					_resultItems.add(new UniqueItemEnchantHolder(failureItem, addedFailureItem.getObjectId()));
				}
			}
			else
			{
				player.sendPacket(ExUpgradeSystemNormalResult.FAIL);
			}
		}
		
		if (isNeedToSendUpdate)
		{
			player.sendItemList(); // for see enchant level in Upgrade UI
		}
		
		// Why need map of item and count? because method "addItem" return item, and if it exists in result will be count of all items, not of obtained.
		player.sendPacket(new ExUpgradeSystemNormalResult(1, _typeId, true, _resultItems, _bonusItems));
	}
}
