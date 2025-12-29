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
package ai.others.OlyBuffer;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Olympiad Buffer AI.
 * @author St3eT, Mobius
 */
public class OlyBuffer extends Script
{
	// NPC
	private static final int OLYMPIAD_BUFFER = 36402;
	
	// Skills
	private static final SkillHolder[] ALLOWED_BUFFS =
	{
		new SkillHolder(1086, 1), // Haste - Atk. Spd. +15%.
		new SkillHolder(1085, 1), // Acumen - Casting Spd. +15%
		new SkillHolder(1204, 1), // Wind Walk - Speed +20.
		new SkillHolder(1068, 1), // Might - P. Atk. +8%.
		new SkillHolder(1040, 1), // Shield - P. Def. +8%.
		new SkillHolder(1036, 1), // Magic Barrier - M. Def. +23%.
		new SkillHolder(1045, 1), // Blessed Body - Max HP +10%.
		new SkillHolder(1048, 1), // Blessed Soul - Max MP +10%.
		new SkillHolder(1062, 1), // Berserker Spirit - P. Def. -5%, M. Def. -10%, P. Evasion -2, P. Atk. +5%, M. Atk. +10%, Atk. Spd. +5%, Casting Spd. +5% and Speed +5.
	};
	
	private OlyBuffer()
	{
		if (OlympiadConfig.OLYMPIAD_ENABLED)
		{
			addStartNpc(OLYMPIAD_BUFFER);
			addFirstTalkId(OLYMPIAD_BUFFER);
			addTalkId(OLYMPIAD_BUFFER);
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (npc.getScriptValue() < 5)
		{
			htmltext = "OlyBuffer-index.html";
		}
		
		return htmltext;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (event.startsWith("giveBuff;") && (npc.getScriptValue() < 5))
		{
			final int buffId = Integer.parseInt(event.replace("giveBuff;", ""));
			if (ALLOWED_BUFFS[buffId] != null)
			{
				npc.setScriptValue(npc.getScriptValue() + 1);
				final Skill skill = ALLOWED_BUFFS[buffId].getSkill();
				player.sendPacket(new MagicSkillUse(npc, player, skill.getDisplayId(), skill.getLevel(), 0, 0));
				skill.applyEffects(npc, player);
				htmltext = "OlyBuffer-afterBuff.html";
			}
			
			if (npc.getScriptValue() >= 5)
			{
				htmltext = "OlyBuffer-noMore.html";
				getTimers().addTimer("DELETE_ME", 5000, evnt -> npc.deleteMe());
			}
		}
		
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new OlyBuffer();
	}
}
