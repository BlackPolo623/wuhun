package custom.BossAuctionSystem;

import java.awt.Color;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.l2jmobius.discord.DiscordManager;
import org.l2jmobius.gameserver.config.custom.DiscordConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.item.ItemTemplate;

import net.dv8tion.jda.api.EmbedBuilder;

/**
 * Boss 競標 Discord Embed 通知集中管理器。
 *
 * 共管理 5 個通知事件：
 *   事件① sendSpawnWarning      — 世界首領重生預警     （金黃色，@everyone）
 *   事件② sendBossSpawned       — 世界首領降臨         （深紅色，@everyone）
 *   事件③ sendAuctionStarted    — 競標開始 + 道具清單  （橙色）
 *   事件④ sendAuctionEndWarning — 競標倒數警告         （亮紅色，@everyone）
 *   事件⑤ sendAuctionSettlement — 競標結算彙整報告     （綠色）
 *
 * 所有方法均為靜態，呼叫前先做 isReady() 檢查：
 *   - Discord Bot 停用 → 靜默跳過
 *   - 頻道 ID 未設定  → 靜默跳過
 *
 * @author Custom
 */
public class BossAuctionDiscordNotifier
{
	private static final Logger LOGGER = Logger.getLogger(BossAuctionDiscordNotifier.class.getName());

	// ── Embed 顏色常數 ────────────────────────────────────────────────────────
	private static final Color COLOR_SPAWN_WARNING = new Color(0xFF, 0xC1, 0x07); // 事件① 金黃 #FFC107
	private static final Color COLOR_BOSS_SPAWNED  = new Color(0xE5, 0x39, 0x35); // 事件② 深紅 #E53935
	private static final Color COLOR_AUCTION_START = new Color(0xFF, 0x6D, 0x00); // 事件③ 橙色 #FF6D00
	private static final Color COLOR_END_WARNING   = new Color(0xF4, 0x43, 0x36); // 事件④ 亮紅 #F44336
	private static final Color COLOR_SETTLEMENT    = new Color(0x43, 0xA0, 0x47); // 事件⑤ 綠色 #43A047

	// ── 時間格式 ─────────────────────────────────────────────────────────────
	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter FULL_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

	// Discord Embed field value 最大長度
	private static final int EMBED_FIELD_LIMIT = 1000;

	// ── 事件① 世界首領重生預警 ──────────────────────────────────────────────

	/**
	 * 發送世界首領重生預警（@everyone）。
	 * 在 scheduleNextSpawn() 中排程，於 spawnTime - warningMinutes 觸發。
	 *
	 * @param bossName   首領名稱（重生前尚未確定時傳 "世界首領"）
	 * @param spawnTimeMs 預計重生時間（毫秒 epoch）
	 */
	public static void sendSpawnWarning(String bossName, long spawnTimeMs)
	{
		if (!isReady())
		{
			return;
		}

		final int warnMinutes = DiscordConfig.DISCORD_BOSS_SPAWN_WARNING_MINUTES;
		final LocalDateTime spawnTime = toLocalDateTime(spawnTimeMs);
		final int lifetime = WorldBossConfig.getBossLifetime();

		final EmbedBuilder embed = new EmbedBuilder()
			.setColor(COLOR_SPAWN_WARNING)
			.setTitle("⚔️  世界首領即將降臨！")
			.setDescription("🐉 **" + bossName + "** 將於 **" + warnMinutes + " 分鐘後** 在重生點現身！\n📍 前往世界首領重生點備戰！")
			.addField("⏰ 預計降臨時間", TIME_FMT.format(spawnTime), true)
			.addField("⚠️ 首領存活時間", lifetime + " 分鐘（逾時自動消失）", true)
			.setFooter("武魂天堂2")
			.setTimestamp(Instant.now());

		sendToChannel(embed, true);
	}

	// ── 事件② 世界首領降臨 ──────────────────────────────────────────────────

