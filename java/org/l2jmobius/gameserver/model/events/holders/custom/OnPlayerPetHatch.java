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
package org.l2jmobius.gameserver.model.events.holders.custom;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.IBaseEvent;

/**
 * 寵物孵化完成事件
 * @author Custom
 */
public class OnPlayerPetHatch implements IBaseEvent
{
	private final Player _player;
	private final int _petItemId;
	private final int _tier;
	private final boolean _upgraded;

	public OnPlayerPetHatch(Player player, int petItemId, int tier, boolean upgraded)
	{
		_player = player;
		_petItemId = petItemId;
		_tier = tier;
		_upgraded = upgraded;
	}

	public Player getPlayer()
	{
		return _player;
	}

	public int getPetItemId()
	{
		return _petItemId;
	}

	public int getTier()
	{
		return _tier;
	}

	public boolean isUpgraded()
	{
		return _upgraded;
	}

	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_PET_HATCH;
	}
}
