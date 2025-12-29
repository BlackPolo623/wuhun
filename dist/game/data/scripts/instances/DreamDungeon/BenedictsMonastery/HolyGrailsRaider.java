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
package instances.DreamDungeon.BenedictsMonastery;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author Index
 */
public class HolyGrailsRaider extends Script
{
	private static final int HOLY_GRAILS_RIDER = 34313;
	
	private HolyGrailsRaider()
	{
		addFirstTalkId(HOLY_GRAILS_RIDER);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance instance = ((player == null) || (npc == null)) ? null : player.getInstanceWorld();
		if ((instance == null) || (instance.getTemplateId() != BenedictsMonastery.INSTANCE_ID))
		{
			return super.onFirstTalk(npc, player);
		}
		
		switch (instance.getStatus())
		{
			case BenedictsMonastery.TALK_WITH_HOLY_GRAILS_RAIDER_NPC_ON_START:
			{
				instance.setStatus(BenedictsMonastery.KILL_FOUR_DREAM_WATCHERS_INSIDE);
				break;
			}
			case BenedictsMonastery.TALK_WITH_HOLY_GRAILS_RAIDER_NPC_TO_OPEN_DOORS:
			{
				instance.setStatus(BenedictsMonastery.TALK_WITH_HOLY_GRAIL_BEFORE_BOSS);
				break;
			}
			case BenedictsMonastery.FINISH_INSTANCE:
			{
				instance.destroy();
				break;
			}
		}
		
		return super.onFirstTalk(npc, player);
	}
	
	public static void main(String[] args)
	{
		new HolyGrailsRaider();
	}
}
