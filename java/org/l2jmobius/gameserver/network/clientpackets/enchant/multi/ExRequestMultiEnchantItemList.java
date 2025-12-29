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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.EnchantChallengePointData;
import org.l2jmobius.gameserver.data.xml.EnchantItemData;
import org.l2jmobius.gameserver.data.xml.ItemCrystallizationData;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.managers.events.BlackCouponManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.EnchantItemRequest;
import org.l2jmobius.gameserver.model.item.enchant.EnchantResultType;
import org.l2jmobius.gameserver.model.item.enchant.EnchantScroll;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ShortcutInit;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.enchant.EnchantResult;
import org.l2jmobius.gameserver.network.serverpackets.enchant.challengepoint.ExEnchantChallengePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.enchant.multi.ExResultMultiEnchantItemList;
import org.l2jmobius.gameserver.network.serverpackets.enchant.multi.ExResultSetMultiEnchantItemList;
import org.l2jmobius.gameserver.network.serverpackets.enchant.single.ChangedEnchantTargetItemProbabilityList;

/**
 * @author Index
 */
public class ExRequestMultiEnchantItemList extends ClientPacket
{
	private int _useLateAnnounce;
	private int _slotId;
	private final Map<Integer, Integer> _itemObjectId = new HashMap<>();
	private final Map<Integer, String> _result = new HashMap<>();
	private final Map<Integer, int[]> _successEnchant = new HashMap<>();
	private final Map<Integer, Integer> _failureEnchant = new HashMap<>();
	final Map<Integer, Integer> failChallengePointInfoList = new LinkedHashMap<>();
	
	/**
	 * @code slot_id @code item_holder
	 */
	private final Map<Integer, ItemHolder> _failureReward = new HashMap<>();
	
	protected static final Logger LOGGER_ENCHANT = Logger.getLogger("enchant.items");
	
