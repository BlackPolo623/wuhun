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
package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * 多層副本系統配置
 * @author Claude
 */
public class MultiLayerDungeonConfig
{
	// File（統一使用腳本目錄下的 INI）
	private static final String CONFIG_FILE = "./data/scripts/instances/MultiLayerDungeon/MultiLayerDungeon.ini";

	// General
	public static int MAX_LAYERS;
	public static int ENTRANCE_NPC_ID;
	public static int TREASURE_CHEST_NPC_ID;
	public static int TELEPORT_NPC_ID;
	public static long EXIT_DELAY;

	// Entrance requirements
	public static int ENTRANCE_ITEM_ID;
	public static long ENTRANCE_ITEM_COUNT;
	public static int MAX_ENTRY_DISTANCE;
	public static int MIN_GROUP;
	public static int MAX_GROUP;

	// Layer Teleport Chances
	public static int LAYER_1_TELEPORT_CHANCE;
	public static int LAYER_2_TELEPORT_CHANCE;
	public static int LAYER_3_TELEPORT_CHANCE;
	public static int LAYER_4_TELEPORT_CHANCE;
	public static int LAYER_5_TELEPORT_CHANCE;

	// Layer Templates
	public static String LAYER_1_TEMPLATES;
	public static String LAYER_2_TEMPLATES;
	public static String LAYER_3_TEMPLATES;
	public static String LAYER_4_TEMPLATES;
	public static String LAYER_5_TEMPLATES;

	// Layer Monster Stat Multipliers - HP
	public static double LAYER_1_HP_MULTIPLIER;
	public static double LAYER_2_HP_MULTIPLIER;
	public static double LAYER_3_HP_MULTIPLIER;
	public static double LAYER_4_HP_MULTIPLIER;
	public static double LAYER_5_HP_MULTIPLIER;

	// Layer Monster Stat Multipliers - Physical Attack
	public static double LAYER_1_PATK_MULTIPLIER;
	public static double LAYER_2_PATK_MULTIPLIER;
	public static double LAYER_3_PATK_MULTIPLIER;
	public static double LAYER_4_PATK_MULTIPLIER;
	public static double LAYER_5_PATK_MULTIPLIER;

	// Layer Monster Stat Multipliers - Magical Attack
	public static double LAYER_1_MATK_MULTIPLIER;
	public static double LAYER_2_MATK_MULTIPLIER;
	public static double LAYER_3_MATK_MULTIPLIER;
	public static double LAYER_4_MATK_MULTIPLIER;
	public static double LAYER_5_MATK_MULTIPLIER;

	// Layer Monster Stat Multipliers - Physical Defense
	public static double LAYER_1_PDEF_MULTIPLIER;
	public static double LAYER_2_PDEF_MULTIPLIER;
	public static double LAYER_3_PDEF_MULTIPLIER;
	public static double LAYER_4_PDEF_MULTIPLIER;
	public static double LAYER_5_PDEF_MULTIPLIER;

	// Layer Monster Stat Multipliers - Magical Defense
	public static double LAYER_1_MDEF_MULTIPLIER;
	public static double LAYER_2_MDEF_MULTIPLIER;
	public static double LAYER_3_MDEF_MULTIPLIER;
	public static double LAYER_4_MDEF_MULTIPLIER;
	public static double LAYER_5_MDEF_MULTIPLIER;

	// Layer Monster Stat Multipliers - Physical Attack Speed
	public static double LAYER_1_ATKSPD_MULTIPLIER;
	public static double LAYER_2_ATKSPD_MULTIPLIER;
	public static double LAYER_3_ATKSPD_MULTIPLIER;
	public static double LAYER_4_ATKSPD_MULTIPLIER;
	public static double LAYER_5_ATKSPD_MULTIPLIER;

	// Layer Monster Stat Multipliers - Cast Speed
	public static double LAYER_1_CASTSPD_MULTIPLIER;
	public static double LAYER_2_CASTSPD_MULTIPLIER;
	public static double LAYER_3_CASTSPD_MULTIPLIER;
	public static double LAYER_4_CASTSPD_MULTIPLIER;
	public static double LAYER_5_CASTSPD_MULTIPLIER;

	// Layer Monster Final Damage Reduce (%)
	// 範圍: 0.0 ~ 99.999，套用於怪物被攻擊時的最終減傷
	public static double LAYER_1_FINAL_DAMAGE_REDUCE;
	public static double LAYER_2_FINAL_DAMAGE_REDUCE;
	public static double LAYER_3_FINAL_DAMAGE_REDUCE;
	public static double LAYER_4_FINAL_DAMAGE_REDUCE;
	public static double LAYER_5_FINAL_DAMAGE_REDUCE;

