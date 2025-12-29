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
package org.l2jmobius.gameserver.network.enums;

/**
 * @author St3eT, Mobius
 */
public enum ChatType
{
	GENERAL(0),
	SHOUT(1),
	WHISPER(2),
	PARTY(3),
	CLAN(4),
	GM(5),
	PETITION_PLAYER(6),
	PETITION_GM(7),
	TRADE(8),
	ALLIANCE(9),
	ANNOUNCEMENT(10),
	BOAT(11),
	FRIEND(12),
	MSNCHAT(13),
	PARTYMATCH_ROOM(14),
	PARTYROOM_COMMANDER(15),
	PARTYROOM_ALL(16),
	HERO_VOICE(17),
	CRITICAL_ANNOUNCE(18),
	SCREEN_ANNOUNCE(19),
	BATTLEFIELD(20),
	MPCC_ROOM(21),
	NPC_GENERAL(22),
	NPC_SHOUT(23),
	NPC_WHISPER(24),
	WORLD(25),
	UNIVERSAL(29);
	
	private final int _clientId;
	
	private ChatType(int clientId)
	{
		_clientId = clientId;
	}
	
	/**
	 * @return the client id.
	 */
	public int getClientId()
	{
		return _clientId;
	}
	
	/**
	 * Finds the {@code ChatType} by its clientId
	 * @param clientId the clientId
	 * @return the {@code ChatType} if it is found, {@code null} otherwise.
	 */
	public static ChatType findByClientId(int clientId)
	{
		for (ChatType ChatType : values())
		{
			if (ChatType.getClientId() == clientId)
			{
				return ChatType;
			}
		}
		
		return null;
	}
}
