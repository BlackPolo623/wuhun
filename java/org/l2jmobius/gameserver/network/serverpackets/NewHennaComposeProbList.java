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

import java.util.List;
import java.util.stream.Collectors;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.HennaCombinationData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.henna.CombinationHenna;
import org.l2jmobius.gameserver.model.item.henna.Henna;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius, smo2015
 */
public class NewHennaComposeProbList extends ServerPacket
{
	private final Player _player;
	
	public NewHennaComposeProbList(Player player)
	{
		_player = player;
	}

	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		final int allHennaSlots = _player.getAvailableHennaSlots();
		final int emptyHennaSlots = _player.getHennaEmptySlots();

		ServerPackets.EX_NEW_HENNA_COMPOSE_PROB_LIST.writeId(this, buffer);
		buffer.writeInt(allHennaSlots - emptyHennaSlots);
		for (int slot = 1; slot <= allHennaSlots; slot++)
		{
			final Henna henna = _player.getHenna(slot);
			if (henna != null)
			{
				// 獲取該染料的所有配置
				final List<CombinationHenna> hennaList = HennaCombinationData.getInstance().getAllByHenna(henna.getDyeId());

				buffer.writeInt(henna.getDyeId());
				buffer.writeInt(hennaList.size());

				for (CombinationHenna item : hennaList)
				{
					// 優先顯示 itemTwo，如果沒有則顯示 itemOne
					int displayItemId = item.getItemTwo() != -1 ? item.getItemTwo() : item.getItemOne();
					buffer.writeInt(displayItemId);
					buffer.writeInt((int) (item.getChance() * 100));
				}
			}
		}
	}
}
