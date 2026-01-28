package custom.BossAuctionSystem;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * 世界首領管理器
 * 負責定時召喚世界首領
 * @author 黑普羅
 */
public class WorldBossManager extends Script
{
	private static final Logger LOGGER = Logger.getLogger(WorldBossManager.class.getName());
	private static WorldBossManager _instance;

	private Monster _currentBoss = null;
	private int _currentBossId = 0;
	private long _nextSpawnTime = 0;
	private ScheduledFuture<?> _spawnTask = null;
	private ScheduledFuture<?> _despawnTask = null;
	private final Random _random = new Random();

	public WorldBossManager()
	{
		_instance = this;

		// 載入配置
		WorldBossConfig.load();

		if (WorldBossConfig.isEnabled())
		{
			// 計算下次召喚時間並安排任務
			scheduleNextSpawn();
			LOGGER.info("【世界首領】系統已啟動");
		}
	}

	public static WorldBossManager getInstance()
	{
		return _instance;
	}

	/**
	 * 計算並安排下次召喚
	 */
	private void scheduleNextSpawn()
	{
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime nextSpawn = calculateNextSpawnTime(now);

		_nextSpawnTime = nextSpawn.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

		long delay = ChronoUnit.MILLIS.between(now, nextSpawn);

		if (_spawnTask != null)
		{
			_spawnTask.cancel(false);
		}

		_spawnTask = ThreadPool.schedule(() -> {
			spawnWorldBoss();
			scheduleNextSpawn(); // 安排下一次召喚
		}, delay);

		LOGGER.info("【世界首領】下次召喚時間: " + nextSpawn + " (距離現在 " + (delay / 1000 / 60) + " 分鐘)");
	}

	/**
	 * 計算下次召喚時間
	 */
	private LocalDateTime calculateNextSpawnTime(LocalDateTime from)
	{
		String[] timeParts = WorldBossConfig.getSpawnTime().split(":");
		int hour = Integer.parseInt(timeParts[0]);
		int minute = Integer.parseInt(timeParts[1]);

		LocalTime spawnTime = LocalTime.of(hour, minute);
		List<Integer> spawnDays = WorldBossConfig.getSpawnDays();

		LocalDateTime candidate = from;

		// 如果今天的時間已過,從明天開始找
		if (from.toLocalTime().isAfter(spawnTime))
		{
			candidate = from.plusDays(1).with(spawnTime);
		}
		else
		{
			candidate = from.with(spawnTime);
		}

		// 找到最近的召喚日期
		for (int i = 0; i < 7; i++)
		{
			int dayOfWeek = candidate.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
			if (spawnDays.contains(dayOfWeek))
			{
				return candidate;
			}
			candidate = candidate.plusDays(1);
		}

		return candidate;
	}

	/**
	 * 召喚世界首領
	 */
	private void spawnWorldBoss()
	{
		// 如果已有首領存在,先清除
		if (_currentBoss != null)
		{
			_currentBoss.deleteMe();
			_currentBoss = null;
		}

		List<Integer> bossIds = WorldBossConfig.getBossNpcIds();
		if (bossIds.isEmpty())
		{
			LOGGER.warning("【世界首領】沒有配置首領 NPC ID");
			return;
		}

		// 隨機選擇一個首領
		_currentBossId = bossIds.get(_random.nextInt(bossIds.size()));

		NpcTemplate template = NpcData.getInstance().getTemplate(_currentBossId);
		if (template == null)
		{
			LOGGER.warning("【世界首領】找不到 NPC 模板: " + _currentBossId);
			return;
		}

		Location loc = WorldBossConfig.getRespawnLocation();

		try
		{
			_currentBoss = new Monster(template);
			_currentBoss.setSpawn(null);
			_currentBoss.setXYZ(loc.getX(), loc.getY(), loc.getZ());
			_currentBoss.setHeading(0);
			_currentBoss.setCurrentHpMp(_currentBoss.getMaxHp(), _currentBoss.getMaxMp());
			_currentBoss.spawnMe();

			// 公告
			if (WorldBossConfig.isAnnounceSpawn())
			{
				World.getInstance().getPlayers().forEach(player -> {
					CreatureSay cs = new CreatureSay(null, ChatType.ANNOUNCEMENT, "", "【世界首領】" + template.getName() + " 已在世界首領重生點出現!");
					player.sendPacket(cs);
				});
			}

			LOGGER.info("【世界首領】已召喚: " + template.getName() + " 於 " + loc);

			// 設定自動消失時間
			int lifetime = WorldBossConfig.getBossLifetime();
			if (lifetime > 0)
			{
				if (_despawnTask != null)
				{
					_despawnTask.cancel(false);
				}

				_despawnTask = ThreadPool.schedule(() -> {
					if (_currentBoss != null && !_currentBoss.isDead())
					{
						_currentBoss.deleteMe();
						_currentBoss = null;
						LOGGER.info("【世界首領】已自動消失(超時)");
					}
				}, lifetime * 60 * 1000L);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("【世界首領】召喚失敗: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 首領死亡事件
	 */
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onCreatureDeath(OnCreatureDeath event)
	{
		if (event.getTarget() == _currentBoss)
		{
			// 取消自動消失任務
			if (_despawnTask != null)
			{
				_despawnTask.cancel(false);
				_despawnTask = null;
			}

			_currentBoss = null;
			LOGGER.info("【世界首領】已被擊殺");
		}
	}

	/**
	 * 獲取當前首領
	 */
	public Monster getCurrentBoss()
	{
		return _currentBoss;
	}

	/**
	 * 獲取下次召喚時間
	 */
	public long getNextSpawnTime()
	{
		return _nextSpawnTime;
	}

	/**
	 * 是否有首領存在
	 */
	public boolean hasBoss()
	{
		return _currentBoss != null && !_currentBoss.isDead();
	}

	/**
	 * 獲取重生點座標
	 */
	public Location getRespawnLocation()
	{
		return WorldBossConfig.getRespawnLocation();
	}
}
