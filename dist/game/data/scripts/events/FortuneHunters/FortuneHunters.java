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
package events.FortuneHunters;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Mobius
 */
public class FortuneHunters extends LongTimeEvent
{
	// NPC
	private static final int VERLINS_MESSENGER = 9034;
	
	// Skill
	private static final SkillHolder VERLINS_PROTECTION = new SkillHolder(59853, 1);
	
	// Misc
	private static final String VERLIN_REWARD_VAR = "VERLIN_REWARD_VAR";
	private static final long REWARD_DELAY = 86400000; // 1 day
	
	private FortuneHunters()
	{
		addFirstTalkId(VERLINS_MESSENGER);
		addTalkId(VERLINS_MESSENGER);
		addSpawnId(VERLINS_MESSENGER);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("buff"))
		{
			final long currentTime = System.currentTimeMillis();
			if ((player.getVariables().getLong(VERLIN_REWARD_VAR, 0) - currentTime) > 0)
			{
				return "9034-02.html";
			}
			
			player.getVariables().set(VERLIN_REWARD_VAR, currentTime + REWARD_DELAY);
			SkillCaster.triggerCast(npc, player, VERLINS_PROTECTION.getSkill());
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "9034-01.html";
	}
	
	public static void main(String[] args)
	{
		new FortuneHunters();
	}
}
