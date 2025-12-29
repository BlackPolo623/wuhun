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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.serverpackets.ExItemAnnounce;
import org.l2jmobius.gameserver.network.serverpackets.secretshop.ExFestivalBmAllItemInfo;
import org.l2jmobius.gameserver.network.serverpackets.secretshop.ExFestivalBmGame;
import org.l2jmobius.gameserver.network.serverpackets.secretshop.ExFestivalBmInfo;
import org.l2jmobius.gameserver.network.serverpackets.secretshop.ExFestivalBmTopItemInfo;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Serenitty
 */
public class SecretShopEventManager
{
	private static final Logger LOGGER = Logger.getLogger(SecretShopEventManager.class.getName());
	
	private static final int UPDATE_INTERVAL = 5000;
	private static final int CLAN_REWARD_ITEM_ID = 94834;
	private static final int CLAN_REWARD_ITEM_COUNT = 1000;
	
	private static final int TICKET_AMOUNT_PER_GAME = 1;
	
	private static final ConcurrentHashMap<Player, PlayerTicketData> PLAYER_REWARD_QUEUES = new ConcurrentHashMap<>();
	private static final Set<Player> LIST_TO_UPDATE = ConcurrentHashMap.newKeySet();
	
	private static Map<Integer, List<SecretShopRewardHolder>> _rewardData = new HashMap<>();
	private static List<SecretShopRewardHolder> _activeRewards = new ArrayList<>(); // 初始化為空列表而不是 null
	private static boolean _isEventPeriod;
	private static boolean _exchangeEnabled;
	private static long _startTime;
	private static long _endTime;
	private static int _ticketId;

//	private static int _startHour;
//	private static int _startMinute;
//	private static int _endHour;
//	private static int _endMinute;
	
	private ScheduledFuture<?> _updateTask = null;
	private ScheduledFuture<?> _startTask = null;
	private ScheduledFuture<?> _endTask = null;

	// 替換原本的單一時間變數
	public static class TimePeriod
	{
		public final int startHour;
		public final int startMinute;
		public final int endHour;
		public final int endMinute;

		public TimePeriod(int startHour, int startMinute, int endHour, int endMinute)
		{
			this.startHour = startHour;
			this.startMinute = startMinute;
			this.endHour = endHour;
			this.endMinute = endMinute;
		}
	}

	// 存儲多個時間段
	private static List<TimePeriod> _timePeriods = new ArrayList<>();
	private static TimePeriod _currentPeriod = null;


	protected SecretShopEventManager()
	{
	}

	public void init(Map<Integer, List<SecretShopRewardHolder>> rewardData, int ticketId, List<TimePeriod> timePeriods) {
		_rewardData = rewardData;
		_ticketId = ticketId;
		_timePeriods = timePeriods;
	}
	
	private void start()
	{
		if (!_isEventPeriod)
		{
			return;
		}

		// 確保設定當前時段
		if (!isExchangePeriod())
		{
			LOGGER.warning("Start called but not in exchange period!");
			return;
		}
		
		LOGGER.warning(getClass().getSimpleName() + ": Started!");
		_exchangeEnabled = true;
		_endTime = getNextEndTime();
		_startTime = 0;
		
		initRewards(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)); // 1 (Sunday) to 7 (Saturday).
		broadcastInfo();
		
		if (_updateTask != null)
		{
			_updateTask.cancel(true);
			_updateTask = null;
		}
		
		_updateTask = ThreadPool.scheduleAtFixedRate(this::updateInfo, 500, UPDATE_INTERVAL);
		
		if (_endTask != null)
		{
			_endTask.cancel(false);
			_endTask = null;
		}
		
