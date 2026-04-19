package handlers.itemhandlers;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class SkillController implements IItemHandler
{
	// 強制轉換技能組（同組只能啟用一個）- 按等級分組
	private static final int[][] SKILL_GROUPS = {
		{101001, 101005}, // 初級組（物理+法師）
		{101002, 101006}, // 中級組（物理+法師）
		{101003, 101007}, // 高級組（物理+法師）
		{101004, 101008}  // 頂級組（物理+法師）
	};

	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
			return false;
		}

		Player player = playable.asPlayer();

		// 掠奪之地副本內禁止使用技能控制器
		final Instance instanceWorld = player.getInstanceWorld();
		if ((instanceWorld != null) && (instanceWorld.getTemplateId() == 1000))
		{
			player.sendMessage("掠奪之地內無法使用技能控制器！");
			return false;
		}

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
		sb.append("<font color=FF9900>※ 同組技能只能啟用一個</font><br><br>");
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

			// 檢查是否有同組其他技能已啟用
			boolean groupConflict = false;
			if (!hasSkill)
			{
				int groupIndex = getSkillGroup(skillId);
				if (groupIndex >= 0)
				{
					for (int otherSkillId : SKILL_GROUPS[groupIndex])
					{
						if (otherSkillId != skillId && player.getKnownSkill(otherSkillId) != null)
						{
							groupConflict = true;
							break;
						}
					}
				}
			}

			sb.append("<tr bgcolor=222222 height=30>");
			sb.append("<td width=160><font color=").append(hasSkill ? "00FF00" : (groupConflict ? "FF6666" : "FFFF00")).append(">")
					.append(skillName).append("</font></td>");
			sb.append("<td width=60 align=center><font color=").append(hasSkill ? "00FF66" : (groupConflict ? "FF3333" : "808080"))
					.append(">").append(hasSkill ? "已啟用" : (groupConflict ? "衝突" : "未啟用")).append("</font></td>");
			sb.append("<td width=60 align=center>");

			if (groupConflict)
			{
				sb.append("<font color=808080>---</font>");
			}
			else
			{
				sb.append("<button action=\"bypass voice .SK").append(hasSkill ? "off" : "on").append(" ").append(skillId)
						.append("\" value=\"").append(hasSkill ? "關閉" : "開啟")
						.append("\" width=50 height=20 back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			}

			sb.append("</td></tr>");
		}

		if (count == 0)
		{
			sb.append("<tr bgcolor=222222><td colspan=3 align=center height=80>");
			sb.append("<font color=FF3333>尚未解鎖任何技能</font>");
			sb.append("</td></tr>");
		}

		sb.append("</table>");
		sb.append("<br><font color=AAAAAA>提示：開啟新技能會自動關閉同組舊技能</font>");
		sb.append("</center></body></html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}

	/**
	 * 獲取技能所屬的組別索引
	 * @param skillId 技能ID
	 * @return 組別索引，如果不屬於任何組則返回-1
	 */
	private int getSkillGroup(int skillId)
	{
		for (int i = 0; i < SKILL_GROUPS.length; i++)
		{
			for (int id : SKILL_GROUPS[i])
			{
				if (id == skillId)
				{
					return i;
				}
			}
		}
		return -1;
	}
}