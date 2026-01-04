// RunMerchant.java
package custom.RunMerchant;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import custom.RunMerchant.RunMerchantDAO;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * RunMerchant - 跑商系統（貨物購買版）
 */
public class RunMerchant extends Script
{
	public static final Logger LOGGER = Logger.getLogger(RunMerchant.class.getName());

	private static RunMerchant instance = null;
	private static ScheduledFuture<?> staticRefreshTask = null;
	private static final Object TASK_LOCK = new Object();

	// ==================== 城市配置類 ====================
	public static class City
	{
		private final int id;
		private final int npcId;
		private final String name;
		private final double multMin;
		private final double multMax;

		public City(int id, int npcId, String name, double multMin, double multMax)
		{
			this.id = id;
			this.npcId = npcId;
			this.name = name;
			this.multMin = multMin;
			this.multMax = multMax;
		}

		public int getId()
		{
			return id;
		}

		public int getNpcId()
		{
			return npcId;
		}

		public String getName()
		{
			return name;
		}

		public double getMultMin()
		{
			return multMin;
		}

		public double getMultMax()
		{
			return multMax;
		}

		public double generateRandomMultiplier()
		{
			return roundTwoDecimals(multMin + (Rnd.nextDouble() * (multMax - multMin)));
		}
	}

	// ==================== 貨物配置類 ====================
	public static class CargoType
	{
		private final int itemId;           // 貨物物品ID
		private final long pricePerUnit;    // 單價
		private final int pointsPerUnit;    // 單位積分

		public CargoType(int itemId, long pricePerUnit, int pointsPerUnit)
		{
			this.itemId = itemId;
			this.pricePerUnit = pricePerUnit;
			this.pointsPerUnit = pointsPerUnit;
		}

		public int getItemId()
		{
			return itemId;
		}

		public long getPricePerUnit()
		{
			return pricePerUnit;
		}

		public int getPointsPerUnit()
		{
			return pointsPerUnit;
		}
	}

	// ==================== 配置區 ====================

	// HTML 文件路徑
	private static final String HTML_PATH = "data/scripts/custom/RunMerchant/";

	// 起始NPC - 玩家在這裡接取跑商任務
	private static final int START_NPC_ID = 900005;

	private static final double DISTANCE_UNIT = 5000.0;
	private static final double DISTANCE_BONUS_RATE = 0.03;
	// Adena ID
	private static final int ADENA_ID = 57;
	// 跑商奖励道具ID（新增）
	private static final int REWARD_ITEM_ID = 97145;

	private static final CargoType[] CARGO_TYPES = {
			new CargoType(106001, 1000000, 10),
			new CargoType(106002, 1700000, 20),
			new CargoType(106003, 2100000, 30),
			new CargoType(106004, 3000000, 50),
			new CargoType(106005, 5000000, 100),
			new CargoType(106006, 8000000, 200),
			new CargoType(106007, 15000000, 500),
			new CargoType(106008, 27000000, 1500),
			new CargoType(106009, 18000000, 1500)
	};

	// 目的地城市配置
	private static final City[] DESTINATION_CITIES =
			{
					new City(0, 910001, "奇岩城接頭人(哥肯的花園附近)", 0.1, 1.0),
					new City(1, 910002, "龍之谷接頭人(龍之谷入口處)", 0.2, 1.2),
					new City(2, 910003, "歐瑞城鎮接頭人(龍之谷上方釣點處)", 0.4, 1.5),
					new City(3, 910004, "歐瑞城鎮接頭人(蜥蜴草原T字路處)", 0.8, 2.0),
					new City(4, 910005, "薄霧山脈接頭人(克塔右側迷宮深處)", 0.7, 1.9),
					new City(5, 910006, "狄恩城鎮接頭人(狄恩城鎮中心處)", 0.5, 1.5)
			};

	// 任務物品配置（用於標記玩家攜帶貨物）
	private static final int MERCH_ITEM_ID = 106000;

	// 每日跑商次數限制
	private static final int DAILY_MAX_RUNS = 10;

