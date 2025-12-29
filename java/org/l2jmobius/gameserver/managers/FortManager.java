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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.Fort;

public class FortManager
{
	public static final int ORC_FORTRESS = 122;
	public static final int ORC_FORTRESS_FLAG = 93331; // 9819
	public static final int FLAG_MAX_COUNT = 3;
	public static final int ORC_FORTRESS_FLAGPOLE_ID = 23170500;
	private static final Logger LOGGER = Logger.getLogger(FortManager.class.getName());
	
	private static final Map<Integer, Fort> _forts = new ConcurrentSkipListMap<>();
	
	public Fort findNearestFort(WorldObject obj)
	{
		return findNearestFort(obj, Long.MAX_VALUE);
	}
	
	public Fort findNearestFort(WorldObject obj, long maxDistanceValue)
	{
		Fort nearestFort = getFort(obj);
		if (nearestFort == null)
		{
			long maxDistance = maxDistanceValue;
			for (Fort fort : _forts.values())
			{
				final double distance = fort.getDistance(obj);
				if (maxDistance > distance)
				{
					maxDistance = (long) distance;
					nearestFort = fort;
				}
			}
		}
		
		return nearestFort;
	}
	
	public Fort getFortById(int fortId)
	{
		for (Fort fort : _forts.values())
		{
			if (fort.getResidenceId() == fortId)
			{
				return fort;
			}
		}
		
		return null;
	}
	
	public Fort getFortByOwner(Clan clan)
	{
		if (clan == null)
		{
			return null;
		}
		
		for (Fort fort : _forts.values())
		{
			if (fort.getOwnerClan() == clan)
			{
				return fort;
			}
		}
		
		return null;
	}
	
	public Fort getFort(String name)
	{
		for (Fort fort : _forts.values())
		{
			if (fort.getName().equalsIgnoreCase(name.trim()))
			{
				return fort;
			}
		}
		
		return null;
	}
	
	public Fort getFort(int x, int y, int z)
	{
		for (Fort fort : _forts.values())
		{
			if (fort.checkIfInZone(x, y, z))
			{
				return fort;
			}
		}
		
		return null;
	}
	
	public Fort getFort(WorldObject activeObject)
	{
		return getFort(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}
	
	public Collection<Fort> getForts()
	{
		return _forts.values();
	}
	
	public void loadInstances()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT id FROM fort ORDER BY id"))
		{
			while (rs.next())
			{
				final int fortId = rs.getInt("id");
				_forts.put(fortId, new Fort(fortId));
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _forts.values().size() + " fortress.");
			for (Fort fort : _forts.values())
			{
				fort.getSiege().loadSiegeGuard();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception: loadFortData(): " + e.getMessage(), e);
		}
	}
	
	public void activateInstances()
	{
		for (Fort fort : _forts.values())
		{
			fort.activateInstance();
		}
	}
	
	public static FortManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FortManager INSTANCE = new FortManager();
	}
}
