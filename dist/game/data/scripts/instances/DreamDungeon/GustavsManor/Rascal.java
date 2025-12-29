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
package instances.DreamDungeon.GustavsManor;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author Index
 */
public class Rascal extends Script
{
	private static final int RASCAL_NPC_ID = 34312;
	
	// NPC STRINGS
	private static final NpcStringId STRING_ID_01 = NpcStringId.MASTER_GUSTAV_TOOK_MY_HORSIE;
	private static final NpcStringId STRING_ID_02 = NpcStringId.THE_HORSIE_IS_IN_THE_MANOR_I_WILL_OPEN_THE_DOORS_AND_WE_LL_GO_IN_AND_PLAY;
	
	private Rascal()
	{
		addFirstTalkId(RASCAL_NPC_ID);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance instance = ((player == null) || (npc == null)) ? null : player.getInstanceWorld();
		if ((instance == null) || (instance.getTemplateId() != GustavsManor.INSTANCE_ID))
		{
			return super.onFirstTalk(npc, player);
		}
		
		switch (instance.getStatus())
		{
			case GustavsManor.CREATED:
			{
				instance.setStatus(GustavsManor.GO_TO_GATES_AND_KILL_GIRL);
				
				// Master Gustav took my horsie.
				instance.broadcastPacket(new ExShowScreenMessage(STRING_ID_01, ExShowScreenMessage.TOP_CENTER, 10000, true));
				break;
			}
			case GustavsManor.GO_TO_GATES_AND_KILL_GIRL:
			{
				return super.onFirstTalk(npc, player);
			}
			case GustavsManor.TALK_WITH_RASCAL:
			{
				instance.setStatus(GustavsManor.KILL_FOUR_DREAM_WATCHERS_INSIDE);
				
				// The horsie is in the manor. I will open the doors and we'll go in and play.
				instance.broadcastPacket(new ExShowScreenMessage(STRING_ID_02, ExShowScreenMessage.TOP_CENTER, 10000, true));
				break;
			}
			case GustavsManor.FINISH_INSTANCE:
			{
				// instance.getPlayers().forEach(p -> p.sendPacket(new TimedHuntingZoneExit(0)));
				instance.destroy();
				break;
			}
		}
		
		return super.onFirstTalk(npc, player);
	}
	
	public static void main(String[] args)
	{
		new Rascal();
	}
}
