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

import org.l2jmobius.gameserver.ai.Action;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.ai.NextAction;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.pet.ExPetSkillList;
import org.l2jmobius.gameserver.network.serverpackets.pet.PetSummonInfo;

/**
 * @author Berezkin Nikolay
 */
public class ExPetUnequipItem extends ClientPacket
{
	private int _objectId;
	private int _itemId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Pet pet = player.getPet();
		if (pet == null)
		{
			return;
		}
		
		// Flood protect UseItem
		if (!getClient().getFloodProtectors().canUseItem())
		{
			return;
		}
		
		if (player.isInsideZone(ZoneId.JAIL))
		{
			player.sendMessage("You cannot use items while jailed.");
			return;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.cancelActiveTrade();
		}
		
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.WHILE_OPERATING_A_PRIVATE_STORE_OR_WORKSHOP_YOU_CANNOT_DISCARD_DESTROY_OR_TRADE_AN_ITEM);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Item item = pet.getInventory().getItemByObjectId(_objectId);
		
		// No UseItem is allowed while the player is in special conditions
		if (player.hasBlockActions() || player.isControlBlocked() || player.isAlikeDead())
		{
			return;
		}
		
		// Char cannot use item when dead
		if (player.isDead() || pet.isDead() || !player.getInventory().canManipulateWithItemId(item.getId()))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
			sm.addItemName(item);
			player.sendPacket(sm);
			return;
		}
		
		if (!item.isEquipable())
		{
			return;
		}
		
		_itemId = item.getId();
		if (player.isFishing() && ((_itemId < 6535) || (_itemId > 6540)))
		{
			// You cannot do anything else while fishing
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_3);
			return;
		}
		
		player.onActionRequest();
		
		if (item.isEquipable())
		{
			if (pet.getInventory().isItemSlotBlocked(item.getTemplate().getBodyPart()))
			{
				player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
				return;
			}
			
			switch (item.getTemplate().getBodyPart())
			{
				case LR_HAND:
				case L_HAND:
				case R_HAND:
				{
					if ((player.getActiveWeaponItem() != null) && (player.getActiveWeaponItem().getId() == FortManager.ORC_FORTRESS_FLAG))
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
						return;
					}
					break;
				}
				case DECO:
				{
					if (!item.isEquipped() && (player.getInventory().getTalismanSlots() == 0))
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
						return;
					}
					break;
				}
				case BROOCH_JEWEL:
				{
					if (!item.isEquipped() && (player.getInventory().getBroochJewelSlots() == 0))
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_CANNOT_EQUIP_S1_WITHOUT_EQUIPPING_A_BROOCH);
						sm.addItemName(item);
						player.sendPacket(sm);
						return;
					}
					break;
				}
				case AGATHION:
				{
					if (!item.isEquipped() && (player.getInventory().getAgathionSlots() == 0))
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
						return;
					}
					break;
				}
				case ARTIFACT:
				{
					if (!item.isEquipped() && (player.getInventory().getArtifactSlots() == 0))
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
						sm.addItemName(item);
						player.sendPacket(sm);
						return;
					}
					break;
				}
			}
			
			if (player.isCastingNow())
			{
				// Create and Bind the next action to the AI.
				player.getAI().setNextAction(new NextAction(Action.FINISH_CASTING, Intention.CAST, () ->
				{
					pet.transferItem(ItemProcessType.TRANSFER, item.getObjectId(), 1, player.getInventory(), player, null);
					sendInfos(pet, player);
				}));
			}
			else if (player.isAttackingNow())
			{
				// Equip or unEquip.
				pet.transferItem(ItemProcessType.TRANSFER, item.getObjectId(), 1, player.getInventory(), player, null);
				sendInfos(pet, player);
			}
			else
			{
				pet.transferItem(ItemProcessType.TRANSFER, item.getObjectId(), 1, player.getInventory(), player, null);
				sendInfos(pet, player);
			}
		}
	}
	
	private void sendInfos(Pet pet, Player player)
	{
		pet.getStat().recalculateStats(true);
		player.sendPacket(new PetSummonInfo(pet, 1));
		player.sendPacket(new ExPetSkillList(false, pet));
	}
}
