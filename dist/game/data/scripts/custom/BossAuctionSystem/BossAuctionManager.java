package custom.BossAuctionSystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.DiscordConfig;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.Broadcast;

import custom.BossAuctionSystem.BossAuctionDAO.*;

/**
 * Boss Auction System Manager
 * 管理競標會話、出價、獎勵分配等核心功能
 * @author 黑普羅
 */
public class BossAuctionManager
{
	private static final Logger LOGGER = Logger.getLogger(BossAuctionManager.class.getName());
	private static BossAuctionManager _instance;

	// 系統配置
	private long _auctionDurationMs;
	private long _minDamageRequired;
	private int _currencyItemId;
	private Set<Integer> _enabledBossIds;
	private long _minBidIncrement;
	private boolean _announcementEnabled;

	// 【新增】延長時間機制配置
	private long _extensionTriggerTimeMs; // 最後多少毫秒內出價會觸發延長
	private long _extensionDurationMs; // 每次延長多少毫秒
	private int _maxExtensionCount; // 最多延長次數

	// 【新增】出價冷卻配置
	private long _bidCooldownMs; // 出價冷卻時間（毫秒）

	// 當前活躍的會話 (sessionId -> AuctionSession)
	private final Map<Integer, AuctionSession> _activeSessions = new ConcurrentHashMap<>();

	// 傷害追蹤 (sessionId -> (playerId -> damage))
	private final Map<Integer, Map<Integer, DamageInfo>> _damageTracker = new ConcurrentHashMap<>();

	// 【新增】追蹤每個會話的延長次數 (sessionId -> extensionCount)
	private final Map<Integer, Integer> _sessionExtensionCount = new ConcurrentHashMap<>();

	// 【新增】追蹤玩家最後出價時間 (playerId -> lastBidTime)
	private final Map<Integer, Long> _playerLastBidTime = new ConcurrentHashMap<>();

	// 已發送競標結束倒數警告的 SessionId 集合（防止重複發送）
	private final Set<Integer> _endWarningSentSessions = ConcurrentHashMap.newKeySet();

	private BossAuctionManager()
	{
		loadConfiguration();
		loadActiveSessions();
		startAuctionCheckTask();
		LOGGER.info("【競標系統】初始化完成");
	}

