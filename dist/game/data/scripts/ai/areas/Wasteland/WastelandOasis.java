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
package ai.areas.Wasteland;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Wasteland Oasis AI.
 * @URL https://l2central.info/essence/articles/2491.html
 * @author Mobius
 */
public class WastelandOasis extends Script
{
	// NPCs
	private static final int OASIS_CREATURE = 22918;
	private static final int VANDER = 22928;
	private static final int ELITE_RAIDER = 22923;
	private static final int ARCHON_OF_DARKNESS = 22924;
	private static final int ASSASSIN_OF_DARKNESS = 22927;
	
	private WastelandOasis()
	{
		addKillId(OASIS_CREATURE, ARCHON_OF_DARKNESS, ASSASSIN_OF_DARKNESS);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getRandom(10) == 0) // 10%
		{
			if (npc.getId() == OASIS_CREATURE)
			{
				final Npc vander = addSpawn(VANDER, npc);
				final Playable attacker = isSummon ? killer.getServitors().values().stream().findFirst().orElse(killer.getPet()) : killer;
				addAttackPlayerDesire(vander, attacker);
				npc.deleteMe();
			}
			else
			{
				final Npc eliteRaider = addSpawn(ELITE_RAIDER, npc);
				final Playable attacker = isSummon ? killer.getServitors().values().stream().findFirst().orElse(killer.getPet()) : killer;
				addAttackPlayerDesire(eliteRaider, attacker);
				npc.deleteMe();
			}
		}
	}
	
	public static void main(String[] args)
	{
		new WastelandOasis();
	}
}
