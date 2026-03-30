/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package instances.MultiLayerDungeon;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.MultiLayerDungeonConfig;
import org.l2jmobius.gameserver.data.xml.MultiLayerRewardData;
import org.l2jmobius.gameserver.data.xml.MultiLayerRewardData.RewardConfig;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * 無限城迷宮 — 多層副本系統
 * @author Claude
 */
public class MultiLayerDungeon extends InstanceScript
{
	private static final Logger LOGGER = Logger.getLogger(MultiLayerDungeon.class.getName());

	// 副本模板ID（所有層共用）
	private static final int[] TEMPLATE_IDS = {300, 301, 302};

	// 副本內怪物ID列表（請根據實際配置修改）
	private static final int[] MONSTER_IDS =
	{
		22001, 22002, 22003, 22004, 22005,
		22006, 22007, 22008, 22009, 22010,
		22011, 22012, 22013, 22014, 22015
	};

	// Instance StatSet 參數 key
	private static final String PARAM_CURRENT_LAYER = "currentLayer";
	// 下一層副本已建立時，儲存其 instanceId 供其他玩家使用
	private static final String PARAM_NEXT_INSTANCE_ID = "nextInstanceId";

	// 需要全服廣播的最低層數
	private static final int BROADCAST_LAYER_MIN = 4;

	public MultiLayerDungeon()
	{
		super(TEMPLATE_IDS);

		addKillId(MONSTER_IDS);
		addSpawnId(MONSTER_IDS);
		addInstanceCreatedId(TEMPLATE_IDS);
		addFirstTalkId(MultiLayerDungeonConfig.TREASURE_CHEST_NPC_ID, MultiLayerDungeonConfig.TELEPORT_NPC_ID);
		addTalkId(MultiLayerDungeonConfig.TREASURE_CHEST_NPC_ID, MultiLayerDungeonConfig.TELEPORT_NPC_ID);
	}

	// -------------------------------------------------------------------------
	// Instance lifecycle
	// -------------------------------------------------------------------------

	@Override
	public void onInstanceCreated(Instance instance, Player player)
	{
		final int currentLayer = player.getVariables().getInt("MULTI_LAYER_CURRENT", 1);
		instance.getParameters().set(PARAM_CURRENT_LAYER, currentLayer);

		LOGGER.info("無限城迷宮: 玩家 " + player.getName() + " 進入第 " + currentLayer + " 層，副本ID: " + instance.getId());
	}

	// -------------------------------------------------------------------------
	// Spawn — 套用每層怪物能力倍率
	// -------------------------------------------------------------------------

	@Override
	public void onSpawn(Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance == null)
		{
			return;
		}

		final int layer = instance.getParameters().getInt(PARAM_CURRENT_LAYER, 1);

		// HP / MP（上限 100 億）
		final double hpMul = MultiLayerDungeonConfig.getHpMultiplier(layer);
		final double hpCap = 10_000_000_000.0 / npc.getMaxHp();
		final double mpCap = 10_000_000_000.0 / npc.getMaxMp();
		npc.getStat().mergeMul(Stat.MAX_HP, Math.min(hpMul, hpCap));
		npc.getStat().mergeMul(Stat.MAX_MP, Math.min(hpMul, mpCap));
		npc.setCurrentHp(npc.getMaxHp());
		npc.setCurrentMp(npc.getMaxMp());

		// 攻防倍率（不限制上限）
		npc.setPhysicalAttackMultiplier(MultiLayerDungeonConfig.getPAtkMultiplier(layer));
		npc.setMagicalAttackMultiplier(MultiLayerDungeonConfig.getMAtkMultiplier(layer));
		npc.setPhysicalDefenseMultiplier(MultiLayerDungeonConfig.getPDefMultiplier(layer));
		npc.setMagicalDefenseMultiplier(MultiLayerDungeonConfig.getMDefMultiplier(layer));

