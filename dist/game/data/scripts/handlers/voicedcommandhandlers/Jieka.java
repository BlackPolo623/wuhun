package handlers.voicedcommandhandlers;

import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

public class Jieka implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
			"QLFB",
			"解卡",
			"ex",
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		if (command.equals("QLFB") || command.equals("解卡") || command.equals("ex"))
		{
			final Map<Integer, Long> instanceTimes = InstanceManager.getInstance().getAllInstanceTimes(activeChar);
			for (Entry<Integer, Long> entry : instanceTimes.entrySet())
			{
				final int id = entry.getKey();
				InstanceManager.getInstance().deleteInstanceTime(activeChar, id);
			}
			activeChar.sendPacket(new CreatureSay(null, ChatType.WORLD, "解卡功能", "已經完成解卡！"));
			
		}
		return true;
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}