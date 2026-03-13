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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Npc;

/**
 * @author Mobius
 * 優化說明（武魂伺服器）：
 * 動態分片佇列（Sharded Queue）設計：
 * 1. 每個分片擁有獨立的佇列和執行緒，完全無共享資料 → 零競爭
 * 2. 當分片佇列超過 MAX_PENDING_PER_PARTITION 時，自動開啟新分片
 * 3. 每個分片的執行緒啟動時間錯開 250ms，避免同時觸發造成 CPU 峰值
 * 4. 最多允許 MAX_PARTITIONS 個分片，超過上限時加入負載最少的分片
 * 5. 使用 AtomicBoolean.compareAndSet() 確保每個分片同一時間只有一個執行緒在運行
 */
public class RespawnTaskManager
{
	private static final Logger LOGGER = Logger.getLogger(RespawnTaskManager.class.getName());

	// ==================== 參數配置 ====================

	/**
	 * 每個分片的最大待處理數量。
	 * 當分片佇列超過此數值時，自動開啟新分片。
	 * 建議範圍：3000-8000
	 */
	private static final int MAX_PENDING_PER_PARTITION = 5000;

	/**
	 * 最大分片數量（防止無限擴展）。
	 * 4 個分片 = 最多同時處理 20000 個待重生 NPC。
	 * 可根據伺服器 CPU 核心數調整（建議不超過 CPU 核心數的一半）。
	 */
	private static final int MAX_PARTITIONS = 4;

	/**
	 * 每個分片的啟動時間錯開間隔（毫秒）。
	 * 避免多個分片同時觸發造成 CPU 峰值。
	 */
	private static final int STAGGER_MS = 250;

	// ==================== 分片管理 ====================

	/** 所有分片的佇列列表 */
	private static final List<Map<Npc, Long>> PARTITIONS = new ArrayList<>();

	/** 每個分片的工作狀態旗標（使用 AtomicBoolean 確保原子操作） */
	private static final List<AtomicBoolean> WORKING_FLAGS = new ArrayList<>();

	// ==================== 建構子 ====================

	protected RespawnTaskManager()
	{
		// 初始化第一個分片
		createPartition();
	}

	// ==================== 分片管理方法 ====================

	/**
	 * 創建新分片並啟動對應的執行緒。
	 * 使用 synchronized 確保不會同時創建多個分片。
	 */
	private synchronized void createPartition()
	{
		if (PARTITIONS.size() >= MAX_PARTITIONS)
		{
			return;
		}

		final int partitionId = PARTITIONS.size();
		final Map<Npc, Long> queue = new ConcurrentHashMap<>();
		final AtomicBoolean working = new AtomicBoolean(false);

		PARTITIONS.add(queue);
		WORKING_FLAGS.add(working);

		// 啟動時間錯開，避免多個分片同時觸發
		final long startDelay = partitionId * STAGGER_MS;
		ThreadPool.scheduleAtFixedRate(new PartitionWorker(partitionId, queue, working), startDelay, 1000);

		LOGGER.info("RespawnTaskManager: 已啟動分片 #" + (partitionId + 1) + "（當前分片總數：" + PARTITIONS.size() + "）");
	}

	// ==================== 公開方法 ====================

	/**
	 * 將 NPC 加入待重生佇列。
	 * 優先加入尚未達到上限的分片；
	 * 若所有分片都已達到上限，則嘗試創建新分片；
	 * 若已達最大分片數，則加入負載最少的分片。
	 *
	 * @param npc  待重生的 NPC
	 * @param time 預計重生的時間戳（毫秒）
	 */
	public synchronized void add(Npc npc, long time)
	{
		// 找一個還沒超過上限的分片
		for (Map<Npc, Long> partition : PARTITIONS)
		{
			if (partition.size() < MAX_PENDING_PER_PARTITION)
			{
				partition.put(npc, time);
				return;
			}
		}

		// 所有分片都達到上限
		if (PARTITIONS.size() < MAX_PARTITIONS)
		{
			// 創建新分片並加入
			createPartition();
			PARTITIONS.get(PARTITIONS.size() - 1).put(npc, time);
		}
		else
		{
			// 已達最大分片數，加入負載最少的分片（兜底保護）
			Map<Npc, Long> leastLoaded = PARTITIONS.get(0);
			for (Map<Npc, Long> partition : PARTITIONS)
			{
				if (partition.size() < leastLoaded.size())
				{
					leastLoaded = partition;
				}
			}
			leastLoaded.put(npc, time);
		}
	}

	// ==================== 分片 Worker ====================

	/**
	 * 每個分片的處理執行緒。
	 * 每秒執行一次，處理當前分片中所有到期的 NPC 重生。
	 * 使用 AtomicBoolean.compareAndSet() 確保同一時間只有一個執行緒在運行。
	 */
	private class PartitionWorker implements Runnable
	{
		private final int _id;
		private final Map<Npc, Long> _queue;
		private final AtomicBoolean _working;
		private long _lastLogTime = 0;
		private int _totalRespawned = 0;

		public PartitionWorker(int id, Map<Npc, Long> queue, AtomicBoolean working)
		{
			_id = id;
			_queue = queue;
			_working = working;
		}

		@Override
		public void run()
		{
			// 原子操作：確保同一時間只有一個執行緒在處理此分片
			if (!_working.compareAndSet(false, true))
			{
				return;
			}

			try
			{
				if (_queue.isEmpty())
				{
					return;
				}

				final long currentTime = System.currentTimeMillis();
				final Iterator<Entry<Npc, Long>> iterator = _queue.entrySet().iterator();
				Entry<Npc, Long> entry;

				while (iterator.hasNext())
				{
					entry = iterator.next();
					if (currentTime > entry.getValue())
					{
						iterator.remove();

						final Npc npc = entry.getKey();
						final Spawn spawn = npc.getSpawn();
						if (spawn != null)
						{
							spawn.respawnNpc(npc);
							spawn._scheduledCount--;
							_totalRespawned++;
						}
					}
				}

				// 每 10 分鐘輸出一次統計日誌
				if ((currentTime - _lastLogTime) > 600000)
				{
					if (_totalRespawned > 0)
					{
						LOGGER.info("RespawnTaskManager [分片 " + (_id + 1) + "]: 過去 10 分鐘重生了 " + _totalRespawned + " 隻 NPC，待處理：" + _queue.size());
					}
					_lastLogTime = currentTime;
					_totalRespawned = 0;
				}
			}
			finally
			{
				// 無論如何都要釋放工作旗標
				_working.set(false);
			}
		}
	}

	// ==================== Singleton ====================

	public static RespawnTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final RespawnTaskManager INSTANCE = new RespawnTaskManager();
	}
}