	/**
	 * 發送世界首領降臨通知（@everyone）。
	 * 在 spawnWorldBoss() 成功召喚後立即呼叫。
	 *
	 * @param bossName       已確定的首領名稱
	 * @param lifetimeMinutes 首領存活時間（分鐘）
	 */
	public static void sendBossSpawned(String bossName, int lifetimeMinutes)
	{
		if (!isReady())
		{
			return;
		}

		final LocalDateTime despawnTime = LocalDateTime.now().plusMinutes(lifetimeMinutes);

		final EmbedBuilder embed = new EmbedBuilder()
			.setColor(COLOR_BOSS_SPAWNED)
			.setTitle("🔥  世界首領已降臨！")
			.setDescription("冒險者們！決戰時刻到來！")
			.addField("🐉 首領名稱", bossName, true)
			.addField("⏰ 消失時間", lifetimeMinutes + " 分鐘後（" + TIME_FMT.format(despawnTime) + " 前）", true)
			.addField("", "💡 擊殺可參與競標分潤，達到傷害門檻即可瓜分收益！", false)
			.setFooter("武魂天堂2")
			.setTimestamp(Instant.now());

		sendToChannel(embed, true);
	}

	// ── 事件③ 競標開始（含道具清單）─────────────────────────────────────────

	/**
	 * 發送競標開始通知，包含本次競標道具清單。
	 * 在 createAuctionSession() 成功後呼叫。
	 *
	 * @param sessionId       競標 Session ID（供日誌追蹤）
	 * @param bossName        被擊殺的首領名稱
	 * @param drops           本次掉落道具列表
	 * @param durationMinutes 競標時長（分鐘）
	 */
	public static void sendAuctionStarted(int sessionId, String bossName, List<BossAuctionManager.DropItem> drops, int durationMinutes)
	{
		if (!isReady())
		{
			return;
		}

		final LocalDateTime endTime = LocalDateTime.now().plusMinutes(durationMinutes);

		// 組裝道具清單字串
		final StringBuilder itemList = new StringBuilder();
		for (BossAuctionManager.DropItem drop : drops)
		{
			itemList.append(formatItemName(drop.itemId, drop.enchantLevel, drop.count)).append("\n");
		}
		final String itemListStr = (itemList.length() > 0) ? itemList.toString().trim() : "（無掉落物品）";

		final EmbedBuilder embed = new EmbedBuilder()
			.setColor(COLOR_AUCTION_START)
			.setTitle("💀  " + bossName + " 已被擊殺！競標開始！")
			.addField("🏆 本次競標道具清單", itemListStr, false)
			.addField("⏰ 競標時間", durationMinutes + " 分鐘（至 " + TIME_FMT.format(endTime) + " 截止）", true)
			.addField("💰 競標貨幣", "L幣", true)
			.addField("", "📌 前往遊戲內 **競標管理員** NPC 進行出價", false)
			.setFooter("武魂天堂2")
			.setTimestamp(Instant.now());

		sendToChannel(embed, false);
		LOGGER.info("BossAuctionDiscordNotifier: [事件③] 競標開始通知已發送 - SessionID: " + sessionId + ", Boss: " + bossName);
	}

	// ── 事件④ 競標結束倒數警告 ──────────────────────────────────────────────

	/**
	 * 發送競標結束倒數警告（@everyone）。
	 * 由 startAuctionCheckTask() 60秒輪詢偵測剩餘時間後呼叫。
	 *
	 * @param bossName  首領名稱
	 * @param endTimeMs 競標結束時間（毫秒 epoch）
	 */
	public static void sendAuctionEndWarning(String bossName, long endTimeMs)
	{
		if (!isReady())
		{
			return;
		}

		final int warnMinutes = DiscordConfig.DISCORD_BOSS_AUCTION_END_WARNING_MINUTES;
		final LocalDateTime endTime = toLocalDateTime(endTimeMs);

		final EmbedBuilder embed = new EmbedBuilder()
			.setColor(COLOR_END_WARNING)
			.setTitle("⏰  競標即將結束！")
			.setDescription("🐉 **" + bossName + "** 的競標將於 **" + warnMinutes + " 分鐘後** 結束！")
			.addField("⏳ 剩餘時間", warnMinutes + " 分鐘", true)
			.addField("🕐 截止時間", TIME_FMT.format(endTime), true)
			.addField("", "🏃 還沒出價的趕快！前往 **競標管理員** NPC", false)
			.setFooter("武魂天堂2")
			.setTimestamp(Instant.now());

		sendToChannel(embed, true);
		LOGGER.info("BossAuctionDiscordNotifier: [事件④] 競標倒數警告已發送 - Boss: " + bossName);
	}

