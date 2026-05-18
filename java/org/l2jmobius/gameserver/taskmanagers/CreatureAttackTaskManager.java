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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.network.serverpackets.Attack;

/**
 * Creature Attack task manager class.
 * @author Mobius
 */
public class CreatureAttackTaskManager
{
	private static final Logger LOGGER = Logger.getLogger(CreatureAttackTaskManager.class.getName());

	private static final Set<Map<Creature, ScheduledAttack>> ATTACK_POOLS = ConcurrentHashMap.newKeySet();
	private static final Set<Map<Creature, ScheduledFinish>> FINISH_POOLS = ConcurrentHashMap.newKeySet();
	private static final int POOL_SIZE = 300;
	private static final int TASK_DELAY = 10;

	// 【武魂伺服器診斷】每 30 秒輸出一次 pool 統計，用於確認 ATTACK_POOLS / FINISH_POOLS 是否隨時間累積
	private static final long STATS_LOG_INTERVAL_MS = 30_000L;

	protected CreatureAttackTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(CreatureAttackTaskManager::logPoolStats, STATS_LOG_INTERVAL_MS, STATS_LOG_INTERVAL_MS);
	}

	/**
	 * 【武魂伺服器診斷】輸出 ATTACK_POOLS / FINISH_POOLS 的目前狀態。<br>
	 * 觀察重點：<br>
	 * - <b>pools=</b> 數值若隨時間單調上升、不會下降，就是 pool 永不回收的徵兆<br>
	 * - <b>entries=</b> 全部 pool 內的攻擊結算總數，反映即時負載<br>
	 * - <b>each=</b> 每個 pool 的個別大小，可看分布
	 */
	private static void logPoolStats()
	{
		final int attackPools = ATTACK_POOLS.size();
		final int finishPools = FINISH_POOLS.size();
		int attackTotal = 0;
		int finishTotal = 0;
		final StringBuilder attackDetail = new StringBuilder();
		final StringBuilder finishDetail = new StringBuilder();

		int idx = 0;
		for (Map<Creature, ScheduledAttack> pool : ATTACK_POOLS)
		{
			final int size = pool.size();
			attackTotal += size;
			if (idx > 0)
			{
				attackDetail.append(",");
			}
			attackDetail.append(size);
			idx++;
		}

		idx = 0;
		for (Map<Creature, ScheduledFinish> pool : FINISH_POOLS)
		{
			final int size = pool.size();
			finishTotal += size;
			if (idx > 0)
			{
				finishDetail.append(",");
			}
			finishDetail.append(size);
			idx++;
		}

		LOGGER.info("【攻擊任務管理】攻擊池數=" + attackPools + "  待處理=" + attackTotal + "  各池=[" + attackDetail + "]"
			+ "  |  結算池數=" + finishPools + "  待處理=" + finishTotal + "  各池=[" + finishDetail + "]");
	}
	
	private class ScheduleAttackTask implements Runnable
	{
		private final Map<Creature, ScheduledAttack> _creatureAttackData;
		
		public ScheduleAttackTask(Map<Creature, ScheduledAttack> creatureattackData)
		{
			_creatureAttackData = creatureattackData;
		}
		
		@Override
		public void run()
		{
			if (_creatureAttackData.isEmpty())
			{
				return;
			}
			
			final long currentTime = System.currentTimeMillis();
			final Iterator<Entry<Creature, ScheduledAttack>> iterator = _creatureAttackData.entrySet().iterator();
			Entry<Creature, ScheduledAttack> entry;
			ScheduledAttack scheduledAttack;
			
			while (iterator.hasNext())
			{
				entry = iterator.next();
				scheduledAttack = entry.getValue();
				
				if (currentTime >= scheduledAttack.endTime)
				{
					iterator.remove();
					final Creature creature = entry.getKey();
					TYPE_SELECT: switch (scheduledAttack.type)
					{
						case NORMAL:
						{
							creature.onHitTimeNotDual(scheduledAttack.weapon, scheduledAttack.attack, scheduledAttack.hitTime, scheduledAttack.attackTime);
							break TYPE_SELECT;
						}
						case DUAL_FIRST:
						{
							creature.onFirstHitTimeForDual(scheduledAttack.weapon, scheduledAttack.attack, scheduledAttack.hitTime, scheduledAttack.attackTime, scheduledAttack.delayForSecondAttack);
							break TYPE_SELECT;
						}
						case DUAL_SECOND:
						{
							creature.onSecondHitTimeForDual(scheduledAttack.weapon, scheduledAttack.attack, scheduledAttack.hitTime, scheduledAttack.delayForSecondAttack, scheduledAttack.attackTime);
							break TYPE_SELECT;
						}
					}
				}
			}
		}
	}
	
	private class ScheduleAbortTask implements Runnable
	{
		private final Map<Creature, ScheduledFinish> _creatureFinishData;
		
		public ScheduleAbortTask(Map<Creature, ScheduledFinish> creatureFinishData)
		{
			_creatureFinishData = creatureFinishData;
		}
		
		@Override
		public void run()
		{
			if (_creatureFinishData.isEmpty())
			{
				return;
			}
			
			final long currentTime = System.currentTimeMillis();
			final Iterator<Entry<Creature, ScheduledFinish>> iterator = _creatureFinishData.entrySet().iterator();
			Entry<Creature, ScheduledFinish> entry;
			ScheduledFinish scheduledFinish;
			
			while (iterator.hasNext())
			{
				entry = iterator.next();
				scheduledFinish = entry.getValue();
				
				if (currentTime >= scheduledFinish.endTime)
				{
					iterator.remove();
					final Creature creature = entry.getKey();
					creature.onAttackFinish(scheduledFinish.attack);
				}
			}
		}
	}
	
	public void onHitTimeNotDual(Creature creature, Weapon weapon, Attack attack, int hitTime, int attackTime)
	{
		scheduleAttack(ScheduledAttackType.NORMAL, creature, weapon, attack, hitTime, attackTime, 0, hitTime);
	}
	
	public void onFirstHitTimeForDual(Creature creature, Weapon weapon, Attack attack, int hitTime, int attackTime, int delayForSecondAttack)
	{
		scheduleAttack(ScheduledAttackType.DUAL_FIRST, creature, weapon, attack, hitTime, attackTime, delayForSecondAttack, hitTime);
	}
	
	public void onSecondHitTimeForDual(Creature creature, Weapon weapon, Attack attack, int hitTime, int attackTime, int delayForSecondAttack)
	{
		scheduleAttack(ScheduledAttackType.DUAL_SECOND, creature, weapon, attack, hitTime, attackTime, delayForSecondAttack, delayForSecondAttack);
	}
	
	private void scheduleAttack(ScheduledAttackType type, Creature creature, Weapon weapon, Attack attack, int hitTime, int attackTime, int delayForSecondAttack, int taskDelay)
	{
		final ScheduledAttack scheduledAttack = new ScheduledAttack(type, weapon, attack, hitTime, attackTime, delayForSecondAttack, taskDelay + System.currentTimeMillis());
		
		for (Map<Creature, ScheduledAttack> pool : ATTACK_POOLS)
		{
			if (pool.size() < POOL_SIZE)
			{
				pool.put(creature, scheduledAttack);
				return;
			}
		}
		
		final Map<Creature, ScheduledAttack> pool = new ConcurrentHashMap<>();
		pool.put(creature, scheduledAttack);
		ThreadPool.schedulePriorityTaskAtFixedRate(new ScheduleAttackTask(pool), TASK_DELAY, TASK_DELAY);
		ATTACK_POOLS.add(pool);
	}
	
	public void onAttackFinish(Creature creature, Attack attack, int taskDelay)
	{
		final ScheduledFinish scheduledFinish = new ScheduledFinish(attack, taskDelay + System.currentTimeMillis());
		
		for (Map<Creature, ScheduledFinish> pool : FINISH_POOLS)
		{
			if (pool.size() < POOL_SIZE)
			{
				pool.put(creature, scheduledFinish);
				return;
			}
		}
		
		final Map<Creature, ScheduledFinish> pool = new ConcurrentHashMap<>();
		pool.put(creature, scheduledFinish);
		ThreadPool.schedulePriorityTaskAtFixedRate(new ScheduleAbortTask(pool), TASK_DELAY, TASK_DELAY);
		FINISH_POOLS.add(pool);
	}
	
	public void abortAttack(Creature creature)
	{
		for (Map<Creature, ScheduledAttack> pool : ATTACK_POOLS)
		{
			if (pool.remove(creature) != null)
			{
				break;
			}
		}
		
		for (Map<Creature, ScheduledFinish> pool : FINISH_POOLS)
		{
			if (pool.remove(creature) != null)
			{
				return;
			}
		}
	}
	
	private class ScheduledAttack
	{
		public final ScheduledAttackType type;
		public final Weapon weapon;
		public final Attack attack;
		public final int hitTime;
		public final int attackTime;
		public final int delayForSecondAttack;
		public final long endTime;
		
		public ScheduledAttack(ScheduledAttackType type, Weapon weapon, Attack attack, int hitTime, int attackTime, int delayForSecondAttack, long endTime)
		{
			this.type = type;
			this.weapon = weapon;
			this.attack = attack;
			this.hitTime = hitTime;
			this.attackTime = attackTime;
			this.delayForSecondAttack = delayForSecondAttack;
			this.endTime = endTime;
		}
	}
	
	private class ScheduledFinish
	{
		public final Attack attack;
		public final long endTime;
		
		public ScheduledFinish(Attack attack, long endTime)
		{
			this.attack = attack;
			this.endTime = endTime;
		}
	}
	
	private enum ScheduledAttackType
	{
		NORMAL,
		DUAL_FIRST,
		DUAL_SECOND,
	}
	
	public static final CreatureAttackTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CreatureAttackTaskManager INSTANCE = new CreatureAttackTaskManager();
	}
}
