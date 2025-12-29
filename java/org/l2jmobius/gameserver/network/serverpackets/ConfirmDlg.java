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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage.SMParam;

/**
 * @author Mobius
 */
public class ConfirmDlg extends ServerPacket
{
	private int _time;
	private int _requesterId;
	private final SystemMessage _systemMessage;
	
	public ConfirmDlg(SystemMessageId smId)
	{
		_systemMessage = new SystemMessage(smId);
	}
	
	public ConfirmDlg(int id)
	{
		_systemMessage = new SystemMessage(id);
	}
	
	public ConfirmDlg(String text)
	{
		_systemMessage = new SystemMessage(SystemMessageId.S1_3);
		_systemMessage.addString(text);
	}
	
	public ConfirmDlg addTime(int time)
	{
		_time = time;
		return this;
	}
	
	public ConfirmDlg addRequesterId(int id)
	{
		_requesterId = id;
		return this;
	}
	
	public SystemMessage getSystemMessage()
	{
		return _systemMessage;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.CONFIRM_DLG.writeId(this, buffer);
		final SMParam[] params = _systemMessage.getParams();
		buffer.writeInt(_systemMessage.getId());
		buffer.writeInt(params.length);
		for (SMParam param : params)
		{
			buffer.writeInt(param.getType());
			switch (param.getType())
			{
				case SystemMessage.TYPE_ELEMENT_NAME:
				case SystemMessage.TYPE_BYTE:
				case SystemMessage.TYPE_FACTION_NAME:
				{
					buffer.writeByte(param.getIntValue());
					break;
				}
				case SystemMessage.TYPE_CASTLE_NAME:
				case SystemMessage.TYPE_SYSTEM_STRING:
				case SystemMessage.TYPE_INSTANCE_NAME:
				case SystemMessage.TYPE_CLASS_ID:
				{
					buffer.writeShort(param.getIntValue());
					break;
				}
				case SystemMessage.TYPE_ITEM_NAME:
				case SystemMessage.TYPE_INT_NUMBER:
				case SystemMessage.TYPE_NPC_NAME:
				case SystemMessage.TYPE_DOOR_NAME:
				{
					buffer.writeInt(param.getIntValue());
					break;
				}
				case SystemMessage.TYPE_LONG_NUMBER:
				{
					buffer.writeLong(param.getLongValue());
					break;
				}
				case SystemMessage.TYPE_TEXT:
				case SystemMessage.TYPE_PLAYER_NAME:
				{
					buffer.writeString(param.getStringValue());
					break;
				}
				case SystemMessage.TYPE_SKILL_NAME:
				{
					final int[] array = param.getIntArrayValue();
					buffer.writeInt(array[0]); // skill id
					buffer.writeShort(array[1]); // skill level
					buffer.writeShort(array[2]); // skill sub level
					break;
				}
				case SystemMessage.TYPE_POPUP_ID:
				case SystemMessage.TYPE_ZONE_NAME:
				{
					final int[] array = param.getIntArrayValue();
					buffer.writeInt(array[0]); // x
					buffer.writeInt(array[1]); // y
					buffer.writeInt(array[2]); // z
					break;
				}
			}
		}
		
		buffer.writeInt(_time);
		buffer.writeInt(_requesterId);
	}
}
