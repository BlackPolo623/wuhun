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
package events.DefeatTheQueen;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Mobius
 */
public class DefeatTheQueen extends LongTimeEvent
{
	// NPCs
	private static final int ANTRI = 34184;
	private static final int JIO = 34185;
	
	// Skill
	private static final SkillHolder ANT_PROPHECY = new SkillHolder(48200, 1);
	
	// Teleport
	private static final Location ANT_NEST = new Location(-9997, 175533, -4152);
	
	// Misc
	private static final int MIN_LEVEL = 65;
	
	private DefeatTheQueen()
	{
		addStartNpc(ANTRI, JIO);
		addFirstTalkId(ANTRI, JIO);
		addTalkId(ANTRI, JIO);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!isEventPeriod())
		{
			return null;
		}
		
		switch (event)
		{
			case "34184.htm":
			case "34184-1.htm":
			{
				return event;
			}
			case "TeleportToAntNest":
			{
				if ((npc != null) && (npc.getId() == ANTRI))
				{
					player.teleToLocation(ANT_NEST);
				}
				break;
			}
			case "ExitInstance":
			{
				if ((npc != null) && (npc.getId() == JIO) && npc.isInInstance())
				{
					npc.getInstanceWorld().ejectPlayer(player);
				}
				break;
			}
			case "GiveAntProphecy":
			{
				if ((npc != null) && (npc.getId() == JIO) && npc.isInInstance())
				{
					SkillCaster.triggerCast(npc, player, ANT_PROPHECY.getSkill());
				}
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getId() == JIO)
		{
			if (npc.isInInstance())
			{
				return "34185-2.htm";
			}
			
			if (player.getLevel() < MIN_LEVEL)
			{
				return "34185-1.htm";
			}
		}
		
		return npc.getId() + ".htm";
	}
	
	public static void main(String[] args)
	{
		new DefeatTheQueen();
	}
}
