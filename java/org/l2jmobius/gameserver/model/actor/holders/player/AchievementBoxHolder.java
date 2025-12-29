package org.l2jmobius.gameserver.model.actor.holders.player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.config.AchievementBoxConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.AchievementBoxStateType;
import org.l2jmobius.gameserver.model.actor.enums.player.AchievementBoxType;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.serverpackets.achievementbox.ExSteadyAllBoxUpdate;
import org.l2jmobius.gameserver.network.serverpackets.achievementbox.ExSteadyBoxReward;

public class AchievementBoxHolder
{
	private static final Logger LOGGER = Logger.getLogger(AchievementBoxHolder.class.getName());

	// ==================== 時間配置 ====================
	private static final int ACHIEVEMENT_BOX_2H = 7200000;   // 2小時（毫秒）
	private static final int ACHIEVEMENT_BOX_6H = 21600000;  // 6小時（毫秒）
	private static final int ACHIEVEMENT_BOX_12H = 43200000; // 12小時（毫秒）

	// ==================== 寶箱類型機率配置 ====================
	// 獲得寶箱時的類型機率（百分比）
	private static final int BOX_12H_CHANCE = 12;  // 12小時寶箱：12%
	private static final int BOX_6H_CHANCE = 40;   // 6小時寶箱：28% (40-12)
	// 2小時寶箱：68% (100-40)

	// ==================== 槽位解鎖費用配置 ====================
	private static final long SLOT_2_ADENA_COST = 100000000;  // 第2槽：1億金幣
	private static final long SLOT_3_LCOIN_COST = 2000;       // 第3槽：2000 L-Coin
	private static final long SLOT_4_LCOIN_COST = 8000;       // 第4槽：8000 L-Coin

	// ==================== 寶箱獎勵配置類 ====================
	private static class BoxRewardConfig
	{
		public final int itemId;      // 物品ID
		public final int count;       // 數量
		public final int chanceWeight; // 機率權重（用於累加計算）

		public BoxRewardConfig(int itemId, int count, int chanceWeight)
		{
			this.itemId = itemId;
			this.count = count;
			this.chanceWeight = chanceWeight;
		}
	}

	// ==================== 2小時寶箱獎勵配置 ====================
	// 格式：{物品ID, 數量, 機率權重}
	// 機率計算：累加權重，隨機數落在哪個範圍就給哪個獎勵
	private static final BoxRewardConfig[] BOX_2H_REWARDS = {
			new BoxRewardConfig(103070, 1, 3),
			new BoxRewardConfig(93274, 5, 30),     // 餅乾 x5：27% (30-3)
			new BoxRewardConfig(90907, 250, 70),   // 彈藥券 x250：40% (70-30)
			new BoxRewardConfig(3031, 50, 100)     // 精靈礦石 x50：30% (100-70)
	};

	// ==================== 6小時寶箱獎勵配置 ====================
	private static final BoxRewardConfig[] BOX_6H_REWARDS = {
			new BoxRewardConfig(103070, 1, 10),
			new BoxRewardConfig(93274, 10, 30),    // 餅乾 x10：20% (30-10)
			new BoxRewardConfig(90907, 500, 70),   // 彈藥券 x500：40% (70-30)
			new BoxRewardConfig(3031, 100, 100)    // 精靈礦石 x100：30% (100-70)
	};

	// ==================== 12小時寶箱獎勵配置 ====================
	private static final BoxRewardConfig[] BOX_12H_REWARDS = {
			new BoxRewardConfig(103070, 1, 20),
			new BoxRewardConfig(93274, 20, 30),    // 餅乾 x20：10% (30-20)
			new BoxRewardConfig(90907, 1000, 70),  // 彈藥券 x1000：40% (70-30)
			new BoxRewardConfig(3031, 200, 100)    // 精靈礦石 x200：30% (100-70)
	};

	// ==================== 染料ID範圍配置 ====================
	private static final int DYE_MIN_ID = 72084;  // 染料最小ID
	private static final int DYE_MAX_ID = 72102;  // 染料最大ID

	// ==================== 成員變量 ====================
	private final Player _owner;
	private int _boxOwned = 1;
	private int _monsterPoints = 0;
	private int _pvpPoints = 0;
	private int _pendingBoxSlotId = 0;
	private int _pvpEndDate;
	private long _boxTimeForOpen;
	private final List<AchievementBoxInfoHolder> _achievementBox = new ArrayList<>();
	private ScheduledFuture<?> _boxOpenTask;

