package custom.BossAuctionSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.handler.BossDropHandler;
import org.l2jmobius.gameserver.handler.IBossDropHandler;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;

import custom.BossAuctionSystem.BossAuctionManager.DropItem;

/**
 * Boss Kill Listener
 * 監聽BOSS擊殺事件並記錄傷害
 * 同時實現掉落攔截功能
 * @author 黑普羅
 */
public class BossKillListener extends Script implements IBossDropHandler
{
	private static final Logger LOGGER = Logger.getLogger(BossKillListener.class.getName());

	// 【優化】批次更新間隔（毫秒）- 每 5 秒批次更新一次
	private static final long BATCH_UPDATE_INTERVAL = 5000;

	// 【優化】本地傷害緩存 - 每個 BOSS 的傷害暫存
	// Key: BOSS ObjectId, Value: Map<PlayerId, long[]{damage, playerName_hashRef}>
	// 使用 PlayerCache 記錄 playerId -> playerName，避免玩家離線後丟失名稱
	private final Map<Integer, Map<Integer, AtomicLong>> _localDamageCache = new ConcurrentHashMap<>();
	// 【Bug3修復】緩存玩家名稱，離線後仍可查詢
	private final Map<Integer, String> _playerNameCache = new ConcurrentHashMap<>();

	// 【優化】追蹤每個 BOSS 的批次更新任務
	private final Map<Integer, java.util.concurrent.ScheduledFuture<?>> _batchUpdateTasks = new ConcurrentHashMap<>();

	public BossKillListener()
	{
		// 【修復：載入順序問題】BossAuctionSystem 腳本比 WorldBossNpc 先載入（字母順序），
		// 所以 WorldBossConfig 此時可能尚未初始化，getBossNpcIds() 可能回傳空列表。
		// 改為使用兩個來源取聯集，確保所有 BOSS 都被監聽：
		// 1. BossAuctionConfig（由 BossAuctionManager.getInstance() 在此之前已載入）
		// 2. WorldBossConfig（若已載入則一併加入，否則為空）
		java.util.Set<Integer> monitorIds = new java.util.HashSet<>();
		monitorIds.addAll(BossAuctionConfig.getEnabledBossIds());
		monitorIds.addAll(WorldBossConfig.getBossNpcIds());

		for (int bossId : monitorIds)
		{
			addAttackId(bossId);
			addKillId(bossId);
		}

		// ========== 【重要】註冊掉落攔截處理器 ==========
		BossDropHandler.registerHandler(this);

		LOGGER.info("========================================");
		LOGGER.info("【競標系統】BOSS擊殺監聽器已載入");
		LOGGER.info("【競標系統】已註冊監聽 BOSS ID: " + monitorIds);
		LOGGER.info("【競標系統】已註冊掉落攔截處理器");
		LOGGER.info("========================================");
	}

	/**
	 * 監聽攻擊事件（記錄對BOSS的傷害）
	 * 【優化】使用本地緩存，不直接寫入主 Map
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		// 檢查此BOSS是否啟用競標系統
		if (!BossAuctionManager.getInstance().isBossEnabled(npc.getId()))
		{
			return;
		}

		if (attacker == null || damage <= 0)
		{
			return;
		}

		// 【優化】累積到本地緩存，而不是直接寫入主 Map
		int bossObjectId = npc.getObjectId();
		int playerId = attacker.getObjectId();

		// 【Bug3修復】緩存玩家名稱，確保離線後仍可查詢
		_playerNameCache.put(playerId, attacker.getName());

		// 獲取或創建此 BOSS 的本地緩存
		Map<Integer, AtomicLong> bossCache = _localDamageCache.computeIfAbsent(bossObjectId, k -> new ConcurrentHashMap<>());

		// 累加傷害到本地緩存（使用 AtomicLong 保證線程安全）
		AtomicLong playerDamage = bossCache.computeIfAbsent(playerId, k -> new AtomicLong(0));
		playerDamage.addAndGet(damage);

		// 【優化】如果這是第一次攻擊此 BOSS，啟動批次更新任務
		if (!_batchUpdateTasks.containsKey(bossObjectId))
		{
			startBatchUpdateTask(bossObjectId);
		}
	}

	/**
	 * 【優化】啟動批次更新任務
	 * 每 5 秒將本地緩存的傷害同步到主 Map
	 */
	private void startBatchUpdateTask(int bossObjectId)
	{
		java.util.concurrent.ScheduledFuture<?> task = ThreadPool.scheduleAtFixedRate(() -> {
			try
			{
				syncDamageToMainMap(bossObjectId);
			}
			catch (Exception e)
			{
				LOGGER.warning("【競標系統】批次更新傷害失敗: " + e.getMessage());
			}
		}, BATCH_UPDATE_INTERVAL, BATCH_UPDATE_INTERVAL);

		_batchUpdateTasks.put(bossObjectId, task);
		LOGGER.info("【競標系統】已啟動 BOSS " + bossObjectId + " 的批次更新任務（每 " + (BATCH_UPDATE_INTERVAL / 1000) + " 秒）");
	}