	// 稀有獎勵物品（保留）
	private static final int[][] RARE_REWARD_ITEMS =
			{
					{ 150030, 100 },
			};

	private static final double RARE_ITEM_DROP_CHANCE = 0.00;

	// 跑商要求
	private static final int MIN_LEVEL = 1;
	private static final int MIN_SOULRING = 0;

	// 刷新間隔
	private static final long REFRESH_INTERVAL_MS = 60L * 60L * 1000L; // 1小時

	// PlayerVariables 前綴
	private static final String PV_PREFIX = "RunMerchant_";

	// ==================== 運行時數據 ====================
	private final Map<Integer, Double> cityMultipliers = new ConcurrentHashMap<>();
	private final Map<Integer, City> npcToCityMap = new HashMap<>();

	// ==================== 初始化 ====================
	public RunMerchant()
	{
		// ===== 修改單例檢查 =====
		synchronized (TASK_LOCK)
		{
			if (instance != null)
			{
				LOGGER.warning("[RunMerchant] 檢測到重複加載，清理舊實例...");
				// 注意：不再取消舊任務，因為我們要保持靜態任務運行
			}
			instance = this;
			LOGGER.info("[RunMerchant] 當前實例已設為主實例");
		}
		// ===== 修改結束 =====

		// 註冊起始NPC
		addStartNpc(START_NPC_ID);
		addTalkId(START_NPC_ID);
		addFirstTalkId(START_NPC_ID);

		// 註冊所有目的地城市NPC
		for (City city : DESTINATION_CITIES)
		{
			addStartNpc(city.getNpcId());
			addTalkId(city.getNpcId());
			addFirstTalkId(city.getNpcId());
			npcToCityMap.put(city.getNpcId(), city);
		}

		// 從數據庫加載或生成初始倍率
		loadOrGenerateAllMultipliers();

		// 啟動定時刷新任務
		startRefreshTask();

		LOGGER.info("[RunMerchant] 跑商系統已啟動");
	}

	// ==================== 倍率管理 ====================

	private void loadOrGenerateAllMultipliers()
	{
		for (City city : DESTINATION_CITIES)
		{
			double stored = RunMerchantDAO.loadMultiplier(city.getId());

			if ((stored <= 0.0) || (stored < city.getMultMin()) || (stored > city.getMultMax()))
			{
				double newMult = city.generateRandomMultiplier();
				cityMultipliers.put(city.getId(), newMult);
				RunMerchantDAO.saveMultiplier(city.getId(), newMult);
			}
			else
			{
				cityMultipliers.put(city.getId(), roundTwoDecimals(stored));
			}
		}
		LOGGER.info("[RunMerchant] 已加載城市倍率");
	}

