/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.fishing.FishingBait;
import org.l2jmobius.gameserver.model.fishing.FishingCatch;
import org.l2jmobius.gameserver.model.fishing.FishingRod;

/**
 * @author bit
 */
public class FishingData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(FishingData.class.getName());
	
	private final Map<Integer, FishingBait> _baitData = new HashMap<>();
	private final Map<Integer, FishingRod> _rodData = new HashMap<>();
	private int _baitDistanceMin;
	private int _baitDistanceMax;
	private double _expRateMin;
	private double _expRateMax;
	private double _spRateMin;
	private double _spRateMax;
	
	protected FishingData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_baitData.clear();
		parseDatapackFile("data/Fishing.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _baitData.size() + " bait and " + _rodData.size() + " rod data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node listItem = n.getFirstChild(); listItem != null; listItem = listItem.getNextSibling())
				{
					switch (listItem.getNodeName())
					{
						case "baitDistance":
						{
							_baitDistanceMin = parseInteger(listItem.getAttributes(), "min");
							_baitDistanceMax = parseInteger(listItem.getAttributes(), "max");
							break;
						}
						case "xpRate":
						{
							_expRateMin = parseDouble(listItem.getAttributes(), "min");
							_expRateMax = parseDouble(listItem.getAttributes(), "max");
							break;
						}
						case "spRate":
						{
							_spRateMin = parseDouble(listItem.getAttributes(), "min");
							_spRateMax = parseDouble(listItem.getAttributes(), "max");
							break;
						}
						case "baits":
						{
							for (Node bait = listItem.getFirstChild(); bait != null; bait = bait.getNextSibling())
							{
								if ("bait".equalsIgnoreCase(bait.getNodeName()))
								{
									final NamedNodeMap attrs = bait.getAttributes();
									final int itemId = parseInteger(attrs, "itemId");
									final int level = parseInteger(attrs, "level", 1);
									final int minPlayerLevel = parseInteger(attrs, "minPlayerLevel");
									final int maxPlayerLevel = parseInteger(attrs, "maxPlayerLevel", PlayerConfig.PLAYER_MAXIMUM_LEVEL);
									final double chance = parseDouble(attrs, "chance");
									final int timeMin = parseInteger(attrs, "timeMin");
									final int timeMax = parseInteger(attrs, "timeMax", timeMin);
									final int waitMin = parseInteger(attrs, "waitMin");
									final int waitMax = parseInteger(attrs, "waitMax", waitMin);
									final boolean isPremiumOnly = parseBoolean(attrs, "isPremiumOnly", false);
									if (ItemData.getInstance().getTemplate(itemId) == null)
									{
										LOGGER.info(getClass().getSimpleName() + ": Could not find item with id " + itemId);
										continue;
									}
									
									final FishingBait baitData = new FishingBait(itemId, level, minPlayerLevel, maxPlayerLevel, chance, timeMin, timeMax, waitMin, waitMax, isPremiumOnly);
									for (Node c = bait.getFirstChild(); c != null; c = c.getNextSibling())
									{
										if ("catch".equalsIgnoreCase(c.getNodeName()))
										{
											final NamedNodeMap cAttrs = c.getAttributes();
											final int cId = parseInteger(cAttrs, "itemId");
											final float cChance = parseFloat(cAttrs, "chance");
											final float cMultiplier = parseFloat(cAttrs, "multiplier", 1f);
											if (ItemData.getInstance().getTemplate(cId) == null)
											{
												LOGGER.info(getClass().getSimpleName() + ": Could not find item with id " + itemId);
												continue;
											}
											
											baitData.addReward(new FishingCatch(cId, cChance, cMultiplier));
										}
									}
									
									_baitData.put(baitData.getItemId(), baitData);
								}
							}
							break;
						}
						case "rods":
						{
							for (Node rod = listItem.getFirstChild(); rod != null; rod = rod.getNextSibling())
							{
								if ("rod".equalsIgnoreCase(rod.getNodeName()))
								{
									final NamedNodeMap attrs = rod.getAttributes();
									final int itemId = parseInteger(attrs, "itemId");
									final int reduceFishingTime = parseInteger(attrs, "reduceFishingTime", 0);
									final float xpMultiplier = parseFloat(attrs, "xpMultiplier", 1f);
									final float spMultiplier = parseFloat(attrs, "spMultiplier", 1f);
									if (ItemData.getInstance().getTemplate(itemId) == null)
									{
										LOGGER.info(getClass().getSimpleName() + ": Could not find item with id " + itemId);
										continue;
									}
									
									_rodData.put(itemId, new FishingRod(itemId, reduceFishingTime, xpMultiplier, spMultiplier));
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the fishing bait data associated with the specified bait item ID.
	 * @param baitItemId the item ID of the fishing bait.
	 * @return the {@link FishingBait} associated with the specified item ID, or {@code null} if not found.
	 */
	public FishingBait getBaitData(int baitItemId)
	{
		return _baitData.get(baitItemId);
	}
	
	/**
	 * Retrieves the fishing rod data associated with the specified rod item ID.
	 * @param rodItemId the item ID of the fishing rod.
	 * @return the {@link FishingRod} associated with the specified item ID, or {@code null} if not found.
	 */
	public FishingRod getRodData(int rodItemId)
	{
		return _rodData.get(rodItemId);
	}
	
	/**
	 * Gets the minimum distance for bait.
	 * @return the minimum bait distance.
	 */
	public int getBaitDistanceMin()
	{
		return _baitDistanceMin;
	}
	
	/**
	 * Gets the maximum distance for bait.
	 * @return the maximum bait distance.
	 */
	public int getBaitDistanceMax()
	{
		return _baitDistanceMax;
	}
	
	/**
	 * Gets the minimum experience rate.
	 * @return the minimum experience rate.
	 */
	public double getExpRateMin()
	{
		return _expRateMin;
	}
	
	/**
	 * Gets the maximum experience rate.
	 * @return the maximum experience rate.
	 */
	public double getExpRateMax()
	{
		return _expRateMax;
	}
	
	/**
	 * Gets the minimum skill point rate.
	 * @return the minimum skill point rate.
	 */
	public double getSpRateMin()
	{
		return _spRateMin;
	}
	
	/**
	 * Gets the maximum skill point rate.
	 * @return the maximum skill point rate.
	 */
	public double getSpRateMax()
	{
		return _spRateMax;
	}
	
	public static FishingData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FishingData INSTANCE = new FishingData();
	}
}
