/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.morph.MorphStatEntry;
import org.l2jmobius.gameserver.model.morph.MorphStatEntry.Operation;
import org.l2jmobius.gameserver.model.sacrifice.SacrificeAltarEntry;
import org.l2jmobius.gameserver.model.sacrifice.SacrificeMaterialEntry;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * 祭祀系統數據加載器。
 *
 * 從 {@code data/scripts/custom/Sacrifice/SacrificeData.xml} 加載所有祭壇定義。
 *
 * XML 結構：
 * <pre>
 * &lt;list&gt;
 *   &lt;altar id="1" name="力量祭壇"
 *          chancePercent="80"
 *          upgradeRate="0.10"
 *          maxLevel="20"&gt;
 *     &lt;material itemId="57"    count="1000000"/&gt;
 *     &lt;material itemId="10639" count="10000"/&gt;
 *     &lt;stat name="pAtk"    value="500" operation="add"/&gt;
 *     &lt;stat name="critDmg" value="10"  operation="mul"/&gt;
 *   &lt;/altar&gt;
 * &lt;/list&gt;
 * </pre>
 *
 * @author Custom
 */
public class SacrificeData implements IXmlReader
{
	/** altarId → SacrificeAltarEntry，TreeMap 保持升序 */
	private final Map<Integer, SacrificeAltarEntry> _altars = new TreeMap<>();

	// ── 構造器 ────────────────────────────────────────────────────────────

	protected SacrificeData()
	{
		load();
	}

	// ── 加載 ─────────────────────────────────────────────────────────────

	@Override
	public void load()
	{
		_altars.clear();
		parseDatapackFile("data/scripts/custom/Sacrifice/SacrificeData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _altars.size() + " sacrifice altar(s).");
	}

	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node root = document.getFirstChild(); root != null; root = root.getNextSibling())
		{
			if (!"list".equalsIgnoreCase(root.getNodeName()))
			{
				continue;
			}
			for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling())
			{
				if ("altar".equalsIgnoreCase(child.getNodeName()))
				{
					final SacrificeAltarEntry entry = parseAltar(child, file.getName());
					if (entry != null)
					{
						_altars.put(entry.getId(), entry);
					}
				}
			}
		}
	}

	// ── 解析 <altar> ─────────────────────────────────────────────────────

	private SacrificeAltarEntry parseAltar(Node altarNode, String fileName)
	{
		final NamedNodeMap attrs = altarNode.getAttributes();
		if (attrs == null)
		{
			return null;
		}

		// 必填屬性
		final Node idAttr = attrs.getNamedItem("id");
		final Node nameAttr = attrs.getNamedItem("name");
		final Node chanceAttr = attrs.getNamedItem("chancePercent");
		final Node rateAttr = attrs.getNamedItem("upgradeRate");
		final Node maxLvAttr = attrs.getNamedItem("maxLevel");

		if ((idAttr == null) || (nameAttr == null) || (chanceAttr == null) || (rateAttr == null) || (maxLvAttr == null))
		{
			LOGGER.warning(getClass().getSimpleName() + ": <altar> missing required attribute(s) in " + fileName);
			return null;
		}

		int id, chancePercent, maxLevel;
		double upgradeRate;
		try
		{
			id = Integer.parseInt(idAttr.getNodeValue());
			chancePercent = Integer.parseInt(chanceAttr.getNodeValue());
			maxLevel = Integer.parseInt(maxLvAttr.getNodeValue());
			upgradeRate = Double.parseDouble(rateAttr.getNodeValue());
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Invalid numeric attribute in <altar> (" + fileName + ")");
			return null;
		}

		// 範圍校驗
		chancePercent = Math.max(1, Math.min(100, chancePercent));
		maxLevel = Math.max(1, maxLevel);

		final SacrificeAltarEntry entry = new SacrificeAltarEntry(id, nameAttr.getNodeValue(), chancePercent, upgradeRate, maxLevel);

		// 子節點
		for (Node child = altarNode.getFirstChild(); child != null; child = child.getNextSibling())
		{
			final String nodeName = child.getNodeName();
			if ("material".equalsIgnoreCase(nodeName))
			{
				final SacrificeMaterialEntry mat = parseMaterial(child, id, fileName);
				if (mat != null)
				{
					entry.addMaterial(mat);
				}
			}
			else if ("stat".equalsIgnoreCase(nodeName))
			{
				final MorphStatEntry stat = parseStat(child, id, fileName);
				if (stat != null)
				{
					entry.addStat(stat);
				}
			}
		}

		return entry;
	}

	// ── 解析 <material> ──────────────────────────────────────────────────

	private SacrificeMaterialEntry parseMaterial(Node node, int altarId, String fileName)
	{
		final NamedNodeMap attrs = node.getAttributes();
		if (attrs == null)
		{
			return null;
		}

		final Node itemIdAttr = attrs.getNamedItem("itemId");
		final Node countAttr = attrs.getNamedItem("count");

		if ((itemIdAttr == null) || (countAttr == null))
		{
			LOGGER.warning(getClass().getSimpleName() + ": <material> missing itemId or count in altar id=" + altarId + " (" + fileName + ")");
			return null;
		}

		try
		{
			final int itemId = Integer.parseInt(itemIdAttr.getNodeValue());
			final long count = Long.parseLong(countAttr.getNodeValue());
			return new SacrificeMaterialEntry(itemId, count);
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Invalid material value in altar id=" + altarId + " (" + fileName + ")");
			return null;
		}
	}

	// ── 解析 <stat> ──────────────────────────────────────────────────────

	private MorphStatEntry parseStat(Node node, int altarId, String fileName)
	{
		final NamedNodeMap attrs = node.getAttributes();
		if (attrs == null)
		{
			return null;
		}

		final Node nameAttr = attrs.getNamedItem("name");
		final Node valueAttr = attrs.getNamedItem("value");
		if ((nameAttr == null) || (valueAttr == null))
		{
			return null;
		}

		final String statName = nameAttr.getNodeValue().trim();
		final Stat stat = MorphStatEntry.STAT_PARSE_MAP.get(statName);
		if (stat == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Unknown stat name '" + statName + "' in altar id=" + altarId + " (" + fileName + ")");
			return null;
		}

		double value;
		try
		{
			value = Double.parseDouble(valueAttr.getNodeValue());
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Invalid stat value for '" + statName + "' in altar id=" + altarId);
			return null;
		}

		final Node opAttr = attrs.getNamedItem("operation");
		final Operation op = (opAttr != null) && "mul".equalsIgnoreCase(opAttr.getNodeValue()) ? Operation.MUL : Operation.ADD;

		return new MorphStatEntry(stat, value, op);
	}

	// ── 查詢接口 ─────────────────────────────────────────────────────────

	/**
	 * 按 altarId 獲取祭壇定義。
	 */
	public SacrificeAltarEntry getAltar(int altarId)
	{
		return _altars.get(altarId);
	}

	/**
	 * 返回所有祭壇列表（按 id 升序，不可修改）。
	 */
	public List<SacrificeAltarEntry> getAltarList()
	{
		return Collections.unmodifiableList(new ArrayList<>(_altars.values()));
	}

	/**
	 * 返回已定義的祭壇數量。
	 */
	public int getAltarCount()
	{
		return _altars.size();
	}

	// ── Singleton ─────────────────────────────────────────────────────────

	public static SacrificeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final SacrificeData INSTANCE = new SacrificeData();
	}
}
