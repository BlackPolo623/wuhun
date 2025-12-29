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
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;

/**
 * @author Mobius
 */
public class ClanLevelData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ClanLevelData.class.getName());
	
	private static final int EXPECTED_CLAN_LEVEL_DATA = 16; // Level 0 included.
	
	private int[] CLAN_EXP;
	private int MAX_CLAN_LEVEL = 0;
	private int MAX_CLAN_EXP = 0;
	
	protected ClanLevelData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		CLAN_EXP = new int[EXPECTED_CLAN_LEVEL_DATA];
		MAX_CLAN_LEVEL = 0;
		MAX_CLAN_EXP = 0;
		
		parseDatapackFile("data/ClanLevelData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + (EXPECTED_CLAN_LEVEL_DATA - 1) /* level 0 excluded */ + " clan level data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("clan".equals(d.getNodeName()))
					{
						NamedNodeMap attrs = d.getAttributes();
						
						final int level = parseInteger(attrs, "level");
						final int exp = parseInteger(attrs, "exp");
						
						if (MAX_CLAN_LEVEL < level)
						{
							MAX_CLAN_LEVEL = level;
						}
						
						if (MAX_CLAN_EXP < exp)
						{
							MAX_CLAN_EXP = exp;
						}
						
						CLAN_EXP[level] = exp;
					}
				}
			}
		}
	}
	
	public int getLevelExp(int clanLevel)
	{
		return CLAN_EXP[clanLevel];
	}
	
	public int getMaxLevel()
	{
		return MAX_CLAN_LEVEL;
	}
	
	public int getMaxExp()
	{
		return MAX_CLAN_EXP;
	}
	
	public static ClanLevelData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanLevelData INSTANCE = new ClanLevelData();
	}
}
