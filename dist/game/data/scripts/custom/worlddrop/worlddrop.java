package custom.worlddrop;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Quest;
import org.l2jmobius.gameserver.model.variables.GlobalVariables;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

public class worlddrop extends Quest
{
	// 掉落物配置
	private static final int ITEM_A = 10001;
	private static final int ITEM_B = 10002;
	private static final int ITEM_C = 10003;
	
	// 全服最大掉落数量
	private static final int LIMIT_A = 8;
	private static final int LIMIT_B = 6;
	private static final int LIMIT_C = 10;
	
	// 掉落概率（全局每次击杀）
	private static final double CHANCE_A = 0.5; // 50%
	private static final double CHANCE_B = 0.3;
	private static final double CHANCE_C = 0.2;
	
	// 7天刷新周期
	private static final long RESET_TIME = 7 * 24 * 60 * 60 * 1000L;
	
	// 掉落怪物范围
	private static final int[] MONSTERS =
	{
		20001,
		20002,
		20003
	};
	
	// GlobalVars KEY
	private static final String KEY_A = "WLORD_DROP_A_COUNT";
	private static final String KEY_B = "WLORD_DROP_B_COUNT";
	private static final String KEY_C = "WLORD_DROP_C_COUNT";
	private static final String RESET_KEY = "WLORD_DROP_LAST_RESET";
	
	public worlddrop()
	{
		super(-1);
		addKillId(MONSTERS);
		init();
	}
	
	private void init()
	{
		// 初始化数据
		if (GlobalVariables.getLong(RESET_KEY, 0) == 0)
		{
			resetAll();
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		checkReset();
		
		// 当前剩余
		int aLeft = GlobalVariables.getInt(KEY_A, 0);
		int bLeft = GlobalVariables.getInt(KEY_B, 0);
		int cLeft = GlobalVariables.getInt(KEY_C, 0);
		
		ThreadLocalRandom r = ThreadLocalRandom.current();
		
		// 依序掉落，优先A
		if ((aLeft > 0) && (r.nextDouble() < CHANCE_A))
		{
			dropItem(killer, ITEM_A, KEY_A);
			return;
		}
		if ((bLeft > 0) && (r.nextDouble() < CHANCE_B))
		{
			dropItem(killer, ITEM_B, KEY_B);
			return;
		}
		if ((cLeft > 0) && (r.nextDouble() < CHANCE_C))
		{
			dropItem(killer, ITEM_C, KEY_C);
			return;
		}
		
		return;
	}
	
	private void dropItem(Player killer, int itemId, String key)
	{
		killer.addItem(null, itemId, 1, killer, true);
		
		int now = GlobalVariables.getInt(key, 0);
		int left = now - 1;
		GlobalVariables.set(key, left);
		// 公告广播
		broadcast(itemId, killer, left);
	}
	
	private void broadcast(int itemId, Player killer, int left)
	{
		long lastReset = GlobalVariables.getLong(RESET_KEY, 0);
		long nextReset = lastReset + RESET_TIME;
		// 格式化时间显示
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		String nextTime = sdf.format(new Date(nextReset));
		String msg = "【世界掉落】玩家【" + killer.getName() + "】获得限量掉落物品：" + itemId + " | 剩余数量：" + left + " | 下次刷新：" + nextTime;
		killer.sendPacket(new CreatureSay(null, ChatType.GENERAL, "稀有掉落", msg));
	}
	
	private void checkReset()
	{
		long last = GlobalVariables.getLong(RESET_KEY, 0);
		long now = System.currentTimeMillis();
		
		if ((now - last) >= RESET_TIME)
		{
			resetAll();
			System.out.println("WLORD DROP RESETED");
		}
	}
	
	private void resetAll()
	{
		GlobalVariables.set(KEY_A, LIMIT_A);
		GlobalVariables.set(KEY_B, LIMIT_B);
		GlobalVariables.set(KEY_C, LIMIT_C);
		GlobalVariables.set(RESET_KEY, System.currentTimeMillis());
	}
}
