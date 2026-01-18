/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.voicedcommandhandlers;

import java.util.function.Consumer;

import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerAction;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * 離線掛機命令處理器
 * @author Mobius
 */
public class OfflinePlay implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
			{
					"offlineplay",
					"op",
					"離線掛機"
			};

	private static final Consumer<OnPlayerLogin> ON_PLAYER_LOGIN = event ->
	{
		if (OfflinePlayConfig.ENABLE_OFFLINE_PLAY_COMMAND && !OfflinePlayConfig.OFFLINE_PLAY_LOGIN_MESSAGE.isEmpty())
		{
			event.getPlayer().sendPacket(new CreatureSay(null, ChatType.ANNOUNCEMENT, "離線掛機", OfflinePlayConfig.OFFLINE_PLAY_LOGIN_MESSAGE));
		}
	};

	public OfflinePlay()
	{
		Containers.Players().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_PLAYER_LOGIN, ON_PLAYER_LOGIN, this));
	}

	@Override
	public boolean onCommand(String command, Player player, String target)
	{
		if ((command.equals("offlineplay")||command.equals("op")||command.equals("離線掛機")) && OfflinePlayConfig.ENABLE_OFFLINE_PLAY_COMMAND)
		{
			// 檢查是否為VIP玩家
			if (OfflinePlayConfig.OFFLINE_PLAY_PREMIUM && !player.hasPremiumStatus())
			{
				player.sendPacket(new ExShowScreenMessage("此功能僅限VIP玩家使用。", 5000));
				player.sendMessage("此功能僅限VIP玩家使用。");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			// 檢查是否已啟用自動掛機
			if (!player.isAutoPlaying())
			{
				player.sendPacket(new ExShowScreenMessage("請先啟用自動掛機功能再離線。", 5000));
				player.sendMessage("請先啟用自動掛機功能再離線。");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			// 檢查是否在載具或和平區域
			if (player.isInVehicle() || player.isInsideZone(ZoneId.PEACE))
			{
				player.sendPacket(new ExShowScreenMessage("您無法在此位置登出。", 5000));
				player.sendPacket(SystemMessageId.YOU_MAY_NOT_LOG_OUT_FROM_THIS_LOCATION);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			// 檢查是否已註冊活動
			if (player.isRegisteredOnEvent())
			{
				player.sendPacket(new ExShowScreenMessage("已註冊活動時無法使用此命令。", 5000));
				player.sendMessage("已註冊活動時無法使用此命令。");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			// 檢查是否處於囚禁狀態
			if (player.isPrisoner())
			{
				player.sendPacket(new ExShowScreenMessage("處於囚禁狀態時無法使用此命令。", 5000));
				player.sendMessage("處於囚禁狀態時無法使用此命令。");
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}

			// 設定離線掛機動作並顯示確認對話框
			player.addAction(PlayerAction.OFFLINE_PLAY);
			player.sendPacket(new ConfirmDlg("您確定要離線並繼續自動掛機嗎？"));
		}

		return true;
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}