	public AchievementBoxHolder(Player owner)
	{
		_owner = owner;
	}

	public int pvpEndDate()
	{
		return _pvpEndDate;
	}

	public void addPoints(int value)
	{
		final int newPoints = Math.min(AchievementBoxConfig.ACHIEVEMENT_BOX_POINTS_FOR_REWARD, _monsterPoints + value);
		if (newPoints >= AchievementBoxConfig.ACHIEVEMENT_BOX_POINTS_FOR_REWARD)
		{
			if (addNewBox())
			{
				_monsterPoints = 0;
			}
			else
			{
				_monsterPoints = AchievementBoxConfig.ACHIEVEMENT_BOX_POINTS_FOR_REWARD;
			}
			return;
		}

		_monsterPoints += value;
	}

	public void addPvpPoints(int value)
	{
		final int newPoints = Math.min(AchievementBoxConfig.ACHIEVEMENT_BOX_PVP_POINTS_FOR_REWARD, _pvpPoints);
		while (newPoints >= AchievementBoxConfig.ACHIEVEMENT_BOX_PVP_POINTS_FOR_REWARD)
		{
			if (addNewBox())
			{
				_pvpPoints = 0;
			}
			else
			{
				_pvpPoints = AchievementBoxConfig.ACHIEVEMENT_BOX_PVP_POINTS_FOR_REWARD;
			}
			return;
		}

		_pvpPoints += value;
	}

	public void restore()
	{
		tryFinishBox();
		refreshPvpEndDate();
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement("SELECT * FROM achievement_box WHERE charId=?"))
		{
			ps.setInt(1, _owner.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					try
					{
						_boxOwned = rs.getInt("box_owned");
						_monsterPoints = rs.getInt("monster_point");
						_pvpPoints = rs.getInt("pvp_point");
						_pendingBoxSlotId = rs.getInt("pending_box");
						_boxTimeForOpen = rs.getLong("open_time");
						for (int i = 1; i <= 4; i++)
						{
							int state = rs.getInt("box_state_slot_" + i);
							int type = rs.getInt("boxtype_slot_" + i);
							if ((i == 1) && (state == 0))
							{
								state = 1;
							}

							final AchievementBoxInfoHolder holder = new AchievementBoxInfoHolder(i, state, type);
							_achievementBox.add(i - 1, holder);
						}
					}
					catch (Exception e)
					{
						LOGGER.warning("Could not restore Achievement box for " + _owner);
					}
				}
				else
				{
					storeNew();
					_achievementBox.add(0, new AchievementBoxInfoHolder(1, 1, 0));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore achievement box for " + _owner, e);
		}
	}

