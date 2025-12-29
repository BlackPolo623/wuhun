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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.SubjugationHolder;
import org.l2jmobius.gameserver.model.StatSet;

/**
 * @author Berezkin Nikolay
 */
public class SubjugationData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SubjugationData.class.getName());
	
	private static final List<SubjugationHolder> _subjugations = new ArrayList<>();
	
	public SubjugationData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_subjugations.clear();
		parseDatapackFile("data/SubjugationData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _subjugations.size() + " data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "purge", purgeNode ->
		{
			final StatSet set = new StatSet(parseAttributes(purgeNode));
			final int category = set.getInt("category");
			final List<int[]> hottimes = Arrays.stream(set.getString("hottimes").split(";")).map(it -> Arrays.stream(it.split("-")).mapToInt(Integer::parseInt).toArray()).collect(Collectors.toList());
			final Map<Integer, Integer> npcs = new HashMap<>();
			forEach(purgeNode, "npc", npcNode ->
			{
				final StatSet stats = new StatSet(parseAttributes(npcNode));
				final int npcId = stats.getInt("id");
				final int points = stats.getInt("points");
				npcs.put(npcId, points);
			});
			_subjugations.add(new SubjugationHolder(category, hottimes, npcs));
		}));
	}
	
	public SubjugationHolder getSubjugation(int category)
	{
		return _subjugations.stream().filter(it -> it.getCategory() == category).findFirst().orElse(null);
	}
	
	public static SubjugationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SubjugationData INSTANCE = new SubjugationData();
	}
}
