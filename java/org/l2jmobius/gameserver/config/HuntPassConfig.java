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
 * This class loads all the hunt pass related configurations.
 * @author Mobius
 */
public class HuntPassConfig
{
	// File
	private static final String HUNT_PASS_CONFIG_FILE = "./config/HuntPass.ini";
	
	// Constants
	public static boolean ENABLE_HUNT_PASS;
	public static int HUNT_PASS_PREMIUM_ITEM_ID;
	public static int HUNT_PASS_PREMIUM_ITEM_COUNT;
	public static int HUNT_PASS_POINTS_FOR_STEP;
	public static int HUNT_PASS_PERIOD;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(HUNT_PASS_CONFIG_FILE);
		ENABLE_HUNT_PASS = config.getBoolean("EnabledHuntPass", true);
		HUNT_PASS_PREMIUM_ITEM_ID = config.getInt("PremiumItemId", 91663);
		HUNT_PASS_PREMIUM_ITEM_COUNT = config.getInt("PremiumItemCount", 3600);
		HUNT_PASS_POINTS_FOR_STEP = config.getInt("PointsForStep", 2400);
		HUNT_PASS_PERIOD = config.getInt("DayOfMonth", 1);
	}
}