	public void storeNew()
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement("INSERT INTO achievement_box VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)"))
		{
			ps.setInt(1, _owner.getObjectId());
			ps.setInt(2, _boxOwned);
			ps.setInt(3, _monsterPoints);
			ps.setInt(4, _pvpPoints);
			ps.setInt(5, _pendingBoxSlotId);
			ps.setLong(6, _boxTimeForOpen);
			for (int i = 0; i < 4; i++)
			{
				ps.setInt(7 + (i * 2), 0);
				ps.setInt(8 + (i * 2), 0);
			}

			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.warning("Could not store new Archivement Box for: " + _owner);
			LOGGER.warning(TraceUtil.getStackTrace(e));
		}
	}

	public void store()
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement("UPDATE achievement_box SET box_owned=?,monster_point=?,pvp_point=?,pending_box=?,open_time=?,box_state_slot_1=?,boxtype_slot_1=?,box_state_slot_2=?,boxtype_slot_2=?,box_state_slot_3=?,boxtype_slot_3=?,box_state_slot_4=?,boxtype_slot_4=? WHERE charId=?"))
		{
			ps.setInt(1, getBoxOwned());
			ps.setInt(2, getMonsterPoints());
			ps.setInt(3, getPvpPoints());
			ps.setInt(4, getPendingBoxSlotId());
			ps.setLong(5, getBoxOpenTime());
			for (int i = 0; i < 4; i++)
			{
				if (_achievementBox.size() >= (i + 1))
				{
					AchievementBoxInfoHolder holder = _achievementBox.get(i);
					ps.setInt(6 + (i * 2), holder == null ? 0 : holder.getState().ordinal());
					ps.setInt(7 + (i * 2), holder == null ? 0 : holder.getType().ordinal());
				}
				else
				{
					ps.setInt(6 + (i * 2), 0);
					ps.setInt(7 + (i * 2), 0);
				}
			}

			ps.setInt(14, _owner.getObjectId());
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Could not store Achievement Box for: " + _owner, e);
		}
	}

	public List<AchievementBoxInfoHolder> getAchievementBox()
	{
		return _achievementBox;
	}

	public boolean addNewBox()
	{
		AchievementBoxInfoHolder free = null;
		int id = -1;
		for (int i = 1; i <= getBoxOwned(); i++)
		{
			final AchievementBoxInfoHolder holder = getAchievementBox().get(i - 1);
			if (holder.getState() == AchievementBoxStateType.AVAILABLE)
			{
				free = holder;
				id = i;
				break;
			}
		}

		if (free != null)
		{
			// 使用配置的機率決定寶箱類型
			int rnd = Rnd.get(0, 100);
			free.setType(rnd < BOX_12H_CHANCE ? AchievementBoxType.BOX_12H :
					rnd < BOX_6H_CHANCE ? AchievementBoxType.BOX_6H :
							AchievementBoxType.BOX_2H);

			switch (free.getType())
			{
				case BOX_2H:
				case BOX_6H:
				case BOX_12H:
				{
					free.setState(AchievementBoxStateType.OPEN);
					getAchievementBox().remove(id - 1);
					getAchievementBox().add(id - 1, free);
					sendBoxUpdate();
					break;
				}
			}

			return true;
		}

		return false;
	}

	public void openBox(int slotId)
	{
		if (slotId > getBoxOwned())
		{
			return;
		}

		final AchievementBoxInfoHolder holder = getAchievementBox().get(slotId - 1);
		if ((holder == null) || (_boxTimeForOpen != 0))
		{
			return;
		}

		_pendingBoxSlotId = slotId;

		switch (holder.getType())
		{
			case BOX_2H:
			{
				setBoxTimeForOpen(ACHIEVEMENT_BOX_2H);
				holder.setState(AchievementBoxStateType.UNLOCK_IN_PROGRESS);
				getAchievementBox().remove(slotId - 1);
				getAchievementBox().add(slotId - 1, holder);
				sendBoxUpdate();
				break;
			}
			case BOX_6H:
			{
				setBoxTimeForOpen(ACHIEVEMENT_BOX_6H);
				holder.setState(AchievementBoxStateType.UNLOCK_IN_PROGRESS);
				getAchievementBox().remove(slotId - 1);
				getAchievementBox().add(slotId - 1, holder);
				sendBoxUpdate();
				break;
			}
			case BOX_12H:
			{
				setBoxTimeForOpen(ACHIEVEMENT_BOX_12H);
				holder.setState(AchievementBoxStateType.UNLOCK_IN_PROGRESS);
				getAchievementBox().remove(slotId - 1);
				getAchievementBox().add(slotId - 1, holder);
				sendBoxUpdate();
				break;
			}
		}
	}

	public void skipBoxOpenTime(int slotId, long fee)
	{
		if (slotId > getBoxOwned())
		{
			return;
		}

		final AchievementBoxInfoHolder holder = getAchievementBox().get(slotId - 1);
		if ((holder != null) && _owner.destroyItemByItemId(ItemProcessType.FEE, Inventory.LCOIN_ID, fee, _owner, true))
		{
			if (_pendingBoxSlotId == slotId)
			{
				cancelTask();
			}

			finishAndUnlockChest(slotId);
		}
	}

	public boolean setBoxTimeForOpen(long time)
	{
		if ((_boxOpenTask != null) && !(_boxOpenTask.isDone() || _boxOpenTask.isCancelled()))
		{
			return false;
		}

		_boxTimeForOpen = System.currentTimeMillis() + time;
		return true;
	}

	public void tryFinishBox()
	{
		if ((_boxTimeForOpen == 0) || (_boxTimeForOpen >= System.currentTimeMillis()))
		{
			return;
		}

		if ((_owner == null) || !_owner.isOnline())
		{
			return;
		}

		final AchievementBoxInfoHolder holder = getAchievementBox().get(_pendingBoxSlotId - 1);
		if (holder != null)
		{
			finishAndUnlockChest(_pendingBoxSlotId);
		}
	}

	public int getBoxOwned()
	{
		return _boxOwned;
	}

	public int getMonsterPoints()
	{
		return _monsterPoints;
	}

	public int getPvpPoints()
	{
		return _pvpPoints;
	}

	public int getPendingBoxSlotId()
	{
		return _pendingBoxSlotId;
	}

	public long getBoxOpenTime()
	{
		return _boxTimeForOpen;
	}

	public void finishAndUnlockChest(int id)
	{
		if (id > getBoxOwned())
		{
			return;
		}

		if (_pendingBoxSlotId == id)
		{
			_boxTimeForOpen = 0;
			_pendingBoxSlotId = 0;
		}

		getAchievementBox().get(id - 1).setState(AchievementBoxStateType.RECEIVE_REWARD);
		sendBoxUpdate();
	}

	public void sendBoxUpdate()
	{
		_owner.sendPacket(new ExSteadyAllBoxUpdate(_owner));
	}

	public void cancelTask()
	{
		if (_boxOpenTask == null)
		{
			return;
		}

		_boxOpenTask.cancel(false);
		_boxOpenTask = null;
	}

	public void unlockSlot(int slotId)
	{
		if (((slotId - 1) != getBoxOwned()) || (slotId > 4))
		{
			return;
		}

		boolean paidSlot = false;
		switch (slotId)
		{
			case 2:
			{
				if (_owner.reduceAdena(ItemProcessType.FEE, SLOT_2_ADENA_COST, _owner, true))
				{
					paidSlot = true;
				}
				break;
			}
			case 3:
			{
				if (_owner.destroyItemByItemId(ItemProcessType.FEE, Inventory.LCOIN_ID, SLOT_3_LCOIN_COST, _owner, true))
				{
					paidSlot = true;
				}
				break;
			}
			case 4:
			{
				if (_owner.destroyItemByItemId(ItemProcessType.FEE, Inventory.LCOIN_ID, SLOT_4_LCOIN_COST, _owner, true))
				{
					paidSlot = true;
				}
				break;
			}
		}

		if (paidSlot)
		{
			_boxOwned = slotId;
			final AchievementBoxInfoHolder holder = new AchievementBoxInfoHolder(slotId, 1, 0);
			holder.setState(AchievementBoxStateType.AVAILABLE);
			holder.setType(AchievementBoxType.LOCKED);
			getAchievementBox().add(slotId - 1, holder);
			sendBoxUpdate();
		}
	}

	/**
	 * 從獎勵配置中隨機選擇一個獎勵
	 */
	private ItemHolder selectRandomReward(BoxRewardConfig[] rewards)
	{
		if (rewards == null || rewards.length == 0)
		{
			return null;
		}

		final int rnd = Rnd.get(100);

		for (BoxRewardConfig reward : rewards)
		{
			if (rnd < reward.chanceWeight)
			{
				// 如果是染料範圍，隨機選擇一個染料
				if (reward.itemId == DYE_MIN_ID)
				{
					int dyeId = Rnd.get(DYE_MIN_ID, DYE_MAX_ID);
					return new ItemHolder(dyeId, reward.count);
				}

				return new ItemHolder(reward.itemId, reward.count);
			}
		}

		// 如果沒有命中任何獎勵（理論上不應該發生），返回最後一個
		BoxRewardConfig lastReward = rewards[rewards.length - 1];
		return new ItemHolder(lastReward.itemId, lastReward.count);
	}

	public void getReward(int slotId)
	{
		final AchievementBoxInfoHolder holder = getAchievementBox().get(slotId - 1);
		if (holder.getState() != AchievementBoxStateType.RECEIVE_REWARD)
		{
			return;
		}

		// 根據寶箱類型選擇對應的獎勵配置
		ItemHolder reward = null;
		switch (holder.getType())
		{
			case BOX_2H:
			{
				reward = selectRandomReward(BOX_2H_REWARDS);
				break;
			}
			case BOX_6H:
			{
				reward = selectRandomReward(BOX_6H_REWARDS);
				break;
			}
			case BOX_12H:
			{
				reward = selectRandomReward(BOX_12H_REWARDS);
				break;
			}
		}

		holder.setState(AchievementBoxStateType.AVAILABLE);
		holder.setType(AchievementBoxType.LOCKED);
		sendBoxUpdate();

		if (reward != null)
		{
			_owner.addItem(ItemProcessType.REWARD, reward, _owner, true);
			_owner.sendPacket(new ExSteadyBoxReward(slotId, reward.getId(), reward.getCount()));
		}
	}

	public void refreshPvpEndDate()
	{
		final long currentTime = System.currentTimeMillis();
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(currentTime);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 6);
		if (calendar.getTimeInMillis() < currentTime)
		{
			calendar.add(Calendar.MONTH, 1);
		}

		_pvpEndDate = (int) (calendar.getTimeInMillis() / 1000);
	}
}