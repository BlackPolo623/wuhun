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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.holders.RelicCouponHolder;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.model.actor.enums.player.RelicGrade;

/**
 * @author Brado
 */
public class RelicCouponData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RelicCouponData.class.getName());
	
	private static final Map<Integer, RelicCouponHolder> RELIC_COUPONS = new HashMap<>();
	private static final Map<Integer, LinkedHashMap<Integer, Long>> CACHED_CHANCES = new LinkedHashMap<>();
	
	protected RelicCouponData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		RELIC_COUPONS.clear();
		parseDatapackFile("data/RelicCouponData.xml");
		cacheChances();
		if (!RELIC_COUPONS.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + RELIC_COUPONS.size() + " relic coupon data.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "coupon", couponNode ->
		{
			final Element couponElement = (Element) couponNode;
			final int itemId = Integer.parseInt(couponElement.getAttribute("itemId"));
			if (ItemData.getInstance().getTemplate(itemId) == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Could not find coupon with item id " + itemId + ".");
				return;
			}
			
			final int summonCount = couponElement.hasAttribute("summonCount") ? Integer.parseInt(couponElement.getAttribute("summonCount")) : 1;
			if (couponElement.hasAttribute("relicId"))
			{
				final int relicId = Integer.parseInt(couponElement.getAttribute("relicId"));
				RELIC_COUPONS.put(itemId, new RelicCouponHolder(itemId, relicId, summonCount));
			}
			else
			{
				Map<RelicGrade, Integer> groups = new HashMap<>();
				Set<Integer> disabledDollsIds = new HashSet<>();
				Map<Integer, Integer> chanceRolls = new HashMap<>();
				forEach(couponNode, "disabledDolls", disabledDollsNode ->
				{
					forEach(disabledDollsNode, "disabled", disabledNode ->
					{
						int id = Integer.parseInt(((Element) disabledNode).getAttribute("id"));
						disabledDollsIds.add(id);
					});
				});
				
				forEach(couponNode, "chanceGroups", groupChanceNode ->
				{
					forEach(groupChanceNode, "group", groupNode ->
					{
						final Element groupElement = (Element) groupNode;
						final RelicGrade grade = RelicGrade.valueOf(groupElement.getAttribute("grade"));
						final int chance = Integer.parseInt(groupElement.getAttribute("chance"));
						groups.put(grade, chance);
					});
				});
				
				if (!groups.isEmpty())
				{
					RELIC_COUPONS.put(itemId, new RelicCouponHolder(itemId, summonCount, groups, disabledDollsIds));
				}
				else
				{
					forEach(couponNode, "chanceRollGroups", chanceRollGroupsNode ->
					{
						forEach(chanceRollGroupsNode, "chanceRoll", chanceRollNode ->
						{
							final Element chanceRollElement = (Element) chanceRollNode;
							final int dollId = Integer.parseInt(chanceRollElement.getAttribute("dollId"));
							final int chance = Integer.parseInt(chanceRollElement.getAttribute("chance"));
							chanceRolls.put(dollId, chance);
						});
					});
				}
				
				if (!chanceRolls.isEmpty())
				{
					RELIC_COUPONS.put(itemId, new RelicCouponHolder(itemId, summonCount, chanceRolls));
				}
			}
		}));
	}
	
	public RelicCouponHolder getCouponFromCouponItemId(int itemId)
	{
		return RELIC_COUPONS.get(itemId);
	}
	
	public int getRelicIdByCouponItemId(int itemId)
	{
		return RELIC_COUPONS.get(itemId) != null ? RELIC_COUPONS.get(itemId).getRelicId() : 0;
	}
	
	private int getRelicIdFromSummon(RelicCouponHolder coupon)
	{
		if (coupon.getRelicId() != 0)
		{
			return coupon.getRelicId();
		}
		
		final Map<RelicGrade, Integer> grades = new HashMap<>();
		grades.putAll(coupon.getCouponRelicGrades());
		final int MAX_ATTEMPTS = 1000;
		int curAttempt = 0;
		if (!grades.isEmpty())
		{
			RelicGrade rolledGrade = null;
			while (rolledGrade == null)
			{
				curAttempt++;
				for (Entry<RelicGrade, Integer> entry : grades.entrySet())
				{
					if ((Math.max(1, entry.getValue()) < Rnd.get(100)) || (curAttempt > MAX_ATTEMPTS))
					{
						rolledGrade = entry.getKey();
						break;
					}
				}
			}
			
			final List<RelicDataHolder> relicsByGrade = new ArrayList<>();
			relicsByGrade.addAll(RelicData.getInstance().getRelicsByGrade(rolledGrade).stream().filter(r -> !coupon.getDisabledIds().contains(r.getRelicId())).collect(Collectors.toList()));
			final long totalWeight = relicsByGrade.stream().mapToLong(RelicDataHolder::getSummonChance).sum();
			if (totalWeight <= 0)
			{
				LOGGER.warning("No valid relics available for summoning with coupon " + coupon.getItemId());
				return 0;
			}
			
			final long rng = Rnd.get(totalWeight);
			long cumulativeWeight = 0;
			for (RelicDataHolder relic : relicsByGrade)
			{
				cumulativeWeight += relic.getSummonChance();
				if (rng < cumulativeWeight)
				{
					return relic.getRelicId();
				}
			}
		}
		
		if (!coupon.getChanceRolls().isEmpty())
		{
			curAttempt = 0;
			final Map<Integer, Integer> chanceRolls = new HashMap<>();
			chanceRolls.putAll(chanceRolls);
			while (true)
			{
				curAttempt++;
				for (Entry<Integer, Integer> entry : chanceRolls.entrySet())
				{
					if ((entry.getValue() < Rnd.get(100)) || (curAttempt > MAX_ATTEMPTS))
					{
						return entry.getKey();
					}
				}
			}
		}
		
		return 0;
	}
	
	private void cacheChances()
	{
		CACHED_CHANCES.clear();
		for (RelicCouponHolder holder : RELIC_COUPONS.values())
		{
			final Map<Integer, Long> results = new HashMap<>();
			if (holder.getRelicId() != 0)
			{
				results.put(holder.getRelicId(), 10_000_000_000L);
			}
			
			final Map<RelicGrade, Integer> grades = new HashMap<>();
			grades.putAll(holder.getCouponRelicGrades());
			if (!grades.isEmpty())
			{
				final Map<Integer, Integer> tempResults = new HashMap<>();
				for (Entry<RelicGrade, Integer> entry : grades.entrySet())
				{
					for (RelicDataHolder rh : RelicData.getInstance().getRelicsByGrade(entry.getKey()).stream().filter(r -> !holder.getDisabledIds().contains(r.getRelicId())).collect(Collectors.toList()))
					{
						tempResults.put(rh.getRelicId(), entry.getValue());
					}
				}
				
				for (Entry<Integer, Integer> entry : tempResults.entrySet())
				{
					results.put(entry.getKey(), (entry.getValue() * 100_000_000L) / tempResults.size());
				}
			}
			
			if (!holder.getChanceRolls().isEmpty())
			{
				final Map<Integer, Integer> chanceRolls = new HashMap<>();
				chanceRolls.putAll(chanceRolls);
				for (Entry<Integer, Integer> entry : chanceRolls.entrySet())
				{
					results.put(entry.getKey(), entry.getValue() * 10_000_000L);
				}
			}
			
			LinkedHashMap<Integer, Long> sortedRelics = results.entrySet().stream().sorted(Map.Entry.<Integer, Long> comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, _) -> e1, LinkedHashMap::new));
			CACHED_CHANCES.put(holder.getItemId(), sortedRelics);
		}
		
	}
	
	public Map<Integer, Long> getCachedChances(int itemId)
	{
		return CACHED_CHANCES.get(itemId);
	}
	
	public List<Integer> generateSummonRelics(RelicCouponHolder coupon)
	{
		final List<Integer> relics = new ArrayList<>();
		for (int i = 1; i <= coupon.getRelicSummonCount(); i++)
		{
			final int obtainedRelicId = getRelicIdFromSummon(coupon);
			relics.add(obtainedRelicId);
		}
		
		return relics;
	}
	
	public static RelicCouponData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RelicCouponData INSTANCE = new RelicCouponData();
	}
}
