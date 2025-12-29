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
package org.l2jmobius.gameserver.managers.events;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.serverpackets.ExItemAnnounce;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.balthusevent.ExBalthusEvent;
import org.l2jmobius.gameserver.network.serverpackets.balthusevent.ExBalthusEventJackpotUser;

/**
 * @author Index
 */
public class BalthusEventManager
{
	private static final Logger LOGGER = Logger.getLogger(BalthusEventManager.class.getName());
	
	private final Set<Player> _players = ConcurrentHashMap.newKeySet();
	private final Map<Entry<Integer, Integer>, Collection<BalthusItemHolder>> _rewards = new HashMap<>();
	private BalthusItemHolder _rewardItem = null;
	private boolean _isRunning = false;
	private boolean _participationRewardOnRedeem = false;
	private int _minimumLevel = 65;
	private int _maxRollPerHour = 5;
	private boolean _announceWinnerByExItemAnnounce = true;
	private boolean _useSystemMessageToAnnounce = true;
	private boolean _showAnnounceWithName = true;
	private ItemHolder _consolation = new ItemHolder(49783, 100); // Sibi's Coin
	private ItemHolder _dailySupplyItem = new ItemHolder(49782, 1); // Balthus Knights' Supply Box
	private ItemHolder _dailySupplyFeeItem = new ItemHolder(57, 1); // Adena
	private Entry<Integer, Integer> _winnerCount = new SimpleEntry<>(1, 1);
	private boolean _participationRewardToWinner = false;
	private Map<String, Entry<String, String>> _mail = new HashMap<>();
	private final AtomicInteger _currentRoll = new AtomicInteger(0);
	private int _currProgress = -1;
	private long[] _timeForRoll = null;
	private ScheduledFuture<?> _cycleConcurrentScheduling = null;
	private Date _eventStartDate;
	private Date _eventEndDate;
	
	public BalthusEventManager()
	{
	}
	
	public void init()
	{
		final Date currentDate = new Date();
		if (!isValidPeriod() || currentDate.before(_eventStartDate) || currentDate.after(_eventEndDate))
		{
			if (currentDate.after(_eventEndDate))
			{
				// LOGGER.warning(getClass().getSimpleName() + ": Balthus event cannot be started because the event period has ended.");
			}
			else
			{
				LOGGER.warning(getClass().getSimpleName() + ": Balthus event cannot be started because the event period is not valid. Trying to create a thread to run the event;");
				final long delay = _eventStartDate.getTime() - System.currentTimeMillis();
				if (delay > 0)
				{
					if (_cycleConcurrentScheduling != null)
					{
						_cycleConcurrentScheduling.cancel(true);
					}
					
					_cycleConcurrentScheduling = ThreadPool.schedule(this::init, delay);
				}
				else
				{
					LOGGER.warning(getClass().getSimpleName() + ": Balthus event start time has already passed. Event cannot be started.");
				}
			}
			return;
		}
		
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		initRollTime(calendar.getTimeInMillis());
		if (_currentRoll.get() > _maxRollPerHour)
		{
			calendar.add(Calendar.HOUR_OF_DAY, 1);
			initRollTime(calendar.getTimeInMillis());
		}
		
		rollNextReward();
		initNextRoll();
		_isRunning = true;
	}
	
