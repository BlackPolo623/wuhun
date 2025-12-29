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
package handlers.playeractions;

import org.l2jmobius.gameserver.handler.IPlayerActionHandler;
import org.l2jmobius.gameserver.model.ActionDataHolder;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Unsummon Pet player action handler.
 * @author St3eT
 */
public class UnsummonPet implements IPlayerActionHandler
{
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		final Summon pet = player.getPet();
		if (pet == null)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_GUARDIAN);
		}
		else if (pet.asPet().isUncontrollable())
		{
			player.sendPacket(SystemMessageId.WHEN_YOUR_GUARDIAN_S_SATIETY_REACHES_0_YOU_CANNOT_CONTROL_IT);
		}
		else if (pet.isBetrayed())
		{
			player.sendPacket(SystemMessageId.WHEN_YOUR_GUARDIAN_S_SATIETY_REACHES_0_YOU_CANNOT_CONTROL_IT);
		}
		else if (pet.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_A_DEAD_GUARDIAN);
		}
		else if (pet.isAttackingNow() || pet.isInCombat() || pet.isMovementDisabled())
		{
			player.sendPacket(SystemMessageId.A_GUARDIAN_CANNOT_BE_UNSUMMONED_WHILE_IN_COMBAT);
		}
		else if (pet.isHungry())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RETURN_A_HUNGRY_GUARDIAN);
		}
		else
		{
			pet.unSummon(player);
		}
	}
	
	@Override
	public boolean isPetAction()
	{
		return true;
	}
}
