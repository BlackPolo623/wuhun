/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package instances.icegongg;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 冰凍君主之城副本
 * @author 黑普羅
 * 特色：
 * - 每日時間限制
 * - 兩區共享時間
 * - 時間到自動傳送出副本
 */
public class icegongg extends InstanceScript
{
	// NPCs
	private static final int SORA = 900018; // 進入NPC

	// 副本模板ID
	private static final int TEMPLATE_ID_ZONE1 = 801; // 一區
	private static final int TEMPLATE_ID_ZONE2 = 802; // 二區

	// 副本實例ID
	private static final int INSTANCE_ID_ZONE1 = 600; // 一區實例ID
	private static final int INSTANCE_ID_ZONE2 = 601; // 二區實例ID

	// 傳送座標
	private static final int X = 10441;
	private static final int Y = 249385;
	private static final int Z = -2019;

	// 離開座標（時間到後傳送到這裡）
	private static final int EXIT_X = 147931;
	private static final int EXIT_Y = 213039;
	private static final int EXIT_Z = -2177;

	// ==================== 時間限制配置 ====================
	private static final int DAILY_TIME_LIMIT_MINUTES = 480; // 每日可用時間（分鐘）
	private static final int ENTER_COST = 1000000; // 進入消耗金幣

	// 變量名稱（統一管理，兩區共用）
	private static final String VAR_USED_TIME = "IceGong_UsedTime_"; // 已使用時間（分鐘）
	private static final String VAR_ENTER_TIME = "IceGong_EnterTime"; // 本次進入時間戳
	private static final String VAR_CURRENT_ZONE = "IceGong_CurrentZone"; // 當前所在區域

	// 定時任務管理（玩家ID -> 定時任務）
	private static final Map<Integer, ScheduledFuture<?>> PLAYER_TASKS = new ConcurrentHashMap<>();

	public icegongg()
	{
		super(TEMPLATE_ID_ZONE1, TEMPLATE_ID_ZONE2);
		addStartNpc(SORA);
		addInstanceLeaveId(TEMPLATE_ID_ZONE1, TEMPLATE_ID_ZONE2);
		addFirstTalkId(SORA);
		addTalkId(SORA);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("enterInstance"))
		{
			String zoneName = event.substring(14);

			// 確定進入哪個區域
			int templateId;
			int instanceId;

			if (zoneName.startsWith("冰凍君主之城一區"))
			{
				templateId = TEMPLATE_ID_ZONE1;
				instanceId = INSTANCE_ID_ZONE1;
			}
			else if (zoneName.startsWith("冰凍君主之城二區"))
			{
				templateId = TEMPLATE_ID_ZONE2;
				instanceId = INSTANCE_ID_ZONE2;
			}
			else
			{
				return null;
			}

			// ==================== 時間檢查 ====================
			int remainingMinutes = getRemainingMinutes(player);

			if (remainingMinutes <= 0)
			{
				player.sendPacket(new ExShowScreenMessage("今日副本時間已用完！明天再來吧！", 3000));
				player.sendMessage("========================================");
				player.sendMessage("【冰凍君主之城】今日時間已用完");
				player.sendMessage("每日限制：" + DAILY_TIME_LIMIT_MINUTES + " 分鐘");
				player.sendMessage("已使用：" + getUsedMinutes(player) + " 分鐘");
				player.sendMessage("========================================");
				return null;
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

			// 記錄進入時間和區域
			player.getVariables().set(VAR_ENTER_TIME, System.currentTimeMillis());
			player.getVariables().set(VAR_CURRENT_ZONE, templateId);

			// 啟動定時任務（剩餘時間後踢出）
			scheduleKickTask(player, remainingMinutes);

			// 發送提示
			player.sendMessage("========================================");
			player.sendMessage("【冰凍君主之城】歡迎進入副本");
			player.sendMessage("今日剩餘時間：" + remainingMinutes + " 分鐘");
			player.sendMessage("時間到後將自動傳送離開");
			player.sendMessage("========================================");

			// 每10分鐘提醒一次剩餘時間
			scheduleTimeReminders(player, remainingMinutes);
		}

		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/instances/icegongg/icegongg.htm");

		// 替換剩餘時間信息
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
		// 玩家離開副本時，計算並記錄使用時間
		recordUsedTime(player);

		// 取消定時任務
		cancelPlayerTasks(player);

		// 清除進入時間記錄
		player.getVariables().remove(VAR_ENTER_TIME);
		player.getVariables().remove(VAR_CURRENT_ZONE);
	}

