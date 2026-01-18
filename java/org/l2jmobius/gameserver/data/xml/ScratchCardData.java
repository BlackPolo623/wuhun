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
	private static final int PROBABILITY_BASE = 1000;

	// 所有刮刮樂配置 <cardId, config>
	private final Map<Integer, ScratchCardConfig> configs = new HashMap<>();

	protected ScratchCardData()
	{
		load();
	}

	@Override
	public void load()
	{
		configs.clear();
		parseDatapackFile("data/ScratchCardConfig.xml");

		LOGGER.info(getClass().getSimpleName() + ": 載入了 " + configs.size() + " 種刮刮樂");
		for (ScratchCardConfig config : configs.values())
		{
			LOGGER.info("- [" + config.getId() + "] " + config.getName() +
					" (格子:" + config.getGridSize() + ", 價格:" + config.getPriceCount() + ")");
		}
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
					if ("scratchCard".equalsIgnoreCase(d.getNodeName()))
					{
						parseScratchCard(d);
					}
				}
			}
		}
	}

	private void parseScratchCard(Node node)
	{
		NamedNodeMap attrs = node.getAttributes();
		int id = parseInteger(attrs, "id");
		String name = parseString(attrs, "name");
		boolean enabled = parseBoolean(attrs, "enabled", true);

		if (!enabled)
		{
			return; // 跳過未啟用的
		}

		ScratchCardConfig config = new ScratchCardConfig(id, name);

		for (Node d = node.getFirstChild(); d != null; d = d.getNextSibling())
		{
			switch (d.getNodeName())
			{
				case "config":
					parseConfig(d, config);
					break;
				case "probabilities":
					parseProbabilities(d, config);
					break;
				case "accumulateRewards":
					parseAccumulateRewards(d, config);
					break;
				case "jackpotRewards":
					parseJackpotRewards(d, config);
					break;
			}
		}

		// 驗證機率
		int totalChance = config.getEmptyChance() + config.getAccumulateChance() + config.getJackpotChance();
		if (totalChance != PROBABILITY_BASE)
		{
			LOGGER.warning("刮刮樂 [" + id + "] " + name + " 機率總和不等於1000! 當前: " + totalChance);
		}

		configs.put(id, config);
	}

	private void parseConfig(Node node, ScratchCardConfig config)
	{
		for (Node d = node.getFirstChild(); d != null; d = d.getNextSibling())
		{
			switch (d.getNodeName())
			{
				case "price":
					NamedNodeMap attrs = d.getAttributes();
					config.setPriceItemId(parseInteger(attrs, "itemId"));
					config.setPriceCount(parseLong(attrs, "count"));
					break;
				case "dailyLimit":
					config.setDailyLimit(Integer.parseInt(d.getTextContent()));
					break;
				case "gridSize":
					config.setGridSize(Integer.parseInt(d.getTextContent()));
					break;
			}
		}
	}

	private void parseProbabilities(Node node, ScratchCardConfig config)
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
						config.setEmptyChance(chance);
						break;
					case "accumulate":
						config.setAccumulateChance(chance);
						break;
					case "jackpot":
						config.setJackpotChance(chance);
						break;
				}
			}
		}
	}

	private void parseAccumulateRewards(Node node, ScratchCardConfig config)
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
						items.add(new RewardItem(
								parseInteger(itemAttrs, "id"),
								parseLong(itemAttrs, "amount")
						));
					}
				}
				config.addAccumulateReward(count, items);
			}
		}
	}

	private void parseJackpotRewards(Node node, ScratchCardConfig config)
	{
		for (Node d = node.getFirstChild(); d != null; d = d.getNextSibling())
		{
			if ("reward".equalsIgnoreCase(d.getNodeName()))
			{
				NamedNodeMap attrs = d.getAttributes();
				int weight = parseInteger(attrs, "weight");

				List<RewardItem> items = new ArrayList<>();
				for (Node item = d.getFirstChild(); item != null; item = item.getNextSibling())
				{
					if ("item".equalsIgnoreCase(item.getNodeName()))
					{
						NamedNodeMap itemAttrs = item.getAttributes();
						items.add(new RewardItem(
								parseInteger(itemAttrs, "id"),
								parseLong(itemAttrs, "amount")
						));
					}
				}
				config.addJackpotReward(new JackpotReward(weight, items));
			}
		}
	}

	// ==================== 對外API ====================

	/**
	 * 獲取指定ID的刮刮樂配置
	 */
	public ScratchCardConfig getConfig(int cardId)
	{
		return configs.get(cardId);
	}

	/**
	 * 獲取所有啟用的刮刮樂配置
	 */
	public Map<Integer, ScratchCardConfig> getAllConfigs()
	{
		return configs;
	}

	// ==================== 內部類 ====================

	public static class ScratchCardConfig
	{
		private final int id;
		private final String name;
		private int priceItemId;
		private long priceCount;
		private int dailyLimit;
		private int gridSize;
		private int emptyChance;
		private int accumulateChance;
		private int jackpotChance;
		private final Map<Integer, List<RewardItem>> accumulateRewards = new HashMap<>();
		private final List<JackpotReward> jackpotRewards = new ArrayList<>();
		private int totalJackpotWeight = 0;

		public ScratchCardConfig(int id, String name)
		{
			this.id = id;
			this.name = name;
		}

		public int generateRandomSymbol()
		{
			int roll = Rnd.get(PROBABILITY_BASE);
			if (roll < emptyChance) return 0;
			else if (roll < emptyChance + accumulateChance) return 1;
			else return 2;
		}

		public JackpotReward getRandomJackpot()
		{
			int roll = Rnd.get(totalJackpotWeight);
			int currentWeight = 0;
			for (JackpotReward reward : jackpotRewards)
			{
				currentWeight += reward.getWeight();
				if (roll < currentWeight) return reward;
			}
			return jackpotRewards.get(0);
		}

		public List<RewardItem> getAccumulateReward(int count)
		{
			List<RewardItem> bestReward = null;
			int bestCount = 0;
			for (Map.Entry<Integer, List<RewardItem>> entry : accumulateRewards.entrySet())
			{
				if (count >= entry.getKey() && entry.getKey() > bestCount)
				{
					bestCount = entry.getKey();
					bestReward = entry.getValue();
				}
			}
			return bestReward;
		}

		public void addAccumulateReward(int count, List<RewardItem> items)
		{
			accumulateRewards.put(count, items);
		}

		public void addJackpotReward(JackpotReward reward)
		{
			jackpotRewards.add(reward);
			totalJackpotWeight += reward.getWeight();
		}

		// Getters and Setters
		public int getId() { return id; }
		public String getName() { return name; }
		public int getPriceItemId() { return priceItemId; }
		public void setPriceItemId(int priceItemId) { this.priceItemId = priceItemId; }
		public long getPriceCount() { return priceCount; }
		public void setPriceCount(long priceCount) { this.priceCount = priceCount; }
		public int getDailyLimit() { return dailyLimit; }
		public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
		public int getGridSize() { return gridSize; }
		public void setGridSize(int gridSize) { this.gridSize = gridSize; }
		public int getEmptyChance() { return emptyChance; }
		public void setEmptyChance(int emptyChance) { this.emptyChance = emptyChance; }
		public int getAccumulateChance() { return accumulateChance; }
		public void setAccumulateChance(int accumulateChance) { this.accumulateChance = accumulateChance; }
		public int getJackpotChance() { return jackpotChance; }
		public void setJackpotChance(int jackpotChance) { this.jackpotChance = jackpotChance; }
		public Map<Integer, List<RewardItem>> getAccumulateRewards() { return accumulateRewards; }
		public List<JackpotReward> getJackpotRewards() { return jackpotRewards; }
		public int getTotalJackpotWeight() { return totalJackpotWeight; }
	}

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

	public static ScratchCardData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final ScratchCardData INSTANCE = new ScratchCardData();
	}
}