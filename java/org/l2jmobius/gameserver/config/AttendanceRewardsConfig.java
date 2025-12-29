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

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the attendance reward related configurations.
 * @author Mobius
 */
public class AttendanceRewardsConfig
{
	// File
	private static final String ATTENDANCE_CONFIG_FILE = "./config/AttendanceRewards.ini";
	
	// Constants
	public static boolean ENABLE_ATTENDANCE_REWARDS;
	public static boolean PREMIUM_ONLY_ATTENDANCE_REWARDS;
	public static boolean VIP_ONLY_ATTENDANCE_REWARDS;
	public static boolean ATTENDANCE_REWARDS_SHARE_ACCOUNT;
	public static int ATTENDANCE_REWARD_DELAY;
	public static boolean ATTENDANCE_POPUP_START;
	public static boolean ATTENDANCE_POPUP_WINDOW;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(ATTENDANCE_CONFIG_FILE);
		ENABLE_ATTENDANCE_REWARDS = config.getBoolean("EnableAttendanceRewards", false);
		PREMIUM_ONLY_ATTENDANCE_REWARDS = config.getBoolean("PremiumOnlyAttendanceRewards", false);
		VIP_ONLY_ATTENDANCE_REWARDS = config.getBoolean("VipOnlyAttendanceRewards", false);
		ATTENDANCE_REWARDS_SHARE_ACCOUNT = config.getBoolean("AttendanceRewardsShareAccount", false);
		ATTENDANCE_REWARD_DELAY = config.getInt("AttendanceRewardDelay", 30);
		ATTENDANCE_POPUP_START = config.getBoolean("AttendancePopupStart", true);
		ATTENDANCE_POPUP_WINDOW = config.getBoolean("AttendancePopupWindow", false);
	}
}
