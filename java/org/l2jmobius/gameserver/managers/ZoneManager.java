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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.zone.AbstractZoneSettings;
import org.l2jmobius.gameserver.model.zone.ZoneForm;
import org.l2jmobius.gameserver.model.zone.ZoneRegion;
import org.l2jmobius.gameserver.model.zone.ZoneRespawn;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.form.ZoneCuboid;
import org.l2jmobius.gameserver.model.zone.form.ZoneCylinder;
import org.l2jmobius.gameserver.model.zone.form.ZoneNPoly;
import org.l2jmobius.gameserver.model.zone.type.ArenaZone;
import org.l2jmobius.gameserver.model.zone.type.CastleZone;
import org.l2jmobius.gameserver.model.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.model.zone.type.ConditionZone;
import org.l2jmobius.gameserver.model.zone.type.DamageZone;
import org.l2jmobius.gameserver.model.zone.type.DerbyTrackZone;
import org.l2jmobius.gameserver.model.zone.type.EffectZone;
import org.l2jmobius.gameserver.model.zone.type.FishingZone;
import org.l2jmobius.gameserver.model.zone.type.FortZone;
import org.l2jmobius.gameserver.model.zone.type.HqZone;
import org.l2jmobius.gameserver.model.zone.type.JailZone;
import org.l2jmobius.gameserver.model.zone.type.LandingZone;
import org.l2jmobius.gameserver.model.zone.type.MotherTreeZone;
import org.l2jmobius.gameserver.model.zone.type.NoLandingZone;
import org.l2jmobius.gameserver.model.zone.type.NoRestartZone;
import org.l2jmobius.gameserver.model.zone.type.NoStoreZone;
import org.l2jmobius.gameserver.model.zone.type.NoSummonFriendZone;
import org.l2jmobius.gameserver.model.zone.type.OlympiadStadiumZone;
import org.l2jmobius.gameserver.model.zone.type.PeaceZone;
import org.l2jmobius.gameserver.model.zone.type.PrisonZone;
import org.l2jmobius.gameserver.model.zone.type.ResidenceHallTeleportZone;
import org.l2jmobius.gameserver.model.zone.type.ResidenceTeleportZone;
import org.l2jmobius.gameserver.model.zone.type.ResidenceZone;
import org.l2jmobius.gameserver.model.zone.type.RespawnZone;
import org.l2jmobius.gameserver.model.zone.type.SayuneZone;
import org.l2jmobius.gameserver.model.zone.type.ScriptZone;
import org.l2jmobius.gameserver.model.zone.type.SiegableHallZone;
import org.l2jmobius.gameserver.model.zone.type.SiegeZone;
import org.l2jmobius.gameserver.model.zone.type.SpawnTerritory;
import org.l2jmobius.gameserver.model.zone.type.SwampZone;
import org.l2jmobius.gameserver.model.zone.type.TaxZone;
import org.l2jmobius.gameserver.model.zone.type.TeleportZone;
import org.l2jmobius.gameserver.model.zone.type.TimedHuntingZone;
import org.l2jmobius.gameserver.model.zone.type.UndyingZone;
import org.l2jmobius.gameserver.model.zone.type.WaterZone;

/**
 * This class manages the zones.
 * @author durgus, Mobius
 */
