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
package org.l2jmobius.gameserver.network.serverpackets.balok;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.BattleWithBalokManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class BalrogWarShowUI extends ServerPacket
{
	private final Player _player;
	private final int _personalPoints;
	private final long _globalPoints;
	private final int _rank;
	private final int _active;
	private final int _reward;
	
	public BalrogWarShowUI(Player player)
	{
		_player = player;
		_personalPoints = BattleWithBalokManager.getInstance().getMonsterPoints(_player);
		_globalPoints = BattleWithBalokManager.getInstance().getGlobalPoints();
		_rank = _personalPoints < 1 ? 0 : BattleWithBalokManager.getInstance().getPlayerRank(_player);
		_active = _player.getVariables().getInt(PlayerVariables.BALOK_AVAILABLE_REWARD, 0);
		_reward = BattleWithBalokManager.getInstance().getReward();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_BALROGWAR_SHOW_UI.writeId(this, buffer);
		buffer.writeInt(_rank); // Personal rank.
		buffer.writeInt(_personalPoints); // Personal points.
		buffer.writeLong(_globalPoints); // total points of players.
		buffer.writeInt(_active); // Activated reward or not.
		buffer.writeInt(_reward); // Reward item id.
		buffer.writeLong(1); // Unknown.
	}
}
