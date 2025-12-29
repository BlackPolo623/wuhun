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
package ai.areas.SilentValley;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

public class SilentValley extends Script
{
	// Monsters
	private static final int CREATURE_OF_THE_PAST = 20967;
	private static final int FORGOTTEN_FACE = 20968;
	private static final int GIANT_SHADOW = 20969;
	private static final int WARRIOR_OF_ANCIENT_TIMES = 20970;
	private static final int SHAMAN_OF_ANCIENT_TIMES = 20971;
	private static final int FORGOTTEN_ANCIENT_CREATURE = 20972;
	
	// Guard
	private static final int ANCIENT_GUARDIAN = 22106;
	
	private SilentValley()
	{
		addKillId(CREATURE_OF_THE_PAST, FORGOTTEN_FACE, GIANT_SHADOW, WARRIOR_OF_ANCIENT_TIMES, SHAMAN_OF_ANCIENT_TIMES, FORGOTTEN_ANCIENT_CREATURE);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getRandom(100) < 10)
		{
			final Npc spawnBanshee = addSpawn(ANCIENT_GUARDIAN, npc, false, 300000);
			final Playable attacker = isSummon ? killer.getServitors().values().stream().findFirst().orElse(killer.getPet()) : killer;
			addAttackPlayerDesire(spawnBanshee, attacker);
			npc.deleteMe();
		}
	}
	
	public static void main(String[] args)
	{
		new SilentValley();
	}
}
