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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.EnchantSkillHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillEnchantType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Micr0
 */
public class EnchantSkillGroupsData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EnchantSkillGroupsData.class.getName());
	
	private final Map<Integer, EnchantSkillHolder> _enchantSkillHolders = new ConcurrentHashMap<>();
	private final Map<SkillHolder, Set<Integer>> _enchantSkillTrees = new ConcurrentHashMap<>();
	
	public static int MAX_ENCHANT_LEVEL;
	
	protected EnchantSkillGroupsData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_enchantSkillHolders.clear();
		parseDatapackFile("data/EnchantSkillGroups.xml");
		MAX_ENCHANT_LEVEL = _enchantSkillHolders.size();
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _enchantSkillHolders.size() + " enchant routes, max enchant set to " + MAX_ENCHANT_LEVEL + ".");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "enchant", enchantNode ->
		{
			final EnchantSkillHolder enchantSkillHolder = new EnchantSkillHolder(new StatSet(parseAttributes(enchantNode)));
			forEach(enchantNode, "sps", spsNode -> forEach(spsNode, "sp", spNode -> enchantSkillHolder.addSp(parseEnum(spNode.getAttributes(), SkillEnchantType.class, "type"), parseInteger(spNode.getAttributes(), "amount"))));
			forEach(enchantNode, "chances", chancesNode -> forEach(chancesNode, "chance", chanceNode -> enchantSkillHolder.addChance(parseEnum(chanceNode.getAttributes(), SkillEnchantType.class, "type"), parseInteger(chanceNode.getAttributes(), "value"))));
			forEach(enchantNode, "items", itemsNode -> forEach(itemsNode, "item", itemNode -> enchantSkillHolder.addRequiredItem(parseEnum(itemNode.getAttributes(), SkillEnchantType.class, "type"), new ItemHolder(new StatSet(parseAttributes(itemNode))))));
			_enchantSkillHolders.put(parseInteger(enchantNode.getAttributes(), "level"), enchantSkillHolder);
		}));
	}
	
	/**
	 * Adds an enchant route for a specific skill ID, level, and route.
	 * @param skillId the ID of the skill.
	 * @param level the level of the skill.
	 * @param route the route to add for this skill.
	 */
	public void addRouteForSkill(int skillId, int level, int route)
	{
		addRouteForSkill(new SkillHolder(skillId, level), route);
	}
	
	/**
	 * Adds an enchant route for a specific skill holder and route.
	 * @param holder the skill holder representing the skill.
	 * @param route the route to add for this skill holder.
	 */
	public void addRouteForSkill(SkillHolder holder, int route)
	{
		_enchantSkillTrees.computeIfAbsent(holder, _ -> new HashSet<>()).add(route);
	}
	
	/**
	 * Retrieves the set of enchant routes available for a specific skill ID and level.
	 * @param skillId the ID of the skill.
	 * @param level the level of the skill.
	 * @return a set of available enchant routes for the specified skill and level, or an empty set if none exist.
	 */
	public Set<Integer> getRouteForSkill(int skillId, int level)
	{
		return getRouteForSkill(skillId, level, 0);
	}
	
	/**
	 * Retrieves the set of enchant routes available for a specific skill ID, level, and sub-level.
	 * @param skillId the ID of the skill.
	 * @param level the level of the skill.
	 * @param subLevel the sub-level of the skill.
	 * @return a set of available enchant routes for the specified skill, level, and sub-level, or an empty set if none exist.
	 */
	public Set<Integer> getRouteForSkill(int skillId, int level, int subLevel)
	{
		return getRouteForSkill(new SkillHolder(skillId, level, subLevel));
	}
	
	/**
	 * Retrieves the set of enchant routes for a specific skill holder.
	 * @param holder the skill holder representing the skill.
	 * @return a set of available enchant routes for the specified skill holder, or an empty set if none exist.
	 */
	public Set<Integer> getRouteForSkill(SkillHolder holder)
	{
		return _enchantSkillTrees.getOrDefault(holder, Collections.emptySet());
	}
	
	/**
	 * Checks if a specific skill is enchantable.
	 * @param skill the skill to check.
	 * @return {@code true} if the skill is enchantable, {@code false} otherwise.
	 */
	public boolean isEnchantable(Skill skill)
	{
		return isEnchantable(new SkillHolder(skill.getId(), skill.getLevel()));
	}
	
	/**
	 * Checks if a specific skill holder is enchantable.
	 * @param holder the skill holder representing the skill.
	 * @return {@code true} if the skill holder is enchantable, {@code false} otherwise.
	 */
	public boolean isEnchantable(SkillHolder holder)
	{
		return _enchantSkillTrees.containsKey(holder);
	}
	
	/**
	 * Retrieves the enchant skill holder associated with a specific enchant level.
	 * @param level the enchant level.
	 * @return the {@link EnchantSkillHolder} for the specified level, or {@code null} if none exists.
	 */
	public EnchantSkillHolder getEnchantSkillHolder(int level)
	{
		return _enchantSkillHolders.getOrDefault(level, null);
	}
	
	public static EnchantSkillGroupsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantSkillGroupsData INSTANCE = new EnchantSkillGroupsData();
	}
}
