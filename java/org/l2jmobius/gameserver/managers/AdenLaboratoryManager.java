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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.AdenLaboratoryConfig;
import org.l2jmobius.gameserver.data.enums.AdenLabGameType;
import org.l2jmobius.gameserver.data.holders.AdenLabHolder;
import org.l2jmobius.gameserver.data.holders.AdenLabSkillHolder;
import org.l2jmobius.gameserver.data.holders.AdenLabStageHolder;
import org.l2jmobius.gameserver.data.xml.AdenLaboratoryData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.AdenLabRequest;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.adenlab.ExAdenLabBossInfo;
import org.l2jmobius.gameserver.network.serverpackets.adenlab.ExAdenLabBossList;
import org.l2jmobius.gameserver.network.serverpackets.adenlab.ExAdenLabBossUnlock;
import org.l2jmobius.gameserver.network.serverpackets.adenlab.ExAdenLabNormalPlay;
import org.l2jmobius.gameserver.network.serverpackets.adenlab.ExAdenLabNormalSlot;
import org.l2jmobius.gameserver.network.serverpackets.adenlab.ExAdenLabSpecialFix;
import org.l2jmobius.gameserver.network.serverpackets.adenlab.ExAdenLabSpecialPlay;
import org.l2jmobius.gameserver.network.serverpackets.adenlab.ExAdenLabSpecialProb;
import org.l2jmobius.gameserver.network.serverpackets.adenlab.ExAdenLabSpecialSlot;
import org.l2jmobius.gameserver.network.serverpackets.adenlab.ExAdenLabTranscendAnnounce;
import org.l2jmobius.gameserver.network.serverpackets.adenlab.ExAdenLabTranscendEnchant;
import org.l2jmobius.gameserver.network.serverpackets.adenlab.ExAdenLabTranscendProb;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author SaltyMike
 */
public class AdenLaboratoryManager
{
	private static final Logger LOGGER = Logger.getLogger(AdenLaboratoryManager.class.getName());
	
	public static int sortingComparator(byte p1, byte p2)
	{
		final int group1 = p1 / 10;
		final int group2 = p2 / 10;
		if (group1 != group2)
		{
			return Integer.compare(group1, group2);
		}
		
		return Byte.compare(p1, p2);
	}
	
	/**
	 * @param quickLookupMap
	 * @param countTypePages True = pages False = skills
	 * @return
	 */
	public static int getTotalCount(Map<Byte, Map<Byte, List<int[]>>> quickLookupMap, boolean countTypePages)
	{
		int totalCount = 0;
		for (Entry<Byte, Map<Byte, List<int[]>>> bossEntry : quickLookupMap.entrySet())
		{
			// final byte bossId = bossEntry.getKey();
			final Map<Byte, List<int[]>> bossSkills = bossEntry.getValue();
			
			// Get the max page index for this bossId.
			final byte maxPageIndex = bossSkills.keySet().stream().max(AdenLaboratoryManager::sortingComparator).orElse((byte) -1); // Default to -1 if no pages exist for this boss
			if (maxPageIndex == -1)
			{
				continue; // If no valid page exists, skip this boss.
			}
			
			if (countTypePages)
			{
				totalCount += maxPageIndex;
			}
			else
			{
				// Get the map corresponding to the highest page index for this boss.
				final List<int[]> highestPageSkills = bossSkills.get(maxPageIndex);
				if ((highestPageSkills != null) && !highestPageSkills.isEmpty())
				{
					// Add the size of the highest page (i.e., number of skills at that page).
					totalCount += highestPageSkills.size();
				}
			}
		}
		
		return totalCount;
	}
	
	public static void addSkillToCache(int bossId, int pageIndex, int optionIndex, int stageLevel, int[] skill)
	{
		final int skillId = skill[0];
		final int skillLevel = skill[1];
		
		// Create skill holder.
		final AdenLabSkillHolder skillHolder = new AdenLabSkillHolder(skillId, skillLevel);
		
		// Get or initialize the first-level map (bossId -> pages).
		final Map<Byte, Map<Byte, Map<Byte, List<AdenLabSkillHolder>>>> pageMap = AdenLaboratoryData.getInstance().getSkillsLookupTable().computeIfAbsent((byte) bossId, k -> new HashMap<>());
		
		// Get or initialize the second-level map (pageIndex -> optionIndices).
		final Map<Byte, Map<Byte, List<AdenLabSkillHolder>>> optionsMap = pageMap.computeIfAbsent((byte) pageIndex, k -> new HashMap<>());
		
		// Get or initialize the third-level map (optionIndex -> stageLevels).
		final Map<Byte, List<AdenLabSkillHolder>> stageMap = optionsMap.computeIfAbsent((byte) optionIndex, k -> new HashMap<>());
		
		// Get or initialize the list for the stage level.
		final List<AdenLabSkillHolder> skillList = stageMap.computeIfAbsent((byte) stageLevel, k -> new ArrayList<>());
		
		// Add the skill holder instead of replacing.
		skillList.add(skillHolder);
	}
	
