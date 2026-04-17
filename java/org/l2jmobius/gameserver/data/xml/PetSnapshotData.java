package org.l2jmobius.gameserver.data.xml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;

/**
 * 魂契系統 — 資料管理器
 *
 * 職責：
 *   - 維護記憶體快取 Map<playerId, PetSnapshotEntry>
 *   - 提供 DB 讀取 / 寫入 / 清除
 *   - 對外提供 hasSnapshot() / getSnapshot()
 *
 * 內部代碼使用 snapshot，對玩家展示使用「魂契」。
 *
 * @author Custom
 */
public class PetSnapshotData
{
	private static final Logger LOGGER = Logger.getLogger(PetSnapshotData.class.getName());

	/** 記憶體快取：playerId → 魂契數據 */
	private final ConcurrentHashMap<Integer, PetSnapshotEntry> _cache = new ConcurrentHashMap<>();

	// ==================== 內部資料結構 ====================

	/**
	 * 魂契數據條目（4 個共享屬性 + 元資料）
	 */
	public static class PetSnapshotEntry
	{
		public final int    petItemId;
		public final double patk;
		public final double matk;
		public final double pdef;
		public final double mdef;
		public final long   snapshotTime;

		public PetSnapshotEntry(int petItemId, double patk, double matk, double pdef, double mdef, long snapshotTime)
		{
			this.petItemId    = petItemId;
			this.patk         = patk;
			this.matk         = matk;
			this.pdef         = pdef;
			this.mdef         = mdef;
			this.snapshotTime = snapshotTime;
		}
	}

	// ==================== 單例 ====================

	private PetSnapshotData()
	{
	}

	public static PetSnapshotData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		private static final PetSnapshotData INSTANCE = new PetSnapshotData();
	}

	// ==================== 公開 API ====================

	/**
	 * 玩家是否已締結魂契（快取優先）
	 */
	public boolean hasSnapshot(int playerId)
	{
		return _cache.containsKey(playerId);
	}

	/**
	 * 取得玩家的魂契數據（快取優先，無則回傳 null）
	 */
	public PetSnapshotEntry getSnapshot(int playerId)
	{
		return _cache.get(playerId);
	}

	/**
	 * 從資料庫載入指定玩家的魂契數據到記憶體快取。
	 * 用於玩家登入時還原。
	 */
	public void loadSnapshot(int playerId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"SELECT pet_item_id, patk, matk, pdef, mdef, snapshot_time FROM pet_snapshot WHERE player_id = ?"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					final PetSnapshotEntry entry = new PetSnapshotEntry(
						rs.getInt("pet_item_id"),
						rs.getDouble("patk"),
						rs.getDouble("matk"),
						rs.getDouble("pdef"),
						rs.getDouble("mdef"),
						rs.getLong("snapshot_time"));
					_cache.put(playerId, entry);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("PetSnapshotData: loadSnapshot 失敗 playerId=" + playerId + " " + e.getMessage());
		}
	}

	/**
	 * 儲存魂契數據至資料庫，並更新記憶體快取。
	 */
	public void saveSnapshot(int playerId, int petItemId, double patk, double matk, double pdef, double mdef)
	{
		final long now = System.currentTimeMillis();
		final PetSnapshotEntry entry = new PetSnapshotEntry(petItemId, patk, matk, pdef, mdef, now);

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"INSERT INTO pet_snapshot (player_id, pet_item_id, patk, matk, pdef, mdef, snapshot_time) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?) " +
				"ON DUPLICATE KEY UPDATE pet_item_id=VALUES(pet_item_id), patk=VALUES(patk), matk=VALUES(matk), " +
				"pdef=VALUES(pdef), mdef=VALUES(mdef), snapshot_time=VALUES(snapshot_time)"))
		{
			ps.setInt(1, playerId);
			ps.setInt(2, petItemId);
			ps.setDouble(3, patk);
			ps.setDouble(4, matk);
			ps.setDouble(5, pdef);
			ps.setDouble(6, mdef);
			ps.setLong(7, now);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetSnapshotData: saveSnapshot 失敗 playerId=" + playerId + " " + e.getMessage());
			return;
		}

		_cache.put(playerId, entry);
	}

	/**
	 * 清除玩家的魂契數據（資料庫 + 記憶體快取）。
	 */
	public void clearSnapshot(int playerId)
	{
		_cache.remove(playerId);

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"DELETE FROM pet_snapshot WHERE player_id = ?"))
		{
			ps.setInt(1, playerId);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning("PetSnapshotData: clearSnapshot 失敗 playerId=" + playerId + " " + e.getMessage());
		}
	}

	/**
	 * 從記憶體快取移除（不刪 DB）。
	 * 用於玩家下線時釋放記憶體。
	 */
	public void unloadSnapshot(int playerId)
	{
		_cache.remove(playerId);
	}
}
