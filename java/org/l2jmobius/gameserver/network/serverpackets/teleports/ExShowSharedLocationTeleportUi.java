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
package org.l2jmobius.gameserver.network.serverpackets.teleports;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.SharedTeleportHolder;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author NasSeKa
 */
public class ExShowSharedLocationTeleportUi extends ServerPacket
{
	private final SharedTeleportHolder _teleport;
	
	public ExShowSharedLocationTeleportUi(SharedTeleportHolder teleport)
	{
		_teleport = teleport;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SHARED_POSITION_TELEPORT_UI.writeId(this, buffer);
		
		buffer.writeSizedString(_teleport.getName());
		buffer.writeInt(_teleport.getId());
		buffer.writeInt(_teleport.getCount());
		buffer.writeShort(150);
		
		final Location location = _teleport.getLocation();
		buffer.writeInt(location.getX());
		buffer.writeInt(location.getY());
		buffer.writeInt(location.getZ());
		
		buffer.writeLong(GeneralConfig.TELEPORT_SHARE_LOCATION_COST);
	}
}
