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
package events.LetterCollector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.managers.events.LetterCollectorManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExLetterCollectorUI;

/**
 * @author Mobius
 */
public class LetterCollector extends LongTimeEvent implements IXmlReader
{
	private LetterCollector()
	{
		if (isEventPeriod())
		{
			load();
			LetterCollectorManager.getInstance().init();
		}
	}
	
	public void reloadRewards()
	{
		LetterCollectorManager.getInstance().resetField();
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/scripts/events/LetterCollector/rewards.xml");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		final AtomicInteger minimumLevel = new AtomicInteger();
		final AtomicInteger maximumLevel = new AtomicInteger();
		final Map<String, Integer> letters = new HashMap<>();
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "params", paramNode ->
			{
				forEach(paramNode, "minimum", minimumLevelNode -> minimumLevel.set(new StatSet(parseAttributes(minimumLevelNode)).getInt("level")));
				forEach(paramNode, "maximum", maximumLevelNode -> maximumLevel.set(new StatSet(parseAttributes(maximumLevelNode)).getInt("level")));
			});
			forEach(listNode, "letters", letterNode -> forEach(letterNode, "item", itemNode ->
			{
				StatSet letterSet = new StatSet(parseAttributes(itemNode));
				letters.put(letterSet.getString("name"), letterSet.getInt("id"));
			}));
			forEach(listNode, "reward", rewardNode ->
			{
				final int id = new StatSet(parseAttributes(rewardNode)).getInt("id");
				final List<ItemHolder> word = new ArrayList<>();
				final List<ItemChanceHolder> rewards = new ArrayList<>();
				final AtomicBoolean needToSumAllChance = new AtomicBoolean(false);
				final AtomicReference<Double> chanceSum = new AtomicReference<>(0d);
				forEach(rewardNode, "word", wordNode ->
				{
					String[] letter = wordNode.getTextContent().trim().split(";");
					for (String token : letter)
					{
						int count = 1;
						for (ItemHolder check : word)
						{
							if (check.getId() == letters.get(token))
							{
								count = Math.toIntExact(check.getCount() + 1);
								word.remove(check);
								break;
							}
						}
						
						word.add(new ItemHolder(letters.get(token), count));
					}
				});
				forEach(rewardNode, "rewards", rewardsNode ->
				{
					needToSumAllChance.set(new StatSet(parseAttributes(rewardsNode)).getBoolean("sumChances"));
					forEach(rewardsNode, "item", itemNode ->
					{
						StatSet itemSet = new StatSet(parseAttributes(itemNode));
						final double chance = itemSet.getDouble("chance");
						if (needToSumAllChance.get())
						{
							chanceSum.set(chanceSum.get() + chance);
						}
						
						rewards.add(new ItemChanceHolder(itemSet.getInt("id"), chance, itemSet.getLong("count"), (byte) itemSet.getInt("enchantLevel", 0)));
					});
				});
				final LetterCollectorManager lcm = LetterCollectorManager.getInstance();
				lcm.addWords(id, word);
				lcm.addRewards(id, new LetterCollectorManager.LetterCollectorRewardHolder(rewards, chanceSum.get() == 0d ? 100d : chanceSum.get()));
				lcm.setLetters(letters);
				lcm.setMinLevel(minimumLevel.get());
				lcm.setMaxLevel(maximumLevel.get());
			});
		});
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		if (!isEventPeriod())
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if (player != null)
		{
			player.sendPacket(new ExLetterCollectorUI(player));
		}
	}
	
	public static void main(String[] args)
	{
		new LetterCollector();
	}
}
