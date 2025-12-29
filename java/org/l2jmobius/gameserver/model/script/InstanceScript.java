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
package org.l2jmobius.gameserver.model.script;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.instancezone.InstanceReenterType;
import org.l2jmobius.gameserver.model.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Abstract base class for instance zone management and player entry handling.<br>
 * Provides core functionality for creating, entering and managing dungeon instances.
 * <ul>
 * <li>Multi-template instance support with flexible configuration.</li>
 * <li>Group-based entry validation and condition checking.</li>
 * <li>Screen message broadcasting and player communication.</li>
 * <li>Instance lifecycle management with teleportation handling.</li>
 * </ul>
 * @author FallenAngel, Mobius
 */
public abstract class InstanceScript extends Script
{
	// Instance Configuration.
	private final Set<Integer> _templateIds = new HashSet<>();
	
	/**
	 * Creates an abstract instance handler for specified template identifiers.<br>
	 * Validates that at least one template ID is provided during initialization.
	 * @param templateIds the instance template identifiers to handle
	 * @throws IllegalStateException if no template IDs are provided
	 */
	protected InstanceScript(int... templateIds)
	{
		if (templateIds.length == 0)
		{
			throw new IllegalStateException("No template ids were provided!");
		}
		
		for (int templateId : templateIds)
		{
			_templateIds.add(templateId);
		}
	}
	
	/**
	 * Returns the set of template identifiers managed by this instance handler.
	 * @return immutable set of template IDs
	 */
	public Set<Integer> getTemplateId()
	{
		return _templateIds;
	}
	
	/**
	 * Checks if the specified instance is managed by this handler.
	 * @param instance the instance to check
	 * @return true if this handler manages the instance template
	 */
	public boolean isInInstance(Instance instance)
	{
		return (instance != null) && _templateIds.contains(instance.getTemplateId());
	}
	
	/**
	 * Retrieves the active instance world associated with the specified player.
	 * @param player the player to get instance world for
	 * @return active instance if found, otherwise null
	 */
	public Instance getPlayerInstance(Player player)
	{
		return InstanceManager.getInstance().getPlayerInstance(player, false);
	}
	
	/**
	 * Broadcasts an on-screen message to all players inside the instance.
	 * @param instance the instance to broadcast to
	 * @param npcStringId the NPC string identifier to display
	 * @param position the screen position for the message
	 * @param timeInMilliseconds the display duration in milliseconds
	 * @param parameters values to replace NPC string parameters
	 */
	public void showOnScreenMsg(Instance instance, NpcStringId npcStringId, int position, int timeInMilliseconds, String... parameters)
	{
		instance.broadcastPacket(new ExShowScreenMessage(npcStringId, position, timeInMilliseconds, parameters));
	}
	
	/**
	 * Broadcasts an on-screen message with optional visual effects to all players.<br>
	 * Provides enhanced visual feedback with configurable effect display.
	 * @param instance the instance to broadcast to
	 * @param npcStringId the NPC string identifier to display
	 * @param position the screen position for the message
	 * @param timeInMilliseconds the display duration in milliseconds
	 * @param showVisualEffect whether to show visual effects with the message
	 * @param parameters values to replace NPC string parameters
	 */
	public void showOnScreenMsg(Instance instance, NpcStringId npcStringId, int position, int timeInMilliseconds, boolean showVisualEffect, String... parameters)
	{
		instance.broadcastPacket(new ExShowScreenMessage(npcStringId, position, timeInMilliseconds, showVisualEffect, parameters));
	}
	
