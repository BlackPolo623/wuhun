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
public class PlayerVariables extends AbstractVariables
{
	private static final Logger LOGGER = Logger.getLogger(PlayerVariables.class.getName());
	
	// SQL Queries.
	private static final String SELECT_QUERY = "SELECT * FROM character_variables WHERE charId = ?";
	private static final String DELETE_QUERY = "DELETE FROM character_variables WHERE charId = ? AND var = ?";
	private static final String DELETE_ALL_QUERY = "DELETE FROM character_variables WHERE charId = ?";
	private static final String INSERT_QUERY = "INSERT INTO character_variables (charId, var, val) VALUES (?, ?, ?)";
	private static final String UPDATE_QUERY = "UPDATE character_variables SET val = ? WHERE charId = ? AND var = ?";
	
	// Asynchronous persistence.
	private static final long SAVE_INTERVAL = 60000; // 1 minute.
	private static final boolean ASYNC_SAVE_ENABLED = true;
	
	// Public variables.
	public static final String INSTANCE_ORIGIN = "INSTANCE_ORIGIN";
	public static final String INSTANCE_RESTORE = "INSTANCE_RESTORE";
	public static final String RESTORE_LOCATION = "RESTORE_LOCATION";
	public static final String HAIR_ACCESSORY_VARIABLE_NAME = "HAIR_ACCESSORY_ENABLED";
	public static final String WORLD_CHAT_VARIABLE_NAME = "WORLD_CHAT_USED";
	public static final String VITALITY_ITEMS_USED_VARIABLE_NAME = "VITALITY_ITEMS_USED";
	public static final String UI_KEY_MAPPING = "UI_KEY_MAPPING";
	public static final String CLIENT_SETTINGS = "CLIENT_SETTINGS";
	public static final String ATTENDANCE_DATE = "ATTENDANCE_DATE";
	public static final String ATTENDANCE_INDEX = "ATTENDANCE_INDEX";
	public static final String ABILITY_POINTS_MAIN_CLASS = "ABILITY_POINTS";
	public static final String ABILITY_POINTS_DUAL_CLASS = "ABILITY_POINTS_DUAL_CLASS";
	public static final String ABILITY_POINTS_USED_MAIN_CLASS = "ABILITY_POINTS_USED";
	public static final String ABILITY_POINTS_USED_DUAL_CLASS = "ABILITY_POINTS_DUAL_CLASS_USED";
	public static final String REVELATION_SKILL_1_MAIN_CLASS = "RevelationSkill1";
	public static final String REVELATION_SKILL_2_MAIN_CLASS = "RevelationSkill2";
	public static final String REVELATION_SKILL_1_DUAL_CLASS = "DualclassRevelationSkill1";
	public static final String REVELATION_SKILL_2_DUAL_CLASS = "DualclassRevelationSkill2";
	public static final String LAST_PLEDGE_REPUTATION_LEVEL = "LAST_PLEDGE_REPUTATION_LEVEL";
	public static final String FORTUNE_TELLING_VARIABLE = "FortuneTelling";
	public static final String FORTUNE_TELLING_BLACK_CAT_VARIABLE = "FortuneTellingBlackCat";
	public static final String DELUSION_RETURN = "DELUSION_RETURN";
	public static final String BALTHUS_REWARD = "BALTHUS_REWARD";
	public static final String BALTHUS_BAG = "BALTHUS_BAG";
	public static final String AUTO_USE_SETTINGS = "AUTO_USE_SETTINGS";
	public static final String AUTO_USE_SHORTCUTS = "AUTO_USE_SHORTCUTS";
	public static final String LAST_HUNTING_ZONE_ID = "LAST_HUNTING_ZONE_ID";
	public static final String HUNTING_ZONE_ENTRY = "HUNTING_ZONE_ENTRY_";
	public static final String HUNTING_ZONE_TIME = "HUNTING_ZONE_TIME_";
	public static final String HUNTING_ZONE_REMAIN_REFILL = "HUNTING_ZONE_REMAIN_REFILL_";
	public static final String SAYHA_GRACE_SUPPORT_ENDTIME = "SAYHA_GRACE_SUPPORT_ENDTIME";
	public static final String LIMITED_SAYHA_GRACE_ENDTIME = "LIMITED_SAYHA_GRACE_ENDTIME";
	public static final String MAGIC_LAMP_EXP = "MAGIC_LAMP_EXP";
	public static final String DEATH_POINT_COUNT = "DEATH_POINT_COUNT";
	public static final String BEAST_POINT_COUNT = "BEAST_POINT_COUNT";
	public static final String ASSASSINATION_POINT_COUNT = "ASSASSINATION_POINT_COUNT";
	public static final String LIGHT_POINT_COUNT = "LIGHT_POINT_COUNT";
	public static final String WOLF_POINT_COUNT = "WOLF_POINT_COUNT";
	public static final String FAVORITE_TELEPORTS = "FAVORITE_TELEPORTS";
	public static final String ELIXIRS_AVAILABLE = "ELIXIRS_AVAILABLE";
	public static final String STAT_POINTS = "STAT_POINTS";
	public static final String STAT_STR = "STAT_STR";
	public static final String STAT_DEX = "STAT_DEX";
	public static final String STAT_CON = "STAT_CON";
	public static final String STAT_INT = "STAT_INT";
	public static final String STAT_WIT = "STAT_WIT";
	public static final String STAT_MEN = "STAT_MEN";
	public static final String RESURRECT_BY_PAYMENT_COUNT = "RESURRECT_BY_PAYMENT_COUNT";
	public static final String PURGE_LAST_CATEGORY = "PURGE_LAST_CATEGORY";
	public static final String CLAN_JOIN_TIME = "CLAN_JOIN_TIME";
	public static final String CLAN_DONATION_POINTS = "CLAN_DONATION_POINTS";
	public static final String HENNA1_DURATION = "HENNA1_DURATION";
	public static final String HENNA2_DURATION = "HENNA2_DURATION";
	public static final String HENNA3_DURATION = "HENNA3_DURATION";
	public static final String HENNA4_DURATION = "HENNA4_DURATION";
	public static final String DYE_POTENTIAL_DAILY_STEP = "DYE_POTENTIAL_DAILY_STEP";
	public static final String DYE_POTENTIAL_DAILY_COUNT = "DYE_POTENTIAL_DAILY_COUNT";
	public static final String DYE_POTENTIAL_DAILY_COUNT_ENCHANT_RESET = "DYE_POTENTIAL_DAILY_COUNT_ENCHANT_RESET";
	public static final String MISSION_LEVEL_PROGRESS = "MISSION_LEVEL_PROGRESS_";
	public static final String BALOK_AVAILABLE_REWARD = "BALOK_AVAILABLE_REWARD";
	public static final String PRISON_WAIT_TIME = "PRISON_WAIT_TIME";
	public static final String PRISON_2_POINTS = "PRISON_2_POINTS";
	public static final String PRISON_3_POINTS = "PRISON_3_POINTS";
	public static final String DUAL_INVENTORY_SLOT = "DUAL_INVENTORY_SLOT";
	public static final String DUAL_INVENTORY_SET_A = "DUAL_INVENTORY_SET_A";
	public static final String DUAL_INVENTORY_SET_B = "DUAL_INVENTORY_SET_B";
	public static final String DAILY_EXTRACT_ITEM = "DAILY_EXTRACT_ITEM";
	public static final String SKILL_ENCHANT_STAR = "SKILL_ENCHANT_STAR_";
	public static final String SKILL_TRY_ENCHANT = "SKILL_TRY_ENCHANT_";
	public static final String CROSS_EVENT_DAILY_RESET_COUNT = "CROSS_EVENT_DAILY_RESET_COUNT";
	public static final String CROSS_EVENT_CELLS = "CROSS_EVENT_CELLS";
	public static final String CROSS_EVENT_REWARDS = "CROSS_EVENT_REWARDS";
	public static final String CROSS_EVENT_ADVANCED_COUNT = "CROSS_EVENT_ADVANCED_COUNT";
	public static final String CHAT_BACKGROUND_BLUE = "CHAT_BACKGROUND_BLUE";
	public static final String CHAT_BACKGROUND_YELLOW = "CHAT_BACKGROUND_YELLOW";
	public static final String ENABLE_CHAT_BACKGROUND = "ENABLE_CHAT_BACKGROUND";
	public static final String ACTIVE_CHAT_BACKGROUND = "ACTIVE_CHAT_BACKGROUND";
	public static final String ACTIVE_RELIC = "ACTIVE_RELIC";
	public static final String SUMMONED_GUARDIAN_NPC_IDS = "SUMMONED_GUARDIAN_NPC_IDS";
	public static final String AVAILABLE_CHARACTER_STYLES = "AVAILABLE_CHARACTER_STYLES_";
	public static final String FAVORITE_CHARACTER_STYLES = "FAVORITE_CHARACTER_STYLES_";
	public static final String ACTIVE_CHARACTER_STYLE = "ACTIVE_CHARACTER_STYLE_";
	public static final String FIRST_LOGIN_BUFF = "FIRST_LOGIN_BUFF";
	