	@Override
	protected void readImpl()
	{
		_useLateAnnounce = readByte();
		_slotId = readInt();
		for (int i = 1; remaining() != 0; i++)
		{
			_itemObjectId.put(i, readInt());
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		player.getChallengeInfo().setChallengePointsPendingRecharge(-1, -1);
		
		final EnchantItemRequest request = player.getRequest(EnchantItemRequest.class);
		if (request == null)
		{
			return;
		}
		
		if ((request.getEnchantingScroll() == null) || request.isProcessing())
		{
			return;
		}
		
		final Item scroll = request.getEnchantingScroll();
		if (scroll.getCount() < _slotId)
		{
			player.removeRequest(request.getClass());
			player.sendPacket(new ExResultSetMultiEnchantItemList(player, 1));
			Logger.getLogger("MultiEnchant - player " + player.getObjectId() + " " + player.getName() + " trying enchant items, when scroll count is less than items!");
			return;
		}
		
		final EnchantScroll scrollTemplate = EnchantItemData.getInstance().getEnchantScroll(scroll);
		if (scrollTemplate == null)
		{
			return;
		}
		
		final int[] slots = new int[_slotId];
		for (int i = 1; i <= _slotId; i++)
		{
			if (!request.checkMultiEnchantingItemsByObjectId(_itemObjectId.get(i)))
			{
				player.removeRequest(request.getClass());
				return;
			}
			
			slots[i - 1] = getMultiEnchantingSlotByObjectId(request, _itemObjectId.get(i));
		}
		
		_itemObjectId.clear();
		request.setProcessing(true);
		
		for (int slotCounter = 0; slotCounter < slots.length; slotCounter++)
		{
			final int i = slots[slotCounter];
			if ((i == -1) || (request.getMultiEnchantingItemsBySlot(i) == -1))
			{
				player.sendPacket(new ExResultMultiEnchantItemList(player, true));
				player.removeRequest(request.getClass());
				return;
			}
			
			final Item enchantItem = player.getInventory().getItemByObjectId(request.getMultiEnchantingItemsBySlot(i));
			if (enchantItem == null)
			{
				player.removeRequest(request.getClass());
				return;
			}
			
			if (scrollTemplate.getMaxEnchantLevel() < enchantItem.getEnchantLevel())
			{
				Logger.getLogger("MultiEnchant - player " + player.getObjectId() + " " + player.getName() + " trying over-enchant item " + enchantItem.getItemName() + " " + enchantItem.getObjectId());
				player.removeRequest(request.getClass());
				return;
			}
			
			if (player.getInventory().destroyItemByItemId(ItemProcessType.FEE, scroll.getId(), 1, player, enchantItem) == null)
			{
				player.removeRequest(request.getClass());
				return;
			}
			
			// final InventoryUpdate iu = new InventoryUpdate();
			synchronized (enchantItem)
			{
				if ((enchantItem.getOwnerId() != player.getObjectId()) || !enchantItem.isEnchantable())
				{
					player.sendPacket(SystemMessageId.AUGMENTATION_REQUIREMENTS_ARE_NOT_FULFILLED);
					player.removeRequest(request.getClass());
					player.sendPacket(new ExResultMultiEnchantItemList(player, true));
					return;
				}
				
				final EnchantResultType resultType = scrollTemplate.calculateSuccess(player, enchantItem, null);
				switch (resultType)
				{
					case ERROR:
					{
						player.sendPacket(SystemMessageId.AUGMENTATION_REQUIREMENTS_ARE_NOT_FULFILLED);
						player.removeRequest(request.getClass());
						_result.put(slots[i - 1], "ERROR");
						break;
					}
					case SUCCESS:
					{
						if (scrollTemplate.isCursed())
						{
							// Blessed enchant: Enchant value down by 1.
							player.sendPacket(SystemMessageId.THE_ENCHANT_VALUE_IS_DECREASED_BY_1);
							enchantItem.setEnchantLevel(enchantItem.getEnchantLevel() - 1);
						}
						// Increase enchant level only if scroll's base template has chance, some armors can success over +20 but they shouldn't have increased.
						else if (scrollTemplate.getChance(player, enchantItem) > 0)
						{
							enchantItem.setEnchantLevel(enchantItem.getEnchantLevel() + Math.min(Rnd.get(scrollTemplate.getRandomEnchantMin(), scrollTemplate.getRandomEnchantMax()), scrollTemplate.getMaxEnchantLevel()));
							enchantItem.updateDatabase();
						}
						
						_result.put(i, "SUCCESS");
						if (GeneralConfig.LOG_ITEM_ENCHANTS)
						{
							final StringBuilder sb = new StringBuilder();
							if (enchantItem.getEnchantLevel() > 0)
							{
								LOGGER_ENCHANT.info(sb.append("Success, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(enchantItem.getEnchantLevel()).append(" ").append(enchantItem.getName()).append("(").append(enchantItem.getCount()).append(") [").append(enchantItem.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
							}
							else
							{
								LOGGER_ENCHANT.info(sb.append("Success, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", ").append(enchantItem.getName()).append("(").append(enchantItem.getCount()).append(") [").append(enchantItem.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
							}
						}
						break;
					}
					case FAILURE:
					{
						if (scrollTemplate.isSafe())
						{
							// Safe enchant: Remain old value.
							player.sendPacket(SystemMessageId.ENCHANT_FAILED_THE_ENCHANT_SKILL_FOR_THE_CORRESPONDING_ITEM_WILL_BE_EXACTLY_RETAINED);
							player.sendPacket(new EnchantResult(EnchantResult.SAFE_FAIL, new ItemHolder(enchantItem.getId(), 1), null, 0));
							if (GeneralConfig.LOG_ITEM_ENCHANTS)
							{
								final StringBuilder sb = new StringBuilder();
								if (enchantItem.getEnchantLevel() > 0)
								{
									LOGGER_ENCHANT.info(sb.append("Safe Fail, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(enchantItem.getEnchantLevel()).append(" ").append(enchantItem.getName()).append("(").append(enchantItem.getCount()).append(") [").append(enchantItem.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
								}
								else
								{
									LOGGER_ENCHANT.info(sb.append("Safe Fail, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", ").append(enchantItem.getName()).append("(").append(enchantItem.getCount()).append(") [").append(enchantItem.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
								}
							}
						}
						
						if (scrollTemplate.isBlessed() || scrollTemplate.isBlessedDown() || scrollTemplate.isCursed())
						{
							// Blessed enchant: Enchant value down by 1.
							if (scrollTemplate.isBlessedDown() || scrollTemplate.isCursed())
							{
								player.sendPacket(SystemMessageId.THE_ENCHANT_VALUE_IS_DECREASED_BY_1);
								enchantItem.setEnchantLevel(enchantItem.getEnchantLevel() - 1);
							}
							else // Blessed enchant: Clear enchant value.
							{
								player.sendPacket(SystemMessageId.THE_BLESSED_ENCHANT_FAILED_THE_ENCHANT_VALUE_OF_THE_ITEM_BECAME_0);
								enchantItem.setEnchantLevel(0);
							}
							
							_result.put(i, "BLESSED_FAIL");
							enchantItem.updateDatabase();
							if (GeneralConfig.LOG_ITEM_ENCHANTS)
							{
								final StringBuilder sb = new StringBuilder();
								if (enchantItem.getEnchantLevel() > 0)
								{
									LOGGER_ENCHANT.info(sb.append("Blessed Fail, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(enchantItem.getEnchantLevel()).append(" ").append(enchantItem.getName()).append("(").append(enchantItem.getCount()).append(") [").append(enchantItem.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
								}
								else
								{
									LOGGER_ENCHANT.info(sb.append("Blessed Fail, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", ").append(enchantItem.getName()).append("(").append(enchantItem.getCount()).append(") [").append(enchantItem.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
								}
							}
						}
						else
						{
							final int[] challengePoints = EnchantChallengePointData.getInstance().handleFailure(player, enchantItem);
							if ((challengePoints[0] != -1) && (challengePoints[1] != -1))
							{
								failChallengePointInfoList.compute(challengePoints[0], (_, v) -> v == null ? challengePoints[1] : v + challengePoints[1]);
							}
							
							BlackCouponManager.getInstance().createNewRecord(player.getObjectId(), enchantItem.getId(), (short) enchantItem.getEnchantLevel());
							
							if (player.getInventory().destroyItem(ItemProcessType.FEE, enchantItem, player, null) == null)
							{
								// Unable to destroy item, cheater?
								PunishmentManager.handleIllegalPlayerAction(player, "Unable to delete item on enchant failure from " + player + ", possible cheater !", GeneralConfig.DEFAULT_PUNISH);
								player.removeRequest(request.getClass());
								_result.put(i, "ERROR");
								if (GeneralConfig.LOG_ITEM_ENCHANTS)
								{
									final StringBuilder sb = new StringBuilder();
									if (enchantItem.getEnchantLevel() > 0)
									{
										LOGGER_ENCHANT.info(sb.append("Unable to destroy, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(enchantItem.getEnchantLevel()).append(" ").append(enchantItem.getName()).append("(").append(enchantItem.getCount()).append(") [").append(enchantItem.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
									}
									else
									{
										LOGGER_ENCHANT.info(sb.append("Unable to destroy, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", ").append(enchantItem.getName()).append("(").append(enchantItem.getCount()).append(") [").append(enchantItem.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
									}
								}
								return;
							}
							
							World.getInstance().removeObject(enchantItem);
							
							int count = 0;
							if (enchantItem.getTemplate().isCrystallizable())
							{
								count = Math.max(0, enchantItem.getCrystalCount() - ((enchantItem.getTemplate().getCrystalCount() + 1) / 2));
							}
							
							Item crystals = null;
							final int crystalId = enchantItem.getTemplate().getCrystalItemId();
							if (count > 0)
							{
								crystals = player.getInventory().addItem(ItemProcessType.COMPENSATE, crystalId, count, player, enchantItem);
								final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
								sm.addItemName(crystals);
								sm.addLong(count);
								player.sendPacket(sm);
								ItemHolder itemHolder = new ItemHolder(crystalId, count);
								_failureReward.put(_failureReward.size() + 1, itemHolder);
							}
							
							// if (crystals != null)
							// {
							// iu.addItem(crystals); // FIXME: Packet never sent?
							// }
							
							if ((crystalId == 0) || (count == 0))
							{
								ItemHolder itemHolder = new ItemHolder(0, 0);
								_failureReward.put(_failureReward.size() + 1, itemHolder);
								_result.put(i, "NO_CRYSTAL");
							}
							else
							{
								ItemHolder itemHolder = new ItemHolder(0, 0);
								_failureReward.put(_failureReward.size() + 1, itemHolder);
								_result.put(i, "FAIL");
							}
							
							final List<ItemChanceHolder> destroyRewards = ItemCrystallizationData.getInstance().getRewardItems(enchantItem, enchantItem.getEnchantLevel());
							final ItemHolder crystalss = new ItemHolder(crystalId, count);
							for (ItemChanceHolder reward : destroyRewards)
							{
								if (Rnd.get(100f) < reward.getChance())
								{
									// Optional system message.
									final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
									sm.addItemName(reward.getId());
									sm.addLong(reward.getCount());
									player.sendPacket(sm);
									
									// Send one packet per reward.
									player.sendPacket(new EnchantResult(EnchantResult.FAIL, crystalss, reward, 0));
								}
							}
							
							// If no rewards, still send crystals.
							if (destroyRewards.isEmpty())
							{
								player.sendPacket(new EnchantResult(EnchantResult.FAIL, crystalss, null, 0));
							}
							
							if (GeneralConfig.LOG_ITEM_ENCHANTS)
							{
								final StringBuilder sb = new StringBuilder();
								if (enchantItem.getEnchantLevel() > 0)
								{
									LOGGER_ENCHANT.info(sb.append("Fail, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(enchantItem.getEnchantLevel()).append(" ").append(enchantItem.getName()).append("(").append(enchantItem.getCount()).append(") [").append(enchantItem.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
								}
								else
								{
									LOGGER_ENCHANT.info(sb.append("Fail, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", ").append(enchantItem.getName()).append("(").append(enchantItem.getCount()).append(") [").append(enchantItem.getObjectId()).append("], ").append(scroll.getName()).append("(").append(scroll.getCount()).append(") [").append(scroll.getObjectId()).append("]").toString());
								}
							}
						}
						break;
					}
				}
			}
		}
		
		for (int slotCounter = 0; slotCounter < slots.length; slotCounter++)
		{
			final int i = slots[slotCounter];
			if (_result.get(i).equals("SUCCESS"))
			{
				int[] intArray = new int[2];
				intArray[0] = request.getMultiEnchantingItemsBySlot(i);
				intArray[1] = player.getInventory().getItemByObjectId(request.getMultiEnchantingItemsBySlot(i)).getEnchantLevel();
				_successEnchant.put(i, intArray);
			}
			else if (_result.get(i).equals("NO_CRYSTAL") || _result.get(i).equals("FAIL"))
			{
				_failureEnchant.put(i, request.getMultiEnchantingItemsBySlot(i));
				request.changeMultiEnchantingItemsBySlot(i, 0);
			}
			else
			{
				player.sendPacket(new ExResultMultiEnchantItemList(player, _successEnchant, _failureEnchant, failChallengePointInfoList, true));
				player.sendPacket(new ShortcutInit(player));
				return;
			}
		}
		
		for (ItemHolder failure : _failureReward.values())
		{
			request.addMultiEnchantFailItems(failure);
		}
		
		request.setProcessing(false);
		
		player.sendItemList();
		player.broadcastUserInfo();
		player.sendPacket(new ChangedEnchantTargetItemProbabilityList(player, true));
		
		if (_useLateAnnounce == 1)
		{
			request.setMultiSuccessEnchantList(_successEnchant);
			request.setMultiFailureEnchantList(_failureEnchant);
		}
		
		player.sendPacket(new ExResultMultiEnchantItemList(player, _successEnchant, _failureEnchant, failChallengePointInfoList, true));
		player.sendPacket(new ShortcutInit(player));
		player.sendPacket(new ExEnchantChallengePointInfo(player));
	}
	
	public int getMultiEnchantingSlotByObjectId(EnchantItemRequest request, int objectId)
	{
		int slotId = -1;
		for (int i = 1; i <= request.getMultiEnchantingItemsCount(); i++)
		{
			if ((request.getMultiEnchantingItemsCount() == 0) || (objectId == 0))
			{
				return slotId;
			}
			else if (request.getMultiEnchantingItemsBySlot(i) == objectId)
			{
				return i;
			}
		}
		
		return slotId;
	}
}