	/**
	 * Handles player entry into instance world through NPC interaction.<br>
	 * Creates new instance if needed or enters existing one with full validation.
	 * @param player the player requesting entry
	 * @param npc the NPC handling the entry request
	 * @param templateId the instance template identifier
	 */
	protected void enterInstance(Player player, Npc npc, int templateId)
	{
		Instance instance = getPlayerInstance(player);
		if (instance != null) // Player has already any instance active.
		{
			if (instance.getTemplateId() != templateId)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_ENTER_AS_C1_IS_IN_ANOTHER_INSTANCE_ZONE).addString(player.getName()));
				return;
			}
			
			onEnter(player, instance, false);
		}
		else
		{
			// Get instance template.
			final InstanceManager instanceManager = InstanceManager.getInstance();
			final InstanceTemplate instanceTemplate = instanceManager.getInstanceTemplate(templateId);
			if (instanceTemplate == null)
			{
				LOGGER.warning(player + " wants to create instance with unknown template id " + templateId + "!");
				return;
			}
			
			// Get instance enter scope.
			final List<Player> group = instanceTemplate.getEnterGroup(player);
			
			// When nobody can enter.
			if (group == null)
			{
				LOGGER.warning("Instance " + instanceTemplate.getName() + " (" + templateId + ") has invalid group size limits!");
				return;
			}
			
			// Validate conditions for group.
			if (!player.isGM() && (!instanceTemplate.validateConditions(group, npc, this::showHtmlFile) || !validateConditions(group, npc, instanceTemplate)))
			{
				return;
			}
			
			// Check if maximum world count limit is exceeded.
			if ((instanceTemplate.getMaxWorlds() != -1 /* unlimited instances */) && (instanceManager.getWorldCount(templateId) >= instanceTemplate.getMaxWorlds()))
			{
				player.sendPacket(SystemMessageId.THE_NUMBER_OF_INSTANCE_ZONES_THAT_CAN_BE_CREATED_HAS_BEEN_EXCEEDED_PLEASE_TRY_AGAIN_LATER);
				return;
			}
			
			// Check if any player from enter group has active instance.
			for (Player member : group)
			{
				if (getPlayerInstance(member) != null)
				{
					group.forEach(p -> p.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_ENTER_AS_C1_IS_IN_ANOTHER_INSTANCE_ZONE).addString(member.getName())));
					return;
				}
				
				// Check if any player from the group has already finished the instance.
				if (InstanceManager.getInstance().getInstanceTime(member, templateId) > 0)
				{
					group.forEach(p -> p.sendPacket(new SystemMessage(SystemMessageId.C1_CANNOT_ENTER_YET).addString(member.getName())));
					return;
				}
			}
			
			// Create new instance for enter player group.
			instance = instanceManager.createInstance(instanceTemplate, player);
			
			// Move each player from enter group to instance.
			for (Player member : group)
			{
				instance.addAllowed(member);
				onEnter(member, instance, true);
			}
			
			// Apply condition success effects.
			instanceTemplate.applyConditionEffects(group);
			
			// Set re-enter for instances with re-enter on start.
			if (instance.getReenterType() == InstanceReenterType.ON_ENTER)
			{
				instance.setReenterTime();
			}
		}
	}
	
	/**
	 * Called when a player enters the instance through NPC interaction.<br>
	 * Default implementation handles player teleportation to instance entrance.
	 * @param player the player entering the instance
	 * @param instance the instance world being entered
	 * @param isFirstEntry true if this is the player's first time entering
	 */
	protected void onEnter(Player player, Instance instance, boolean isFirstEntry)
	{
		teleportPlayerIn(player, instance);
	}
	
	/**
	 * Teleports a player into the instance using configured entrance location.<br>
	 * Uses XML teleport data from instance template configuration.
	 * @param player the player to teleport
	 * @param instance the destination instance
	 */
	protected void teleportPlayerIn(Player player, Instance instance)
	{
		final Location location = instance.getEnterLocation();
		if (location != null)
		{
			player.teleToLocation(location, instance);
		}
		else
		{
			LOGGER.warning("Missing start location for instance " + instance.getName() + " (" + instance.getId() + ")");
		}
	}
	
	/**
	 * Teleports a player out of the instance world using standard ejection.<br>
	 * Handles cleanup and removal from instance player list.
	 * @param player the player to remove from instance
	 * @param instance the instance to eject the player from
	 */
	protected void teleportPlayerOut(Player player, Instance instance)
	{
		instance.ejectPlayer(player);
	}
	
	/**
	 * Sets the player's current instance to finished state with default timing.
	 * @param player the player whose instance should be finished
	 */
	protected void finishInstance(Player player)
	{
		final Instance instance = player.getInstanceWorld();
		if (instance != null)
		{
			instance.finishInstance();
		}
	}
	
	/**
	 * Sets the player's current instance to finished state with custom delay.<br>
	 * Allows specific control over instance cleanup timing.
	 * @param player the player whose instance should be finished
	 * @param delayInMinutes the delay before instance cleanup in minutes
	 */
	protected void finishInstance(Player player, int delayInMinutes)
	{
		final Instance instance = player.getInstanceWorld();
		if (instance != null)
		{
			instance.finishInstance(delayInMinutes);
		}
	}
	
	/**
	 * Validates additional instance-specific conditions for player group entry.<br>
	 * Called after XML template conditions are validated successfully.
	 * @param group the group of players requesting entry
	 * @param npc the NPC handling the entry request
	 * @param template the template for the instance being created
	 * @return true if additional conditions are met
	 */
	protected boolean validateConditions(List<Player> group, Npc npc, InstanceTemplate template)
	{
		return true;
	}
}
