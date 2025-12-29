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
package org.l2jmobius.gameserver.network.serverpackets.surveillance;

import java.util.LinkedList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author MacuK
 */
public class ExUserWatcherTargetList extends ServerPacket
{
	private final List<TargetInfo> _info = new LinkedList<>();
	
	public ExUserWatcherTargetList(Player player)
	{
		for (int objId : player.getSurveillanceList())
		{
			final String name = CharInfoTable.getInstance().getNameById(objId);
			final Player target = World.getInstance().getPlayer(objId);
			boolean online = false;
			int level = 0;
			int classId = 0;
			if (target != null)
			{
				online = true;
				level = target.getLevel();
				classId = target.getPlayerClass().getId();
			}
			else
			{
				level = CharInfoTable.getInstance().getLevelById(objId);
				classId = CharInfoTable.getInstance().getClassIdById(objId);
			}
			
			_info.add(new TargetInfo(name, online, level, classId));
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_USER_WATCHER_TARGET_LIST.writeId(this, buffer);
		buffer.writeInt(_info.size());
		for (TargetInfo info : _info)
		{
			buffer.writeSizedString(info._name);
			buffer.writeInt(0); // client.getProxyServerId()
			buffer.writeInt(info._level);
			buffer.writeInt(info._classId);
			buffer.writeByte(info._online ? 1 : 0);
		}
	}
	
	private static class TargetInfo
	{
		private final String _name;
		private final int _level;
		private final int _classId;
		private final boolean _online;
		
		public TargetInfo(String name, boolean online, int level, int classId)
		{
			_name = name;
			_online = online;
			_level = level;
			_classId = classId;
		}
	}
}