	public static void calculateAdenLabCombatPower(Player player)
	{
		int totalCombatPower = 0;
		for (Entry<Byte, Map<Integer, AdenLabHolder>> bossMap : AdenLaboratoryData.getInstance().getAllAdenLabData().entrySet())
		{
			final byte bossId = bossMap.getKey();
			final Map<Integer, AdenLabHolder> holderMap = bossMap.getValue();
			
			final int currentPageIndex = player.getAdenLabCurrentlyUnlockedPage(bossId);
			
			// Special stages.
			for (byte pageIndex : AdenLaboratoryData.getInstance().getSpecialStageIndicesByBossId(bossId))
			{
				if (pageIndex > currentPageIndex)
				{
					continue;
				}
				
				final AdenLabHolder holder = holderMap.get((int) pageIndex);
				if ((holder == null) || (holder.getGameType() != AdenLabGameType.SPECIAL))
				{
					continue;
				}
				
				final Map<Byte, Map<Byte, Integer>> confirmedOptions = player.getAdenLabSpecialGameStagesConfirmedOptions().get(bossId);
				if (confirmedOptions == null)
				{
					continue;
				}
				
				final Map<Byte, Integer> optionToLevelMap = confirmedOptions.get(pageIndex);
				if (optionToLevelMap == null)
				{
					continue;
				}
				
				OPTIONS: for (Entry<Byte, Integer> entry : optionToLevelMap.entrySet())
				{
					final byte optionIndex = entry.getKey();
					final byte stageLevel = entry.getValue().byteValue();
					
					final List<AdenLabStageHolder> stageList = holder.getStageHolderListByLevel(optionIndex, stageLevel);
					if ((stageList == null) || stageList.isEmpty())
					{
						continue;
					}
					
					for (AdenLabStageHolder stageHolder : stageList)
					{
						if (stageHolder.getStageLevel() == stageLevel)
						{
							totalCombatPower += stageHolder.getCombatPower();
							break OPTIONS;
						}
					}
				}
				
				// totalCombatPower += AdenLabData.getInstance().getSpecialStageCombatPower(bossId, pageIndex); // shortcut
			}
			
			// Transcendent stages.
			final byte transcendentStageLevel = (byte) player.getAdenLabCurrentTranscendLevel(bossId);
			if (transcendentStageLevel > 0)
			{
				for (byte pageIndex : AdenLaboratoryData.getInstance().getTranscendentStageIndicesByBossId(bossId))
				{
					if (pageIndex > currentPageIndex)
					{
						return; // We assume that Transcendent has only 1 page.
					}
					
					totalCombatPower += AdenLaboratoryData.getInstance().getTranscendentCombatPower(bossId, pageIndex, transcendentStageLevel); // shortcut
				}
			}
		}
		
		player.getCombatPower().setAdenLabCombatPower(totalCombatPower);
	}
	
	/**
	 * @param bossId
	 * @param pageIndex
	 * @param optionIndex - counter-intuitively it starts from 1, sorry. Too lazy to fix it now! :P
	 * @param player
	 * @param simulateSpecial
	 * @param numberOfSimulations
	 * @return
	 */
	private static byte calculateSuccess(byte bossId, byte pageIndex, byte optionIndex, Player player, boolean simulateSpecial, long numberOfSimulations)
	{
		final AdenLabHolder holder = AdenLaboratoryData.getInstance().getAdenLabDataByPageIndex(bossId, pageIndex);
		final double random = (Rnd.nextDouble() * 100);
		final float successRate = holder.getGameSuccessRate() * 100;
		final float bonusChance = player.getAdenLabBonusChance();
		float finalSuccessRate = 0f;
		
		// Special game type must always pick a stage
		if (holder.getGameType() == AdenLabGameType.SPECIAL)
		{
			final int result = getWeightedResult(player, holder, optionIndex, false);
			
			// SIMULATION BLOCK
			if (simulateSpecial && (numberOfSimulations > 0))
			{
				// Uncomment below in order to override the value in the method call.
				// numberOfSimulations = 100000;
				player.sendMessage("Initiating simulation of weighted probabilities. Simulating " + numberOfSimulations + " times...");
				
				final Map<Integer, Integer> stageDistribution = new HashMap<>(); // Stores counts of selected stages
				final long startTime = System.nanoTime();
				
				// Simulate odds
				for (int i = 0; i < numberOfSimulations; i++)
				{
					final int selectedStage = getWeightedResult(player, holder, optionIndex, true); // Run weighted selection.
					stageDistribution.put(selectedStage, stageDistribution.getOrDefault(selectedStage, 0) + 1); // Count occurrences.
				}
				
				final long endTime = System.nanoTime();
				
				// Distribution results
				player.sendMessage("Simulation Results:");
				for (Entry<Integer, Integer> entry : stageDistribution.entrySet())
				{
					final int stage = entry.getKey();
					final int count = entry.getValue();
					final double percentage = ((double) count / numberOfSimulations) * 100f; // Convert to percentage.
					float originalStageChance = 0f;
					for (Entry<Byte, List<AdenLabStageHolder>> stages : holder.getOptions().get(optionIndex).entrySet())
					{
						for (AdenLabStageHolder stageHolder : stages.getValue())
						{
							if (stageHolder.getStageLevel() != stage)
							{
								continue;
							}
							
							originalStageChance = stageHolder.getStageChance() * 100; // Convert to percentage.
							
						}
					}
					
					player.sendMessage("Stage " + stage + " (" + String.format("%.2f", originalStageChance) + "%): Selected [" + count + "] times or " + String.format("%.2f", percentage) + "% (" + String.format("%+.2f", (percentage - originalStageChance)) + ") of the draws.");
				}
				
				// Display total time taken.
				final long elapsedTimeMs = (endTime - startTime) / 1000000; // Convert nanoseconds to milliseconds.
				player.sendMessage("Total simulation time: " + elapsedTimeMs + " ms");
			}
			
			return (byte) result;
		}
		else if (holder.getGameType() == AdenLabGameType.NORMAL)
		{
			final byte totalCardCount = holder.getCardCount();
			final byte openedCardCount = (byte) player.getAdenLabNormalGameOpenedCardsCount(bossId);
			if (openedCardCount >= totalCardCount)
			{
				LOGGER.warning("CHEAT attempt! Player" + player.getName() + "[" + player.getObjectId() + "] tried to open a card that should not exist: total cards=" + totalCardCount + " <= opened cards=" + openedCardCount);
				return 0;
			}
			
			// If there is only one card remaining, automatically return 1 (success = true).
			if ((totalCardCount - openedCardCount) == 1)
			{
				return 1;
			}
			
			final byte totalCards = holder.getCardCount();
			final byte openedCards = (byte) Math.min(player.getAdenLabNormalGameOpenedCardsCount(bossId), 5);
			
			// Just a test. The flat values must be rebalanced if used on a live server!
			final float cardProgressionBonus = ((float) openedCards / totalCards) * (25 - successRate);
			
			// Final success rate is limited to 100 for debug msg purposes. Has no effect outside of that.
			finalSuccessRate = Math.min(successRate + bonusChance + cardProgressionBonus, 100f);
		}
		else // Incredible Stage
		{
			final int stageIndex = player.getAdenLabCurrentTranscendLevel(bossId) + 1;
			for (Entry<Byte, List<AdenLabStageHolder>> stages : holder.getOptions().get(optionIndex).entrySet())
			{
				for (AdenLabStageHolder stageHolder : stages.getValue())
				{
					if (stageHolder.getStageLevel() != stageIndex)
					{
						continue;
					}
					
					finalSuccessRate = stageHolder.getStageChance() * 100;
				}
			}
		}
		
		return (byte) ((random <= finalSuccessRate) ? 1 : 0);
	}
	
