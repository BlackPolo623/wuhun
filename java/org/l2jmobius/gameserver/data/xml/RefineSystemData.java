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

		public RangeEntry(int from, int to, int weight)
		{
			this.from = from;
			this.to = to;
			this.weight = weight;
		}
	}

	// ── 設定欄位 ──────────────────────────────────────────────────────────────

	private int _refineItemId = 99001;
	private int _refineItemCount = 1;
	private int _defaultCharges = 10;

	private final Map<Integer, Integer> _specialCharges = new HashMap<>();
	private final int[] _tierChances = new int[4];

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
					case "specialCharges":
						parseSpecialCharges(child);
						break;
					case "tierChances":
						parseTierChances(child);
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
		_refineItemId = parseInteger(attrs, "refineItemId", 99001);
		_refineItemCount = parseInteger(attrs, "refineItemCount", 1);
		_defaultCharges = parseInteger(attrs, "defaultCharges", 10);
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
			entry.ranges.add(new RangeEntry(from, to, rw));
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

	public int getRefineItemId()    { return _refineItemId; }
	public int getRefineItemCount() { return _refineItemCount; }
	public int getDefaultCharges()  { return _defaultCharges; }

	public int getSpecialCharges(int templateId)
	{
		final Integer v = _specialCharges.get(templateId);
		return v != null ? v : _defaultCharges;
	}

	public boolean hasSpecialCharges(int templateId)
	{
		return _specialCharges.containsKey(templateId);
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
