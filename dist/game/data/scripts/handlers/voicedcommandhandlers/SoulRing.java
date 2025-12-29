/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.voicedcommandhandlers;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class trades Gold Bars for Adena and vice versa.
 * @author Ahmed
 */
public class SoulRing implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"",
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		if (command.equals("WXStart"))
		{
			boolean doing = activeChar.getVariables().getBoolean("MINGXIANG_DOING", false);
			if (doing)
			{
				activeChar.getVariables().set("MINGXIANG_DOING", false);
				activeChar.setImmobilized(false);
				activeChar.setBlockActions(false);
				activeChar.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.V_C_R_PURSUIT_AIRBORNE);
				showMainHtml(activeChar);
			}
			else
			{
				activeChar.getVariables().set("MINGXIANG_DOING", true);
				activeChar.setImmobilized(true);
				activeChar.setBlockActions(true);
				activeChar.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.V_C_R_PURSUIT_AIRBORNE);
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
			sb.append("<button action=\"bypass voice .WXStart \" value=\"開始冥想\" width=120 height=28 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		else
		{
			sb.append("<button action=\"bypass voice .WXStart\" value=\"停止冥想\" width=120 height=28 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br>");
		}
		
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
