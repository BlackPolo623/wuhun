package custom.PetHatchingSystem;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.holders.PetCollectionData;
import org.l2jmobius.gameserver.data.holders.PetHatchData;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.data.xml.PetHatchingDAO;
import org.l2jmobius.gameserver.data.xml.PetSnapshotData;
import org.l2jmobius.gameserver.data.xml.PetSnapshotData.PetSnapshotEntry;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.holders.custom.OnPlayerPetHatch;
import org.l2jmobius.gameserver.model.events.holders.custom.OnPlayerPetHatchStart;
import org.l2jmobius.gameserver.model.events.holders.custom.OnPlayerPetUpgrade;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * 寵物孵化系統
 * @author Custom
 */
public class PetHatchingSystem extends Script
{
	private static final int NPC_ID = 900037;

	// 配置
	private static Properties config = new Properties();
	private static int INITIAL_HATCH_SLOTS;
	private static int HATCH_COUNT_PER_SLOT;
	private static int MAX_HATCH_SLOTS;
	private static int NORMAL_HATCH_TIME;
	private static int SPECIAL_HATCH_TIME;
	private static int RARE_HATCH_TIME;
	private static int EPIC_HATCH_TIME;
	private static int LEGENDARY_HATCH_TIME;
	private static int BASE_UPGRADE_CHANCE;
	private static int VIP_UPGRADE_BONUS;
	private static int NORMAL_MAX_UPGRADE_CHANCE;
	private static int SPECIAL_MAX_UPGRADE_CHANCE;
	private static int RARE_MAX_UPGRADE_CHANCE;
	private static int EPIC_MAX_UPGRADE_CHANCE;
	private static int NORMAL_FEED_PER_PERCENT;
	private static int SPECIAL_FEED_PER_PERCENT;
	private static int RARE_FEED_PER_PERCENT;
	private static int EPIC_FEED_PER_PERCENT;
	private static int FEED_ITEM_ID;
	private static int NORMAL_EGG_ID;
	private static int SPECIAL_EGG_ID;
	private static int RARE_EGG_ID;
	private static int EPIC_EGG_ID;
	private static int LEGENDARY_EGG_ID;
	private static int NORMAL_PET_START_ID;
	private static int NORMAL_PET_END_ID;
	private static int SPECIAL_PET_START_ID;
	private static int SPECIAL_PET_END_ID;
	private static int RARE_PET_START_ID;
	private static int RARE_PET_END_ID;
	private static int EPIC_PET_START_ID;
	private static int EPIC_PET_END_ID;
	private static int LEGENDARY_PET_START_ID;
	private static int LEGENDARY_PET_END_ID;
	private static int COLLECTION_PET_START_ID;
	private static int COLLECTION_PET_END_ID;
	private static int COLLECTION_REWARD_SKILL_ID;

	private static final ConcurrentHashMap<String, ScheduledFuture<?>> hatchingTasks = new ConcurrentHashMap<>();
	private static final Random random = new Random();

	public PetHatchingSystem()
	{
		super();
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
		loadConfig();
		restoreAllHatchingTasks();
		registerPlayerLoginListener();
	}

	private void loadConfig()
	{
		try
		{
			File configFile = new File("data/scripts/custom/PetHatchingSystem/PetHatchingSystem.ini");
			if (!configFile.exists())
			{
				LOGGER.warning("PetHatchingSystem: 配置文件不存在!");
				return;
			}
			config.load(new FileInputStream(configFile));

			INITIAL_HATCH_SLOTS = Integer.parseInt(config.getProperty("InitialHatchSlots", "1"));
			HATCH_COUNT_PER_SLOT = Integer.parseInt(config.getProperty("HatchCountPerSlot", "50"));
			MAX_HATCH_SLOTS = Integer.parseInt(config.getProperty("MaxHatchSlots", "5"));
			NORMAL_HATCH_TIME = Integer.parseInt(config.getProperty("NormalHatchTime", "60"));
			SPECIAL_HATCH_TIME = Integer.parseInt(config.getProperty("SpecialHatchTime", "120"));
			RARE_HATCH_TIME = Integer.parseInt(config.getProperty("RareHatchTime", "240"));
			EPIC_HATCH_TIME = Integer.parseInt(config.getProperty("EpicHatchTime", "480"));
			LEGENDARY_HATCH_TIME = Integer.parseInt(config.getProperty("LegendaryHatchTime", "960"));
			BASE_UPGRADE_CHANCE = Integer.parseInt(config.getProperty("BaseUpgradeChance", "10"));
			VIP_UPGRADE_BONUS = Integer.parseInt(config.getProperty("VipUpgradeBonus", "5"));
			NORMAL_MAX_UPGRADE_CHANCE = Integer.parseInt(config.getProperty("NormalMaxUpgradeChance", "50"));
			SPECIAL_MAX_UPGRADE_CHANCE = Integer.parseInt(config.getProperty("SpecialMaxUpgradeChance", "40"));
			RARE_MAX_UPGRADE_CHANCE = Integer.parseInt(config.getProperty("RareMaxUpgradeChance", "30"));
			EPIC_MAX_UPGRADE_CHANCE = Integer.parseInt(config.getProperty("EpicMaxUpgradeChance", "20"));
			NORMAL_FEED_PER_PERCENT = Integer.parseInt(config.getProperty("NormalFeedPerPercent", "5"));
			SPECIAL_FEED_PER_PERCENT = Integer.parseInt(config.getProperty("SpecialFeedPerPercent", "8"));
			RARE_FEED_PER_PERCENT = Integer.parseInt(config.getProperty("RareFeedPerPercent", "10"));
			EPIC_FEED_PER_PERCENT = Integer.parseInt(config.getProperty("EpicFeedPerPercent", "15"));
			FEED_ITEM_ID = Integer.parseInt(config.getProperty("FeedItemId", "113000"));
			NORMAL_EGG_ID = Integer.parseInt(config.getProperty("NormalEggId", "112995"));
			SPECIAL_EGG_ID = Integer.parseInt(config.getProperty("SpecialEggId", "112996"));
			RARE_EGG_ID = Integer.parseInt(config.getProperty("RareEggId", "112997"));
			EPIC_EGG_ID = Integer.parseInt(config.getProperty("EpicEggId", "112998"));
			LEGENDARY_EGG_ID = Integer.parseInt(config.getProperty("LegendaryEggId", "112999"));
			NORMAL_PET_START_ID = Integer.parseInt(config.getProperty("NormalPetStartId", "113101"));
			NORMAL_PET_END_ID = Integer.parseInt(config.getProperty("NormalPetEndId", "113120"));
			SPECIAL_PET_START_ID = Integer.parseInt(config.getProperty("SpecialPetStartId", "113121"));
			SPECIAL_PET_END_ID = Integer.parseInt(config.getProperty("SpecialPetEndId", "113140"));
			RARE_PET_START_ID = Integer.parseInt(config.getProperty("RarePetStartId", "113141"));
			RARE_PET_END_ID = Integer.parseInt(config.getProperty("RarePetEndId", "113160"));
			EPIC_PET_START_ID = Integer.parseInt(config.getProperty("EpicPetStartId", "113161"));
			EPIC_PET_END_ID = Integer.parseInt(config.getProperty("EpicPetEndId", "113180"));
			LEGENDARY_PET_START_ID = Integer.parseInt(config.getProperty("LegendaryPetStartId", "113181"));
			LEGENDARY_PET_END_ID = Integer.parseInt(config.getProperty("LegendaryPetEndId", "113200"));
			COLLECTION_PET_START_ID = Integer.parseInt(config.getProperty("CollectionPetStartId", "113001"));
			COLLECTION_PET_END_ID = Integer.parseInt(config.getProperty("CollectionPetEndId", "113100"));
			COLLECTION_REWARD_SKILL_ID = Integer.parseInt(config.getProperty("CollectionRewardSkillId", "104999"));

			LOGGER.info("PetHatchingSystem: 配置加載成功!");
		}
		catch (Exception e)
		{
			LOGGER.warning("PetHatchingSystem: 配置加載失敗! " + e.getMessage());
		}
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return showMainMenu(player);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "main": return showMainMenu(player);
			case "hatch_menu": return handleHatchMenu(player);
			case "feed_menu": return handleFeedMenu(player);
			case "unclaimed_pets": return handleUnclaimedPets(player);
			case "snapshot_menu": return handleSnapshotMenu(player);
			case "save_snapshot": return handleSaveSnapshot(player);
			case "clear_snapshot": return handleClearSnapshot(player);
		}
		if (event.startsWith("collection_page_"))
		{
			return handleCollectionMenu(player, Integer.parseInt(event.substring(16)));
		}
		else if (event.startsWith("unclaimed_tier_page_"))
		{
			// format: unclaimed_tier_page_<tier>_<page>
			try
			{
				String[] parts = event.substring(20).split("_");
				int tier = Integer.parseInt(parts[0]);
				int page = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
				return handleUnclaimedPetsByTier(player, tier, page);
			}
			catch (NumberFormatException e)
			{
				return handleUnclaimedPets(player);
			}
		}
		else if (event.startsWith("unclaimed_tier_"))
		{
			// format: unclaimed_tier_<tier>
			try
			{
				int tier = Integer.parseInt(event.substring(15));
				return handleUnclaimedPetsByTier(player, tier, 0);
			}
			catch (NumberFormatException e)
			{
				return handleUnclaimedPets(player);
			}
		}
		else if (event.startsWith("claim_pet_byid "))
		{
			// format: claim_pet_byid <petItemId> <tier>
			String[] parts = event.substring(15).trim().split(" ");
			int petItemId = Integer.parseInt(parts[0]);
			int tier = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
			return handleClaimPetByItemId(player, petItemId, tier);
		}
		else if (event.startsWith("claim_pet "))
		{
			// format: claim_pet <unclaimedId> <tier> <page>
			String[] parts = event.substring(10).trim().split(" ");
			int unclaimedId = Integer.parseInt(parts[0]);
			int tier = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
			int page = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
			return handleClaimPet(player, unclaimedId, tier, page);
		}
		else if (event.startsWith("select_egg "))
		{
			return handleSelectEgg(player, Integer.parseInt(event.substring(11)));
		}
		else if (event.startsWith("start_hatch_with_egg "))
		{
			String[] parts = event.substring(21).split(" ");
			return handleStartHatchWithEgg(player, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
		}
		else if (event.startsWith("hatch_detail "))
		{
			return handleHatchDetail(player, Integer.parseInt(event.substring(13)));
		}
		else if (event.startsWith("start_hatch "))
		{
			return handleStartHatch(player, Integer.parseInt(event.substring(12)));
		}
		else if (event.startsWith("deposit_feed_confirm "))
		{
			return handleDepositFeedConfirm(player, event.substring(21).trim());
		}
		else if (event.startsWith("withdraw_feed_confirm "))
		{
			return handleWithdrawFeedConfirm(player, event.substring(22).trim());
		}
		else if (event.startsWith("deposit_feed "))
		{
			return handleDepositFeed(player, Integer.parseInt(event.substring(13)));
		}
		else if (event.startsWith("withdraw_feed "))
		{
			return handleWithdrawFeed(player, Integer.parseInt(event.substring(14)));
		}
		else if (event.equals("show_resetable_pets"))
		{
			return handleShowResetablePets(player, 0);
		}
		else if (event.startsWith("show_resetable_pets_"))
		{
			return handleShowResetablePets(player, Integer.parseInt(event.substring(20)));
		}
		else if (event.startsWith("confirm_reset_pet "))
		{
			// format: confirm_reset_pet <objectId> <petItemId> <page>
			String[] parts = event.substring(18).trim().split(" ");
			int objectId = Integer.parseInt(parts[0]);
			int petItemId = Integer.parseInt(parts[1]);
			int page = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
			return handleConfirmResetPet(player, objectId, petItemId, page);
		}
		else if (event.startsWith("force_reset_pet "))
		{
			// format: force_reset_pet <objectId> <petItemId> <page>
			String[] parts = event.substring(16).trim().split(" ");
			int objectId = Integer.parseInt(parts[0]);
			int petItemId = Integer.parseInt(parts[1]);
			int page = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
			return handleForceResetPet(player, objectId, petItemId, page);
		}
		else if (event.startsWith("store_pet "))
		{
			return handleStorePet(player, Integer.parseInt(event.substring(10)));
		}
		else if (event.startsWith("retrieve_pet "))
		{
			return handleRetrievePet(player, Integer.parseInt(event.substring(13)));
		}
		else if (event.equals("shop"))
		{
			MultisellData.getInstance().separateAndSend(9000371, player, npc, false);
			return null;
		}
		else if (event.equals("exchange_coins"))
		{
			MultisellData.getInstance().separateAndSend(90003701, player, npc, false);
			return null;
		}
		else if (event.equals("buy_supplies"))
		{
			MultisellData.getInstance().separateAndSend(90003702, player, npc, false);
			return null;
		}
		return showMainMenu(player);
	}

