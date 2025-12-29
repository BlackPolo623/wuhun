/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.model.actor.instance;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.DoppelgangerAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.effects.EffectFlag;
import org.l2jmobius.gameserver.model.olympiad.OlympiadGameManager;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExDamagePopUp;
import org.l2jmobius.gameserver.network.serverpackets.ExMagicAttackInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Nik
 */
public class Doppelganger extends Attackable
{
	private boolean _copySummonerEffects = true;
	private ScheduledFuture<?> _attackTask = null;
	private Creature _attackTarget = null;
	
	public Doppelganger(NpcTemplate template, Player owner)
	{
		super(template);
		
		setSummoner(owner);
		setCloneObjId(owner.getObjectId());
		setClanId(owner.getClanId());
		setInstance(owner.getInstanceWorld()); // set instance to same as owner
		setXYZInvisible(owner.getX() + Rnd.get(-100, 100), owner.getY() + Rnd.get(-100, 100), owner.getZ());
		((DoppelgangerAI) getAI()).setStartFollowController(true);
		followSummoner(true);
	}
	
	@Override
	protected CreatureAI initAI()
	{
		return new DoppelgangerAI(this);
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		if (_copySummonerEffects && (getSummoner() != null))
		{
			for (BuffInfo summonerInfo : getSummoner().getEffectList().getEffects())
			{
				if (summonerInfo.getAbnormalTime() > 0)
				{
					final BuffInfo info = new BuffInfo(getSummoner(), this, summonerInfo.getSkill(), false, null, null);
					info.setAbnormalTime(summonerInfo.getAbnormalTime());
					getEffectList().add(info);
				}
			}
		}
	}
	
	public void followSummoner(boolean followSummoner)
	{
		if (followSummoner)
		{
			if ((getAI().getIntention() == Intention.IDLE) || (getAI().getIntention() == Intention.ACTIVE))
			{
				setRunning();
				getAI().setIntention(Intention.FOLLOW, getSummoner());
			}
		}
		else if (getAI().getIntention() == Intention.FOLLOW)
		{
			getAI().setIntention(Intention.IDLE);
		}
	}
	
	public void setCopySummonerEffects(boolean copySummonerEffects)
	{
		_copySummonerEffects = copySummonerEffects;
	}
	
	public void stopAttackTask()
	{
		if ((_attackTask != null) && !_attackTask.isCancelled() && !_attackTask.isDone())
		{
			_attackTask.cancel(false);
			_attackTask = null;
			_attackTarget = null;
		}
	}
	
	public void startAttackTask(Creature target)
	{
		stopAttackTask();
		_attackTarget = target;
		_attackTask = ThreadPool.scheduleAtFixedRate(this::thinkCombat, 1000, 1000);
	}
	
	private void thinkCombat()
	{
		if (_attackTarget == null)
		{
			stopAttackTask();
			return;
		}
		
		doAutoAttack(_attackTarget);
		// TODO: Cast skills.
	}
	
	@Override
	public byte getPvpFlag()
	{
		return getSummoner() != null ? getSummoner().getPvpFlag() : 0;
	}
	
	@Override
	public Team getTeam()
	{
		return getSummoner() != null ? getSummoner().getTeam() : Team.NONE;
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return (getSummoner() != null) ? getSummoner().isAutoAttackable(attacker) : super.isAutoAttackable(attacker);
	}
	
	@Override
	public void doAttack(double damage, Creature target, Skill skill, boolean isDOT, boolean directlyToHp, boolean critical, boolean reflect)
	{
		super.doAttack(damage, target, skill, isDOT, directlyToHp, critical, reflect);
		sendDamageMessage(target, skill, (int) damage, 0, critical, false, false);
	}
	
	@Override
	public void sendDamageMessage(Creature target, Skill skill, int damage, double elementalDamage, boolean crit, boolean miss, boolean elementalCrit)
	{
		if (miss || (getSummoner() == null) || !getSummoner().isPlayer())
		{
			sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.DODGE));
			
			// target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.DODGE));
			return;
		}
		
		// Prevents the double spam of system messages, if the target is the owning player.
		if (target.getObjectId() != getSummoner().getObjectId())
		{
			final Player player = asPlayer();
			if (player.isInOlympiadMode() && (target.isPlayer()) && target.asPlayer().isInOlympiadMode() && (target.asPlayer().getOlympiadGameId() == player.getOlympiadGameId()))
			{
				OlympiadGameManager.getInstance().notifyCompetitorDamage(getSummoner().asPlayer(), damage);
			}
			
			final SystemMessage sm;
			if ((target.isHpBlocked() && !target.isNpc()) || (target.isPlayer() && target.isAffected(EffectFlag.DUELIST_FURY) && !player.isAffected(EffectFlag.FACEOFF)))
			{
				if (skill == null)
				{
					sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.IMMUNE));
					// target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), 0, ExDamagePopUp.IMMUNE));
				}
				else
				{
					sendPacket(new ExMagicAttackInfo(getObjectId(), target.getObjectId(), ExMagicAttackInfo.RESISTED));
					// target.sendPacket(new ExMagicAttackInfo(getObjectId(), target.getObjectId(), ExMagicAttackInfo.RESISTED));
				}
				
				sm = new SystemMessage(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
			}
			else
			{
				if (crit)
				{
					sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), damage, ExDamagePopUp.CRITICAL));
					target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), damage, ExDamagePopUp.CRITICAL));
				}
				else if (skill != null)
				{
					sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), damage, ExDamagePopUp.SKILL_HIT));
					target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), damage, ExDamagePopUp.SKILL_HIT));
				}
				else
				{
					sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), damage, ExDamagePopUp.NORMAL_ATTACK));
					target.sendPacket(new ExDamagePopUp(getObjectId(), target.getObjectId(), damage, ExDamagePopUp.NORMAL_ATTACK));
				}
				
				sm = new SystemMessage(SystemMessageId.C1_HAS_DEALT_S3_DAMAGE_TO_C2);
				sm.addNpcName(this);
				sm.addString(target.getName());
				sm.addInt(damage);
				sm.addPopup(target.getObjectId(), getObjectId(), (damage * -1));
			}
			
			sendPacket(sm);
		}
	}
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, Skill skill)
	{
		super.reduceCurrentHp(damage, attacker, skill);
		
		if ((getSummoner() != null) && getSummoner().isPlayer() && (attacker != null) && !isDead() && !isHpBlocked())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_RECEIVED_S3_DAMAGE_FROM_C2);
			sm.addNpcName(this);
			sm.addString(attacker.getName());
			sm.addInt((int) damage);
			sm.addPopup(getObjectId(), attacker.getObjectId(), (int) -damage);
			sendPacket(sm);
		}
	}
	
	@Override
	public Player asPlayer()
	{
		return getSummoner() != null ? getSummoner().asPlayer() : super.asPlayer();
	}
	
	@Override
	public boolean deleteMe()
	{
		stopAttackTask();
		return super.deleteMe();
	}
	
	@Override
	public void onTeleported()
	{
		deleteMe(); // In retail, doppelgangers disappear when summoner teleports.
	}
	
	@Override
	public void sendPacket(ServerPacket packet)
	{
		if (getSummoner() != null)
		{
			getSummoner().sendPacket(packet);
		}
	}
	
	@Override
	public void sendPacket(SystemMessageId id)
	{
		if (getSummoner() != null)
		{
			getSummoner().sendPacket(id);
		}
	}
	
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("(");
		sb.append(getId());
		sb.append(") Summoner: ");
		sb.append(getSummoner());
		return sb.toString();
	}
}
