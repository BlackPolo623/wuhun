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
package ai.areas.FieldofMassacre;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Field of Massacre AI.
 * @author QuangNguyen
 */
public class FieldOfMassacre extends Script
{
	// Monsters
	private static final int DOOM_KNIGHT = 20674;
	private static final int ACHER_OF_DESTRUCTION = 21001;
	private static final int DOOM_SCOUT = 21002;
	private static final int GRAVEYARD_LICH = 21003;
	private static final int DISMAL_OAK = 21004;
	private static final int GRAVEYARD_PREDATOR = 21005;
	private static final int DOOM_SERVANT = 21006;
	private static final int DOOM_GUARD = 21007;
	private static final int DOOM_ARCHER = 21008;
	private static final int DOOM_TROOPER = 21009;
	private static final int DOOM_WARRIOR = 21010;
	
	// Guard
	private static final int GUARD_BUTCHER = 22101;
	
	private FieldOfMassacre()
	{
		addKillId(ACHER_OF_DESTRUCTION, GRAVEYARD_LICH, DISMAL_OAK, GRAVEYARD_PREDATOR, DOOM_KNIGHT, DOOM_SCOUT, DOOM_SERVANT, DOOM_GUARD, DOOM_ARCHER, DOOM_TROOPER, DOOM_WARRIOR);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getRandom(100) < 10)
		{
			final Npc spawnBanshee = addSpawn(GUARD_BUTCHER, npc, false, 300000);
			final Playable attacker = isSummon ? killer.getServitors().values().stream().findFirst().orElse(killer.getPet()) : killer;
			addAttackPlayerDesire(spawnBanshee, attacker);
			npc.deleteMe();
		}
	}
	
	public static void main(String[] args)
	{
		new FieldOfMassacre();
	}
}
