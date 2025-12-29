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
package org.l2jmobius.gameserver.network.serverpackets.penaltyitemdrop;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemPenaltyHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

public class ExPenaltyItemList extends ServerPacket
{
	private final Player _player;
	
	public ExPenaltyItemList(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_PENALTY_ITEM_LIST.writeId(this, buffer);
		if (_player.getItemPenaltyList().isEmpty())
		{
			buffer.writeInt(0);
		}
		else
		{
			buffer.writeInt(_player.getItemPenaltyList().size());
			final List<ItemPenaltyHolder> listItems = new ArrayList<>(_player.getItemPenaltyList());
			for (ItemPenaltyHolder holder : listItems.reversed())
			{
				final Item item = _player.getItemPenalty().getItemByObjectId(holder.getItemObjectId());
				buffer.writeInt(item.getObjectId()); // On/off unknown value specific to an item, possibly ID used to restore the global list
				buffer.writeInt((int) (holder.getDateLost().getTime() / 1000)); // Time when the item was lost, in seconds
				buffer.writeLong(FeatureConfig.ITEM_PENALTY_RESTORE_ADENA); // Adena price for restoration
				buffer.writeInt(FeatureConfig.ITEM_PENALTY_RESTORE_LCOIN); // Lcoin price for restoration
				buffer.writeInt(49); // Unknown value, observed as 49 on JP servers; 0 causes the item not to display, misaligning subsequent rows
				buffer.writeInt(0); // Unknown
				buffer.writeShort(0); // Unknown
				buffer.writeInt(item.getId()); // Item ID
				buffer.writeByte(0); // Unknown
				buffer.writeLong(item.getCount()); // Item count
				buffer.writeInt(1); // Unknown
				buffer.writeInt(64); // Observed as 64 on JP servers; second value can be 4096. 0 causes display issues for the item and misalignment
				buffer.writeInt(0); // Unknown
				buffer.writeInt(item.getEnchantLevel()); // Enchant level
				buffer.writeShort(0); // Unknown
				buffer.writeByte(0); // Unknown
				buffer.writeInt(-9999); // Observed as -9999 on JP servers; purpose unclear
				buffer.writeShort(1); // Unknown
				buffer.writeByte(0); // Unknown
				buffer.writeInt(item.getObjectId()); // On/off unknown value specific to the item, possibly ID used to restore the global list
			}
		}
	}
}
