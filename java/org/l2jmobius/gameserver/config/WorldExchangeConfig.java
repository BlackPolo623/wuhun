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
 * This class loads all the world exchange related configurations.
 * @author Mobius
 */
public class WorldExchangeConfig
{
	// File
	private static final String WORLD_EXCHANGE_FILE = "./config/WorldExchange.ini";
	
	// Constants
	public static boolean ENABLE_WORLD_EXCHANGE;
	public static String WORLD_EXCHANGE_DEFAULT_LANG;
	public static long WORLD_EXCHANGE_SAVE_INTERVAL;
	public static double WORLD_EXCHANGE_LCOIN_TAX;
	public static long WORLD_EXCHANGE_MAX_LCOIN_TAX;
	public static double WORLD_EXCHANGE_ADENA_FEE;
	public static long WORLD_EXCHANGE_MAX_ADENA_FEE;
	public static boolean WORLD_EXCHANGE_LAZY_UPDATE;
	public static int WORLD_EXCHANGE_ITEM_SELL_PERIOD;
	public static int WORLD_EXCHANGE_ITEM_BACK_PERIOD;
	public static int WORLD_EXCHANGE_PAYMENT_TAKE_PERIOD;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(WORLD_EXCHANGE_FILE);
		ENABLE_WORLD_EXCHANGE = config.getBoolean("EnableWorldExchange", true);
		WORLD_EXCHANGE_DEFAULT_LANG = config.getString("WorldExchangeDefaultLanguage", "en");
		WORLD_EXCHANGE_SAVE_INTERVAL = config.getLong("BidItemsIntervalStatusCheck", 30000);
		WORLD_EXCHANGE_LCOIN_TAX = config.getDouble("LCoinFee", 0.05);
		WORLD_EXCHANGE_MAX_LCOIN_TAX = config.getLong("MaxLCoinFee", 20000);
		WORLD_EXCHANGE_ADENA_FEE = config.getDouble("AdenaFee", 10000.0);
		WORLD_EXCHANGE_MAX_ADENA_FEE = config.getLong("MaxAdenaFee", -1);
		WORLD_EXCHANGE_LAZY_UPDATE = config.getBoolean("DBLazy", false);
		WORLD_EXCHANGE_ITEM_SELL_PERIOD = config.getInt("ItemSellPeriod", 14);
		WORLD_EXCHANGE_ITEM_BACK_PERIOD = config.getInt("ItemBackPeriod", 120);
		WORLD_EXCHANGE_PAYMENT_TAKE_PERIOD = config.getInt("PaymentTakePeriod", 120);
	}
}
