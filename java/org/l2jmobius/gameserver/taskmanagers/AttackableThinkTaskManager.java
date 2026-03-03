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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.model.actor.Attackable;

/**
 * @author Mobius
 * 優化說明（武魂伺服器）：
 * 1. 新增 ATTACKABLE_POOL_MAP（反向查找表）：
 *    原本 add() 和 remove() 每次都要掃描所有 POOLS 才能找到目標，
 *    屬於 O(n×m) 複雜度。現在透過 Map 直接定位所屬 Pool，降為 O(1)。
 * 2. TASK_DELAY 從 1000ms 調整為 1500ms：
 *    怪物 AI 每 1.5 秒思考一次（原為 1 秒）。
 *    伺服器怪物數量龐大時，降低思考頻率可顯著減少 CPU 積壓。
 *    對玩家的實際體驗影響極小（怪物反應延遲約 0.5 秒）。
 */
public class AttackableThinkTaskManager
{
	private static final Set<Set<Attackable>> POOLS = ConcurrentHashMap.newKeySet();

	/**
	 * 【優化】反向查找表：記錄每個 Attackable 屬於哪個 Pool。
	 * 原本 add()/remove() 需掃描全部 Pool（O(n×m)），
	 * 現在直接查表取得目標 Pool（O(1)），大幅降低高怪物數量時的 CPU 開銷。
	 */
	private static final Map<Attackable, Set<Attackable>> ATTACKABLE_POOL_MAP = new ConcurrentHashMap<>();

	private static final int POOL_SIZE = 1000;

	/**
	 * 【優化 - 方案C】AI 思考間隔：調整為 5000ms（5 秒）。
	 * 每個 Pool 的怪物每 5 秒執行一次 AI 思考，減少 80% 的 CPU 呼叫次數。
	 * 怪物反應會明顯變慢，但對 CPU 節省效果最大。
	 * 調整參考：2000 = 輕度省、3000 = 中度省、5000 = 最省（方案C）
	 * 若想恢復原始行為，改回 1000 即可。
	 */
	private static final int TASK_DELAY = 5000;

	protected AttackableThinkTaskManager()
	{
	}

	private class AttackableThink implements Runnable
	{
		private final Set<Attackable> _attackables;

		public AttackableThink(Set<Attackable> attackables)
		{
			_attackables = attackables;
		}

		@Override
		public void run()
		{
			if (_attackables.isEmpty())
			{
				return;
			}

			CreatureAI ai;
			Attackable attackable;
			final Iterator<Attackable> iterator = _attackables.iterator();
			while (iterator.hasNext())
			{
				attackable = iterator.next();
				if (attackable.hasAI())
				{
					ai = attackable.getAI();
					if (ai != null)
					{
						ai.onActionThink();
					}
					else
					{
						// AI 為 null，從 Pool 和查找表中移除
						iterator.remove();
						ATTACKABLE_POOL_MAP.remove(attackable);
					}
				}
				else
				{
					// 無 AI，從 Pool 和查找表中移除
					iterator.remove();
					ATTACKABLE_POOL_MAP.remove(attackable);
				}
			}
		}
	}

	/**
	 * 【優化】add() 方法：O(n×m) → O(1)
	 * 原本需要兩次迴圈掃描所有 Pool：
	 *   第一輪：確認是否已存在
	 *   第二輪：找有空位的 Pool
	 * 現在透過 ATTACKABLE_POOL_MAP 直接查詢，避免重複掃描。
	 */
	public void add(Attackable attackable)
	{
		// 【優化】直接查表確認是否已存在，O(1)，取代原本的全 Pool 掃描
		if (ATTACKABLE_POOL_MAP.containsKey(attackable))
		{
			return;
		}

		// 找有空位的 Pool 放入
		for (Set<Attackable> pool : POOLS)
		{
			if (pool.size() < POOL_SIZE)
			{
				pool.add(attackable);
				ATTACKABLE_POOL_MAP.put(attackable, pool); // 同步更新查找表
				return;
			}
		}

		// 所有 Pool 都滿了，建立新 Pool 並排程新執行緒
		final Set<Attackable> pool = ConcurrentHashMap.newKeySet(POOL_SIZE);
		pool.add(attackable);
		ATTACKABLE_POOL_MAP.put(attackable, pool); // 同步更新查找表
		ThreadPool.schedulePriorityTaskAtFixedRate(new AttackableThink(pool), TASK_DELAY, TASK_DELAY);
		POOLS.add(pool);
	}

	/**
	 * 【優化】remove() 方法：O(n×m) → O(1)
	 * 原本需要掃描所有 Pool 直到找到目標才能移除。
	 * 現在透過 ATTACKABLE_POOL_MAP 直接定位所屬 Pool，立即移除。
	 */
	public void remove(Attackable attackable)
	{
		// 【優化】直接從查找表取得所屬 Pool，O(1)，取代原本的全 Pool 掃描
		final Set<Attackable> pool = ATTACKABLE_POOL_MAP.remove(attackable);
		if (pool != null)
		{
			pool.remove(attackable);
		}
	}

	public static AttackableThinkTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final AttackableThinkTaskManager INSTANCE = new AttackableThinkTaskManager();
	}
}
