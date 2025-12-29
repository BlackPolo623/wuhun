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
package ai.areas.WarTornPlains;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Cave Maiden, Keeper AI.
 * @author proGenitor
 */
public class WarTornPlains extends Script
{
	// Monsters
	private static final int GRAVEYARD_WANDERER = 20659;
	private static final int ARCHER_OF_GREED = 20660;
	private static final int HATAR_RATMAN_THIEF = 20661;
	private static final int HATAR_RATMAN_BOSS = 20662;
	private static final int HATAR_HANISHEE = 20663;
	private static final int DEMONIC_EYE = 20664;
	private static final int TAIK_ORC_ELDER = 20665;
	private static final int FARCRAN = 20667;
	
	// Guard
	private static final int FIERCE_GUARD = 22103;
	
	private WarTornPlains()
	{
		addKillId(GRAVEYARD_WANDERER, ARCHER_OF_GREED, HATAR_RATMAN_THIEF, HATAR_RATMAN_BOSS, HATAR_HANISHEE, DEMONIC_EYE, TAIK_ORC_ELDER, FARCRAN);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getRandom(100) < 10)
		{
			final Npc spawnBanshee = addSpawn(FIERCE_GUARD, npc, false, 300000);
			final Playable attacker = isSummon ? killer.getServitors().values().stream().findFirst().orElse(killer.getPet()) : killer;
			addAttackPlayerDesire(spawnBanshee, attacker);
			npc.deleteMe();
		}
	}
	
	public static void main(String[] args)
	{
		new WarTornPlains();
	}
}
