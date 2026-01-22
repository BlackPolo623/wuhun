package custom.BossAuctionSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Boss Auction System Configuration Reader
 * <p>
 * Reads configuration from BossAuction.ini file
 * Replaces database config table with a simple INI file
 * </p>
 * @author 黑普羅
 */
public class BossAuctionConfig
{
	private static final Logger LOGGER = Logger.getLogger(BossAuctionConfig.class.getName());

	// Configuration file path
	private static final String CONFIG_FILE = "data/scripts/custom/BossAuctionSystem/BossAuction.ini";

	// Configuration values
	private static Set<Integer> ENABLED_BOSS_IDS = new HashSet<>();
	private static int AUCTION_DURATION_HOURS = 2;
	private static long MIN_DAMAGE_REQUIRED = 200000000L;
	private static int CURRENCY_ITEM_ID = 91663;
	private static long MIN_BID_INCREMENT = 5000L;
	private static boolean ANNOUNCEMENT_ENABLED = true;
	private static long DEFAULT_STARTING_PRICE = 10000L;
	private static double REWARD_DISTRIBUTION_RATIO = 0.8;
	private static int AUCTION_NPC_ID = 900029;
	private static boolean DEBUG_MODE = false;
	private static int AUTO_CLEANUP_DAYS = 7;
	private static String MAIL_TITLE_WIN = "[競標系統] 恭喜得標";
	private static String MAIL_TITLE_REWARD = "[競標系統] 參與分紅";
	private static String MAIL_TITLE_REFUND = "[競標系統] 競標返還";
	private static String MAIL_SENDER_NAME = "競標系統";

	// 【新增】延長時間機制配置
	private static int EXTENSION_TRIGGER_MINUTES = 5; // 最後N分鐘內出價會觸發延長
	private static int EXTENSION_DURATION_MINUTES = 5; // 每次延長N分鐘
	private static int MAX_EXTENSION_COUNT = 3; // 最多延長N次

	// 【新增】出價冷卻配置
	private static int BID_COOLDOWN_SECONDS = 30; // 出價冷卻N秒

	// Boss-specific settings
	private static Map<Integer, Integer> BOSS_SPECIFIC_DURATION = new HashMap<>();

