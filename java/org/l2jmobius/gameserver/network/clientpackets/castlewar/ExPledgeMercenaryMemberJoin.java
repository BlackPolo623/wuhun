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
package org.l2jmobius.gameserver.network.clientpackets.castlewar;

import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.siege.Siege;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.castlewar.MercenaryCastleWarCastleSiegeAttackerList;
import org.l2jmobius.gameserver.network.serverpackets.castlewar.MercenaryCastleWarCastleSiegeDefenderList;

public class ExPledgeMercenaryMemberJoin extends ClientPacket
{
	private int _castleId;
	private boolean _type;
	private int _pledgeId;
	
	@Override
	protected void readImpl()
	{
		readInt(); // objectId
		_type = readInt() == 1;
		_castleId = readInt();
		_pledgeId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		final Siege siege = SiegeManager.getInstance().getSiege(_castleId);
		if ((siege != null) && siege.isInProgress())
		{
			return;
		}
		
		if (_type)
		{
			if (player.getParty() != null)
			{
				player.sendPacket(SystemMessageId.A_CHARACTER_WHICH_IS_A_MEMBER_OF_A_PARTY_CANNOT_FILE_A_MERCENARY_REQUEST);
				return;
			}
			else if (player.isMercenary())
			{
				player.sendPacket(SystemMessageId.THE_CHARACTER_IS_PARTICIPATING_AS_A_MERCENARY);
				return;
			}
			else if (player.getLevel() < 40)
			{
				player.sendPacket(SystemMessageId.YOUR_CHARACTER_DOES_NOT_MEET_THE_LEVEL_REQUIREMENTS_TO_BE_A_MERCENARY);
				return;
			}
			else if (player.getClan() != null)
			{
				final Clan clan = player.getClan();
				if (clan.getId() == _pledgeId)
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_BE_A_MERCENARY_AT_THE_CLAN_YOU_ARE_A_MEMBER_OF);
					return;
				}
				
				if ((siege != null) && (siege.checkIsAttacker(clan) || siege.checkIsDefender(clan)))
				{
					player.sendPacket(SystemMessageId.ATTACKERS_AND_DEFENDERS_CANNOT_BE_RECRUITED_AS_MERCENARIES);
					return;
				}
			}
		}
		
		player.setMercenary(_type, _pledgeId);
		player.sendPacket(new MercenaryCastleWarCastleSiegeAttackerList(_castleId));
		player.sendPacket(new MercenaryCastleWarCastleSiegeDefenderList(_castleId));
	}
}
