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
package org.l2jmobius.gameserver.instancemechanics.impl;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.instancemechanics.AbstractBossMechanic;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;

/**
 * 點名機制：隨機點名一個玩家，其他玩家必須遠離指定距離，否則全體受到懲罰
 * 懲罰計算：基礎扣血% + (未達標玩家數 * 額外扣血%)
 * 此機制需要由外部手動觸發（例如在特定血量時觸發）
 * @author Custom
 */
public class TargetMarkMechanic extends AbstractBossMechanic
{
	// ========== 機制配置（可在此處修改） ==========
	private static final int REQUIRED_DISTANCE = 1000;           // 要求的距離
	private static final int COUNTDOWN_SECONDS = 5;              // 倒數秒數
	private static final int BASE_PENALTY_PERCENT = 50;          // 基礎懲罰百分比（50 = 扣50%血）
	private static final int ADDITIONAL_PENALTY_PERCENT = 25;    // 每個未達標玩家的額外懲罰（25 = 每人+25%）
	// ============================================

	private ScheduledFuture<?> _countdownTask;
	private Player _markedPlayer;
	private volatile boolean _isTriggering = false;

	/**
	 * 創建點名機制
	 * @param boss BOSS NPC
	 * @param world 副本實例
	 */
	public TargetMarkMechanic(Npc boss, Instance world)
	{
		super(boss, world);
	}

	@Override
	public void onBossSpawn(Npc boss, Instance world)
	{
		_active = true;
		LOGGER.info("TargetMarkMechanic: 已啟動 - 距離=" + REQUIRED_DISTANCE + ", 倒數=" + COUNTDOWN_SECONDS + "秒");
	}

	/**
	 * 手動觸發點名機制（由外部調用，例如在特定血量時）
	 */
	public void trigger()
	{
		if (!_active || _boss.isDead() || _isTriggering)
		{
			return;
		}

		final List<Player> players = getPlayersInInstance();
		if (players.isEmpty())
		{
			return;
		}

		_isTriggering = true;

		// 隨機選擇一個玩家
		_markedPlayer = players.get(Rnd.get(players.size()));

		// 廣播點名訊息
		final String markMessage = "玩家 " + _markedPlayer.getName() + " 被標記！所有人遠離 " + REQUIRED_DISTANCE + " 距離！";
		broadcastScreenMessage(markMessage, 3000);
		broadcastMessage("§c[天罰警告] " + markMessage);

		// 啟動倒數
		startCountdown();
	}

	/**
	 * 觸發點名機制（已廢棄，請使用 trigger()）
	 */
	@Deprecated
	private void triggerMechanic()
	{
		trigger();
	}

	/**
	 * 啟動倒數計時
	 */
	private void startCountdown()
	{
		final AtomicInteger countdown = new AtomicInteger(COUNTDOWN_SECONDS);

		_countdownTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			final int remaining = countdown.getAndDecrement();

			if (remaining > 0)
			{
				// 顯示倒數
				broadcastScreenMessage("§e天罰倒數：" + remaining + " 秒", 1000);
			}
			else
			{
				// 倒數結束，執行檢查
				checkDistanceAndApplyPenalty();

				// 取消倒數任務
				if (_countdownTask != null)
				{
					_countdownTask.cancel(false);
					_countdownTask = null;
				}
			}
		}, 0, 1000); // 每秒執行一次
	}

	/**
	 * 檢查距離並執行懲罰
	 */
	private void checkDistanceAndApplyPenalty()
	{
		if (_markedPlayer == null || !_active)
		{
			return;
		}

		final List<Player> players = getPlayersInInstance();
		int failedCount = 0;

		// 檢查每個玩家與被點名玩家的距離
		for (Player player : players)
		{
			if (player.equals(_markedPlayer))
			{
				continue; // 跳過被點名的玩家
			}

			final double distance = player.calculateDistance2D(_markedPlayer);
			if (distance < REQUIRED_DISTANCE)
			{
				failedCount++;
			}
		}

		if (failedCount > 0)
		{
			// 計算總懲罰百分比
			final int totalPenalty = BASE_PENALTY_PERCENT + (failedCount * ADDITIONAL_PENALTY_PERCENT);

			// 對所有玩家執行懲罰
			for (Player player : players)
			{
				final double currentHp = player.getCurrentHp();
				final double penaltyAmount = currentHp * (totalPenalty / 100.0);
				final double newHp = Math.max(1, currentHp - penaltyAmount);
				player.setCurrentHp(newHp);
			}

			// 廣播懲罰訊息
			final String penaltyMessage = "§c天罰降臨！" + failedCount + " 人未達標，全體扣除 " + totalPenalty + "% 生命值！";
			broadcastScreenMessage(penaltyMessage, 5000);
			broadcastMessage("[天罰] " + penaltyMessage);

			// 如果懲罰達到或超過100%，廣播團滅訊息
			if (totalPenalty >= 100)
			{
				broadcastScreenMessage("§4團滅！", 3000);
			}
		}
		else
		{
			// 所有玩家都達標
			broadcastScreenMessage("§a距離檢查通過！", 3000);
			broadcastMessage("[天罰] 所有玩家成功遠離，躲過一劫！");
		}

		// 清除標記
		_markedPlayer = null;
	}

	@Override
	public void cleanup()
	{
		super.cleanup();

		if (_countdownTask != null)
		{
			_countdownTask.cancel(false);
			_countdownTask = null;
		}

		_markedPlayer = null;
		_isTriggering = false;
		LOGGER.info("TargetMarkMechanic: 已清理");
	}

	@Override
	public String getName()
	{
		return "TargetMarkMechanic";
	}
}
