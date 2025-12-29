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
package ai.areas.UndergroundLabyrinth;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author Mobius
 * @URL https://l2central.info/essence/articles/1440.html?lang=en
 */
public class UndergroundLabyrinthMonsters extends Script
{
	// NPCs
	private static final int[] MONSTERS =
	{
		22541, // Labyrinth Executioner
		22542, // Lost Watchman
		22543, // Labyrinth Leader
		22544, // Herald of Purification
	};
	
	// Item
	private static final int MARK_OF_REPENTANCE = 97783;
	
	private UndergroundLabyrinthMonsters()
	{
		addKillId(MONSTERS);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		giveItems(killer.asPlayer(), MARK_OF_REPENTANCE, 1);
	}
	
	public static void main(String[] args)
	{
		new UndergroundLabyrinthMonsters();
	}
}
