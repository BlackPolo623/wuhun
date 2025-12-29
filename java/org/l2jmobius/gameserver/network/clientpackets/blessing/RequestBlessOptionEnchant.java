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
package org.l2jmobius.gameserver.network.clientpackets.blessing;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.BlessingItemRequest;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.CommonSkill;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.blessing.ExBlessOptionEnchant;
import org.l2jmobius.gameserver.network.serverpackets.blessing.ExOpenBlessOptionScroll;
import org.l2jmobius.gameserver.network.serverpackets.enchant.EnchantResult;

/**
 * @author Horus
 */
public class RequestBlessOptionEnchant extends ClientPacket
{
	private int _scrollId;
	private int _itemObjectId;
	
	@Override
	protected void readImpl()
	{
		_scrollId = readInt();
		_itemObjectId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!player.isOnline() || getClient().isDetached())
		{
			return;
		}
		
		final Item scroll = player.getInventory().getItemByItemId(_scrollId);
		if (scroll == null)
		{
			player.sendPacket(new ExBlessOptionEnchant(0, EnchantResult.ERROR));
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(_itemObjectId);
		if (item == null)
		{
			player.sendPacket(new ExBlessOptionEnchant(0, EnchantResult.ERROR));
			return;
		}
		
		final BlessingItemRequest request = player.getRequest(BlessingItemRequest.class);
		if ((request == null) || request.isProcessing())
		{
			player.sendPacket(new ExBlessOptionEnchant(0, EnchantResult.ERROR));
			return;
		}
		
		request.setProcessing(true);
		request.setTimestamp(System.currentTimeMillis());
		
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ENCHANT_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.sendPacket(new ExBlessOptionEnchant(0, EnchantResult.ERROR));
			request.setProcessing(false);
			player.sendItemList();
			player.broadcastUserInfo();
			return;
		}
		
		// first validation check - also over enchant check
		if (item.isBlessed())
		{
			ThreadPool.schedule(() ->
			{
				player.sendPacket(new ExBlessOptionEnchant(0, EnchantResult.SUCCESS));
				player.sendPacket(new ExOpenBlessOptionScroll(scroll.getId()));
			}, 3000);
			request.setProcessing(false);
			player.sendItemList();
			player.broadcastUserInfo();
			return;
		}
		
		final Item targetScroll = player.getInventory().getItemByItemId(request.getBlessScrollId());
		if (targetScroll == null)
		{
			player.sendPacket(new ExBlessOptionEnchant(0, EnchantResult.ERROR));
			request.setProcessing(false);
			player.sendItemList();
			player.broadcastUserInfo();
			return;
		}
		
		// attempting to destroy scroll
		if (player.getInventory().destroyItem(ItemProcessType.FEE, targetScroll.getObjectId(), 1, player, item) == null)
		{
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			PunishmentManager.handleIllegalPlayerAction(player, player + " tried to bless with a scroll he doesn't have", GeneralConfig.DEFAULT_PUNISH);
			player.sendPacket(new ExBlessOptionEnchant(0, EnchantResult.ERROR));
			request.setProcessing(false);
			player.sendItemList();
			player.broadcastUserInfo();
			return;
		}

		if (Rnd.get(10000) < RatesConfig.BLESSING_CHANCE * 100) // Success
		{
			// Increase enchant level only if scroll's base template has chance, some armors can success over +20 but they shouldn't have increased.
			item.setBlessed(true);
			item.updateDatabase();
			player.sendPacket(new ExBlessOptionEnchant(targetScroll.getId(), EnchantResult.SUCCESS));
			
			// Announce the success.
			if ((item.getEnchantLevel() >= (item.isArmor() ? PlayerConfig.MIN_ARMOR_ENCHANT_ANNOUNCE : PlayerConfig.MIN_WEAPON_ENCHANT_ANNOUNCE)) //
				&& (item.getEnchantLevel() <= (item.isArmor() ? PlayerConfig.MAX_ARMOR_ENCHANT_ANNOUNCE : PlayerConfig.MAX_WEAPON_ENCHANT_ANNOUNCE)))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_ENCHANTED_S3_UP_TO_S2);
				sm.addString(player.getName());
				sm.addInt(item.getEnchantLevel());
				sm.addItemName(item);
				player.broadcastPacket(sm);
				// Broadcast.toAllOnlinePlayers(new ExItemAnnounce(player, item, ExItemAnnounce.ENCHANT));
				
				final Skill skill = CommonSkill.FIREWORK.getSkill();
				if (skill != null)
				{
					player.broadcastSkillPacket(new MagicSkillUse(player, player, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()), player);
				}
			}
			else // Send success message to player.
			{
				player.sendMessage(item.getName() + " has been blessed.");
			}
			
			if (item.isEquipped())
			{
				item.getTemplate().forEachSkill(ItemSkillType.ON_BLESSING, holder -> player.addSkill(holder.getSkill()));
				player.sendSkillList();
			}
		}
		else // Failure.
		{
			player.sendPacket(new ExBlessOptionEnchant(targetScroll.getId(), 0));
		}
		
		request.setProcessing(false);
		
		player.sendItemList();
		player.broadcastUserInfo();
	}
}
