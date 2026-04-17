package instances.FireDragonTemple.Teleporter;

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

import instances.FireDragonTemple.FireDragonTemple;

/**
 * 火龍副本傳送員
 * 負責進入副本、掃蕩功能、次數顯示。
 */
public class FireDragonTeleport extends InstanceScript
{
	/** 傳送 NPC ID */
	public static final int TELEPORT_NPC_ID = 900049;

	// ========================================
	// 掃蕩設定
	// ========================================
	/** 掃蕩消耗的道具 ID */
	private static final int SWEEP_ITEM_ID = 109005;
	/** 掃蕩每次消耗的道具數量 */
	private static final int SWEEP_ITEM_COUNT = 1;

	private FireDragonTeleport()
	{
		super(FireDragonTemple.FIRE_DRAGON_TEMPLE_INSTANCE_ID);
		addTalkId(TELEPORT_NPC_ID);
		addFirstTalkId(TELEPORT_NPC_ID);
		addStartNpc(TELEPORT_NPC_ID);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final InstanceEntryManager manager = InstanceEntryManager.getInstance();
		final int instanceId = FireDragonTemple.FIRE_DRAGON_TEMPLE_INSTANCE_ID;
		final int remaining = manager.getRemainingEntries(instanceId, player);
		final int maxEntries = manager.getMaxEntries(instanceId);
		final String resetTime = manager.getNextResetString(instanceId);

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/FireDragonTemple/Teleporter/main.htm");
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
		final int instanceId = FireDragonTemple.FIRE_DRAGON_TEMPLE_INSTANCE_ID;

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
			player.sendPacket(new ExShowScreenMessage("以 GM 身份進入火龍副本", 3000));
			return;
		}

		// 必須組隊
		if (!player.isInParty())
		{
			player.sendPacket(new ExShowScreenMessage("你必須組隊才能進入火龍副本", 3000));
			return;
		}

		// 檢查隊長自身次數
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
			if (member.getLevel() < 90)
			{
				player.sendPacket(new ExShowScreenMessage("隊員 " + member.getName() + " 等級不足（需要90級）", 3000));
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

	private String handleShowSweep(Npc npc, Player player)
	{
		// ① 未通關過
		if (!player.getVariables().getBoolean(FireDragonTemple.PLAYER_CLEARED_VAR, false))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, "data/scripts/instances/FireDragonTemple/Teleporter/no-clear.htm");
			player.sendPacket(html);
			return null;
		}

		final InstanceEntryManager manager = InstanceEntryManager.getInstance();
		final int instanceId = FireDragonTemple.FIRE_DRAGON_TEMPLE_INSTANCE_ID;

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
		html.setFile(player, "data/scripts/instances/FireDragonTemple/Teleporter/sweep.htm");
		html.replace("%sweep_item_name%", getItemName(SWEEP_ITEM_ID));
		html.replace("%sweep_item_count%", String.valueOf(SWEEP_ITEM_COUNT));
		html.replace("%remaining%", String.valueOf(manager.getRemainingEntries(instanceId, player)));
		html.replace("%maxEntries%", String.valueOf(manager.getMaxEntries(instanceId)));
		player.sendPacket(html);
		return null;
	}

	private String handleExecuteSweep(Npc npc, Player player)
	{
		// 再次驗證
		if (!player.getVariables().getBoolean(FireDragonTemple.PLAYER_CLEARED_VAR, false))
		{
			player.sendMessage("你尚未通關火龍副本，無法使用掃蕩功能。");
			return onFirstTalk(npc, player);
		}

		final InstanceEntryManager manager = InstanceEntryManager.getInstance();
		final int instanceId = FireDragonTemple.FIRE_DRAGON_TEMPLE_INSTANCE_ID;

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

		// 發放掃蕩獎勵
		for (int i = 0; i < FireDragonTemple.SWEEP_REWARD_ITEMS_COUNT; i++)
		{
			int totalChance = 0;
			for (int[] reward : FireDragonTemple.REWARD_ITEMS)
			{
				totalChance += reward[2];
			}
			final int random = getRandom(totalChance);
			int currentChance = 0;
			for (int[] reward : FireDragonTemple.REWARD_ITEMS)
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

	private static String getItemName(int itemId)
	{
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		return template != null ? template.getName() : "道具ID " + itemId;
	}

	public static void main(String[] args)
	{
		new FireDragonTeleport();
	}
}
