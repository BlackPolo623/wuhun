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
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * This class loads all the prison related configurations.
 * @author Mobius
 */
public class PrisonConfig
{
	// File
	private static final String PRISON_CONFIG_FILE = "./config/Prison.ini";
	
	// Constants
	public static boolean ENABLE_PRISON;
	public static long REPUTATION_FOR_ZONE_1;
	public static int PK_FOR_ZONE_1;
	public static int PK_FOR_ZONE_2;
	public static long SENTENCE_TIME_ZONE_1;
	public static long SENTENCE_TIME_ZONE_2;
	public static Location ENTRANCE_LOC_ZONE_1;
	public static Location ENTRANCE_LOC_ZONE_2;
	public static Location RELEASE_LOC_ZONE_1;
	public static Location RELEASE_LOC_ZONE_2;
	public static int MARK_RELEASE_AMOUNT;
	public static long LCOIN_RELEASE_AMOUNT;
	public static int REP_POINTS_RECEIVED_BY_ZONE_1;
	public static int REP_POINTS_RECEIVED_BY_ZONE_2;
	public static ItemHolder BAIL_ZONE_1;
	public static ItemHolder BAIL_ZONE_2;
	public static ItemHolder DONATION_BAIL_ZONE_1;
	public static ItemHolder DONATION_BAIL_ZONE_2;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(PRISON_CONFIG_FILE);
		ENABLE_PRISON = config.getBoolean("Enable", false);
		REPUTATION_FOR_ZONE_1 = config.getLong("ReputationForZone1", -150000L);
		PK_FOR_ZONE_1 = config.getInt("PKsForZone1", 1);
		PK_FOR_ZONE_2 = config.getInt("PKsForZone2", 50);
		SENTENCE_TIME_ZONE_1 = config.getLong("SentenceTimeZone1", 1440) * 60000L;
		SENTENCE_TIME_ZONE_2 = config.getLong("SentenceTimeZone2", 4320) * 60000L;
		final String[] entrance1 = config.getString("EntranceLocZone1", "-77998,-52649,-11494").split(",");
		ENTRANCE_LOC_ZONE_1 = new Location(Integer.parseInt(entrance1[0]), Integer.parseInt(entrance1[1]), Integer.parseInt(entrance1[2]));
		final String[] entrance2 = config.getString("EntranceLocZone2", "-77998,-52649,-11494").split(",");
		ENTRANCE_LOC_ZONE_2 = new Location(Integer.parseInt(entrance2[0]), Integer.parseInt(entrance2[1]), Integer.parseInt(entrance2[2]));
		final String[] release1 = config.getString("ReleaseLocZone1", "83401,148645,-3380").split(",");
		RELEASE_LOC_ZONE_1 = new Location(Integer.parseInt(release1[0]), Integer.parseInt(release1[1]), Integer.parseInt(release1[2]));
		final String[] release2 = config.getString("ReleaseLocZone2", "83401,148645,-3380").split(",");
		RELEASE_LOC_ZONE_2 = new Location(Integer.parseInt(release2[0]), Integer.parseInt(release2[1]), Integer.parseInt(release2[2]));
		MARK_RELEASE_AMOUNT = config.getInt("MarkReleaseAmount", 100);
		LCOIN_RELEASE_AMOUNT = config.getLong("LCoinReleaseAmount", 10000L);
		REP_POINTS_RECEIVED_BY_ZONE_1 = config.getInt("RepPointsReceivedByZone1", 100);
		REP_POINTS_RECEIVED_BY_ZONE_2 = config.getInt("RepPointsReceivedByZone2", 200);
		final String[] bail1 = config.getString("BailZone1", "57,2000000").split(",");
		BAIL_ZONE_1 = new ItemHolder(Integer.parseInt(bail1[0]), Long.parseLong(bail1[1]));
		final String[] bail2 = config.getString("BailZone2", "82402,108").split(",");
		BAIL_ZONE_2 = new ItemHolder(Integer.parseInt(bail2[0]), Long.parseLong(bail2[1]));
		final String[] donationBailZone1 = config.getString("DonationBailZone1", "57,1000000000").split(",");
		DONATION_BAIL_ZONE_1 = new ItemHolder(Integer.parseInt(donationBailZone1[0]), Long.parseLong(donationBailZone1[1]));
		final String[] donationBailZone2 = config.getString("DonationBailZone2", "57,1500000000").split(",");
		DONATION_BAIL_ZONE_2 = new ItemHolder(Integer.parseInt(donationBailZone2[0]), Long.parseLong(donationBailZone2[1]));
	}
}
