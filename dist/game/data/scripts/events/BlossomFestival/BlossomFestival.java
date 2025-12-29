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
package events.BlossomFestival;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;

/**
 * @author Mobius
 */
public class BlossomFestival extends LongTimeEvent
{
	// NPC
	private static final int AUGUSTINA = 34163;
	
	// Item
	private static final int REWARD = 94448;
	
	// Misc
	private static final String REWARD_VAR = "AUGUSTINA_REWARD_VAR";
	private static final Object REWARD_LOCK = new Object();
	
	private BlossomFestival()
	{
		addFirstTalkId(AUGUSTINA);
		addTalkId(AUGUSTINA);
		addSpawnId(AUGUSTINA);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "34163-02.html":
			case "34163-03.html":
			{
				return event;
			}
			case "reward":
			{
				if (player.getLevel() < 60)
				{
					return "34163-04.html";
				}
				
				synchronized (REWARD_LOCK)
				{
					final long currentTime = System.currentTimeMillis();
					if (player.getVariables().getLong(REWARD_VAR, 0) < currentTime)
					{
						player.getVariables().set(REWARD_VAR, currentTime + 86400000); // 24 hours
						giveItems(player, REWARD, 1);
					}
					else
					{
						return "34163-05.html";
					}
				}
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "34163-01.html";
	}
	
	public static void main(String[] args)
	{
		new BlossomFestival();
	}
}
