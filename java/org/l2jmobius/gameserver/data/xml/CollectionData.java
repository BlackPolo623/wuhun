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
import org.l2jmobius.gameserver.data.holders.CollectionDataHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.holders.ItemEnchantHolder;

/**
 * @author Berezkin Nikolay
 */
public class CollectionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CollectionData.class.getName());
	
	private final Map<Integer, CollectionDataHolder> _collections = new HashMap<>();
	private final Map<Integer, List<CollectionDataHolder>> _collectionsByTabId = new HashMap<>();
	
	protected CollectionData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_collections.clear();
		parseDatapackFile("data/CollectionData.xml");
		
		if (!_collections.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _collections.size() + " collections.");
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
					if ("collection".equalsIgnoreCase(d.getNodeName()))
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
						final List<ItemEnchantHolder> items = new ArrayList<>();
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							attrs = b.getAttributes();
							if ("item".equalsIgnoreCase(b.getNodeName()))
							{
								final int itemId = parseInteger(attrs, "id");
								final long itemCount = parseLong(attrs, "count", 1L);
								final int itemEnchantLevel = parseInteger(attrs, "enchantLevel", 0);
								final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
								if (item == null)
								{
									LOGGER.severe(getClass().getSimpleName() + ": Item template null for itemId: " + itemId + " collection item: " + id);
									continue;
								}
								
								items.add(new ItemEnchantHolder(itemId, itemCount, itemEnchantLevel));
							}
						}
						
						final CollectionDataHolder template = new CollectionDataHolder(id, optionId, category, completeCount, items);
						_collections.put(id, template);
						_collectionsByTabId.computeIfAbsent(template.getCategory(), _ -> new ArrayList<>()).add(template);
					}
				}
			}
		}
	}
	
	public CollectionDataHolder getCollection(int id)
	{
		return _collections.get(id);
	}
	
	public List<CollectionDataHolder> getCollectionsByTabId(int tabId)
	{
		if (_collectionsByTabId.containsKey(tabId))
		{
			return _collectionsByTabId.get(tabId);
		}
		
		return Collections.emptyList();
	}
	
	public Collection<CollectionDataHolder> getCollections()
	{
		return _collections.values();
	}
	
	public static CollectionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CollectionData INSTANCE = new CollectionData();
	}
}
