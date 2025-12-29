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

import org.l2jmobius.gameserver.config.custom.AutoPotionsConfig;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.taskmanagers.AutoPotionTaskManager;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Mobius, Gigi
 */
public class AutoPotion implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
			"開水",
			"自動喝水",
			"關水",
			"關閉自動喝水",
		"apon",
		"apoff",
		"potionon",
		"potionoff"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar, String target)
	{
		if (!AutoPotionsConfig.AUTO_POTIONS_ENABLED || (activeChar == null))
		{
			return false;
		}
		
		switch (command)
		{
			case "開水":
			case "自動喝水":
			case "apon":
			case "potionon":
			{
				AutoPotionTaskManager.getInstance().add(activeChar);
				activeChar.sendPacket(new CreatureSay(null, ChatType.WORLD, "自動喝水", "自動喝水功能已經開啟"));
				break;
			}
			case "關水":
			case "關閉自動喝水":
			case "apoff":
			case "potionoff":
			{
				AutoPotionTaskManager.getInstance().remove(activeChar);
				activeChar.sendPacket(new CreatureSay(null, ChatType.WORLD, "自動喝水", "自動喝水功能已經關閉"));
				break;
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
