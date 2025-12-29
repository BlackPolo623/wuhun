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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.Clan.SubPledge;
import org.l2jmobius.gameserver.model.clan.ClanMember;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class PledgeShowMemberListAll extends ServerPacket
{
	private final Clan _clan;
	private final SubPledge _pledge;
	private final String _name;
	private final String _leaderName;
	private final Collection<ClanMember> _members;
	private final int _pledgeId;
	private final boolean _isSubPledge;
	private String _allyMasterLeaderName = "";
	private String _allyMasterName = "";
	private String _allyClansNames = "";
	
	private PledgeShowMemberListAll(Clan clan, SubPledge pledge, boolean isSubPledge)
	{
		_clan = clan;
		_pledge = pledge;
		_pledgeId = _pledge == null ? 0 : _pledge.getId();
		_leaderName = pledge == null ? clan.getLeaderName() : CharInfoTable.getInstance().getNameById(pledge.getLeaderId());
		_name = pledge == null ? clan.getName() : pledge.getName();
		_members = _clan.getMembers();
		_isSubPledge = isSubPledge;
		
		if (clan.getAllyId() != 0)
		{
			final Clan leaderAlyClan = ClanTable.getInstance().getClan(clan.getAllyId());
			if (leaderAlyClan.getLeader() != null)
			{
				_allyMasterName = leaderAlyClan.getName();
				_allyMasterLeaderName = leaderAlyClan.getLeaderName();
				
				final List<Clan> allies = ClanTable.getInstance().getClanAllies(clan.getAllyId());
				if ((allies != null) && !allies.isEmpty())
				{
					_allyClansNames = allies.stream().map(Clan::getName).filter(clanFilterName -> !clanFilterName.equalsIgnoreCase(_allyMasterName)).collect(Collectors.joining(", "));
				}
			}
		}
	}
	
	public static void sendAllTo(Player player)
	{
		final Clan clan = player.getClan();
		if (clan != null)
		{
			for (SubPledge subPledge : clan.getAllSubPledges())
			{
				player.sendPacket(new PledgeShowMemberListAll(clan, subPledge, false));
			}
			
			player.sendPacket(new PledgeShowMemberListAll(clan, null, true));
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.PLEDGE_SHOW_MEMBER_LIST_ALL.writeId(this, buffer);
		buffer.writeInt(!_isSubPledge);
		buffer.writeInt(_clan.getId());
		buffer.writeInt(ServerConfig.SERVER_ID);
		buffer.writeInt(_pledgeId);
		buffer.writeString(_name);
		buffer.writeString(_leaderName);
		buffer.writeInt(_clan.getCrestId()); // crest id .. is used again
		buffer.writeInt(_clan.getLevel());
		buffer.writeInt(_clan.getCastleId());
		buffer.writeInt(0);
		buffer.writeInt(_clan.getHideoutId());
		buffer.writeInt(_clan.getFortId());
		buffer.writeInt(_clan.getRank());
		buffer.writeInt(_clan.getReputationScore());
		buffer.writeInt(0); // 0
		buffer.writeInt(0); // 0
		buffer.writeInt(_clan.getAllyId());
		buffer.writeString(_clan.getAllyName());
		buffer.writeInt(_clan.getAllyCrestId());
		buffer.writeString(_allyMasterLeaderName);
		buffer.writeString(_allyMasterName);
		buffer.writeString(_allyClansNames);
		buffer.writeInt(_clan.isAtWar()); // new c3
		buffer.writeInt(0); // Territory castle ID
		buffer.writeInt(_clan.getSubPledgeMembersCount(_pledgeId));
		for (ClanMember m : _members)
		{
			if (m.getPledgeType() != _pledgeId)
			{
				continue;
			}
			
			buffer.writeString(m.getName());
			buffer.writeInt(m.getLevel());
			buffer.writeInt(m.getClassId());
			final Player player = m.getPlayer();
			if (player != null)
			{
				buffer.writeInt(player.getAppearance().isFemale()); // no visible effect
				buffer.writeInt(player.getRace().ordinal()); // buffer.writeInt(1);
			}
			else
			{
				buffer.writeInt(1); // no visible effect
				buffer.writeInt(1); // buffer.writeInt(1);
			}
			
			buffer.writeInt(m.isOnline() ? m.getObjectId() : 0); // objectId = online 0 = offline
			buffer.writeInt(m.getSponsor() != 0);
			buffer.writeByte(m.getOnlineStatus());
		}
	}
}
