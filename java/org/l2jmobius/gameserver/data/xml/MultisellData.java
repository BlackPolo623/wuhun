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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.MultisellEntryHolder;
import org.l2jmobius.gameserver.data.holders.MultisellListHolder;
import org.l2jmobius.gameserver.data.holders.PreparedMultisellListHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enchant.EnchantItemGroup;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.enums.SpecialItemType;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.serverpackets.MultiSellList;

public class MultisellData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(MultisellData.class.getName());
	
	public static final int PAGE_SIZE = 40;
	
	private final Map<Integer, MultisellListHolder> _multisells = new ConcurrentHashMap<>();
	
	protected MultisellData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_multisells.clear();
		parseDatapackDirectory("data/multisell", false);
		parseDatapackDirectory("data/multisell/items", false);
		if (GeneralConfig.CUSTOM_MULTISELL_LOAD)
		{
			parseDatapackDirectory("data/multisell/custom", false);
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _multisells.size() + " multisell lists.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		final EnchantItemGroup magicWeaponGroup = EnchantItemGroupsData.getInstance().getItemGroup("MAGE_WEAPON_GROUP");
		final int magicWeaponGroupMax = magicWeaponGroup != null ? magicWeaponGroup.getMaximumEnchant() : -2;
		final EnchantItemGroup weapongroup = EnchantItemGroupsData.getInstance().getItemGroup("FIGHTER_WEAPON_GROUP");
		final int weaponGroupMax = weapongroup != null ? weapongroup.getMaximumEnchant() : -2;
		final EnchantItemGroup fullArmorGroup = EnchantItemGroupsData.getInstance().getItemGroup("FULL_ARMOR_GROUP");
		final int fullArmorGroupMax = fullArmorGroup != null ? fullArmorGroup.getMaximumEnchant() : -2;
		final EnchantItemGroup armorGroup = EnchantItemGroupsData.getInstance().getItemGroup("ARMOR_GROUP");
		final int armorGroupMax = armorGroup != null ? armorGroup.getMaximumEnchant() : -2;
		
		try
		{
			forEach(document, "list", listNode ->
			{
				final StatSet set = new StatSet(parseAttributes(listNode));
				final int listId = Integer.parseInt(file.getName().substring(0, file.getName().length() - 4));
				final List<MultisellEntryHolder> entries = new ArrayList<>(listNode.getChildNodes().getLength());
				final AtomicInteger entryCounter = new AtomicInteger();
				
				forEach(listNode, itemNode ->
				{
					if ("item".equalsIgnoreCase(itemNode.getNodeName()))
					{
						long totalPrice = 0;
						int lastIngredientId = 0;
						long lastIngredientCount = 0;
						entryCounter.incrementAndGet();
						
						final List<ItemChanceHolder> ingredients = new ArrayList<>(1);
						final List<ItemChanceHolder> products = new ArrayList<>(1);
						final MultisellEntryHolder entry = new MultisellEntryHolder(ingredients, products);
						for (Node d = itemNode.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if ("ingredient".equalsIgnoreCase(d.getNodeName()))
							{
								final int id = parseInteger(d.getAttributes(), "id");
								final long count = parseLong(d.getAttributes(), "count");
								final byte enchantmentLevel = parseByte(d.getAttributes(), "enchantmentLevel", (byte) 0);
								final Boolean maintainIngredient = parseBoolean(d.getAttributes(), "maintainIngredient", false);
								final ItemChanceHolder ingredient = new ItemChanceHolder(id, 0, count, enchantmentLevel, maintainIngredient);
								if (itemExists(ingredient))
								{
									ingredients.add(ingredient);
									
									lastIngredientId = id;
									lastIngredientCount = count;
								}
								else
								{
									LOGGER.warning("Invalid ingredient id or count for itemId: " + ingredient.getId() + ", count: " + ingredient.getCount() + " in list: " + listId);
									continue;
								}
							}
							else if ("production".equalsIgnoreCase(d.getNodeName()))
							{
								final int id = parseInteger(d.getAttributes(), "id");
								final long count = parseLong(d.getAttributes(), "count");
								final double chance = parseDouble(d.getAttributes(), "chance", Double.NaN);
								byte enchantmentLevel = parseByte(d.getAttributes(), "enchantmentLevel", (byte) 0);
								if (enchantmentLevel > 0)
								{
									final ItemTemplate item = ItemData.getInstance().getTemplate(id);
									if (item != null)
									{
										if (item.isWeapon())
										{
											enchantmentLevel = (byte) Math.min(enchantmentLevel, item.isMagicWeapon() ? magicWeaponGroupMax > -2 ? magicWeaponGroupMax : enchantmentLevel : weaponGroupMax > -2 ? weaponGroupMax : enchantmentLevel);
										}
										else if (item.isArmor())
										{
											enchantmentLevel = (byte) Math.min(enchantmentLevel, item.getBodyPart() == BodyPart.FULL_ARMOR ? fullArmorGroupMax > -2 ? fullArmorGroupMax : enchantmentLevel : armorGroupMax > -2 ? armorGroupMax : enchantmentLevel);
										}
									}
								}
								
								final ItemChanceHolder product = new ItemChanceHolder(id, chance, count, enchantmentLevel);
								if (itemExists(product))
								{
									// Check chance only of items that have set chance. Items without chance (NaN) are used for displaying purposes.
									if ((!Double.isNaN(chance) && (chance < 0)) || (chance > 100))
									{
										LOGGER.warning("Invalid chance for itemId: " + product.getId() + ", count: " + product.getCount() + ", chance: " + chance + " in list: " + listId);
										continue;
									}
									
									products.add(product);
									
									final ItemTemplate item = ItemData.getInstance().getTemplate(id);
									if (item != null)
									{
										if (chance > 0)
										{
											totalPrice += ((item.getReferencePrice() / 2) * count) * (chance / 100);
										}
										else
										{
											totalPrice += ((item.getReferencePrice() / 2) * count);
										}
									}
								}
								else
								{
									LOGGER.warning("Invalid product id or count for itemId: " + product.getId() + ", count: " + product.getCount() + " in list: " + listId);
									continue;
								}
							}
						}
						
						final double totalChance = products.stream().filter(i -> !Double.isNaN(i.getChance())).mapToDouble(ItemChanceHolder::getChance).sum();
						if (totalChance > 100)
						{
							LOGGER.warning("Products' total chance of " + totalChance + "% exceeds 100% for list: " + listId + " at entry " + entries.size() + 1 + ".");
						}
						
						// Check if buy price is lower than sell price.
						// Only applies when there is only one ingredient and it is adena.
						if (GeneralConfig.CORRECT_PRICES && (ingredients.size() == 1) && (lastIngredientId == 57) && (lastIngredientCount < totalPrice))
						{
							LOGGER.warning("Buy price " + lastIngredientCount + " is less than sell price " + totalPrice + " at entry " + entryCounter.intValue() + " of multisell " + listId + ".");
							
							// Adjust price.
							final ItemChanceHolder ingredient = new ItemChanceHolder(57, 0, totalPrice, (byte) 0, ingredients.get(0).isMaintainIngredient());
							ingredients.clear();
							ingredients.add(ingredient);
						}
						
						entries.add(entry);
					}
					else if ("npcs".equalsIgnoreCase(itemNode.getNodeName()))
					{
						// Initialize NPCs with the size of child nodes.
						final Set<Integer> allowNpc = new HashSet<>(itemNode.getChildNodes().getLength());
						forEach(itemNode, n -> "npc".equalsIgnoreCase(n.getNodeName()), n -> allowNpc.add(Integer.parseInt(n.getTextContent())));
						
						// Add npcs to stats set.
						set.set("allowNpc", allowNpc);
					}
				});
				
				set.set("listId", listId);
				set.set("entries", entries);
				
				if (_multisells.put(listId, new MultisellListHolder(set)) != null)
				{
					LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Duplicate multisell with id: " + listId);
				}
			});
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Error in file " + file, e);
		}
	}
	
	@Override
	public boolean isValidXmlFile(File file)
	{
		return (file != null) && file.isFile() && file.getName().toLowerCase().matches("\\d+\\.xml");
	}
	
	/**
	 * Generates and sends the multisell list to the player. The multisell list can be customized based on various parameters:
	 * <ol>
	 * <li><b>Inventory only:</b> Determines if only items with primary ingredients (weapons or armor) in the player's inventory are shown.</li>
	 * <li><b>Maintain enchantment:</b> If set to true, the product retains the enchantment level of the ingredient.</li>
	 * <li><b>Apply taxes:</b> Includes a tax in the ingredient list, specified by the "taxIngredient" entry.</li>
	 * <li><b>Product and ingredient multipliers:</b> Additional multipliers can be applied to the product and ingredient quantities.</li>
	 * </ol>
	 * @param listId the ID of the multisell list to be generated.
	 * @param player the player requesting the multisell list.
	 * @param npc the NPC through which the multisell is accessed.
	 * @param inventoryOnly if true, only shows items for which the primary ingredients are in the player's inventory.
	 * @param ingredientMultiplierValue multiplier applied to the ingredients in the multisell list.
	 * @param productMultiplierValue multiplier applied to the products in the multisell list.
	 * @param type the type of multisell list.
	 */
	public void separateAndSend(int listId, Player player, Npc npc, boolean inventoryOnly, double ingredientMultiplierValue, double productMultiplierValue, int type)
	{
		final MultisellListHolder template = _multisells.get(listId);
		if (template == null)
		{
			LOGGER.warning("Can't find list id: " + listId + " requested by player: " + player.getName() + ", npcId: " + (npc != null ? npc.getId() : 0));
			return;
		}
		
		if (!template.isNpcAllowed(-1) && ((npc == null) || !template.isNpcAllowed(npc.getId())))
		{
			if (player.isGM())
			{
				player.sendMessage("Multisell " + listId + " is restricted. Under current conditions cannot be used. Only GMs are allowed to use it.");
			}
			else
			{
				LOGGER.warning(getClass().getSimpleName() + ": " + player + " attempted to open multisell " + listId + " from npc " + npc + " which is not allowed!");
				return;
			}
		}
		
		// Check if ingredient/product multipliers are set, if not, set them to the template value.
		final double ingredientMultiplier = (Double.isNaN(ingredientMultiplierValue) ? template.getIngredientMultiplier() : ingredientMultiplierValue);
		final double productMultiplier = (Double.isNaN(productMultiplierValue) ? template.getProductMultiplier() : productMultiplierValue);
		final PreparedMultisellListHolder list = new PreparedMultisellListHolder(template, inventoryOnly, player.getInventory(), npc, ingredientMultiplier, productMultiplier);
		int index = 0;
		do
		{
			// Send list at least once even if size = 0.
			player.sendPacket(new MultiSellList(player, list, index, type));
			index += PAGE_SIZE;
		}
		while (index < list.getEntries().size());
		
		player.setMultiSell(list);
	}
	
	/**
	 * Generates and sends the multisell list to the player using default multipliers and type.
	 * @param listId the ID of the multisell list to be generated.
	 * @param player the player requesting the multisell list.
	 * @param npc the NPC through which the multisell is accessed.
	 * @param inventoryOnly if true, only shows items for which the primary ingredients are in the player's inventory.
	 */
	public void separateAndSend(int listId, Player player, Npc npc, boolean inventoryOnly)
	{
		separateAndSend(listId, player, npc, inventoryOnly, Double.NaN, Double.NaN, 0);
	}
	
	/**
	 * Checks if an item specified by the {@code ItemHolder} exists in the game database. Stackable items must have a count of at least 1, while non-stackable items must have a count of exactly 1.
	 * @param holder the {@code ItemHolder} instance representing the item to check.
	 * @return {@code true} if the item exists and meets the count requirements, {@code false} otherwise.
	 */
	private final boolean itemExists(ItemHolder holder)
	{
		final SpecialItemType specialItem = SpecialItemType.getByClientId(holder.getId());
		if (specialItem != null)
		{
			return true;
		}
		
		final ItemTemplate template = ItemData.getInstance().getTemplate(holder.getId());
		return (template != null) && (template.isStackable() ? (holder.getCount() >= 1) : (holder.getCount() == 1));
	}
	
	/**
	 * Retrieves a {@code MultisellListHolder} by its ID.
	 * @param id the ID of the multisell list to retrieve.
	 * @return the {@code MultisellListHolder} instance for the specified ID, or {@code null} if it does not exist.
	 */
	public MultisellListHolder getMultisell(int id)
	{
		return _multisells.getOrDefault(id, null);
	}
	
	public static MultisellData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MultisellData INSTANCE = new MultisellData();
	}
}