		// 攻擊速度 / 施法速度（1.0 時跳過）
		final double atkSpdMul = MultiLayerDungeonConfig.getAtkSpdMultiplier(layer);
		if (atkSpdMul != 1.0)
		{
			npc.setAttackSpeedMultiplier(atkSpdMul);
		}
		final double castSpdMul = MultiLayerDungeonConfig.getCastSpdMultiplier(layer);
		if (castSpdMul != 1.0)
		{
			npc.setCastSpeedMultiplier(castSpdMul);
		}

		// Final Damage Reduce（mergeAdd，因 base=0）
		final double fdr = MultiLayerDungeonConfig.getFinalDamageReduce(layer);
		if (fdr > 0)
		{
			npc.getStat().mergeAdd(Stat.FINAL_DAMAGE_REDUCE, fdr);
		}
	}

	// -------------------------------------------------------------------------
	// Kill — 清怪後生成寶箱與傳送門
	// -------------------------------------------------------------------------

	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance == null)
		{
			return;
		}

		if (!instance.getAliveNpcs(MONSTER_IDS).isEmpty())
		{
			return;
		}

		final int currentLayer = instance.getParameters().getInt(PARAM_CURRENT_LAYER, 1);

		instance.broadcastPacket(new ExShowScreenMessage("所有敵人已被消滅！神秘寶箱已現身！", 5000));
		instance.spawnGroup("treasure_chest");

		final int teleportChance = MultiLayerDungeonConfig.getTeleportChance(currentLayer);
		if ((currentLayer < MultiLayerDungeonConfig.MAX_LAYERS) && (Rnd.get(100) < teleportChance))
		{
			instance.spawnGroup("teleport_npc");
			instance.broadcastPacket(new ExShowScreenMessage("通往更深處的入口已開啟！前往下一層繼續挑戰吧！", 5000));
		}
		else
		{
			final long delaySeconds = MultiLayerDungeonConfig.EXIT_DELAY / 1000;
			if (currentLayer >= MultiLayerDungeonConfig.MAX_LAYERS)
			{
				instance.broadcastPacket(new ExShowScreenMessage("恭喜！你們征服了無限城迷宮的最深處！" + delaySeconds + "秒後將返回城鎮。", 5000));
			}
			else
			{
				instance.broadcastPacket(new ExShowScreenMessage("深處的入口此次未能開啟……" + delaySeconds + "秒後將返回城鎮。", 5000));
			}

			ThreadPool.schedule(() ->
			{
				for (Player p : instance.getPlayers())
				{
					p.teleToLocation(TeleportWhereType.TOWN);
				}
				instance.destroy();
			}, MultiLayerDungeonConfig.EXIT_DELAY);
		}
	}

	// -------------------------------------------------------------------------
	// Event — 個人點擊「踏入更深處」
	// -------------------------------------------------------------------------

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("enter_next_layer".equals(event))
		{
			handleEnterNextLayer(npc, player);
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// Talk — 寶箱互動 / 傳送門互動（顯示 HTML）
	// -------------------------------------------------------------------------

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance == null)
		{
			return null;
		}

		if (npc.getId() == MultiLayerDungeonConfig.TREASURE_CHEST_NPC_ID)
		{
			handleTreasureChest(npc, player, instance);
			return null;
		}

		if (npc.getId() == MultiLayerDungeonConfig.TELEPORT_NPC_ID)
		{
			return "teleport.html";
		}

		return null;
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (npc.getId() == MultiLayerDungeonConfig.TELEPORT_NPC_ID)
		{
			return "teleport.html";
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// 核心邏輯：個人進入下一層
	// 第一個玩家建立新副本並儲存 ID；後續玩家直接加入同一個副本
	// -------------------------------------------------------------------------

	private void handleEnterNextLayer(Npc npc, Player player)
	{
		final Instance oldInstance = player.getInstanceWorld();
		if (oldInstance == null)
		{
			player.sendMessage("你目前不在迷宮內。");
			return;
		}

		final int currentLayer = oldInstance.getParameters().getInt(PARAM_CURRENT_LAYER, 1);
		final int nextLayer = currentLayer + 1;

		if (nextLayer > MultiLayerDungeonConfig.MAX_LAYERS)
		{
			player.sendMessage("已抵達迷宮最深處，無路可進了。");
			return;
		}

		final InstanceManager instanceManager = InstanceManager.getInstance();
		Instance newInstance = null;

		// 嘗試讀取已建立的下一層副本 ID（其他隊友可能已建立）
		final int existingNextId = oldInstance.getParameters().getInt(PARAM_NEXT_INSTANCE_ID, -1);
		if (existingNextId > 0)
		{
			newInstance = instanceManager.getInstance(existingNextId);
		}

		// 若尚未有人建立，由本玩家負責建立
		if (newInstance == null)
		{
			final int[] templates = MultiLayerDungeonConfig.getLayerTemplates(nextLayer);
			final int templateId = templates[Rnd.get(templates.length)];

			newInstance = instanceManager.createInstance(templateId, player);
			if (newInstance == null)
			{
				player.sendMessage("深處的入口似乎被什麼力量封印了，請稍後再試。");
				return;
			}

			// 設置新副本的層數參數
			newInstance.getParameters().set(PARAM_CURRENT_LAYER, nextLayer);

			// 儲存新副本 ID 到舊副本，供其他玩家使用
			oldInstance.getParameters().set(PARAM_NEXT_INSTANCE_ID, newInstance.getId());

			LOGGER.info("無限城迷宮: 玩家 " + player.getName() + " 開啟第 " + nextLayer + " 層，副本ID: " + newInstance.getId());

			// 第4、5層發出全服廣播
			if (nextLayer >= BROADCAST_LAYER_MIN)
			{
				final Set<Player> oldPlayers = oldInstance.getPlayers();
				final StringBuilder nameList = new StringBuilder();
				for (Player p : oldPlayers)
				{
					if (nameList.length() > 0)
					{
						nameList.append("、");
					}
					nameList.append(p.getName());
				}
				final String announcement = nameList + " 成功開啟了第 " + nextLayer + " 層入口！";
				Broadcast.toAllOnlinePlayersOnScreen(announcement);
				Broadcast.toAllOnlinePlayers(announcement);
			}

			// 當舊副本內所有玩家都轉移後，延遲銷毀舊副本
			// 使用較長的延遲確保所有人都有時間進入
			ThreadPool.schedule(() ->
			{
				// 只有當舊副本內已無玩家時才銷毀
				if (oldInstance.getPlayers().isEmpty())
				{
					oldInstance.destroy();
				}
				else
				{
					// 若還有人沒進，強制傳回城鎮
					for (Player remaining : oldInstance.getPlayers())
					{
						remaining.sendMessage("傳送門已關閉，你被送回了城鎮。");
						remaining.teleToLocation(TeleportWhereType.TOWN);
					}
					oldInstance.destroy();
				}
			}, 60000); // 給 60 秒讓所有人進入
		}

		// 將玩家加入新副本並傳送
		newInstance.addAllowed(player);
		player.getVariables().set("MULTI_LAYER_CURRENT", nextLayer);
		player.teleToLocation(newInstance.getEnterLocation(), newInstance);
		player.sendPacket(new ExShowScreenMessage("你踏入了無限城迷宮第 " + nextLayer + " 層！", 5000));
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	private void handleTreasureChest(Npc npc, Player player, Instance instance)
	{
		final int currentLayer = instance.getParameters().getInt(PARAM_CURRENT_LAYER, 1);
		final List<RewardConfig> rewards = MultiLayerRewardData.getInstance().getRewards(currentLayer);

		if (rewards.isEmpty())
		{
			player.sendMessage("寶箱內空無一物……");
			npc.deleteMe();
			return;
		}

		int rewardCount = 0;
		for (RewardConfig reward : rewards)
		{
			if (Rnd.get(100) < reward.getChance())
			{
				player.addItem(ItemProcessType.NONE, reward.getItemId(), reward.getRandomCount(), npc, true);
				rewardCount++;
			}
		}

		player.sendMessage(rewardCount > 0 ? "你從寶箱中獲得了珍貴的戰利品！" : "寶箱內的財寶與你無緣，繼續前進吧。");
		npc.deleteMe();
	}

	public static void main(String[] args)
	{
		new MultiLayerDungeon();
	}
}
