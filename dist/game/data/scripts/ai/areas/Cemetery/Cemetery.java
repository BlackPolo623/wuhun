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
package ai.areas.Cemetery;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Cave Maiden, Keeper AI.
 * @author proGenitor
 */
public class Cemetery extends Script
{
	// Monsters
	private static final int SOUL_OF_RUINS = 21000;
	private static final int ROVING_SOUL = 20999;
	private static final int CRUEL_PUNISHER = 20998;
	private static final int SOLDIER_OF_GRIEF = 20997;
	private static final int SPITEFUL_GHOST_OF_RUINS = 20996;
	private static final int TORTURED_UNDEAD = 20678;
	private static final int TAIRIM = 20675;
	private static final int TAIK_ORC_SUPPLY_OFFICER = 20669;
	private static final int GRAVE_GUARD = 20668;
	private static final int TAIK_ORC_WATCHMAN = 20666;
	
	// Guard
	private static final int GRAVE_WARDEN = 22128;
	
	private Cemetery()
	{
		addKillId(SOUL_OF_RUINS, ROVING_SOUL, CRUEL_PUNISHER, SOLDIER_OF_GRIEF, SPITEFUL_GHOST_OF_RUINS, TORTURED_UNDEAD, TAIRIM, TAIK_ORC_SUPPLY_OFFICER, GRAVE_GUARD, TAIK_ORC_WATCHMAN);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getRandom(100) < 10)
		{
			final Npc spawnBanshee = addSpawn(GRAVE_WARDEN, npc, false, 300000);
			final Playable attacker = isSummon ? killer.getServitors().values().stream().findFirst().orElse(killer.getPet()) : killer;
			addAttackPlayerDesire(spawnBanshee, attacker);
			npc.deleteMe();
		}
	}
	
	public static void main(String[] args)
	{
		new Cemetery();
	}
}
