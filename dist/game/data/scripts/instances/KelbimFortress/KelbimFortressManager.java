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
package instances.KelbimFortress;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.holders.instance.OnInstanceStatusChange;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.model.script.InstanceScript;

/**
 * @author Index
 */
public class KelbimFortressManager extends InstanceScript
{
	public static final int INSTANCE_TEMPLATE_ID = 1020;
	public static final int TIME_HUNTING_ZONE_ID = 27;
	
	public static final int NORMAL = 1;
	public static final int CLOSED = 2;
	public static final int KELBIM_FIRST = 3;
	public static final int KELBIM_SECOND = 4;
	public static final int KELBIM_DEAD = 5;
	
	public static final String TASK_KELBIM_CHECK_STATUS = "KELBIM_CHECK_STATUS";
	public static final String VARIABLE_KELBIM_GUARD_STATUS = "KELBIM_GUARD_STATUS";
	public static final String VARIABLE_KELBIM_SECOND_MOVIE = "KELBIM_SECOND_MOVIE";
	
	private static final String VARIABLE_ELITE_MONSTER_SPAWNED = "ELITE_MONSTER_SPAWNED";
	
	private static final SchedulingPattern PATTERN_START_OF_ZONE = new SchedulingPattern("0 0 * * 1");
	private static final SchedulingPattern PATTERN_END_OF_ZONE = new SchedulingPattern("0 0 * * 6");
	private static final String TASK_START_ZONE = "ACTIVE_ZONE";
	private static final String TASK_END_ZONE = "DE_ACTIVE_ZONE";
	
	private static final SchedulingPattern PATTERN_SPAWN_ELITE_MONSTER = new SchedulingPattern("0 18 * * 5");
	private static final SchedulingPattern PATTERN_SPAWN_KELBIM_BOSS = new SchedulingPattern("0 22 * * 5");
	private static final String TASK_SPAWN_ELITE = "SPAWN_ELITE";
	private static final String TASK_SPAWN_KELBIM = "SPAWN_KELBIM";
	
	public KelbimFortressManager()
	{
		super(INSTANCE_TEMPLATE_ID);
		addInstanceCreatedId(INSTANCE_TEMPLATE_ID);
		addInstanceEnterId(INSTANCE_TEMPLATE_ID);
		setInstanceStatusChangeId(this::onInstanceStatusChange, INSTANCE_TEMPLATE_ID);
	}
	
	@Override
	protected void onLoad()
	{
		final InstanceTemplate kelbimInstanceTemplate = InstanceManager.getInstance().getInstanceTemplate(INSTANCE_TEMPLATE_ID);
		if (kelbimInstanceTemplate == null)
		{
			return;
		}
		
		final Instance kelbimInstance = new Instance(INSTANCE_TEMPLATE_ID, kelbimInstanceTemplate, null);
		onInstanceCreated(kelbimInstance, null);
		InstanceManager.getInstance().register(kelbimInstance);
	}
	
	@Override
	public void onInstanceCreated(Instance instance, Player player)
	{
		super.onInstanceCreated(instance, player);
		
		if (!isAvailableToEnter(null))
		{
			changeStatusOfInstance(instance, CLOSED);
		}
		else
		{
			changeStatusOfInstance(instance, NORMAL);
		}
		
		final int instanceId = instance.getId();
		closeDoor(18190002, instanceId);
		closeDoor(18190004, instanceId);
		closeDoor(18190006, instanceId);
		closeDoor(18190008, instanceId);
		closeDoor(18190010, instanceId);
		closeDoor(18190012, instanceId);
	}
	
	@Override
	public void onInstanceEnter(Player player, Instance instance)
	{
		// if (instance.getStatus() == CLOSED || !isAvailableToEnter(player))
		// {
		// instance.ejectPlayer(player);
		// return;
		// }
		
		super.onInstanceEnter(player, instance);
	}
	
