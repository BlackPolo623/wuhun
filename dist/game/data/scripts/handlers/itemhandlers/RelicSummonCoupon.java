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
package handlers.itemhandlers;

import java.util.List;

import org.l2jmobius.gameserver.config.RelicSystemConfig;
import org.l2jmobius.gameserver.data.holders.RelicCouponHolder;
import org.l2jmobius.gameserver.data.xml.RelicCouponData;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.RelicSummonRequest;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.variables.AccountVariables;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsSummonResult;

/**
 * @author CostyKiller, Brado
 */
public class RelicSummonCoupon implements IItemHandler
{
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
			return false;
		}
		
		final Player player = playable.asPlayer();
		if (player.isCastingNow())
		{
			return false;
		}
		
		if (player.hasRequest(RelicSummonRequest.class))
		{
			return false;
		}
		
		// If you have 100 relics in your confirmation list, restrictions are applied to the relic summoning and compounding functions.
		if (player.getAccountVariables().getInt(AccountVariables.UNCONFIRMED_RELICS_COUNT, 0) == 100)
		{
			player.sendPacket(SystemMessageId.SUMMON_COMPOUND_IS_UNAVAILABLE_AS_YOU_HAVE_MORE_THAN_100_UNCONFIRMED_DOLLS);
			return false;
		}
		
		final RelicCouponHolder relicCoupon = RelicCouponData.getInstance().getCouponFromCouponItemId(item.getId());
		if ((relicCoupon == null) || !player.destroyItem(ItemProcessType.DESTROY, item, 1, player, true))
		{
			player.sendPacket(SystemMessageId.FAILURE_ALL_ITEMS_HAVE_DISAPPEARED);
			return false;
		}
		
		player.addRequest(new RelicSummonRequest(player));
		
		final List<Integer> obtainedRelics = RelicCouponData.getInstance().generateSummonRelics(relicCoupon);
		if (obtainedRelics.isEmpty())
		{
			player.sendPacket(SystemMessageId.AN_ERROR_HAS_OCCURRED_PLEASE_TRY_AGAIN_LATER);
			PacketLogger.finer("Relic Coupon: " + item.getId() + " generated 0 relics!");
			return false;
		}
		
		for (int relicId : obtainedRelics)
		{
			player.handleRelicAcquisition(relicId);
			if (RelicSystemConfig.RELIC_SYSTEM_DEBUG_ENABLED)
			{
				player.sendMessage("Summoned relic ID: " + relicId);
			}
		}
		
		player.sendPacket(new ExRelicsSummonResult(relicCoupon, obtainedRelics));
		return true;
	}
}
