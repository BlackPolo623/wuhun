/*
 * This file is part of the L2J Mobius project.
 *
 * 冰凍君主之城副本 - 時間戳版本
 * @author 黑普羅
 *
 * 修復內容：
 * 1. 使用時間戳管理每日重置
 * 2. 防止時間重複計算
 * 3. 時間上限保護
 * 4. 詳細日誌追蹤
 */
package instances.icegongg;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class icegongg extends InstanceScript
{
	private static final Logger LOGGER = Logger.getLogger(icegongg.class.getName());

	// NPCs
	private static final int SORA = 900018;

	// 副本模板ID（統一使用 801）
	private static final int TEMPLATE_ID = 801;

	// 副本實例ID（8個區域）
	private static final int INSTANCE_ID_ZONE1 = 600;
	private static final int INSTANCE_ID_ZONE2 = 601;
	private static final int INSTANCE_ID_ZONE3 = 602;
	private static final int INSTANCE_ID_ZONE4 = 603;
	private static final int INSTANCE_ID_ZONE5 = 604;
	private static final int INSTANCE_ID_ZONE6 = 605;
	private static final int INSTANCE_ID_ZONE7 = 606;
	private static final int INSTANCE_ID_ZONE8 = 607;

	// 傳送座標
	private static final int X = 10441;
	private static final int Y = 249385;
	private static final int Z = -2019;

	// 離開座標
	private static final int EXIT_X = 147931;
	private static final int EXIT_Y = 213039;
	private static final int EXIT_Z = -2177;

	// ==================== 時間配置 ====================
	private static final int DAILY_TIME_LIMIT_MINUTES = 600;  // 每日限制分鐘
	private static final int ENTER_COST = 1000000;            // 進入金幣
	private static final int RESET_HOUR = 6;                  // 重置時間：每天凌晨6點
	private static final int RESET_MINUTE = 0;

	// ==================== 變量名稱（時間戳版本）====================
	private static final String VAR_USED_TIME = "IceGong_UsedMinutes";      // 已使用分鐘數
	private static final String VAR_LAST_RESET = "IceGong_LastResetTime";   // 上次重置時間戳
	private static final String VAR_ENTER_TIME = "IceGong_EnterTime";       // 進入時間戳
	private static final String VAR_CURRENT_ZONE = "IceGong_CurrentZone";   // 當前區域
	private static final String VAR_IS_INSIDE = "IceGong_IsInside";         // 是否在副本內（防重複計算）

	// 定時任務管理
	private static final Map<Integer, ScheduledFuture<?>> PLAYER_KICK_TASKS = new ConcurrentHashMap<>();

	public icegongg()
	{
		super(TEMPLATE_ID);
		addStartNpc(SORA);
		addInstanceLeaveId(TEMPLATE_ID);
		addFirstTalkId(SORA);
		addTalkId(SORA);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("enterInstance"))
		{
			String zoneName = event.substring(14);

			int instanceId;
			if (zoneName.startsWith("冰凍君主之城一區"))
			{
				instanceId = INSTANCE_ID_ZONE1;
			}
			else if (zoneName.startsWith("冰凍君主之城二區"))
			{
				instanceId = INSTANCE_ID_ZONE2;
			}
			else if (zoneName.startsWith("冰凍君主之城三區"))
			{
				instanceId = INSTANCE_ID_ZONE3;
			}
			else if (zoneName.startsWith("冰凍君主之城四區"))
			{
				instanceId = INSTANCE_ID_ZONE4;
			}
			else if (zoneName.startsWith("冰凍君主之城五區"))
			{
				instanceId = INSTANCE_ID_ZONE5;
			}
			else if (zoneName.startsWith("冰凍君主之城六區"))
			{
				instanceId = INSTANCE_ID_ZONE6;
			}
			else if (zoneName.startsWith("冰凍君主之城七區"))
			{
				instanceId = INSTANCE_ID_ZONE7;
			}
			else if (zoneName.startsWith("冰凍君主之城八區"))
			{
				instanceId = INSTANCE_ID_ZONE8;
			}
			else
			{
				return null;
			}

			int templateId = TEMPLATE_ID;

			// ==================== 檢查並執行每日重置 ====================
			checkAndResetDaily(player);

			// ==================== 時間檢查 ====================
			int remainingMinutes = getRemainingMinutes(player);

			if (remainingMinutes <= 0)
			{
				player.sendPacket(new ExShowScreenMessage("今日副本時間已用完！明天再來吧！", 3000));
				player.sendMessage("========================================");
				player.sendMessage("【冰凍君主之城】今日時間已用完");
				player.sendMessage("每日限制：" + DAILY_TIME_LIMIT_MINUTES + " 分鐘");
				player.sendMessage("已使用：" + getUsedMinutes(player) + " 分鐘");
				player.sendMessage("下次重置：" + getNextResetTimeString());
				player.sendMessage("========================================");
				return null;
			}

			// ==================== 防止重複進入（修復版）====================
			boolean isMarkedInside = player.getVariables().getBoolean(VAR_IS_INSIDE, false);

			// 如果標記在內，但實際不在副本中，清除異常標記
			if (isMarkedInside)
			{
				Instance currentInstance = player.getInstanceWorld();
				boolean actuallyInside = currentInstance != null &&
						currentInstance.getTemplateId() == TEMPLATE_ID;

				if (!actuallyInside)
				{
					// 狀態異常，清除標記
					LOGGER.warning("[IceGong] 玩家 " + player.getName() + " 標記異常，已自動修復");
					clearPlayerState(player);
					isMarkedInside = false;
				}
				else
				{
					// 真的在副本內，拒絕進入
					player.sendMessage("系統檢測到您已在副本中，請稍候...");
					LOGGER.warning("[IceGong] 玩家 " + player.getName() + " 嘗試重複進入副本");
					return null;
				}
			}

			// ==================== 金幣檢查 ====================
			if (player.getInventory().getInventoryItemCount(57, 0) < ENTER_COST)
			{
				player.sendPacket(new ExShowScreenMessage("所需道具不足，無法進入副本！需要 " + ENTER_COST + " 金幣", 3000));
				return null;
			}

			// ==================== 扣除金幣並進入 ====================
			takeItems(player, 57, ENTER_COST);

			// 創建或進入副本
			boolean instanceExists = InstanceManager.getInstance().createInstanceFromTemplate(instanceId, templateId);
			Instance instance;

			if (instanceExists)
			{
				instance = InstanceManager.getInstance().createInstance(templateId, instanceId, player);
			}
			else
			{
				instance = InstanceManager.getInstance().getInstance(instanceId);
			}

			// 傳送進入
			player.setInstance(instance);
			player.teleToLocation(X, Y, Z, 0, instance);

			// ==================== 記錄進入狀態 ====================
			long enterTime = System.currentTimeMillis();
			player.getVariables().set(VAR_ENTER_TIME, enterTime);
			player.getVariables().set(VAR_CURRENT_ZONE, templateId);
			player.getVariables().set(VAR_IS_INSIDE, true);  // 標記在副本內

			// 日誌記錄
			LOGGER.info("[IceGong] 玩家 " + player.getName() + " 進入副本" +
					" | 模板=" + templateId+ "(" + instance + ")" +
					" | 剩餘時間=" + remainingMinutes + "分鐘" +
					" | 進入時間戳=" + enterTime);

			// ==================== 啟動定時任務 ====================
			scheduleKickTask(player, remainingMinutes);
			scheduleTimeReminders(player, remainingMinutes);

			// 發送提示
			player.sendMessage("========================================");
			player.sendMessage("【冰凍君主之城】歡迎進入副本");
			player.sendMessage("今日剩餘時間：" + remainingMinutes + " 分鐘");
			player.sendMessage("時間到後將自動傳送離開");
			player.sendMessage("========================================");
		}
		// ==================== GM指令：重置玩家時間 ====================
		else if (event.startsWith("admin_reset_icegong"))
		{
			if (!player.isGM())
			{
				return null;
			}

			player.sendMessage("已重置副本時間");
			resetPlayerTime(player);
		}
		// ==================== GM指令：查看玩家時間 ====================
		else if (event.equals("admin_check_icegong"))
		{
			if (!player.isGM())
			{
				return null;
			}

			showDebugInfo(player);
		}

		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		// 先檢查重置
		checkAndResetDaily(player);

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/instances/icegongg/icegongg.htm");

		int remainingMinutes = getRemainingMinutes(player);
		int usedMinutes = getUsedMinutes(player);

		html.replace("%remaining_time%", String.valueOf(remainingMinutes));
		html.replace("%used_time%", String.valueOf(usedMinutes));
		html.replace("%total_time%", String.valueOf(DAILY_TIME_LIMIT_MINUTES));

		player.sendPacket(html);
		return null;
	}

	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		// ==================== 防止重複計算 ====================
		if (!player.getVariables().getBoolean(VAR_IS_INSIDE, false))
		{
			LOGGER.info("[IceGong] 玩家 " + player.getName() + " 離開副本但未標記在內，跳過時間計算");
			return;
		}

		// 計算並記錄使用時間
		recordUsedTime(player, "onInstanceLeave");

		// 取消所有定時任務
		cancelKickTask(player);

		// 清除所有狀態
		clearPlayerState(player);
	}

	// ==================== 核心：每日重置檢查 ====================

	/**
	 * 檢查是否需要重置，如果需要則執行重置
	 */
	private void checkAndResetDaily(Player player)
	{
		long lastResetTime = player.getVariables().getLong(VAR_LAST_RESET, 0);
		long nextResetTime = calculateNextResetTime(lastResetTime);
		long currentTime = System.currentTimeMillis();

		// 如果當前時間已經超過下次重置時間，執行重置
		if (currentTime >= nextResetTime)
		{
			int oldUsedTime = getUsedMinutes(player);

			// 重置已使用時間
			player.getVariables().set(VAR_USED_TIME, 0);
			// 更新重置時間為今天的重置時間點
			player.getVariables().set(VAR_LAST_RESET, getTodayResetTime());

			LOGGER.info("[IceGong] 玩家 " + player.getName() + " 每日重置" +
					" | 舊使用時間=" + oldUsedTime +
					" | 上次重置=" + formatTime(lastResetTime) +
					" | 新重置時間=" + formatTime(getTodayResetTime()));

			if (oldUsedTime > 0)
			{
				player.sendMessage("【冰凍君主之城】每日時間已重置！");
			}
		}
	}

	/**
	 * 計算下次重置時間
	 * @param lastResetTime 上次重置時間戳（0表示從未重置）
	 * @return 下次重置時間戳
	 */
	private long calculateNextResetTime(long lastResetTime)
	{
		if (lastResetTime == 0)
		{
			// 從未重置過，返回今天的重置時間
			return getTodayResetTime();
		}

		// 上次重置時間 + 24小時 = 下次重置時間
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(lastResetTime);
		cal.add(Calendar.DAY_OF_MONTH, 1);
		return cal.getTimeInMillis();
	}

	/**
	 * 獲取今天的重置時間點
	 */
	private long getTodayResetTime()
	{
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, RESET_HOUR);
		cal.set(Calendar.MINUTE, RESET_MINUTE);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		return cal.getTimeInMillis();
	}

	/**
	 * 獲取下次重置時間的字符串
	 */
	private String getNextResetTimeString()
	{
		Calendar cal = Calendar.getInstance();
		long currentTime = System.currentTimeMillis();
		long todayReset = getTodayResetTime();

		if (currentTime < todayReset)
		{
			cal.setTimeInMillis(todayReset);
		}
		else
		{
			cal.setTimeInMillis(todayReset);
			cal.add(Calendar.DAY_OF_MONTH, 1);
		}

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
		return sdf.format(cal.getTime());
	}

	// ==================== 時間管理方法 ====================

	/**
	 * 獲取剩餘分鐘數
	 */
	private int getRemainingMinutes(Player player)
	{
		int usedMinutes = getUsedMinutes(player);
		return Math.max(0, DAILY_TIME_LIMIT_MINUTES - usedMinutes);
	}

	/**
	 * 獲取已使用分鐘數
	 */
	private int getUsedMinutes(Player player)
	{
		return player.getVariables().getInt(VAR_USED_TIME, 0);
	}

	/**
	 * 記錄本次使用時間（帶來源標記，方便除錯）
	 */
	private void recordUsedTime(Player player, String source)
	{
		long enterTime = player.getVariables().getLong(VAR_ENTER_TIME, 0);

		if (enterTime == 0)
		{
			LOGGER.warning("[IceGong] " + source + " - 玩家 " + player.getName() + " 沒有進入時間記錄");
			return;
		}

		long currentTime = System.currentTimeMillis();
		int minutesSpent = (int) ((currentTime - enterTime) / 60000);

		// 防止負數或異常值
		if (minutesSpent < 0)
		{
			LOGGER.warning("[IceGong] " + source + " - 玩家 " + player.getName() +
					" 計算時間為負數！enterTime=" + enterTime + ", currentTime=" + currentTime);
			minutesSpent = 0;
		}

		// 防止單次超過每日上限（異常保護）
		if (minutesSpent > DAILY_TIME_LIMIT_MINUTES)
		{
			LOGGER.warning("[IceGong] " + source + " - 玩家 " + player.getName() +
					" 單次時間異常：" + minutesSpent + "分鐘，限制為" + DAILY_TIME_LIMIT_MINUTES);
			minutesSpent = DAILY_TIME_LIMIT_MINUTES;
		}

		// 累加時間
		int currentUsed = getUsedMinutes(player);
		int newTotal = currentUsed + minutesSpent;

		// 總時間上限保護
		if (newTotal > DAILY_TIME_LIMIT_MINUTES)
		{
			newTotal = DAILY_TIME_LIMIT_MINUTES;
		}

		player.getVariables().set(VAR_USED_TIME, newTotal);

		// 日誌記錄
		LOGGER.info("[IceGong] " + source + " - 玩家 " + player.getName() +
				" 時間記錄 | 本次=" + minutesSpent + "分鐘" +
				" | 之前累計=" + currentUsed + "分鐘" +
				" | 新累計=" + newTotal + "分鐘");

		if (minutesSpent > 0)
		{
			player.sendMessage("本次使用時間：" + minutesSpent + " 分鐘");
			player.sendMessage("今日累計使用：" + newTotal + "/" + DAILY_TIME_LIMIT_MINUTES + " 分鐘");
		}
	}

	/**
	 * 清除玩家狀態
	 */
	private void clearPlayerState(Player player)
	{
		player.getVariables().remove(VAR_ENTER_TIME);
		player.getVariables().remove(VAR_CURRENT_ZONE);
		player.getVariables().set(VAR_IS_INSIDE, false);
	}

	/**
	 * 重置玩家時間（GM用）
	 */
	private void resetPlayerTime(Player player)
	{
		player.getVariables().set(VAR_USED_TIME, 0);
		player.getVariables().set(VAR_LAST_RESET, System.currentTimeMillis());
		clearPlayerState(player);

		LOGGER.info("[IceGong] GM重置玩家 " + player.getName() + " 的副本時間");
		player.sendMessage("【冰凍君主之城】時間已被重置");
	}

	/**
	 * 顯示除錯信息（GM用）
	 */
	private void showDebugInfo(Player player)
	{
		player.sendMessage("========== 冰凍君主副本除錯 ==========");
		player.sendMessage("已使用時間：" + getUsedMinutes(player) + " 分鐘");
		player.sendMessage("剩餘時間：" + getRemainingMinutes(player) + " 分鐘");
		player.sendMessage("每日上限：" + DAILY_TIME_LIMIT_MINUTES + " 分鐘");
		player.sendMessage("上次重置：" + formatTime(player.getVariables().getLong(VAR_LAST_RESET, 0)));
		player.sendMessage("進入時間：" + formatTime(player.getVariables().getLong(VAR_ENTER_TIME, 0)));
		player.sendMessage("是否在內：" + player.getVariables().getBoolean(VAR_IS_INSIDE, false));
		player.sendMessage("下次重置：" + getNextResetTimeString());
		player.sendMessage("重置時間：每天 " + RESET_HOUR + ":" + String.format("%02d", RESET_MINUTE));
		player.sendMessage("=========================================");
	}

	// ==================== 定時任務管理 ====================

	/**
	 * 啟動踢出定時任務
	 */
	private void scheduleKickTask(Player player, int minutes)
	{
		cancelKickTask(player);

		long delayMs = (long) minutes * 60 * 1000;
		ScheduledFuture<?> task = ThreadPool.schedule(() -> kickPlayer(player), delayMs);
		PLAYER_KICK_TASKS.put(player.getObjectId(), task);

		LOGGER.info("[IceGong] 玩家 " + player.getName() + " 踢出任務已設定：" + minutes + "分鐘後");
	}

	/**
	 * 啟動時間提醒任務
	 */
	private void scheduleTimeReminders(Player player, int totalMinutes)
	{
		int[] reminders = {30, 10, 5, 3, 1};

		for (int reminder : reminders)
		{
			if (totalMinutes > reminder)
			{
				long delayMs = (long) (totalMinutes - reminder) * 60 * 1000;
				ThreadPool.schedule(() ->
				{
					if (player.isOnline() && player.getVariables().getBoolean(VAR_IS_INSIDE, false))
					{
						player.sendPacket(new ExShowScreenMessage("副本剩餘時間：" + reminder + " 分鐘", 5000));
						player.sendMessage("【提醒】副本時間還剩 " + reminder + " 分鐘！");
					}
				}, delayMs);
			}
		}
	}

	/**
	 * 踢出玩家
	 */
	private void kickPlayer(Player player)
	{
		if (!player.isOnline())
		{
			PLAYER_KICK_TASKS.remove(player.getObjectId());
			return;
		}

		if (!player.getVariables().getBoolean(VAR_IS_INSIDE, false))
		{
			PLAYER_KICK_TASKS.remove(player.getObjectId());
			return;
		}

		LOGGER.info("[IceGong] 時間到，踢出玩家 " + player.getName());

		// 記錄時間（這裡是唯一的記錄點，onInstanceLeave會檢查IS_INSIDE標記避免重複）
		recordUsedTime(player, "kickPlayer");

		// 先清除狀態，防止onInstanceLeave重複計算
		clearPlayerState(player);

		// 傳送出去
		player.setInstance(null);
		player.teleToLocation(EXIT_X, EXIT_Y, EXIT_Z);

		// 發送消息
		player.sendPacket(new ExShowScreenMessage("副本時間已到！已傳送離開", 5000));
		player.sendMessage("========================================");
		player.sendMessage("【冰凍君主之城】時間到了");
		player.sendMessage("您已被傳送出副本");
		player.sendMessage("今日剩餘時間：0 分鐘");
		player.sendMessage("下次重置：" + getNextResetTimeString());
		player.sendMessage("========================================");

		PLAYER_KICK_TASKS.remove(player.getObjectId());
	}

	/**
	 * 取消踢出任務
	 */
	private void cancelKickTask(Player player)
	{
		ScheduledFuture<?> task = PLAYER_KICK_TASKS.remove(player.getObjectId());
		if (task != null && !task.isDone())
		{
			task.cancel(false);
		}
	}

	// ==================== 工具方法 ====================

	/**
	 * 格式化時間戳
	 */
	private String formatTime(long timestamp)
	{
		if (timestamp == 0)
		{
			return "無";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(new Date(timestamp));
	}

	public static void main(String[] args)
	{
		new icegongg();
		System.out.println("【副本】限時多區冰凍君主之城載入完畢！");
		System.out.println("【副本】每日重置時間：" + RESET_HOUR + ":" + String.format("%02d", RESET_MINUTE));
		System.out.println("【副本】每日時間限制：" + DAILY_TIME_LIMIT_MINUTES + " 分鐘");
	}
}