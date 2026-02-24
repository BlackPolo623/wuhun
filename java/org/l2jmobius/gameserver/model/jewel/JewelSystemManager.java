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
package org.l2jmobius.gameserver.model.jewel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;

/**
 * 寶玉系統管理器
 * @author YourName
 */
public class JewelSystemManager
{
	private static final Logger LOGGER = Logger.getLogger(JewelSystemManager.class.getName());

	// SQL 語句
	private static final String SELECT_JEWEL_DATA = "SELECT * FROM player_jewel_data WHERE char_id = ?";
	private static final String INSERT_JEWEL_DATA = "INSERT INTO player_jewel_data (char_id, is_activated, current_value_stage, current_bonus_stage) VALUES (?, ?, ?, ?)";
	private static final String UPDATE_JEWEL_DATA = "UPDATE player_jewel_data SET is_activated = ?, current_value_stage = ?, current_bonus_stage = ?, " +
		"stage1_value = ?, stage2_value = ?, stage3_value = ?, stage4_value = ?, stage5_value = ?, " +
		"stage6_value = ?, stage7_value = ?, stage8_value = ?, stage9_value = ?, stage10_value = ?, " +
		"stage11_value = ?, stage12_value = ?, stage13_value = ?, stage14_value = ?, stage15_value = ?, " +
		"stage16_value = ?, stage17_value = ?, stage18_value = ?, stage19_value = ?, stage20_value = ? " +
		"WHERE char_id = ?";
	private static final String DELETE_JEWEL_DATA = "DELETE FROM player_jewel_data WHERE char_id = ?";

	// 玩家數據緩存
	private final Map<Integer, PlayerJewelData> _playerData = new ConcurrentHashMap<>();

	protected JewelSystemManager()
	{
		JewelSystemConfig.load();
		LOGGER.info("【寶玉系統】管理器初始化完成");
	}

	/**
	 * 獲取玩家寶玉數據
	 * @param player 玩家
	 * @return 寶玉數據
	 */
	public PlayerJewelData getPlayerData(Player player)
	{
		return getPlayerData(player.getObjectId());
	}

	/**
	 * 獲取玩家寶玉數據
	 * @param charId 角色ID
	 * @return 寶玉數據
	 */
	public PlayerJewelData getPlayerData(int charId)
	{
		PlayerJewelData data = _playerData.get(charId);
		if (data == null)
		{
			data = loadFromDatabase(charId);
			if (data == null)
			{
				data = new PlayerJewelData(charId);
			}
			_playerData.put(charId, data);
		}
		return data;
	}

