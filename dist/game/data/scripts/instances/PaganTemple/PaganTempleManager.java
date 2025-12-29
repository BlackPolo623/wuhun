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
package instances.PaganTemple;

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
public class PaganTempleManager extends InstanceScript
{
	public static final int INSTANCE_TEMPLATE_ID = 1021;
	public static final int TIME_HUNTING_ZONE_ID = 28;
	
	public static final int NORMAL = 1;
	public static final int CLOSED = 2;
	public static final int ANDREAS_BOSS = 3;
	public static final int ANDREAS_DEAD = 4;
	
	public static final String VARIABLE_TRIOLS_REVALATION_USES = "TRIOLS_REVALATION";
	public static final String VARIABLE_PLAYERS_FIGHT_LIST = "PLAYERS_FIGHT_LIST";
	public static final String VARIABLE_ANDREAS_BOSS_THINK_TASK = "ANDREAS_BOSS_THINK_TASK";
	public static final String VARIABLE_ANDREAS_BOSS_GUARD_SPAWN_PERCENT = "ANDREAS_BOSS_GUARD_SPAWN_PERCENT";
	
	private static final String VARIABLE_ELITE_MONSTER_SPAWNED_01 = "ELITE_MONSTER_SPAWNED_01";
	private static final String VARIABLE_ELITE_MONSTER_SPAWNED_02 = "ELITE_MONSTER_SPAWNED_02";
	private static final String TASK_SPAWN_ANDREAS = "SPAWN_ANDREAS";
	private static final String TASK_SPAWN_ELITE_01 = "TASK_ELITE_MONSTER_SPAWN_01";
	private static final String TASK_SPAWN_ELITE_02 = "TASK_ELITE_MONSTER_SPAWN_02";
	
	private static final SchedulingPattern PATTERN_ELITE_MONSTER_01 = new SchedulingPattern("0 18 * * 5");
	private static final SchedulingPattern PATTERN_ELITE_MONSTER_02 = new SchedulingPattern("0 22 * * 5");
	private static final SchedulingPattern PATTERN_SPAWN_ANDREAS = new SchedulingPattern("0 22 * * 5");
	
	private static final SchedulingPattern PATTERN_START_OF_ZONE = new SchedulingPattern("0 0 * * 1");
	public static final SchedulingPattern PATTERN_END_OF_ZONE = new SchedulingPattern("0 0 * * 6");
	private static final String TASK_START_ZONE = "ACTIVE_ZONE";
	private static final String TASK_END_ZONE = "DE_ACTIVE_ZONE";
	
	public PaganTempleManager()
	{
		super(INSTANCE_TEMPLATE_ID);
		addInstanceCreatedId(INSTANCE_TEMPLATE_ID);
		addInstanceEnterId(INSTANCE_TEMPLATE_ID);
		setInstanceStatusChangeId(this::onInstanceStatusChange, INSTANCE_TEMPLATE_ID);
	}
	
	@Override
	protected void onLoad()
	{
		super.onLoad();
		final InstanceTemplate paganInstanceTemplate = InstanceManager.getInstance().getInstanceTemplate(INSTANCE_TEMPLATE_ID);
		if (paganInstanceTemplate == null)
		{
			return;
		}
		
		final Instance paganInstance = new Instance(INSTANCE_TEMPLATE_ID, paganInstanceTemplate, null);
		onInstanceCreated(paganInstance, null);
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
	}
	
	@Override
	public void onInstanceEnter(Player player, Instance instance)
	{
		if ((instance.getStatus() == CLOSED) || !isAvailableToEnter(player))
		{
			instance.ejectPlayer(player);
			return;
		}
		
		super.onInstanceEnter(player, instance);
	}
	
