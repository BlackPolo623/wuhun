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
package org.l2jmobius.gameserver.network.serverpackets.limitshop;

import java.util.Collection;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.holders.LimitShopProductHolder;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExPurchaseLimitCraftItemList extends ServerPacket
{
	private final Player _player;
	private final int _page;
	private final int _totalPages;
	private final Collection<LimitShopProductHolder> _products;
	
	public ExPurchaseLimitCraftItemList(Player player, int page, int totalPages, Collection<LimitShopProductHolder> products)
	{
		_player = player;
		_page = page;
		_totalPages = totalPages;
		_products = products;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PURCHASE_LIMIT_CRAFT_ITEM_LIST.writeId(this, buffer);
		buffer.writeByte(_page);
		buffer.writeByte(_totalPages);
		buffer.writeInt(_products.size());
		for (LimitShopProductHolder product : _products)
		{
			buffer.writeInt(product.getId()); // nSlotNum
			buffer.writeInt(product.getProductionId()); // nItemClassID
			
			// Check limits.
			if (product.getAccountDailyLimit() > 0) // Sale period.
			{
				if (_player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + product.getProductionId(), 0) >= product.getAccountDailyLimit())
				{
					buffer.writeInt(0);
				}
				else
				{
					buffer.writeInt(product.getAccountDailyLimit() - _player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + product.getProductionId(), 0));
				}
			}
			else if (product.getAccountWeeklyLimit() > 0)
			{
				if (_player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + product.getProductionId(), 0) >= product.getAccountWeeklyLimit())
				{
					buffer.writeInt(0);
				}
				else
				{
					buffer.writeInt(product.getAccountWeeklyLimit() - _player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + product.getProductionId(), 0));
				}
			}
			else if (product.getAccountMonthlyLimit() > 0)
			{
				if (_player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + product.getProductionId(), 0) >= product.getAccountMonthlyLimit())
				{
					buffer.writeInt(0);
				}
				else
				{
					buffer.writeInt(product.getAccountMonthlyLimit() - _player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + product.getProductionId(), 0));
				}
			}
			else if (product.getAccountBuyLimit() > 0) // Count limit.
			{
				if (_player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_COUNT + product.getProductionId(), 0) >= product.getAccountBuyLimit())
				{
					buffer.writeInt(0);
				}
				else
				{
					buffer.writeInt(product.getAccountBuyLimit() - _player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_COUNT + product.getProductionId(), 0));
				}
			}
			else // No account limits.
			{
				buffer.writeInt(1);
			}
			
			buffer.writeInt(0); // nRemainSec
			buffer.writeInt(0); // nRemainServerItemAmount
			
			// probList
			final float chance1 = product.getChance();
			final float chance2value = product.getChance2();
			final float chance3value = product.getChance3();
			final float chance4value = product.getChance4();
			final float chance2 = chance2value == 100f ? 100f - chance1 : chance2value;
			final float sum2 = chance1 + chance2;
			final float chance3 = chance3value == 100f ? 100f - sum2 : chance3value;
			final float sum3 = sum2 + chance3;
			final float chance4 = chance4value == 100f ? 100f - sum3 : chance4value;
			final float chance5 = chance4value != 100f ? 100f - (sum3 + chance4) : 0f;
			buffer.writeInt((int) (chance1 * 100000));
			buffer.writeInt((int) (chance2 * 100000));
			buffer.writeInt((int) (chance3 * 100000));
			buffer.writeInt((int) (chance4 * 100000));
			buffer.writeInt((int) (chance5 * 100000));
			
			buffer.writeInt(0); // nStartTime
		}
	}
}
