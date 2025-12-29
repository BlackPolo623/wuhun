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
package ai.areas.PrimevalIsle;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author petryxa
 */
public class SayhaBuff extends Script
{
	// NPCs
	private static final int[] MONSTERS =
	{
		21962, // Wild Strider
		21963, // Elroki
		21964, // Pachycephalosaurus
		21966, // Ornithomimus
		21968, // Ornithomimus
		21969, // Deinonychus
		21971, // Velociraptor
		21974, // Ornithomimus
		21976, // Deinonychus
		21978, // Tyrannosaurus
		22056, // Pachycephalosaurus
		22057, // Strider
		22058, // Ornithomimus
		22059, // Pterosaur
	};
	
	// Skill
	private static final SkillHolder BENEFACTION_OF_BLUE_HAWK = new SkillHolder(50123, 1);
	
	private SayhaBuff()
	{
		addKillId(MONSTERS);
	}
	
	@Override
	public void onKill(Npc npc, Player attacker, boolean isSummon)
	{
		if (!attacker.isAffectedBySkill(BENEFACTION_OF_BLUE_HAWK))
		{
			BENEFACTION_OF_BLUE_HAWK.getSkill().applyEffects(attacker, attacker);
		}
	}
	
	public static void main(String[] args)
	{
		new SayhaBuff();
	}
}
