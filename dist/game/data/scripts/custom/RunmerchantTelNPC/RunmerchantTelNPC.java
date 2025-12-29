package custom.RunmerchantTelNPC;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 跑商傳送NPC - 只有跑商狀態才能使用
 * @author 黑普羅
 */
public class RunmerchantTelNPC extends Script
{
	// ==================== 配置區域 ====================

	// NPC ID
	private static final int NPC_ID = 910007;

	// 目的地坐標
	private static final Location DESTINATION = new Location(83078, 148632, -3408);

	// 跑商狀態變量前綴
	private static final String PV_PREFIX = "RunMerchant_";

	// ==================== 配置區域結束 ====================

	private RunmerchantTelNPC()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("start_teleport"))
		{
			startTeleport(player);
		}

		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainHtml(player, npc);
		return null;
	}

	/**
	 * 显示主界面
	 */
	private void showMainHtml(Player player, Npc npc)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/RunmerchantTelNPC/910007.htm");
		player.sendPacket(html);
	}

	/**
	 * 開始傳送
	 */
	private void startTeleport(Player player)
	{
		// 檢查是否在跑商狀態
		if (!player.getVariables().getBoolean(PV_PREFIX + "active", false))
		{
			player.sendMessage("只有在跑商狀態下才能使用傳送服務！");
			return;
		}

		// 檢查玩家是否死亡
		if (player.isDead())
		{
			player.sendPacket(SystemMessageId.DEAD_CHARACTERS_CANNOT_USE_TELEPORTATION);
			return;
		}

		// 檢查玩家狀態
		if (player.isCastingNow() || player.isInCombat() || player.isImmobilized())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_RIGHT_NOW);
			return;
		}

		// 中止當前動作
		player.abortCast();
		player.stopMove(null);
		// ===== 設置跑商專用傳送標記 =====
		String allowedDestination = DESTINATION.getX() + "," + DESTINATION.getY() + "," + DESTINATION.getZ();
		player.getVariables().set("RunMerchant_AllowTeleport", allowedDestination);


		// 設置傳送目的地
		player.setTeleportLocation(DESTINATION);

		// 播放傳送技能動畫並執行傳送
		player.castTeleportSkill();

		// 發送提示消息
		player.sendMessage("正在傳送至目的地...");
	}

	public static void main(String[] args)
	{
		new RunmerchantTelNPC();
		System.out.println("跑商信使傳送NPC載入完畢！");
	}
}