	/**
	 * 【優化】將本地緩存的傷害同步到主 Map
	 */
	private void syncDamageToMainMap(int bossObjectId)
	{
		Map<Integer, AtomicLong> bossCache = _localDamageCache.get(bossObjectId);
		if (bossCache == null || bossCache.isEmpty())
		{
			return;
		}

		// 批次更新到主 Map
		int updateCount = 0;
		for (Map.Entry<Integer, AtomicLong> entry : bossCache.entrySet())
		{
			int playerId = entry.getKey();
			long damage = entry.getValue().getAndSet(0); // 讀取並重置為 0

			if (damage > 0)
			{
				// 【Bug3修復】從名稱緩存取得玩家名稱，不需要玩家在線
				String playerName = _playerNameCache.get(playerId);
				if (playerName == null)
				{
					// 理論上不應發生（名稱在 onAttack 時緩存），保留安全回退
					Player player = org.l2jmobius.gameserver.model.World.getInstance().getPlayer(playerId);
					if (player != null)
					{
						playerName = player.getName();
					}
					else
					{
						LOGGER.warning("【競標系統】找不到玩家名稱 ID: " + playerId + "，跳過此筆傷害記錄");
						continue;
					}
				}

				BossAuctionManager.getInstance().recordDamage(bossObjectId, playerId, playerName, damage);
				updateCount++;
			}
		}

		if (updateCount > 0)
		{
			LOGGER.info("【競標系統】批次更新 BOSS " + bossObjectId + " 的傷害記錄，共 " + updateCount + " 個玩家");
		}
	}

	/**
	 * 【優化】立即同步所有未更新的傷害（BOSS 死亡時調用）
	 */
	private void syncAllDamageImmediately(int bossObjectId)
	{
		// 停止批次更新任務
		java.util.concurrent.ScheduledFuture<?> task = _batchUpdateTasks.remove(bossObjectId);
		if (task != null)
		{
			task.cancel(false);
		}

		// 立即同步所有傷害
		syncDamageToMainMap(bossObjectId);

		// 清除本地緩存（包含名稱緩存中屬於此 BOSS 戰鬥的玩家）
		Map<Integer, AtomicLong> bossCache = _localDamageCache.remove(bossObjectId);
		if (bossCache != null)
		{
			// 清除這場戰鬥的玩家名稱緩存
			for (int playerId : bossCache.keySet())
			{
				_playerNameCache.remove(playerId);
			}
		}

		LOGGER.info("【競標系統】已停止 BOSS " + bossObjectId + " 的批次更新任務並同步所有傷害");
	}

	// ========== IBossDropHandler Interface Implementation ==========

	/**
	 * 檢查此BOSS是否應該由競標系統處理
	 */
	@Override
	public boolean shouldHandle(int bossId)
	{
		// 檢查是否啟用（移除日誌輸出）
		return BossAuctionManager.getInstance().isBossEnabled(bossId);
	}

