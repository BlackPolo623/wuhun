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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldRegion;
import org.l2jmobius.gameserver.model.actor.enums.creature.FenceState;
import org.l2jmobius.gameserver.model.actor.instance.Fence;
import org.l2jmobius.gameserver.model.instancezone.Instance;

/**
 * @author HorridoJoho
 */
public class FenceData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(FenceData.class.getSimpleName());
	
	private static final int MAX_Z_DIFF = 100;
	
	private final Map<Integer, Fence> _fences = new ConcurrentHashMap<>();
	
	protected FenceData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		if (!_fences.isEmpty())
		{
			// Remove old fences when reloading
			_fences.values().forEach(this::removeFence);
		}
		
		parseDatapackFile("data/FenceData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _fences.size() + " fences.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "fence", this::spawnFence));
	}
	
	private void spawnFence(Node fenceNode)
	{
		final StatSet set = new StatSet(parseAttributes(fenceNode));
		spawnFence(set.getInt("x"), set.getInt("y"), set.getInt("z"), set.getString("name"), set.getInt("width"), set.getInt("length"), set.getInt("height"), 0, set.getEnum("state", FenceState.class, FenceState.CLOSED));
	}
	
	/**
	 * Spawns a fence at the specified coordinates with the given dimensions and state, without a name.
	 * @param x the x-coordinate where the fence will be spawned.
	 * @param y the y-coordinate where the fence will be spawned.
	 * @param z the z-coordinate where the fence will be spawned.
	 * @param width the width of the fence.
	 * @param length the length of the fence.
	 * @param height the height of the fence.
	 * @param instanceId the instance ID for the fence, or 0 for the default instance.
	 * @param state the {@link FenceState} for the fence.
	 * @return the spawned {@link Fence} object.
	 */
	public Fence spawnFence(int x, int y, int z, int width, int length, int height, int instanceId, FenceState state)
	{
		return spawnFence(x, y, z, null, width, length, height, instanceId, state);
	}
	
	/**
	 * Spawns a fence at the specified coordinates with the given name, dimensions, and state.
	 * @param x the x-coordinate where the fence will be spawned.
	 * @param y the y-coordinate where the fence will be spawned.
	 * @param z the z-coordinate where the fence will be spawned.
	 * @param name the name of the fence, or {@code null} if it should be unnamed.
	 * @param width the width of the fence.
	 * @param length the length of the fence.
	 * @param height the height of the fence.
	 * @param instanceId the instance ID for the fence, or 0 for the default instance.
	 * @param state the {@link FenceState} for the fence.
	 * @return the spawned {@link Fence} object.
	 */
	public Fence spawnFence(int x, int y, int z, String name, int width, int length, int height, int instanceId, FenceState state)
	{
		final Fence fence = new Fence(x, y, name, width, length, height, state);
		if (instanceId > 0)
		{
			fence.setInstanceById(instanceId);
		}
		
		fence.spawnMe(x, y, z);
		addFence(fence);
		
		return fence;
	}
	
	/**
	 * Adds the specified fence to the internal collection of fences.
	 * @param fence the {@link Fence} to add.
	 */
	private void addFence(Fence fence)
	{
		_fences.put(fence.getObjectId(), fence);
	}
	
	/**
	 * Removes the specified fence from the internal collection of fences.
	 * @param fence the {@link Fence} to remove.
	 */
	public void removeFence(Fence fence)
	{
		_fences.remove(fence.getObjectId());
	}
	
	/**
	 * Retrieves all currently spawned fences.
	 * @return a map of all spawned fences, keyed by their object ID.
	 */
	public Map<Integer, Fence> getFences()
	{
		return _fences;
	}
	
	/**
	 * Retrieves a specific fence by its object ID.
	 * @param objectId the object ID of the fence.
	 * @return the {@link Fence} with the specified object ID, or {@code null} if no such fence exists.
	 */
	public Fence getFence(int objectId)
	{
		return _fences.get(objectId);
	}
	
	/**
	 * Gets the total number of loaded fences.
	 * @return the count of loaded fences.
	 */
	public int getLoadedElementsCount()
	{
		return _fences.size();
	}
	
	/**
	 * Checks if there is a fence with geodata enabled between two sets of coordinates within a specified instance.
	 * @param x the x-coordinate of the starting point.
	 * @param y the y-coordinate of the starting point.
	 * @param z the z-coordinate of the starting point.
	 * @param tx the x-coordinate of the ending point.
	 * @param ty the y-coordinate of the ending point.
	 * @param tz the z-coordinate of the ending point.
	 * @param instance the instance in which to check for fences.
	 * @return {@code true} if there is a fence between the coordinates, {@code false} otherwise.
	 */
	public boolean checkIfFenceBetween(int x, int y, int z, int tx, int ty, int tz, Instance instance)
	{
		final WorldRegion region = World.getInstance().getRegion(x, y);
		final Collection<Fence> fences = region != null ? region.getFences() : null;
		if ((fences == null) || fences.isEmpty())
		{
			return false;
		}
		
		for (Fence fence : fences)
		{
			// Check if fence is geodata enabled.
			if (!fence.getState().isGeodataEnabled())
			{
				continue;
			}
			
			// Check if fence is within the instance we search for.
			final int instanceId = (instance == null) ? 0 : instance.getId();
			if (fence.getInstanceId() != instanceId)
			{
				continue;
			}
			
			final int xMin = fence.getXMin();
			final int xMax = fence.getXMax();
			final int yMin = fence.getYMin();
			final int yMax = fence.getYMax();
			if ((x < xMin) && (tx < xMin))
			{
				continue;
			}
			
			if ((x > xMax) && (tx > xMax))
			{
				continue;
			}
			
			if ((y < yMin) && (ty < yMin))
			{
				continue;
			}
			
			if ((y > yMax) && (ty > yMax))
			{
				continue;
			}
			
			if ((x > xMin) && (tx > xMin) && (x < xMax) && (tx < xMax) && (y > yMin) && (ty > yMin) && (y < yMax) && (ty < yMax))
			{
				continue;
			}
			
			if ((crossLinePart(xMin, yMin, xMax, yMin, x, y, tx, ty, xMin, yMin, xMax, yMax) || crossLinePart(xMax, yMin, xMax, yMax, x, y, tx, ty, xMin, yMin, xMax, yMax) || crossLinePart(xMax, yMax, xMin, yMax, x, y, tx, ty, xMin, yMin, xMax, yMax) || crossLinePart(xMin, yMax, xMin, yMin, x, y, tx, ty, xMin, yMin, xMax, yMax)) && (z > (fence.getZ() - MAX_Z_DIFF)) && (z < (fence.getZ() + MAX_Z_DIFF)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Determines if a line segment crosses a section of another line segment, restricted by boundaries.
	 * @param x1 the x-coordinate of the first point of the first line.
	 * @param y1 the y-coordinate of the first point of the first line.
	 * @param x2 the x-coordinate of the second point of the first line.
	 * @param y2 the y-coordinate of the second point of the first line.
	 * @param x3 the x-coordinate of the first point of the second line.
	 * @param y3 the y-coordinate of the first point of the second line.
	 * @param x4 the x-coordinate of the second point of the second line.
	 * @param y4 the y-coordinate of the second point of the second line.
	 * @param xMin the minimum x-boundary for the segment.
	 * @param yMin the minimum y-boundary for the segment.
	 * @param xMax the maximum x-boundary for the segment.
	 * @param yMax the maximum y-boundary for the segment.
	 * @return {@code true} if the line segments cross within the specified boundaries, {@code false} otherwise.
	 */
	private boolean crossLinePart(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, double xMin, double yMin, double xMax, double yMax)
	{
		final double[] result = intersection(x1, y1, x2, y2, x3, y3, x4, y4);
		if (result == null)
		{
			return false;
		}
		
		final double xCross = result[0];
		final double yCross = result[1];
		if ((xCross <= xMax) && (xCross >= xMin))
		{
			return true;
		}
		
		if ((yCross <= yMax) && (yCross >= yMin))
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Calculates the intersection point of two line segments.
	 * @param x1 the x-coordinate of the first point of the first line.
	 * @param y1 the y-coordinate of the first point of the first line.
	 * @param x2 the x-coordinate of the second point of the first line.
	 * @param y2 the y-coordinate of the second point of the first line.
	 * @param x3 the x-coordinate of the first point of the second line.
	 * @param y3 the y-coordinate of the first point of the second line.
	 * @param x4 the x-coordinate of the second point of the second line.
	 * @param y4 the y-coordinate of the second point of the second line.
	 * @return an array containing the x and y coordinates of the intersection point, or {@code null} if there is no intersection.
	 */
	private double[] intersection(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4)
	{
		final double d = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));
		if (d == 0)
		{
			return null;
		}
		
		final double xi = (((x3 - x4) * ((x1 * y2) - (y1 * x2))) - ((x1 - x2) * ((x3 * y4) - (y3 * x4)))) / d;
		final double yi = (((y3 - y4) * ((x1 * y2) - (y1 * x2))) - ((y1 - y2) * ((x3 * y4) - (y3 * x4)))) / d;
		return new double[]
		{
			xi,
			yi
		};
	}
	
	public static FenceData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FenceData INSTANCE = new FenceData();
	}
}
