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
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.PetExtractionHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Geremy
 */
public class PetExtractData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PetExtractData.class.getName());
	
	// <Pet_Id, <Pet_Level, Cost>>
	private final Map<Integer, Map<Integer, PetExtractionHolder>> _extractionData = new HashMap<>();
	
	protected PetExtractData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_extractionData.clear();
		parseDatapackFile("data/PetExtractData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _extractionData.size() + " pet extraction data.");
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
					if ("extraction".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final int petId = parseInteger(attrs, "petId");
						final int petLevel = parseInteger(attrs, "petLevel");
						final long extractExp = parseLong(attrs, "extractExp");
						final int extractItem = parseInteger(attrs, "extractItem");
						final int defaultCostId = parseInteger(attrs, "defaultCostId");
						final int defaultCostCount = parseInteger(attrs, "defaultCostCount");
						final int extractCostId = parseInteger(attrs, "extractCostId");
						final int extractCostCount = parseInteger(attrs, "extractCostCount");
						Map<Integer, PetExtractionHolder> data = _extractionData.get(petId);
						if (data == null)
						{
							data = new HashMap<>();
							_extractionData.put(petId, data);
						}
						
						data.put(petLevel, new PetExtractionHolder(petId, petLevel, extractExp, extractItem, new ItemHolder(defaultCostId, defaultCostCount), new ItemHolder(extractCostId, extractCostCount)));
					}
				}
			}
		}
	}
	
	public PetExtractionHolder getExtraction(int petId, int petLevel)
	{
		final Map<Integer, PetExtractionHolder> map = _extractionData.get(petId);
		if (map == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Missing pet extraction data: [PetId: " + petId + "] [PetLevel: " + petLevel + "]");
			return null;
		}
		
		return map.get(petLevel);
	}
	
	public static PetExtractData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PetExtractData INSTANCE = new PetExtractData();
	}
}
