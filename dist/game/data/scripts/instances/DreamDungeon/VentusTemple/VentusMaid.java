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
package instances.DreamDungeon.VentusTemple;

import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author Index
 */
public class VentusMaid extends Script
{
	public static final int VENTUS_MAID_NPC_ID = 34314;
	
	private VentusMaid()
	{
		addFirstTalkId(VENTUS_MAID_NPC_ID);
		addCreatureSeeId(VENTUS_MAID_NPC_ID);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance instance = ((player == null) || (npc == null)) ? null : player.getInstanceWorld();
		if ((instance == null) || (instance.getTemplateId() != VentusTemple.INSTANCE_ID))
		{
			return super.onFirstTalk(npc, player);
		}
		
		switch (instance.getStatus())
		{
			case VentusTemple.TALK_WITH_VANTUS_MAID_ON_CREATE:
			{
				instance.setStatus(VentusTemple.KILL_FOUR_DREAM_WATCHERS_INSIDE);
				break;
			}
			case VentusTemple.TALK_WITH_VENTUS_MAID_FOR_RESPAWN_BALLISTA:
			{
				instance.setStatus(VentusTemple.SHOOT_FROM_BALLISTA);
				break;
			}
			case VentusTemple.FINISH_INSTANCE:
			{
				instance.destroy();
				break;
			}
		}
		
		return super.onFirstTalk(npc, player);
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (((creature == null) || !creature.isPlayer() || (npc == null)))
		{
			return;
		}
		
		final Instance instance = creature.getInstanceWorld();
		if ((instance == null) || (instance.getTemplateId() != VentusTemple.INSTANCE_ID))
		{
			return;
		}
		
		if (instance.getStatus() == VentusTemple.GO_TO_VENTUS_ROOM)
		{
			npc.setUndying(true);
			instance.setStatus(VentusTemple.SAVE_VENTUS_MAID_IN_VENTUS_ROOM);
		}
	}
	
	public static void main(String[] args)
	{
		new VentusMaid();
	}
}
