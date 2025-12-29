package org.l2jmobius.gameserver.model.drops;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * 世界限量掉落管理器
 * 特色：
 * 1. 全服限量掉落
 * 2. 統一觸發機率 + 隨機選擇道具
 * 3. 統一重置時間
 */
public class WorldDropManager
{
	// ★★★ 核心配置：統一掉落觸發機率（100000分之X）★★★
	private static final int CHANCE_BASE = 100000;
	private static final int DROP_CHANCE = 1; // 1/100000 的機率觸發掉落
	
	// 單例
	private static WorldDropManager _instance;
	
	// 掉落配置快取
	private final List<WorldDropConfig> _dropConfigs = new CopyOnWriteArrayList<>();
	
	// 重置設定
	private long _lastReset = 0;
	private long _nextReset = 0;
	private int _resetDays = 7;
	
	// 時間格式
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	
	private WorldDropManager()
	{
		load();
		scheduleResetCheck();
		System.out.println("### WorldDropManager: 已啟動");
		System.out.println("  觸發機率: " + DROP_CHANCE + "/" + CHANCE_BASE + " (" + ((double) DROP_CHANCE / CHANCE_BASE * 100) + "%)");
	}
	
	public static WorldDropManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new WorldDropManager();
		}
		return _instance;
	}
	
	/**
	 * 從資料庫載入配置
	 */
	public void load()
	{
		_dropConfigs.clear();
		
		// 載入重置設定
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT last_reset, next_reset, reset_days FROM world_drop_settings WHERE id = 1"))
		{
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					_lastReset = rs.getLong("last_reset");
					_nextReset = rs.getLong("next_reset");
					_resetDays = rs.getInt("reset_days");
				}
				else
				{
					// 初始化
					initializeSettings();
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("WorldDropManager: 載入重置設定失敗 - " + e.getMessage());
			e.printStackTrace();
		}
		
		// 載入掉落配置
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"SELECT item_id, item_name, max_count, current_count FROM world_drop_config WHERE enabled = 1"))
		{
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					WorldDropConfig config = new WorldDropConfig();
					config.itemId = rs.getInt("item_id");
					config.itemName = rs.getString("item_name");
					config.maxCount = rs.getInt("max_count");
					config.currentCount = rs.getInt("current_count");
					_dropConfigs.add(config);
				}
			}
			
			System.out.println("### WorldDropManager: 已載入 " + _dropConfigs.size() + " 個掉落配置");
			for (WorldDropConfig config : _dropConfigs)
			{
				System.out.println("  - " + config.itemName + " (ID:" + config.itemId + ") 剩餘:" + config.currentCount + "/" + config.maxCount);
			}
			System.out.println("  下次重置時間: " + DATE_FORMAT.format(new Date(_nextReset)));
		}
		catch (Exception e)
		{
			System.err.println("WorldDropManager: 載入配置失敗 - " + e.getMessage());
			e.printStackTrace();
		}
		
		// 啟動時立即檢查是否需要重置
		checkAndReset();
	}
	
	/**
	 * 初始化重置設定
	 */
	private void initializeSettings()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"INSERT INTO world_drop_settings (id, last_reset, next_reset, reset_days) VALUES (1, ?, ?, ?)"))
		{
			long now = System.currentTimeMillis();
			long nextReset = now + TimeUnit.DAYS.toMillis(7);
			
			ps.setLong(1, now);
			ps.setLong(2, nextReset);
			ps.setInt(3, 7);
			ps.executeUpdate();
			
			_lastReset = now;
			_nextReset = nextReset;
			_resetDays = 7;
			
			System.out.println("### WorldDropManager: 已初始化重置設定");
		}
		catch (Exception e)
		{
			System.err.println("WorldDropManager: 初始化設定失敗 - " + e.getMessage());
		}
	}
	
	/**
	 * 定時檢查重置（每小時）
	 */
	private void scheduleResetCheck()
	{
		// 1小時 = 60分鐘 * 60秒 * 1000毫秒 = 3600000 毫秒
		ThreadPool.scheduleAtFixedRate(() ->
		{
			checkAndReset();
		}, 60 * 60 * 1000, 60 * 60 * 1000);
	}
	
	/**
	 * 檢查並執行重置
	 */
	private void checkAndReset()
	{
		long now = System.currentTimeMillis();
		
		if (now >= _nextReset)
		{
			resetAll();
		}
	}
	
	/**
	 * 重置所有掉落
	 */
	private synchronized void resetAll()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			// 重置所有道具數量
			try (PreparedStatement ps = con.prepareStatement(
				"UPDATE world_drop_config SET current_count = max_count WHERE enabled = 1"))
			{
				ps.executeUpdate();
			}
			
			// 更新重置時間
			long now = System.currentTimeMillis();
			long nextReset = now + TimeUnit.DAYS.toMillis(_resetDays);
			
			try (PreparedStatement ps = con.prepareStatement(
				"UPDATE world_drop_settings SET last_reset = ?, next_reset = ? WHERE id = 1"))
			{
				ps.setLong(1, now);
				ps.setLong(2, nextReset);
				ps.executeUpdate();
			}
			
			_lastReset = now;
			_nextReset = nextReset;
			
			// 重新載入配置
			load();
			
			// 廣播重置公告
			String message = "【世界掉落】所有限量道具已重置！下次重置時間：" + DATE_FORMAT.format(new Date(_nextReset));
			broadcastToWorld(message);
			
			System.out.println("### WorldDropManager: 已執行重置，下次重置：" + DATE_FORMAT.format(new Date(_nextReset)));
		}
		catch (Exception e)
		{
			System.err.println("WorldDropManager: 重置失敗 - " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * ★★★ 核心方法：嘗試掉落世界限量道具 ★★★
	 * 邏輯：先判定是否觸發（統一機率），如果觸發則從列表中隨機選擇一個道具
	 * @return 掉落的道具ID，如果沒有掉落則返回 0
	 */
	public int tryDrop(Player player)
	{
		if ((player == null) || _dropConfigs.isEmpty())
		{
			return 0;
		}
		
		// ★ 步驟1：統一機率判定
		int randomValue = ThreadLocalRandom.current().nextInt(CHANCE_BASE);
		if (randomValue >= DROP_CHANCE)
		{
			// 未觸發掉落
			return 0;
		}
		
		// ★ 步驟2：收集所有有剩餘數量的道具
		List<WorldDropConfig> availableDrops = new ArrayList<>();
		for (WorldDropConfig config : _dropConfigs)
		{
			if (config.currentCount > 0)
			{
				availableDrops.add(config);
			}
		}
		
		if (availableDrops.isEmpty())
		{
			// 所有道具都已掉完
			return 0;
		}
		
		// ★ 步驟3：從可用清單中隨機選擇一個
		WorldDropConfig selectedConfig = availableDrops.get(ThreadLocalRandom.current().nextInt(availableDrops.size()));
		
		// ★ 步驟4：執行掉落
		return executeDrop(player, selectedConfig);
	}
	
	/**
	 * 執行掉落
	 */
	private synchronized int executeDrop(Player player, WorldDropConfig config)
	{
		// 雙重檢查
		if (config.currentCount <= 0)
		{
			return 0;
		}
		
		// 更新資料庫
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(
				"UPDATE world_drop_config SET current_count = current_count - 1 WHERE item_id = ? AND current_count > 0"))
		{
			ps.setInt(1, config.itemId);
			int updated = ps.executeUpdate();
			
			if (updated > 0)
			{
				// 更新快取
				config.currentCount--;
				
				// 廣播公告
				broadcastDrop(player.getName(), config.itemName, config.currentCount);
				
				return config.itemId;
			}
		}
		catch (Exception e)
		{
			System.err.println("WorldDropManager: 執行掉落失敗 - " + e.getMessage());
		}
		
		return 0;
	}
	
	/**
	 * 廣播掉落公告
	 */
	private void broadcastDrop(String playerName, String itemName, int leftCount)
	{
		String nextTimeStr = DATE_FORMAT.format(new Date(_nextReset));
		String message = "【世界掉落】恭喜玩家【" + playerName + "】獲得限量道具【" + itemName + "】！剩餘數量：" + leftCount + " 個 | 下次重置：" + nextTimeStr;
		broadcastToWorld(message);
	}
	
	/**
	 * 向全世界廣播
	 */
	private void broadcastToWorld(String message)
	{
		CreatureSay packet = new CreatureSay(null, ChatType.WORLD, "公告", message);
		
		for (Player player : World.getInstance().getPlayers())
		{
			if ((player != null) && player.isOnline())
			{
				player.sendPacket(packet);
			}
		}
		
		System.out.println("WorldDrop 公告: " + message);
	}
	
	/**
	 * 獲取下次重置時間
	 */
	public long getNextResetTime()
	{
		return _nextReset;
	}
	
	/**
	 * 掉落配置類別
	 */
	private static class WorldDropConfig
	{
		int itemId;
		String itemName;
		int maxCount;
		int currentCount;
	}
}