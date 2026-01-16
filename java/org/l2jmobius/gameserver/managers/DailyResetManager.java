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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.config.AttendanceRewardsConfig;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.HuntPassConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.TrainingCampConfig;
import org.l2jmobius.gameserver.config.WorldExchangeConfig;
import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.*;
import org.l2jmobius.gameserver.managers.events.BlackCouponManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.model.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.model.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.OnDailyReset;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.primeshop.PrimeShopGroup;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.model.vip.VipManager;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExVoteSystemInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExWorldChatCnt;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.data.holders.LimitShopProductHolder;
import org.l2jmobius.gameserver.network.serverpackets.limitshop.ExPurchaseLimitShopItemListNew;
import org.l2jmobius.gameserver.network.serverpackets.limitshop.ExPurchaseLimitCraftItemList;

/**
 * @author Mobius
 * Optimized by 黑普羅 (2025) - Performance improvements for LimitShop resets
 */
public class DailyResetManager
{
	private static final Logger LOGGER = Logger.getLogger(DailyResetManager.class.getName());

	private static final Set<Integer> RESET_SKILLS = new HashSet<>();
	static
	{
		RESET_SKILLS.add(39199); // Hero's Wondrous Cubic
	}
	public static final Set<Integer> RESET_ITEMS = new HashSet<>();
	static
	{
		RESET_ITEMS.add(49782); // Balthus Knights' Supply Box
	}

	protected DailyResetManager()
	{
		// Schedule reset everyday at 6:30.
		final long nextResetTime = TimeUtil.getNextTime(6, 30).getTimeInMillis();
		final long currentTime = System.currentTimeMillis();

		// Get today's 6:30 AM timestamp.
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 6);
		calendar.set(Calendar.MINUTE, 30);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		final long currentResetTime = calendar.getTimeInMillis();

