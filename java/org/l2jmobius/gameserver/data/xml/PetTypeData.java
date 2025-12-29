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
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Mobius
 */
public class PetTypeData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PetTypeData.class.getName());
	
	private final Map<Integer, SkillHolder> _skills = new HashMap<>();
	private final Map<Integer, String> _names = new HashMap<>();
	
	protected PetTypeData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_skills.clear();
		parseDatapackFile("data/PetTypes.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _skills.size() + " pet types.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "pet", petNode ->
		{
			final StatSet set = new StatSet(parseAttributes(petNode));
			final int id = set.getInt("id");
			_skills.put(id, new SkillHolder(set.getInt("skillId", 0), set.getInt("skillLvl", 0)));
			_names.put(id, set.getString("name"));
		}));
	}
	
	public SkillHolder getSkillByName(String name)
	{
		for (Entry<Integer, String> entry : _names.entrySet())
		{
			if (name.startsWith(entry.getValue()))
			{
				return _skills.get(entry.getKey());
			}
		}
		
		return null;
	}
	
	public int getIdByName(String name)
	{
		if (name == null)
		{
			return 0;
		}
		
		final int spaceIndex = name.indexOf(' ');
		final String searchName;
		if (spaceIndex != -1)
		{
			searchName = name.substring(spaceIndex + 1);
		}
		else
		{
			searchName = name;
		}
		
		for (Entry<Integer, String> entry : _names.entrySet())
		{
			if (searchName.endsWith(entry.getValue()))
			{
				return entry.getKey();
			}
		}
		
		return 0;
	}
	
	public String getNamePrefix(Integer id)
	{
		return _names.get(id);
	}
	
	public String getRandomName()
	{
		String result = null;
		final List<Entry<Integer, String>> entryList = new ArrayList<>(_names.entrySet());
		while (result == null)
		{
			final Entry<Integer, String> temp = entryList.get(Rnd.get(entryList.size()));
			if (temp.getKey() > 100)
			{
				result = temp.getValue();
			}
		}
		
		return result;
	}
	
	public Entry<Integer, SkillHolder> getRandomSkill()
	{
		Entry<Integer, SkillHolder> result = null;
		final List<Entry<Integer, SkillHolder>> entryList = new ArrayList<>(_skills.entrySet());
		while (result == null)
		{
			final Entry<Integer, SkillHolder> temp = entryList.get(Rnd.get(entryList.size()));
			if (temp.getValue().getSkillId() > 0)
			{
				result = temp;
			}
		}
		
		return result;
	}
	
	public static PetTypeData getInstance()
	{
		return PetTypeData.SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PetTypeData INSTANCE = new PetTypeData();
	}
}
