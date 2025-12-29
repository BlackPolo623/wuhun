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
package org.l2jmobius.gameserver.ai;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.managers.WalkingManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.WorldRegion;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.actor.transform.Transform;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.npc.OnNpcMoveFinished;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.item.enums.ItemLocation;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.AutoAttackStop;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * This class manages AI of Creature.<br>
 * CreatureAI :
 * <ul>
 * <li>AttackableAI</li>
 * <li>DoorAI</li>
 * <li>PlayerAI</li>
 * <li>SummonAI</li>
 * </ul>
 */
public class CreatureAI extends AbstractAI
{
	private OnNpcMoveFinished _onNpcMoveFinished = null;
	
	public static class IntentionCommand
	{
		protected final Intention _intention;
		protected final Object _arg0;
		protected final Object _arg1;
		
		protected IntentionCommand(Intention pIntention, Object pArg0, Object pArg1)
		{
			_intention = pIntention;
			_arg0 = pArg0;
			_arg1 = pArg1;
		}
		
		public Intention getIntention()
		{
			return _intention;
		}
	}
	
	/**
	 * Cast Task
	 * @author Zoey76
	 */
	public static class CastTask implements Runnable
	{
		private final Creature _creature;
		private final WorldObject _target;
		private final Skill _skill;
		private final Item _item;
		private final boolean _forceUse;
		private final boolean _dontMove;
		
		public CastTask(Creature actor, Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
		{
			_creature = actor;
			_target = target;
			_skill = skill;
			_item = item;
			_forceUse = forceUse;
			_dontMove = dontMove;
		}
		
		@Override
		public void run()
		{
			if (_creature.isAttackingNow())
			{
				_creature.abortAttack();
			}
			
			_creature.getAI().changeIntentionToCast(_skill, _target, _item, _forceUse, _dontMove);
		}
	}
	
	public CreatureAI(Creature creature)
	{
		super(creature);
	}
	
	public IntentionCommand getNextIntention()
	{
		return null;
	}
	
	@Override
	protected void onActionAttacked(Creature attacker)
	{
		clientStartAutoAttack();
	}
	
