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
package org.l2jmobius.gameserver.network.clientpackets.primeshop;

import java.util.Calendar;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.VipSystemConfig;
import org.l2jmobius.gameserver.data.xml.PrimeShopData;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.PrimeShopRequest;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.primeshop.PrimeShopGroup;
import org.l2jmobius.gameserver.model.primeshop.PrimeShopItem;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.enums.ExBrProductReplyType;
import org.l2jmobius.gameserver.network.serverpackets.primeshop.ExBRBuyProduct;
import org.l2jmobius.gameserver.network.serverpackets.primeshop.ExBRGamePoint;

/**
 * @author Mobius
 */
public class RequestBRBuyProduct extends ClientPacket
{
	private static final int HERO_COINS = 23805;
	
	private int _brId;
	private int _count;
	
	@Override
	protected void readImpl()
	{
		_brId = readInt();
		_count = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.hasItemRequest() || player.hasRequest(PrimeShopRequest.class))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER_STATE));
			return;
		}
		
		player.addRequest(new PrimeShopRequest(player));
		
		final PrimeShopGroup item = PrimeShopData.getInstance().getItem(_brId);
		if (validatePlayer(item, _count, player))
		{
			final int price = item.getPrice() * _count;
			if (price < 1)
			{
				player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.LACK_OF_POINT));
				player.removeRequest(PrimeShopRequest.class);
				return;
			}
			
			final int paymentId = validatePaymentId(item);
			if (paymentId < 0)
			{
				player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.LACK_OF_POINT));
				player.removeRequest(PrimeShopRequest.class);
				return;
			}
			else if (paymentId > 0)
			{
				if (!player.destroyItemByItemId(ItemProcessType.FEE, paymentId, price, player, true))
				{
					player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.LACK_OF_POINT));
					player.removeRequest(PrimeShopRequest.class);
					return;
				}
			}
			else if (paymentId == 0)
			{
				if (player.getPrimePoints() < price)
				{
					player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.LACK_OF_POINT));
					player.removeRequest(PrimeShopRequest.class);
					return;
				}
				
				player.setPrimePoints(player.getPrimePoints() - price);
				if (VipSystemConfig.VIP_SYSTEM_PRIME_AFFECT)
				{
					player.updateVipPoints(price);
				}
			}
			
			for (PrimeShopItem subItem : item.getItems())
			{
				player.addItem(ItemProcessType.BUY, subItem.getId(), subItem.getCount() * _count, player, true);
			}
			
			if (item.isVipGift())
			{
				player.getAccountVariables().set(AccountVariables.VIP_ITEM_BOUGHT, System.currentTimeMillis());
			}
			
			// Update account variables.
			if (item.getAccountDailyLimit() > 0)
			{
				player.getAccountVariables().set(AccountVariables.PRIME_SHOP_PRODUCT_DAILY_COUNT + item.getBrId(), player.getAccountVariables().getInt(AccountVariables.PRIME_SHOP_PRODUCT_DAILY_COUNT + item.getBrId(), 0) + (item.getCount() * _count));
			}
			else if (item.getAccountBuyLimit() > 0)
			{
				player.getAccountVariables().set(AccountVariables.PRIME_SHOP_PRODUCT_COUNT + item.getBrId(), player.getAccountVariables().getInt(AccountVariables.PRIME_SHOP_PRODUCT_COUNT + item.getBrId(), 0) + (item.getCount() * _count));
			}
			
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.SUCCESS));
			player.sendPacket(new ExBRGamePoint(player));
		}
		
		ThreadPool.schedule(() -> player.removeRequest(PrimeShopRequest.class), 1000);
	}
	
	/**
	 * @param item
	 * @param count
	 * @param player
	 * @return
	 */
	private static boolean validatePlayer(PrimeShopGroup item, int count, Player player)
	{
		final long currentTime = System.currentTimeMillis() / 1000;
		if (item == null)
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_PRODUCT));
			PunishmentManager.handleIllegalPlayerAction(player, player + " tried to buy invalid brId from Prime", GeneralConfig.DEFAULT_PUNISH);
			return false;
		}
		else if ((count < 1) || (count > 99))
		{
			PunishmentManager.handleIllegalPlayerAction(player, player + " tried to buy invalid itemcount [" + count + "] from Prime", GeneralConfig.DEFAULT_PUNISH);
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER_STATE));
			return false;
		}
		else if ((item.getMinLevel() > 0) && (item.getMinLevel() > player.getLevel()))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER));
			return false;
		}
		else if ((item.getMaxLevel() > 0) && (item.getMaxLevel() < player.getLevel()))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER));
			return false;
		}
		else if ((item.getMinBirthday() > 0) && (item.getMinBirthday() > player.getBirthdays()))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER_STATE));
			return false;
		}
		else if ((item.getMaxBirthday() > 0) && (item.getMaxBirthday() < player.getBirthdays()))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVALID_USER_STATE));
			return false;
		}
		else if ((Calendar.getInstance().get(Calendar.DAY_OF_WEEK) & item.getDaysOfWeek()) == 0)
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.NOT_DAY_OF_WEEK));
			return false;
		}
		else if ((item.getStartSale() > 1) && (item.getStartSale() > currentTime))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.BEFORE_SALE_DATE));
			return false;
		}
		else if ((item.getEndSale() > 1) && (item.getEndSale() < currentTime))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.AFTER_SALE_DATE));
			return false;
		}
		else if ((item.getAccountDailyLimit() > 0) && ((count + player.getAccountVariables().getInt(AccountVariables.PRIME_SHOP_PRODUCT_DAILY_COUNT + item.getBrId(), 0)) > item.getAccountDailyLimit()))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.SOLD_OUT));
			return false;
		}
		else if ((item.getAccountBuyLimit() > 0) && ((count + player.getAccountVariables().getInt(AccountVariables.PRIME_SHOP_PRODUCT_COUNT + item.getBrId(), 0)) > item.getAccountBuyLimit()))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.SOLD_OUT));
			return false;
		}
		
		if ((item.getVipTier() > player.getVipTier()) || (item.isVipGift() && !canReceiveGift(player, item)))
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.SOLD_OUT));
			return false;
		}
		
		final int weight = item.getWeight() * count;
		final long slots = item.getCount() * count;
		if (player.getInventory().validateWeight(weight))
		{
			if (item.getCount() == 1)
			{
				if (!player.getInventory().validateCapacity(slots))
				{
					player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVENTORY_OVERFLOW));
					return false;
				}
			}
			else if (!player.getInventory().validateCapacity(count))
			{
				player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVENTORY_OVERFLOW));
				return false;
			}
		}
		else
		{
			player.sendPacket(new ExBRBuyProduct(ExBrProductReplyType.INVENTORY_OVERFLOW));
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check if player can receive Gift from L2 Store
	 * @param player player in question
	 * @param item requested item.
	 * @return true if player can receive gift item.
	 */
	private static boolean canReceiveGift(Player player, PrimeShopGroup item)
	{
		if (!VipSystemConfig.VIP_SYSTEM_ENABLED)
		{
			return false;
		}
		
		if (player.getVipTier() <= 0)
		{
			return false;
		}
		else if (item.getVipTier() != player.getVipTier())
		{
			player.sendMessage("This item is not for your vip tier!");
			return false;
		}
		else
		{
			return player.getAccountVariables().getLong(AccountVariables.VIP_ITEM_BOUGHT, 0) <= 0;
		}
	}
	
	private static int validatePaymentId(PrimeShopGroup item)
	{
		switch (item.getPaymentType())
		{
			case 0: // Prime points
			{
				return 0;
			}
			case 1: // Adenas
			{
				return Inventory.ADENA_ID;
			}
			case 2: // Hero coins
			{
				return HERO_COINS;
			}
		}
		
		return -1;
	}
}
