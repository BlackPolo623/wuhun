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
import org.l2jmobius.gameserver.data.xml.SkillData;
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
		}
		if (event.startsWith("collection_page_"))
		{
			return handleCollectionMenu(player, Integer.parseInt(event.substring(16)));
		}
		else if (event.startsWith("claim_pet "))
		{
			return handleClaimPet(player, Integer.parseInt(event.substring(10)));
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

	private String handleStorePet(Player player, int petItemId)
	{
		int playerId = player.getObjectId();
		int page = (petItemId - COLLECTION_PET_START_ID) / 20;
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

	private String handleUnclaimedPets(Player player)
	{
		int playerId = player.getObjectId();
		List<org.l2jmobius.gameserver.data.holders.UnclaimedPetData> unclaimedPets = PetHatchingDAO.getUnclaimedPets(playerId);

		NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/scripts/custom/PetHatchingSystem/unclaimed_pets.htm");
		html.replace("%unclaimed_count%", String.valueOf(unclaimedPets.size()));

		StringBuilder petsHtml = new StringBuilder();
		if (unclaimedPets.isEmpty())
		{
			petsHtml.append("<tr bgcolor=\"222222\"><td colspan=3 align=center><font color=\"AAAAAA\">目前沒有未領取的寵物</font></td></tr>");
		}
		else
		{
			for (org.l2jmobius.gameserver.data.holders.UnclaimedPetData petData : unclaimedPets)
			{
				String petName = org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(petData.petItemId) != null
					? org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(petData.petItemId).getName()
					: "未知寵物";

				petsHtml.append("<tr bgcolor=\"222222\">");
				petsHtml.append("<td width=80 align=center><font color=\"FFCC00\">").append(getTierName(petData.tier)).append("</font></td>");
				petsHtml.append("<td width=120 align=center>").append(petName).append("</td>");
				petsHtml.append("<td width=70 align=center><button value=\"領取\" action=\"bypass -h Quest PetHatchingSystem claim_pet ").append(petData.id).append("\" width=60 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				petsHtml.append("</tr>");
			}
		}

		html.replace("%pets_list%", petsHtml.toString());
		player.sendPacket(html);
		return null;
	}

	private String handleClaimPet(Player player, int unclaimedId)
	{
		int playerId = player.getObjectId();
		org.l2jmobius.gameserver.data.holders.UnclaimedPetData petData = PetHatchingDAO.getUnclaimedPetById(playerId, unclaimedId);

		if (petData == null)
		{
			player.sendMessage("該寵物不存在或已被領取!");
			return handleUnclaimedPets(player);
		}

		player.addItem(ItemProcessType.NONE, petData.petItemId, 1, player, true);
		PetHatchingDAO.removeUnclaimedPet(playerId, unclaimedId);
		player.sendMessage("成功領取" + getTierName(petData.tier) + "寵物!");
		return handleUnclaimedPets(player);
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
		String[] tierNames = {"一般蛋", "特殊蛋", "稀有蛋", "史詩蛋", "傳說蛋"};
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

		PetHatchingDAO.saveHatchData(playerId, slotIndex, eggItemId, eggTier, System.currentTimeMillis(), hatchTime, feedConsumed, currentChance);
		startHatchingTask(playerId, slotIndex, eggTier, hatchTime);

		// 【事件】觸發開始孵化事件（玩家必定在線）
		System.out.println("[PetHatchingSystem] Firing OnPlayerPetHatchStart event for player " + player.getName() + ", eggTier=" + eggTier);
		EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPetHatchStart(player, eggTier));
		System.out.println("[PetHatchingSystem] Event fired successfully");

		player.sendMessage("開始孵化" + getTierName(eggTier) + "蛋! 進階機率: " + currentChance + "%");
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
		html.replace("%upgrade_chance%", String.valueOf(data.upgradeChance));
		html.replace("%used_feed%", String.valueOf(data.feedConsumed));
		html.replace("%hatch_time%", String.valueOf(data.hatchDuration));
		html.replace("%base_chance%", String.valueOf(BASE_UPGRADE_CHANCE));

		// 【VIP加成】VIP會員的最高進階率額外+5%
		int maxChance = getMaxUpgradeChance(data.eggTier);
		if (player.getVipTier() > 0)
		{
			maxChance += VIP_UPGRADE_BONUS;
		}
		html.replace("%max_chance%", String.valueOf(maxChance));

		int feedPerPercent = getFeedPerPercent(data.eggTier);
		html.replace("%feed_per_percent%", String.valueOf(feedPerPercent));

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
	 * 玩家登入時檢查並補發離線期間完成的孵化事件
	 */
	private void onPlayerLogin(Player player)
	{
		int playerId = player.getObjectId();
		List<org.l2jmobius.gameserver.data.holders.UnclaimedPetData> unclaimedPets = PetHatchingDAO.getUnclaimedPets(playerId);

		// 為每個未領取且未觸發過事件的寵物補發孵化完成事件
		for (org.l2jmobius.gameserver.data.holders.UnclaimedPetData petData : unclaimedPets)
		{
			// 只有未觸發過事件的才補發
			if (!petData.eventFired)
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerPetHatch(player, petData.petItemId, petData.tier, false));
				// 標記為已觸發事件
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
			case 3: return "史詩";
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
