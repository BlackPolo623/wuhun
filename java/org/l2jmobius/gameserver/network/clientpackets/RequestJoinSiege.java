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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanAccess;
import org.l2jmobius.gameserver.model.siege.Castle;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.castlewar.MercenaryCastleWarCastleSiegeAttackerList;
import org.l2jmobius.gameserver.network.serverpackets.castlewar.MercenaryCastleWarCastleSiegeDefenderList;

/**
 * @author KenM, Mobius
 */
public class RequestJoinSiege extends ClientPacket

{
	private int _castleId;
	private int _isAttacker;
	private int _isJoining;
	
	@Override
	protected void readImpl()
	{
		_castleId = readInt();
		_isAttacker = readInt();
		_isJoining = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!player.hasAccess(ClanAccess.CASTLE_SIEGE))
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return;
		}
		
		final Castle castle = CastleManager.getInstance().getCastleById(_castleId);
		if (castle != null)
		{
			if (_isJoining == 1)
			{
				if (System.currentTimeMillis() < clan.getDissolvingExpiryTime())
				{
					player.sendPacket(SystemMessageId.YOUR_CLAN_MAY_NOT_REGISTER_TO_PARTICIPATE_IN_A_SIEGE_WHILE_UNDER_A_GRACE_PERIOD_OF_THE_CLAN_S_DISSOLUTION);
					return;
				}
				
				if (_isAttacker == 1)
				{
					castle.getSiege().registerAttacker(player);
					player.sendPacket(new MercenaryCastleWarCastleSiegeAttackerList(castle.getResidenceId()));
				}
				else
				{
					castle.getSiege().registerDefender(player);
					player.sendPacket(new MercenaryCastleWarCastleSiegeDefenderList(castle.getResidenceId()));
				}
			}
			else
			{
				if (clan.isRecruitMercenary() && (clan.getMapMercenary().size() > 0))
				{
					return;
				}
				
				castle.getSiege().removeSiegeClan(player);
				if (_isAttacker == 1)
				{
					player.sendPacket(new MercenaryCastleWarCastleSiegeAttackerList(castle.getResidenceId()));
				}
				else
				{
					player.sendPacket(new MercenaryCastleWarCastleSiegeDefenderList(castle.getResidenceId()));
				}
			}
			
			// Managed by new packets.
			// castle.getSiege().listRegisterClan(player);
		}
	}
}
