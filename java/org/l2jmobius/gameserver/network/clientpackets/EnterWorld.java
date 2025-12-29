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
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.AchievementBoxConfig;
import org.l2jmobius.gameserver.config.AdenLaboratoryConfig;
import org.l2jmobius.gameserver.config.AttendanceRewardsConfig;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.HuntPassConfig;
import org.l2jmobius.gameserver.config.MagicLampConfig;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RandomCraftConfig;
import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.config.custom.ScreenWelcomeMessageConfig;
import org.l2jmobius.gameserver.config.custom.WeddingConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.sql.AnnouncementsTable;
import org.l2jmobius.gameserver.data.sql.OfflineTraderTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.data.xml.BeautyShopData;
import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.data.xml.EnchantItemGroupsData;
import org.l2jmobius.gameserver.data.xml.MableGameData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.managers.AdenLaboratoryManager;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.CoupleManager;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.managers.PcCafePointsManager;
import org.l2jmobius.gameserver.managers.PetitionManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.managers.ServerRestartManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.managers.WorldExchangeManager;
import org.l2jmobius.gameserver.model.Couple;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.model.actor.enums.player.SubclassInfoType;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.actor.holders.player.AttendanceInfoHolder;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.EtcItemType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.model.punishment.PunishmentType;
import org.l2jmobius.gameserver.model.residences.ClanHall;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.model.siege.Fort;
import org.l2jmobius.gameserver.model.siege.FortSiege;
import org.l2jmobius.gameserver.model.siege.Siege;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.ConnectionState;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.holders.ClientHardwareInfoHolder;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.Die;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ExAdenaInvenCount;
import org.l2jmobius.gameserver.network.serverpackets.ExAutoSoulShot;
import org.l2jmobius.gameserver.network.serverpackets.ExBasicActionList;
import org.l2jmobius.gameserver.network.serverpackets.ExBeautyItemList;
import org.l2jmobius.gameserver.network.serverpackets.ExBrPremiumState;
import org.l2jmobius.gameserver.network.serverpackets.ExEnterWorld;
import org.l2jmobius.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExNoticePostArrived;
import org.l2jmobius.gameserver.network.serverpackets.ExNotifyPremiumItem;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExPledgeCoinInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExPledgeCount;
import org.l2jmobius.gameserver.network.serverpackets.ExPledgeWaitingListAlarm;
import org.l2jmobius.gameserver.network.serverpackets.ExQuestItemList;
import org.l2jmobius.gameserver.network.serverpackets.ExRotation;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExSubjobInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExUnReadMailCount;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoEquipSlot;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoInvenWeight;
import org.l2jmobius.gameserver.network.serverpackets.ExVitalityEffectInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExVoteSystemInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExWorldChatCnt;
import org.l2jmobius.gameserver.network.serverpackets.HennaInfo;
import org.l2jmobius.gameserver.network.serverpackets.ItemDeletionInfo;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListAll;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PledgeSkillList;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutInit;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.network.serverpackets.SkillList;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.achievementbox.ExSteadyBoxUiInit;
import org.l2jmobius.gameserver.network.serverpackets.attendance.ExVipAttendanceItemList;
import org.l2jmobius.gameserver.network.serverpackets.attendance.ExVipAttendanceList;
import org.l2jmobius.gameserver.network.serverpackets.attendance.ExVipAttendanceNotify;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionActiveEvent;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionInfo;
import org.l2jmobius.gameserver.network.serverpackets.dailymission.ExConnectedTimeAndGettableReward;
import org.l2jmobius.gameserver.network.serverpackets.dailymission.ExOneDayReceiveRewardList;
import org.l2jmobius.gameserver.network.serverpackets.enchant.challengepoint.ExEnchantChallengePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.friend.L2FriendList;
import org.l2jmobius.gameserver.network.serverpackets.huntpass.HuntPassSimpleInfo;
import org.l2jmobius.gameserver.network.serverpackets.limitshop.ExBloodyCoinCount;
import org.l2jmobius.gameserver.network.serverpackets.mablegame.ExMableGameUILauncher;
import org.l2jmobius.gameserver.network.serverpackets.magiclamp.ExMagicLampInfo;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadInfo;
import org.l2jmobius.gameserver.network.serverpackets.pledgedonation.ExPledgeContributionList;
import org.l2jmobius.gameserver.network.serverpackets.randomcraft.ExCraftInfo;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsCollectionInfo;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsList;
import org.l2jmobius.gameserver.network.serverpackets.settings.ExItemAnnounceSetting;
import org.l2jmobius.gameserver.network.serverpackets.subjugation.ExSubjugationSidebar;

