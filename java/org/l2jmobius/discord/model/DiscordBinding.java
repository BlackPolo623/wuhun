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
package org.l2jmobius.discord.model;

/**
 * 角色與 Discord 帳號的綁定資料。
 * @author Custom
 */
public class DiscordBinding
{
	private final int _playerId;
	private final String _playerName;
	private final String _discordId;

	public DiscordBinding(int playerId, String playerName, String discordId)
	{
		_playerId = playerId;
		_playerName = playerName;
		_discordId = discordId;
	}

	/** 遊戲角色的 ObjectId */
	public int getPlayerId()
	{
		return _playerId;
	}

	/** 遊戲角色名稱 */
	public String getPlayerName()
	{
		return _playerName;
	}

	/** Discord 用戶 ID（18 位數字字串） */
	public String getDiscordId()
	{
		return _discordId;
	}

	@Override
	public String toString()
	{
		return "DiscordBinding{playerId=" + _playerId + ", playerName=" + _playerName + ", discordId=" + _discordId + "}";
	}
}
