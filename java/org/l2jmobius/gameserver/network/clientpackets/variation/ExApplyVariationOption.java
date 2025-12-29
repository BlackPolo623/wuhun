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
package org.l2jmobius.gameserver.network.clientpackets.variation;

import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.VariationRequest;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.variation.ApplyVariationOption;

/**
 * @author Index, Mobius
 */
public class ExApplyVariationOption extends ClientPacket
{
	private int _enchantedObjectId;
	private int _option1;
	private int _option2;
	private int _option3;
	
	@Override
	protected void readImpl()
	{
		_enchantedObjectId = readInt();
		_option1 = readInt();
		_option2 = readInt();
		_option3 = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final VariationRequest request = player.getRequest(VariationRequest.class);
		if (request == null)
		{
			return;
		}
		
		final Item targetItem = request.getAugmentedItem();
		final VariationInstance augment = request.getAugment();
		final int option1Id = augment.getOption1Id();
		final int option2Id = augment.getOption2Id();
		final int option3Id = augment.getOption3Id();
		if ((targetItem.getObjectId() != _enchantedObjectId) || (_option1 != option1Id) || (_option2 != option2Id) || (_option3 != option3Id))
		{
			player.sendPacket(new ApplyVariationOption(0, 0, 0, 0, 0));
			return;
		}
		
		targetItem.setAugmentation(augment, true);
		
		player.sendPacket(new ApplyVariationOption(1, _enchantedObjectId, _option1, _option2, _option3));
		
		// Apply new augment.
		if (targetItem.isEquipped())
		{
			targetItem.getAugmentation().applyBonus(player);
		}
		
		// Recalculate all stats.
		player.getStat().recalculateStats(true);
		
		player.sendItemList();
		player.removeRequest(VariationRequest.class);
	}
}
