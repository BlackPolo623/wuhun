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
 * This class loads all the allowed player races related configurations.
 * @author Mobius
 */
public class AllowedPlayerRacesConfig
{
	// File
	private static final String ALLOWED_PLAYER_RACES_CONFIG_FILE = "./config/Custom/AllowedPlayerRaces.ini";
	
	// Constants
	public static boolean ALLOW_HUMAN;
	public static boolean ALLOW_ELF;
	public static boolean ALLOW_DARKELF;
	public static boolean ALLOW_ORC;
	public static boolean ALLOW_DWARF;
	public static boolean ALLOW_KAMAEL;
	public static boolean ALLOW_DEATH_KNIGHT;
	public static boolean ALLOW_SYLPH;
	public static boolean ALLOW_VANGUARD;
	public static boolean ALLOW_ASSASSIN;
	public static boolean ALLOW_HIGH_ELF;
	public static boolean ALLOW_WARG;
	public static boolean ALLOW_BLOOD_ROSE;
	public static boolean ALLOW_SAMURAI;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(ALLOWED_PLAYER_RACES_CONFIG_FILE);
		ALLOW_HUMAN = config.getBoolean("AllowHuman", true);
		ALLOW_ELF = config.getBoolean("AllowElf", true);
		ALLOW_DARKELF = config.getBoolean("AllowDarkElf", true);
		ALLOW_ORC = config.getBoolean("AllowOrc", true);
		ALLOW_DWARF = config.getBoolean("AllowDwarf", true);
		ALLOW_KAMAEL = config.getBoolean("AllowKamael", true);
		ALLOW_DEATH_KNIGHT = config.getBoolean("AllowDeathKnight", true);
		ALLOW_SYLPH = config.getBoolean("AllowSylph", true);
		ALLOW_VANGUARD = config.getBoolean("AllowVanguard", true);
		ALLOW_ASSASSIN = config.getBoolean("AllowAssassin", true);
		ALLOW_HIGH_ELF = config.getBoolean("AllowHighElf", true);
		ALLOW_WARG = config.getBoolean("AllowWarg", true);
		ALLOW_BLOOD_ROSE = config.getBoolean("AllowBloodRose", true);
		ALLOW_SAMURAI = config.getBoolean("AllowSamurai", true);
	}
}