	// ── 事件⑤ 競標結算彙整報告 ──────────────────────────────────────────────

	/**
	 * 發送競標結算彙整報告（包含得標結果、分潤明細、達標玩家）。
	 * 在 processExpiredAuction() 完整結算後呼叫。
	 *
	 * @param bossName          首領名稱
	 * @param soldItems         已成交的道具清單（含得標者資訊）
	 * @param unsoldItems       流標的道具清單
	 * @param totalRevenue      本次競標總收益（L幣）
	 * @param distributedRevenue 實際用於分潤的金額（已套用分潤比例）
	 * @param qualifiedCount    達標參與人數
	 * @param rewardPerPlayer   每位達標玩家分到的 L幣
	 * @param minDamage         傷害門檻
	 * @param participants      達標玩家列表（含傷害數字）
	 */
	public static void sendAuctionSettlement(
		String bossName,
		List<ItemResult> soldItems,
		List<ItemResult> unsoldItems,
		long totalRevenue,
		long distributedRevenue,
		int qualifiedCount,
		long rewardPerPlayer,
		long minDamage,
		List<ParticipantInfo> participants)
	{
		if (!isReady())
		{
			return;
		}

		final EmbedBuilder embed = new EmbedBuilder()
			.setColor(COLOR_SETTLEMENT)
			.setTitle("📊  " + bossName + " 競標結算完成！");

		// 得標結果
		final StringBuilder resultStr = new StringBuilder();
		for (ItemResult item : soldItems)
		{
			resultStr.append(formatItemName(item.itemId, item.enchantLevel, item.count))
				.append("  →  **").append(item.winnerName).append("**（")
				.append(formatNumber(item.finalBid)).append(" L幣）\n");
		}
		for (ItemResult item : unsoldItems)
		{
			resultStr.append(formatItemName(item.itemId, item.enchantLevel, item.count))
				.append("  →  ❌ 流標（無人出價）\n");
		}
		if (resultStr.length() > 0)
		{
			embed.addField("🏆 得標結果", truncate(resultStr.toString().trim()), false);
		}
		else
		{
			embed.addField("🏆 得標結果", "（本次無競標物品）", false);
		}

		// 分潤結算
		if (totalRevenue > 0)
		{
			final String revenueStr =
				"總競標收益：" + formatNumber(totalRevenue) + " L幣\n" +
				"分潤金額：" + formatNumber(distributedRevenue) + " L幣\n" +
				"達標人數：" + qualifiedCount + " 人\n" +
				"每人分潤：" + formatNumber(rewardPerPlayer) + " L幣";
			embed.addField("💰 分潤結算", revenueStr, false);
		}
		else
		{
			embed.addField("💰 分潤結算", "本次無競標收益（全部流標或無人出價）", false);
		}

		// 達標玩家列表
		if (!participants.isEmpty())
		{
			final boolean showDamage = DiscordConfig.DISCORD_BOSS_SHOW_DAMAGE_IN_REPORT;
			final StringBuilder playerStr = new StringBuilder();

			if (showDamage)
			{
				// 每人一行，顯示傷害數字
				for (ParticipantInfo p : participants)
				{
					playerStr.append("**").append(p.playerName).append("**  ")
						.append(formatNumber(p.damageDealt)).append(" 傷害\n");
				}
			}
			else
			{
				// 橫排顯示，每 5 人換行
				for (int i = 0; i < participants.size(); i++)
				{
					playerStr.append("**").append(participants.get(i).playerName).append("**");
					if (i < (participants.size() - 1))
					{
						playerStr.append("  /  ");
					}
					if (((i + 1) % 5) == 0)
					{
						playerStr.append("\n");
					}
				}
			}

			final String fieldTitle = "✅ 達標玩家（傷害門檻：" + formatNumber(minDamage) + "）";
			embed.addField(fieldTitle, truncate(playerStr.toString().trim()), false);
		}
		else
		{
			embed.addField("✅ 達標玩家", "本次無達標玩家（無人造成足夠傷害）", false);
		}

		embed.addField("", "📌 前往 **世界首領管理員** NPC 領取您的獎勵", false)
			.setFooter("武魂天堂2")
			.setTimestamp(Instant.now());

		sendToChannel(embed, false);
		LOGGER.info("BossAuctionDiscordNotifier: [事件⑤] 結算報告已發送 - Boss: " + bossName +
			"，收益: " + formatNumber(totalRevenue) + " L幣，達標人數: " + qualifiedCount);
	}

