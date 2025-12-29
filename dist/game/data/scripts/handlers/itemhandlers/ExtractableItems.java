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
package handlers.itemhandlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.managers.DailyResetManager;
import org.l2jmobius.gameserver.model.ExtractableProduct;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.AutoPeelRequest;
import org.l2jmobius.gameserver.model.item.EtcItem;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.enums.SpecialItemType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.autopeel.ExResultItemAutoPeel;
import org.l2jmobius.gameserver.network.serverpackets.autopeel.ExStopItemAutoPeel;

/**
 * Extractable Items handler.
 * @author HorridoJoho, Mobius
 */
public class ExtractableItems implements IItemHandler
{
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
			return false;
		}
		
		final Player player = playable.asPlayer();
		final EtcItem etcitem = (EtcItem) item.getTemplate();
		final List<ExtractableProduct> exitems = etcitem.getExtractableItems();
		if (exitems == null)
		{
			LOGGER.info("No extractable data defined for " + etcitem);
			return false;
		}
		
		if (!player.isInventoryUnder80(false))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_SPACE_IN_INVENTORY_UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_IS_LESS_THAN_80_AND_SLOT_COUNT_IS_LESS_THAN_90_OF_CAPACITY);
			return false;
		}
		
		// destroy item
		if (!DailyResetManager.RESET_ITEMS.contains(item.getId()) && !player.destroyItem(ItemProcessType.FEE, item.getObjectId(), 1, player, true))
		{
			return false;
		}
		
		boolean specialReward = false;
		final Map<Item, Long> extractedItems = new HashMap<>();
		final List<Item> enchantedItems = new ArrayList<>();
		if (etcitem.getExtractableCountMin() > 0)
		{
			while (extractedItems.size() < etcitem.getExtractableCountMin())
			{
				for (ExtractableProduct expi : exitems)
				{
					if ((etcitem.getExtractableCountMax() > 0) && (extractedItems.size() == etcitem.getExtractableCountMax()))
					{
						break;
					}
					
					if (Rnd.get(100000) <= expi.getChance())
					{
						final long min = (long) (expi.getMin() * RatesConfig.RATE_EXTRACTABLE);
						final long max = (long) (expi.getMax() * RatesConfig.RATE_EXTRACTABLE);
						long createItemAmount = (max == min) ? min : (Rnd.get((max - min) + 1) + min);
						if (createItemAmount == 0)
						{
							continue;
						}
						
						// Do not extract the same item.
						boolean alreadyExtracted = false;
						for (Item i : extractedItems.keySet())
						{
							if (i.getTemplate().getId() == expi.getId())
							{
								alreadyExtracted = true;
								break;
							}
						}
						
						if (alreadyExtracted && (exitems.size() >= etcitem.getExtractableCountMax()))
						{
							continue;
						}
						
						if (expi.getId() == -1) // Prime points
						{
							player.setPrimePoints(player.getPrimePoints() + (int) createItemAmount);
							player.sendMessage("You have obtained " + (createItemAmount / 100) + " Euro!");
							specialReward = true;
							continue;
						}
						else if (expi.getId() == SpecialItemType.PC_CAFE_POINTS.getClientId())
						{
							final int currentPoints = player.getPcCafePoints();
							final int upgradePoints = player.getPcCafePoints() + (int) createItemAmount;
							player.setPcCafePoints(upgradePoints);
							final SystemMessage message = new SystemMessage(SystemMessageId.YOU_HAVE_RECEIVED_S1_POINT_S_AS_A_DAILY_REWARD_FOR_USING_BLESSING_OF_ABUNDANCE).addInt((int) createItemAmount);
							player.sendPacket(message);
							player.sendPacket(new ExPCCafePointInfo(currentPoints, upgradePoints, 1));
							specialReward = true;
							continue;
						}
						else if (expi.getId() == SpecialItemType.HONOR_COINS.getClientId())
						{
							player.setHonorCoins(player.getHonorCoins() + (int) createItemAmount);
							player.sendMessage("You have obtained " + (createItemAmount) + " Honor Coin.");
							specialReward = true;
							continue;
						}
						
						final ItemTemplate template = ItemData.getInstance().getTemplate(expi.getId());
						if (template == null)
						{
							LOGGER.warning("ExtractableItems: Could not find " + item + " product template with id " + expi.getId() + "!");
							continue;
						}
						
						if (template.isStackable() || (createItemAmount == 1))
						{
							final Item newItem = player.addItem(ItemProcessType.REWARD, expi.getId(), createItemAmount, player, false);
							if (expi.getMaxEnchant() > 0)
							{
								newItem.setEnchantLevel(Rnd.get(expi.getMinEnchant(), expi.getMaxEnchant()));
								enchantedItems.add(newItem);
							}
							
							addItem(extractedItems, newItem, createItemAmount);
						}
						else
						{
							while (createItemAmount > 0)
							{
								final Item newItem = player.addItem(ItemProcessType.REWARD, expi.getId(), 1, player, false);
								if (expi.getMaxEnchant() > 0)
								{
									newItem.setEnchantLevel(Rnd.get(expi.getMinEnchant(), expi.getMaxEnchant()));
									enchantedItems.add(newItem);
								}
								
								addItem(extractedItems, newItem, 1);
								createItemAmount--;
							}
						}
					}
				}
			}
		}
		else
		{
			for (ExtractableProduct expi : exitems)
			{
				if ((etcitem.getExtractableCountMax() > 0) && (extractedItems.size() == etcitem.getExtractableCountMax()))
				{
					break;
				}
				
				if (Rnd.get(100000) <= expi.getChance())
				{
					final long min = (long) (expi.getMin() * RatesConfig.RATE_EXTRACTABLE);
					final long max = (long) (expi.getMax() * RatesConfig.RATE_EXTRACTABLE);
					long createItemAmount = (max == min) ? min : (Rnd.get((max - min) + 1) + min);
					if (createItemAmount == 0)
					{
						continue;
					}
					
					if (expi.getId() == -1) // Prime points
					{
						player.setPrimePoints(player.getPrimePoints() + (int) createItemAmount);
						player.sendMessage("You have obtained " + (createItemAmount / 100) + " Euro!");
						specialReward = true;
						continue;
					}
					else if (expi.getId() == SpecialItemType.PC_CAFE_POINTS.getClientId())
					{
						final int currentPoints = player.getPcCafePoints();
						final int upgradePoints = player.getPcCafePoints() + (int) createItemAmount;
						player.setPcCafePoints(upgradePoints);
						final SystemMessage message = new SystemMessage(SystemMessageId.YOU_HAVE_RECEIVED_S1_POINT_S_AS_A_DAILY_REWARD_FOR_USING_BLESSING_OF_ABUNDANCE).addInt((int) createItemAmount);
						player.sendPacket(message);
						player.sendPacket(new ExPCCafePointInfo(currentPoints, upgradePoints, 1));
						specialReward = true;
						continue;
					}
					else if (expi.getId() == SpecialItemType.HONOR_COINS.getClientId())
					{
						player.setHonorCoins(player.getHonorCoins() + (int) createItemAmount);
						player.sendMessage("You have obtained " + (createItemAmount) + " Honor Coin.");
						specialReward = true;
						continue;
					}
					
					final ItemTemplate template = ItemData.getInstance().getTemplate(expi.getId());
					if (template == null)
					{
						LOGGER.warning("ExtractableItems: Could not find " + item + " product template with id " + expi.getId() + "!");
						continue;
					}
					
					if (template.isStackable() || (createItemAmount == 1))
					{
						final Item newItem = player.addItem(ItemProcessType.REWARD, expi.getId(), createItemAmount, player, false);
						if (expi.getMaxEnchant() > 0)
						{
							newItem.setEnchantLevel(Rnd.get(expi.getMinEnchant(), expi.getMaxEnchant()));
							enchantedItems.add(newItem);
						}
						
						addItem(extractedItems, newItem, createItemAmount);
					}
					else
					{
						while (createItemAmount > 0)
						{
							final Item newItem = player.addItem(ItemProcessType.REWARD, expi.getId(), 1, player, false);
							if (expi.getMaxEnchant() > 0)
							{
								newItem.setEnchantLevel(Rnd.get(expi.getMinEnchant(), expi.getMaxEnchant()));
								enchantedItems.add(newItem);
							}
							
							addItem(extractedItems, newItem, 1);
							createItemAmount--;
						}
					}
				}
			}
		}
		
		if (extractedItems.isEmpty() && !specialReward)
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CHANGE_THE_ITEM);
		}
		
		if (!enchantedItems.isEmpty())
		{
			final InventoryUpdate playerIU = new InventoryUpdate();
			for (Item i : enchantedItems)
			{
				playerIU.addModifiedItem(i);
			}
			
			player.sendInventoryUpdate(playerIU);
		}
		
		for (Entry<Item, Long> entry : extractedItems.entrySet())
		{
			sendMessage(player, entry.getKey(), entry.getValue());
		}
		
		final AutoPeelRequest request = player.getRequest(AutoPeelRequest.class);
		if (request != null)
		{
			if (request.isProcessing())
			{
				request.setProcessing(false);
				final List<ItemHolder> rewards = new LinkedList<>();
				for (Entry<Item, Long> entry : extractedItems.entrySet())
				{
					rewards.add(new ItemHolder(entry.getKey().getId(), entry.getValue()));
				}
				
				player.sendPacket(new ExResultItemAutoPeel(true, request.getTotalPeelCount(), request.getRemainingPeelCount() - 1, rewards));
			}
			else
			{
				player.sendPacket(new ExStopItemAutoPeel(false));
			}
		}
		else // Fixes last auto peel not shown. No effect if auto peel window is closed.
		{
			final List<ItemHolder> rewards = new LinkedList<>();
			for (Entry<Item, Long> entry : extractedItems.entrySet())
			{
				rewards.add(new ItemHolder(entry.getKey().getId(), entry.getValue()));
			}
			
			player.sendPacket(new ExResultItemAutoPeel(true, 0, 0, rewards));
		}
		
		return true;
	}
	
	private void addItem(Map<Item, Long> extractedItems, Item newItem, long count)
	{
		if (extractedItems.containsKey(newItem))
		{
			extractedItems.put(newItem, extractedItems.get(newItem) + count);
		}
		else
		{
			extractedItems.put(newItem, count);
		}
	}
	
	private void sendMessage(Player player, Item item, long count)
	{
		final SystemMessage sm;
		if (count > 1)
		{
			sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
			sm.addItemName(item);
			sm.addLong(count);
		}
		else if (item.getEnchantLevel() > 0)
		{
			sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_S2);
			sm.addInt(item.getEnchantLevel());
			sm.addItemName(item);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1);
			sm.addItemName(item);
		}
		
		player.sendPacket(sm);
	}
}
