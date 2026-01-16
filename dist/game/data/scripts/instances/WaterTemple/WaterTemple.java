package instances.WaterTemple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.Earthquake;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;

/**
 * 水之神殿副本 - 優化版
 * @author Serenitty (原作者)
 * @author 黑普羅 (優化)
 */
public class WaterTemple extends InstanceScript
{
	// NPCs
	private static final int WATER_SLIME = 19109;
	private static final int TROOP_CHIEFT = 19108;
	private static final int BREATH_WATER = 19111;
	private static final int UNDINE = 19110;
	private static final int EROSION_PRIEST = 19114;
	private static final int TEMPLE_GUARDIAN_WARRIOR = 19112;
	private static final int TEMPLE_GUARDIAN_WIZARD = 19113;
	private static final int ABER = 19115;
	private static final int ELLE = 19116;
	private static final int AOS = 34464;
	private static final int ANIDA = 34463;

	// 機率設定
	private static final int FLOOD_CHANCE = 5;
	private static final int ELLE_BOSS_CHANCE = 20;

	// 觸發器ID
	private static final int FLOOD_STAGE_1 = 22250130;
	private static final int FLOOD_STAGE_2 = 22250132;
	private static final int FLOOD_STAGE_3 = 22250134;
	private static final int FLOOD_STAGE_FINAL_FLOOR_1 = 22250144;
	private static final int FLOOD_STAGE_FINAL_FLOOR_2 = 22250146;

	// 傳送點
	private static final Location TELEPORT_STAGE_2 = new Location(71101, 242498, -8425);
	private static final Location TELEPORT_STAGE_3 = new Location(72944, 242782, -7651);
	private static final Location TELEPORT_OUTSIDE = new Location(83763, 147184, -3404);

	// 區域
	private static final ZoneType WATER_ZONE_1 = ZoneManager.getInstance().getZoneByName("Water Temple 1");
	private static final ZoneType WATER_ZONE_2 = ZoneManager.getInstance().getZoneByName("Water Temple 2");
	private static final ZoneType WATER_ZONE_3 = ZoneManager.getInstance().getZoneByName("Water Temple 3");

	// 副本ID
	private static final int TEMPLATE_ID = 2008;

	// 階段怪物數量配置
	private static final Map<Integer, Integer> STAGE_MONSTER_COUNTS = new HashMap<>();
	static
	{
		STAGE_MONSTER_COUNTS.put(1, 20); // Stage1需要擊殺20隻怪
		STAGE_MONSTER_COUNTS.put(2, 15); // Stage2需要擊殺15隻怪
		STAGE_MONSTER_COUNTS.put(3, 12); // Stage3需要擊殺12隻怪
	}

