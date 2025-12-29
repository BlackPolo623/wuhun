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
package ai.others.Subjugation;

import java.util.Calendar;

import org.l2jmobius.gameserver.data.holders.SubjugationHolder;
import org.l2jmobius.gameserver.data.xml.SubjugationData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerPurgeHolder;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.subjugation.ExSubjugationSidebar;

/**
 * 討伐系統 - Category 11
 * 對應 Subjugation.xml 中 category="11" 的區域
 * @author 黑普羅
 */
public class Category11 extends Script
{
	// ==================== 唯一需要修改的地方 ====================
	private static final int CATEGORY = 11;  // 對應 XML 中的 category
	// ========================================================
	
	// 以下配置通常不需要修改
	private static final int MAX_KEYS = 40;           // 最大鑰匙數量
	private static final int PURGE_MAX_POINT = 1000000;  // 滿條所需點數
	
	// 從 XML 載入怪物數據
	private static final SubjugationHolder PURGE_DATA = SubjugationData.getInstance().getSubjugation(CATEGORY);
	
	private Category11()
	{
		// 自動註冊 XML 中配置的所有怪物
		addKillId(PURGE_DATA.getNpcs().keySet().stream().mapToInt(it -> it).toArray());
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		// 檢查玩家是否有體力值
		if (killer.getVitalityPoints() > 0)
		{
			// 判斷是否為熱門時段
			boolean isHotTime = false;
			for (int[] it : SubjugationData.getInstance().getSubjugation(CATEGORY).getHottimes())
			{
				final int minHour = it[0];
				final int maxHour = it[1];
				final int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				if ((currentHour >= minHour) && (maxHour > currentHour))
				{
					isHotTime = true;
				}
			}
			
			// 計算點數（熱門時段x2）
			final int pointsForMob = isHotTime ? PURGE_DATA.getNpcs().get(npc.getId()) * 2 : PURGE_DATA.getNpcs().get(npc.getId());
			final int currentPurgePoints = (killer.getPurgePoints().get(CATEGORY) == null) ? 0 : killer.getPurgePoints().get(CATEGORY).getPoints();
			final int currentKeys = (killer.getPurgePoints().get(CATEGORY) == null) ? 0 : killer.getPurgePoints().get(CATEGORY).getKeys();
			final int remainingKeys = (killer.getPurgePoints().get(CATEGORY) == null) ? 0 : killer.getPurgePoints().get(CATEGORY).getRemainingKeys();
			
			// 更新討伐點數
			killer.getPurgePoints().put(CATEGORY, new PlayerPurgeHolder(Math.min(PURGE_MAX_POINT, currentPurgePoints + pointsForMob), currentKeys, remainingKeys));
			lastCategory(killer);
			checkPurgeComplete(killer);
			
			// 發送UI更新
			killer.sendPacket(new ExSubjugationSidebar(killer, killer.getPurgePoints().get(CATEGORY)));
		}
	}
	
	/**
	 * 檢查是否達到滿條，自動轉換為鑰匙
	 */
	private void checkPurgeComplete(Player player)
	{
		final int points = player.getPurgePoints().get(CATEGORY).getPoints();
		final int keys = player.getPurgePoints().get(CATEGORY).getKeys();
		if ((points >= PURGE_MAX_POINT) && (keys < MAX_KEYS))
		{
			player.getPurgePoints().put(CATEGORY, new PlayerPurgeHolder(points - PURGE_MAX_POINT, keys + 1, player.getPurgePoints().get(CATEGORY).getRemainingKeys() - 1));
		}
	}
	
	/**
	 * 記錄玩家最後打怪的討伐分類
	 */
	private void lastCategory(Player player)
	{
		if (player.getPurgeLastCategory() == CATEGORY)
		{
			return;
		}
		
		player.getVariables().remove(PlayerVariables.PURGE_LAST_CATEGORY);
		player.getVariables().set(PlayerVariables.PURGE_LAST_CATEGORY, CATEGORY);
	}
	
	public static void main(String[] args)
	{
		new Category11();
	}
}