	/**
	 * 從資料庫載入玩家數據
	 */
	private PlayerJewelData loadFromDatabase(int charId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_JEWEL_DATA))
		{
			ps.setInt(1, charId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					final PlayerJewelData data = new PlayerJewelData(charId);
					data.setActivated(rs.getBoolean("is_activated"));
					data.setCurrentValueStage(rs.getInt("current_value_stage"));
					data.setCurrentBonusStage(rs.getInt("current_bonus_stage"));

					for (int i = 1; i <= 20; i++)
					{
						data.setStageValue(i, rs.getLong("stage" + i + "_value"));
					}

					return data;
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("【寶玉系統】載入玩家數據失敗: " + e.getMessage());
		}
		return null;
	}

	/**
	 * 保存玩家數據到資料庫
	 */
	public void savePlayerData(PlayerJewelData data)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			// 先嘗試更新
			try (PreparedStatement ps = con.prepareStatement(UPDATE_JEWEL_DATA))
			{
				ps.setBoolean(1, data.isActivated());
				ps.setInt(2, data.getCurrentValueStage());
				ps.setInt(3, data.getCurrentBonusStage());

				final long[] values = data.getAllStageValues();
				for (int i = 0; i < 20; i++)
				{
					ps.setLong(4 + i, values[i]);
				}

				ps.setInt(24, data.getCharId());

				if (ps.executeUpdate() == 0)
				{
					// 如果更新失敗，則插入新記錄
					try (PreparedStatement insertPs = con.prepareStatement(INSERT_JEWEL_DATA))
					{
						insertPs.setInt(1, data.getCharId());
						insertPs.setBoolean(2, data.isActivated());
						insertPs.setInt(3, data.getCurrentValueStage());
						insertPs.setInt(4, data.getCurrentBonusStage());
						insertPs.executeUpdate();

						// 再次更新以保存階段數值
						try (PreparedStatement updatePs = con.prepareStatement(UPDATE_JEWEL_DATA))
						{
							updatePs.setBoolean(1, data.isActivated());
							updatePs.setInt(2, data.getCurrentValueStage());
							updatePs.setInt(3, data.getCurrentBonusStage());

							for (int i = 0; i < 20; i++)
							{
								updatePs.setLong(4 + i, values[i]);
							}

							updatePs.setInt(24, data.getCharId());
							updatePs.executeUpdate();
						}
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("【寶玉系統】保存玩家數據失敗: " + e.getMessage());
		}
	}

	/**
	 * 啟用寶玉系統
	 * @param player 玩家
	 * @return 是否成功
	 */
	public boolean activateSystem(Player player)
	{
		final PlayerJewelData data = getPlayerData(player);
		if (data.isActivated())
		{
			player.sendMessage("您已經啟用過寶玉系統了。");
			return false;
		}

		// 檢查並消耗道具
		if (player.getInventory().getInventoryItemCount(JewelSystemConfig.ACTIVATION_ITEM_ID, -1) < 1)
		{
			player.sendMessage("您沒有足夠的啟用道具。");
			return false;
		}

		player.destroyItemByItemId(ItemProcessType.JEWEL, JewelSystemConfig.ACTIVATION_ITEM_ID, 1, player, true);
		data.setActivated(true);
		savePlayerData(data);

		// 更新玩家屬性
		player.updateJewelBonusCache();
		player.getStat().recalculateStats(true);
		player.broadcastUserInfo();

		player.sendMessage("寶玉系統啟用成功！");
		return true;
	}

	/**
	 * 揭露數值
	 * @param player 玩家
	 * @return 揭露的數值，-1表示失敗
	 */
	public long revealValue(Player player)
	{
		final PlayerJewelData data = getPlayerData(player);

		if (!data.isActivated())
		{
			player.sendMessage("請先啟用寶玉系統。");
			return -1;
		}

		final int nextStage = data.getCurrentValueStage() + 1;
		if (nextStage > 20)
		{
			player.sendMessage("已達到最高階段。");
			return -1;
		}

		// 獲取消耗數量
		final long cost = JewelSystemConfig.STAGE_VALUES[nextStage - 1][2];
		if (player.getInventory().getInventoryItemCount(JewelSystemConfig.REVEAL_VALUE_ITEM_ID, -1) < cost)
		{
			player.sendMessage("您沒有足夠的揭露道具。需要 " + cost + " 個。");
			return -1;
		}

		// 消耗道具
		player.destroyItemByItemId(ItemProcessType.JEWEL, JewelSystemConfig.REVEAL_VALUE_ITEM_ID, cost, player, true);

		// 隨機生成數值
		final long minValue = JewelSystemConfig.STAGE_VALUES[nextStage - 1][0];
		final long maxValue = JewelSystemConfig.STAGE_VALUES[nextStage - 1][1];
		final long revealedValue = Rnd.get(minValue, maxValue);

		// 更新數據
		data.setStageValue(nextStage, revealedValue);
		data.setCurrentValueStage(nextStage);
		savePlayerData(data);

		// 更新玩家屬性
		player.updateJewelBonusCache();
		player.getStat().recalculateStats(true);
		player.broadcastUserInfo();

		player.sendMessage("第 " + nextStage + " 階段揭露數值: " + revealedValue);
		return revealedValue;
	}

	/**
	 * 加成突破
	 * @param player 玩家
	 * @return 結果: 1=成功, 0=失敗, -1=倒退, -2=錯誤
	 */
	public int breakthrough(Player player)
	{
		final PlayerJewelData data = getPlayerData(player);

		if (!data.isActivated())
		{
			player.sendMessage("請先啟用寶玉系統。");
			return -2;
		}

		final int currentBonusStage = data.getCurrentBonusStage();
		if (currentBonusStage >= 20)
		{
			player.sendMessage("加成已達到最高階段。");
			return -2;
		}

		// 獲取當前在5階段循環中的位置
		final int cyclePosition = currentBonusStage % 5;
		final long cost = JewelSystemConfig.BREAKTHROUGH_COSTS[cyclePosition];

		if (player.getInventory().getInventoryItemCount(JewelSystemConfig.BREAKTHROUGH_ITEM_ID, -1) < cost)
		{
			player.sendMessage("您沒有足夠的突破道具。需要 " + cost + " 個。");
			return -2;
		}

		// 消耗道具
		player.destroyItemByItemId(ItemProcessType.JEWEL, JewelSystemConfig.BREAKTHROUGH_ITEM_ID, cost, player, true);

		// 計算結果
		final int[] rates = JewelSystemConfig.BREAKTHROUGH_RATES[cyclePosition];
		final int roll = Rnd.get(100);

		int result;
		if (roll < rates[0])
		{
			// 成功
			data.setCurrentBonusStage(currentBonusStage + 1);
			result = 1;
			player.sendMessage("突破成功！加成階段提升至 " + (currentBonusStage + 1));
		}
		else if (roll < (rates[0] + rates[1]))
		{
			// 失敗
			result = 0;
			player.sendMessage("突破失敗，階段維持不變。");
		}
		else
		{
			// 倒退
			if (currentBonusStage > 0)
			{
				// 只能倒退到當前5階段區間的起始
				final int tierStart = (currentBonusStage / 5) * 5;
				final int newStage = Math.max(tierStart, currentBonusStage - 1);
				data.setCurrentBonusStage(newStage);
				player.sendMessage("突破失敗，階段倒退至 " + newStage);
			}
			else
			{
				player.sendMessage("突破失敗，階段維持不變。");
			}
			result = -1;
		}

		savePlayerData(data);

		// 更新玩家屬性
		player.updateJewelBonusCache();
		player.getStat().recalculateStats(true);
		player.broadcastUserInfo();

		return result;
	}

	/**
	 * 初始化所有數值 (從頭到當前階段全部清除)
	 * @param player 玩家
	 * @return 是否成功
	 */
	public boolean resetCurrentTier(Player player)
	{
		final PlayerJewelData data = getPlayerData(player);

		if (!data.isActivated())
		{
			player.sendMessage("請先啟用寶玉系統。");
			return false;
		}

		if (!data.canReset())
		{
			player.sendMessage("只有在第5、10、15、20階段才能初始化。");
			return false;
		}

		data.resetAllValues();
		savePlayerData(data);

		// 更新玩家屬性
		player.updateJewelBonusCache();
		player.getStat().recalculateStats(true);
		player.broadcastUserInfo();

		player.sendMessage("所有數值已初始化。");
		return true;
	}

	/**
	 * 獲取玩家總攻擊加成
	 * @param player 玩家
	 * @return 總加成值
	 */
	public long getTotalBonus(Player player)
	{
		final PlayerJewelData data = _playerData.get(player.getObjectId());
		if (data == null)
		{
			return 0;
		}
		return data.calculateTotalBonus();
	}

	/**
	 * 玩家登出時保存數據
	 */
	public void onPlayerLogout(Player player)
	{
		final PlayerJewelData data = _playerData.get(player.getObjectId());
		if (data != null)
		{
			savePlayerData(data);
		}
	}

	/**
	 * 從緩存中移除玩家數據
	 */
	public void removePlayerData(int charId)
	{
		_playerData.remove(charId);
	}

	public static JewelSystemManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final JewelSystemManager INSTANCE = new JewelSystemManager();
	}
}