	private static int getWeightedResult(Player player, AdenLabHolder holder, byte optionIndex, boolean isSimulation)
	{
		final List<Float> stageWeights = new ArrayList<>();
		float totalWeight = 0f;
		
		// Calculate the weights of each stage based on its chance.
		for (Entry<Byte, List<AdenLabStageHolder>> entry : holder.getOptions().get(optionIndex).entrySet())
		{
			for (AdenLabStageHolder stageHolder : entry.getValue())
			{
				final float originalStageChance = stageHolder.getStageChance() * 100;
				stageWeights.add(originalStageChance);
				totalWeight += originalStageChance;
			}
		}
		
		float randomWeight = (float) (Rnd.nextDouble() * totalWeight);
		
		// Select the stage based on the weighted random value.
		float cumulativeWeight = 0f;
		for (int i = 0; i < stageWeights.size(); i++)
		{
			cumulativeWeight += stageWeights.get(i);
			if (randomWeight <= cumulativeWeight)
			{
				return (byte) (i + 1);
			}
		}
		
		// Fallback to stage 1 if no valid stage was selected for whatever reason.
		return 1;
	}
	
	private static void takeItemsAndUpdateInventory(Player player, Map<Item, Long> feeItemsMap, long adenaAmount)
	{
		// Handle Adena-Only case, if no items in the feeItemsMap.
		if ((feeItemsMap == null) || feeItemsMap.isEmpty())
		{
			if (adenaAmount > 0)
			{
				player.getInventory().reduceAdena(ItemProcessType.ADENLAB, adenaAmount, player, null);
				
				// Handle Adena Update.
				InventoryUpdate iuAdena = new InventoryUpdate();
				if ((player.getInventory().getAdena() - adenaAmount) > 0)
				{
					iuAdena.addModifiedItem(player.getInventory().getAdenaInstance());
				}
				else
				{
					iuAdena.addRemovedItem(player.getInventory().getAdenaInstance());
				}
				
				player.sendPacket(iuAdena);
				player.updateAdenaAndWeight();
			}
			return;
		}
		
		// Process Adena fee first if any.
		if (adenaAmount > 0)
		{
			player.getInventory().reduceAdena(ItemProcessType.ADENLAB, adenaAmount, player, null);
			
			// Send Adena Update.
			final InventoryUpdate iuAdena = new InventoryUpdate();
			if ((player.getInventory().getAdena() - adenaAmount) > 0)
			{
				iuAdena.addModifiedItem(player.getInventory().getAdenaInstance());
			}
			else
			{
				iuAdena.addRemovedItem(player.getInventory().getAdenaInstance());
			}
			
			player.sendPacket(iuAdena);
		}
		
		// Process item fees from feeItemsMap.
		for (Entry<Item, Long> entry : feeItemsMap.entrySet())
		{
			final Item item = entry.getKey();
			final long feeAmount = entry.getValue();
			
			// Destroy the item from player's inventory.
			player.getInventory().destroyItemByItemId(ItemProcessType.ADENLAB, item.getId(), feeAmount, player, null);
			
			final InventoryUpdate iuItem = new InventoryUpdate();
			if ((item.getCount() - feeAmount) > 0)
			{
				iuItem.addModifiedItem(item);
			}
			else
			{
				iuItem.addRemovedItem(item);
			}
			
			player.sendPacket(iuItem);
		}
		
		// player.sendItemList();
		player.updateAdenaAndWeight();
	}
	
	public static Map<Item, Long> getFeeItemsFromCache(Player player, Map<Integer, Object[]> feeCache, int feeIndex)
	{
		final Map<Item, Long> feeItemsMap = new HashMap<>();
		
		// Retrieve the fee details from the cache.
		final Object[] itemFee = feeCache.get(feeIndex);
		if ((itemFee != null) && (itemFee.length == 2))
		{
			try
			{
				final int itemId = (int) itemFee[0];
				final long itemAmount = (long) itemFee[1];
				
				final Item item = player.getInventory().getItemByItemId(itemId);
				if (item != null)
				{
					feeItemsMap.put(item, itemAmount);
				}
				else
				{
					LOGGER.warning("Item with ID " + itemId + " not found in inventory. Skipping.");
				}
			}
			catch (ClassCastException e)
			{
				LOGGER.warning("Invalid fee cache structure for index " + feeIndex + ". Expected (Integer, Long).");
			}
		}
		else
		{
			LOGGER.warning("Fee index " + feeIndex + " not found in cache.");
		}
		
		return feeItemsMap;
	}
	
	/*
	 * * * * * * * * * * * * * * * * * * * SKILLS BLOCK * * * * * * * * * * * * * * * * * *
	 */
	public static void checkPlayerSkills(Player player)
	{
		final AdenLaboratoryData adenLabData = AdenLaboratoryData.getInstance();
		for (Entry<Byte, Map<Integer, AdenLabHolder>> bossEntry : adenLabData.getAllAdenLabData().entrySet())
		{
			final byte bossId = bossEntry.getKey();
			final int currentUnlockedPage = player.getAdenLabCurrentlyUnlockedPage(bossId);
			
			processNormalSkills(player, bossId, currentUnlockedPage);
			processSpecialSkills(player, bossId, bossEntry.getValue());
			processTranscendentSkills(player, bossId, bossEntry.getValue());
		}
	}
	
	public static void giveAdenLabSkills(Player player, byte bossId, byte pageIndex, byte optionIndex, int stageLevel)
	{
		for (AdenLabStageHolder stageHolder : AdenLaboratoryData.getInstance().getAdenLabDataByPageIndex(bossId, pageIndex).getStageHolderListByLevel(optionIndex, stageLevel))
		{
			if (stageHolder.getStageLevel() == stageLevel)
			{
				final List<AdenLabSkillHolder> skills = stageHolder.getSkills();
				if ((skills == null) || skills.isEmpty())
				{
					continue;
				}
				
				addSkillIfNeeded(player, skills.get(optionIndex - 1).getId(), skills.get(optionIndex - 1).getLvl());
			}
		}
	}
	