	public static void load()
	{
		final ConfigReader config = new ConfigReader(CONFIG_FILE);

		MAX_LAYERS = config.getInt("MaxLayers", 5);
		ENTRANCE_NPC_ID = config.getInt("EntranceNpcId", 900043);
		TREASURE_CHEST_NPC_ID = config.getInt("TreasureChestNpcId", 920001);
		TELEPORT_NPC_ID = config.getInt("TeleportNpcId", 900045);
		EXIT_DELAY = config.getInt("ExitDelay", 60) * 1000L;

		ENTRANCE_ITEM_ID = config.getInt("EntranceItemId", 0);
		ENTRANCE_ITEM_COUNT = config.getLong("EntranceItemCount", 1);
		MAX_ENTRY_DISTANCE = config.getInt("MaxEntryDistance", 1000);
		MIN_GROUP = config.getInt("GroupMin", 2);
		MAX_GROUP = config.getInt("GroupMax", 7);

		// 每層傳送NPC出現機率
		LAYER_1_TELEPORT_CHANCE = config.getInt("Layer1TeleportChance", 90);
		LAYER_2_TELEPORT_CHANCE = config.getInt("Layer2TeleportChance", 80);
		LAYER_3_TELEPORT_CHANCE = config.getInt("Layer3TeleportChance", 70);
		LAYER_4_TELEPORT_CHANCE = config.getInt("Layer4TeleportChance", 60);
		LAYER_5_TELEPORT_CHANCE = config.getInt("Layer5TeleportChance", 50);

		// 每層模板
		LAYER_1_TEMPLATES = config.getString("Layer1Templates", "300,301");
		LAYER_2_TEMPLATES = config.getString("Layer2Templates", "300,301,302");
		LAYER_3_TEMPLATES = config.getString("Layer3Templates", "301,302");
		LAYER_4_TEMPLATES = config.getString("Layer4Templates", "300,302");
		LAYER_5_TEMPLATES = config.getString("Layer5Templates", "302");

		// HP 倍率
		LAYER_1_HP_MULTIPLIER = config.getDouble("Layer1HpMultiplier", 20.0);
		LAYER_2_HP_MULTIPLIER = config.getDouble("Layer2HpMultiplier", 40.0);
		LAYER_3_HP_MULTIPLIER = config.getDouble("Layer3HpMultiplier", 60.0);
		LAYER_4_HP_MULTIPLIER = config.getDouble("Layer4HpMultiplier", 80.0);
		LAYER_5_HP_MULTIPLIER = config.getDouble("Layer5HpMultiplier", 100.0);

		// 物理攻擊倍率
		LAYER_1_PATK_MULTIPLIER = config.getDouble("Layer1PAtkMultiplier", 20.0);
		LAYER_2_PATK_MULTIPLIER = config.getDouble("Layer2PAtkMultiplier", 40.0);
		LAYER_3_PATK_MULTIPLIER = config.getDouble("Layer3PAtkMultiplier", 60.0);
		LAYER_4_PATK_MULTIPLIER = config.getDouble("Layer4PAtkMultiplier", 80.0);
		LAYER_5_PATK_MULTIPLIER = config.getDouble("Layer5PAtkMultiplier", 100.0);

		// 魔法攻擊倍率
		LAYER_1_MATK_MULTIPLIER = config.getDouble("Layer1MAtkMultiplier", 20.0);
		LAYER_2_MATK_MULTIPLIER = config.getDouble("Layer2MAtkMultiplier", 40.0);
		LAYER_3_MATK_MULTIPLIER = config.getDouble("Layer3MAtkMultiplier", 60.0);
		LAYER_4_MATK_MULTIPLIER = config.getDouble("Layer4MAtkMultiplier", 80.0);
		LAYER_5_MATK_MULTIPLIER = config.getDouble("Layer5MAtkMultiplier", 100.0);

		// 物理防禦倍率
		LAYER_1_PDEF_MULTIPLIER = config.getDouble("Layer1PDefMultiplier", 20.0);
		LAYER_2_PDEF_MULTIPLIER = config.getDouble("Layer2PDefMultiplier", 40.0);
		LAYER_3_PDEF_MULTIPLIER = config.getDouble("Layer3PDefMultiplier", 60.0);
		LAYER_4_PDEF_MULTIPLIER = config.getDouble("Layer4PDefMultiplier", 80.0);
		LAYER_5_PDEF_MULTIPLIER = config.getDouble("Layer5PDefMultiplier", 100.0);

		// 魔法防禦倍率
		LAYER_1_MDEF_MULTIPLIER = config.getDouble("Layer1MDefMultiplier", 20.0);
		LAYER_2_MDEF_MULTIPLIER = config.getDouble("Layer2MDefMultiplier", 40.0);
		LAYER_3_MDEF_MULTIPLIER = config.getDouble("Layer3MDefMultiplier", 60.0);
		LAYER_4_MDEF_MULTIPLIER = config.getDouble("Layer4MDefMultiplier", 80.0);
		LAYER_5_MDEF_MULTIPLIER = config.getDouble("Layer5MDefMultiplier", 100.0);

		// 物理攻擊速度倍率
		LAYER_1_ATKSPD_MULTIPLIER = config.getDouble("Layer1AtkSpdMultiplier", 1.0);
		LAYER_2_ATKSPD_MULTIPLIER = config.getDouble("Layer2AtkSpdMultiplier", 1.0);
		LAYER_3_ATKSPD_MULTIPLIER = config.getDouble("Layer3AtkSpdMultiplier", 1.0);
		LAYER_4_ATKSPD_MULTIPLIER = config.getDouble("Layer4AtkSpdMultiplier", 1.0);
		LAYER_5_ATKSPD_MULTIPLIER = config.getDouble("Layer5AtkSpdMultiplier", 1.0);

		// 施法速度倍率
		LAYER_1_CASTSPD_MULTIPLIER = config.getDouble("Layer1CastSpdMultiplier", 1.0);
		LAYER_2_CASTSPD_MULTIPLIER = config.getDouble("Layer2CastSpdMultiplier", 1.0);
		LAYER_3_CASTSPD_MULTIPLIER = config.getDouble("Layer3CastSpdMultiplier", 1.0);
		LAYER_4_CASTSPD_MULTIPLIER = config.getDouble("Layer4CastSpdMultiplier", 1.0);
		LAYER_5_CASTSPD_MULTIPLIER = config.getDouble("Layer5CastSpdMultiplier", 1.0);

		// 最終減傷 (%) — 上限 99.999，0.0 表示不套用
		LAYER_1_FINAL_DAMAGE_REDUCE = Math.min(config.getDouble("Layer1FinalDamageReduce", 0.0), 99.99999);
		LAYER_2_FINAL_DAMAGE_REDUCE = Math.min(config.getDouble("Layer2FinalDamageReduce", 0.0), 99.99999);
		LAYER_3_FINAL_DAMAGE_REDUCE = Math.min(config.getDouble("Layer3FinalDamageReduce", 0.0), 99.99999);
		LAYER_4_FINAL_DAMAGE_REDUCE = Math.min(config.getDouble("Layer4FinalDamageReduce", 0.0), 99.99999);
		LAYER_5_FINAL_DAMAGE_REDUCE = Math.min(config.getDouble("Layer5FinalDamageReduce", 0.0), 99.99999);
	}

