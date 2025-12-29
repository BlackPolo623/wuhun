package handlers.voicedcommandhandlers;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class MingXiangStart implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
			{
					"WXStart",
			};

	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		if (command.equals("WXStart"))
		{
			boolean doing = activeChar.getVariables().getBoolean("MINGXIANG_DOING", false);
			if (doing)
			{
				// 停止冥想
				activeChar.getVariables().set("MINGXIANG_DOING", false);
				activeChar.setImmobilized(false);
				// 移除 setBlockActions - 這會阻止使用道具！
				activeChar.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.V_C_R_PURSUIT_AIRBORNE);
				activeChar.sendMessage("已停止冥想。");
				showMainHtml(activeChar);
			}
			else
			{
				// 開始冥想
				activeChar.getVariables().set("MINGXIANG_DOING", true);
				activeChar.setImmobilized(true);
				// 不要使用 setBlockActions！改用其他限制方式
				activeChar.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.V_C_R_PURSUIT_AIRBORNE);
				activeChar.sendMessage("開始冥想中...");
				showMainHtml(activeChar);
			}
		}
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

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}