	private static void processNormalSkills(Player player, byte bossId, int currentUnlockedPage)
	{
		final List<int[]> normalSkills = AdenLaboratoryData.getInstance().getNormalStageSkillsUpToPage(bossId, (byte) currentUnlockedPage);
		for (int[] skill : normalSkills)
		{
			addSkillIfNeeded(player, skill[0], skill[1]);
		}
	}
	
	private static void processSpecialSkills(Player player, byte bossId, Map<Integer, AdenLabHolder> adenLabHolders)
	{
		final Map<Byte, Map<Byte, Integer>> confirmedStages = player.getAdenLabSpecialGameStagesConfirmedOptions().getOrDefault(bossId, Collections.emptyMap());
		for (int pageIndex : AdenLaboratoryData.getInstance().getSpecialStageIndicesByBossId(bossId))
		{
			final AdenLabHolder holder = adenLabHolders.get(pageIndex);
			if ((holder == null) || (holder.getGameType() != AdenLabGameType.SPECIAL))
			{
				continue;
			}
			
			confirmedStages.getOrDefault((byte) pageIndex, Collections.emptyMap()).forEach((optionIndex, stageLevel) -> processStageSkills(player, holder, optionIndex, stageLevel));
		}
	}
	
	private static void processStageSkills(Player player, AdenLabHolder holder, byte optionIndex, int stageLevel)
	{
		final List<AdenLabStageHolder> stageHolders = holder.getStageHolderListByLevel(optionIndex, stageLevel);
		if ((stageHolders == null) || stageHolders.isEmpty())
		{
			return;
		}
		
		for (AdenLabStageHolder stageHolder : stageHolders)
		{
			if (stageHolder.getStageLevel() != stageLevel)
			{
				continue;
			}
			
			final List<AdenLabSkillHolder> skills = stageHolder.getSkills();
			if ((skills == null) || skills.isEmpty())
			{
				continue;
			}
			
			addSkillIfNeeded(player, skills.get(optionIndex - 1).getId(), skills.get(optionIndex - 1).getLvl());
		}
	}
	
	private static void processTranscendentSkills(Player player, byte bossId, Map<Integer, AdenLabHolder> adenLabHolderMap)
	{
		final byte transcendentLevel = (byte) player.getAdenLabCurrentTranscendLevel(bossId);
		for (Entry<Integer, AdenLabHolder> entry : adenLabHolderMap.entrySet())
		{
			if (entry.getValue().getGameType() != AdenLabGameType.INCREDIBLE)
			{
				continue;
			}
			
			// int pageIndex = entry.getKey();
			final AdenLabHolder holder = entry.getValue();
			for (Entry<Byte, Map<Byte, List<AdenLabStageHolder>>> option : holder.getOptions().entrySet())
			{
				// byte optionIndex = option.getKey();
				final List<AdenLabStageHolder> stageList = option.getValue().get(transcendentLevel);
				if ((stageList != null) && !stageList.isEmpty())
				{
					stageList.forEach(stageHolder ->
					{
						stageHolder.getSkills().forEach(skill ->
						{
							final int skillid = skill.getId();
							final int skillLevel = skill.getLvl();
							addSkillIfNeeded(player, skillid, skillLevel);
						});
					});
				}
			}
		}
	}
	
	private static void addSkillIfNeeded(Player player, int skillId, int skillLevel)
	{
		final Skill existingSkill = player.getKnownSkill(skillId);
		if ((existingSkill == null) || (existingSkill.getLevel() != skillLevel))
		{
			player.addSkill(SkillData.getInstance().getSkill(skillId, skillLevel), false);
		}
	}
	
	public static void deletePlayerSkills(Player player, byte bossId)
	{
		for (Entry<Integer, AdenLabHolder> entry : AdenLaboratoryData.getInstance().getAdenLabData(bossId).entrySet())
		{
			// final int pageIndex = entry.getKey();
			final AdenLabHolder holder = entry.getValue();
			for (Entry<Byte, Map<Byte, List<AdenLabStageHolder>>> optionEntries : holder.getOptions().entrySet())
			{
				// final int optionIndex = optionEntries.getKey();
				final Map<Byte, List<AdenLabStageHolder>> stageMap = optionEntries.getValue();
				for (Entry<Byte, List<AdenLabStageHolder>> stageHolder : stageMap.entrySet())
				{
					// final int stageLevel = stageHolder.getKey();
					final List<AdenLabStageHolder> data = stageHolder.getValue();
					if ((data != null) && !data.isEmpty())
					{
						data.forEach(dataHolder ->
						{
							dataHolder.getSkills().forEach(skill ->
							{
								final int skillid = skill.getId();
								final int skillLevel = skill.getLvl();
								if (player.getKnownSkill(skillid) != null)
								{
									Skill toRemove = SkillData.getInstance().getSkill(skillid, skillLevel);
									player.removeSkill(toRemove);
								}
							});
						});
					}
				}
			}
		}
	}
	