	/**
	 * Load configuration from INI file
	 */
	public static void load()
	{
		LOGGER.info("========================================");
		LOGGER.info("【競標系統】載入配置文件...");

		Properties properties = new Properties();
		File configFile = new File(CONFIG_FILE);

		// Check if config file exists
		if (!configFile.exists())
		{
			LOGGER.warning("【競標系統】配置文件不存在: " + CONFIG_FILE);
			LOGGER.warning("【競標系統】使用預設配置");
			loadDefaults();
			return;
		}

		try (InputStream input = new FileInputStream(configFile))
		{
			properties.load(input);

			// Parse enabled boss IDs
			String bossIdsStr = properties.getProperty("EnabledBossIds", "29001,29006,29014");
			ENABLED_BOSS_IDS.clear();
			for (String idStr : bossIdsStr.split(","))
			{
				try
				{
					int bossId = Integer.parseInt(idStr.trim());
					ENABLED_BOSS_IDS.add(bossId);
				}
				catch (NumberFormatException e)
				{
					LOGGER.warning("【競標系統】無效的 BOSS ID: " + idStr);
				}
			}

			// Parse other settings
			AUCTION_DURATION_HOURS = parseInt(properties, "AuctionDurationHours", 2);
			MIN_DAMAGE_REQUIRED = parseLong(properties, "MinDamageRequired", 200000000L);
			CURRENCY_ITEM_ID = parseInt(properties, "CurrencyItemId", 91663);
			MIN_BID_INCREMENT = parseLong(properties, "MinBidIncrement", 5000L);
			ANNOUNCEMENT_ENABLED = parseBoolean(properties, "AnnouncementEnabled", true);
			DEFAULT_STARTING_PRICE = parseLong(properties, "DefaultStartingPrice", 10000L);
			REWARD_DISTRIBUTION_RATIO = parseDouble(properties, "RewardDistributionRatio", 0.8);
			AUCTION_NPC_ID = parseInt(properties, "AuctionNpcId", 900029);
			DEBUG_MODE = parseBoolean(properties, "DebugMode", false);
			AUTO_CLEANUP_DAYS = parseInt(properties, "AutoCleanupDays", 7);
			MAIL_TITLE_WIN = properties.getProperty("MailTitleWin", "[競標系統] 恭喜得標");
			MAIL_TITLE_REWARD = properties.getProperty("MailTitleReward", "[競標系統] 參與分紅");
			MAIL_TITLE_REFUND = properties.getProperty("MailTitleRefund", "[競標系統] 競標返還");
			MAIL_SENDER_NAME = properties.getProperty("MailSenderName", "競標系統");

			// 【新增】載入延長時間和冷卻配置
			EXTENSION_TRIGGER_MINUTES = parseInt(properties, "ExtensionTriggerMinutes", 5);
			EXTENSION_DURATION_MINUTES = parseInt(properties, "ExtensionDurationMinutes", 5);
			MAX_EXTENSION_COUNT = parseInt(properties, "MaxExtensionCount", 3);
			BID_COOLDOWN_SECONDS = parseInt(properties, "BidCooldownSeconds", 30);

			// Parse boss-specific durations
			BOSS_SPECIFIC_DURATION.clear();
			for (String key : properties.stringPropertyNames())
			{
				if (key.startsWith("Boss.") && key.endsWith(".Duration"))
				{
					try
					{
						// Extract boss ID from key: Boss.29001.Duration
						String bossIdStr = key.substring(5, key.indexOf(".Duration"));
						int bossId = Integer.parseInt(bossIdStr);
						int duration = parseInt(properties, key, AUCTION_DURATION_HOURS);
						BOSS_SPECIFIC_DURATION.put(bossId, duration);
					}
					catch (Exception e)
					{
						LOGGER.warning("【競標系統】無效的 BOSS 特殊設置: " + key);
					}
				}
			}

			LOGGER.info("【競標系統】配置文件載入完成");
			LOGGER.info("【啟用 BOSS 數量】" + ENABLED_BOSS_IDS.size());
			LOGGER.info("【競標時長】" + AUCTION_DURATION_HOURS + " 小時");
			LOGGER.info("【貨幣物品 ID】" + CURRENCY_ITEM_ID);
			LOGGER.info("【最低加價】" + MIN_BID_INCREMENT);
			LOGGER.info("【全服公告】" + (ANNOUNCEMENT_ENABLED ? "啟用" : "停用"));
			LOGGER.info("【延長機制】觸發時間: " + EXTENSION_TRIGGER_MINUTES + "分鐘, 延長時間: " + EXTENSION_DURATION_MINUTES + "分鐘, 最多延長: " + MAX_EXTENSION_COUNT + "次");
			LOGGER.info("【出價冷卻】" + BID_COOLDOWN_SECONDS + " 秒");
			if (!BOSS_SPECIFIC_DURATION.isEmpty())
			{
				LOGGER.info("【特殊設置 BOSS 數量】" + BOSS_SPECIFIC_DURATION.size());
			}
			LOGGER.info("========================================");
		}
		catch (Exception e)
		{
			LOGGER.severe("【競標系統】載入配置文件時發生錯誤: " + e.getMessage());
			e.printStackTrace();
			LOGGER.warning("【競標系統】使用預設配置");
			loadDefaults();
		}
	}

	/**
	 * Load default configuration
	 */
	private static void loadDefaults()
	{
		ENABLED_BOSS_IDS.clear();
		ENABLED_BOSS_IDS.add(29001); // Queen Ant
		ENABLED_BOSS_IDS.add(29006); // Core
		ENABLED_BOSS_IDS.add(29014); // Orfen

		AUCTION_DURATION_HOURS = 2;
		MIN_DAMAGE_REQUIRED = 200000000L;
		CURRENCY_ITEM_ID = 91663;
		MIN_BID_INCREMENT = 5000L;
		ANNOUNCEMENT_ENABLED = true;
		DEFAULT_STARTING_PRICE = 10000L;
		REWARD_DISTRIBUTION_RATIO = 0.8;
		AUCTION_NPC_ID = 900029;
		DEBUG_MODE = false;

		LOGGER.info("【競標系統】預設配置載入完成");
	}