	// ==================== 未收藏 ====================

	private String showMainMenu(Player player)
	{
		int playerId = player.getObjectId();
		int storedFeed = PetHatchingDAO.getPlayerStoredFeed(playerId);
		int totalHatches = PetHatchingDAO.getPlayerTotalHatches(playerId);
		List<PetHatchData> hatchDataList = PetHatchingDAO.getPlayerHatchData(playerId);
		int unclaimedCount = PetHatchingDAO.getUnclaimedPetsCount(playerId);

		NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/scripts/custom/PetHatchingSystem/main.htm");
		html.replace("%stored_feed%", String.valueOf(storedFeed));
		html.replace("%hatching_count%", String.valueOf(hatchDataList.size()));
		html.replace("%total_hatches%", String.valueOf(totalHatches));
		html.replace("%unclaimed_count%", String.valueOf(unclaimedCount));
		player.sendPacket(html);
		return null;
	}

	private String handleHatchMenu(Player player)
	{
		int playerId = player.getObjectId();
		int totalHatches = PetHatchingDAO.getPlayerTotalHatches(playerId);
		int currentSlots = PetHatchingDAO.getPlayerCurrentSlots(playerId, INITIAL_HATCH_SLOTS);
		List<PetHatchData> hatchDataList = PetHatchingDAO.getPlayerHatchData(playerId);

		NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/scripts/custom/PetHatchingSystem/hatch_menu.htm");
		html.replace("%total_hatches%", String.valueOf(totalHatches));
		html.replace("%current_slots%", String.valueOf(currentSlots));
		html.replace("%max_slots%", String.valueOf(MAX_HATCH_SLOTS));

		StringBuilder slotsHtml = new StringBuilder();
		for (int i = 0; i < currentSlots; i++)
		{
			PetHatchData data = null;
			for (PetHatchData d : hatchDataList)
			{
				if (d.slotIndex == i) { data = d; break; }
			}
			slotsHtml.append(buildSlotHtml(i, data));
		}
		html.replace("%slots%", slotsHtml.toString());
		player.sendPacket(html);
		return null;
	}

