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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.holders.PetSkillAcquireHolder;

/**
 * @author Berezkin Nikolay
 */
public class PetAcquireList implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PetAcquireList.class.getName());
	
	private final Map<Integer, List<PetSkillAcquireHolder>> _skills = new HashMap<>();
	
	protected PetAcquireList()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_skills.clear();
		parseDatapackFile("data/PetAcquireList.xml");
		
		if (!_skills.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _skills.size() + " pet skills.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("pet".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						Node att;
						final StatSet set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final int type = parseInteger(attrs, "type");
						final List<PetSkillAcquireHolder> list = new ArrayList<>();
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							attrs = b.getAttributes();
							if ("skill".equalsIgnoreCase(b.getNodeName()))
							{
								final int skillId = parseInteger(attrs, "id");
								final int skillLvl = parseInteger(attrs, "lvl");
								final int reqLvl = parseInteger(attrs, "reqLvl");
								final int evolve = parseInteger(attrs, "evolve");
								
								// Handle multiple item values.
								final List<ItemHolder> items = new ArrayList<>();
								final String itemAttr = parseString(attrs, "item");
								final String itemAmountAttr = parseString(attrs, "itemAmount");
								if ((itemAttr != null) && (itemAmountAttr != null))
								{
									final String[] itemIds = itemAttr.split(";");
									final String[] itemAmounts = itemAmountAttr.split(";");
									if (itemIds.length == itemAmounts.length)
									{
										for (int i = 0; i < itemIds.length; i++)
										{
											final int itemId = Integer.parseInt(itemIds[i].trim());
											final long itemAmount = Long.parseLong(itemAmounts[i].trim());
											items.add(new ItemHolder(itemId, itemAmount));
										}
									}
									else
									{
										LOGGER.warning("Mismatch in item and itemAmount counts for skill ID: " + skillId);
									}
								}
								
								list.add(new PetSkillAcquireHolder(skillId, skillLvl, reqLvl, evolve, items));
							}
						}
						
						_skills.put(type, list);
					}
				}
			}
		}
	}
	
	public List<PetSkillAcquireHolder> getSkills(int type)
	{
		return _skills.get(type);
	}
	
	public Map<Integer, List<PetSkillAcquireHolder>> getAllSkills()
	{
		return _skills;
	}
	
	public int getSpecialSkillByType(int petType)
	{
		switch (petType)
		{
			case 15:
			{
				return 49001;
			}
			case 14:
			{
				return 49011;
			}
			case 12:
			{
				return 49021;
			}
			case 13:
			{
				return 49031;
			}
			case 17:
			{
				return 49041;
			}
			case 16:
			{
				return 49051;
			}
			default:
			{
				throw new IllegalStateException("Unexpected value: " + petType);
			}
		}
	}
	
	public static PetAcquireList getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PetAcquireList INSTANCE = new PetAcquireList();
	}
}
