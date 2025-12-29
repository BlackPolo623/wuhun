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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.siege.Castle;

public class CastleManager
{
	private static final Logger LOGGER = Logger.getLogger(CastleManager.class.getName());
	
	private final Map<Integer, Castle> _castles = new ConcurrentSkipListMap<>();
	private final Map<Integer, Long> _castleSiegeDate = new ConcurrentHashMap<>();
	
	private static final int[] _castleCirclets =
	{
		0,
		6838,
		6835,
		6839,
		6837,
		6840,
		6834,
		6836,
		8182,
		8183
	};
	
	public Castle findNearestCastle(WorldObject obj)
	{
		return findNearestCastle(obj, Long.MAX_VALUE);
	}
	
	public Castle findNearestCastle(WorldObject obj, long maxDistanceValue)
	{
		Castle nearestCastle = getCastle(obj);
		if (nearestCastle == null)
		{
			double distance;
			long maxDistance = maxDistanceValue;
			for (Castle castle : _castles.values())
			{
				distance = castle.getDistance(obj);
				if (maxDistance > distance)
				{
					maxDistance = (long) distance;
					nearestCastle = castle;
				}
			}
		}
		
		return nearestCastle;
	}
	
	public Castle getCastleById(int castleId)
	{
		return _castles.get(castleId);
	}
	
	public Castle getCastleByOwner(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		
		for (Castle temp : _castles.values())
		{
			if (temp.getOwnerId() == clan.getId())
			{
				return temp;
			}
		}
		
		return null;
	}
	
	public Castle getCastle(String name)
	{
		for (Castle temp : _castles.values())
		{
			if (temp.getName().equalsIgnoreCase(name.trim()))
			{
				return temp;
			}
		}
		
		return null;
	}
	
	public Castle getCastle(int x, int y, int z)
	{
		for (Castle temp : _castles.values())
		{
			if (temp.checkIfInZone(x, y, z))
			{
				return temp;
			}
		}
		
		return null;
	}
	
	public Castle getCastle(WorldObject activeObject)
	{
		return getCastle(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public Collection<Castle> getCastles()
	{
		return _castles.values();
	}
	
	public boolean hasOwnedCastle()
	{
		boolean hasOwnedCastle = false;
		for (Castle castle : _castles.values())
		{
			if (castle.getOwnerId() > 0)
			{
				hasOwnedCastle = true;
				break;
			}
		}
		
		return hasOwnedCastle;
	}
	
	public int getCircletByCastleId(int castleId)
	{
		if ((castleId > 0) && (castleId < 10))
		{
			return _castleCirclets[castleId];
		}
		
		return 0;
	}
	
	// remove this castle's circlets from the clan
	public void removeCirclet(Clan clan, int castleId)
	{
		for (ClanMember member : clan.getMembers())
		{
			removeCirclet(member, castleId);
		}
	}
	
	public void removeCirclet(ClanMember member, int castleId)
	{
		if (member == null)
		{
			return;
		}
		
		final Player player = member.getPlayer();
		final int circletId = getCircletByCastleId(castleId);
		if (circletId != 0)
		{
			// online-player circlet removal
			if (player != null)
			{
				try
				{
					final Item circlet = player.getInventory().getItemByItemId(circletId);
					if (circlet != null)
					{
						if (circlet.isEquipped())
						{
							player.getInventory().unEquipItemInSlot(circlet.getLocationSlot());
						}
						
						player.destroyItemByItemId(ItemProcessType.DESTROY, circletId, 1, player, true);
					}
					return;
				}
				catch (NullPointerException e)
				{
					// continue removing offline
				}
			}
			// else offline-player circlet removal
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM items WHERE owner_id = ? and item_id = ?"))
			{
				ps.setInt(1, member.getObjectId());
				ps.setInt(2, circletId);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to remove castle circlets offline for player " + member.getName() + ": ", e);
			}
		}
	}
	
	public void loadInstances()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT id FROM castle ORDER BY id"))
		{
			while (rs.next())
			{
				final int castleId = rs.getInt("id");
				_castles.put(castleId, new Castle(castleId));
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _castles.values().size() + " castles.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Exception: loadCastleData():", e);
		}
	}
	
	public void activateInstances()
	{
		for (Castle castle : _castles.values())
		{
			castle.activateInstance();
		}
	}
	
	public void registerSiegeDate(int castleId, long siegeDate)
	{
		_castleSiegeDate.put(castleId, siegeDate);
	}
	
	public int getSiegeDates(long siegeDate)
	{
		int count = 0;
		for (long date : _castleSiegeDate.values())
		{
			if (Math.abs(date - siegeDate) < 1000)
			{
				count++;
			}
		}
		
		return count;
	}
	
	public static CastleManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CastleManager INSTANCE = new CastleManager();
	}
}
