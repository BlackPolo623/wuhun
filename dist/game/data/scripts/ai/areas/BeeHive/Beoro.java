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
package ai.areas.BeeHive;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.AggroInfo;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import ai.others.Atingo;

public class Beoro extends Script
{
	private static final int BEORO = 25948;
	private static final int HERMIT_BEORO = 25949;
	private static final int ROYDA = 25950;
	private static final int SIN_EATER = 25951;
	private static final NpcStringId[] ANNOUNCEMENTS_50_HP =
	{
		NpcStringId.I_WILL_FIGHT_TO_THE_DEATH,
		NpcStringId.DON_T_FORGET_THAT_BEE_HIVE_IS_A_TRAINING_GROUND_FOR_PETS_IF_YOU_RE_NOT_A_PET_DON_T_COME,
		NpcStringId.I_AM_THE_RULER_OF_BEE_HIVE_YOUR_ATTACKS_ARE_NOTHING_MORE_THAN_BEE_STINGS_TO_ME,
		NpcStringId.YOU_ARE_NOT_PETS_BUT_YOU_STILL_WANT_ME_TO_SURRENDER_DIE,
	};
	private static final SkillHolder BEORO_ROAR = new SkillHolder(48469, 1);
	private static final Map<Integer, Boolean> ROYDA_SPAWN_HP_PERCENTAGES = new HashMap<>();
	static
	{
		ROYDA_SPAWN_HP_PERCENTAGES.put(80, false);
		ROYDA_SPAWN_HP_PERCENTAGES.put(70, false);
		ROYDA_SPAWN_HP_PERCENTAGES.put(60, false);
		ROYDA_SPAWN_HP_PERCENTAGES.put(47, false);
		ROYDA_SPAWN_HP_PERCENTAGES.put(36, false);
		ROYDA_SPAWN_HP_PERCENTAGES.put(25, false);
		ROYDA_SPAWN_HP_PERCENTAGES.put(17, false);
		ROYDA_SPAWN_HP_PERCENTAGES.put(7, false);
	}
	private boolean _beoroTransformAttempted = false;
	
	private Beoro()
	{
		addSpawnId(BEORO);
		addAttackId(BEORO, HERMIT_BEORO, ROYDA);
		addSkillSeeId(BEORO, HERMIT_BEORO, ROYDA);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		_beoroTransformAttempted = false;
		for (int key : ROYDA_SPAWN_HP_PERCENTAGES.keySet())
		{
			ROYDA_SPAWN_HP_PERCENTAGES.put(key, false);
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if ((npc.getId() != ROYDA) && !attacker.isPet())
		{
			petrify(npc, attacker);
			npc.setCurrentHp(npc.getCurrentHp() + damage);
			return;
		}
		
		if (npc.getId() == BEORO)
		{
			synchronized (this)
			{
				if (!_beoroTransformAttempted && (npc.getCurrentHpPercent() < 50))
				{
					_beoroTransformAttempted = true;
					npc.broadcastPacket(new ExShowScreenMessage(ANNOUNCEMENTS_50_HP[Rnd.get(ANNOUNCEMENTS_50_HP.length)], ExShowScreenMessage.BOTTOM_RIGHT, 5000, false));
					if (Rnd.get(100) < 50)
					{
						addSpawn(HERMIT_BEORO, npc).asAttackable().addDamageHate(attacker, 1, 5000);
						npc.deleteMe();
					}
				}
			}
		}
		
		if ((npc.getId() == BEORO) || (npc.getId() == HERMIT_BEORO))
		{
			synchronized (npc)
			{
				for (Entry<Integer, Boolean> entry : ROYDA_SPAWN_HP_PERCENTAGES.entrySet())
				{
					if ((entry.getKey() <= npc.getCurrentHpPercent()) && !entry.getValue())
					{
						ROYDA_SPAWN_HP_PERCENTAGES.put(entry.getKey(), true);
						addSpawn(ROYDA, npc, true, 10 * 60 * 1000).asAttackable().addDamageHate(attacker, 1, 5000);
						break;
					}
				}
			}
		}
		
		if (npc.getId() == ROYDA)
		{
			synchronized (npc)
			{
				if ((npc.getScriptValue() == 0) && (npc.getCurrentHpPercent() < 70))
				{
					npc.setScriptValue(1);
					final int currentPetId = Rnd.get(100) < 25 ? SIN_EATER : Atingo.PETS[Rnd.get(Atingo.PETS.length)];
					addSpawn(currentPetId, npc, true, 10 * 60 * 1000).asAttackable().addDamageHate(attacker, 1, 5000);
				}
			}
		}
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, Collection<WorldObject> targets, boolean isSummon)
	{
		if (isSummon)
		{
			return;
		}
		
		boolean isTarget = false;
		for (WorldObject target : targets)
		{
			if (target == npc)
			{
				isTarget = true;
				break;
			}
		}
		
		if (isTarget)
		{
			petrify(npc, caster);
		}
	}
	
	private void petrify(Npc npc, Playable playable)
	{
		playable.stopSkillEffects(SkillFinishType.REMOVED, 50196); // Opal
		playable.stopSkillEffects(SkillFinishType.REMOVED, 1411); // Mystic Immunity
		BEORO_ROAR.getSkill().applyEffects(npc, playable);
		
		final AggroInfo aggr = npc.asAttackable().getAggroList().getOrDefault(playable, null);
		if (aggr != null)
		{
			npc.asAttackable().reduceHate(playable, -aggr.getHate());
		}
		
		if (npc.getTarget() == playable)
		{
			npc.setTarget(null);
		}
	}
	
	public static void main(String[] args)
	{
		new Beoro();
	}
}