	/*
	 * * * * * * * * * * * * * * * * * * PACKETS BLOCK * * * * * * * * * * * * * * * * *
	 */
	// incoming
	public static void processRequestAdenLabBossInfo(Player activeChar, byte bossId)
	{
		final Map<Byte, Map<Integer, AdenLabHolder>> holder = AdenLaboratoryData.getInstance().getAllAdenLabData();
		if ((holder == null) || !holder.containsKey(bossId))
		{
			LOGGER.warning("Player " + activeChar.getName() + " [" + activeChar.getObjectId() + "] requested INFO for a non-existing boss with ID: " + bossId);
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Needs to be sent here, else it won't show the Incredible stages' chance.
		processTranscendentProbabilities(activeChar, bossId);
		
		activeChar.sendPacket(new ExAdenLabBossInfo(bossId, activeChar));
	}
	
	public static void processRequestAdenLabBossList(Player player, List<Integer> bossList)
	{
		player.addRequest(new AdenLabRequest(player));
		
		final Map<Byte, Map<Integer, AdenLabHolder>> holder = AdenLaboratoryData.getInstance().getAllAdenLabData();
		if ((holder == null) || holder.isEmpty())
		{
			player.removeRequest(AdenLabRequest.class);
			return;
		}
		
		for (int bossId : bossList)
		{
			if (!holder.containsKey((byte) bossId))
			{
				player.removeRequest(AdenLabRequest.class);
				LOGGER.warning("Player " + player.getName() + " [" + player.getObjectId() + "] requested the wrong BOSS LIST, specifically missing a boss with ID: " + bossId);
				return;
			}
		}
		
		player.removeRequest(AdenLabRequest.class);
		player.sendPacket(new ExAdenLabBossList(bossList));
	}
	
	public static void processRequestAdenLabBossUnlock(Player player, int bossId)
	{
		player.addRequest(new AdenLabRequest(player));
		
		final Map<Byte, Map<Integer, AdenLabHolder>> adenLabHolder = AdenLaboratoryData.getInstance().getAllAdenLabData();
		if ((adenLabHolder == null) || !adenLabHolder.containsKey((byte) bossId))
		{
			player.removeRequest(AdenLabRequest.class);
			LOGGER.warning("Player " + player.getName() + " [" + player.getObjectId() + "] tried to access missing data for boss with ID " + bossId);
			return;
		}
		
		player.removeRequest(AdenLabRequest.class);
		
		// TODO: FINISH when they introduce more bosses.
		player.sendPacket(new ExAdenLabBossUnlock(bossId, false));
	}
	
	public static void processRequestAdenLabNormalPlay(Player player, byte bossId, byte pageIndex, int feeIndex)
	{
		final AdenLabHolder adenLabHolder = AdenLaboratoryData.getInstance().getAdenLabDataByPageIndex(bossId, pageIndex);
		if (adenLabHolder == null)
		{
			LOGGER.warning("Player " + player.getName() + " [" + player.getObjectId() + "] tried to access missing data for boss with ID " + bossId + " and page with ID " + pageIndex);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.addRequest(new AdenLabRequest(player));
		
		final long adenaCount = AdenLaboratoryConfig.ADENLAB_NORMAL_ADENA_FEE_AMOUNT;
		if (player.getInventory().getAdena() < adenaCount)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
			player.removeRequest(AdenLabRequest.class);
			return;
		}
		
		// Fetch fee items and validate if the player has enough of them.
		final Map<Item, Long> feeItemsMap = getFeeItemsFromCache(player, AdenLaboratoryConfig.ADENLAB_NORMAL_ROLL_FEE_TYPE_CACHE, feeIndex);
		if (!feeItemsMap.isEmpty())
		{
			for (Entry<Item, Long> entry : feeItemsMap.entrySet())
			{
				final Item item = entry.getKey();
				final long requiredAmount = entry.getValue();
				if ((item == null) || (item.getCount() < requiredAmount))
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					player.removeRequest(AdenLabRequest.class);
					return;
				}
			}
		}
		
		// Take items and update inventory.
		takeItemsAndUpdateInventory(player, feeItemsMap.isEmpty() ? new HashMap<>() : feeItemsMap, adenaCount);
		
		// TODO: Keep an eye out on newer releases since this might change. At the moment we assume that all Normal games have ONLY ONE stage!
		final byte result = calculateSuccess(bossId, pageIndex, (byte) 1, player, false, 0);
		if (result == 0)
		{
			// We failed to open the correct card, so we increment the count.
			player.incrementAdenLabNormalGameOpenedCardsCount(bossId);
		}
		else
		{
			// We have found the right card, so we increment the page number and reset the card count back to 0
			// increment the page limit only if a higher page is not already opened to prevent exploits.
			if (player.getAdenLabCurrentlyUnlockedPage(bossId) == pageIndex)
			{
				player.incrementAdenLabCurrentPage(bossId);
			}
			
			player.setAdenLabNormalGameOpenedCardsCount(bossId, 0);
			giveAdenLabSkills(player, bossId, pageIndex, (byte) 1, 1); // optionIndex starts from 1
		}
		
		player.removeRequest(AdenLabRequest.class);
		player.sendPacket(new ExAdenLabNormalPlay(bossId, pageIndex, result));
	}
	
