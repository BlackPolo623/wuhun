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
			new Location(57225, -93704, -1388),
			new Location(57352, -93704, -1388),
			new Location(57479, -93704, -1388),
			new Location(57605, -93704, -1388),
			new Location(57732, -93704, -1388),
			new Location(57859, -93704, -1388),
			new Location(57985, -93704, -1388),
			new Location(58112, -93704, -1388),
			new Location(58239, -93704, -1388),
			new Location(58365, -93704, -1388),
			new Location(57225, -93579, -1388),
			new Location(57352, -93579, -1388),
			new Location(57479, -93579, -1388),
			new Location(57605, -93579, -1388),
			new Location(57732, -93579, -1388),
			new Location(57859, -93579, -1388),
			new Location(57985, -93579, -1388),
			new Location(58112, -93579, -1388),
			new Location(58239, -93579, -1388),
			new Location(58365, -93579, -1388),
			new Location(57225, -93453, -1388),
			new Location(57352, -93453, -1388),
			new Location(57479, -93453, -1388),
			new Location(57605, -93453, -1388),
			new Location(57732, -93453, -1388),
			new Location(57859, -93453, -1388),
			new Location(57985, -93453, -1388),
			new Location(58112, -93453, -1388),
			new Location(58239, -93453, -1388),
			new Location(58365, -93453, -1388),
			new Location(57225, -93328, -1388),
			new Location(57352, -93328, -1388),
			new Location(57479, -93328, -1388),
			new Location(57605, -93328, -1388),
			new Location(57732, -93328, -1388),
			new Location(57859, -93328, -1388),
			new Location(57985, -93328, -1388),
			new Location(58112, -93328, -1388),
			new Location(58239, -93328, -1388),
			new Location(58365, -93328, -1388),
			new Location(57225, -93202, -1388),
			new Location(57352, -93202, -1388),
			new Location(57479, -93202, -1388),
			new Location(57605, -93202, -1388),
			new Location(57732, -93202, -1388),
			new Location(57859, -93202, -1388),
			new Location(57985, -93202, -1388),
			new Location(58112, -93202, -1388),
			new Location(58239, -93202, -1388),
			new Location(58365, -93202, -1388),
			new Location(57225, -93077, -1388),
			new Location(57352, -93077, -1388),
			new Location(57479, -93077, -1388),
			new Location(57605, -93077, -1388),
			new Location(57732, -93077, -1388),
			new Location(57859, -93077, -1388),
			new Location(57985, -93077, -1388),
			new Location(58112, -93077, -1388),
			new Location(58239, -93077, -1388),
			new Location(58365, -93077, -1388),
			new Location(57225, -92951, -1388),
			new Location(57352, -92951, -1388),
			new Location(57479, -92951, -1388),
			new Location(57605, -92951, -1388),
			new Location(57732, -92951, -1388),
			new Location(57859, -92951, -1388),
			new Location(57985, -92951, -1388),
			new Location(58112, -92951, -1388),
			new Location(58239, -92951, -1388),
			new Location(58365, -92951, -1388),
			new Location(57225, -92826, -1388),
			new Location(57352, -92826, -1388),
			new Location(57479, -92826, -1388),
			new Location(57605, -92826, -1388),
			new Location(57732, -92826, -1388),
			new Location(57859, -92826, -1388),
			new Location(57985, -92826, -1388),
			new Location(58112, -92826, -1388),
			new Location(58239, -92826, -1388),
			new Location(58365, -92826, -1388),
			new Location(57225, -92700, -1388),
			new Location(57352, -92700, -1388),
			new Location(57479, -92700, -1388),
			new Location(57605, -92700, -1388),
			new Location(57732, -92700, -1388),
			new Location(57859, -92700, -1388),
			new Location(57985, -92700, -1388),
			new Location(58112, -92700, -1388),
			new Location(58239, -92700, -1388),
			new Location(58365, -92700, -1388),
			new Location(57225, -92575, -1388),
			new Location(57352, -92575, -1388),
			new Location(57479, -92575, -1388),
			new Location(57605, -92575, -1388),
			new Location(57732, -92575, -1388),
			new Location(57859, -92575, -1388),
			new Location(57985, -92575, -1388),
			new Location(58112, -92575, -1388),
			new Location(58239, -92575, -1388),
			new Location(58365, -92575, -1388)
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