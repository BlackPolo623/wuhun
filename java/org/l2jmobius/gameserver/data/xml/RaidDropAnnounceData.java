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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.ItemTemplate;

/**
 * @author MacuK
 */
public class RaidDropAnnounceData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RaidDropAnnounceData.class.getName());
	
	private final Set<Integer> _itemIds = new HashSet<>();
	
	protected RaidDropAnnounceData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_itemIds.clear();
		parseDatapackFile("data/RaidDropAnnounceData.xml");
		if (!_itemIds.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _itemIds.size() + " raid drop announce data.");
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
					if ("item".equalsIgnoreCase(d.getNodeName()))
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
						final ItemTemplate item = ItemData.getInstance().getTemplate(id);
						if (item != null)
						{
							_itemIds.add(id);
						}
						else
						{
							LOGGER.warning(getClass().getSimpleName() + ": Could not find item with id: " + id);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Checks if an item with the specified item ID is in the list of announced items.
	 * @param itemId the ID of the item to check
	 * @return {@code true} if the item ID exists in the list of announced items, {@code false} otherwise
	 */
	public boolean isAnnounce(int itemId)
	{
		return _itemIds.contains(itemId);
	}
	
	public static RaidDropAnnounceData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RaidDropAnnounceData INSTANCE = new RaidDropAnnounceData();
	}
}
