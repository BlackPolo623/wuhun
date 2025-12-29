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

import java.util.Collections;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.item.holders.UniqueItemEnchantHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.AbstractItemPacket;

/**
 * @author Index
 */
public class ExUpgradeSystemNormalResult extends AbstractItemPacket
{
	public static final ExUpgradeSystemNormalResult FAIL = new ExUpgradeSystemNormalResult(0, 0, false, Collections.emptyList(), Collections.emptyList());
	
	private final int _result;
	private final int _upgradeId;
	private final boolean _success;
	private final List<UniqueItemEnchantHolder> _resultItems;
	private final List<UniqueItemEnchantHolder> _bonusItems;
	
	public ExUpgradeSystemNormalResult(int result, int upgradeId, boolean success, List<UniqueItemEnchantHolder> resultItems, List<UniqueItemEnchantHolder> bonusItems)
	{
		_result = result;
		_upgradeId = upgradeId;
		_success = success;
		_resultItems = resultItems;
		_bonusItems = bonusItems;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_UPGRADE_SYSTEM_NORMAL_RESULT.writeId(this, buffer);
		buffer.writeShort(_result); // Result ID
		buffer.writeInt(_upgradeId); // Upgrade ID
		buffer.writeByte(_success); // Success
		buffer.writeInt(_resultItems.size()); // Array of result items (success/failure) start.
		for (UniqueItemEnchantHolder item : _resultItems)
		{
			buffer.writeInt(item.getObjectId());
			buffer.writeInt(item.getId());
			buffer.writeInt(item.getEnchantLevel());
			buffer.writeInt(Math.toIntExact(item.getCount()));
		}
		
		buffer.writeByte(0); // Is bonus? Do not see any effect.
		buffer.writeInt(_bonusItems.size()); // Array of bonus items start.
		for (UniqueItemEnchantHolder bonus : _bonusItems)
		{
			buffer.writeInt(bonus.getObjectId());
			buffer.writeInt(bonus.getId());
			buffer.writeInt(bonus.getEnchantLevel());
			buffer.writeInt(Math.toIntExact(bonus.getCount()));
		}
	}
}