/**
 * Enter World Packet Handler
 * <p>
 * <p>
 * 0000: 03
 * <p>
 * packet format rev87 bddddbdcccccccccccccccccccc
 * <p>
 */
public class EnterWorld extends ClientPacket
{
	private static final Map<String, ClientHardwareInfoHolder> TRACE_HWINFO = new ConcurrentHashMap<>();
	
	private final int[][] _tracert = new int[5][4];
	
	@Override
	protected void readImpl()
	{
		for (int i = 0; i < 5; i++)
		{
			for (int o = 0; o < 4; o++)
			{
				_tracert[i][o] = readUnsignedByte();
			}
		}
		
		readInt(); // Unknown Value
		readInt(); // Unknown Value
		readInt(); // Unknown Value
		readInt(); // Unknown Value
		readBytes(64); // Unknown Byte Array
		readInt(); // Unknown Value
	}
	
	@Override
	protected void runImpl()
	{
		final GameClient client = getClient();
		final Player player = client.getPlayer();
		if (player == null)
		{
			PacketLogger.warning("EnterWorld failed! player returned 'null'.");
			Disconnection.of(client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
			return;
		}
		
		client.setConnectionState(ConnectionState.IN_GAME);
		
		final String[] adress = new String[5];
		for (int i = 0; i < 5; i++)
		{
			adress[i] = _tracert[i][0] + "." + _tracert[i][1] + "." + _tracert[i][2] + "." + _tracert[i][3];
		}
		
		LoginServerThread.getInstance().sendClientTracert(player.getAccountName(), adress);
		client.setClientTracert(_tracert);
		
		player.sendPacket(new UserInfo(player));
		
		// Restore to instanced area if enabled.
		final PlayerVariables vars = player.getVariables();
		if (GeneralConfig.RESTORE_PLAYER_INSTANCE)
		{
			final Instance instance = InstanceManager.getInstance().getPlayerInstance(player, false);
			if ((instance != null) && (instance.getId() == vars.getInt(PlayerVariables.INSTANCE_RESTORE, 0)))
			{
				player.setInstance(instance);
			}
			
			vars.remove(PlayerVariables.INSTANCE_RESTORE);
		}
		
		if (!player.isGM())
		{
			player.updatePvpTitleAndColor(false);
		}
		
		// Apply special GM properties to the GM when entering
		else
		{
			gmStartupProcess:
			{
				if (GeneralConfig.GM_STARTUP_BUILDER_HIDE && AdminData.getInstance().hasAccess("admin_hide", player.getAccessLevel()))
				{
					player.setHiding(true);
					player.sendSysMessage("hide is default for builder.");
					player.sendSysMessage("FriendAddOff is default for builder.");
					player.sendSysMessage("whisperoff is default for builder.");
					
					// It isn't recommend to use the below custom L2J GMStartup functions together with retail-like GMStartupBuilderHide, so breaking the process at that stage.
					break gmStartupProcess;
				}
				
				if (GeneralConfig.GM_STARTUP_INVULNERABLE && AdminData.getInstance().hasAccess("admin_invul", player.getAccessLevel()))
				{
					player.setInvul(true);
				}
				
				if (GeneralConfig.GM_STARTUP_INVISIBLE && AdminData.getInstance().hasAccess("admin_invisible", player.getAccessLevel()))
				{
					player.setInvisible(true);
					player.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.STEALTH);
				}
				
				if (GeneralConfig.GM_STARTUP_SILENCE && AdminData.getInstance().hasAccess("admin_silence", player.getAccessLevel()))
				{
					player.setSilenceMode(true);
				}
				
				if (GeneralConfig.GM_STARTUP_DIET_MODE && AdminData.getInstance().hasAccess("admin_diet", player.getAccessLevel()))
				{
					player.setDietMode(true);
					player.refreshOverloaded(true);
				}
			}
			
			if (GeneralConfig.GM_STARTUP_AUTO_LIST && AdminData.getInstance().hasAccess("admin_gmliston", player.getAccessLevel()))
			{
				AdminData.getInstance().addGm(player, false);
			}
			else
			{
				AdminData.getInstance().addGm(player, true);
			}
			
			if (GeneralConfig.GM_GIVE_SPECIAL_SKILLS)
			{
				SkillTreeData.getInstance().addSkills(player, false);
			}
			
			if (GeneralConfig.GM_GIVE_SPECIAL_AURA_SKILLS)
			{
				SkillTreeData.getInstance().addSkills(player, true);
			}
		}
		
		// Set dead status if applies
		if (player.getCurrentHp() < 0.5)
		{
			player.setDead(true);
		}
		
		boolean showClanNotice = false;
		
		// Clan related checks are here
		final Clan clan = player.getClan();
		if (clan != null)
		{
			notifyClanMembers(player);
			notifySponsorOrApprentice(player);
			
			for (Siege siege : SiegeManager.getInstance().getSieges())
			{
				if (!siege.isInProgress())
				{
					continue;
				}
				
				if (siege.checkIsAttacker(clan))
				{
					player.setSiegeState((byte) 1);
					player.setSiegeSide(siege.getCastle().getResidenceId());
				}
				else if (siege.checkIsDefender(clan))
				{
					player.setSiegeState((byte) 2);
					player.setSiegeSide(siege.getCastle().getResidenceId());
				}
			}
			
			for (FortSiege siege : FortSiegeManager.getInstance().getSieges())
			{
				if (!siege.isInProgress())
				{
					continue;
				}
				
				if (siege.checkIsAttacker(clan))
				{
					player.setSiegeState((byte) 1);
					player.setSiegeSide(siege.getFort().getResidenceId());
				}
				else if (siege.checkIsDefender(clan))
				{
					player.setSiegeState((byte) 2);
					player.setSiegeSide(siege.getFort().getResidenceId());
				}
			}
			
			// Residential skills support
			if (clan.getCastleId() > 0)
			{
				final Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
				if (castle != null)
				{
					castle.giveResidentialSkills(player);
				}
			}
			
			if (clan.getFortId() > 0)
			{
				final Fort fort = FortManager.getInstance().getFortByOwner(clan);
				if (fort != null)
				{
					fort.giveResidentialSkills(player);
				}
			}
			
			showClanNotice = clan.isNoticeEnabled();
		}
		
		// Mercenary Siege.
		if (player.isMercenary())
		{
			player.updateMercenary();
		}
		
		// Send time.
		player.sendPacket(new ExEnterWorld());
		
		// Send Macro List
		player.getMacros().sendAllMacros();
		
		// Send Teleport Bookmark List
		player.sendPacket(new ExGetBookMarkInfoPacket(player));
		
		// Send Item List
		player.sendPacket(new ItemList(1, player));
		player.sendPacket(new ItemList(2, player));
		
		// Send Quest Item List
		player.sendPacket(new ExQuestItemList(1, player));
		player.sendPacket(new ExQuestItemList(2, player));
		
		// Send Shortcuts
		player.sendPacket(new ShortcutInit(player));
		
		// Send Action list
		player.sendPacket(ExBasicActionList.STATIC_PACKET);
		
		// Send blank skill list
		player.sendPacket(new SkillList());
		
		// Send GG check
		// player.queryGameGuard();
		
		// Send Dye Information
		player.sendPacket(new HennaInfo(player));
		
		// Aden Laboratory.
		if (AdenLaboratoryConfig.ADENLAB_ENABLED)
		{
			AdenLaboratoryManager.restorePlayerData(player);
			AdenLaboratoryManager.checkPlayerSkills(player);
			AdenLaboratoryManager.calculateAdenLabCombatPower(player);
		}
		
		// Send Skill list
		player.sendSkillList();
		
		// Send EtcStatusUpdate
		player.sendPacket(new EtcStatusUpdate(player));
		
		// Calculate stat increase skills.
		player.calculateStatIncreaseSkills();
		
		// Clan packets
		if (clan != null)
		{
			clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(player));
			PledgeShowMemberListAll.sendAllTo(player);
			clan.broadcastToOnlineMembers(new ExPledgeCount(clan));
			player.sendPacket(new PledgeSkillList(clan));
			final ClanHall ch = ClanHallData.getInstance().getClanHallByClan(clan);
			if ((ch != null) && (ch.getCostFailDay() > 0) && (ch.getResidenceId() < 186))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.THE_PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_DEPOSIT_THE_NECESSARY_AMOUNT_OF_ADENA_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
				sm.addInt(ch.getLease());
				player.sendPacket(sm);
			}
		}
		else
		{
			player.sendPacket(ExPledgeWaitingListAlarm.STATIC_PACKET);
		}
		
		// Send SubClass Info
		player.sendPacket(new ExSubjobInfo(player, SubclassInfoType.NO_CHANGES));
		
		// Send Inventory Info
		player.sendPacket(new ExUserInfoInvenWeight(player));
		
		// Send Adena / Inventory Count Info
		player.sendPacket(new ExAdenaInvenCount(player));
		
		// Send LCoin count.
		player.sendPacket(new ExBloodyCoinCount(player));
		
		// Send honor coin count.
		player.sendPacket(new ExPledgeCoinInfo(player));
		
		// Send VIP/Premium Info
		player.sendPacket(new ExBrPremiumState(player));
		
		// Send Challenge Point info.
		player.sendPacket(new ExEnchantChallengePointInfo(player));
		
		// Send Unread Mail Count
		if (MailManager.getInstance().hasUnreadPost(player))
		{
			player.sendPacket(new ExUnReadMailCount(player));
		}
		
		// Faction System
		if (FactionSystemConfig.FACTION_SYSTEM_ENABLED)
		{
			if (player.isGood())
			{
				final PlayerAppearance appearance = player.getAppearance();
				appearance.setNameColor(FactionSystemConfig.FACTION_GOOD_NAME_COLOR);
				appearance.setTitleColor(FactionSystemConfig.FACTION_GOOD_NAME_COLOR);
				player.sendMessage("Welcome " + player.getName() + ", you are fighting for the " + FactionSystemConfig.FACTION_GOOD_TEAM_NAME + " faction.");
				player.sendPacket(new ExShowScreenMessage("Welcome " + player.getName() + ", you are fighting for the " + FactionSystemConfig.FACTION_GOOD_TEAM_NAME + " faction.", 10000));
			}
			else if (player.isEvil())
			{
				final PlayerAppearance appearance = player.getAppearance();
				appearance.setNameColor(FactionSystemConfig.FACTION_EVIL_NAME_COLOR);
				appearance.setTitleColor(FactionSystemConfig.FACTION_EVIL_NAME_COLOR);
				player.sendMessage("Welcome " + player.getName() + ", you are fighting for the " + FactionSystemConfig.FACTION_EVIL_TEAM_NAME + " faction.");
				player.sendPacket(new ExShowScreenMessage("Welcome " + player.getName() + ", you are fighting for the " + FactionSystemConfig.FACTION_EVIL_TEAM_NAME + " faction.", 10000));
			}
		}
		
		Quest.playerEnter(player);
		
		// Send quest list.
		if (!PlayerConfig.DISABLE_TUTORIAL)
		{
			player.sendQuestList();
		}
		
		if (PlayerConfig.PLAYER_SPAWN_PROTECTION > 0)
		{
			player.setSpawnProtection(true);
		}
		
		player.spawnMe(player.getX(), player.getY(), player.getZ());
		player.sendPacket(new ExRotation(player.getObjectId(), player.getHeading()));
		
		if (player.isCursedWeaponEquipped())
		{
			CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).cursedOnLogin();
		}
		
		if (PremiumSystemConfig.PC_CAFE_ENABLED)
		{
			if (player.getPcCafePoints() > 0)
			{
				player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), 0, 1));
			}
			else
			{
				player.sendPacket(new ExPCCafePointInfo());
			}
		}
		
		// Expand Skill
		player.sendStorageMaxCount();
		
		// Send Equipped Items
		player.sendPacket(new ExUserInfoEquipSlot(player));
		
		// Friend list
		player.sendPacket(new L2FriendList(player));
		SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_FRIEND_S1_JUST_LOGGED_IN);
		sm.addString(player.getName());
		for (int id : player.getFriendList())
		{
			final WorldObject obj = World.getInstance().findObject(id);
			if (obj != null)
			{
				obj.sendPacket(sm);
			}
		}
		
		player.sendPacket(SystemMessageId.WELCOME_TO_THE_WORLD_OF_LINEAGE_II);
		
		AnnouncementsTable.getInstance().showAnnouncements(player);
		
		if ((ServerConfig.SERVER_RESTART_SCHEDULE_ENABLED) && (ServerConfig.SERVER_RESTART_SCHEDULE_MESSAGE))
		{
			player.sendPacket(new CreatureSay(null, ChatType.BATTLEFIELD, "[SERVER]", "Next restart is scheduled at " + ServerRestartManager.getInstance().getNextRestartTime() + "."));
		}
		
		if (showClanNotice)
		{
			final NpcHtmlMessage notice = new NpcHtmlMessage();
			notice.setFile(player, "data/html/clanNotice.htm");
			notice.replace("%clan_name%", player.getClan().getName());
			notice.replace("%notice_text%", player.getClan().getNotice().replaceAll("(\r\n|\n)", "<br>"));
			notice.disableValidation();
			player.sendPacket(notice);
		}
		else if (GeneralConfig.SERVER_NEWS)
		{
			final String serverNews = HtmCache.getInstance().getHtm(player, "data/html/servnews.htm");
			if (serverNews != null)
			{
				player.sendPacket(new NpcHtmlMessage(serverNews));
			}
		}
		
		if (PlayerConfig.PETITIONING_ALLOWED)
		{
			PetitionManager.getInstance().checkPetitionMessages(player);
		}
		
		player.onPlayerEnter();
		
		player.sendPacket(new SkillCoolTime(player));
		player.sendPacket(new ExVoteSystemInfo(player));
		
		if (player.isAlikeDead()) // dead or fake dead
		{
			// no broadcast needed since the player will already spawn dead to others
			player.sendPacket(new Die(player));
		}
		
		for (Item item : player.getInventory().getItems())
		{
			if (item.isTimeLimitedItem())
			{
				item.scheduleLifeTimeTask();
			}
			
			if (item.isShadowItem() && item.isEquipped())
			{
				item.decreaseMana(false);
			}
		}
		
		for (Item whItem : player.getWarehouse().getItems())
		{
			if (whItem.isTimeLimitedItem())
			{
				whItem.scheduleLifeTimeTask();
			}
		}
		
		if (player.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_DISMISSED_FROM_A_CLAN_YOU_CANNOT_JOIN_ANOTHER_FOR_24_H);
		}
		
		// remove combat flag before teleporting
		if (player.getInventory().getItemByItemId(FortManager.ORC_FORTRESS_FLAG) != null)
		{
			final Fort fort = FortManager.getInstance().getFort(player);
			if (fort != null)
			{
				FortSiegeManager.getInstance().dropCombatFlag(player, fort.getResidenceId());
			}
			else
			{
				final BodyPart bodyPart = BodyPart.fromItem(player.getInventory().getItemByItemId(FortManager.ORC_FORTRESS_FLAG));
				player.getInventory().unEquipItemInBodySlot(bodyPart);
				player.destroyItem(ItemProcessType.DESTROY, player.getInventory().getItemByItemId(FortManager.ORC_FORTRESS_FLAG), null, true);
			}
		}
		
		// Attacker or spectator logging in to a siege zone.
		// Actually should be checked for inside castle only?
		if (!player.isGM() && player.isInsideZone(ZoneId.SIEGE) && (!player.isInSiege() || (player.getSiegeState() < 2)))
		{
			player.teleToLocation(TeleportWhereType.TOWN);
		}
		
		// Over-enchant protection.
		if (PlayerConfig.OVER_ENCHANT_PROTECTION && !player.isGM())
		{
			boolean punish = false;
			for (Item item : player.getInventory().getItems())
			{
				if (item.isEquipable() //
					&& ((item.isWeapon() && (item.getEnchantLevel() > EnchantItemGroupsData.getInstance().getMaxWeaponEnchant())) //
						|| ((item.getTemplate().getType2() == ItemTemplate.TYPE2_ACCESSORY) && (item.getEnchantLevel() > EnchantItemGroupsData.getInstance().getMaxAccessoryEnchant())) //
						|| (item.isArmor() && (item.getTemplate().getType2() != ItemTemplate.TYPE2_ACCESSORY) && (item.getEnchantLevel() > EnchantItemGroupsData.getInstance().getMaxArmorEnchant()))))
				{
					PacketLogger.info("Over-enchanted (+" + item.getEnchantLevel() + ") " + item + " has been removed from " + player);
					player.getInventory().destroyItem(ItemProcessType.DESTROY, item, player, null);
					punish = true;
				}
			}
			
			if (punish && (PlayerConfig.OVER_ENCHANT_PUNISHMENT != IllegalActionPunishmentType.NONE))
			{
				player.sendMessage("[Server]: You have over-enchanted items!");
				player.sendMessage("[Server]: Respect our server rules.");
				player.sendPacket(new ExShowScreenMessage("You have over-enchanted items!", 6000));
				PunishmentManager.handleIllegalPlayerAction(player, player.getName() + " has over-enchanted items.", PlayerConfig.OVER_ENCHANT_PUNISHMENT);
			}
		}
		
		// Remove demonic weapon if character is not cursed weapon equipped.
		if ((player.getInventory().getItemByItemId(8190) != null) && !player.isCursedWeaponEquipped())
		{
			player.destroyItem(ItemProcessType.DESTROY, player.getInventory().getItemByItemId(8190), null, true);
		}
		
		if ((player.getInventory().getItemByItemId(8689) != null) && !player.isCursedWeaponEquipped())
		{
			player.destroyItem(ItemProcessType.DESTROY, player.getInventory().getItemByItemId(8689), null, true);
		}
		
		if (GeneralConfig.ALLOW_MAIL)
		{
			if (MailManager.getInstance().hasUnreadPost(player))
			{
				player.sendPacket(ExNoticePostArrived.valueOf(false));
			}
		}
		
		if (ScreenWelcomeMessageConfig.WELCOME_MESSAGE_ENABLED)
		{
			player.sendPacket(new ExShowScreenMessage(ScreenWelcomeMessageConfig.WELCOME_MESSAGE_TEXT, ScreenWelcomeMessageConfig.WELCOME_MESSAGE_TIME));
		}
		
		if (!player.getPremiumItemList().isEmpty())
		{
			player.sendPacket(ExNotifyPremiumItem.STATIC_PACKET);
		}
		
		if ((OfflineTradeConfig.OFFLINE_TRADE_ENABLE || OfflineTradeConfig.OFFLINE_CRAFT_ENABLE) && OfflineTradeConfig.STORE_OFFLINE_TRADE_IN_REALTIME)
		{
			OfflineTraderTable.getInstance().onTransaction(player, true, false);
		}
		
		// Check if expoff is enabled.
		if (vars.getBoolean("EXPOFF", false))
		{
			player.disableExpGain();
			player.sendMessage("Experience gain is disabled.");
		}
		
		// Send packet that olympiad is opened.
		if (OlympiadConfig.OLYMPIAD_ENABLED && Olympiad.getInstance().inCompPeriod())
		{
			player.sendPacket(new ExOlympiadInfo(1, Olympiad.getInstance().getRemainingTime()));
		}
		else
		{
			player.sendPacket(new ExOlympiadInfo(0, 0));
		}
		
		player.broadcastUserInfo();
		
		if (BeautyShopData.getInstance().hasBeautyData(player.getRace(), player.getAppearance().getSexType()))
		{
			player.sendPacket(new ExBeautyItemList(player));
		}
		
		if (GeneralConfig.ENABLE_WORLD_CHAT)
		{
			player.sendPacket(new ExWorldChatCnt(player));
		}
		
		// Initial mission level progress for correct show RewardList.
		player.getMissionLevelProgress();
		player.sendPacket(new ExConnectedTimeAndGettableReward(player));
		player.sendPacket(new ExOneDayReceiveRewardList(player, true));
		
		// Handle soulshots, disable all on EnterWorld
		player.sendPacket(new ExAutoSoulShot(0, true, 0));
		player.sendPacket(new ExAutoSoulShot(0, true, 1));
		player.sendPacket(new ExAutoSoulShot(0, true, 2));
		player.sendPacket(new ExAutoSoulShot(0, true, 3));
		
		// Auto use restore.
		player.restoreAutoShortcuts();
		player.restoreAutoSettings();
		
		// Client settings restore.
		player.getClientSettings();
		player.sendPacket(new ExItemAnnounceSetting(player.getClientSettings().isAnnounceDisabled()));
		player.restoreChatBackground();
		
		// Fix for equipped item skills
		if (!player.getEffectList().getCurrentAbnormalVisualEffects().isEmpty())
		{
			player.updateAbnormalVisualEffects();
		}
		
		// Death Knight death points init.
		if (player.isDeathKnight())
		{
			// Send twice.
			player.setDeathPoints(500);
			player.setDeathPoints(vars.getInt(PlayerVariables.DEATH_POINT_COUNT, 0));
		}
		// Vanguard beast points init.
		else if (player.isVanguard())
		{
			player.setBeastPoints(1000);
			player.setBeastPoints(vars.getInt(PlayerVariables.BEAST_POINT_COUNT, 0));
		}
		// Assassin points init.
		else if (player.isAssassin() && player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			player.setAssassinationPoints(vars.getInt(PlayerVariables.ASSASSINATION_POINT_COUNT, 0));
		}
		// High elf points init.
		else if (player.isInCategory(CategoryType.HIGH_ELF_TEMPLAR))
		{
			player.setLightPoints(1);
			player.setLightPoints(vars.getInt(PlayerVariables.LIGHT_POINT_COUNT, 0));
		}
		// Wolf points init.
		else if (player.isWarg())
		{
			player.setWolfPoints(1000);
			player.setWolfPoints(vars.getInt(PlayerVariables.WOLF_POINT_COUNT, 0));
		}
		
		// Sayha's Grace.
		if (PlayerConfig.ENABLE_VITALITY)
		{
			player.sendPacket(new ExVitalityEffectInfo(player));
		}
		
		if (MagicLampConfig.ENABLE_MAGIC_LAMP)
		{
			player.sendPacket(new ExMagicLampInfo(player));
		}
		
		if (RandomCraftConfig.ENABLE_RANDOM_CRAFT)
		{
			player.sendPacket(new ExCraftInfo(player));
		}
		
		if (HuntPassConfig.ENABLE_HUNT_PASS)
		{
			player.sendPacket(new HuntPassSimpleInfo(player));
		}
		
		if (AchievementBoxConfig.ENABLE_ACHIEVEMENT_BOX)
		{
			player.sendPacket(new ExSteadyBoxUiInit(player));
		}
		
		if ((player.getLevel() >= 40) && (player.getPlayerClass().level() > 1))
		{
			player.initElementalSpirits();
		}
		
		for (int category = 1; category <= 7; category++)
		{
			player.sendPacket(new ExCollectionInfo(player, category));
		}
		
		player.sendPacket(new ExCollectionActiveEvent());
		
		player.sendPacket(new ExSubjugationSidebar(player, player.getPurgePoints().get(player.getPurgeLastCategory())));
		
		// Relic Collections.
		if (RelicSystemConfig.RELIC_SYSTEM_ENABLED)
		{
			player.sendPacket(new ExRelicsList(player));
			player.sendPacket(new ExRelicsCollectionInfo(player));
		}
		
		player.sendPacket(new ItemDeletionInfo());
		
		player.applyKarmaPenalty();
		
		SiegeManager.getInstance().sendSiegeInfo(player);
		
		// Activate first agathion when available.
		final Item agathion = player.getInventory().unEquipItemInBodySlot(BodyPart.AGATHION);
		if (agathion != null)
		{
			player.getInventory().equipItemAndRecord(agathion);
		}
		
		// Old ammunition check.
		final Item leftHandItem = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if ((leftHandItem != null) && ((leftHandItem.getItemType() == EtcItemType.ARROW) || (leftHandItem.getItemType() == EtcItemType.BOLT) || (leftHandItem.getItemType() == EtcItemType.ELEMENTAL_ORB)))
		{
			player.getInventory().unEquipItemInBodySlot(BodyPart.L_HAND);
		}
		
		// Mable event.
		if (MableGameData.getInstance().isEnabled())
		{
			player.sendPacket(ExMableGameUILauncher.STATIC_PACKET);
		}
		
		// World Trade.
		WorldExchangeManager.getInstance().checkPlayerSellAlarm(player);
		
		// Dual inventory.
		player.restoreDualInventory();
		
		if (AttendanceRewardsConfig.ENABLE_ATTENDANCE_REWARDS)
		{
			final AttendanceInfoHolder attendanceInfo = player.getAttendanceInfo();
			if (attendanceInfo.isRewardAvailable())
			{
				player.setAttendanceDelay(AttendanceRewardsConfig.ATTENDANCE_REWARD_DELAY);
			}
			
			ThreadPool.schedule(() ->
			{
				// Check if player can receive reward today.
				if (attendanceInfo.isRewardAvailable())
				{
					final int lastRewardIndex = attendanceInfo.getRewardIndex() + 1;
					player.sendPacket(new ExShowScreenMessage("Your attendance day " + lastRewardIndex + " reward is ready.", ExShowScreenMessage.TOP_CENTER, 7000, 0, true, true));
					player.sendMessage("Your attendance day " + lastRewardIndex + " reward is ready.");
					player.sendMessage("Click on General Menu -> Attendance Check.");
					if (AttendanceRewardsConfig.ATTENDANCE_POPUP_WINDOW)
					{
						player.sendPacket(new ExVipAttendanceList(player));
					}
					
					player.sendPacket(new ExVipAttendanceNotify());
				}
			}, AttendanceRewardsConfig.ATTENDANCE_REWARD_DELAY * 60 * 1000);
			
			if (AttendanceRewardsConfig.ATTENDANCE_POPUP_START)
			{
				player.sendPacket(new ExVipAttendanceList(player));
			}
			
			player.sendPacket(new ExVipAttendanceItemList());
		}
		
		// Delayed HWID checks.
		if (ServerConfig.HARDWARE_INFO_ENABLED)
		{
			ThreadPool.schedule(() ->
			{
				// Generate trace string.
				final StringBuilder sb = new StringBuilder();
				for (int[] i : _tracert)
				{
					for (int j : i)
					{
						sb.append(j);
						sb.append(".");
					}
				}
				
				final String trace = sb.toString();
				
				// Get hardware info from client.
				ClientHardwareInfoHolder hwInfo = client.getHardwareInfo();
				if (hwInfo != null)
				{
					hwInfo.store(player);
					TRACE_HWINFO.put(trace, hwInfo);
				}
				else
				{
					// Get hardware info from stored tracert map.
					hwInfo = TRACE_HWINFO.get(trace);
					if (hwInfo != null)
					{
						hwInfo.store(player);
						client.setHardwareInfo(hwInfo);
					}
					// Get hardware info from account variables.
					else
					{
						final String storedInfo = player.getAccountVariables().getString(AccountVariables.HWID, "");
						if (!storedInfo.isEmpty())
						{
							hwInfo = new ClientHardwareInfoHolder(storedInfo);
							TRACE_HWINFO.put(trace, hwInfo);
							client.setHardwareInfo(hwInfo);
						}
					}
				}
				
				// Banned?
				if ((hwInfo != null) && PunishmentManager.getInstance().hasPunishment(hwInfo.getMacAddress(), PunishmentAffect.HWID, PunishmentType.BAN))
				{
					Disconnection.of(client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
					return;
				}
				
				// Check max players.
				if (ServerConfig.KICK_MISSING_HWID && (hwInfo == null))
				{
					Disconnection.of(client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
				}
				else if (ServerConfig.MAX_PLAYERS_PER_HWID > 0)
				{
					int count = 0;
					for (Player plr : World.getInstance().getPlayers())
					{
						if (plr.isOnlineInt() == 1)
						{
							final ClientHardwareInfoHolder hwi = plr.getClient().getHardwareInfo();
							if ((hwi != null) && hwi.equals(hwInfo))
							{
								count++;
							}
						}
					}
					
					if (count > ServerConfig.MAX_PLAYERS_PER_HWID)
					{
						Disconnection.of(client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
					}
				}
			}, 5000);
		}
		
		// Chat banned icon.
		ThreadPool.schedule(() ->
		{
			if (player.isChatBanned())
			{
				player.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.NO_CHAT);
			}
		}, 5500);
		
		AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OFFLINE_PLAY, player);
		
		// EnterWorld has finished.
		player.setEnteredWorld();
		
		// Wedding checks.
		if (WeddingConfig.ALLOW_WEDDING)
		{
			final int playerObjectId = player.getObjectId();
			for (Couple couple : CoupleManager.getInstance().getCouples())
			{
				if ((couple.getPlayer1Id() == playerObjectId) || (couple.getPlayer2Id() == playerObjectId))
				{
					if (couple.getMaried())
					{
						player.setMarried(true);
					}
					
					player.setCoupleId(couple.getId());
					
					if (couple.getPlayer1Id() == playerObjectId)
					{
						player.setPartnerId(couple.getPlayer2Id());
					}
					else
					{
						player.setPartnerId(couple.getPlayer1Id());
					}
				}
			}
			
			final int partnerId = player.getPartnerId();
			if (partnerId != 0)
			{
				final Player partner = World.getInstance().getPlayer(partnerId);
				if (partner != null)
				{
					partner.sendMessage("Your partner has logged in.");
				}
			}
		}
		
		if ((player.hasPremiumStatus() || !PremiumSystemConfig.PC_CAFE_ONLY_PREMIUM) && PremiumSystemConfig.PC_CAFE_RETAIL_LIKE)
		{
			PcCafePointsManager.getInstance().run(player);
		}
		
		// Remove variable used by hunting zone system.
		player.getVariables().remove(PlayerVariables.LAST_HUNTING_ZONE_ID);
	}
	
	private void notifyClanMembers(Player player)
	{
		final Clan clan = player.getClan();
		if (clan != null)
		{
			clan.getClanMember(player.getObjectId()).setPlayer(player);
			
			final SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_HAS_LOGGED_IN);
			msg.addString(player.getName());
			clan.broadcastToOtherOnlineMembers(msg, player);
			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(player), player);
			
			player.sendPacket(new ExPledgeContributionList(clan.getMembers()));
		}
	}
	
	private void notifySponsorOrApprentice(Player player)
	{
		if (player.getSponsor() != 0)
		{
			final Player sponsor = World.getInstance().getPlayer(player.getSponsor());
			if (sponsor != null)
			{
				final SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_MENTEE_S1_HAS_LOGGED_IN);
				msg.addString(player.getName());
				sponsor.sendPacket(msg);
			}
		}
		else if (player.getApprentice() != 0)
		{
			final Player apprentice = World.getInstance().getPlayer(player.getApprentice());
			if (apprentice != null)
			{
				final SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_SPONSOR_C1_HAS_LOGGED_IN);
				msg.addString(player.getName());
				apprentice.sendPacket(msg);
			}
		}
	}
}
