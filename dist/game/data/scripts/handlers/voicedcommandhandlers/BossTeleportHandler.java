package handlers.voicedcommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.l2jmobius.commons.database.DatabaseFactory;
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
	 * 傳送玩家到指定 BOSS 位置
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

		// 獲取 BOSS 位置
		Location bossLocation = BossTeleportScroll.getBossLocation(bossId);
		if (bossLocation == null)
		{
			player.sendPacket(new ExShowScreenMessage("找不到該BOSS資訊", 3000));
			return;
		}

		// 檢查 BOSS 是否存活
		if (!isBossAlive(bossId))
		{
			player.sendPacket(new ExShowScreenMessage("BOSS已死亡，無法傳送", 3000));
			return;
		}

		// 扣除卷軸並傳送
		player.destroyItemByItemId(null, SCROLL_ITEM_ID, 1, player, true);
		player.teleToLocation(bossLocation);
		player.sendPacket(new ExShowScreenMessage("已傳送至BOSS領地", 3000));
	}

	/**
	 * 從資料庫檢查 BOSS 是否存活
	 * @param npcId BOSS ID
	 * @return true 如果已重生(存活), false 如果還在重生倒計時(死亡)
	 */
	private boolean isBossAlive(int npcId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement statement = con.prepareStatement("SELECT respawnTime FROM npc_respawns WHERE id = ?"))
		{
			statement.setInt(1, npcId);

			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					long respawnTime = rset.getLong("respawnTime");
					// 如果 respawnTime <= 當前時間,表示已經重生(存活)
					return respawnTime <= System.currentTimeMillis();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// 如果資料庫中沒有記錄,預設為存活
		return true;
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}