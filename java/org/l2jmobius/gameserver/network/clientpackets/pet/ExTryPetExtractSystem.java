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
package org.l2jmobius.gameserver.network.clientpackets.pet;

import org.l2jmobius.gameserver.data.holders.PetExtractionHolder;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.data.xml.PetExtractData;
import org.l2jmobius.gameserver.model.PetData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.PetInventory;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.pet.ResultPetExtractSystem;

/**
 * @author Geremy
 */
public class ExTryPetExtractSystem extends ClientPacket
{
	private int _itemObjId;
	
	@Override
	protected void readImpl()
	{
		_itemObjId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item petItem = player.getInventory().getItemByObjectId(_itemObjId);
		if ((petItem == null) || ((player.getPet() != null) && (player.getPet().getControlItem() == petItem)))
		{
			player.sendPacket(new ResultPetExtractSystem(false));
			return;
		}
		
		final PetData petData = PetDataTable.getInstance().getPetDataByItemId(petItem.getId());
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(petData.getNpcId());
		final Pet pet = new Pet(npcTemplate, player, petItem);
		final PetInventory petInventory = pet.getInventory();
		final PlayerInventory playerInventory = player.getInventory();
		if ((petInventory == null) || (playerInventory == null))
		{
			player.sendPacket(new ResultPetExtractSystem(false));
			return;
		}
		
		if (!playerInventory.validateWeight(petInventory.getTotalWeight()) || !playerInventory.validateCapacity(petInventory.getSize()))
		{
			player.sendPacket(SystemMessageId.CANNOT_BE_SENT_VIA_MAIL_SOLD_AT_A_SHOP_OR_VIA_THE_AUCTION_THE_GUARDIAN_S_INVENTORY_IS_NOT_EMPTY_PLEASE_TAKE_EVERYTHING_FROM_THERE_FIRST);
			player.sendPacket(new ResultPetExtractSystem(false));
			return;
		}
		
		petInventory.transferItemsToOwner();
		
		final Pet petInfo = Pet.restore(petItem, NpcData.getInstance().getTemplate(petData.getNpcId()), player);
		final int petId = PetDataTable.getInstance().getPetDataByItemId(petItem.getId()).getType();
		final int petLevel = petInfo.getLevel();
		final PetExtractionHolder holder = PetExtractData.getInstance().getExtraction(petId, petLevel);
		if (holder != null)
		{
			final int extractItemId = holder.getExtractItem();
			final int extractItemCount = (int) (petInfo.getStat().getExp() / holder.getExtractExp());
			final int extractCostId = holder.getExtractCost().getId();
			final long extractCostCount = holder.getExtractCost().getCount() * extractItemCount;
			final int defaultCostId = holder.getDefaultCost().getId();
			final long defaultCostCount = holder.getDefaultCost().getCount();
			if ((player.getInventory().getInventoryItemCount(extractCostId, -1) >= extractCostCount) && (player.getInventory().getInventoryItemCount(defaultCostId, -1) >= defaultCostCount))
			{
				if (player.destroyItemByItemId(ItemProcessType.FEE, extractCostId, extractCostCount, player, true) && player.destroyItemByItemId(ItemProcessType.FEE, defaultCostId, defaultCostCount, player, true) && player.destroyItem(ItemProcessType.FEE, petItem, player, true))
				{
					player.addItem(ItemProcessType.REWARD, extractItemId, extractItemCount, player, true);
					player.sendPacket(new ResultPetExtractSystem(true));
				}
			}
			else
			{
				player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT);
				player.sendPacket(new ResultPetExtractSystem(false));
			}
			return;
		}
		
		player.sendPacket(new ResultPetExtractSystem(false));
	}
}
