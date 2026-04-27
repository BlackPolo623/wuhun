package handlers.voicedcommandhandlers;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * 系統訊息顯示開關
 * .開啟系統訊息 / .關閉系統訊息
 */
public class SystemMessageToggle implements IVoicedCommandHandler
{
	private static final String VAR_KEY = "SYSTEM_MSG_VISIBLE";

	private static final String[] VOICED_COMMANDS =
	{
		"開啟系統訊息",
		"關閉系統訊息",
	};

	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		if (player == null)
		{
			return false;
		}

		switch (command)
		{
			case "開啟系統訊息":
			{
				player.getVariables().set(VAR_KEY, 1);
				player.broadcastUserInfo();
				player.sendMessage("[系統訊息] 已開啟顯示系統訊息。");
				return true;
			}
			case "關閉系統訊息":
			{
				player.getVariables().set(VAR_KEY, 0);
				player.broadcastUserInfo();
				player.sendMessage("[系統訊息] 已關閉顯示系統訊息。");
				return true;
			}
		}
		return false;
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