public class ZoneManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ZoneManager.class.getName());
	
	private static final Map<String, AbstractZoneSettings> SETTINGS = new HashMap<>();
	
	private static final int SHIFT_BY = 15;
	private static final int OFFSET_X = Math.abs(World.WORLD_X_MIN >> SHIFT_BY);
	private static final int OFFSET_Y = Math.abs(World.WORLD_Y_MIN >> SHIFT_BY);
	
	private final Map<Class<? extends ZoneType>, ConcurrentHashMap<Integer, ? extends ZoneType>> _classZones = new ConcurrentHashMap<>();
	private final Map<String, SpawnTerritory> _spawnTerritories = new ConcurrentHashMap<>();
	private final AtomicInteger _lastDynamicId = new AtomicInteger(300000);
	private List<Item> _debugItems;
	
	private final ZoneRegion[][] _zoneRegions = new ZoneRegion[(World.WORLD_X_MAX >> SHIFT_BY) + OFFSET_X + 1][(World.WORLD_Y_MAX >> SHIFT_BY) + OFFSET_Y + 1];
	
	protected ZoneManager()
	{
		for (int x = 0; x < _zoneRegions.length; x++)
		{
			for (int y = 0; y < _zoneRegions[x].length; y++)
			{
				_zoneRegions[x][y] = new ZoneRegion(x, y);
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + " " + _zoneRegions.length + " by " + _zoneRegions[0].length + " Zone Region Grid set up.");
		load();
	}
	
	@Override
	public void load()
	{
		_classZones.clear();
		_classZones.put(ArenaZone.class, new ConcurrentHashMap<>());
		_classZones.put(CastleZone.class, new ConcurrentHashMap<>());
		_classZones.put(ClanHallZone.class, new ConcurrentHashMap<>());
		_classZones.put(ConditionZone.class, new ConcurrentHashMap<>());
		_classZones.put(DamageZone.class, new ConcurrentHashMap<>());
		_classZones.put(DerbyTrackZone.class, new ConcurrentHashMap<>());
		_classZones.put(EffectZone.class, new ConcurrentHashMap<>());
		_classZones.put(FishingZone.class, new ConcurrentHashMap<>());
		_classZones.put(FortZone.class, new ConcurrentHashMap<>());
		_classZones.put(HqZone.class, new ConcurrentHashMap<>());
		_classZones.put(JailZone.class, new ConcurrentHashMap<>());
		_classZones.put(LandingZone.class, new ConcurrentHashMap<>());
		_classZones.put(MotherTreeZone.class, new ConcurrentHashMap<>());
		_classZones.put(NoLandingZone.class, new ConcurrentHashMap<>());
		_classZones.put(NoRestartZone.class, new ConcurrentHashMap<>());
		_classZones.put(NoStoreZone.class, new ConcurrentHashMap<>());
		_classZones.put(NoSummonFriendZone.class, new ConcurrentHashMap<>());
		_classZones.put(OlympiadStadiumZone.class, new ConcurrentHashMap<>());
		_classZones.put(PeaceZone.class, new ConcurrentHashMap<>());
		_classZones.put(PrisonZone.class, new ConcurrentHashMap<>());
		_classZones.put(ResidenceHallTeleportZone.class, new ConcurrentHashMap<>());
		_classZones.put(ResidenceTeleportZone.class, new ConcurrentHashMap<>());
		_classZones.put(ResidenceZone.class, new ConcurrentHashMap<>());
		_classZones.put(RespawnZone.class, new ConcurrentHashMap<>());
		_classZones.put(SayuneZone.class, new ConcurrentHashMap<>());
		_classZones.put(ScriptZone.class, new ConcurrentHashMap<>());
		_classZones.put(SiegableHallZone.class, new ConcurrentHashMap<>());
		_classZones.put(SiegeZone.class, new ConcurrentHashMap<>());
		_classZones.put(SwampZone.class, new ConcurrentHashMap<>());
		_classZones.put(TaxZone.class, new ConcurrentHashMap<>());
		_classZones.put(TeleportZone.class, new ConcurrentHashMap<>());
		_classZones.put(TimedHuntingZone.class, new ConcurrentHashMap<>());
		_classZones.put(UndyingZone.class, new ConcurrentHashMap<>());
		_classZones.put(WaterZone.class, new ConcurrentHashMap<>());
		_spawnTerritories.clear();
		
		parseDatapackDirectory("data/zones", false);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _classZones.size() + " zone classes and " + getSize() + " zones.");
		
		final OptionalInt maxId = _classZones.values().stream().flatMap(map -> map.keySet().stream()).mapToInt(Integer.class::cast).filter(value -> value < 300000).max();
		LOGGER.info(getClass().getSimpleName() + ": Last static id " + maxId.getAsInt() + ".");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		Node attribute;
		String zoneName;
		int[][] coords;
		int zoneId;
		int minZ;
		int maxZ;
		String zoneType;
		String zoneShape;
		final List<int[]> rs = new ArrayList<>();
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				attrs = n.getAttributes();
				attribute = attrs.getNamedItem("enabled");
				if ((attribute != null) && !Boolean.parseBoolean(attribute.getNodeValue()))
				{
					continue;
				}
				
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("zone".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						attribute = attrs.getNamedItem("type");
						if (attribute != null)
						{
							zoneType = attribute.getNodeValue();
						}
						else
						{
							LOGGER.warning("ZoneData: Missing type for zone in file: " + file.getName());
							continue;
						}
						
						attribute = attrs.getNamedItem("id");
						if (attribute != null)
						{
							zoneId = Integer.parseInt(attribute.getNodeValue());
						}
						else
						{
							zoneId = zoneType.equalsIgnoreCase("NpcSpawnTerritory") ? 0 : _lastDynamicId.incrementAndGet();
						}
						
						attribute = attrs.getNamedItem("name");
						if (attribute != null)
						{
							zoneName = attribute.getNodeValue();
						}
						else
						{
							zoneName = null;
						}
						
						// Check zone name for NpcSpawnTerritory. Must exist and to be unique
						if (zoneType.equalsIgnoreCase("NpcSpawnTerritory"))
						{
							if (zoneName == null)
							{
								LOGGER.warning("ZoneData: Missing name for NpcSpawnTerritory in file: " + file.getName() + ", skipping zone");
								continue;
							}
							else if (_spawnTerritories.containsKey(zoneName))
							{
								LOGGER.warning("ZoneData: Name " + zoneName + " already used for another zone, check file: " + file.getName() + ". Skipping zone");
								continue;
							}
						}
						
						minZ = parseInteger(attrs, "minZ");
						maxZ = parseInteger(attrs, "maxZ");
						zoneType = parseString(attrs, "type");
						zoneShape = parseString(attrs, "shape");
						
						// Get the zone shape from xml
						ZoneForm zoneForm = null;
						try
						{
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if ("node".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									final int[] point = new int[2];
									point[0] = parseInteger(attrs, "X");
									point[1] = parseInteger(attrs, "Y");
									rs.add(point);
								}
							}
							
							coords = rs.toArray(new int[rs.size()][2]);
							rs.clear();
							
							if ((coords == null) || (coords.length == 0))
							{
								LOGGER.warning(getClass().getSimpleName() + ": ZoneData: missing data for zone: " + zoneId + " XML file: " + file.getName());
								continue;
							}
							
							// Create this zone. Parsing for cuboids is a bit different than for other polygons cuboids need exactly 2 points to be defined.
							// Other polygons need at least 3 (one per vertex)
							if (zoneShape.equalsIgnoreCase("Cuboid"))
							{
								if (coords.length == 2)
								{
									zoneForm = new ZoneCuboid(coords[0][0], coords[1][0], coords[0][1], coords[1][1], minZ, maxZ);
								}
								else
								{
									LOGGER.warning(getClass().getSimpleName() + ": ZoneData: Missing cuboid vertex data for zone: " + zoneId + " in file: " + file.getName());
									continue;
								}
							}
							else if (zoneShape.equalsIgnoreCase("NPoly"))
							{
								// nPoly needs to have at least 3 vertices
								if (coords.length > 2)
								{
									final int[] aX = new int[coords.length];
									final int[] aY = new int[coords.length];
									for (int i = 0; i < coords.length; i++)
									{
										aX[i] = coords[i][0];
										aY[i] = coords[i][1];
									}
									
									zoneForm = new ZoneNPoly(aX, aY, minZ, maxZ);
								}
								else
								{
									LOGGER.warning(getClass().getSimpleName() + ": ZoneData: Bad data for zone: " + zoneId + " in file: " + file.getName());
									continue;
								}
							}
							else if (zoneShape.equalsIgnoreCase("Cylinder"))
							{
								// A Cylinder zone requires a center point
								// at x,y and a radius
								attrs = d.getAttributes();
								final int zoneRad = Integer.parseInt(attrs.getNamedItem("rad").getNodeValue());
								if ((coords.length == 1) && (zoneRad > 0))
								{
									zoneForm = new ZoneCylinder(coords[0][0], coords[0][1], minZ, maxZ, zoneRad);
								}
								else
								{
									LOGGER.warning(getClass().getSimpleName() + ": ZoneData: Bad data for zone: " + zoneId + " in file: " + file.getName());
									continue;
								}
							}
							else
							{
								LOGGER.warning(getClass().getSimpleName() + ": ZoneData: Unknown shape: \"" + zoneShape + "\"  for zone: " + zoneId + " in file: " + file.getName());
								continue;
							}
						}
						catch (Exception e)
						{
							LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": ZoneData: Failed to load zone " + zoneId + " coordinates: " + e.getMessage(), e);
						}
						
						// No further parameters needed, if NpcSpawnTerritory is loading
						if (zoneType.equalsIgnoreCase("NpcSpawnTerritory"))
						{
							_spawnTerritories.put(zoneName, new SpawnTerritory(zoneName, zoneForm));
							continue;
						}
						
						// Create the zone
						Class<?> newZone = null;
						Constructor<?> zoneConstructor = null;
						ZoneType temp;
						try
						{
							newZone = Class.forName("org.l2jmobius.gameserver.model.zone.type." + zoneType);
							zoneConstructor = newZone.getConstructor(int.class);
							temp = (ZoneType) zoneConstructor.newInstance(zoneId);
							temp.setZone(zoneForm);
						}
						catch (Exception e)
						{
							LOGGER.warning(getClass().getSimpleName() + ": ZoneData: No such zone type: " + zoneType + " in file: " + file.getName());
							continue;
						}
						
						// Check for additional parameters
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("stat".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								final String name = attrs.getNamedItem("name").getNodeValue();
								final String val = attrs.getNamedItem("val").getNodeValue();
								temp.setParameter(name, val);
							}
							else if ("spawn".equalsIgnoreCase(cd.getNodeName()) && (temp instanceof ZoneRespawn))
							{
								attrs = cd.getAttributes();
								final int spawnX = Integer.parseInt(attrs.getNamedItem("X").getNodeValue());
								final int spawnY = Integer.parseInt(attrs.getNamedItem("Y").getNodeValue());
								final int spawnZ = Integer.parseInt(attrs.getNamedItem("Z").getNodeValue());
								final Node val = attrs.getNamedItem("type");
								((ZoneRespawn) temp).parseLoc(spawnX, spawnY, spawnZ, val == null ? null : val.getNodeValue());
							}
							else if ("race".equalsIgnoreCase(cd.getNodeName()) && (temp instanceof RespawnZone))
							{
								attrs = cd.getAttributes();
								final String race = attrs.getNamedItem("name").getNodeValue();
								final String point = attrs.getNamedItem("point").getNodeValue();
								((RespawnZone) temp).addRaceRespawnPoint(race, point);
							}
						}
						
						if (checkId(zoneId))
						{
							LOGGER.config(getClass().getSimpleName() + ": Caution: Zone (" + zoneId + ") from file: " + file.getName() + " overrides previous definition.");
						}
						
						if ((zoneName != null) && !zoneName.isEmpty())
						{
							temp.setName(zoneName);
						}
						
						addZone(zoneId, temp);
						
						// Register the zone into any world region it intersects with.
						for (int x = 0; x < _zoneRegions.length; x++)
						{
							for (int y = 0; y < _zoneRegions[x].length; y++)
							{
								final int ax = (x - OFFSET_X) << SHIFT_BY;
								final int bx = ((x + 1) - OFFSET_X) << SHIFT_BY;
								final int ay = (y - OFFSET_Y) << SHIFT_BY;
								final int by = ((y + 1) - OFFSET_Y) << SHIFT_BY;
								if (temp.getZone().intersectsRectangle(ax, bx, ay, by))
								{
									_zoneRegions[x][y].getZones().put(temp.getId(), temp);
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Reloads all zones, unloading existing zones and reloading from definitions.
	 * <p>
	 * This method clears all zone data, reloads zones, and revalidates zones for all visible characters.
	 * </p>
	 */
	public void reload()
	{
		// Unload zones.
		unload();
		
		// Load the zones.
		load();
		
		// Re-validate all characters in zones.
		for (WorldObject obj : World.getInstance().getVisibleObjects())
		{
			if (obj.isCreature())
			{
				obj.asCreature().revalidateZone(true);
			}
		}
		
		SETTINGS.clear();
	}
	
	/**
	 * Unloads all zones from memory, clearing any associated settings and regions.
	 * <p>
	 * Existing zone settings are backed up, and zones in all regions are cleared.
	 * </p>
	 */
	public void unload()
	{
		// Get the world regions.
		int count = 0;
		
		// Backup old zone settings.
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			for (ZoneType zone : map.values())
			{
				if (zone.getSettings() != null)
				{
					SETTINGS.put(zone.getName(), zone.getSettings());
				}
			}
		}
		
		// Clear zones.
		for (ZoneRegion[] zoneRegions : _zoneRegions)
		{
			for (ZoneRegion zoneRegion : zoneRegions)
			{
				zoneRegion.getZones().clear();
				count++;
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Removed zones in " + count + " regions.");
	}
	
	/**
	 * Retrieves the total number of zones loaded in the system.
	 * @return the number of zones
	 */
	public int getSize()
	{
		int i = 0;
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			i += map.size();
		}
		
		return i;
	}
	
	/**
	 * Checks if a zone ID already exists in the system.
	 * @param id the ID of the zone to check
	 * @return {@code true} if the ID exists, otherwise {@code false}
	 */
	private boolean checkId(int id)
	{
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			if (map.containsKey(id))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Adds a new zone to the system based on its ID and instance.
	 * @param <T> the generic type of the zone
	 * @param id the unique ID of the zone
	 * @param zone the zone instance to add
	 */
	@SuppressWarnings("unchecked")
	private <T extends ZoneType> void addZone(Integer id, T zone)
	{
		ConcurrentHashMap<Integer, T> map = (ConcurrentHashMap<Integer, T>) _classZones.get(zone.getClass());
		if (map == null)
		{
			_classZones.put(zone.getClass(), new ConcurrentHashMap<>());
			map = (ConcurrentHashMap<Integer, T>) _classZones.get(zone.getClass());
		}
		
		map.put(id, zone);
	}
	
	/**
	 * Retrieves all zones of a specified class type.
	 * @param <T> the generic type of the zone
	 * @param zoneType the class type of the zone
	 * @return a collection of zones matching the specified type
	 */
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> Collection<T> getAllZones(Class<T> zoneType)
	{
		return (Collection<T>) _classZones.get(zoneType).values();
	}
	
	/**
	 * Retrieves a zone by its unique ID.
	 * @param id the ID of the zone
	 * @return the {@link ZoneType} with the specified ID, or {@code null} if not found
	 */
	public ZoneType getZoneById(int id)
	{
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			if (map.containsKey(id))
			{
				return map.get(id);
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a zone by its name.
	 * @param name the name of the zone
	 * @return the {@link ZoneType} with the specified name, or {@code null} if not found
	 */
	public ZoneType getZoneByName(String name)
	{
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			for (ZoneType zone : map.values())
			{
				if ((zone.getName() != null) && zone.getName().equals(name))
				{
					return zone;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a zone by its ID and class type.
	 * @param <T> the generic type of the zone
	 * @param id the ID of the zone
	 * @param zoneType the class type of the zone
	 * @return the zone instance if found, or {@code null} if not found
	 */
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> T getZoneById(int id, Class<T> zoneType)
	{
		return (T) _classZones.get(zoneType).get(id);
	}
	
	/**
	 * Retrieves a zone by its name and class type.
	 * @param <T> the generic type of the zone
	 * @param name the name of the zone
	 * @param zoneType the class type of the zone
	 * @return the zone instance if found, or {@code null} if not found
	 */
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> T getZoneByName(String name, Class<T> zoneType)
	{
		if (_classZones.containsKey(zoneType))
		{
			for (ZoneType zone : _classZones.get(zoneType).values())
			{
				if ((zone.getName() != null) && zone.getName().equals(name))
				{
					return (T) zone;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Returns all zones at the specified location.
	 * @param locational the {@link ILocational} object representing the location
	 * @return a list of zones containing the location
	 */
	public List<ZoneType> getZones(ILocational locational)
	{
		return getZones(locational.getX(), locational.getY(), locational.getZ());
	}
	
	/**
	 * Retrieves a zone at the specified location and of the specified class type.
	 * @param <T> the generic type of the zone
	 * @param locational the {@link ILocational} location of the object
	 * @param type the class type of the zone
	 * @return the zone instance if found, or {@code null} if not found
	 */
	public <T extends ZoneType> T getZone(ILocational locational, Class<T> type)
	{
		if (locational == null)
		{
			return null;
		}
		
		return getZone(locational.getX(), locational.getY(), locational.getZ(), type);
	}
	
	/**
	 * Returns all zones at the specified coordinates (X and Y).
	 * @param x the X-coordinate
	 * @param y the Y-coordinate
	 * @return a list of zones containing the specified coordinates
	 */
	public List<ZoneType> getZones(int x, int y)
	{
		final ZoneRegion region = getRegion(x, y);
		if (region == null)
		{
			return Collections.emptyList();
		}
		
		final List<ZoneType> result = new ArrayList<>();
		for (ZoneType zone : region.getZones().values())
		{
			if (zone.isInsideZone(x, y))
			{
				result.add(zone);
			}
		}
		
		return result;
	}
	
	/**
	 * Returns all zones at the specified coordinates (X, Y, and Z).
	 * @param x the X-coordinate
	 * @param y the Y-coordinate
	 * @param z the Z-coordinate
	 * @return a list of zones containing the specified coordinates
	 */
	public List<ZoneType> getZones(int x, int y, int z)
	{
		final ZoneRegion region = getRegion(x, y);
		if (region == null)
		{
			return Collections.emptyList();
		}
		
		final List<ZoneType> result = new ArrayList<>();
		for (ZoneType zone : region.getZones().values())
		{
			if (zone.isInsideZone(x, y, z))
			{
				result.add(zone);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a zone at the specified coordinates and of the specified class type.
	 * @param <T> the generic type of the zone
	 * @param x the X-coordinate
	 * @param y the Y-coordinate
	 * @param z the Z-coordinate
	 * @param type the class type of the zone
	 * @return the zone instance if found, or {@code null} if not found
	 */
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> T getZone(int x, int y, int z, Class<T> type)
	{
		final ZoneRegion region = getRegion(x, y);
		if (region == null)
		{
			return null;
		}
		
		for (ZoneType zone : region.getZones().values())
		{
			if (zone.isInsideZone(x, y, z) && type.isInstance(zone))
			{
				return (T) zone;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a spawn territory by its name.
	 * @param name the name of the spawn territory
	 * @return the {@link SpawnTerritory} with the specified name, or {@code null} if not found
	 */
	public SpawnTerritory getSpawnTerritory(String name)
	{
		return _spawnTerritories.get(name);
	}
	
	/**
	 * Retrieves all spawn territories that contain the specified world object.
	 * @param object the {@link WorldObject} located within the spawn territory
	 * @return a list of {@link SpawnTerritory} containing the object
	 */
	public List<SpawnTerritory> getSpawnTerritories(WorldObject object)
	{
		final List<SpawnTerritory> result = new ArrayList<>();
		for (SpawnTerritory territory : _spawnTerritories.values())
		{
			if (territory.isInsideZone(object.getX(), object.getY(), object.getZ()))
			{
				result.add(territory);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the Olympiad stadium zone for the specified creature.
	 * @param creature the {@link Creature} located within the Olympiad stadium zone
	 * @return the {@link OlympiadStadiumZone} containing the creature, or {@code null} if not found
	 */
	public OlympiadStadiumZone getOlympiadStadium(Creature creature)
	{
		if (creature == null)
		{
			return null;
		}
		
		for (ZoneType zone : getInstance().getZones(creature.getX(), creature.getY(), creature.getZ()))
		{
			if ((zone instanceof OlympiadStadiumZone) && zone.isCharacterInZone(creature))
			{
				return (OlympiadStadiumZone) zone;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a list of debug items used to visualize zones.
	 * @return a list of debug {@link Item} instances
	 */
	public List<Item> getDebugItems()
	{
		if (_debugItems == null)
		{
			_debugItems = new ArrayList<>();
		}
		
		return _debugItems;
	}
	
	/**
	 * Removes all debug items from the world.
	 */
	public synchronized void clearDebugItems()
	{
		if (_debugItems != null)
		{
			for (Item item : _debugItems)
			{
				if (item != null)
				{
					item.decayMe();
				}
			}
			
			_debugItems.clear();
		}
	}
	
	/**
	 * Retrieves the zone region for the specified X and Y coordinates.
	 * @param x the X-coordinate
	 * @param y the Y-coordinate
	 * @return the {@link ZoneRegion} at the specified coordinates, or {@code null} if out of bounds
	 */
	public ZoneRegion getRegion(int x, int y)
	{
		try
		{
			return _zoneRegions[(x >> SHIFT_BY) + OFFSET_X][(y >> SHIFT_BY) + OFFSET_Y];
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			// LOGGER.warning(getClass().getSimpleName() + ": Incorrect zone region X: " + ((x >> SHIFT_BY) + OFFSET_X) + " Y: " + ((y >> SHIFT_BY) + OFFSET_Y) + " for coordinates x: " + x + " y: " + y);
			return null;
		}
	}
	
	/**
	 * Retrieves the zone region for a given location.
	 * @param point the {@link ILocational} point
	 * @return the {@link ZoneRegion} at the specified location, or {@code null} if out of bounds
	 */
	public ZoneRegion getRegion(ILocational point)
	{
		return getRegion(point.getX(), point.getY());
	}
	
	/**
	 * Retrieves the settings associated with a specific zone by name.
	 * @param name the name of the zone
	 * @return the {@link AbstractZoneSettings} of the specified zone, or {@code null} if not found
	 */
	public static AbstractZoneSettings getSettings(String name)
	{
		return SETTINGS.get(name);
	}
	
	public static ZoneManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ZoneManager INSTANCE = new ZoneManager();
	}
}
