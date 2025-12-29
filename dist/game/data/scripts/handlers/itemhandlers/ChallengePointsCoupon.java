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

import org.l2jmobius.gameserver.data.xml.EnchantChallengePointData;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.enchant.challengepoint.ExEnchantChallengePointInfo;

/**
 * @author Serenitty
 */
public class ChallengePointsCoupon implements IItemHandler
{
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
			return false;
		}
		
		final int pointsToGive;
		final int categoryId;
		switch (item.getId())
		{
			case 97125: // Rare Accessory Challenge Points +50
			{
				pointsToGive = 50;
				categoryId = 1;
				break;
			}
			case 97126: // Talisman Challenge Points +50
			{
				pointsToGive = 50;
				categoryId = 2;
				break;
			}
			case 97127: // Special Equipment Challenge Points +50
			{
				pointsToGive = 50;
				categoryId = 3;
				break;
			}
			case 97276: // Rare Accessory Challenge Points +20
			{
				pointsToGive = 20;
				categoryId = 1;
				break;
			}
			case 97277: // Talisman Challenge Points +20
			{
				pointsToGive = 20;
				categoryId = 2;
				break;
			}
			case 97278: // Special Equipment Challenge Points +20
			{
				pointsToGive = 20;
				categoryId = 3;
				break;
			}
			default:
			{
				return false;
			}
		}
		
		final Player player = playable.asPlayer();
		if (player.getChallengeInfo().canAddPoints(categoryId, pointsToGive))
		{
			player.destroyItem(ItemProcessType.FEE, item.getObjectId(), 1, null, false);
			player.getChallengeInfo().getChallengePoints().compute(categoryId, (k, v) -> v == null ? Math.min(EnchantChallengePointData.getInstance().getMaxPoints(), pointsToGive) : Math.min(EnchantChallengePointData.getInstance().getMaxPoints(), v + pointsToGive));
			player.sendPacket(new ExEnchantChallengePointInfo(player));
		}
		else
		{
			player.sendMessage("The points of this coupon exceed the limit.");
		}
		
		return true;
	}
}
