package instances.DungeonTeleporter;

import java.util.List;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.MultisellData;
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
import instances.IceQueenTemple.IceQueenTemple;

/**
 * 副本整合入口傳送員
 * NPC 900049 統一管理所有副本的進入、掃蕩功能。
 *
 * 目前管理副本：
 *   - 火龍神殿（231）
 *   - 冰凍女王副本（232）
 */
public class DungeonTeleport extends InstanceScript
{
	/** 傳送 NPC ID */
	public static final int TELEPORT_NPC_ID = 900049;

	// ========================================
	// 火龍副本掃蕩設定
	// ========================================
	private static final int SWEEP_ITEM_ID   = 109005;
	private static final int SWEEP_ITEM_COUNT = 1;

	// ========================================
	// 冰凍女王副本掃蕩設定（一/二/三階消耗數量）
	// ========================================
	private static final int   ICE_SWEEP_ITEM_ID    = 109005;
	private static final int[] ICE_SWEEP_ITEM_COSTS = { 1, 2, 3 };

	// ========================================
	// Multisell IDs
	// ========================================
	private static final int MULTISELL_FIRE_DRAGON = 9000491;
	private static final int MULTISELL_ICE_QUEEN   = 9000492;
	private static final int EXCHANGE_ICE_QUEEN_1  = 9000493;
	private static final int EXCHANGE_ICE_QUEEN_2  = 9000494;
	private static final int EXCHANGE_ICE_QUEEN_3  = 9000495;

	// ========================================
	// 裝備過濾開關（玩家變數 key）
	// ========================================
	private static final String EQUIP_FILTER_VAR = "SWEEP_EQUIP_FILTER";

	private DungeonTeleport()
	{
		super(FireDragonTemple.FIRE_DRAGON_TEMPLE_INSTANCE_ID);
		addTalkId(TELEPORT_NPC_ID);
		addFirstTalkId(TELEPORT_NPC_ID);
		addStartNpc(TELEPORT_NPC_ID);
	}

