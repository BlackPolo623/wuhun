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
package org.l2jmobius.gameserver.model.variables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;

/**
 * @author UnAfraid, Mobius
 */
public class ClanVariables extends AbstractVariables
{
	private static final Logger LOGGER = Logger.getLogger(ClanVariables.class.getName());
	
	// SQL Queries.
	private static final String SELECT_QUERY = "SELECT * FROM clan_variables WHERE clanId = ?";
	private static final String DELETE_QUERY = "DELETE FROM clan_variables WHERE clanId = ? AND var = ?";
	private static final String DELETE_ALL_QUERY = "DELETE FROM clan_variables WHERE clanId = ?";
	private static final String INSERT_QUERY = "INSERT INTO clan_variables (clanId, var, val) VALUES (?, ?, ?)";
	private static final String UPDATE_QUERY = "UPDATE clan_variables SET val = ? WHERE clanId = ? AND var = ?";
	private static final String DELETE_WEAKLY_QUERY = "DELETE FROM clan_variables WHERE var LIKE 'CONTRIBUTION_WEEKLY_%' AND clanId = ?";
	
	// Asynchronous persistence.
	private static final long SAVE_INTERVAL = 60000; // 1 minute.
	private static final boolean ASYNC_SAVE_ENABLED = true;
	
	// Public variables.
	public static final String CONTRIBUTION = "CONTRIBUTION_";
	public static final String CONTRIBUTION_WEEKLY = "CONTRIBUTION_WEEKLY_";
	
	// Private variables.
	private final AtomicBoolean _scheduledSave = new AtomicBoolean(false);
	private final int _objectId;
	
	public ClanVariables(int objectId)
	{
		_objectId = objectId;
		restoreMe();
	}
	
	public boolean restoreMe()
	{
		clearChangeTracking();
		
		// Restore previous variables.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(SELECT_QUERY))
		{
			st.setInt(1, _objectId);
			try (ResultSet rset = st.executeQuery())
			{
				while (rset.next())
				{
					set(rset.getString("var"), rset.getString("val"), false);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not restore variables for: " + _objectId, e);
			return false;
		}
		finally
		{
			compareAndSetChanges(true, false);
		}
		
		return true;
	}
	
	public boolean storeMe()
	{
		// No changes, nothing to store.
		if (!hasChanges())
		{
			return false;
		}
		
		// If async saving is enabled and not already scheduled, schedule a save.
		if (ASYNC_SAVE_ENABLED && !_scheduledSave.get())
		{
			_scheduledSave.set(true);
			ThreadPool.schedule(() ->
			{
				_scheduledSave.set(false);
				saveNow();
			}, SAVE_INTERVAL);
			return true;
		}
		
		return saveNow();
	}
	
	/**
	 * Force an immediate save of the variables.
	 * @return true if successful, false otherwise.
	 */
	public boolean saveNow()
	{
		if (!hasChanges())
		{
			return false;
		}
		
		// FIXME: May store after server shutdown.
		// If async is enabled, offload to ThreadPool.
		// if (ASYNC_SAVE_ENABLED)
		// {
		// ThreadPool.execute(this::saveNowSync);
		// return true;
		// }
		
		return saveNowSync();
	}
	
	/**
	 * Synchronous implementation of variable saving with optimized database operations.
	 * @return true if successful, false otherwise.
	 */
	private boolean saveNowSync()
	{
		_saveLock.lock();
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Process deletions.
			if (!_deleted.isEmpty())
			{
				try (PreparedStatement st = con.prepareStatement(DELETE_QUERY))
				{
					for (String name : _deleted)
					{
						st.setInt(1, _objectId);
						st.setString(2, name);
						st.addBatch();
					}
					
					st.executeBatch();
				}
			}
			
			// Process additions.
			if (!_added.isEmpty())
			{
				try (PreparedStatement st = con.prepareStatement(INSERT_QUERY))
				{
					for (String name : _added)
					{
						final Object value = getSet().get(name);
						if (value != null)
						{
							st.setInt(1, _objectId);
							st.setString(2, name);
							st.setString(3, String.valueOf(value));
							st.addBatch();
						}
					}
					
					st.executeBatch();
				}
			}
			
			// Process modifications.
			if (!_modified.isEmpty())
			{
				try (PreparedStatement st = con.prepareStatement(UPDATE_QUERY))
				{
					for (String name : _modified)
					{
						final Object value = getSet().get(name);
						if (value != null)
						{
							st.setString(1, String.valueOf(value));
							st.setInt(2, _objectId);
							st.setString(3, name);
							st.addBatch();
						}
					}
					
					st.executeBatch();
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not update variables for: " + _objectId, e);
			_saveLock.unlock();
			return false;
		}
		finally
		{
			clearChangeTracking();
			compareAndSetChanges(true, false);
			_saveLock.unlock();
		}
		
		return true;
	}
	
	public boolean deleteMe()
	{
		_saveLock.lock();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(DELETE_ALL_QUERY))
		{
			st.setInt(1, _objectId);
			st.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not delete variables for: " + _objectId, e);
			_saveLock.unlock();
			return false;
		}
		
		// Clear all variables.
		getSet().clear();
		clearChangeTracking();
		
		_saveLock.unlock();
		return true;
	}
	
	public boolean deleteWeeklyContribution()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Clear previous entries.
			try (PreparedStatement st = con.prepareStatement(DELETE_WEAKLY_QUERY))
			{
				st.setInt(1, _objectId);
				st.execute();
			}
			
			// Clear all entries that match the pattern.
			getSet().entrySet().stream().filter(it -> it.getKey().startsWith(CONTRIBUTION_WEEKLY)).collect(Collectors.toList()).forEach(it ->
			{
				remove(it.getKey()); // Use remove to properly track changes.
			});
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not delete variables for: " + _objectId, e);
			return false;
		}
		
		return true;
	}
}
