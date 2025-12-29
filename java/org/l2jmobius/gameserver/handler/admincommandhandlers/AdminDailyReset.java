/*
 * Copyright (c) 2013 L2jMobius
 */
package org.l2jmobius.gameserver.handler.admincommandhandlers;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.DailyResetManager;
import org.l2jmobius.gameserver.model.actor.Player;

/**
 * @author 黑普羅
 * Admin command to manually trigger daily reset
 * 手動觸發每日重置的管理員命令
 */
public class AdminDailyReset implements IAdminCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(AdminDailyReset.class.getName());

	private static final String[] ADMIN_COMMANDS =
			{
					"admin_dailyreset",
					"admin_resetdaily"
			};

	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		// ========== 第一層保護：GM 權限檢查 ==========
		if (!activeChar.isGM())
		{
			activeChar.sendMessage("你沒有權限使用此命令！");
			LOGGER.warning("非GM玩家 " + activeChar.getName() + " 嘗試使用每日重置命令！");
			return false;
		}

		// ========== 第二層保護：高級GM權限檢查（可選） ==========
		// 如果想限制只有最高級別 GM 才能用，取消下面註釋
		/*
		if (activeChar.getAccessLevel().getLevel() < 10) // 10 = 最高級別 GM
		{
			activeChar.sendMessage("此命令需要最高級別GM權限！");
			LOGGER.warning("低級別GM " + activeChar.getName() + " (等級: " + activeChar.getAccessLevel().getLevel() + ") 嘗試使用每日重置命令！");
			return false;
		}
		*/

		if (command.startsWith("admin_dailyreset") || command.startsWith("admin_resetdaily"))
		{
			activeChar.sendMessage("====================================");
			activeChar.sendMessage("手動觸發每日重置...");
			activeChar.sendMessage("警告：此操作將重置所有玩家的每日數據！");
			activeChar.sendMessage("====================================");

			// 記錄操作日誌
			LOGGER.info("GM " + activeChar.getName() + " 手動觸發每日重置...");

			try
			{
				// 直接調用 public 方法（需要先將 DailyResetManager 的 onReset 改為 public）
				DailyResetManager.getInstance().onReset();

				activeChar.sendMessage("====================================");
				activeChar.sendMessage("每日重置執行完成！");
				activeChar.sendMessage("====================================");

				LOGGER.info("GM " + activeChar.getName() + " 成功執行每日重置。");
			}
			catch (Exception e)
			{
				activeChar.sendMessage("重置失敗：" + e.getMessage());
				activeChar.sendMessage("請查看伺服器控制台以獲取詳細錯誤信息。");

				LOGGER.log(Level.SEVERE, "GM " + activeChar.getName() + " 執行每日重置時發生錯誤！", e);
			}
		}

		return true;
	}

	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}