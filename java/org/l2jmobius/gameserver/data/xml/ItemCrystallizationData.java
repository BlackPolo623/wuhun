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
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.enums.CrystallizationType;
import org.l2jmobius.gameserver.data.holders.CrystallizationDataHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.Armor;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enchant.RewardItemsOnFailure;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.CrystalType;

/**
 * @author UnAfraid, Index
 */
public class ItemCrystallizationData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ItemCrystallizationData.class.getName());
	
	private final Map<CrystalType, Map<CrystallizationType, List<ItemChanceHolder>>> _crystallizationTemplates = new EnumMap<>(CrystalType.class);
	private final Map<Integer, CrystallizationDataHolder> _items = new HashMap<>();
	
	private RewardItemsOnFailure _weaponDestroyGroup = new RewardItemsOnFailure();
	private RewardItemsOnFailure _armorDestroyGroup = new RewardItemsOnFailure();
	
	protected ItemCrystallizationData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_crystallizationTemplates.clear();
		for (CrystalType crystalType : CrystalType.values())
		{
			_crystallizationTemplates.put(crystalType, new EnumMap<>(CrystallizationType.class));
		}
		
		_items.clear();
		
		_weaponDestroyGroup = new RewardItemsOnFailure();
		_armorDestroyGroup = new RewardItemsOnFailure();
		
		parseDatapackFile("data/CrystallizableItems.xml");
		
		if (_crystallizationTemplates.size() > 0)
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _crystallizationTemplates.size() + " crystallization templates.");
		}
		
		if (_items.size() > 0)
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _items.size() + " pre-defined crystallizable items.");
		}
		
		// Generate remaining data.
		generateCrystallizationData();
		
		if (_weaponDestroyGroup.size() > 0)
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _weaponDestroyGroup.size() + " weapon enchant failure rewards.");
		}
		
		if (_armorDestroyGroup.size() > 0)
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _armorDestroyGroup.size() + " armor enchant failure rewards.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node o = n.getFirstChild(); o != null; o = o.getNextSibling())
				{
					if ("templates".equalsIgnoreCase(o.getNodeName()))
					{
						for (Node d = o.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("crystallizable_template".equalsIgnoreCase(d.getNodeName()))
							{
								final CrystalType crystalType = parseEnum(d.getAttributes(), CrystalType.class, "crystalType");
								final CrystallizationType crystallizationType = parseEnum(d.getAttributes(), CrystallizationType.class, "crystallizationType");
								final List<ItemChanceHolder> crystallizeRewards = new ArrayList<>();
								for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if ("item".equalsIgnoreCase(c.getNodeName()))
									{
										final NamedNodeMap attrs = c.getAttributes();
										final int itemId = parseInteger(attrs, "id");
										final long itemCount = parseLong(attrs, "count");
										final double itemChance = parseDouble(attrs, "chance");
										crystallizeRewards.add(new ItemChanceHolder(itemId, itemChance, itemCount));
									}
								}
								
								_crystallizationTemplates.get(crystalType).put(crystallizationType, crystallizeRewards);
							}
						}
					}
					else if ("items".equalsIgnoreCase(o.getNodeName()))
					{
						for (Node d = o.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("crystallizable_item".equalsIgnoreCase(d.getNodeName()))
							{
								final int id = parseInteger(d.getAttributes(), "id");
								final List<ItemChanceHolder> crystallizeRewards = new ArrayList<>();
								for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if ("item".equalsIgnoreCase(c.getNodeName()))
									{
										final NamedNodeMap attrs = c.getAttributes();
										final int itemId = parseInteger(attrs, "id");
										final long itemCount = parseLong(attrs, "count");
										final double itemChance = parseDouble(attrs, "chance");
										crystallizeRewards.add(new ItemChanceHolder(itemId, itemChance, itemCount));
									}
								}
								
								_items.put(id, new CrystallizationDataHolder(id, crystallizeRewards));
							}
						}
					}
					else if ("itemsOnEnchantFailure".equals(o.getNodeName()))
					{
						for (Node d = o.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("armor".equalsIgnoreCase(d.getNodeName()))
							{
								_armorDestroyGroup = getFormedHolder(d);
							}
							else if ("weapon".equalsIgnoreCase(d.getNodeName()))
							{
								_weaponDestroyGroup = getFormedHolder(d);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Creates a RewardItemsOnFailure holder from an XMLStreamReader positioned at an "armor" or "weapon" element.
	 * @param node the Node positioned at the element containing item data.
	 * @return a populated RewardItemsOnFailure instance.
	 */
	private RewardItemsOnFailure getFormedHolder(Node node)
	{
		final RewardItemsOnFailure holder = new RewardItemsOnFailure();
		for (Node z = node.getFirstChild(); z != null; z = z.getNextSibling())
		{
			if ("item".equals(z.getNodeName()))
			{
				final StatSet failItems = new StatSet(parseAttributes(z));
				final int rewardId = failItems.getInt("id");
				final int enchantLevel = failItems.getInt("enchant");
				final double chance = failItems.getDouble("chance");
				
				final String[] targetIds = failItems.getString("targetIds", "").split(",");
				for (String s : targetIds)
				{
					if (!s.isEmpty())
					{
						final int destroyedItemId = Integer.parseInt(s.trim());
						final long count = failItems.getLong("amountR", 1);
						holder.addItemToHolder(destroyedItemId, enchantLevel, rewardId, count, chance);
					}
				}
			}
		}
		
		return holder;
	}
	
	public List<ItemChanceHolder> getRewardItems(Item item, int enchantLevel)
	{
		if (item == null)
		{
			return Collections.emptyList();
		}
		
		final RewardItemsOnFailure holder = item.isWeapon() ? _weaponDestroyGroup : _armorDestroyGroup;
		if (holder == null)
		{
			return Collections.emptyList();
		}
		
		return holder.getRewardItems(item.getId(), enchantLevel);
	}
	
	/**
	 * Retrieves the count of loaded crystallization templates.
	 * @return the number of crystallization templates currently loaded.
	 */
	public int getLoadedCrystallizationTemplateCount()
	{
		return _crystallizationTemplates.size();
	}
	
	/**
	 * Calculates the rewards for crystallizing an item based on its template and a list of crystallization rewards. Adjusts the reward count and chance based on the item's crystal count.
	 * @param item the {@link ItemTemplate} representing the item being crystallized.
	 * @param crystallizeRewards a list of {@link ItemChanceHolder} defining the base rewards for crystallization.
	 * @return a list of adjusted {@link ItemChanceHolder} rewards for the item, or {@code null} if no rewards are provided.
	 */
	private List<ItemChanceHolder> calculateCrystallizeRewards(ItemTemplate item, List<ItemChanceHolder> crystallizeRewards)
	{
		if (crystallizeRewards == null)
		{
			return null;
		}
		
		final List<ItemChanceHolder> rewards = new ArrayList<>();
		for (ItemChanceHolder reward : crystallizeRewards)
		{
			double chance = reward.getChance() * item.getCrystalCount();
			long count = reward.getCount();
			if (chance > 100.)
			{
				final double countMul = Math.ceil(chance / 100.);
				chance /= countMul;
				count *= countMul;
			}
			
			rewards.add(new ItemChanceHolder(reward.getId(), chance, count));
		}
		
		return rewards;
	}
	
	/**
	 * Generates crystallization data for all items in the game, identifying crystallizable items and calculating their crystallization rewards if they have not been generated previously.
	 */
	private void generateCrystallizationData()
	{
		final int previousCount = _items.size();
		for (ItemTemplate item : ItemData.getInstance().getAllItems())
		{
			// Check if the data has not been generated.
			if (((item instanceof Weapon) || (item instanceof Armor)) && item.isCrystallizable() && !_items.containsKey(item.getId()))
			{
				final List<ItemChanceHolder> holder = _crystallizationTemplates.get(item.getCrystalType()).get((item instanceof Weapon) ? CrystallizationType.WEAPON : CrystallizationType.ARMOR);
				if (holder != null)
				{
					_items.put(item.getId(), new CrystallizationDataHolder(item.getId(), calculateCrystallizeRewards(item, holder)));
				}
			}
		}
		
		final int generated = _items.size() - previousCount;
		if (generated > 0)
		{
			LOGGER.info(getClass().getSimpleName() + ": Generated " + generated + " crystallizable items from templates.");
		}
	}
	
	/**
	 * Retrieves the crystallization template for a specified crystal type and crystallization type.
	 * @param crystalType the {@link CrystalType} of the template.
	 * @param crystallizationType the {@link CrystallizationType} indicating if the template is for weapons or armor.
	 * @return a list of {@link ItemChanceHolder} defining the crystallization rewards for the specified types, or {@code null} if none exist.
	 */
	public List<ItemChanceHolder> getCrystallizationTemplate(CrystalType crystalType, CrystallizationType crystallizationType)
	{
		return _crystallizationTemplates.get(crystalType).get(crystallizationType);
	}
	
	/**
	 * Retrieves crystallization data for a specific item by ID. The crystallization data provides the rewards for crystallizing an unenchanted version of the item.
	 * @param itemId the ID of the item to get crystallization data for.
	 * @return the {@link CrystallizationDataHolder} containing the crystallization rewards, or {@code null} if not available.
	 */
	public CrystallizationDataHolder getCrystallizationData(int itemId)
	{
		return _items.get(itemId);
	}
	
	/**
	 * Calculates the crystallization rewards for a specified item, considering its crystal count. If no crystallization data is available for the item, a basic crystal reward is provided.
	 * @param item the {@link Item} to calculate crystallization rewards for.
	 * @return a list of {@link ItemChanceHolder} representing the crystallization rewards.
	 */
	public List<ItemChanceHolder> getCrystallizationRewards(Item item)
	{
		final List<ItemChanceHolder> result = new ArrayList<>();
		final int crystalItemId = item.getTemplate().getCrystalItemId();
		final CrystallizationDataHolder data = getCrystallizationData(item.getId());
		if (data != null)
		{
			// If there are no crystals on the template, add such.
			boolean found = false;
			final List<ItemChanceHolder> items = data.getItems();
			for (ItemChanceHolder holder : items)
			{
				if (holder.getId() == crystalItemId)
				{
					found = true;
					break;
				}
			}
			
			if (!found)
			{
				result.add(new ItemChanceHolder(crystalItemId, 100, item.getCrystalCount()));
			}
			
			result.addAll(items);
		}
		else
		{
			// Add basic crystal reward.
			result.add(new ItemChanceHolder(crystalItemId, 100, item.getCrystalCount()));
		}
		
		return result;
	}
	
	/**
	 * Retrieves the item reward that the player will receive upon destroying an item, based on the item's type, crystal grade, and enchant level. This method checks if a reward is available for the specified item's properties.
	 * @param player the {@link Player} attempting to destroy the item.
	 * @param item the {@link Item} being destroyed.
	 * @return an {@link ItemChanceHolder} representing the item reward on destruction, or {@code null} if no reward is available.
	 */
	public List<ItemChanceHolder> getItemOnDestroy(Player player, Item item)
	{
		if ((player == null) || (item == null))
		{
			return Collections.emptyList();
		}
		
		final RewardItemsOnFailure holder = item.isWeapon() ? _weaponDestroyGroup : _armorDestroyGroup;
		final int destroyedItemId = item.getId();
		if (holder.getRewardItems(destroyedItemId, item.getEnchantLevel()).isEmpty())
		{
			return Collections.emptyList();
		}
		
		return holder.getRewardItems(destroyedItemId, item.getEnchantLevel());
	}
	
	public static ItemCrystallizationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ItemCrystallizationData INSTANCE = new ItemCrystallizationData();
	}
}
