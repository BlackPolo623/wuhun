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
package instances.Lunhuiinstance;

import java.time.LocalDate;
import java.time.ZoneId;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 六道輪迴的挑戰 - 每日副本
 * @author BlackPolo
 */
public class Lunhuiinstance extends InstanceScript
{
	// NPCs
	private static final int SORA = 900016; // 進入NPC
	private static final int GOLBERG = 61000; // BOSS
	
	// BOSS刷新位置
	private final Location BOSS_LOCATION = new Location(-185854, 147878, -15312);
	
	// 道具配置
	private static final int ITEM_ID = 57; // 消耗道具ID(金幣)
	private static final long ITEM_COUNT = 1000000; // 消耗道具數量
	private static final int REWARD_ITEM_ID = 105805; // 獎勵道具ID
	private static final long REWARD_COUNT = 1; // 獎勵道具數量
	
	// 副本配置
	private static final int TEMPLATE_ID = 500; // 副本模板ID
	private static final int DAILY_MAX_COUNT = 1; // 每日最大挑戰次數
	
	// PlayerVariables 鍵值
	private static final String VAR_LAST_RESET_TIME = "Lunhui_LastResetTime"; // 改為存儲時間戳
	private static final String VAR_DAILY_COUNT = "Lunhui_DailyCount";
	
	public Lunhuiinstance()
	{
		super(TEMPLATE_ID);
		addStartNpc(SORA);
		addKillId(GOLBERG);
		addInstanceLeaveId(TEMPLATE_ID);
		addFirstTalkId(SORA);
		addTalkId(SORA);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = "";
		
		if (event.equals("enterInstance"))
		{
			// 檢查並重置每日次數
			checkAndResetDailyCount(player);
			
			// 獲取今日已使用次數
			int dailyCount = player.getVariables().getInt(VAR_DAILY_COUNT, 0);
			
			// 檢查是否還有剩餘次數
			if (dailyCount >= DAILY_MAX_COUNT)
			{
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "六道輪迴", "今日挑戰次數已用盡!明天再來吧。"));
				player.sendPacket(new ExShowScreenMessage("今日挑戰次數已用盡!", 3000));
				return htmltext;
			}
			
			// 檢查是否有足夠的道具
			if (!player.destroyItemByItemId(null, ITEM_ID, ITEM_COUNT, player, true))
			{
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "六道輪迴", "挑戰道具不足!需要 " + ITEM_COUNT + " 個金幣。"));
				player.sendPacket(new ExShowScreenMessage("挑戰道具不足!", 2000));
				return htmltext;
			}
			// 處理舊副本
			Instance oldInstance = getPlayerInstance(player);
			if (oldInstance != null)
			{
				// player.teleToLocation(81292, 148159, -3464);
				player.setInstanceById(0);
			}
			// 增加今日挑戰次數
			player.getVariables().set(VAR_DAILY_COUNT, dailyCount + 1);
			
			// 進入副本
			enterInstance(player, npc, TEMPLATE_ID);
			LOGGER.info("玩家 " + player.getName() + " 進入六道輪迴副本");
			
			// 刷新BOSS
			doSpawns(player, BOSS_LOCATION.getX(), BOSS_LOCATION.getY(), BOSS_LOCATION.getZ());
			
			// 發送進入訊息
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "六道輪迴", "歡迎進入六道輪迴的挑戰!擊敗BOSS獲得豐厚獎勵!"));
			player.sendPacket(new ExShowScreenMessage("進入六道輪迴的挑戰!", 3000));
		}
		
		return htmltext;
	}
	
	/**
	 * 檢查並重置每日次數(使用時間戳方式)
	 */
	private void checkAndResetDailyCount(Player player)
	{
		PlayerVariables vars = player.getVariables();
		
		// 獲取上次重置時間(當天0點的時間戳)
		long lastResetTime = vars.getLong(VAR_LAST_RESET_TIME, 0);
		
		// 獲取今天0點的時間戳
		long currentDayStart = getTodayStartTimestamp();
		
		// 如果上次重置時間小於今天0點,說明已經跨天了,需要重置
		if (lastResetTime < currentDayStart)
		{
			vars.set(VAR_LAST_RESET_TIME, currentDayStart);
			vars.set(VAR_DAILY_COUNT, 0);
			
			LOGGER.info("玩家 " + player.getName() + " 的六道輪迴副本次數已重置");
		}
	}
	
	/**
	 * 獲取今天0點的時間戳(毫秒) 這樣可以確保過了23:59:59就會是新的一天
	 */
	private long getTodayStartTimestamp()
	{
		LocalDate today = LocalDate.now(ZoneId.systemDefault());
		return today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
	
	/**
	 * 獲取今日剩餘挑戰次數
	 */
	private int getRemainingCount(Player player)
	{
		checkAndResetDailyCount(player);
		int dailyCount = player.getVariables().getInt(VAR_DAILY_COUNT, 0);
		return Math.max(0, DAILY_MAX_COUNT - dailyCount);
	}
	
	/**
	 * 刷新BOSS
	 */
	public Npc doSpawns(Player player, int X, int Y, int Z)
	{
		try
		{
			final Spawn spawnDat = new Spawn(GOLBERG);
			spawnDat.setXYZ(X, Y, Z);
			spawnDat.setInstanceId(player.getInstanceId());
			Npc npc = spawnDat.doSpawn();
			npc.setCurrentHp(npc.getMaxHp());
			return spawnDat.getLastSpawn();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public final void onKill(Npc npc, Player player, boolean isPet)
	{
		if (npc.getId() == GOLBERG)
		{
			// 發放獎勵
			player.addItem(null, REWARD_ITEM_ID, REWARD_COUNT, player, true);
			
			// 發送成功訊息
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "六道輪迴", "恭喜您成功完成六道輪迴的挑戰!"));
			player.sendPacket(new ExShowScreenMessage("挑戰成功!獲得豐厚獎勵!5秒後將你傳送出去!", 5000));
			LOGGER.info("玩家 " + player.getName() + " 完成六道輪迴副本");
			
			// 5秒後傳送回普通世界
			ThreadPool.schedule(() ->
			{
				if (player.isOnline())
				{
					// 設定你要傳送的地點座標
					player.teleToLocation(148145, 213248, -2178, null);
					player.sendPacket(new ExShowScreenMessage("已傳送回普通世界", 3000));
				}
			}, 5000); // 5000毫秒 = 5秒
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = "";
		
		// 檢查並重置每日次數
		checkAndResetDailyCount(player);
		
		// 獲取剩餘次數
		int remainingCount = getRemainingCount(player);
		
		// 構建HTML頁面
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/Lunhui/start.htm");
		
		// 替換變量
		html.replace("%remaining_count%", String.valueOf(remainingCount));
		html.replace("%max_count%", String.valueOf(DAILY_MAX_COUNT));
		html.replace("%item_count%", String.valueOf(ITEM_COUNT));
		html.replace("%reward_count%", String.valueOf(REWARD_COUNT));
		
		// 判斷是否可以進入
		String statusText;
		String statusColor;
		if (remainingCount > 0)
		{
			statusText = "可以挑戰";
			statusColor = "00FF00";
		}
		else
		{
			statusText = "次數已用盡";
			statusColor = "FF0000";
		}
		html.replace("%status_text%", statusText);
		html.replace("%status_color%", statusColor);
		
		player.sendPacket(html);
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new Lunhuiinstance();
		System.out.println("【系統】六道輪迴的挑戰副本載入完畢!");
	}
}