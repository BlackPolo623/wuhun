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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.instancemechanics.AbstractBossMechanic;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;

/**
 * 距離控制機制：隨機點名兩個玩家，在他們身上生成NPC，最後檢查兩個NPC的距離
 * 如果距離不符合設定值，根據距離差異對全體玩家造成懲罰
 * @author Custom
 */
public class DistanceControlMechanic extends AbstractBossMechanic
{
	// ========== 機制配置（可在此處修改） ==========
	private static final int SPAWN_NPC_ID = 18150;               // 生成的NPC ID
	private static final int FIRST_MARK_DELAY = 3000;            // 第一次點名後延遲（毫秒，3000 = 3秒）
	private static final int SECOND_MARK_INTERVAL = 2000;        // 兩次點名之間的間隔（毫秒，2000 = 2秒）
	private static final int SECOND_MARK_DELAY = 3000;           // 第二次點名後延遲（毫秒，3000 = 3秒）
	private static final int CHECK_DELAY = 5000;                 // 結算延遲（毫秒，5000 = 5秒）
	private static final int TARGET_DISTANCE = 2000;             // 目標距離
	private static final int DISTANCE_TOLERANCE = 100;           // 距離容差單位（每相差100距離）
	private static final int PENALTY_PER_UNIT = 10;              // 每個容差單位的懲罰百分比（10 = 10%）
	// ============================================

	private Player _firstMarkedPlayer;
	private Player _secondMarkedPlayer;
	private Npc _firstSpawnedNpc;
	private Npc _secondSpawnedNpc;
	private final List<Npc> _spawnedNpcs = new ArrayList<>();
	private volatile boolean _isTriggering = false;

	/**
	 * 創建距離控制機制
	 * @param boss BOSS NPC
	 * @param world 副本實例
	 */
	public DistanceControlMechanic(Npc boss, Instance world)
	{
		super(boss, world);
	}

	@Override
	public void onBossSpawn(Npc boss, Instance world)
	{
		_active = true;
		LOGGER.info("DistanceControlMechanic: 已啟動 - NPC ID=" + SPAWN_NPC_ID + ", 目標距離=" + TARGET_DISTANCE);
	}

	/**
	 * 手動觸發距離控制機制（由外部調用）
	 */
	public void trigger()
	{
		if (!_active || _boss.isDead() || _isTriggering)
		{
			return;
		}

		final List<Player> players = getPlayersInInstance();
		if (players.size() < 2)
		{
			broadcastMessage("§c[距離控制] 玩家人數不足，無法啟動機制！");
			return;
		}

		_isTriggering = true;

		// 階段1：點名第一個玩家
		_firstMarkedPlayer = players.get(Rnd.get(players.size()));
		broadcastScreenMessage("§e玩家 " + _firstMarkedPlayer.getName() + " 被第一次點名！", 3000);
		broadcastMessage("[距離控制] 玩家 " + _firstMarkedPlayer.getName() + " 被第一次點名！");

		// 階段2：延遲後在第一個玩家身上生成NPC
		ThreadPool.schedule(() -> spawnFirstNpc(), FIRST_MARK_DELAY);
	}

	/**
	 * 在第一個玩家身上生成NPC，然後點名第二個玩家
	 */
	private void spawnFirstNpc()
	{
		if (!_active || _boss.isDead() || _firstMarkedPlayer == null)
		{
			reset();
			return;
		}

		// 在第一個玩家位置生成NPC
		_firstSpawnedNpc = spawnNpc(SPAWN_NPC_ID, _firstMarkedPlayer);
		if (_firstSpawnedNpc != null)
		{
			_spawnedNpcs.add(_firstSpawnedNpc);
		}

		broadcastScreenMessage("§e第一個標記已落下！", 2000);

		// 延遲後點名第二個玩家
		ThreadPool.schedule(() -> markSecondPlayer(), SECOND_MARK_INTERVAL);
	}

	/**
	 * 點名第二個玩家
	 */
	private void markSecondPlayer()
	{
		if (!_active || _boss.isDead())
		{
			reset();
			return;
		}

		final List<Player> players = getPlayersInInstance();

		// 排除第一個被點名的玩家
		final List<Player> candidates = new ArrayList<>(players);
		candidates.remove(_firstMarkedPlayer);

		if (candidates.isEmpty())
		{
			broadcastMessage("[距離控制] 沒有其他玩家可以點名！");
			reset();
			return;
		}

		_secondMarkedPlayer = candidates.get(Rnd.get(candidates.size()));
		broadcastScreenMessage("§e玩家 " + _secondMarkedPlayer.getName() + " 被第二次點名！", 3000);
		broadcastMessage("[距離控制] 玩家 " + _secondMarkedPlayer.getName() + " 被第二次點名！");

		// 延遲後在第二個玩家身上生成NPC
		ThreadPool.schedule(() -> spawnSecondNpc(), SECOND_MARK_DELAY);
	}

