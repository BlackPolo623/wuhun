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
package org.l2jmobius.gameserver.network.clientpackets.attributechange;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.item.enchant.attribute.AttributeHolder;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.attributechange.ExChangeAttributeFail;
import org.l2jmobius.gameserver.network.serverpackets.attributechange.ExChangeAttributeOk;

/**
 * @author Mobius
 */
public class RequestChangeAttributeItem extends ClientPacket
{
	private int _consumeItemId;
	private int _itemObjId;
	private int _newElementId;
	
	@Override
	protected void readImpl()
	{
		_consumeItemId = readInt();
		_itemObjId = readInt();
		_newElementId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final PlayerInventory inventory = player.getInventory();
		final Item item = inventory.getItemByObjectId(_itemObjId);
		
		// attempting to destroy item
		if (player.getInventory().destroyItemByItemId(ItemProcessType.FEE, _consumeItemId, 1, player, item) == null)
		{
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			player.sendPacket(ExChangeAttributeFail.STATIC);
			PunishmentManager.handleIllegalPlayerAction(player, player + " tried to change attribute without an attribute change crystal.", GeneralConfig.DEFAULT_PUNISH);
			return;
		}
		
		// get values
		final int oldElementId = item.getAttackAttributeType().getClientId();
		final int elementValue = item.getAttackAttribute().getValue();
		item.clearAllAttributes();
		item.setAttribute(new AttributeHolder(AttributeType.findByClientId(_newElementId), elementValue), true);
		
		// send packets
		final SystemMessage msg = new SystemMessage(SystemMessageId.S1_S_S2_ATTRIBUTE_HAS_SUCCESSFULLY_CHANGED_TO_S3_ATTRIBUTE);
		msg.addItemName(item);
		msg.addAttribute(oldElementId);
		msg.addAttribute(_newElementId);
		player.sendPacket(msg);
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(item);
		for (Item i : player.getInventory().getAllItemsByItemId(_consumeItemId))
		{
			iu.addItem(i);
		}
		
		player.sendInventoryUpdate(iu);
		player.broadcastUserInfo();
		player.sendPacket(ExChangeAttributeOk.STATIC);
	}
}
