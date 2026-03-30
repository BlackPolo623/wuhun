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
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.MultiLayerDungeonConfig;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * 無限城迷宮入口NPC
 * @author Claude
 */
public class EntranceNpc extends Script
{
	private static final Logger LOGGER = Logger.getLogger(EntranceNpc.class.getName());

	public EntranceNpc()
	{
		addStartNpc(MultiLayerDungeonConfig.ENTRANCE_NPC_ID);
		addFirstTalkId(MultiLayerDungeonConfig.ENTRANCE_NPC_ID);
		addTalkId(MultiLayerDungeonConfig.ENTRANCE_NPC_ID);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "entrance.html";
	}

	@Override
	public String onTalk(Npc npc, Player player)
	{
		return "entrance.html";
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("enter".equals(event))
		{
			handleEnter(npc, player);
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// 入場邏輯：驗證隊伍、距離、道具，通過後整隊傳送進第一層
	// -------------------------------------------------------------------------

	private void handleEnter(Npc npc, Player player)
	{
		// 必須在隊伍中
		if (!player.isInParty())
		{
			player.sendMessage("你必須組隊才能踏入無限城迷宮。");
			return;
		}

		final Party party = player.getParty();

		// 必須是隊長才能發起入場
		if (!party.isLeader(player))
		{
			player.sendMessage("只有隊長才能帶領隊伍進入迷宮。");
			return;
		}

		final List<Player> members = party.getMembers();
		final int memberCount = members.size();
		final int minGroup = MultiLayerDungeonConfig.MIN_GROUP;
		final int maxGroup = MultiLayerDungeonConfig.MAX_GROUP;

		// 人數檢查
		if (memberCount < minGroup)
		{
			player.sendMessage("隊伍人數不足，至少需要 " + minGroup + " 名勇士同行（目前 " + memberCount + " 人）。");
			return;
		}
		if (memberCount > maxGroup)
		{
			player.sendMessage("隊伍人數已達上限，最多 " + maxGroup + " 人（目前 " + memberCount + " 人）。");
			return;
		}

		final int maxDist = MultiLayerDungeonConfig.MAX_ENTRY_DISTANCE;
		final int itemId = MultiLayerDungeonConfig.ENTRANCE_ITEM_ID;
		final long itemCount = MultiLayerDungeonConfig.ENTRANCE_ITEM_COUNT;
		final boolean needItem = (itemId > 0) && (itemCount > 0);

		// 逐一檢查每位隊員
		for (Player member : members)
		{
			// 距離檢查
			if (!member.isInsideRadius3D(npc, maxDist))
			{
				player.sendMessage(member.getName() + " 距離入口太遠，請讓所有隊員靠近後再試。");
				return;
			}

			// 已在副本中
			if (member.getInstanceWorld() != null)
			{
				player.sendMessage(member.getName() + " 目前仍在其他空間中，無法一同入場。");
				return;
			}

			// 入場道具檢查
			if (needItem)
			{
				final long owned = member.getInventory().getInventoryItemCount(itemId, -1);
				if (owned < itemCount)
				{
					player.sendMessage(member.getName() + " 入場道具不足（需要 " + itemCount + " 個，目前擁有 " + owned + " 個）。");
					return;
				}
			}
		}

		// ── 全部通過：選模板、建立副本 ──
		final int[] templates = MultiLayerDungeonConfig.getLayerTemplates(1);
		final int templateId = templates[Rnd.get(templates.length)];

		final Instance instance = InstanceManager.getInstance().createInstance(templateId, player);
		if (instance == null)
		{
			player.sendMessage("迷宮入口此刻被神秘力量封印，請稍後再試。");
			return;
		}

		// 扣除道具並傳送所有隊員
		for (Player member : members)
		{
			if (needItem)
			{
				member.destroyItemByItemId(ItemProcessType.FEE, itemId, itemCount, npc, true);
			}

			instance.addAllowed(member);
			member.getVariables().set("MULTI_LAYER_CURRENT", 1);
			member.teleToLocation(instance.getEnterLocation(), instance);
			member.sendPacket(new ExShowScreenMessage("歡迎踏入無限城迷宮 — 第一層", 5000));
		}

		LOGGER.info("無限城迷宮: 隊長 " + player.getName() + " 帶領 " + memberCount + " 人進入第1層，副本ID: " + instance.getId());
	}

	public static void main(String[] args)
	{
		new EntranceNpc();
	}
}
