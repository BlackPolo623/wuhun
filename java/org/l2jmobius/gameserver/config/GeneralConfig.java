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
package org.l2jmobius.gameserver.config;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.model.actor.enums.player.ChatBroadcastType;
import org.l2jmobius.gameserver.model.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class loads all the general related configurations.
 * @author Mobius
 */
public class GeneralConfig
{
	private static final Logger LOGGER = Logger.getLogger(GeneralConfig.class.getName());
	
	// File
	private static final String GENERAL_CONFIG_FILE = "./config/General.ini";
	
	// Constants
	public static int DEFAULT_ACCESS_LEVEL;
	public static boolean SERVER_GMONLY;
	public static boolean GM_HERO_AURA;
	public static boolean GM_STARTUP_BUILDER_HIDE;
	public static boolean GM_STARTUP_INVULNERABLE;
	public static boolean GM_STARTUP_INVISIBLE;
	public static boolean GM_STARTUP_SILENCE;
	public static boolean GM_STARTUP_AUTO_LIST;
	public static boolean GM_STARTUP_DIET_MODE;
	public static boolean GM_ITEM_RESTRICTION;
	public static boolean GM_SKILL_RESTRICTION;
	public static boolean GM_TRADE_RESTRICTED_ITEMS;
	public static boolean GM_RESTART_FIGHTING;
	public static boolean GM_ANNOUNCER_NAME;
	public static boolean GM_GIVE_SPECIAL_SKILLS;
	public static boolean GM_GIVE_SPECIAL_AURA_SKILLS;
	public static boolean GM_DEBUG_HTML_PATHS;
	public static boolean USE_SUPER_HASTE_AS_GM_SPEED;
	public static boolean LOG_CHAT;
	public static boolean LOG_ITEMS;
	public static boolean LOG_ITEMS_SMALL_LOG;
	public static boolean LOG_ITEMS_IDS_ONLY;
	public static Set<Integer> LOG_ITEMS_IDS_LIST;
	public static boolean LOG_ITEM_ENCHANTS;
	public static boolean LOG_SKILL_ENCHANTS;
	public static boolean GMAUDIT;
	public static boolean SKILL_CHECK_ENABLE;
	public static boolean SKILL_CHECK_REMOVE;
	public static boolean SKILL_CHECK_GM;
	public static boolean ALLOW_DISCARDITEM;
	public static int AUTODESTROY_ITEM_AFTER;
	public static int HERB_AUTO_DESTROY_TIME;
	public static Set<Integer> LIST_PROTECTED_ITEMS;
	public static int CHAR_DATA_STORE_INTERVAL;
	public static int CLAN_VARIABLES_STORE_INTERVAL;
	public static boolean LAZY_ITEMS_UPDATE;
	public static boolean UPDATE_ITEMS_ON_CHAR_STORE;
	public static boolean DESTROY_DROPPED_PLAYER_ITEM;
	public static boolean DESTROY_EQUIPABLE_PLAYER_ITEM;
	public static boolean DESTROY_ALL_ITEMS;
	public static boolean SAVE_DROPPED_ITEM;
	public static boolean EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD;
	public static int SAVE_DROPPED_ITEM_INTERVAL;
	public static boolean CLEAR_DROPPED_ITEM_TABLE;
	public static boolean ORDER_QUEST_LIST_BY_QUESTID;
	public static boolean AUTODELETE_INVALID_QUEST_DATA;
	public static boolean ENABLE_STORY_QUEST_BUFF_REWARD;
	public static boolean MULTIPLE_ITEM_DROP;
	public static boolean HTM_CACHE;
	public static boolean CHECK_HTML_ENCODING;
	public static int MIN_NPC_ANIMATION;
	public static int MAX_NPC_ANIMATION;
	public static int MIN_MONSTER_ANIMATION;
	public static int MAX_MONSTER_ANIMATION;
	public static boolean GRIDS_ALWAYS_ON;
	public static int GRID_NEIGHBOR_TURNON_TIME;
	public static int GRID_NEIGHBOR_TURNOFF_TIME;
	public static boolean CORRECT_PRICES;
	public static long MULTISELL_AMOUNT_LIMIT;
	public static boolean ENABLE_FALLING_DAMAGE;
	public static int PEACE_ZONE_MODE;
	public static ChatBroadcastType DEFAULT_GLOBAL_CHAT;
	public static ChatBroadcastType DEFAULT_TRADE_CHAT;
	public static boolean ENABLE_WORLD_CHAT;
	public static int MINIMUM_CHAT_LEVEL;
	public static boolean ALLOW_WAREHOUSE;
	public static boolean ALLOW_REFUND;
	public static boolean ALLOW_MAIL;
	public static boolean ALLOW_ATTACHMENTS;
	public static boolean ALLOW_WEAR;
	public static int WEAR_DELAY;
	public static int WEAR_PRICE;
	public static int STORE_REVIEW_LIMIT;
	public static int STORE_REVIEW_CACHE_TIME;
	public static int INSTANCE_FINISH_TIME;
	public static boolean RESTORE_PLAYER_INSTANCE;
	public static int EJECT_DEAD_PLAYER_TIME;
	public static boolean ALLOW_RACE;
	public static boolean ALLOW_WATER;
	public static boolean ALLOW_FISHING;
	public static boolean ALLOW_MANOR;
	public static boolean ALLOW_BOAT;
	public static int BOAT_BROADCAST_RADIUS;
	public static boolean ALLOW_CURSED_WEAPONS;
	public static boolean ALLOW_PARTY_IN_SAME_EVENT;
	public static boolean SERVER_NEWS;
	public static boolean ENABLE_COMMUNITY_BOARD;
	public static String BBS_DEFAULT;
	public static boolean USE_SAY_FILTER;
	public static String CHAT_FILTER_CHARS;
	public static Set<ChatType> BAN_CHAT_CHANNELS;
	public static int WORLD_CHAT_MIN_LEVEL;
	public static int WORLD_CHAT_POINTS_PER_DAY;
	public static Duration WORLD_CHAT_INTERVAL;
	public static int ALT_MANOR_REFRESH_TIME;
	public static int ALT_MANOR_REFRESH_MIN;
	public static int ALT_MANOR_APPROVE_TIME;
	public static int ALT_MANOR_APPROVE_MIN;
	public static int ALT_MANOR_MAINTENANCE_MIN;
	public static boolean ALT_MANOR_SAVE_ALL_ACTIONS;
	public static int ALT_MANOR_SAVE_PERIOD_RATE;
	public static boolean ALT_ITEM_AUCTION_ENABLED;
	public static int ALT_ITEM_AUCTION_EXPIRED_AFTER;
	public static long ALT_ITEM_AUCTION_TIME_EXTENDS_ON_BID;
	public static IllegalActionPunishmentType DEFAULT_PUNISH;
	public static long DEFAULT_PUNISH_PARAM;
	public static boolean ONLY_GM_ITEMS_FREE;
	public static boolean JAIL_IS_PVP;
	public static boolean JAIL_DISABLE_CHAT;
	public static boolean JAIL_DISABLE_TRANSACTION;
	public static boolean CUSTOM_NPC_DATA;
	public static boolean CUSTOM_SKILLS_LOAD;
	public static boolean CUSTOM_ITEMS_LOAD;
	public static boolean CUSTOM_MULTISELL_LOAD;
	public static boolean CUSTOM_BUYLIST_LOAD;
	public static int BOOKMARK_CONSUME_ITEM_ID;
	public static int ALT_BIRTHDAY_GIFT;
	public static String ALT_BIRTHDAY_MAIL_SUBJECT;
	public static String ALT_BIRTHDAY_MAIL_TEXT;
	public static boolean BOTREPORT_ENABLE;
	public static String[] BOTREPORT_RESETPOINT_HOUR;
	public static long BOTREPORT_REPORT_DELAY;
	public static boolean BOTREPORT_ALLOW_REPORTS_FROM_SAME_CLAN_MEMBERS;
	public static boolean ENABLE_AUTO_PLAY;
	public static boolean ENABLE_AUTO_POTION;
	public static boolean ENABLE_AUTO_PET_POTION;
	public static boolean ENABLE_AUTO_SKILL;
	public static boolean ENABLE_AUTO_ITEM;
	public static boolean AUTO_PLAY_ATTACK_ACTION;
	public static boolean RESUME_AUTO_PLAY;
	public static boolean ENABLE_AUTO_ASSIST;
	public static AbnormalVisualEffect BLUE_TEAM_ABNORMAL_EFFECT;
	public static AbnormalVisualEffect RED_TEAM_ABNORMAL_EFFECT;
	public static String SUBJUGATION_TOPIC_BODY;
	public static String SUBJUGATION_TOPIC_HEADER;
	public static int SHARING_LOCATION_COST;
	public static int TELEPORT_SHARE_LOCATION_COST;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(GENERAL_CONFIG_FILE);
		DEFAULT_ACCESS_LEVEL = config.getInt("DefaultAccessLevel", 0);
		SERVER_GMONLY = config.getBoolean("ServerGMOnly", false);
		GM_HERO_AURA = config.getBoolean("GMHeroAura", false);
		GM_STARTUP_BUILDER_HIDE = config.getBoolean("GMStartupBuilderHide", false);
		GM_STARTUP_INVULNERABLE = config.getBoolean("GMStartupInvulnerable", false);
		GM_STARTUP_INVISIBLE = config.getBoolean("GMStartupInvisible", false);
		GM_STARTUP_SILENCE = config.getBoolean("GMStartupSilence", false);
		GM_STARTUP_AUTO_LIST = config.getBoolean("GMStartupAutoList", false);
		GM_STARTUP_DIET_MODE = config.getBoolean("GMStartupDietMode", false);
		GM_ITEM_RESTRICTION = config.getBoolean("GMItemRestriction", true);
		GM_SKILL_RESTRICTION = config.getBoolean("GMSkillRestriction", true);
		GM_TRADE_RESTRICTED_ITEMS = config.getBoolean("GMTradeRestrictedItems", false);
		GM_RESTART_FIGHTING = config.getBoolean("GMRestartFighting", true);
		GM_ANNOUNCER_NAME = config.getBoolean("GMShowAnnouncerName", false);
		GM_GIVE_SPECIAL_SKILLS = config.getBoolean("GMGiveSpecialSkills", false);
		GM_GIVE_SPECIAL_AURA_SKILLS = config.getBoolean("GMGiveSpecialAuraSkills", false);
		GM_DEBUG_HTML_PATHS = config.getBoolean("GMDebugHtmlPaths", true);
		USE_SUPER_HASTE_AS_GM_SPEED = config.getBoolean("UseSuperHasteAsGMSpeed", false);
		LOG_CHAT = config.getBoolean("LogChat", false);
		LOG_ITEMS = config.getBoolean("LogItems", false);
		LOG_ITEMS_SMALL_LOG = config.getBoolean("LogItemsSmallLog", false);
		LOG_ITEMS_IDS_ONLY = config.getBoolean("LogItemsIdsOnly", false);
		final String[] splitItemIds = config.getString("LogItemsIdsList", "0").split(",");
		LOG_ITEMS_IDS_LIST = new HashSet<>(splitItemIds.length);
		for (String id : splitItemIds)
		{
			LOG_ITEMS_IDS_LIST.add(Integer.parseInt(id));
		}
		LOG_ITEM_ENCHANTS = config.getBoolean("LogItemEnchants", false);
		LOG_SKILL_ENCHANTS = config.getBoolean("LogSkillEnchants", false);
		GMAUDIT = config.getBoolean("GMAudit", false);
		SKILL_CHECK_ENABLE = config.getBoolean("SkillCheckEnable", false);
		SKILL_CHECK_REMOVE = config.getBoolean("SkillCheckRemove", false);
		SKILL_CHECK_GM = config.getBoolean("SkillCheckGM", true);
		ALLOW_DISCARDITEM = config.getBoolean("AllowDiscardItem", true);
		AUTODESTROY_ITEM_AFTER = config.getInt("AutoDestroyDroppedItemAfter", 600);
		HERB_AUTO_DESTROY_TIME = config.getInt("AutoDestroyHerbTime", 60) * 1000;
		final String[] split = config.getString("ListOfProtectedItems", "0").split(",");
		LIST_PROTECTED_ITEMS = new HashSet<>(split.length);
		for (String id : split)
		{
			LIST_PROTECTED_ITEMS.add(Integer.parseInt(id));
		}
		CHAR_DATA_STORE_INTERVAL = config.getInt("CharacterDataStoreInterval", 15) * 60 * 1000;
		CLAN_VARIABLES_STORE_INTERVAL = config.getInt("ClanVariablesStoreInterval", 15) * 60 * 1000;
		LAZY_ITEMS_UPDATE = config.getBoolean("LazyItemsUpdate", false);
		UPDATE_ITEMS_ON_CHAR_STORE = config.getBoolean("UpdateItemsOnCharStore", false);
		DESTROY_DROPPED_PLAYER_ITEM = config.getBoolean("DestroyPlayerDroppedItem", false);
		DESTROY_EQUIPABLE_PLAYER_ITEM = config.getBoolean("DestroyEquipableItem", false);
		DESTROY_ALL_ITEMS = config.getBoolean("DestroyAllItems", false);
		SAVE_DROPPED_ITEM = config.getBoolean("SaveDroppedItem", false);
		EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = config.getBoolean("EmptyDroppedItemTableAfterLoad", false);
		SAVE_DROPPED_ITEM_INTERVAL = config.getInt("SaveDroppedItemInterval", 60) * 60000;
		CLEAR_DROPPED_ITEM_TABLE = config.getBoolean("ClearDroppedItemTable", false);
		ORDER_QUEST_LIST_BY_QUESTID = config.getBoolean("OrderQuestListByQuestId", true);
		AUTODELETE_INVALID_QUEST_DATA = config.getBoolean("AutoDeleteInvalidQuestData", false);
		ENABLE_STORY_QUEST_BUFF_REWARD = config.getBoolean("StoryQuestRewardBuff", true);
		MULTIPLE_ITEM_DROP = config.getBoolean("MultipleItemDrop", true);
		HTM_CACHE = config.getBoolean("HtmCache", true);
		CHECK_HTML_ENCODING = config.getBoolean("CheckHtmlEncoding", true);
		MIN_NPC_ANIMATION = config.getInt("MinNpcAnimation", 5);
		MAX_NPC_ANIMATION = config.getInt("MaxNpcAnimation", 60);
		MIN_MONSTER_ANIMATION = config.getInt("MinMonsterAnimation", 5);
		MAX_MONSTER_ANIMATION = config.getInt("MaxMonsterAnimation", 60);
		GRIDS_ALWAYS_ON = config.getBoolean("GridsAlwaysOn", false);
		GRID_NEIGHBOR_TURNON_TIME = config.getInt("GridNeighborTurnOnTime", 1);
		GRID_NEIGHBOR_TURNOFF_TIME = config.getInt("GridNeighborTurnOffTime", 90);
		CORRECT_PRICES = config.getBoolean("CorrectPrices", true);
		MULTISELL_AMOUNT_LIMIT = config.getLong("MultisellAmountLimit", 10000);
		ENABLE_FALLING_DAMAGE = config.getBoolean("EnableFallingDamage", true);
		PEACE_ZONE_MODE = config.getInt("PeaceZoneMode", 0);
		DEFAULT_GLOBAL_CHAT = Enum.valueOf(ChatBroadcastType.class, config.getString("GlobalChat", "ON"));
		DEFAULT_TRADE_CHAT = Enum.valueOf(ChatBroadcastType.class, config.getString("TradeChat", "ON"));
		ENABLE_WORLD_CHAT = config.getBoolean("WorldChatEnabled", true);
		MINIMUM_CHAT_LEVEL = config.getInt("MinimumChatLevel", 20);
		ALLOW_WAREHOUSE = config.getBoolean("AllowWarehouse", true);
		ALLOW_REFUND = config.getBoolean("AllowRefund", true);
		ALLOW_MAIL = config.getBoolean("AllowMail", true);
		ALLOW_ATTACHMENTS = config.getBoolean("AllowAttachments", true);
		ALLOW_WEAR = config.getBoolean("AllowWear", true);
		WEAR_DELAY = config.getInt("WearDelay", 5);
		WEAR_PRICE = config.getInt("WearPrice", 10);
		STORE_REVIEW_LIMIT = config.getInt("PrivateStoreReviewLimit", 30);
		STORE_REVIEW_CACHE_TIME = config.getInt("PrivateStoreReviewCacheTime", 5000);
		INSTANCE_FINISH_TIME = config.getInt("DefaultFinishTime", 5);
		RESTORE_PLAYER_INSTANCE = config.getBoolean("RestorePlayerInstance", false);
		EJECT_DEAD_PLAYER_TIME = config.getInt("EjectDeadPlayerTime", 1);
		ALLOW_RACE = config.getBoolean("AllowRace", true);
		ALLOW_WATER = config.getBoolean("AllowWater", true);
		ALLOW_FISHING = config.getBoolean("AllowFishing", true);
		ALLOW_MANOR = config.getBoolean("AllowManor", true);
		ALLOW_BOAT = config.getBoolean("AllowBoat", true);
		BOAT_BROADCAST_RADIUS = config.getInt("BoatBroadcastRadius", 20000);
		ALLOW_CURSED_WEAPONS = config.getBoolean("AllowCursedWeapons", true);
		ALLOW_PARTY_IN_SAME_EVENT = config.getBoolean("AllowPartyInSameEvent", true);
		SERVER_NEWS = config.getBoolean("ShowServerNews", false);
		ENABLE_COMMUNITY_BOARD = config.getBoolean("EnableCommunityBoard", true);
		BBS_DEFAULT = config.getString("BBSDefault", "_bbshome");
		USE_SAY_FILTER = config.getBoolean("UseChatFilter", false);
		CHAT_FILTER_CHARS = config.getString("ChatFilterChars", "^_^");
		final String[] propertySplit4 = config.getString("BanChatChannels", "GENERAL;SHOUT;WORLD;TRADE;HERO_VOICE").trim().split(";");
		BAN_CHAT_CHANNELS = new HashSet<>();
		try
		{
			for (String chatId : propertySplit4)
			{
				BAN_CHAT_CHANNELS.add(Enum.valueOf(ChatType.class, chatId));
			}
		}
		catch (NumberFormatException nfe)
		{
			LOGGER.log(Level.WARNING, "There was an error while parsing ban chat channels: ", nfe);
		}
		WORLD_CHAT_MIN_LEVEL = config.getInt("WorldChatMinLevel", 95);
		WORLD_CHAT_POINTS_PER_DAY = config.getInt("WorldChatPointsPerDay", 10);
		WORLD_CHAT_INTERVAL = config.getDuration("WorldChatInterval", "20secs");
		ALT_MANOR_REFRESH_TIME = config.getInt("AltManorRefreshTime", 20);
		ALT_MANOR_REFRESH_MIN = config.getInt("AltManorRefreshMin", 0);
		ALT_MANOR_APPROVE_TIME = config.getInt("AltManorApproveTime", 4);
		ALT_MANOR_APPROVE_MIN = config.getInt("AltManorApproveMin", 30);
		ALT_MANOR_MAINTENANCE_MIN = config.getInt("AltManorMaintenanceMin", 6);
		ALT_MANOR_SAVE_ALL_ACTIONS = config.getBoolean("AltManorSaveAllActions", false);
		ALT_MANOR_SAVE_PERIOD_RATE = config.getInt("AltManorSavePeriodRate", 2);
		ALT_ITEM_AUCTION_ENABLED = config.getBoolean("AltItemAuctionEnabled", true);
		ALT_ITEM_AUCTION_EXPIRED_AFTER = config.getInt("AltItemAuctionExpiredAfter", 14);
		ALT_ITEM_AUCTION_TIME_EXTENDS_ON_BID = config.getInt("AltItemAuctionTimeExtendsOnBid", 0) * 1000;
		DEFAULT_PUNISH = IllegalActionPunishmentType.findByName(config.getString("DefaultPunish", "KICK"));
		DEFAULT_PUNISH_PARAM = config.getLong("DefaultPunishParam", 0);
		if (DEFAULT_PUNISH_PARAM == 0)
		{
			DEFAULT_PUNISH_PARAM = 3155695200L; // One hundred years in seconds.
		}
		ONLY_GM_ITEMS_FREE = config.getBoolean("OnlyGMItemsFree", true);
		JAIL_IS_PVP = config.getBoolean("JailIsPvp", false);
		JAIL_DISABLE_CHAT = config.getBoolean("JailDisableChat", true);
		JAIL_DISABLE_TRANSACTION = config.getBoolean("JailDisableTransaction", false);
		CUSTOM_NPC_DATA = config.getBoolean("CustomNpcData", false);
		CUSTOM_SKILLS_LOAD = config.getBoolean("CustomSkillsLoad", false);
		CUSTOM_ITEMS_LOAD = config.getBoolean("CustomItemsLoad", false);
		CUSTOM_MULTISELL_LOAD = config.getBoolean("CustomMultisellLoad", false);
		CUSTOM_BUYLIST_LOAD = config.getBoolean("CustomBuyListLoad", false);
		BOOKMARK_CONSUME_ITEM_ID = config.getInt("BookmarkConsumeItemId", -1);
		ALT_BIRTHDAY_GIFT = config.getInt("AltBirthdayGift", 72078);
		ALT_BIRTHDAY_MAIL_SUBJECT = config.getString("AltBirthdayMailSubject", "Happy Birthday!");
		ALT_BIRTHDAY_MAIL_TEXT = config.getString("AltBirthdayMailText", "Hello Adventurer!! Seeing as you're one year older now, I thought I would send you some birthday cheer :) Please find your birthday pack attached. May these gifts bring you joy and happiness on this very special day." + System.lineSeparator() + System.lineSeparator() + "Sincerely, Alegria");
		BOTREPORT_ENABLE = config.getBoolean("EnableBotReportButton", false);
		BOTREPORT_RESETPOINT_HOUR = config.getString("BotReportPointsResetHour", "00:00").split(":");
		BOTREPORT_REPORT_DELAY = config.getInt("BotReportDelay", 30) * 60000;
		BOTREPORT_ALLOW_REPORTS_FROM_SAME_CLAN_MEMBERS = config.getBoolean("AllowReportsFromSameClanMembers", false);
		ENABLE_AUTO_PLAY = config.getBoolean("EnableAutoPlay", true);
		ENABLE_AUTO_POTION = config.getBoolean("EnableAutoPotion", true);
		ENABLE_AUTO_PET_POTION = config.getBoolean("EnableAutoPetPotion", true);
		ENABLE_AUTO_SKILL = config.getBoolean("EnableAutoSkill", true);
		ENABLE_AUTO_ITEM = config.getBoolean("EnableAutoItem", true);
		AUTO_PLAY_ATTACK_ACTION = config.getBoolean("AutoPlayAttackAction", true);
		RESUME_AUTO_PLAY = config.getBoolean("ResumeAutoPlay", false);
		ENABLE_AUTO_ASSIST = config.getBoolean("AssistLeader", false);
		BLUE_TEAM_ABNORMAL_EFFECT = null;
		final String blueTeamAve = config.getString("BlueTeamAbnormalEffect", "").trim();
		if (!blueTeamAve.isEmpty())
		{
			BLUE_TEAM_ABNORMAL_EFFECT = Enum.valueOf(AbnormalVisualEffect.class, blueTeamAve);
		}
		RED_TEAM_ABNORMAL_EFFECT = null;
		final String redTeamAve = config.getString("RedTeamAbnormalEffect", "").trim();
		if (!redTeamAve.isEmpty())
		{
			RED_TEAM_ABNORMAL_EFFECT = Enum.valueOf(AbnormalVisualEffect.class, redTeamAve);
		}
		SUBJUGATION_TOPIC_BODY = config.getString("SubjugationTopicBody", "Reward for being in the top of the best players in clearing the lands of Aden");
		SUBJUGATION_TOPIC_HEADER = config.getString("SubjugationTopicHeader", "Purge reward");
		SHARING_LOCATION_COST = config.getInt("ShareLocationLcoinCost", 50);
		TELEPORT_SHARE_LOCATION_COST = config.getInt("TeleportShareLocationLcoinCost", 400);
	}
}