	public static BossAuctionManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new BossAuctionManager();
		}
		return _instance;
	}

	/**
	 * 載入配置 - 從 INI 文件讀取
	 */
	private void loadConfiguration()
	{
		// 載入 INI 配置
		BossAuctionConfig.load();

		// 從配置類獲取設置
		_auctionDurationMs = BossAuctionConfig.getAuctionDurationMinutes() * 60000L;
		_minDamageRequired = BossAuctionConfig.getMinDamageRequired();
		_currencyItemId = BossAuctionConfig.getCurrencyItemId();
		_minBidIncrement = BossAuctionConfig.getMinBidIncrement();
		_announcementEnabled = BossAuctionConfig.isAnnouncementEnabled();
		_enabledBossIds = BossAuctionConfig.getEnabledBossIds();

		// 【新增】載入延長時間配置
		_extensionTriggerTimeMs = BossAuctionConfig.getExtensionTriggerMinutes() * 60000L;
		_extensionDurationMs = BossAuctionConfig.getExtensionDurationMinutes() * 60000L;
		_maxExtensionCount = BossAuctionConfig.getMaxExtensionCount();

		// 【新增】載入出價冷卻配置
		_bidCooldownMs = BossAuctionConfig.getBidCooldownSeconds() * 1000L;

		LOGGER.info("【競標系統】配置載入完成 - 競標時長: " + (_auctionDurationMs / 60000) + "分鐘, 最低傷害: " + _minDamageRequired);
		LOGGER.info("【競標系統】延長機制 - 觸發時間: " + (_extensionTriggerTimeMs / 60000) + "分鐘, 延長時間: " + (_extensionDurationMs / 60000) + "分鐘, 最多延長: " + _maxExtensionCount + "次");
		LOGGER.info("【競標系統】出價冷卻 - " + (_bidCooldownMs / 1000) + "秒");
	}

	/**
	 * 載入現有的活躍會話
	 */
	private void loadActiveSessions()
	{
		List<AuctionSession> sessions = BossAuctionDAO.getActiveSessions();
		for (AuctionSession session : sessions)
		{
			_activeSessions.put(session.sessionId, session);
		}
		LOGGER.info("【競標系統】載入 " + sessions.size() + " 個活躍會話");
	}

	/**
	 * 檢查BOSS是否啟用競標。
	 * 優先檢查 BossAuction.ini 的 EnabledBossIds；
	 * 若不在其中，亦檢查 WorldBoss.ini 的 BossNpcIds（世界首領必然參與競標）。
	 * EnabledBossIds 為空時，視為全部啟用（相容舊行為）。
	 */
	public boolean isBossEnabled(int npcId)
	{
		if (_enabledBossIds.isEmpty())
		{
			return true;
		}
		if (_enabledBossIds.contains(npcId))
		{
			return true;
		}
		// 兜底：世界首領 NPC ID 一律允許參與競標分紅
		return WorldBossConfig.getBossNpcIds().contains(npcId);
	}

	/**
	 * 記錄對BOSS造成的傷害
	 */
	public void recordDamage(int sessionId, Player player, long damage)
	{
		if (player == null || damage <= 0)
		{
			return;
		}
		recordDamage(sessionId, player.getObjectId(), player.getName(), damage);
	}

	/**
	 * 記錄對BOSS造成的傷害（支援離線玩家）
	 */
	public void recordDamage(int sessionId, int playerId, String playerName, long damage)
	{
		if (playerId <= 0 || playerName == null || damage <= 0)
		{
			return;
		}

		Map<Integer, DamageInfo> sessionDamage = _damageTracker.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
		DamageInfo info = sessionDamage.computeIfAbsent(playerId, k -> new DamageInfo(playerId, playerName));
		info.addDamage(damage);
	}

	/**
	 * BOSS被擊殺時創建競標會話（無傷害數據版本，用於掉落攔截器）
	 * @param bossNpcId BOSS的NPC ID
	 * @param bossName BOSS名稱
	 * @param drops 掉落物品列表
	 */
	public int createAuctionSession(int bossNpcId, String bossName, List<DropItem> drops)
	{
		// 調用完整版本，傳入 -1 表示沒有臨時SessionId
		return createAuctionSession(bossNpcId, bossName, drops, -1);
	}

	/**
	 * BOSS被擊殺時創建競標會話（完整版本，包含傷害數據）
	 * @param bossNpcId BOSS的NPC ID
	 * @param bossName BOSS名稱
	 * @param drops 掉落物品列表
	 * @param tempSessionId 臨時SessionId（BOSS的ObjectId），用於查找傷害數據；-1 表示沒有傷害數據
	 */
	public int createAuctionSession(int bossNpcId, String bossName, List<DropItem> drops, int tempSessionId)
	{
		long killTime = System.currentTimeMillis();
		long endTime = killTime + _auctionDurationMs;

		// 創建會話
		int sessionId = BossAuctionDAO.createAuctionSession(bossNpcId, bossName, killTime, endTime);
		if (sessionId <= 0)
		{
			LOGGER.warning("【競標系統】創建會話失敗");
			return -1;
		}

		// 添加掉落物品到競標
		for (DropItem drop : drops)
		{
			int auctionItemId = BossAuctionDAO.addAuctionItem(
				sessionId,
				drop.itemId,
				drop.count,
				drop.enchantLevel,
				drop.itemData
			);

			if (auctionItemId <= 0)
			{
				LOGGER.warning("【競標系統】添加物品失敗: " + drop.itemId);
			}
		}

		// ========== 【修復】使用 tempSessionId 查找傷害數據 ==========
		// 傷害數據以 BOSS ObjectId 作為 key 存入 _damageTracker
		// tempSessionId == -1 表示沒有傷害數據
		if (tempSessionId > 0)
		{
			Map<Integer, DamageInfo> sessionDamage = _damageTracker.get(tempSessionId);
			LOGGER.info("【競標系統】查詢傷害資料 - tempSessionId(bossObjectId)=" + tempSessionId +
				"，_damageTracker 中的記錄數：" + (sessionDamage != null ? sessionDamage.size() : "null（無資料！）"));

			if (sessionDamage != null && !sessionDamage.isEmpty())
			{
				int totalPlayers = sessionDamage.size();
				int savedCount = 0;
				for (DamageInfo info : sessionDamage.values())
				{
					LOGGER.info("【競標系統】玩家傷害記錄 - " + info.playerName +
						"：" + info.totalDamage + "（門檻 " + _minDamageRequired + "，" +
						(info.totalDamage >= _minDamageRequired ? "✓ 達標" : "✗ 未達標") + "）");
					if (info.totalDamage >= _minDamageRequired)
					{
						BossAuctionDAO.recordParticipant(sessionId, info.playerId, info.playerName, info.totalDamage);
						savedCount++;
					}
				}
				LOGGER.info("【競標系統】已保存 " + savedCount + "/" + totalPlayers + " 個合格參與者");
			}
			else
			{
				LOGGER.warning("【競標系統】⚠ 找不到傷害數據！tempSessionId=" + tempSessionId +
					"，_damageTracker keys=" + _damageTracker.keySet() +
					"。可能原因：addAttackId 未對此 NPC 生效，或世界首領 NPC ID 未在任何設定檔中登記。");
			}
		}

		// 創建並添加到活躍會話（不需要從資料庫查詢）
		AuctionSession newSession = new AuctionSession();
		newSession.sessionId = sessionId;
		newSession.bossNpcId = bossNpcId;
		newSession.bossName = bossName;
		newSession.killTime = killTime;
		newSession.endTime = endTime;
		newSession.status = "ACTIVE";
		_activeSessions.put(sessionId, newSession);

		LOGGER.info("【競標系統】會話已添加到活躍列表 - SessionID: " + sessionId);

		// ── 事件③ Discord 競標開始通知（含道具清單）────────────────────────
		BossAuctionDiscordNotifier.sendAuctionStarted(sessionId, bossName, drops, (int) (_auctionDurationMs / 60000L));

		// 全服公告
		if (_announcementEnabled)
		{
			Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.ANNOUNCEMENT, "競標系統",
				"【BOSS競標】" + bossName + " 已被擊殺！掉落物品開始競標，持續 " + (_auctionDurationMs / 60000) + " 分鐘！"));
		}

		LOGGER.info("【競標系統】創建會話成功 - SessionID: " + sessionId + ", BOSS: " + bossName);
		return sessionId;
	}

	/**
	 * 玩家出價
	 */
	public BidResult placeBid(Player player, int auctionItemId, long bidAmount)
	{
		// 獲取物品資訊
		AuctionItem item = getAuctionItem(auctionItemId);
		if (item == null)
		{
			return new BidResult(false, "物品不存在");
		}

		// 檢查會話是否還在進行
		AuctionSession session = _activeSessions.get(item.sessionId);
		if (session == null || !"ACTIVE".equals(session.status))
		{
			return new BidResult(false, "競標已結束");
		}

		// 檢查時間
		long currentTime = System.currentTimeMillis();
		if (currentTime > session.endTime)
		{
			return new BidResult(false, "競標時間已過");
		}

		// 【新增】檢查出價冷卻
		Long lastBidTime = _playerLastBidTime.get(player.getObjectId());
		if (lastBidTime != null)
		{
			long timeSinceLastBid = currentTime - lastBidTime;
			if (timeSinceLastBid < _bidCooldownMs)
			{
				long remainingCooldown = (_bidCooldownMs - timeSinceLastBid) / 1000;
				return new BidResult(false, "出價冷卻中，請等待 " + remainingCooldown + " 秒");
			}
		}

		// 檢查出價是否高於當前價格
		if (bidAmount <= item.currentBid)
		{
			return new BidResult(false, "出價必須高於當前價格: " + item.currentBid);
		}

		// 檢查加價幅度
		if (item.currentBid > 0 && (bidAmount - item.currentBid) < _minBidIncrement)
		{
			return new BidResult(false, "加價幅度不得低於: " + _minBidIncrement);
		}

		// 檢查玩家是否有足夠的貨幣
		long playerCurrency = player.getInventory().getInventoryItemCount(_currencyItemId, -1);
		if (playerCurrency < bidAmount)
		{
			return new BidResult(false, "L Coin 不足，當前擁有: " + playerCurrency);
		}

		// 如果有前一個出價者，返還其貨幣
		if (item.currentBidderId > 0 && item.currentBid > 0)
		{
			String itemName = getItemName(item.itemId);
			String returnReason = "您對「" + itemName + "」的出價已被超越，出價金額已退還";
			returnCurrencyToPlayer(item.currentBidderId, item.currentBid, returnReason);
		}

		// 扣除玩家貨幣
		if (!player.destroyItemByItemId(ItemProcessType.DROP, _currencyItemId, bidAmount, player, false))
		{
			return new BidResult(false, "扣除貨幣失敗");
		}

		// 更新資料庫
		if (!BossAuctionDAO.updateItemBid(auctionItemId, bidAmount, player.getObjectId(), player.getName()))
		{
			// 如果更新失敗，返還貨幣
			player.addItem(ItemProcessType.DROP, _currencyItemId, bidAmount, player, false);
			return new BidResult(false, "出價失敗，請重試");
		}

		// 記錄出價
		BossAuctionDAO.recordBid(auctionItemId, item.sessionId, player.getObjectId(), player.getName(), bidAmount, currentTime);

		// 【新增】更新玩家最後出價時間
		_playerLastBidTime.put(player.getObjectId(), currentTime);

		// 【新增】檢查是否需要延長競標時間
		long timeRemaining = session.endTime - currentTime;
		if (timeRemaining <= _extensionTriggerTimeMs)
		{
			// 檢查是否還能延長
			int currentExtensions = _sessionExtensionCount.getOrDefault(item.sessionId, 0);
			if (currentExtensions < _maxExtensionCount)
			{
				// 延長時間
				long newEndTime = session.endTime + _extensionDurationMs;
				session.endTime = newEndTime;
				_sessionExtensionCount.put(item.sessionId, currentExtensions + 1);

				// 更新資料庫中的結束時間
				BossAuctionDAO.updateSessionEndTime(item.sessionId, newEndTime);

				// 廣播延長訊息
				if (_announcementEnabled)
				{
					int remainingExtensions = _maxExtensionCount - currentExtensions - 1;
					Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.ANNOUNCEMENT, "競標系統",
						"【BOSS競標】競標時間已延長 " + (_extensionDurationMs / 60000) + " 分鐘！(剩餘可延長次數: " + remainingExtensions + ")"));
				}

				LOGGER.info("【競標系統】會話 " + item.sessionId + " 時間已延長，當前延長次數: " + (currentExtensions + 1) + "/" + _maxExtensionCount);
			}
		}

		// 廣播訊息
		if (_announcementEnabled)
		{
			Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.ANNOUNCEMENT, "競標系統",
				"【BOSS競標】" + player.getName() + " 出價 " + bidAmount + " L Coin 競標物品！"));
		}

		return new BidResult(true, "出價成功");
	}

	/**
	 * 獲取競標物品
	 */
	private AuctionItem getAuctionItem(int auctionItemId)
	{
		return BossAuctionDAO.getAuctionItem(auctionItemId);
	}

	/**
	 * 返還貨幣給玩家
	 */
	private void returnCurrencyToPlayer(int playerId, long amount, String reason)
	{
		Player player = World.getInstance().getPlayer(playerId);
		if (player != null && player.isOnline())
		{
			player.addItem(ItemProcessType.DROP, _currencyItemId, amount, player, false);
			player.sendMessage("返還 " + amount + " L Coin (" + reason + ")");
		}
		else
		{
			// 離線玩家透過郵件發送
			sendMailToPlayer(playerId, "競標返還", reason, _currencyItemId, amount);
		}
	}

	/**
	 * 發送郵件給玩家
	 */
	private void sendMailToPlayer(int playerId, String title, String body, int itemId, long count)
	{
		Message msg = new Message(playerId, title, body, MailType.REGULAR);
		Mail attachments = msg.createAttachments();
		attachments.addItem(ItemProcessType.NONE, itemId, count, null, null);
		MailManager.getInstance().sendMessage(msg);
		LOGGER.info("【競標系統】已發送郵件給玩家 ID: " + playerId + "，標題: " + title + "，物品數量: " + count);
	}

	/**
	 * 發送通知郵件給玩家（不附帶物品）
	 */
	private void sendNotificationMail(int playerId, String title, String body)
	{
		Message msg = new Message(playerId, title, body, MailType.REGULAR);
		MailManager.getInstance().sendMessage(msg);
		LOGGER.info("【競標系統】已發送通知郵件給玩家 ID: " + playerId + "，標題: " + title);
	}

	/**
	 * 獲取物品名稱
	 */
	private String getItemName(int itemId)
	{
		try
		{
			return org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(itemId).getName();
		}
		catch (Exception e)
		{
			return "物品 ID: " + itemId;
		}
	}

	// 【Bug2修復】PROCESSING 超時重試閾值：超過此時間仍在 PROCESSING 視為卡死
	private static final long PROCESSING_TIMEOUT_MS = 10 * 60 * 1000L; // 10分鐘

	/**
	 * 定時檢查並處理過期的競標
	 */
	private void startAuctionCheckTask()
	{
		ThreadPool.scheduleAtFixedRate(() ->
		{
			try
			{
				// ── 事件④ Discord 競標結束倒數警告偵測 ──────────────────────
				// 每分鐘檢查一次，在剩餘時間 <= warnMs 且尚未發送過警告時觸發
				final int endWarnMinutes = DiscordConfig.DISCORD_BOSS_AUCTION_END_WARNING_MINUTES;
				if (endWarnMinutes > 0)
				{
					final long warnMs = endWarnMinutes * 60000L;
					final long now = System.currentTimeMillis();
					for (AuctionSession session : _activeSessions.values())
					{
						if (!"ACTIVE".equals(session.status))
						{
							continue;
						}
						final long remaining = session.endTime - now;
						if ((remaining > 0) && (remaining <= warnMs) && !_endWarningSentSessions.contains(session.sessionId))
						{
							BossAuctionDiscordNotifier.sendAuctionEndWarning(session.bossName, session.endTime);
							_endWarningSentSessions.add(session.sessionId);
						}
					}
				}

				// 處理正常到期的 ACTIVE 會話
				List<Integer> expiredSessions = BossAuctionDAO.getExpiredSessions();
				for (int sessionId : expiredSessions)
				{
					processExpiredAuction(sessionId);
				}

				// 【Bug2修復】重試卡死在 PROCESSING 超過 10 分鐘的會話
				List<Integer> stuckSessions = BossAuctionDAO.getStuckProcessingSessions(PROCESSING_TIMEOUT_MS);
				for (int sessionId : stuckSessions)
				{
					LOGGER.warning("【競標系統】會話 " + sessionId + " 卡在 PROCESSING 超過 10 分鐘，強制重置並重試");
					BossAuctionDAO.resetStuckSession(sessionId);
					processExpiredAuction(sessionId);
				}
			}
			catch (Exception e)
			{
				LOGGER.warning("【競標系統】檢查過期會話時發生錯誤: " + e.getMessage());
			}
		}, 60000, 60000); // 每分鐘檢查一次
	}

	/**
	 * 處理過期的競標
	 */
	private void processExpiredAuction(int sessionId)
	{
		// 先標記為處理中，避免重複處理
		synchronized (this)
		{
			if (!BossAuctionDAO.updateSessionStatus(sessionId, "PROCESSING"))
			{
				LOGGER.warning("【競標系統】會話 " + sessionId + " 已在處理中或已結束，跳過");
				return;
			}
		}

		LOGGER.info("【競標系統】處理過期會話: " + sessionId);

		try
		{
			// 獲取會話信息
			AuctionSession session = BossAuctionDAO.getSession(sessionId);
			if (session == null)
			{
				LOGGER.warning("【競標系統】會話 " + sessionId + " 不存在，標記為 ENDED");
				BossAuctionDAO.updateSessionStatus(sessionId, "ENDED");
				return;
			}

			// 獲取所有物品
			List<AuctionItem> items = BossAuctionDAO.getSessionItems(sessionId);
			long totalRevenue = 0;

			// ── 事件⑤ 結算資料收集（供 Discord 結算通知使用）──────────────
			final List<BossAuctionDiscordNotifier.ItemResult> soldItemResults = new ArrayList<>();
			final List<BossAuctionDiscordNotifier.ItemResult> unsoldItemResults = new ArrayList<>();

			for (AuctionItem item : items)
			{
				if (item.currentBidderId > 0 && item.currentBid > 0)
				{
					// 創建待領取獎勵 (得標物品)
					int rewardId = BossAuctionDAO.addPendingReward(
						item.currentBidderId,
						item.currentBidderName,
						sessionId,
						session.bossNpcId,
						session.bossName,
						"BID_WIN",
						item.itemId,
						item.itemCount,
						item.itemData,
						item.currentBid,
						0
					);

					if (rewardId > 0)
					{
						sendNotificationMail(
							item.currentBidderId,
							"【BOSS競標】得標通知",
							String.format("恭喜您以 %d L Coin 成功得標 %s 掉落的物品！\n\n請前往世界BOSS管理員處領取您的獎勵。",
								item.currentBid, session.bossName)
						);

						totalRevenue += item.currentBid;
						BossAuctionDAO.updateItemStatus(item.auctionItemId, "SOLD");

						// 收集已成交道具資料
						soldItemResults.add(new BossAuctionDiscordNotifier.ItemResult(
							item.itemId, item.enchantLevel, item.itemCount,
							item.currentBidderName, item.currentBid));

						LOGGER.info("【競標系統】物品 " + item.itemId + " 由 " + item.currentBidderName + " 得標，價格: " + item.currentBid + "，待領取ID: " + rewardId);
					}
				}
				else
				{
					BossAuctionDAO.updateItemStatus(item.auctionItemId, "CANCELLED");

					// 收集流標道具資料
					unsoldItemResults.add(new BossAuctionDiscordNotifier.ItemResult(
						item.itemId, item.enchantLevel, item.itemCount, null, 0));

					LOGGER.info("【競標系統】物品 " + item.itemId + " 流標");
				}
			}

			// ── 事件⑤ 預先抓取參與者（必須在 distributeRevenue 之前！）────────
			// distributeRevenue() 內部會呼叫 markParticipantRewarded()，
			// 將 reward_received 設為 1；之後再查 getQualifiedParticipants() 會回傳空列表。
			// 因此必須在 distributeRevenue 之前取得資料，供 Discord 結算通知使用。
			final List<BossAuctionDAO.Participant> discordParticipants =
				BossAuctionDAO.getQualifiedParticipants(sessionId, _minDamageRequired);

			LOGGER.info("【競標系統】達標參與者人數：" + discordParticipants.size() + "（傷害門檻 " + _minDamageRequired + "）");

			// 分配收益給參與者
			if (totalRevenue > 0)
			{
				distributeRevenue(sessionId, session, totalRevenue);
			}
			else
			{
				LOGGER.info("【競標系統】會話 " + sessionId + " 無收益（無人出價或物品全數流標），跳過分紅");
			}

			// ── 事件⑤ Discord 競標結算彙整報告（使用已預先抓取的參與者資料）──
			try
			{
				final List<BossAuctionDiscordNotifier.ParticipantInfo> participantInfos = new ArrayList<>();
				for (BossAuctionDAO.Participant p : discordParticipants)
				{
					participantInfos.add(new BossAuctionDiscordNotifier.ParticipantInfo(p.playerName, p.damageDealt));
				}

				final int qualifiedCount = discordParticipants.size();
				// distributeRevenue() 實際分配 100% 收益（totalRevenue / participants.size()）
				// Discord 通知顯示需與 distributeRevenue() 內部邏輯一致
				final long rewardPerPlayer = (qualifiedCount > 0) ? (totalRevenue / qualifiedCount) : 0L;

				BossAuctionDiscordNotifier.sendAuctionSettlement(
					session.bossName,
					soldItemResults,
					unsoldItemResults,
					totalRevenue,
					totalRevenue,  // distributedRevenue == totalRevenue（100% 分配）
					qualifiedCount,
					rewardPerPlayer,
					_minDamageRequired,
					participantInfos);
			}
			catch (Exception discordEx)
			{
				// Discord 通知失敗不影響主流程
				LOGGER.warning("【競標系統】發送 Discord 結算通知時發生錯誤（不影響結算）：" + discordEx.getMessage());
			}

			// 正常結束：更新會話狀態
			BossAuctionDAO.updateSessionStatus(sessionId, "ENDED");
			_activeSessions.remove(sessionId);
			_damageTracker.remove(sessionId);
			_endWarningSentSessions.remove(sessionId); // 清理倒數警告記錄

			LOGGER.info("【競標系統】會話 " + sessionId + " 處理完成，總收益: " + totalRevenue);
		}
		catch (Exception e)
		{
			// 【Bug2修復】任何例外都標記為 ENDED 而非讓 session 卡在 PROCESSING
			LOGGER.severe("【競標系統】處理會話 " + sessionId + " 時發生嚴重錯誤，標記為 ENDED: " + e.getMessage());
			e.printStackTrace();
			BossAuctionDAO.updateSessionStatus(sessionId, "ENDED");
			_activeSessions.remove(sessionId);
			_damageTracker.remove(sessionId);
			_endWarningSentSessions.remove(sessionId); // 清理倒數警告記錄
		}
	}

	/**
	 * 分配收益給參與擊殺的玩家
	 */
	private void distributeRevenue(int sessionId, AuctionSession session, long totalRevenue)
	{
		List<Participant> participants = BossAuctionDAO.getQualifiedParticipants(sessionId, _minDamageRequired);

		if (participants.isEmpty())
		{
			LOGGER.warning("【競標系統】沒有合格的參與者，收益無法分配");
			return;
		}

		long rewardPerPlayer = totalRevenue / participants.size();

		for (Participant p : participants)
		{
			// 創建待領取獎勵 (分紅)
			int rewardId = BossAuctionDAO.addPendingReward(
				p.playerId,
				p.playerName,
				sessionId,
				session.bossNpcId,
				session.bossName,
				"REVENUE_SHARE",
				_currencyItemId,
				rewardPerPlayer,
				null,
				0,
				p.damageDealt
			);

			if (rewardId > 0)
			{
				// 發送通知郵件（不附帶物品）
				sendNotificationMail(
					p.playerId,
					"【BOSS競標】分紅通知",
					String.format("您參與擊殺 %s 並造成 %d 點傷害，獲得競標分紅 %d L Coin！\n\n請前往世界BOSS管理員處領取您的獎勵。",
						session.bossName, p.damageDealt, rewardPerPlayer)
				);

				BossAuctionDAO.markParticipantRewarded(p.participantId);

				LOGGER.info("【競標系統】分配 " + rewardPerPlayer + " L Coin 給 " + p.playerName + "，待領取ID: " + rewardId);
			}
		}
	}

	/**
	 * 獲取活躍的會話列表
	 */
	public List<AuctionSession> getActiveSessions()
	{
		// 從資料庫讀取最新數據
		List<AuctionSession> sessions = BossAuctionDAO.getActiveSessions();

		// 【Bug4修復】改為 merge 更新，不清空整個 Map 以免干擾 processExpiredAuction
		Set<Integer> dbSessionIds = new HashSet<>();
		for (AuctionSession session : sessions)
		{
			_activeSessions.put(session.sessionId, session);
			dbSessionIds.add(session.sessionId);
		}
		// 移除 DB 中已不存在的 session（已 ENDED 的）
		_activeSessions.keySet().removeIf(id -> !dbSessionIds.contains(id));

		return sessions;
	}

	/**
	 * 獲取會話的物品列表
	 */
	public List<AuctionItem> getSessionItems(int sessionId)
	{
		return BossAuctionDAO.getSessionItems(sessionId);
	}

	/**
	 * 獲取傷害追蹤器（供內部使用）
	 */
	public Map<Integer, Map<Integer, DamageInfo>> getDamageTracker()
	{
		return _damageTracker;
	}

	/**
	 * 獲取玩家待領取的獎勵列表
	 */
	public List<BossAuctionDAO.PendingReward> getPlayerPendingRewards(int playerId)
	{
		return BossAuctionDAO.getPlayerPendingRewards(playerId);
	}

	/**
	 * 玩家領取獎勵
	 */
	public boolean claimReward(Player player, int rewardId)
	{
		// 獲取獎勵信息
		List<BossAuctionDAO.PendingReward> rewards = BossAuctionDAO.getPlayerPendingRewards(player.getObjectId());
		BossAuctionDAO.PendingReward reward = null;

		for (BossAuctionDAO.PendingReward r : rewards)
		{
			if (r.rewardId == rewardId)
			{
				reward = r;
				break;
			}
		}

		if (reward == null)
		{
			player.sendMessage("獎勵不存在或已被領取。");
			return false;
		}

		// 檢查背包空間
		if (player.getInventory().getSize() >= player.getInventoryLimit() - 10)
		{
			player.sendMessage("背包空間不足，請清理後再領取。");
			return false;
		}

		// 發放獎勵
		player.addItem(ItemProcessType.NONE, reward.itemId, reward.itemCount, player, true);

		// 標記為已領取
		if (BossAuctionDAO.claimReward(rewardId))
		{
			String rewardTypeName = "BID_WIN".equals(reward.rewardType) ? "競標獎勵" : "擊殺分紅";
			player.sendMessage(String.format("成功領取 %s 的%s！", reward.bossName, rewardTypeName));
			LOGGER.info("【競標系統】玩家 " + player.getName() + " 領取獎勵 ID: " + rewardId);
			return true;
		}
		else
		{
			// 領取失敗，回收物品
			player.destroyItemByItemId(ItemProcessType.NONE, reward.itemId, reward.itemCount, player, false);
			player.sendMessage("領取失敗，請重試。");
			return false;
		}
	}

	/**
	 * 獲取玩家待領取獎勵數量
	 */
	public int getPlayerPendingRewardCount(int playerId)
	{
		return BossAuctionDAO.getPlayerPendingRewardCount(playerId);
	}

	/**
	 * 【新增】獲取會話當前的延長次數
	 */
	public int getSessionExtensionCount(int sessionId)
	{
		return _sessionExtensionCount.getOrDefault(sessionId, 0);
	}

	// ==================== 輔助類別 ====================

	public static class DropItem
	{
		public int itemId;
		public long count;
		public int enchantLevel;
		public String itemData;

		public DropItem(int itemId, long count, int enchantLevel, String itemData)
		{
			this.itemId = itemId;
			this.count = count;
			this.enchantLevel = enchantLevel;
			this.itemData = itemData;
		}
	}

	public static class DamageInfo
	{
		public int playerId;
		public String playerName;
		public long totalDamage;

		public DamageInfo(int playerId, String playerName)
		{
			this.playerId = playerId;
			this.playerName = playerName;
			this.totalDamage = 0;
		}

		public void addDamage(long damage)
		{
			this.totalDamage += damage;
		}
	}

	public static class BidResult
	{
		public boolean success;
		public String message;

		public BidResult(boolean success, String message)
		{
			this.success = success;
			this.message = message;
		}
	}
}