	/**
	 * 處理BOSS掉落（攔截掉落物品並創建競標會話）
	 * 這個方法會在物品掉落到地面之前被調用
	 */
	@Override
	public boolean handleBossDrop(Attackable boss, Player killer, Collection<ItemHolder> drops)
	{
		try
		{
			// 【優化】BOSS 死亡時，立即同步所有未更新的傷害
			syncAllDamageImmediately(boss.getObjectId());

			// 驗證輸入
			if (drops == null || drops.isEmpty())
			{
				LOGGER.warning("【競標系統】BOSS " + boss.getName() + " (ID: " + boss.getId() + ") 沒有掉落物");
				return false; // 無掉落，使用正常行為
			}

			if (killer == null)
			{
				LOGGER.warning("【競標系統】BOSS " + boss.getName() + " (ID: " + boss.getId() + ") 擊殺者為空");
				return false; // 無擊殺者，使用正常行為
			}

			LOGGER.info("【競標系統】" + boss.getName() + " 被擊殺，開始處理掉落（共 " + drops.size() + " 件物品）");

			// 轉換掉落物為競標物品
			List<DropItem> auctionItems = new ArrayList<>();
			for (ItemHolder drop : drops)
			{
				// 創建競標物品（移除詳細日誌）
				String itemData = buildItemData(drop);
				DropItem auctionItem = new DropItem(
					drop.getId(),
					drop.getCount(),
					0, // 起標價格由管理器設定
					itemData
				);
				auctionItems.add(auctionItem);
			}

			// 獲取臨時SessionId（使用BOSS的ObjectId）
			int tempSessionId = boss.getObjectId();

			// 創建競標會話
			int sessionId = BossAuctionManager.getInstance().createAuctionSession(
				boss.getId(),
				boss.getName(),
				auctionItems,
				tempSessionId  // 傳入tempSessionId以便查找傷害數據
			);

			if (sessionId > 0)
			{
				// 將傷害記錄從臨時SessionId轉移到真實SessionId
				transferDamageData(tempSessionId, sessionId);

				LOGGER.info("【競標系統】成功創建競標會話 ID: " + sessionId + "，掉落物已攔截");
				return true; // 成功處理，不要掉落物品到地面
			}
			else
			{
				LOGGER.warning("【競標系統】創建競標會話失敗，物品將正常掉落");
				return false; // 創建會話失敗，使用正常掉落
			}
		}
		catch (Exception e)
		{
			LOGGER.severe("【競標系統】處理BOSS掉落時發生錯誤: " + e.getMessage());
			e.printStackTrace();
			LOGGER.warning("【系統】由於錯誤，物品將正常掉落到地面");
			return false; // 發生錯誤，使用正常掉落以防止物品丟失
		}
	}

	/**
	 * 構建物品數據字串
	 * 可以擴展以包含強化、附魔等資訊
	 */
	private String buildItemData(ItemHolder drop)
	{
		// 目前只儲存基本物品信息
		// 可以擴展以儲存:
		// - Enhancement level (強化等級)
		// - Enchantment level (附魔等級)
		// - Augmentation data (增幅數據)
		// - Elemental attributes (屬性)
		// 等等
		return "itemId:" + drop.getId() + ",count:" + drop.getCount();
	}

	/**
	 * 將臨時SessionId的傷害數據轉移到真實SessionId
	 */
	private void transferDamageData(int tempSessionId, int realSessionId)
	{
		// 從記憶體中獲取臨時SessionId的傷害數據
		Map<Integer, Map<Integer, BossAuctionManager.DamageInfo>> damageTracker =
			BossAuctionManager.getInstance().getDamageTracker();

		Map<Integer, BossAuctionManager.DamageInfo> sessionDamage = damageTracker.get(tempSessionId);

		if (sessionDamage != null && !sessionDamage.isEmpty())
		{
			// 將數據保存到真實SessionId
			damageTracker.put(realSessionId, sessionDamage);

			// 清除臨時數據
			damageTracker.remove(tempSessionId);
		}
	}

	public static void main(String[] args)
	{
		new BossKillListener();
		System.out.println("【系統】BOSS擊殺監聽器載入完畢！");
	}
}
