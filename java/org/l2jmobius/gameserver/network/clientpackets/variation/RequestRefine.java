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

import org.l2jmobius.gameserver.data.xml.VariationData;
import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.Variation;
import org.l2jmobius.gameserver.model.options.VariationFee;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.AbstractRefinePacket;
import org.l2jmobius.gameserver.network.serverpackets.ExVariationResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

/**
 * Format:(ch) ddc
 * @author -Wooden-, Index
 */
public class RequestRefine extends AbstractRefinePacket
{
	private int _targetItemObjId;
	private int _mineralItemObjId;
	
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readInt();
		_mineralItemObjId = readInt();
		readByte(); // is event
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item targetItem = player.getInventory().getItemByObjectId(_targetItemObjId);
		if (targetItem == null)
		{
			return;
		}
		
		final Item mineralItem = player.getInventory().getItemByObjectId(_mineralItemObjId);
		if (mineralItem == null)
		{
			return;
		}
		
		final VariationFee fee = VariationData.getInstance().getFee(targetItem.getId(), mineralItem.getId());
		if (fee == null)
		{
			return;
		}
		
		final Item feeItem = player.getInventory().getItemByItemId(fee.getItemId());
		if ((feeItem == null) && (fee.getItemId() != 0))
		{
			PacketLogger.warning(getClass().getSimpleName() + ": " + player.getName() + " does not have required fee item (ID: " + fee.getItemId() + ") for mineral ID: " + mineralItem.getId());
			player.sendPacket(ExVariationResult.FAIL);
			player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}
		
		if (!isValid(player, targetItem, mineralItem, feeItem, fee))
		{
			player.sendPacket(ExVariationResult.FAIL);
			player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}
		
		if (fee.getAdenaFee() <= 0)
		{
			player.sendPacket(ExVariationResult.FAIL);
			player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}
		
		final long adenaFee = fee.getAdenaFee();
		if ((adenaFee > 0) && (player.getAdena() < adenaFee))
		{
			player.sendPacket(ExVariationResult.FAIL);
			player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}
		
		final Variation variation = VariationData.getInstance().getVariation(mineralItem.getId(), targetItem);
		if (variation == null)
		{
			player.sendPacket(ExVariationResult.FAIL);
			return;
		}
		
		VariationInstance augment = VariationData.getInstance().generateRandomVariation(variation, targetItem);
		if (augment == null)
		{
			player.sendPacket(ExVariationResult.FAIL);
			return;
		}
		
		// Only reuse old options for slots that are truly optional and intentionally left empty (if needed).
		// For your rare group, we don't fallback to old augment.
		final int option1 = augment.getOption1Id();
		final int option2 = augment.getOption2Id();
		final int option3 = augment.getOption3Id();
		
		// Optional: only fallback for option1/2 if your design requires at least 2 guaranteed options.
		final VariationInstance oldAugment = targetItem.getAugmentation();
		if (oldAugment != null)
		{
			final int newOption1 = (option1 > 0) ? option1 : 0;
			final int newOption2 = (option2 > 0) ? option2 : 0;
			final int newOption3 = (option3 > 0) ? option3 : 0; // Do not fallback to old 3rd option.
			augment = new VariationInstance(augment.getMineralId(), newOption1, newOption2, newOption3);
		}
		
		// Essence does not support creating a new augment without losing old one.
		targetItem.setAugmentation(augment, true);
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		player.sendInventoryUpdate(iu);
		
		player.sendPacket(new ExVariationResult(augment.getOption1Id(), augment.getOption2Id(), augment.getOption3Id(), true));
		
		// Consume the life stone.
		player.destroyItem(ItemProcessType.FEE, mineralItem, 1, null, false);
		
		// Consume the gemstones.
		if (feeItem != null)
		{
			player.destroyItem(ItemProcessType.FEE, feeItem, fee.getItemCount(), null, false);
		}
		
		// Consume Adena.
		if (adenaFee > 0)
		{
			player.reduceAdena(ItemProcessType.FEE, adenaFee, player, false);
		}
	}
}
