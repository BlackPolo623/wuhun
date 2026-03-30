/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.data.xml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.Rnd;

/**
 * 多層副本獎勵數據管理器
 * @author Claude
 */
public class MultiLayerRewardData
{
	private static final Logger LOGGER = Logger.getLogger(MultiLayerRewardData.class.getName());

	private final Map<Integer, List<RewardConfig>> _rewards = new HashMap<>();

	protected MultiLayerRewardData()
	{
		load();
	}

	public void load()
	{
		_rewards.clear();

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM multilayer_dungeon_rewards WHERE enabled = 1 ORDER BY layer, id"))
		{
			try (ResultSet rs = ps.executeQuery())
			{
				int count = 0;
				while (rs.next())
				{
					final int layer = rs.getInt("layer");
					final int itemId = rs.getInt("item_id");
					final int minCount = rs.getInt("min_count");
					final int maxCount = rs.getInt("max_count");
					final double chance = rs.getDouble("chance");

					final RewardConfig config = new RewardConfig(itemId, minCount, maxCount, chance);
					_rewards.computeIfAbsent(layer, k -> new ArrayList<>()).add(config);
					count++;
				}

				LOGGER.info("多層副本獎勵數據: 已加載 " + _rewards.size() + " 層共 " + count + " 個獎勵配置");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("多層副本獎勵數據: 加載失敗 - " + e.getMessage());
		}
	}

	/**
	 * 獲取指定層數的所有獎勵配置
	 * @param layer 層數
	 * @return 獎勵配置列表
	 */
	public List<RewardConfig> getRewards(int layer)
	{
		return _rewards.getOrDefault(layer, new ArrayList<>());
	}

	/**
	 * 獎勵配置類
	 */
	public static class RewardConfig
	{
		private final int _itemId;
		private final int _minCount;
		private final int _maxCount;
		private final double _chance;

		public RewardConfig(int itemId, int minCount, int maxCount, double chance)
		{
			_itemId = itemId;
			_minCount = minCount;
			_maxCount = maxCount;
			_chance = chance;
		}

		public int getItemId()
		{
			return _itemId;
		}

		public int getRandomCount()
		{
			if (_minCount == _maxCount)
			{
				return _minCount;
			}
			return Rnd.get(_minCount, _maxCount);
		}

		public double getChance()
		{
			return _chance;
		}
	}

	public static MultiLayerRewardData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final MultiLayerRewardData INSTANCE = new MultiLayerRewardData();
	}
}
