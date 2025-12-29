package custom.WuHunMingXiang;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

public class WuHunMingXiang extends Script
{
	private static final int PERIOD_SECONDS = 120;

	// 獎勵池：{物品ID, 數量}
	private static final int[][] RANDOM_ITEMS =
			{
					{57, 100},
					{57, 1000},
					{57, 10000},
					{57, 100},
					{57, 1000},
					{57, 10000},
					{57, 100},
					{57, 1000},
					{57, 10000},
					{57, 100},
					{57, 1000},
					{57, 10000},
					{57, 100},
					{57, 1000},
					{57, 10000},
					{105801, 1},
					{105801, 3},
					{105801, 5},
					{105801, 7},
					{105801, 9},
					{105801, 15},
					{105801, 18},
					{105801, 24},
					{105801, 30},
					{105801, 50},
					{94871, 1},
					{94871, 2},
					{94871, 3},
					{92314, 1},
					{92314, 2},
					{92314, 3},
					{92314, 4},
					{92314, 5},
					{108000, 1},
					{108001, 1},
					{108002, 1},
					{108003, 1},
					{108004, 1},
					{108005, 1},
					{108006, 1},
					{108007, 1},
					{108008, 1},
					{108000, 2},
					{108001, 2},
					{108002, 2},
					{108003, 2},
					{108004, 2},
					{108005, 2},
					{108006, 2},
					{108007, 2},
					{108008, 2},
					{108000, 1},
					{108001, 1},
					{108002, 1},
					{108003, 1},
					{108004, 1},
					{108005, 1},
					{108006, 1},
					{108007, 1},
					{108008, 1},
					{108000, 2},
					{108001, 2},
					{108002, 2},
					{108003, 2},
					{108004, 2},
					{108005, 2},
					{108006, 2},
					{108007, 2},
					{108008, 2},
			};

	private static final String VAR_DOING = "MINGXIANG_DOING";
	private static final String VAR_COUNT = "MINGXIANG_COUNT";

	private ScheduledFuture<?> _task;

	public WuHunMingXiang()
	{
		startGlobalTask();
	}

	/** 啟動全域每 N 秒檢查一次 */
	private void startGlobalTask()
	{
		if ((_task != null) && !_task.isCancelled())
		{
			_task.cancel(true);
		}

		_task = ThreadPool.scheduleAtFixedRate(() ->
		{
			for (Player player : World.getInstance().getPlayers())
			{
				if ((player == null) || !player.isOnline())
				{
					continue;
				}

				boolean doing = player.getVariables().getBoolean(VAR_DOING, false);
				if (!doing)
				{
					continue;
				}

				giveReward(player);
			}
		}, PERIOD_SECONDS * 1000, PERIOD_SECONDS * 1000); // 轉換為毫秒
	}

	/** 給予獎勵 */
	private void giveReward(Player player)
	{
		int[] reward = RANDOM_ITEMS[getRandom(RANDOM_ITEMS.length)];
		int itemId = reward[0];   // 物品ID
		int amount = reward[1];   // 數量

		player.addItem(null, itemId, amount, player, true);

		int count = player.getVariables().getInt(VAR_COUNT, 0) + 1;
		player.getVariables().set(VAR_COUNT, count);
	}

	public static void main(String[] args)
	{
		new WuHunMingXiang();
	}
}