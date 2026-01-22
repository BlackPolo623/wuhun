package custom.PlayerBase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class BaseMonsterNpc extends Script
{
	private static final int MONSTER_MANAGER_NPC = 900027;

	// ==================== 重生時間配置 ====================
	private static final int RESPAWN_TIME = 15; // 重生時間（秒）
	private static final int RESPAWN_RANDOM_TIME = 0; // 隨機重生時間範圍（秒）

	// 怪物生成座標（100個）
	private static final Location[] SPAWN_LOCATIONS = {
			// Row 1

			new Location(57148, -93780, -1388),
			new Location(57216, -93780, -1388),
			new Location(57284, -93780, -1388),
			new Location(57352, -93780, -1388),
			new Location(57420, -93780, -1388),
			new Location(57488, -93780, -1388),
			new Location(57556, -93780, -1388),
			new Location(57624, -93780, -1388),
			new Location(57692, -93780, -1388),
			new Location(57760, -93780, -1388),
			new Location(57828, -93780, -1388),
			new Location(57896, -93780, -1388),
			new Location(57964, -93780, -1388),
			new Location(58032, -93780, -1388),
			new Location(58100, -93780, -1388),
			new Location(58168, -93780, -1388),
			new Location(58236, -93780, -1388),
			new Location(58304, -93780, -1388),
			new Location(58372, -93780, -1388),
			new Location(58441, -93780, -1388),

// Row 2

			new Location(57148, -93637, -1388),
			new Location(57216, -93637, -1388),
			new Location(57284, -93637, -1388),
			new Location(57352, -93637, -1388),
			new Location(57420, -93637, -1388),
			new Location(57488, -93637, -1388),
			new Location(57556, -93637, -1388),
			new Location(57624, -93637, -1388),
			new Location(57692, -93637, -1388),
			new Location(57760, -93637, -1388),
			new Location(57828, -93637, -1388),
			new Location(57896, -93637, -1388),
			new Location(57964, -93637, -1388),
			new Location(58032, -93637, -1388),
			new Location(58100, -93637, -1388),
			new Location(58168, -93637, -1388),
			new Location(58236, -93637, -1388),
			new Location(58304, -93637, -1388),
			new Location(58372, -93637, -1388),
			new Location(58441, -93637, -1388),

// Row 3

			new Location(57148, -93495, -1388),
			new Location(57216, -93495, -1388),
			new Location(57284, -93495, -1388),
			new Location(57352, -93495, -1388),
			new Location(57420, -93495, -1388),
			new Location(57488, -93495, -1388),
			new Location(57556, -93495, -1388),
			new Location(57624, -93495, -1388),
			new Location(57692, -93495, -1388),
			new Location(57760, -93495, -1388),
			new Location(57828, -93495, -1388),
			new Location(57896, -93495, -1388),
			new Location(57964, -93495, -1388),
			new Location(58032, -93495, -1388),
			new Location(58100, -93495, -1388),
			new Location(58168, -93495, -1388),
			new Location(58236, -93495, -1388),
			new Location(58304, -93495, -1388),
			new Location(58372, -93495, -1388),
			new Location(58441, -93495, -1388),

// Row 4

			new Location(57148, -93353, -1388),
			new Location(57216, -93353, -1388),
			new Location(57284, -93353, -1388),
			new Location(57352, -93353, -1388),
			new Location(57420, -93353, -1388),
			new Location(57488, -93353, -1388),
			new Location(57556, -93353, -1388),
			new Location(57624, -93353, -1388),
			new Location(57692, -93353, -1388),
			new Location(57760, -93353, -1388),
			new Location(57828, -93353, -1388),
			new Location(57896, -93353, -1388),
			new Location(57964, -93353, -1388),
			new Location(58032, -93353, -1388),
			new Location(58100, -93353, -1388),
			new Location(58168, -93353, -1388),
			new Location(58236, -93353, -1388),
			new Location(58304, -93353, -1388),
			new Location(58372, -93353, -1388),
			new Location(58441, -93353, -1388),

// Row 5

			new Location(57148, -93211, -1388),
			new Location(57216, -93211, -1388),
			new Location(57284, -93211, -1388),
			new Location(57352, -93211, -1388),
			new Location(57420, -93211, -1388),
			new Location(57488, -93211, -1388),
			new Location(57556, -93211, -1388),
			new Location(57624, -93211, -1388),
			new Location(57692, -93211, -1388),
			new Location(57760, -93211, -1388),
			new Location(57828, -93211, -1388),
			new Location(57896, -93211, -1388),
			new Location(57964, -93211, -1388),
			new Location(58032, -93211, -1388),
			new Location(58100, -93211, -1388),
			new Location(58168, -93211, -1388),
			new Location(58236, -93211, -1388),
			new Location(58304, -93211, -1388),
			new Location(58372, -93211, -1388),
			new Location(58441, -93211, -1388),

// Row 6

			new Location(57148, -93069, -1388),
			new Location(57216, -93069, -1388),
			new Location(57284, -93069, -1388),
			new Location(57352, -93069, -1388),
			new Location(57420, -93069, -1388),
			new Location(57488, -93069, -1388),
			new Location(57556, -93069, -1388),
			new Location(57624, -93069, -1388),
			new Location(57692, -93069, -1388),
			new Location(57760, -93069, -1388),
			new Location(57828, -93069, -1388),
			new Location(57896, -93069, -1388),
			new Location(57964, -93069, -1388),
			new Location(58032, -93069, -1388),
			new Location(58100, -93069, -1388),
			new Location(58168, -93069, -1388),
			new Location(58236, -93069, -1388),
			new Location(58304, -93069, -1388),
			new Location(58372, -93069, -1388),
			new Location(58441, -93069, -1388),

// Row 7

			new Location(57148, -92927, -1388),
			new Location(57216, -92927, -1388),
			new Location(57284, -92927, -1388),
			new Location(57352, -92927, -1388),
			new Location(57420, -92927, -1388),
			new Location(57488, -92927, -1388),
			new Location(57556, -92927, -1388),
			new Location(57624, -92927, -1388),
			new Location(57692, -92927, -1388),
			new Location(57760, -92927, -1388),
			new Location(57828, -92927, -1388),
			new Location(57896, -92927, -1388),
			new Location(57964, -92927, -1388),
			new Location(58032, -92927, -1388),
			new Location(58100, -92927, -1388),
			new Location(58168, -92927, -1388),
			new Location(58236, -92927, -1388),
			new Location(58304, -92927, -1388),
			new Location(58372, -92927, -1388),
			new Location(58441, -92927, -1388),

// Row 8

			new Location(57148, -92785, -1388),
			new Location(57216, -92785, -1388),
			new Location(57284, -92785, -1388),
			new Location(57352, -92785, -1388),
			new Location(57420, -92785, -1388),
			new Location(57488, -92785, -1388),
			new Location(57556, -92785, -1388),
			new Location(57624, -92785, -1388),
			new Location(57692, -92785, -1388),
			new Location(57760, -92785, -1388),
			new Location(57828, -92785, -1388),
			new Location(57896, -92785, -1388),
			new Location(57964, -92785, -1388),
			new Location(58032, -92785, -1388),
			new Location(58100, -92785, -1388),
			new Location(58168, -92785, -1388),
			new Location(58236, -92785, -1388),
			new Location(58304, -92785, -1388),
			new Location(58372, -92785, -1388),
			new Location(58441, -92785, -1388),

// Row 9

			new Location(57148, -92643, -1388),
			new Location(57216, -92643, -1388),
			new Location(57284, -92643, -1388),
			new Location(57352, -92643, -1388),
			new Location(57420, -92643, -1388),
			new Location(57488, -92643, -1388),
			new Location(57556, -92643, -1388),
			new Location(57624, -92643, -1388),
			new Location(57692, -92643, -1388),
			new Location(57760, -92643, -1388),
			new Location(57828, -92643, -1388),
			new Location(57896, -92643, -1388),
			new Location(57964, -92643, -1388),
			new Location(58032, -92643, -1388),
			new Location(58100, -92643, -1388),
			new Location(58168, -92643, -1388),
			new Location(58236, -92643, -1388),
			new Location(58304, -92643, -1388),
			new Location(58372, -92643, -1388),
			new Location(58441, -92643, -1388),

// Row 10

			new Location(57148, -92501, -1388),
			new Location(57216, -92501, -1388),
			new Location(57284, -92501, -1388),
			new Location(57352, -92501, -1388),
			new Location(57420, -92501, -1388),
			new Location(57488, -92501, -1388),
			new Location(57556, -92501, -1388),
			new Location(57624, -92501, -1388),
			new Location(57692, -92501, -1388),
			new Location(57760, -92501, -1388),
			new Location(57828, -92501, -1388),
			new Location(57896, -92501, -1388),
			new Location(57964, -92501, -1388),
			new Location(58032, -92501, -1388),
			new Location(58100, -92501, -1388),
			new Location(58168, -92501, -1388),
			new Location(58236, -92501, -1388),
			new Location(58304, -92501, -1388),
			new Location(58372, -92501, -1388),
			new Location(58441, -92501, -1388)
	};

	// 可選怪物列表 {顯示名稱, NPC_ID}
	private static final Object[][] AVAILABLE_MONSTERS = {
			{"帝王騎士", 22348},
			{"神聖法師", 22773}
	};

	private static final Map<Integer, List<Spawn>> SPAWNED_MONSTERS = new ConcurrentHashMap<>();

	public BaseMonsterNpc()
	{
		addStartNpc(MONSTER_MANAGER_NPC);
		addTalkId(MONSTER_MANAGER_NPC);
		addFirstTalkId(MONSTER_MANAGER_NPC);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!isBaseOwner(player))
		{
			player.sendMessage("只有基地主人才能使用此功能!");
			return null;
		}

		if (event.startsWith("add_monster "))
		{
			String[] params = event.substring(12).split(" ");
			if (params.length == 2)
			{
				String monsterName = params[0];
				int monsterId = getMonsterIdByName(monsterName);

				if (monsterId == -1)
				{
					player.sendMessage("無效的怪物類型!");
					return showConfigPage(player);
				}

				int count = Integer.parseInt(params[1]);
				return handleAddMonster(player, monsterId, count);
			}
		}
		else if (event.startsWith("remove_monster "))
		{
			int monsterId = Integer.parseInt(event.substring(15));
			return handleRemoveMonster(player, monsterId);
		}
		else if (event.equals("spawn_all"))
		{
			return handleSpawnAll(player);
		}
		else if (event.equals("despawn_all"))
		{
			return handleDespawnAll(player);
		}

		return null;
	}

	private boolean isBaseOwner(Player player)
	{
		Instance instance = player.getInstanceWorld();
		if (instance == null)
		{
			return false;
		}

		Map<String, Object> baseInfo = PlayerBaseDAO.getBaseInfo(player.getObjectId());
		return !baseInfo.isEmpty() && (int) baseInfo.get("instance_id") == instance.getId();
	}

	private String handleAddMonster(Player player, int monsterId, int count)
	{
		if (count <= 0)
		{
			player.sendMessage("數量必須大於0!");
			return showConfigPage(player);
		}

		Map<String, Object> baseInfo = PlayerBaseDAO.getBaseInfo(player.getObjectId());
		int maxCount = (int) baseInfo.get("max_monster_count");
		int currentCount = PlayerBaseDAO.getCurrentMonsterCount(player.getObjectId());

		if (currentCount + count > maxCount)
		{
			player.sendMessage("超過怪物數量上限!當前:" + currentCount + "/" + maxCount);
			return showConfigPage(player);
		}

		if (PlayerBaseDAO.addMonsterConfig(player.getObjectId(), monsterId, count))
		{
			player.sendMessage("添加成功!");
		}
		else
		{
			player.sendMessage("添加失敗!");
		}

		return showConfigPage(player);
	}

	private String handleRemoveMonster(Player player, int monsterId)
	{
		if (PlayerBaseDAO.removeMonsterConfig(player.getObjectId(), monsterId))
		{
			player.sendMessage("已移除該怪物配置");
		}
		else
		{
			player.sendMessage("移除失敗!");
		}

		return showConfigPage(player);
	}

	private String handleSpawnAll(Player player)
	{
		Instance instance = player.getInstanceWorld();
		if (instance == null)
		{
			player.sendMessage("您不在基地中!");
			return null;
		}

		List<Map<String, Integer>> configs = PlayerBaseDAO.getAllMonsterConfigs(player.getObjectId());

		if (configs.isEmpty())
		{
			player.sendMessage("沒有配置任何怪物!");
			return null;
		}

		// 先清除已存在的怪物
		handleDespawnAll(player);

		List<Spawn> spawnList = new java.util.ArrayList<>();
		int totalSpawned = 0;
		int locationIndex = 0;

		for (Map<String, Integer> config : configs)
		{
			int monsterId = config.get("monster_id");
			int count = config.get("monster_count");

			// 獲取 NPC 模板
			NpcTemplate template = NpcData.getInstance().getTemplate(monsterId);
			if (template == null)
			{
				player.sendMessage("無效的怪物ID: " + monsterId);
				continue;
			}

			for (int i = 0; i < count; i++)
			{
				if (locationIndex >= SPAWN_LOCATIONS.length)
				{
					player.sendMessage("座標不足,已生成" + totalSpawned + "只怪物");
					SPAWNED_MONSTERS.put(player.getObjectId(), spawnList);
					return null;
				}

				Location loc = SPAWN_LOCATIONS[locationIndex];

				try
				{
					// ===== 關鍵修改：使用 Spawn 對象並設置重生時間 =====
					Spawn spawn = new Spawn(template);
					spawn.setLocation(loc);
					spawn.setHeading(0);
					spawn.setAmount(1);
					spawn.setInstanceId(instance.getId());

					// 設置重生時間（秒）
					spawn.setRespawnDelay(RESPAWN_TIME, RESPAWN_RANDOM_TIME);

					// 開始生成並啟用自動重生
					spawn.init();
					spawn.startRespawn();

					spawnList.add(spawn);
					totalSpawned++;
					// ===== 修改結束 =====
				}
				catch (Exception e)
				{
					player.sendMessage("生成怪物時發生錯誤: " + e.getMessage());
					e.printStackTrace();
				}

				locationIndex++;
			}
		}

		SPAWNED_MONSTERS.put(player.getObjectId(), spawnList);

		player.sendMessage("========================================");
		player.sendMessage("已生成 " + totalSpawned + " 只怪物");
		player.sendMessage("重生時間: " + RESPAWN_TIME + "秒 (±" + RESPAWN_RANDOM_TIME + "秒)");
		player.sendMessage("========================================");

		return null;
	}

	private String handleDespawnAll(Player player)
	{
		List<Spawn> spawns = SPAWNED_MONSTERS.get(player.getObjectId());

		if (spawns != null)
		{
			for (Spawn spawn : spawns)
			{
				if (spawn != null)
				{
					// 停止重生
					spawn.stopRespawn();

					// 刪除當前的 NPC
					Npc npc = spawn.getLastSpawn();
					if (npc != null && !npc.isDead())
					{
						npc.deleteMe();
					}
				}
			}
			spawns.clear();
		}

		player.sendMessage("已清除所有怪物");
		return null;
	}

	private String showConfigPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/PlayerBase/monster_config.htm");

		// 生成怪物選項列表
		StringBuilder monsterOptions = new StringBuilder();
		for (Object[] monster : AVAILABLE_MONSTERS)
		{
			monsterOptions.append(monster[0]).append(";");
		}
		html.replace("%monster_list%", monsterOptions.toString());

		// 生成當前配置列表
		List<Map<String, Integer>> configs = PlayerBaseDAO.getAllMonsterConfigs(player.getObjectId());
		StringBuilder configList = new StringBuilder();

		if (configs.isEmpty())
		{
			configList.append("<tr><td align=center height=30 colspan=3><font color=\"808080\">暫無配置</font></td></tr>");
		}
		else
		{
			for (Map<String, Integer> config : configs)
			{
				int monsterId = config.get("monster_id");
				int count = config.get("monster_count");

				NpcTemplate template = NpcData.getInstance().getTemplate(monsterId);
				String monsterName = template != null ? template.getName() : "未知";

				configList.append("<tr bgcolor=\"222222\">");
				configList.append("<td width=140 align=center><font color=\"00FF66\">").append(monsterName).append("</font></td>");
				configList.append("<td width=70 align=center>").append(count).append("</td>");
				configList.append("<td width=70 align=center>");
				configList.append("<button value=\"刪除\" action=\"bypass -h Quest BaseMonsterNpc remove_monster ").append(monsterId);
				configList.append("\" width=50 height=20 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
				configList.append("</td></tr>");
			}
		}

		html.replace("%config_list%", configList.toString());

		// 統計信息
		Map<String, Object> baseInfo = PlayerBaseDAO.getBaseInfo(player.getObjectId());
		int currentCount = PlayerBaseDAO.getCurrentMonsterCount(player.getObjectId());
		int maxCount = (int) baseInfo.get("max_monster_count");

		html.replace("%current_count%", String.valueOf(currentCount));
		html.replace("%max_count%", String.valueOf(maxCount));

		player.sendPacket(html);
		return null;
	}

	private int getMonsterIdByName(String name)
	{
		for (Object[] monster : AVAILABLE_MONSTERS)
		{
			if (monster[0].equals(name))
			{
				return (int) monster[1];
			}
		}
		return -1;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return showConfigPage(player);
	}

	public static void main(String[] args)
	{
		new BaseMonsterNpc();
		System.out.println("【系統】基地怪物管理系統載入完畢!");
	}
}