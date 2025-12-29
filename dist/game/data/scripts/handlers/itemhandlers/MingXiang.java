package handlers.itemhandlers;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class MingXiang implements IItemHandler
{
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
			return false;
		}

		final Player player = playable.asPlayer();

		// 檢查玩家狀態
		if (player.isDead())
		{
			player.sendMessage("死亡狀態無法使用！");
			return false;
		}

		// 顯示介面（即使在冥想中也能打開）
		showMainHtml(player);

		return true;
	}

	private void showMainHtml(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		StringBuilder sb = new StringBuilder();

		boolean doing = player.getVariables().getBoolean("MINGXIANG_DOING", false);

		sb.append("<html><body><center>");
		sb.append("<br><font color=LEVEL>武魂冥想系統</font><br><br>");

		sb.append("累積獲得物品次數：<font color=00FF00>").append(player.getVariables().getInt("MINGXIANG_COUNT", 0)).append("</font><br><br>");

		if (!doing)
		{
			sb.append("<button action=\"bypass voice .WXStart\" value=\"開始冥想\" width=120 height=28 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		else
		{
			sb.append("<font color=00FF00>▶ 冥想中...</font><br><br>");
			sb.append("<button action=\"bypass voice .WXStart\" value=\"停止冥想\" width=120 height=28 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}

		sb.append("<br><br>");
		sb.append("<font color=808080 size=1>※ 冥想期間無法移動</font>");
		sb.append("</center></body></html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}
}