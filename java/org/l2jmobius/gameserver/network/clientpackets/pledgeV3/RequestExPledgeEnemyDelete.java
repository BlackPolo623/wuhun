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
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.pledgeV3.ExPledgeEnemyInfoList;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * @author Mobius
 */
public class RequestExPledgeEnemyDelete extends ClientPacket
{
	private int _clanId;
	
	@Override
	protected void readImpl()
	{
		_clanId = readInt();
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
		
		final Clan enemyClan = ClanTable.getInstance().getClan(_clanId);
		if (enemyClan == null)
		{
			player.sendPacket(SystemMessageId.THE_CLAN_IS_NOT_FOUND_2);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!playerClan.isAtWarWith(enemyClan.getId()))
		{
			player.sendPacket(SystemMessageId.ENTER_THE_NAME_OF_THE_CLAN_YOU_WISH_TO_END_THE_WAR_WITH);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (!player.hasAccess(ClanAccess.WAR_DECLARATION))
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		for (ClanMember member : playerClan.getMembers())
		{
			if ((member == null) || (member.getPlayer() == null))
			{
				continue;
			}
			
			if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(member.getPlayer()))
			{
				player.sendPacket(SystemMessageId.THE_CLAN_WAR_CANNOT_BE_STOPPED_BECAUSE_SOMEONE_FROM_YOUR_CLAN_IS_STILL_ENGAGED_IN_BATTLE);
				return;
			}
		}
		
		// Reduce reputation.
		playerClan.takeReputationScore(500);
		ClanTable.getInstance().deleteClanWars(playerClan.getId(), enemyClan.getId());
		
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
