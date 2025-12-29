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
package org.l2jmobius.gameserver.network.clientpackets.pledgedonation;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.serverpackets.pledgedonation.ExPledgeDonationInfo;
import org.l2jmobius.gameserver.network.serverpackets.pledgedonation.ExPledgeDonationRequest;

/**
 * @author Berezkin Nikolay
 */
public class RequestExPledgeDonationRequest extends ClientPacket
{
	private int _type;
	
	@Override
	protected void readImpl()
	{
		_type = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return;
		}
		
		switch (_type)
		{
			case 0:
			{
				if (player.reduceAdena(ItemProcessType.FEE, 100000, null, true))
				{
					clan.addExp(player.getObjectId(), 50);
				}
				else
				{
					player.sendPacket(new ExPledgeDonationRequest(false, _type, 2));
				}
				break;
			}
			case 1:
			{
				if (player.getInventory().getInventoryItemCount(Inventory.LCOIN_ID, -1) >= 100)
				{
					player.destroyItemByItemId(ItemProcessType.FEE, Inventory.LCOIN_ID, 100, player, true);
					clan.addExp(player.getObjectId(), 100);
					player.setHonorCoins(player.getHonorCoins() + 100);
				}
				else
				{
					player.sendPacket(new ExPledgeDonationRequest(false, _type, 2));
				}
				break;
			}
			case 2:
			{
				if (player.getInventory().getInventoryItemCount(Inventory.LCOIN_ID, -1) >= 500)
				{
					player.destroyItemByItemId(ItemProcessType.FEE, Inventory.LCOIN_ID, 500, player, true);
					clan.addExp(player.getObjectId(), 500);
					player.setHonorCoins(player.getHonorCoins() + 500);
				}
				else
				{
					player.sendPacket(new ExPledgeDonationRequest(false, _type, 2));
				}
				break;
			}
		}
		
		player.getVariables().set(PlayerVariables.CLAN_DONATION_POINTS, Math.max(player.getClanDonationPoints() - 1, 0));
		criticalSuccess(player, clan, _type);
		player.sendItemList();
		player.sendPacket(new ExPledgeDonationRequest(true, _type, player.getClanDonationPoints()));
		player.sendPacket(new ExPledgeDonationInfo(player.getClanDonationPoints(), true));
	}
	
	private void criticalSuccess(Player player, Clan clan, int type)
	{
		if (type == 1)
		{
			if (Rnd.get(100) < 5)
			{
				player.setHonorCoins(player.getHonorCoins() + 200);
				clan.getMembers().forEach(clanMember -> sendMail(clanMember.getObjectId(), 1, player.getName()));
			}
		}
		else if (type == 2)
		{
			if (Rnd.get(100) < 5)
			{
				player.setHonorCoins(player.getHonorCoins() + 1000);
				clan.getMembers().forEach(clanMember -> sendMail(clanMember.getObjectId(), 5, player.getName()));
			}
		}
	}
	
	private void sendMail(int charId, int amount, String donator)
	{
		final Message msg = new Message(charId, "Clan Rewards for " + donator + " Donation", "The entire clan receives rewards for " + donator + " donation.", MailType.PLEDGE_DONATION_CRITICAL_SUCCESS);
		final Mail attachment = msg.createAttachments();
		attachment.addItem(ItemProcessType.REWARD, 95672, amount, null, donator); // Honor Coin Pouch
		MailManager.getInstance().sendMessage(msg);
	}
}