	// ── 內部輔助方法 ──────────────────────────────────────────────────────────

	/**
	 * 檢查是否已啟用並就緒，任一條件不符則靜默跳過。
	 */
	private static boolean isReady()
	{
		if (!DiscordConfig.DISCORD_BOSS_NOTIFY_ENABLED)
		{
			return false;
		}
		if ((DiscordConfig.DISCORD_BOSS_NOTIFY_CHANNEL_ID == null) || DiscordConfig.DISCORD_BOSS_NOTIFY_CHANNEL_ID.isEmpty())
		{
			return false;
		}
		return DiscordManager.getInstance().isRunning();
	}

	/**
	 * 將 EmbedBuilder 送至 Boss 通知頻道。
	 *
	 * @param embed   已組裝的 EmbedBuilder
	 * @param mention 是否附加 @everyone（受 Config 開關控制）
	 */
	private static void sendToChannel(EmbedBuilder embed, boolean mention)
	{
		final String channelId = DiscordConfig.DISCORD_BOSS_NOTIFY_CHANNEL_ID;
		final String mentionText = (mention && DiscordConfig.DISCORD_BOSS_NOTIFY_MENTION_EVERYONE) ? "@everyone" : null;
		DiscordManager.getInstance().sendChannelEmbed(channelId, mentionText, embed.build());
	}

	/**
	 * 將 epoch 毫秒轉換為本地時間 LocalDateTime。
	 */
	private static LocalDateTime toLocalDateTime(long epochMs)
	{
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
	}

	/**
	 * 格式化道具名稱，加入強化等級與數量。
	 * 例：enchant > 0 → "暗影之劍 +10"，count > 1 → "最高藥水 ×10"
	 */
	private static String formatItemName(int itemId, int enchantLevel, long count)
	{
		String name;
		try
		{
			final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
			name = (template != null) ? template.getName() : ("物品 #" + itemId);
		}
		catch (Exception e)
		{
			name = "物品 #" + itemId;
		}

		final StringBuilder sb = new StringBuilder();

		// 道具圖示（簡易分類）
		if (enchantLevel > 0)
		{
			sb.append("⚔️ ");
		}
		else if (count > 1)
		{
			sb.append("💊 ");
		}
		else
		{
			sb.append("🔹 ");
		}

		sb.append(name);

		if (enchantLevel > 0)
		{
			sb.append(" +").append(enchantLevel);
		}
		if (count > 1)
		{
			sb.append(" ×").append(formatNumber(count));
		}

		return sb.toString();
	}

	/**
	 * 千分位格式化數字。
	 * 例：1234567 → "1,234,567"
	 */
	private static String formatNumber(long value)
	{
		return NumberFormat.getNumberInstance(Locale.US).format(value);
	}

	/**
	 * 截斷超過 Discord Embed field value 上限的字串，末尾加 "..."。
	 */
	private static String truncate(String text)
	{
		if ((text == null) || (text.length() <= EMBED_FIELD_LIMIT))
		{
			return text;
		}
		return text.substring(0, EMBED_FIELD_LIMIT - 3) + "...";
	}

	// ── 資料模型（供 BossAuctionManager 組裝結算資料用）───────────────────────

	/**
	 * 競標道具結果資料模型。
	 * winnerName == null 且 finalBid == 0 表示流標。
	 */
	public static class ItemResult
	{
		public final int itemId;
		public final int enchantLevel;
		public final long count;
		public final String winnerName; // null = 流標
		public final long finalBid;     // 0 = 流標

		public ItemResult(int itemId, int enchantLevel, long count, String winnerName, long finalBid)
		{
			this.itemId = itemId;
			this.enchantLevel = enchantLevel;
			this.count = count;
			this.winnerName = winnerName;
			this.finalBid = finalBid;
		}
	}

	/**
	 * 競標參與者資料模型（達標玩家）。
	 */
	public static class ParticipantInfo
	{
		public final String playerName;
		public final long damageDealt;

		public ParticipantInfo(String playerName, long damageDealt)
		{
			this.playerName = playerName;
			this.damageDealt = damageDealt;
		}
	}
}
