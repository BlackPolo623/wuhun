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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the Aden Laboratory related configurations.
 * @author Mobius
 */
public class AdenLaboratoryConfig
{
	private static final Logger LOGGER = Logger.getLogger(AdenLaboratoryConfig.class.getName());
	
	// File
	private static final String ADENLAB_CONFIG_FILE = "./config/AdenLaboratory.ini";
	
	// Constants
	public static boolean ADENLAB_ENABLED;
	public static Map<Integer, Object[]> ADENLAB_NORMAL_ROLL_FEE_TYPE_CACHE;
	public static long ADENLAB_NORMAL_ADENA_FEE_AMOUNT;
	public static long ADENLAB_SPECIAL_RESEARCH_FEE;
	public static long ADENLAB_SPECIAL_CONFIRM_FEE;
	public static Map<Integer, Object[]> ADENLAB_INCREDIBLE_ROLL_FEE_TYPE_CACHE;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(ADENLAB_CONFIG_FILE);
		ADENLAB_ENABLED = config.getBoolean("AdenLabEnabled", true);
		
		// Normal stages have Aden's Exploration Report (all 3 types) + Adena fees.
		final String[] normalFeeType = config.getString("NormalItemFeeList", "101588,1;101567,1;101572,1").split(";"); // [IDX1,AMOUNT1;IDX2,AMOUNT2]
		ADENLAB_NORMAL_ROLL_FEE_TYPE_CACHE = new HashMap<>(normalFeeType.length);
		int adenLabCounter = 0;
		for (String feeType : normalFeeType)
		{
			if (!feeType.isEmpty())
			{
				try
				{
					final String[] tempString = feeType.split(",", 2); // Must have exactly 2 elements.
					if (tempString.length < 2)
					{
						LOGGER.warning("AdenLab: Incorrect fee type format `" + feeType + "`. Skipping.");
						continue;
					}
					
					final int itemId = Integer.parseInt(tempString[0].trim().replace("_", ""));
					final long amount = Long.parseLong(tempString[1].trim().replace("_", ""));
					ADENLAB_NORMAL_ROLL_FEE_TYPE_CACHE.put(adenLabCounter++, new Object[]
					{
						itemId,
						amount
					});
				}
				catch (NumberFormatException e)
				{
					LOGGER.warning("AdenLab: Invalid fee type structure `" + feeType + "`. Skipping.");
				}
			}
		}
		ADENLAB_NORMAL_ADENA_FEE_AMOUNT = config.getLong("NormalAdenaFeeAmount", 10000000); // *0.1 for Essence
		
		// Special stages only have adena fee.
		ADENLAB_SPECIAL_RESEARCH_FEE = config.getLong("SpecialResearchAdenaFeeAmount", 10000000); // *0.1 for Essence
		ADENLAB_SPECIAL_CONFIRM_FEE = config.getLong("SpecialConfirmAdenaFeeAmount", 200000000); // *0.1 for Essence
		
		// Incredible stages have Transcendent Upgrade Stone (all 3 types) as fee options.
		final String[] incredibleFeeType = config.getString("IncredibleItemFeeList", "98039,1;98040,1;100602,1").split(";");
		ADENLAB_INCREDIBLE_ROLL_FEE_TYPE_CACHE = new HashMap<>(incredibleFeeType.length);
		adenLabCounter = 0;
		for (String feeType : incredibleFeeType)
		{
			if (!feeType.isEmpty())
			{
				try
				{
					// Split with limit to ensure only two elements: itemId and amount.
					final String[] tempString = feeType.split(",", 2);
					
					// Ensure we have exactly two parts: itemId and amount.
					if (tempString.length == 2)
					{
						final int itemId = Integer.parseInt(tempString[0].trim().replace("_", "")); // Parse itemId
						final long amount = Long.parseLong(tempString[1].trim().replace("_", "")); // Parse amount
						
						// Store fee data in cache.
						final Object[] tempFee = new Object[]
						{
							itemId,
							amount
						};
						ADENLAB_INCREDIBLE_ROLL_FEE_TYPE_CACHE.put(adenLabCounter++, tempFee);
					}
					else
					{
						LOGGER.warning("AdenLab: Invalid fee structure for entry `" + feeType + "`. Skipping.");
					}
				}
				catch (NumberFormatException e)
				{
					LOGGER.warning("AdenLab: Invalid fee type structure for entry `" + feeType + "`. Skipping.");
				}
			}
		}
	}
}