	/**
	 * 獲取指定層數的副本模板列表
	 */
	public static int[] getLayerTemplates(int layer)
	{
		final String templatesStr;
		switch (layer)
		{
			case 1: templatesStr = LAYER_1_TEMPLATES; break;
			case 2: templatesStr = LAYER_2_TEMPLATES; break;
			case 3: templatesStr = LAYER_3_TEMPLATES; break;
			case 4: templatesStr = LAYER_4_TEMPLATES; break;
			case 5: templatesStr = LAYER_5_TEMPLATES; break;
			default: templatesStr = LAYER_1_TEMPLATES;
		}
		final String[] parts = templatesStr.split(",");
		final int[] result = new int[parts.length];
		for (int i = 0; i < parts.length; i++)
		{
			result[i] = Integer.parseInt(parts[i].trim());
		}
		return result;
	}

	/**
	 * 獲取指定層數的傳送NPC出現機率
	 */
	public static int getTeleportChance(int layer)
	{
		switch (layer)
		{
			case 1: return LAYER_1_TELEPORT_CHANCE;
			case 2: return LAYER_2_TELEPORT_CHANCE;
			case 3: return LAYER_3_TELEPORT_CHANCE;
			case 4: return LAYER_4_TELEPORT_CHANCE;
			case 5: return LAYER_5_TELEPORT_CHANCE;
			default: return 50;
		}
	}