	/**
	 * Manage the Idle Intention : Stop Attack, Movement and Stand Up the actor.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Set the AI Intention to IDLE</li>
	 * <li>Init cast and attack target</li>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Stand up the actor server side AND client side by sending Server->Client packet ChangeWaitType (broadcast)</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionIdle()
	{
		// Set the AI Intention to IDLE
		changeIntention(Intention.IDLE);
		
		// Init cast target
		setCastTarget(null);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
	}
	
	/**
	 * Manage the Active Intention : Stop Attack, Movement and Launch Think Action.<br>
	 * <br>
	 * <b><u>Actions</u> : <i>if the Intention is not already Active</i></b>
	 * <ul>
	 * <li>Set the AI Intention to ACTIVE</li>
	 * <li>Init cast and attack target</li>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Launch the Think Action</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionActive()
	{
		// Check if the Intention is not already Active
		if (getIntention() == Intention.ACTIVE)
		{
			return;
		}
		
		// Set the AI Intention to ACTIVE
		changeIntention(Intention.ACTIVE);
		
		// Check if region and its neighbors are active.
		final WorldRegion region = _actor.getWorldRegion();
		if ((region == null) || !region.areNeighborsActive())
		{
			return;
		}
		
		// Init cast target
		setCastTarget(null);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		// Launch the Think Action
		onActionThink();
	}
	
	/**
	 * Manage the Rest Intention.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Set the AI Intention to IDLE</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionRest()
	{
		// Set the AI Intention to IDLE
		setIntention(Intention.IDLE);
	}
	
	/**
	 * Manage the Attack Intention : Stop current Attack (if necessary), Start a new Attack and Launch Think Action.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the Intention of this AI to ATTACK</li>
	 * <li>Set or change the AI attack target</li>
	 * <li>Start the actor Auto Attack client side by sending Server->Client packet AutoAttackStart (broadcast)</li>
	 * <li>Launch the Think Action</li>
	 * </ul>
	 * <br>
	 * <b><u>Overridden in</u>:</b>
	 * <ul>
	 * <li>AttackableAI : Calculate attack timeout</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionAttack(Creature target)
	{
		if ((target == null) || !target.isTargetable())
		{
			clientActionFailed();
			return;
		}
		
		if (getIntention() == Intention.REST)
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow() || _actor.isControlBlocked())
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		// Check if the Intention is already ATTACK
		if (getIntention() == Intention.ATTACK)
		{
			// Check if the AI already targets the Creature
			if (getTarget() != target)
			{
				// Set the AI attack target (change target)
				setTarget(target);
				
				// stopFollow();
				
				// Launch the Think Action
				notifyAction(Action.THINK, null);
			}
			else
			{
				clientActionFailed(); // else client freezes until cancel target
			}
		}
		else
		{
			// Set the Intention of this AbstractAI to ATTACK
			changeIntention(Intention.ATTACK, target);
			
			// Set the AI attack target
			setTarget(target);
			
			// stopFollow();
			
			// Launch the Think Action
			notifyAction(Action.THINK, null);
		}
	}
	
	/**
	 * Manage the Cast Intention : Stop current Attack, Init the AI in order to cast and Launch Think Action.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Set the AI cast target</li>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Cancel action client side by sending Server->Client packet ActionFailed to the Player actor</li>
	 * <li>Set the AI skill used by INTENTION_CAST</li>
	 * <li>Set the Intention of this AI to CAST</li>
	 * <li>Launch the Think Action</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		if ((getIntention() == Intention.REST) && skill.isMagic())
		{
			clientActionFailed();
			return;
		}
		
		final long currentTime = System.nanoTime();
		final long attackEndTime = _actor.getAttackEndTime();
		if (attackEndTime > currentTime)
		{
			ThreadPool.schedule(new CastTask(_actor, skill, target, item, forceUse, dontMove), TimeUnit.NANOSECONDS.toMillis(attackEndTime - currentTime));
		}
		else
		{
			changeIntentionToCast(skill, target, item, forceUse, dontMove);
		}
	}
	
	protected void changeIntentionToCast(Skill skill, WorldObject target, Item item, boolean forceUse, boolean dontMove)
	{
		// Set the AI cast target
		setCastTarget(target);
		
		// Set the AI skill used by INTENTION_CAST
		_skill = skill;
		
		// Set the AI item that triggered this skill
		_item = item;
		
		// Set the ctrl/shift pressed parameters
		_forceUse = forceUse;
		_dontMove = dontMove;
		
		// Change the Intention of this AbstractAI to CAST
		changeIntention(Intention.CAST, skill);
		
		// Launch the Think Action
		notifyAction(Action.THINK, null);
	}
	
	/**
	 * Manage the Move To Intention : Stop current Attack and Launch a Move to Location Task.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the Intention of this AI to MOVE_TO</li>
	 * <li>Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionMoveTo(ILocational loc)
	{
		if (getIntention() == Intention.REST)
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		// Set the Intention of this AbstractAI to MOVE_TO
		changeIntention(Intention.MOVE_TO, loc);
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		// Abort the attack of the Creature and send Server->Client ActionFailed packet
		_actor.abortAttack();
		
		// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet MoveToLocation (broadcast)
		moveTo(loc.getX(), loc.getY(), loc.getZ());
	}
	
	/**
	 * Manage the Follow Intention : Stop current Attack and Launch a Follow Task.<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the Intention of this AI to FOLLOW</li>
	 * <li>Create and Launch an AI Follow Task to execute every 1s</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionFollow(Creature target)
	{
		if (getIntention() == Intention.REST)
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		if (_actor.isMovementDisabled() || (_actor.getMoveSpeed() <= 0))
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		// Dead actors can`t follow
		if (_actor.isDead())
		{
			clientActionFailed();
			return;
		}
		
		// do not follow yourself
		if (_actor == target)
		{
			clientActionFailed();
			return;
		}
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		// Set the Intention of this AbstractAI to FOLLOW
		changeIntention(Intention.FOLLOW, target);
		
		// Create and Launch an AI Follow Task to execute every 1s
		startFollow(target);
	}
	
	/**
	 * Manage the PickUp Intention : Set the pick up target and Launch a Move To Pawn Task (offset=20).<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Set the AI pick up target</li>
	 * <li>Set the Intention of this AI to PICK_UP</li>
	 * <li>Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionPickUp(WorldObject object)
	{
		if (getIntention() == Intention.REST)
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		if (object.isItem() && (((Item) object).getItemLocation() != ItemLocation.VOID))
		{
			return;
		}
		
		// Set the Intention of this AbstractAI to PICK_UP
		changeIntention(Intention.PICK_UP, object);
		
		// Set the AI pick up target
		setTarget(object);
		
		if ((object.getX() == 0) && (object.getY() == 0))
		{
			// LOGGER.warning("Object in coords 0,0 - using a temporary fix");
			object.setXYZ(getActor().getX(), getActor().getY(), getActor().getZ() + 5);
		}
		
		// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
		moveToPawn(object, 20);
	}
	
	/**
	 * Manage the Interact Intention : Set the interact target and Launch a Move To Pawn Task (offset=60).<br>
	 * <br>
	 * <b><u>Actions</u> : </b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Set the AI interact target</li>
	 * <li>Set the Intention of this AI to INTERACT</li>
	 * <li>Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
	 * </ul>
	 */
	@Override
	protected void onIntentionInteract(WorldObject object)
	{
		if (getIntention() == Intention.REST)
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		if (_actor.isAllSkillsDisabled() || _actor.isCastingNow())
		{
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}
		
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();
		
		if (getIntention() != Intention.INTERACT)
		{
			// Set the Intention of this AbstractAI to INTERACT
			changeIntention(Intention.INTERACT, object);
			
			// Set the AI interact target
			setTarget(object);
			
			// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
			moveToPawn(object, 60);
		}
	}
	
