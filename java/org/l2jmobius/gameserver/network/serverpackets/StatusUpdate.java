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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.StatusUpdateType;

/**
 * @author Mobius
 */
public class StatusUpdate extends ServerPacket
{
	private final int _objectId;
	private int _casterObjectId = 0;
	private final boolean _isPlayable;
	private boolean _isVisible = false;
	private final Map<StatusUpdateType, Number> _updates = new LinkedHashMap<>();
	
	/**
	 * Create {@link StatusUpdate} packet for given {@link WorldObject}.
	 * @param object
	 */
	public StatusUpdate(WorldObject object)
	{
		_objectId = object.getObjectId();
		_isPlayable = object.isPlayable();
	}
	
	public void addUpdate(StatusUpdateType type, long level)
	{
		_updates.put(type, level);
		if (_isPlayable)
		{
			switch (type)
			{
				case CUR_HP:
				case CUR_MP:
				case CUR_CP:
				case CUR_DP:
				case CUR_BP:
				{
					_isVisible = true;
				}
			}
		}
	}
	
	public void addCaster(WorldObject object)
	{
		_casterObjectId = object.getObjectId();
	}
	
	public boolean hasUpdates()
	{
		return !_updates.isEmpty();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.STATUS_UPDATE.writeId(this, buffer);
		buffer.writeInt(_objectId); // casterId
		buffer.writeInt(_isVisible ? _casterObjectId : 0);
		buffer.writeByte(_isVisible);
		buffer.writeByte(_updates.size());
		for (Entry<StatusUpdateType, Number> entry : _updates.entrySet())
		{
			final int statusTypeId = entry.getKey().getClientId();
			buffer.writeByte(statusTypeId);
			if ((statusTypeId == StatusUpdateType.CUR_HP.getClientId()) || (statusTypeId == StatusUpdateType.MAX_HP.getClientId()))
			{
				buffer.writeLong(entry.getValue().longValue());
			}
			else
			{
				buffer.writeInt(entry.getValue().intValue());
			}
		}
	}
	
	@Override
	public boolean canBeDropped(GameClient client)
	{
		return true;
	}
}
