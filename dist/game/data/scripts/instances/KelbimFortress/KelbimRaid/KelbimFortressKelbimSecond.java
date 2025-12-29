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
package instances.KelbimFortress.KelbimRaid;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.holders.instance.OnInstanceStatusChange;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

import instances.KelbimFortress.KelbimFortressManager;

/**
 * @author Index
 */
public class KelbimFortressKelbimSecond extends Script
{
	private static final int KELBIM_NPC_ID = 29205;
	private static final SkillHolder FATAL_STRIKE = new SkillHolder(48669, 1);
	private static final SkillHolder FROST_STORM = new SkillHolder(48672, 1);
	private static final SkillHolder REFLECTION_ARMOR = new SkillHolder(48673, 1);
	
	public KelbimFortressKelbimSecond()
	{
		setInstanceStatusChangeId(this::onInstanceStatusChange, KelbimFortressManager.INSTANCE_TEMPLATE_ID);
		addAttackId(KELBIM_NPC_ID);
		addKillId(KELBIM_NPC_ID);
		addSpellFinishedId(KELBIM_NPC_ID);
	}
	
	public void onInstanceStatusChange(OnInstanceStatusChange event)
	{
		final Instance world = event.getWorld();
		if (world == null)
		{
			return;
		}
		
		switch (world.getStatus())
		{
			case KelbimFortressManager.NORMAL:
			case KelbimFortressManager.CLOSED:
			case KelbimFortressManager.KELBIM_DEAD:
			case KelbimFortressManager.KELBIM_FIRST:
			{
				KelbimFortressManager.deSpawnNpcGroup(world, "KELBIM_02");
				for (int index = 1; index <= 7; index++)
				{
					KelbimFortressManager.deSpawnNpcGroup(world, "KELBIM_02_GUARD_0" + index);
				}
				break;
			}
			case KelbimFortressManager.KELBIM_SECOND:
			{
				// TODO: Film?
				KelbimFortressManager.spawnNpcGroup(world, "KELBIM_02", false, true); // addSpawn(KELBIM_NPC_ID, KELBIM_SPAWN_LOCATION, false, 0, false, world.getId());
				break;
			}
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final Instance world = (attacker == null) || (npc == null) ? null : attacker.getInstanceWorld();
		if ((npc == null) || (world == null) || (world.getTemplateId() != KelbimFortressManager.INSTANCE_TEMPLATE_ID))
		{
			super.onAttack(npc, attacker, damage, isSummon, skill);
		}
		else if (!world.getParameters().contains(KelbimFortressManager.TASK_KELBIM_CHECK_STATUS))
		{
			world.getParameters().set(KelbimFortressManager.TASK_KELBIM_CHECK_STATUS, ThreadPool.scheduleAtFixedRate(() -> thinkAction(world, npc), 2_000, 2_000));
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		final Instance world = (player == null) || (npc == null) ? null : player.getInstanceWorld();
		if ((npc == null) || (world == null) || (world.getTemplateId() != KelbimFortressManager.INSTANCE_TEMPLATE_ID))
		{
			return;
		}
		
		if (!world.getParameters().contains(KelbimFortressManager.TASK_KELBIM_CHECK_STATUS))
		{
			world.getParameters().set(KelbimFortressManager.TASK_KELBIM_CHECK_STATUS, ThreadPool.scheduleAtFixedRate(() -> thinkAction(world, npc), 2_000, 2_000));
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = (killer == null) || (npc == null) ? null : killer.getInstanceWorld();
		if ((npc == null) || (world == null) || (world.getTemplateId() != KelbimFortressManager.INSTANCE_TEMPLATE_ID))
		{
			return;
		}
		
		world.setStatus(KelbimFortressManager.KELBIM_DEAD);
	}
	
	@Override
	public void onNpcDespawn(Npc npc)
	{
		super.onNpcDespawn(npc);
		final Instance world = (npc == null) ? null : npc.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != KelbimFortressManager.INSTANCE_TEMPLATE_ID))
		{
			return;
		}
		
		if (world.getParameters().contains(KelbimFortressManager.TASK_KELBIM_CHECK_STATUS))
		{
			final ScheduledFuture<?> task = world.getParameters().getObject(KelbimFortressManager.TASK_KELBIM_CHECK_STATUS, ScheduledFuture.class, null);
			if (task != null)
			{
				task.cancel(true);
			}
			
			world.getParameters().remove(KelbimFortressManager.TASK_KELBIM_CHECK_STATUS);
		}
	}
	
	public static void thinkAction(Instance world, Npc npc)
	{
		checkAndAddGuard(world, npc);
		if (!npc.isCastingNow() && !npc.isMovementDisabled())
		{
			if (npc.getEffectList().getBuffInfoBySkillId(REFLECTION_ARMOR.getSkillId()) == null)
			{
				SkillCaster.triggerCast(npc, npc, SkillData.getInstance().getSkill(REFLECTION_ARMOR.getSkillId(), REFLECTION_ARMOR.getSkillLevel()));
			}
			else if (Rnd.get(100_000) < 75_000)
			{
				npc.doCast(SkillData.getInstance().getSkill(FATAL_STRIKE.getSkillId(), FATAL_STRIKE.getSkillLevel()));
			}
			else if (Rnd.get(100_000) < 25_000)
			{
				npc.doCast(SkillData.getInstance().getSkill(FROST_STORM.getSkillId(), FROST_STORM.getSkillLevel()));
			}
		}
	}
	
	private static void checkAndAddGuard(Instance world, Npc npc)
	{
		final int hpPercent = npc.getCurrentHpPercent();
		final int guardStatus = world.getParameters().getInt(KelbimFortressManager.VARIABLE_KELBIM_GUARD_STATUS, 0);
		if (((guardStatus == 0) && (hpPercent < 95.0)) || ((guardStatus == 1) && (hpPercent < 90.0)) || ((guardStatus == 2) && (hpPercent < 85.0)) || ((guardStatus == 3) && (hpPercent < 80.0)) || ((guardStatus == 4) && (hpPercent < 75.0)) || ((guardStatus == 5) && (hpPercent < 70.0)) || ((guardStatus == 6) && (hpPercent < 65.0)) || ((guardStatus == 7) && (hpPercent < 60.0)) || ((guardStatus == 8) && (hpPercent < 55.0)) || ((guardStatus == 9) && (hpPercent < 50.0)))
		{
			world.getParameters().increaseInt(KelbimFortressManager.VARIABLE_KELBIM_GUARD_STATUS, 0, 1);
			KelbimFortressManager.spawnNpcGroup(world, "KELBIM_01_GUARD_0" + Rnd.get(1, KelbimFortressKelbimGuards.GUARD_IDS.length), true, true);
		}
	}
	
	public static void main(String[] args)
	{
		new KelbimFortressKelbimSecond();
	}
}