	/** 獲取指定層數的怪物 HP 倍率 */
	public static double getHpMultiplier(int layer)
	{
		switch (layer)
		{
			case 1: return LAYER_1_HP_MULTIPLIER;
			case 2: return LAYER_2_HP_MULTIPLIER;
			case 3: return LAYER_3_HP_MULTIPLIER;
			case 4: return LAYER_4_HP_MULTIPLIER;
			case 5: return LAYER_5_HP_MULTIPLIER;
			default: return 1.0;
		}
	}

	/** 獲取指定層數的怪物物理攻擊倍率 */
	public static double getPAtkMultiplier(int layer)
	{
		switch (layer)
		{
			case 1: return LAYER_1_PATK_MULTIPLIER;
			case 2: return LAYER_2_PATK_MULTIPLIER;
			case 3: return LAYER_3_PATK_MULTIPLIER;
			case 4: return LAYER_4_PATK_MULTIPLIER;
			case 5: return LAYER_5_PATK_MULTIPLIER;
			default: return 1.0;
		}
	}

	/** 獲取指定層數的怪物魔法攻擊倍率 */
	public static double getMAtkMultiplier(int layer)
	{
		switch (layer)
		{
			case 1: return LAYER_1_MATK_MULTIPLIER;
			case 2: return LAYER_2_MATK_MULTIPLIER;
			case 3: return LAYER_3_MATK_MULTIPLIER;
			case 4: return LAYER_4_MATK_MULTIPLIER;
			case 5: return LAYER_5_MATK_MULTIPLIER;
			default: return 1.0;
		}
	}

	/** 獲取指定層數的怪物物理防禦倍率 */
	public static double getPDefMultiplier(int layer)
	{
		switch (layer)
		{
			case 1: return LAYER_1_PDEF_MULTIPLIER;
			case 2: return LAYER_2_PDEF_MULTIPLIER;
			case 3: return LAYER_3_PDEF_MULTIPLIER;
			case 4: return LAYER_4_PDEF_MULTIPLIER;
			case 5: return LAYER_5_PDEF_MULTIPLIER;
			default: return 1.0;
		}
	}

	/** 獲取指定層數的怪物魔法防禦倍率 */
	public static double getMDefMultiplier(int layer)
	{
		switch (layer)
		{
			case 1: return LAYER_1_MDEF_MULTIPLIER;
			case 2: return LAYER_2_MDEF_MULTIPLIER;
			case 3: return LAYER_3_MDEF_MULTIPLIER;
			case 4: return LAYER_4_MDEF_MULTIPLIER;
			case 5: return LAYER_5_MDEF_MULTIPLIER;
			default: return 1.0;
		}
	}

	/** 獲取指定層數的怪物物理攻擊速度倍率 */
	public static double getAtkSpdMultiplier(int layer)
	{
		switch (layer)
		{
			case 1: return LAYER_1_ATKSPD_MULTIPLIER;
			case 2: return LAYER_2_ATKSPD_MULTIPLIER;
			case 3: return LAYER_3_ATKSPD_MULTIPLIER;
			case 4: return LAYER_4_ATKSPD_MULTIPLIER;
			case 5: return LAYER_5_ATKSPD_MULTIPLIER;
			default: return 1.0;
		}
	}

	/** 獲取指定層數的怪物施法速度倍率 */
	public static double getCastSpdMultiplier(int layer)
	{
		switch (layer)
		{
			case 1: return LAYER_1_CASTSPD_MULTIPLIER;
			case 2: return LAYER_2_CASTSPD_MULTIPLIER;
			case 3: return LAYER_3_CASTSPD_MULTIPLIER;
			case 4: return LAYER_4_CASTSPD_MULTIPLIER;
			case 5: return LAYER_5_CASTSPD_MULTIPLIER;
			default: return 1.0;
		}
	}

	/**
	 * 獲取指定層數的怪物最終減傷（%）
	 * 範圍: 0.0 ~ 99.999，0.0 表示不套用
	 */
	public static double getFinalDamageReduce(int layer)
	{
		switch (layer)
		{
			case 1: return LAYER_1_FINAL_DAMAGE_REDUCE;
			case 2: return LAYER_2_FINAL_DAMAGE_REDUCE;
			case 3: return LAYER_3_FINAL_DAMAGE_REDUCE;
			case 4: return LAYER_4_FINAL_DAMAGE_REDUCE;
			case 5: return LAYER_5_FINAL_DAMAGE_REDUCE;
			default: return 0.0;
		}
	}
}
