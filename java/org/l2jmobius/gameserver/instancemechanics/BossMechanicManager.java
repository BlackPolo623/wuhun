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
package org.l2jmobius.gameserver.instancemechanics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * BOSS 機制管理器，負責管理和協調多個 BOSS 機制
 * @author Custom
 */
public class BossMechanicManager
{
	private static final Logger LOGGER = Logger.getLogger(BossMechanicManager.class.getName());

	private final Npc _boss;
	private final Instance _world;
	private final List<IBossMechanic> _mechanics = new CopyOnWriteArrayList<>();
	private ScheduledFuture<?> _hpCheckTask;
	private double _lastHpPercent = 100.0;
	private volatile boolean _active = false;

	/**
	 * 創建 BOSS 機制管理器
	 * @param boss BOSS NPC
	 * @param world 副本實例
	 */
	public BossMechanicManager(Npc boss, Instance world)
	{
		_boss = boss;
		_world = world;
	}

	/**
	 * 註冊機制
	 * @param mechanic 要註冊的機制
	 */
	public void registerMechanic(IBossMechanic mechanic)
	{
		_mechanics.add(mechanic);
		LOGGER.info("BossMechanicManager: 已註冊機制 - " + mechanic.getName());
	}

	/**
	 * 啟動所有機制
	 */
	public void start()
	{
		if (_active)
		{
			LOGGER.warning("BossMechanicManager: 已經在運行中");
			return;
		}

		_active = true;
		LOGGER.info("BossMechanicManager: 啟動 " + _mechanics.size() + " 個機制");

		// 觸發所有機制的 onBossSpawn
		for (IBossMechanic mechanic : _mechanics)
		{
			try
			{
				mechanic.onBossSpawn(_boss, _world);
			}
			catch (Exception e)
			{
				LOGGER.warning("BossMechanicManager: 機制 " + mechanic.getName() + " 啟動失敗: " + e.getMessage());
			}
		}

		// 啟動血量監控（用於階段切換）
		_hpCheckTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			if (!_active || _boss.isDead())
			{
				stop();
				return;
			}

			final double currentHpPercent = (_boss.getCurrentHp() / _boss.getMaxHp()) * 100;
			if (Math.abs(currentHpPercent - _lastHpPercent) > 0.1)
			{
				for (IBossMechanic mechanic : _mechanics)
				{
					try
					{
						mechanic.onBossHpChange(_boss, _lastHpPercent, currentHpPercent);
					}
					catch (Exception e)
					{
						LOGGER.warning("BossMechanicManager: 機制 " + mechanic.getName() + " 血量變化處理失敗: " + e.getMessage());
					}
				}
				_lastHpPercent = currentHpPercent;
			}
		}, 1000, 1000); // 每秒檢查一次
	}

	/**
	 * 停止所有機制
	 */
	public void stop()
	{
		if (!_active)
		{
			return;
		}

		_active = false;
		LOGGER.info("BossMechanicManager: 停止所有機制");

		if (_hpCheckTask != null)
		{
			_hpCheckTask.cancel(false);
			_hpCheckTask = null;
		}

		for (IBossMechanic mechanic : _mechanics)
		{
			try
			{
				mechanic.cleanup();
			}
			catch (Exception e)
			{
				LOGGER.warning("BossMechanicManager: 機制 " + mechanic.getName() + " 清理失敗: " + e.getMessage());
			}
		}

		_mechanics.clear();
	}

	/**
	 * 分發攻擊事件
	 * @param target 攻擊目標
	 * @param skill 使用的技能
	 */
	public void notifyAttack(Creature target, Skill skill)
	{
		if (!_active)
		{
			return;
		}

		for (IBossMechanic mechanic : _mechanics)
		{
			try
			{
				mechanic.onBossAttack(_boss, target, skill);
			}
			catch (Exception e)
			{
				LOGGER.warning("BossMechanicManager: 機制 " + mechanic.getName() + " 攻擊事件處理失敗: " + e.getMessage());
			}
		}
	}

	/**
	 * 分發受傷事件
	 * @param attacker 攻擊者
	 * @param damage 傷害值
	 */
	public void notifyDamaged(Creature attacker, double damage)
	{
		if (!_active)
		{
			return;
		}

		for (IBossMechanic mechanic : _mechanics)
		{
			try
			{
				mechanic.onBossDamaged(_boss, attacker, damage);
			}
			catch (Exception e)
			{
				LOGGER.warning("BossMechanicManager: 機制 " + mechanic.getName() + " 受傷事件處理失敗: " + e.getMessage());
			}
		}
	}

	/**
	 * 分發死亡事件
	 * @param killer 擊殺者
	 */
	public void notifyDeath(Creature killer)
	{
		if (!_active)
		{
			return;
		}

		for (IBossMechanic mechanic : _mechanics)
		{
			try
			{
				mechanic.onBossDeath(_boss, killer);
			}
			catch (Exception e)
			{
				LOGGER.warning("BossMechanicManager: 機制 " + mechanic.getName() + " 死亡事件處理失敗: " + e.getMessage());
			}
		}

		stop();
	}

	/**
	 * 檢查管理器是否在運行
	 * @return true 如果在運行
	 */
	public boolean isActive()
	{
		return _active;
	}
}
