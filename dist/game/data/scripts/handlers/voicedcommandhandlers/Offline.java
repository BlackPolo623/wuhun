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
package handlers.voicedcommandhandlers;

import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;

/**
 * @author Mobius
 */
public class Offline implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"offline",
		"離線商店"
	};
	
	@Override
	public boolean onCommand(String command, Player player, String target)
	{
		if (( command.equals("offline")||command.equals("離線商店") )&& OfflineTradeConfig.ENABLE_OFFLINE_COMMAND && (OfflineTradeConfig.OFFLINE_TRADE_ENABLE || OfflineTradeConfig.OFFLINE_CRAFT_ENABLE))
		{
			if (player.isPrisoner())
			{
				player.sendMessage("You can't use .offline as a prisoner.");
				return false;
			}

			// 【修復漏洞】檢查玩家是否正在冥想中
			// 冥想狀態下不可進入離線商店，否則角色會持續留在世界中並繼續獲得冥想獎勵
			if (player.getVariables().getBoolean("MINGXIANG_DOING", false))
			{
				player.sendMessage("冥想中無法使用離線商店！請先停止冥想再開啟離線商店。");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			if (!player.isInStoreMode())
			{
				player.sendMessage("你並沒有開啟商店，無法使用離線商店");
				player.sendPacket(SystemMessageId.PRIVATE_STORE_ALREADY_CLOSED);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			if (player.isInInstance() || player.isInVehicle() || !player.canLogout())
			{
				player.sendMessage("你現在的狀態無法使用離線商店");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			player.sendPacket(new ConfirmDlg(SystemMessageId.THE_GAME_WILL_BE_CLOSED_CONTINUE));
		}

		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
