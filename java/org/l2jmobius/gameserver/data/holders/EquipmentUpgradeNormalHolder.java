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
package org.l2jmobius.gameserver.data.holders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.model.item.enums.UpgradeDataType;
import org.l2jmobius.gameserver.model.item.holders.ItemEnchantHolder;

public class EquipmentUpgradeNormalHolder
{
	private final int _id;
	private final int _type;
	private final long _commission;
	private final double _chance;
	private final ItemEnchantHolder _initialItem;
	private final double _chanceToReceiveBonusItems;
	private final Map<UpgradeDataType, List<ItemEnchantHolder>> _items = new HashMap<>();
	
	/**
	 * @implNote Holder for "UpgradeNormal" equipment system; <list>
	 *           <li>Final Holder will be have getter getItems which get UpgradeDataType;</li>
	 *           <li>Don't forget to check in isHasCategory category type in getItems, for don`t get null or empty collections;</li> </list>
	 * @param id Upgrade ID in DAT file; (yep, duplication);
	 * @param type Upgrade type in DAT file (1 / 2 (used in classic);
	 * @param commission Default Adena count, needed for make "Transformation";
	 * @param chance Success chance of made "Transformation";
	 * @param initialItem Item for upgrade; (cannot be empty)
	 * @param materialItems Materials for upgrade; (can be empty)
	 * @param onSuccessItems Items, which player gets if RND will be < chance (if win);
	 * @param onFailureItems Items, which player gets if RND will be > chance (if lose);
	 * @param chanceToReceiveBonusItems Chance to obtain additional reward on Success (if win);
	 * @param bonusItems Bonus Items;
	 */
	public EquipmentUpgradeNormalHolder(int id, int type, long commission, double chance, ItemEnchantHolder initialItem, List<ItemEnchantHolder> materialItems, List<ItemEnchantHolder> onSuccessItems, List<ItemEnchantHolder> onFailureItems, double chanceToReceiveBonusItems, List<ItemEnchantHolder> bonusItems)
	{
		_id = id;
		_type = type;
		_commission = commission;
		_chance = chance;
		_initialItem = initialItem;
		_chanceToReceiveBonusItems = chanceToReceiveBonusItems;
		if (materialItems != null)
		{
			_items.put(UpgradeDataType.MATERIAL, materialItems);
		}
		
		_items.put(UpgradeDataType.ON_SUCCESS, onSuccessItems);
		if (onFailureItems != null)
		{
			_items.put(UpgradeDataType.ON_FAILURE, onFailureItems);
		}
		
		if (bonusItems != null)
		{
			_items.put(UpgradeDataType.BONUS_TYPE, bonusItems);
		}
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getType()
	{
		return _type;
	}
	
	public long getCommission()
	{
		return _commission;
	}
	
	public double getChance()
	{
		return _chance;
	}
	
	public ItemEnchantHolder getInitialItem()
	{
		return _initialItem;
	}
	
	public double getChanceToReceiveBonusItems()
	{
		return _chanceToReceiveBonusItems;
	}
	
	public List<ItemEnchantHolder> getItems(UpgradeDataType upgradeDataType)
	{
		return _items.get(upgradeDataType);
	}
	
	public boolean isHasCategory(UpgradeDataType upgradeDataType)
	{
		if (_items.isEmpty())
		{
			return false;
		}
		
		return _items.containsKey(upgradeDataType);
	}
}
