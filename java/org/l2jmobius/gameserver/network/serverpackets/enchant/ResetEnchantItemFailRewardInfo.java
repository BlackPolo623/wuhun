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
package org.l2jmobius.gameserver.network.serverpackets.enchant;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.EnchantItemData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.ChallengePoint;
import org.l2jmobius.gameserver.model.actor.request.EnchantItemRequest;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enchant.EnchantScroll;
import org.l2jmobius.gameserver.model.item.enchant.EnchantSupportItem;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Index
 */
public class ResetEnchantItemFailRewardInfo extends ServerPacket
{
	private final Item _enchantItem;
	private int _challengeGroup;
	private int _challengePoint;
	private ItemHolder _result;
	private Item _addedItem;
	
	public ResetEnchantItemFailRewardInfo(Player player)
	{
		final EnchantItemRequest request = player.getRequest(EnchantItemRequest.class);
		if ((request == null) || (request.getEnchantingItem() == null) || request.isProcessing() || (request.getEnchantingScroll() == null))
		{
			_enchantItem = null;
			return;
		}
		
		final EnchantItemData enchantItemData = EnchantItemData.getInstance();
		final EnchantScroll enchantScroll = enchantItemData.getEnchantScroll(request.getEnchantingScroll());
		_enchantItem = request.getEnchantingItem();
		_addedItem = new Item(_enchantItem.getId());
		_addedItem.setOwnerId(player.getObjectId());
		_addedItem.setEnchantLevel(_enchantItem.getEnchantLevel());
		
		_result = null;
		EnchantSupportItem enchantSupportItem = null;
		if (request.getSupportItem() != null)
		{
			enchantSupportItem = enchantItemData.getSupportItem(request.getSupportItem());
		}
		
		if (enchantScroll.isBlessed() || ((request.getSupportItem() != null) && (enchantSupportItem != null) && enchantSupportItem.isBlessed()))
		{
			_addedItem.setEnchantLevel(0);
		}
		else if (enchantScroll.isBlessedDown() || enchantScroll.isCursed() /* || ((request.getSupportItem() != null) && (enchantSupportItem != null) && enchantSupportItem.isDown()) */)
		{
			_addedItem.setEnchantLevel(_enchantItem.getEnchantLevel() - 1);
		}
		else if (enchantScroll.isSafe())
		{
			_addedItem.setEnchantLevel(_enchantItem.getEnchantLevel());
		}
		else
		{
			_addedItem = null;
			
			final ItemTemplate template = _enchantItem.getTemplate();
			if (template.isCrystallizable())
			{
				_result = new ItemHolder(template.getCrystalItemId(), Math.max(0, _enchantItem.getCrystalCount() - ((template.getCrystalCount() + 1) / 2)));
			}
		}
		
		final ChallengePoint challenge = player.getChallengeInfo();
		_challengeGroup = challenge.getNowGroup();
		_challengePoint = challenge.getNowPoint();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		if (_enchantItem == null)
		{
			return;
		}
		
		ServerPackets.EX_RES_ENCHANT_ITEM_FAIL_REWARD_INFO.writeId(this, buffer);
		
		buffer.writeInt(_enchantItem.getObjectId());
		
		buffer.writeInt(_challengeGroup);
		buffer.writeInt(_challengePoint);
		
		if (_result != null)
		{
			buffer.writeInt(1); // Loop count.
			buffer.writeInt(_result.getId());
			buffer.writeInt((int) _result.getCount());
		}
		else if (_addedItem != null)
		{
			buffer.writeInt(1); // Loop count.
			buffer.writeInt(_enchantItem.getId());
			buffer.writeInt(1);
		}
		else
		{
			buffer.writeInt(0); // Loop count.
		}
	}
}