		// Check if 24 hours have passed since the last daily reset.
		if ((currentTime < currentResetTime) || (GlobalVariablesManager.getInstance().getLong(GlobalVariablesManager.DAILY_TASK_RESET, 0) > currentResetTime))
		{
			LOGGER.info(getClass().getSimpleName() + ": Next schedule at " + TimeUtil.getDateTimeString(nextResetTime) + ".");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": Daily task will run now.");
			onReset();
		}

		// Daily reset task.
		final long startDelay = Math.max(0, nextResetTime - currentTime);
		ThreadPool.scheduleAtFixedRate(this::onReset, startDelay, 86400000); // 86400000 = 1 day

		// Global save task.
		ThreadPool.scheduleAtFixedRate(this::onSave, 1800000, 1800000); // 1800000 = 30 minutes
	}

	/**
	 * 移除在線玩家所有匹配前綴的 AccountVariables
	 * @param prefix 變數前綴（例如 "LCSDailyCount"）
	 * @return 總共移除的數量
	 */
	private int removeOnlineAccountVariables(String prefix)
	{
		int totalRemoved = 0;
		for (Player player : World.getInstance().getPlayers())
		{
			final Set<String> toRemove = new HashSet<>();

			// 找出所有匹配的變數
			for (String key : player.getAccountVariables().getSet().keySet())
			{
				if (key.startsWith(prefix))
				{
					toRemove.add(key);
				}
			}

			// 批量移除
			for (String key : toRemove)
			{
				player.getAccountVariables().remove(key);
			}

			// ========== 關鍵修正：立即保存到資料庫 ==========
			if (!toRemove.isEmpty())
			{
				player.getAccountVariables().saveNow();  // 強制立即保存
				player.broadcastUserInfo();
			}

			totalRemoved += toRemove.size();
		}

		return totalRemoved;
	}

	/**
	 * 發送系統公告給所有在線玩家
	 * @param message 公告訊息
	 */
	private void announceToAll(String message)
	{
		final CreatureSay cs = new CreatureSay(null, ChatType.ANNOUNCEMENT, "", message);
		Broadcast.toAllOnlinePlayers(cs);
	}

	public void onReset()
	{
		LOGGER.info("Starting reset of daily tasks...");
		announceToAll("======================================");
		announceToAll("每日重置開始執行，請稍候...");
		announceToAll("======================================");

		// Store last reset time.
		GlobalVariablesManager.getInstance().set(GlobalVariablesManager.DAILY_TASK_RESET, System.currentTimeMillis());

		// Wednesday weekly tasks.
		final Calendar calendar = Calendar.getInstance();
		if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY)
		{
			clanLeaderApply();
			resetMonsterArenaWeekly();
			resetTimedHuntingZonesWeekly();
			resetVitalityWeekly();
			resetPrivateStoreHistory();
			resetWeeklyLimitShopData();
		}
		else // All days, except Wednesday.
		{
			resetVitalityDaily();
		}

		if (HuntPassConfig.ENABLE_HUNT_PASS && (calendar.get(Calendar.DAY_OF_MONTH) == HuntPassConfig.HUNT_PASS_PERIOD))
		{
			resetHuntPass();
		}

		if (calendar.get(Calendar.DAY_OF_MONTH) == 1)
		{
			resetMonthlyLimitShopData();
		}

		// Daily tasks.
		resetClanBonus();
		resetClanContributionList();
		resetClanDonationPoints();
		resetDailyHennaPattern();
		resetDailyPouchExtract();
		resetDailySkills();
		resetDailyItems();
		resetDailyPrimeShopData();
		resetDailyLimitShopData();
		resetWorldChatPoints();
		resetRecommends();
		resetTrainingCamp();
		resetTimedHuntingZones();
		resetMorgosMilitaryBase();
		resetDailyMissionRewards();
		resetAttendanceRewards();
		resetVip();
		resetResurrectionByPayment();
		resetScratchCard();
		checkWeekSwap();

		// Trigger daily reset event.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_DAILY_RESET))
		{
			EventDispatcher.getInstance().notifyEvent(new OnDailyReset());
		}

		// Store player variables.
		for (Player player : World.getInstance().getPlayers())
		{
			player.getVariables().storeMe();
			player.getAccountVariables().storeMe();
			player.broadcastUserInfo();
		}


		announceToAll("======================================");
		announceToAll("每日重置執行完畢！");
		announceToAll("======================================");
		LOGGER.info("Daily tasks reset completed.");
	}

	private void checkWeekSwap()
	{
		final long nextEvenWeekSwap = GlobalVariablesManager.getInstance().getLong(GlobalVariablesManager.NEXT_EVEN_WEEK_SWAP, 0);
		if (nextEvenWeekSwap < System.currentTimeMillis())
		{
			final boolean isEvenWeek = GlobalVariablesManager.getInstance().getBoolean(GlobalVariablesManager.IS_EVEN_WEEK, true);
			GlobalVariablesManager.getInstance().set(GlobalVariablesManager.IS_EVEN_WEEK, !isEvenWeek);
			final Calendar calendar = TimeUtil.getNextDayTime(Calendar.WEDNESDAY, 6, 25);
			GlobalVariablesManager.getInstance().set(GlobalVariablesManager.NEXT_EVEN_WEEK_SWAP, calendar.getTimeInMillis());
		}
	}

	private void onSave()
	{
		GlobalVariablesManager.getInstance().storeMe();

		BlackCouponManager.getInstance().storeMe();
		RevengeHistoryManager.getInstance().storeMe();

		if (WorldExchangeConfig.WORLD_EXCHANGE_LAZY_UPDATE)
		{
			WorldExchangeManager.getInstance().storeMe();
		}

		if (Olympiad.getInstance().inCompPeriod())
		{
			Olympiad.getInstance().saveOlympiadStatus();
			LOGGER.info("Olympiad System: Data updated.");
		}

		MableGameData.getInstance().save();
	}

	private void clanLeaderApply()
	{
		for (Clan clan : ClanTable.getInstance().getClans())
		{
			if (clan.getNewLeaderId() != 0)
			{
				final ClanMember member = clan.getClanMember(clan.getNewLeaderId());
				if (member == null)
				{
					continue;
				}

				clan.setNewLeader(member);
			}
		}

		LOGGER.info("Clan leaders have been updated.");
	}

	private void resetClanContributionList()
	{
		for (Clan clan : ClanTable.getInstance().getClans())
		{
			clan.getVariables().deleteWeeklyContribution();
		}
	}

	private void resetVitalityDaily()
	{
		if (!PlayerConfig.ENABLE_VITALITY)
		{
			return;
		}

		int vitality = PlayerStat.MAX_VITALITY_POINTS / 4;
		for (Player player : World.getInstance().getPlayers())
		{
			final int VP = player.getVitalityPoints();
			player.setVitalityPoints(VP + vitality, false);
			for (SubClassHolder subclass : player.getSubClasses().values())
			{
				final int VPS = subclass.getVitalityPoints();
				subclass.setVitalityPoints(VPS + vitality);
			}
		}

		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement st = con.prepareStatement("UPDATE character_subclasses SET vitality_points = IF(vitality_points = ?, vitality_points, vitality_points + ?)"))
			{
				st.setInt(1, PlayerStat.MAX_VITALITY_POINTS);
				st.setInt(2, PlayerStat.MAX_VITALITY_POINTS / 4);
				st.execute();
			}

			try (PreparedStatement st = con.prepareStatement("UPDATE characters SET vitality_points = IF(vitality_points = ?, vitality_points, vitality_points + ?)"))
			{
				st.setInt(1, PlayerStat.MAX_VITALITY_POINTS);
				st.setInt(2, PlayerStat.MAX_VITALITY_POINTS / 4);
				st.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while updating vitality", e);
		}

		LOGGER.info("Daily vitality added successfully.");
	}

	private void resetVitalityWeekly()
	{
		if (!PlayerConfig.ENABLE_VITALITY)
		{
			return;
		}

		for (Player player : World.getInstance().getPlayers())
		{
			player.setVitalityPoints(PlayerStat.MAX_VITALITY_POINTS, false);
			for (SubClassHolder subclass : player.getSubClasses().values())
			{
				subclass.setVitalityPoints(PlayerStat.MAX_VITALITY_POINTS);
			}
		}

		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement st = con.prepareStatement("UPDATE character_subclasses SET vitality_points = ?"))
			{
				st.setInt(1, PlayerStat.MAX_VITALITY_POINTS);
				st.execute();
			}

			try (PreparedStatement st = con.prepareStatement("UPDATE characters SET vitality_points = ?"))
			{
				st.setInt(1, PlayerStat.MAX_VITALITY_POINTS);
				st.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while updating vitality", e);
		}

		LOGGER.info("Vitality points have been reset.");
	}

	private void resetMonsterArenaWeekly()
	{
		for (Clan clan : ClanTable.getInstance().getClans())
		{
			GlobalVariablesManager.getInstance().remove(GlobalVariablesManager.MONSTER_ARENA_VARIABLE + clan.getId());
		}
	}

	private void resetClanBonus()
	{
		ClanTable.getInstance().getClans().forEach(Clan::resetClanBonus);
		LOGGER.info("Daily clan bonuses have been reset.");
	}

	private void resetDailySkills()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			for (int skillId : RESET_SKILLS)
			{
				try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_skills_save WHERE skill_id=? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
				{
					ps.setInt(1, skillId);
					ps.execute();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not reset daily skill reuse: ", e);
		}

		// Update data for online players.
		// final Set<Player> updates = new HashSet<>();
		for (int skillId : RESET_SKILLS)
		{
			final Skill skill = SkillData.getInstance().getSkill(skillId, 1 /* No known need for more levels */);
			if (skill != null)
			{
				for (Player player : World.getInstance().getPlayers())
				{
					if (player.hasSkillReuse(skill.getReuseHashCode()))
					{
						player.removeTimeStamp(skill);
						// updates.add(player);
					}
				}
			}
		}

		// for (Player player : updates)
		// {
		// player.sendSkillList();
		// }

		LOGGER.info("Daily skill reuse cleaned.");
	}

	private void resetDailyItems()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			for (int itemId : RESET_ITEMS)
			{
				try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_item_reuse_save WHERE itemId=? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
				{
					ps.setInt(1, itemId);
					ps.execute();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not reset daily item reuse: ", e);
		}

		// Update data for online players.
		boolean update;
		for (Player player : World.getInstance().getPlayers())
		{
			update = false;
			for (int itemId : RESET_ITEMS)
			{
				for (Item item : player.getInventory().getAllItemsByItemId(itemId))
				{
					player.getItemReuseTimeStamps().remove(item.getObjectId());
					update = true;
				}
			}

			if (update)
			{
				player.sendItemList();
			}
		}

		LOGGER.info("Daily item reuse cleaned.");
	}

	private void resetClanDonationPoints()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
			{
				ps.setString(1, PlayerVariables.CLAN_DONATION_POINTS);
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not reset clan donation points: ", e);
		}

		// Update data for online players.
		for (Player player : World.getInstance().getPlayers())
		{
			player.getVariables().remove(PlayerVariables.CLAN_DONATION_POINTS);
		}

		LOGGER.info("Daily clan donation points have been reset.");
	}

	private void resetWorldChatPoints()
	{
		if (!GeneralConfig.ENABLE_WORLD_CHAT)
		{
			return;
		}

		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement("UPDATE character_variables SET val = ? WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
		{
			ps.setInt(1, 0);
			ps.setString(2, PlayerVariables.WORLD_CHAT_VARIABLE_NAME);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not reset daily world chat points: ", e);
		}

		// Update data for online players.
		for (Player player : World.getInstance().getPlayers())
		{
			player.setWorldChatUsed(0);
			player.sendPacket(new ExWorldChatCnt(player));
		}

		LOGGER.info("Daily world chat points have been reset.");
	}

	private void resetRecommends()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("UPDATE character_reco_bonus SET rec_left = ?, rec_have = 0 WHERE rec_have <= 20"))
			{
				ps.setInt(1, 0); // Rec left = 0
				ps.execute();
			}

			try (PreparedStatement ps = con.prepareStatement("UPDATE character_reco_bonus SET rec_left = ?, rec_have = GREATEST(rec_have - 20,0) WHERE rec_have > 20"))
			{
				ps.setInt(1, 0); // Rec left = 0
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not reset Recommendations System: ", e);
		}

		for (Player player : World.getInstance().getPlayers())
		{
			player.setRecomLeft(0);
			player.setRecomHave(player.getRecomHave() - 20);
			player.sendPacket(new ExVoteSystemInfo(player));
			player.broadcastUserInfo();
		}
	}

	private void resetTrainingCamp()
	{
		if (TrainingCampConfig.TRAINING_CAMP_ENABLE)
		{
			// Update data for offline players.
			try (Connection con = DatabaseFactory.getConnection();
				 PreparedStatement ps = con.prepareStatement("DELETE FROM account_gsdata WHERE var = ? AND account_name NOT IN (SELECT account_name FROM characters WHERE online = 1)"))
			{
				ps.setString(1, "TRAINING_CAMP_DURATION");
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Could not reset Training Camp: ", e);
			}

			// Update data for online players.
			for (Player player : World.getInstance().getPlayers())
			{
				player.resetTraingCampDuration();
			}

			LOGGER.info("Training Camp durations have been reset.");
		}
	}

	private void resetVip()
	{
		// Delete all entries for received gifts
		AccountVariables.deleteVipPurchases(AccountVariables.VIP_ITEM_BOUGHT);

		// Checks the tier expiration for online players
		// offline players get handled on next time they log in.
		for (Player player : World.getInstance().getPlayers())
		{
			if (player.getVipTier() > 0)
			{
				VipManager.getInstance().checkVipTierExpiration(player);
			}

			player.getAccountVariables().restoreMe();
		}
	}

	private void resetDailyMissionRewards()
	{
		DailyMissionData.getInstance().getDailyMissionData().forEach(DailyMissionDataHolder::reset);
	}

	private void resetTimedHuntingZones()
	{
		for (TimedHuntingZoneHolder holder : TimedHuntingZoneData.getInstance().getAllHuntingZones())
		{
			if (holder.isWeekly())
			{
				continue;
			}

			// Update data for offline players.
			try (Connection con = DatabaseFactory.getConnection();
				 PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var IN (?, ?, ?) AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
			{
				ps.setString(1, PlayerVariables.HUNTING_ZONE_ENTRY + holder.getZoneId());
				ps.setString(2, PlayerVariables.HUNTING_ZONE_TIME + holder.getZoneId());
				ps.setString(3, PlayerVariables.HUNTING_ZONE_REMAIN_REFILL + holder.getZoneId());
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Could not reset Special Hunting Zones: ", e);
			}

			// Update data for online players.
			for (Player player : World.getInstance().getPlayers())
			{
				player.getVariables().remove(PlayerVariables.HUNTING_ZONE_ENTRY + holder.getZoneId());
				player.getVariables().remove(PlayerVariables.HUNTING_ZONE_TIME + holder.getZoneId());
				player.getVariables().remove(PlayerVariables.HUNTING_ZONE_REMAIN_REFILL + holder.getZoneId());
			}
		}

		LOGGER.info("Special Hunting Zones have been reset.");
	}

	private void resetTimedHuntingZonesWeekly()
	{
		for (TimedHuntingZoneHolder holder : TimedHuntingZoneData.getInstance().getAllHuntingZones())
		{
			if (!holder.isWeekly())
			{
				continue;
			}

			// Update data for offline players.
			try (Connection con = DatabaseFactory.getConnection();
				 PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var IN (?, ?, ?) AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
			{
				ps.setString(1, PlayerVariables.HUNTING_ZONE_ENTRY + holder.getZoneId());
				ps.setString(2, PlayerVariables.HUNTING_ZONE_TIME + holder.getZoneId());
				ps.setString(3, PlayerVariables.HUNTING_ZONE_REMAIN_REFILL + holder.getZoneId());
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Could not reset Weekly Special Hunting Zones: ", e);
			}

			// Update data for online players.
			for (Player player : World.getInstance().getPlayers())
			{
				player.getVariables().remove(PlayerVariables.HUNTING_ZONE_ENTRY + holder.getZoneId());
				player.getVariables().remove(PlayerVariables.HUNTING_ZONE_TIME + holder.getZoneId());
				player.getVariables().remove(PlayerVariables.HUNTING_ZONE_REMAIN_REFILL + holder.getZoneId());
			}
		}

		LOGGER.info("Weekly Special Hunting Zones have been reset.");
	}

	private void resetAttendanceRewards()
	{
		if (AttendanceRewardsConfig.ATTENDANCE_REWARDS_SHARE_ACCOUNT)
		{
			// Update data for offline players.
			try (Connection con = DatabaseFactory.getConnection())
			{
				try (PreparedStatement ps = con.prepareStatement("DELETE FROM account_gsdata WHERE var = ? AND account_name NOT IN (SELECT account_name FROM characters WHERE online = 1)"))
				{
					ps.setString(1, PlayerVariables.ATTENDANCE_DATE);
					ps.execute();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset Attendance Rewards: " + e);
			}

			// Update data for online players.
			for (Player player : World.getInstance().getPlayers())
			{
				player.getAccountVariables().remove(PlayerVariables.ATTENDANCE_DATE);
			}

			LOGGER.info("Account shared Attendance Rewards have been reset.");
		}
		else
		{
			// Update data for offline players.
			try (Connection con = DatabaseFactory.getConnection())
			{
				try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
				{
					ps.setString(1, PlayerVariables.ATTENDANCE_DATE);
					ps.execute();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset Attendance Rewards: " + e);
			}

			// Update data for online players.
			for (Player player : World.getInstance().getPlayers())
			{
				player.getVariables().remove(PlayerVariables.ATTENDANCE_DATE);
			}

			LOGGER.info("Attendance Rewards have been reset.");
		}
	}

	private void resetDailyPrimeShopData()
	{
		for (PrimeShopGroup holder : PrimeShopData.getInstance().getPrimeItems().values())
		{
			// Update data for offline players.
			try (Connection con = DatabaseFactory.getConnection();
				 PreparedStatement ps = con.prepareStatement("DELETE FROM account_gsdata WHERE var = ? AND account_name NOT IN (SELECT account_name FROM characters WHERE online = 1)"))
			{
				ps.setString(1, AccountVariables.PRIME_SHOP_PRODUCT_DAILY_COUNT + holder.getBrId());
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset PrimeShopData: " + e);
			}

			// Update data for online players.
			for (Player player : World.getInstance().getPlayers())
			{
				player.getAccountVariables().remove(AccountVariables.PRIME_SHOP_PRODUCT_DAILY_COUNT + holder.getBrId());
			}
		}

		LOGGER.info("PrimeShopData have been reset.");
	}

	/**
	 * 每日限購商店重置 (優化版本)
	 * 使用 SQL LIKE 模式匹配，一次性刪除所有相關記錄
	 */
	private void resetDailyLimitShopData()
	{
		announceToAll("=== 每日限購商店重置開始 ===");

		int offlineDeleted = 0;
		int onlineDeleted = 0;

		// 離線玩家:使用 LIKE 一次性刪除所有匹配的記錄
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "DELETE FROM account_gsdata WHERE var LIKE ? AND account_name NOT IN (SELECT account_name FROM characters WHERE online = 1)"))
		{
			ps.setString(1, AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + "%");
			offlineDeleted = ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset daily LimitShopData: ", e);
			return;
		}

		// 在線玩家:批量移除匹配的變數
		onlineDeleted = removeOnlineAccountVariables(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT);

		// 新增:強制刷新在線玩家的商店界面
		refreshLimitShopForOnlinePlayers();

		final String result = String.format("每日限購商店重置完成:離線 %d 筆,在線 %d 筆,共 %d 筆。",
				offlineDeleted, onlineDeleted, offlineDeleted + onlineDeleted);
		LOGGER.info(result);
		announceToAll("=== " + result + " ===");
	}

	/**
	 * 每週限購商店重置 (優化版本)
	 * 使用 SQL LIKE 模式匹配，一次性刪除所有相關記錄
	 */
	private void resetWeeklyLimitShopData()
	{
		announceToAll("=== 每週限購商店重置開始 ===");

		int offlineDeleted = 0;
		int onlineDeleted = 0;

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "DELETE FROM account_gsdata WHERE var LIKE ? AND account_name NOT IN (SELECT account_name FROM characters WHERE online = 1)"))
		{
			ps.setString(1, AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + "%");
			offlineDeleted = ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset weekly LimitShopData: ", e);
			return;
		}

		onlineDeleted = removeOnlineAccountVariables(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT);

		// 新增:強制刷新在線玩家的商店界面
		refreshLimitShopForOnlinePlayers();

		final String result = String.format("每週限購商店重置完成:離線 %d 筆,在線 %d 筆,共 %d 筆。",
				offlineDeleted, onlineDeleted, offlineDeleted + onlineDeleted);
		LOGGER.info(result);
		announceToAll("=== " + result + " ===");
	}

	/**
	 * 每月限購商店重置 (優化版本)
	 * 使用 SQL LIKE 模式匹配，一次性刪除所有相關記錄
	 */
	private void resetMonthlyLimitShopData()
	{
		announceToAll("=== 每月限購商店重置開始 ===");

		int offlineDeleted = 0;
		int onlineDeleted = 0;

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(
					 "DELETE FROM account_gsdata WHERE var LIKE ? AND account_name NOT IN (SELECT account_name FROM characters WHERE online = 1)"))
		{
			ps.setString(1, AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + "%");
			offlineDeleted = ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset monthly LimitShopData: ", e);
			return;
		}

		onlineDeleted = removeOnlineAccountVariables(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT);

		// 新增:強制刷新在線玩家的商店界面
		refreshLimitShopForOnlinePlayers();

		final String result = String.format("每月限購商店重置完成:離線 %d 筆,在線 %d 筆,共 %d 筆。",
				offlineDeleted, onlineDeleted, offlineDeleted + onlineDeleted);
		LOGGER.info(result);
		announceToAll("=== " + result + " ===");
	}

	private void refreshLimitShopForOnlinePlayers()
	{

		for (Player player : World.getInstance().getPlayers())
		{
			try
			{
				// 方法1:重新發送商店列表封包 (推薦)
				// 這會模擬玩家重新打開商店的行為

				// Lcoin 商店
				final List<LimitShopProductHolder> normalProducts = LimitShopData.getInstance().getProducts();
				if (!normalProducts.isEmpty())
				{
					final int totalPages = (normalProducts.size() / 350) + ((normalProducts.size() % 350) == 0 ? 0 : 1);
					for (int page = 0; page < totalPages; page++)
					{
						final int start = page * 350;
						final int end = Math.min(start + 350, normalProducts.size());
						final List<LimitShopProductHolder> pageProducts = normalProducts.subList(start, end);
						player.sendPacket(new ExPurchaseLimitShopItemListNew(player, 3, page + 1, totalPages, pageProducts));
					}
				}

				// Lcoin Special Craft
				final List<LimitShopProductHolder> craftProducts = LimitShopCraftData.getInstance().getProducts();
				if (!craftProducts.isEmpty())
				{
					final int totalPages = (craftProducts.size() / 350) + ((craftProducts.size() % 350) == 0 ? 0 : 1);
					for (int page = 0; page < totalPages; page++)
					{
						final int start = page * 350;
						final int end = Math.min(start + 350, craftProducts.size());
						final List<LimitShopProductHolder> pageProducts = craftProducts.subList(start, end);
						player.sendPacket(new ExPurchaseLimitCraftItemList(player, page + 1, totalPages, pageProducts));
					}
				}

				// Clan Shop
				final List<LimitShopProductHolder> clanProducts = LimitShopClanData.getInstance().getProducts();
				if (!clanProducts.isEmpty())
				{
					final int totalPages = (clanProducts.size() / 350) + ((clanProducts.size() % 350) == 0 ? 0 : 1);
					for (int page = 0; page < totalPages; page++)
					{
						final int start = page * 350;
						final int end = Math.min(start + 350, clanProducts.size());
						final List<LimitShopProductHolder> pageProducts = clanProducts.subList(start, end);
						player.sendPacket(new ExPurchaseLimitShopItemListNew(player, 100, page + 1, totalPages, pageProducts));
					}
				}

				LOGGER.fine("Refreshed LimitShop for player: " + player.getName());
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Failed to refresh LimitShop for player: " + player.getName(), e);
			}
		}

		LOGGER.info("LimitShop refreshed for all online players.");
	}

	private void resetHuntPass()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement statement = con.prepareStatement("DELETE FROM huntpass"))
		{
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not delete entries from hunt pass: " + e);
		}

		// Update data for online players.
		for (Player player : World.getInstance().getPlayers())
		{
			player.getHuntPass().resetHuntPass();
		}

		LOGGER.info("HuntPassData have been reset.");
	}

	private void resetResurrectionByPayment()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
			{
				ps.setString(1, PlayerVariables.RESURRECT_BY_PAYMENT_COUNT);
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset payment resurrection count for players: " + e);
		}

		// Update data for online players.
		for (Player player : World.getInstance().getPlayers())
		{
			player.getVariables().remove(PlayerVariables.RESURRECT_BY_PAYMENT_COUNT);
		}

		LOGGER.info("Daily payment resurrection count for player have been reset.");
	}

	public void resetPrivateStoreHistory()
	{
		try
		{
			PrivateStoreHistoryManager.getInstance().reset();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset private store history! " + e);
		}

		LOGGER.info("Private store history records have been reset.");
	}

	private void resetDailyHennaPattern()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
			{
				ps.setString(1, PlayerVariables.DYE_POTENTIAL_DAILY_COUNT);
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset Daily Henna Count: " + e);
		}

		// Update data for online players.
		for (Player player : World.getInstance().getPlayers())
		{
			player.getVariables().remove(PlayerVariables.DYE_POTENTIAL_DAILY_COUNT);
		}

		LOGGER.info("Daily Henna Count have been reset.");
	}

	private void resetMorgosMilitaryBase()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
			{
				ps.setString(1, "MORGOS_MILITARY_FREE");
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset MorgosMilitaryBase: " + e);
		}

		// Update data for online players.
		for (Player player : World.getInstance().getPlayers())
		{
			player.getAccountVariables().remove("MORGOS_MILITARY_FREE");
		}

		LOGGER.info("MorgosMilitaryBase have been reset.");
	}

	private void resetDailyPouchExtract()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
			{
				ps.setString(1, PlayerVariables.DAILY_EXTRACT_ITEM + Inventory.SP_POUCH);
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset Daily Pouch Extract: " + e);
		}

		// Update data for online players.
		for (Player player : World.getInstance().getPlayers())
		{
			player.getVariables().remove(PlayerVariables.DAILY_EXTRACT_ITEM + Inventory.SP_POUCH);
		}

		LOGGER.info("Daily Pouch Extract Count have been reset.");
	}

	/**
	 * 重置刮刮樂每日購買次數
	 */
	private void resetScratchCard()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
			{
				ps.setString(1, "ScratchCard_daily_count");
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset Scratch Card daily count: ", e);
		}

		// Update data for online players.
		for (Player player : World.getInstance().getPlayers())
		{
			player.getVariables().remove("ScratchCard_daily_count");
		}

		LOGGER.info("Scratch Card daily purchase count have been reset.");
	}

	public static DailyResetManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final DailyResetManager INSTANCE = new DailyResetManager();
	}
}