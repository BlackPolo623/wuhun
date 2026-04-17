/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package instances.ValakasTemple.Teleporter;

import java.util.List;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.managers.events.InstanceEntryManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

import instances.ValakasTemple.ValakasTemple;

/**
 * @author Index (simplified version)
 */
public class ValakasTempleTeleport extends InstanceScript
{
	public static final int PARME_NPC_ID = 34258;

	// ========================================
	// 設定區：可自行調整
	// ========================================
	/** 掃蕩消耗的道具 ID（請修改為你要設定的道具） */
	private static final int SWEEP_ITEM_ID = 109005;
	/** 掃蕩每次消耗的道具數量 */
	private static final int SWEEP_ITEM_COUNT = 1;
	// ========================================

	private ValakasTempleTeleport()
	{
		super(ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID);
		addTalkId(PARME_NPC_ID);
		addFirstTalkId(PARME_NPC_ID);
		addStartNpc(PARME_NPC_ID);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final InstanceEntryManager manager = InstanceEntryManager.getInstance();
		final int instanceId = ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID;
		final int remaining = manager.getRemainingEntries(instanceId, player);
		final int maxEntries = manager.getMaxEntries(instanceId);
		final String resetTime = manager.getNextResetString(instanceId);

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/ValakasTemple/Teleporter/34258.htm");
		html.replace("%remaining%", String.valueOf(remaining));
		html.replace("%maxEntries%", String.valueOf(maxEntries));
		html.replace("%resetTime%", resetTime);
		player.sendPacket(html);
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enterInstance":
			{
				handleEnter(npc, player);
				break;
			}
			case "show_sweep":
			{
				return handleShowSweep(npc, player);
			}
			case "execute_sweep":
			{
				return handleExecuteSweep(npc, player);
			}
		}
		return super.onEvent(event, npc, player);
	}

	private void handleEnter(Npc npc, Player player)
	{
		final InstanceEntryManager manager = InstanceEntryManager.getInstance();
		final int instanceId = ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID;

		// GM 可直接進入（單人或帶隊）
		if (player.isGM())
		{
			if (player.isInParty())
			{
				for (Player member : player.getParty().getMembers())
				{
					enterInstance(member, npc, instanceId);
				}
			}
			else
			{
				enterInstance(player, npc, instanceId);
			}
			player.sendPacket(new ExShowScreenMessage("以 GM 身份進入巴拉卡斯神殿", 3000));
			return;
		}

		// 必須組隊
		if (!player.isInParty())
		{
			player.sendPacket(new ExShowScreenMessage("你必須組隊才能進入巴拉卡斯神殿", 3000));
			return;
		}

		// 檢查自己的次數
		if (!manager.canEnter(instanceId, player))
		{
			final int remaining = manager.getRemainingEntries(instanceId, player);
			final int maxEntries = manager.getMaxEntries(instanceId);
			player.sendPacket(new ExShowScreenMessage("你本週的進入次數已用完 (" + remaining + "/" + maxEntries + ")", 3000));
			return;
		}

		final Party party = player.getParty();
		final List<Player> members = party.getMembers();

		// 先檢查所有隊員條件
		for (Player member : members)
		{
			if (!member.isInsideRadius3D(npc, 1500))
			{
				player.sendPacket(new ExShowScreenMessage("隊員 " + member.getName() + " 距離太遠", 3000));
				return;
			}
			if (member.getLevel() < 76)
			{
				player.sendPacket(new ExShowScreenMessage("隊員 " + member.getName() + " 等級不足（需要76級）", 3000));
				return;
			}
		}

		// 全部符合條件，逐一傳送
		for (Player member : members)
		{
			if (manager.canEnter(instanceId, member))
			{
				enterInstance(member, npc, instanceId);
			}
			else
			{
				member.sendPacket(new ExShowScreenMessage("你本週的進入次數已用完", 3000));
			}
		}
	}

	/**
	 * 掃蕩前置檢查：通關資格、次數、道具，全部通過才顯示確認頁。
	 */
	private String handleShowSweep(Npc npc, Player player)
	{
		// ① 未通關過
		if (!player.getVariables().getBoolean(ValakasTemple.PLAYER_CLEARED_VAR, false))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, "data/scripts/instances/ValakasTemple/Teleporter/34258-no-clear.htm");
			player.sendPacket(html);
			return null;
		}

		final InstanceEntryManager manager = InstanceEntryManager.getInstance();
		final int instanceId = ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID;

		// ② 週次數已用完
		if (!manager.canEnter(instanceId, player))
		{
			player.sendPacket(new ExShowScreenMessage("本週掃蕩次數已用完，請等待每週重置後再試。", 4000));
			return onFirstTalk(npc, player);
		}

		// ③ 道具不足
		if (player.getInventory().getInventoryItemCount(SWEEP_ITEM_ID, -1) < SWEEP_ITEM_COUNT)
		{
			final String itemName = getItemName(SWEEP_ITEM_ID);
			player.sendPacket(new ExShowScreenMessage("掃蕩道具不足！需要 " + itemName + " x" + SWEEP_ITEM_COUNT, 4000));
			return onFirstTalk(npc, player);
		}

		// 全部通過 → 顯示確認頁
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/ValakasTemple/Teleporter/34258-sweep.htm");
		html.replace("%sweep_item_name%", getItemName(SWEEP_ITEM_ID));
		html.replace("%sweep_item_count%", String.valueOf(SWEEP_ITEM_COUNT));
		html.replace("%remaining%", String.valueOf(manager.getRemainingEntries(instanceId, player)));
		html.replace("%maxEntries%", String.valueOf(manager.getMaxEntries(instanceId)));
		player.sendPacket(html);
		return null;
	}

	/**
	 * 執行掃蕩：再次驗證條件後消耗道具、次數，並直接發放通關獎勵。
	 */
	private String handleExecuteSweep(Npc npc, Player player)
	{
		// 再次驗證（防止中途條件改變）
		if (!player.getVariables().getBoolean(ValakasTemple.PLAYER_CLEARED_VAR, false))
		{
			player.sendMessage("你尚未通關巴拉卡斯神殿，無法使用掃蕩功能。");
			return onFirstTalk(npc, player);
		}

		final InstanceEntryManager manager = InstanceEntryManager.getInstance();
		final int instanceId = ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID;

		if (!manager.canEnter(instanceId, player))
		{
			player.sendMessage("本週掃蕩次數已用完。");
			return onFirstTalk(npc, player);
		}

		if (player.getInventory().getInventoryItemCount(SWEEP_ITEM_ID, -1) < SWEEP_ITEM_COUNT)
		{
			player.sendMessage("掃蕩道具不足，操作取消。");
			return onFirstTalk(npc, player);
		}

		// 消耗道具
		if (!player.destroyItemByItemId(ItemProcessType.NONE, SWEEP_ITEM_ID, SWEEP_ITEM_COUNT, player, true))
		{
			player.sendMessage("扣除掃蕩道具失敗，請聯繫管理員。");
			return null;
		}

		// 消耗一次週次數
		manager.incrementEntryCount(instanceId, player);

		// 發放與通關副本相同的獎勵（相同機率表）
		for (int i = 0; i < ValakasTemple.SWEEP_REWARD_ITEMS_COUNT; i++)
		{
			int totalChance = 0;
			for (int[] reward : ValakasTemple.REWARD_ITEMS)
			{
				totalChance += reward[2];
			}
			final int random = getRandom(totalChance);
			int currentChance = 0;
			for (int[] reward : ValakasTemple.REWARD_ITEMS)
			{
				currentChance += reward[2];
				if (random < currentChance)
				{
					giveItems(player, reward[0], reward[1]);
					break;
				}
			}
		}

		final int remaining = manager.getRemainingEntries(instanceId, player);
		final int maxEntries = manager.getMaxEntries(instanceId);
		player.sendPacket(new ExShowScreenMessage("掃蕩完成！獎勵已發放至背包，剩餘次數：" + remaining + "/" + maxEntries, 5000));
		return handleShowSweep(npc, player);
	}

	/** 取得道具名稱，找不到時回傳 ID 字串。 */
	private static String getItemName(int itemId)
	{
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		return template != null ? template.getName() : "道具ID " + itemId;
	}

	public static void main(String[] args)
	{
		new ValakasTempleTeleport();
	}
}
