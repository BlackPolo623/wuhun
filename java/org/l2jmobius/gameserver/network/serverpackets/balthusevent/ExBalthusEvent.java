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
package org.l2jmobius.gameserver.network.serverpackets.balthusevent;

import java.util.Calendar;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.managers.events.BalthusEventManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * Consolation prize changing in SysTextures/ui6.ugx file "RewardClip.as" -> configUI -> this.tokenItemID = 49783;
 * @author Index
 */
public class ExBalthusEvent extends ServerPacket
{
	private final int _rewardItemId;
	private final int _currentProgress;
	private final int _rewardTokenCount;
	private final int _consolationCount;
	private final boolean _isParticipant;
	private final boolean _isRunning;
	private final int _hour;
	
	public ExBalthusEvent(Player player)
	{
		final BalthusEventManager manager = BalthusEventManager.getInstance();
		final ItemHolder currentReward = manager.getCurrRewardItem();
		_rewardItemId = currentReward != null ? currentReward.getId() : 57;
		_currentProgress = manager.getCurrentProgress() * 20;
		_rewardTokenCount = player.getVariables().getInt(PlayerVariables.BALTHUS_REWARD, 0);
		_consolationCount = (int) Math.min(manager.getConsolation().getCount(), Integer.MAX_VALUE);
		_isParticipant = manager.isPlayerParticipant(player);
		_isRunning = manager.isRunning();
		_hour = Calendar.getInstance().get(Calendar.HOUR);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_BALTHUS_EVENT.writeId(this, buffer);
		buffer.writeInt(_hour);
		buffer.writeInt(_currentProgress);
		buffer.writeInt(_rewardItemId);
		buffer.writeInt(_rewardTokenCount); // Current items for withdraw (available rewards).
		buffer.writeInt(_consolationCount);
		buffer.writeInt(_isParticipant);
		buffer.writeByte(_isRunning);
		buffer.writeInt(_hour * 60);
	}
}