	/**
	 * Do nothing.
	 */
	@Override
	public void onActionThink()
	{
		// do nothing
	}
	
	/**
	 * Do nothing.
	 */
	@Override
	protected void onActionAggression(Creature target, int aggro)
	{
		// do nothing
	}
	
	/**
	 * Launch actions corresponding to the Action Stunned then onAttacked Action.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Break an attack and send Server->Client ActionFailed packet and a System Message to the Creature</li>
	 * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the Creature</li>
	 * <li>Launch actions corresponding to the Action onAttacked (only for AttackableAI after the stunning periode)</li>
	 * </ul>
	 */
	@Override
	protected void onActionBlocked(Creature attacker)
	{
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		}
		
		// Stop Server AutoAttack also
		setAutoAttacking(false);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
	}
	
	/**
	 * Launch actions corresponding to the Action Rooted.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Launch actions corresponding to the Action onAttacked</li>
	 * </ul>
	 */
	@Override
	protected void onActionRooted(Creature attacker)
	{
		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		// _actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		// if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		// AttackStanceTaskManager.getInstance().removeAttackStanceTask(_actor);
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Launch actions corresponding to the Action onAttacked
		onActionAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Action Confused.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Launch actions corresponding to the Action onAttacked</li>
	 * </ul>
	 */
	@Override
	protected void onActionConfused(Creature attacker)
	{
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Launch actions corresponding to the Action onAttacked
		onActionAttacked(attacker);
	}
	
	/**
	 * Launch actions corresponding to the Action Muted.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Break a cast and send Server->Client ActionFailed packet and a System Message to the Creature</li>
	 * </ul>
	 */
	@Override
	protected void onActionMuted(Creature attacker)
	{
		// Break a cast and send Server->Client ActionFailed packet and a System Message to the Creature
		onActionAttacked(attacker);
	}
	
	/**
	 * Do nothing.
	 */
	@Override
	protected void onActionEvaded(Creature attacker)
	{
		// do nothing
	}
	
	/**
	 * Launch actions corresponding to the Action ReadyToAct.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	protected void onActionReadyToAct()
	{
		// Launch actions corresponding to the Action Think
		onActionThink();
	}
	
	/**
	 * Launch actions corresponding to the Action Arrived.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>If the Intention was MOVE_TO, set the Intention to ACTIVE</li>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	protected void onActionArrived()
	{
		getActor().revalidateZone(true);
		
		if (getActor().moveToNextRoutePoint())
		{
			return;
		}
		
		clientStoppedMoving();
		
		// If the Intention was MOVE_TO, set the Intention to ACTIVE
		if (getIntention() == Intention.MOVE_TO)
		{
			setIntention(Intention.ACTIVE);
		}
		
		if (_actor.isNpc())
		{
			final Npc npc = _actor.asNpc();
			WalkingManager.getInstance().onArrived(npc); // Walking Manager support
			
			// Notify to scripts
			if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_MOVE_FINISHED, npc))
			{
				if (_onNpcMoveFinished == null)
				{
					_onNpcMoveFinished = new OnNpcMoveFinished(npc);
				}
				
				EventDispatcher.getInstance().notifyEventAsync(_onNpcMoveFinished, npc);
			}
		}
		
		// Launch actions corresponding to the Action Think
		onActionThink();
	}
	
	/**
	 * Launch actions corresponding to the Action ArrivedRevalidate.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	protected void onActionArrivedRevalidate()
	{
		// Launch actions corresponding to the Action Think
		onActionThink();
	}
	
	/**
	 * Launch actions corresponding to the Action ArrivedBlocked.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>If the Intention was MOVE_TO, set the Intention to ACTIVE</li>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	protected void onActionArrivedBlocked(Location location)
	{
		// If the Intention was MOVE_TO, set the Intention to ACTIVE
		if ((getIntention() == Intention.MOVE_TO) || (getIntention() == Intention.CAST))
		{
			setIntention(Intention.ACTIVE);
		}
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(location);
		
		// Launch actions corresponding to the Action Think
		onActionThink();
	}
	
	/**
	 * Launch actions corresponding to the Action ForgetObject.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>If the object was targeted and the Intention was INTERACT or PICK_UP, set the Intention to ACTIVE</li>
	 * <li>If the object was targeted to attack, stop the auto-attack, cancel target and set the Intention to ACTIVE</li>
	 * <li>If the object was targeted to cast, cancel target and set the Intention to ACTIVE</li>
	 * <li>If the object was targeted to follow, stop the movement, cancel AI Follow Task and set the Intention to ACTIVE</li>
	 * <li>If the targeted object was the actor , cancel AI target, stop AI Follow Task, stop the movement and set the Intention to IDLE</li>
	 * </ul>
	 */
	@Override
	protected void onActionForgetObject(WorldObject object)
	{
		final WorldObject target = getTarget();
		
		// Stop any casting pointing to this object.
		getActor().abortCast(sc -> sc.getTarget() == object);
		
		// If the object was targeted and the Intention was INTERACT or PICK_UP, set the Intention to ACTIVE
		if (target == object)
		{
			setTarget(null);
			
			if (isFollowing())
			{
				// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
				clientStopMoving(null);
				
				// Stop an AI Follow Task
				stopFollow();
			}
			
			// Stop any intention that has target we want to forget.
			if (getIntention() != Intention.MOVE_TO)
			{
				setIntention(Intention.ACTIVE);
			}
		}
		
		// Check if the targeted object was the actor
		if (_actor == object)
		{
			// Cancel AI target
			setTarget(null);
			
			// Init cast target
			setCastTarget(null);
			
			// Stop an AI Follow Task
			stopFollow();
			
			// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
			clientStopMoving(null);
			
			// Set the Intention of this AbstractAI to IDLE
			changeIntention(Intention.IDLE);
		}
	}
	
	/**
	 * Launch actions corresponding to the Action Cancel.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop an AI Follow Task</li>
	 * <li>Launch actions corresponding to the Action Think</li>
	 * </ul>
	 */
	@Override
	protected void onActionCancel()
	{
		_actor.abortCast();
		
		// Stop an AI Follow Task
		stopFollow();
		
		if (!AttackStanceTaskManager.getInstance().hasAttackStanceTask(_actor))
		{
			_actor.broadcastPacket(new AutoAttackStop(_actor.getObjectId()));
		}
		
		// Launch actions corresponding to the Action Think
		onActionThink();
	}
	
	/**
	 * Launch actions corresponding to the Action Dead.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop an AI Follow Task</li>
	 * <li>Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast)</li>
	 * </ul>
	 */
	@Override
	protected void onActionDeath()
	{
		// Stop an AI Tasks
		stopAITask();
		
		if (_actor.isNpc())
		{
			_actor.asNpc().setDisplayEffect(0);
		}
		
		// Kill the actor client side by sending Server->Client packet AutoAttackStop, StopMove/StopRotation, Die (broadcast)
		clientNotifyDead();
		
		if (!_actor.isPlayable() && !_actor.isFakePlayer())
		{
			_actor.setWalking();
		}
	}
	
	/**
	 * Launch actions corresponding to the Action Fake Death.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Stop an AI Follow Task</li>
	 * </ul>
	 */
	@Override
	protected void onActionFakeDeath()
	{
		// Stop an AI Follow Task
		stopFollow();
		
		// Stop the actor movement and send Server->Client packet StopMove/StopRotation (broadcast)
		clientStopMoving(null);
		
		// Init AI
		_intention = Intention.IDLE;
		setTarget(null);
		setCastTarget(null);
	}
	
	/**
	 * Do nothing.
	 */
	@Override
	protected void onActionFinishCasting()
	{
		// do nothing
	}
	
	protected boolean maybeMoveToPosition(ILocational worldPosition, int offset)
	{
		if (worldPosition == null)
		{
			// LOGGER.warning("maybeMoveToPosition: worldPosition == NULL!");
			return false;
		}
		
		if (offset < 0)
		{
			return false; // skill radius -1
		}
		
		if (!_actor.isInsideRadius2D(worldPosition, offset + _actor.getTemplate().getCollisionRadius()))
		{
			if (_actor.isMovementDisabled() || (_actor.getMoveSpeed() <= 0))
			{
				return true;
			}
			
			if (!_actor.isRunning() && !(this instanceof PlayerAI) && !(this instanceof SummonAI))
			{
				_actor.setRunning();
			}
			
			stopFollow();
			
			int x = _actor.getX();
			int y = _actor.getY();
			
			final double dx = worldPosition.getX() - x;
			final double dy = worldPosition.getY() - y;
			double dist = Math.hypot(dx, dy);
			
			final double sin = dy / dist;
			final double cos = dx / dist;
			dist -= offset - 5;
			x += (int) (dist * cos);
			y += (int) (dist * sin);
			moveTo(x, y, worldPosition.getZ());
			return true;
		}
		
		if (isFollowing())
		{
			stopFollow();
		}
		
		return false;
	}
	
	/**
	 * Manage the Move to Pawn action in function of the distance and of the Interact area.<br>
	 * <br>
	 * <b><u>Actions</u>:</b>
	 * <ul>
	 * <li>Get the distance between the current position of the Creature and the target (x,y)</li>
	 * <li>If the distance > offset+20, move the actor (by running) to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)</li>
	 * <li>If the distance <= offset+20, Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * </ul>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>PLayerAI, SummonAI</li>
	 * </ul>
	 * @param target The targeted WorldObject
	 * @param offsetValue The Interact area radius
	 * @return True if a movement must be done
	 */
	protected boolean maybeMoveToPawn(WorldObject target, int offsetValue)
	{
		// Get the distance between the current position of the Creature and the target (x,y)
		if (target == null)
		{
			// LOGGER.warning("maybeMoveToPawn: target == NULL!");
			return false;
		}
		
		if (offsetValue < 0)
		{
			return false; // skill radius -1
		}
		
		int offsetWithCollision = offsetValue + _actor.getTemplate().getCollisionRadius();
		if (target.isCreature())
		{
			offsetWithCollision += target.asCreature().getTemplate().getCollisionRadius();
		}
		
		if (!_actor.isInsideRadius2D(target, offsetWithCollision))
		{
			// Caller should be Playable and thinkAttack/thinkCast/thinkInteract/thinkPickUp
			if (isFollowing())
			{
				// allow larger hit range when the target is moving (check is run only once per second)
				if (!_actor.isInsideRadius2D(target, offsetWithCollision + 100))
				{
					return true;
				}
				
				stopFollow();
				return false;
			}
			
			if (_actor.isMovementDisabled() || (_actor.getMoveSpeed() <= 0))
			{
				// If player is trying attack target but he cannot move to attack target
				// change his intention to idle
				if (_actor.getAI().getIntention() == Intention.ATTACK)
				{
					_actor.getAI().setIntention(Intention.IDLE);
				}
				
				return true;
			}
			
			// While flying there is no move to cast.
			if (_actor.isPlayer() && (_actor.getAI().getIntention() == Intention.CAST))
			{
				final Transform transform = _actor.getTransformation();
				if ((transform != null) && !transform.canUseWeaponStats())
				{
					_actor.sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_CANCELLED);
					_actor.sendPacket(ActionFailed.STATIC_PACKET);
					return true;
				}
			}
			
			// If not running, set the Creature movement type to run and send Server->Client packet ChangeMoveType to all others Player
			if (!_actor.isRunning() && !(this instanceof PlayerAI) && !(this instanceof SummonAI))
			{
				_actor.setRunning();
			}
			
			stopFollow();
			int offset = offsetValue;
			if (target.isCreature() && !target.isDoor())
			{
				if (target.asCreature().isMoving())
				{
					offset -= 100;
				}
				
				if (offset < 5)
				{
					offset = 5;
				}
				
				startFollow(target.asCreature(), offset);
			}
			else
			{
				// Move the actor to Pawn server side AND client side by sending Server->Client packet MoveToPawn (broadcast)
				moveToPawn(target, offset);
			}
			
			return true;
		}
		
		if (isFollowing())
		{
			stopFollow();
		}
		
		// Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)
		// clientStopMoving(null);
		return false;
	}
	
