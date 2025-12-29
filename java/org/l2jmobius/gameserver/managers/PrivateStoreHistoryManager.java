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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * @author Mobius
 */
public class PrivateStoreHistoryManager
{
	protected static final Logger LOGGER = Logger.getLogger(PrivateStoreHistoryManager.class.getName());
	
	private static final String SELECT = "SELECT * FROM item_transaction_history";
	private static final String INSERT = "INSERT INTO item_transaction_history (created_time,item_id,transaction_type,enchant_level,price,count) VALUES (?,?,?,?,?,?)";
	private static final String TRUNCATE = "TRUNCATE TABLE item_transaction_history";
	
	private static final ArrayList<ItemHistoryTransaction> _items = new ArrayList<>();
	
	public void registerTransaction(PrivateStoreType transactionType, Item item, long count, long price)
	{
		try
		{
			final ItemHistoryTransaction historyItem = new ItemHistoryTransaction(transactionType, count, price, item);
			_items.add(historyItem);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store history for item: " + item, e);
		}
	}
	
	public void restore()
	{
		_items.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT))
		{
			try (ResultSet rs = statement.executeQuery())
			{
				while (rs.next())
				{
					final ItemHistoryTransaction item = new ItemHistoryTransaction(rs);
					_items.add(item);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore history.", e);
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _items.size() + " items history.");
	}
	
	public void reset()
	{
		_items.clear();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(TRUNCATE))
		{
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not reset history.", e);
		}
		
		LOGGER.info(getClass().getSimpleName() + ": weekly reset.");
	}
	
	public List<ItemHistoryTransaction> getHistory()
	{
		return getHistory(false);
	}
	
	public List<ItemHistoryTransaction> getHistory(boolean full)
	{
		if (!full)
		{
			final List<ItemHistoryTransaction> tempList = new ArrayList<>(_items);
			final Map<Integer, Integer> uniqueItemIds = new HashMap<>();
			for (ItemHistoryTransaction transaction : tempList)
			{
				final int itemId = transaction.getItemId();
				if (!uniqueItemIds.containsKey(itemId))
				{
					uniqueItemIds.put(itemId, 0);
				}
			}
			
			tempList.sort(new SortByDate());
			
			final List<ItemHistoryTransaction> finalList = new ArrayList<>();
			for (ItemHistoryTransaction transaction : tempList)
			{
				final int itemId = transaction.getItemId();
				if (uniqueItemIds.get(itemId) < GeneralConfig.STORE_REVIEW_LIMIT)
				{
					finalList.add(transaction);
					uniqueItemIds.put(itemId, uniqueItemIds.get(itemId) + 1);
				}
			}
			
			return finalList;
		}
		
		return _items;
	}
	
	public List<ItemHistoryTransaction> getTopHighestItem()
	{
		final List<ItemHistoryTransaction> list = new ArrayList<>(_items);
		list.sort(new SortByPrice());
		return list;
	}
	
	public List<ItemHistoryTransaction> getTopMostItem()
	{
		final Map<Integer, ItemHistoryTransaction> map = new HashMap<>();
		for (ItemHistoryTransaction transaction : _items)
		{
			if (map.get(transaction.getItemId()) == null)
			{
				map.put(transaction.getItemId(), new ItemHistoryTransaction(transaction.getTransactionType(), transaction.getCount(), transaction.getPrice(), transaction.getItemId(), 0, false));
			}
			else
			{
				map.get(transaction.getItemId()).addCount(transaction.getCount());
			}
		}
		
		final List<ItemHistoryTransaction> list = new ArrayList<>();
		map.forEach((_, transaction) -> list.add(transaction));
		list.sort(new SortByQuantity());
		return list;
	}
	
	protected static class SortByPrice implements Comparator<ItemHistoryTransaction>
	{
		@Override
		public int compare(ItemHistoryTransaction a, ItemHistoryTransaction b)
		{
			return a.getPrice() > b.getPrice() ? -1 : a.getPrice() == b.getPrice() ? 0 : 1;
		}
	}
	
	protected static class SortByQuantity implements Comparator<ItemHistoryTransaction>
	{
		@Override
		public int compare(ItemHistoryTransaction a, ItemHistoryTransaction b)
		{
			return a.getCount() > b.getCount() ? -1 : a.getCount() == b.getCount() ? 0 : 1;
		}
	}
	
	protected static class SortByDate implements Comparator<ItemHistoryTransaction>
	{
		@Override
		public int compare(ItemHistoryTransaction a, ItemHistoryTransaction b)
		{
			return a.getTransactionDate() > b.getTransactionDate() ? -1 : a.getTransactionDate() == b.getTransactionDate() ? 0 : 1;
		}
	}
	
	public static class ItemHistoryTransaction
	{
		private final long _transactionDate;
		private final int _itemId;
		private final PrivateStoreType _transactionType;
		private final int _enchantLevel;
		private final long _price;
		private long _count;
		
		public ItemHistoryTransaction(ResultSet rs) throws SQLException
		{
			_transactionDate = rs.getLong("created_time");
			_itemId = rs.getInt("item_id");
			_transactionType = rs.getInt("transaction_type") == 0 ? PrivateStoreType.SELL : PrivateStoreType.BUY;
			_enchantLevel = rs.getInt("enchant_level");
			_price = rs.getLong("price");
			_count = rs.getLong("count");
		}
		
		public ItemHistoryTransaction(PrivateStoreType transactionType, long count, long price, Item item)
		{
			this(transactionType, count, price, item.getId(), item.getEnchantLevel(), true);
		}
		
		public ItemHistoryTransaction(PrivateStoreType transactionType, long count, long price, int itemId, int enchantLevel, boolean saveToDB)
		{
			_transactionDate = System.currentTimeMillis();
			_itemId = itemId;
			_transactionType = transactionType;
			_enchantLevel = enchantLevel;
			_price = price;
			_count = count;
			
			if (saveToDB)
			{
				storeInDB();
			}
		}
		
		public long getTransactionDate()
		{
			return _transactionDate;
		}
		
		public PrivateStoreType getTransactionType()
		{
			return _transactionType;
		}
		
		public int getItemId()
		{
			return _itemId;
		}
		
		public int getEnchantLevel()
		{
			return _enchantLevel;
		}
		
		public long getPrice()
		{
			return _price;
		}
		
		public long getCount()
		{
			return _count;
		}
		
		public void addCount(long count)
		{
			_count += count;
		}
		
		private void storeInDB()
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(INSERT))
			{
				ps.setLong(1, _transactionDate);
				ps.setInt(2, _itemId);
				ps.setInt(3, _transactionType == PrivateStoreType.SELL ? 0 : 1);
				ps.setInt(4, _enchantLevel);
				ps.setLong(5, _price);
				ps.setLong(6, _count);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Could not insert history item " + this + " into DB: Reason: " + e.getMessage(), e);
			}
		}
		
		@Override
		public String toString()
		{
			return _transactionDate + "(" + _transactionType + ")" + "[" + _itemId + " +" + _enchantLevel + " c:" + _count + " p:" + _price + " ]";
		}
	}
	
	public static PrivateStoreHistoryManager getInstance()
	{
		return PrivateStoreHistoryManager.SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PrivateStoreHistoryManager INSTANCE = new PrivateStoreHistoryManager();
	}
}