	public void initParameters(StatSet params)
	{
		_participationRewardOnRedeem = params.getBoolean("give_coin_only_if_redeem", true);
		_minimumLevel = params.getInt("minLevel", 65);
		_maxRollPerHour = Math.min(40, Math.max(1, params.getInt("try_to_roll_per_hour", 5)));
		_announceWinnerByExItemAnnounce = params.getBoolean("announce_by_ex_item_announce", true);
		_useSystemMessageToAnnounce = params.getBoolean("announce_by_system_message", true);
		_showAnnounceWithName = params.getBoolean("show_name_in_announce", true);
		_consolation = new ItemHolder(params.getInt("coin_id", 49783), params.getLong("coin_count", 100L));
		_dailySupplyItem = (params.getBoolean("daily_supply_enabled", true) ? new ItemHolder(params.getInt("daily_supply_id", 49782), params.getLong("daily_supply_count", 1L)) : null);
		_dailySupplyFeeItem = (params.getBoolean("daily_supply_fee_enabled", true) ? new ItemHolder(params.getInt("daily_supply_fee_id", 57), params.getLong("daily_supply_fee_count", 1L)) : null);
		_winnerCount = new SimpleEntry<>(params.getInt("min_winner", 1), params.getInt("max_winner", 1));
		_participationRewardToWinner = params.getBoolean("give_participation_reward_to_winner", false);
		
		final Map<String, Entry<String, String>> mailContent = new HashMap<>();
		final Set<String> availableLang = new HashSet<>();
		final Map<String, String> subject = new HashMap<>();
		final Map<String, String> content = new HashMap<>();
		for (Entry<String, Object> entry : params.getSet().entrySet())
		{
			final String key = entry.getKey();
			if (key.startsWith("mailSubject_"))
			{
				final String value = String.valueOf(entry.getValue());
				final String lang = key.substring("mailSubject_".length());
				subject.put(lang, value);
				availableLang.add(lang);
			}
			
			if (key.startsWith("mailContent_"))
			{
				final String value = String.valueOf(entry.getValue());
				final String lang = key.substring("mailContent_".length());
				content.put(lang, value);
				availableLang.add(lang);
			}
		}
		
		for (String lang : availableLang)
		{
			final String subject2 = subject.getOrDefault(lang, "");
			final String content2 = content.getOrDefault(lang, "");
			mailContent.put(lang, new SimpleEntry<>(subject2, content2));
		}
		
		_mail = mailContent;
	}
	
	public void setEventPeriod(Date startDate, Date endDate)
	{
		_eventStartDate = startDate;
		_eventEndDate = endDate;
	}
	
	public Date getEventStartDate()
	{
		return _eventStartDate;
	}
	
	public Date getEventEndDate()
	{
		return _eventEndDate;
	}
	
	private boolean isValidPeriod()
	{
		return (_eventStartDate != null) && (_eventEndDate != null) && _eventStartDate.before(_eventEndDate);
	}
	
	public void addRewards(Entry<Integer, Integer> period, int itemId, long itemCount, double chanceToObtain, int enchantLevel, double chanceToNextGame, boolean redeemInAnyCase)
	{
		_rewards.computeIfAbsent(period, _ -> new ArrayList<>()).add(new BalthusItemHolder(itemId, itemCount, chanceToObtain, enchantLevel, chanceToNextGame, redeemInAnyCase));
	}
	
	private void initRollTime(long millis)
	{
		final long currentTime = System.currentTimeMillis();
		_timeForRoll = calculateTimeForRolls(millis);
		
		for (int attempt = 1; attempt <= _maxRollPerHour; attempt++)
		{
			int rollIndex = _currentRoll.incrementAndGet();
			if (_timeForRoll[rollIndex] > currentTime)
			{
				break;
			}
		}
	}
	
	private void initNextRoll()
	{
		if (!isValidPeriod())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Event period is not valid. Event end.");
			return;
		}
		
		if (_currentRoll.get() > _maxRollPerHour)
		{
			rollNextReward();
			_isRunning = true;
			_currentRoll.set(0);
			_timeForRoll = calculateTimeForRolls(System.currentTimeMillis());
			_currProgress = -1;
		}
		
		_currentRoll.addAndGet(1);
		
		final long timeForRoll = Math.max(1L, _timeForRoll[_currentRoll.get()] - System.currentTimeMillis());
		if (_cycleConcurrentScheduling != null)
		{
			_cycleConcurrentScheduling.cancel(true);
		}
		
		_cycleConcurrentScheduling = ThreadPool.schedule(this::redeemReward, timeForRoll);
		
