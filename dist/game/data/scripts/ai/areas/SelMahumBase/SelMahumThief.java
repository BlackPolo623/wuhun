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
package ai.areas.SelMahumBase;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author petryxa
 */
public class SelMahumThief extends Script
{
	// NPCs
	private static final int THIEF = 22241; // Sel Mahum Thief
	private static final Set<Integer> MONSTERS = new HashSet<>();
	static
	{
		MONSTERS.add(22239);
		MONSTERS.add(22238);
		MONSTERS.add(22240);
		MONSTERS.add(22237);
		MONSTERS.add(22254);
	}
	
	// Misc
	private static final long DESPAWN_TIME = 5 * 60 * 1000; // 5 min
	
	private SelMahumThief()
	{
		addKillId(MONSTERS);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getRandom(100) < 20)
		{
			final Npc spawn = addSpawn(THIEF, npc.getLocation(), false, DESPAWN_TIME);
			addAttackPlayerDesire(spawn, killer);
		}
	}
	
	public static void main(String[] args)
	{
		new SelMahumThief();
	}
}
