package custom.BossAuctionSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.handler.BossDropHandler;
import org.l2jmobius.gameserver.handler.IBossDropHandler;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;

import custom.BossAuctionSystem.BossAuctionManager.DamageInfo;
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
	// Key: BOSS ObjectId, Value: Map<PlayerId, AtomicLong(damage)>
	private final Map<Integer, Map<Integer, AtomicLong>> _localDamageCache = new ConcurrentHashMap<>();

	// 【優化】追蹤每個 BOSS 的批次更新任務
	private final Map<Integer, java.util.concurrent.ScheduledFuture<?>> _batchUpdateTasks = new ConcurrentHashMap<>();

	public BossKillListener()
	{
		// ========== 【重要修復】註冊要監聽的BOSS ID ==========
		// 方案1: 註冊特定的BOSS ID（從配置讀取）
		// 如果配置中有啟用的BOSS ID，則註冊它們
		if (BossAuctionManager.getInstance() != null)
		{
			// 這裡暫時使用通用方式：註冊所有可能的BOSS ID
			// 你可以根據實際需要修改為只註冊特定的BOSS
			addAttackId(70000); // 戈爾
			addKillId(70000);

			// 如果有更多BOSS，繼續添加
			// addAttackId(70001, 70002, 70003...);
			// addKillId(70001, 70002, 70003...);
		}

		// ========== 【重要】註冊掉落攔截處理器 ==========
		BossDropHandler.registerHandler(this);

		LOGGER.info("========================================");
		LOGGER.info("【競標系統】BOSS擊殺監聽器已載入（使用傳統事件系統）");
		LOGGER.info("【競標系統】已註冊監聽 BOSS ID: 70000");
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
		// 檢查目標是否為GrandBoss
		if (!(npc instanceof GrandBoss))
		{
			return;
		}

		GrandBoss boss = (GrandBoss) npc;

		// 檢查此BOSS是否啟用競標系統
		if (!BossAuctionManager.getInstance().isBossEnabled(boss.getId()))
		{
			return;
		}

		// 處理召喚獸的情況
		Player player = attacker;
		if (isSummon && attacker.hasSummon())
		{
			player = attacker;
		}

		if (player == null || damage <= 0)
		{
			return;
		}

		// 【優化】累積到本地緩存，而不是直接寫入主 Map
		int bossObjectId = boss.getObjectId();
		int playerId = player.getObjectId();

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
				// 獲取玩家名稱（從 World 查找）
				Player player = org.l2jmobius.gameserver.model.World.getInstance().getPlayer(playerId);
				if (player != null)
				{
					BossAuctionManager.getInstance().recordDamage(bossObjectId, player, damage);
					updateCount++;
				}
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

		// 清除本地緩存
		_localDamageCache.remove(bossObjectId);

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
