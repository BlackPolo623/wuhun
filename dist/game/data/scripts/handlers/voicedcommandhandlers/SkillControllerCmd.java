package handlers.voicedcommandhandlers;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SkillList;

import handlers.itemhandlers.SkillPermission;

public class SkillControllerCmd implements IVoicedCommandHandler
{
	private static final String[] COMMANDS = {"SKon", "SKoff"};

	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		if (params == null || params.isEmpty())
		{
			return false;
		}

		int skillId = Integer.parseInt(params);

		if (!player.getVariables().getBoolean("SkillPerm_" + skillId, false))
		{
			player.sendMessage("沒有該技能權限！");
			return false;
		}

		Object[] config = null;
		for (Object[] skill : SkillPermission.getAllSkills())
		{
			if ((int) skill[1] == skillId)
			{
				config = skill;
				break;
			}
		}

		if (config == null)
		{
			return false;
		}

		int skillLevel = (int) config[2];
		String skillName = (String) config[3];

		if (command.equals("SKon"))
		{
			Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
			if (skill != null)
			{
				player.addSkill(skill, true);

				// 檢查是否有從屬技能
				int slaveSkillId = SkillPermission.getSlaveSkill(skillId);
				if (slaveSkillId > 0)
				{
					// 找從屬技能等級
					for (Object[] cfg : SkillPermission.getAllSkills())
					{
						if ((int)cfg[1] == slaveSkillId)
						{
							Skill slaveSkill = SkillData.getInstance().getSkill(slaveSkillId, (int)cfg[2]);
							if (slaveSkill != null)
								player.addSkill(slaveSkill, true);
							break;
						}
					}
				}

				player.sendMessage("已開啟：" + skillName);
				player.sendSkillList();
				player.broadcastUserInfo();
			}
		}
		else if (command.equals("SKoff"))
		{
			// 先獲取技能對象
			Skill skillToRemove = player.getKnownSkill(skillId);
			if (skillToRemove != null)
			{
				player.removeSkill(skillToRemove, true, true);
			}

			// 同時移除從屬技能
			int slaveSkillId = SkillPermission.getSlaveSkill(skillId);
			if (slaveSkillId > 0)
			{
				Skill slaveSkillToRemove = player.getKnownSkill(slaveSkillId);
				if (slaveSkillToRemove != null)
				{
					player.removeSkill(slaveSkillToRemove, true, true);
				}
			}

			player.sendMessage("已關閉：" + skillName);
			player.sendSkillList();
			player.broadcastUserInfo();
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

	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}