		LOGGER.warning(getClass().getSimpleName() + ": Scheduled end in " + _endTime + " ms.");
		ThreadPool.schedule(this::stop, _endTime);
	}
	
	private void stop()
	{
		_exchangeEnabled = false;
		_startTime = getNextStartTime();
		_endTime = 0;
		
		for (SecretShopRewardHolder reward : _activeRewards)
		{
			reward._currentAmount = 0;
		}
		
		if (_updateTask != null)
		{
			_updateTask.cancel(true);
			_updateTask = null;
		}
		
		if (_endTask != null)
		{
			_endTask.cancel(false);
			_endTask = null;
		}
		
		updateInfo();
		LIST_TO_UPDATE.clear();
		broadcastInfo();
		
		LOGGER.warning(getClass().getSimpleName() + ": Scheduled start in " + _startTime + " ms.");
		ThreadPool.schedule(this::start, _startTime);
	}
	
	public void startEvent()
	{
		if (_rewardData.isEmpty())
		{
			return;
		}
		
		_isEventPeriod = true;
		LOGGER.warning(getClass().getSimpleName() + ": Activated");
		if (_startTask != null)
		{
			_startTask.cancel(false);
			_startTask = null;
		}
		
		if (_endTask != null)
		{
			_endTask.cancel(false);
			_endTask = null;
		}
		
		if (_updateTask != null)
		{
			_updateTask.cancel(true);
			_updateTask = null;
		}
		
		LIST_TO_UPDATE.clear();
		
		if (isExchangePeriod())
		{
			start();
		}
		else
		{
			_startTime = getNextStartTime();
			LOGGER.warning(getClass().getSimpleName() + ": Scheduled start in " + _startTime + " ms.");
			_startTask = ThreadPool.schedule(this::start, _startTime);
		}
	}
	
	public void stopEvent()
	{
		_isEventPeriod = false;
	}
	
	public synchronized void exchange(Player player)
	{
		// 檢查是否在活動期間
		if (!_isEventPeriod)
		{
			player.sendMessage("秘密商店活動尚未開始。");
			return;
		}

		// 檢查是否在交換時段
		if (!_exchangeEnabled)
		{
			// 計算下一個開放時間
			long nextStart = _startTime;
			long hoursLeft = nextStart / 3600000;
			long minutesLeft = (nextStart % 3600000) / 60000;

			player.sendMessage("秘密商店目前關閉中。");
			player.sendMessage("下次開放時間還有: " + hoursLeft + " 小時 " + minutesLeft + " 分鐘");

			// 或者使用系統訊息
			// player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THE_SHOP_IS_CURRENTLY_CLOSED));
			return;
		}

		// 檢查是否有可用獎勵
		if (!hasAvailableRewards())
		{
			player.sendMessage("秘密商店的獎勵已全部售完。");
			return;
		}

		// 檢查票券數量
		int ticketAmount = (int) player.getInventory().getInventoryItemCount(_ticketId, -1);
		if (ticketAmount < TICKET_AMOUNT_PER_GAME)
		{
			player.sendMessage("您沒有足夠的票券。");
			return;
		}
		
		double totalChance = 0;
		double totalPossibleChance = 0;
		for (SecretShopRewardHolder reward : _activeRewards)
		{
			if (reward._currentAmount <= 0)
			{
				continue;
			}
			
			totalPossibleChance += reward._chance;
		}
		
		final double chance = Rnd.get(0, totalPossibleChance);
		for (SecretShopRewardHolder reward : _activeRewards)
		{
			if (reward._currentAmount <= 0)
			{
				continue;
			}
			
			totalChance += reward._chance;
			if (totalChance >= chance)
			{
				reward._currentAmount--;
				
				player.destroyItemByItemId(ItemProcessType.FEE, _ticketId, TICKET_AMOUNT_PER_GAME, player, true);
				ticketAmount -= TICKET_AMOUNT_PER_GAME;
				
				final Item item = player.addItem(ItemProcessType.REWARD, reward._id, reward._count, player, true);
				player.sendPacket(new ExFestivalBmGame(_ticketId, ticketAmount, TICKET_AMOUNT_PER_GAME, reward._grade, reward._id, (int) reward._count, 1));
				
				if (reward.isTopGrade())
				{
					Broadcast.toAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.EVENT_PARTICIPATE));
					sendClanReward(player); // Optional
					if (noTopGradeRewardsRemaining())
					{
						show(player, true);
						updateInfo();
						stop();
						return;
					}
					
					broadcastInfo();
				}
				break;
			}
		}
		
		show(player, true);
	}
	
	public void show(Player player, boolean show)
	{
		if (!_isEventPeriod)
		{
			return;
		}

		// 添加這個檢查
		if (_activeRewards == null)
		{
			player.sendMessage("秘密商店正在準備中，請稍後再試。");
			return;
		}
		
		if (show)
		{
			if (!LIST_TO_UPDATE.contains(player))
			{
				LIST_TO_UPDATE.add(player);
			}
			
			final int ticketAmount = (int) player.getInventory().getInventoryItemCount(_ticketId, -1);
			player.sendPacket(new ExFestivalBmInfo(_ticketId, ticketAmount, TICKET_AMOUNT_PER_GAME));
			player.sendPacket(new ExFestivalBmAllItemInfo(getSortedRewards()));
		}
		else
		{
			LIST_TO_UPDATE.remove(player);
		}
	}
	
	private void sendClanReward(Player player)
	{
		if (player != null)
		{
			final Clan clan = player.getClan();
			if (clan == null)
			{
				return;
			}
			
			LOGGER.warning(getClass().getSimpleName() + ": Give Clan Reward->: clan=" + clan + " player=" + player);
			clan.getOnlineMembers(player.getObjectId()).forEach(this::sendMail);
		}
		else
		{
			LOGGER.warning(getClass().getSimpleName() + ": Player is Null!");
		}
	}
	
	private void sendMail(Player player)
	{
		final Message message = new Message(player.getObjectId(), "Secret Shop Clan Reward", "Your clan member won one of the main prize of the event. All (online) clan members receive an additional reward.", MailType.REGULAR);
		final Mail attachment = message.createAttachments();
		attachment.addItem(ItemProcessType.REWARD, CLAN_REWARD_ITEM_ID, CLAN_REWARD_ITEM_COUNT, null, null);
		MailManager.getInstance().sendMessage(message);
	}
	
	private void initRewards(int currentGame)
	{
		_activeRewards = new ArrayList<>();
		final List<SecretShopRewardHolder> topRewards = new ArrayList<>();
		final List<SecretShopRewardHolder> rewards = new ArrayList<>();
		for (SecretShopRewardHolder eventReward : _rewardData.get(currentGame))
		{
			final SecretShopRewardHolder reward = new SecretShopRewardHolder(eventReward);
			switch (reward._grade)
			{
				case 3: // 1 Star
				case 2: // 2 Star
				{
					rewards.add(reward);
					break;
				}
				case 1: // 3 Star
				{
					topRewards.add(reward);
					break;
				}
				default:
				{
					LOGGER.warning(getClass().getSimpleName() + ": Incorrect reward grade " + reward._grade);
				}
			}
		}
		
		_activeRewards.clear();
		Collections.shuffle(topRewards);
		_activeRewards.addAll(topRewards.stream().limit(3).collect(Collectors.toList()));
		_activeRewards.addAll(rewards);
	}
	
	private void updateInfo()
	{
		final Iterator<Player> iterator = LIST_TO_UPDATE.iterator();
		while (iterator.hasNext())
		{
			final Player player = iterator.next();
			final GameClient client = player.getClient();
			if (!player.isOnline() || (client == null) || client.isDetached())
			{
				LIST_TO_UPDATE.remove(player);
			}
			else
			{
				sendInfo(player);
				player.sendPacket(new ExFestivalBmAllItemInfo(getSortedRewards()));
			}
		}
	}
	
	public void sendInfo(Player player)
	{
		if (!_isEventPeriod || (player == null))
		{
			return;
		}
		
		player.sendPacket(new ExFestivalBmTopItemInfo(_exchangeEnabled ? getNextEndTime() : getNextStartTime(), _exchangeEnabled, _activeRewards));
	}
	
	private void broadcastInfo()
	{
		if (!_isEventPeriod)
		{
			return;
		}
		
		Broadcast.toAllOnlinePlayers(new ExFestivalBmTopItemInfo(_exchangeEnabled ? getNextEndTime() : getNextStartTime(), _exchangeEnabled, _activeRewards));
	}

	private List<SecretShopRewardHolder> getSortedRewards()
	{
		// 檢查 _activeRewards 是否為 null
		if (_activeRewards == null || _activeRewards.isEmpty())
		{
			return new ArrayList<>(); // 返回空列表
		}

		final List<SecretShopRewardHolder> showRewards = new ArrayList<>(_activeRewards);
		showRewards.sort(Comparator.comparing(SecretShopRewardHolder::getId));
		return showRewards;
	}

	private long getNextStartTime() {
		final Calendar now = Calendar.getInstance();
		long nearestTime = Long.MAX_VALUE;

		for (TimePeriod period : _timePeriods) {
			final Calendar startTime = Calendar.getInstance();
			startTime.set(Calendar.HOUR_OF_DAY, period.startHour);
			startTime.set(Calendar.MINUTE, period.startMinute);
			startTime.set(Calendar.SECOND, 0);
			startTime.set(Calendar.MILLISECOND, 0);

			if (startTime.getTimeInMillis() <= now.getTimeInMillis()) {
				// 如果已經過了今天的這個時段，檢查明天
				startTime.add(Calendar.DAY_OF_MONTH, 1);
			}

			final long timeUntilStart = startTime.getTimeInMillis() - now.getTimeInMillis();
			if (timeUntilStart < nearestTime) {
				nearestTime = timeUntilStart;
				_currentPeriod = period; // 記錄下一個時段
			}
		}

		return nearestTime;
	}



	private long getNextEndTime()
	{
		if (!_exchangeEnabled || _currentPeriod == null)
		{
			return 0;
		}

		final Calendar endTime = Calendar.getInstance();
		endTime.set(Calendar.HOUR_OF_DAY, _currentPeriod.endHour);  // 使用 _currentPeriod
		endTime.set(Calendar.MINUTE, _currentPeriod.endMinute);      // 使用 _currentPeriod
		endTime.set(Calendar.SECOND, 0);
		endTime.set(Calendar.MILLISECOND, 0);

		return Math.max(0, endTime.getTimeInMillis() - System.currentTimeMillis());
	}

	private boolean isExchangePeriod() {
		if (!_isEventPeriod) {
			return false;
		}

		final Calendar now = Calendar.getInstance();
		final int currentHour = now.get(Calendar.HOUR_OF_DAY);
		final int currentMinute = now.get(Calendar.MINUTE);

		for (TimePeriod period : _timePeriods) {
			boolean afterStart = (currentHour > period.startHour) ||
					((currentHour == period.startHour) && (currentMinute >= period.startMinute));
			boolean beforeEnd = (currentHour < period.endHour) ||
					((currentHour == period.endHour) && (currentMinute < period.endMinute));

			if (afterStart && beforeEnd) {
				_currentPeriod = period;
				return true;
			}
		}

		return false;
	}
	
	private boolean hasAvailableRewards()
	{
		return _activeRewards.stream().anyMatch(SecretShopRewardHolder::hasAvailableReward);
	}
	
	private boolean noTopGradeRewardsRemaining()
	{
		for (SecretShopRewardHolder reward : _activeRewards)
		{
			if (reward.isTopGrade() && (reward._currentAmount > 0))
			{
				return false;
			}
		}
		
		return true;
	}
	
	public boolean isEventPeriod()
	{
		return _isEventPeriod;
	}
	
	public void addTickets(Player player)
	{
		PLAYER_REWARD_QUEUES.computeIfAbsent(player, _ -> new PlayerTicketData());
		
		final PlayerTicketData playerData = PLAYER_REWARD_QUEUES.get(player);
		final Queue<TicketTask> queue = playerData.getQueue();
		queue.add(new TicketTask(player));
		
		if (playerData.getProcessingFlag().compareAndSet(false, true))
		{
			processPlayerQueue(playerData);
		}
	}
	
	private void processPlayerQueue(PlayerTicketData playerData)
	{
		ThreadPool.execute(() ->
		{
			final Queue<TicketTask> queue = playerData.getQueue();
			while (!queue.isEmpty())
			{
				final TicketTask task = queue.poll();
				if (task != null)
				{
					if (!task.getPlayer().isInventoryUnder80(false))
					{
						task.getPlayer().sendPacket(SystemMessageId.YOUR_INVENTORY_IS_FULL);
						continue;
					}
					
					exchange(task.getPlayer());
				}
				
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					break;
				}
			}
			
			playerData.getProcessingFlag().set(false);
		});
	}
	
	private static class PlayerTicketData
	{
		private final ConcurrentLinkedQueue<TicketTask> _queue = new ConcurrentLinkedQueue<>();
		private final AtomicBoolean _processingFlag = new AtomicBoolean();
		
		public Queue<TicketTask> getQueue()
		{
			return _queue;
		}
		
		public AtomicBoolean getProcessingFlag()
		{
			return _processingFlag;
		}
	}
	
	private static class TicketTask
	{
		private final Player _player;
		
		public TicketTask(Player player)
		{
			_player = player;
		}
		
		public Player getPlayer()
		{
			return _player;
		}
	}
	
	public static class SecretShopRewardHolder
	{
		private static final int TOP_GRADE = 1;
		private static final int MIDDLE_GRADE = 2;
		private static final int LOW_GRADE = 3;
		
		private final int _id;
		private final long _count;
		private final int _grade;
		private final long _totalAmount;
		private long _currentAmount;
		private final double _chance;
		
		public SecretShopRewardHolder(int id, long count, int grade, long totalAmount, long currentAmount, double chance)
		{
			_id = id;
			_count = count;
			_grade = grade;
			_totalAmount = totalAmount;
			_currentAmount = currentAmount;
			_chance = chance;
		}
		
		public SecretShopRewardHolder(SecretShopRewardHolder reward)
		{
			_id = reward._id;
			_count = reward._count;
			_grade = reward._grade;
			_totalAmount = reward._totalAmount;
			_currentAmount = reward._currentAmount;
			_chance = reward._chance;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public long getCount()
		{
			return _count;
		}
		
		public int getGrade()
		{
			return _grade;
		}
		
		public long getTotalAmount()
		{
			return _totalAmount;
		}
		
		public long getCurrentAmount()
		{
			return _currentAmount;
		}
		
		public double getChance()
		{
			return _chance;
		}
		
		public boolean hasAvailableReward()
		{
			return _totalAmount > 0;
		}
		
		public boolean isTopGrade()
		{
			return _grade == TOP_GRADE;
		}
		
		public boolean isMiddleGrade()
		{
			return _grade == MIDDLE_GRADE;
		}
		
		public boolean isLowGrade()
		{
			return _grade == LOW_GRADE;
		}
	}
	
	public static SecretShopEventManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SecretShopEventManager INSTANCE = new SecretShopEventManager();
	}
}