	public static void processRequestAdenLabNormalSlot(Player player, int bossId, int pageIndex)
	{
		player.addRequest(new AdenLabRequest(player));
		
		final AdenLabHolder adenLabHolder = AdenLaboratoryData.getInstance().getAdenLabDataByPageIndex(bossId, pageIndex);
		if (adenLabHolder == null)
		{
			player.removeRequest(AdenLabRequest.class);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.removeRequest(AdenLabRequest.class);
		player.sendPacket(new ExAdenLabNormalSlot(bossId, pageIndex, (byte) (adenLabHolder.getCardCount() - player.getAdenLabNormalGameOpenedCardsCount((byte) bossId))));
	}

	/**
	 * @param player
	 * @param bossId
	 * @param pageIndex
	 * @param feeIndex currently not in use. That might change in future L2 updates.
	 */
	public static void processRequestAdenLabSpecialFix(Player player, byte bossId, byte pageIndex, int feeIndex)
	{
		player.addRequest(new AdenLabRequest(player));

		// 檢查是否為該頁面的第一次確認(第一次免費)
		String firstFreeVar = "AdenLab_FirstFree_" + bossId + "_" + pageIndex;
		boolean isFirstFree = !player.getVariables().getBoolean(firstFreeVar, false);

		// 第一次免費,之後才收費
		final long adenaCount = isFirstFree ? 0 : AdenLaboratoryConfig.ADENLAB_SPECIAL_CONFIRM_FEE;

		// 除錯訊息(伺服器日誌)
		//LOGGER.info("【亞丁研究所】玩家 " + player.getName() + " - Boss:" + bossId + " 頁面:" + pageIndex + " 第一次:" + isFirstFree + " 需要:" + adenaCount + " 擁有:" + player.getInventory().getAdena());

		if (player.getInventory().getAdena() < adenaCount)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
			player.removeRequest(AdenLabRequest.class);
			player.sendPacket(new ExAdenLabSpecialFix(bossId, pageIndex, false));
			return;
		}

		// Ensure the first-level map is not null for 'drawnOptions'.
		Map<Byte, Map<Byte, Integer>> drawnOptionsMap = player.getAdenLabSpecialGameStagesDrawnOptions().get(bossId);
		if (drawnOptionsMap == null)
		{
			drawnOptionsMap = new HashMap<>(); // Initialize as empty if null.
		}

		Map<Byte, Integer> drawnOptions = drawnOptionsMap.get(pageIndex);
		if (drawnOptions == null)
		{
			drawnOptions = new HashMap<>(); // Initialize as empty if null.
		}

		final Iterator<Entry<Byte, Integer>> iterator = drawnOptions.entrySet().iterator();
		while (iterator.hasNext())
		{
			final Entry<Byte, Integer> optionsMap = iterator.next();
			final byte optionIndex = optionsMap.getKey();
			final int stageLevel = optionsMap.getValue();

			player.setAdenLabSpecialGameConfirmedOptionsIndividual(bossId, pageIndex, optionIndex, stageLevel);

			giveAdenLabSkills(player, bossId, pageIndex, optionIndex, stageLevel);

			iterator.remove();

			// Reset drawn options.
			player.setAdenLabSpecialGameDrawnOptionsIndividual(bossId, pageIndex, optionsMap.getKey(), -1);
		}

		// Increment the page limit only if a higher page is not already opened to prevent exploits.
		if (player.getAdenLabCurrentlyUnlockedPage(bossId) == pageIndex)
		{
			player.incrementAdenLabCurrentPage(bossId);

			// Update combat power.
			calculateAdenLabCombatPower(player);
		}

		// 扣除金幣
		takeItemsAndUpdateInventory(player, new HashMap<>(), adenaCount);

		// 如果是第一次使用,標記為已使用
		if (isFirstFree)
		{
			player.getVariables().set(firstFreeVar, true);
		}

		player.removeRequest(AdenLabRequest.class);
		player.sendPacket(new ExAdenLabSpecialFix(bossId, pageIndex, true));
	}
	
	/**
	 * @param player
	 * @param bossId
	 * @param pageIndex
	 * @param feeIndex currently not in use. That might change in future L2 updates.
	 */
	public static void processRequestAdenLabSpecialPlay(Player player, byte bossId, byte pageIndex, int feeIndex)
	{
		player.addRequest(new AdenLabRequest(player));
		
		final AdenLabHolder adenLabHolder = AdenLaboratoryData.getInstance().getAdenLabDataByPageIndex(bossId, pageIndex);
		if (adenLabHolder == null)
		{
			player.removeRequest(AdenLabRequest.class);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final long adenaCount = AdenLaboratoryConfig.ADENLAB_SPECIAL_RESEARCH_FEE;
		if (player.getInventory().getAdena() < adenaCount)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
			player.removeRequest(AdenLabRequest.class);
			return;
		}
		
		// Calculate success based on the number of stageIndices in the holder.
		final Map<Integer, Integer> drawnOptions = new HashMap<>();
		for (int i = 1; i <= adenLabHolder.getOptions().size(); i++)
		{
			final int result = calculateSuccess(bossId, pageIndex, (byte) i, player, false, 0);
			drawnOptions.put(i, result);
		}
		
		// Take items and handle UI update.
		takeItemsAndUpdateInventory(player, new HashMap<>(), adenaCount); // yes, the empty map is intentionally sent
		
		for (Entry<Integer, Integer> temp : drawnOptions.entrySet())
		{
			player.setAdenLabSpecialGameDrawnOptionsIndividual(bossId, pageIndex, temp.getKey().byteValue(), temp.getValue());
		}
		
		player.removeRequest(AdenLabRequest.class);
		player.sendPacket(new ExAdenLabSpecialPlay(bossId, pageIndex, (byte) drawnOptions.size(), drawnOptions));
	}
	
	public static void processRequestAdenLabSpecialProbability(Player player, int bossId, int pageIndex)
	{
		player.addRequest(new AdenLabRequest(player));
		
		final AdenLabHolder adenLabHolder = AdenLaboratoryData.getInstance().getAdenLabDataByPageIndex(bossId, pageIndex);
		if (adenLabHolder == null)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.removeRequest(AdenLabRequest.class);
			return;
		}
		
		final Map<Integer, List<Integer>> probMap = new ConcurrentHashMap<>();
		
		int counter = 0;
		for (Entry<Byte, Map<Byte, List<AdenLabStageHolder>>> option : adenLabHolder.getOptions().entrySet())
		{
			final List<Integer> options = new ArrayList<>();
			for (List<AdenLabStageHolder> stages : option.getValue().values())
			{
				for (AdenLabStageHolder stage : stages)
				{
					final int levelIndex = stage.getStageLevel() - 1;
					final int chance = Math.round(stage.getStageChance() * 10000); // Must always multiply by 10000 because every point = 0.01%
					
					// Ensure the list is large enough.
					while (options.size() <= levelIndex)
					{
						options.add(0); // Fill missing indices with 0.
					}
					
					options.set(levelIndex, chance);
				}
			}
			
			probMap.put(counter, options);
			counter++;
		}
		
