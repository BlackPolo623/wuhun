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

import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.model.BlockList;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

public class RequestBlock extends ClientPacket
{
	private static final int BLOCK = 0;
	private static final int UNBLOCK = 1;
	private static final int BLOCKLIST = 2;
	private static final int ALLBLOCK = 3;
	private static final int ALLUNBLOCK = 4;
	
	private String _name;
	private Integer _type;
	
	@Override
	protected void readImpl()
	{
		_type = readInt(); // 0x00 - block, 0x01 - unblock, 0x03 - allblock, 0x04 - allunblock
		if ((_type == BLOCK) || (_type == UNBLOCK))
		{
			_name = readString();
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		final int targetId = CharInfoTable.getInstance().getIdByName(_name);
		final int targetAL = CharInfoTable.getInstance().getAccessLevelById(targetId);
		if (player == null)
		{
			return;
		}
		
		switch (_type)
		{
			case BLOCK:
			case UNBLOCK:
			{
				// TODO: Save in database? :P
				if (FakePlayerData.getInstance().isTalkable(_name))
				{
					if (_type == BLOCK)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_ADDED_TO_YOUR_IGNORE_LIST);
						sm.addString(FakePlayerData.getInstance().getProperName(_name));
						player.sendPacket(sm);
					}
					else
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_REMOVED_FROM_YOUR_IGNORE_LIST);
						sm.addString(FakePlayerData.getInstance().getProperName(_name));
						player.sendPacket(sm);
					}
					return;
				}
				
				// can't use block/unblock for locating invisible characters
				if (targetId <= 0)
				{
					// Incorrect player name.
					player.sendPacket(SystemMessageId.ERROR_WHEN_ADDING_A_USER_TO_YOUR_IGNORE_LIST);
					return;
				}
				
				if (targetAL > 0)
				{
					// Cannot block a GM character.
					player.sendPacket(SystemMessageId.YOU_CANNOT_BAN_A_GM);
					return;
				}
				
				if (player.getObjectId() == targetId)
				{
					return;
				}
				
				if (_type == BLOCK)
				{
					BlockList.addToBlockList(player, targetId);
				}
				else
				{
					BlockList.removeFromBlockList(player, targetId);
				}
				break;
			}
			case BLOCKLIST:
			{
				BlockList.sendListToOwner(player);
				break;
			}
			case ALLBLOCK:
			{
				player.sendPacket(SystemMessageId.MESSAGE_REFUSAL_MODE);
				BlockList.setBlockAll(player, true);
				break;
			}
			case ALLUNBLOCK:
			{
				player.sendPacket(SystemMessageId.MESSAGE_ACCEPTANCE_MODE);
				BlockList.setBlockAll(player, false);
				break;
			}
			default:
			{
				PacketLogger.info("Unknown 0xA9 block type: " + _type);
			}
		}
	}
}
