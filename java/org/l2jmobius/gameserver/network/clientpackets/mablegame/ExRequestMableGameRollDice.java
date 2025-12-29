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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.MableGameData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.mablegame.ExMableGameDiceResult;
import org.l2jmobius.gameserver.network.serverpackets.mablegame.ExMableGamePrison;

public class ExRequestMableGameRollDice extends ClientPacket
{
	private byte _diceType;
	
	@Override
	public void readImpl()
	{
		_diceType = readByte();
	}
	
	@Override
	public void runImpl()
	{
		if (!MableGameData.getInstance().isEnabled())
		{
			return;
		}
		
		final Player player = getClient().getPlayer();
		if (player == null)
		{
			return;
		}
		
		final MableGameData data = MableGameData.getInstance();
		final MableGameData.MableGamePlayerState playerState = data.getPlayerState(player.getAccountName());
		playerState.setMoved(false);
		
		if (playerState.getCurrentCellId() >= data.getHighestCellId())
		{
			return;
		}
		
		if (_diceType == 0)
		{
			if (playerState.getRemainCommonDice() <= 0)
			{
				return;
			}
			
			playerState.setRemainCommonDice(playerState.getRemainCommonDice() - 1);
		}
		
		if (!player.destroyItemByItemId(ItemProcessType.FEE, _diceType == 1 ? MableGameData.ENHANCED_DICE_ITEM_ID : MableGameData.COMMON_DICE_ITEM_ID, 1, player, true))
		{
			return;
		}
		
		int dice = _diceType == 1 ? Rnd.get(5, 6) : Rnd.get(1, 6);
		boolean diceChanged = false;
		if (playerState.getRemainingPrisonRolls() > 0)
		{
			if ((dice >= MableGameData.MIN_PRISON_DICE) && (dice <= MableGameData.MAX_PRISON_DICE))
			{
				playerState.setRemainingPrisonRolls(0);
				player.sendPacket(new ExMableGameDiceResult(dice, playerState.getCurrentCellId() + 1, data.getCellById(playerState.getCurrentCellId() + 1).getColor().getClientId(), playerState.getRemainCommonDice()));
				dice = 1;
				diceChanged = true;
			}
			else
			{
				playerState.setRemainingPrisonRolls(playerState.getRemainingPrisonRolls() - 1);
				if (playerState.getRemainingPrisonRolls() <= 0)
				{
					player.sendPacket(new ExMableGameDiceResult(dice, playerState.getCurrentCellId() + 1, data.getCellById(playerState.getCurrentCellId() + 1).getColor().getClientId(), playerState.getRemainCommonDice()));
					dice = 1;
					diceChanged = true;
				}
				else
				{
					player.sendPacket(new ExMableGameDiceResult(dice, playerState.getCurrentCellId(), data.getCellById(playerState.getCurrentCellId()).getColor().getClientId(), playerState.getRemainCommonDice()));
					player.sendPacket(new ExMableGamePrison(MableGameData.MIN_PRISON_DICE, MableGameData.MAX_PRISON_DICE, playerState.getRemainingPrisonRolls()));
					return;
				}
			}
		}
		
		final int newCellId = Math.min(playerState.getCurrentCellId() + dice, data.getHighestCellId());
		playerState.setCurrentCellId(newCellId);
		
		final MableGameData.MableGameCell newCell = data.getCellById(newCellId);
		if (!diceChanged)
		{
			player.sendPacket(new ExMableGameDiceResult(dice, newCellId, newCell.getColor().getClientId(), playerState.getRemainCommonDice()));
		}
		
		playerState.handleCell(player, newCell);
	}
}
