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
package handlers.admincommandhandlers;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * 郵件管理 GM 指令
 * @author 黑普羅
 */
public class AdminMail implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_mail_clear",
		"admin_mail_info"
	};

	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();

		if (actualCommand.equalsIgnoreCase("admin_mail_clear"))
		{
			if (!st.hasMoreTokens())
			{
				activeChar.sendMessage("用法: //mail_clear <角色名稱>");
				return false;
			}

			final String targetName = st.nextToken();
			final int targetId = CharInfoTable.getInstance().getIdByName(targetName);

			if (targetId <= 0)
			{
				activeChar.sendMessage("找不到角色: " + targetName);
				return false;
			}

			// 收集要刪除的郵件 ID
			final List<Integer> toDelete = new ArrayList<>();
			for (Message msg : MailManager.getInstance().getMessages())
			{
				if ((msg.getSenderId() == targetId) || (msg.getReceiverId() == targetId))
				{
					toDelete.add(msg.getId());
				}
			}

			// 刪除郵件
			int count = 0;
			for (int msgId : toDelete)
			{
				MailManager.getInstance().deleteMessageInDb(msgId);
				count++;
			}

			activeChar.sendMessage("已刪除角色 " + targetName + " 的 " + count + " 封郵件");

			// 如果玩家在線，更新郵件計數
			final Player target = World.getInstance().getPlayer(targetId);
			if (target != null)
			{
				target.sendMessage("您的郵件已被 GM 清理");
			}

			return true;
		}
		else if (actualCommand.equalsIgnoreCase("admin_mail_info"))
		{
			if (!st.hasMoreTokens())
			{
				activeChar.sendMessage("用法: //mail_info <角色名稱>");
				return false;
			}

			final String targetName = st.nextToken();
			final int targetId = CharInfoTable.getInstance().getIdByName(targetName);

			if (targetId <= 0)
			{
				activeChar.sendMessage("找不到角色: " + targetName);
				return false;
			}

			final int inboxSize = MailManager.getInstance().getInboxSize(targetId);
			final int outboxSize = MailManager.getInstance().getOutboxSize(targetId);

			activeChar.sendMessage("========== 郵件資訊 ==========");
			activeChar.sendMessage("角色: " + targetName);
			activeChar.sendMessage("收件匣: " + inboxSize + " / 240");
			activeChar.sendMessage("寄件匣: " + outboxSize + " / 240");
			activeChar.sendMessage("============================");

			return true;
		}

		return false;
	}

	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
