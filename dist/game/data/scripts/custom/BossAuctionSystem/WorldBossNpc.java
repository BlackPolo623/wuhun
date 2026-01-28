package custom.BossAuctionSystem;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 世界首領管理員 NPC
 * NPC ID: 900030
 * @author 黑普羅
 */
public class WorldBossNpc extends Script
{
	private static final int NPC_ID = 900030;

	public WorldBossNpc()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);

		// 初始化管理器
		new WorldBossManager();
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("main"))
		{
			showMainPage(player);
			return null;
		}
		else if (event.equals("teleport"))
		{
			Location loc = WorldBossManager.getInstance().getRespawnLocation();
			player.teleToLocation(loc);
			player.sendMessage("已傳送到世界首領重生點");
			return null;
		}
		else if (event.equals("info"))
		{
			showInfoPage(player);
			return null;
		}
		else if (event.equals("rewards"))
		{
			showRewardsPage(player);
			return null;
		}
		else if (event.startsWith("claim_"))
		{
			// 領取獎勵
			try
			{
				int rewardId = Integer.parseInt(event.substring(6));
				if (BossAuctionManager.getInstance().claimReward(player, rewardId))
				{
					// 領取成功，刷新頁面
					showRewardsPage(player);
				}
				else
				{
					// 領取失敗，返回獎勵頁面
					showRewardsPage(player);
				}
			}
			catch (Exception e)
			{
				player.sendMessage("領取失敗，請重試。");
				showRewardsPage(player);
			}
			return null;
		}

		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showMainPage(player);
		return null;
	}

	/**
	 * 顯示主頁面
	 */
	private void showMainPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/BossAuctionSystem/worldboss_main.htm");

		WorldBossManager manager = WorldBossManager.getInstance();

		// 首領狀態
		String bossStatus;
		if (manager.hasBoss())
		{
			Monster boss = manager.getCurrentBoss();
			double currentHp = boss.getStatus().getCurrentHp();
			double maxHp = boss.getMaxHp();
			int hpPercent = (int) ((currentHp * 100) / maxHp);

			bossStatus = "<font color=\"00FF00\">存活中</font><br>" +
				"<font color=\"FFFF00\">" + boss.getName() + "</font><br>" +
				"<font color=\"FF6B6B\">HP: " + hpPercent + "%</font>";
		}
		else
		{
			bossStatus = "<font color=\"808080\">尚未出現</font>";
		}

		html.replace("%boss_status%", bossStatus);

		// 下次重生時間
		long nextSpawn = manager.getNextSpawnTime();
		long now = System.currentTimeMillis();
		long diff = nextSpawn - now;

		String timeLeft;
		if (diff <= 0)
		{
			timeLeft = "<font color=\"00FF00\">即將出現</font>";
		}
		else
		{
			long days = diff / (24 * 60 * 60 * 1000);
			long hours = (diff % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
			long minutes = (diff % (60 * 60 * 1000)) / (60 * 1000);

			if (days > 0)
			{
				timeLeft = "<font color=\"FFFF00\">" + days + "天 " + hours + "小時 " + minutes + "分</font>";
			}
			else if (hours > 0)
			{
				timeLeft = "<font color=\"FFFF00\">" + hours + "小時 " + minutes + "分</font>";
			}
			else
			{
				timeLeft = "<font color=\"FFFF00\">" + minutes + "分鐘</font>";
			}
		}

		html.replace("%next_spawn%", timeLeft);

		player.sendPacket(html);
	}

	/**
	 * 顯示資訊頁面
	 */
	private void showInfoPage(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/BossAuctionSystem/worldboss_info.htm");

		// 獲取配置資訊
		String spawnDays = WorldBossConfig.getSpawnDays().toString();
		String spawnTime = WorldBossConfig.getSpawnTime();
		int lifetime = WorldBossConfig.getBossLifetime();

		html.replace("%spawn_days%", spawnDays.replace("[", "").replace("]", ""));
		html.replace("%spawn_time%", spawnTime);
		html.replace("%lifetime%", String.valueOf(lifetime));

		player.sendPacket(html);
	}

	/**
	 * 顯示獎勵領取頁面
	 */
	private void showRewardsPage(Player player)
	{
		java.util.List<BossAuctionDAO.PendingReward> rewards = BossAuctionManager.getInstance().getPlayerPendingRewards(player.getObjectId());

		StringBuilder rewardList = new StringBuilder();

		if (rewards.isEmpty())
		{
			rewardList.append("<br><center><font color=\"LEVEL\">目前沒有待領取的獎勵</font></center><br>");
		}
		else
		{
			rewardList.append("<br><table width=280><tr>");
			rewardList.append("<td width=80 align=center><font color=\"LEVEL\">BOSS名稱</font></td>");
			rewardList.append("<td width=80 align=center><font color=\"LEVEL\">類型</font></td>");
			rewardList.append("<td width=60 align=center><font color=\"LEVEL\">獎勵</font></td>");
			rewardList.append("<td width=60 align=center><font color=\"LEVEL\">操作</font></td>");
			rewardList.append("</tr>");

			for (BossAuctionDAO.PendingReward reward : rewards)
			{
				String rewardType = "BID_WIN".equals(reward.rewardType) ? "得標" : "分紅";
				String itemName = getItemName(reward.itemId);

				rewardList.append("<tr>");
				rewardList.append("<td width=80 align=center>").append(reward.bossName).append("</td>");
				rewardList.append("<td width=80 align=center><font color=\"LEVEL\">").append(rewardType).append("</font></td>");
				rewardList.append("<td width=60 align=center>").append(itemName).append(" x").append(reward.itemCount).append("</td>");
				rewardList.append("<td width=60 align=center><button value=\"領取\" action=\"bypass -h Quest WorldBossNpc claim_").append(reward.rewardId).append("\" width=50 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				rewardList.append("</tr>");
			}

			rewardList.append("</table>");
		}

		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/custom/BossAuctionSystem/worldboss_rewards.htm");
		html.replace("%reward_list%", rewardList.toString());
		html.replace("%reward_count%", String.valueOf(rewards.size()));

		player.sendPacket(html);
	}

	/**
	 * 獲取物品名稱
	 */
	private String getItemName(int itemId)
	{
		try
		{
			return org.l2jmobius.gameserver.data.xml.ItemData.getInstance().getTemplate(itemId).getName();
		}
		catch (Exception e)
		{
			return "物品ID:" + itemId;
		}
	}

	public static void main(String[] args)
	{
		new WorldBossNpc();
	}
}
