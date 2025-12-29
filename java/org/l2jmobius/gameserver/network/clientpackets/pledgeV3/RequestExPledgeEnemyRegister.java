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
package org.l2jmobius.gameserver.network.clientpackets.pledgeV3;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanAccess;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.clan.ClanWar;
import org.l2jmobius.gameserver.model.clan.enums.ClanWarState;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.pledgeV3.ExPledgeEnemyInfoList;

/**
 * @author Mobius
 */
public class RequestExPledgeEnemyRegister extends ClientPacket
{
	private String _pledgeName;
	
	@Override
	protected void readImpl()
	{
		_pledgeName = readSizedString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Clan playerClan = player.getClan();
		if (playerClan == null)
		{
			return;
		}
		
		if (!player.hasAccess(ClanAccess.WAR_DECLARATION))
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (playerClan.getWarCount() >= 30)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DECLARE_WAR_ON_MORE_THAN_30_CLANS_AT_A_TIME);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Clan enemyClan = ClanTable.getInstance().getClanByName(_pledgeName);
		if (enemyClan == null)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.A_CLAN_WAR_CANNOT_BE_DECLARED_AGAINST_A_CLAN_THAT_DOES_NOT_EXIST));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (enemyClan == playerClan)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.FOOL_YOU_CANNOT_DECLARE_WAR_AGAINST_YOUR_OWN_CLAN));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if ((playerClan.getAllyId() == enemyClan.getAllyId()) && (playerClan.getAllyId() != 0))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_DECLARE_WAR_ON_AN_ALLIED_CLAN_2));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (enemyClan.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.A_CLAN_WAR_CAN_NOT_BE_DECLARED_AGAINST_A_CLAN_THAT_IS_BEING_DISSOLVED));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final ClanWar clanWar = playerClan.getWarWith(enemyClan.getId());
		if (clanWar != null)
		{
			if (clanWar.getClanWarState(playerClan) == ClanWarState.WIN)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_CANNOT_DECLARE_WAR_AS_THE_21_DAY_PERIOD_HAS_NOT_PASSED_SINCE_THE_DEFEAT_FROM_THE_S1_CLAN);
				sm.addString(enemyClan.getName());
				player.sendPacket(sm);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if ((clanWar.getClanWarState(playerClan) != ClanWarState.BLOOD_DECLARATION) || (clanWar.getAttackerClanId() == playerClan.getId()))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ALREADY_BEEN_AT_WAR_WITH_THE_S1_CLAN_5_DAYS_MUST_PASS_BEFORE_YOU_CAN_DECLARE_WAR_AGAIN);
				sm.addString(enemyClan.getName());
				player.sendPacket(sm);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (clanWar.getClanWarState(playerClan) == ClanWarState.BLOOD_DECLARATION)
			{
				clanWar.mutualClanWarAccepted(enemyClan, playerClan);
				broadcastClanInfo(playerClan, enemyClan);
				return;
			}
		}
		
		final ClanWar newClanWar = new ClanWar(playerClan, enemyClan);
		ClanTable.getInstance().storeClanWars(newClanWar);
		
		broadcastClanInfo(playerClan, enemyClan);
	}
	
	private void broadcastClanInfo(Clan playerClan, Clan enemyClan)
	{
		for (ClanMember member : playerClan.getMembers())
		{
			if ((member != null) && member.isOnline())
			{
				member.getPlayer().sendPacket(new ExPledgeEnemyInfoList(playerClan));
				member.getPlayer().broadcastUserInfo();
			}
		}
		
		for (ClanMember member : enemyClan.getMembers())
		{
			if ((member != null) && member.isOnline())
			{
				member.getPlayer().sendPacket(new ExPledgeEnemyInfoList(enemyClan));
				member.getPlayer().broadcastUserInfo();
			}
		}
	}
}
