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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.data.holders.RelicCompoundFeeHolder;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.data.holders.RelicEnchantHolder;
import org.l2jmobius.gameserver.data.holders.RelicSummonCategoryHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author CostyKiller, Brado
 */
public class RelicData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RelicData.class.getName());
	
	private static final int MAX_ACTIVE_CATEGORIES = 2;
	private static final List<Integer> ENCHANT_CHANCES = new ArrayList<>();
	private static ItemHolder ENCHANT_FEE_HOLDER;
	private static final Map<Integer, RelicSummonCategoryHolder> SUMMON_CATEGORIES = new HashMap<>(4); // Max Size (Client Restriction)
	private static final Set<RelicSummonCategoryHolder> ACTIVE_SUMMON_CATEGORIES = new HashSet<>(MAX_ACTIVE_CATEGORIES); // Max Size (Client Restriction)
	private static final Map<Integer, RelicDataHolder> RELICS = new HashMap<>();
	private static final Map<RelicGrade, Set<RelicDataHolder>> GRADE_RELICS = new HashMap<>();
	private static final Map<RelicGrade, RelicCompoundFeeHolder> GRADE_COMPOUND_FEES = new HashMap<>();
	
	protected RelicData()
	{
		if (RelicSystemConfig.RELIC_SYSTEM_ENABLED)
		{
			load();
		}
	}
	
	@Override
	public void load()
	{
		RELICS.clear();
		
		if (RelicSystemConfig.RELIC_SYSTEM_ENABLED)
		{
			parseDatapackFile("data/RelicData.xml");
			generateGradeRelics();
		}
		
		if (!RELICS.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + RELICS.size() + " relics.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("relicSummonCategoryData".equalsIgnoreCase(d.getNodeName()))
					{
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							if ("summonCategory".equalsIgnoreCase(b.getNodeName()))
							{
								NamedNodeMap attrs = b.getAttributes();
								final int categoryId = parseInteger(attrs, "id");
								final int priceId = parseInteger(attrs, "priceId");
								final long amount = parseLong(attrs, "amount");
								final boolean active = parseBoolean(attrs, "active");
								final int summonCount = parseInteger(attrs, "summonCount");
								final RelicSummonCategoryHolder holder = new RelicSummonCategoryHolder(categoryId, priceId, amount, summonCount);
								SUMMON_CATEGORIES.put(categoryId, holder);
								if (active && (ACTIVE_SUMMON_CATEGORIES.size() < MAX_ACTIVE_CATEGORIES))
								{
									ACTIVE_SUMMON_CATEGORIES.add(holder);
								}
							}
						}
					}
					else if ("relicEnchantData".equalsIgnoreCase(d.getNodeName()))
					{
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							if ("enchantFee".equalsIgnoreCase(b.getNodeName()))
							{
								NamedNodeMap attrs = b.getAttributes();
								final int feeId = parseInteger(attrs, "feeItemId", 57);
								final long feeCount = parseLong(attrs, "feeCount", 100L);
								ENCHANT_FEE_HOLDER = new ItemHolder(feeId, feeCount);
							}
							
							if ("enchantData".equalsIgnoreCase(b.getNodeName()))
							{
								NamedNodeMap attrs = b.getAttributes();
								final String[] chances = parseString(attrs, "chancePerIngredients").split(",");
								for (String chance : chances)
								{
									ENCHANT_CHANCES.add(Integer.parseInt(chance.trim()));
								}
							}
						}
					}
					else if ("relicCombineData".equalsIgnoreCase(d.getNodeName()))
					{
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							if ("combineCategory".equalsIgnoreCase(b.getNodeName()))
							{
								NamedNodeMap attrs = b.getAttributes();
								final RelicGrade grade = RelicGrade.valueOf(parseString(attrs, "grade"));
								final int feeId = parseInteger(attrs, "feeItemId", 57);
								final long feeCount = parseLong(attrs, "feeCount", 100L);
								final RelicCompoundFeeHolder holder = new RelicCompoundFeeHolder(grade, feeId, feeCount);
								GRADE_COMPOUND_FEES.put(grade, holder);
							}
						}
					}
					else if ("relic".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						Node att;
						final StatSet set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final int relicId = parseInteger(attrs, "id");
						final int parentRelicId = parseInteger(attrs, "baseRelicId");
						final RelicGrade grade = RelicGrade.valueOf(parseString(attrs, "grade"));
						final long summonChance = parseLong(attrs, "summonChance");
						final float compoundChanceModifier = parseFloat(attrs, "compoundChanceModifier", 3.3f);
						final float compoundUpGradeChanceModifier = parseFloat(attrs, "compoundUpGradeChanceModifier", 3.3f);
						int enchantLevel = 0;
						int skillId = 0;
						int skillLevel = 0;
						int combatPower = 0;
						List<RelicEnchantHolder> enchantHolder = new ArrayList<>();
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							attrs = b.getAttributes();
							if ("relicStat".equalsIgnoreCase(b.getNodeName()))
							{
								enchantLevel = parseInteger(attrs, "enchantLevel");
								skillId = parseInteger(attrs, "skillId");
								skillLevel = parseInteger(attrs, "skillLevel");
								combatPower = parseInteger(attrs, "combatPower", 0);
								enchantHolder.add(new RelicEnchantHolder(enchantLevel, skillId, skillLevel, combatPower));
							}
						}
						
						final RelicDataHolder template = new RelicDataHolder(relicId, parentRelicId, grade, summonChance, enchantHolder, compoundChanceModifier, compoundUpGradeChanceModifier);
						RELICS.put(relicId, template);
					}
				}
			}
		}
	}
	
	/**
	 * Populates the {@code GRADE_RELICS} map with relic data grouped by their grades.
	 */
	private void generateGradeRelics()
	{
		for (RelicDataHolder holder : RELICS.values())
		{
			Set<RelicDataHolder> existingSet = GRADE_RELICS.get(holder.getGrade());
			if (existingSet == null)
			{
				existingSet = new HashSet<>();
				GRADE_RELICS.put(holder.getGrade(), existingSet);
			}
			
			existingSet.add(holder);
		}
	}
	
	/**
	 * Retrieves all relics of the specified grade.
	 * @param grade the grade of the relics to retrieve
	 * @return a collection of {@code RelicDataHolder} objects of the specified grade, or {@code null} if no relics exist for that grade
	 */
	public Collection<RelicDataHolder> getRelicsByGrade(RelicGrade grade)
	{
		return GRADE_RELICS.get(grade);
	}
	
	/**
	 * Retrieves the relic data associated with a specified relic ID.
	 * @param id the unique ID of the relic to retrieve
	 * @return the {@code RelicDataHolder} containing the details of the relic, or {@code null} if no relic is associated with the specified ID
	 */
	public RelicDataHolder getRelic(int id)
	{
		return RELICS.get(id);
	}
	
	/**
	 * Retrieves the skill ID associated with a specified relic ID.
	 * @param id the unique ID of the relic to retrieve the skill ID for
	 * @param enchant
	 * @return the skill ID of the relic, or {@code 0} if no relic is associated with the specified ID
	 */
	public int getRelicSkillId(int id, int enchant)
	{
		return RELICS.get(id).getEnchantHolderByEnchant(enchant).getSkillId();
	}
	
	/**
	 * Retrieves the skill level associated with a specified relic ID.
	 * @param id the unique ID of the relic to retrieve the skill level for
	 * @param enchant
	 * @return the skill level of the relic, or {@code 0} if no relic is associated with the specified ID
	 */
	public int getRelicSkillLevel(int id, int enchant)
	{
		return RELICS.get(id).getEnchantHolderByEnchant(enchant).getSkillLevel();
	}
	
	/**
	 * Retrieves the compound fee holder for the specified relic grade.
	 * @param grade the grade of the relic
	 * @return the {@code RelicCompoundFeeHolder} associated with the grade, or {@code null} if no fee holder is defined for that grade
	 */
	public RelicCompoundFeeHolder getCompoundFeeHolderByGrade(RelicGrade grade)
	{
		return GRADE_COMPOUND_FEES.get(grade);
	}
	
	/**
	 * Retrieves a collection of all available relic data.
	 * @return a collection of {@code RelicDataHolder} objects representing all relics
	 */
	public Collection<RelicDataHolder> getRelics()
	{
		return RELICS.values();
	}
	
	/**
	 * Retrieves the relic summon categories.
	 * @return a map of category IDs to {@code RelicSummonCategoryHolder} objects
	 */
	public Map<Integer, RelicSummonCategoryHolder> getRelicSummonCategories()
	{
		return SUMMON_CATEGORIES;
	}
	
	/**
	 * Retrieves the active relic summon categories.
	 * @return a collection of {@code RelicSummonCategoryHolder} objects representing active summon categories
	 */
	public Collection<RelicSummonCategoryHolder> getRelicActiveCategories()
	{
		return ACTIVE_SUMMON_CATEGORIES;
	}
	
	/**
	 * Retrieves a specific relic summon category by its ID.
	 * @param categoryId the ID of the summon category
	 * @return the {@code RelicSummonCategoryHolder} object, or {@code null} if no category exists for the specified ID
	 */
	public RelicSummonCategoryHolder getRelicSummonCategory(int categoryId)
	{
		return SUMMON_CATEGORIES.get(categoryId);
	}
	
	/**
	 * Retrieves the enchant fee details.
	 * @return the {@code ItemHolder} containing enchant fee information
	 */
	public ItemHolder getEnchantFee()
	{
		return ENCHANT_FEE_HOLDER;
	}
	
	/**
	 * Retrieves the enchant rate based on the count of ingredients.
	 * @param count the number of ingredients
	 * @return the enchant rate for the given ingredient count
	 */
	public int getEnchantRateByIngredientCount(int count)
	{
		return ENCHANT_CHANCES.get(count - 1);
	}
	
	/**
	 * Retrieves a relic ID for summoning based on weighted random selection.
	 * @return the relic ID selected for summoning, or {@code 0} if no valid relics are available
	 */
	public int getRelicBySummon()
	{
		final List<RelicDataHolder> relics = RelicData.getInstance().getRelics().stream().filter(relic -> relic.getSummonChance() > 0).toList();
		final long totalWeight = relics.stream().mapToLong(RelicDataHolder::getSummonChance).sum();
		if (totalWeight <= 0)
		{
			LOGGER.warning("No valid relics available for summoning.");
			return 0;
		}
		
		final long rng = Rnd.get(totalWeight);
		long cumulativeWeight = 0;
		for (RelicDataHolder relic : relics)
		{
			cumulativeWeight += relic.getSummonChance();
			if (rng < cumulativeWeight)
			{
				return relic.getRelicId();
			}
		}
		
		return 0;
	}
	
	/**
	 * Retrieves a relic for compounding based on its grade and calculates whether the operation succeeds.
	 * @param grade the grade of the relic
	 * @return an {@code Entry} containing a success flag and the ID of the resulting relic
	 */
	public Entry<Boolean, Integer> getRelicByCompound(RelicGrade grade)
	{
		final RelicGrade successGrade = RelicGrade.values()[grade.ordinal() + 1];
		final long successWeight = getRelicsByGrade(successGrade).stream().mapToLong(relic ->
		{
			final float div = Math.max(1.0f, relic.getCompoundUpGradeChanceModifier());
			final long summonChance = relic.getSummonChance() / 100_000_000L;
			final long scaledSummonChance = Math.max(1, summonChance);
			return Math.max(1, (long) (scaledSummonChance / div));
		}).sum();
		if (successWeight <= 0)
		{
			return new SimpleEntry<>(false, 0);
		}
		
		final int normalizedSuccessWeight = (int) Math.min(100, successWeight);
		final boolean success = Rnd.get(100) < normalizedSuccessWeight;
		final RelicGrade resultGrade = success ? successGrade : grade;
		final Collection<RelicDataHolder> relics = getRelicsByGrade(resultGrade);
		if (relics.isEmpty())
		{
			return new SimpleEntry<>(success, 0);
		}
		
		final long totalWeight = Math.max(1, relics.stream().mapToLong(relic -> calculateCompoundChance(relic.getRelicId(), grade)).sum());
		final long relicRng = Rnd.get(totalWeight);
		long cumulativeWeight = 0;
		final List<RelicDataHolder> relicList = new ArrayList<>(relics);
		for (RelicDataHolder relic : relicList)
		{
			cumulativeWeight += calculateCompoundChance(relic.getRelicId(), grade);
			if (relicRng < cumulativeWeight)
			{
				return new SimpleEntry<>(success, relic.getRelicId());
			}
		}
		
		final RelicDataHolder randomRelic = relicList.get(Rnd.get(relicList.size() - 1)); // ERROR
		return new SimpleEntry<>(success, randomRelic.getRelicId());
	}
	
	/**
	 * Calculates the compound chance for a relic based on its ID and grade.
	 * @param relicId the ID of the relic
	 * @param grade the grade of the relic
	 * @return the calculated compound chance as a long value
	 */
	public long calculateCompoundChance(int relicId, RelicGrade grade)
	{
		final RelicDataHolder relic = RelicData.getInstance().getRelic(relicId);
		final float div = relic.getGrade() == grade ? relic.getCompoundChanceModifier() : relic.getCompoundUpGradeChanceModifier();
		final long combineChance = relic.getSummonChance() == 0 ? (relic.getGrade() == grade ? 1000000000L : 100000000L) : relic.getSummonChance();
		return new BigDecimal((combineChance / div) / 100000000f).setScale(4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100000000)).longValue();
	}
	
	/**
	 * Generates a list of relics obtained through summoning.
	 * @param summonCount the number of relics to summon
	 * @return a list of relic IDs obtained through summoning
	 */
	public List<Integer> generateSummonRelics(int summonCount)
	{
		final List<Integer> relics = new ArrayList<>();
		for (int i = 1; i <= summonCount; i++)
		{
			final int obtainedRelicId = getRelicBySummon();
			relics.add(obtainedRelicId);
		}
		
		return relics;
	}
	
	public List<RelicDataHolder> getRelicsByParentId(int parentRelicId)
	{
		final List<RelicDataHolder> relics = new ArrayList<>();
		for (RelicDataHolder relic : RELICS.values())
		{
			if (relic.getParentRelicId() == parentRelicId)
			{
				relics.add(relic);
			}
		}
		
		return relics;
	}
	
	public static RelicData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RelicData INSTANCE = new RelicData();
	}
}
