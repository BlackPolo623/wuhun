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
package org.l2jmobius.gameserver.network.serverpackets.equipmentupgradenormal;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.EquipmentUpgradeNormalData;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.AbstractItemPacket;

/**
 * @author Index
 */
public class ExShowUpgradeSystemNormal extends AbstractItemPacket
{
	private final int _mode;
	private final int _type;
	private final int _commission;
	private final List<Integer> _materials = new ArrayList<>();
	private final List<Integer> _discountRatio = new ArrayList<>();
	
	public ExShowUpgradeSystemNormal(int mode, int type)
	{
		_mode = mode;
		_type = type;
		
		final EquipmentUpgradeNormalData data = EquipmentUpgradeNormalData.getInstance();
		_commission = data.getCommission();
		
		for (ItemHolder item : data.getDiscount())
		{
			_materials.add(item.getId());
			_discountRatio.add((int) item.getCount());
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_SHOW_UPGRADE_SYSTEM_NORMAL.writeId(this, buffer);
		buffer.writeShort(_mode);
		buffer.writeShort(_type);
		buffer.writeShort(_commission); // default - 100
		buffer.writeInt(_materials.size()); // array of materials with discount
		for (int id : _materials)
		{
			buffer.writeInt(id);
		}
		
		buffer.writeInt(_discountRatio.size()); // array of discount count
		for (int discount : _discountRatio)
		{
			buffer.writeInt(discount);
		}
	}
}
