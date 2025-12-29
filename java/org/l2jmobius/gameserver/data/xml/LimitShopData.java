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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.LimitShopProductHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.ItemTemplate;

/**
 * @author Mobius
 */
public class LimitShopData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(LimitShopData.class.getName());

	private final List<LimitShopProductHolder> _products = new ArrayList<>();

	protected LimitShopData()
	{
		load();
	}

	@Override
	public void load()
	{
		_products.clear();
		parseDatapackFile("data/LimitShop.xml");

		if (!_products.isEmpty())
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _products.size() + " items.");
		}
		else
		{
			LOGGER.info(getClass().getSimpleName() + ": System is disabled.");
		}
	}

	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				final NamedNodeMap at = n.getAttributes();
				final Node attribute = at.getNamedItem("enabled");
				if ((attribute != null) && Boolean.parseBoolean(attribute.getNodeValue()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("product".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							Node att;
							final StatSet set = new StatSet();
							for (int i = 0; i < attrs.getLength(); i++)
							{
								att = attrs.item(i);
								set.set(att.getNodeName(), att.getNodeValue());
							}

							final int id = parseInteger(attrs, "id");
							final int category = parseInteger(attrs, "category");
							final int minLevel = parseInteger(attrs, "minLevel", 1);
							final int maxLevel = parseInteger(attrs, "maxLevel", 999);

							// ========== 解析 relay 屬性 ==========
							final int relayGroup = parseInteger(attrs, "relayGroup", 0);
							final int relayStage = parseInteger(attrs, "relayStage", 0);
							final int relayTotal = parseInteger(attrs, "relayTotal", 0);
							final int prerequisite = parseInteger(attrs, "prerequisite", 0);

							final int[] ingredientIds = new int[5];
							ingredientIds[0] = 0;
							ingredientIds[1] = 0;
							ingredientIds[2] = 0;
							ingredientIds[3] = 0;
							ingredientIds[4] = 0;
							final long[] ingredientQuantities = new long[5];
							ingredientQuantities[0] = 0;
							ingredientQuantities[1] = 0;
							ingredientQuantities[2] = 0;
							ingredientQuantities[3] = 0;
							ingredientQuantities[4] = 0;
							final int[] ingredientEnchants = new int[5];
							ingredientEnchants[0] = 0;
							ingredientEnchants[1] = 0;
							ingredientEnchants[2] = 0;
							ingredientEnchants[3] = 0;
							ingredientEnchants[4] = 0;

							// ========== 初始化所有產品相關變量 ==========
							int productionId = 0;
							int productionCount = 0;
							float productionChance = 100f;
							boolean productionAnnounce = false;
							int productionEnchant = 0;

							int productionId2 = 0;
							int productionCount2 = 0;
							float productionChance2 = 0f;
							boolean productionAnnounce2 = false;

							int productionId3 = 0;
							int productionCount3 = 0;
							float productionChance3 = 0f;
							boolean productionAnnounce3 = false;

							int productionId4 = 0;
							int productionCount4 = 0;
							float productionChance4 = 0f;
							boolean productionAnnounce4 = false;

							int productionId5 = 0;
							int productionCount5 = 0;
							boolean productionAnnounce5 = false;

							int accountDailyLimit = 0;
							int accountWeeklyLimit = 0;
							int accountMonthlyLimit = 0;
							int accountBuyLimit = 0;

							for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
							{
								attrs = b.getAttributes();

								if ("ingredient".equalsIgnoreCase(b.getNodeName()))
								{
									final int ingredientId = parseInteger(attrs, "id");
									final long ingredientQuantity = parseLong(attrs, "count", 1L);
									final int ingredientEnchant = parseInteger(attrs, "enchant", 0);

									if (ingredientId > 0)
									{
										final ItemTemplate template = ItemData.getInstance().getTemplate(ingredientId);
										if (template == null)
										{
											LOGGER.severe(getClass().getSimpleName() + ": Item template null for itemId: " + ingredientId + " productId: " + id);
											continue;
										}

										if ((ingredientQuantity > 1) && !template.isStackable() && !template.isEquipable())
										{
											LOGGER.warning(getClass().getSimpleName() + ": Item template for itemId: " + ingredientId + " should be stackable!");
										}
									}

									if (ingredientIds[0] == 0)
									{
										ingredientIds[0] = ingredientId;
									}
									else if (ingredientIds[1] == 0)
									{
										ingredientIds[1] = ingredientId;
									}
									else if (ingredientIds[2] == 0)
									{
										ingredientIds[2] = ingredientId;
									}
									else if (ingredientIds[3] == 0)
									{
										ingredientIds[3] = ingredientId;
									}
									else
									{
										ingredientIds[4] = ingredientId;
									}

									if (ingredientQuantities[0] == 0)
									{
										ingredientQuantities[0] = ingredientQuantity;
									}
									else if (ingredientQuantities[1] == 0)
									{
										ingredientQuantities[1] = ingredientQuantity;
									}
									else if (ingredientQuantities[2] == 0)
									{
										ingredientQuantities[2] = ingredientQuantity;
									}
									else if (ingredientQuantities[3] == 0)
									{
										ingredientQuantities[3] = ingredientQuantity;
									}
									else
									{
										ingredientQuantities[4] = ingredientQuantity;
									}

									if (ingredientEnchants[0] == 0)
									{
										ingredientEnchants[0] = ingredientEnchant;
									}
									else if (ingredientEnchants[1] == 0)
									{
										ingredientEnchants[1] = ingredientEnchant;
									}
									else if (ingredientEnchants[2] == 0)
									{
										ingredientEnchants[2] = ingredientEnchant;
									}
									else if (ingredientEnchants[3] == 0)
									{
										ingredientEnchants[3] = ingredientEnchant;
									}
									else
									{
										ingredientEnchants[4] = ingredientEnchant;
									}
								}
								else if ("production".equalsIgnoreCase(b.getNodeName()))
								{
									// ========== 解析第一個產品（必需） ==========
									productionId = parseInteger(attrs, "id");
									productionCount = parseInteger(attrs, "count", 1);
									productionChance = parseFloat(attrs, "chance", 100f);
									productionAnnounce = parseBoolean(attrs, "announce", false);
									productionEnchant = parseInteger(attrs, "enchant", 0);

									// ========== 解析第二個產品（可選） ==========
									productionId2 = parseInteger(attrs, "id2", 0);
									productionCount2 = parseInteger(attrs, "count2", 0);
									productionChance2 = parseFloat(attrs, "chance2", 0f);
									productionAnnounce2 = parseBoolean(attrs, "announce2", false);

									// ========== 解析第三個產品（可選） ==========
									productionId3 = parseInteger(attrs, "id3", 0);
									productionCount3 = parseInteger(attrs, "count3", 0);
									productionChance3 = parseFloat(attrs, "chance3", 0f);
									productionAnnounce3 = parseBoolean(attrs, "announce3", false);

									// ========== 解析第四個產品（可選） ==========
									productionId4 = parseInteger(attrs, "id4", 0);
									productionCount4 = parseInteger(attrs, "count4", 0);
									productionChance4 = parseFloat(attrs, "chance4", 0f);
									productionAnnounce4 = parseBoolean(attrs, "announce4", false);

									// ========== 解析第五個產品（可選） ==========
									productionId5 = parseInteger(attrs, "id5", 0);
									productionCount5 = parseInteger(attrs, "count5", 0);
									productionAnnounce5 = parseBoolean(attrs, "announce5", false);

									// ========== 解析限購屬性 ==========
									accountDailyLimit = parseInteger(attrs, "accountDailyLimit", 0);
									accountWeeklyLimit = parseInteger(attrs, "accountWeeklyLimit", 0);
									accountMonthlyLimit = parseInteger(attrs, "accountMonthlyLimit", 0);
									accountBuyLimit = parseInteger(attrs, "accountBuyLimit", 0);

									// ========== 驗證第一個產品 ==========
									final ItemTemplate item = ItemData.getInstance().getTemplate(productionId);
									if (item == null)
									{
										LOGGER.severe(getClass().getSimpleName() + ": Item template null for itemId: " + productionId + " productId: " + id);
										continue;
									}

									// ========== 驗證第二個產品（如果有） ==========
									if (productionId2 > 0)
									{
										final ItemTemplate item2 = ItemData.getInstance().getTemplate(productionId2);
										if (item2 == null)
										{
											LOGGER.warning(getClass().getSimpleName() + ": Item2 template null for itemId: " + productionId2 + " productId: " + id);
											productionId2 = 0; // 重置為無效
										}
									}

									// ========== 驗證第三個產品（如果有） ==========
									if (productionId3 > 0)
									{
										final ItemTemplate item3 = ItemData.getInstance().getTemplate(productionId3);
										if (item3 == null)
										{
											LOGGER.warning(getClass().getSimpleName() + ": Item3 template null for itemId: " + productionId3 + " productId: " + id);
											productionId3 = 0;
										}
									}

									// ========== 驗證第四個產品（如果有） ==========
									if (productionId4 > 0)
									{
										final ItemTemplate item4 = ItemData.getInstance().getTemplate(productionId4);
										if (item4 == null)
										{
											LOGGER.warning(getClass().getSimpleName() + ": Item4 template null for itemId: " + productionId4 + " productId: " + id);
											productionId4 = 0;
										}
									}

									// ========== 驗證第五個產品（如果有） ==========
									if (productionId5 > 0)
									{
										final ItemTemplate item5 = ItemData.getInstance().getTemplate(productionId5);
										if (item5 == null)
										{
											LOGGER.warning(getClass().getSimpleName() + ": Item5 template null for itemId: " + productionId5 + " productId: " + id);
											productionId5 = 0;
										}
									}
								}
							}

							// ========== 創建商品持有者，使用解析的所有數據 ==========
							_products.add(new LimitShopProductHolder(
									id, category, minLevel, maxLevel,
									ingredientIds, ingredientQuantities, ingredientEnchants,
									productionId, productionCount, productionChance, productionAnnounce, productionEnchant,
									productionId2, productionCount2, productionChance2, productionAnnounce2,
									productionId3, productionCount3, productionChance3, productionAnnounce3,
									productionId4, productionCount4, productionChance4, productionAnnounce4,
									productionId5, productionCount5, productionAnnounce5,
									accountDailyLimit, accountWeeklyLimit, accountMonthlyLimit, accountBuyLimit,
									relayGroup, relayStage, relayTotal, prerequisite
							));
						}
					}
				}
			}
		}
	}

	/**
	 * 獲取指定 ID 和階段的商品
	 * @param id 商品ID
	 * @param stage 階段（0 = 不限階段，返回第一個）
	 * @return 商品持有者
	 */
	public LimitShopProductHolder getProduct(int id, int stage)
	{
		for (LimitShopProductHolder product : _products)
		{
			if (product.getId() == id)
			{
				// 如果不限制階段，返回第一個
				if (stage == 0)
				{
					return product;
				}
				// 如果指定階段，必須匹配
				if (product.getRelayStage() == stage)
				{
					return product;
				}
			}
		}
		return null;
	}

	/**
	 * 獲取指定 ID 的所有階段商品（確保正確排序）
	 */
	public List<LimitShopProductHolder> getProductStages(int id)
	{
		List<LimitShopProductHolder> stages = new ArrayList<>();
		for (LimitShopProductHolder product : _products)
		{
			if (product.getId() == id)
			{
				stages.add(product);
			}
		}

		// 按 relayStage 排序（從小到大：1, 2, 3）
		stages.sort((a, b) -> {
			// 如果不是接力商品，保持原順序
			if (!a.isRelay() || !b.isRelay())
			{
				return 0;
			}
			// 按階段排序
			return Integer.compare(a.getRelayStage(), b.getRelayStage());
		});

		return stages;
	}

	public LimitShopProductHolder getCurrentStageProduct(Player player, int productId)
	{
		// 先嘗試用 ID 查找
		List<LimitShopProductHolder> stages = getProductStages(productId);

		// 如果找不到，嘗試用 relayGroup 查找
		if (stages.isEmpty())
		{
			stages = getProductStagesByGroup(productId);
		}

		if (stages.isEmpty())
		{
			return null;
		}

		// 如果不是接力商品，直接返回第一個
		if (!stages.get(0).isRelay())
		{
			return stages.get(0);
		}

		// 從第1階段開始檢查
		for (LimitShopProductHolder stage : stages)
		{
			String purchaseKey = "RELAY_PRODUCT_" + stage.getRelayGroup() + "_STAGE_" + stage.getRelayStage();
			int purchased = player.getAccountVariables().getInt(purchaseKey, 0);

			if (purchased == 0)
			{
				return stage;
			}
		}

		// 全部購買完畢，返回最後一個階段
		return stages.get(stages.size() - 1);
	}

	// 新增：根據 relayGroup 查找
	public List<LimitShopProductHolder> getProductStagesByGroup(int relayGroup)
	{
		List<LimitShopProductHolder> stages = new ArrayList<>();
		for (LimitShopProductHolder product : _products)
		{
			if (product.getRelayGroup() == relayGroup)
			{
				stages.add(product);
			}
		}

		stages.sort((a, b) -> Integer.compare(a.getRelayStage(), b.getRelayStage()));
		return stages;
	}

	/**
	 * 檢查玩家是否已購買指定階段
	 * @param player 玩家
	 * @param product 商品
	 * @return true 表示已購買
	 */
	public boolean isRelayProductPurchased(Player player, LimitShopProductHolder product)
	{
		if (!product.isRelay())
		{
			return false;
		}

		String purchaseKey = "RELAY_PRODUCT_" + product.getId() + "_STAGE_" + product.getRelayStage();
		return player.getAccountVariables().getInt(purchaseKey, 0) > 0;
	}

	/**
	 * 記錄玩家購買了某個階段
	 * @param player 玩家
	 * @param product 商品
	 */
	public void markRelayProductPurchased(Player player, LimitShopProductHolder product)
	{
		if (!product.isRelay())
		{
			return;
		}

		String purchaseKey = "RELAY_PRODUCT_" + product.getId() + "_STAGE_" + product.getRelayStage();
		player.getAccountVariables().set(purchaseKey, 1);
	}

	/**
	 * 檢查玩家是否已購買完所有階段
	 * @param player 玩家
	 * @param productId 商品ID
	 * @return true 表示全部購買完畢
	 */
	public boolean isAllRelayStagesPurchased(Player player, int productId)
	{
		List<LimitShopProductHolder> stages = getProductStages(productId);
		if (stages.isEmpty() || !stages.get(0).isRelay())
		{
			return false;
		}

		for (LimitShopProductHolder stage : stages)
		{
			String purchaseKey = "RELAY_PRODUCT_" + productId + "_STAGE_" + stage.getRelayStage();
			if (player.getAccountVariables().getInt(purchaseKey, 0) == 0)
			{
				return false; // 有階段未購買
			}
		}

		return true; // 全部購買完畢
	}

	// 保留原有方法，向後兼容
	public LimitShopProductHolder getProduct(int id)
	{
		return getProduct(id, 0);
	}

	public List<LimitShopProductHolder> getProducts()
	{
		return _products;
	}

	public static LimitShopData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final LimitShopData INSTANCE = new LimitShopData();
	}
}