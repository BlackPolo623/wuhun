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
 * This class loads all the random craft related configurations.
 * @author Mobius
 */
public class RandomCraftConfig
{
	// File
	private static final String RANDOM_CRAFT_FILE = "./config/RandomCraft.ini";
	
	// Constants
	public static boolean ENABLE_RANDOM_CRAFT;
	public static int RANDOM_CRAFT_REFRESH_FEE;
	public static int RANDOM_CRAFT_CREATE_FEE;
	public static boolean DROP_RANDOM_CRAFT_MATERIALS;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(RANDOM_CRAFT_FILE);
		ENABLE_RANDOM_CRAFT = config.getBoolean("RandomCraftEnabled", true);
		RANDOM_CRAFT_REFRESH_FEE = config.getInt("RandomCraftRefreshFee", 50000);
		RANDOM_CRAFT_CREATE_FEE = config.getInt("RandomCraftCreateFee", 300000);
		DROP_RANDOM_CRAFT_MATERIALS = config.getBoolean("DropRandomCraftMaterials", true);
	}
}