		player.removeRequest(AdenLabRequest.class);
		player.sendPacket(new ExAdenLabSpecialProb(bossId, pageIndex, probMap));
	}
	
	public static void processRequestAdenLabSpecialSlot(Player player, int bossId, int pageIndex)
	{
		player.addRequest(new AdenLabRequest(player));
		
		// Ensure the first-level map is not null for 'drawnOptions'.
		Map<Byte, Map<Byte, Integer>> drawnOptionsMap = player.getAdenLabSpecialGameStagesDrawnOptions().get((byte) bossId);
		if (drawnOptionsMap == null)
		{
			drawnOptionsMap = new HashMap<>(); // Initialize as empty if null.
		}
		
		Map<Byte, Integer> drawnOptions = drawnOptionsMap.get((byte) pageIndex);
		if (drawnOptions == null)
		{
			drawnOptions = new HashMap<>(); // Initialize as empty if null.
		}
		
		for (Entry<Byte, Integer> stage : drawnOptions.entrySet())
		{
			drawnOptions.putIfAbsent(stage.getKey(), stage.getValue());
		}
		
		// Ensure the first-level map is not null for 'confirmedOptions'.
		Map<Byte, Map<Byte, Integer>> confirmedOptionsMap = player.getAdenLabSpecialGameStagesConfirmedOptions().get((byte) bossId);
		if (confirmedOptionsMap == null)
		{
			confirmedOptionsMap = new HashMap<>(); // Initialize as empty if null.
		}
		
		Map<Byte, Integer> confirmedOptions = confirmedOptionsMap.get((byte) pageIndex);
		if (confirmedOptions == null)
		{
			confirmedOptions = new HashMap<>(); // Initialize as empty if null
		}
		
		for (Entry<Byte, Integer> stage : confirmedOptions.entrySet())
		{
			confirmedOptions.putIfAbsent(stage.getKey(), stage.getValue());
		}
		
		player.removeRequest(AdenLabRequest.class);
		player.sendPacket(new ExAdenLabSpecialSlot(bossId, pageIndex, drawnOptions, confirmedOptions));
	}
	
	public static void processRequestAdenLabTranscendentEnchant(Player player, byte bossId, int feeIndex)
	{
		if (!AdenLaboratoryConfig.ADENLAB_INCREDIBLE_ROLL_FEE_TYPE_CACHE.containsKey(feeIndex))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.sendPacket(SystemMessageId.THE_FUNCTION_IS_UNAVAILABLE);
			// player.sendPacket(new ExAdenLabTranscendEnchant(bossId,(byte) 0));
			LOGGER.warning("Player " + player.getName() + " [" + player.getObjectId() + "] sent unknown fee index [" + feeIndex + "].");
			return;
		}
		
		player.addRequest(new AdenLabRequest(player));
		final int currentTranscendLevel = player.getAdenLabCurrentTranscendLevel(bossId);
		final int currentUnlockedPage = player.getAdenLabCurrentlyUnlockedPage(bossId);
		
		// Make sure the player has unlocked the Transcendent stages.
		for (byte pageIndex : AdenLaboratoryData.getInstance().getTranscendentStageIndicesByBossId(bossId))
		{
			if (currentUnlockedPage < pageIndex)
			{
				player.removeRequest(AdenLabRequest.class);
				return;
			}
		}
		
		// Fetch fee items and validate if the player has enough of them
		final Map<Item, Long> feeItemsMap = getFeeItemsFromCache(player, AdenLaboratoryConfig.ADENLAB_INCREDIBLE_ROLL_FEE_TYPE_CACHE, feeIndex);
		if (!feeItemsMap.isEmpty())
		{
			for (Entry<Item, Long> entry : feeItemsMap.entrySet())
			{
				final Item item = entry.getKey();
				final long requiredAmount = entry.getValue();
				if ((item == null) || (item.getCount() < requiredAmount))
				{
					player.sendPacket(ActionFailed.STATIC_PACKET);
					player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
					player.removeRequest(AdenLabRequest.class);
					return;
				}
			}
		}
		else
		{
			player.removeRequest(AdenLabRequest.class);
			player.sendPacket(SystemMessageId.NOT_WORKING_PLEASE_TRY_AGAIN_LATER);
			LOGGER.warning("Missing or incorrectly set incredible stage fees.");
			return;
		}
		
		// Take items and update inventory.
		takeItemsAndUpdateInventory(player, feeItemsMap.isEmpty() ? new HashMap<>() : feeItemsMap, 0);
		
		// TODO: keep an eye out on newer releases since this might change.
		// At the moment we assume that all Transcendent stages have a pageIndex of 25 and optionIndex of 1
		final byte result = calculateSuccess(bossId, (byte) 25, (byte) 1, player, false, 0);
		if (result == 1)
		{
			player.incrementAdenLabTranscendLevel(bossId);
			
			final int newLevel = currentTranscendLevel + 1;
			giveAdenLabSkills(player, bossId, (byte) 25, (byte) 1, newLevel);
			if (newLevel >= 2)
			{
				player.getInventory().applyItemSkills();
				player.getStat().recalculateStats(false);
			}
			
			calculateAdenLabCombatPower(player);
			
			Broadcast.toAllOnlinePlayers(new ExAdenLabTranscendAnnounce(player.getName(), bossId, (byte) newLevel));
		}
		
		player.removeRequest(AdenLabRequest.class);
		player.sendPacket(new ExAdenLabTranscendEnchant(bossId, result));
	}
	
	public static void processTranscendentProbabilities(Player player, byte bossId)
	{
		player.addRequest(new AdenLabRequest(player));
		
		// We assume all Incredible stages have the same index - 25. Else we would have to either create a new cache
		// or loop through all bosses and all pages and check which are of gameType = AdenLabGameType.INCREDIBLE
		final AdenLabHolder adenLabHolder = AdenLaboratoryData.getInstance().getAdenLabDataByPageIndex(bossId, 25);
		if (adenLabHolder == null)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			player.removeRequest(AdenLabRequest.class);
			return;
		}
		
		final List<Integer> options = new ArrayList<>();
		
		// The outer loop is not really necessary but makes it future-proof as it allows for multiple option paths for each page.
		for (Entry<Byte, Map<Byte, List<AdenLabStageHolder>>> optionIndices : adenLabHolder.getOptions().entrySet())
		{
			for (List<AdenLabStageHolder> stages : optionIndices.getValue().values())
			{
				for (AdenLabStageHolder holder : stages)
				{
					final int levelIndex = holder.getStageLevel() - 1;
					final int chance = Math.round(holder.getStageChance() * 10000); // Must always multiply by 10000 because every point = 0.01%
					while (options.size() <= levelIndex)
					{
						options.add(0); // Filling missing indices with 0 as a precaution.
					}
					
					options.set(levelIndex, chance);
				}
			}
		}
		
		player.removeRequest(AdenLabRequest.class);
		player.sendPacket(new ExAdenLabTranscendProb(bossId, options));
	}
	
	/*
	 * * * * * * * * * * * * * * * * * * DAO BLOCK * * * * * * * * * * * * * * * * *
	 */
	public static void ensureAdenLabTableExists()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final DatabaseMetaData metaData = con.getMetaData();
			final ResultSet result = metaData.getTables(null, null, "aden_laboratory", null);
			if (!result.next())
			{
				try (Statement statement = con.createStatement())
				{
					statement.executeUpdate("CREATE TABLE aden_laboratory (" + "charId INT NOT NULL, " + "bossId INT NOT NULL, " + "unlockedPage INT NOT NULL, " + "openedCardsCount INT NOT NULL, " + "specialDrawnOptions TEXT, " + "specialConfirmedOptions TEXT, " + "transcendLevel INT NOT NULL, " + "UNIQUE (charId, bossId) " + ");");
					LOGGER.info("Missing 'aden_laboratory' table was successfully created.");
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Error creating 'aden_laboratory' table.", e);
		}
	}
	
	/**
	 * Save all Aden Lab-related data for a player.
	 * @param player
	 */
	public static void storeAdenLabBossData(Player player)
	{
		for (Entry<Byte, Map<Integer, AdenLabHolder>> entry : AdenLaboratoryData.getInstance().getAllAdenLabData().entrySet())
		{
			final byte bossId = entry.getKey();
			final String drawnOptions = transformSpecialGameOptionsToString(player.getAdenLabSpecialGameStagesDrawnOptions());
			final String confirmedOptions = transformSpecialGameOptionsToString(player.getAdenLabSpecialGameStagesConfirmedOptions());
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement("REPLACE INTO aden_laboratory (charId,bossId,unlockedPage,openedCardsCount,specialDrawnOptions,specialConfirmedOptions,transcendLevel) VALUES (?,?,?,?,?,?,?)"))
			{
				statement.setInt(1, player.getObjectId());
				statement.setInt(2, bossId);
				statement.setInt(3, player.getAdenLabCurrentlyUnlockedPage(bossId));
				statement.setInt(4, player.getAdenLabNormalGameOpenedCardsCount(bossId));
				statement.setString(5, drawnOptions);
				statement.setString(6, confirmedOptions);
				statement.setInt(7, player.getAdenLabCurrentTranscendLevel(bossId));
				statement.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Could not update Aden Lab data for player: " + player.getObjectId(), e);
			}
		}
	}
	
	public static void restorePlayerData(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM aden_laboratory WHERE charId=?"))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet statement = ps.executeQuery())
			{
				while (statement.next())
				{
					final int bossId = statement.getInt("bossId");
					final int unlockedPage = statement.getInt("unlockedPage");
					final int openedCardsCount = statement.getInt("openedCardsCount");
					final String specialDrawnOptions = statement.getString("specialDrawnOptions");
					final String specialConfirmedOptions = statement.getString("specialConfirmedOptions");
					final int transcendLevel = statement.getInt("transcendLevel");
					setPlayerData(player, (byte) bossId, (byte) unlockedPage, openedCardsCount, specialDrawnOptions, specialConfirmedOptions, transcendLevel);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore Aden Lab data for player " + player.getName() + "[" + player.getObjectId() + "].", e);
		}
	}
	
	private static void setPlayerData(Player player, byte bossId, byte unlockedPage, int openedCardsCount, String specialDrawnOptions, String specialConfirmedOptions, int transcendLevel)
	{
		player.setAdenLabCurrentlyUnlockedPage(bossId, unlockedPage);
		player.setAdenLabNormalGameOpenedCardsCount(bossId, openedCardsCount);
		player.setAdenLabSpecialGameDrawnOptionsBulk(getSpecialGameOptionsFromString(specialDrawnOptions));
		player.setAdenLabSpecialGameConfirmedOptionsBulk(getSpecialGameOptionsFromString(specialConfirmedOptions));
		player.setAdenLabCurrentTranscendLevel(bossId, transcendLevel);
	}
	
	public static void deletePlayerData(Player player)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM aden_laboratory WHERE charId=?"))
		{
			statement.setInt(1, player.getObjectId());
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not delete Aden Lab data for player: " + player.getObjectId(), e);
		}
	}
	
	private static String transformSpecialGameOptionsToString(Map<Byte, Map<Byte, Map<Byte, Integer>>> options)
	{
		final StringBuilder sb = new StringBuilder();
		
		for (Entry<Byte, Map<Byte, Map<Byte, Integer>>> bossEntry : options.entrySet())
		{
			byte bossId = bossEntry.getKey();
			
			for (Entry<Byte, Map<Byte, Integer>> pageEntry : bossEntry.getValue().entrySet())
			{
				byte pageIndex = pageEntry.getKey();
				
				for (Entry<Byte, Integer> optionEntry : pageEntry.getValue().entrySet())
				{
					byte optionIndex = optionEntry.getKey();
					int stageLevel = optionEntry.getValue();
					
					// Append the formatted entry
					if (!sb.isEmpty())
					{
						sb.append(";");
					}
					sb.append(bossId).append(",").append(pageIndex).append(",").append(optionIndex).append(",").append(stageLevel);
				}
			}
		}
		
		return sb.toString();
	}
	
	public static Map<Byte, Map<Byte, Map<Byte, Integer>>> getSpecialGameOptionsFromString(String data)
	{
		final Map<Byte, Map<Byte, Map<Byte, Integer>>> options = new HashMap<>();
		if ((data == null) || data.isEmpty())
		{
			return options;
		}
		
		final String[] entries = data.split(";");
		for (String entry : entries)
		{
			String[] parts = entry.split(",");
			
			if (parts.length != 4)
			{
				continue;
			}
			
			try
			{
				byte bossId = Byte.parseByte(parts[0]);
				byte pageIndex = Byte.parseByte(parts[1]);
				byte optionIndex = Byte.parseByte(parts[2]);
				int stageLevel = Integer.parseInt(parts[3]);
				
				options.computeIfAbsent(bossId, k -> new HashMap<>()).computeIfAbsent(pageIndex, k -> new HashMap<>()).put(optionIndex, stageLevel);
				
			}
			catch (NumberFormatException e)
			{
				LOGGER.log(Level.WARNING, "Skipping invalid entry: " + entry);
			}
		}
		
		return options;
	}
}
