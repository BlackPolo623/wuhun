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
package handlers.dailymissionhandlers;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.DailyMissionStatus;
import org.l2jmobius.gameserver.model.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.model.actor.holders.player.DailyMissionPlayerEntry;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.custom.OnPlayerPetHatch;
import org.l2jmobius.gameserver.model.events.holders.custom.OnPlayerPetHatchStart;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.network.serverpackets.dailymission.ExOneDayReceiveRewardList;

/**
 * 寵物孵化每日任務處理器
 * 支援兩種計算方式：
 * 1. countOnStart=true: 開始孵化時計算（玩家必定在線，不區分最終品質）
 * 2. countOnStart=false: 孵化完成時計算（支援離線補發，可區分最終品質）
 * @author Custom
 */
public class PetHatchDailyMissionHandler extends AbstractDailyMissionHandler
{
	private static final Logger LOGGER = Logger.getLogger(PetHatchDailyMissionHandler.class.getName());

	private final int _amount;
	private final int _minTier;
	private final int _maxTier;
	private boolean _initialized = false;

	public PetHatchDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_amount = holder.getRequiredCompletions();
		_minTier = holder.getParams().getInt("minTier", 0);
		_maxTier = holder.getParams().getInt("maxTier", 4);
	}

	@Override
	public void init()
	{
		// 防止重複初始化
		if (_initialized)
		{
			return;
		}
		_initialized = true;

		// 在 init() 中讀取參數，此時子類別建構子已完成
		final boolean countOnStart = getHolder().getParams().getBoolean("countOnStart", true);

		if (countOnStart)
		{
			// 監聽開始孵化事件
			Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_PET_HATCH_START, (OnPlayerPetHatchStart event) -> onPetHatchStart(event), this));
		}
		else
		{
			// 監聽孵化完成事件
			Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_PET_HATCH, (OnPlayerPetHatch event) -> onPetHatch(event), this));
		}
	}

	@Override
	public boolean isAvailable(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), false);
		if (entry != null)
		{
			switch (entry.getStatus())
			{
				case NOT_AVAILABLE:
				{
					if (entry.getProgress() >= _amount)
					{
						entry.setStatus(DailyMissionStatus.AVAILABLE);
						storePlayerEntry(entry);
					}
					break;
				}
				case AVAILABLE:
				{
					return true;
				}
			}
		}

		return false;
	}

	private void onPetHatchStart(OnPlayerPetHatchStart event)
	{
		final Player player = event.getPlayer();
		final int eggTier = event.getEggTier();


		// 檢查品質範圍
		if ((eggTier < _minTier) || (eggTier > _maxTier))
		{
			return;
		}

		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), true);

		if (entry.getStatus() == DailyMissionStatus.NOT_AVAILABLE)
		{
			final int newProgress = entry.increaseProgress();

			if (newProgress >= _amount)
			{
				entry.setStatus(DailyMissionStatus.AVAILABLE);
			}

			storePlayerEntry(entry);

			// 發送封包更新客戶端顯示
			player.sendPacket(new ExOneDayReceiveRewardList(player, true));
		}
	}

	private void onPetHatch(OnPlayerPetHatch event)
	{
		final Player player = event.getPlayer();
		final int tier = event.getTier();

		// 檢查品質範圍
		if ((tier < _minTier) || (tier > _maxTier))
		{
			return;
		}

		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), true);
		if (entry.getStatus() == DailyMissionStatus.NOT_AVAILABLE)
		{
			if (entry.increaseProgress() >= _amount)
			{
				entry.setStatus(DailyMissionStatus.AVAILABLE);
			}

			storePlayerEntry(entry);

			// 發送封包更新客戶端顯示
			player.sendPacket(new ExOneDayReceiveRewardList(player, true));
		}
	}
}
