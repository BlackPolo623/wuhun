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

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.henna.HennaPoten;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.newhenna.NewHennaPotenOpenslot;

/**
 * @author Liamxroy, smo2015
 */
public class RequestNewHennaPotenOpenslot extends ClientPacket
{
	private int _slotId;
	private int _reqOpenSlotStep;
	
	@Override
	protected void readImpl()
	{
		_slotId = readInt();
		_reqOpenSlotStep = readInt();
		readInt(); // feeItemId
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final HennaPoten henna = player.getHennaPotenList()[_slotId - 1];
		if ((_reqOpenSlotStep > 0) && (_reqOpenSlotStep < 31))
		{
			if ((henna.getUnlockSlot() + 1) == _reqOpenSlotStep)
			{
				henna.setUnlockSlot(_reqOpenSlotStep);
				player.sendPacket(new NewHennaPotenOpenslot(true, _slotId, _reqOpenSlotStep, henna.getActiveStep()));
				player.applyDyePotenSkills();
			}
			else
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.sendPacket(new NewHennaPotenOpenslot(false, _slotId, _reqOpenSlotStep, henna.getActiveStep()));
			}
		}
	}
}
