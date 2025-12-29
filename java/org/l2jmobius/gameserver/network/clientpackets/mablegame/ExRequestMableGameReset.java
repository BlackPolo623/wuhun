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
package org.l2jmobius.gameserver.network.clientpackets.mablegame;

import org.l2jmobius.gameserver.data.xml.MableGameData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.mablegame.ExMableGameShowPlayerState;

public class ExRequestMableGameReset extends ClientPacket
{
	@Override
	public void readImpl()
	{
		readInt(); // ResetItemType (Always 1)
	}
	
	@Override
	public void runImpl()
	{
		final MableGameData data = MableGameData.getInstance();
		if (!data.isEnabled())
		{
			return;
		}
		
		final Player player = getClient().getPlayer();
		if (player == null)
		{
			return;
		}
		
		// Check if can reset.
		final MableGameData.MableGamePlayerState playerState = data.getPlayerState(player.getAccountName());
		if ((playerState.getCurrentCellId() < data.getHighestCellId()) || (playerState.getRound() == data.getDailyAvailableRounds()))
		{
			return;
		}
		
		// Check items.
		for (ItemHolder itemHolder : data.getResetItems())
		{
			if (player.getInventory().getInventoryItemCount(itemHolder.getId(), -1) < itemHolder.getCount())
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT);
				return;
			}
		}
		
		// Remove Items.
		for (ItemHolder itemHolder : data.getResetItems())
		{
			if (!player.destroyItemByItemId(ItemProcessType.FEE, itemHolder.getId(), itemHolder.getCount(), player, true))
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT);
				return;
			}
		}
		
		// Reset.
		playerState.setRound(playerState.getRound() + 1);
		playerState.setCurrentCellId(1);
		playerState.setRemainCommonDice(data.getCommonDiceLimit());
		player.sendPacket(new ExMableGameShowPlayerState(player));
	}
}
