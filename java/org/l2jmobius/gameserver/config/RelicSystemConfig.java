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
 * This class loads all the relic system related configurations.
 * @author Mobius
 */
public class RelicSystemConfig
{
	// File
	private static final String RELIC_SYSTEM_CONFIG_FILE = "./config/RelicSystem.ini";
	
	// Constants
	public static boolean RELIC_SYSTEM_ENABLED;
	public static boolean RELIC_SYSTEM_DEBUG_ENABLED;
	public static boolean RELIC_SUMMON_ANNOUNCE;
	public static boolean RELIC_ANNOUNCE_ONLY_A_B_GRADE;
	public static int RELIC_UNCONFIRMED_LIST_LIMIT;
	public static int RELIC_UNCONFIRMED_TIME_LIMIT;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(RELIC_SYSTEM_CONFIG_FILE);
		RELIC_SYSTEM_ENABLED = config.getBoolean("RelicSystemEnabled", true);
		RELIC_SYSTEM_DEBUG_ENABLED = config.getBoolean("RelicSystemDebugEnabled", false);
		RELIC_SUMMON_ANNOUNCE = config.getBoolean("RelicSummonAnnounce", true);
		RELIC_ANNOUNCE_ONLY_A_B_GRADE = config.getBoolean("RelicAnnounceOnlyABGrade", true);
		RELIC_UNCONFIRMED_LIST_LIMIT = config.getInt("RelicUnconfirmedListLimit", 100);
		RELIC_UNCONFIRMED_TIME_LIMIT = config.getInt("RelicUnconfirmedTimeLimit", 7);
	}
}
