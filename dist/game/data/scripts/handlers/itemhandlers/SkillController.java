package handlers.itemhandlers;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class SkillController implements IItemHandler
{
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
			return false;
		}

		Player player = playable.asPlayer();

		if (player.isDead() || player.isInCombat() || player.isCastingNow() || player.isAttackingNow())
		{
			player.sendMessage("此狀態無法使用！");
			return false;
		}

		showHtml(player);
		return true;
	}

	private void showHtml(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		StringBuilder sb = new StringBuilder();

		sb.append("<html><body><center>");
		sb.append("<br><font color=LEVEL>技能控制器</font><br><br>");
		sb.append("<table width=280 bgcolor=111111 border=0 cellspacing=1 cellpadding=2>");
		sb.append("<tr bgcolor=333333 height=25>");
		sb.append("<td width=160 align=center><font color=FFCC33>技能名稱</font></td>");
		sb.append("<td width=60 align=center><font color=FFCC33>狀態</font></td>");
		sb.append("<td width=60 align=center><font color=FFCC33>操作</font></td>");
		sb.append("</tr>");

		Object[][] skills = SkillPermission.getAllSkills();
		int count = 0;

		for (Object[] skill : skills)
		{
			int skillId = (int) skill[1];
			String skillName = (String) skill[3];

			// 跳過從屬技能
			if (SkillPermission.isSlaveSkill(skillId))
				continue;

			if (!player.getVariables().getBoolean("SkillPerm_" + skillId, false))
			{
				continue;
			}

			count++;
			boolean hasSkill = player.getKnownSkill(skillId) != null;

			sb.append("<tr bgcolor=222222 height=30>");
			sb.append("<td width=160><font color=").append(hasSkill ? "00FF00" : "FFFF00").append(">")
					.append(skillName).append("</font></td>");
			sb.append("<td width=60 align=center><font color=").append(hasSkill ? "00FF66" : "808080")
					.append(">").append(hasSkill ? "已啟用" : "未啟用").append("</font></td>");
			sb.append("<td width=60 align=center>");
			sb.append("<button action=\"bypass voice .SK").append(hasSkill ? "off" : "on").append(" ").append(skillId)
					.append("\" value=\"").append(hasSkill ? "關閉" : "開啟")
					.append("\" width=50 height=20 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			sb.append("</td></tr>");
		}

		if (count == 0)
		{
			sb.append("<tr bgcolor=222222><td colspan=3 align=center height=80>");
			sb.append("<font color=FF3333>尚未解鎖任何技能</font>");
			sb.append("</td></tr>");
		}

		sb.append("</table></center></body></html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}
}