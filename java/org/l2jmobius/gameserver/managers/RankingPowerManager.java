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
package org.l2jmobius.gameserver.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Decoy;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Serenitty
 */
public class RankingPowerManager
{
	private static final int COOLDOWN = 43200000;
	private static final int LEADER_STATUE = 18485;
	private static final SkillHolder LEADER_POWER = new SkillHolder(52018, 1);
	
	private Decoy _decoyInstance;
	private ScheduledFuture<?> _decoyTask;
	
	protected RankingPowerManager()
	{
		reset();
	}
	
	public void activatePower(Player player)
	{
		final Location location = player.getLocation();
		final List<Integer> array = new ArrayList<>(3);
		array.add(location.getX());
		array.add(location.getY());
		array.add(location.getZ());
		GlobalVariablesManager.getInstance().setIntegerList(GlobalVariablesManager.RANKING_POWER_LOCATION, array);
		GlobalVariablesManager.getInstance().set(GlobalVariablesManager.RANKING_POWER_COOLDOWN, System.currentTimeMillis() + COOLDOWN);
		createClone(player);
		cloneTask();
		final SystemMessage msg = new SystemMessage(SystemMessageId.THE_RANKING_LEADER_C1_HAS_USED_LEADER_S_POWER_IN_S2);
		msg.addString(player.getName());
		msg.addZoneName(location.getX(), location.getY(), location.getZ());
		Broadcast.toAllOnlinePlayers(msg);
	}
	
	private void createClone(Player player)
	{
		final Location location = player.getLocation();
		
		final NpcTemplate template = NpcData.getInstance().getTemplate(LEADER_STATUE);
		_decoyInstance = new Decoy(template, player, COOLDOWN, false);
		_decoyInstance.setTargetable(false);
		_decoyInstance.setImmobilized(true);
		_decoyInstance.setInvul(true);
		_decoyInstance.spawnMe(location.getX(), location.getY(), location.getZ());
		_decoyInstance.setHeading(location.getHeading());
		_decoyInstance.broadcastStatusUpdate();
		
		Quest.addSpawn(null, LEADER_STATUE, location, false, COOLDOWN);
	}
	
	private void cloneTask()
	{
		_decoyTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			World.getInstance().forEachVisibleObjectInRange(_decoyInstance, Player.class, 300, nearby ->
			{
				final BuffInfo info = nearby.getEffectList().getBuffInfoBySkillId(LEADER_POWER.getSkillId());
				if ((info == null) || (info.getTime() < (LEADER_POWER.getSkill().getAbnormalTime() - 60)))
				{
					nearby.sendPacket(new MagicSkillUse(_decoyInstance, nearby, LEADER_POWER.getSkillId(), LEADER_POWER.getSkillLevel(), 0, 0));
					LEADER_POWER.getSkill().applyEffects(_decoyInstance, nearby);
				}
			});
			if (Rnd.nextBoolean()) // Add some randomness?
			{
				ThreadPool.schedule(() -> _decoyInstance.broadcastSocialAction(2), 4500);
			}
		}, 1000, 10000);
		
		ThreadPool.schedule(this::reset, COOLDOWN);
	}
	
	public void reset()
	{
		if (_decoyTask != null)
		{
			_decoyTask.cancel(false);
			_decoyTask = null;
		}
		
		if (_decoyInstance != null)
		{
			_decoyInstance.deleteMe();
		}
		
		GlobalVariablesManager.getInstance().remove(GlobalVariablesManager.RANKING_POWER_COOLDOWN);
		GlobalVariablesManager.getInstance().remove(GlobalVariablesManager.RANKING_POWER_LOCATION);
	}
	
	public static RankingPowerManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RankingPowerManager INSTANCE = new RankingPowerManager();
	}
}
