package custom.BossAuctionSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.Location;

/**
 * 世界首領配置
 * @author 黑普羅
 */
public class WorldBossConfig
{
	private static final Logger LOGGER = Logger.getLogger(WorldBossConfig.class.getName());
	private static final String CONFIG_FILE = "data/scripts/custom/BossAuctionSystem/WorldBoss.ini";

	// 配置項
	private static boolean _enableWorldBoss = true;
	private static Location _respawnLocation = new Location(83400, 148600, -3400);
	private static List<Integer> _bossNpcIds = new ArrayList<>();
	private static List<Integer> _spawnDays = new ArrayList<>();
	private static String _spawnTime = "20:00";
	private static int _bossLifetime = 120;
	private static boolean _announceSpawn = true;
	private static boolean _announceKill = true;

	public static void load()
	{
		Properties properties = new Properties();
		File configFile = new File(CONFIG_FILE);

		if (!configFile.exists())
		{
			LOGGER.warning("【世界首領】配置文件不存在: " + CONFIG_FILE);
			return;
		}

		try (InputStream input = new FileInputStream(configFile))
		{
			properties.load(input);

			_enableWorldBoss = Boolean.parseBoolean(properties.getProperty("EnableWorldBoss", "true"));

			// 解析重生點座標
			String[] locParts = properties.getProperty("RespawnLocation", "83400,148600,-3400").split(",");
			if (locParts.length == 3)
			{
				int x = Integer.parseInt(locParts[0].trim());
				int y = Integer.parseInt(locParts[1].trim());
				int z = Integer.parseInt(locParts[2].trim());
				_respawnLocation = new Location(x, y, z);
			}

			// 解析首領 NPC ID 列表
			_bossNpcIds.clear();
			String[] bossIds = properties.getProperty("BossNpcIds", "29001").split(",");
			for (String id : bossIds)
			{
				try
				{
					_bossNpcIds.add(Integer.parseInt(id.trim()));
				}
				catch (NumberFormatException e)
				{
					LOGGER.warning("【世界首領】無效的 BOSS NPC ID: " + id);
				}
			}

			// 解析召喚日期
			_spawnDays.clear();
			String[] days = properties.getProperty("SpawnDays", "1,3,5,7").split(",");
			for (String day : days)
			{
				try
				{
					int dayNum = Integer.parseInt(day.trim());
					if ((dayNum >= 1) && (dayNum <= 7))
					{
						_spawnDays.add(dayNum);
					}
				}
				catch (NumberFormatException e)
				{
					LOGGER.warning("【世界首領】無效的日期: " + day);
				}
			}

			_spawnTime = properties.getProperty("SpawnTime", "20:00");
			_bossLifetime = Integer.parseInt(properties.getProperty("BossLifetime", "120"));
			_announceSpawn = Boolean.parseBoolean(properties.getProperty("AnnounceSpawn", "true"));
			_announceKill = Boolean.parseBoolean(properties.getProperty("AnnounceKill", "true"));

			LOGGER.info("【世界首領】配置載入完成");
			LOGGER.info("  - 重生點: " + _respawnLocation);
			LOGGER.info("  - 首領列表: " + _bossNpcIds);
			LOGGER.info("  - 召喚日期: " + _spawnDays);
			LOGGER.info("  - 召喚時間: " + _spawnTime);
		}
		catch (Exception e)
		{
			LOGGER.warning("【世界首領】載入配置失敗: " + e.getMessage());
		}
	}

	public static boolean isEnabled()
	{
		return _enableWorldBoss;
	}

	public static Location getRespawnLocation()
	{
		return _respawnLocation;
	}

	public static List<Integer> getBossNpcIds()
	{
		return _bossNpcIds;
	}

	public static List<Integer> getSpawnDays()
	{
		return _spawnDays;
	}

	public static String getSpawnTime()
	{
		return _spawnTime;
	}

	public static int getBossLifetime()
	{
		return _bossLifetime;
	}

	public static boolean isAnnounceSpawn()
	{
		return _announceSpawn;
	}

	public static boolean isAnnounceKill()
	{
		return _announceKill;
	}
}
