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
package instances.DreamDungeon.DraconidFortress;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author Index
 */
public class RedDraconidTraitor extends Script
{
	public static final int RED_DRACONIT_TRAITOR_NPC_ID = 34315;
	
	private RedDraconidTraitor()
	{
		addFirstTalkId(RED_DRACONIT_TRAITOR_NPC_ID);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance instance = ((player == null) || (npc == null)) ? null : player.getInstanceWorld();
		if ((instance == null) || (instance.getTemplateId() != DraconidFortress.INSTANCE_ID))
		{
			return super.onFirstTalk(npc, player);
		}
		
		switch (instance.getStatus())
		{
			case DraconidFortress.CREATED:
			{
				instance.setStatus(DraconidFortress.KILL_FOUR_DREAM_WATCHERS_INSIDE_FIRST_TIME);
				break;
			}
			case DraconidFortress.TALK_WITH_TRAITOR_FIRST_TIME:
			{
				instance.setStatus(DraconidFortress.TALK_WITH_TRAITOR_SECOND_TIME);
				break;
			}
			case DraconidFortress.TALK_WITH_TRAITOR_SECOND_TIME:
			{
				instance.setStatus(DraconidFortress.KILL_FOUR_DREAM_WATCHERS_INSIDE_SECOND_TIME);
				break;
			}
			case DraconidFortress.FINISH_INSTANCE:
			{
				instance.destroy();
				break;
			}
		}
		
		return super.onFirstTalk(npc, player);
	}
	
	public static void main(String[] args)
	{
		new RedDraconidTraitor();
	}
}
