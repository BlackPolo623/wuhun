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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.holders.AgathionSkillHolder;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author Mobius
 */
public class AgathionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(AgathionData.class.getName());
	
	private static final Map<Integer, AgathionSkillHolder> AGATHION_SKILLS = new HashMap<>();
	
	protected AgathionData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		AGATHION_SKILLS.clear();
		parseDatapackFile("data/AgathionData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + AGATHION_SKILLS.size() + " agathion data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "agathion", agathionNode ->
		{
			// Parse attributes into a StatSet.
			final StatSet set = new StatSet(parseAttributes(agathionNode));
			
			final int id = set.getInt("id");
			if (ItemData.getInstance().getTemplate(id) == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Could not find agathion with id " + id + ".");
				return;
			}
			
			final int enchant = set.getInt("enchant", 0);
			
			// Process main skills.
			final Map<Integer, List<Skill>> mainSkills = AGATHION_SKILLS.containsKey(id) ? AGATHION_SKILLS.get(id).getMainSkills() : new HashMap<>();
			final List<Skill> mainSkillList = new ArrayList<>();
			final String main = set.getString("mainSkill", "");
			
			for (String ids : main.split(";"))
			{
				if (ids.isEmpty())
				{
					continue;
				}
				
				final String[] split = ids.split(",");
				final int skillId = Integer.parseInt(split[0]);
				final int level = Integer.parseInt(split[1]);
				
				final Skill skill = SkillData.getInstance().getSkill(skillId, level);
				if (skill == null)
				{
					LOGGER.info(getClass().getSimpleName() + ": Could not find agathion skill id " + skillId + ".");
					return;
				}
				
				mainSkillList.add(skill);
			}
			
			mainSkills.put(enchant, mainSkillList);
			
			// Process sub skills.
			final Map<Integer, List<Skill>> subSkills = AGATHION_SKILLS.containsKey(id) ? AGATHION_SKILLS.get(id).getSubSkills() : new HashMap<>();
			final List<Skill> subSkillList = new ArrayList<>();
			final String sub = set.getString("subSkill", "");
			
			for (String ids : sub.split(";"))
			{
				if (ids.isEmpty())
				{
					continue;
				}
				
				final String[] split = ids.split(",");
				final int skillId = Integer.parseInt(split[0]);
				final int level = Integer.parseInt(split[1]);
				
				final Skill skill = SkillData.getInstance().getSkill(skillId, level);
				if (skill == null)
				{
					LOGGER.info(getClass().getSimpleName() + ": Could not find agathion skill id " + skillId + ".");
					return;
				}
				
				subSkillList.add(skill);
			}
			
			subSkills.put(enchant, subSkillList);
			
			// Add agathion skills to AGATHION_SKILLS map.
			AGATHION_SKILLS.put(id, new AgathionSkillHolder(mainSkills, subSkills));
		}));
	}
	
	public AgathionSkillHolder getSkills(int agathionId)
	{
		return AGATHION_SKILLS.get(agathionId);
	}
	
	public static AgathionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AgathionData INSTANCE = new AgathionData();
	}
}
