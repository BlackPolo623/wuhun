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
 * This class loads all the training camp related configurations.
 * @author Mobius
 */
public class TrainingCampConfig
{
	// File
	private static final String TRAINING_CAMP_CONFIG_FILE = "./config/TrainingCamp.ini";
	
	// Constants
	public static boolean TRAINING_CAMP_ENABLE;
	public static boolean TRAINING_CAMP_PREMIUM_ONLY;
	public static int TRAINING_CAMP_MAX_DURATION;
	public static int TRAINING_CAMP_MIN_LEVEL;
	public static int TRAINING_CAMP_MAX_LEVEL;
	public static double TRAINING_CAMP_EXP_MULTIPLIER;
	public static double TRAINING_CAMP_SP_MULTIPLIER;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(TRAINING_CAMP_CONFIG_FILE);
		TRAINING_CAMP_ENABLE = config.getBoolean("TrainingCampEnable", false);
		TRAINING_CAMP_PREMIUM_ONLY = config.getBoolean("TrainingCampPremiumOnly", false);
		TRAINING_CAMP_MAX_DURATION = config.getInt("TrainingCampDuration", 18000);
		TRAINING_CAMP_MIN_LEVEL = config.getInt("TrainingCampMinLevel", 18);
		TRAINING_CAMP_MAX_LEVEL = config.getInt("TrainingCampMaxLevel", 127);
		TRAINING_CAMP_EXP_MULTIPLIER = config.getDouble("TrainingCampExpMultiplier", 1.0);
		TRAINING_CAMP_SP_MULTIPLIER = config.getDouble("TrainingCampSpMultiplier", 1.0);
	}
}
