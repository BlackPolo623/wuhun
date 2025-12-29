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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.holders.SharedTeleportHolder;
import org.l2jmobius.gameserver.model.actor.Creature;

/**
 * Shared Teleport Manager
 * @author NasSeKa
 */
public class SharedTeleportManager
{
	protected static final Logger LOGGER = Logger.getLogger(SharedTeleportManager.class.getName());
	
	private static final int TELEPORT_COUNT = 5;
	
	private final Map<Integer, SharedTeleportHolder> _sharedTeleports = new ConcurrentHashMap<>();
	private int _lastSharedTeleportId = 0;
	
	protected SharedTeleportManager()
	{
		LOGGER.info(getClass().getSimpleName() + ": initialized.");
	}
	
	public SharedTeleportHolder getTeleport(int id)
	{
		return _sharedTeleports.get(id);
	}
	
	public synchronized int nextId(Creature creature)
	{
		final int nextId = ++_lastSharedTeleportId;
		_sharedTeleports.put(nextId, new SharedTeleportHolder(nextId, creature.getName(), TELEPORT_COUNT, creature.getX(), creature.getY(), creature.getZ()));
		return nextId;
	}
	
	/**
	 * Gets the single instance of {@code SharedTeleportManager}.
	 * @return single instance of {@code SharedTeleportManager}
	 */
	public static SharedTeleportManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SharedTeleportManager INSTANCE = new SharedTeleportManager();
	}
}
