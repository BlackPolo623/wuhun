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
package handlers.admincommandhandlers;

import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.data.holders.AdenLabHolder;
import org.l2jmobius.gameserver.data.xml.AdenLaboratoryData;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.AdenLaboratoryManager;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.IllegalActionPunishmentType;
import org.l2jmobius.gameserver.network.serverpackets.adenlab.ExAdenLabBossInfo;

/**
 * @author SaltyMike
 */
public class AdminAdenLab implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_adenlab",
		"admin_adenlab_reset",
		"admin_adenlab_setmax",
		"admin_adenlab_reload"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equalsIgnoreCase("admin_adenlab"))
		{
			// TODO: Create and handle main menu in HTML.
		}
		else if (command.equalsIgnoreCase("admin_adenlab_reload"))
		{
			activeChar.sendMessage("Reloading Aden Laboratory data...");
			AdenLaboratoryData.getInstance().reload();
			AdminData.getInstance().broadcastMessageToGMs(activeChar.getName() + ": Reloaded Aden Laboratory data.");
		}
		else if (command.equalsIgnoreCase("admin_adenlab_reset"))
		{
			final WorldObject target = activeChar.getTarget();
			if ((target == null) || !target.isPlayer())
			{
				activeChar.sendMessage("You need to have selected a player.");
				return false;
			}
			
			for (Entry<Byte, Map<Integer, AdenLabHolder>> entry : AdenLaboratoryData.getInstance().getAllAdenLabData().entrySet())
			{
				final byte bossId = entry.getKey();
				target.asPlayer().setAdenLabCurrentlyUnlockedPage(bossId, 1);
				target.asPlayer().setAdenLabNormalGameOpenedCardsCount(bossId, 0);
				target.asPlayer().getAdenLabSpecialGameStagesDrawnOptions().clear();
				target.asPlayer().getAdenLabSpecialGameStagesConfirmedOptions().clear();
				target.asPlayer().setAdenLabCurrentTranscendLevel(bossId, 0);
				
				AdenLaboratoryManager.deletePlayerSkills(target.asPlayer(), bossId);
				target.asPlayer().sendPacket(new ExAdenLabBossInfo(bossId, target.asPlayer()));
			}
			
			AdenLaboratoryManager.deletePlayerData(target.asPlayer());
			AdenLaboratoryManager.calculateAdenLabCombatPower(target.asPlayer());
			
			target.asPlayer().getInventory().applyItemSkills();
			target.asPlayer().getStat().recalculateStats(true);
			PunishmentManager.handleIllegalPlayerAction(target.asPlayer(), "Your Aden Lab progression has been reset by an admin.", IllegalActionPunishmentType.KICK);
		}
		else if (command.startsWith("admin_adenlab_setmax"))
		{
			final WorldObject target = activeChar.getTarget();
			if ((target == null) || !target.isPlayer())
			{
				activeChar.sendMessage("You need to have selected a player.");
				return false;
			}
			
			final String[] parts = command.split(" ");
			if (parts.length != 2)
			{
				activeChar.sendMessage("Aden Lab `setMax` admin command usage: //adenlab_setmax [BOSS_ID]");
				activeChar.sendMessage(" Example: //adenlab_setmax 1");
				return false;
			}
			
			final byte bossId = Byte.parseByte(parts[1]);
			if ((target.asPlayer().getAdenLabCurrentlyUnlockedPage(bossId) >= 25) && (target.asPlayer().getAdenLabCurrentTranscendLevel(bossId) >= 3))
			{
				activeChar.sendMessage("Your target's Aden Lab progression is already maxed out.");
				return false;
			}
			
			target.asPlayer().setAdenLabCurrentlyUnlockedPage(bossId, 25);
			target.asPlayer().setAdenLabCurrentTranscendLevel(bossId, 3);
			target.asPlayer().setAdenLabSpecialGameConfirmedOptionsBulk(AdenLaboratoryManager.getSpecialGameOptionsFromString("1,16,1,50;1,5,1,20;1,21,1,50;1,21,2,50;1,10,1,50"));
			target.asPlayer().setAdenLabNormalGameOpenedCardsCount(bossId, 0);
			
			AdenLaboratoryManager.checkPlayerSkills(target.asPlayer());
			AdenLaboratoryManager.calculateAdenLabCombatPower(target.asPlayer());
			
			target.asPlayer().getInventory().applyItemSkills();
			target.asPlayer().getStat().recalculateStats(true);
			target.asPlayer().sendPacket(new ExAdenLabBossInfo(bossId, target.asPlayer()));
		}
		
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
