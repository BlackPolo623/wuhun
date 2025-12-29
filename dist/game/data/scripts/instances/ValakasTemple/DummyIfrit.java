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
package instances.ValakasTemple;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author Index
 */
public class DummyIfrit extends Script
{
	private static final int DUMMY_IFRIT_NPC_ID = 25966;
	// private static final int BLAZING_DRAGON_NPC_ID = 25967;
	
	private DummyIfrit()
	{
		addSpawnId(DUMMY_IFRIT_NPC_ID);
		addAttackId(DUMMY_IFRIT_NPC_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final Instance world = npc.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID))
		{
			return null;
		}
		
		if (world.getStatus() == ValakasTemple.GOTO_DUMMY_IFRIT)
		{
			if (World.getInstance().getVisibleObjectsInRange(npc, Player.class, 400).isEmpty())
			{
				startQuestTimer("CHECK_STATUS", 2_000, npc, null);
			}
			else
			{
				world.setStatus(ValakasTemple.OPEN_GATE_TIMER);
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID))
		{
			return;
		}
		
		if (world.getStatus() == ValakasTemple.GOTO_DUMMY_IFRIT)
		{
			npc.setImmobilized(true);
			startQuestTimer("CHECK_STATUS", 10_000, npc, null);
		}
		else
		{
			npc.setImmobilized(false);
			npc.setUndying(true);
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID))
		{
			return;
		}
		
		if (npc.isUndying() && (npc.getCurrentHpPercent() <= 50))
		{
			npc.setUndying(false);
			if (getRandom(100) < 15)
			{
				final Npc blazzingDragon = world.spawnGroup("blazzing_dragon").get(0);
				blazzingDragon.addAttackerToAttackByList(attacker);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new DummyIfrit();
	}
}
