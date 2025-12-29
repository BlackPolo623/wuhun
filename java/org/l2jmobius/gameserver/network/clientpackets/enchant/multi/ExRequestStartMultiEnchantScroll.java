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
package org.l2jmobius.gameserver.network.clientpackets.enchant.multi;

import org.l2jmobius.gameserver.data.xml.EnchantItemData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.EnchantItemRequest;
import org.l2jmobius.gameserver.model.item.enchant.EnchantScroll;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.enchant.multi.ExResetSelectMultiEnchantScroll;

/**
 * @author Index
 */
public class ExRequestStartMultiEnchantScroll extends ClientPacket
{
	private int _scrollObjectId;
	
	@Override
	protected void readImpl()
	{
		_scrollObjectId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.getRequest(EnchantItemRequest.class) == null)
		{
			player.addRequest(new EnchantItemRequest(player, _scrollObjectId));
		}
		
		final EnchantItemRequest request = player.getRequest(EnchantItemRequest.class);
		
		final Item scroll = player.getInventory().getItemByObjectId(_scrollObjectId);
		final EnchantScroll scrollTemplate = EnchantItemData.getInstance().getEnchantScroll(scroll);
		if ((scrollTemplate == null) || scrollTemplate.isBlessed() || scrollTemplate.isBlessedDown() || scrollTemplate.isSafe() || scrollTemplate.isGiant())
		{
			player.sendPacket(new ExResetSelectMultiEnchantScroll(player, _scrollObjectId, 1));
			return;
		}
		
		request.setEnchantingScroll(_scrollObjectId);
		
		player.sendPacket(new ExResetSelectMultiEnchantScroll(player, _scrollObjectId, 0));
	}
}
