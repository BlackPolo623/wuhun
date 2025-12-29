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
package org.l2jmobius.gameserver.network.clientpackets.surveillance;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.RelationChanged;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.surveillance.ExUserWatcherTargetList;

/**
 * @author MacuK
 */
public class ExRequestUserWatcherAdd extends ClientPacket
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readSizedString();
		readInt(); // World Id
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final int targetId = CharInfoTable.getInstance().getIdByName(_name);
		if (targetId == -1)
		{
			player.sendPacket(SystemMessageId.THAT_CHARACTER_DOES_NOT_EXIST);
			return;
		}
		
		// You cannot add yourself to your own friend list.
		if (targetId == player.getObjectId())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ADD_YOURSELF_TO_YOUR_SURVEILLANCE_LIST);
			return;
		}
		
		// Target already in surveillance list.
		if (player.getSurveillanceList().contains(targetId))
		{
			player.sendPacket(SystemMessageId.THE_CHARACTER_IS_ALREADY_IN_YOUR_SURVEILLANCE_LIST);
			return;
		}
		
		if (player.getSurveillanceList().size() >= 10)
		{
			player.sendPacket(SystemMessageId.MAXIMUM_NUMBER_OF_PEOPLE_ADDED_YOU_CANNOT_ADD_MORE);
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO character_surveillances (charId, targetId) VALUES (?, ?)"))
		{
			statement.setInt(1, player.getObjectId());
			statement.setInt(2, targetId);
			statement.execute();
		}
		catch (Exception e)
		{
			PacketLogger.warning("ExRequestUserWatcherAdd: Could not add surveillance objectid: " + e.getMessage());
		}
		
		// Player added to your friend list.
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_VE_ADDED_C1_TO_YOUR_SURVEILLANCE_LIST);
		sm.addString(_name);
		player.sendPacket(sm);
		player.getSurveillanceList().add(targetId);
		player.sendPacket(new ExUserWatcherTargetList(player));
		
		final Player target = World.getInstance().getPlayer(targetId);
		if ((target != null) && target.isVisibleFor(player))
		{
			player.sendPacket(new RelationChanged());
		}
	}
}
