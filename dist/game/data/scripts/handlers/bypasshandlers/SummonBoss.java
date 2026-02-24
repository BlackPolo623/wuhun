package handlers.bypasshandlers;

import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;

import handlers.itemhandlers.SummonBossSelector;

/**
 * BOSS 召喚 Bypass Handler
 * 處理來自 SummonBossSelector 道具的 bypass 命令
 */
public class SummonBoss implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"summon_boss",
		"close_window"
	};

	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		// 處理關閉視窗
		if (command.equals("close_window"))
		{
			return true;
		}

		// 處理 BOSS 召喚
		if (command.startsWith("summon_boss"))
		{
			return SummonBossSelector.handleBossSummon(player, command);
		}

		return false;
	}

	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
