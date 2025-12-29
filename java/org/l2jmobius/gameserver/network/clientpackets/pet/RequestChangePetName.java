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

import org.l2jmobius.gameserver.data.sql.PetNameTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * @version $Revision: 1.3.4.4 $ $Date: 2005/04/06 16:13:48 $
 */
public class RequestChangePetName extends ClientPacket
{
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Summon pet = player.getPet();
		if (pet == null)
		{
			return;
		}
		
		if (!pet.isPet())
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_GUARDIAN);
			return;
		}
		
		if ((pet.getName() != null) && !pet.getName().equals(""))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SET_A_NAME_FOR_THE_GUARDIAN);
			return;
		}
		
		if (PetNameTable.getInstance().doesPetNameExist(_name, pet.getTemplate().getId()))
		{
			player.sendPacket(SystemMessageId.THE_NAME_IS_ALREADY_IN_USE_BY_ANOTHER_GUARDIAN);
			return;
		}
		
		if ((_name.length() < 3) || (_name.length() > 16))
		{
			// player.sendPacket(SystemMessageId.YOUR_PET_S_NAME_CAN_BE_UP_TO_8_CHARACTERS_IN_LENGTH);
			player.sendMessage("Your pet's name can be up to 16 characters in length.");
			return;
		}
		
		if (!PetNameTable.getInstance().isValidPetName(_name))
		{
			player.sendPacket(SystemMessageId.THE_GUARDIAN_S_NAME_INCLUDES_PROHIBITED_CHARACTERS);
			return;
		}
		
		pet.setName(_name);
		pet.updateAndBroadcastStatus(1);
	}
}
