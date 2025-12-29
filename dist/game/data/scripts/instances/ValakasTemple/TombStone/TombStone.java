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
package instances.ValakasTemple.TombStone;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;

import instances.ValakasTemple.ValakasTemple;

/**
 * @author Index
 */
public class TombStone extends Script
{
	// Monsters
	public static final int SEALED_TOMB_STONE = 18727;
	public static final int DUMMY_TOMB_STORE = 18728;
	
	// Misc
	private static final Map<Integer, Npc> DUMMY_STONE_NPC = new HashMap<>();
	private static final String DUMMY_TOMB_STATE = "DUMMY_TOMB_STATE";
	private static final int TOMB_100 = 0; // NPC state on HP percent
	private static final int TOMB_066 = 1; // 1 chain break
	private static final int TOMB_033 = 2; // 2 chain break
	private static final int TOMB_000 = 3; // all tomb break
	
	private TombStone()
	{
		addAttackId(SEALED_TOMB_STONE);
		addKillId(SEALED_TOMB_STONE);
		addSpawnId(DUMMY_TOMB_STORE);
		addSpawnId(DUMMY_TOMB_STORE);
		addInstanceDestroyId(ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID))
		{
			return;
		}
		
		npc.setDisplayEffect(TOMB_100);
		npc.setUndying(true);
		npc.setTargetable(false);
		npc.setImmobilized(true);
		DUMMY_STONE_NPC.put(world.getId(), npc);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID))
		{
			return;
		}
		
		if (world.getStatus() != ValakasTemple.KILL_TOMB)
		{
			npc.setImmobilized(true);
			return;
		}
		
		final Npc dummyStone = DUMMY_STONE_NPC.get(world.getId());
		switch (npc.getCurrentHpPercent())
		{
			case 66:
			{
				dummyStone.setDisplayEffect(TOMB_066);
				world.getParameters().set(DUMMY_TOMB_STATE, TOMB_066);
				break;
			}
			case 50:
			{
				world.spawnGroup("raid_boss_tomb");
				break; // was fallthrough
			}
			case 33:
			{
				dummyStone.setDisplayEffect(TOMB_033);
				world.getParameters().set(DUMMY_TOMB_STATE, TOMB_033);
				break;
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if ((world == null) || (world.getTemplateId() != ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID))
		{
			return;
		}
		
		final Npc dummyStone = DUMMY_STONE_NPC.get(world.getId());
		DUMMY_STONE_NPC.remove(world.getId());
		dummyStone.setDisplayEffect(TOMB_000);
		dummyStone.deleteMe();
		world.getParameters().set(DUMMY_TOMB_STATE, TOMB_000);
		world.setStatus(ValakasTemple.GOTO_DUMMY_IFRIT);
	}
	
	@Override
	public void onInstanceDestroy(Instance instance)
	{
		DUMMY_STONE_NPC.remove(instance.getId());
		super.onInstanceDestroy(instance);
	}
	
	public static void main(String[] args)
	{
		new TombStone();
	}
}
