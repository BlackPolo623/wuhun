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
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * 精煉系統 XML 設定讀取器。
 * 讀取 data/scripts/custom/RefineSystem/RefineSystemData.xml。
 */
public class RefineSystemData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RefineSystemData.class.getName());

	public enum ValueType
	{
		PERCENT, // raw/100.0 → % 數字，消費端 getValue，公式含 value/100
		FLAT,    // raw       → 整數，消費端 getValue，直接加到屬性
		MUL      // raw/10000.0 → 倍率增量，消費端 getMul
	}

	public static class SeriesEntry
	{
		public final int base;
		public final String name;
		public final Stat stat;
		public final int weight;
		public final ValueType valueType;
		public final List<RangeEntry> ranges = new ArrayList<>();

		public SeriesEntry(int base, String name, Stat stat, int weight, ValueType valueType)
		{
			this.base = base;
			this.name = name;
			this.stat = stat;
			this.weight = weight;
			this.valueType = valueType;
		}
	}

	public static class RangeEntry
	{
		public final int from;
		public final int to;
		public final int weight;
		public final int tier; // 0 = 未設定

		public RangeEntry(int from, int to, int weight, int tier)
		{
			this.from = from;
			this.to = to;
			this.weight = weight;
			this.tier = tier;
		}
	}

	public static class ResetCostEntry
	{
		public final int itemId;
		public final int count;

		public ResetCostEntry(int itemId, int count)
		{
			this.itemId = itemId;
			this.count = count;
		}
	}

	/**
	 * 突破配方：一份配方包含 ID、顯示名稱、成功機率、與多種材料。
	 */
	public static class BreakthroughRecipe
	{
		public final int id;
		public final String name;
		public final int chance;
		public final List<ResetCostEntry> materials = new ArrayList<>();

		public BreakthroughRecipe(int id, String name, int chance)
		{
			this.id = id;
			this.name = name;
			this.chance = chance;
		}
	}

	// ── 設定欄位 ──────────────────────────────────────────────────────────────

	private int _defaultCharges = 10;

	// 普通精煉消耗道具列表（支援多種）
	private final List<ResetCostEntry> _refineItemList = new ArrayList<>();

	// 高級精煉道具設定
	private final List<ResetCostEntry> _premiumItemList = new ArrayList<>();
	private int _premiumMinTier = 1;
	private int _premiumMaxTier = 5;

	// 突破設定
	private final List<BreakthroughRecipe> _breakthroughRecipes = new ArrayList<>();

	// 禁忌精煉設定
	private boolean _forbiddenEnabled = false;
	private int _forbiddenDestroyChance = 50;
	private int _forbiddenMaxDailyCount = 1;
	private int _forbiddenMinTier = 1;
	private int _forbiddenMaxTier = 5;
	private final List<ResetCostEntry> _forbiddenCostItems = new ArrayList<>();
	private final int[] _forbiddenTierChances = new int[4];

	// 防爆道具設定（失敗時消耗 1 個抵銷裝備消失）
	private int _forbiddenProtectionItemId = 0;
	private int _forbiddenProtectionCount = 1;

	// T 數顏色 (tier -> hex color)
	private final Map<Integer, String> _tierColors = new HashMap<>();

	private final Map<Integer, Integer> _specialCharges = new HashMap<>();
	private final int[] _tierChances = new int[4];
	private final List<ResetCostEntry> _resetCostList = new ArrayList<>();

	private final Map<Integer, SeriesEntry> _series = new LinkedHashMap<>();
	private final List<SeriesEntry> _seriesList = new ArrayList<>();
	private int _totalSeriesWeight = 0;

	// ── 建構 ──────────────────────────────────────────────────────────────────

	protected RefineSystemData()
	{
		load();
	}

	@Override
	public void load()
	{
		_series.clear();
		_seriesList.clear();
		_specialCharges.clear();
		_tierColors.clear();
		_resetCostList.clear();
		_refineItemList.clear();
		_premiumItemList.clear();
		_forbiddenCostItems.clear();
		java.util.Arrays.fill(_forbiddenTierChances, 0);
		_forbiddenProtectionItemId = 0;
		_forbiddenProtectionCount = 1;
		_forbiddenEnabled = false;
		_breakthroughRecipes.clear();
		_totalSeriesWeight = 0;

		parseDatapackFile("data/scripts/custom/RefineSystem/RefineSystemData.xml");

		LOGGER.info(getClass().getSimpleName() + ": 已載入 " + _series.size() + " 個精煉系列。");
	}

	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node root = document.getFirstChild(); root != null; root = root.getNextSibling())
		{
			if (!"refineSystem".equals(root.getNodeName()))
			{
				continue;
			}
			for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling())
			{
				switch (child.getNodeName())
				{
					case "settings":
						parseSettings(child);
						break;
					case "premiumSettings":
						parsePremiumSettings(child);
						break;
					case "forbiddenSettings":
						parseForbiddenSettings(child);
						break;
					case "breakthroughSettings":
						parseBreakthroughSettings(child);
						break;
					case "tierColors":
						parseTierColors(child);
						break;
					case "specialCharges":
						parseSpecialCharges(child);
						break;
					case "tierChances":
						parseTierChances(child);
						break;
					case "chargeResetCost":
						parseChargeResetCost(child);
						break;
					case "series":
						parseSeries(child);
						break;
				}
			}
		}

		for (SeriesEntry se : _series.values())
		{
			_seriesList.add(se);
			_totalSeriesWeight += se.weight;
		}
	}

	// ── XML 解析 ──────────────────────────────────────────────────────────────

	private void parseSettings(Node node)
	{
		final NamedNodeMap attrs = node.getAttributes();
		_defaultCharges = parseInteger(attrs, "defaultCharges", 10);
		for (Node item = node.getFirstChild(); item != null; item = item.getNextSibling())
		{
			if (!"item".equals(item.getNodeName()))
			{
				continue;
			}
			final NamedNodeMap ia = item.getAttributes();
			final int itemId = parseInteger(ia, "itemId", 0);
			final int count = parseInteger(ia, "count", 1);
			if (itemId > 0)
			{
				_refineItemList.add(new ResetCostEntry(itemId, count));
			}
		}
	}

	private void parsePremiumSettings(Node node)
	{
		final NamedNodeMap attrs = node.getAttributes();
		_premiumMinTier = parseInteger(attrs, "minTier", 1);
		_premiumMaxTier = parseInteger(attrs, "maxTier", 5);
		for (Node item = node.getFirstChild(); item != null; item = item.getNextSibling())
		{
			if (!"item".equals(item.getNodeName()))
			{
				continue;
			}
			final NamedNodeMap ia = item.getAttributes();
			final int itemId = parseInteger(ia, "itemId", 0);
			final int count = parseInteger(ia, "count", 1);
			if (itemId > 0)
			{
				_premiumItemList.add(new ResetCostEntry(itemId, count));
			}
		}
	}

	private void parseForbiddenSettings(Node node)
	{
		final NamedNodeMap attrs = node.getAttributes();
		_forbiddenDestroyChance = parseInteger(attrs, "destroyChance", 50);
		_forbiddenMaxDailyCount = parseInteger(attrs, "maxDailyCount", 1);
		_forbiddenMinTier = parseInteger(attrs, "minTier", 1);
		_forbiddenMaxTier = parseInteger(attrs, "maxTier", 5);
		_forbiddenEnabled = true;

		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
		{
			final String nname = child.getNodeName();
			if ("costItems".equals(nname))
			{
				for (Node item = child.getFirstChild(); item != null; item = item.getNextSibling())
				{
					if (!"item".equals(item.getNodeName())) continue;
					final NamedNodeMap ia = item.getAttributes();
					final int itemId = parseInteger(ia, "itemId", 0);
					final int count = parseInteger(ia, "count", 1);
					if (itemId > 0)
					{
						_forbiddenCostItems.add(new ResetCostEntry(itemId, count));
					}
				}
			}
			else if ("forbiddenTierChances".equals(nname))
			{
				for (Node tier = child.getFirstChild(); tier != null; tier = tier.getNextSibling())
				{
					if (!"tier".equals(tier.getNodeName())) continue;
					final NamedNodeMap ta = tier.getAttributes();
					final int count = parseInteger(ta, "count", 1);
					final int chance = parseInteger(ta, "chance", 0);
					if (count >= 1 && count <= 4)
					{
						_forbiddenTierChances[count - 1] = chance;
					}
				}
			}
			else if ("protection".equals(nname))
			{
				final NamedNodeMap pa = child.getAttributes();
				_forbiddenProtectionItemId = parseInteger(pa, "itemId", 0);
				_forbiddenProtectionCount = parseInteger(pa, "count", 1);
			}
		}
	}

	private void parseBreakthroughSettings(Node node)
	{
		for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if (!"recipe".equals(child.getNodeName()))
			{
				continue;
			}
			final NamedNodeMap attrs = child.getAttributes();
			final int id = parseInteger(attrs, "id", 0);
			final String name = parseString(attrs, "name");
			final int chance = parseInteger(attrs, "chance", 0);
			if (id <= 0 || name == null)
			{
				continue;
			}
			final BreakthroughRecipe recipe = new BreakthroughRecipe(id, name, chance);
			for (Node mat = child.getFirstChild(); mat != null; mat = mat.getNextSibling())
			{
				if (!"material".equals(mat.getNodeName()))
				{
					continue;
				}
				final NamedNodeMap ma = mat.getAttributes();
				final int itemId = parseInteger(ma, "itemId", 0);
				final int count = parseInteger(ma, "count", 1);
				if (itemId > 0)
				{
					recipe.materials.add(new ResetCostEntry(itemId, count));
				}
			}
			if (!recipe.materials.isEmpty())
			{
				_breakthroughRecipes.add(recipe);
			}
		}
	}

	private void parseTierColors(Node node)
	{
		for (Node item = node.getFirstChild(); item != null; item = item.getNextSibling())
		{
			if (!"tierColor".equals(item.getNodeName()))
			{
				continue;
			}
			final NamedNodeMap attrs = item.getAttributes();
			final int tier = parseInteger(attrs, "tier", 0);
			final String color = parseString(attrs, "color");
			if (tier > 0 && color != null)
			{
				_tierColors.put(tier, color);
			}
		}
	}

	private void parseSpecialCharges(Node node)
	{
		for (Node item = node.getFirstChild(); item != null; item = item.getNextSibling())
		{
			if (!"item".equals(item.getNodeName()))
			{
				continue;
			}
			final NamedNodeMap attrs = item.getAttributes();
			final int templateId = parseInteger(attrs, "templateId", 0);
			final int charges = parseInteger(attrs, "charges", 0);
			if (templateId > 0 && charges > 0)
			{
				_specialCharges.put(templateId, charges);
			}
		}
	}

	private void parseTierChances(Node node)
	{
		for (Node tier = node.getFirstChild(); tier != null; tier = tier.getNextSibling())
		{
			if (!"tier".equals(tier.getNodeName()))
			{
				continue;
			}
			final NamedNodeMap attrs = tier.getAttributes();
			final int count = parseInteger(attrs, "count", 1);
			final int chance = parseInteger(attrs, "chance", 0);
			if (count >= 1 && count <= 4)
			{
				_tierChances[count - 1] = chance;
			}
		}
	}

	private void parseChargeResetCost(Node node)
	{
		for (Node item = node.getFirstChild(); item != null; item = item.getNextSibling())
		{
			if (!"item".equals(item.getNodeName()))
			{
				continue;
			}
			final NamedNodeMap attrs = item.getAttributes();
			final int itemId = parseInteger(attrs, "itemId", 0);
			final int count = parseInteger(attrs, "count", 0);
			if (itemId > 0 && count > 0)
			{
				_resetCostList.add(new ResetCostEntry(itemId, count));
			}
		}
	}

	private void parseSeries(Node node)
	{
		final NamedNodeMap attrs = node.getAttributes();
		final int base = parseInteger(attrs, "base", 0);
		final String name = parseString(attrs, "name");
		final String statId = parseString(attrs, "stat");
		final int weight = parseInteger(attrs, "weight", 100);
		final String vtStr = parseString(attrs, "valueType");

		if (base <= 0 || name == null || statId == null)
		{
			return;
		}

		Stat stat;
		try
		{
			stat = Stat.valueOf(statId);
		}
		catch (IllegalArgumentException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": 未知 Stat: " + statId);
			return;
		}

		ValueType valueType = ValueType.PERCENT;
		if ("FLAT".equalsIgnoreCase(vtStr))
		{
			valueType = ValueType.FLAT;
		}
		else if ("MUL".equalsIgnoreCase(vtStr))
		{
			valueType = ValueType.MUL;
		}

		final SeriesEntry entry = new SeriesEntry(base, name, stat, weight, valueType);

		for (Node range = node.getFirstChild(); range != null; range = range.getNextSibling())
		{
			if (!"range".equals(range.getNodeName()))
			{
				continue;
			}
			final NamedNodeMap ra = range.getAttributes();
			final int from = parseInteger(ra, "from", 0);
			final int to = parseInteger(ra, "to", 0);
			final int rw = parseInteger(ra, "weight", 1);
			final int tier = parseInteger(ra, "tier", 0);
			entry.ranges.add(new RangeEntry(from, to, rw, tier));
		}

		_series.put(base, entry);
	}

	// ── 隨機邏輯 ──────────────────────────────────────────────────────────────

	public int rollTierCount()
	{
		int count = 1; // 第1條一定有
		for (int i = 1; i < _tierChances.length; i++)
		{
			if (Rnd.get(100) < _tierChances[i])
			{
				count++;
			}
			else
			{
				break; // 某條沒出現，後面的也不出現
			}
		}
		return count;
	}

	public int rollRefineId()
	{
		if (_seriesList.isEmpty())
		{
			return 0;
		}

		final SeriesEntry series = weightedRandom(_seriesList, _totalSeriesWeight);
		if (series == null || series.ranges.isEmpty())
		{
			return 0;
		}

		int totalRangeWeight = 0;
		for (RangeEntry r : series.ranges)
		{
			totalRangeWeight += r.weight;
		}
		final RangeEntry range = weightedRandom(series.ranges, totalRangeWeight);
		if (range == null)
		{
			return 0;
		}

		return series.base + range.from + Rnd.get(range.to - range.from + 1);
	}

	/**
	 * 高級精煉專用：只從 tier 在 [minTier, maxTier] 的 range 中抽取。
	 * series 的 weight 照舊，range 只考慮符合 tier 的。
	 */
	public int rollRefineIdForTier(int minTier, int maxTier)
	{
		if (_seriesList.isEmpty())
		{
			return 0;
		}

		// 建立符合 tier 的 (series, range) 候選清單，以 seriesWeight * rangeWeight 加權
		final List<int[]> candidates = new ArrayList<>(); // [seriesIndex, rangeIndex, effectiveWeight]
		int totalWeight = 0;

		for (int si = 0; si < _seriesList.size(); si++)
		{
			final SeriesEntry se = _seriesList.get(si);
			for (int ri = 0; ri < se.ranges.size(); ri++)
			{
				final RangeEntry re = se.ranges.get(ri);
				if (re.tier == 0 || (re.tier >= minTier && re.tier <= maxTier))
				{
					final int w = se.weight * re.weight;
					candidates.add(new int[]{ si, ri, w });
					totalWeight += w;
				}
			}
		}

		if (candidates.isEmpty() || totalWeight <= 0)
		{
			return 0;
		}

		int roll = Rnd.get(totalWeight);
		int cumulative = 0;
		int[] chosen = candidates.get(candidates.size() - 1);
		for (int[] c : candidates)
		{
			cumulative += c[2];
			if (roll < cumulative)
			{
				chosen = c;
				break;
			}
		}

		final SeriesEntry se = _seriesList.get(chosen[0]);
		final RangeEntry re = se.ranges.get(chosen[1]);
		return se.base + re.from + Rnd.get(re.to - re.from + 1);
	}

	private <T> T weightedRandom(List<T> list, int totalWeight)
	{
		if (totalWeight <= 0)
		{
			return list.isEmpty() ? null : list.get(0);
		}
		int roll = Rnd.get(totalWeight);
		int cumulative = 0;
		for (T entry : list)
		{
			if (entry instanceof SeriesEntry)
			{
				cumulative += ((SeriesEntry) entry).weight;
			}
			else if (entry instanceof RangeEntry)
			{
				cumulative += ((RangeEntry) entry).weight;
			}
			if (roll < cumulative)
			{
				return entry;
			}
		}
		return list.get(list.size() - 1);
	}

	// ── 精煉加成計算 ──────────────────────────────────────────────────────────

	public double getRefineBonus(VariationInstance aug, Stat stat)
	{
		if (aug == null || stat == null)
		{
			return 0;
		}
		double bonus = 0;
		bonus += calcOption(aug.getOption1Id(), stat, false);
		bonus += calcOption(aug.getOption2Id(), stat, false);
		bonus += calcOption(aug.getOption3Id(), stat, false);
		bonus += calcOption(aug.getOption4Id(), stat, false);
		return bonus;
	}

	public double getRefineMulBonus(VariationInstance aug, Stat stat)
	{
		if (aug == null || stat == null)
		{
			return 0;
		}
		double bonus = 0;
		bonus += calcOption(aug.getOption1Id(), stat, true);
		bonus += calcOption(aug.getOption2Id(), stat, true);
		bonus += calcOption(aug.getOption3Id(), stat, true);
		bonus += calcOption(aug.getOption4Id(), stat, true);
		return bonus;
	}

	/**
	 * Item 版本：包含 4 條普通詞條 + 突破第五條詞條的加成（PERCENT/FLAT 類型）。
	 * 套用裝備加成時應使用此方法以包含突破效果。
	 */
	public double getRefineBonus(Item item, Stat stat)
	{
		if (item == null || stat == null)
		{
			return 0;
		}
		final VariationInstance aug = item.getAugmentation();
		if (aug == null)
		{
			return 0;
		}
		double bonus = 0;
		bonus += calcOption(aug.getOption1Id(), stat, false);
		bonus += calcOption(aug.getOption2Id(), stat, false);
		bonus += calcOption(aug.getOption3Id(), stat, false);
		bonus += calcOption(aug.getOption4Id(), stat, false);
		bonus += calcOption(aug.getOption5Id(), stat, false);
		return bonus;
	}

	/**
	 * Item 版本：包含 4 條普通詞條 + 突破第五條詞條的加成（MUL 類型）。
	 */
	public double getRefineMulBonus(Item item, Stat stat)
	{
		if (item == null || stat == null)
		{
			return 0;
		}
		final VariationInstance aug = item.getAugmentation();
		if (aug == null)
		{
			return 0;
		}
		double bonus = 0;
		bonus += calcOption(aug.getOption1Id(), stat, true);
		bonus += calcOption(aug.getOption2Id(), stat, true);
		bonus += calcOption(aug.getOption3Id(), stat, true);
		bonus += calcOption(aug.getOption4Id(), stat, true);
		bonus += calcOption(aug.getOption5Id(), stat, true);
		return bonus;
	}

	private double calcOption(int optionId, Stat stat, boolean mulMode)
	{
		if (optionId <= 0)
		{
			return 0;
		}
		final int base = (optionId / 10000) * 10000;
		final SeriesEntry series = _series.get(base);
		if (series == null || series.stat != stat)
		{
			return 0;
		}
		final int raw = optionId % 10000;
		if (mulMode)
		{
			return series.valueType == ValueType.MUL ? raw / 10000.0 : 0;
		}
		switch (series.valueType)
		{
			case PERCENT: return raw / 100.0;
			case FLAT:    return raw;
			default:      return 0;
		}
	}

	/** 供 PlayerStat 直接用 optionId 查詢 PERCENT/FLAT 加成 */
	public double calcOptionValue(int optionId, Stat stat)
	{
		return calcOption(optionId, stat, false);
	}

	/** 供 PlayerStat 直接用 optionId 查詢 MUL 加成 */
	public double calcOptionMul(int optionId, Stat stat)
	{
		return calcOption(optionId, stat, true);
	}

	// ── 查詢方法 ──────────────────────────────────────────────────────────────

	/** 普通精煉消耗道具列表（可多種） */
	public List<ResetCostEntry> getRefineItemList() { return _refineItemList; }

	/** 取第一個普通精煉道具 ID，供 VariationInstance.ofRaw() 標記用 */
	public int getRefineItemId()
	{
		return _refineItemList.isEmpty() ? 0 : _refineItemList.get(0).itemId;
	}

	/** @deprecated 僅為相容舊呼叫，請改用 getRefineItemList() */
	public int getRefineItemCount()
	{
		return _refineItemList.isEmpty() ? 1 : _refineItemList.get(0).count;
	}

	public int getDefaultCharges()   { return _defaultCharges; }

	/** 高級精煉消耗道具列表（可多種） */
	public List<ResetCostEntry> getPremiumItemList() { return _premiumItemList; }

	/** 取第一個高級精煉道具 ID，供 VariationInstance.ofRaw() 標記用 */
	public int getPremiumItemId()
	{
		return _premiumItemList.isEmpty() ? 0 : _premiumItemList.get(0).itemId;
	}

	/** @deprecated 僅為相容舊呼叫，請改用 getPremiumItemList() */
	public int getPremiumItemCount()
	{
		return _premiumItemList.isEmpty() ? 1 : _premiumItemList.get(0).count;
	}

	public int getPremiumMinTier()   { return _premiumMinTier; }
	public int getPremiumMaxTier()   { return _premiumMaxTier; }
	public boolean hasPremiumItem()  { return !_premiumItemList.isEmpty(); }

	// ── 禁忌精煉 ──────────────────────────────────────────────────────────────

	public boolean isForbiddenEnabled()      { return _forbiddenEnabled && !_forbiddenCostItems.isEmpty(); }
	public int getForbiddenDestroyChance()   { return _forbiddenDestroyChance; }
	public int getForbiddenMaxDailyCount()   { return _forbiddenMaxDailyCount; }
	public int getForbiddenMinTier()         { return _forbiddenMinTier; }
	public int getForbiddenMaxTier()         { return _forbiddenMaxTier; }
	public List<ResetCostEntry> getForbiddenCostItems()    { return _forbiddenCostItems; }

	/** 防爆道具 ID（0 表示未設定） */
	public int getForbiddenProtectionItemId() { return _forbiddenProtectionItemId; }
	/** 觸發防爆時消耗的數量 */
	public int getForbiddenProtectionCount() { return _forbiddenProtectionCount; }
	/** 是否已設定防爆道具 */
	public boolean hasForbiddenProtection() { return _forbiddenProtectionItemId > 0; }

	/** 禁忌精煉專屬：獨立判斷詞條數量 */
	public int rollForbiddenTierCount()
	{
		int count = 1; // 第1條一定有
		for (int i = 1; i < _forbiddenTierChances.length; i++)
		{
			if (Rnd.get(100) < _forbiddenTierChances[i])
			{
				count++;
			}
			else
			{
				break;
			}
		}
		return count;
	}

	/** 禁忌精煉專屬：在 [minTier, maxTier] 範圍內抽詞條 */
	public int rollForbiddenRefineId()
	{
		return rollRefineIdForTier(_forbiddenMinTier, _forbiddenMaxTier);
	}

	// ── 突破系統 ──────────────────────────────────────────────────────────────

	public List<BreakthroughRecipe> getBreakthroughRecipes()
	{
		return _breakthroughRecipes;
	}

	public boolean isBreakthroughEnabled()
	{
		return !_breakthroughRecipes.isEmpty();
	}

	public BreakthroughRecipe getBreakthroughRecipeById(int id)
	{
		for (BreakthroughRecipe r : _breakthroughRecipes)
		{
			if (r.id == id)
			{
				return r;
			}
		}
		return null;
	}

	/** 取得 optionId 對應的 tier（0 = 未設定） */
	public int getTier(int optionId)
	{
		if (optionId <= 0)
		{
			return 0;
		}
		final int base = (optionId / 10000) * 10000;
		final SeriesEntry se = _series.get(base);
		if (se == null)
		{
			return 0;
		}
		final int raw = optionId % 10000;
		for (RangeEntry re : se.ranges)
		{
			if (raw >= re.from && raw <= re.to)
			{
				return re.tier;
			}
		}
		return 0;
	}

	/** 取得 tier 對應的顏色 hex（未設定回傳 "FFFFFF"） */
	public String getTierColor(int tier)
	{
		return _tierColors.getOrDefault(tier, "FFFFFF");
	}

	public int getSpecialCharges(int templateId)
	{
		final Integer v = _specialCharges.get(templateId);
		return v != null ? v : _defaultCharges;
	}

	public boolean hasSpecialCharges(int templateId)
	{
		return _specialCharges.containsKey(templateId);
	}

	public List<ResetCostEntry> getResetCostList()
	{
		return _resetCostList;
	}

	public boolean hasResetCost()
	{
		return !_resetCostList.isEmpty();
	}

	public SeriesEntry getSeriesByBase(int base)
	{
		return _series.get(base);
	}

	public List<SeriesEntry> getAllSeries()
	{
		return _seriesList;
	}

	public String getSeriesName(int optionId)
	{
		if (optionId <= 0)
		{
			return "";
		}
		final int base = (optionId / 10000) * 10000;
		final SeriesEntry se = _series.get(base);
		return se != null ? se.name : "未知";
	}

	public String getValueDisplay(int optionId)
	{
		if (optionId <= 0)
		{
			return "0";
		}
		final int base = (optionId / 10000) * 10000;
		final SeriesEntry se = _series.get(base);
		final int raw = optionId % 10000;
		if (se == null)
		{
			return String.format("%.2f%%", raw / 100.0);
		}
		switch (se.valueType)
		{
			case FLAT: return String.valueOf(raw);
			default:   return String.format("%.2f%%", raw / 100.0);
		}
	}

	// ── Singleton ─────────────────────────────────────────────────────────────

	public static RefineSystemData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final RefineSystemData INSTANCE = new RefineSystemData();
	}
}
