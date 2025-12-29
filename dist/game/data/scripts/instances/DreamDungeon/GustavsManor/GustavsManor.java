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
package instances.DreamDungeon.GustavsManor;

import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.holders.instance.OnInstanceStatusChange;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import instances.DreamDungeon.CatGuildsLair.CatGuildsLair;

/**
 * @author Index
 */
public class GustavsManor extends InstanceScript
{
	/** Instance World IDS */
	public static final int CREATED = 0;
	public static final int GO_TO_GATES_AND_KILL_GIRL = 1;
	public static final int TALK_WITH_RASCAL = 2;
	public static final int KILL_FOUR_DREAM_WATCHERS_INSIDE = 3;
	public static final int TALK_WITH_HORSIE_IN_MANOR = 4;
	public static final int BOSS_FIGHT = 5;
	public static final int FINISH_INSTANCE = 6;
	
	public static final int INSTANCE_ID = 221;
	private static final String DREAM_WATCHER_COUNTER = "DREAM_WATCHER_COUNTER";
	private static final NpcStringId DREAM_WATCHER = NpcStringId.DREAM_WATCHER;
	private static final NpcStringId STRING_ID_01 = NpcStringId.WHO_ARE_YOU_AND_HOW_DARE_YOU_ENTER_MY_MANOR;
	private static final NpcStringId STRING_ID_02 = NpcStringId.I_WON_T_FORGIVE_YOU_FOR_INTRUDING_IN_GUSTAV_S_MANOR;
	private static final NpcStringId STRING_ID_03 = NpcStringId.LEAVING_ALREADY_LET_S_PLAY_A_BIT_MORE;
	private static final int[] MONSTERS =
	{
		22391, // Mansion Manager
		22392, // Touchy Servant
		22393, // Housekeeper
		22394, // Cursed Girl
	};
	private static final int GUSTAV = 18678;
	private static final int GUSTAV_STEWARD = 18679;
	
	private static final int[] MAIN_DOOR_IDS =
	{
		21170001,
		21170002,
		21170005,
		21170006,
		21170003,
		21170004,
	};
	
	private GustavsManor()
	{
		super(INSTANCE_ID);
		addKillId(MONSTERS);
		addKillId(GUSTAV, GUSTAV_STEWARD);
		setInstanceStatusChangeId(this::onInstanceStatusChange, INSTANCE_ID);
	}
	
	public void onInstanceStatusChange(OnInstanceStatusChange event)
	{
		final Instance instance = event.getWorld();
		final int status = event.getStatus();
		switch (status)
		{
			case GO_TO_GATES_AND_KILL_GIRL:
			{
				instance.setReenterTime();
				instance.despawnGroup("GustavNPC_1");
				instance.spawnGroup("GustavNPC_2").forEach(n -> n.getSpawn().stopRespawn());
				instance.spawnGroup("GustavMonsters_1").forEach(n -> n.getSpawn().stopRespawn());
				break;
			}
			// case 2 - to talk with Rascal
			case KILL_FOUR_DREAM_WATCHERS_INSIDE:
			{
				for (int doorId : MAIN_DOOR_IDS)
				{
					instance.openCloseDoor(doorId, true);
				}
				
				instance.despawnGroup("GustavNPC_2");
				instance.spawnGroup("GustavMonsters_2").forEach(n -> n.getSpawn().stopRespawn());
				setRandomTitles(instance, true);
				break;
			}
			case TALK_WITH_HORSIE_IN_MANOR:
			{
				instance.getParameters().remove(DREAM_WATCHER_COUNTER);
				instance.broadcastPacket(new ExShowScreenMessage(NpcStringId.HERE_IS_MY_HORSIE_LET_S_PLAY, ExShowScreenMessage.TOP_CENTER, 10000, true));
				ThreadPool.schedule(() ->
				{
					for (Npc npc : instance.getNpcs(MONSTERS))
					{
						npc.getSpawn().stopRespawn();
						npc.doDie(null);
					}
					
					instance.spawnGroup("GustavNPC_3").forEach(n -> n.getSpawn().stopRespawn());
				}, 10000);
				break;
			}
			case BOSS_FIGHT:
			{
				instance.despawnGroup("GustavNPC_3");
				spawnBoss(instance);
				break;
			}
			case FINISH_INSTANCE:
			{
				instance.setDuration(5);
				instance.broadcastPacket(new ExShowScreenMessage(STRING_ID_03, ExShowScreenMessage.TOP_CENTER, 10000, true));
				instance.spawnGroup("GustavNPC_4").forEach(n -> n.getSpawn().stopRespawn());
				break;
			}
		}
	}
	
