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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.ExtractableProduct;
import org.l2jmobius.gameserver.model.item.EtcItem;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Smoto
 */
public class CreateItemProbList extends ServerPacket
{
	private final int _itemId;
	private final List<ExtractableProduct> _guaranteedItems;
	private final List<ExtractableProduct> _randomItems;
	
	public CreateItemProbList(int itemId)
	{
		_itemId = itemId;
		_guaranteedItems = new ArrayList<>();
		_randomItems = new ArrayList<>();
		
		final ItemTemplate template = ItemData.getInstance().getTemplate(_itemId);
		if ((template == null) || !template.isEtcItem())
		{
			return;
		}
		
		final List<ExtractableProduct> extractableItems = ((EtcItem) template).getExtractableItems();
		if (extractableItems != null)
		{
			for (ExtractableProduct expi : extractableItems)
			{
				if (expi.getChance() == 100000)
				{
					_guaranteedItems.add(expi);
				}
				else
				{
					_randomItems.add(expi);
				}
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_CREATE_ITEM_PROB_LIST.writeId(this, buffer);
		buffer.writeInt(_itemId);
		
		// Guaranteed Items.
		buffer.writeInt(_guaranteedItems.size());
		for (ExtractableProduct product : _guaranteedItems)
		{
			buffer.writeInt(product.getId()); // ItemId
			buffer.writeInt(product.getMinEnchant());// Enchant
			buffer.writeLong(product.getMin()); // ItemCount
			buffer.writeLong(0); // Chance?
		}
		
		// Random Items.
		buffer.writeInt(_randomItems.size());
		for (ExtractableProduct product : _randomItems)
		{
			buffer.writeInt(product.getId()); // ItemId
			buffer.writeInt(product.getMinEnchant());// Enchant
			buffer.writeLong(product.getMin()); // ItemCount
			buffer.writeLong(0); // Chance?
		}
		
		// Multi Items?
		// buffer.writeInt(_randomItems.size());
		// for (ExtractableProduct product : _randomItems)
		// {
		// buffer.writeInt(product.getId()); // ItemId
		// buffer.writeInt(product.getMinEnchant());// Enchant
		// buffer.writeLong(product.getMin()); // ItemCount
		// buffer.writeLong(0); // Chance?
		// }
		
		buffer.writeInt(0);
	}
}
