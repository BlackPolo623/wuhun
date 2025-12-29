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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;

/**
 * @author Berezkin Nikolay
 */
public class SubjugationGacha implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SubjugationGacha.class.getName());
	
	private static final Map<Integer, Map<Integer, Double>> _subjugations = new HashMap<>();
	
	public SubjugationGacha()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_subjugations.clear();
		parseDatapackFile("data/SubjugationGacha.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _subjugations.size() + " data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "purge", purgeNode ->
		{
			final StatSet set = new StatSet(parseAttributes(purgeNode));
			final int category = set.getInt("category");
			final Map<Integer, Double> items = new LinkedHashMap<>();
			forEach(purgeNode, "item", npcNode ->
			{
				final StatSet stats = new StatSet(parseAttributes(npcNode));
				final int itemId = stats.getInt("id");
				final double rate = stats.getDouble("rate");
				items.put(itemId, rate);
			});
			_subjugations.put(category, items);
		}));
	}
	
	public Map<Integer, Double> getSubjugation(int category)
	{
		return _subjugations.get(category);
	}
	
	public static SubjugationGacha getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SubjugationGacha INSTANCE = new SubjugationGacha();
	}
}
