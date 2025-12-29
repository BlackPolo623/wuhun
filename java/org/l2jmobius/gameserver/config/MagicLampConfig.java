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
 * This class loads all the magic lamp related configurations.
 * @author Mobius
 */
public class MagicLampConfig
{
	// File
	private static final String MAGIC_LAMP_FILE = "./config/MagicLamp.ini";
	
	// Constants
	public static boolean ENABLE_MAGIC_LAMP;
	public static int MAGIC_LAMP_MAX_LEVEL_EXP;
	public static double MAGIC_LAMP_CHARGE_RATE;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(MAGIC_LAMP_FILE);
		ENABLE_MAGIC_LAMP = config.getBoolean("MagicLampEnabled", false);
		MAGIC_LAMP_MAX_LEVEL_EXP = config.getInt("MagicLampMaxLevelExp", 10000000);
		MAGIC_LAMP_CHARGE_RATE = config.getDouble("MagicLampChargeRate", 0.1);
	}
}
