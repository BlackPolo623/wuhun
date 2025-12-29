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
package ai.others;

import java.time.Duration;
import java.util.function.Consumer;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureHpChange;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;

/**
 * @author Berezkin Nikolay
 */
public class Atingo extends Script
{
	// NPCs
	private static final int ATINGO = 25914;
	private static final int SIN_EATER = 25924;
	public static final int[] PETS =
	{
		25923,
		25922,
		25921,
		25918,
		25920,
		25919
	};
	
	// Locations
	private static final Location[] SPAWNS =
	{
		new Location(83928, 94232, -3453, 41157), // Primeval Isle
		new Location(83928, 94232, -3453, 41157), // Plains of the Lizardmen
		new Location(113906, 14873, -3612, 49656), // Tower of Insolence
		new Location(171896, 20824, -3334, 16115), // Orc Barracks
	};
	
	// Misc
	private static final Duration ATINGO_RESPAWN_DURATION = Duration.ofMinutes(10);
	private static final double ATINGO_PET_SPAWN_RATE = 10;
	
	public Atingo()
	{
		addSpawnId(ATINGO);
		addKillId(ATINGO);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.addListener(new ConsumerEventListener(npc, EventType.ON_CREATURE_HP_CHANGE, (Consumer<OnCreatureHpChange>) this::onHpChange, this));
	}
	
	@Override
	protected void onLoad()
	{
		ThreadPool.schedule(() ->
		{
			if (World.getInstance().getVisibleObjects().stream().noneMatch(it -> it.getId() == ATINGO))
			{
				addSpawn(ATINGO, getRandomEntry(SPAWNS));
			}
		}, ATINGO_RESPAWN_DURATION.toMillis());
		
		super.onLoad();
	}
	
	private void onHpChange(OnCreatureHpChange hpChangeEvent)
	{
		final Npc creature = hpChangeEvent.getCreature().asNpc();
		if ((creature.getScriptValue() == 0) && !creature.isDead() && creature.isInCombat())
		{
			final Npc pet = addSpawn(getRandom(100) <= ATINGO_PET_SPAWN_RATE ? getRandomEntry(PETS) : SIN_EATER, GeoEngine.getInstance().getValidLocation(creature.getX(), creature.getY(), creature.getZ(), creature.getX() + 50, creature.getY() + 50, creature.getZ(), null));
			creature.setScriptValue(pet.getObjectId());
			pet.setInvul(true);
			pet.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.H_ULTIMATE_DEFENCE_B_AVE);
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int petObjId = npc.getScriptValue();
		if (petObjId > 0)
		{
			final Npc pet = World.getInstance().findObject(petObjId).asNpc();
			if (pet != null)
			{
				pet.setInvul(false);
				pet.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.H_ULTIMATE_DEFENCE_B_AVE);
			}
		}
		
		ThreadPool.schedule(() ->
		{
			if (World.getInstance().getVisibleObjects().stream().noneMatch(it -> it.getId() == ATINGO))
			{
				addSpawn(ATINGO, getRandomEntry(SPAWNS));
			}
		}, ATINGO_RESPAWN_DURATION.toMillis());
	}
	
	public static void main(String[] args)
	{
		new Atingo();
	}
}