	private void spawnBoss(Instance instance)
	{
		final boolean random = Rnd.nextBoolean();
		
		// show text
		final NpcStringId bossText = random ? STRING_ID_01 : STRING_ID_02;
		instance.broadcastPacket(new ExShowScreenMessage(bossText, ExShowScreenMessage.TOP_CENTER, 10000, true));
		
		// spawn boss
		final String bossTemplate = random ? "GustavMonsters_Boss_Gustav" : "GustavMonsters_Boss_GustavSteward";
		instance.spawnGroup(bossTemplate).forEach(n -> n.getSpawn().stopRespawn());
	}
	
	private void setRandomTitles(Instance instance, boolean randomMonsters)
	{
		if (randomMonsters)
		{
			for (int monsterId : MONSTERS)
			{
				// final Npc monster = instance.getNpcsOfGroup("GustavMonsters_2").stream().filter(npc -> npc.getTemplate().getId() == monsterId).findAny().orElse(instance.getNpc(monsterId));
				final List<Npc> monsters = instance.getNpcsOfGroup("GustavMonsters_2").stream().filter(npc -> npc.getTemplate().getId() == monsterId).toList();
				final Npc monster = monsters.isEmpty() ? instance.getNpc(monsterId) : monsters.get(Rnd.get(1, monsters.size()) - 1);
				monster.setTitleString(DREAM_WATCHER);
				monster.broadcastInfo();
			}
		}
		else
		{
			final List<Npc> dreamWatcherNpcs = instance.spawnGroup("GustavMonsters_3");
			dreamWatcherNpcs.forEach(n -> n.getSpawn().stopRespawn());
			for (Npc npc : dreamWatcherNpcs)
			{
				npc.setTitleString(DREAM_WATCHER);
				npc.broadcastInfo();
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (killer == null)
		{
			return;
		}
		
		final Instance instance = killer.getInstanceWorld();
		if ((instance == null) || (instance.getTemplateId() != INSTANCE_ID))
		{
			return;
		}
		
		switch (instance.getStatus())
		{
			case GO_TO_GATES_AND_KILL_GIRL:
			{
				if (instance.getAliveNpcs(MONSTERS).isEmpty())
				{
					instance.setStatus(TALK_WITH_RASCAL);
				}
				// just guess
				else if (npc.getTemplate().getId() == 22394)
				{
					instance.setStatus(TALK_WITH_RASCAL);
				}
				break;
			}
			case KILL_FOUR_DREAM_WATCHERS_INSIDE:
			{
				if (instance.getAliveNpcs(MONSTERS).isEmpty())
				{
					instance.setStatus(TALK_WITH_HORSIE_IN_MANOR);
					break;
				}
				
				if ((npc.getTitleString() == null) || !npc.getTitleString().equals(DREAM_WATCHER))
				{
					return;
				}
				
				final int dreamWatcherCount = instance.getParameters().increaseInt(DREAM_WATCHER_COUNTER, 0, 1);
				switch (dreamWatcherCount)
				{
					case 1:
					{
						instance.broadcastPacket(new ExShowScreenMessage(NpcStringId.DREAM_WATCHER_S_DEATH_1_4, ExShowScreenMessage.BOTTOM_RIGHT, 10000, false));
						break;
					}
					case 2:
					{
						instance.broadcastPacket(new ExShowScreenMessage(NpcStringId.DREAM_WATCHER_S_DEATH_2_4, ExShowScreenMessage.BOTTOM_RIGHT, 10000, false));
						break;
					}
					case 3:
					{
						instance.broadcastPacket(new ExShowScreenMessage(NpcStringId.DREAM_WATCHER_S_DEATH_3_4, ExShowScreenMessage.BOTTOM_RIGHT, 10000, false));
						break;
					}
					case 4:
					{
						instance.setStatus(TALK_WITH_HORSIE_IN_MANOR);
						break;
					}
				}
				break;
			}
			case BOSS_FIGHT:
			{
				if ((npc.getTemplate().getId() == GUSTAV) || (npc.getTemplate().getId() == GUSTAV_STEWARD))
				{
					if (!CatGuildsLair.calculateChanceForCatLair(instance))
					{
						instance.setStatus(FINISH_INSTANCE);
					}
				}
				break;
			}
		}
	}
	
	public static void main(String[] args)
	{
		new GustavsManor();
	}
}
