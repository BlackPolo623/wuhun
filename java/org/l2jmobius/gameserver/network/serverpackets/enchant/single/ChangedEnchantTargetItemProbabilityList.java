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
package org.l2jmobius.gameserver.network.serverpackets.enchant.single;

import static org.l2jmobius.gameserver.model.stats.Stat.ENCHANT_RATE;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.EnchantItemData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.EnchantItemRequest;
import org.l2jmobius.gameserver.model.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.model.item.enchant.EnchantScroll;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.CrystalType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Index
 */
public class ChangedEnchantTargetItemProbabilityList extends ServerPacket
{
	private final Player _player;
	private final boolean _isMulti;
	
	public ChangedEnchantTargetItemProbabilityList(Player player, Boolean isMulti)
	{
		_player = player;
		_isMulti = isMulti;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		final EnchantItemRequest request = _player.getRequest(EnchantItemRequest.class);
		if (request == null)
		{
			return;
		}
		
		if ((!_isMulti && (request.getEnchantingItem() == null)) || request.isProcessing() || (request.getEnchantingScroll() == null))
		{
			return;
		}
		
		int count = 1;
		if (_isMulti)
		{
			count = request.getMultiEnchantingItemsCount();
		}
		
		ServerPackets.EX_CHANGED_ENCHANT_TARGET_ITEM_PROB_LIST.writeId(this, buffer);
		buffer.writeInt(count);
		for (int i = 1; i <= count; i++)
		{
			// 100,00 % = 10000, because last 2 numbers going after float comma.
			double baseRate;
			double passiveRate;
			if (!_isMulti || (request.getMultiEnchantingItemsBySlot(i) != 0))
			{
				baseRate = getBaseRate(request, i);
				passiveRate = getPassiveRate(request, i);
			}
			else
			{
				baseRate = 0;
				passiveRate = 0;
			}
			
			double passiveBaseRate = 0;
			final double supportRate = getSupportRate(request);
			if (passiveRate != 0)
			{
				passiveBaseRate = (baseRate * passiveRate) / 10000;
			}
			
			double totalRate = baseRate + supportRate + passiveBaseRate;
			if (totalRate >= 10000)
			{
				totalRate = 10000;
			}
			
			if (!_isMulti)
			{
				buffer.writeInt(request.getEnchantingItem().getObjectId());
			}
			else
			{
				buffer.writeInt(request.getMultiEnchantingItemsBySlot(i));
			}
			
			buffer.writeInt((int) totalRate); // Total success.
			buffer.writeInt((int) baseRate); // Base success.
			buffer.writeInt((int) supportRate); // Support success.
			buffer.writeInt((int) passiveBaseRate); // Passive success (items, skills).
			buffer.writeInt(0); // VariationProbPermyriad (493)
		}
	}
	
	private int getBaseRate(EnchantItemRequest request, int iteration)
	{
		final EnchantScroll enchantScroll = EnchantItemData.getInstance().getEnchantScroll(request.getEnchantingScroll());
		if (enchantScroll == null)
		{
			return 0;
		}
		
		return (int) Math.min(100, enchantScroll.getChance(_player, _isMulti ? _player.getInventory().getItemByObjectId(request.getMultiEnchantingItemsBySlot(iteration)) : request.getEnchantingItem()) + enchantScroll.getBonusRate()) * 100;
	}
	
	private int getSupportRate(EnchantItemRequest request)
	{
		double supportRate = 0;
		if (!_isMulti && (request.getSupportItem() != null))
		{
			supportRate = EnchantItemData.getInstance().getSupportItem(request.getSupportItem()).getBonusRate();
			supportRate = supportRate * 100;
		}
		
		return (int) supportRate;
	}
	
	private int getPassiveRate(EnchantItemRequest request, int iteration)
	{
		double passiveRate = 0;
		final PlayerStat stat = _player.getStat();
		if (stat.getValue(ENCHANT_RATE) != 0)
		{
			if (!_isMulti)
			{
				final int crystalLevel = request.getEnchantingItem().getTemplate().getCrystalType().getLevel();
				if ((crystalLevel == CrystalType.NONE.getLevel()) || (crystalLevel == CrystalType.EVENT.getLevel()))
				{
					passiveRate = 0;
				}
				else
				{
					passiveRate = stat.getValue(ENCHANT_RATE);
					passiveRate = passiveRate * 100;
				}
			}
			else
			{
				final Item item = _player.getInventory().getItemByObjectId(request.getMultiEnchantingItemsBySlot(iteration));
				if (item == null)
				{
					return 0;
				}
				
				final int crystalLevel = item.getTemplate().getCrystalType().getLevel();
				if ((crystalLevel == CrystalType.NONE.getLevel()) || (crystalLevel == CrystalType.EVENT.getLevel()))
				{
					passiveRate = 0;
				}
				else
				{
					passiveRate = stat.getValue(ENCHANT_RATE);
					passiveRate = passiveRate * 100;
				}
			}
		}
		
		return (int) passiveRate;
	}
}
