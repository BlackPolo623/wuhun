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
package org.l2jmobius.gameserver.ai;

import java.util.concurrent.Future;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;

public class SummonAI extends PlayableAI implements Runnable
{
	private static final int AVOID_RADIUS = 70;
	
	private volatile boolean _thinking; // to prevent recursive thinking
	private volatile boolean _startFollow = _actor.asSummon().getFollowStatus();
	private Creature _lastAttack = null;
	
	private volatile boolean _startAvoid;
	private volatile boolean _isDefending;
	private Future<?> _avoidTask = null;
	
	// Fix: Infinite Atk. Spd. exploit
	private IntentionCommand _nextIntention = null;
	
	private void saveNextIntention(Intention intention, Object arg0, Object arg1)
	{
		_nextIntention = new IntentionCommand(intention, arg0, arg1);
	}
	
	@Override
	public IntentionCommand getNextIntention()
	{
		return _nextIntention;
	}
	
	public SummonAI(Summon summon)
	{
		super(summon);
	}
	
	@Override
	protected void onIntentionIdle()
	{
		stopFollow();
		_startFollow = false;
		onIntentionActive();
	}
	
	@Override
	protected void onIntentionActive()
	{
		final Summon summon = _actor.asSummon();
		if (_startFollow)
		{
			setIntention(Intention.FOLLOW, summon.getOwner());
		}
		else
		{
			super.onIntentionActive();
		}
	}
	
	@Override
	synchronized void changeIntention(Intention intention, Object... args)
	{
		switch (intention)
		{
			case ACTIVE:
			case FOLLOW:
			{
				startAvoidTask();
				break;
			}
			default:
			{
				stopAvoidTask();
			}
		}
		
		super.changeIntention(intention, args);
	}
	
	private void thinkAttack()
	{
		final WorldObject target = getTarget();
		final Creature attackTarget = (target != null) && target.isCreature() ? target.asCreature() : null;
		if (checkTargetLostOrDead(attackTarget))
		{
			setTarget(null);
			if (_startFollow)
			{
				_actor.asSummon().setFollowStatus(true);
			}
			return;
		}
		
		if (maybeMoveToPawn(attackTarget, _actor.getPhysicalAttackRange()))
		{
			return;
		}
		
		clientStopMoving(null);
		
		// Fix: Infinite Atk. Spd. exploit
		if (_actor.isAttackingNow())
		{
			saveNextIntention(Intention.ATTACK, attackTarget, null);
			return;
		}
		
		_actor.doAutoAttack(attackTarget);
	}
	
