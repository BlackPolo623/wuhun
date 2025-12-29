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

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.VariationData;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.OptionDataCategory;
import org.l2jmobius.gameserver.model.options.OptionDataGroup;
import org.l2jmobius.gameserver.model.options.Options;
import org.l2jmobius.gameserver.model.options.Variation;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class VariationProbList extends ServerPacket
{
	private final int _refineryId;
	private final Item _targetItem;
	private final Map<Options, Double> _options1 = new TreeMap<>(Comparator.comparingInt(Options::getId));
	private final Map<Options, Double> _options2 = new TreeMap<>(Comparator.comparingInt(Options::getId));
	private final Map<Options, Double> _options3 = new TreeMap<>(Comparator.comparingInt(Options::getId));
	
	public VariationProbList(int refineryId, Item targetItem)
	{
		_refineryId = refineryId;
		_targetItem = targetItem;
		
		final Variation variation = VariationData.getInstance().getVariation(_refineryId, targetItem);
		final OptionDataGroup group1 = variation.getOptionDataGroup()[0];
		if (group1 != null)
		{
			for (OptionDataCategory category : group1.getCategories())
			{
				for (Entry<Options, Double> entry : category.getOptions().entrySet())
				{
					_options1.put(entry.getKey(), entry.getValue());
				}
			}
		}
		
		final OptionDataGroup group2 = variation.getOptionDataGroup()[1];
		if (group2 != null)
		{
			for (OptionDataCategory category : group2.getCategories())
			{
				for (Entry<Options, Double> entry : category.getOptions().entrySet())
				{
					_options2.put(entry.getKey(), entry.getValue());
				}
			}
		}
		
		final OptionDataGroup group3 = variation.getOptionDataGroup()[2];
		if (group3 != null)
		{
			for (OptionDataCategory category : group3.getCategories())
			{
				for (Entry<Options, Double> entry : category.getOptions().entrySet())
				{
					_options3.put(entry.getKey(), entry.getValue());
				}
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_VARIATION_PROB_LIST.writeId(this, buffer);
		buffer.writeInt(_refineryId);
		buffer.writeInt(_targetItem.getId());
		
		// With 0 values the output is doubled at each checked augmented item.
		buffer.writeInt(1); // CurrentPage
		buffer.writeInt(1); // MaxPage
		
		buffer.writeInt(_options1.size() + _options2.size() + _options3.size());
		for (Entry<Options, Double> entry : _options1.entrySet())
		{
			buffer.writeInt(1);
			buffer.writeInt(entry.getKey().getId());
			buffer.writeLong((long) (entry.getValue() * 1000000));
		}
		
		for (Entry<Options, Double> entry : _options2.entrySet())
		{
			buffer.writeInt(2);
			buffer.writeInt(entry.getKey().getId());
			buffer.writeLong((long) (entry.getValue() * 1000000));
		}
		
		for (Entry<Options, Double> entry : _options3.entrySet())
		{
			buffer.writeInt(3);
			buffer.writeInt(entry.getKey().getId());
			buffer.writeLong((long) (entry.getValue() * 1000000));
		}
	}
}
