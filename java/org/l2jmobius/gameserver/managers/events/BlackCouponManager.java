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
package org.l2jmobius.gameserver.managers.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.BlackCouponRestoreCategory;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemRestoreHolder;

public class BlackCouponManager
{
	private static final Logger LOGGER = Logger.getLogger(BlackCouponManager.class.getName());
	
	private ItemHolder _coupon;
	private int _multisellId;
	private final Map<Integer, EnumMap<BlackCouponRestoreCategory, List<ItemRestoreHolder>>> _playerRecords = new HashMap<>();
	private final Map<Integer, HashMap<BlackCouponRestoreCategory, Integer>> _xmlRestoreList = new HashMap<>(new HashMap<>());
	private final List<ItemRestoreHolder> _holdersToStore = new ArrayList<>();
	private final List<ItemRestoreHolder> _holdersToDelete = new ArrayList<>();
	private boolean _restoreIdFromXml;
	private boolean _eventPeriod;
	private final long[] _dateRange =
	{
		1651134344000L,
		2524608000000L
	};
	
	protected BlackCouponManager()
	{
	}
	
	public void setCouponItem(ItemHolder coupon)
	{
		_coupon = coupon;
	}
	
	public int getBlackCouponId()
	{
		return _coupon.getId();
	}
	
	public long getBlackCouponCount()
	{
		return _coupon.getCount();
	}
	
	public void setMultisellId(int id)
	{
		_multisellId = id;
	}
	
	public int getMultisellId()
	{
		return _multisellId;
	}
	
	public void setRestoreIdFromXml(boolean restoreIdByXml)
	{
		_restoreIdFromXml = restoreIdByXml;
	}
	
	public void setXmlRestoreList(Map<Integer, HashMap<BlackCouponRestoreCategory, Integer>> list)
	{
		_xmlRestoreList.putAll(list);
	}
	
	public void setEventStatus(boolean status)
	{
		_eventPeriod = status;
	}
	
	public boolean getEventStatus()
	{
		return _eventPeriod;
	}
	
	public void setDateRange(long first, long second)
	{
		_dateRange[0] = first;
		_dateRange[1] = second;
	}
	
	public boolean isInEventRange(long addTime)
	{
		return (_dateRange[0] < addTime) && (_dateRange[1] > addTime);
	}
	
	public BlackCouponRestoreCategory getCategoryByItemId(int itemId)
	{
		final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
		if (_restoreIdFromXml)
		{
			if (_xmlRestoreList.containsKey(itemId))
			{
				return _xmlRestoreList.get(item.getId()).keySet().stream().findFirst().orElse(null);
			}
			
			return null;
		}
		
		if (item.isWeapon() || item.isMagicWeapon())
		{
			return BlackCouponRestoreCategory.WEAPON;
		}
		
		if (item.isArmor())
		{
			return BlackCouponRestoreCategory.ARMOR;
		}
		
		if ((item.getBodyPart() == BodyPart.L_EAR) || (item.getBodyPart() == BodyPart.R_EAR) || (item.getBodyPart() == BodyPart.LR_EAR) || (item.getBodyPart() == BodyPart.L_FINGER) || (item.getBodyPart() == BodyPart.R_FINGER) || (item.getBodyPart() == BodyPart.LR_FINGER) || (item.getBodyPart() == BodyPart.NECK) || (item.getBodyPart() == BodyPart.ALLDRESS))
		{
			return BlackCouponRestoreCategory.BOSS_ACCESSORIES;
		}
		
		return BlackCouponRestoreCategory.MISC;
	}
	
	public List<ItemRestoreHolder> getRestoreItems(int ownerId, BlackCouponRestoreCategory category)
	{
		final EnumMap<BlackCouponRestoreCategory, List<ItemRestoreHolder>> ownerMap = _playerRecords.get(ownerId);
		if (ownerMap == null)
		{
			return Collections.emptyList();
		}
		
		return ownerMap.getOrDefault(category, Collections.emptyList());
	}
	
	public synchronized void removePlayerRecords(int ownerId)
	{
		final List<ItemRestoreHolder> toDelete = _holdersToDelete.stream().filter(holder -> holder.getOwnerId() == ownerId).toList();
		if (!toDelete.isEmpty())
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("DELETE FROM `black_coupon` WHERE owner_id = ? AND item_id = ? AND enchant_level = ? AND add_time = ?"))
			{
				for (ItemRestoreHolder holder : toDelete)
				{
					ps.setInt(1, holder.getOwnerId());
					ps.setInt(2, holder.getDestroyedItemId());
					ps.setShort(3, holder.getEnchantLevel());
					ps.setLong(4, holder.getDestroyDate());
					ps.addBatch();
				}
				
				ps.executeBatch();
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Problem deleting player records before removal. " + e.getMessage());
			}
			