	private void actionForEliteMonsters(Instance instance, boolean spawn)
	{
		if ((instance == null) || !isAvailableToEnter(null))
		{
			return;
		}
		
		if (spawn)
		{
			instance.setParameter(VARIABLE_ELITE_MONSTER_SPAWNED, true);
			deSpawnNpcGroup(instance, "hottime_mobs");
			
			// TODO: ELITE MONSTERS HERE
			// instance.spawnGroup("");
		}
		else
		{
			instance.setParameter(VARIABLE_ELITE_MONSTER_SPAWNED, false);
			spawnNpcGroup(instance, "hottime_mobs", false, false);
			
			// TODO: ELITE MONSTERS HERE
			// instance.spawnGroup("");
		}
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
			case NORMAL:
			{
				if (!isAvailableToEnter(null))
				{
					changeStatusOfInstance(world, CLOSED);
					return;
				}
				
				openDoor(18190002, world.getId());
				openDoor(18190004, world.getId());
				spawnNpcGroup(world, "PortSpot_01", false, false);
				spawnNpcGroup(world, "PortSpot_02", false, false);
				spawnNpcGroup(world, "2_Floor", false, false);
				spawnNpcGroup(world, "2_Floor_elite", false, false);
				spawnNpcGroup(world, "3_Floor_elite", false, false);
				
				final int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
				final int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				if ((dayOfWeek == Calendar.FRIDAY) && (currentHour >= 22))
				{
					changeStatusOfInstance(world, KELBIM_FIRST);
				}
				else
				{
					createTask(world, TASK_SPAWN_KELBIM, ThreadPool.schedule(() -> changeStatusOfInstance(world, KELBIM_FIRST), PATTERN_SPAWN_KELBIM_BOSS.nextFromNow()));
				}
				
				if ((dayOfWeek == Calendar.FRIDAY) && (currentHour >= 18))
				{
					actionForEliteMonsters(world, true);
				}
				else
				{
					createTask(world, TASK_SPAWN_ELITE, ThreadPool.schedule(() -> actionForEliteMonsters(world, true), PATTERN_SPAWN_ELITE_MONSTER.nextFromNow()));
				}
				
				createTask(world, TASK_END_ZONE, ThreadPool.schedule(() -> changeStatusOfInstance(world, CLOSED), PATTERN_END_OF_ZONE.nextFromNow()));
				break;
			}
			case CLOSED:
			{
				for (Player player : world.getPlayers())
				{
					world.ejectPlayer(player);
				}
				
				actionForEliteMonsters(world, false);
				deSpawnNpcGroup(world, "PortSpot_01");
				deSpawnNpcGroup(world, "PortSpot_02");
				deSpawnNpcGroup(world, "2_Floor");
				deSpawnNpcGroup(world, "2_Floor_elite");
				deSpawnNpcGroup(world, "3_Floor_elite");
				KelbimFortressManager.deSpawnNpcGroup(world, "KELBIM_01");
				KelbimFortressManager.deSpawnNpcGroup(world, "KELBIM_02");
				for (int index = 1; index <= 7; index++)
				{
					KelbimFortressManager.deSpawnNpcGroup(world, "KELBIM_01_GUARD_0" + index);
					KelbimFortressManager.deSpawnNpcGroup(world, "KELBIM_02_GUARD_0" + index);
				}
				
				world.getParameters().remove(KelbimFortressManager.VARIABLE_KELBIM_GUARD_STATUS);
				
				// DO NOT STOP TASK_END_ZONE BECAUSE IT WILL BREAK THIS RUNNING
				stopTask(world, TASK_END_ZONE, true, false);
				stopTask(world, TASK_KELBIM_CHECK_STATUS, true, true);
				stopTask(world, TASK_SPAWN_ELITE, true, true);
				createTask(world, TASK_START_ZONE, ThreadPool.schedule(() -> changeStatusOfInstance(world, NORMAL), PATTERN_START_OF_ZONE.nextFromNow()));
				break;
			}
			case KELBIM_FIRST:
			case KELBIM_SECOND:
			{
				stopTask(world, TASK_KELBIM_CHECK_STATUS, true, true);
				openDoor(18190002, world.getId());
				openDoor(18190004, world.getId());
				break;
			}
			case KELBIM_DEAD:
			{
				stopTask(world, TASK_KELBIM_CHECK_STATUS, true, true);
				closeDoor(18190002, world.getId());
				closeDoor(18190004, world.getId());
				closeDoor(18190006, world.getId());
				closeDoor(18190008, world.getId());
				closeDoor(18190010, world.getId());
				closeDoor(18190012, world.getId());
				break;
			}
		}
	}
	
	private void changeStatusOfInstance(Instance world, int status)
	{
		if (world == null)
		{
			return;
		}
		
		if (world.getStatus() == status)
		{
			return;
		}
		
		world.setParameter("INSTANCE_STATUS", status);
		onInstanceStatusChange(new OnInstanceStatusChange(world, status));
	}
	
	private void createTask(Instance world, String name, ScheduledFuture<?> task)
	{
		if ((world == null) || (name == null) || (task == null))
		{
			return;
		}
		
		stopTask(world, name, true, false);
		world.getParameters().set(name, task);
	}
	
	private void stopTask(Instance world, String name, boolean removeFromParams, boolean stopIfRunning)
	{
		if (world.getParameters().contains(name))
		{
			final ScheduledFuture<?> scheduledFuture = world.getParameters().getObject(name, ScheduledFuture.class, null);
			if (scheduledFuture != null)
			{
				scheduledFuture.cancel(stopIfRunning);
			}
			
			if (removeFromParams)
			{
				world.getParameters().remove(name);
			}
		}
	}
	
	public static List<Npc> spawnNpcsGroup(Instance world, String groupSpawnName, boolean canDuplicate, boolean stopRespawn)
	{
		if ((world == null) || (groupSpawnName == null))
		{
			return Collections.emptyList();
		}
		
		List<Npc> spawnedNpcs;
		if (!canDuplicate)
		{
			spawnedNpcs = world.getNpcsOfGroup(groupSpawnName);
			boolean isAllDead = true;
			for (Npc npc : spawnedNpcs)
			{
				if (npc.isDead() || npc.isDecayed())
				{
					continue;
				}
				
				isAllDead = false;
				break;
			}
			
			if (isAllDead)
			{
				deSpawnNpcGroup(world, groupSpawnName);
				spawnedNpcs = Collections.emptyList();
			}
			
			spawnedNpcs = spawnedNpcs.isEmpty() ? world.spawnGroup(groupSpawnName) : spawnedNpcs;
		}
		else
		{
			spawnedNpcs = world.spawnGroup(groupSpawnName);
		}
		
		if (stopRespawn)
		{
			for (Npc npc : spawnedNpcs)
			{
				Spawn spawn = npc == null ? null : npc.getSpawn();
				if (spawn != null)
				{
					spawn.stopRespawn();
				}
			}
		}
		
		return spawnedNpcs;
	}
	
	public static Npc spawnNpcGroup(Instance world, String groupSpawnName, boolean canDuplicate, boolean stopRespawn)
	{
		final List<Npc> npcs = spawnNpcsGroup(world, groupSpawnName, canDuplicate, stopRespawn);
		return npcs.isEmpty() ? null : npcs.get(0);
	}
	
	public static void deSpawnNpcGroup(Instance world, String groupSpawnName)
	{
		if ((world == null) || (groupSpawnName == null))
		{
			return;
		}
		
		world.despawnGroup(groupSpawnName);
	}
	
	public static boolean isAvailableToEnter(Player player)
	{
		return true; // TimedHuntingZoneData.getInstance().canEnter(TIME_HUNTING_ZONE_ID, player, true);
	}
	
	public static void main(String[] args)
	{
		new KelbimFortressManager();
	}
}