	// ==================== 時間管理方法 ====================

	/**
	 * 獲取今日剩餘分鐘數
	 */
	private int getRemainingMinutes(Player player)
	{
		int usedMinutes = getUsedMinutes(player);
		return Math.max(0, DAILY_TIME_LIMIT_MINUTES - usedMinutes);
	}

	/**
	 * 獲取今日已使用分鐘數
	 */
	private int getUsedMinutes(Player player)
	{
		String today = getTodayString();
		return player.getVariables().getInt(VAR_USED_TIME + today, 0);
	}

	/**
	 * 記錄本次使用時間
	 */
	private void recordUsedTime(Player player)
	{
		long enterTime = player.getVariables().getLong(VAR_ENTER_TIME, 0);

		if (enterTime == 0)
		{
			return; // 沒有進入記錄
		}

		// 計算本次停留時間（分鐘）
		long currentTime = System.currentTimeMillis();
		int minutesSpent = (int) ((currentTime - enterTime) / 60000);

		if (minutesSpent <= 0)
		{
			return; // 停留時間太短，不計算
		}

		// 累加到今日已使用時間
		String today = getTodayString();
		int totalUsed = getUsedMinutes(player) + minutesSpent;
		player.getVariables().set(VAR_USED_TIME + today, totalUsed);

		player.sendMessage("本次使用時間：" + minutesSpent + " 分鐘");
		player.sendMessage("今日累計使用：" + totalUsed + " 分鐘");
	}

	/**
	 * 獲取今日日期字符串（用於變量名）
	 */
	private String getTodayString()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		return sdf.format(new Date());
	}

	/**
	 * 啟動踢出定時任務
	 */
	private void scheduleKickTask(Player player, int minutes)
	{
		// 取消舊任務
		cancelPlayerTasks(player);

		// 創建新任務（分鐘轉毫秒）
		ScheduledFuture<?> task = ThreadPool.schedule(() -> kickPlayer(player), minutes * 60 * 1000);
		PLAYER_TASKS.put(player.getObjectId(), task);
	}

	/**
	 * 啟動時間提醒任務
	 */
	private void scheduleTimeReminders(Player player, int totalMinutes)
	{
		// 每10分鐘提醒一次
		int[] reminders = {10, 5, 3, 1}; // 剩餘10/5/3/1分鐘時提醒

		for (int reminder : reminders)
		{
			if (totalMinutes > reminder)
			{
				int delay = (totalMinutes - reminder) * 60 * 1000;
				ThreadPool.schedule(() ->
				{
					if (player.isOnline() && isInIceGongInstance(player))
					{
						player.sendPacket(new ExShowScreenMessage("副本剩餘時間：" + reminder + " 分鐘", 5000));
						player.sendMessage("【提醒】副本時間還剩 " + reminder + " 分鐘！");
					}
				}, delay);
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
			return;
		}

		if (!isInIceGongInstance(player))
		{
			return; // 已經不在副本中
		}

		// 記錄使用時間
		recordUsedTime(player);

		// 傳送出去
		player.setInstance(null);
		player.teleToLocation(EXIT_X, EXIT_Y, EXIT_Z);

		// 發送消息
		player.sendPacket(new ExShowScreenMessage("副本時間已到！已傳送離開", 5000));
		player.sendMessage("========================================");
		player.sendMessage("【冰凍君主之城】時間到了");
		player.sendMessage("您已被傳送出副本");
		player.sendMessage("今日剩餘時間：0 分鐘");
		player.sendMessage("========================================");

		// 清除任務
		PLAYER_TASKS.remove(player.getObjectId());
	}

	/**
	 * 取消玩家的定時任務
	 */
	private void cancelPlayerTasks(Player player)
	{
		ScheduledFuture<?> task = PLAYER_TASKS.remove(player.getObjectId());
		if (task != null && !task.isDone())
		{
			task.cancel(false);
		}
	}

	/**
	 * 檢查玩家是否在冰凍君主副本中
	 */
	private boolean isInIceGongInstance(Player player)
	{
		Instance instance = player.getInstanceWorld();
		if (instance == null)
		{
			return false;
		}

		int templateId = instance.getTemplateId();
		return templateId == TEMPLATE_ID_ZONE1 || templateId == TEMPLATE_ID_ZONE2;
	}

	public static void main(String[] args)
	{
		new icegongg();
		System.out.println("【副本】限時多區冰凍君主之城載入完畢！");
	}
}