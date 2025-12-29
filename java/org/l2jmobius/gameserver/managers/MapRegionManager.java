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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.MapRegion;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.model.zone.type.RespawnZone;

/**
 * @author Nyaran
 */
public class MapRegionManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(MapRegionManager.class.getName());
	
	private static final Map<String, MapRegion> REGIONS = new HashMap<>();
	private static final String DEFAULT_RESPAWN = "talking_island_town";
	
	protected MapRegionManager()
	{
		load();
	}
	
	@Override
	public void load()
	{
		REGIONS.clear();
		parseDatapackDirectory("data/mapregion", false);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + REGIONS.size() + " map regions.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		String name;
		String town;
		int locId;
		int bbs;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("region".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						name = attrs.getNamedItem("name").getNodeValue();
						town = attrs.getNamedItem("town").getNodeValue();
						locId = parseInteger(attrs, "locId");
						bbs = parseInteger(attrs, "bbs");
						
						final MapRegion region = new MapRegion(name, town, locId, bbs);
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							attrs = c.getAttributes();
							if ("respawnPoint".equalsIgnoreCase(c.getNodeName()))
							{
								final int spawnX = parseInteger(attrs, "X");
								final int spawnY = parseInteger(attrs, "Y");
								final int spawnZ = parseInteger(attrs, "Z");
								final boolean other = parseBoolean(attrs, "isOther", false);
								final boolean chaotic = parseBoolean(attrs, "isChaotic", false);
								final boolean banish = parseBoolean(attrs, "isBanish", false);
								if (other)
								{
									region.addOtherSpawn(spawnX, spawnY, spawnZ);
								}
								else if (chaotic)
								{
									region.addChaoticSpawn(spawnX, spawnY, spawnZ);
								}
								else if (banish)
								{
									region.addBanishSpawn(spawnX, spawnY, spawnZ);
								}
								else
								{
									region.addSpawn(spawnX, spawnY, spawnZ);
								}
							}
							else if ("map".equalsIgnoreCase(c.getNodeName()))
							{
								region.addMap(parseInteger(attrs, "X"), parseInteger(attrs, "Y"));
							}
							else if ("banned".equalsIgnoreCase(c.getNodeName()))
							{
								region.addBannedRace(attrs.getNamedItem("race").getNodeValue(), attrs.getNamedItem("point").getNodeValue());
							}
						}
						
						REGIONS.put(name, region);
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the map region based on the specified X and Y coordinates.
	 * <p>
	 * This method searches through all available regions and checks if the specified coordinates fall within any region's boundaries.
	 * </p>
	 * @param locX the X-coordinate to locate the map region
	 * @param locY the Y-coordinate to locate the map region
	 * @return the {@link MapRegion} that contains the specified coordinates, or {@code null} if no region is found
	 */
	public MapRegion getMapRegion(int locX, int locY)
	{
		for (MapRegion region : REGIONS.values())
		{
			if (region.isZoneInRegion(getMapRegionX(locX), getMapRegionY(locY)))
			{
				return region;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the location ID of the map region based on the specified X and Y coordinates.
	 * <p>
	 * If no region is found at the specified coordinates, returns {@code 0} as a default value.
	 * </p>
	 * @param locX the X-coordinate to locate the map region
	 * @param locY the Y-coordinate to locate the map region
	 * @return the location ID of the found {@link MapRegion}, or {@code 0} if no region is found
	 */
	public int getMapRegionLocId(int locX, int locY)
	{
		final MapRegion region = getMapRegion(locX, locY);
		if (region != null)
		{
			return region.getLocId();
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the map region that contains the specified {@link WorldObject}.
	 * @param obj the {@link WorldObject} whose location is used to find the map region
	 * @return the {@link MapRegion} containing the object, or {@code null} if no region is found
	 */
	public MapRegion getMapRegion(WorldObject obj)
	{
		return getMapRegion(obj.getX(), obj.getY());
	}
	
	/**
	 * Retrieves the location ID of the map region containing the specified {@link WorldObject}.
	 * <p>
	 * If the object is not located within any map region, returns {@code 0} as a default value.
	 * </p>
	 * @param obj the {@link WorldObject} whose location is used to find the map region ID
	 * @return the location ID of the found {@link MapRegion}, or {@code 0} if no region is found
	 */
	public int getMapRegionLocId(WorldObject obj)
	{
		return getMapRegionLocId(obj.getX(), obj.getY());
	}
	
	/**
	 * Calculates the map region's X-coordinate index for a given position.
	 * <p>
	 * The calculation shifts the given position value to map it to a region-based coordinate.
	 * </p>
	 * @param posX the X-coordinate of the position
	 * @return the calculated X-coordinate index of the map region
	 */
	public int getMapRegionX(int posX)
	{
		return (posX >> 15) + 9 + 11; // + centerTileX;
	}
	
	/**
	 * Calculates the map region's Y-coordinate index for a given position.
	 * <p>
	 * The calculation shifts the given position value to map it to a region-based coordinate.
	 * </p>
	 * @param posY the Y-coordinate of the position
	 * @return the calculated Y-coordinate index of the map region
	 */
	public int getMapRegionY(int posY)
	{
		return (posY >> 15) + 10 + 8; // + centerTileX;
	}
	
	/**
	 * Retrieves the closest town name based on the position of the specified creature.
	 * <p>
	 * If no specific region is found for the creature, returns "Aden Castle Town" as a default.
	 * </p>
	 * @param creature the {@link Creature} whose position is used to determine the closest town
	 * @return the name of the closest town or "Aden Castle Town" if no region is found
	 */
	public String getClosestTownName(Creature creature)
	{
		final MapRegion region = getMapRegion(creature);
		return region == null ? "Aden Castle Town" : region.getTown();
	}
	
	/**
	 * Determines the teleportation location for a creature based on its type and status.
	 * <p>
	 * The method considers various teleportation options, including clan hall, castle, fortress, siege flag, faction base, timed hunting zones, and instances.
	 * </p>
	 * @param creature the {@link Creature} to teleport
	 * @param teleportWhere the {@link TeleportWhereType} specifying the teleport destination type
	 * @return the {@link Location} to which the creature will be teleported, or {@code null} if conditions are unmet
	 */
	public Location getTeleToLocation(Creature creature, TeleportWhereType teleportWhere)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			Castle castle = null;
			Fort fort = null;
			ClanHall clanhall = null;
			final Clan clan = player.getClan();
			if ((clan != null) && !player.isFlyingMounted() && !player.isFlying()) // flying players in gracia cannot use teleports to aden continent
			{
				// If teleport to clan hall
				if (teleportWhere == TeleportWhereType.CLANHALL)
				{
					clanhall = ClanHallData.getInstance().getClanHallByClan(clan);
					if ((clanhall != null) && !player.isFlyingMounted())
					{
						return clanhall.getOwnerLocation();
					}
				}
				
				// If teleport to castle
				if (teleportWhere == TeleportWhereType.CASTLE)
				{
					castle = CastleManager.getInstance().getCastleByOwner(clan);
					
					// Otherwise check if player is on castle or fortress ground
					// and player's clan is defender
					if (castle == null)
					{
						castle = CastleManager.getInstance().getCastle(player);
						if (!((castle != null) && castle.getSiege().isInProgress() && (castle.getSiege().getDefenderClan(clan) != null)))
						{
							castle = null;
						}
					}
					
					if ((castle != null) && (castle.getResidenceId() > 0))
					{
						if (player.getReputation() < 0)
						{
							return castle.getResidenceZone().getChaoticSpawnLoc();
						}
						
						return castle.getResidenceZone().getSpawnLoc();
					}
				}
				
				// If teleport to fortress
				if (teleportWhere == TeleportWhereType.FORTRESS)
				{
					fort = FortManager.getInstance().getFortByOwner(clan);
					
					// Otherwise check if player is on castle or fortress ground
					// and player's clan is defender
					if (fort == null)
					{
						fort = FortManager.getInstance().getFort(player);
						if (!((fort != null) && fort.getSiege().isInProgress() && (fort.getOwnerClan() == clan)))
						{
							fort = null;
						}
					}
					
					if ((fort != null) && (fort.getResidenceId() > 0))
					{
						if (player.getReputation() < 0)
						{
							return fort.getResidenceZone().getChaoticSpawnLoc();
						}
						
						return fort.getResidenceZone().getSpawnLoc();
					}
				}
				
				// If teleport to SiegeHQ
				if (teleportWhere == TeleportWhereType.SIEGEFLAG)
				{
					castle = CastleManager.getInstance().getCastle(player);
					fort = FortManager.getInstance().getFort(player);
					if (castle != null)
					{
						if (castle.getSiege().isInProgress())
						{
							// Check if player's clan is attacker
							final Set<Npc> flags = castle.getSiege().getFlag(clan);
							if ((flags != null) && !flags.isEmpty())
							{
								// Spawn to flag - Need more work to get player to the nearest flag
								return flags.stream().findAny().get().getLocation();
							}
						}
					}
					else if (fort != null)
					{
						if (fort.getSiege().isInProgress())
						{
							// Check if player's clan is attacker
							final Set<Npc> flags = fort.getSiege().getFlag(clan);
							if ((flags != null) && !flags.isEmpty())
							{
								// Spawn to flag - Need more work to get player to the nearest flag
								return flags.stream().findAny().get().getLocation();
							}
						}
					}
				}
			}
			
			// Timed Hunting zones.
			final TimedHuntingZoneHolder timedHuntingZone = player.getTimedHuntingZone();
			if (timedHuntingZone != null)
			{
				final Location exitLocation = timedHuntingZone.getExitLocation();
				if (exitLocation != null)
				{
					return exitLocation;
				}
			}
			
			// Karma player land out of city
			if (player.getReputation() < 0)
			{
				return getNearestKarmaRespawn(player);
			}
			
			// Checking if needed to be respawned in "far" town from the castle;
			// Check if player's clan is participating
			castle = CastleManager.getInstance().getCastle(player);
			if ((castle != null) && castle.getSiege().isInProgress() && (castle.getSiege().checkIsDefender(clan) || castle.getSiege().checkIsAttacker(clan)))
			{
				return castle.getResidenceZone().getOtherSpawnLoc();
			}
			
			// Checking if in an instance
			final Instance inst = player.getInstanceWorld();
			if (inst != null)
			{
				final Location loc = inst.getExitLocation(player);
				if (loc != null)
				{
					return loc;
				}
			}
			
			if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_RESPAWN_AT_BASE)
			{
				if (player.isGood())
				{
					return FactionSystemConfig.FACTION_GOOD_BASE_LOCATION;
				}
				
				if (player.isEvil())
				{
					return FactionSystemConfig.FACTION_EVIL_BASE_LOCATION;
				}
			}
		}
		
		// Get the nearest town
		return getNearestTownRespawn(creature);
	}
	
	/**
	 * Retrieves the nearest respawn location for a player with negative reputation (karma).
	 * <p>
	 * If no specific respawn zone is found, a default chaotic respawn location is returned.
	 * </p>
	 * @param player the {@link Player} with negative reputation to respawn
	 * @return the nearest {@link Location} for a karma-based respawn
	 */
	public Location getNearestKarmaRespawn(Player player)
	{
		try
		{
			final RespawnZone zone = ZoneManager.getInstance().getZone(player, RespawnZone.class);
			if (zone != null)
			{
				return getRestartRegion(player, zone.getRespawnPoint(player)).getChaoticSpawnLoc();
			}
			
			// Opposing race check.
			if (getMapRegion(player).getBannedRace().containsKey(player.getRace()))
			{
				return REGIONS.get(getMapRegion(player).getBannedRace().get(player.getRace())).getChaoticSpawnLoc();
			}
			
			return getMapRegion(player).getChaoticSpawnLoc();
		}
		catch (Exception e)
		{
			if (player.isFlyingMounted())
			{
				return REGIONS.get("union_base_of_kserth").getChaoticSpawnLoc();
			}
			
			return REGIONS.get(DEFAULT_RESPAWN).getChaoticSpawnLoc();
		}
	}
	
	/**
	 * Retrieves the nearest town respawn location for the specified creature.
	 * <p>
	 * If no specific respawn zone is found, a default town respawn location is returned.
	 * </p>
	 * @param creature the {@link Creature} to respawn
	 * @return the nearest town {@link Location} for the creature
	 */
	public Location getNearestTownRespawn(Creature creature)
	{
		try
		{
			final RespawnZone zone = ZoneManager.getInstance().getZone(creature, RespawnZone.class);
			if (zone != null)
			{
				return getRestartRegion(creature, zone.getRespawnPoint(creature.asPlayer())).getSpawnLoc();
			}
			
			// Opposing race check.
			if (getMapRegion(creature).getBannedRace().containsKey(creature.getRace()))
			{
				return REGIONS.get(getMapRegion(creature).getBannedRace().get(creature.getRace())).getSpawnLoc();
			}
			
			return getMapRegion(creature).getSpawnLoc();
		}
		catch (Exception e)
		{
			// Port to the default respawn if no closest town found.
			return REGIONS.get(DEFAULT_RESPAWN).getSpawnLoc();
		}
	}
	
	/**
	 * Retrieves the restart region based on the creature's race and the specified point.
	 * <p>
	 * If the creature's race is banned in the specified region, an alternate region is provided.
	 * </p>
	 * @param creature the {@link Creature} for which the restart region is determined
	 * @param point the point identifier for the desired restart location
	 * @return the {@link MapRegion} associated with the specified point, or a default if not found
	 */
	public MapRegion getRestartRegion(Creature creature, String point)
	{
		try
		{
			final Player player = creature.asPlayer();
			final MapRegion region = REGIONS.get(point);
			if (region.getBannedRace().containsKey(player.getRace()))
			{
				getRestartRegion(player, region.getBannedRace().get(player.getRace()));
			}
			
			return region;
		}
		catch (Exception e)
		{
			return REGIONS.get(DEFAULT_RESPAWN);
		}
	}
	
	/**
	 * Retrieves a map region by its name.
	 * @param regionName the name of the map region
	 * @return the {@link MapRegion} identified by the name, or {@code null} if not found
	 */
	public MapRegion getMapRegionByName(String regionName)
	{
		return REGIONS.get(regionName);
	}
	
	/**
	 * Retrieves the bulletin board system (BBS) ID for a location.
	 * <p>
	 * Uses the map region associated with the specified location to determine the BBS ID.
	 * </p>
	 * @param loc the {@link ILocational} object with coordinates to check
	 * @return the BBS ID for the map region or the default respawn region's BBS ID if not found
	 */
	public int getBBs(ILocational loc)
	{
		final MapRegion region = getMapRegion(loc.getX(), loc.getY());
		return region != null ? region.getBbs() : REGIONS.get(DEFAULT_RESPAWN).getBbs();
	}
	
	public static MapRegionManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MapRegionManager INSTANCE = new MapRegionManager();
	}
}
