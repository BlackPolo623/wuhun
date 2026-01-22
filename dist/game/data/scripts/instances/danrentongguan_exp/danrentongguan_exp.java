package instances.danrentongguan_exp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 生存副本 - 單人通關經驗副本
 * 玩家需要在限定時間內擊殺盡可能多的怪物，擊殺數量會被記錄到排行榜
 */
public class danrentongguan_exp extends InstanceScript
{
	// ================================
	// 副本設定區（可自訂調整）
	// ================================

	// NPC配置
	private static final int SORA = 900031; // 進入NPC ID
	private static final int GOLBERG = 80000; // 刷新怪物ID

	// 副本基礎配置
	private static final int TEMPLATE_ID = 901; // 副本模板ID
	private static final int ITEM_ID = 57; // 挑戰道具ID（金幣）
	private static final int ITEM_COUNT = 10000000; // 挑戰道具數量（1000萬金幣）
	private static final int DAILY_LIMIT = 5; // 每日挑戰次數上限

	// 時間配置（秒）
	private static final int SPAWN_INTERVAL = 1; // 怪物刷新間隔（秒）
	private static final int SURVIVAL_TIME = 120; // 生存時間要求（秒）
	private static final int CLEAR_TIME_LIMIT = 130; // 清理時間上限（秒）

	// 重置時間配置（使用時間戳方式，可自訂刷新時間）
	private static final int RESET_HOUR = 0; // 重置小時（0-23）例如：0 = 凌晨12點
	private static final int RESET_MINUTE = 0; // 重置分鐘（0-59）
	private static final int RESET_SECOND = 0; // 重置秒數（0-59）

	// 排行榜配置
	private static final int LEADERBOARD_SIZE = 15; // 排行榜顯示人數

	// ================================
	// 系統常量區（請勿修改）
	// ================================
	
	// 怪物刷新位置
	private static final Location[] SPAWN_LOCATIONS =
	{
		new Location(-147433, 213320, -10064),
		new Location(-147093, 212487, -10064),
		new Location(-146843, 213069, -10064),
		new Location(-147682, 212734, -10064)
	};

	// 玩家變量名稱
	private static final String VAR_DAILY_COUNT = "猛襲生存次數";
	private static final String VAR_ENTER_TIME = "生存進入時間";
	private static final String VAR_RESET_TIME = "生存刷新時間";
	private static final String VAR_KILL_COUNT = "生存擊殺數量"; // 當前副本擊殺數
	
	// 玩家任務追蹤（使用線程安全的Map）
	private static final Map<Integer, PlayerTaskData> playerTasks = new ConcurrentHashMap<>();

	// 排行榜內存緩存（減少數據庫查詢）
	private static final Map<String, LeaderboardEntry> leaderboardCache = new ConcurrentHashMap<>();
	private static volatile boolean cacheLoaded = false;

	// 全局定時任務
	private static ScheduledFuture<?> globalTask = null;
	
	/**
	 * 玩家任務數據類
	 */
	private static class PlayerTaskData
	{
		long enterTime;
		int killCount;

		PlayerTaskData(long enterTime)
		{
			this.enterTime = enterTime;
			this.killCount = 0;
		}
	}

	/**
	 * 排行榜記錄類
	 */
	public static class LeaderboardEntry
	{
		private final String playerName;
		private final int killCount;
		private final long recordTime;

		public LeaderboardEntry(String playerName, int killCount, long recordTime)
		{
			this.playerName = playerName;
			this.killCount = killCount;
			this.recordTime = recordTime;
		}

		public String getPlayerName()
		{
			return playerName;
		}

		public int getKillCount()
		{
			return killCount;
		}

		public long getRecordTime()
		{
			return recordTime;
		}
	}
	
