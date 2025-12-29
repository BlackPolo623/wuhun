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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.model.Location;

/**
 * @author Mobius
 */
public class TimedHuntingZoneData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(TimedHuntingZoneData.class.getName());
	
	private final Map<Integer, TimedHuntingZoneHolder> _timedHuntingZoneData = new HashMap<>();
	
	protected TimedHuntingZoneData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_timedHuntingZoneData.clear();
		parseDatapackFile("data/TimedHuntingZoneData.xml");
		
		if (!_timedHuntingZoneData.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _timedHuntingZoneData.size() + " timed hunting zones.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node xmlNode = document.getFirstChild(); xmlNode != null; xmlNode = xmlNode.getNextSibling())
		{
			if ("list".equalsIgnoreCase(xmlNode.getNodeName()))
			{
				final NamedNodeMap listAttributes = xmlNode.getAttributes();
				final Node attribute = listAttributes.getNamedItem("enabled");
				if ((attribute != null) && Boolean.parseBoolean(attribute.getNodeValue()))
				{
					for (Node listNode = xmlNode.getFirstChild(); listNode != null; listNode = listNode.getNextSibling())
					{
						if ("zone".equalsIgnoreCase(listNode.getNodeName()))
						{
							final NamedNodeMap zoneAttributes = listNode.getAttributes();
							int id = parseInteger(zoneAttributes, "id");
							String name = parseString(zoneAttributes, "name", "");
							int initialTime = 0;
							int maxAddedTime = 0;
							int resetDelay = 0;
							int entryItemId = 57;
							int entryFee = 10000;
							int minLevel = 1;
							int maxLevel = 999;
							int remainRefillTime = 3600;
							int refillTimeMax = 3600;
							boolean pvpZone = false;
							boolean noPvpZone = false;
							int instanceId = 0;
							boolean soloInstance = true;
							boolean weekly = false;
							boolean useWorldPrefix = false;
							boolean zonePremiumUserOnly = false;
							Location enterLocation = null;
							Location exitLocation = null;
							boolean isEvenWeek = true;
							boolean isSwapWeek = false;
							for (Node zoneNode = listNode.getFirstChild(); zoneNode != null; zoneNode = zoneNode.getNextSibling())
							{
								switch (zoneNode.getNodeName())
								{
									case "enterLocation":
									{
										final String[] coordinates = zoneNode.getTextContent().split(",");
										enterLocation = new Location(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2]));
										break;
									}
									case "exitLocation":
									{
										final String[] coordinates = zoneNode.getTextContent().split(",");
										exitLocation = new Location(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2]));
										break;
									}
									case "initialTime":
									{
										initialTime = Integer.parseInt(zoneNode.getTextContent()) * 1000;
										break;
									}
									case "maxAddedTime":
									{
										maxAddedTime = Integer.parseInt(zoneNode.getTextContent()) * 1000;
										break;
									}
									case "resetDelay":
									{
										resetDelay = Integer.parseInt(zoneNode.getTextContent()) * 1000;
										break;
									}
									case "entryItemId":
									{
										entryItemId = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "entryFee":
									{
										entryFee = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "minLevel":
									{
										minLevel = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "maxLevel":
									{
										maxLevel = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "remainRefillTime":
									{
										remainRefillTime = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "refillTimeMax":
									{
										refillTimeMax = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "pvpZone":
									{
										pvpZone = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									case "noPvpZone":
									{
										noPvpZone = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									case "instanceId":
									{
										instanceId = Integer.parseInt(zoneNode.getTextContent());
										break;
									}
									case "soloInstance":
									{
										soloInstance = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									case "weekly":
									{
										weekly = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									case "useWorldPrefix":
									{
										useWorldPrefix = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									case "zonePremiumUserOnly":
									{
										zonePremiumUserOnly = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									
									case "isEvenWeek":
									{
										isEvenWeek = Boolean.parseBoolean(zoneNode.getTextContent());
										break;
									}
									case "isSwapWeek":
									{
										isSwapWeek = Boolean.parseBoolean(zoneNode.getTextContent());
									}
								}
							}
							
							_timedHuntingZoneData.put(id, new TimedHuntingZoneHolder(id, name, initialTime, maxAddedTime, resetDelay, entryItemId, entryFee, minLevel, maxLevel, remainRefillTime, refillTimeMax, pvpZone, noPvpZone, instanceId, soloInstance, weekly, useWorldPrefix, zonePremiumUserOnly, enterLocation, exitLocation, isEvenWeek, isSwapWeek));
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the hunting zone data for the specified zone ID.
	 * @param zoneId the unique identifier of the hunting zone
	 * @return the {@link TimedHuntingZoneHolder} associated with the given zone ID, or {@code null} if no data exists for the specified ID
	 */
	public TimedHuntingZoneHolder getHuntingZone(int zoneId)
	{
		return _timedHuntingZoneData.get(zoneId);
	}
	
	/**
	 * Retrieves a collection of all available hunting zones.
	 * @return a {@link Collection} containing all {@link TimedHuntingZoneHolder} instances representing each hunting zone
	 */
	public Collection<TimedHuntingZoneHolder> getAllHuntingZones()
	{
		return _timedHuntingZoneData.values();
	}
	
	/**
	 * Gets the total number of configured hunting zones.
	 * @return the count of hunting zones available in the data
	 */
	public int getSize()
	{
		return _timedHuntingZoneData.size();
	}
	
	public static TimedHuntingZoneData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TimedHuntingZoneData INSTANCE = new TimedHuntingZoneData();
	}
}
