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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.AutoPotionsConfig;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.managers.PremiumManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;

/**
 * @author Mobius, Gigi, Custom
 */
public class AutoPotionTaskManager implements Runnable
{
	private static final Set<Player> PLAYERS = ConcurrentHashMap.newKeySet();
	private static boolean _working = false;

	// 各類型上次喝水時間，key=objectId
	private static final Map<Integer, Long> LAST_HP = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_MP = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_CP = new ConcurrentHashMap<>();

	// 「藥水用完」提示冷卻（60秒一次），避免每秒刷頻
	private static final Map<Integer, Long> LAST_WARN = new ConcurrentHashMap<>();
	private static final long WARN_INTERVAL = 60000;

	// 非會員固定設定
	private static final int FREE_INTERVAL = 5000;

	protected AutoPotionTaskManager()
	{
		ThreadPool.schedulePriorityTaskAtFixedRate(this, 0, 1000);
	}

	@Override
	public void run()
	{
		if (_working)
		{
			return;
		}
		_working = true;

		if (!PLAYERS.isEmpty())
		{
			final long now = System.currentTimeMillis();

			PLAYER: for (Player player : PLAYERS)
			{
				if ((player == null) || player.isAlikeDead() || (player.isOnlineInt() != 1) || (!AutoPotionsConfig.AUTO_POTIONS_IN_OLYMPIAD && player.isInOlympiadMode()))
				{
					remove(player);
					continue PLAYER;
				}

				final boolean isPremium = PremiumManager.getInstance().getPremiumExpiration(player.getAccountName()) > 0;
				final PlayerVariables vars = player.getVariables();

				// ── HP ────────────────────────────────────────────────────────
				if (vars.getBoolean(PlayerVariables.AP_HP_ENABLED, false) && AutoPotionsConfig.AUTO_HP_ENABLED)
				{
					final int hpPercent = isPremium ? vars.getInt(PlayerVariables.AP_HP_PERCENT, AutoPotionsConfig.AUTO_HP_PERCENTAGE) : AutoPotionsConfig.AUTO_HP_PERCENTAGE;
					final long hpInterval = isPremium ? vars.getInt(PlayerVariables.AP_HP_INTERVAL, FREE_INTERVAL) : FREE_INTERVAL;

					if ((now - LAST_HP.getOrDefault(player.getObjectId(), 0L)) >= hpInterval)
					{
						final boolean restoreHP = ((player.getStatus().getCurrentHp() / player.getMaxHp()) * 100) < hpPercent;
						if (restoreHP)
						{
							final String itemStr = vars.getString(PlayerVariables.AP_HP_ITEMS, "");
							boolean used = false;
							if (!itemStr.isEmpty())
							{
								for (String sid : itemStr.split(","))
								{
									sid = sid.trim();
									if (sid.isEmpty()) continue;
									try
									{
										final Item potion = player.getInventory().getItemByItemId(Integer.parseInt(sid));
										if ((potion != null) && (potion.getCount() > 0))
										{
											ItemHandler.getInstance().getHandler(potion.getEtcItem()).onItemUse(player, potion, false);
											LAST_HP.put(player.getObjectId(), now);
											used = true;
											break;
										}
									}
									catch (NumberFormatException ignored)
									{
									}
								}
							}
							if (!used)
							{
								sendWarnOnce(player, now, "[自動喝水] HP 藥水已用完，請補充！");
							}
						}
					}
				}

				// ── MP ────────────────────────────────────────────────────────
				if (vars.getBoolean(PlayerVariables.AP_MP_ENABLED, false) && AutoPotionsConfig.AUTO_MP_ENABLED)
				{
					final int mpPercent = isPremium ? vars.getInt(PlayerVariables.AP_MP_PERCENT, AutoPotionsConfig.AUTO_MP_PERCENTAGE) : AutoPotionsConfig.AUTO_MP_PERCENTAGE;
					final long mpInterval = isPremium ? vars.getInt(PlayerVariables.AP_MP_INTERVAL, FREE_INTERVAL) : FREE_INTERVAL;

					if ((now - LAST_MP.getOrDefault(player.getObjectId(), 0L)) >= mpInterval)
					{
						final boolean restoreMP = ((player.getStatus().getCurrentMp() / player.getMaxMp()) * 100) < mpPercent;
						if (restoreMP)
						{
							final String itemStr = vars.getString(PlayerVariables.AP_MP_ITEMS, "");
							boolean used = false;
							if (!itemStr.isEmpty())
							{
								for (String sid : itemStr.split(","))
								{
									sid = sid.trim();
									if (sid.isEmpty()) continue;
									try
									{
										final Item potion = player.getInventory().getItemByItemId(Integer.parseInt(sid));
										if ((potion != null) && (potion.getCount() > 0))
										{
											ItemHandler.getInstance().getHandler(potion.getEtcItem()).onItemUse(player, potion, false);
											LAST_MP.put(player.getObjectId(), now);
											used = true;
											break;
										}
									}
									catch (NumberFormatException ignored)
									{
									}
								}
							}
							if (!used)
							{
								sendWarnOnce(player, now, "[自動喝水] MP 藥水已用完，請補充！");
							}
						}
					}
				}

				// ── CP ────────────────────────────────────────────────────────
				if (vars.getBoolean(PlayerVariables.AP_CP_ENABLED, false) && AutoPotionsConfig.AUTO_CP_ENABLED)
				{
					final int cpPercent = isPremium ? vars.getInt(PlayerVariables.AP_CP_PERCENT, AutoPotionsConfig.AUTO_CP_PERCENTAGE) : AutoPotionsConfig.AUTO_CP_PERCENTAGE;
					final long cpInterval = isPremium ? vars.getInt(PlayerVariables.AP_CP_INTERVAL, FREE_INTERVAL) : FREE_INTERVAL;

					if ((now - LAST_CP.getOrDefault(player.getObjectId(), 0L)) >= cpInterval)
					{
						final boolean restoreCP = ((player.getStatus().getCurrentCp() / player.getMaxCp()) * 100) < cpPercent;
						if (restoreCP)
						{
							final String itemStr = vars.getString(PlayerVariables.AP_CP_ITEMS, "");
							boolean used = false;
							if (!itemStr.isEmpty())
							{
								for (String sid : itemStr.split(","))
								{
									sid = sid.trim();
									if (sid.isEmpty()) continue;
									try
									{
										final Item potion = player.getInventory().getItemByItemId(Integer.parseInt(sid));
										if ((potion != null) && (potion.getCount() > 0))
										{
											ItemHandler.getInstance().getHandler(potion.getEtcItem()).onItemUse(player, potion, false);
											LAST_CP.put(player.getObjectId(), now);
											used = true;
											break;
										}
									}
									catch (NumberFormatException ignored)
									{
									}
								}
							}
							if (!used)
							{
								sendWarnOnce(player, now, "[自動喝水] CP 藥水已用完，請補充！");
							}
						}
					}
				}
			}
		}

		_working = false;
	}

	public void add(Player player)
	{
		PLAYERS.add(player);
	}

	public void remove(Player player)
	{
		if (player != null)
		{
			PLAYERS.remove(player);
			LAST_HP.remove(player.getObjectId());
			LAST_MP.remove(player.getObjectId());
			LAST_CP.remove(player.getObjectId());
			LAST_WARN.remove(player.getObjectId());
		}
	}

	private void sendWarnOnce(Player player, long now, String msg)
	{
		final long lastWarn = LAST_WARN.getOrDefault(player.getObjectId(), 0L);
		if ((now - lastWarn) >= WARN_INTERVAL)
		{
			LAST_WARN.put(player.getObjectId(), now);
			player.sendMessage(msg);
		}
	}

	public boolean hasPlayer(Player player)
	{
		return PLAYERS.contains(player);
	}

	public static AutoPotionTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final AutoPotionTaskManager INSTANCE = new AutoPotionTaskManager();
	}
}
