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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 寶玉系統配置類
 * @author YourName
 */
public class JewelSystemConfig
{
	private static final Logger LOGGER = Logger.getLogger(JewelSystemConfig.class.getName());
	private static final String CONFIG_FILE = "data/scripts/custom/JewelSystem/JewelSystem.ini";

	// 基本設置
	public static int ACTIVATION_ITEM_ID = 100000;
	public static int REVEAL_VALUE_ITEM_ID = 100001;
	public static int BREAKTHROUGH_ITEM_ID = 100002;
	public static int NPC_ID = 900030;

	// 數值揭露設置 [階段][0=最小值, 1=最大值, 2=消耗數量]
	public static final long[][] STAGE_VALUES = new long[20][3];

	// 加成百分比 (按區間)
	public static int BONUS_PERCENT_TIER1 = 100;
	public static int BONUS_PERCENT_TIER2 = 200;
	public static int BONUS_PERCENT_TIER3 = 300;
	public static int BONUS_PERCENT_TIER4 = 400;

	// 突破機率 [階段][0=成功, 1=失敗, 2=倒退]
	public static final int[][] BREAKTHROUGH_RATES = new int[5][3];

	// 突破消耗
	public static final int[] BREAKTHROUGH_COSTS = new int[5];

	/**
	 * 載入配置
	 */
	public static void load()
	{
		LOGGER.info("========================================");
		LOGGER.info("【寶玉系統】載入配置文件...");

		final Properties props = new Properties();
		final File configFile = new File(CONFIG_FILE);

		if (!configFile.exists())
		{
			LOGGER.warning("【寶玉系統】配置文件不存在: " + CONFIG_FILE);
			LOGGER.warning("【寶玉系統】使用預設配置");
			loadDefaults();
			return;
		}

		try (FileInputStream fis = new FileInputStream(configFile))
		{
			props.load(fis);

			// 基本設置
			ACTIVATION_ITEM_ID = parseInt(props, "ActivationItemId", 100000);
			REVEAL_VALUE_ITEM_ID = parseInt(props, "RevealValueItemId", 100001);
			BREAKTHROUGH_ITEM_ID = parseInt(props, "BreakthroughItemId", 100002);
			NPC_ID = parseInt(props, "NpcId", 900030);

			// 數值揭露設置
			for (int i = 1; i <= 20; i++)
			{
				final String key = "Stage" + i + ".Value";
				final String value = props.getProperty(key, "10000,50000,1");
				final String[] parts = value.split(",");
				STAGE_VALUES[i - 1][0] = Long.parseLong(parts[0].trim());
				STAGE_VALUES[i - 1][1] = Long.parseLong(parts[1].trim());
				STAGE_VALUES[i - 1][2] = Long.parseLong(parts[2].trim());
			}

			// 加成百分比
			BONUS_PERCENT_TIER1 = parseInt(props, "BonusPercent.Tier1", 100);
			BONUS_PERCENT_TIER2 = parseInt(props, "BonusPercent.Tier2", 200);
			BONUS_PERCENT_TIER3 = parseInt(props, "BonusPercent.Tier3", 300);
			BONUS_PERCENT_TIER4 = parseInt(props, "BonusPercent.Tier4", 400);

			// 突破機率
			parseBreakthroughRate(props, "Breakthrough.0to1", 0);
			parseBreakthroughRate(props, "Breakthrough.1to2", 1);
			parseBreakthroughRate(props, "Breakthrough.2to3", 2);
			parseBreakthroughRate(props, "Breakthrough.3to4", 3);
			parseBreakthroughRate(props, "Breakthrough.4to5", 4);

			// 突破消耗
			BREAKTHROUGH_COSTS[0] = parseInt(props, "BreakthroughCost.0to1", 1);
			BREAKTHROUGH_COSTS[1] = parseInt(props, "BreakthroughCost.1to2", 2);
			BREAKTHROUGH_COSTS[2] = parseInt(props, "BreakthroughCost.2to3", 3);
			BREAKTHROUGH_COSTS[3] = parseInt(props, "BreakthroughCost.3to4", 4);
			BREAKTHROUGH_COSTS[4] = parseInt(props, "BreakthroughCost.4to5", 5);

			LOGGER.info("【寶玉系統】配置載入完成");
			LOGGER.info("【啟用道具】" + ACTIVATION_ITEM_ID);
			LOGGER.info("【揭露道具】" + REVEAL_VALUE_ITEM_ID);
			LOGGER.info("【突破道具】" + BREAKTHROUGH_ITEM_ID);
			LOGGER.info("========================================");
		}
		catch (Exception e)
		{
			LOGGER.severe("【寶玉系統】載入配置時發生錯誤: " + e.getMessage());
			loadDefaults();
		}
	}

	private static void loadDefaults()
	{
		// 預設數值揭露設置
		for (int i = 0; i < 20; i++)
		{
			STAGE_VALUES[i][0] = 10000 + (i * 10000);
			STAGE_VALUES[i][1] = 50000 + (i * 25000);
			STAGE_VALUES[i][2] = i + 1;
		}

		// 預設突破機率
		BREAKTHROUGH_RATES[0] = new int[] {50, 40, 10};
		BREAKTHROUGH_RATES[1] = new int[] {40, 45, 15};
		BREAKTHROUGH_RATES[2] = new int[] {30, 50, 20};
		BREAKTHROUGH_RATES[3] = new int[] {20, 55, 25};
		BREAKTHROUGH_RATES[4] = new int[] {10, 60, 30};

		// 預設突破消耗
		for (int i = 0; i < 5; i++)
		{
			BREAKTHROUGH_COSTS[i] = i + 1;
		}

		LOGGER.info("【寶玉系統】預設配置載入完成");
	}

	private static int parseInt(Properties props, String key, int defaultValue)
	{
		try
		{
			return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)).trim());
		}
		catch (NumberFormatException e)
		{
			return defaultValue;
		}
	}

	private static void parseBreakthroughRate(Properties props, String key, int index)
	{
		final String value = props.getProperty(key, "50,40,10");
		final String[] parts = value.split(",");
		BREAKTHROUGH_RATES[index][0] = Integer.parseInt(parts[0].trim());
		BREAKTHROUGH_RATES[index][1] = Integer.parseInt(parts[1].trim());
		BREAKTHROUGH_RATES[index][2] = Integer.parseInt(parts[2].trim());
	}

	/**
	 * 獲取指定階段的加成百分比
	 * @param stage 階段 (1-20)
	 * @return 加成百分比
	 */
	public static int getBonusPercent(int stage)
	{
		if (stage <= 5)
		{
			return BONUS_PERCENT_TIER1;
		}
		else if (stage <= 10)
		{
			return BONUS_PERCENT_TIER2;
		}
		else if (stage <= 15)
		{
			return BONUS_PERCENT_TIER3;
		}
		else
		{
			return BONUS_PERCENT_TIER4;
		}
	}

	/**
	 * 獲取突破機率索引 (0-4 循環)
	 * @param currentStage 當前加成階段
	 * @return 機率索引
	 */
	public static int getBreakthroughRateIndex(int currentStage)
	{
		return currentStage % 5;
	}
}
