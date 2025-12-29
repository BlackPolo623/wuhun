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
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

public class ExRequestMableGamePopupOk extends ClientPacket
{
	@Override
	public void readImpl()
	{
		readByte(); // cellType
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
		
		final MableGameData.MableGamePlayerState playerState = data.getPlayerState(player.getAccountName());
		final int pendingCellId = playerState.getPendingCellIdPopup();
		if (pendingCellId < 1)
		{
			return;
		}
		
		final MableGameData.MableGameCell cell = data.getCellById(pendingCellId);
		if (cell != null)
		{
			// if ((cell.getId() != data.getHighestCellId()) && (cell.getColor().getClientId() != _cellType) && (MableGameCellColor.getByClientId(_cellType) != MableGameCellColor.LIGHT_BLUE))
			// if (MableGameCellColor.getByClientId(_cellType) == MableGameCellColor.LIGHT_BLUE)
			// {
			// return;
			// }
			
			playerState.setCurrentCellId(pendingCellId);
			playerState.setPendingCellIdPopup(-1);
			switch (cell.getColor())
			{
				case LIGHT_BLUE:
				case GREEN:
				case RED:
				case BURNING_RED:
				{
					if (playerState.getPendingReward() != null)
					{
						player.addItem(ItemProcessType.REWARD, playerState.getPendingReward(), player, true);
						playerState.setPendingReward(null);
					}
					else if (playerState.isMoved())
					{
						playerState.handleCell(player, cell);
					}
					break;
				}
				case YELLOW:
				{
					// Popup is handling other type.
					break;
				}
				case PURPLE:
				{
					if (playerState.isMoved())
					{
						playerState.handleCell(player, cell);
					}
					break;
				}
				default:
				{
					PacketLogger.warning(getClass().getSimpleName() + ": Unhandled Cell Id:" + cell.getId() + " Color:" + cell.getColor());
					break;
				}
			}
		}
	}
}