	// Parse helper methods
	private static int parseInt(Properties props, String key, int defaultValue)
	{
		try
		{
			return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)).trim());
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning("【競標系統】無效的數值設定 " + key + "，使用預設值: " + defaultValue);
			return defaultValue;
		}
	}

	private static long parseLong(Properties props, String key, long defaultValue)
	{
		try
		{
			return Long.parseLong(props.getProperty(key, String.valueOf(defaultValue)).trim());
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning("【競標系統】無效的數值設定 " + key + "，使用預設值: " + defaultValue);
			return defaultValue;
		}
	}

	private static double parseDouble(Properties props, String key, double defaultValue)
	{
		try
		{
			return Double.parseDouble(props.getProperty(key, String.valueOf(defaultValue)).trim());
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning("【競標系統】無效的數值設定 " + key + "，使用預設值: " + defaultValue);
			return defaultValue;
		}
	}

	private static boolean parseBoolean(Properties props, String key, boolean defaultValue)
	{
		String value = props.getProperty(key, String.valueOf(defaultValue)).trim().toLowerCase();
		return value.equals("true") || value.equals("1") || value.equals("yes");
	}

	// Getters
	public static Set<Integer> getEnabledBossIds()
	{
		return new HashSet<>(ENABLED_BOSS_IDS);
	}

	public static boolean isBossEnabled(int bossId)
	{
		return ENABLED_BOSS_IDS.contains(bossId);
	}

	public static int getAuctionDurationHours()
	{
		return AUCTION_DURATION_HOURS;
	}

	public static int getAuctionDurationHours(int bossId)
	{
		return BOSS_SPECIFIC_DURATION.getOrDefault(bossId, AUCTION_DURATION_HOURS);
	}

	public static long getMinDamageRequired()
	{
		return MIN_DAMAGE_REQUIRED;
	}

	public static int getCurrencyItemId()
	{
		return CURRENCY_ITEM_ID;
	}

	public static long getMinBidIncrement()
	{
		return MIN_BID_INCREMENT;
	}

	public static boolean isAnnouncementEnabled()
	{
		return ANNOUNCEMENT_ENABLED;
	}

	public static long getDefaultStartingPrice()
	{
		return DEFAULT_STARTING_PRICE;
	}

	public static double getRewardDistributionRatio()
	{
		return REWARD_DISTRIBUTION_RATIO;
	}

	public static int getAuctionNpcId()
	{
		return AUCTION_NPC_ID;
	}

	public static boolean isDebugMode()
	{
		return DEBUG_MODE;
	}

	public static int getAutoCleanupDays()
	{
		return AUTO_CLEANUP_DAYS;
	}

	public static String getMailTitleWin()
	{
		return MAIL_TITLE_WIN;
	}

	public static String getMailTitleReward()
	{
		return MAIL_TITLE_REWARD;
	}

	public static String getMailTitleRefund()
	{
		return MAIL_TITLE_REFUND;
	}

	public static String getMailSenderName()
	{
		return MAIL_SENDER_NAME;
	}

	// 【新增】延長時間和冷卻相關 Getter
	public static int getExtensionTriggerMinutes()
	{
		return EXTENSION_TRIGGER_MINUTES;
	}

	public static int getExtensionDurationMinutes()
	{
		return EXTENSION_DURATION_MINUTES;
	}

	public static int getMaxExtensionCount()
	{
		return MAX_EXTENSION_COUNT;
	}

	public static int getBidCooldownSeconds()
	{
		return BID_COOLDOWN_SECONDS;
	}
}
