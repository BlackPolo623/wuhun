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
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.MagicLampDataHolder;
import org.l2jmobius.gameserver.model.StatSet;

/**
 * @author Serenitty
 */
public class MagicLampData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(MagicLampData.class.getName());
	private static final List<MagicLampDataHolder> LAMPS = new ArrayList<>();
	
	protected MagicLampData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		LAMPS.clear();
		parseDatapackFile("data/MagicLampData.xml");
		LOGGER.info("MagicLampData: Loaded " + LAMPS.size() + " magic lamps exp types.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		final NodeList list = document.getFirstChild().getChildNodes();
		for (int i = 0; i < list.getLength(); i++)
		{
			final Node n = list.item(i);
			if ("levelRange".equalsIgnoreCase(n.getNodeName()))
			{
				final int minLevel = parseInteger(n.getAttributes(), "fromLevel");
				final int maxLevel = parseInteger(n.getAttributes(), "toLevel");
				final NodeList lamps = n.getChildNodes();
				for (int j = 0; j < lamps.getLength(); j++)
				{
					final Node d = lamps.item(j);
					if ("lamp".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final StatSet set = new StatSet();
						set.set("type", parseString(attrs, "type"));
						set.set("exp", parseInteger(attrs, "exp"));
						set.set("sp", parseInteger(attrs, "sp"));
						set.set("chance", parseInteger(attrs, "chance"));
						set.set("minLevel", minLevel);
						set.set("maxLevel", maxLevel);
						LAMPS.add(new MagicLampDataHolder(set));
					}
				}
			}
		}
	}
	
	public List<MagicLampDataHolder> getLamps()
	{
		return LAMPS;
	}
	
	public static MagicLampData getInstance()
	{
		return Singleton.INSTANCE;
	}
	
	private static class Singleton
	{
		protected static final MagicLampData INSTANCE = new MagicLampData();
	}
}