	private void randomizeAllMultipliersAndSave()
	{
		Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.WORLD, "跑商系統", "【跑商公告】各城市倍率已更新！"));

		for (City city : DESTINATION_CITIES)
		{
			double newMult = city.generateRandomMultiplier();
			cityMultipliers.put(city.getId(), newMult);
			RunMerchantDAO.saveMultiplier(city.getId(), newMult);

			String cityAnnouncement = city.getName() + ": " + String.format("%.2f", newMult) + "倍";
			Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.WORLD, "跑商系統", cityAnnouncement));

			LOGGER.info("[RunMerchant] 倍率已刷新: " + city.getName() + " = " + newMult);
		}
	}

	private void startRefreshTask()
	{
		synchronized (TASK_LOCK)
		{
			// 如果已經有運行中的任務，不要創建新任務
			if (staticRefreshTask != null && !staticRefreshTask.isDone() && !staticRefreshTask.isCancelled())
			{
				LOGGER.info("[RunMerchant] 定時任務已在運行中，跳過創建");
				return;
			}

			// 取消舊任務（如果存在）
			if (staticRefreshTask != null && !staticRefreshTask.isCancelled())
			{
				LOGGER.info("[RunMerchant] 取消舊的刷新任務");
				staticRefreshTask.cancel(true);
				staticRefreshTask = null;
			}

			LOGGER.info("[RunMerchant] 設定刷新間隔: " + REFRESH_INTERVAL_MS + " 毫秒 = " + (REFRESH_INTERVAL_MS / 1000 / 60) + " 分鐘");

			staticRefreshTask = ThreadPool.scheduleAtFixedRate(
					this::randomizeAllMultipliersAndSave,
					REFRESH_INTERVAL_MS,  // 首次執行延遲 60 分鐘
					REFRESH_INTERVAL_MS   // 之後每 60 分鐘執行一次
			);

			LOGGER.info("[RunMerchant] 新的刷新任務已啟動");
		}
	}

	private static double roundTwoDecimals(double v)
	{
		return Math.round(v * 100.0) / 100.0;
	}

	private static class DistanceResult
	{
		public final double distance;
		public final double factor;

		public DistanceResult(double distance, double factor)
		{
			this.distance = distance;
			this.factor = factor;
		}
	}

	private DistanceResult calculateDistance(int startX, int startY, int startZ, int endX, int endY, int endZ)
	{
		double dx = startX - endX;
		double dy = startY - endY;
		double dz = startZ - endZ;
		double distance = Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
		double bonus = (distance / DISTANCE_UNIT) * DISTANCE_BONUS_RATE;
		double factor = 1.0 + bonus;
		return new DistanceResult(distance, factor);
	}

	// ==================== 事件處理 ====================

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (player == null)
		{
			return null;
		}

		if (event.startsWith("buyCargo "))
		{
			String[] parts = event.split(" ");
			if (parts.length >= 3)
			{
				try
				{
					int cargoIndex = Integer.parseInt(parts[1]);  // 改為 cargoIndex
					int quantity = Integer.parseInt(parts[2]);
					return handleBuyCargo(npc, player, cargoIndex, quantity);
				}
				catch (NumberFormatException e)
				{
					player.sendMessage("輸入格式錯誤！");
					return null;
				}
			}
		}

		switch (event)
		{
			case "acceptTask":
			{
				return handleAcceptTask(npc, player);
			}
			case "giveUp":
			{
				if (!hasActiveTask(player))
				{
					player.sendMessage("您當前沒有進行中的跑商任務！");
					return null;
				}

				clearTask(player, true);
				player.sendMessage("任務已放棄！");
				return null;
			}
			case "showMultipliers":
			{
				return showMultipliersHtml(player);
			}
			case "showCargoList":
			{
				return showCargoListHtml(player);
			}
		}

		return null;
	}

	private String handleAcceptTask(Npc npc, Player player)
	{
		// 檢查是否已有任務
		if (hasActiveTask(player))
		{
			player.sendMessage("您已有未完成的跑商任務，請先完成或放棄。");
			return null;
		}

		// 檢查屬性要求
		boolean CheckOk = (player.getLevel() >= MIN_LEVEL) || (player.getSoulringCount() >= MIN_SOULRING);

		if (!CheckOk)
		{
			player.sendMessage("條件不足！需求：等級 ≥ " + formatNumber(MIN_LEVEL) + "，魂環 ≥ " + formatNumber(MIN_SOULRING));
			return null;
		}

		// 檢查每日次數
		PlayerVariables pv = player.getVariables();
		int dailyRuns = pv.getInt(PV_PREFIX + "daily_runs", 0);

		if (dailyRuns >= DAILY_MAX_RUNS)
		{
			player.sendMessage("今日跑商次數已達上限（" + DAILY_MAX_RUNS + "次），請明日再來。");
			return null;
		}

		// 初始化任務並記錄起始坐標
		pv.set(PV_PREFIX + "active", true);
		pv.set(PV_PREFIX + "start_time", System.currentTimeMillis());
		pv.set(PV_PREFIX + "start_x", npc.getX());
		pv.set(PV_PREFIX + "start_y", npc.getY());
		pv.set(PV_PREFIX + "start_z", npc.getZ());
		pv.set(PV_PREFIX + "cargo_points", 0);  // 初始化商會點數為0

		// 添加死亡和登出监听器
		addDeathListener(player);
		addLogoutListener(player);

		// 任務接取成功提示
		player.sendMessage("========================================");
		player.sendMessage("跑商任務已接取！");
		player.sendMessage("請記得購買你要運輸的貨物，否則會白跑一趟！");
		player.sendMessage("您可以再次與我對話查看貨物列表進行購買。");
		player.sendMessage("========================================");

		return buildStartNpcHtml(player);
	}

	private String handleBuyCargo(Npc npc, Player player, int cargoIndex, int quantity)
	{
		// 檢查是否有活動任務
		if (!hasActiveTask(player))
		{
			player.sendMessage("請先接取跑商任務！");
			return null;
		}

		// 檢查數量是否合法
		if (quantity <= 0)
		{
			player.sendMessage("購買數量必須大於0！");
			return null;
		}

		// 驗證索引並查找貨物
		if (cargoIndex < 0 || cargoIndex >= CARGO_TYPES.length)
		{
			player.sendMessage("無效的貨物類型！");
			return null;
		}

		CargoType cargo = CARGO_TYPES[cargoIndex];

		// 獲取物品名稱
		ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(cargo.getItemId());
		String itemName = itemTemplate != null ? itemTemplate.getName() : "未知貨物";

		// 計算總價
		long totalPrice = cargo.getPricePerUnit() * quantity;

		// 檢查玩家是否有足夠的adena
		long playerAdena = player.getInventory().getAdena();
		if (playerAdena < totalPrice)
		{
			player.sendMessage("金幣不足！需要 " + formatNumber(totalPrice) + " 金幣，您只有 " + formatNumber(playerAdena) + " 金幣。");
			return null;
		}

		// 扣除adena
		player.reduceAdena(null, totalPrice, npc, true);

		// 發放實際的貨物物品
		player.addItem(null, cargo.getItemId(), quantity, npc, true);


		// 發放任務物品標記（如果還沒有）
		if (player.getInventory().getItemByItemId(MERCH_ITEM_ID) == null)
		{
			player.addItem(null, MERCH_ITEM_ID, 1, npc, true);
		}

		// 发送成功消息
		player.sendMessage("========================================");
		player.sendMessage("購買成功！");
		player.sendMessage("貨物：" + itemName + " x" + quantity);
		player.sendMessage("花費：" + formatNumber(totalPrice) + " adena");
		player.sendMessage("========================================");
		player.sendMessage("請前往城市交付貨物以獲得商會點數！");
		player.sendMessage("========================================");

		return null;
	}

	// ==================== NPC對話處理 ====================

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if ((player == null) || (npc == null))
		{
			return null;
		}

		// 起始NPC - 接取任務
		if (npc.getId() == START_NPC_ID)
		{
			return buildStartNpcHtml(player);
		}

		// 目的地城市NPC - 交付任務
		City city = npcToCityMap.get(npc.getId());
		if (city != null)
		{
			return handleDestinationNpc(npc, player, city);
		}

		return null;
	}

	private String buildStartNpcHtml(Player player)
	{
		PlayerVariables pv = player.getVariables();
		int dailyRuns = pv.getInt(PV_PREFIX + "daily_runs", 0);
		boolean hasTask = hasActiveTask(player);
		int cargoPoints = pv.getInt(PV_PREFIX + "cargo_points", 0);
		long totalPoints = pv.getLong(PV_PREFIX + "total_guild_points", 0L);

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, HTML_PATH + "start.html");
		html.replace("%daily_runs%", String.valueOf(dailyRuns));
		html.replace("%max_runs%", String.valueOf(DAILY_MAX_RUNS));
		html.replace("%has_task%", hasTask ? "是" : "否");
		html.replace("%total_points%", formatNumber(totalPoints));
		player.sendPacket(html);

		return null;
	}

	private String handleDestinationNpc(Npc npc, Player player, City city)
	{
		// 檢查是否有活動任務
		if (!hasActiveTask(player))
		{
			return buildCityInfoHtml(player, city);
		}

		// 檢查是否有貨物標記
		if (player.getInventory().getItemByItemId(MERCH_ITEM_ID) == null)
		{
			player.sendMessage("您身上沒有證明道具，無法完成任務。");
			return null;
		}

		// 檢查是否購買了貨物（檢查背包中是否有貨物）
		boolean hasCargo = false;
		for (CargoType cargo : CARGO_TYPES)
		{
			if (player.getInventory().getInventoryItemCount(cargo.getItemId(), -1) > 0)
			{
				hasCargo = true;
				break;
			}
		}

		if (!hasCargo)
		{
			player.sendMessage("您還沒有購買任何貨物！請先返回跑商中心購買貨物。");
			return null;
		}

		// 可以在任何城市完成任務
		return completeTask(npc, player, city);
	}

	private String completeTask(Npc npc, Player player, City city)
	{
		PlayerVariables pv = player.getVariables();

		// ===== 改为检查背包中的货物 =====
		int basePoints = 0;

		// 遍历所有货物类型，检查玩家背包
		for (CargoType cargo : CARGO_TYPES)
		{
			long itemCount = player.getInventory().getInventoryItemCount(cargo.getItemId(), -1);

			if (itemCount > 0)
			{
				// 计算基础点数
				basePoints += cargo.getPointsPerUnit() * itemCount;

				// 销毁货物
				player.destroyItemByItemId(null, cargo.getItemId(), itemCount, npc, true);
			}
		}

		// 检查是否有货物
		if (basePoints <= 0)
		{
			player.sendMessage("您沒有任何貨物可交付！");
			clearTask(player, true);
			return null;
		}
		// ===== 修改结束 =====

		// 移除任务标记
		player.destroyItemByItemId(null, MERCH_ITEM_ID, 1, npc, true);

		// 计算奖励倍率
		double cityMult = cityMultipliers.getOrDefault(city.getId(), 0.1);

		// 获取起始坐标并计算距离
		int startX = pv.getInt(PV_PREFIX + "start_x", 0);
		int startY = pv.getInt(PV_PREFIX + "start_y", 0);
		int startZ = pv.getInt(PV_PREFIX + "start_z", 0);

		if (startX == 0 && startY == 0 && startZ == 0)
		{
			player.sendMessage("任務數據異常，請重新接取任務。");
			clearTask(player, true);
			return null;
		}

		int endX = npc.getX();
		int endY = npc.getY();
		int endZ = npc.getZ();

		DistanceResult result = calculateDistance(startX, startY, startZ, endX, endY, endZ);
		double actualDistance = result.distance;
		double distFactor = result.factor;
		double totalMult = cityMult * distFactor;

		// 计算最终商会点数
		long finalPoints = (long) (basePoints * totalMult);

		// 累加到玩家的总商会点数
		long totalGuildPoints = pv.getLong(PV_PREFIX + "total_guild_points", 0L);
		totalGuildPoints += finalPoints;
		pv.set(PV_PREFIX + "total_guild_points", totalGuildPoints);

		// 稀有奖励判定
		boolean gotRare = false;
		if (Rnd.nextDouble() < RARE_ITEM_DROP_CHANCE)
		{
			int[] rareReward = RARE_REWARD_ITEMS[Rnd.get(RARE_REWARD_ITEMS.length)];
			player.addItem(null, rareReward[0], (long)(rareReward[1] * totalMult), npc, true);
			gotRare = true;
		}

		// 更新完成次数
		int dailyRuns = pv.getInt(PV_PREFIX + "daily_runs", 0);
		pv.set(PV_PREFIX + "daily_runs", dailyRuns + 1);

		// 清除任务
		clearTask(player, false);

		// 发送完成消息
		player.sendMessage("========================================");
		player.sendMessage("任務完成！交付城市：" + city.getName());
		player.sendMessage("運送距離：" + String.format("%.0f", actualDistance) + " 單位");
		player.sendMessage("城市倍率：" + String.format("%.2f", cityMult) + "，距離加成：" + String.format("%.2f", distFactor) + "，總倍率：" + String.format("%.2f", totalMult));
		player.sendMessage("基礎商會點數：" + formatNumber(basePoints) + " 點");
		player.sendMessage("最終獲得商會點數：" + formatNumber(finalPoints) + " 點");
		player.sendMessage("累計商會點數：" + formatNumber(totalGuildPoints) + " 點");
		player.sendMessage("今日完成：" + (dailyRuns + 1) + "/" + DAILY_MAX_RUNS);
		if (gotRare)
		{
			player.sendMessage("★ 恭喜獲得稀有獎勵！");
		}
		player.sendMessage("========================================");

		return null;
	}

	private String buildCityInfoHtml(Player player, City city)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, HTML_PATH + "city_destination.html");
		html.replace("%city_name%", city.getName());
		html.replace("%city_mult%", String.format("%.2f", cityMultipliers.getOrDefault(city.getId(), 0.1)));
		html.replace("%mult_min%", String.format("%.2f", city.getMultMin()));
		html.replace("%mult_max%", String.format("%.2f", city.getMultMax()));
		player.sendPacket(html);
		return null;
	}

	private String showMultipliersHtml(Player player)
	{
		StringBuilder sb = new StringBuilder();

		for (City city : DESTINATION_CITIES)
		{
			double mult = cityMultipliers.getOrDefault(city.getId(), 0.1);
			String color = mult >= 1.5 ? "FF00FF" : (mult >= 1.0 ? "FFFF00" : "00FF00");
			sb.append("<tr>");
			sb.append("<td width=\"150\" align=\"left\">").append(city.getName()).append("</td>");
			sb.append("<td width=\"130\" align=\"right\"><font color=\"").append(color).append("\">").append(String.format("%.2f", mult)).append("</font></td>");
			sb.append("</tr>");
		}

		// 構建簡單的倍率顯示HTML
		StringBuilder html = new StringBuilder();
		html.append("<html><body><center>");
		html.append("<br><font color=\"LEVEL\"><b>各城市當前倍率</b></font><br>");
		html.append("<img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\"><br>");
		html.append("<table width=\"280\">");
		html.append(sb.toString());
		html.append("</table>");
		html.append("<br><img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\"><br>");
		html.append("<font color=\"808080\">倍率每小時自動刷新</font>");
		html.append("</center></body></html>");

		NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0, 1);
		htmlMsg.setHtml(html.toString());
		player.sendPacket(htmlMsg);

		return null;
	}

	private String showCargoListHtml(Player player)
	{
		if (!hasActiveTask(player))
		{
			player.sendMessage("請先接取跑商任務！");
			return null;
		}

		PlayerVariables pv = player.getVariables();
		long playerAdena = player.getInventory().getAdena();

		StringBuilder html = new StringBuilder();
		html.append("<html><head><title>貨物購買</title></head>");
		html.append("<body scroll=\"no\">");
		html.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"292\" height=\"358\">");
		html.append("<tr><td valign=\"top\" align=\"center\">");

		html.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
		html.append("<tr><td height=\"20\"></td></tr>");
		html.append("<tr><td align=\"center\"><font color=\"LEVEL\" size=\"6\"><b>貨物購買</b></font></td></tr>");
		html.append("<tr><td height=\"15\"></td></tr>");

		// 玩家信息
		html.append("<tr><td align=\"center\"><font color=\"00FF66\">持有金幣：</font><font color=\"FFFF00\">").append(formatNumber(playerAdena)).append("</font></td></tr>");
		html.append("<tr><td height=\"10\"></td></tr>");
		html.append("<tr><td align=\"center\"><img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"></td></tr>");
		html.append("<tr><td height=\"10\"></td></tr>");


		// 貨物列表
		for (int i = 0; i < CARGO_TYPES.length; i++)
		{
			CargoType cargo = CARGO_TYPES[i];

			// 獲取物品名稱
			ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(cargo.getItemId());
			String itemName = itemTemplate != null ? itemTemplate.getName() : "未知貨物";

			// 第一個表格：物品名稱和價格/點數
			html.append("<tr><td align=\"center\">");
			html.append("<table width=\"292\" border=\"0\" cellpadding=\"2\" cellspacing=\"0\">");
			html.append("<tr>");
			html.append("<td width=\"122\" ><font color=\"LEVEL\">").append(itemName + "(重" + itemTemplate.getWeight()+")").append("</font></td>");
			html.append("<td width=\"170\" align=\"right\">");
			html.append("<font color=\"808080\">").append(formatNumber(cargo.getPricePerUnit())).append(" 金幣</font>");
			html.append(" / ");
			html.append("<font color=\"00FFFF\">").append(cargo.getPointsPerUnit()).append("點</font>");
			html.append("</td>");
			html.append("</tr>");
			html.append("<tr><td height=\"10\"></td></tr>");
			html.append("</table>");
			html.append("</td></tr>");

			// 第二個表格：數量、輸入框、購買按鈕
			html.append("<tr><td align=\"center\">");
			html.append("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\">");
			html.append("<tr>");
			html.append("<td><font color=\"LEVEL\">數量：</font></td>");
			html.append("<td><edit var=\"qty_").append(i).append("\" width=\"60\" height=\"15\"></td>");
			html.append("<td width=\"5\"></td>");
			html.append("<td><button action=\"bypass -h Quest RunMerchant buyCargo ").append(i).append(" $qty_").append(i).append("\" value=\"購買\" width=\"80\" height=\"21\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td>");
			html.append("</tr>");
			html.append("<tr><td height=\"20\"></td></tr>");
			html.append("</table>");
			html.append("</td></tr>");

		}

		html.append("<tr><td height=\"10\"></td></tr>");
		html.append("<tr><td align=\"center\"><img src=\"L2UI.SquareGray\" width=\"270\" height=\"1\"></td></tr>");
		html.append("<tr><td height=\"10\"></td></tr>");

		// 說明
		html.append("<tr><td align=\"center\"><font color=\"FFD700\">購買貨物後前往各城交付</font></td></tr>");
		html.append("<tr><td align=\"center\"><font color=\"808080\">可以購買多種貨物</font></td></tr>");
		html.append("<tr><td align=\"center\"><font color=\"808080\">距離與倍率影響最終點數</font></td></tr>");

		html.append("</table>");
		html.append("</td></tr></table>");
		html.append("</body></html>");

		NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0, 1);
		htmlMsg.setHtml(html.toString());
		player.sendPacket(htmlMsg);

		return null;
	}

	// ==================== 輔助方法 ====================

	private City getCityById(int cityId)
	{
		for (City city : DESTINATION_CITIES)
		{
			if (city.getId() == cityId)
			{
				return city;
			}
		}
		return null;
	}

	private boolean hasActiveTask(Player player)
	{
		return player.getVariables().getBoolean(PV_PREFIX + "active", false);
	}

	private void clearTask(Player player, boolean removeItems)
	{
		PlayerVariables pv = player.getVariables();

		if (removeItems)
		{
			// 計算背包中的貨物並退款
			long totalValue = 0;

			// 遍歷所有貨物類型，檢查玩家背包
			for (CargoType cargo : CARGO_TYPES)
			{
				// 獲取玩家背包中該貨物的數量
				long itemCount = player.getInventory().getInventoryItemCount(cargo.getItemId(), -1);

				if (itemCount > 0)
				{
					// 計算價值
					totalValue += cargo.getPricePerUnit() * itemCount;

					// 銷毀貨物
					player.destroyItemByItemId(null, cargo.getItemId(), itemCount, player, true);
				}
			}

			// 退還50%
			if (totalValue > 0)
			{
				long refund = totalValue / 2;
				player.addAdena(null, refund, player, true);
				player.sendMessage("已回收貨物並退還 " + formatNumber(refund) + " 金幣（50%）");
			}

			// 移除任務標記物品
			if (player.getInventory().getItemByItemId(MERCH_ITEM_ID) != null)
			{
				player.destroyItemByItemId(null, MERCH_ITEM_ID, 1, player, true);
			}
		}

		pv.remove(PV_PREFIX + "active");
		pv.remove(PV_PREFIX + "start_time");
		pv.remove(PV_PREFIX + "start_x");
		pv.remove(PV_PREFIX + "start_y");
		pv.remove(PV_PREFIX + "start_z");

		// 🔧 新增這一行 - 清理監聽器（關鍵！）
		removeListeners(player);
	}

	private String formatNumber(long number)
	{
		return String.format("%,d", number);
	}

	private String formatNumber(int number)
	{
		return String.format("%,d", number);
	}

	/**
	 * 为玩家添加死亡监听器
	 */
	private void addDeathListener(Player player)
	{
		player.addListener(new ConsumerEventListener(player, EventType.ON_CREATURE_DEATH, (OnCreatureDeath event) -> onPlayerDeath(event), this));
	}

	/**
	 * 为玩家添加登出监听器
	 */
	private void addLogoutListener(Player player)
	{
		player.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout event) -> onPlayerLogout(event), this));
	}

	/**
	 * 移除玩家的所有监听器
	 */
	private void removeListeners(Player player)
	{
		// 移除死亡监听器
		for (AbstractEventListener listener : player.getListeners(EventType.ON_CREATURE_DEATH))
		{
			if (listener.getOwner() == this)
			{
				listener.unregisterMe();
			}
		}

		// 移除登出监听器
		for (AbstractEventListener listener : player.getListeners(EventType.ON_PLAYER_LOGOUT))
		{
			if (listener.getOwner() == this)
			{
				listener.unregisterMe();
			}
		}
	}

	/**
	 * 玩家死亡事件处理
	 */
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	public void onPlayerDeath(OnCreatureDeath event)
	{
		if (!event.getTarget().isPlayer())
		{
			return;
		}

		final Player victim = event.getTarget().asPlayer();

		// 检查是否有活动任务
		if (!hasActiveTask(victim))
		{
			return;
		}

		// 检查是否被玩家杀死
		final Creature attacker = event.getAttacker();
		if (attacker == null || !attacker.isPlayer())
		{
			return;
		}

		final Player killer = attacker.asPlayer();
		if (killer == victim)
		{
			return; // 排除自杀
		}

		// 清理跑商任务
		handlePlayerDeathCleanup(victim);
	}

	/**
	 * 玩家登出事件处理
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	private void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();

		if (hasActiveTask(player))
		{
			// 玩家登出时自动放弃任务
			clearTask(player, true);
			LOGGER.info("[RunMerchant] 玩家 " + player.getName() + " 登出，任務已自動放棄");
		}
	}

	/**
	 * 处理玩家死亡时的跑商任务清理
	 */
	private void handlePlayerDeathCleanup(Player player)
	{
		if (player == null)
		{
			return;
		}

		PlayerVariables pv = player.getVariables();

		// 删除所有货物
		for (CargoType cargo : CARGO_TYPES)
		{
			long itemCount = player.getInventory().getInventoryItemCount(cargo.getItemId(), -1);
			if (itemCount > 0)
			{
				player.destroyItemByItemId(ItemProcessType.NONE, cargo.getItemId(), itemCount, null, false);
			}
		}

		// 删除任务标记物品
		if (player.getInventory().getItemByItemId(MERCH_ITEM_ID) != null)
		{
			player.destroyItemByItemId(ItemProcessType.NONE, MERCH_ITEM_ID, 1, null, false);
		}

		// 清除所有任务相关变量
		pv.remove(PV_PREFIX + "active");
		pv.remove(PV_PREFIX + "start_time");
		pv.remove(PV_PREFIX + "start_x");
		pv.remove(PV_PREFIX + "start_y");
		pv.remove(PV_PREFIX + "start_z");
		pv.remove(PV_PREFIX + "cargo_points");

		// 移除监听器
		removeListeners(player);

		player.sendMessage("========================================");
		player.sendMessage("死亡導致跑商任務失敗！");
		player.sendMessage("所有貨物已遺失！");
		player.sendMessage("========================================");

		LOGGER.info("[RunMerchant] 玩家 " + player.getName() + " 被玩家擊殺，跑商任務已清除");
	}

	public static void main(String[] args)
	{
		System.out.println("跑商系統加載完畢！");
		new RunMerchant();
	}
}