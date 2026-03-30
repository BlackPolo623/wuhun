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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;

/**
 * 魂環能力系統 XML 設定讀取器。
 * 讀取 data/scripts/custom/SoulRingAbility/SoulRingAbilityData.xml。
 * @author Custom
 */
public class SoulRingAbilityData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SoulRingAbilityData.class.getName());

	/** 單一能力的完整設定 */
	public static class StatConfig
	{
		public final String id;
		public final String name;
		public final String varName;
		public final double percentPerPoint;
		public final int maxPoints;
		public final int pointCost;

		public StatConfig(String id, String name, String varName, double percentPerPoint, int maxPoints, int pointCost)
		{
			this.id = id;
			this.name = name;
			this.varName = varName;
			this.percentPerPoint = percentPerPoint;
			this.maxPoints = maxPoints;
			this.pointCost = pointCost;
		}
	}

	/** 分類（pve_normal / pve_raid / pvp / special） */
	public static class CategoryConfig
	{
		public final String id;
		public final String name;
		public final List<StatConfig> stats;

		public CategoryConfig(String id, String name, List<StatConfig> stats)
		{
			this.id = id;
			this.name = name;
			this.stats = Collections.unmodifiableList(stats);
		}
	}

	// categoryId -> CategoryConfig（保留插入順序）
	private final Map<String, CategoryConfig> _categories = new LinkedHashMap<>();
	// varName -> StatConfig（快速查找）
	private final Map<String, StatConfig> _statByVar = new LinkedHashMap<>();

	protected SoulRingAbilityData()
	{
		load();
	}

	@Override
	public void load()
	{
		_categories.clear();
		_statByVar.clear();
		parseDatapackFile("data/scripts/custom/SoulRingAbility/SoulRingAbilityData.xml");

		int totalStats = 0;
		for (CategoryConfig cat : _categories.values())
		{
			totalStats += cat.stats.size();
		}
		LOGGER.info(getClass().getSimpleName() + ": 已載入 " + _categories.size() + " 個分類，共 " + totalStats + " 項能力設定。");
	}

	@Override
	public boolean isValidating()
	{
		return false;
	}

	@Override
	public void parseFile(File file)
	{
		if (!isValidXmlFile(file))
		{
			LOGGER.warning("Cannot parse " + file.getName() + ": file does not exist or is not valid.");
			return;
		}
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final DocumentBuilder builder = factory.newDocumentBuilder();
			parseDocument(builder.parse(file), file);
		}
		catch (Exception e)
		{
			LOGGER.warning("Error parsing " + file.getName() + ": " + e.getMessage());
		}
	}

	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node root = document.getFirstChild(); root != null; root = root.getNextSibling())
		{
			if (!"soulRingAbility".equals(root.getNodeName()))
			{
				continue;
			}

			for (Node catNode = root.getFirstChild(); catNode != null; catNode = catNode.getNextSibling())
			{
				if (!"category".equals(catNode.getNodeName()))
				{
					continue;
				}

				final String catId = parseString(catNode.getAttributes(), "id");
				final String catName = parseString(catNode.getAttributes(), "name");
				final List<StatConfig> statList = new ArrayList<>();

				for (Node statNode = catNode.getFirstChild(); statNode != null; statNode = statNode.getNextSibling())
				{
					if (!"stat".equals(statNode.getNodeName()))
					{
						continue;
					}

					final String id = parseString(statNode.getAttributes(), "id");
					final String name = parseString(statNode.getAttributes(), "name");
					final String varName = parseString(statNode.getAttributes(), "var");
					final double percentPerPoint = parseDouble(statNode.getAttributes(), "percentPerPoint");
					final int maxPoints = parseInteger(statNode.getAttributes(), "maxPoints");
					final int pointCost = parseInteger(statNode.getAttributes(), "pointCost");

					final StatConfig sc = new StatConfig(id, name, varName, percentPerPoint, maxPoints, pointCost);
					statList.add(sc);
					_statByVar.put(varName, sc);
				}

				_categories.put(catId, new CategoryConfig(catId, catName, statList));
			}
		}
	}

	// ==================== 查詢方法 ====================

	/** 取得所有分類（按 XML 順序） */
	public List<CategoryConfig> getCategories()
	{
		return new ArrayList<>(_categories.values());
	}

	/** 取得指定分類，不存在回傳 null */
	public CategoryConfig getCategory(String categoryId)
	{
		return _categories.get(categoryId);
	}

	/** 依 varName 取得設定，不存在回傳 null */
	public StatConfig getStatByVar(String varName)
	{
		return _statByVar.get(varName);
	}

	/** 取得指定 varName 的 percentPerPoint，找不到回傳預設值 */
	public double getPercentPerPoint(String varName)
	{
		final StatConfig sc = _statByVar.get(varName);
		return sc != null ? sc.percentPerPoint : 0.3;
	}

	/** 取得指定 varName 的 pointCost，找不到回傳 1 */
	public int getPointCost(String varName)
	{
		final StatConfig sc = _statByVar.get(varName);
		return sc != null ? sc.pointCost : 1;
	}

	/** 取得指定 varName 的 maxPoints，找不到回傳 350 */
	public int getMaxPoints(String varName)
	{
		final StatConfig sc = _statByVar.get(varName);
		return sc != null ? sc.maxPoints : 350;
	}

	// ==================== Singleton ====================

	public static SoulRingAbilityData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final SoulRingAbilityData INSTANCE = new SoulRingAbilityData();
	}
}