	public danrentongguan_exp()
	{
		super(TEMPLATE_ID);
		addStartNpc(SORA);
		addFirstTalkId(SORA);
		addTalkId(SORA);
		addKillId(GOLBERG);
		addAttackId(GOLBERG);
		addInstanceLeaveId(TEMPLATE_ID);

		// 初始化全局任務
		initializeGlobalTask();

		// 預載入排行榜緩存
		loadLeaderboardToCache();
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enterInstance":
			{
				return handleEnterInstance(player);
			}
			case "showLeaderboard":
			{
				return showLeaderboard(player);
			}
		}
		return "";
	}
	
	/**
	 * 處理進入副本邏輯
	 */
	private String handleEnterInstance(Player player)
	{
		// 檢查每日次數限制
		if (!checkDailyLimit(player))
		{
			player.sendPacket(new ExShowScreenMessage("每天最大挑戰次數為:" + DAILY_LIMIT + "!", 2000));
			return "";
		}
		
		// 檢查挑戰道具
		if (!player.destroyItemByItemId(null, ITEM_ID, ITEM_COUNT, player, true))
		{
			player.sendPacket(new ExShowScreenMessage("挑戰道具不足!", 2000));
			return "";
		}
		
		// 進入副本
		enterInstance(player, null, TEMPLATE_ID);
		
		// 記錄玩家數據
		long nowMills = System.currentTimeMillis();
		playerTasks.put(player.getObjectId(), new PlayerTaskData(nowMills));

		// 重置擊殺計數
		player.getVariables().set(VAR_KILL_COUNT, 0);

		// 更新玩家變量
		updatePlayerVariables(player, nowMills);

		return "";
	}
	
	/**
	 * 檢查每日次數限制
	 */
	private boolean checkDailyLimit(Player player)
	{
		long resetTime = player.getVariables().getLong(VAR_RESET_TIME, 0);
		
		// 如果已過重置時間，重置次數
		if (resetTime < System.currentTimeMillis())
		{
			player.getVariables().set(VAR_DAILY_COUNT, 0);
		}
		
		int dailyCount = player.getVariables().getInt(VAR_DAILY_COUNT, 0);
		return dailyCount < DAILY_LIMIT;
	}
	
	/**
	 * 更新玩家變量
	 */
	private void updatePlayerVariables(Player player, long nowMills)
	{
		// 增加挑戰次數
		int currentCount = player.getVariables().getInt(VAR_DAILY_COUNT, 0);
		player.getVariables().set(VAR_DAILY_COUNT, currentCount + 1);

		// 設置進入時間
		player.getVariables().set(VAR_ENTER_TIME, nowMills);

		// 計算下次重置時間（使用配置的重置時間）
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, RESET_HOUR);
		calendar.set(Calendar.MINUTE, RESET_MINUTE);
		calendar.set(Calendar.SECOND, RESET_SECOND);
		calendar.set(Calendar.MILLISECOND, 0);

		// 如果當前時間已經過了今天的重置時間，則設定為明天的重置時間
		if (calendar.getTimeInMillis() <= nowMills)
		{
			calendar.add(Calendar.DAY_OF_MONTH, 1);
		}

		player.getVariables().set(VAR_RESET_TIME, calendar.getTimeInMillis());
	}
	
	/**
	 * 初始化全局任務
	 */
	private void initializeGlobalTask()
	{
		// 如果已存在任務，先取消
		if (globalTask != null)
		{
			globalTask.cancel(true);
		}
		
		// 啟動全局定時任務（每秒執行一次）
		globalTask = ThreadPool.scheduleAtFixedRate(this::processPlayerTasks, 1000, 1000);
	}
	
	/**
	 * 處理所有玩家的任務
	 */
	private void processPlayerTasks()
	{
		// 使用迭代器安全遍歷
		playerTasks.entrySet().removeIf(entry ->
		{
			int playerId = entry.getKey();
			PlayerTaskData taskData = entry.getValue();
			
			Player player = World.getInstance().getPlayer(playerId);
			if (player == null)
			{
				return true; // 玩家不存在，移除任務
			}
			
			return processPlayerTask(player, taskData);
		});
	}
	
	/**
	 * 處理單個玩家任務
	 * @return true表示需要移除任務，false表示繼續保留
	 */
	private boolean processPlayerTask(Player player, PlayerTaskData taskData)
	{
		Instance world = player.getInstanceWorld();
		
		// 檢查玩家是否離開副本
		if ((world == null) || (player.getInstanceId() == 0))
		{
			player.getVariables().set(VAR_ENTER_TIME, 0);
			player.sendPacket(new ExShowScreenMessage("玩家[" + player.getName() + "]退出副本,副本結束", 3000));
			return true;
		}
		
		// 檢查玩家是否死亡
		if (player.isDead())
		{
			player.getVariables().set(VAR_ENTER_TIME, 0);
			player.sendPacket(new ExShowScreenMessage("玩家[" + player.getName() + "]死亡,挑戰失敗,退出副本,副本結束", 3000));
			world.removeNpcs();
			world.removePlayer(player);
			return true;
		}
		
		// 計算經過的時間
		long timeSpent = System.currentTimeMillis() - taskData.enterTime;
		int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(timeSpent);
		
		// 生存階段（0-120秒）
		if (seconds < SURVIVAL_TIME)
		{
			// 每秒刷新怪物
			if ((seconds % SPAWN_INTERVAL) == 0)
			{
				spawnMonsters(player, world);
			}
			player.sendPacket(new ExShowScreenMessage("當前生存時間為:[" + seconds + "]秒,每" + SPAWN_INTERVAL + "秒刷新" + SPAWN_LOCATIONS.length + "個怪物,堅持" + SURVIVAL_TIME + "秒即可通關", 3000));
			return false;
		}
		// 清理階段（120-130秒）
		else if (seconds < CLEAR_TIME_LIMIT)
		{
			// 檢查是否清理完畢
			if (world.getAliveNpcCount() == 0)
			{
				int killCount = player.getVariables().getInt(VAR_KILL_COUNT, 0);
				player.getVariables().set(VAR_ENTER_TIME, 0);
				player.sendPacket(new ExShowScreenMessage("恭喜通關！共擊殺 " + killCount + " 隻怪物！", 3000));

				// 保存排行榜記錄
				saveLeaderboardRecord(player, killCount);

				return true;
			}

			// 顯示剩餘怪物數量
			int remainingMonsters = world.getAliveNpcCount();
			int killCount = player.getVariables().getInt(VAR_KILL_COUNT, 0);
			player.sendPacket(new ExShowScreenMessage("剩餘 " + remainingMonsters + " 隻怪物，已擊殺 " + killCount + " 隻", 3000));

			return false;
		}
		// 超時失敗
		else
		{
			player.getVariables().set(VAR_ENTER_TIME, 0);
			player.sendPacket(new ExShowScreenMessage("請在" + CLEAR_TIME_LIMIT + "秒內清理所有怪物,否則無法獲取獎勵", 3000));
			world.removeNpcs();
			world.removePlayer(player);
			return true;
		}
	}
	
	/**
	 * 刷新怪物
	 */
	private void spawnMonsters(Player player, Instance world)
	{
		for (Location location : SPAWN_LOCATIONS)
		{
			spawnMonster(player, world, location);
		}
	}
	
	/**
	 * 在指定位置刷新單個怪物
	 */
	private Npc spawnMonster(Player player, Instance world, Location location)
	{
		try
		{
			Spawn spawn = new Spawn(GOLBERG);
			spawn.setXYZ(location.getX(), location.getY(), location.getZ());
			spawn.setInstanceId(world.getId());
			
			Npc npc = spawn.doSpawn();
			if (npc != null)
			{
				npc.setCurrentHp(npc.getMaxHp());
				npc.setAutoAttackable(true);
			}
			
			return npc;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if ((npc.getId() == GOLBERG) && (killer != null))
		{
			// 增加擊殺計數
			int currentKills = killer.getVariables().getInt(VAR_KILL_COUNT, 0);
			killer.getVariables().set(VAR_KILL_COUNT, currentKills + 1);

			// 更新任務數據
			PlayerTaskData taskData = playerTasks.get(killer.getObjectId());
			if (taskData != null)
			{
				taskData.killCount = currentKills + 1;
			}
		}
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/instances/danrentongguan_exp/danrentongguan.htm");
		player.sendPacket(html);
		return "";
	}

	/**
	 * 顯示排行榜
	 */
	private String showLeaderboard(Player player)
	{
		List<LeaderboardEntry> leaderboard = loadLeaderboard();

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		StringBuilder sb = new StringBuilder();

		sb.append("<html><title>擊殺排行榜</title><head><body scroll=\"no\">");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=292 height=358>");
		sb.append("<tr><td valign=\"top\" align=\"center\">");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=\"272\">");
		sb.append("<tr><td align=\"center\" height=\"40\"><font color=\"LEVEL\">擊殺排行榜 TOP " + LEADERBOARD_SIZE + "</font></td></tr>");
		sb.append("<tr><td height=\"10\"></td></tr>");

		if (leaderboard.isEmpty())
		{
			sb.append("<tr><td align=\"center\" height=\"30\"><font color=\"FF0055\">目前暫無排行記錄</font></td></tr>");
		}
		else
		{
			sb.append("<tr><td><table border=0 cellpadding=0 cellspacing=0 width=\"272\">");
			sb.append("<tr>");
			sb.append("<td width=\"50\" align=\"center\"><font color=\"LEVEL\">名次</font></td>");
			sb.append("<td width=\"140\" align=\"center\"><font color=\"LEVEL\">玩家名稱</font></td>");
			sb.append("<td width=\"82\" align=\"center\"><font color=\"LEVEL\">擊殺數</font></td>");
			sb.append("</tr>");

			for (int i = 0; i < leaderboard.size(); i++)
			{
				LeaderboardEntry entry = leaderboard.get(i);
				String rankColor = "FFFFFF";

				if (i == 0)
				{
					rankColor = "FFD700"; // 金色
				}
				else if (i == 1)
				{
					rankColor = "C0C0C0"; // 銀色
				}
				else if (i == 2)
				{
					rankColor = "CD7F32"; // 銅色
				}

				sb.append("<tr>");
				sb.append("<td align=\"center\"><font color=\"" + rankColor + "\">" + (i + 1) + "</font></td>");
				sb.append("<td align=\"center\"><font color=\"" + rankColor + "\">" + entry.getPlayerName() + "</font></td>");
				sb.append("<td align=\"center\"><font color=\"" + rankColor + "\">" + entry.getKillCount() + "</font></td>");
				sb.append("</tr>");
			}

			sb.append("</table></td></tr>");
		}

		sb.append("<tr><td height=\"10\"></td></tr>");
		sb.append("<tr><td align=center>");
		sb.append("<button value=\"返回\" action=\"bypass -h npc_%objectId%_Quest danrentongguan_exp\" width=100 height=30 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</td></tr>");
		sb.append("</table>");
		sb.append("</td></tr></table>");
		sb.append("</body></html>");

		html.setHtml(sb.toString());
		player.sendPacket(html);
		return "";
	}

	/**
	 * 保存排行榜記錄（先更新內存，異步寫入數據庫）
	 */
	private void saveLeaderboardRecord(Player player, int killCount)
	{
		if (killCount <= 0)
		{
			return;
		}

		String playerName = player.getName();
		long currentTime = System.currentTimeMillis();

		// 確保緩存已載入
		if (!cacheLoaded)
		{
			loadLeaderboardToCache();
		}

		// 檢查是否需要更新記錄
		LeaderboardEntry existingEntry = leaderboardCache.get(playerName);

		if (existingEntry != null)
		{
			int existingKills = existingEntry.getKillCount();
			// 只有當新記錄更好時才更新
			if (killCount > existingKills)
			{
				// 更新內存緩存
				leaderboardCache.put(playerName, new LeaderboardEntry(playerName, killCount, currentTime));

				// 異步寫入數據庫
				asyncUpdateLeaderboard(playerName, killCount, currentTime);

				player.sendPacket(new ExShowScreenMessage("恭喜！你刷新了個人最佳記錄：" + killCount + " 隻（舊記錄：" + existingKills + " 隻）", 5000));
			}
		}
		else
		{
			// 新記錄，添加到內存緩存
			leaderboardCache.put(playerName, new LeaderboardEntry(playerName, killCount, currentTime));

			// 異步寫入數據庫
			asyncUpdateLeaderboard(playerName, killCount, currentTime);

			player.sendPacket(new ExShowScreenMessage("你的記錄已登上排行榜：" + killCount + " 隻怪物！", 5000));
		}
	}

	/**
	 * 異步更新數據庫排行榜（避免阻塞主線程）
	 */
	private void asyncUpdateLeaderboard(String playerName, int killCount, long recordTime)
	{
		ThreadPool.execute(() ->
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				// 創建表（如果不存在）
				createTableIfNotExists(con);

				// 使用 REPLACE INTO 或 INSERT ... ON DUPLICATE KEY UPDATE
				String upsertQuery = "INSERT INTO danren_leaderboard (player_name, kill_count, record_time) " + "VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE kill_count = VALUES(kill_count), record_time = VALUES(record_time)";

				try (PreparedStatement ps = con.prepareStatement(upsertQuery))
				{
					ps.setString(1, playerName);
					ps.setInt(2, killCount);
					ps.setLong(3, recordTime);
					ps.executeUpdate();
				}
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		});
	}

	/**
	 * 載入排行榜（優先從內存緩存讀取）
	 */
	private List<LeaderboardEntry> loadLeaderboard()
	{
		// 如果緩存未載入，先從數據庫載入
		if (!cacheLoaded)
		{
			loadLeaderboardToCache();
		}

		// 從緩存中獲取並排序
		List<LeaderboardEntry> leaderboard = new ArrayList<>(leaderboardCache.values());
		leaderboard.sort((a, b) -> Integer.compare(b.getKillCount(), a.getKillCount()));

		// 返回前N名
		return leaderboard.subList(0, Math.min(LEADERBOARD_SIZE, leaderboard.size()));
	}

	/**
	 * 從數據庫載入排行榜到內存緩存
	 */
	private void loadLeaderboardToCache()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			// 創建表（如果不存在）
			createTableIfNotExists(con);

			String query = "SELECT player_name, kill_count, record_time FROM danren_leaderboard";
			try (PreparedStatement ps = con.prepareStatement(query))
			{
				try (ResultSet rs = ps.executeQuery())
				{
					leaderboardCache.clear();
					while (rs.next())
					{
						String playerName = rs.getString("player_name");
						int killCount = rs.getInt("kill_count");
						long recordTime = rs.getLong("record_time");
						leaderboardCache.put(playerName, new LeaderboardEntry(playerName, killCount, recordTime));
					}
					cacheLoaded = true;
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * 創建排行榜表（如果不存在）
	 */
	private void createTableIfNotExists(Connection con) throws SQLException
	{
		String createTableQuery = "CREATE TABLE IF NOT EXISTS danren_leaderboard (" + "player_name VARCHAR(35) PRIMARY KEY," + "kill_count INT NOT NULL DEFAULT 0," + "record_time BIGINT NOT NULL DEFAULT 0" + ")";

		try (PreparedStatement ps = con.prepareStatement(createTableQuery))
		{
			ps.execute();
		}
	}

	
	public static void main(String[] args)
	{
		new danrentongguan_exp();
		System.out.println("單人猛襲副本準備完畢！");
	}
}