	/**
	 * Modify current Intention and actions if the target is lost or dead.<br>
	 * <br>
	 * <b><u>Actions</u> : <i>If the target is lost or dead</i></b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Set the Intention of this AbstractAI to ACTIVE</li>
	 * </ul>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>PLayerAI, SummonAI</li>
	 * </ul>
	 * @param target The targeted WorldObject
	 * @return True if the target is lost or dead (false if fakedeath)
	 */
	protected boolean checkTargetLostOrDead(Creature target)
	{
		if ((target == null) || target.isDead())
		{
			setIntention(Intention.ACTIVE);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Modify current Intention and actions if the target is lost.<br>
	 * <br>
	 * <b><u>Actions</u> : <i>If the target is lost</i></b>
	 * <ul>
	 * <li>Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)</li>
	 * <li>Stop the actor movement server side AND client side by sending Server->Client packet StopMove/StopRotation (broadcast)</li>
	 * <li>Set the Intention of this AbstractAI to ACTIVE</li>
	 * </ul>
	 * <br>
	 * <b><u>Example of use</u>:</b>
	 * <ul>
	 * <li>PlayerAI, SummonAI</li>
	 * </ul>
	 * @param target The targeted WorldObject
	 * @return True if the target is lost
	 */
	protected boolean checkTargetLost(WorldObject target)
	{
		if (target == null)
		{
			setIntention(Intention.ACTIVE);
			return true;
		}
		
		if (_actor != null)
		{
			if ((_skill != null) && _skill.hasNegativeEffect() && (_skill.getAffectRange() > 0))
			{
				if (_actor.isPlayer() && _actor.isMoving())
				{
					if (!GeoEngine.getInstance().canMoveToTarget(_actor, target))
					{
						setIntention(Intention.ACTIVE);
						return true;
					}
				}
				else
				{
					if (!GeoEngine.getInstance().canSeeTarget(_actor, target))
					{
						setIntention(Intention.ACTIVE);
						return true;
					}
				}
			}
			
			if (_actor.isSummon())
			{
				if (GeoEngine.getInstance().canMoveToTarget(_actor, target))
				{
					return false;
				}
				
				setIntention(Intention.ACTIVE);
				return true;
			}
		}
		
		return false;
	}
	
	protected class SelfAnalysis
	{
		public boolean isMage = false;
		public boolean isBalanced;
		public boolean isArcher = false;
		public boolean isHealer = false;
		public boolean isFighter = false;
		public boolean cannotMoveOnLand = false;
		public Set<Skill> generalSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> buffSkills = ConcurrentHashMap.newKeySet();
		public int lastBuffTick = 0;
		public Set<Skill> debuffSkills = ConcurrentHashMap.newKeySet();
		public int lastDebuffTick = 0;
		public Set<Skill> cancelSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> healSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> generalDisablers = ConcurrentHashMap.newKeySet();
		public Set<Skill> sleepSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> rootSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> muteSkills = ConcurrentHashMap.newKeySet();
		public Set<Skill> resurrectSkills = ConcurrentHashMap.newKeySet();
		public boolean hasHealOrResurrect = false;
		public boolean hasLongRangeSkills = false;
		public boolean hasLongRangeDamageSkills = false;
		public int maxCastRange = 0;
		
		public SelfAnalysis()
		{
		}
		
		public void init()
		{
			switch (((NpcTemplate) _actor.getTemplate()).getAIType())
			{
				case FIGHTER:
				{
					isFighter = true;
					break;
				}
				case MAGE:
				{
					isMage = true;
					break;
				}
				case CORPSE:
				case BALANCED:
				{
					isBalanced = true;
					break;
				}
				case ARCHER:
				{
					isArcher = true;
					break;
				}
				case HEALER:
				{
					isHealer = true;
					break;
				}
				default:
				{
					isFighter = true;
					break;
				}
			}
			
			// water movement analysis
			if (_actor.isNpc())
			{
				switch (_actor.getId())
				{
					case 20314: // great white shark
					case 20849: // Light Worm
					{
						cannotMoveOnLand = true;
						break;
					}
					default:
					{
						cannotMoveOnLand = false;
						break;
					}
				}
			}
			
			// skill analysis
			for (Skill sk : _actor.getAllSkills())
			{
				if (sk.isPassive())
				{
					continue;
				}
				
				final int castRange = sk.getCastRange();
				boolean hasLongRangeDamageSkill = false;
				if (sk.isContinuous())
				{
					if (!sk.isDebuff())
					{
						buffSkills.add(sk);
					}
					else
					{
						debuffSkills.add(sk);
					}
					continue;
				}
				
				if (sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
				{
					cancelSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.HEAL))
				{
					healSkills.add(sk);
					hasHealOrResurrect = true;
				}
				else if (sk.hasEffectType(EffectType.SLEEP))
				{
					sleepSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.BLOCK_ACTIONS))
				{
					// hardcoding petrification until improvements are made to
					// EffectTemplate... petrification is totally different for
					// AI than paralyze
					switch (sk.getId())
					{
						case 367:
						case 4111:
						case 4383:
						case 4616:
						case 4578:
						{
							sleepSkills.add(sk);
							break;
						}
						default:
						{
							generalDisablers.add(sk);
							break;
						}
					}
				}
				else if (sk.hasEffectType(EffectType.ROOT))
				{
					rootSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.BLOCK_CONTROL))
				{
					debuffSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.MUTE))
				{
					muteSkills.add(sk);
				}
				else if (sk.hasEffectType(EffectType.RESURRECTION))
				{
					resurrectSkills.add(sk);
					hasHealOrResurrect = true;
				}
				else
				{
					generalSkills.add(sk);
					hasLongRangeDamageSkill = true;
				}
				
				if (castRange > 150)
				{
					hasLongRangeSkills = true;
					if (hasLongRangeDamageSkill)
					{
						hasLongRangeDamageSkills = true;
					}
				}
				
				if (castRange > maxCastRange)
				{
					maxCastRange = castRange;
				}
			}
			
			// Because of missing skills, some mages/balanced cannot play like mages
			if (!hasLongRangeDamageSkills && isMage)
			{
				isBalanced = true;
				isMage = false;
				isFighter = false;
			}
			
			if (!hasLongRangeSkills && (isMage || isBalanced))
			{
				isBalanced = false;
				isMage = false;
				isFighter = true;
			}
			
			if (generalSkills.isEmpty() && isMage)
			{
				isBalanced = true;
				isMage = false;
			}
		}
	}
}
