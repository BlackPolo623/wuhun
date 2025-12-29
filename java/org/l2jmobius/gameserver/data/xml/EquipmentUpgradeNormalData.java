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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.EquipmentUpgradeNormalHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.serverpackets.equipmentupgradenormal.ExUpgradeSystemNormalResult;

/**
 * @author Index
 */
public class EquipmentUpgradeNormalData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EquipmentUpgradeNormalData.class.getName());
	
	private final Map<Integer, EquipmentUpgradeNormalHolder> _upgrades = new HashMap<>();
	private final Set<ItemHolder> _discount = new HashSet<>();
	private int _commission;
	
	protected EquipmentUpgradeNormalData()
	{
		load();
	}
	
	public void reload()
	{
		for (Player player : World.getInstance().getPlayers())
		{
			player.sendPacket(ExUpgradeSystemNormalResult.FAIL);
		}
		
		load();
	}
	
	@Override
	public void load()
	{
		_commission = -1;
		_discount.clear();
		_upgrades.clear();
		parseDatapackFile("data/EquipmentUpgradeNormalData.xml");
		
		if (!_upgrades.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _upgrades.size() + " upgrade-normal equipment data. Adena commission is " + _commission + ".");
		}
		
		if (!_discount.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _discount.size() + " upgrade-normal discount data.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "params", paramNode -> _commission = new StatSet(parseAttributes(paramNode)).getInt("commission")));
		if (_commission < 0)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Commission in file EquipmentUpgradeNormalData.xml not set or less than 0! Setting up default value - 100!");
			_commission = 100;
		}
		
		forEach(document, "list", listNode -> forEach(listNode, "discount", discountNode -> forEach(discountNode, "item", itemNode ->
		{
			final StatSet successSet = new StatSet(parseAttributes(itemNode));
			_discount.add(new ItemHolder(successSet.getInt("id"), successSet.getLong("count")));
		})));
		forEach(document, "list", listNode -> forEach(listNode, "upgrade", upgradeNode ->
		{
			final AtomicReference<ItemEnchantHolder> initialItem = new AtomicReference<>();
			final AtomicReference<List<ItemEnchantHolder>> materialItems = new AtomicReference<>(new ArrayList<>());
			final AtomicReference<List<ItemEnchantHolder>> onSuccessItems = new AtomicReference<>(new ArrayList<>());
			final AtomicReference<List<ItemEnchantHolder>> onFailureItems = new AtomicReference<>(new ArrayList<>());
			final AtomicReference<List<ItemEnchantHolder>> bonusItems = new AtomicReference<>(new ArrayList<>());
			final AtomicReference<Double> bonusChance = new AtomicReference<>();
			final StatSet headerSet = new StatSet(parseAttributes(upgradeNode));
			final int id = headerSet.getInt("id");
			final int type = headerSet.getInt("type");
			final double chance = headerSet.getDouble("chance");
			final long commission = _commission == 0 ? 0 : ((headerSet.getLong("commission") / 100) * _commission);
			forEach(upgradeNode, "upgradeItem", upgradeItemNode ->
			{
				final StatSet initialSet = new StatSet(parseAttributes(upgradeItemNode));
				initialItem.set(new ItemEnchantHolder(initialSet.getInt("id"), initialSet.getLong("count"), initialSet.getByte("enchantLevel")));
				if (initialItem.get() == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": upgradeItem in file EquipmentUpgradeNormalData.xml for upgrade id " + id + " seems like broken!");
				}
				
				if (initialItem.get().getCount() < 0)
				{
					LOGGER.warning(getClass().getSimpleName() + ": upgradeItem -> item -> count in file EquipmentUpgradeNormalData.xml for upgrade id " + id + " cannot be less than 0!");
				}
			});
			forEach(upgradeNode, "material", materialItemNode -> forEach(materialItemNode, "item", itemNode ->
			{
				final StatSet successSet = new StatSet(parseAttributes(itemNode));
				materialItems.get().add(new ItemEnchantHolder(successSet.getInt("id"), successSet.getLong("count"), successSet.getInt("enchantLevel")));
				
				// {
				// LOGGER.warning(getClass().getSimpleName() + ": material -> item -> count in file EquipmentUpgradeNormalData.xml for upgrade id " + id +" cannot be less than 0!");
				// }
			}));
			forEach(upgradeNode, "successItems", successItemNode -> forEach(successItemNode, "item", itemNode ->
			{
				final StatSet successSet = new StatSet(parseAttributes(itemNode));
				onSuccessItems.get().add(new ItemEnchantHolder(successSet.getInt("id"), successSet.getLong("count"), successSet.getInt("enchantLevel")));
			}));
			forEach(upgradeNode, "failureItems", failureItemNode -> forEach(failureItemNode, "item", itemNode ->
			{
				final StatSet successSet = new StatSet(parseAttributes(itemNode));
				onFailureItems.get().add(new ItemEnchantHolder(successSet.getInt("id"), successSet.getLong("count"), successSet.getInt("enchantLevel")));
			}));
			forEach(upgradeNode, "bonus_items", bonusItemNode ->
			{
				bonusChance.set(new StatSet(parseAttributes(bonusItemNode)).getDouble("chance"));
				if (bonusChance.get() < 0)
				{
					LOGGER.warning(getClass().getSimpleName() + ": bonus_items -> chance in file EquipmentUpgradeNormalData.xml for upgrade id " + id + " cannot be less than 0!");
				}
				
				forEach(bonusItemNode, "item", itemNode ->
				{
					final StatSet successSet = new StatSet(parseAttributes(itemNode));
					bonusItems.get().add(new ItemEnchantHolder(successSet.getInt("id"), successSet.getLong("count"), successSet.getInt("enchantLevel")));
				});
			});
			_upgrades.put(id, new EquipmentUpgradeNormalHolder(id, type, commission, chance, initialItem.get(), materialItems.get(), onSuccessItems.get(), onFailureItems.get(), bonusChance.get() == null ? 0 : bonusChance.get(), bonusItems.get()));
		}));
	}
	
	/**
	 * Retrieves the {@link EquipmentUpgradeNormalHolder} associated with the specified upgrade ID.
	 * @param id the unique identifier of the upgrade to retrieve
	 * @return the {@link EquipmentUpgradeNormalHolder} for the specified ID, or {@code null} if no upgrade exists with that ID
	 */
	public EquipmentUpgradeNormalHolder getUpgrade(int id)
	{
		return _upgrades.get(id);
	}
	
	/**
	 * Returns a set of items that are available at a discount.
	 * @return a {@link Set} of {@link ItemHolder} objects representing items with a discount
	 */
	public Set<ItemHolder> getDiscount()
	{
		return _discount;
	}
	
	/**
	 * Retrieves the commission rate applied to upgrades.
	 * @return the commission rate as an integer
	 */
	public int getCommission()
	{
		return _commission;
	}
	
	public static EquipmentUpgradeNormalData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EquipmentUpgradeNormalData INSTANCE = new EquipmentUpgradeNormalData();
	}
}
