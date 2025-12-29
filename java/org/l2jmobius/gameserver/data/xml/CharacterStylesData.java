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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.l2jmobius.gameserver.model.item.type.ArmorType;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.enums.CharacterStyleCategoryType;
import org.l2jmobius.gameserver.data.holders.CharacterStyleDataHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.type.WeaponType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Brado
 */
public class CharacterStylesData implements IXmlReader
{
	private final Map<CharacterStyleCategoryType, List<CharacterStyleDataHolder>> STYLES = new HashMap<>();
	private final Map<CharacterStyleCategoryType, ItemHolder> SWAP_COST_HOLDER = new HashMap<>();
	
	private final ConcurrentMap<Integer, Integer> _cachedWeaponMap = new ConcurrentHashMap<>();
	private final ConcurrentMap<Integer, SkillHolder> _cachedKillEffectMap = new ConcurrentHashMap<>();
	
	public CharacterStylesData()
	{
		load();
		buildCacheMaps();
	}
	
	@Override
	public void load()
	{
		STYLES.clear();
		SWAP_COST_HOLDER.clear();
		parseDatapackFile("data/CharacterStylesData.xml");
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + STYLES.size() + " Character Styles.");
		for (CharacterStyleCategoryType type : STYLES.keySet())
		{
			LOGGER.info(getClass().getSimpleName() + ":   " + type + " -> " + STYLES.get(type).size() + " styles.");
		}
	}
	
	public void loadDatapack(String relativePath)
	{
		parseDatapackFile(relativePath);
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		final Node root = document.getDocumentElement();
		if (root == null)
		{
			return;
		}
		
		forEach(root, "category", categoryNode ->
		{
			final NamedNodeMap catAttr = categoryNode.getAttributes();
			final String typeStr = parseString(catAttr, "type", null);
			if ((typeStr == null) || typeStr.isEmpty())
			{
				return;
			}
			
			final CharacterStyleCategoryType type = CharacterStyleCategoryType.from(typeStr);
			final List<CharacterStyleDataHolder> stylesList = STYLES.computeIfAbsent(type, k -> new ArrayList<>());
			
			final Integer styleCostId = parseInteger(catAttr, "swapCostId", 0);
			final Long styleCostCount = parseLong(catAttr, "swapCostCount", 0L);
			if ((styleCostId != null) && (styleCostCount != null) && (styleCostId > 0) && (styleCostCount > 0))
			{
				SWAP_COST_HOLDER.put(type, new ItemHolder(styleCostId, styleCostCount));
			}
			forEach(categoryNode, "style", styleNode ->
			{
				final NamedNodeMap sAttr = styleNode.getAttributes();
				final Integer styleId = parseInteger(sAttr, "styleId");
				if (styleId == null)
				{
					return;
				}
				final String name = parseString(sAttr, "name", "");
				final Integer shiftWeaponId = parseInteger(sAttr, "shiftWeaponId", 0);
				final WeaponType weaponType = parseEnum(sAttr, WeaponType.class, "weaponType", WeaponType.NONE);
				final Integer skillId = parseInteger(sAttr, "skillId", 0);
				final Integer skillLevel = parseInteger(sAttr, "skillLevel", 0);
				
				final List<ItemHolder> cost = new ArrayList<>();
				forEach(styleNode, "cost", costNode -> forEach(costNode, "item", itemNode ->
				{
					final NamedNodeMap iAttr = itemNode.getAttributes();
					final Integer itemId = parseInteger(iAttr, "id");
					final Long count = parseLong(iAttr, "count", 1L);
					if (itemId != null)
					{
						cost.add(new ItemHolder(itemId, (count != null) ? count : 1L));
					}
				}));
				CharacterStyleDataHolder holder = null;
				if (type == CharacterStyleCategoryType.APPEARANCE_WEAPON)
				{
					holder = new CharacterStyleDataHolder(styleId, name, shiftWeaponId, weaponType, cost);
				}
				else if (type == CharacterStyleCategoryType.APPEARANCE_ARMOR)  // ★ 新增
				{
					final Integer shiftArmorId = parseInteger(sAttr, "shiftArmorId", 0);
					holder = new CharacterStyleDataHolder(styleId, name, shiftArmorId, cost);
				}
				else if (type == CharacterStyleCategoryType.KILL_EFFECT)
				{
					final SkillHolder sHolder = new SkillHolder(skillId, skillLevel);
					holder = new CharacterStyleDataHolder(styleId, name, sHolder, cost);
				}
				else
				{
					holder = new CharacterStyleDataHolder(styleId, name, cost);
				}
				stylesList.add(holder);
			});
		});
	}
	
	public void buildCacheMaps()
	{
		_cachedWeaponMap.clear();
		_cachedKillEffectMap.clear();
		for (CharacterStyleCategoryType styleType : CharacterStyleCategoryType.values())
		{
			final List<CharacterStyleDataHolder> list = STYLES.get(styleType);
			if (list != null && list.size() > 0)
			{
				if (styleType == CharacterStyleCategoryType.APPEARANCE_WEAPON)
				{
					for (CharacterStyleDataHolder listEntry : list)
					{
						_cachedWeaponMap.putIfAbsent(listEntry.getStyleId(), listEntry.getShiftWeaponId());
					}
				}
				else if (styleType == CharacterStyleCategoryType.KILL_EFFECT)
				{
					for (CharacterStyleDataHolder listEntry : list)
					{
						_cachedKillEffectMap.putIfAbsent(listEntry.getStyleId(), listEntry.getSkillHolder());
					}
				}
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Cached " + _cachedWeaponMap.size() + " Weapon Styles.");
		LOGGER.info(getClass().getSimpleName() + ": Cached " + _cachedKillEffectMap.size() + " Kill Effect Styles.");
	}
	
	public int getWeaponStyleByStyleId(int styleId)
	{
		if (_cachedWeaponMap.containsKey(styleId))
		{
			return _cachedWeaponMap.get(styleId);
		}
		return 0;
	}

	public int getArmorStyleByStyleId(int styleId)
	{
		final CharacterStyleDataHolder holder = getSpecificStyleByCategoryAndId(CharacterStyleCategoryType.APPEARANCE_ARMOR, styleId);
		if (holder != null)
		{
			return holder.getShiftArmorId();
		}
		return 0;
	}
	
	public SkillHolder getKillEffectStyleByStyleId(int styleId)
	{
		if (_cachedKillEffectMap.containsKey(styleId))
		{
			return _cachedKillEffectMap.get(styleId);
		}
		return null;
	}
	
	public List<CharacterStyleDataHolder> getStylesByCategory(CharacterStyleCategoryType category)
	{
		return STYLES.getOrDefault(category, Collections.emptyList());
	}
	
	public CharacterStyleDataHolder getSpecificStyleByCategoryAndId(CharacterStyleCategoryType category, int styleId)
	{
		for (CharacterStyleDataHolder holder : getStylesByCategory(category))
		{
			if (holder._styleId == styleId)
			{
				return holder;
			}
		}
		return null;
	}
	
	public ItemHolder getSwapCostItemByCategory(CharacterStyleCategoryType category)
	{
		return SWAP_COST_HOLDER.get(category);
	}
	
	public static CharacterStylesData getInstance()
	{
		return Singleton.INSTANCE;
	}
	
	private static class Singleton
	{
		protected static final CharacterStylesData INSTANCE = new CharacterStylesData();
	}
}
