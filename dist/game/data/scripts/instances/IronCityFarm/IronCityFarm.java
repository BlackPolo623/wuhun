/*
 * 鋼鐵之城公共副本 - 8區共享版本
 * 玩家可自由選擇進入哪一區，同區所有玩家共享同一個世界實例。
 * NPC ID: 900040
 * 副本模板 ID: 804
 */
package instances.IronCityFarm;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class IronCityFarm extends InstanceScript
{
	// 入口 NPC（請在世界中放置此 ID 的 NPC）
	private static final int NPC_ID = 900040;

	// 副本模板 ID（對應 鋼鐵之城公共.xml 的 id="804"）
	private static final int TEMPLATE_ID = 803;

	// ==================== 8 個區的共享世界 ID ====================
	// 同一區的所有玩家共用同一個 instanceId，形成公共副本
	private static final int INSTANCE_ID_ZONE1 = 820;
	private static final int INSTANCE_ID_ZONE2 = 821;
	private static final int INSTANCE_ID_ZONE3 = 822;
	private static final int INSTANCE_ID_ZONE4 = 823;
	private static final int INSTANCE_ID_ZONE5 = 824;
	private static final int INSTANCE_ID_ZONE6 = 825;
	private static final int INSTANCE_ID_ZONE7 = 826;
	private static final int INSTANCE_ID_ZONE8 = 827;

	// 副本內傳送座標（鋼鐵之城地圖入口）
	private static final int ENTER_X = 180182;
	private static final int ENTER_Y = 75498;
	private static final int ENTER_Z = -13768;

	public IronCityFarm()
	{
		super(TEMPLATE_ID);
		addStartNpc(NPC_ID);
		addFirstTalkId(NPC_ID);
		addTalkId(NPC_ID);
		addInstanceLeaveId(TEMPLATE_ID);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/instances/IronCityFarm/IronCityFarm.htm");
		// 顯示玩家本週已取得的特殊道具數量（供 HTM 顯示用）
		final int weeklyCount = player.getVariables().getInt("WeeklyDrop_92476", 0);
		html.replace("%weekly_count%", String.valueOf(weeklyCount));
		player.sendPacket(html);
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("enterZone"))
		{
			try
			{
				final int zoneNum = Integer.parseInt(event.substring(9).trim());
				enterZone(player, zoneNum);
			}
			catch (NumberFormatException e)
			{
				// 忽略格式錯誤
			}
		}
		return null;
	}

	private void enterZone(Player player, int zoneNum)
	{
		final int instanceId = getZoneInstanceId(zoneNum);
		if (instanceId < 0)
		{
			player.sendMessage("無效的區域編號！");
			return;
		}

		// 防止重複進入同一副本
		final Instance current = player.getInstanceWorld();
		if ((current != null) && (current.getTemplateId() == TEMPLATE_ID))
		{
			player.sendMessage("您已在鋼鐵之城副本中，請先離開再重新進入。");
			return;
		}

		// 取得或建立共享世界
		boolean instanceExists = InstanceManager.getInstance().createInstanceFromTemplate(instanceId, TEMPLATE_ID);
		final Instance instance;
		if (instanceExists)
		{
			instance = InstanceManager.getInstance().createInstance(TEMPLATE_ID, instanceId, player);
		}
		else
		{
			instance = InstanceManager.getInstance().getInstance(instanceId);
		}

		if (instance == null)
		{
			player.sendMessage("無法建立副本，請稍後再試。");
			return;
		}

		// 傳送進入
		player.setInstance(instance);
		player.teleToLocation(ENTER_X, ENTER_Y, ENTER_Z, 0, instance);

		player.sendMessage("========================================");
		player.sendMessage("【鋼鐵之城】歡迎進入第 " + zoneNum + " 區");
		player.sendMessage("本週特殊道具已取得：" + player.getVariables().getInt("WeeklyDrop_92476", 0) + " 個");
		player.sendMessage("========================================");
	}

	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		player.sendMessage("已離開鋼鐵之城副本。");
	}

	/**
	 * 根據區號回傳對應的共享世界 ID
	 */
	private int getZoneInstanceId(int zone)
	{
		switch (zone)
		{
			case 1: return INSTANCE_ID_ZONE1;
			case 2: return INSTANCE_ID_ZONE2;
			case 3: return INSTANCE_ID_ZONE3;
			case 4: return INSTANCE_ID_ZONE4;
			case 5: return INSTANCE_ID_ZONE5;
			case 6: return INSTANCE_ID_ZONE6;
			case 7: return INSTANCE_ID_ZONE7;
			case 8: return INSTANCE_ID_ZONE8;
			default: return -1;
		}
	}

	public static void main(String[] args)
	{
		new IronCityFarm();
		System.out.println("【鋼鐵之城公共副本】載入完畢！8區共享版本 (TEMPLATE_ID=804, NPC=900040)");
	}
}
