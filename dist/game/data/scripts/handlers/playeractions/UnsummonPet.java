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
			player.sendMessage("【無法召回】寵物的飽食度已歸零，完全失控，請先餵食後再嘗試召回。");
		}
		else if (pet.isBetrayed())
		{
			player.sendPacket(SystemMessageId.WHEN_YOUR_GUARDIAN_S_SATIETY_REACHES_0_YOU_CANNOT_CONTROL_IT);
			player.sendMessage("【無法召回】寵物處於背叛狀態，無法控制，請先餵食恢復狀態後再嘗試。");
		}
		else if (pet.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_A_DEAD_GUARDIAN);
			player.sendMessage("【無法召回】寵物已死亡，無法召回。");
		}
		else if (pet.isAttackingNow() || pet.isInCombat() || pet.isMovementDisabled())
		{
			player.sendPacket(SystemMessageId.A_GUARDIAN_CANNOT_BE_UNSUMMONED_WHILE_IN_COMBAT);
			player.sendMessage("【無法召回】寵物正在戰鬥中或行動被封鎖，請等待戰鬥結束後再召回。");
		}
		else if (pet.isHungry())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_RETURN_A_HUNGRY_GUARDIAN);
			player.sendMessage("【無法召回】寵物目前處於飢餓狀態（飽食度不足），請先餵食後再嘗試召回。");
			player.sendMessage("提示：若開啟自動使用功能，安全區內不會自動餵食，請手動餵食。");
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
