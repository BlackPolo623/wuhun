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
package events.UniqueGacha;

import java.io.File;
import java.math.BigDecimal;
import java.util.logging.Level;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.managers.events.UniqueGachaManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.UniqueGachaRank;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;
import org.l2jmobius.gameserver.model.script.Quest;

public class UniqueGacha extends LongTimeEvent implements IXmlReader
{
	public UniqueGacha()
	{
		if (isEventPeriod())
		{
			load();
		}
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/scripts/events/UniqueGacha/rewards.xml");
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("RELOAD"))
		{
			load();
		}
		
		return null;
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		final UniqueGachaManager manager = UniqueGachaManager.getInstance();
		forEach(document, "list", listNode ->
		{
			final StatSet paramSet = new StatSet();
			paramSet.set("isActive", isEventPeriod());
			paramSet.set("activeUntilPeriod", getEndDate().getTime());
			forEach(listNode, "configuration", configurationNode ->
			{
				forEach(configurationNode, "param", paramNode ->
				{
					final StatSet forParamSet = new StatSet(parseAttributes(paramNode));
					if (!forParamSet.isEmpty())
					{
						final String paramName = forParamSet.getString("name");
						final String paramValue = forParamSet.getString("value");
						paramSet.set(paramName, paramValue);
					}
				});
			});
			forEach(listNode, "rewards", rewardsNode ->
			{
				forEach(rewardsNode, "reward", rewardNode ->
				{
					final UniqueGachaRank rank = UniqueGachaRank.valueOf(new StatSet(parseAttributes(rewardNode)).getString("rank"));
					forEach(rewardNode, "item", itemNode ->
					{
						final StatSet itemSet = new StatSet(parseAttributes(itemNode));
						final int itemId = itemSet.getInt("id");
						final long count = Long.parseLong(itemSet.getString("count", "1").replaceAll("_", ""));
						final int chance = parseDoubleWithoutPoint(itemSet.getString("chance"), "0", UniqueGachaManager.MINIMUM_CHANCE_AFTER_DOT);
						if (chance == 1)
						{
							System.currentTimeMillis();
						}
						
						final int enchantLevel = itemSet.getInt("enchantLevel", 0);
						manager.addReward(rank, itemId, count, chance, enchantLevel);
					});
				});
			});
			forEach(listNode, "roll", rollNode ->
			{
				paramSet.set("currencyItemId", new StatSet(parseAttributes(rollNode)).getInt("currencyItemId", 57));
				forEach(rollNode, "game", gameNode ->
				{
					final StatSet gameSet = new StatSet(parseAttributes(gameNode));
					final int gameCount = gameSet.getInt("gameCount");
					final long count = Long.parseLong(gameSet.getString("costCount", "1").replaceAll("_", ""));
					manager.addGameCost(gameCount, count);
				});
			});
			manager.recalculateChances();
			manager.setParameters(paramSet);
		});
	}
	
	public static int parseDoubleWithoutPoint(String value, String defaultValue, int scaleTo)
	{
		return parseValue(value, new BigDecimal(defaultValue)).scaleByPowerOfTen(scaleTo).intValue();
	}
	
	public static BigDecimal parseValue(String val, BigDecimal def)
	{
		try
		{
			return (val == null) || val.isEmpty() ? def : new BigDecimal(val);
		}
		catch (Exception e)
		{
			Quest.LOGGER.log(Level.WARNING, "Value parse error, value type: double, input data: " + val, e);
		}
		
		return def;
	}
	
	public static void main(String[] args)
	{
		new UniqueGacha();
	}
}