	private void actionForEliteMonsters(Instance instance, boolean isFirst, boolean spawn)
	{
		if ((instance == null) || !isAvailableToEnter(null))
		{
			return;
		}
		
		if (spawn)
		{
			if (isFirst)
			{
				instance.setParameter(VARIABLE_ELITE_MONSTER_SPAWNED_01, true);
				spawnNpcsGroup(instance, "hall_prime_01", false, false);
				spawnNpcsGroup(instance, "left_01_hall_01_prime", false, false);
				spawnNpcsGroup(instance, "left_02_hall_01_prime", false, false);
				spawnNpcsGroup(instance, "right_01_hall_01_prime", false, false);
				spawnNpcsGroup(instance, "right_02_hall_01_prime", false, false);
				deSpawnNpcGroup(instance, "hall_no_prime_01");
				deSpawnNpcGroup(instance, "Left_first_rooms_in_the_first_hall");
				deSpawnNpcGroup(instance, "Right_first_rooms_in_the_first_hall");
			}
			else
			{
				instance.setParameter(VARIABLE_ELITE_MONSTER_SPAWNED_02, true);
				spawnNpcsGroup(instance, "hall_prime_02", false, false);
				spawnNpcsGroup(instance, "left_02_room_02_prime_02", false, false);
				spawnNpcsGroup(instance, "right_02_room_02_prime_02", false, false);
				deSpawnNpcGroup(instance, "hall_no_prime_02");
				deSpawnNpcGroup(instance, "Left_side_of_the_second_room");
				deSpawnNpcGroup(instance, "Right_side_of_the_second_room");
				deSpawnNpcGroup(instance, "Left_second_rooms_in_the_first_hall");
				deSpawnNpcGroup(instance, "Right_second_rooms_in_the_first_hall");
			}
		}
		else
		{
			if (isFirst)
			{
				instance.setParameter(VARIABLE_ELITE_MONSTER_SPAWNED_01, false);
				deSpawnNpcGroup(instance, "hall_prime_01");
				deSpawnNpcGroup(instance, "left_01_hall_01_prime");
				deSpawnNpcGroup(instance, "left_02_hall_01_prime");
				deSpawnNpcGroup(instance, "right_01_hall_01_prime");
				deSpawnNpcGroup(instance, "right_02_hall_01_prime");
				spawnNpcsGroup(instance, "hall_no_prime_01", false, false);
				spawnNpcsGroup(instance, "Left_first_rooms_in_the_first_hall", false, false);
				spawnNpcsGroup(instance, "Right_first_rooms_in_the_first_hall", false, false);
			}
			else
			{
				instance.setParameter(VARIABLE_ELITE_MONSTER_SPAWNED_02, false);
				deSpawnNpcGroup(instance, "hall_prime_02");
				deSpawnNpcGroup(instance, "left_02_room_02_prime_02");
				deSpawnNpcGroup(instance, "right_02_room_02_prime_02");
				spawnNpcsGroup(instance, "hall_no_prime_02", false, false);
				spawnNpcsGroup(instance, "Left_side_of_the_second_room", false, false);
				spawnNpcsGroup(instance, "Right_side_of_the_second_room", false, false);
				spawnNpcsGroup(instance, "Left_second_rooms_in_the_first_hall", false, false);
				spawnNpcsGroup(instance, "Right_second_rooms_in_the_first_hall", false, false);
			}
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
				
				openDoor(19160010, world.getId());
				openDoor(19160011, world.getId());
				spawnNpcsGroup(world, "hall_no_prime_01", false, false);
				spawnNpcsGroup(world, "Left_second_rooms_in_the_first_hall", false, false);
				spawnNpcsGroup(world, "Right_second_rooms_in_the_first_hall", false, false);
				spawnNpcsGroup(world, "hall_no_prime_02", false, false);
				spawnNpcsGroup(world, "Left_first_rooms_in_the_first_hall", false, false);
				spawnNpcsGroup(world, "Right_first_rooms_in_the_first_hall", false, false);
				
				final int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
				final int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				if ((dayOfWeek == Calendar.FRIDAY) && (currentHour >= 22))
				{
					changeStatusOfInstance(world, ANDREAS_BOSS);
				}
				else
				{
					createTask(world, TASK_SPAWN_ANDREAS, ThreadPool.schedule(() -> changeStatusOfInstance(world, ANDREAS_BOSS), PATTERN_SPAWN_ANDREAS.nextFromNow()));
				}
				
				if ((dayOfWeek == Calendar.FRIDAY) && (currentHour >= 18))
				{
					actionForEliteMonsters(world, true, true);
				}
				else
				{
					actionForEliteMonsters(world, true, false);
					createTask(world, TASK_SPAWN_ELITE_01, ThreadPool.schedule(() -> actionForEliteMonsters(world, true, true), PATTERN_ELITE_MONSTER_01.nextFromNow()));
				}
				
				if ((dayOfWeek == Calendar.FRIDAY) && (currentHour >= 22))
				{
					actionForEliteMonsters(world, false, true);
				}
				else
				{
					actionForEliteMonsters(world, false, false);
					createTask(world, TASK_SPAWN_ELITE_02, ThreadPool.schedule(() -> actionForEliteMonsters(world, false, true), PATTERN_ELITE_MONSTER_02.nextFromNow()));
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
				
				closeDoor(19160010, world.getId());
				closeDoor(19160011, world.getId());
				deSpawnNpcGroup(world, "ESCORT");
				deSpawnNpcGroup(world, "BLOODY_ADREAS");
				deSpawnNpcGroup(world, "NORMAL_ADREAS");
				deSpawnNpcGroup(world, "AKOLYTH_GUARD_01");
				deSpawnNpcGroup(world, "AKOLYTH_GUARD_CENTER_LEFT");
				deSpawnNpcGroup(world, "AKOLYTH_GUARD_CENTER_RIGHT");
				deSpawnNpcGroup(world, "AKOLYTH_GUARD_FAR_LEFT");
				deSpawnNpcGroup(world, "AKOLYTH_GUARD_FAR_RIGHT");
				
				deSpawnNpcGroup(world, "hall_prime_02");
				deSpawnNpcGroup(world, "left_02_room_02_prime_02");
				deSpawnNpcGroup(world, "right_02_room_02_prime_02");
				deSpawnNpcGroup(world, "hall_no_prime_01");
				deSpawnNpcGroup(world, "Left_first_rooms_in_the_first_hall");
				deSpawnNpcGroup(world, "Right_first_rooms_in_the_first_hall");
				deSpawnNpcGroup(world, "hall_no_prime_02");
				deSpawnNpcGroup(world, "Left_side_of_the_second_room");
				deSpawnNpcGroup(world, "Right_side_of_the_second_room");
				deSpawnNpcGroup(world, "Left_second_rooms_in_the_first_hall");
				deSpawnNpcGroup(world, "Right_second_rooms_in_the_first_hall");
				deSpawnNpcGroup(world, "hall_prime_01");
				deSpawnNpcGroup(world, "left_01_hall_01_prime");
				deSpawnNpcGroup(world, "left_02_hall_01_prime");
				deSpawnNpcGroup(world, "right_01_hall_01_prime");
				deSpawnNpcGroup(world, "right_02_hall_01_prime");
				
				world.setParameter(VARIABLE_ELITE_MONSTER_SPAWNED_01, false);
				world.setParameter(VARIABLE_ELITE_MONSTER_SPAWNED_02, false);
				world.setParameter(VARIABLE_TRIOLS_REVALATION_USES, 0);
				world.getParameters().remove(VARIABLE_PLAYERS_FIGHT_LIST);
				
				// DO NOT STOP TASK_END_ZONE BECAUSE IT WILL BREAK THIS RUNNING
				stopTask(world, TASK_END_ZONE, true, false);
				stopTask(world, VARIABLE_ANDREAS_BOSS_THINK_TASK, true, true);
				stopTask(world, TASK_SPAWN_ELITE_01, true, true);
				stopTask(world, TASK_SPAWN_ELITE_02, true, true);
				createTask(world, TASK_START_ZONE, ThreadPool.schedule(() -> changeStatusOfInstance(world, NORMAL), PATTERN_START_OF_ZONE.nextFromNow()));
				break;
			}
			case ANDREAS_BOSS:
			{
				spawnNpcGroup(world, "ESCORT", false, false);
				spawnNpcGroup(world, (getRandomBoolean() ? "BLOODY_ADREAS" : "NORMAL_ADREAS"), false, true);
				break;
			}
			case ANDREAS_DEAD:
			{
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
		new PaganTempleManager();
	}
}
