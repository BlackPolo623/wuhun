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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.EnchantItemExpHolder;
import org.l2jmobius.gameserver.data.holders.EnchantStarHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillEnchantHolder;

/**
 * @author Serenitty
 */
public class SkillEnchantData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SkillEnchantData.class.getName());
	
	private final Map<Integer, EnchantStarHolder> _enchantStarMap = new HashMap<>();
	private final Map<Integer, SkillEnchantHolder> _skillEnchantMap = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, EnchantItemExpHolder>> _enchantItemMap = new HashMap<>();
	
	private final Map<Integer, Integer> _chanceEnchantMap = new HashMap<>();
	
	public SkillEnchantData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/SkillEnchantData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _enchantStarMap.size() + " star levels.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _enchantItemMap.size() + " enchant items.");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _skillEnchantMap.size() + " skill enchants.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "skills", skills -> forEach(skills, "skill", skill ->
			{
				final StatSet set = new StatSet(parseAttributes(skill));
				final int id = set.getInt("id");
				final int starLevel = set.getInt("starLevel");
				final int maxEnchantLevel = set.getInt("maxEnchantLevel");
				_skillEnchantMap.put(id, new SkillEnchantHolder(id, starLevel, maxEnchantLevel));
			}));
			forEach(listNode, "stars", stars -> forEach(stars, "star", star ->
			{
				final StatSet set = new StatSet(parseAttributes(star));
				final int level = set.getInt("level");
				final EnchantStarHolder starHolder = new EnchantStarHolder(set);
				_enchantStarMap.put(level, starHolder);
			}));
			forEach(listNode, "chances", itemsPoints -> forEach(itemsPoints, "chance", item ->
			{
				final StatSet set = new StatSet(parseAttributes(item));
				final int enchantLevel = set.getInt("enchantLevel");
				final int chance = set.getInt("chance");
				_chanceEnchantMap.put(enchantLevel, chance);
			}));
			forEach(listNode, "itemsPoints", itemsPoints -> forEach(itemsPoints, "star", star ->
			{
				final StatSet set = new StatSet(parseAttributes(star));
				final int level = set.getInt("level");
				final Map<Integer, EnchantItemExpHolder> itemMap = new HashMap<>();
				forEach(star, "item", item ->
				{
					final StatSet statSet = new StatSet(parseAttributes(item));
					final int id = statSet.getInt("id");
					itemMap.put(id, new EnchantItemExpHolder(statSet));
				});
				_enchantItemMap.put(level, itemMap);
			}));
		});
	}
	
	public EnchantStarHolder getEnchantStar(int level)
	{
		return _enchantStarMap.get(level);
	}
	
	public SkillEnchantHolder getSkillEnchant(int id)
	{
		return _skillEnchantMap.get(id);
	}
	
	public EnchantItemExpHolder getEnchantItem(int level, int id)
	{
		return _enchantItemMap.get(level).get(id);
	}
	
	public Map<Integer, EnchantItemExpHolder> getEnchantItem(int level)
	{
		return _enchantItemMap.get(level);
	}
	
	public void addReplacedSkillEnchant(int oldId, int newId)
	{
		if (!_skillEnchantMap.containsKey(oldId) || _skillEnchantMap.containsKey(newId))
		{
			return;
		}
		
		final SkillEnchantHolder oldHolder = _skillEnchantMap.get(oldId);
		_skillEnchantMap.put(newId, new SkillEnchantHolder(newId, oldHolder.getStarLevel(), oldHolder.getMaxEnchantLevel()));
	}
	
	public int getChanceEnchantMap(Skill skill)
	{
		final int enchantLevel = skill.getSubLevel() == 0 ? 1 : (skill.getSubLevel() + 1) - 1000;
		if (enchantLevel > getSkillEnchant(skill.getId()).getMaxEnchantLevel())
		{
			return 0;
		}
		
		return _chanceEnchantMap.get(enchantLevel);
	}
	
	public static SkillEnchantData getInstance()
	{
		return SkillEnchantData.SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillEnchantData INSTANCE = new SkillEnchantData();
	}
}