		for (Player player : World.getInstance().getPlayers())
		{
			if ((player != null) && !player.isInOfflineMode())
			{
				if (player.getClient() == null)
				{
					continue;
				}
				
				player.sendPacket(new ExBalthusEvent(player));
			}
		}
	}
	
	private void redeemReward()
	{
		if (!_isRunning)
		{
			initNextRoll();
			return;
		}
		
		if (_rewardItem == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": No reward, careful!");
		}
		
		if (!_rewardItem.isRedeemInAnyCase() || (_currentRoll.get() < _maxRollPerHour))
		{
			if (_rewardItem.getChance() <= 2)
			{
				initNextRoll();
				return;
			}
		}
		
		final Set<Player> winners = selectWinnerFromParticipators();
		final Item itemToWinner = (winners != null) ? sendMessageToWinner(winners) : null;
		if (winners != null)
		{
			for (Player winner : winners)
			{
				LOGGER.warning(getClass().getSimpleName() + ": " + winner + " win " + itemToWinner + " in hour " + Calendar.getInstance().get(11));
			}
		}
		
		if ((winners != null) || (_currentRoll.get() >= _maxRollPerHour))
		{
			_currProgress = _currentRoll.get();
			_isRunning = false;
			_currentRoll.set(_maxRollPerHour + 1);
			sendParticipatorsCoin(winners, itemToWinner);
			
			if (_cycleConcurrentScheduling != null)
			{
				_cycleConcurrentScheduling.cancel(true);
			}
			
			_cycleConcurrentScheduling = ThreadPool.schedule(this::initNextRoll, Math.max(1L, _timeForRoll[_maxRollPerHour + 1] - System.currentTimeMillis()));
		}
	}
	
	private Set<Player> selectWinnerFromParticipators()
	{
		final int winnersCount = Math.min(Rnd.get(_winnerCount.getKey(), _winnerCount.getValue()), _players.size());
		final List<Player> participators = new ArrayList<>(_players);
		Collections.shuffle(participators);
		
		final Set<Player> winners = (winnersCount <= 0) ? null : new HashSet<>();
		for (Player participator : participators)
		{
			if (winners == null)
			{
				break;
			}
			
			if (winnersCount <= winners.size())
			{
				break;
			}
			
			if ((participator == null) || participator.isInOfflineMode())
			{
				continue;
			}
			
			if (participator.getClient() == null)
			{
				continue;
			}
			
			if (participator.getLevel() < _minimumLevel)
			{
				continue;
			}
			
			winners.add(participator);
		}
		
		return winners;
	}
	
	private void sendParticipatorsCoin(Set<Player> winners, Item itemToWinner)
	{
		final Set<ServerPacket> packetsForSend = new HashSet<>();
		if (winners != null)
		{
			for (Player winner : winners)
			{
				if (_announceWinnerByExItemAnnounce)
				{
					packetsForSend.add(new ExItemAnnounce(winner, itemToWinner, 5));
				}
				
				if (_useSystemMessageToAnnounce)
				{
					SystemMessage messageAnnounce = null;
					if (_showAnnounceWithName)
					{
						messageAnnounce = new SystemMessage(SystemMessageId.BALTHUS_KNIGHTS_HAVE_GIVEN_THE_GRAND_PRIZE_AWAY_S2_THE_WINNER_S1);
						messageAnnounce.addPcName(winner);
						messageAnnounce.addItemName(itemToWinner);
					}
					else
					{
						messageAnnounce = new SystemMessage(SystemMessageId.THE_SECRET_SUPPLIES_OF_THE_BALTHUS_KNIGHTS_ARRIVED_SOMEONE_RECEIVED_S1);
						messageAnnounce.addItemName(itemToWinner);
					}
					
					packetsForSend.add(messageAnnounce);
				}
			}
		}
		
		if (!_participationRewardOnRedeem || (winners != null))
		{
			for (Player player : World.getInstance().getPlayers())
			{
				if ((player != null) && !player.isInOfflineMode())
				{
					if (player.getClient() == null)
					{
						continue;
					}
					
					if (winners != null)
					{
						player.sendPacket(new ExBalthusEventJackpotUser());
					}
					
					for (ServerPacket packet : packetsForSend)
					{
						player.sendPacket(packet);
					}
					
					if (_players.contains(player) && ((winners == null) || !winners.contains(player) || _participationRewardToWinner))
					{
						player.getVariables().increaseLong(PlayerVariables.BALTHUS_REWARD, 0L, _consolation.getCount());
						player.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_SIBI_S_COIN_X_S1).addInt((int) _consolation.getCount()));
					}
					
					player.sendPacket(new ExBalthusEvent(player));
				}
			}
		}
	}
	
	private Item sendMessageToWinner(Set<Player> winners)
	{
		Item returnItem = null;
		for (Player winner : winners)
		{
			final String lang = winner.getLang();
			final String subject = ((lang != null) && _mail.containsKey(lang)) ? _mail.get(lang).getKey() : ((String) _mail.get("en").getKey());
			final String content = ((lang != null) && _mail.containsKey(lang)) ? _mail.get(lang).getValue() : ((String) _mail.get("en").getValue());
			final Message msg = new Message(winner.getObjectId(), subject, content, MailType.NEWS_INFORMER);
			final Mail attachments = msg.createAttachments();
			returnItem = attachments.addItem(ItemProcessType.REWARD, _rewardItem.getId(), _rewardItem.getCount(), null, null);
			if (returnItem != null)
			{
				returnItem.setEnchantLevel(_rewardItem.getEnchantmentLevel());
			}
			
			MailManager.getInstance().sendMessage(msg);
		}
		
		return returnItem;
	}
	
	public void rollNextReward()
	{
		final int hour = Calendar.getInstance().get(11);
		for (Entry<Entry<Integer, Integer>, Collection<BalthusItemHolder>> entry : _rewards.entrySet())
		{
			final Entry<Integer, Integer> period = entry.getKey();
			if ((period.getKey() <= hour) && (period.getValue() >= hour))
			{
				final Collection<BalthusItemHolder> value = entry.getValue();
				final double rnd = Rnd.get(0.0, 100.0);
				double val = 0.0;
				for (BalthusItemHolder reward : value)
				{
					val += reward.getChanceToNextGame();
					if (rnd <= val)
					{
						_rewardItem = reward;
						break;
					}
				}
				break;
			}
		}
		
		if (_rewardItem == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": No reward, careful!");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": Reward for next " + hour + " hour " + _rewardItem.toString());
		}
	}
	
	private long[] calculateTimeForRolls(long requestedTime)
	{
		final long[] rollTime = new long[_maxRollPerHour + 2];
		rollTime[0] = requestedTime;
		for (int i = 1; i <= _maxRollPerHour; ++i)
		{
			rollTime[i] = rollTime[i - 1] + Rnd.get(TimeUnit.MINUTES.toMillis(1L), TimeUnit.MINUTES.toMillis(60 / _maxRollPerHour));
		}
		
		rollTime[_maxRollPerHour + 1] = rollTime[0] + TimeUnit.MINUTES.toMillis(60L);
		return rollTime;
	}
	
	public void addPlayerToList(Player player)
	{
		_players.add(player);
		player.sendPacket(new ExBalthusEvent(player));
	}
	
	public void removePlayerFromList(Player player)
	{
		_players.remove(player);
		player.sendPacket(new ExBalthusEvent(player));
	}
	
	public int getMinimumLevel()
	{
		return _minimumLevel;
	}
	
	public ItemHolder getConsolation()
	{
		return _consolation;
	}
	
	public ItemHolder getDailySupplyItem()
	{
		return _dailySupplyItem;
	}
	
	public ItemHolder getDailySupplyFeeItem()
	{
		return _dailySupplyFeeItem;
	}
	
	public ItemHolder getCurrRewardItem()
	{
		return _rewardItem;
	}
	
	public int getCurrentProgress()
	{
		return (_currProgress == -1) ? _currentRoll.get() : _currProgress;
	}
	
	public boolean isPlayerParticipant(Player player)
	{
		return _players.contains(player) && (player.getLevel() > _minimumLevel);
	}
	
	public boolean isRunning()
	{
		return _isRunning;
	}
	
	private static class BalthusItemHolder extends ItemChanceHolder
	{
		final double _chanceToNextGame;
		final boolean _redeemInAnyCase;
		
		public BalthusItemHolder(int itemId, long itemCount, double chanceToObtain, int enchantLevel, double chanceToNextGame, boolean redeemInAnyCase)
		{
			super(itemId, chanceToObtain, itemCount, (byte) enchantLevel);
			_chanceToNextGame = chanceToNextGame;
			_redeemInAnyCase = redeemInAnyCase;
		}
		
		public double getChanceToNextGame()
		{
			return _chanceToNextGame;
		}
		
		public boolean isRedeemInAnyCase()
		{
			return _redeemInAnyCase;
		}
	}
	
	public static BalthusEventManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BalthusEventManager INSTANCE = new BalthusEventManager();
	}
}