	public WaterTemple()
	{
		super(TEMPLATE_ID);
		addInstanceEnterId(TEMPLATE_ID);
		addInstanceLeaveId(TEMPLATE_ID);
		addInstanceCreatedId(TEMPLATE_ID);
		addSpawnId(EROSION_PRIEST);
		addKillId(WATER_SLIME, TROOP_CHIEFT, BREATH_WATER, UNDINE,
				TEMPLE_GUARDIAN_WARRIOR, TEMPLE_GUARDIAN_WIZARD, ABER, ELLE);
		addAttackId(ABER, ELLE);
		addFirstTalkId(AOS, ANIDA);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "ENTER":
			{
				handleEnterInstance(player, npc);
				break;
			}
			case "RESUME_POSITION":
			{
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					teleportToCurrentStage(player, world);
				}
				break;
			}
			case "EROSION_SPAWN":
			{
				handleErosionSpawn(npc);
				break;
			}
			case "START_SPAWN":
			{
				handleStartSpawn(npc);
				break;
			}
			case "FINAL_BOSS":
			{
				handleFinalBoss(npc, player);
				break;
			}
		}
		return null;
	}

	@Override
	public void onInstanceEnter(Player player, Instance world)
	{
		// 發送UI計時器
		player.sendPacket(new ExSendUIEvent(player, false, false,
				(int) (world.getRemainingTime() / 1000), 0,
				NpcStringId.WATER_TEMPLE_S_REMAINING_TIME));

		// 根據當前階段顯示對應的水淹效果
		final int currentStage = world.getParameters().getInt("stage", 1);
		updateFloodVisuals(player, currentStage);

		world.getParameters().set("isOutside", false);
	}

	@Override
	public void onInstanceCreated(Instance world, Player player)
	{
		// 初始化副本參數
		world.getParameters().set("stage", 1);
		world.getParameters().set("stage1_kills", 0);
		world.getParameters().set("stage2_kills", 0);
		world.getParameters().set("stage3_kills", 0);

		// 刷新AOS NPC
		final Npc aosNpc = addSpawn(AOS, 69000, 243368, -8734, 0, false, 0, false, world.getId());
		aosNpc.broadcastSay(ChatType.NPC_GENERAL,
				NpcStringId.PLEASE_KILL_THE_MONSTERS_THAT_ARE_TRYING_TO_STEAL_THE_WATER_ENERGY_AND_HELP_ME_CLEAR_THIS_PLACE);
		aosNpc.getAI().setIntention(Intention.MOVE_TO, new Location(70441, 243470, -9179));

		// 10秒後開始刷怪
		startQuestTimer("START_SPAWN", 10000, aosNpc, null);
	}

	@Override
	public void onInstanceLeave(Player player, Instance world)
	{
		// 清除UI計時器
		player.sendPacket(new ExSendUIEvent(player, false, false, 0, 0,
				NpcStringId.WATER_TEMPLE_S_REMAINING_TIME));
		world.getParameters().set("isOutside", true);
	}

	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final Instance world = npc.getInstanceWorld();
		if ((world == null) || (attacker == null))
		{
			return;
		}

		// 處理寵物/召喚獸的情況
		Player actualAttacker = attacker;
		if (isSummon && (attacker.getServitors() != null))
		{
			actualAttacker = attacker;
		}

		// 記錄攻擊者
		final String playerKey = "attacker_" + actualAttacker.getObjectId();
		world.getParameters().set(playerKey, true);
	}

	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world == null)
		{
			return;
		}

		final int npcId = npc.getId();
		final int currentStage = world.getParameters().getInt("stage", 1);

		// 階段1怪物
		if ((npcId == WATER_SLIME) || (npcId == TROOP_CHIEFT))
		{
			handleStageKill(world, 1, FLOOD_STAGE_1, WATER_ZONE_1, "Stage1", "Stage2",
					NpcStringId.TO_THE_UPPER_LEVEL_WHERE_MONSTERS_SEEKING_THE_WATER_SPIRIT_S_POWER_DWELL);
		}
		// 階段2怪物
		else if ((npcId == BREATH_WATER) || (npcId == UNDINE))
		{
			handleStageKill(world, 2, FLOOD_STAGE_2, WATER_ZONE_2, "Stage2", "Stage3", NpcStringId.WHERE_MONSTERS_REVEAL_THEIR_TRUE_FACES);
		}
		// 階段3怪物
		else if ((npcId == TEMPLE_GUARDIAN_WARRIOR) || (npcId == TEMPLE_GUARDIAN_WIZARD))
		{
			handleStage3Kill(world, npc, killer);
		}
		// 最終BOSS
		else if ((npcId == ABER) || (npcId == ELLE))
		{
			handleBossKill(npc, world);
		}
	}

	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			// 讓怪物攻擊所有玩家
			for (Player player : world.getPlayers())
			{
				addAttackPlayerDesire(npc, player);
			}
		}
	}

	// ==================== 私有方法 ====================

	/**
	 * 處理進入副本
	 */
	private void handleEnterInstance(Player player, Npc npc)
	{
		if (player.isInParty())
		{
			final Party party = player.getParty();
			final boolean isInCC = party.isInCommandChannel();
			final List<Player> members = isInCC ? party.getCommandChannel().getMembers() : party.getMembers();

			// 檢查所有成員距離
			for (Player member : members)
			{
				if (!member.isInsideRadius3D(npc, 1000))
				{
					player.sendMessage("玩家 " + member.getName() + " 必須靠近NPC。");
					return;
				}
			}

			// 全部進入
			for (Player member : members)
			{
				enterInstance(member, npc, TEMPLATE_ID);
			}
		}
		else if (player.isGM())
		{
			enterInstance(player, npc, TEMPLATE_ID);
			player.sendMessage("SYS: GM模式進入水之神殿副本。");
		}
		else
		{
			if (!player.isInsideRadius3D(npc, 1000))
			{
				player.sendMessage("您必須靠近NPC。");
				return;
			}
			enterInstance(player, npc, TEMPLATE_ID);
		}
	}

	/**
	 * 處理侵蝕祭司刷新
	 */
	private void handleErosionSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if ((world == null))
		{
			return; // 副本結束則不再刷新
		}

		if (world.getNpc(EROSION_PRIEST) == null)
		{
			world.spawnGroup("ErosionPriest");
			// 80秒後再次刷新
			startQuestTimer("EROSION_SPAWN", 80000, npc, null);
		}
	}

	/**
	 * 處理開始刷怪
	 */
	private void handleStartSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (world == null)
		{
			return;
		}

		// 發送UI計時器給所有玩家
		for (Player player : world.getPlayers())
		{
			player.sendPacket(new ExSendUIEvent(player, false, false,
					(int) (world.getRemainingTime() / 1000), 0,
					NpcStringId.WATER_TEMPLE_S_REMAINING_TIME));
		}

		// 顯示開始訊息
		world.broadcastPacket(new ExShowScreenMessage(
				NpcStringId.PLEASE_KILL_THE_MONSTERS_THAT_THREATEN_OUR_MOTHER_TREE,
				2, 10000, true));

		// 刷新階段1怪物
		world.spawnGroup("Stage1");

		// 刪除AOS NPC
		final Npc aosNpc = world.getNpc(AOS);
		if (aosNpc != null)
		{
			aosNpc.deleteMe();
		}
	}

	/**
	 * 處理階段擊殺計數
	 */
	private void handleStageKill(Instance world, int stageNum, int floodTriggerId,
								 ZoneType waterZone, String despawnGroup, String spawnGroup,
								 NpcStringId message)
	{
		final int currentStage = world.getParameters().getInt("stage", 1);
		if (currentStage != stageNum)
		{
			return; // 不是當前階段則不處理
		}

		// 增加擊殺計數
		final String killKey = "stage" + stageNum + "_kills";
		int kills = world.getParameters().getInt(killKey, 0) + 1;
		world.getParameters().set(killKey, kills);

		// 取得需要擊殺的總數
		final int requiredKills = STAGE_MONSTER_COUNTS.getOrDefault(stageNum, 20);

		// 檢查是否清完全部怪物
		if (kills >= requiredKills)
		{
			// 判斷是否觸發水淹
			if (Rnd.get(100) < FLOOD_CHANCE)
			{
				triggerFlood(world, floodTriggerId, waterZone, stageNum + 1,
						despawnGroup, spawnGroup, message);
			}
		}
	}

	/**
	 * 處理階段3擊殺(特殊處理)
	 */
	private void handleStage3Kill(Instance world, Npc npc, Player killer)
	{
		final int currentStage = world.getParameters().getInt("stage", 1);
		if (currentStage != 3)
		{
			return;
		}

		// 增加擊殺計數
		int kills = world.getParameters().getInt("stage3_kills", 0) + 1;
		world.getParameters().set("stage3_kills", kills);

		final int requiredKills = STAGE_MONSTER_COUNTS.getOrDefault(3, 12);

		// 檢查是否清完
		if (kills >= requiredKills)
		{
			// 判斷是否觸發水淹進入BOSS階段
			if (Rnd.get(100) < FLOOD_CHANCE)
			{
				world.broadcastPacket(new OnEventTrigger(FLOOD_STAGE_3, true));
				WATER_ZONE_3.setEnabled(true, world.getId());
				world.getParameters().set("stage", 4);
				world.despawnGroup("Stage3");
				sendEarthquake(world);

				// 4秒後刷新BOSS
				startQuestTimer("FINAL_BOSS", 4000, npc, killer);
			}
		}
	}

	/**
	 * 處理最終BOSS刷新
	 */
	private void handleFinalBoss(Npc npc, Player player)
	{
		final Instance world = npc.getInstanceWorld();
		if (world == null)
		{
			return;
		}

		// 隨機決定BOSS
		final boolean spawnElle = Rnd.get(100) < ELLE_BOSS_CHANCE;

		if (spawnElle)
		{
			world.broadcastPacket(new ExShowScreenMessage(
					NpcStringId.GODDESS_OF_WATER_ELLE_APPEARS, 2, 10000, true));
			addSpawn(ELLE, 71796, 242611, -6918, 16145, false, 0, false, world.getId());
		}
		else
		{
			world.broadcastPacket(new ExShowScreenMessage(
					NpcStringId.EVA_S_ADEPT_ABER_APPEARS, 2, 10000, true));
			addSpawn(ABER, 71796, 242611, -6918, 16145, false, 0, false, world.getId());
		}

		// 顯示最終地板水淹效果
		world.broadcastPacket(new OnEventTrigger(FLOOD_STAGE_FINAL_FLOOR_1, true));
		world.broadcastPacket(new OnEventTrigger(FLOOD_STAGE_FINAL_FLOOR_2, true));

		// 50秒後刷新侵蝕祭司
		startQuestTimer("EROSION_SPAWN", 50000, npc, null);
	}

	/**
	 * 觸發水淹效果
	 */
	private void triggerFlood(Instance world, int floodTriggerId, ZoneType waterZone,
							  int nextStage, String despawnGroup, String spawnGroup,
							  NpcStringId message)
	{
		// 觸發水淹視覺效果
		world.broadcastPacket(new OnEventTrigger(floodTriggerId, true));

		// 啟用水區域
		if (waterZone != null)
		{
			waterZone.setEnabled(true, world.getId());
		}

		// 顯示訊息
		if (message != null)
		{
			world.broadcastPacket(new ExShowScreenMessage(message,
					ExShowScreenMessage.BOTTOM_RIGHT, 10000, false));
		}

		// 更新階段
		world.getParameters().set("stage", nextStage);

		// 清除舊怪物
		if (despawnGroup != null)
		{
			world.despawnGroup(despawnGroup);
		}

		// 刷新新怪物
		if (spawnGroup != null)
		{
			world.spawnGroup(spawnGroup);
		}

		// 發送地震效果
		sendEarthquake(world);
	}

	/**
	 * 處理BOSS擊殺獎勵
	 */
	private void handleBossKill(Npc npc, Instance world)
	{
		// 取消侵蝕祭司刷新計時器
		cancelQuestTimer("EROSION_SPAWN", npc, null);

		// 發放獎勵
		final int rewardItemId = (npc.getId() == ABER) ? 101259 : 101260;

		for (Player player : world.getPlayers())
		{
			if ((player != null) && player.isOnline())
			{
				final String attackerKey = "attacker_" + player.getObjectId();
				final boolean didAttack = world.getParameters().getBoolean(attackerKey, false);

				if (didAttack)
				{
					player.addItem(ItemProcessType.REWARD, rewardItemId, 1, player, true);
				}
			}
		}

		// 刷新出口NPC
		addSpawn(ANIDA, 71242, 242569, -6922, 5826, false, 0, false, world.getId());

		// 結束副本
		world.finishInstance();
	}

	/**
	 * 發送地震效果
	 */
	private void sendEarthquake(Instance world)
	{
		for (Player player : world.getPlayers())
		{
			player.sendPacket(new Earthquake(player, 15, 5));
		}
	}

	/**
	 * 根據階段更新水淹視覺效果
	 */
	private void updateFloodVisuals(Player player, int stage)
	{
		switch (stage)
		{
			case 2:
			{
				player.sendPacket(new OnEventTrigger(FLOOD_STAGE_1, true));
				break;
			}
			case 3:
			{
				player.sendPacket(new OnEventTrigger(FLOOD_STAGE_1, true));
				player.sendPacket(new OnEventTrigger(FLOOD_STAGE_2, true));
				break;
			}
			case 4:
			{
				player.sendPacket(new OnEventTrigger(FLOOD_STAGE_1, true));
				player.sendPacket(new OnEventTrigger(FLOOD_STAGE_2, true));
				player.sendPacket(new OnEventTrigger(FLOOD_STAGE_3, true));
				player.sendPacket(new OnEventTrigger(FLOOD_STAGE_FINAL_FLOOR_1, true));
				player.sendPacket(new OnEventTrigger(FLOOD_STAGE_FINAL_FLOOR_2, true));
				break;
			}
		}
	}

	/**
	 * 傳送到當前階段位置
	 */
	private void teleportToCurrentStage(Player player, Instance world)
	{
		final int currentStage = world.getParameters().getInt("stage", 1);

		switch (currentStage)
		{
			case 1:
			{
				// 階段1保持在入口
				break;
			}
			case 2:
			{
				player.teleToLocation(TELEPORT_STAGE_2);
				break;
			}
			case 3:
			case 4:
			{
				player.teleToLocation(TELEPORT_STAGE_3);
				break;
			}
			default:
			{
				player.teleToLocation(TELEPORT_OUTSIDE, null);
				break;
			}
		}
	}

	public static void main(String[] args)
	{
		new WaterTemple();
	}
}