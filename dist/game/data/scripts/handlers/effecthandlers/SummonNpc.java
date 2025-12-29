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
package handlers.effecthandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.Decoy;
import org.l2jmobius.gameserver.model.actor.instance.EffectPoint;
import org.l2jmobius.gameserver.model.actor.instance.Servitor;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

/**
 * Summon Npc effect implementation.
 * @author Zoey76, Liamxroy
 */
public class SummonNpc extends AbstractEffect
{
	private final int _despawnDelay;
	private final int _npcId;
	private final int _npcCount;
	private final boolean _randomOffset;
	private final boolean _isSummonSpawn;
	private final boolean _singleInstance; // Only one instance of this NPC is allowed.
	private final boolean _isAggressive;
	private final boolean _summonOnTarget;
	private final int _skillDelay;
	private final List<SkillHolder> _skills = new ArrayList<>(1);
	
	public SummonNpc(StatSet params)
	{
		_despawnDelay = params.getInt("despawnDelay", 0);
		_npcId = params.getInt("npcId", 0);
		_npcCount = params.getInt("npcCount", 1);
		_randomOffset = params.getBoolean("randomOffset", false);
		_isSummonSpawn = params.getBoolean("isSummonSpawn", false);
		_singleInstance = params.getBoolean("singleInstance", false);
		_isAggressive = params.getBoolean("aggressive", true); // Used by Decoy.
		_summonOnTarget = params.getBoolean("summonOnTarget", false); // Used for Effect point on RANGE skills.
		_skillDelay = params.getInt("skillDelay", 3000);
		if (params.contains("skills"))
		{
			for (String skill : params.getString("skills", "").split(";"))
			{
				final String[] split = skill.split(",");
				_skills.add(new SkillHolder(Integer.parseInt(split[0]), split.length == 1 ? 1 : Integer.parseInt(split[1])));
			}
		}
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SUMMON_NPC;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((_npcId <= 0) || (_npcCount <= 0))
		{
			LOGGER.warning(SummonNpc.class.getSimpleName() + ": Invalid NPC ID or count skill ID: " + skill.getId());
			return;
		}
		
		if (!effected.isPlayer() || effected.isAlikeDead())
		{
			return;
		}
		
		final Player player = effector.asPlayer();
		if (player.inObserverMode())
		{
			return;
		}
		
		if (player.isMounted())
		{
			return;
		}
		
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(_npcId);
		if (npcTemplate == null)
		{
			LOGGER.warning(SummonNpc.class.getSimpleName() + ": Spawn of the nonexisting NPC ID: " + _npcId + ", skill ID:" + skill.getId());
			return;
		}
		
		int x = player.getX();
		int y = player.getY();
		int z = player.getZ();
		if (skill.getTargetType() == TargetType.GROUND)
		{
			final Location wordPosition = player.getCurrentSkillWorldPosition();
			if (wordPosition != null)
			{
				x = wordPosition.getX();
				y = wordPosition.getY();
				z = wordPosition.getZ();
			}
		}
		else if (_summonOnTarget)
		{
			x = effected.getX();
			y = effected.getY();
			z = effected.getZ();
		}
		else
		{
			x = effector.getX();
			y = effector.getY();
			z = effector.getZ();
		}
		
		if (_randomOffset)
		{
			x += (Rnd.nextBoolean() ? Rnd.get(20, 50) : Rnd.get(-50, -20));
			y += (Rnd.nextBoolean() ? Rnd.get(20, 50) : Rnd.get(-50, -20));
		}
		
		// If only single instance is allowed, delete previous NPCs.
		if (_singleInstance)
		{
			for (Npc npc : player.getSummonedNpcs())
			{
				if (npc.getId() == _npcId)
				{
					npc.deleteMe();
				}
			}
			
			if (player.hasServitors())
			{
				for (Summon s : player.getServitors().values())
				{
					if (s.getId() == _npcId)
					{
						s.unSummon(player);
					}
				}
			}
		}
		
		switch (npcTemplate.getType())
		{
			case "Servitor":
			{
				final Servitor servitor = new Servitor(npcTemplate, player);
				servitor.setInvul(true);
				servitor.setSummoner(player);
				servitor.fullRestore();
				player.addServitor(servitor);
				player.setRecallCreature(servitor);
				servitor.setTitle(player.getName());
				
				final ScheduledFuture<?> skillTask = _skills.isEmpty() ? null : skillTask(servitor);
				servitor.spawnMe(x, y, z);
				
				if (_despawnDelay > 0)
				{
					ThreadPool.schedule(() ->
					{
						if (skillTask != null)
						{
							skillTask.cancel(false);
						}
						
						player.setRecallCreature(null);
						servitor.unSummon(player);
					}, _despawnDelay);
				}
				
				servitor.broadcastInfo();
				break;
			}
			case "Decoy":
			{
				final Decoy decoy = new Decoy(npcTemplate, player, _despawnDelay > 0 ? _despawnDelay : 20000, _isAggressive);
				decoy.fullRestore();
				decoy.setHeading(player.getHeading());
				decoy.setInstance(player.getInstanceWorld());
				decoy.setSummoner(player);
				decoy.spawnMe(x, y, z);
				break;
			}
			case "EffectPoint":
			{
				final EffectPoint effectPoint = new EffectPoint(npcTemplate, player);
				final ScheduledFuture<?> skillTask = _skills.isEmpty() ? null : skillTask(effectPoint);
				
				effectPoint.fullRestore();
				effectPoint.setInvul(true);
				effectPoint.setSummoner(player);
				effectPoint.setTitle(player.getName());
				effectPoint.spawnMe(x, y, z);
				player.setRecallCreature(effectPoint);
				
				// First consider NPC template despawn_time parameter.
				final long despawnTime = (long) (effectPoint.getParameters().getFloat("despawn_time", 0) * 1000);
				if (despawnTime > 0)
				{
					ThreadPool.schedule(() ->
					{
						if (skillTask != null)
						{
							skillTask.cancel(false);
						}
						
						player.setRecallCreature(null);
					}, despawnTime);
					effectPoint.scheduleDespawn(despawnTime);
				}
				else if (_despawnDelay > 0) // Use skill despawnDelay parameter.
				{
					ThreadPool.schedule(() ->
					{
						if (skillTask != null)
						{
							skillTask.cancel(false);
						}
						
						player.setRecallCreature(null);
					}, _despawnDelay);
					effectPoint.scheduleDespawn(_despawnDelay);
				}
				break;
			}
			default:
			{
				Spawn spawn;
				try
				{
					spawn = new Spawn(npcTemplate);
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, SummonNpc.class.getSimpleName() + ": Unable to create spawn. " + e.getMessage(), e);
					return;
				}
				
				spawn.setXYZ(x, y, z);
				spawn.setHeading(player.getHeading());
				spawn.stopRespawn();
				
				final Npc npc = spawn.doSpawn(_isSummonSpawn);
				final ScheduledFuture<?> skillTask = _skills.isEmpty() ? null : skillTask(npc);
				
				player.addSummonedNpc(npc);
				
				// npc.setSummoner(player);
				player.setRecallCreature(npc);
				npc.setName(npcTemplate.getName());
				npc.setTitle(npcTemplate.getName());
				if (_despawnDelay > 0)
				{
					if (skillTask != null)
					{
						ThreadPool.schedule(() -> skillTask.cancel(false), _despawnDelay);
					}
					
					player.setRecallCreature(null);
					npc.scheduleDespawn(_despawnDelay);
				}
				
				npc.broadcastInfo();
			}
		}
	}
	
	private ScheduledFuture<?> skillTask(Creature creature)
	{
		return ThreadPool.scheduleAtFixedRate(() -> World.getInstance().forEachVisibleObjectInRange(creature, Player.class, 300, nearby ->
		{
			if (!creature.isSpawned())
			{
				return;
			}
			
			for (SkillHolder holder : _skills)
			{
				final BuffInfo info = nearby.getEffectList().getBuffInfoBySkillId(holder.getSkillId());
				if ((info == null) || (info.getTime() < (holder.getSkill().getAbnormalTime() - 60)))
				{
					nearby.broadcastSkillPacket(new MagicSkillUse(creature, nearby, holder.getSkillId(), holder.getSkillLevel(), 0, 0), nearby);
					holder.getSkill().applyEffects(creature, nearby);
				}
			}
			
		}), 1000, _skillDelay);
	}
}