	private void thinkCast()
	{
		final Summon summon = _actor.asSummon();
		if (summon.isCastingNow(SkillCaster::isAnyNormalType))
		{
			return;
		}
		
		final WorldObject target = getCastTarget();
		if (checkTargetLost(target))
		{
			setTarget(null);
			setCastTarget(null);
			summon.setFollowStatus(true);
			return;
		}
		
		final boolean val = _startFollow;
		if (maybeMoveToPawn(target, _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		summon.setFollowStatus(false);
		setIntention(Intention.IDLE);
		_startFollow = val;
		_actor.doCast(_skill, _item, _skill.hasNegativeEffect(), _dontMove);
	}
	
	private void thinkPickUp()
	{
		final WorldObject target = getTarget();
		if (checkTargetLost(target))
		{
			return;
		}
		
		if (maybeMoveToPawn(target, 36))
		{
			return;
		}
		
		setIntention(Intention.IDLE);
		getActor().doPickupItem(target);
	}
	
	private void thinkInteract()
	{
		final WorldObject target = getTarget();
		if (checkTargetLost(target))
		{
			return;
		}
		
		if (maybeMoveToPawn(target, 36))
		{
			return;
		}
		
		setIntention(Intention.IDLE);
	}
	
	@Override
	public void onActionThink()
	{
		if (_thinking || _actor.isCastingNow() || _actor.isAllSkillsDisabled())
		{
			return;
		}
		
		_thinking = true;
		try
		{
			switch (getIntention())
			{
				case ATTACK:
				{
					thinkAttack();
					break;
				}
				case CAST:
				{
					thinkCast();
					break;
				}
				case PICK_UP:
				{
					thinkPickUp();
					break;
				}
				case INTERACT:
				{
					thinkInteract();
					break;
				}
			}
		}
		finally
		{
			_thinking = false;
		}
	}
	
	@Override
	protected void onActionFinishCasting()
	{
		if (_lastAttack == null)
		{
			_actor.asSummon().setFollowStatus(_startFollow);
		}
		else
		{
			setIntention(Intention.ATTACK, _lastAttack);
			_lastAttack = null;
		}
	}
	
	@Override
	protected void onActionAttacked(Creature attacker)
	{
		super.onActionAttacked(attacker);
		
		if (_isDefending)
		{
			allServitorsDefend(attacker);
		}
		else
		{
			avoidAttack(attacker);
		}
	}
	
	@Override
	protected void onActionEvaded(Creature attacker)
	{
		super.onActionEvaded(attacker);
		
		if (_isDefending)
		{
			allServitorsDefend(attacker);
		}
		else
		{
			avoidAttack(attacker);
		}
	}
	
	private void allServitorsDefend(Creature attacker)
	{
		final Creature owner = getActor().getOwner();
		if ((owner != null) && owner.asPlayer().hasServitors())
		{
			for (Summon summon : owner.asPlayer().getServitors().values())
			{
				final SummonAI ai = (SummonAI) summon.getAI();
				if (ai.isDefending())
				{
					ai.defendAttack(attacker);
				}
			}
		}
		else
		{
			defendAttack(attacker);
		}
	}
	
	private void avoidAttack(Creature attacker)
	{
		// Don't move while casting. It breaks casting animation, but still casts the skill... looks so bugged.
		if (_actor.isCastingNow())
		{
			return;
		}
		
		final Creature owner = getActor().getOwner();
		
		// trying to avoid if summon near owner
		if ((owner != null) && (owner != attacker) && owner.isInsideRadius3D(_actor, 2 * AVOID_RADIUS))
		{
			_startAvoid = true;
		}
	}
	
	public void defendAttack(Creature attacker)
	{
		// Cannot defend while attacking or casting.
		if (_actor.isAttackingNow() || _actor.isCastingNow())
		{
			return;
		}
		
		final Summon summon = getActor();
		final Player owner = summon.getOwner();
		if (owner != null)
		{
			if (summon.calculateDistance3D(owner) > 3000)
			{
				summon.getAI().setIntention(Intention.FOLLOW, owner);
			}
			else if ((owner != attacker) && !summon.isMoving() && summon.canAttack(attacker, false))
			{
				summon.doAttack(attacker);
			}
		}
	}
	
	@Override
	public void run()
	{
		if (_startAvoid)
		{
			_startAvoid = false;
			if (!_actor.isMoving() && !_actor.isDead() && !_actor.isMovementDisabled() && (_actor.getMoveSpeed() > 0))
			{
				final int ownerX = _actor.asSummon().getOwner().getX();
				final int ownerY = _actor.asSummon().getOwner().getY();
				final double angle = Math.toRadians(Rnd.get(-90, 90)) + Math.atan2(ownerY - _actor.getY(), ownerX - _actor.getX());
				final int targetX = ownerX + (int) (AVOID_RADIUS * Math.cos(angle));
				final int targetY = ownerY + (int) (AVOID_RADIUS * Math.sin(angle));
				if (GeoEngine.getInstance().canMoveToTarget(_actor.getX(), _actor.getY(), _actor.getZ(), targetX, targetY, _actor.getZ(), _actor.getInstanceWorld()))
				{
					moveTo(targetX, targetY, _actor.getZ());
				}
			}
		}
	}
	
	public void notifyFollowStatusChange()
	{
		_startFollow = !_startFollow;
		switch (getIntention())
		{
			case ACTIVE:
			case FOLLOW:
			case IDLE:
			case MOVE_TO:
			case PICK_UP:
			{
				_actor.asSummon().setFollowStatus(_startFollow);
			}
		}
	}
	
	public void setStartFollowController(boolean value)
	{
		_startFollow = value;
	}
	
	@Override
	protected void onIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		if (getIntention() == Intention.ATTACK)
		{
			_lastAttack = (getTarget() != null) && getTarget().isCreature() ? getTarget().asCreature() : null;
		}
		else
		{
			_lastAttack = null;
		}
		
		super.onIntentionCast(skill, target, item, forceUse, dontMove);
	}
	
	private void startAvoidTask()
	{
		if (_avoidTask == null)
		{
			_avoidTask = ThreadPool.scheduleAtFixedRate(this, 100, 100);
		}
	}
	
	private void stopAvoidTask()
	{
		if (_avoidTask != null)
		{
			_avoidTask.cancel(false);
			_avoidTask = null;
		}
	}
	
	@Override
	public void stopAITask()
	{
		stopAvoidTask();
		super.stopAITask();
	}
	
	@Override
	public Summon getActor()
	{
		return super.getActor().asSummon();
	}
	
	/**
	 * @return if the summon is defending itself or master.
	 */
	public boolean isDefending()
	{
		return _isDefending;
	}
	
	/**
	 * @param isDefending set the summon to defend itself and master, or be passive and avoid while being attacked.
	 */
	public void setDefending(boolean isDefending)
	{
		_isDefending = isDefending;
	}
}
