// ScratchCardData.java (修正版)
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;

public class ScratchCardData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ScratchCardData.class.getName());

	// 機率基數 (千分比)
	private static final int PROBABILITY_BASE = 1000;

	// 配置數據
	private int priceItemId;
	private long priceCount;
	private int dailyLimit;
	private int gridSize;

	// 機率配置 (千分比)
	private int emptyChance;
	private int accumulateChance;
	private int jackpotChance;

	// 累積獎勵 <需要數量, 獎勵列表>
	private final Map<Integer, List<RewardItem>> accumulateRewards = new HashMap<>();

	// 神秘大獎池
	private final List<JackpotReward> jackpotRewards = new ArrayList<>();
	private int totalJackpotWeight = 0;

	protected ScratchCardData()
	{
		load();
	}

	@Override
	public void load()
	{
		accumulateRewards.clear();
		jackpotRewards.clear();
		totalJackpotWeight = 0;

		parseDatapackFile("data/ScratchCardConfig.xml");

		// 驗證機率總和
		int totalChance = emptyChance + accumulateChance + jackpotChance;
		if (totalChance != PROBABILITY_BASE)
		{
			LOGGER.warning("機率總和不等於" + PROBABILITY_BASE + "! 當前總和: " + totalChance);
		}

		LOGGER.info(getClass().getSimpleName() + ": 載入完成");
		LOGGER.info("- 格子數: " + gridSize);
		LOGGER.info("- 空白機率: " + emptyChance + "‰ (" + (emptyChance / 10.0) + "%)");
		LOGGER.info("- 累積機率: " + accumulateChance + "‰ (" + (accumulateChance / 10.0) + "%)");
		LOGGER.info("- 大獎機率: " + jackpotChance + "‰ (" + (jackpotChance / 10.0) + "%)");
		LOGGER.info("- 累積獎勵層級: " + accumulateRewards.size());
		LOGGER.info("- 神秘大獎數: " + jackpotRewards.size());
	}

	@Override
	public void parseDocument(Document doc, File f)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					switch (d.getNodeName())
					{
						case "config":
						{
							parseConfig(d);
							break;
						}
						case "probabilities":
						{
							parseProbabilities(d);
							break;
						}
						case "accumulateRewards":
						{
							parseAccumulateRewards(d);
							break;
						}
						case "jackpotRewards":
						{
							parseJackpotRewards(d);
							break;
						}
					}
				}
			}
		}
	}

	private void parseConfig(Node node)
	{
		for (Node d = node.getFirstChild(); d != null; d = d.getNextSibling())
		{
			switch (d.getNodeName())
			{
				case "price":
				{
					NamedNodeMap attrs = d.getAttributes();
					priceItemId = parseInteger(attrs, "itemId");
					priceCount = parseLong(attrs, "count");
					break;
				}
				case "dailyLimit":
				{
					dailyLimit = Integer.parseInt(d.getTextContent());
					break;
				}
				case "gridSize":
				{
					gridSize = Integer.parseInt(d.getTextContent());
					break;
				}
			}
		}
	}

	private void parseProbabilities(Node node)
	{
		for (Node d = node.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if ("symbol".equalsIgnoreCase(d.getNodeName()))
			{
				NamedNodeMap attrs = d.getAttributes();
				String type = parseString(attrs, "type");
				int chance = parseInteger(attrs, "chance");

				switch (type)
				{
					case "empty":
						emptyChance = chance;
						break;
					case "accumulate":
						accumulateChance = chance;
						break;
					case "jackpot":
						jackpotChance = chance;
						break;
				}
			}
		}
	}

	private void parseAccumulateRewards(Node node)
	{
		for (Node d = node.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if ("reward".equalsIgnoreCase(d.getNodeName()))
			{
				NamedNodeMap attrs = d.getAttributes();
				int count = parseInteger(attrs, "count");

				List<RewardItem> items = new ArrayList<>();
				for (Node item = d.getFirstChild(); item != null; item = item.getNextSibling())
				{
					if ("item".equalsIgnoreCase(item.getNodeName()))
					{
						NamedNodeMap itemAttrs = item.getAttributes();
						int id = parseInteger(itemAttrs, "id");
						long amount = parseLong(itemAttrs, "amount");
						items.add(new RewardItem(id, amount));
					}
				}

				accumulateRewards.put(count, items);  // ← 這裡
			}
		}
	}

	private void parseJackpotRewards(Node node)
	{
		for (Node d = node.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if ("reward".equalsIgnoreCase(d.getNodeName()))
			{
				NamedNodeMap attrs = d.getAttributes();
				int weight = parseInteger(attrs, "weight");  // ← 這裡用 weight

				List<RewardItem> items = new ArrayList<>();
				for (Node item = d.getFirstChild(); item != null; item = item.getNextSibling())
				{
					if ("item".equalsIgnoreCase(item.getNodeName()))
					{
						NamedNodeMap itemAttrs = item.getAttributes();
						int id = parseInteger(itemAttrs, "id");
						long amount = parseLong(itemAttrs, "amount");
						items.add(new RewardItem(id, amount));
					}
				}

				jackpotRewards.add(new JackpotReward(weight, items));  // ← 這裡
				totalJackpotWeight += weight;
			}
		}
	}

	// ==================== 遊戲邏輯方法 ====================

	/**
	 * 生成隨機符號 (0=空, 1=累積, 2=大獎)
	 * 使用千分比機率
	 */
	public int generateRandomSymbol()
	{
		int roll = Rnd.get(PROBABILITY_BASE); // 0-999

		if (roll < emptyChance)
		{
			return 0; // 空白
		}
		else if (roll < emptyChance + accumulateChance)
		{
			return 1; // 累積
		}
		else
		{
			return 2; // 大獎
		}
	}

	/**
	 * 從大獎池抽取獎勵
	 */
	public JackpotReward getRandomJackpot()
	{
		int roll = Rnd.get(totalJackpotWeight);
		int currentWeight = 0;

		for (JackpotReward reward : jackpotRewards)
		{
			currentWeight += reward.getWeight();
			if (roll < currentWeight)
			{
				return reward;
			}
		}

		// 備用(理論上不會到這)
		return jackpotRewards.get(0);
	}

	/**
	 * 獲取對應累積數量的獎勵
	 * 返回最高的符合條件獎勵
	 */
	public List<RewardItem> getAccumulateReward(int count)
	{
		List<RewardItem> bestReward = null;
		int bestCount = 0;

		for (Map.Entry<Integer, List<RewardItem>> entry : accumulateRewards.entrySet())
		{
			int requiredCount = entry.getKey();
			if (count >= requiredCount && requiredCount > bestCount)
			{
				bestCount = requiredCount;
				bestReward = entry.getValue();
			}
		}

		return bestReward;
	}

	// ==================== Getters ====================

	public int getPriceItemId() { return priceItemId; }
	public long getPriceCount() { return priceCount; }
	public int getDailyLimit() { return dailyLimit; }
	public int getGridSize() { return gridSize; }
	public int getEmptyChance() { return emptyChance; }
	public int getAccumulateChance() { return accumulateChance; }
	public int getJackpotChance() { return jackpotChance; }
	public Map<Integer, List<RewardItem>> getAccumulateRewards() { return accumulateRewards; }
	public List<JackpotReward> getJackpotRewards() { return jackpotRewards; }
	public int getTotalJackpotWeight() { return totalJackpotWeight; }

	// ==================== 內部類 ====================

	public static class RewardItem
	{
		private final int itemId;
		private final long amount;

		public RewardItem(int itemId, long amount)
		{
			this.itemId = itemId;
			this.amount = amount;
		}

		public int getItemId() { return itemId; }
		public long getAmount() { return amount; }
	}

	public static class JackpotReward
	{
		private final int weight;
		private final List<RewardItem> items;

		public JackpotReward(int weight, List<RewardItem> items)
		{
			this.weight = weight;
			this.items = items;
		}

		public int getWeight() { return weight; }
		public List<RewardItem> getItems() { return items; }
	}

	// ==================== 單例模式 ====================

	public static ScratchCardData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final ScratchCardData INSTANCE = new ScratchCardData();
	}
}