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

import java.util.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.l2jmobius.gameserver.data.xml.LimitShopData;
import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.holders.LimitShopProductHolder;
import org.l2jmobius.gameserver.data.xml.LimitShopData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExPurchaseLimitShopItemListNew extends ServerPacket
{
	private final Player _player;
	private final int _shopType; // 3 Lcoin Store, 4 Special Craft, 100 Clan Shop
	private final int _page;
	private final int _totalPages;
	private final Collection<LimitShopProductHolder> _products;

	public ExPurchaseLimitShopItemListNew(Player player, int shopType, int page, int totalPages, Collection<LimitShopProductHolder> products)
	{
		_player = player;
		_shopType = shopType;
		_page = page;
		_totalPages = totalPages;
		_products = products;
	}

	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PURCHASE_LIMIT_SHOP_ITEM_LIST_NEW.writeId(this, buffer);
		buffer.writeByte(_shopType);
		buffer.writeByte(_page);
		buffer.writeByte(_totalPages);

		// ========== 過濾接力商品，只發送當前階段 ==========
		final List<LimitShopProductHolder> filteredProducts = new ArrayList<>();
		final Set<Integer> processedRelayGroups = new HashSet<>();

		for (LimitShopProductHolder product : _products)
		{
			if (product.isRelay())
			{
				// 接力商品處理
				final int relayGroup = product.getRelayGroup();

				// 檢查是否已處理過這個接力組
				if (processedRelayGroups.contains(relayGroup))
				{
					continue; // 跳過，已經添加過這個組的當前階段
				}

				// 獲取玩家當前可購買的階段（或最後一個階段）
				final LimitShopProductHolder currentStage = LimitShopData.getInstance().getCurrentStageProduct(_player, relayGroup);
				if (currentStage != null)
				{
					filteredProducts.add(currentStage);
					processedRelayGroups.add(relayGroup);
				}
			}
			else
			{
				// 普通商品，直接添加
				filteredProducts.add(product);
			}
		}

		// 寫入過濾後的商品數量
		buffer.writeInt(filteredProducts.size());

		for (LimitShopProductHolder product : filteredProducts)
		{
			buffer.writeInt(product.getId());
			buffer.writeInt(product.getProductionId());
			buffer.writeInt(product.getIngredientIds()[0]);
			buffer.writeInt(product.getIngredientIds()[1]);
			buffer.writeInt(product.getIngredientIds()[2]);
			buffer.writeInt(product.getIngredientIds()[3]);
			buffer.writeInt(product.getIngredientIds()[4]);
			buffer.writeLong(product.getIngredientQuantities()[0]);
			buffer.writeLong(product.getIngredientQuantities()[1]);
			buffer.writeLong(product.getIngredientQuantities()[2]);
			buffer.writeLong(product.getIngredientQuantities()[3]);
			buffer.writeLong(product.getIngredientQuantities()[4]);
			buffer.writeShort(product.getIngredientEnchants()[0]);
			buffer.writeShort(product.getIngredientEnchants()[1]);
			buffer.writeShort(product.getIngredientEnchants()[2]);
			buffer.writeShort(product.getIngredientEnchants()[3]);
			buffer.writeShort(product.getIngredientEnchants()[4]);

			// ========== 剩餘購買次數檢查 ==========
			int remainingCount = 1; // 預設可購買

			// 接力商品的特殊處理
			if (product.isRelay())
			{
				// 檢查此階段是否已購買
				if (LimitShopData.getInstance().isRelayProductPurchased(_player, product))
				{
					remainingCount = 0; // 此階段已購買
				}
				// 不檢查 accountBuyLimit，因為接力商品每個階段都可以買一次
			}
			else
			{
				// 普通商品的限購檢查
				if (product.getAccountDailyLimit() > 0)
				{
					final int dailyCount = _player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_DAILY_COUNT + product.getProductionId(), 0);
					remainingCount = Math.max(0, product.getAccountDailyLimit() - dailyCount);
				}
				else if (product.getAccountWeeklyLimit() > 0)
				{
					final int weeklyCount = _player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_WEEKLY_COUNT + product.getProductionId(), 0);
					remainingCount = Math.max(0, product.getAccountWeeklyLimit() - weeklyCount);
				}
				else if (product.getAccountMonthlyLimit() > 0)
				{
					final int monthlyCount = _player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_MONTHLY_COUNT + product.getProductionId(), 0);
					remainingCount = Math.max(0, product.getAccountMonthlyLimit() - monthlyCount);
				}
				else if (product.getAccountBuyLimit() > 0)
				{
					final int buyCount = _player.getAccountVariables().getInt(AccountVariables.LCOIN_SHOP_PRODUCT_COUNT + product.getProductionId(), 0);
					remainingCount = Math.max(0, product.getAccountBuyLimit() - buyCount);
				}
			}

			buffer.writeInt(remainingCount);
			buffer.writeInt(0); // nRemainSec
			buffer.writeInt(0); // nRemainServerItemAmount

			// ========== 寫入接力階段信息 ==========
			buffer.writeShort(product.isRelay() ? (product.getRelayStage() - 1) : 0); // sCircleNum

			// ========== 修正：機率轉換邏輯 ==========
			// XML 中存儲的是累積機率，需要轉換成獨立機率發送給客戶端
			// 例如：XML 中 chance="50" chance2="100"
			//      → 客戶端應該顯示：產品1=50%, 產品2=50%

			final float cumulativeChance1 = product.getChance();  // 第一個產品的累積機率（0-chance1）
			final float cumulativeChance2 = product.getChance2(); // 第二個產品的累積機率（0-chance2）
			final float cumulativeChance3 = product.getChance3(); // 第三個產品的累積機率（0-chance3）
			final float cumulativeChance4 = product.getChance4(); // 第四個產品的累積機率（0-chance4）

			// 轉換成獨立機率
			float independentChance1 = cumulativeChance1;
			float independentChance2 = 0f;
			float independentChance3 = 0f;
			float independentChance4 = 0f;
			float independentChance5 = 0f;

			// 計算第二個產品的獨立機率
			if (product.getProductionId2() > 0)
			{
				if (cumulativeChance2 > 0)
				{
					independentChance2 = cumulativeChance2 - cumulativeChance1;
				}
				else
				{
					// 如果沒有配置 chance2，預設為"剩餘全部"
					independentChance2 = 100f - cumulativeChance1;
				}
			}

			// 計算第三個產品的獨立機率
			if (product.getProductionId3() > 0)
			{
				if (cumulativeChance3 > 0)
				{
					independentChance3 = cumulativeChance3 - cumulativeChance2;
				}
				else
				{
					// 如果沒有配置 chance3，預設為"剩餘全部"
					independentChance3 = 100f - cumulativeChance2;
				}
			}

			// 計算第四個產品的獨立機率
			if (product.getProductionId4() > 0)
			{
				if (cumulativeChance4 > 0)
				{
					independentChance4 = cumulativeChance4 - cumulativeChance3;
				}
				else
				{
					// 如果沒有配置 chance4，預設為"剩餘全部"
					independentChance4 = 100f - cumulativeChance3;
				}
			}

			// 計算第五個產品的獨立機率
			if (product.getProductionId5() > 0)
			{
				// 第五個產品是"剩餘全部"
				independentChance5 = 100f - cumulativeChance4;
			}

			// 確保機率不為負數（防禦性編程）
			independentChance1 = Math.max(0f, independentChance1);
			independentChance2 = Math.max(0f, independentChance2);
			independentChance3 = Math.max(0f, independentChance3);
			independentChance4 = Math.max(0f, independentChance4);
			independentChance5 = Math.max(0f, independentChance5);

			// 寫入獨立機率給客戶端（乘以100000是客戶端的格式要求）
			buffer.writeInt((int) (independentChance1 * 100000));
			buffer.writeInt((int) (independentChance2 * 100000));
			buffer.writeInt((int) (independentChance3 * 100000));
			buffer.writeInt((int) (independentChance4 * 100000));
			buffer.writeInt((int) (independentChance5 * 100000));
		}
	}
}