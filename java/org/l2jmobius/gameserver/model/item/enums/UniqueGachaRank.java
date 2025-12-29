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
package org.l2jmobius.gameserver.model.item.enums;

import java.util.HashMap;
import java.util.Map;

public enum UniqueGachaRank
{
	RANK_UR(1),
	RANK_SR(2),
	RANK_R(3);
	
	private final int _clientId;
	
	private static final Map<Integer, UniqueGachaRank> VALUES = new HashMap<>(3);
	static
	{
		for (UniqueGachaRank rank : values())
		{
			VALUES.put(rank.getClientId(), rank);
		}
	}
	
	UniqueGachaRank(int clientId)
	{
		_clientId = clientId;
	}
	
	public static UniqueGachaRank getRankByClientId(int clientId)
	{
		return VALUES.getOrDefault(clientId, null);
	}
	
	public int getClientId()
	{
		return _clientId;
	}
}
