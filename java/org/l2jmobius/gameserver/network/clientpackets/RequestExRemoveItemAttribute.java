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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.item.Weapon;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExBaseAttributeCancelResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Mobius
 */
public class RequestExRemoveItemAttribute extends ClientPacket
{
	private int _objectId;
	private long _price;
	private byte _element;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
		_element = (byte) readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item targetItem = player.getInventory().getItemByObjectId(_objectId);
		if (targetItem == null)
		{
			return;
		}
		
		final AttributeType type = AttributeType.findByClientId(_element);
		if (type == null)
		{
			return;
		}
		
		if ((targetItem.getAttributes() == null) || (targetItem.getAttribute(type) == null))
		{
			return;
		}
		
		if (player.reduceAdena(ItemProcessType.FEE, getPrice(targetItem), player, true))
		{
			targetItem.clearAttribute(type);
			player.updateUserInfo();
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(targetItem);
			player.sendInventoryUpdate(iu);
			SystemMessage sm;
			final AttributeType realElement = targetItem.isArmor() ? type.getOpposite() : type;
			if (targetItem.getEnchantLevel() > 0)
			{
				if (targetItem.isArmor())
				{
					sm = new SystemMessage(SystemMessageId.S3_POWER_HAS_BEEN_REMOVED_FROM_S1_S2_S4_RESISTANCE_IS_DECREASED);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_S2_S_S3_ATTRIBUTE_HAS_BEEN_REMOVED);
				}
				
				sm.addInt(targetItem.getEnchantLevel());
				sm.addItemName(targetItem);
				sm.addAttribute(realElement.getClientId());
				if (targetItem.isArmor())
				{
					sm.addAttribute(realElement.getClientId());
				}
			}
			else
			{
				if (targetItem.isArmor())
				{
					sm = new SystemMessage(SystemMessageId.S2_POWER_HAS_BEEN_REMOVED_FROM_S1_S3_RESISTANCE_IS_DECREASED);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_S_S2_ATTRIBUTE_HAS_BEEN_REMOVED);
				}
				
				sm.addItemName(targetItem);
				if (targetItem.isArmor())
				{
					sm.addAttribute(realElement.getClientId());
					sm.addAttribute(realElement.getOpposite().getClientId());
				}
			}
			
			player.sendPacket(sm);
			player.sendPacket(new ExBaseAttributeCancelResult(targetItem.getObjectId(), _element));
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_FUNDS_TO_CANCEL_THIS_ATTRIBUTE);
		}
	}
	
	private long getPrice(Item item)
	{
		switch (item.getTemplate().getCrystalType())
		{
			case S:
			{
				if (item.getTemplate() instanceof Weapon)
				{
					_price = 50000;
				}
				else
				{
					_price = 40000;
				}
				break;
			}
			case S80:
			{
				if (item.getTemplate() instanceof Weapon)
				{
					_price = 100000;
				}
				else
				{
					_price = 80000;
				}
				break;
			}
			case S84:
			{
				if (item.getTemplate() instanceof Weapon)
				{
					_price = 200000;
				}
				else
				{
					_price = 160000;
				}
				break;
			}
			case R:
			{
				if (item.getTemplate() instanceof Weapon)
				{
					_price = 400000;
				}
				else
				{
					_price = 320000;
				}
				break;
			}
			case R95:
			{
				if (item.getTemplate() instanceof Weapon)
				{
					_price = 800000;
				}
				else
				{
					_price = 640000;
				}
				break;
			}
			case R99:
			{
				if (item.getTemplate() instanceof Weapon)
				{
					_price = 3200000;
				}
				else
				{
					_price = 2560000;
				}
				break;
			}
		}
		
		return _price;
	}
}
