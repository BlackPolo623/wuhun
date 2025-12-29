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
package org.l2jmobius.gameserver.network.clientpackets.olympiad;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.olympiad.CompetitionType;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadInfo;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMatchMakingResult;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadRecord;

public class OlympiadMatchMaking extends ClientPacket
{
	private byte _gameRuleType;
	
	@Override
	protected void readImpl()
	{
		_gameRuleType = 1;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isPrisoner())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_FUNCTION_IN_THE_UNDERGROUND_LABYRINTH);
			return;
		}
		
		if (player.isRegisteredOnEvent())
		{
			player.sendMessage("You cannot register to Olympiad while participating in events!");
			return;
		}
		
		if (player.isInTimedHuntingZone() || player.isInInstance() || player.isCursedWeaponEquipped() || player.isInsideZone(ZoneId.PVP) || (player.getReputation() < 0))
		{
			player.sendMessage("You cannot register to Olympiad at the moment!"); // TODO: find proper systemmessages
			return;
		}
		
		if (Olympiad.getInstance().getMillisToCompEnd() < 600000)
		{
			player.sendPacket(SystemMessageId.GAME_PARTICIPATION_REQUEST_MUST_BE_FILED_NOT_EARLIER_THAN_10_MIN_AFTER_THE_GAME_ENDS);
			return;
		}
		
		if (_gameRuleType == 1)
		{
			if (!player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) || (player.getLevel() < 75))
			{
				player.sendPacket(SystemMessageId.CHARACTER_S_LEVEL_IS_TOO_LOW);
			}
			else if (!player.isInventoryUnder80(false))
			{
				player.sendPacket(SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
			}
			else
			{
				OlympiadManager.getInstance().registerNoble(player, CompetitionType.NON_CLASSED);
			}
		}
		
		player.sendPacket(new ExOlympiadMatchMakingResult(_gameRuleType, 1));
		player.sendPacket(new ExOlympiadInfo(2, Olympiad.getInstance().getRemainingTime()));
		player.sendPacket(new ExOlympiadRecord(player, _gameRuleType));
	}
}