	/**
	 * 在第二個玩家身上生成NPC，然後開始結算倒數
	 */
	private void spawnSecondNpc()
	{
		if (!_active || _boss.isDead() || _secondMarkedPlayer == null)
		{
			reset();
			return;
		}

		// 在第二個玩家位置生成NPC
		_secondSpawnedNpc = spawnNpc(SPAWN_NPC_ID, _secondMarkedPlayer);
		if (_secondSpawnedNpc != null)
		{
			_spawnedNpcs.add(_secondSpawnedNpc);
		}

		broadcastScreenMessage("§e第二個標記已落下！" + CHECK_DELAY / 1000 + " 秒後結算！", 3000);
		broadcastMessage("[距離控制] 兩個標記已落下！目標距離：" + TARGET_DISTANCE + "，" + CHECK_DELAY / 1000 + " 秒後結算！");

		// 延遲後結算
		ThreadPool.schedule(() -> checkAndApplyPenalty(), CHECK_DELAY);
	}

	/**
	 * 在指定玩家位置生成NPC
	 */
	private Npc spawnNpc(int npcId, Player player)
	{
		try
		{
			final Spawn spawn = new Spawn(npcId);
			spawn.setInstanceId(_world.getId());
			spawn.setHeading(player.getHeading());
			spawn.setXYZ(player.getX(), player.getY(), player.getZ());
			spawn.stopRespawn();
			return spawn.doSpawn(false);
		}
		catch (Exception e)
		{
			LOGGER.warning("DistanceControlMechanic: 生成NPC失敗 - " + e.getMessage());
			return null;
		}
	}

	/**
	 * 結算：檢查兩個NPC的距離並計算懲罰
	 */
	private void checkAndApplyPenalty()
	{
		if (!_active)
		{
			reset();
			return;
		}

		if (_firstSpawnedNpc == null || _secondSpawnedNpc == null)
		{
			broadcastMessage("[距離控制] 結算失敗：NPC不存在！");
			reset();
			return;
		}

		// 計算兩個NPC之間的距離
		final double actualDistance = _firstSpawnedNpc.calculateDistance2D(_secondSpawnedNpc);
		final double distanceDiff = Math.abs(actualDistance - TARGET_DISTANCE);
		final int units = (int) (distanceDiff / DISTANCE_TOLERANCE);
		final int penaltyPercent = units * PENALTY_PER_UNIT;

		broadcastMessage("[距離控制] 實際距離：" + (int) actualDistance + "，目標距離：" + TARGET_DISTANCE + "，差距：" + (int) distanceDiff);

		if (penaltyPercent > 0)
		{
			// 對所有玩家執行懲罰
			final List<Player> players = getPlayersInInstance();
			for (Player player : players)
			{
				final double currentHp = player.getCurrentHp();
				final double penaltyAmount = currentHp * (penaltyPercent / 100.0);
				final double newHp = Math.max(1, currentHp - penaltyAmount);
				player.setCurrentHp(newHp);
			}

			broadcastScreenMessage("§c距離偏差 " + (int) distanceDiff + "！全體扣除 " + penaltyPercent + "% 生命值！", 5000);
			broadcastMessage("[距離控制] 懲罰：全體扣除 " + penaltyPercent + "% 生命值！");
		}
		else
		{
			broadcastScreenMessage("§a距離完美！全體免受懲罰！", 5000);
			broadcastMessage("[距離控制] 距離控制完美，全體免受懲罰！");
		}

		// 清理NPC並重置狀態
		despawnNpcs();
		reset();
	}

	/**
	 * 清除所有生成的NPC
	 */
	private void despawnNpcs()
	{
		for (Npc npc : _spawnedNpcs)
		{
			if (npc != null && !npc.isDead())
			{
				npc.deleteMe();
			}
		}
		_spawnedNpcs.clear();
	}

	/**
	 * 重置機制狀態
	 */
	private void reset()
	{
		_firstMarkedPlayer = null;
		_secondMarkedPlayer = null;
		_firstSpawnedNpc = null;
		_secondSpawnedNpc = null;
		_isTriggering = false;
	}

	@Override
	public void cleanup()
	{
		super.cleanup();
		despawnNpcs();
		reset();
		LOGGER.info("DistanceControlMechanic: 已清理");
	}

	@Override
	public String getName()
	{
		return "DistanceControlMechanic";
	}
}
