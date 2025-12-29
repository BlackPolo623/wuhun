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
package ai.areas.PlainsOfGlory;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Plains of Glory AI.
 * @author QuangNguyen
 */
public class PlainsOfGlory extends Script
{
	// Monsters
	private static final int VANOR_SILENOS = 20681;
	private static final int VANOR_SILENOS_SOLDIER = 20682;
	private static final int VANOR_SILENOS_SCOUT = 20683;
	private static final int VANOR_SILENOS_WARRIOR = 20684;
	private static final int VANOR_SILENOS_SHAMAN = 20685;
	private static final int VANOR_SILENOS_CHIEFTAIN = 20686;
	private static final int VANOR = 24014;
	
	// Guard
	private static final int GUARD_OF_HONOR = 22102;
	
	private PlainsOfGlory()
	{
		addKillId(VANOR_SILENOS, VANOR_SILENOS_SOLDIER, VANOR_SILENOS_SCOUT, VANOR_SILENOS_WARRIOR, VANOR_SILENOS_SHAMAN, VANOR_SILENOS_CHIEFTAIN, VANOR);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getRandom(100) < 10)
		{
			final Npc spawnBanshee = addSpawn(GUARD_OF_HONOR, npc, false, 300000);
			final Playable attacker = isSummon ? killer.getServitors().values().stream().findFirst().orElse(killer.getPet()) : killer;
			addAttackPlayerDesire(spawnBanshee, attacker);
			npc.deleteMe();
		}
	}
	
	public static void main(String[] args)
	{
		new PlainsOfGlory();
	}
}
