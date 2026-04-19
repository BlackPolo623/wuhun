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
package org.l2jmobius.gameserver.listeners;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.handler.MorphScrollHandler;

import org.l2jmobius.gameserver.managers.MorphManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.listeners.AnnotationEventListener;

/**
 * 變身系統事件監聽器 MorphListener
 *
 * 負責：
 *   1. 玩家登入：從 DB 加載變身激活狀態，並應用所有屬性加成
 *   2. 玩家登出：清除內存緩存（DB 已在激活時實時寫入）
 *
 * 註冊方式：在服務器啟動時調用 MorphListener.init()
 * （例如在 GameServer.java 的初始化序列中添加）
 *
 * @author Custom
 */
public class MorphListener
{
	private static final Logger LOGGER = Logger.getLogger(MorphListener.class.getName());

	/**
	 * 初始化並註冊變身事件監聽器。
	 * 在服務器啟動時調用一次。
	 */
	public static void init()
	{
		try
		{
			final MorphListener instance = new MorphListener();

			// ON_PLAYER_LOGIN
			final Method loginMethod = MorphListener.class.getMethod("onPlayerLogin", OnPlayerLogin.class);
			Containers.Global().addListener(new AnnotationEventListener(
				Containers.Global(), EventType.ON_PLAYER_LOGIN, loginMethod, instance, 0));

			// ON_PLAYER_LOGOUT
			final Method logoutMethod = MorphListener.class.getMethod("onPlayerLogout", OnPlayerLogout.class);
			Containers.Global().addListener(new AnnotationEventListener(
				Containers.Global(), EventType.ON_PLAYER_LOGOUT, logoutMethod, instance, 0));

			// 初始化 BypassHandler 註冊（MorphScrollHandler 構造器內自動註冊）
			MorphScrollHandler.getInstance();

			LOGGER.info("MorphListener: Listener initialized successfully.");
		}
		catch (Exception e)
		{
			LOGGER.warning("MorphListener: Failed to initialize: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// ── 事件處理 ──────────────────────────────────────────────────────────

	/**
	 * 玩家登入處理：加載變身數據 → 應用屬性。
	 */
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}

		// 從 DB 加載變身收藏記錄到內存緩存（屬性/外觀僅在玩家主動通過捲軸應用變身後生效）
		MorphManager.getInstance().loadPlayer(player);

		LOGGER.fine("MorphListener: Loaded morph collection for player " + player.getName());
	}

	/**
	 * 玩家登出處理：清除內存緩存。
	 */
	public void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}

		// 清除內存緩存（DB 數據已在激活/升級時實時寫入，無需此處再存）
		MorphManager.getInstance().unloadPlayer(player.getObjectId());
	}
}
