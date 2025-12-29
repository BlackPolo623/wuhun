/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.voicedcommandhandlers;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.SendTradeRequest;

/**
 * @author Mobius, Gigi
 */
public class Trade implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"交易",
		"trade"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar, String target)
	{
		switch (command)
		{
			case "交易":
			case "trade":
			{
				WorldObject targeta = activeChar.getTarget();
				if(targeta != null){
					if(!targeta.isNpc() && !targeta.isFakePlayer() && (targeta.asPlayer().getObjectId() != activeChar.getObjectId()) ){
						if(activeChar.calculateDistance3D(targeta.asPlayer()) <= 150){
							activeChar.onTransactionRequest(targeta.asPlayer());
							targeta.asPlayer().sendPacket(new SendTradeRequest(activeChar.getObjectId()));
						}else{
							activeChar.sendPacket(SystemMessageId.YOUR_TARGET_IS_OUT_OF_RANGE);
						}
					}else{
						activeChar.sendPacket(new ExShowScreenMessage("請選擇玩家作為目標", 5000));
					}
				}else{
					activeChar.sendPacket(new ExShowScreenMessage("請選擇正確目標", 5000));
				}
			}
		}
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}