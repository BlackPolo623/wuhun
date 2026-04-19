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
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.morph.MorphEntry;
import org.l2jmobius.gameserver.model.morph.MorphGradeHolder;
import org.l2jmobius.gameserver.model.morph.MorphStatEntry;
import org.l2jmobius.gameserver.model.morph.MorphStatEntry.Operation;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * 變身系統數據加載器 MorphData 從 data/stats/TransformMorph.xml 加載所有變身定義。 XML 結構（grade 為主導）：
 *
 * <pre>
 * &lt;grade level="1"&gt;
 *   &lt;morph id="1" name="兔子" npcId="64" itemId="900001"
 *          collisionRadius="14" collisionHeight="24.0"
 *          abnormalEffects="STEALTH"&gt;
 *     &lt;stat name="物理攻擊" value="1000" operation="add"/&gt;
 *   &lt;/morph&gt;
 * &lt;/grade&gt;
 * </pre>
 *
 * 索引： gradeLevel → MorphGradeHolder（按階級查全部變身） itemId → MorphEntry（道具激活快速查找） morphId → Map{gradeLevel → MorphEntry}（按變身ID跨階級查） stat name 支持英文和中文，見 {@link MorphStatEntry#STAT_PARSE_MAP}。
 * @author Custom
 */
public class MorphData implements IXmlReader
{
	/** gradeLevel → MorphGradeHolder，TreeMap 保證升序 */
	private final Map<Integer, MorphGradeHolder> _grades = new TreeMap<>();

	/** itemId → MorphEntry（道具激活快速查找） */
	private final Map<Integer, MorphEntry> _itemToEntry = new ConcurrentHashMap<>();

	/**
	 * morphId → (gradeLevel → MorphEntry) 用於查詢某變身在各階級的數據（例如 UI 展示全部階級）
	 */
	private final Map<Integer, Map<Integer, MorphEntry>> _morphGradeMap = new ConcurrentHashMap<>();

	// ── 構造器 ────────────────────────────────────────────────────────────

	protected MorphData()
	{
		load();
	}

	// ── 加載 ─────────────────────────────────────────────────────────────

	@Override
	public void load()
	{
		_grades.clear();
		_itemToEntry.clear();
		_morphGradeMap.clear();
		parseDatapackFile("data/stats/TransformMorph.xml");

		int totalMorphEntries = 0;
		for (MorphGradeHolder g : _grades.values())
		{
			totalMorphEntries += g.getEntries().size();
		}
		LOGGER.info(getClass().getSimpleName() + ": 讀取 " + _grades.size() + " 變身階級, " + totalMorphEntries + " 個變身資料, " + _itemToEntry.size() + " 變身物品.");
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
				if ("grade".equalsIgnoreCase(child.getNodeName()))
				{
					parseGrade(child, file.getName());
				}
			}
		}
	}

	// ── 解析 <grade> ─────────────────────────────────────────────────────

	private void parseGrade(Node gradeNode, String fileName)
	{
		final NamedNodeMap attrs = gradeNode.getAttributes();
		if (attrs == null)
		{
			return;
		}

		final Node levelAttr = attrs.getNamedItem("level");
		if (levelAttr == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": <grade> missing 'level' in " + fileName);
			return;
		}

		int level;
		try
		{
			level = Integer.parseInt(levelAttr.getNodeValue());
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Invalid grade level in " + fileName);
			return;
		}

		// 同一 level 可以分散在多個文件，合併處理
		final MorphGradeHolder gradeHolder = _grades.computeIfAbsent(level, MorphGradeHolder::new);

		for (Node child = gradeNode.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if ("morph".equalsIgnoreCase(child.getNodeName()))
			{
				final MorphEntry entry = parseMorphEntry(child, level, fileName);
				if (entry != null)
				{
					gradeHolder.addEntry(entry);
					// 註冊 itemId → entry
					_itemToEntry.put(entry.getItemId(), entry);
					// 註冊 morphId → grade → entry
					_morphGradeMap.computeIfAbsent(entry.getMorphId(), k -> new TreeMap<>()).put(level, entry);
				}
			}
		}
	}

	// ── 解析 <morph> ─────────────────────────────────────────────────────

	private MorphEntry parseMorphEntry(Node morphNode, int gradeLevel, String fileName)
	{
		final NamedNodeMap attrs = morphNode.getAttributes();
		if (attrs == null)
		{
			return null;
		}

		// 必填屬性
		final Node idAttr = attrs.getNamedItem("id");
		final Node nameAttr = attrs.getNamedItem("name");
		final Node npcIdAttr = attrs.getNamedItem("npcId");
		final Node itemIdAttr = attrs.getNamedItem("itemId");

		if ((idAttr == null) || (nameAttr == null) || (npcIdAttr == null) || (itemIdAttr == null))
		{
			LOGGER.warning(getClass().getSimpleName() + ": <morph> missing required attribute(s) in grade=" + gradeLevel + " (" + fileName + ")");
			return null;
		}

		int morphId, npcId, itemId;
		try
		{
			morphId = Integer.parseInt(idAttr.getNodeValue());
			npcId = Integer.parseInt(npcIdAttr.getNodeValue());
			itemId = Integer.parseInt(itemIdAttr.getNodeValue());
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Invalid number in <morph> grade=" + gradeLevel + " (" + fileName + ")");
			return null;
		}

		// 可選：客戶端 transform_data 中的 transform_id（供 ExUserInfoAbnormalVisualEffect 使用）
		// 若未填寫則默認為 0（不發送變身 ID，自己看不到外觀變化）
		int transformId = 0;
		final Node transformIdAttr = attrs.getNamedItem("transformId");
		if (transformIdAttr != null)
		{
			try
			{
				transformId = Integer.parseInt(transformIdAttr.getNodeValue());
			}
			catch (NumberFormatException ignored)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Invalid transformId in <morph> id=" + morphId + " grade=" + gradeLevel + " (" + fileName + ")");
			}
		}

		// 可選：碰撞參數
		double radius = 9.0, height = 23.0;
		final Node rAttr = attrs.getNamedItem("collisionRadius");
		final Node hAttr = attrs.getNamedItem("collisionHeight");
		try
		{
			if (rAttr != null)
			{
				radius = Double.parseDouble(rAttr.getNodeValue());
			}
			if (hAttr != null)
			{
				height = Double.parseDouble(hAttr.getNodeValue());
			}
		}
		catch (NumberFormatException ignored)
		{
		}

		final MorphEntry entry = new MorphEntry(morphId, nameAttr.getNodeValue(), npcId, transformId, itemId, radius, height);

		// 可選：視覺特效
		final Node aveAttr = attrs.getNamedItem("abnormalEffects");
		if (aveAttr != null)
		{
			parseAbnormalEffects(aveAttr.getNodeValue(), entry, morphId, gradeLevel, fileName);
		}

		// 子節點 <stat>
		for (Node child = morphNode.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if ("stat".equalsIgnoreCase(child.getNodeName()))
			{
				final MorphStatEntry stat = parseStat(child, morphId, gradeLevel, fileName);
				if (stat != null)
				{
					entry.addStat(stat);
				}
			}
		}

		return entry;
	}

	// ── 解析 <stat> ──────────────────────────────────────────────────────

	private MorphStatEntry parseStat(Node statNode, int morphId, int gradeLevel, String fileName)
	{
		final NamedNodeMap attrs = statNode.getAttributes();
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
			LOGGER.warning(getClass().getSimpleName() + ": Unknown stat name '" + statName + "' in morph id=" + morphId + " grade=" + gradeLevel + " (" + fileName + ")");
			return null;
		}

		double value;
		try
		{
			value = Double.parseDouble(valueAttr.getNodeValue());
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Invalid stat value for '" + statName + "' in morph id=" + morphId + " grade=" + gradeLevel);
			return null;
		}

		final Node opAttr = attrs.getNamedItem("operation");
		final Operation op = (opAttr != null) && "mul".equalsIgnoreCase(opAttr.getNodeValue()) ? Operation.MUL : Operation.ADD;

		return new MorphStatEntry(stat, value, op);
	}

	// ── 解析 abnormalEffects 屬性值 ──────────────────────────────────────

	private void parseAbnormalEffects(String text, MorphEntry entry, int morphId, int gradeLevel, String fileName)
	{
		if ((text == null) || text.trim().isEmpty())
		{
			return;
		}
		for (String token : text.split(","))
		{
			final String name = token.trim();
			if (name.isEmpty())
			{
				continue;
			}
			try
			{
				entry.addAbnormalEffect(AbnormalVisualEffect.valueOf(name));
			}
			catch (IllegalArgumentException e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Unknown AbnormalVisualEffect '" + name + "' in morph id=" + morphId + " grade=" + gradeLevel + " (" + fileName + ")");
			}
		}
	}

	// ── 查詢接口 ─────────────────────────────────────────────────────────

	/**
	 * 按階級獲取該階級所有變身數據。
	 */
	public MorphGradeHolder getGrade(int level)
	{
		return _grades.get(level);
	}

	/**
	 * 所有階級列表（升序，不可修改）。
	 */
	public List<MorphGradeHolder> getGradeList()
	{
		return Collections.unmodifiableList(new ArrayList<>(_grades.values()));
	}

	/**
	 * 通過激活道具 ID 查找對應的變身條目（含階級信息）。
	 */
	public MorphEntry getEntryByItemId(int itemId)
	{
		return _itemToEntry.get(itemId);
	}

	/**
	 * 通過變身 ID + 階級等級獲取變身條目。
	 */
	public MorphEntry getEntry(int morphId, int gradeLevel)
	{
		final Map<Integer, MorphEntry> gradeMap = _morphGradeMap.get(morphId);
		return (gradeMap == null) ? null : gradeMap.get(gradeLevel);
	}

	/**
	 * 返回指定變身 ID 在所有階級的條目（升序，不可修改）。 用於 UI 展示該變身的全部階級。
	 */
	public List<MorphEntry> getEntriesByMorphId(int morphId)
	{
		final Map<Integer, MorphEntry> gradeMap = _morphGradeMap.get(morphId);
		if (gradeMap == null)
		{
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new ArrayList<>(gradeMap.values()));
	}

	/**
	 * 返回所有已定義的變身 ID 集合。
	 */
	public java.util.Set<Integer> getAllMorphIds()
	{
		return Collections.unmodifiableSet(_morphGradeMap.keySet());
	}

	/**
	 * 返回指定變身 ID 的最大階級（不存在返回 0）。
	 */
	public int getMaxGradeLevel(int morphId)
	{
		final Map<Integer, MorphEntry> gradeMap = _morphGradeMap.get(morphId);
		if ((gradeMap == null) || gradeMap.isEmpty())
		{
			return 0;
		}
		return ((TreeMap<Integer, MorphEntry>) gradeMap).lastKey();
	}
	
	// ── Singleton ─────────────────────────────────────────────────────────
	
	public static MorphData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MorphData INSTANCE = new MorphData();
	}
}
