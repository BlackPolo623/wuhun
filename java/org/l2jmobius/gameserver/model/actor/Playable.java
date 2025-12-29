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
package org.l2jmobius.gameserver.model.actor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.ai.Action;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.model.actor.holders.player.AutoUseSettingsHolder;
import org.l2jmobius.gameserver.model.actor.stat.PlayableStat;
import org.l2jmobius.gameserver.model.actor.status.PlayableStatus;
import org.l2jmobius.gameserver.model.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanWar;
import org.l2jmobius.gameserver.model.clan.enums.ClanWarState;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.model.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.script.QuestState;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.AbnormalStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;

/**
 * This class represents all Playable characters in the world.<br>
 * Playable:
 * <ul>
 * <li>Player</li>
 * <li>Summon</li>
 * </ul>
 */
public abstract class Playable extends Creature
{
	private Creature _lockedTarget = null;
	private Player transferDmgTo = null;
	
	private final Map<Integer, Integer> _replacedSkills = new ConcurrentHashMap<>(1);
	private final Map<Integer, Integer> _originalSkills = new ConcurrentHashMap<>(1);
	
	/**
	 * Constructor of Playable.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Call the Creature constructor to create an empty _skills slot and link copy basic Calculator set to this Playable</li>
	 * </ul>
	 * @param objectId the object id
	 * @param template The CreatureTemplate to apply to the Playable
	 */
	public Playable(int objectId, CreatureTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.Playable);
		setInvul(false);
	}
	
	public Playable(CreatureTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Playable);
		setInvul(false);
	}
	
	@Override
	public PlayableStat getStat()
	{
		return (PlayableStat) super.getStat();
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new PlayableStat(this));
	}
	
	@Override
	public PlayableStatus getStatus()
	{
		return (PlayableStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new PlayableStatus(this));
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_DEATH, this))
		{
			final TerminateReturn returnBack = EventDispatcher.getInstance().notifyEvent(new OnCreatureDeath(killer, this), this, TerminateReturn.class);
			if ((returnBack != null) && returnBack.terminate())
			{
				return false;
			}
		}
		
		// killing is only possible one time
		synchronized (this)
		{
			if (isDead())
			{
				return false;
			}
			
			// now reset currentHp to zero
			setCurrentHp(0);
			setDead(true);
		}
		
		// Set target to null and cancel Attack or Cast
		setTarget(null);
		
		// Abort casting after target has been cancelled.
		abortAttack();
		abortCast();
		
		// Stop movement
		stopMove(null);
		
		// Stop HP/MP/CP Regeneration task
		getStatus().stopHpMpRegeneration();
		
		boolean deleteBuffs = true;
		if (isNoblesseBlessedAffected())
		{
			stopEffects(EffectFlag.NOBLESS_BLESSING);
			deleteBuffs = false;
		}
		
		if (isResurrectSpecialAffected())
		{
			stopEffects(EffectFlag.RESURRECTION_SPECIAL);
			deleteBuffs = false;
		}
		
		final Player player = asPlayer();
		if (isPlayer())
		{
			if (player.hasCharmOfCourage())
			{
				if (player.isInSiege())
				{
					player.reviveRequest(player, false, 0, 0, 0, 0);
				}
				
				player.setCharmOfCourage(false);
				player.sendPacket(new EtcStatusUpdate(player));
			}
		}
		
		if (deleteBuffs)
		{
			stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		
		// Send the Server->Client packet StatusUpdate with current HP and MP to all other Player to inform
		broadcastStatusUpdate();
		
		ZoneManager.getInstance().getRegion(this).onDeath(this);
		
		// Notify Quest of Playable's death
		if (!player.isNotifyQuestOfDeathEmpty())
		{
			for (QuestState qs : player.getNotifyQuestOfDeath())
			{
				qs.getQuest().onDeath(killer == null ? this : killer, this, qs);
			}
		}
		
		// Notify instance
		if (isPlayer())
		{
			final Instance instance = getInstanceWorld();
			if (instance != null)
			{
				instance.onDeath(player);
			}
		}
		
		if (killer != null)
		{
			final Player killerPlayer = killer.asPlayer();
			if (killerPlayer != null)
			{
				killerPlayer.onPlayerKill(this);
			}
		}
		
		// Notify Creature AI
		getAI().notifyAction(Action.DEATH);
		return true;
	}
	
	public boolean checkIfPvP(Player target)
	{
		final Player player = asPlayer();
		if ((player == null) //
			|| (target == null) //
			|| (player == target) //
			|| (target.getReputation() < 0) //
			|| (target.getPvpFlag() > 0) //
			|| target.isOnDarkSide())
		{
			return true;
		}
		else if (player.isInParty() && player.getParty().containsPlayer(target))
		{
			return false;
		}
		
		final Clan playerClan = player.getClan();
		if ((playerClan != null) && !player.isAcademyMember() && !target.isAcademyMember())
		{
			final ClanWar war = playerClan.getWarWith(target.getClanId());
			return (war != null) && (war.getState() == ClanWarState.MUTUAL);
		}
		
		return false;
	}
	
	/**
	 * Return True.
	 */
	@Override
	public boolean canBeAttacked()
	{
		return true;
	}
	
	// Support for Noblesse Blessing skill, where buffs are retained after resurrect
	public boolean isNoblesseBlessedAffected()
	{
		return isAffected(EffectFlag.NOBLESS_BLESSING);
	}
	
	/**
	 * @return {@code true} if char can resurrect by himself, {@code false} otherwise
	 */
	public boolean isResurrectSpecialAffected()
	{
		return isAffected(EffectFlag.RESURRECTION_SPECIAL);
	}
	
	/**
	 * @return {@code true} if the Silent Moving mode is active, {@code false} otherwise
	 */
	public boolean isSilentMovingAffected()
	{
		return isAffected(EffectFlag.SILENT_MOVE);
	}
	
	/**
	 * For Newbie Protection Blessing skill, keeps you safe from an attack by a chaotic character >= 10 levels apart from you.
	 * @return
	 */
	public boolean isProtectionBlessingAffected()
	{
		return isAffected(EffectFlag.PROTECTION_BLESSING);
	}
	
	@Override
	public void updateEffectIcons(boolean partyOnly)
	{
		getEffectList().updateEffectIcons(partyOnly);
	}
	
	public boolean isLockedTarget()
	{
		return _lockedTarget != null;
	}
	
	public Creature getLockedTarget()
	{
		return _lockedTarget;
	}
	
	public void setLockedTarget(Creature creature)
	{
		_lockedTarget = creature;
	}
	
	public void setTransferDamageTo(Player val)
	{
		transferDmgTo = val;
	}
	
	public Player getTransferingDamageTo()
	{
		return transferDmgTo;
	}
	
	/**
	 * Adds a replacement for an original skill.<br>
	 * Both original and replacement skill IDs are stored in their respective maps.
	 * @param originalId The ID of the original skill.
	 * @param replacementId The ID of the replacement skill.
	 */
	public void addReplacedSkill(int originalId, int replacementId)
	{
		_replacedSkills.put(originalId, replacementId);
		_originalSkills.put(replacementId, originalId);
		
		final Skill knownSkill = getKnownSkill(originalId);
		if (knownSkill == null)
		{
			return;
		}
		
		final Player player = asPlayer();
		final AutoUseSettingsHolder autoUseSettings = player.getAutoUseSettings();
		if (knownSkill.hasNegativeEffect())
		{
			final List<Integer> autoSkills = autoUseSettings.getAutoSkills();
			if (autoSkills.contains(originalId))
			{
				autoSkills.add(replacementId);
				autoSkills.remove(Integer.valueOf(originalId));
			}
		}
		else
		{
			final Collection<Integer> autoBuffs = autoUseSettings.getAutoBuffs();
			if (autoBuffs.contains(originalId))
			{
				autoBuffs.add(replacementId);
				autoBuffs.remove(originalId);
			}
		}
		
		// Replace continuous effects.
		if (knownSkill.isContinuous() && isAffectedBySkill(originalId))
		{
			int abnormalTime = 0;
			for (BuffInfo info : getEffectList().getEffects())
			{
				if (info.getSkill().getId() == originalId)
				{
					abnormalTime = info.getAbnormalTime();
					break;
				}
			}
			
			if (abnormalTime > 2000)
			{
				final Skill replacementkill = getKnownSkill(replacementId);
				if (replacementkill != null)
				{
					replacementkill.applyEffects(this, this);
					final AbnormalStatusUpdate asu = new AbnormalStatusUpdate();
					for (BuffInfo info : getEffectList().getEffects())
					{
						if (info.getSkill().getId() == replacementId)
						{
							info.resetAbnormalTime(abnormalTime);
							asu.addSkill(info);
						}
					}
					
					player.sendPacket(asu);
				}
			}
		}
		
		removeSkill(knownSkill, false);
		player.sendSkillList();
	}
	
	/**
	 * Removes a replaced skill by the original skill ID.<br>
	 * The corresponding replacement skill ID is also removed from its map.
	 * @param originalId The ID of the original skill to be removed.
	 */
	public void removeReplacedSkill(int originalId)
	{
		final Integer replacementId = _replacedSkills.remove(originalId);
		if (replacementId == null)
		{
			return;
		}
		
		_originalSkills.remove(replacementId);
		
		final Skill knownSkill = getKnownSkill(replacementId);
		if (knownSkill == null)
		{
			return;
		}
		
		final Player player = asPlayer();
		final AutoUseSettingsHolder autoUseSettings = player.getAutoUseSettings();
		if (knownSkill.hasNegativeEffect())
		{
			final List<Integer> autoSkills = autoUseSettings.getAutoSkills();
			if (autoSkills.contains(replacementId))
			{
				autoSkills.add(originalId);
				autoSkills.remove(Integer.valueOf(replacementId));
			}
		}
		else
		{
			final Collection<Integer> autoBuffs = autoUseSettings.getAutoBuffs();
			if (autoBuffs.contains(replacementId))
			{
				autoBuffs.add(originalId);
				autoBuffs.remove(replacementId);
			}
		}
		
		// Replace continuous effects.
		if (knownSkill.isContinuous() && isAffectedBySkill(replacementId))
		{
			int abnormalTime = 0;
			for (BuffInfo info : getEffectList().getEffects())
			{
				if (info.getSkill().getId() == replacementId)
				{
					abnormalTime = info.getAbnormalTime();
					break;
				}
			}
			
			if (abnormalTime > 2000)
			{
				final Skill originalskill = getKnownSkill(originalId);
				if (originalskill != null)
				{
					originalskill.applyEffects(this, this);
					final AbnormalStatusUpdate asu = new AbnormalStatusUpdate();
					for (BuffInfo info : getEffectList().getEffects())
					{
						if (info.getSkill().getId() == originalId)
						{
							info.resetAbnormalTime(abnormalTime);
							asu.addSkill(info);
						}
					}
					
					player.sendPacket(asu);
				}
			}
		}
		
		removeSkill(knownSkill, false);
		player.sendSkillList();
	}
	
	/**
	 * Retrieves the replacement skill for a given original skill.
	 * @param originalId The ID of the original skill.
	 * @return The ID of the replacement skill if it exists, or the original skill ID.
	 */
	public int getReplacementSkill(int originalId)
	{
		// Pet has not been restored yet.
		if (_replacedSkills == null)
		{
			return originalId;
		}
		
		int replacedSkillId = originalId;
		while (true)
		{
			final Integer nextId = _replacedSkills.get(replacedSkillId);
			if ((nextId == null) || (nextId == replacedSkillId))
			{
				break;
			}
			
			replacedSkillId = nextId;
		}
		
		return replacedSkillId;
	}
	
	/**
	 * Retrieves the original skill for a given replacement skill.
	 * @param replacementId The ID of the replacement skill.
	 * @return The ID of the original skill if it exists, or the replacement skill ID.
	 */
	public int getOriginalSkill(int replacementId)
	{
		// Pet has not been restored yet.
		if (_originalSkills == null)
		{
			return replacementId;
		}
		
		int originalSkillId = replacementId;
		while (true)
		{
			final Integer nextId = _originalSkills.get(originalSkillId);
			if ((nextId == null) || (nextId == originalSkillId))
			{
				break;
			}
			
			originalSkillId = nextId;
		}
		
		return originalSkillId;
	}
	
	/**
	 * Retrieves a collection of all original skills that have been replaced.
	 * @return The collection of all replaced skill IDs.
	 */
	public Collection<Integer> getReplacedSkills()
	{
		return _replacedSkills.keySet();
	}
	
	public abstract void doPickupItem(WorldObject object);
	
	public abstract boolean useMagic(Skill skill, Item item, boolean forceUse, boolean dontMove);
	
	public abstract void storeMe();
	
	public abstract void storeEffect(boolean storeEffects);
	
	public abstract void restoreEffects();
	
	public boolean isOnEvent()
	{
		return false;
	}
	
	@Override
	public boolean isPlayable()
	{
		return true;
	}
	
	@Override
	public Playable asPlayable()
	{
		return this;
	}
}
