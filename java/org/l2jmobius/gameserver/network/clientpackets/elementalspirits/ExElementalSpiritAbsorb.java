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
package org.l2jmobius.gameserver.network.clientpackets.elementalspirits;

import org.l2jmobius.gameserver.model.ElementalSpirit;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.ElementalSpiritType;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ElementalSpiritAbsorbItemHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.elementalspirits.ElementalSpiritAbsorb;

/**
 * @author JoeAlisson
 */
public class ExElementalSpiritAbsorb extends ClientPacket
{
	private byte _type;
	private int _itemId;
	private int _amount;
	
	@Override
	protected void readImpl()
	{
		_type = readByte();
		readInt(); // items for now is always 1
		_itemId = readInt();
		_amount = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final ElementalSpirit spirit = player.getElementalSpirit(ElementalSpiritType.of(_type));
		if (spirit == null)
		{
			player.sendPacket(SystemMessageId.NO_SPIRITS_ARE_AVAILABLE);
			return;
		}
		
		final ElementalSpiritAbsorbItemHolder absorbItem = spirit.getAbsorbItem(_itemId);
		if (absorbItem == null)
		{
			player.sendPacket(new ElementalSpiritAbsorb(player, _type, false));
			return;
		}
		
		final boolean canAbsorb = checkConditions(player, spirit);
		if (canAbsorb)
		{
			player.sendPacket(SystemMessageId.SUCCESSFUL_ABSORPTION);
			spirit.addExperience(absorbItem.getExperience() * _amount);
			final UserInfo userInfo = new UserInfo(player);
			userInfo.addComponentType(UserInfoType.ATT_SPIRITS);
			player.sendPacket(userInfo);
		}
		
		player.sendPacket(new ElementalSpiritAbsorb(player, _type, canAbsorb));
	}
	
	private boolean checkConditions(Player player, ElementalSpirit spirit)
	{
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.CANNOT_EVOLVE_ABSORB_EXTRACT_WHILE_USING_THE_PRIVATE_STORE_WORKSHOP);
			return false;
		}
		
		if (player.isInBattle())
		{
			player.sendPacket(SystemMessageId.UNABLE_TO_ABSORB_DURING_BATTLE);
			return false;
		}
		
		if ((spirit.getLevel() == spirit.getMaxLevel()) && (spirit.getExperience() == spirit.getExperienceToNextLevel()))
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_REACHED_THE_MAXIMUM_LEVEL_AND_CANNOT_ABSORB_ANY_FURTHER);
			return false;
		}
		
		if ((_amount < 1) || !player.destroyItemByItemId(ItemProcessType.FEE, _itemId, _amount, player, true))
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_INGREDIENTS_TO_ABSORB);
			return false;
		}
		
		return true;
	}
}
