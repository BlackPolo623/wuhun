/*
 * This file is part of the L2J Mobius project.
 * Custom Mirror Instance System Configuration.
 */
package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * 鏡像副本系統設定
 * Mirror Instance System configuration loader.
 */
public class MirrorInstanceConfig
{
	// 修改為腳本目錄內的 INI 路徑
	private static final String CONFIG_FILE = "./data/scripts/instances/MirrorInstance/MirrorInstance.ini";

	public static boolean MIRROR_INSTANCE_ENABLED;
	public static int MIRROR_INSTANCE_NPC_ID;
	public static int MIRROR_FAKE_PLAYER_NPC_ID;
	public static int MIRROR_INSTANCE_TEMPLATE_ID;
	public static int MIRROR_ENTRY_ITEM_ID;
	public static long MIRROR_ENTRY_ITEM_COUNT;
	public static int MIRROR_DAILY_LIMIT;
	public static int MIRROR_RESET_HOUR;
	public static long MIRROR_BASE_ATTACK;
	public static long MIRROR_BASE_DEFENSE;
	public static long MIRROR_BASE_HP;
	public static int[] MIRROR_AVAILABLE_MULTIPLIERS;
	public static double MIRROR_REWARD_FDR_PER_RUN;
	public static double MIRROR_MAX_FINAL_DAMAGE_REDUCE;
	public static int MIRROR_REWARD_ITEM_ID;
	public static long MIRROR_REWARD_ITEM_COUNT;

	public static void load()
	{
		final ConfigReader config = new ConfigReader(CONFIG_FILE);

		MIRROR_INSTANCE_ENABLED = config.getBoolean("MirrorInstanceEnabled", true);
		MIRROR_INSTANCE_NPC_ID = config.getInt("MirrorInstanceNpcId", 900041);
		MIRROR_FAKE_PLAYER_NPC_ID = config.getInt("MirrorFakePlayerNpcId", 80002);
		MIRROR_INSTANCE_TEMPLATE_ID = config.getInt("MirrorInstanceTemplateId", 902);
		MIRROR_ENTRY_ITEM_ID = config.getInt("MirrorEntryItemId", 57);
		MIRROR_ENTRY_ITEM_COUNT = config.getLong("MirrorEntryItemCount", 10000000L);
		MIRROR_DAILY_LIMIT = config.getInt("MirrorDailyLimit", 3);
		MIRROR_RESET_HOUR = config.getInt("MirrorResetHour", 0);
		MIRROR_BASE_ATTACK = config.getLong("MirrorBaseAttack", 50000L);
		MIRROR_BASE_DEFENSE = config.getLong("MirrorBaseDefense", 10000L);
		MIRROR_BASE_HP = config.getLong("MirrorBaseHp", 1000000L);
		MIRROR_REWARD_FDR_PER_RUN = config.getDouble("MirrorRewardFdrPerRun", 0.01);
		MIRROR_MAX_FINAL_DAMAGE_REDUCE = config.getDouble("MirrorMaxFinalDamageReduce", 50.0);
		MIRROR_REWARD_ITEM_ID = config.getInt("MirrorRewardItemId", -1);
		MIRROR_REWARD_ITEM_COUNT = config.getLong("MirrorRewardItemCount", 0L);

		// Parse comma-separated multiplier list
		final String multipliersStr = config.getString("MirrorAvailableMultipliers", "2,3,5,10");
		final String[] parts = multipliersStr.split(",");
		MIRROR_AVAILABLE_MULTIPLIERS = new int[parts.length];
		for (int i = 0; i < parts.length; i++)
		{
			try
			{
				MIRROR_AVAILABLE_MULTIPLIERS[i] = Integer.parseInt(parts[i].trim());
			}
			catch (NumberFormatException e)
			{
				MIRROR_AVAILABLE_MULTIPLIERS[i] = 2;
			}
		}
	}
}
