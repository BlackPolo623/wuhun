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
import java.sql.ResultSet;
import java.util.AbstractMap.SimpleEntry;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerPurgeHolder;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.serverpackets.subjugation.ExSubjugationSidebar;

/**
 * @author Berezkin Nikolay
 */
public class PurgeRankingManager
{
	protected static final Logger LOGGER = Logger.getLogger(PurgeRankingManager.class.getName());
	
	private static final Map<Integer, Map<Integer, StatSet>> _ranking = new HashMap<>();
	private static final String RESTORE_SUBJUGATION = "SELECT *, `points` + `keys` * 1000000 as `total` FROM `character_purge` WHERE `category`=? ORDER BY `total` DESC";
	private static final String DELETE_SUBJUGATION = "DELETE FROM character_purge WHERE charId=? and category=?";
	
	public PurgeRankingManager()
	{
		updateRankingFromDB();
		
		int nextDate = Calendar.getInstance().get(Calendar.MINUTE);
		while ((nextDate % 5) != 0)
		{
			nextDate = nextDate + 1;
		}
		
		ThreadPool.scheduleAtFixedRate(this::updateRankingFromDB, (nextDate - Calendar.getInstance().get(Calendar.MINUTE)) > 0 ? (long) (nextDate - Calendar.getInstance().get(Calendar.MINUTE)) * 60 * 1000 : (long) ((nextDate + 60) - Calendar.getInstance().get(Calendar.MINUTE)) * 60 * 1000, 300000);
	}

	private void updateRankingFromDB() {
		// Weekly rewards.
		final long lastPurgeRewards = GlobalVariablesManager.getInstance().getLong(GlobalVariablesManager.PURGE_REWARD_TIME, 0);
		if ((Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) && ((System.currentTimeMillis() - lastPurgeRewards) > (604800000 /* 1 week */ - 600000 /* task delay x2 */))) {
			GlobalVariablesManager.getInstance().set(GlobalVariablesManager.PURGE_REWARD_TIME, System.currentTimeMillis());

			for (int category = 11; category <= 16; category++) {
				if (getTop5(category) != null) {
					int counter = 0;
					for (Entry<String, Integer> purgeData : getTop5(category).entrySet()) {
						final int charId = CharInfoTable.getInstance().getIdByName(purgeData.getKey());
						final Message msg = new Message(charId, GeneralConfig.SUBJUGATION_TOPIC_HEADER, GeneralConfig.SUBJUGATION_TOPIC_BODY, MailType.PURGE_REWARD);
						final Mail attachment = msg.createAttachments();

						// 獎勵1：寶石選擇交換卷（200/180/160/140/120）
						int reward1 = 97597;
						int rewardCount1 = 200 - (counter * 20);
						attachment.addItem(ItemProcessType.REWARD, reward1, rewardCount1, null, null);

						// 獎勵2：物品95781（5/4/3/2/1）
						int reward2 = 95781;
						int rewardCount2 = 5 - counter;
						attachment.addItem(ItemProcessType.REWARD, reward2, rewardCount2, null, null);

						// 獎勵3：物品98063（5/4/3/2/1）
						int reward3 = 98063;
						int rewardCount3 = 5 - counter;
						attachment.addItem(ItemProcessType.REWARD, reward3, rewardCount3, null, null);

						MailManager.getInstance().sendMessage(msg);

						try (Connection con = DatabaseFactory.getConnection()) {
							try (PreparedStatement st = con.prepareStatement(DELETE_SUBJUGATION)) {
								st.setInt(1, charId);
								st.setInt(2, category);
								st.execute();
							} catch (Exception e) {
								LOGGER.log(Level.SEVERE, "Failed to delete character subjugation info " + charId, e);
							}
						} catch (Exception e) {
							LOGGER.log(Level.SEVERE, "Failed to delete character subjugation info " + charId, e);
						}

						final Player onlinePlayer = World.getInstance().getPlayer(charId);
						if (onlinePlayer != null) {
							onlinePlayer.getPurgePoints().clear();
							onlinePlayer.sendPacket(new ExSubjugationSidebar(null, new PlayerPurgeHolder(0, 0, 0)));
						}

						counter++;
					}
				}
			}
		}
	}
	
	private void restoreByCategories(int category)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESTORE_SUBJUGATION))
		{
			statement.setInt(1, category);
			try (ResultSet rset = statement.executeQuery())
			{
				int rank = 1;
				Map<Integer, StatSet> rankingInCategory = new HashMap<>();
				while (rset.next())
				{
					final StatSet set = new StatSet();
					set.set("charId", rset.getInt("charId"));
					set.set("points", rset.getInt("total"));
					rankingInCategory.put(rank, set);
					rank++;
				}
				
				_ranking.put(category, rankingInCategory);
			}
			// LOGGER.info(getClass().getSimpleName() + ": Loaded " + _ranking.get(category).size() + " records for category " + category + ".");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Could not restore subjugation ranking data", e);
		}
	}
	
	public Map<String, Integer> getTop5(int category)
	{
		Map<String, Integer> top5 = new HashMap<>();
		for (int i = 1; i <= 5; i++)
		{
			try
			{
				if (_ranking.get(category) == null)
				{
					continue;
				}
				
				final StatSet ss = _ranking.get(category).get(i);
				if (ss == null)
				{
					continue;
				}
				
				final String charName = CharInfoTable.getInstance().getNameById(ss.getInt("charId"));
				final int points = ss.getInt("points");
				top5.put(charName, points);
			}
			catch (IndexOutOfBoundsException ignored)
			{
			}
		}
		
		return top5.entrySet().stream().sorted(Entry.<String, Integer> comparingByValue().reversed()).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (_, e2) -> e2, LinkedHashMap::new));
	}
	
	public SimpleEntry<Integer, Integer> getPlayerRating(int category, int charId)
	{
		if (_ranking.get(category) == null)
		{
			return new SimpleEntry<>(0, 0);
		}
		
		final Optional<Entry<Integer, StatSet>> player = _ranking.get(category).entrySet().stream().filter(it -> it.getValue().getInt("charId") == charId).findFirst();
		if (player.isPresent())
		{
			if (player.get().getValue() == null)
			{
				return new SimpleEntry<>(0, 0);
			}
			
			return new SimpleEntry<>(player.get().getKey(), player.get().getValue().getInt("points"));
		}
		
		return new SimpleEntry<>(0, 0);
	}
	
	public static PurgeRankingManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PurgeRankingManager INSTANCE = new PurgeRankingManager();
	}
}
