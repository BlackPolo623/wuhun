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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.data.holders.RelicCollectionDataHolder;
import org.l2jmobius.gameserver.data.holders.RelicCollectionInfoHolder;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.model.StatSet;

/**
 * @author CostyKiller
 */
public class RelicCollectionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RelicCollectionData.class.getName());
	
	private static final Map<Integer, RelicCollectionDataHolder> RELIC_COLLECTIONS = new HashMap<>();
	private static final Map<Integer, List<RelicCollectionDataHolder>> RELIC_COLLECTION_CATEGORIES = new HashMap<>();
	
	protected RelicCollectionData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		RELIC_COLLECTIONS.clear();
		
		if (RelicSystemConfig.RELIC_SYSTEM_ENABLED)
		{
			parseDatapackFile("data/RelicCollectionData.xml");
		}
		
		if (!RELIC_COLLECTIONS.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + RELIC_COLLECTIONS.size() + " relic collections.");
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
					if ("relicCollection".equalsIgnoreCase(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						Node att;
						final StatSet set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final int id = parseInteger(attrs, "id");
						final int optionId = parseInteger(attrs, "optionId");
						final int category = parseInteger(attrs, "category");
						final int completeCount = parseInteger(attrs, "completeCount");
						final int combatPower = parseInteger(attrs, "combatPower", 0);
						final List<RelicCollectionInfoHolder> relics = new ArrayList<>();
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							attrs = b.getAttributes();
							if ("relic".equalsIgnoreCase(b.getNodeName()))
							{
								final int relicId = parseInteger(attrs, "id");
								final int enchantLevel = parseInteger(attrs, "enchantLevel", 0);
								final RelicDataHolder relic = RelicData.getInstance().getRelic(relicId);
								if (relic == null)
								{
									LOGGER.severe(getClass().getSimpleName() + ": Relic null for relicId: " + relicId + " relics collection item: " + id);
									continue;
								}
								
								relics.add(new RelicCollectionInfoHolder(relicId, enchantLevel));
							}
						}
						
						final RelicCollectionDataHolder template = new RelicCollectionDataHolder(id, optionId, category, completeCount, combatPower, relics);
						RELIC_COLLECTIONS.put(id, template);
						RELIC_COLLECTION_CATEGORIES.computeIfAbsent(template.getCategory(), _ -> new ArrayList<>()).add(template);
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the relic collection associated with the specified collection ID.
	 * @param id the unique ID of the relic collection
	 * @return the {@code RelicCollectionDataHolder} containing the details of the relic collection, or {@code null} if no collection is associated with the specified ID
	 */
	public RelicCollectionDataHolder getRelicCollection(int id)
	{
		return RELIC_COLLECTIONS.get(id);
	}
	
	/**
	 * Retrieves a list of relic collections within a specified category.
	 * @param tabId the ID of the category to retrieve
	 * @return a list of {@code RelicCollectionDataHolder} objects in the specified category, or an empty list if the category ID has no associated relic collections
	 */
	public List<RelicCollectionDataHolder> getRelicCategory(int tabId)
	{
		if (RELIC_COLLECTION_CATEGORIES.containsKey(tabId))
		{
			return RELIC_COLLECTION_CATEGORIES.get(tabId);
		}
		
		return Collections.emptyList();
	}
	
	/**
	 * Retrieves all available relic collections.
	 * @return a collection of {@code RelicCollectionDataHolder} objects representing all relic collections
	 */
	public Collection<RelicCollectionDataHolder> getRelicCollections()
	{
		return RELIC_COLLECTIONS.values();
	}
	
	public static RelicCollectionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RelicCollectionData INSTANCE = new RelicCollectionData();
	}
}
