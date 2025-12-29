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
package org.l2jmobius.gameserver.network.clientpackets.autopeel;

import java.util.Collections;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.AutoPeelRequest;
import org.l2jmobius.gameserver.model.item.EtcItem;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.autopeel.ExResultItemAutoPeel;

/**
 * @author Mobius
 */
public class ExRequestItemAutoPeel extends ClientPacket
{
	private int _itemObjectId;
	private long _totalPeelCount;
	private long _remainingPeelCount;
	
	@Override
	protected void readImpl()
	{
		_itemObjectId = readInt();
		_totalPeelCount = readLong();
		_remainingPeelCount = readLong();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((_totalPeelCount < 1) || (_remainingPeelCount < 0))
		{
			return;
		}
		
		AutoPeelRequest request = player.getRequest(AutoPeelRequest.class);
		if (request == null)
		{
			final Item item = player.getInventory().getItemByObjectId(_itemObjectId);
			if ((item == null) || !item.isEtcItem() || (item.getEtcItem().getExtractableItems() == null) || item.getEtcItem().getExtractableItems().isEmpty())
			{
				return;
			}
			
			request = new AutoPeelRequest(player, item);
			player.addRequest(request);
		}
		else if (request.isProcessing())
		{
			return;
		}
		
		request.setProcessing(true);
		
		final Item item = request.getItem();
		if ((item.getObjectId() != _itemObjectId) || (item.getOwnerId() != player.getObjectId()))
		{
			player.removeRequest(request.getClass());
			return;
		}
		
		if (!item.getTemplate().checkCondition(player, item, true))
		{
			player.sendPacket(new ExResultItemAutoPeel(false, _totalPeelCount, _remainingPeelCount, Collections.emptyList()));
			player.removeRequest(request.getClass());
			return;
		}
		
		request.setTotalPeelCount(_totalPeelCount);
		request.setRemainingPeelCount(_remainingPeelCount);
		
		final EtcItem etcItem = (EtcItem) item.getTemplate();
		if ((etcItem.getExtractableItems() != null) && !etcItem.getExtractableItems().isEmpty())
		{
			final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
			if ((handler != null) && !handler.onItemUse(player, item, false))
			{
				request.setProcessing(false);
				player.sendPacket(new ExResultItemAutoPeel(false, _totalPeelCount, _remainingPeelCount, Collections.emptyList()));
			}
		}
	}
}
