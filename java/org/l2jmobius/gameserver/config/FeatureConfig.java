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
package org.l2jmobius.gameserver.config;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeClientEffectInfo;

/**
 * This class loads all the feature related configurations.
 * @author Mobius
 */
public class FeatureConfig
{
	// File
	private static final String FEATURE_CONFIG_FILE = "./config/Feature.ini";
	
	// Constants
	public static List<Integer> SIEGE_HOUR_LIST;
	public static int CASTLE_BUY_TAX_NEUTRAL;
	public static int CASTLE_BUY_TAX_LIGHT;
	public static int CASTLE_BUY_TAX_DARK;
	public static int CASTLE_SELL_TAX_NEUTRAL;
	public static int CASTLE_SELL_TAX_LIGHT;
	public static int CASTLE_SELL_TAX_DARK;
	public static long CS_TELE_FEE_RATIO;
	public static int CS_TELE1_FEE;
	public static int CS_TELE2_FEE;
	public static long CS_SUPPORT_FEE_RATIO;
	public static int CS_SUPPORT1_FEE;
	public static int CS_SUPPORT2_FEE;
	public static long CS_MPREG_FEE_RATIO;
	public static int CS_MPREG1_FEE;
	public static int CS_MPREG2_FEE;
	public static long CS_HPREG_FEE_RATIO;
	public static int CS_HPREG1_FEE;
	public static int CS_HPREG2_FEE;
	public static long CS_EXPREG_FEE_RATIO;
	public static int CS_EXPREG1_FEE;
	public static int CS_EXPREG2_FEE;
	public static int OUTER_DOOR_UPGRADE_PRICE2;
	public static int OUTER_DOOR_UPGRADE_PRICE3;
	public static int OUTER_DOOR_UPGRADE_PRICE5;
	public static int INNER_DOOR_UPGRADE_PRICE2;
	public static int INNER_DOOR_UPGRADE_PRICE3;
	public static int INNER_DOOR_UPGRADE_PRICE5;
	public static int WALL_UPGRADE_PRICE2;
	public static int WALL_UPGRADE_PRICE3;
	public static int WALL_UPGRADE_PRICE5;
	public static int TRAP_UPGRADE_PRICE1;
	public static int TRAP_UPGRADE_PRICE2;
	public static int TRAP_UPGRADE_PRICE3;
	public static int TRAP_UPGRADE_PRICE4;
	public static long FS_TELE_FEE_RATIO;
	public static int FS_TELE1_FEE;
	public static int FS_TELE2_FEE;
	public static long FS_SUPPORT_FEE_RATIO;
	public static int FS_SUPPORT1_FEE;
	public static int FS_SUPPORT2_FEE;
	public static long FS_MPREG_FEE_RATIO;
	public static int FS_MPREG1_FEE;
	public static int FS_MPREG2_FEE;
	public static long FS_HPREG_FEE_RATIO;
	public static int FS_HPREG1_FEE;
	public static int FS_HPREG2_FEE;
	public static long FS_EXPREG_FEE_RATIO;
	public static int FS_EXPREG1_FEE;
	public static int FS_EXPREG2_FEE;
	public static int FS_UPDATE_FRQ;
	public static int FS_BLOOD_OATH_COUNT;
	public static int FS_MAX_SUPPLY_LEVEL;
	public static int FS_FEE_FOR_CASTLE;
	public static int FS_MAX_OWN_TIME;
	public static int TAKE_FORT_POINTS;
	public static int LOOSE_FORT_POINTS;
	public static int TAKE_CASTLE_POINTS;
	public static int LOOSE_CASTLE_POINTS;
	public static int CASTLE_DEFENDED_POINTS;
	public static int FESTIVAL_WIN_POINTS;
	public static int HERO_POINTS;
	public static int ROYAL_GUARD_COST;
	public static int KNIGHT_UNIT_COST;
	public static int KNIGHT_REINFORCE_COST;
	public static int BALLISTA_POINTS;
	public static int BLOODALLIANCE_POINTS;
	public static int BLOODOATH_POINTS;
	public static int KNIGHTSEPAULETTE_POINTS;
	public static int REPUTATION_SCORE_PER_KILL;
	public static int JOIN_ACADEMY_MIN_REP_SCORE;
	public static int JOIN_ACADEMY_MAX_REP_SCORE;
	public static int LVL_UP_20_AND_25_REP_SCORE;
	public static int LVL_UP_26_AND_30_REP_SCORE;
	public static int LVL_UP_31_AND_35_REP_SCORE;
	public static int LVL_UP_36_AND_40_REP_SCORE;
	public static int LVL_UP_41_AND_45_REP_SCORE;
	public static int LVL_UP_46_AND_50_REP_SCORE;
	public static int LVL_UP_51_AND_55_REP_SCORE;
	public static int LVL_UP_56_AND_60_REP_SCORE;
	public static int LVL_UP_61_AND_65_REP_SCORE;
	public static int LVL_UP_66_AND_70_REP_SCORE;
	public static int LVL_UP_71_AND_75_REP_SCORE;
	public static int LVL_UP_76_AND_80_REP_SCORE;
	public static int LVL_UP_81_AND_90_REP_SCORE;
	public static int LVL_UP_91_PLUS_REP_SCORE;
	public static double LVL_OBTAINED_REP_SCORE_MULTIPLIER;
	public static int CLAN_LEVEL_6_COST;
	public static int CLAN_LEVEL_7_COST;
	public static int CLAN_LEVEL_8_COST;
	public static int CLAN_LEVEL_9_COST;
	public static int CLAN_LEVEL_10_COST;
	public static int CLAN_LEVEL_6_REQUIREMENT;
	public static int CLAN_LEVEL_7_REQUIREMENT;
	public static int CLAN_LEVEL_8_REQUIREMENT;
	public static int CLAN_LEVEL_9_REQUIREMENT;
	public static int CLAN_LEVEL_10_REQUIREMENT;
	public static boolean PK_PENALTY_LIST;
	public static int PK_PENALTY_LIST_MINIMUM_COUNT;
	public static int ITEM_PENALTY_RESTORE_ADENA;
	public static int ITEM_PENALTY_RESTORE_LCOIN;
	public static ExChangeClientEffectInfo GIRAN_DAY_EFFECT;
	public static ExChangeClientEffectInfo GIRAN_NIGHT_EFFECT;
	public static boolean ALLOW_WYVERN_ALWAYS;
	public static boolean ALLOW_WYVERN_DURING_SIEGE;
	public static boolean ALLOW_MOUNTS_DURING_SIEGE;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(FEATURE_CONFIG_FILE);
		SIEGE_HOUR_LIST = new ArrayList<>();
		for (String hour : config.getString("SiegeHourList", "").split(","))
		{
			if (StringUtil.isNumeric(hour))
			{
				SIEGE_HOUR_LIST.add(Integer.parseInt(hour));
			}
		}
		CASTLE_BUY_TAX_NEUTRAL = config.getInt("BuyTaxForNeutralSide", 15);
		CASTLE_BUY_TAX_LIGHT = config.getInt("BuyTaxForLightSide", 0);
		CASTLE_BUY_TAX_DARK = config.getInt("BuyTaxForDarkSide", 30);
		CASTLE_SELL_TAX_NEUTRAL = config.getInt("SellTaxForNeutralSide", 0);
		CASTLE_SELL_TAX_LIGHT = config.getInt("SellTaxForLightSide", 0);
		CASTLE_SELL_TAX_DARK = config.getInt("SellTaxForDarkSide", 20);
		CS_TELE_FEE_RATIO = config.getLong("CastleTeleportFunctionFeeRatio", 604800000);
		CS_TELE1_FEE = config.getInt("CastleTeleportFunctionFeeLvl1", 1000);
		CS_TELE2_FEE = config.getInt("CastleTeleportFunctionFeeLvl2", 10000);
		CS_SUPPORT_FEE_RATIO = config.getLong("CastleSupportFunctionFeeRatio", 604800000);
		CS_SUPPORT1_FEE = config.getInt("CastleSupportFeeLvl1", 49000);
		CS_SUPPORT2_FEE = config.getInt("CastleSupportFeeLvl2", 120000);
		CS_MPREG_FEE_RATIO = config.getLong("CastleMpRegenerationFunctionFeeRatio", 604800000);
		CS_MPREG1_FEE = config.getInt("CastleMpRegenerationFeeLvl1", 45000);
		CS_MPREG2_FEE = config.getInt("CastleMpRegenerationFeeLvl2", 65000);
		CS_HPREG_FEE_RATIO = config.getLong("CastleHpRegenerationFunctionFeeRatio", 604800000);
		CS_HPREG1_FEE = config.getInt("CastleHpRegenerationFeeLvl1", 12000);
		CS_HPREG2_FEE = config.getInt("CastleHpRegenerationFeeLvl2", 20000);
		CS_EXPREG_FEE_RATIO = config.getLong("CastleExpRegenerationFunctionFeeRatio", 604800000);
		CS_EXPREG1_FEE = config.getInt("CastleExpRegenerationFeeLvl1", 63000);
		CS_EXPREG2_FEE = config.getInt("CastleExpRegenerationFeeLvl2", 70000);
		OUTER_DOOR_UPGRADE_PRICE2 = config.getInt("OuterDoorUpgradePriceLvl2", 3000000);
		OUTER_DOOR_UPGRADE_PRICE3 = config.getInt("OuterDoorUpgradePriceLvl3", 4000000);
		OUTER_DOOR_UPGRADE_PRICE5 = config.getInt("OuterDoorUpgradePriceLvl5", 5000000);
		INNER_DOOR_UPGRADE_PRICE2 = config.getInt("InnerDoorUpgradePriceLvl2", 750000);
		INNER_DOOR_UPGRADE_PRICE3 = config.getInt("InnerDoorUpgradePriceLvl3", 900000);
		INNER_DOOR_UPGRADE_PRICE5 = config.getInt("InnerDoorUpgradePriceLvl5", 1000000);
		WALL_UPGRADE_PRICE2 = config.getInt("WallUpgradePriceLvl2", 1600000);
		WALL_UPGRADE_PRICE3 = config.getInt("WallUpgradePriceLvl3", 1800000);
		WALL_UPGRADE_PRICE5 = config.getInt("WallUpgradePriceLvl5", 2000000);
		TRAP_UPGRADE_PRICE1 = config.getInt("TrapUpgradePriceLvl1", 3000000);
		TRAP_UPGRADE_PRICE2 = config.getInt("TrapUpgradePriceLvl2", 4000000);
		TRAP_UPGRADE_PRICE3 = config.getInt("TrapUpgradePriceLvl3", 5000000);
		TRAP_UPGRADE_PRICE4 = config.getInt("TrapUpgradePriceLvl4", 6000000);
		FS_TELE_FEE_RATIO = config.getLong("FortressTeleportFunctionFeeRatio", 604800000);
		FS_TELE1_FEE = config.getInt("FortressTeleportFunctionFeeLvl1", 1000);
		FS_TELE2_FEE = config.getInt("FortressTeleportFunctionFeeLvl2", 10000);
		FS_SUPPORT_FEE_RATIO = config.getLong("FortressSupportFunctionFeeRatio", 86400000);
		FS_SUPPORT1_FEE = config.getInt("FortressSupportFeeLvl1", 7000);
		FS_SUPPORT2_FEE = config.getInt("FortressSupportFeeLvl2", 17000);
		FS_MPREG_FEE_RATIO = config.getLong("FortressMpRegenerationFunctionFeeRatio", 86400000);
		FS_MPREG1_FEE = config.getInt("FortressMpRegenerationFeeLvl1", 6500);
		FS_MPREG2_FEE = config.getInt("FortressMpRegenerationFeeLvl2", 9300);
		FS_HPREG_FEE_RATIO = config.getLong("FortressHpRegenerationFunctionFeeRatio", 86400000);
		FS_HPREG1_FEE = config.getInt("FortressHpRegenerationFeeLvl1", 2000);
		FS_HPREG2_FEE = config.getInt("FortressHpRegenerationFeeLvl2", 3500);
		FS_EXPREG_FEE_RATIO = config.getLong("FortressExpRegenerationFunctionFeeRatio", 86400000);
		FS_EXPREG1_FEE = config.getInt("FortressExpRegenerationFeeLvl1", 9000);
		FS_EXPREG2_FEE = config.getInt("FortressExpRegenerationFeeLvl2", 10000);
		FS_UPDATE_FRQ = config.getInt("FortressPeriodicUpdateFrequency", 360);
		FS_BLOOD_OATH_COUNT = config.getInt("FortressBloodOathCount", 1);
		FS_MAX_SUPPLY_LEVEL = config.getInt("FortressMaxSupplyLevel", 6);
		FS_FEE_FOR_CASTLE = config.getInt("FortressFeeForCastle", 25000);
		FS_MAX_OWN_TIME = config.getInt("FortressMaximumOwnTime", 168);
		TAKE_FORT_POINTS = config.getInt("TakeFortPoints", 200);
		LOOSE_FORT_POINTS = config.getInt("LooseFortPoints", 0);
		TAKE_CASTLE_POINTS = config.getInt("TakeCastlePoints", 1500);
		LOOSE_CASTLE_POINTS = config.getInt("LooseCastlePoints", 3000);
		CASTLE_DEFENDED_POINTS = config.getInt("CastleDefendedPoints", 750);
		FESTIVAL_WIN_POINTS = config.getInt("FestivalOfDarknessWin", 200);
		HERO_POINTS = config.getInt("HeroPoints", 1000);
		ROYAL_GUARD_COST = config.getInt("CreateRoyalGuardCost", 5000);
		KNIGHT_UNIT_COST = config.getInt("CreateKnightUnitCost", 10000);
		KNIGHT_REINFORCE_COST = config.getInt("ReinforceKnightUnitCost", 5000);
		BALLISTA_POINTS = config.getInt("KillBallistaPoints", 500);
		BLOODALLIANCE_POINTS = config.getInt("BloodAlliancePoints", 500);
		BLOODOATH_POINTS = config.getInt("BloodOathPoints", 200);
		KNIGHTSEPAULETTE_POINTS = config.getInt("KnightsEpaulettePoints", 20);
		REPUTATION_SCORE_PER_KILL = config.getInt("ReputationScorePerKill", 1);
		JOIN_ACADEMY_MIN_REP_SCORE = config.getInt("CompleteAcademyMinPoints", 190);
		JOIN_ACADEMY_MAX_REP_SCORE = config.getInt("CompleteAcademyMaxPoints", 650);
		LVL_UP_20_AND_25_REP_SCORE = config.getInt("LevelUp20And25ReputationScore", 4);
		LVL_UP_26_AND_30_REP_SCORE = config.getInt("LevelUp26And30ReputationScore", 8);
		LVL_UP_31_AND_35_REP_SCORE = config.getInt("LevelUp31And35ReputationScore", 12);
		LVL_UP_36_AND_40_REP_SCORE = config.getInt("LevelUp36And40ReputationScore", 16);
		LVL_UP_41_AND_45_REP_SCORE = config.getInt("LevelUp41And45ReputationScore", 25);
		LVL_UP_46_AND_50_REP_SCORE = config.getInt("LevelUp46And50ReputationScore", 30);
		LVL_UP_51_AND_55_REP_SCORE = config.getInt("LevelUp51And55ReputationScore", 35);
		LVL_UP_56_AND_60_REP_SCORE = config.getInt("LevelUp56And60ReputationScore", 40);
		LVL_UP_61_AND_65_REP_SCORE = config.getInt("LevelUp61And65ReputationScore", 54);
		LVL_UP_66_AND_70_REP_SCORE = config.getInt("LevelUp66And70ReputationScore", 63);
		LVL_UP_71_AND_75_REP_SCORE = config.getInt("LevelUp71And75ReputationScore", 75);
		LVL_UP_76_AND_80_REP_SCORE = config.getInt("LevelUp76And80ReputationScore", 90);
		LVL_UP_81_AND_90_REP_SCORE = config.getInt("LevelUp81And90ReputationScore", 120);
		LVL_UP_91_PLUS_REP_SCORE = config.getInt("LevelUp91PlusReputationScore", 150);
		LVL_OBTAINED_REP_SCORE_MULTIPLIER = config.getDouble("LevelObtainedReputationScoreMultiplier", 1.0d);
		CLAN_LEVEL_6_COST = config.getInt("ClanLevel6Cost", 15000);
		CLAN_LEVEL_7_COST = config.getInt("ClanLevel7Cost", 450000);
		CLAN_LEVEL_8_COST = config.getInt("ClanLevel8Cost", 1000000);
		CLAN_LEVEL_9_COST = config.getInt("ClanLevel9Cost", 2000000);
		CLAN_LEVEL_10_COST = config.getInt("ClanLevel10Cost", 4000000);
		CLAN_LEVEL_6_REQUIREMENT = config.getInt("ClanLevel6Requirement", 40);
		CLAN_LEVEL_7_REQUIREMENT = config.getInt("ClanLevel7Requirement", 40);
		CLAN_LEVEL_8_REQUIREMENT = config.getInt("ClanLevel8Requirement", 40);
		CLAN_LEVEL_9_REQUIREMENT = config.getInt("ClanLevel9Requirement", 40);
		CLAN_LEVEL_10_REQUIREMENT = config.getInt("ClanLevel10Requirement", 40);
		PK_PENALTY_LIST = config.getBoolean("PkPenaltyList", true);
		PK_PENALTY_LIST_MINIMUM_COUNT = config.getInt("PkPenaltyMinimumCount", 9);
		ITEM_PENALTY_RESTORE_ADENA = config.getInt("PkPenaltyRestoreAdenaCost", 100);
		ITEM_PENALTY_RESTORE_LCOIN = config.getInt("PkPenaltyRestoreLCoinCost", 100);
		switch (config.getString("GiranDayEffect", ""))
		{
			case "Petals":
			{
				GIRAN_DAY_EFFECT = ExChangeClientEffectInfo.GIRAN_PETALS;
				break;
			}
			case "Snow":
			{
				GIRAN_DAY_EFFECT = ExChangeClientEffectInfo.GIRAN_SNOW;
				break;
			}
			case "Flowers":
			{
				GIRAN_DAY_EFFECT = ExChangeClientEffectInfo.GIRAN_FLOWERS;
				break;
			}
			case "Water":
			{
				GIRAN_DAY_EFFECT = ExChangeClientEffectInfo.GIRAN_WATER;
				break;
			}
			case "Autumn":
			{
				GIRAN_DAY_EFFECT = ExChangeClientEffectInfo.GIRAN_AUTUMN;
				break;
			}
			default:
			{
				GIRAN_DAY_EFFECT = ExChangeClientEffectInfo.GIRAN_NORMAL;
				break;
			}
		}
		switch (config.getString("GiranNightEffect", ""))
		{
			case "Petals":
			{
				GIRAN_NIGHT_EFFECT = ExChangeClientEffectInfo.GIRAN_PETALS;
				break;
			}
			case "Snow":
			{
				GIRAN_NIGHT_EFFECT = ExChangeClientEffectInfo.GIRAN_SNOW;
				break;
			}
			case "Flowers":
			{
				GIRAN_NIGHT_EFFECT = ExChangeClientEffectInfo.GIRAN_FLOWERS;
				break;
			}
			case "Water":
			{
				GIRAN_NIGHT_EFFECT = ExChangeClientEffectInfo.GIRAN_WATER;
				break;
			}
			case "Autumn":
			{
				GIRAN_NIGHT_EFFECT = ExChangeClientEffectInfo.GIRAN_AUTUMN;
				break;
			}
			default:
			{
				GIRAN_NIGHT_EFFECT = ExChangeClientEffectInfo.GIRAN_NORMAL;
				break;
			}
		}
		ALLOW_WYVERN_ALWAYS = config.getBoolean("AllowRideWyvernAlways", false);
		ALLOW_WYVERN_DURING_SIEGE = config.getBoolean("AllowRideWyvernDuringSiege", true);
		ALLOW_MOUNTS_DURING_SIEGE = config.getBoolean("AllowRideMountsDuringSiege", false);
	}
}
