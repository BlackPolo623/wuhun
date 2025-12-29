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
 * This class loads all the VIP system related configurations.
 * @author Mobius
 */
public class VipSystemConfig
{
	// File
	private static final String CUSTOM_VIP_CONFIG_FILE = "./config/Custom/VipSystem.ini";
	
	// Constants
	public static boolean VIP_SYSTEM_ENABLED;
	public static boolean VIP_SYSTEM_PRIME_AFFECT;
	public static boolean VIP_SYSTEM_L_SHOP_AFFECT;
	public static int VIP_SYSTEM_MAX_TIER;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(CUSTOM_VIP_CONFIG_FILE);
		VIP_SYSTEM_ENABLED = config.getBoolean("VipEnabled", false);
		if (VIP_SYSTEM_ENABLED)
		{
			VIP_SYSTEM_PRIME_AFFECT = config.getBoolean("PrimeAffectPoints", false);
			VIP_SYSTEM_L_SHOP_AFFECT = config.getBoolean("LShopAffectPoints", false);
			VIP_SYSTEM_MAX_TIER = config.getInt("MaxVipLevel", 7);
			if (VIP_SYSTEM_MAX_TIER > 10)
			{
				VIP_SYSTEM_MAX_TIER = 10;
			}
		}
	}
}
