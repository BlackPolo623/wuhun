/**
 * Jacky 制作,商业化专用脚本
 */

package custom.Namecolor;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

// VIP 金币建议ＧＭ出售比例１：１００　 

public class Namecolor extends Script
{
	
	private final static int itemid = 105900;// 道具id
	private static ScheduledFuture<?> _selfDestructionTask = null;
	
	private Namecolor()
	{
		if (_selfDestructionTask != null)
		{
			_selfDestructionTask.cancel(true);
			_selfDestructionTask = null;
		}
		_selfDestructionTask = ThreadPool.scheduleAtFixedRate(new Check(), 1000, 1000);
	}
	
	protected class Check implements Runnable
	{
		@Override
		public void run()
		{
			bianming();
		}
	}
	
	private void bianming()
	{
		for (Player player : World.getInstance().getPlayers())
		{
			if (player.isOnline() && (player.getInventory().getInventoryItemCount(itemid, 0) >= 1))
			{
				int COLOR = Rnd.get(16777215);
				if ((COLOR == 65280) || (COLOR == 1044480))
				{
					COLOR = 0;
				}
				player.getAppearance().setNameColor(COLOR);
			}
			player.broadcastUserInfo();
		}
	}
	
	public static void main(String[] args)
	{
		new Namecolor();
		System.out.println("彩名系統载入");
	}
}