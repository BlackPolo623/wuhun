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
package org.l2jmobius.gameserver.network.clientpackets.compound;

import org.l2jmobius.gameserver.data.xml.CombinationItemsData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.CompoundRequest;
import org.l2jmobius.gameserver.model.item.combination.CombinationItem;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.compound.ExEnchantRetryToPutItemFail;
import org.l2jmobius.gameserver.network.serverpackets.compound.ExEnchantRetryToPutItemOk;

/**
 * @author Mobius
 */
public class RequestNewEnchantRetryToPutItems extends ClientPacket
{
	private int _firstItemObjectId;
	private int _secondItemObjectId;
	
	@Override
	protected void readImpl()
	{
		_firstItemObjectId = readInt();
		_secondItemObjectId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_IN_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.sendPacket(ExEnchantRetryToPutItemFail.STATIC_PACKET);
			return;
		}
		
		if (player.isProcessingTransaction() || player.isProcessingRequest())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_SYSTEM_DURING_TRADING_PRIVATE_STORE_AND_WORKSHOP_SETUP);
			player.sendPacket(ExEnchantRetryToPutItemFail.STATIC_PACKET);
			return;
		}
		
		final CompoundRequest request = new CompoundRequest(player);
		if (!player.addRequest(request))
		{
			player.sendPacket(ExEnchantRetryToPutItemFail.STATIC_PACKET);
			return;
		}
		
		// Make sure player owns first item.
		request.setItemOne(_firstItemObjectId);
		final Item itemOne = request.getItemOne();
		if (itemOne == null)
		{
			player.sendPacket(ExEnchantRetryToPutItemFail.STATIC_PACKET);
			player.removeRequest(request.getClass());
			return;
		}
		
		// Make sure player owns second item.
		request.setItemTwo(_secondItemObjectId);
		final Item itemTwo = request.getItemTwo();
		if (itemTwo == null)
		{
			player.sendPacket(ExEnchantRetryToPutItemFail.STATIC_PACKET);
			player.removeRequest(request.getClass());
			return;
		}
		
		// Not implemented or not able to merge!
		final CombinationItem combinationItem = CombinationItemsData.getInstance().getItemsBySlots(itemOne.getId(), itemOne.getEnchantLevel(), itemTwo.getId(), itemTwo.getEnchantLevel());
		if (combinationItem == null)
		{
			player.sendPacket(ExEnchantRetryToPutItemFail.STATIC_PACKET);
			player.removeRequest(request.getClass());
			return;
		}
		
		player.sendPacket(ExEnchantRetryToPutItemOk.STATIC_PACKET);
	}
}
