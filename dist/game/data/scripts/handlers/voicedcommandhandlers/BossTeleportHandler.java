package handlers.voicedcommandhandlers;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

import handlers.itemhandlers.BossTeleportScroll;

public class BossTeleportHandler implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"bosspage",
		"bosstele"
	};

	private static final int SCROLL_ITEM_ID = 109000; // BOSS傳送卷軸道具ID

	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		if (command.equals("bosspage"))
		{
			try
			{
				int page = Integer.parseInt(params);
				BossTeleportScroll.showBossList(player, page);
			}
			catch (NumberFormatException e)
			{
				BossTeleportScroll.showBossList(player, 1);
			}
			return true;
		}
		else if (command.equals("bosstele"))
		{
			try
			{
				int bossId = Integer.parseInt(params);
				teleportToBoss(player, bossId);
			}
			catch (NumberFormatException e)
			{
				player.sendMessage("無效的BOSS ID");
			}
			return true;
		}

		return false;
	}

	/**
	 * [自定義修改] 傳送玩家到指定 BOSS 位置
	 * 座標和存活狀態全部從 npc_respawns 資料庫讀取
	 * 不再依賴硬編碼座標，基地副本內的同ID BOSS不會影響判斷
	 * @param player 玩家
	 * @param bossId BOSS ID
	 */
	private void teleportToBoss(Player player, int bossId)
	{
		// 檢查是否擁有傳送卷軸
		if (player.getInventory().getInventoryItemCount(SCROLL_ITEM_ID, -1) < 1)
		{
			player.sendPacket(new ExShowScreenMessage("您沒有BOSS傳送卷軸", 3000));
			return;
		}

		// [自定義修改] 從資料庫讀取 BOSS 座標（不論存活或死亡都可傳送）
		Location bossLocation = BossTeleportScroll.getBossLocation(bossId);
		if (bossLocation == null)
		{
			player.sendPacket(new ExShowScreenMessage("找不到該BOSS資訊", 3000));
			return;
		}

		// 扣除卷軸並傳送
		player.destroyItemByItemId(null, SCROLL_ITEM_ID, 1, player, true);
		player.teleToLocation(bossLocation);
		player.sendPacket(new ExShowScreenMessage("已傳送至BOSS領地", 3000));
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
