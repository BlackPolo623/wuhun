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
package org.l2jmobius.gameserver.network.serverpackets.characterstyle;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.enums.CharacterStyleCategoryType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Brado
 */
public class ExCharacterStyleList extends ServerPacket
{
	private final CharacterStyleCategoryType _type;
	private final List<Integer> _unlockedStyles;
	private final List<Integer> _favoriteStyles;
	private final Map<Integer, Integer> _activeStyles;
	private final ItemHolder _swapCost;
	
	public ExCharacterStyleList(CharacterStyleCategoryType type, ItemHolder swapCost, List<Integer> styles, List<Integer> favoriteStyles, Map<Integer, Integer> activeStyles)
	{
		_type = type;
		_swapCost = swapCost;
		_unlockedStyles = styles;
		_favoriteStyles = favoriteStyles;
		_activeStyles = activeStyles;
	}
	
	@Override
	protected void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_CHARACTER_STYLE_LIST.writeId(this, buffer);
		buffer.writeInt(_type.getClientId());
		buffer.writeInt(_swapCost.getId());
		buffer.writeLong(_swapCost.getCount());
		
		buffer.writeInt(_activeStyles.size());
		for (Entry<Integer, Integer> style : _activeStyles.entrySet())
		{
			buffer.writeInt(style.getKey());
			buffer.writeInt(style.getValue());
		}
		
		buffer.writeInt(_favoriteStyles.size());
		for (int style : _favoriteStyles)
		{
			buffer.writeInt(style);
		}
		
		buffer.writeInt(_unlockedStyles.size());
		for (int style : _unlockedStyles)
		{
			buffer.writeInt(style);
			buffer.writeInt(0); // 541 nCommissionClassID
			buffer.writeLong(0); // 541 nCommissionAmount
		}
	}
}