	// ========================================
	// 主頁：副本選擇
	// ========================================

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/DungeonTeleporter/main.htm");
		player.sendPacket(html);
		return null;
	}

	// ========================================
	// 事件路由
	// ========================================

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			// ── 主選單 ────────────────────────────────────────────────────────
			case "mainPage":
			{
				onFirstTalk(npc, player);
				break;
			}
			// ── 火龍副本 ──────────────────────────────────────────────────────
			case "showFireDragon":
			{
				showFireDragonPage(npc, player);
				break;
			}
			case "enterFireDragon":
			{
				handleEnter(npc, player,
					FireDragonTemple.FIRE_DRAGON_TEMPLE_INSTANCE_ID,
					"火龍神殿");
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
			case "toggleEquipFilter":
			{
				player.getVariables().set(EQUIP_FILTER_VAR, !player.getVariables().getBoolean(EQUIP_FILTER_VAR, false));
				return handleShowSweep(npc, player);
			}
			// ── 冰凍女王副本 ──────────────────────────────────────────────────
			case "showIceQueen":
			{
				showIceQueenPage(npc, player);
				break;
			}
			case "enterIceQueen":
			{
				handleEnter(npc, player,
					IceQueenTemple.ICE_QUEEN_TEMPLE_INSTANCE_ID,
					"冰凍女王副本");
				break;
			}
			case "show_ice_sweep":
			{
				return handleShowIceSweepSelection(npc, player);
			}
			case "show_ice_sweep_1":
			case "show_ice_sweep_2":
			case "show_ice_sweep_3":
			{
				return handleShowIceSweepTier(npc, player, event.charAt(event.length() - 1) - '0');
			}
			case "execute_ice_sweep_1":
			case "execute_ice_sweep_2":
			case "execute_ice_sweep_3":
			{
				return handleExecuteIceSweep(npc, player, event.charAt(event.length() - 1) - '0');
			}
			case "toggleEquipFilterIce_1":
			case "toggleEquipFilterIce_2":
			case "toggleEquipFilterIce_3":
			{
				player.getVariables().set(EQUIP_FILTER_VAR, !player.getVariables().getBoolean(EQUIP_FILTER_VAR, false));
				return handleShowIceSweepTier(npc, player, event.charAt(event.length() - 1) - '0');
			}
			// ── Multisell ─────────────────────────────────────────────────────
			case "multisell_fire":
			{
				MultisellData.getInstance().separateAndSend(MULTISELL_FIRE_DRAGON, player, npc, false);
				break;
			}
			case "multisell_ice":
			{
				MultisellData.getInstance().separateAndSend(MULTISELL_ICE_QUEEN, player, npc, false);
				break;
			}
			case "exchange_ice_1":
			{
				MultisellData.getInstance().separateAndSend(EXCHANGE_ICE_QUEEN_1, player, npc, false);
				break;
			}
			case "exchange_ice_2":
			{
				MultisellData.getInstance().separateAndSend(EXCHANGE_ICE_QUEEN_2, player, npc, false);
				break;
			}
			case "exchange_ice_3":
			{
				MultisellData.getInstance().separateAndSend(EXCHANGE_ICE_QUEEN_3, player, npc, false);
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}

	// ========================================
	// 火龍副本頁面
	// ========================================

	private void showFireDragonPage(Npc npc, Player player)
	{
		final InstanceEntryManager manager = InstanceEntryManager.getInstance();
		final int instanceId = FireDragonTemple.FIRE_DRAGON_TEMPLE_INSTANCE_ID;
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/DungeonTeleporter/fire_dragon.htm");
		html.replace("%remaining%", String.valueOf(manager.getRemainingEntries(instanceId, player)));
		html.replace("%maxEntries%", String.valueOf(manager.getMaxEntries(instanceId)));
		html.replace("%resetTime%", manager.getNextResetString(instanceId));
		player.sendPacket(html);
	}

	// ========================================
	// 冰凍女王副本頁面
	// ========================================

	private void showIceQueenPage(Npc npc, Player player)
	{
		final InstanceEntryManager manager = InstanceEntryManager.getInstance();
		final int instanceId = IceQueenTemple.ICE_QUEEN_TEMPLE_INSTANCE_ID;
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/DungeonTeleporter/ice_queen.htm");
		html.replace("%remaining%", String.valueOf(manager.getRemainingEntries(instanceId, player)));
		html.replace("%maxEntries%", String.valueOf(manager.getMaxEntries(instanceId)));
		html.replace("%resetTime%", manager.getNextResetString(instanceId));
		player.sendPacket(html);
	}

	// ========================================
	// 共用進入副本邏輯
	// ========================================

	private void handleEnter(Npc npc, Player player, int instanceId, String dungeonName)
	{
		final InstanceEntryManager manager = InstanceEntryManager.getInstance();

		// GM 直接進入
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
			player.sendPacket(new ExShowScreenMessage("以 GM 身份進入" + dungeonName, 3000));
			return;
		}

		// 必須組隊
		if (!player.isInParty())
		{
			player.sendPacket(new ExShowScreenMessage("你必須組隊才能進入" + dungeonName, 3000));
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

	// ========================================
	// 火龍副本掃蕩
	// ========================================

	private String handleShowSweep(Npc npc, Player player)
	{
		if (!player.getVariables().getBoolean(FireDragonTemple.PLAYER_CLEARED_VAR, false))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, "data/scripts/instances/DungeonTeleporter/no-clear.htm");
			player.sendPacket(html);
			return null;
		}

		final InstanceEntryManager manager = InstanceEntryManager.getInstance();
		final int instanceId = FireDragonTemple.FIRE_DRAGON_TEMPLE_INSTANCE_ID;

		if (!manager.canEnter(instanceId, player))
		{
			player.sendPacket(new ExShowScreenMessage("本週掃蕩次數已用完，請等待每週重置後再試。", 4000));
			showFireDragonPage(npc, player);
			return null;
		}

		if (player.getInventory().getInventoryItemCount(SWEEP_ITEM_ID, -1) < SWEEP_ITEM_COUNT)
		{
			final String itemName = getItemName(SWEEP_ITEM_ID);
			player.sendPacket(new ExShowScreenMessage("掃蕩道具不足！需要 " + itemName + " x" + SWEEP_ITEM_COUNT, 4000));
			showFireDragonPage(npc, player);
			return null;
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/DungeonTeleporter/sweep.htm");
		html.replace("%sweep_item_name%", getItemName(SWEEP_ITEM_ID));
		html.replace("%sweep_item_count%", String.valueOf(SWEEP_ITEM_COUNT));
		html.replace("%remaining%", String.valueOf(manager.getRemainingEntries(instanceId, player)));
		html.replace("%maxEntries%", String.valueOf(manager.getMaxEntries(instanceId)));
		html.replace("%sweep_count%", String.valueOf(FireDragonTemple.SWEEP_REWARD_ITEMS_COUNT));
		final boolean filterOn = player.getVariables().getBoolean(EQUIP_FILTER_VAR, false);
		html.replace("%filter_status%", filterOn ? "<font color=00FF88>開啟</font>" : "<font color=888888>關閉</font>");
		html.replace("%filter_btn%", filterOn
			? "<button value=關閉 action=\"bypass -h Quest DungeonTeleport toggleEquipFilter\" width=80 height=20 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>"
			: "<button value=開啟 action=\"bypass -h Quest DungeonTeleport toggleEquipFilter\" width=80 height=20 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>");
		player.sendPacket(html);
		return null;
	}

	private String handleExecuteSweep(Npc npc, Player player)
	{
		if (!player.getVariables().getBoolean(FireDragonTemple.PLAYER_CLEARED_VAR, false))
		{
			player.sendMessage("你尚未通關火龍副本，無法使用掃蕩功能。");
			showFireDragonPage(npc, player);
			return null;
		}

		final InstanceEntryManager manager = InstanceEntryManager.getInstance();
		final int instanceId = FireDragonTemple.FIRE_DRAGON_TEMPLE_INSTANCE_ID;

		if (!manager.canEnter(instanceId, player))
		{
			player.sendMessage("本週掃蕩次數已用完。");
			showFireDragonPage(npc, player);
			return null;
		}

		if (player.getInventory().getInventoryItemCount(SWEEP_ITEM_ID, -1) < SWEEP_ITEM_COUNT)
		{
			player.sendMessage("掃蕩道具不足，操作取消。");
			showFireDragonPage(npc, player);
			return null;
		}

		if (!player.destroyItemByItemId(ItemProcessType.NONE, SWEEP_ITEM_ID, SWEEP_ITEM_COUNT, player, true))
		{
			player.sendMessage("扣除掃蕩道具失敗，請聯繫管理員。");
			return null;
		}

		manager.incrementEntryCount(instanceId, player);

		final boolean filterOn = player.getVariables().getBoolean(EQUIP_FILTER_VAR, false);
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
					if (filterOn)
					{
						final ItemTemplate tmpl = ItemData.getInstance().getTemplate(reward[0]);
						if ((tmpl != null) && tmpl.isEquipable())
						{
							break;
						}
					}
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

	// ========================================
	// 冰凍女王副本掃蕩 - 階級選擇
	// ========================================

	private String handleShowIceSweepSelection(Npc npc, Player player)
	{
		if (!player.getVariables().getBoolean(IceQueenTemple.PLAYER_ICE_CLEARED_VAR, false))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, "data/scripts/instances/DungeonTeleporter/no-clear-ice.htm");
			player.sendPacket(html);
			return null;
		}

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/DungeonTeleporter/sweep-ice.htm");
		html.replace("%ice_sweep_item_name%", getItemName(ICE_SWEEP_ITEM_ID));
		player.sendPacket(html);
		return null;
	}

	// ========================================
	// 冰凍女王副本掃蕩 - 各階確認頁
	// ========================================

	private String handleShowIceSweepTier(Npc npc, Player player, int tier)
	{
		if (!player.getVariables().getBoolean(IceQueenTemple.PLAYER_ICE_CLEARED_VAR, false))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			html.setFile(player, "data/scripts/instances/DungeonTeleporter/no-clear-ice.htm");
			player.sendPacket(html);
			return null;
		}

		final InstanceEntryManager manager = InstanceEntryManager.getInstance();
		final int instanceId = IceQueenTemple.ICE_QUEEN_TEMPLE_INSTANCE_ID;

		if (!manager.canEnter(instanceId, player))
		{
			player.sendPacket(new ExShowScreenMessage("本週冰凍女王掃蕩次數已用完，請等待每週重置後再試。", 4000));
			showIceQueenPage(npc, player);
			return null;
		}

		final int itemCost = ICE_SWEEP_ITEM_COSTS[tier - 1];
		if (player.getInventory().getInventoryItemCount(ICE_SWEEP_ITEM_ID, -1) < itemCost)
		{
			player.sendPacket(new ExShowScreenMessage("掃蕩道具不足！需要 " + getItemName(ICE_SWEEP_ITEM_ID) + " x" + itemCost, 4000));
			return handleShowIceSweepSelection(npc, player);
		}

		final String[] tierLabels  = { "一階", "二階", "三階" };
		final String[] tierColors  = { "44AAFF", "00DDAA", "FFAA00" };

		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/instances/DungeonTeleporter/sweep-ice-tier.htm");
		html.replace("%tier_label%",      tierLabels[tier - 1]);
		html.replace("%tier_color%",      tierColors[tier - 1]);
		html.replace("%sweep_item_name%", getItemName(ICE_SWEEP_ITEM_ID));
		html.replace("%sweep_item_count%", String.valueOf(itemCost));
		html.replace("%remaining%",       String.valueOf(manager.getRemainingEntries(instanceId, player)));
		html.replace("%maxEntries%",      String.valueOf(manager.getMaxEntries(instanceId)));
		html.replace("%sweep_count%",     String.valueOf(IceQueenTemple.SWEEP_REWARD_ITEMS_COUNT));
		final boolean filterOn = player.getVariables().getBoolean(EQUIP_FILTER_VAR, false);
		html.replace("%filter_status%", filterOn ? "<font color=00FF88>開啟</font>" : "<font color=888888>關閉</font>");
		html.replace("%filter_btn%", filterOn
			? "<button value=關閉 action=\"bypass -h Quest DungeonTeleport toggleEquipFilterIce_" + tier + "\" width=80 height=20 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>"
			: "<button value=開啟 action=\"bypass -h Quest DungeonTeleport toggleEquipFilterIce_" + tier + "\" width=80 height=20 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF>");
		html.replace("%execute_event%", "execute_ice_sweep_" + tier);
		player.sendPacket(html);
		return null;
	}

	// ========================================
	// 冰凍女王副本掃蕩 - 執行
	// ========================================

	private String handleExecuteIceSweep(Npc npc, Player player, int tier)
	{
		if (!player.getVariables().getBoolean(IceQueenTemple.PLAYER_ICE_CLEARED_VAR, false))
		{
			player.sendMessage("你尚未通關冰凍女王副本（第三階段），無法使用掃蕩功能。");
			showIceQueenPage(npc, player);
			return null;
		}

		final InstanceEntryManager manager = InstanceEntryManager.getInstance();
		final int instanceId = IceQueenTemple.ICE_QUEEN_TEMPLE_INSTANCE_ID;

		if (!manager.canEnter(instanceId, player))
		{
			player.sendMessage("本週冰凍女王掃蕩次數已用完。");
			showIceQueenPage(npc, player);
			return null;
		}

		final int itemCost = ICE_SWEEP_ITEM_COSTS[tier - 1];
		if (player.getInventory().getInventoryItemCount(ICE_SWEEP_ITEM_ID, -1) < itemCost)
		{
			player.sendMessage("掃蕩道具不足，操作取消。");
			return handleShowIceSweepSelection(npc, player);
		}

		if (!player.destroyItemByItemId(ItemProcessType.NONE, ICE_SWEEP_ITEM_ID, itemCost, player, true))
		{
			player.sendMessage("扣除掃蕩道具失敗，請聯繫管理員。");
			return null;
		}

		manager.incrementEntryCount(instanceId, player);

		final int[][] rewardTable  = (tier == 3) ? IceQueenTemple.REWARD_ITEMS_3 : (tier == 2) ? IceQueenTemple.REWARD_ITEMS_2 : IceQueenTemple.REWARD_ITEMS_1;
		final int     totalChance  = (tier == 3) ? IceQueenTemple.TOTAL_REWARD_CHANCE_3 : (tier == 2) ? IceQueenTemple.TOTAL_REWARD_CHANCE_2 : IceQueenTemple.TOTAL_REWARD_CHANCE_1;

		final boolean filterOn = player.getVariables().getBoolean(EQUIP_FILTER_VAR, false);
		for (int i = 0; i < IceQueenTemple.SWEEP_REWARD_ITEMS_COUNT; i++)
		{
			final int random = getRandom(totalChance);
			int currentChance = 0;
			for (int[] reward : rewardTable)
			{
				currentChance += reward[2];
				if (random < currentChance)
				{
					if (filterOn)
					{
						final ItemTemplate tmpl = ItemData.getInstance().getTemplate(reward[0]);
						if ((tmpl != null) && tmpl.isEquipable())
						{
							break;
						}
					}
					giveItems(player, reward[0], reward[1]);
					break;
				}
			}
		}

		final int remaining = manager.getRemainingEntries(instanceId, player);
		final int maxEntries = manager.getMaxEntries(instanceId);
		final String[] tierLabels = { "一階", "二階", "三階" };
		player.sendPacket(new ExShowScreenMessage("冰凍女王" + tierLabels[tier - 1] + "掃蕩完成！獎勵已發放至背包，剩餘次數：" + remaining + "/" + maxEntries, 5000));
		return handleShowIceSweepSelection(npc, player);
	}

	private static String getItemName(int itemId)
	{
		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		return template != null ? template.getName() : "道具ID " + itemId;
	}

	public static void main(String[] args)
	{
		new DungeonTeleport();
	}
}
