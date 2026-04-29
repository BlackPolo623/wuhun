package ai.bosses.Balok;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Balok battle event configuration.
 * Reads from data/scripts/ai/bosses/Balok/Balok.ini
 */
public class BalokConfig
{
	private static final Logger LOGGER = Logger.getLogger(BalokConfig.class.getName());
	private static final String CONFIG_FILE = "data/scripts/ai/bosses/Balok/Balok.ini";

	public static int BALOK_HOUR;
	public static int BALOK_MINUTE;
	public static int BATTLE_TIME;       // milliseconds
	public static int REWARD_TIME;       // milliseconds
	public static int POINTS_PER_MONSTER;
	public static int SCORPION_MULTIPLIER;
	public static int STAGE2_POINT_THRESHOLD;
	public static int WAVE1_SPAWN_THRESHOLD;
	public static int WAVE2_SPAWN_THRESHOLD;
	public static int LORD_BALOK_THRESHOLD;
	public static int BYPASS_POINT_THRESHOLD;
	public static int BYPASS_RANDOM_CHANCE;
	public static int MIN_POINTS_FOR_REWARD;
	public static int TOP_RANKER_COUNT;

	static
	{
		load();
	}

	public static void load()
	{
		final File configFile = new File(CONFIG_FILE);
		if (!configFile.exists())
		{
			LOGGER.warning("[Balok] Config file not found: " + CONFIG_FILE + " — using defaults.");
			loadDefaults();
			return;
		}

		final Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(configFile))
		{
			props.load(fis);

			final String[] time = props.getProperty("BalokTime", "20:30").trim().split(":");
			BALOK_HOUR = Integer.parseInt(time[0]);
			BALOK_MINUTE = Integer.parseInt(time[1]);

			BATTLE_TIME = getInt(props, "BattleTimeMinutes", 30) * 60000;
			REWARD_TIME = getInt(props, "RewardTimeMinutes", 40) * 60000;

			POINTS_PER_MONSTER = getInt(props, "PointsPerMonster", 10);
			SCORPION_MULTIPLIER = getInt(props, "ScorpionMultiplier", 10);

			STAGE2_POINT_THRESHOLD = getInt(props, "Stage2PointThreshold", 250000);
			WAVE1_SPAWN_THRESHOLD = getInt(props, "Wave1SpawnPointThreshold", 320000);
			WAVE2_SPAWN_THRESHOLD = getInt(props, "Wave2SpawnPointThreshold", 800000);
			LORD_BALOK_THRESHOLD = getInt(props, "LordBalokPointThreshold", 1500000);

			BYPASS_POINT_THRESHOLD = getInt(props, "BypassPointThreshold", 3000);
			BYPASS_RANDOM_CHANCE = getInt(props, "BypassRandomChance", 10);

			MIN_POINTS_FOR_REWARD = getInt(props, "MinPointsForReward", 1000);
			TOP_RANKER_COUNT = getInt(props, "TopRankerCount", 30);

			LOGGER.info("[Balok] Config loaded. Start time: " + BALOK_HOUR + ":" + String.format("%02d", BALOK_MINUTE));
		}
		catch (Exception e)
		{
			LOGGER.severe("[Balok] Failed to load config: " + e.getMessage());
			loadDefaults();
		}
	}

	private static void loadDefaults()
	{
		BALOK_HOUR = 20;
		BALOK_MINUTE = 30;
		BATTLE_TIME = 1800000;
		REWARD_TIME = 2400000;
		POINTS_PER_MONSTER = 10;
		SCORPION_MULTIPLIER = 10;
		STAGE2_POINT_THRESHOLD = 250000;
		WAVE1_SPAWN_THRESHOLD = 320000;
		WAVE2_SPAWN_THRESHOLD = 800000;
		LORD_BALOK_THRESHOLD = 1500000;
		BYPASS_POINT_THRESHOLD = 3000;
		BYPASS_RANDOM_CHANCE = 10;
		MIN_POINTS_FOR_REWARD = 1000;
		TOP_RANKER_COUNT = 30;
	}

	private static int getInt(Properties props, String key, int defaultValue)
	{
		try
		{
			return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)).trim());
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning("[Balok] Invalid value for '" + key + "', using default: " + defaultValue);
			return defaultValue;
		}
	}
}
