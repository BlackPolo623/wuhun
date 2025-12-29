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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.enums.AdenLabGameType;
import org.l2jmobius.gameserver.data.holders.AdenLabHolder;
import org.l2jmobius.gameserver.data.holders.AdenLabSkillHolder;
import org.l2jmobius.gameserver.data.holders.AdenLabStageHolder;
import org.l2jmobius.gameserver.managers.AdenLaboratoryManager;
import org.l2jmobius.gameserver.model.StatSet;

/**
 * @author SaltyMike
 */
public class AdenLaboratoryData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(AdenLaboratoryData.class.getName());
	
	/**
	 * @struct: [bossId, [pageIndex, AdenLabHolder]]
	 */
	private final Map<Byte, Map<Integer, AdenLabHolder>> _adenLabData = new HashMap<>(); // <bossId <pageIndex, holder>>
	
	/**
	 * @struct: [_bossId, [_pageIndex, [_optionIndex, [_stageLevel, List_AdenLabSkillHolder]]]
	 */
	private final Map<Byte, Map<Byte, Map<Byte, Map<Byte, List<AdenLabSkillHolder>>>>> _skillsLookupTable = new HashMap<>();
	
	/**
	 * @struct: [_bossId, [_highestPageIndex, [_skill_ID, _skill_LVL]]]
	 */
	private final Map<Byte, Map<Byte, List<int[]>>> _normalStageSkillsUntilSpecificPageSnapshot = new HashMap<>();
	
	/**
	 * @struct: [_bossId, Map[_pageIndex, _combatPower]]
	 */
	private final Map<Byte, Map<Byte, Integer>> _specialStagesAndCombatPower = new ConcurrentHashMap<>();
	
	/**
	 * @struct: [_bossId, Map[_pageIndex, Map[_stageLevel, _combatPower]]]
	 */
	private final Map<Byte, Map<Byte, Map<Byte, Integer>>> _transcendentStagesAndCombatPower = new ConcurrentHashMap<>();
	
	/**
	 * @struct: [_bossId, Map[_pageIndex, List[_int[_ID, _LEVEL]]]]
	 */
	// private final Map<Byte, Map<Byte, List<int[]>>> _transcendentSkills = new HashMap<>();
	/**
	 * @struct: [_bossId, [_highestPageIndex, [_skill_ID, _skill_LVL]]]
	 */
	// private final Map<Byte, Map<Byte, List<int[]>>> _specialStageSkillsUntilSpecificPageSnapshot = new HashMap<>();
	
	protected AdenLaboratoryData()
	{
	}
	
	public void reload()
	{
		LOGGER.info(getClass().getSimpleName() + ": reload initiated.");
		load();
		LOGGER.info(getClass().getSimpleName() + ": reload completed.");
	}
	
	@Override
	public void load()
	{
		_adenLabData.clear();
		_skillsLookupTable.clear();
		_specialStagesAndCombatPower.clear();
		_transcendentStagesAndCombatPower.clear();
		_normalStageSkillsUntilSpecificPageSnapshot.clear();
		parseDatapackFile("data/AdenLaboratoryData.xml");
		
		initializeNormalSkillsCache();
		// initializeSpecialSkillsCache();
		
		final int bossCount = _normalStageSkillsUntilSpecificPageSnapshot.size();
		byte specialStages = 0;
		for (byte i = 1; i <= bossCount; i++)
		{
			specialStages += (byte) getSpecialStageIndicesByBossId(i).size();
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + bossCount + " boss" + (bossCount > 1 ? "es" : "") + " and " + ((AdenLaboratoryManager.getTotalCount(_normalStageSkillsUntilSpecificPageSnapshot, true) - 1) + specialStages) + " stages.");
		
		AdenLaboratoryManager.ensureAdenLabTableExists();
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (IXmlReader.isNode(n))
			{
				if ("list".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node bossNode = n.getFirstChild(); bossNode != null; bossNode = bossNode.getNextSibling())
					{
						if (IXmlReader.isNode(bossNode))
						{
							if ("boss".equalsIgnoreCase(bossNode.getNodeName()))
							{
								final Node bossAttributes = bossNode.getAttributes().getNamedItem("index");
								final int bossId = parseInt(bossAttributes, -1);
								
								for (Node gameTypeNode = bossNode.getFirstChild(); gameTypeNode != null; gameTypeNode = gameTypeNode.getNextSibling())
								{
									if (IXmlReader.isNode(gameTypeNode))
									{
										if ("game".equalsIgnoreCase(gameTypeNode.getNodeName()))
										{
											final Node gameTypeAttributes = gameTypeNode.getAttributes().getNamedItem("type");
											final AdenLabGameType gameType = parseEnum(gameTypeAttributes, AdenLabGameType.class, AdenLabGameType.NORMAL);
											
											for (Node pageNode = gameTypeNode.getFirstChild(); pageNode != null; pageNode = pageNode.getNextSibling())
											{
												if (IXmlReader.isNode(pageNode))
												{
													if ("page".equalsIgnoreCase(pageNode.getNodeName()))
													{
														final AdenLabHolder adenLabHolder = new AdenLabHolder();
														final NamedNodeMap pageAttributes = pageNode.getAttributes();
														
														// game type - Normal, Special, Incredible
														adenLabHolder.setBossId((byte) bossId);
														adenLabHolder.setGameType(gameType);
														
														if (!handlePageIndex(pageAttributes, adenLabHolder) || !handleCardCount(pageAttributes, adenLabHolder) || !handleSuccessRate(pageAttributes, adenLabHolder))
														{
															return;
														}
														
														final int firstSkillId = parseInteger(pageAttributes, "primarySkillId", -1);
														if (firstSkillId == -1)
														{
															LOGGER.warning("Missing or incorrectly set `primarySkillId` attribute for page index " + adenLabHolder.getPageIndex());
														}
														
														final int secondSkillId = parseInteger(pageAttributes, "secondarySkillId", -1);
														
														// maxLevel is currently not in use.
														// int maxLevel = parseInteger(pageAttributes, "maxLevel", -1);
														int firstSkillLevel;
														int secondSkillLevel;
														
														byte optionIndex = 0;
														for (Node probabilitiesNode = pageNode.getFirstChild(); probabilitiesNode != null; probabilitiesNode = probabilitiesNode.getNextSibling())
														{
															if (IXmlReader.isNode(probabilitiesNode))
															{
																if ("options".equalsIgnoreCase(probabilitiesNode.getNodeName()))
																{
																	optionIndex++;
																	for (Node stageNode = probabilitiesNode.getFirstChild(); stageNode != null; stageNode = stageNode.getNextSibling())
																	{
																		if (IXmlReader.isNode(stageNode))
																		{
																			if ("stage".equalsIgnoreCase(stageNode.getNodeName()))
																			{
																				final StatSet stageAttributes = new StatSet(parseAttributes(stageNode));
																				
																				final AdenLabStageHolder stageHolder = new AdenLabStageHolder();
																				final AdenLabSkillHolder skillHolder = new AdenLabSkillHolder();
																				
																				final int stageLevel = stageAttributes.getInt("level", -1);
																				if (stageLevel != -1)
																				{
																					stageHolder.setStageLevel(stageLevel);
																				}
																				else
																				{
																					LOGGER.warning("Missing or incorrectly set `level` attribute in the `stage` element.");
																				}
																				
																				final float stageChance = stageAttributes.getFloat("chance", -1f);
																				if (stageChance != -1f)
																				{
																					stageHolder.setStageChance(stageChance);
																				}
																				else
																				{
																					LOGGER.warning("Missing or incorrectly set `chance` attribute in the `stage` element.");
																				}
																				
																				firstSkillLevel = stageAttributes.getInt("primarySkillLevel", -1);
																				if (firstSkillLevel != -1)
																				{
																					skillHolder.setId(firstSkillId);
																					skillHolder.setLvl(firstSkillLevel);
																					
																					stageHolder.addSkill(skillHolder);
																					
																					// add to cache too
																					AdenLaboratoryManager.addSkillToCache(bossId, adenLabHolder.getPageIndex(), optionIndex, stageLevel, new int[]
																					{
																						firstSkillId,
																						firstSkillLevel
																					});
																				}
																				else
																				{
																					LOGGER.warning("Missing or incorrectly set `primarySkillLevel` attribute in the `stage` element.");
																				}
																				
																				if (gameType != AdenLabGameType.NORMAL)
																				{
																					final int combatPower = stageAttributes.getInt("combatPower", 0);
																					if (combatPower > 0)
																					{
																						stageHolder.setCombatPower(combatPower);
																						
																						final byte pageIndex = (byte) adenLabHolder.getPageIndex();
																						if (gameType == AdenLabGameType.SPECIAL)
																						{
																							// we assume all stages give the same amount of Combat Power, so we only need to know the page index
																							_specialStagesAndCombatPower.computeIfAbsent((byte) bossId, k -> new HashMap<>()).put(pageIndex, combatPower);
																						}
																						else if (gameType == AdenLabGameType.INCREDIBLE)
																						{
																							_transcendentStagesAndCombatPower.computeIfAbsent((byte) bossId, k -> new HashMap<>()).computeIfAbsent(pageIndex, k -> new HashMap<>()).put((byte) stageLevel, combatPower);
																						}
																					}
																					
																					secondSkillLevel = stageAttributes.getInt("secondarySkillLevel", -1);
																					if (secondSkillLevel != -1)
																					{
																						final AdenLabSkillHolder skillHolder2 = new AdenLabSkillHolder();
																						skillHolder2.setId(secondSkillId);
																						skillHolder2.setLvl(secondSkillLevel);
																						
																						stageHolder.addSkill(skillHolder2);
																						
																						// add to cache too
																						AdenLaboratoryManager.addSkillToCache(bossId, adenLabHolder.getPageIndex(), optionIndex, stageLevel, new int[]
																						{
																							secondSkillId,
																							secondSkillLevel
																						});
																					}
																				}
																				
																				adenLabHolder.addStage(optionIndex, (byte) stageLevel, stageHolder);
																			}
																		}
																	}
																}
															}
														}
														
														_adenLabData.computeIfAbsent((byte) bossId, k -> new HashMap<>()) // Get existing or create new map for bossId
															.put(adenLabHolder.getPageIndex(), adenLabHolder); // Add/update the pageIndex entry
													}
													else
													{
														LOGGER.warning("Missing or incorrectly set `page` element in data/AdenLabData.xml.");
													}
												}
											}
										}
										else
										{
											LOGGER.warning("Missing or incorrectly set `game` element in data/AdenLabData.xml.");
										}
									}
								}
							}
							else
							{
								LOGGER.warning("Missing or incorrectly set `boss` element in data/AdenLabData.xml.");
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * @return [_bossId, (_pageIndex, _holder)]
	 */
	public Map<Byte, Map<Integer, AdenLabHolder>> getAllAdenLabData()
	{
		return _adenLabData;
	}
	
	public Map<Integer, AdenLabHolder> getAdenLabData(byte bossId)
	{
		return _adenLabData.get(bossId);
	}
	
	public AdenLabHolder getAdenLabDataByPageIndex(int bossId, int pageIndex)
	{
		final Map<Integer, AdenLabHolder> bossData = getAdenLabData((byte) bossId);
		if (bossData == null)
		{
			LOGGER.warning("AdenLabData: No data found for bossId " + bossId);
			return null;
		}
		
		AdenLabHolder adenLabHolder = bossData.get(pageIndex);
		
		if (adenLabHolder == null)
		{
			LOGGER.warning("AdenLabData: No data found for pageIndex " + pageIndex + " under bossId " + bossId);
		}
		
		return adenLabHolder;
	}
	
	public Map<Byte, Map<Byte, Map<Byte, Map<Byte, List<AdenLabSkillHolder>>>>> getSkillsLookupTable()
	{
		return _skillsLookupTable;
	}
	
	/**
	 * - Returns a nested map with key <optionIndex> and value Map<StageLevel, SkillHolder>
	 * @param bossId
	 * @param pageIndex
	 * @return
	 */
	public Map<Byte, Map<Byte, List<AdenLabSkillHolder>>> getSkillsLookupTableByBossAndPageIndex(byte bossId, byte pageIndex)
	{
		return _skillsLookupTable.containsKey(bossId) ? _skillsLookupTable.get(bossId).getOrDefault(pageIndex, Collections.emptyMap()) : Collections.emptyMap();
	}
	
	/**
	 * - Returns a map containing Stage Level + SkillHolder
	 * @param bossId
	 * @param pageIndex
	 * @param optionIndex
	 * @return
	 */
	public Map<Byte, List<AdenLabSkillHolder>> getSkillsByOptionIndex(byte bossId, byte pageIndex, byte optionIndex)
	{
		return (_skillsLookupTable.containsKey(bossId) && _skillsLookupTable.get(bossId).containsKey(pageIndex)) ? _skillsLookupTable.get(bossId).get(pageIndex).get(optionIndex) : null;
	}
	
	protected boolean handlePageIndex(NamedNodeMap attributes, AdenLabHolder adenLabHolder)
	{
		final byte pageIndex = parseByte(attributes, "index", (byte) -1);
		if (pageIndex == -1)
		{
			LOGGER.warning("Missing or incorrectly set `pageIndex` attribute in the `page` element.");
			return false;
		}
		
		if (adenLabHolder.getGameType() == AdenLabGameType.SPECIAL)
		{
			_specialStagesAndCombatPower.computeIfAbsent(adenLabHolder.getBossId(), k -> new ConcurrentHashMap<>()).putIfAbsent(pageIndex, 0);
			// LOGGER.info("Special stage: Boss " + adenLabHolder.getBossId() + " -> Page " + pageIndex);
		}
		else if (adenLabHolder.getGameType() == AdenLabGameType.INCREDIBLE)
		{
			_transcendentStagesAndCombatPower.computeIfAbsent(adenLabHolder.getBossId(), k -> new ConcurrentHashMap<>()).computeIfAbsent(pageIndex, l -> new ConcurrentHashMap<>());
			// LOGGER.info("Transcend stage: Boss " + adenLabHolder.getBossId() + " -> Page " + pageIndex);
		}
		// else
		// {
		// LOGGER.info("Normal stage: Boss " + adenLabHolder.getBossId() + " -> Page " + pageIndex);
		// }
		
		adenLabHolder.setPageIndex(pageIndex);
		return true;
	}
	
	protected boolean handleCardCount(NamedNodeMap attributes, AdenLabHolder adenLabHolder)
	{
		final byte cardCount = parseByte(attributes, "cardCount", (byte) -1);
		adenLabHolder.setCardCount(cardCount);
		if ((adenLabHolder.getGameType() == AdenLabGameType.NORMAL) && (cardCount == -1))
		{
			LOGGER.warning("Missing `cardCount` value for page index " + adenLabHolder.getPageIndex() + ". You better fix it ASAP, because it will break things. ;)");
			return false;
		}
		
		return true;
	}
	
	protected boolean handleSuccessRate(NamedNodeMap attributes, AdenLabHolder adenLabHolder)
	{
		final float successRateAttribute = parseFloat(attributes, "successRate", -1f);
		if (successRateAttribute == -1f)
		{
			LOGGER.warning("Missing or incorrectly set `successRate` for page index " + adenLabHolder.getPageIndex() + ". Assigning default value: -1f");
			return false;
		}
		
		adenLabHolder.setGameSuccessRate(successRateAttribute);
		return true;
	}
	
	public void initializeNormalSkillsCache()
	{
		for (Entry<Byte, Map<Byte, Map<Byte, Map<Byte, List<AdenLabSkillHolder>>>>> bossEntry : _skillsLookupTable.entrySet())
		{
			final byte bossId = bossEntry.getKey();
			final Map<Byte, Map<Byte, Map<Byte, List<AdenLabSkillHolder>>>> bossSkills = bossEntry.getValue();
			
			final Map<Byte, List<int[]>> pageCache = new HashMap<>();
			final List<int[]> accumulatedSkills = new ArrayList<>();
			for (byte pageIndex : bossSkills.keySet().stream().sorted(AdenLaboratoryManager::sortingComparator).toList())
			{
				// Skip caching if stage is of type SPECIAL or TRANSCENDENT.
				if (getSpecialStageIndicesByBossId(bossId).contains(pageIndex) || getTranscendentStageIndicesByBossId(bossId).contains(pageIndex))
				{
					continue;
				}
				
				final Map<Byte, Map<Byte, List<AdenLabSkillHolder>>> pageSkills = bossSkills.get(pageIndex);
				for (Entry<Byte, Map<Byte, List<AdenLabSkillHolder>>> optionEntry : pageSkills.entrySet())
				{
					// final byte optionIndex = optionEntry.getKey();
					final Map<Byte, List<AdenLabSkillHolder>> stageMap = optionEntry.getValue();
					for (List<AdenLabSkillHolder> holderList : stageMap.values())
					{
						for (AdenLabSkillHolder skill : holderList)
						{
							accumulatedSkills.add(new int[]
							{
								skill.getId(),
								skill.getLvl()
							});
						}
					}
				}
				
				pageCache.put(pageIndex, new ArrayList<>(accumulatedSkills));
			}
			
			_normalStageSkillsUntilSpecificPageSnapshot.put(bossId, pageCache);
		}
	}
	
	/*
	 * public void initializeSpecialSkillsCache() { for (Entry<Byte, Map<Byte, Map<Byte, Map<Byte, List<AdenLabSkillHolder>>>>> bossEntry : _skillsLookupTable.entrySet()) { byte bossId = bossEntry.getKey(); Map<Byte, Map<Byte, Map<Byte, List<AdenLabSkillHolder>>>> bossSkills = bossEntry.getValue();
	 * Map<Byte, List<int[]>> pageCache = new HashMap<>(); List<int[]> accumulatedSkills = new ArrayList<>(); for (byte pageIndex : bossSkills.keySet().stream().sorted(AdenLabUtils::sortingComparator).toList()) { // skip caching if stage type isn't SPECIAL if
	 * (!_specialStagesIndexList.containsKey(pageIndex)) { continue; } Map<Byte, Map<Byte, List<AdenLabSkillHolder>>> pageSkills = bossSkills.get(pageIndex); for (Entry<Byte, Map<Byte, List<AdenLabSkillHolder>>> optionEntry : pageSkills.entrySet()) { byte optionIndex = optionEntry.getKey(); // Track
	 * the optionIndex Map<Byte, List<AdenLabSkillHolder>> stageMap = optionEntry.getValue(); for (List<AdenLabSkillHolder> holderList : stageMap.values()) { for (AdenLabSkillHolder skill : holderList) { accumulatedSkills.add(new int[]{skill.getId(), skill.getLvl()}); } } } // Store a **copy** of
	 * accumulated skills to prevent mutation issues pageCache.put(pageIndex, new ArrayList<>(accumulatedSkills)); } _specialStageSkillsUntilSpecificPageSnapshot.put(bossId, pageCache); } }
	 */
	
	/**
	 * @param bossId
	 * @param pageIndex
	 * @return Index 0 = Skill ID <br>
	 *         Index 1 = Skill Level
	 */
	public List<int[]> getNormalStageSkillsUpToPage(byte bossId, byte pageIndex)
	{
		final Map<Byte, List<int[]>> pageCache = _normalStageSkillsUntilSpecificPageSnapshot.getOrDefault(bossId, Collections.emptyMap());
		if (pageCache.isEmpty())
		{
			return Collections.emptyList();
		}
		
		// Find the highest key that is less than or equal to the requested pageIndex.
		final byte highestAvailablePage = pageCache.keySet().stream().filter(index -> index <= pageIndex).max(Byte::compare).orElse((byte) -1); // Default to -1 if no valid page is found
		
		return highestAvailablePage == -1 ? Collections.emptyList() : pageCache.get(highestAvailablePage);
	}
	
	public List<Byte> getSpecialStageIndicesByBossId(byte bossId)
	{
		return new ArrayList<>(_specialStagesAndCombatPower.getOrDefault(bossId, Collections.emptyMap()).keySet());
	}
	
	public int getSpecialStageCombatPower(byte bossId, byte pageIndex)
	{
		return _specialStagesAndCombatPower.get(bossId).get(pageIndex);
	}
	
	public List<Byte> getTranscendentStageIndicesByBossId(byte bossId)
	{
		return new ArrayList<>(_transcendentStagesAndCombatPower.getOrDefault(bossId, Collections.emptyMap()).keySet());
	}
	
	public int getTranscendentCombatPower(byte bossId, byte pageIndex, byte currentTranscendLevel)
	{
		return _transcendentStagesAndCombatPower.getOrDefault(bossId, Collections.emptyMap()).getOrDefault(pageIndex, Collections.emptyMap()).getOrDefault(currentTranscendLevel, 0);
	}
	
	public static AdenLaboratoryData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AdenLaboratoryData INSTANCE = new AdenLaboratoryData();
	}
}
