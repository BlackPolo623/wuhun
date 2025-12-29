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

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;

/**
 * @author UnAfraid, Mobius
 */
public class AccountVariables extends AbstractVariables
{
	private static final Logger LOGGER = Logger.getLogger(AccountVariables.class.getName());
	
	// SQL Queries.
	private static final String SELECT_QUERY = "SELECT * FROM account_gsdata WHERE account_name = ?";
	private static final String DELETE_QUERY = "DELETE FROM account_gsdata WHERE account_name = ? AND var = ?";
	private static final String DELETE_ALL_QUERY = "DELETE FROM account_gsdata WHERE account_name = ?";
	private static final String INSERT_QUERY = "INSERT INTO account_gsdata (account_name, var, value) VALUES (?, ?, ?)";
	private static final String UPDATE_QUERY = "UPDATE account_gsdata SET value = ? WHERE account_name = ? AND var = ?";
	private static final String DELETE_QUERY_VAR = "DELETE FROM account_gsdata WHERE var = ?";
	
	// Asynchronous persistence.
	private static final long SAVE_INTERVAL = 60000; // 1 minute.
	private static final boolean ASYNC_SAVE_ENABLED = true;
	
	// Public variables.
	public static final String HWID = "HWID";
	public static final String HWIDSLIT_VAR = "	";
	public static final String PRIME_SHOP_PRODUCT_COUNT = "PSPCount";
	public static final String PRIME_SHOP_PRODUCT_DAILY_COUNT = "PSPDailyCount";
	public static final String LCOIN_SHOP_PRODUCT_COUNT = "LCSCount";
	public static final String LCOIN_SHOP_PRODUCT_DAILY_COUNT = "LCSDailyCount";
	public static final String LCOIN_SHOP_PRODUCT_WEEKLY_COUNT = "LCSWeeklyCount";
	public static final String LCOIN_SHOP_PRODUCT_MONTHLY_COUNT = "LCSMonthlyCount";
	public static final String VIP_POINTS = "VipPoints";
	public static final String VIP_TIER = "VipTier";
	public static final String VIP_EXPIRATION = "VipExpiration";
	public static final String VIP_ITEM_BOUGHT = "Vip_Item_Bought";
	public static final String A_GRADE_RELIC_ATEMPTS = "A_GRADE_RELIC_ATEMPTS";
	public static final String B_GRADE_RELIC_ATEMPTS = "B_GRADE_RELIC_ATEMPTS";
	public static final String UNCONFIRMED_RELICS_COUNT = "UNCONFIRMED_RELICS_COUNT";
	
	// Private variables.
	private final AtomicBoolean _scheduledSave = new AtomicBoolean(false);
	private final String _accountName;
	
	public AccountVariables(String accountName)
	{
		_accountName = accountName;
		restoreMe();
	}
	
	public boolean restoreMe()
	{
		clearChangeTracking();
		
		// Restore previous variables.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(SELECT_QUERY))
		{
			st.setString(1, _accountName);
			try (ResultSet rset = st.executeQuery())
			{
				while (rset.next())
				{
					set(rset.getString("var"), rset.getString("value"), false);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not restore variables for: " + _accountName, e);
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
						st.setString(1, _accountName);
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
							st.setString(1, _accountName);
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
							st.setString(2, _accountName);
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
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not update variables for: " + _accountName, e);
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
			st.setString(1, _accountName);
			st.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not delete variables for: " + _accountName, e);
			_saveLock.unlock();
			return false;
		}
		
		// Clear all variables.
		getSet().clear();
		clearChangeTracking();
		
		_saveLock.unlock();
		return true;
	}
	
	/**
	 * Delete all entries for an requested var
	 * @param var
	 * @return success
	 */
	public static boolean deleteVipPurchases(String var)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement(DELETE_QUERY_VAR))
		{
			st.setString(1, var);
			st.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "AccountVariables: Could not delete vip variables!", e);
			return false;
		}
		
		return true;
	}
}
