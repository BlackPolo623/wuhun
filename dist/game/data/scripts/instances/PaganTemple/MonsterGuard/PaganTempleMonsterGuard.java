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
package instances.PaganTemple.MonsterGuard;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.util.LocationUtil;

import instances.PaganTemple.PaganTempleManager;

/**
 * @author Index
 */
public class PaganTempleMonsterGuard extends Script
{
	private static final int TRIOL_PRIEST = 22651;
	private static final int TRIOL_HIGH_PRIEST = 22656;
	
	private static final int TRIOL_PRIEST_GUARD_01 = 22657;
	private static final int TRIOL_PRIEST_GUARD_WARRIOR_02 = 22658;
	
	private static final int ANDREAS_PRAETORIAN_01 = 29215;
	private static final int ANDREAS_PRAETORIAN_02 = 29216;
	private static final int ANDREAS_PRAETORIAN_03 = 29226;
	
	public PaganTempleMonsterGuard()
	{
		addAttackId(TRIOL_PRIEST);
		addAttackId(TRIOL_HIGH_PRIEST);
		addKillId(TRIOL_PRIEST, TRIOL_HIGH_PRIEST);
		addKillId(ANDREAS_PRAETORIAN_01, ANDREAS_PRAETORIAN_02, ANDREAS_PRAETORIAN_03);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (!npc.isDead() && (npc.getCurrentHpPercent() <= 80) && !npc.getVariables().getBoolean("GUARD_SPAWNED", false))
		{
			final List<Player> aggroPlayers = new ArrayList<>(0);
			for (Creature creature : npc.asMonster().getAggroList().keySet())
			{
				final Player player = creature.asPlayer();
				if ((player != null) && !aggroPlayers.contains(player))
				{
					aggroPlayers.add(player);
				}
			}
			
			npc.getVariables().set("GUARD_SPAWNED", true);
			for (int index = 0; index < 3; index++)
			{
				final Npc guard = addSpawn(npc, TRIOL_PRIEST_GUARD_01, npc.getX()/* + (Rnd.nextBoolean() ? Rnd.get(20, 50) : Rnd.get(-50, -20)) */, npc.getY()/* + (Rnd.nextBoolean() ? Rnd.get(20, 50) : Rnd.get(-50, -20)) */, npc.getZ(), LocationUtil.calculateHeadingFrom(npc.getX(), npc.getY()), true, 0L, false, npc.getInstanceId());
				
				// npc.addSummonedNpc(guard);
				addAttackPlayerDesire(guard, aggroPlayers.get(Rnd.get(aggroPlayers.size())));
			}
			
			for (int index = 0; index < 2; index++)
			{
				final Npc guard = addSpawn(npc, TRIOL_PRIEST_GUARD_WARRIOR_02, npc.getX()/* + (Rnd.nextBoolean() ? Rnd.get(20, 50) : Rnd.get(-50, -20)) */, npc.getY()/* + (Rnd.nextBoolean() ? Rnd.get(20, 50) : Rnd.get(-50, -20)) */, npc.getZ(), LocationUtil.calculateHeadingFrom(npc.getX(), npc.getY()), true, 0L, false, npc.getInstanceId());
				
				// npc.addSummonedNpc(guard);
				addAttackPlayerDesire(guard, aggroPlayers.get(Rnd.get(aggroPlayers.size())));
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = (killer == null) || (npc == null) ? null : killer.getInstanceWorld();
		if ((npc == null) || (world == null) || (world.getTemplateId() != PaganTempleManager.INSTANCE_TEMPLATE_ID))
		{
			return;
		}
		
		if ((npc.getId() == TRIOL_PRIEST) || (npc.getId() == TRIOL_HIGH_PRIEST))
		{
			for (Npc guard : npc.getSummonedNpcs())
			{
				guard.deleteMe();
			}
		}
		else if ((npc.getId() == ANDREAS_PRAETORIAN_01) || (npc.getId() == ANDREAS_PRAETORIAN_02) || (npc.getId() == ANDREAS_PRAETORIAN_03))
		{
			for (int index = 1; index <= 10; index++)
			{
				if (world.getNpcsOfGroup("TRIOLS_REVALATION_" + index).isEmpty())
				{
					final Npc triolRevalation = PaganTempleManager.spawnNpcGroup(world, "TRIOLS_REVALATION_" + index, false, true);
					if (triolRevalation != null)
					{
						triolRevalation.setScriptValue(index);
						break;
					}
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new PaganTempleMonsterGuard();
	}
}
