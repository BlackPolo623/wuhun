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
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.RefineSystemData;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * 精煉系統次數管理器。
 * 負責讀取/寫入 item_refine_charges 表，並維護記憶體快取。
 */
public class RefineSystemManager
{
	private static final Logger LOGGER = Logger.getLogger(RefineSystemManager.class.getName());

	private static final String SELECT_SQL = "SELECT charges FROM item_refine_charges WHERE item_id = ?";
	private static final String REPLACE_SQL = "REPLACE INTO item_refine_charges (item_id, charges) VALUES (?, ?)";

	private final Map<Integer, Integer> _cache = new ConcurrentHashMap<>();

	private RefineSystemManager()
	{
	}

	public int getCharges(Item item)
	{
		final int objectId = item.getObjectId();
		if (_cache.containsKey(objectId))
		{
			return _cache.get(objectId);
		}

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_SQL))
		{
			ps.setInt(1, objectId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					final int charges = rs.getInt("charges");
					_cache.put(objectId, charges);
					return charges;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("[RefineSystem] 讀取精煉次數失敗 itemId=" + objectId + ": " + e.getMessage());
		}

		return getDefaultCharges(item);
	}

	public int consumeCharge(Item item)
	{
		final int objectId = item.getObjectId();
		final int current  = getCharges(item);
		if (current <= 0)
		{
			return 0;
		}

		final int remaining = current - 1;
		_cache.put(objectId, remaining);

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(REPLACE_SQL))
		{
			ps.setInt(1, objectId);
			ps.setInt(2, remaining);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("[RefineSystem] 寫入精煉次數失敗 itemId=" + objectId + ": " + e.getMessage());
			return -1;
		}

		return remaining;
	}

	public void invalidate(int itemObjectId)
	{
		_cache.remove(itemObjectId);
	}

	public void resetCharges(Item item)
	{
		final int objectId = item.getObjectId();
		final int max = getDefaultCharges(item);
		_cache.put(objectId, max);

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(REPLACE_SQL))
		{
			ps.setInt(1, objectId);
			ps.setInt(2, max);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("[RefineSystem] 重置精煉次數失敗 itemId=" + objectId + ": " + e.getMessage());
		}
	}

	/**
	 * 將指定裝備的精煉次數設為 0（用於禁忌精煉等特殊邏輯）。
	 */
	public void exhaustCharges(Item item)
	{
		final int objectId = item.getObjectId();
		_cache.put(objectId, 0);

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(REPLACE_SQL))
		{
			ps.setInt(1, objectId);
			ps.setInt(2, 0);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("[RefineSystem] 清空精煉次數失敗 itemId=" + objectId + ": " + e.getMessage());
		}
	}

	private int getDefaultCharges(Item item)
	{
		final RefineSystemData data = RefineSystemData.getInstance();
		return data.hasSpecialCharges(item.getId()) ? data.getSpecialCharges(item.getId()) : data.getDefaultCharges();
	}

	public static RefineSystemManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final RefineSystemManager INSTANCE = new RefineSystemManager();
	}
}
