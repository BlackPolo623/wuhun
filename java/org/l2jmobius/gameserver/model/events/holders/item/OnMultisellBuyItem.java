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
package org.l2jmobius.gameserver.model.events.holders.item;

import java.util.List;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;

public class OnMultisellBuyItem implements IBaseEvent
{
	private final Player _player;
	private final long _multisellId;
	private final long _amount;
	private final List<ItemChanceHolder> _resourceItems;
	private final List<ItemChanceHolder> _boughtItems;
	
	public OnMultisellBuyItem(Player player, long multisellId, long amount, List<ItemChanceHolder> resourceItems, List<ItemChanceHolder> boughtItems)
	{
		_player = player;
		_multisellId = multisellId;
		_amount = amount;
		_resourceItems = resourceItems;
		_boughtItems = boughtItems;
	}
	
	public Player getPlayer()
	{
		return _player;
	}
	
	public long getMultisellId()
	{
		return _multisellId;
	}
	
	public long getAmount()
	{
		return _amount;
	}
	
	public List<ItemChanceHolder> getResourceItems()
	{
		return _resourceItems;
	}
	
	public List<ItemChanceHolder> getBoughtItems()
	{
		return _boughtItems;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_MULTISELL_BUY_ITEM;
	}
}