	private String buildSlotHtml(int slotIndex, PetHatchData data)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<tr bgcolor=\"222222\">");
		sb.append("<td width=50 align=center>").append(slotIndex + 1).append("</td>");

		if (data == null)
		{
			sb.append("<td width=60 align=center>-</td>");
			sb.append("<td width=80 align=center>空閒</td>");
			sb.append("<td width=80 align=center><button value=\"開始孵化\" action=\"bypass -h Quest PetHatchingSystem select_egg ").append(slotIndex).append("\" width=70 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		}
		else
		{
			sb.append("<td width=60 align=center><font color=\"FFCC00\">").append(getTierName(data.eggTier)).append("</font></td>");
			long remainingTime = (data.startTime + data.hatchDuration * 60 * 1000L) - System.currentTimeMillis();
			if (remainingTime <= 0)
			{
				sb.append("<td width=80 align=center>已完成</td>");
				sb.append("<td width=80 align=center>處理中...</td>");
			}
			else
			{
				long minutes = remainingTime / 60000;
				sb.append("<td width=80 align=center>").append(minutes).append("分鐘</td>");
				sb.append("<td width=80 align=center><button value=\"詳情\" action=\"bypass -h Quest PetHatchingSystem hatch_detail ").append(slotIndex).append("\" width=70 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			}
		}
		sb.append("</tr>");
		return sb.toString();
	}

	private String handleFeedMenu(Player player)
	{
		int storedFeed = PetHatchingDAO.getPlayerStoredFeed(player.getObjectId());
		long inventoryFeed = player.getInventory().getInventoryItemCount(FEED_ITEM_ID, -1);

		NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/scripts/custom/PetHatchingSystem/feed_menu.htm");
		html.replace("%stored_feed%", String.valueOf(storedFeed));
		html.replace("%inventory_feed%", String.valueOf(inventoryFeed));
		player.sendPacket(html);
		return null;
	}

	private String handleCollectionMenu(Player player, int page)
	{
		int playerId = player.getObjectId();
		List<PetCollectionData> collectionList = PetHatchingDAO.getPlayerCollection(playerId);

		int totalCollected = 0;
		for (PetCollectionData d : collectionList)
		{
			if (d.stored) totalCollected++;
		}

		NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/scripts/custom/PetHatchingSystem/collection_menu.htm");
		html.replace("%total_collected%", String.valueOf(totalCollected));
		html.replace("%total_pets%", "100");
		html.replace("%page%", String.valueOf(page + 1));

		String rewardSkillName = "無";
		if (totalCollected > 0)
		{
			Skill skill = SkillData.getInstance().getSkill(COLLECTION_REWARD_SKILL_ID, totalCollected);
			if (skill != null)
			{
				rewardSkillName = skill.getName() + " Lv." + totalCollected;
			}
			else
			{
				rewardSkillName = "技能 " + COLLECTION_REWARD_SKILL_ID + " Lv." + totalCollected;
			}
		}
		html.replace("%reward_skill_name%", rewardSkillName);

		StringBuilder collectionHtml = new StringBuilder();
		int startIndex = page * 20;
		int endIndex = Math.min(startIndex + 20, 100);
		for (int i = startIndex; i < endIndex; i++)
		{
			int petItemId = COLLECTION_PET_START_ID + i;
			boolean stored = false;
			for (PetCollectionData d : collectionList)
			{
				if (d.petItemId == petItemId) { stored = d.stored; break; }
			}
			boolean hasInInventory = player.getInventory().getInventoryItemCount(petItemId, -1) > 0;
			collectionHtml.append(buildCollectionSlotHtml(i + 1, petItemId, stored, hasInInventory));
		}
		html.replace("%collection_list%", collectionHtml.toString());

		StringBuilder pageButtons = new StringBuilder();
		String[] tierNames = {"一般", "特殊", "稀有", "罕見", "傳說"};
		for (int i = 0; i < 5; i++)
		{
			if (i == page)
			{
				pageButtons.append("<td align=center><font color=\"LEVEL\">").append(tierNames[i]).append("</font></td>");
			}
			else
			{
				pageButtons.append("<td align=center><a action=\"bypass -h Quest PetHatchingSystem collection_page_").append(i).append("\">").append(tierNames[i]).append("</a></td>");
			}
		}
		html.replace("%page_buttons%", pageButtons.toString());

		player.sendPacket(html);
		return null;
	}

	private String buildCollectionSlotHtml(int index, int petItemId, boolean stored, boolean hasInInventory)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<tr bgcolor=\"222222\">");
		sb.append("<td width=50 align=center>").append(index).append("</td>");

		String itemName = "未知道具";
		org.l2jmobius.gameserver.model.item.ItemTemplate template = org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(petItemId);
		if (template != null)
		{
			itemName = template.getName();
		}

		sb.append("<td width=120 align=center>").append(itemName).append("</td>");
		if (stored)
		{
			sb.append("<td width=60 align=center><font color=\"00FF00\">已收藏</font></td>");
			sb.append("<td width=40 align=center><button value=\"提取\" action=\"bypass -h Quest PetHatchingSystem retrieve_pet ").append(petItemId).append("\" width=35 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		}
		else if (hasInInventory)
		{
			sb.append("<td width=60 align=center><font color=\"FFFF00\">可收藏</font></td>");
			sb.append("<td width=40 align=center><button value=\"收藏\" action=\"bypass -h Quest PetHatchingSystem store_pet ").append(petItemId).append("\" width=35 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		}
		else
		{
			sb.append("<td width=60 align=center><font color=\"FF0000\">未收藏</font></td>");
			sb.append("<td width=40 align=center>-</td>");
		}
		sb.append("</tr>");
		return sb.toString();
	}

	// ==================== 魂契系統 ====================

	/**
	 * 顯示魂契功能頁面
	 */
	private String handleSnapshotMenu(Player player)
	{
		final int playerId = player.getObjectId();
		final PetSnapshotEntry snap = PetSnapshotData.getInstance().getSnapshot(playerId);

		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/scripts/custom/PetHatchingSystem/snapshot_menu.htm");

		if (snap == null)
		{
			// 無魂契：顯示提示與締結按鈕
			html.replace("%snapshot_status%", "<font color=\"AAAAAA\">未締結</font>");
			html.replace("%snapshot_info%",
				"<tr bgcolor=\"222222\">" +
				"<td align=\"center\" colspan=\"2\"><font color=\"808080\" size=\"1\">請先在和平區召喚寵物後締結魂契</font></td>" +
				"</tr>");
			html.replace("%snapshot_button%",
				"<button value=\"締結魂契\" action=\"bypass -h Quest PetHatchingSystem save_snapshot\" " +
				"width=150 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		else
		{
			// 有魂契：顯示數據（含共享%）與解除按鈕
			final String petName = getPetItemName(snap.petItemId);
			final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm");
			final String timeStr = sdf.format(new java.util.Date(snap.snapshotTime));

			// 取得玩家目前的 ServitorShare 共享比率
			final java.util.Map<Stat, Float> shareRates = getServitorShareRates(player);

			html.replace("%snapshot_status%", "<font color=\"00FF66\">啟用中</font>");
			html.replace("%snapshot_info%",
				"<tr bgcolor=\"222222\">" +
				"<td align=\"center\" width=\"135\"><font color=\"FFCC33\">契約寵物</font></td>" +
				"<td align=\"center\" width=\"135\"><font color=\"FFFF00\">" + petName + "</font></td>" +
				"</tr>" +
				"<tr bgcolor=\"222222\">" +
				"<td align=\"center\"><font color=\"FFCC33\">締結時間</font></td>" +
				"<td align=\"center\"><font color=\"FFFFFF\">" + timeStr + "</font></td>" +
				"</tr>" +
				"<tr bgcolor=\"222222\">" +
				"<td align=\"center\"><font color=\"FFCC33\">物理攻擊</font>" +
				formatShareRate(shareRates, Stat.PHYSICAL_ATTACK) + "</td>" +
				"<td align=\"center\"><font color=\"00FF66\">" + (int) snap.patk + "</font></td>" +
				"</tr>" +
				"<tr bgcolor=\"222222\">" +
				"<td align=\"center\"><font color=\"FFCC33\">魔法攻擊</font>" +
				formatShareRate(shareRates, Stat.MAGIC_ATTACK) + "</td>" +
				"<td align=\"center\"><font color=\"00FF66\">" + (int) snap.matk + "</font></td>" +
				"</tr>" +
				"<tr bgcolor=\"222222\">" +
				"<td align=\"center\"><font color=\"FFCC33\">物理防禦</font>" +
				formatShareRate(shareRates, Stat.PHYSICAL_DEFENCE) + "</td>" +
				"<td align=\"center\"><font color=\"00FF66\">" + (int) snap.pdef + "</font></td>" +
				"</tr>" +
				"<tr bgcolor=\"222222\">" +
				"<td align=\"center\"><font color=\"FFCC33\">魔法防禦</font>" +
				formatShareRate(shareRates, Stat.MAGICAL_DEFENCE) + "</td>" +
				"<td align=\"center\"><font color=\"00FF66\">" + (int) snap.mdef + "</font></td>" +
				"</tr>");
			html.replace("%snapshot_button%",
				"<button value=\"解除魂契\" action=\"bypass -h Quest PetHatchingSystem clear_snapshot\" " +
				"width=150 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}

		player.sendPacket(html);
		return null;
	}

	/**
	 * 締結魂契：
	 * 1. 清除寵物臨時 Buff
	 * 2. 讀取 4 個 stat 數值
	 * 3. 儲存至 DB + 記憶體
	 * 4. 強制收回寵物
	 */
	private String handleSaveSnapshot(Player player)
	{
		// 條件檢查
		if (!player.isInsideZone(ZoneId.PEACE))
		{
			player.sendMessage("請在和平區域才能締結魂契！");
			return handleSnapshotMenu(player);
		}
		if (!player.hasPet())
		{
			player.sendMessage("請先召喚您的寵物，才能締結魂契！");
			return handleSnapshotMenu(player);
		}
		if (PetSnapshotData.getInstance().hasSnapshot(player.getObjectId()))
		{
			player.sendMessage("您已存在魂契，請先解除後再重新締結！");
			return handleSnapshotMenu(player);
		}

		final Summon pet = player.getPet();
		final Item controlItem = player.getInventory().getItemByObjectId(pet.getControlObjectId());
		final int petItemId = controlItem != null ? controlItem.getId() : 0;

		// 清除臨時 Buff，確保數值只含裝備 + 被動（排除臨時加成）
		pet.stopAllEffects();

		// 讀取 4 個屬性的最終值
		final double patk = pet.getStat().getValue(Stat.PHYSICAL_ATTACK);
		final double matk = pet.getStat().getValue(Stat.MAGIC_ATTACK);
		final double pdef = pet.getStat().getValue(Stat.PHYSICAL_DEFENCE);
		final double mdef = pet.getStat().getValue(Stat.MAGICAL_DEFENCE);

		// 儲存魂契數據
		PetSnapshotData.getInstance().saveSnapshot(player.getObjectId(), petItemId, patk, matk, pdef, mdef);

		// 強制收回寵物
		pet.unSummon(player);

		// 玩家 stat 立即重新計算，套用魂契效果
		player.getStat().recalculateStats(true);

		player.sendMessage("魂契締結成功！");
		player.sendMessage("寵物能力已刻印於您的身上。");
		player.sendMessage("無需召喚寵物即可共享力量。");


		return handleSnapshotMenu(player);
	}

	/**
	 * 解除魂契：清除 DB + 記憶體快取，重新計算玩家 stat
	 */
	private String handleClearSnapshot(Player player)
	{
		if (!PetSnapshotData.getInstance().hasSnapshot(player.getObjectId()))
		{
			player.sendMessage("您尚未締結魂契！");
			return handleSnapshotMenu(player);
		}

		PetSnapshotData.getInstance().clearSnapshot(player.getObjectId());
		player.getStat().recalculateStats(true);

		player.sendMessage("魂契已解除。寵物能力共享效果已移除。");
		player.sendMessage("您現在可以在和平區重新召喚寵物。");

		return handleSnapshotMenu(player);
	}

	/**
	 * 取得寵物道具名稱（輔助方法）
	 */
	private String getPetItemName(int petItemId)
	{
		if (petItemId <= 0) return "未知";
		final org.l2jmobius.gameserver.model.item.ItemTemplate template =
			org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(petItemId);
		return template != null ? template.getName() : "寵物 #" + petItemId;
	}

	/**
	 * 取得玩家目前所有 ServitorShare 技能各屬性的共享比率（累加）。
	 *
	 * 實作說明：
	 * 遍歷玩家所有已學習的技能（包含被動技能），從每個技能的 GENERAL 效果列表中
	 * 找出所有 ServitorShare 實例，讀取其 _sharedStats 並累加到結果中。
	 *
	 * 這樣可以支援多個技能都有 ServitorShare 效果的情況，例如：
	 * - 收藏獎勵技能（COLLECTION_REWARD_SKILL_ID）
	 * - 其他職業被動技能
	 * - 裝備賦予的技能
	 *
	 * 最終回傳的 Map 中，每個 Stat 的值是所有技能該 Stat 共享比率的總和。
	 */
	private java.util.Map<Stat, Float> getServitorShareRates(Player player)
	{
		final java.util.Map<Stat, Float> result = new java.util.EnumMap<>(Stat.class);

		// 遍歷玩家所有技能（包含被動、主動、裝備技能等）
		for (Skill skill : player.getAllSkills())
		{
			if (skill == null)
			{
				continue;
			}

			// 取得技能的 GENERAL 效果列表
			final java.util.List<AbstractEffect> effects = skill.getEffects(org.l2jmobius.gameserver.model.skill.EffectScope.GENERAL);
			if (effects == null || effects.isEmpty())
			{
				continue;
			}

			// 檢查是否有 ServitorShare 效果
			for (AbstractEffect effect : effects)
			{
				if (effect instanceof handlers.effecthandlers.ServitorShare)
				{
					final java.util.Map<Stat, Float> rates = ((handlers.effecthandlers.ServitorShare) effect).getSharedStats();
					if (rates != null && !rates.isEmpty())
					{
						// 累加每個 Stat 的共享比率
						for (java.util.Map.Entry<Stat, Float> entry : rates.entrySet())
						{
							final Stat stat = entry.getKey();
							final Float rate = entry.getValue();
							result.merge(stat, rate, Float::sum);
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * 格式化共享%顯示文字。
	 * 若該 Stat 有共享比率，回傳 " (X%)" 灰色文字；否則回傳空字串。
	 */
	private String formatShareRate(java.util.Map<Stat, Float> shareRates, Stat stat)
	{
		final Float rate = shareRates.get(stat);
		if (rate == null || rate <= 0) return "";
		// 將 0.0~1.0 轉為整數百分比（不足1%則顯示小數）
		final float pct = rate * 100f;
		final String pctStr = (pct == (int) pct) ? String.valueOf((int) pct) :
			String.format("%.1f", pct);
		return " <font color=\"AAAAAA\">(" + pctStr + "%)</font>";
	}

	// ==================== 業務邏輯 ====================

	private String handleStartHatch(Player player, int slotIndex)
	{
		int playerId = player.getObjectId();
		int currentSlots = PetHatchingDAO.getPlayerCurrentSlots(playerId, INITIAL_HATCH_SLOTS);
		if (slotIndex >= currentSlots)
		{
			player.sendMessage("該孵化槽尚未解鎖!");
			return handleHatchMenu(player);
		}
		if (PetHatchingDAO.getHatchDataBySlot(playerId, slotIndex) != null)
		{
			player.sendMessage("該孵化槽已被占用!");
			return handleHatchMenu(player);
		}
		int eggItemId = -1;
		int eggTier = -1;
		for (int i = 0; i <= 4; i++)
		{
			int checkId = getEggIdByTier(i);
			if (player.getInventory().getInventoryItemCount(checkId, -1) > 0)
			{
				eggItemId = checkId; eggTier = i; break;
			}
		}
		if (eggItemId == -1)
		{
			player.sendMessage("你沒有寵物蛋!");
			return handleHatchMenu(player);
		}
		if (!player.destroyItemByItemId(ItemProcessType.NONE, eggItemId, 1, player, true))
		{
			player.sendMessage("扣除寵物蛋失敗!");
			return handleHatchMenu(player);
		}
		int hatchTime = getHatchTime(eggTier);

		PetHatchingDAO.saveHatchData(playerId, slotIndex, eggItemId, eggTier, System.currentTimeMillis(), hatchTime, 0, BASE_UPGRADE_CHANCE);
		startHatchingTask(playerId, slotIndex, eggTier, hatchTime);
		player.sendMessage("開始孵化" + getTierName(eggTier) + "蛋!");
		return handleHatchMenu(player);
	}

	private String handleDepositFeed(Player player, int amount)
	{
		if (amount <= 0) { player.sendMessage("數量必須大於0!"); return handleFeedMenu(player); }
		long inventoryFeed = player.getInventory().getInventoryItemCount(FEED_ITEM_ID, -1);
		if (inventoryFeed < amount) { player.sendMessage("你沒有足夠的飼料!"); return handleFeedMenu(player); }
		if (!player.destroyItemByItemId(ItemProcessType.NONE, FEED_ITEM_ID, amount, player, true))
		{
			player.sendMessage("扣除飼料失敗!"); return handleFeedMenu(player);
		}
		int playerId = player.getObjectId();
		PetHatchingDAO.updatePlayerStoredFeed(playerId, PetHatchingDAO.getPlayerStoredFeed(playerId) + amount);
		player.sendMessage("成功存入" + amount + "個飼料!");
		return handleFeedMenu(player);
	}

	private String handleWithdrawFeed(Player player, int amount)
	{
		if (amount <= 0) { player.sendMessage("數量必須大於0!"); return handleFeedMenu(player); }
		int playerId = player.getObjectId();
		int storedFeed = PetHatchingDAO.getPlayerStoredFeed(playerId);
		if (storedFeed < amount) { player.sendMessage("儲存的飼料不足!"); return handleFeedMenu(player); }
		player.addItem(ItemProcessType.NONE, FEED_ITEM_ID, amount, player, true);
		PetHatchingDAO.updatePlayerStoredFeed(playerId, storedFeed - amount);
		player.sendMessage("成功提取" + amount + "個飼料!");
		return handleFeedMenu(player);
	}

	// ==================== 寵物洗白 ====================

	private static final int RESETABLE_PAGE_SIZE = 8;

	/**
	 * 列出玩家背包中所有「無法收藏」的寵物（有訓練記錄或有裝備），支援分頁。
	 */
	private String handleShowResetablePets(Player player, int page)
	{
		// 先收集所有需要洗白的寵物
		java.util.List<int[]> resetableList = new java.util.ArrayList<>(); // [petItemId, objectId, reason(0=trained,1=equip,2=both)]
		for (int petItemId = COLLECTION_PET_START_ID; petItemId <= COLLECTION_PET_END_ID; petItemId++)
		{
			if (player.getInventory().getInventoryItemCount(petItemId, -1) <= 0)
				continue;
			final Item petItem = player.getInventory().getItemByItemId(petItemId);
			if (petItem == null)
				continue;
			int objectId = petItem.getObjectId();
			boolean trained = PetHatchingDAO.isPetTrained(objectId);
			boolean hasEquip = PetHatchingDAO.hasPetEquipment(objectId);
			if (!trained && !hasEquip)
				continue;
			int reason = (trained && hasEquip) ? 2 : (trained ? 0 : 1);
			resetableList.add(new int[]{petItemId, objectId, reason});
		}

		int total = resetableList.size();
		int totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / RESETABLE_PAGE_SIZE);
		if (page < 0) page = 0;
		if (page >= totalPages) page = totalPages - 1;

		// 建立當頁列表
		StringBuilder rows = new StringBuilder();
		if (total == 0)
		{
			rows.append("<tr><td align=center colspan=3><font color=\"00FF00\">沒有需要洗白的寵物！所有寵物都可以正常收藏。</font></td></tr>");
		}
		else
		{
			int start = page * RESETABLE_PAGE_SIZE;
			int end = Math.min(start + RESETABLE_PAGE_SIZE, total);
			for (int i = start; i < end; i++)
			{
				int[] entry = resetableList.get(i);
				int petItemId = entry[0];
				int objectId  = entry[1];
				int reason    = entry[2];

				String itemName = "未知寵物";
				org.l2jmobius.gameserver.model.item.ItemTemplate template = org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(petItemId);
				if (template != null)
					itemName = template.getName();

				String reasonStr;
				if (reason == 2)
					reasonStr = "<font color=\"FF6666\">訓練記錄+裝備</font>";
				else if (reason == 0)
					reasonStr = "<font color=\"FFAA44\">有訓練記錄</font>";
				else
					reasonStr = "<font color=\"FFFF44\">有裝備</font>";

				rows.append("<tr bgcolor=\"222222\">");
				rows.append("<td width=120 align=center>").append(itemName).append("</td>");
				rows.append("<td width=110 align=center>").append(reasonStr).append("</td>");
				rows.append("<td width=60 align=center>");
				rows.append("<button value=\"洗白\" action=\"bypass -h Quest PetHatchingSystem confirm_reset_pet ").append(objectId).append(" ").append(petItemId).append(" ").append(page).append("\" width=50 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
				rows.append("</td>");
				rows.append("</tr>");
			}
		}

		// 建立分頁按鈕
		StringBuilder pageButtons = new StringBuilder();
		if (totalPages > 1)
		{
			pageButtons.append("<table border=0 cellpadding=0 cellspacing=2><tr>");
			if (page > 0)
				pageButtons.append("<td><button value=\"&lt; 上一頁\" action=\"bypass -h Quest PetHatchingSystem show_resetable_pets_").append(page - 1).append("\" width=80 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			pageButtons.append("<td><font color=\"AAAAAA\"> ").append(page + 1).append("/").append(totalPages).append(" </font></td>");
			if (page < totalPages - 1)
				pageButtons.append("<td><button value=\"下一頁 &gt;\" action=\"bypass -h Quest PetHatchingSystem show_resetable_pets_").append(page + 1).append("\" width=80 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			pageButtons.append("</tr></table>");
		}

		NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/scripts/custom/PetHatchingSystem/resetable_pets.htm");
		html.replace("%resetable_list%", rows.toString());
		html.replace("%count%", String.valueOf(total));
		html.replace("%page_buttons%", pageButtons.toString());
		player.sendPacket(html);
		return null;
	}

	/**
	 * 顯示洗白確認頁面，告知玩家將執行的動作。
	 */
	private String handleConfirmResetPet(Player player, int objectId, int petItemId, int page)
	{
		// 再次驗證此道具仍在背包且 objectId 吻合
		final Item petItem = player.getInventory().getItemByItemId(petItemId);
		if (petItem == null || petItem.getObjectId() != objectId)
		{
			player.sendMessage("找不到該寵物，請重新整理列表。");
			return handleShowResetablePets(player, page);
		}

		String itemName = "未知寵物";
		org.l2jmobius.gameserver.model.item.ItemTemplate template = org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(petItemId);
		if (template != null)
			itemName = template.getName();

		boolean hasEquip = PetHatchingDAO.hasPetEquipment(objectId);

		NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/scripts/custom/PetHatchingSystem/confirm_reset_pet.htm");
		html.replace("%pet_name%", itemName);
		html.replace("%equip_line%", hasEquip
			? "<tr bgcolor=\"222222\"><td align=center><font color=\"FF6666\">‧ 刪除寵物身上所有裝備（無法找回）</font></td></tr>"
			: "");
		html.replace("%confirm_bypass%", "bypass -h Quest PetHatchingSystem force_reset_pet " + objectId + " " + petItemId + " " + page);
		html.replace("%back_bypass%", "bypass -h Quest PetHatchingSystem show_resetable_pets_" + page);
		player.sendPacket(html);
		return null;
	}

	/**
	 * 執行洗白：刪除裝備、清除訓練記錄，換發全新寵物道具。
	 */
	private String handleForceResetPet(Player player, int objectId, int petItemId, int page)
	{
		// 防止召喚中的寵物被洗白
		if (player.getPet() != null && player.getPet().getControlObjectId() == objectId)
		{
			player.sendMessage("請先收回召喚中的寵物再進行洗白！");
			return handleShowResetablePets(player, page);
		}

		// 驗證道具仍在背包且 objectId 吻合
		final Item petItem = player.getInventory().getItemByItemId(petItemId);
		if (petItem == null || petItem.getObjectId() != objectId)
		{
			player.sendMessage("找不到該寵物，操作取消。");
			return handleShowResetablePets(player, page);
		}

		// 執行資料庫洗白（刪除裝備 + 刪除 pets 記錄）
		PetHatchingDAO.resetPetData(objectId);

		// 從背包移除舊寵物道具
		if (!player.destroyItem(ItemProcessType.NONE, petItem, player, true))
		{
			player.sendMessage("移除舊寵物道具失敗，請聯繫管理員。");
			return handleShowResetablePets(player, page);
		}

		// 給予全新的同種寵物道具
		player.addItem(ItemProcessType.NONE, petItemId, 1, player, true);

		String itemName = "寵物";
		org.l2jmobius.gameserver.model.item.ItemTemplate template = org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(petItemId);
		if (template != null)
			itemName = template.getName();

		player.sendMessage("洗白完成！[" + itemName + "] 已恢復為全新狀態，可以進行收藏了。");
		return handleShowResetablePets(player, page);
	}

	private String handleStorePet(Player player, int petItemId)
	{
		int playerId = player.getObjectId();
		int page = (petItemId - COLLECTION_PET_START_ID) / 20;

		// 召喚中的寵物不可收藏，防止把已召喚的寵物道具存入收藏
		if (player.getPet() != null)
		{
			player.sendMessage("請先收回召喚中的寵物，再進行收藏！");
			return handleCollectionMenu(player, page);
		}

		if (petItemId < COLLECTION_PET_START_ID || petItemId > COLLECTION_PET_END_ID)
		{
			player.sendMessage("無效的寵物道具!"); return handleCollectionMenu(player, 0);
		}
		if (PetHatchingDAO.isPetStored(playerId, petItemId))
		{
			player.sendMessage("該寵物已經收藏過了!"); return handleCollectionMenu(player, page);
		}
		long count = player.getInventory().getInventoryItemCount(petItemId, -1);
		if (count <= 0) { player.sendMessage("你沒有該寵物!"); return handleCollectionMenu(player, page); }
		if (count > 1) { player.sendMessage("你有多個該寵物，為避免存入練好的寵物，請先處理多餘的!"); return handleCollectionMenu(player, page); }

		// 檢查寵物是否有訓練痕跡或裝備（直接查資料庫，不依賴記憶體快取）
		final Item petItem = player.getInventory().getItemByItemId(petItemId);
		if (petItem != null)
		{
			if (PetHatchingDAO.isPetTrained(petItem.getObjectId()))
			{
				player.sendMessage("該寵物曾被培養過，無法收藏！請使用全新未培養的寵物。");
				return handleCollectionMenu(player, page);
			}

			// 檢查寵物身上是否有裝備
			if (PetHatchingDAO.hasPetEquipment(petItem.getObjectId()))
			{
				player.sendMessage("該寵物身上還有裝備，無法收藏！請先取下寵物的所有裝備。");
				return handleCollectionMenu(player, page);
			}
		}

		if (!player.destroyItemByItemId(ItemProcessType.NONE, petItemId, 1, player, true))
		{
			player.sendMessage("扣除寵物失敗!"); return handleCollectionMenu(player, page);
		}
		PetHatchingDAO.storePet(playerId, petItemId);
		updateCollectionRewardSkill(player);
		player.sendMessage("成功收藏寵物!");
		return handleCollectionMenu(player, page);
	}

	private String handleRetrievePet(Player player, int petItemId)
	{
		int playerId = player.getObjectId();
		int page = (petItemId - COLLECTION_PET_START_ID) / 20;
		if (!PetHatchingDAO.isPetStored(playerId, petItemId))
		{
			player.sendMessage("該寵物未收藏!"); return handleCollectionMenu(player, page);
		}
		player.addItem(ItemProcessType.NONE, petItemId, 1, player, true);
		PetHatchingDAO.removePet(playerId, petItemId);
		updateCollectionRewardSkill(player);
		player.sendMessage("成功提取寵物!");
		return handleCollectionMenu(player, page);
	}

	// ==================== 新增方法 ====================

	private static final int UNCLAIMED_PAGE_SIZE = 12;

	/** 分類總覽頁：每個階級一行，顯示待領取數量 */
	private String handleUnclaimedPets(Player player)
	{
		int playerId = player.getObjectId();
		List<org.l2jmobius.gameserver.data.holders.UnclaimedPetData> unclaimedPets = PetHatchingDAO.getUnclaimedPets(playerId);

		// 統計每個階級的數量
		int[] tierCounts = new int[5];
		for (org.l2jmobius.gameserver.data.holders.UnclaimedPetData petData : unclaimedPets)
		{
			if (petData.tier >= 0 && petData.tier < 5)
			{
				tierCounts[petData.tier]++;
			}
		}

		NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/scripts/custom/PetHatchingSystem/unclaimed_pets.htm");
		html.replace("%unclaimed_count%", String.valueOf(unclaimedPets.size()));

		// 建立各階級分類列
		String[] tierColors = {"AAAAAA", "88CCFF", "AAFFAA", "FFAA44", "FF88FF"};
		StringBuilder tierRows = new StringBuilder();
		for (int tier = 0; tier < 5; tier++)
		{
			int count = tierCounts[tier];
			tierRows.append("<tr bgcolor=\"222222\">");
			tierRows.append("<td width=90 align=center><font color=\"").append(tierColors[tier]).append("\">").append(getTierName(tier)).append("</font></td>");
			if (count > 0)
			{
				tierRows.append("<td width=80 align=center><font color=\"FF9900\">").append(count).append(" 隻</font></td>");
				tierRows.append("<td width=100 align=center><button value=\"查看\" action=\"bypass -h Quest PetHatchingSystem unclaimed_tier_").append(tier).append("\" width=70 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			}
			else
			{
				tierRows.append("<td width=80 align=center><font color=\"555555\">-</font></td>");
				tierRows.append("<td width=100 align=center><font color=\"555555\">-</font></td>");
			}
			tierRows.append("</tr>");
		}
		html.replace("%tier_rows%", tierRows.toString());

		player.sendPacket(html);
		return null;
	}

	/** 階級詳細頁：列出1~20號各種寵物，顯示各自的待領取數量 */
	private String handleUnclaimedPetsByTier(Player player, int tier, int page)
	{
		int playerId = player.getObjectId();
		List<org.l2jmobius.gameserver.data.holders.UnclaimedPetData> allPets = PetHatchingDAO.getUnclaimedPets(playerId);

		// 統計該階級每種 petItemId 各有幾隻
		java.util.Map<Integer, Integer> countByItemId = new java.util.LinkedHashMap<>();
		int startItemId = getPetStartIdByTier(tier);
		int endItemId = getPetEndIdByTier(tier);
		for (int itemId = startItemId; itemId <= endItemId; itemId++)
		{
			countByItemId.put(itemId, 0);
		}
		int totalCount = 0;
		for (org.l2jmobius.gameserver.data.holders.UnclaimedPetData petData : allPets)
		{
			if (petData.tier == tier && countByItemId.containsKey(petData.petItemId))
			{
				countByItemId.put(petData.petItemId, countByItemId.get(petData.petItemId) + 1);
				totalCount++;
			}
		}

		NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/scripts/custom/PetHatchingSystem/unclaimed_tier.htm");
		html.replace("%tier_name%", getTierName(tier));
		html.replace("%tier_count%", String.valueOf(totalCount));

		// 列出 1~20 號
		StringBuilder petsHtml = new StringBuilder();
		int slot = 1;
		for (java.util.Map.Entry<Integer, Integer> entry : countByItemId.entrySet())
		{
			int itemId = entry.getKey();
			int count = entry.getValue();
			String petName = org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(itemId) != null
				? org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(itemId).getName()
				: getTierName(tier) + slot + "號";

			petsHtml.append("<tr bgcolor=\"222222\">");
			petsHtml.append("<td width=30 align=center><font color=\"888888\">").append(slot).append("</font></td>");
			petsHtml.append("<td width=130 align=center>").append(petName).append("</td>");
			if (count > 0)
			{
				petsHtml.append("<td width=50 align=center><font color=\"FF9900\">").append(count).append("</font></td>");
				// claim_pet_byid <itemId> <tier>，領取一隻該種寵物
				petsHtml.append("<td width=60 align=center><button value=\"領取\" action=\"bypass -h Quest PetHatchingSystem claim_pet_byid ").append(itemId).append(" ").append(tier).append("\" width=55 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			}
			else
			{
				petsHtml.append("<td width=50 align=center><font color=\"444444\">-</font></td>");
				petsHtml.append("<td width=60 align=center><font color=\"444444\">-</font></td>");
			}
			petsHtml.append("</tr>");
			slot++;
		}
		html.replace("%pets_list%", petsHtml.toString());
		html.replace("%page_nav%", "");

		player.sendPacket(html);
		return null;
	}

	/** 依 itemId 領取一隻指定種類的寵物 */
	private String handleClaimPet(Player player, int unclaimedId, int tier, int page)
	{
		// 舊格式保留（事件路由仍可能觸發），轉為依 tier 回頁
		int playerId = player.getObjectId();
		org.l2jmobius.gameserver.data.holders.UnclaimedPetData petData = PetHatchingDAO.getUnclaimedPetById(playerId, unclaimedId);
		if (petData == null)
		{
			player.sendMessage("該寵物不存在或已被領取!");
			return handleUnclaimedPetsByTier(player, tier, 0);
		}
		player.addItem(ItemProcessType.NONE, petData.petItemId, 1, player, true);
		PetHatchingDAO.removeUnclaimedPet(playerId, unclaimedId);
		player.sendMessage("成功領取" + getTierName(petData.tier) + "寵物!");
		return handleUnclaimedPetsByTier(player, tier, 0);
	}

	/** 依 petItemId 領取一隻（從該種類任選一筆 unclaimed） */
	private String handleClaimPetByItemId(Player player, int petItemId, int tier)
	{
		int playerId = player.getObjectId();
		List<org.l2jmobius.gameserver.data.holders.UnclaimedPetData> allPets = PetHatchingDAO.getUnclaimedPets(playerId);

		org.l2jmobius.gameserver.data.holders.UnclaimedPetData target = null;
		for (org.l2jmobius.gameserver.data.holders.UnclaimedPetData petData : allPets)
		{
			if (petData.petItemId == petItemId)
			{
				target = petData;
				break;
			}
		}

		if (target == null)
		{
			player.sendMessage("該寵物不存在或已被領取!");
			return handleUnclaimedPetsByTier(player, tier, 0);
		}

		player.addItem(ItemProcessType.NONE, target.petItemId, 1, player, true);
		PetHatchingDAO.removeUnclaimedPet(playerId, target.id);
		player.sendMessage("成功領取" + getTierName(target.tier) + "寵物!");
		return handleUnclaimedPetsByTier(player, tier, 0);
	}

	private String handleSelectEgg(Player player, int slotIndex)
	{
		int playerId = player.getObjectId();
		int currentSlots = PetHatchingDAO.getPlayerCurrentSlots(playerId, INITIAL_HATCH_SLOTS);
		if (slotIndex >= currentSlots)
		{
			player.sendMessage("該孵化槽尚未解鎖!");
			return handleHatchMenu(player);
		}
		if (PetHatchingDAO.getHatchDataBySlot(playerId, slotIndex) != null)
		{
			player.sendMessage("該孵化槽已被占用!");
			return handleHatchMenu(player);
		}

		NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/scripts/custom/PetHatchingSystem/select_egg.htm");
		html.replace("%slot_number%", String.valueOf(slotIndex + 1));

		StringBuilder eggListHtml = new StringBuilder();
		String[] tierNames = {"一般蛋", "特殊蛋", "稀有蛋", "罕見蛋", "傳說蛋"};
		for (int tier = 0; tier <= 4; tier++)
		{
			int eggId = getEggIdByTier(tier);
			long count = player.getInventory().getInventoryItemCount(eggId, -1);
			if (count > 0)
			{
				eggListHtml.append("<tr bgcolor=\"222222\">");
				eggListHtml.append("<td width=100 align=center>").append(tierNames[tier]).append("</td>");
				eggListHtml.append("<td width=80 align=center>").append(count).append("</td>");
				eggListHtml.append("<td width=90 align=center><button value=\"選擇\" action=\"bypass -h Quest PetHatchingSystem start_hatch_with_egg ").append(slotIndex).append(" ").append(tier).append("\" width=70 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				eggListHtml.append("</tr>");
			}
		}

		if (eggListHtml.length() == 0)
		{
			eggListHtml.append("<tr bgcolor=\"222222\"><td colspan=3 align=center><font color=\"FF0000\">背包中沒有寵物蛋</font></td></tr>");
		}

		html.replace("%egg_list%", eggListHtml.toString());
		player.sendPacket(html);
		return null;
	}

	private String handleStartHatchWithEgg(Player player, int slotIndex, int eggTier)
	{
		int playerId = player.getObjectId();
		int eggItemId = getEggIdByTier(eggTier);

		if (player.getInventory().getInventoryItemCount(eggItemId, -1) <= 0)
		{
			player.sendMessage("你沒有該類型的寵物蛋!");
			return handleHatchMenu(player);
		}

		if (!player.destroyItemByItemId(ItemProcessType.NONE, eggItemId, 1, player, true))
		{
			player.sendMessage("扣除寵物蛋失敗!");
			return handleHatchMenu(player);
		}

		int hatchTime = getHatchTime(eggTier);

		// 開始孵化時就消耗飼料
		int storedFeed = PetHatchingDAO.getPlayerStoredFeed(playerId);
		int maxUpgradeChance = getMaxUpgradeChance(eggTier);

		// 【VIP加成】VIP會員的最高進階率額外+5%
		if (player.getVipTier() > 0)
		{
			maxUpgradeChance += VIP_UPGRADE_BONUS;
		}

		int feedPerPercent = getFeedPerPercent(eggTier);
		int currentChance = BASE_UPGRADE_CHANCE;
		int feedConsumed = 0;
		int maxIncrease = maxUpgradeChance - currentChance;

		if (maxIncrease > 0 && storedFeed > 0)
		{
			int feedToUse = Math.min(maxIncrease * feedPerPercent, storedFeed);
			PetHatchingDAO.updatePlayerStoredFeed(playerId, storedFeed - feedToUse);
			currentChance = Math.min(currentChance + feedToUse / feedPerPercent, maxUpgradeChance);
			feedConsumed = feedToUse;
		}

		// 傳說蛋（最高等級）無法進階，進階機率固定為 0
		if (eggTier >= 4)
		{
			currentChance = 0;
			feedConsumed = 0;
		}

		PetHatchingDAO.saveHatchData(playerId, slotIndex, eggItemId, eggTier, System.currentTimeMillis(), hatchTime, feedConsumed, currentChance);
		startHatchingTask(playerId, slotIndex, eggTier, hatchTime);

		// 【事件】觸發開始孵化事件（玩家必定在線）
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPetHatchStart(player, eggTier));

		if (eggTier >= 4)
		{
			player.sendMessage("開始孵化" + getTierName(eggTier) + "蛋！（已是最高等級，孵化完成後將直接獲得寵物）");
		}
		else
		{
			player.sendMessage("開始孵化" + getTierName(eggTier) + "蛋! 進階機率: " + currentChance + "%");
		}
		return handleHatchMenu(player);
	}

	private String handleHatchDetail(Player player, int slotIndex)
	{
		int playerId = player.getObjectId();
		PetHatchData data = PetHatchingDAO.getHatchDataBySlot(playerId, slotIndex);

		if (data == null)
		{
			player.sendMessage("該槽位沒有正在孵化的蛋!");
			return handleHatchMenu(player);
		}

		NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/scripts/custom/PetHatchingSystem/hatch_detail.htm");
		html.replace("%slot_number%", String.valueOf(slotIndex + 1));
		html.replace("%egg_tier%", getTierName(data.eggTier));

		long totalTime = data.hatchDuration * 60 * 1000L;
		long elapsed = System.currentTimeMillis() - data.startTime;
		long remaining = totalTime - elapsed;

		if (remaining < 0) remaining = 0;

		int progress = (int) ((elapsed * 100) / totalTime);
		if (progress > 100) progress = 100;

		html.replace("%progress%", progress + "%");
		html.replace("%remaining_time%", formatTime(remaining));
		html.replace("%hatch_time%", String.valueOf(data.hatchDuration));

		if (data.eggTier >= 4)
		{
			// 傳說蛋為最高等級，不顯示進階相關資訊
			html.replace("%upgrade_chance_row%",
				"<tr bgcolor=\"222222\">" +
				"<td align=\"center\"><font color=\"FFCC33\">進階機率</font></td>" +
				"<td align=\"center\"><font color=\"888888\">無（已是最高等級）</font></td>" +
				"</tr>");
			html.replace("%upgrade_desc%",
				"<tr><td><font color=\"888888\" size=\"1\">• 傳說為最高等級，孵化完成後將直接獲得傳說寵物</font></td></tr>");
			html.replace("%used_feed%", "0");
		}
		else
		{
			// 一般 / 特殊 / 稀有 / 史詩：正常顯示進階機率區塊
			html.replace("%upgrade_chance_row%",
				"<tr bgcolor=\"222222\">" +
				"<td align=\"center\"><font color=\"FFCC33\">進階機率</font></td>" +
				"<td align=\"center\"><font color=\"FF9900\">" + data.upgradeChance + "%</font></td>" +
				"</tr>");

			// 【VIP加成】VIP會員的最高進階率額外+5%
			int maxChance = getMaxUpgradeChance(data.eggTier);
			if (player.getVipTier() > 0)
			{
				maxChance += VIP_UPGRADE_BONUS;
			}
			int feedPerPercent = getFeedPerPercent(data.eggTier);
			html.replace("%upgrade_desc%",
				"<tr><td><font color=\"AAAAAA\" size=\"1\">• 基礎進階率：" + BASE_UPGRADE_CHANCE + "%</font></td></tr>" +
				"<tr><td height=3></td></tr>" +
				"<tr><td><font color=\"AAAAAA\" size=\"1\">• 最高進階率：" + maxChance + "%</font></td></tr>" +
				"<tr><td height=3></td></tr>" +
				"<tr><td><font color=\"AAAAAA\" size=\"1\">• 每 " + feedPerPercent + " 飼料提升 1% 進階率</font></td></tr>");
			html.replace("%used_feed%", String.valueOf(data.feedConsumed));
		}

		player.sendPacket(html);
		return null;
	}

	private String handleDepositFeedConfirm(Player player, String amountStr)
	{
		if (amountStr == null || amountStr.isEmpty())
		{
			player.sendMessage("請輸入數量!");
			return handleFeedMenu(player);
		}

		try
		{
			int amount = Integer.parseInt(amountStr);
			return handleDepositFeed(player, amount);
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("輸入格式錯誤，請輸入正整數!");
			return handleFeedMenu(player);
		}
	}

	private String handleWithdrawFeedConfirm(Player player, String amountStr)
	{
		if (amountStr == null || amountStr.isEmpty())
		{
			player.sendMessage("請輸入數量!");
			return handleFeedMenu(player);
		}

		try
		{
			int amount = Integer.parseInt(amountStr);
			return handleWithdrawFeed(player, amount);
		}
		catch (NumberFormatException e)
		{
			player.sendMessage("輸入格式錯誤，請輸入正整數!");
			return handleFeedMenu(player);
		}
	}

	private String formatTime(long milliseconds)
	{
		long seconds = milliseconds / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;

		if (days > 0)
		{
			return days + "天" + (hours % 24) + "小時";
		}
		else if (hours > 0)
		{
			return hours + "小時" + (minutes % 60) + "分鐘";
		}
		else if (minutes > 0)
		{
			return minutes + "分鐘";
		}
		else
		{
			return seconds + "秒";
		}
	}

	private void updateCollectionRewardSkill(Player player)
	{
		int totalCollected = PetHatchingDAO.getTotalCollectedCount(player.getObjectId());
		if (player.getSkillLevel(COLLECTION_REWARD_SKILL_ID) != totalCollected)
		{
			player.removeSkill(COLLECTION_REWARD_SKILL_ID);
		}
		if (totalCollected > 0)
		{
			Skill skill = SkillData.getInstance().getSkill(COLLECTION_REWARD_SKILL_ID, totalCollected);
			if (skill != null) player.addSkill(skill, true);
		}
	}

	// ==================== 孵化任務 ====================

	private void startHatchingTask(int playerId, int slotIndex, int eggTier, int hatchTimeMinutes)
	{
		String taskKey = playerId + "_" + slotIndex;
		ScheduledFuture<?> oldTask = hatchingTasks.remove(taskKey);
		if (oldTask != null && !oldTask.isDone()) oldTask.cancel(false);
		long delayMs = hatchTimeMinutes * 60 * 1000L;
		ScheduledFuture<?> task = ThreadPool.schedule(() -> processHatchCompletion(playerId, slotIndex, eggTier), delayMs);
		hatchingTasks.put(taskKey, task);
	}

	private void processHatchCompletion(int playerId, int slotIndex, int eggTier)
	{
		PetHatchData data = PetHatchingDAO.getHatchDataBySlot(playerId, slotIndex);
		if (data == null) return;

		int currentChance = data.upgradeChance;

		if (eggTier < 4 && random.nextInt(100) + 1 <= currentChance)
		{
			int newTier = eggTier + 1;
			int newHatchTime = getHatchTime(newTier);

			// 【事件】觸發寵物進階事件
			Player player = org.l2jmobius.gameserver.model.World.getInstance().getPlayer(playerId);
			if (player != null)
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPetUpgrade(player, eggTier, newTier));
			}

			// 進階時重新計算飼料消耗
			int storedFeed = PetHatchingDAO.getPlayerStoredFeed(playerId);
			int maxUpgradeChance = getMaxUpgradeChance(newTier);

			// 【VIP加成】VIP會員的最高進階率額外+5%
			if (player != null && player.getVipTier() > 0)
			{
				maxUpgradeChance += VIP_UPGRADE_BONUS;
			}

			int feedPerPercent = getFeedPerPercent(newTier);
			int newChance = BASE_UPGRADE_CHANCE;
			int feedConsumed = 0;
			int maxIncrease = maxUpgradeChance - newChance;

			if (maxIncrease > 0 && storedFeed > 0)
			{
				int feedToUse = Math.min(maxIncrease * feedPerPercent, storedFeed);
				PetHatchingDAO.updatePlayerStoredFeed(playerId, storedFeed - feedToUse);
				newChance = Math.min(newChance + feedToUse / feedPerPercent, maxUpgradeChance);
				feedConsumed = feedToUse;
			}

			PetHatchingDAO.updateHatchDataForUpgrade(playerId, slotIndex, getEggIdByTier(newTier), newTier, System.currentTimeMillis(), newHatchTime, newChance);
			PetHatchingDAO.updateHatchUpgradeChance(playerId, slotIndex, newChance, feedConsumed);
			startHatchingTask(playerId, slotIndex, newTier, newHatchTime);
		}
		else
		{
			int petItemId = getRandomPetItem(eggTier);
			PetHatchingDAO.addUnclaimedPet(playerId, petItemId, eggTier);
			PetHatchingDAO.deleteHatchData(playerId, slotIndex);
			PetHatchingDAO.incrementPlayerHatchCount(playerId, INITIAL_HATCH_SLOTS, HATCH_COUNT_PER_SLOT, MAX_HATCH_SLOTS);
			hatchingTasks.remove(playerId + "_" + slotIndex);

			// 【事件】觸發寵物孵化完成事件
			Player player = org.l2jmobius.gameserver.model.World.getInstance().getPlayer(playerId);
			if (player != null)
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPetHatch(player, petItemId, eggTier, false));
			}
		}
	}

	private void restoreAllHatchingTasks()
	{
		for (PetHatchData data : PetHatchingDAO.getAllHatchingData())
		{
			long remaining = (data.startTime + data.hatchDuration * 60 * 1000L) - System.currentTimeMillis();
			if (remaining > 0)
			{
				String taskKey = data.playerId + "_" + data.slotIndex;
				ScheduledFuture<?> task = ThreadPool.schedule(() -> processHatchCompletion(data.playerId, data.slotIndex, data.eggTier), remaining);
				hatchingTasks.put(taskKey, task);
			}
			else
			{
				ThreadPool.execute(() -> processHatchCompletion(data.playerId, data.slotIndex, data.eggTier));
			}
		}
	}

	private void sendPetByMail(int playerId, int petItemId, int tier)
	{
		Message msg = new Message(playerId, "寵物孵化系統", "恭喜你孵化出了" + getTierName(tier) + "寵物!", MailType.NPC);
		Mail attachments = msg.createAttachments();
		attachments.addItem(ItemProcessType.NONE, petItemId, 1, null, null);
		MailManager.getInstance().sendMessage(msg);
	}

	/**
	 * 註冊玩家登入監聽器，用於補發離線期間完成的孵化事件
	 */
	private void registerPlayerLoginListener()
	{
		org.l2jmobius.gameserver.model.events.Containers.Players().addListener(
			new org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener(
				org.l2jmobius.gameserver.model.events.Containers.Players(),
				org.l2jmobius.gameserver.model.events.EventType.ON_PLAYER_LOGIN,
				(org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin event) -> onPlayerLogin(event.getPlayer()),
				this
			)
		);
	}

	/**
	 * 玩家登入時：補發離線期間完成的孵化事件。
	 * 注意：魂契快照的 loadSnapshot 已移至核心 EnterWorld.java 執行，此處不重複。
	 */
	private void onPlayerLogin(Player player)
	{
		int playerId = player.getObjectId();

		// 孵化事件補發
		List<org.l2jmobius.gameserver.data.holders.UnclaimedPetData> unclaimedPets = PetHatchingDAO.getUnclaimedPets(playerId);
		for (org.l2jmobius.gameserver.data.holders.UnclaimedPetData petData : unclaimedPets)
		{
			if (!petData.eventFired)
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPetHatch(player, petData.petItemId, petData.tier, false));
				PetHatchingDAO.markEventFired(playerId, petData.id);
			}
		}
	}

	// ==================== 輔助方法 ====================

	private String getTierName(int tier)
	{
		switch (tier)
		{
			case 0: return "一般";
			case 1: return "特殊";
			case 2: return "稀有";
			case 3: return "罕見";
			case 4: return "傳說";
			default: return "未知";
		}
	}

	private int getEggIdByTier(int tier)
	{
		switch (tier)
		{
			case 0: return NORMAL_EGG_ID;
			case 1: return SPECIAL_EGG_ID;
			case 2: return RARE_EGG_ID;
			case 3: return EPIC_EGG_ID;
			case 4: return LEGENDARY_EGG_ID;
			default: return NORMAL_EGG_ID;
		}
	}

	private int getHatchTime(int tier)
	{
		switch (tier)
		{
			case 0: return NORMAL_HATCH_TIME;
			case 1: return SPECIAL_HATCH_TIME;
			case 2: return RARE_HATCH_TIME;
			case 3: return EPIC_HATCH_TIME;
			case 4: return LEGENDARY_HATCH_TIME;
			default: return NORMAL_HATCH_TIME;
		}
	}

	private int getMaxUpgradeChance(int tier)
	{
		switch (tier)
		{
			case 0: return NORMAL_MAX_UPGRADE_CHANCE;
			case 1: return SPECIAL_MAX_UPGRADE_CHANCE;
			case 2: return RARE_MAX_UPGRADE_CHANCE;
			case 3: return EPIC_MAX_UPGRADE_CHANCE;
			default: return 0;
		}
	}

	private int getFeedPerPercent(int tier)
	{
		switch (tier)
		{
			case 0: return NORMAL_FEED_PER_PERCENT;
			case 1: return SPECIAL_FEED_PER_PERCENT;
			case 2: return RARE_FEED_PER_PERCENT;
			case 3: return EPIC_FEED_PER_PERCENT;
			default: return 999;
		}
	}

	private int getPetStartIdByTier(int tier)
	{
		switch (tier)
		{
			case 0: return NORMAL_PET_START_ID;
			case 1: return SPECIAL_PET_START_ID;
			case 2: return RARE_PET_START_ID;
			case 3: return EPIC_PET_START_ID;
			case 4: return LEGENDARY_PET_START_ID;
			default: return NORMAL_PET_START_ID;
		}
	}

	private int getPetEndIdByTier(int tier)
	{
		switch (tier)
		{
			case 0: return NORMAL_PET_END_ID;
			case 1: return SPECIAL_PET_END_ID;
			case 2: return RARE_PET_END_ID;
			case 3: return EPIC_PET_END_ID;
			case 4: return LEGENDARY_PET_END_ID;
			default: return NORMAL_PET_END_ID;
		}
	}

	private int getRandomPetItem(int tier)
	{
		int startId, endId;
		switch (tier)
		{
			case 0: startId = NORMAL_PET_START_ID; endId = NORMAL_PET_END_ID; break;
			case 1: startId = SPECIAL_PET_START_ID; endId = SPECIAL_PET_END_ID; break;
			case 2: startId = RARE_PET_START_ID; endId = RARE_PET_END_ID; break;
			case 3: startId = EPIC_PET_START_ID; endId = EPIC_PET_END_ID; break;
			case 4: startId = LEGENDARY_PET_START_ID; endId = LEGENDARY_PET_END_ID; break;
			default: startId = NORMAL_PET_START_ID; endId = NORMAL_PET_END_ID;
		}
		return startId + random.nextInt(endId - startId + 1);
	}

	public static void main(String[] args)
	{
		new PetHatchingSystem();
	}
}
