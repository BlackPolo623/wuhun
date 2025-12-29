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
package org.l2jmobius.gameserver.network.clientpackets.newhenna;

import org.l2jmobius.gameserver.data.xml.HennaPatternPotentialData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.henna.DyePotential;
import org.l2jmobius.gameserver.model.item.henna.HennaPoten;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.newhenna.NewHennaPotenSelect;

/**
 * @author Index, Serenitty
 */
public class RequestNewHennaPotenSelect extends ClientPacket
{
	private int _slotId;
	private int _potenId;
	
	@Override
	protected void readImpl()
	{
		_slotId = readByte();
		_potenId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((_slotId < 1) || (_slotId > player.getHennaPotenList().length))
		{
			return;
		}
		
		final DyePotential potential = HennaPatternPotentialData.getInstance().getPotential(_potenId);
		final HennaPoten hennaPoten = player.getHennaPoten(_slotId);
		if ((potential == null) || (potential.getSlotId() != _slotId))
		{
			player.sendPacket(new NewHennaPotenSelect(_slotId, _potenId, hennaPoten.getActiveStep(), false));
			return;
		}
		
		hennaPoten.setPotenId(_potenId);
		player.sendPacket(new NewHennaPotenSelect(_slotId, _potenId, hennaPoten.getActiveStep(), true));
		player.applyDyePotenSkills();
	}
}
