package custom.FusionSystem;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.variables.ItemVariables;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * Equipment Fusion System
 * @author 黑普羅
 */
public class FusionSystem extends Script
{
	// ==================== Configuration ====================
	private static final int NPC_ID = 900024;

	// Currently allowed slots (weapons only, can be expanded in future)
	private static final int[] ALLOWED_SLOTS = {
			5,  // RHAND (Right Hand Weapon)
			7   // LHAND (Left Hand Weapon/Shield)
	};

	// Fixed success rates
	private static final int FULL_SUCCESS_RATE = 10;    // 完全成功率: 10%
	private static final int PARTIAL_SUCCESS_RATE = 25; // 部分成功率: 25%
	// 完全失败率: 65% (100 - 10 - 25)

	// Fusion material configuration [itemId, count]
	private static final int[][] FUSION_MATERIALS = {
			{57, 100000000}
	};

	// Announcement threshold (enchant level)
	private static final int ANNOUNCE_THRESHOLD = 40;
	private static final int MAX_FUSED_ENCHANT = 100;

	public FusionSystem()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	// ==================== Event Handling ====================

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((event == null) || (player == null))
		{
			return "";
		}

		if (event.equals("main"))
		{
			return onFirstTalk(npc, player);
		}
		else if (event.equals("selectEquipment"))
		{
			showEquipmentSelectPage(player);
		}
		else if (event.startsWith("chooseEquipment_"))
		{
			int objectId = Integer.parseInt(event.substring(16));
			showFusionConfirmPage(player, objectId);
		}
		else if (event.startsWith("confirmFusion_"))
		{
			String[] parts = event.substring(14).split("_");
			int equipmentObjId = Integer.parseInt(parts[0]);
			int targetObjId = Integer.parseInt(parts[1]);
			processFusion(player, equipmentObjId, targetObjId);
		}

		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/FusionSystem/main.htm");
		player.sendPacket(html);
		return null;
	}

	// ==================== Page Display ====================

	/**
	 * Show equipment selection page
	 */
	private void showEquipmentSelectPage(Player player)
	{
		List<Item> equippedItems = new ArrayList<>();

		// Get equipped items from allowed slots
		for (int slot : ALLOWED_SLOTS)
		{
			Item item = player.getInventory().getPaperdollItem(slot);
			if (item != null && isValidFusionItem(item))
			{
				equippedItems.add(item);
			}
		}

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/FusionSystem/select.htm");
		html.replace("%equipment_list%", generateEquipmentList(player, equippedItems));
		player.sendPacket(html);
	}

	/**
	 * Show fusion confirmation page
	 */
	private void showFusionConfirmPage(Player player, int equipmentObjectId)
	{
		Item equipment = player.getInventory().getItemByObjectId(equipmentObjectId);
		if (equipment == null || !isValidFusionItem(equipment))
		{
			player.sendMessage("無法找到該裝備！");
			showEquipmentSelectPage(player);
			return;
		}

		// Find same items in inventory
		List<Item> sameItems = findSameItemsInInventory(player, equipment.getId(), equipmentObjectId);

		if (sameItems.isEmpty())
		{
			player.sendMessage("背包中沒有相同的裝備可以融合！");
			showEquipmentSelectPage(player);
			return;
		}

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/FusionSystem/confirm.htm");

		// Replace equipment info
		html.replace("%equipment_name%", equipment.getName());
		html.replace("%equipment_enchant%", String.valueOf(equipment.getEnchantLevel()));
		html.replace("%equipment_rebirth%", String.valueOf(equipment.getVariables().getInt(ItemVariables.zbzscsu, 0)));

		// Replace success rates
		html.replace("%full_success_rate%", String.valueOf(FULL_SUCCESS_RATE));
		html.replace("%partial_success_rate%", String.valueOf(PARTIAL_SUCCESS_RATE));
		html.replace("%fail_rate%", String.valueOf(100 - FULL_SUCCESS_RATE - PARTIAL_SUCCESS_RATE));

		// Replace material info
		html.replace("%material_list%", generateMaterialList(player));

		// Generate fusible item list
		html.replace("%target_list%", generateTargetItemList(player, equipment, sameItems));

		player.sendPacket(html);
	}

	// ==================== List Generation ====================

	/**
	 * Generate equipped item list
	 */
	private String generateEquipmentList(Player player, List<Item> items)
	{
		if (items.isEmpty())
		{
			return "<tr bgcolor=\"222222\"><td colspan=\"3\" align=\"center\" height=\"50\"><font color=\"808080\">沒有已裝備的武器</font></td></tr>";
		}

		StringBuilder sb = new StringBuilder();

		for (Item item : items)
		{
			int enchantLevel = item.getEnchantLevel();
			int rebirth = item.getVariables().getInt(ItemVariables.zbzscsu, 0);

			// Check if there are same items in inventory
			int sameItemCount = findSameItemsInInventory(player, item.getId(), item.getObjectId()).size();

			String enchantColor = getEnchantColor(enchantLevel);

			sb.append("<tr bgcolor=\"222222\" height=\"30\">");
			sb.append("<td  width=\"125\">");
			sb.append("<font color=\"00CCFF\">").append(item.getName()).append("</font>");
			sb.append("</td>");
			sb.append("<td  width=\"105\">");
			sb.append("<font color=\"").append(enchantColor).append("\">+").append(enchantLevel).append("</font>");
			sb.append(" / ");
			sb.append("<font color=\"FFFF00\">轉").append(rebirth).append("</font>");
			sb.append("</td>");
			sb.append("<td  width=\"50\">");

			if (sameItemCount > 0)
			{
				sb.append("<button value=\"融合\" action=\"bypass -h Quest FusionSystem chooseEquipment_")
						.append(item.getObjectId())
						.append("\" width=\"45\" height=\"22\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			}
			else
			{
				sb.append("<font color=\"FF0000\" size=\"1\">無可融合</font>");
			}

			sb.append("</td>");
			sb.append("</tr>");

			// Show fusible count
			sb.append("<tr bgcolor=\"222222\">");
			sb.append("<td colspan=\"3\" align=\"center\" height=\"15\">");
			sb.append("<font color=\"808080\" size=\"1\">背包可融合數量: ");
			sb.append(sameItemCount > 0 ? ("<font color=\"00FF00\">" + sameItemCount + "</font>") : "<font color=\"FF0000\">0</font>");
			sb.append("</font></td>");
			sb.append("</tr>");
		}

		return sb.toString();
	}

	/**
	 * Generate fusible target item list
	 */
	private String generateTargetItemList(Player player, Item sourceItem, List<Item> targetItems)
	{
		if (targetItems.isEmpty())
		{
			return "<tr bgcolor=\"222222\"><td colspan=\"3\" align=\"center\" height=\"50\"><font color=\"808080\">沒有可融合的裝備</font></td></tr>";
		}

		StringBuilder sb = new StringBuilder();

		for (Item target : targetItems)
		{
			int enchantLevel = target.getEnchantLevel();
			int rebirth = target.getVariables().getInt(ItemVariables.zbzscsu, 0);

			// Calculate two possible fused values
			int sourceEnchant = sourceItem.getEnchantLevel();
			int fullSuccessEnchant = sourceEnchant + enchantLevel;  // 完全成功：相加
			int partialSuccessEnchant = (sourceEnchant + enchantLevel) / 2;  // 部分成功：平均
			int fusedRebirth = Math.max(
					sourceItem.getVariables().getInt(ItemVariables.zbzscsu, 0),
					rebirth
			) / 2;

			String enchantColor = getEnchantColor(enchantLevel);

			sb.append("<tr bgcolor=\"222222\" height=\"30\">");
			sb.append("<td align=\"center\" width=\"100\">");
			sb.append("<font color=\"").append(enchantColor).append("\">+").append(enchantLevel).append("</font>");
			sb.append(" / ");
			sb.append("<font color=\"FFFF00\">轉").append(rebirth).append("</font>");
			sb.append("</td>");
			sb.append("<td align=\"center\" width=\"100\">");
			// 顯示兩種可能結果
			sb.append("<font color=\"00FF00\">+").append(fullSuccessEnchant).append("</font>");
			sb.append("<font color=\"808080\" size=\"1\">(完全)</font><br1>");
			sb.append("<font color=\"FFFF00\">+").append(partialSuccessEnchant).append("</font>");
			sb.append("<font color=\"808080\" size=\"1\">(部分)</font>");
			sb.append("</td>");
			sb.append("<td align=\"center\" width=\"80\">");
			sb.append("<button value=\"確認融合\" action=\"bypass -h Quest FusionSystem confirmFusion_")
					.append(sourceItem.getObjectId()).append("_").append(target.getObjectId())
					.append("\" width=\"70\" height=\"22\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			sb.append("</td>");
			sb.append("</tr>");
		}

		return sb.toString();
	}

	/**
	 * Generate material list
	 */
	private String generateMaterialList(Player player)
	{
		StringBuilder sb = new StringBuilder();

		for (int[] material : FUSION_MATERIALS)
		{
			int itemId = material[0];
			long requiredCount = material[1];
			long currentCount = player.getInventory().getInventoryItemCount(itemId, 0);

			ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
			String itemName = template != null ? template.getName() : "Unknown Item";

			boolean hasEnough = currentCount >= requiredCount;
			String color = hasEnough ? "00FF00" : "FF0000";

			sb.append("<tr>");
			sb.append("<td width=\"80\" ><font color=\"LEVEL\">").append(itemName).append("</font></td>");
			sb.append("<td width=\"200\" >");
			sb.append("<font color=\"").append(color).append("\">").append(formatNumber(currentCount)).append("</font>");
			sb.append(" / ");
			sb.append("<font color=\"FFD700\">").append(formatNumber(requiredCount)).append("</font>");
			sb.append("</td>");
			sb.append("</tr>");
		}

		return sb.toString();
	}

	// ==================== Fusion Processing ====================

	/**
	 * Process equipment fusion
	 */
	private void processFusion(Player player, int equipmentObjId, int targetObjId)
	{
		// Get both items
		Item equipment = player.getInventory().getItemByObjectId(equipmentObjId);
		Item target = player.getInventory().getItemByObjectId(targetObjId);

		// Validation
		if (equipment == null || target == null)
		{
			player.sendMessage("裝備不存在！");
			return;
		}

		if (!isValidFusionItem(equipment) || !isValidFusionItem(target))
		{
			player.sendMessage("只能融合武器！");
			return;
		}

		if (equipment.getId() != target.getId())
		{
			player.sendMessage("只能融合相同的裝備！");
			return;
		}

		// Check all materials
		boolean hasAllMaterials = true;
		for (int[] material : FUSION_MATERIALS)
		{
			int itemId = material[0];
			long requiredCount = material[1];
			long currentCount = player.getInventory().getInventoryItemCount(itemId, 0);

			if (currentCount < requiredCount)
			{
				ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
				String itemName = template != null ? template.getName() : "Unknown Item";
				player.sendMessage("材料不足！需要 " + formatNumber(requiredCount) + " 個 " + itemName);
				hasAllMaterials = false;
			}
		}

		if (!hasAllMaterials)
		{
			return;
		}

		// Consume all materials
		for (int[] material : FUSION_MATERIALS)
		{
			int itemId = material[0];
			long count = material[1];
			player.destroyItemByItemId(null, itemId, count, null, true);
		}

		// Calculate values
		int equipmentEnchant = equipment.getEnchantLevel();
		int targetEnchant = target.getEnchantLevel();
		int equipmentRebirth = equipment.getVariables().getInt(ItemVariables.zbzscsu, 0);
		int targetRebirth = target.getVariables().getInt(ItemVariables.zbzscsu, 0);

		// 計算兩種可能的融合結果
		int fullSuccessEnchant = equipmentEnchant + targetEnchant;  // 完全成功：相加
		int partialSuccessEnchant = (equipmentEnchant + targetEnchant) / 2;  // 部分成功：平均
		int fusedRebirth = Math.max(equipmentRebirth, targetRebirth) / 2;

		// 限制最大強化值
		fullSuccessEnchant = Math.min(fullSuccessEnchant, MAX_FUSED_ENCHANT);
		partialSuccessEnchant = Math.min(partialSuccessEnchant, MAX_FUSED_ENCHANT);

		// Determine result
		int roll = Rnd.get(100);

		if (roll < FULL_SUCCESS_RATE)
		{
			// ========== 完全成功：刪除兩件，給一件相加值的裝備 ==========
			player.destroyItem(null, equipment, null, true);
			player.destroyItem(null, target, null, true);
			giveItemsWithRebirth(player, equipment.getId(), 1, fullSuccessEnchant, fusedRebirth, false);

			player.sendMessage("========================================");
			player.sendMessage("融合完全成功！");
			player.sendMessage("獲得：" + equipment.getName() + " +" + fullSuccessEnchant + " 轉生" + fusedRebirth);
			player.sendMessage("(" + equipmentEnchant + " + " + targetEnchant + " = " + fullSuccessEnchant + ")");
			player.sendMessage("========================================");

			// Server announcement (if enchant is very high)
			if (fullSuccessEnchant >= ANNOUNCE_THRESHOLD)
			{
				String announcement = "【融合系統】恭喜玩家 " + player.getName() +
						" 完全成功融合出 " + equipment.getName() + " +" + fullSuccessEnchant + "！";
				Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, "系統公告", announcement));
			}
		}
		else if (roll < (FULL_SUCCESS_RATE + PARTIAL_SUCCESS_RATE))
		{
			// ========== 部分成功：刪除兩件，給一件平均值的裝備 ==========
			player.destroyItem(null, equipment, null, true);
			player.destroyItem(null, target, null, true);
			giveItemsWithRebirth(player, equipment.getId(), 1, partialSuccessEnchant, fusedRebirth, false);

			player.sendMessage("========================================");
			player.sendMessage("融合部分成功！");
			player.sendMessage("獲得：" + equipment.getName() + " +" + partialSuccessEnchant + " 轉生" + fusedRebirth);
			player.sendMessage("((" + equipmentEnchant + " + " + targetEnchant + ") / 2 = " + partialSuccessEnchant + ")");
			player.sendMessage("========================================");
		}
		else
		{
			// ========== 完全失敗：刪除兩件，什麼都不給 ==========
			player.destroyItem(null, equipment, null, true);
			player.destroyItem(null, target, null, true);

			player.sendMessage("========================================");
			player.sendMessage("融合失敗！");
			player.sendMessage("兩件裝備都已損毀...");
			player.sendMessage("失敗率：" + (100 - FULL_SUCCESS_RATE - PARTIAL_SUCCESS_RATE) + "%");
			player.sendMessage("========================================");
		}

		// Update player stats and refresh inventory
		player.updateZscsCache();
		player.broadcastUserInfo();
		player.sendItemList();

		// Refresh page
		showEquipmentSelectPage(player);
	}

	// ==================== Helper Methods ====================

	/**
	 * Check if item is valid for fusion
	 */
	private boolean isValidFusionItem(Item item)
	{
		// Currently only weapons, can be expanded in future
		return item != null && item.isWeapon();
	}

	/**
	 * Find same items in inventory
	 */
	private List<Item> findSameItemsInInventory(Player player, int itemId, int excludeObjectId)
	{
		List<Item> result = new ArrayList<>();

		for (Item item : player.getInventory().getItems())
		{
			if (item.getId() == itemId &&
					item.getObjectId() != excludeObjectId &&
					isValidFusionItem(item) &&
					!item.isEquipped())
			{
				result.add(item);
			}
		}

		return result;
	}

	/**
	 * Get enchant color based on level
	 */
	private String getEnchantColor(int enchant)
	{
		if (enchant >= 50) return "FF00FF"; // Purple
		if (enchant >= 30) return "FF0000"; // Red
		if (enchant >= 20) return "00FF00"; // Green
		if (enchant >= 10) return "FFFF00";  // Yellow
		return "FFFFFF"; // White
	}

	/**
	 * Format number with thousand separators
	 */
	private String formatNumber(long number)
	{
		return String.format("%,d", number);
	}

	/**
	 * Give items with enchant level and rebirth count
	 */
	private void giveItemsWithRebirth(Player player, int itemId, long count, int enchantLevel, int rebirthCount, boolean playSound)
	{
		if (count <= 0)
		{
			return;
		}

		final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
		if (template == null)
		{
			return;
		}

		// Add items to player's inventory
		final Item item = player.getInventory().addItem(ItemProcessType.QUEST, itemId, count, player, player.getTarget());
		if (item == null)
		{
			return;
		}

		// Set enchant level
		if (enchantLevel > 0)
		{
			item.setEnchantLevel(enchantLevel);
		}

		// Set rebirth count
		if (rebirthCount > 0)
		{
			item.getVariables().set(ItemVariables.zbzscsu, rebirthCount);
			item.getVariables().storeMe();
		}

		// Update player stats
		player.updateZscsCache();
		player.broadcastUserInfo();

		if (playSound)
		{
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "融合系統", "獲得物品"));
		}
	}

	public static void main(String[] args)
	{
		new FusionSystem();
		System.out.println("【系統】融合系統載入完畢！");
	}
}