	// Private variables.
	private final AtomicBoolean _scheduledSave = new AtomicBoolean(false);
	private final int _objectId;
	
	public PlayerVariables(int objectId)
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
		
		// Async save only: schedule once.
		if (ASYNC_SAVE_ENABLED)
		{
			if (_scheduledSave.compareAndSet(false, true))
			{
				ThreadPool.schedule(() ->
				{
					try
					{
						if (hasChanges())
						{
							saveNow();
						}
					}
					finally
					{
						_scheduledSave.set(false);
					}
				}, SAVE_INTERVAL);
			}
			
			// 已排程 or 剛排程，都算成功
			return true;
		}
		
		// Fallback: no async → synchronous save
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

		int retryCount = 0;
		final int MAX_RETRIES = 3;
		final int DEADLOCK_ERROR_CODE = 1213;

		while (retryCount <= MAX_RETRIES)
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				con.setAutoCommit(false);

				try
				{
					// DELETE batch
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

					// INSERT batch
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

					// UPDATE batch
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

					con.commit();

					clearChangeTracking();
					compareAndSetChanges(true, false);
					_saveLock.unlock();

					if (retryCount > 0)
					{
						LOGGER.info("Successfully saved after " + retryCount + " retries for player: " + _objectId);
					}

					return true;
				}
				catch (SQLException e)
				{
					try
					{
						con.rollback();
					}
					catch (SQLException rollbackEx)
					{
						LOGGER.log(Level.WARNING, "Rollback failed for: " + _objectId, rollbackEx);
					}

					if (e.getErrorCode() == DEADLOCK_ERROR_CODE && retryCount < MAX_RETRIES)
					{
						retryCount++;
						LOGGER.warning("Deadlock detected for player " + _objectId + ", retry " + retryCount + "/" + MAX_RETRIES);

						try
						{
							Thread.sleep(50 + (long)(Math.random() * 100));
						}
						catch (InterruptedException ie)
						{
							Thread.currentThread().interrupt();
							_saveLock.unlock();
							return false;
						}

						continue;  // 重試
					}

					throw e;  // 非死鎖錯誤或重試用盡
				}
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, "Could not update variables for: " + _objectId + " (ErrorCode: " + e.getErrorCode() + ")", e);
				_saveLock.unlock();
				return false;
			}
		}

		_saveLock.unlock();
		return false;
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
}
