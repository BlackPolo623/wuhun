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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanAccess;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.model.clan.ClanWar;
import org.l2jmobius.gameserver.model.clan.enums.ClanWarState;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.PledgeReceiveWarList;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Mobius
 */
public class RequestStartPledgeWar extends ClientPacket
{
	private String _pledgeName;
	
	@Override
	protected void readImpl()
	{
		_pledgeName = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Clan clanDeclaringWar = player.getClan();
		if (clanDeclaringWar == null)
		{
			return;
		}
		
		if ((clanDeclaringWar.getLevel() < 3) || (clanDeclaringWar.getMembersCount() < PlayerConfig.ALT_CLAN_MEMBERS_FOR_WAR))
		{
			player.sendPacket(SystemMessageId.A_CLAN_WAR_CAN_ONLY_BE_DECLARED_IF_THE_CLAN_IS_LV_3_OR_HIGHER_AND_THE_NUMBER_OF_CLAN_MEMBERS_IS_15_OR_GREATER);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else if (!player.hasAccess(ClanAccess.WAR_DECLARATION))
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else if (clanDeclaringWar.getWarCount() >= 30)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DECLARE_WAR_ON_MORE_THAN_30_CLANS_AT_A_TIME);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Clan clanDeclaredWar = ClanTable.getInstance().getClanByName(_pledgeName);
		if (clanDeclaredWar == null)
		{
			player.sendPacket(SystemMessageId.A_CLAN_WAR_CANNOT_BE_DECLARED_AGAINST_A_CLAN_THAT_DOES_NOT_EXIST);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else if (clanDeclaredWar == clanDeclaringWar)
		{
			player.sendPacket(SystemMessageId.FOOL_YOU_CANNOT_DECLARE_WAR_AGAINST_YOUR_OWN_CLAN);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else if ((clanDeclaringWar.getAllyId() == clanDeclaredWar.getAllyId()) && (clanDeclaringWar.getAllyId() != 0))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DECLARE_WAR_ON_AN_ALLIED_CLAN_2);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else if ((clanDeclaredWar.getLevel() < 3) || (clanDeclaredWar.getMembersCount() < PlayerConfig.ALT_CLAN_MEMBERS_FOR_WAR))
		{
			player.sendPacket(SystemMessageId.A_CLAN_WAR_CAN_ONLY_BE_DECLARED_IF_THE_CLAN_IS_LV_3_OR_HIGHER_AND_THE_NUMBER_OF_CLAN_MEMBERS_IS_15_OR_GREATER);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else if (clanDeclaredWar.getDissolvingExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.A_CLAN_WAR_CAN_NOT_BE_DECLARED_AGAINST_A_CLAN_THAT_IS_BEING_DISSOLVED);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final ClanWar clanWar = clanDeclaringWar.getWarWith(clanDeclaredWar.getId());
		if (clanWar != null)
		{
			if (clanWar.getClanWarState(clanDeclaringWar) == ClanWarState.WIN)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_CANNOT_DECLARE_WAR_AS_THE_21_DAY_PERIOD_HAS_NOT_PASSED_SINCE_THE_DEFEAT_FROM_THE_S1_CLAN);
				sm.addString(clanDeclaredWar.getName());
				player.sendPacket(sm);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (clanWar.getState() == ClanWarState.MUTUAL)
			{
				player.sendMessage("You have already been at war with " + clanDeclaredWar.getName() + ".");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			if (clanWar.getState() == ClanWarState.BLOOD_DECLARATION)
			{
				clanWar.mutualClanWarAccepted(clanDeclaredWar, clanDeclaringWar);
				ClanTable.getInstance().storeClanWars(clanWar);
				for (ClanMember member : clanDeclaringWar.getMembers())
				{
					if ((member != null) && member.isOnline())
					{
						member.getPlayer().broadcastUserInfo(UserInfoType.CLAN);
					}
				}
				
				for (ClanMember member : clanDeclaredWar.getMembers())
				{
					if ((member != null) && member.isOnline())
					{
						member.getPlayer().broadcastUserInfo(UserInfoType.CLAN);
					}
				}
				
				player.sendPacket(new PledgeReceiveWarList(player.getClan(), 0));
				return;
			}
		}
		
		final ClanWar newClanWar = new ClanWar(clanDeclaringWar, clanDeclaredWar);
		ClanTable.getInstance().storeClanWars(newClanWar);
		
		for (ClanMember member : clanDeclaringWar.getMembers())
		{
			if ((member != null) && member.isOnline())
			{
				member.getPlayer().broadcastUserInfo(UserInfoType.CLAN);
			}
		}
		
		for (ClanMember member : clanDeclaredWar.getMembers())
		{
			if ((member != null) && member.isOnline())
			{
				member.getPlayer().broadcastUserInfo(UserInfoType.CLAN);
			}
		}
		
		player.sendPacket(new PledgeReceiveWarList(player.getClan(), 0));
	}
}
