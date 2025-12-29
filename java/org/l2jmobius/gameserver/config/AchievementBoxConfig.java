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
 * This class loads all the achievement box related configurations.
 * @author Mobius
 */
public class AchievementBoxConfig
{
	// File
	private static final String ACHIEVEMENT_BOX_CONFIG_FILE = "./config/AchievementBox.ini";
	
	// Constants
	public static boolean ENABLE_ACHIEVEMENT_BOX;
	public static int ACHIEVEMENT_BOX_POINTS_FOR_REWARD;
	public static boolean ENABLE_ACHIEVEMENT_PVP;
	public static int ACHIEVEMENT_BOX_PVP_POINTS_FOR_REWARD;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(ACHIEVEMENT_BOX_CONFIG_FILE);
		ENABLE_ACHIEVEMENT_BOX = config.getBoolean("EnabledAchievementBox", true);
		ACHIEVEMENT_BOX_POINTS_FOR_REWARD = config.getInt("PointsForReward", 1000);
		ENABLE_ACHIEVEMENT_PVP = config.getBoolean("EnabledAchievementPvP", true);
		ACHIEVEMENT_BOX_PVP_POINTS_FOR_REWARD = config.getInt("PointsForPvpReward", 5);
	}
}