			_holdersToDelete.removeAll(toDelete);
		}
		
		final List<ItemRestoreHolder> toStore = _holdersToStore.stream().filter(holder -> holder.getOwnerId() == ownerId).toList();
		if (!toStore.isEmpty())
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement("REPLACE INTO `black_coupon` (`owner_id`, `item_id`, `enchant_level`, `add_time`) values (?, ?, ?, ?)"))
			{
				for (ItemRestoreHolder holder : toStore)
				{
					if (!holder.needToStore())
					{
						continue;
					}
					
					ps.setInt(1, holder.getOwnerId());
					ps.setInt(2, holder.getDestroyedItemId());
					ps.setShort(3, holder.getEnchantLevel());
					ps.setLong(4, holder.getDestroyDate());
					ps.addBatch();
					
					holder.setNeedToStore(false);
				}
				
				ps.executeBatch();
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Problem storing player records before removal. " + e.getMessage());
			}
			
			_holdersToStore.removeAll(toStore);
		}
		
		_playerRecords.remove(ownerId);
	}
	
	public synchronized void restorePlayerRecords(int ownerId)
	{
		if (!_eventPeriod)
		{
			return;
		}
		
		final EnumMap<BlackCouponRestoreCategory, List<ItemRestoreHolder>> formedMap = new EnumMap<>(BlackCouponRestoreCategory.class);
		formedMap.put(BlackCouponRestoreCategory.WEAPON, new ArrayList<>());
		formedMap.put(BlackCouponRestoreCategory.ARMOR, new ArrayList<>());
		formedMap.put(BlackCouponRestoreCategory.BOSS_ACCESSORIES, new ArrayList<>());
		formedMap.put(BlackCouponRestoreCategory.MISC, new ArrayList<>());
		try (Connection conn = DatabaseFactory.getConnection();
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM `black_coupon` WHERE `owner_id` = ?"))
		{
			ps.setInt(1, ownerId);
			
			final ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				final int owner_id = rs.getInt("owner_id");
				final int item_id = rs.getInt("item_id");
				final short enchant_level = rs.getShort("enchant_level");
				final long destroyTime = rs.getLong("add_time");
				final BlackCouponRestoreCategory category = getCategoryByItemId(item_id);
				if (category == null)
				{
					continue;
				}
				
				final ItemRestoreHolder holder = new ItemRestoreHolder(owner_id, item_id, enchant_level, destroyTime, false);
				final Optional<Integer> any = _xmlRestoreList.get(item_id).values().stream().findAny();
				holder.setRepairItemId(any.orElseGet(holder::getDestroyedItemId));
				
				if (!_holdersToDelete.isEmpty())
				{
					final Stream<ItemRestoreHolder> check = _holdersToDelete.stream().filter(h -> h.getOwnerId() == owner_id).filter(h -> h.getDestroyDate() == destroyTime).filter(h -> h.getDestroyedItemId() == item_id).filter(h -> h.getEnchantLevel() == enchant_level);
					if (check.findFirst().isPresent())
					{
						continue;
					}
				}
				
				formedMap.get(category).add(holder);
			}
			
			ps.closeOnCompletion();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Problem loading event player records. " + e.getMessage());
		}
		
		_playerRecords.put(ownerId, formedMap);
	}
	
	public synchronized void createNewRecord(int objectId, int itemId, short enchantLevel)
	{
		final ItemRestoreHolder holder = new ItemRestoreHolder(objectId, itemId, enchantLevel, System.currentTimeMillis(), true);
		_holdersToStore.add(holder);
		
		if (!_eventPeriod)
		{
			return;
		}
		
		final BlackCouponRestoreCategory category = getCategoryByItemId(itemId);
		if (category == null)
		{
			return;
		}
		
		final Optional<Integer> any = _xmlRestoreList.getOrDefault(itemId, new HashMap<>()).values().stream().findAny();
		holder.setRepairItemId(any.orElseGet(holder::getDestroyedItemId));
		
		final EnumMap<BlackCouponRestoreCategory, List<ItemRestoreHolder>> playerRecords = _playerRecords.computeIfAbsent(objectId, _ -> new EnumMap<>(BlackCouponRestoreCategory.class));
		playerRecords.computeIfAbsent(category, _ -> new ArrayList<>()).add(holder);
	}
	
	public synchronized void addToDelete(BlackCouponRestoreCategory category, ItemRestoreHolder holder)
	{
		if (holder.needToStore())
		{
			holder.setNeedToStore(false);
			_holdersToStore.remove(holder);
		}
		
		_holdersToDelete.add(holder);
		_playerRecords.get(holder.getOwnerId()).get(category).remove(holder);
	}
	
	public void storeMe()
	{
		deleteRestoreHolders();
		saveRestoreHolders();
	}
	
	private synchronized void deleteRestoreHolders()
	{
		if (_holdersToDelete.isEmpty())
		{
			return;
		}
		
		try (Connection conn = DatabaseFactory.getConnection();
			PreparedStatement ps = conn.prepareStatement("DELETE FROM `black_coupon` WHERE owner_id = ? AND item_id = ? AND enchant_level = ? AND add_time = ?"))
		{
			for (ItemRestoreHolder holder : _holdersToDelete)
			{
				ps.setInt(1, holder.getOwnerId());
				ps.setInt(2, holder.getDestroyedItemId());
				ps.setShort(3, holder.getEnchantLevel());
				ps.setLong(4, holder.getDestroyDate());
				ps.addBatch();
			}
			
			ps.executeBatch();
			ps.closeOnCompletion();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Problem deleting event player records. " + e.getMessage());
		}
		
		_holdersToDelete.clear();
	}
	
	private synchronized void saveRestoreHolders()
	{
		if (_holdersToStore.isEmpty())
		{
			return;
		}
		
		try (Connection conn = DatabaseFactory.getConnection();
			PreparedStatement ps = conn.prepareStatement("REPLACE INTO `black_coupon` (`owner_id`, `item_id`, `enchant_level`, `add_time`) values (?, ?, ?, ?)"))
		{
			for (ItemRestoreHolder holder : _holdersToStore)
			{
				if (!holder.needToStore())
				{
					continue;
				}
				
				ps.setInt(1, holder.getOwnerId());
				ps.setInt(2, holder.getDestroyedItemId());
				ps.setShort(3, holder.getEnchantLevel());
				ps.setLong(4, holder.getDestroyDate());
				ps.addBatch();
				
				holder.setNeedToStore(false);
			}
			
			ps.executeBatch();
			ps.closeOnCompletion();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Problem storing event player records. " + e.getMessage());
		}
		
		_holdersToStore.clear();
	}
	
	public static BlackCouponManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BlackCouponManager INSTANCE = new BlackCouponManager();
	}
}
