package org.l2jmobius.gameserver.data.xml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.holders.PetCollectionData;
import org.l2jmobius.gameserver.data.holders.PetHatchData;
import org.l2jmobius.gameserver.data.holders.UnclaimedPetData;

/**
 * 寵物孵化系統 DAO
 * 負責所有資料庫讀寫操作
 * @author Custom
 */
public class PetHatchingDAO
{
	private static final Logger LOGGER = Logger.getLogger(PetHatchingDAO.class.getName());

	// ==================== 孵化統計 ====================

	public static int getPlayerTotalHatches(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT total_hatches FROM pet_hatching_stats WHERE player_id = ?"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next()) return rs.getInt("total_hatches");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error getting total hatches: " + e.getMessage());
		}
		return 0;
	}

	public static int getPlayerCurrentSlots(int playerId, int initialSlots)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT current_slots FROM pet_hatching_stats WHERE player_id = ?"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next()) return rs.getInt("current_slots");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error getting current slots: " + e.getMessage());
		}
		initializePlayerStats(playerId, initialSlots);
		return initialSlots;
	}

	public static void initializePlayerStats(int playerId, int initialSlots)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO pet_hatching_stats (player_id, total_hatches, current_slots) VALUES (?, 0, ?) ON DUPLICATE KEY UPDATE player_id=player_id"))
		{
			ps.setInt(1, playerId);
			ps.setInt(2, initialSlots);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error initializing player stats: " + e.getMessage());
		}
	}

	public static void incrementPlayerHatchCount(int playerId, int initialSlots, int hatchCountPerSlot, int maxSlots)
	{
		int totalHatches = getPlayerTotalHatches(playerId) + 1;
		int currentSlots = getPlayerCurrentSlots(playerId, initialSlots);

		if (currentSlots < maxSlots && totalHatches % hatchCountPerSlot == 0)
		{
			currentSlots++;
		}

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO pet_hatching_stats (player_id, total_hatches, current_slots) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE total_hatches = ?, current_slots = ?"))
		{
			ps.setInt(1, playerId); ps.setInt(2, totalHatches); ps.setInt(3, currentSlots);
			ps.setInt(4, totalHatches); ps.setInt(5, currentSlots);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error incrementing hatch count: " + e.getMessage());
		}
	}

	// ==================== 孵化資料 ====================

	public static List<PetHatchData> getPlayerHatchData(int playerId)
	{
		List<PetHatchData> list = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM pet_hatching_data WHERE player_id = ? ORDER BY slot_index"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					list.add(new PetHatchData(
						rs.getInt("player_id"), rs.getInt("slot_index"),
						rs.getInt("egg_item_id"), rs.getInt("egg_tier"),
						rs.getLong("start_time"), rs.getInt("hatch_duration"),
						rs.getInt("feed_consumed"), rs.getInt("upgrade_chance")
					));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error getting player hatch data: " + e.getMessage());
		}
		return list;
	}

	public static PetHatchData getHatchDataBySlot(int playerId, int slotIndex)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM pet_hatching_data WHERE player_id = ? AND slot_index = ?"))
		{
			ps.setInt(1, playerId); ps.setInt(2, slotIndex);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return new PetHatchData(
						rs.getInt("player_id"), rs.getInt("slot_index"),
						rs.getInt("egg_item_id"), rs.getInt("egg_tier"),
						rs.getLong("start_time"), rs.getInt("hatch_duration"),
						rs.getInt("feed_consumed"), rs.getInt("upgrade_chance")
					);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error getting hatch data by slot: " + e.getMessage());
		}
		return null;
	}

	public static void saveHatchData(int playerId, int slotIndex, int eggItemId, int eggTier, long startTime, int hatchDuration, int feedConsumed, int upgradeChance)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO pet_hatching_data (player_id, slot_index, egg_item_id, egg_tier, start_time, hatch_duration, feed_consumed, upgrade_chance) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE egg_item_id=?, egg_tier=?, start_time=?, hatch_duration=?, feed_consumed=?, upgrade_chance=?"))
		{
			ps.setInt(1, playerId); ps.setInt(2, slotIndex); ps.setInt(3, eggItemId);
			ps.setInt(4, eggTier); ps.setLong(5, startTime); ps.setInt(6, hatchDuration);
			ps.setInt(7, feedConsumed); ps.setInt(8, upgradeChance);
			ps.setInt(9, eggItemId); ps.setInt(10, eggTier); ps.setLong(11, startTime);
			ps.setInt(12, hatchDuration); ps.setInt(13, feedConsumed); ps.setInt(14, upgradeChance);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error saving hatch data: " + e.getMessage());
		}
	}

	public static void updateHatchDataForUpgrade(int playerId, int slotIndex, int newEggItemId, int newTier, long newStartTime, int newHatchDuration, int baseUpgradeChance)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE pet_hatching_data SET egg_item_id=?, egg_tier=?, start_time=?, hatch_duration=?, feed_consumed=0, upgrade_chance=? WHERE player_id=? AND slot_index=?"))
		{
			ps.setInt(1, newEggItemId); ps.setInt(2, newTier); ps.setLong(3, newStartTime);
			ps.setInt(4, newHatchDuration); ps.setInt(5, baseUpgradeChance);
			ps.setInt(6, playerId); ps.setInt(7, slotIndex);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error updating hatch data for upgrade: " + e.getMessage());
		}
	}

	public static void updateHatchUpgradeChance(int playerId, int slotIndex, int upgradeChance, int feedConsumed)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE pet_hatching_data SET upgrade_chance=?, feed_consumed=? WHERE player_id=? AND slot_index=?"))
		{
			ps.setInt(1, upgradeChance); ps.setInt(2, feedConsumed);
			ps.setInt(3, playerId); ps.setInt(4, slotIndex);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error updating upgrade chance: " + e.getMessage());
		}
	}

	public static void deleteHatchData(int playerId, int slotIndex)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM pet_hatching_data WHERE player_id=? AND slot_index=?"))
		{
			ps.setInt(1, playerId); ps.setInt(2, slotIndex);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error deleting hatch data: " + e.getMessage());
		}
	}

	public static List<PetHatchData> getAllHatchingData()
	{
		List<PetHatchData> list = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM pet_hatching_data"))
		{
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					list.add(new PetHatchData(
						rs.getInt("player_id"), rs.getInt("slot_index"),
						rs.getInt("egg_item_id"), rs.getInt("egg_tier"),
						rs.getLong("start_time"), rs.getInt("hatch_duration"),
						rs.getInt("feed_consumed"), rs.getInt("upgrade_chance")
					));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error getting all hatching data: " + e.getMessage());
		}
		return list;
	}

	// ==================== 飼料儲存 ====================

	public static int getPlayerStoredFeed(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT feed_count FROM pet_feed_storage WHERE player_id=?"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next()) return rs.getInt("feed_count");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error getting stored feed: " + e.getMessage());
		}
		return 0;
	}

	public static void updatePlayerStoredFeed(int playerId, int amount)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO pet_feed_storage (player_id, feed_count) VALUES (?, ?) ON DUPLICATE KEY UPDATE feed_count=?"))
		{
			ps.setInt(1, playerId); ps.setInt(2, amount); ps.setInt(3, amount);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error updating stored feed: " + e.getMessage());
		}
	}

	// ==================== 收藏系統 ====================

	public static List<PetCollectionData> getPlayerCollection(int playerId)
	{
		List<PetCollectionData> list = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM pet_collection WHERE player_id=?"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					list.add(new PetCollectionData(rs.getInt("player_id"), rs.getInt("pet_item_id"), rs.getInt("stored") == 1));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error getting player collection: " + e.getMessage());
		}
		return list;
	}

	public static boolean isPetStored(int playerId, int petItemId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT stored FROM pet_collection WHERE player_id=? AND pet_item_id=?"))
		{
			ps.setInt(1, playerId); ps.setInt(2, petItemId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next()) return rs.getInt("stored") == 1;
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error checking pet stored: " + e.getMessage());
		}
		return false;
	}

	public static void storePet(int playerId, int petItemId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO pet_collection (player_id, pet_item_id, stored) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE stored=1"))
		{
			ps.setInt(1, playerId); ps.setInt(2, petItemId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error storing pet: " + e.getMessage());
		}
	}

	public static void removePet(int playerId, int petItemId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE pet_collection SET stored=0 WHERE player_id=? AND pet_item_id=?"))
		{
			ps.setInt(1, playerId); ps.setInt(2, petItemId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error removing pet: " + e.getMessage());
		}
	}

	public static int getTotalCollectedCount(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM pet_collection WHERE player_id=? AND stored=1"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next()) return rs.getInt(1);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error getting total collected count: " + e.getMessage());
		}
		return 0;
	}

	// ==================== 未領取寵物 ====================

	public static void addUnclaimedPet(int playerId, int petItemId, int tier)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO pet_unclaimed (player_id, pet_item_id, tier, hatch_time) VALUES (?, ?, ?, ?)"))
		{
			ps.setInt(1, playerId);
			ps.setInt(2, petItemId);
			ps.setInt(3, tier);
			ps.setLong(4, System.currentTimeMillis());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error adding unclaimed pet: " + e.getMessage());
		}
	}

	public static List<UnclaimedPetData> getUnclaimedPets(int playerId)
	{
		List<UnclaimedPetData> list = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM pet_unclaimed WHERE player_id=? ORDER BY hatch_time DESC"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					list.add(new UnclaimedPetData(
						rs.getInt("id"),
						rs.getInt("player_id"),
						rs.getInt("pet_item_id"),
						rs.getInt("tier"),
						rs.getLong("hatch_time"),
						rs.getInt("event_fired") == 1
					));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error getting unclaimed pets: " + e.getMessage());
		}
		return list;
	}

	public static UnclaimedPetData getUnclaimedPetById(int playerId, int id)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM pet_unclaimed WHERE player_id=? AND id=?"))
		{
			ps.setInt(1, playerId);
			ps.setInt(2, id);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					return new UnclaimedPetData(
						rs.getInt("id"),
						rs.getInt("player_id"),
						rs.getInt("pet_item_id"),
						rs.getInt("tier"),
						rs.getLong("hatch_time"),
						rs.getInt("event_fired") == 1
					);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error getting unclaimed pet by id: " + e.getMessage());
		}
		return null;
	}

	public static void removeUnclaimedPet(int playerId, int id)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM pet_unclaimed WHERE player_id=? AND id=?"))
		{
			ps.setInt(1, playerId);
			ps.setInt(2, id);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error removing unclaimed pet: " + e.getMessage());
		}
	}

	public static int getUnclaimedPetsCount(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM pet_unclaimed WHERE player_id=?"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next()) return rs.getInt(1);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error getting unclaimed pets count: " + e.getMessage());
		}
		return 0;
	}

	public static void markEventFired(int playerId, int id)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE pet_unclaimed SET event_fired=1 WHERE player_id=? AND id=?"))
		{
			ps.setInt(1, playerId);
			ps.setInt(2, id);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error marking event fired: " + e.getMessage());
		}
	}

	// ==================== 寵物裝備檢查 ====================

	public static boolean hasPetEquipment(int petItemObjectId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM items WHERE owner_id=? AND loc='PET_EQUIP' AND loc_data=?"))
		{
			ps.setInt(1, petItemObjectId);
			ps.setInt(2, petItemObjectId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next()) return rs.getInt(1) > 0;
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingDAO: Error checking pet equipment: " + e.getMessage());
		}
		return false;
	}
}
