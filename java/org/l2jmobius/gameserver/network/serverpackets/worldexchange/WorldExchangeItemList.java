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
package org.l2jmobius.gameserver.network.serverpackets.worldexchange;

import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.model.item.enums.WorldExchangeItemSubType;
import org.l2jmobius.gameserver.model.item.holders.WorldExchangeHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Index, Brado
 */
public class WorldExchangeItemList extends ServerPacket
{
	private static final int ITEMS_PER_PAGE = 100;
	
	private final List<WorldExchangeHolder> _holders;
	private final WorldExchangeItemSubType _type;
	private final int _page;
	
	public WorldExchangeItemList(List<WorldExchangeHolder> holders, WorldExchangeItemSubType type, int page)
	{
		_holders = holders;
		_type = type;
		_page = page;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_WORLD_EXCHANGE_ITEM_LIST.writeId(this, buffer);
		
		final int totalPages = (int) Math.ceil((double) _holders.size() / ITEMS_PER_PAGE);
		final int startIndex = (_page == 0) ? 0 : (_page - 1) * ITEMS_PER_PAGE;
		final int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, _holders.size());
		if (_holders.isEmpty() || (_page > totalPages))
		{
			buffer.writeShort(0); // Category
			buffer.writeByte(0); // SortType
			buffer.writeInt(_page);
			buffer.writeInt(0); // ItemIDList
			return;
		}
		
		buffer.writeShort(_type.getId());
		buffer.writeByte(0);
		buffer.writeInt(_page);
		buffer.writeInt(endIndex - startIndex);
		for (int i = startIndex; i < endIndex; i++)
		{
			getItemInfo(buffer, _holders.get(i));
		}
	}
	
	private void getItemInfo(WritableBuffer buffer, WorldExchangeHolder holder)
	{
		buffer.writeLong(holder.getWorldExchangeId());
		buffer.writeLong(holder.getPrice());
		buffer.writeInt((int) (holder.getEndTime() / 1000L));
		
		final Item item = holder.getItemInstance();
		buffer.writeInt(item.getId());
		buffer.writeLong(item.getCount());
		buffer.writeInt(item.getEnchantLevel() < 1 ? 0 : item.getEnchantLevel());
		
		final VariationInstance augment = item.getAugmentation();
		if (augment != null)
		{
			buffer.writeInt(augment.getOption1Id());
			buffer.writeInt(augment.getOption2Id());
			buffer.writeInt(augment.getOption3Id());
		}
		else
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
		
		buffer.writeInt(-1);
		buffer.writeShort(item.getAttackAttribute() != null ? item.getAttackAttribute().getType().getClientId() : 0);
		buffer.writeShort(item.getAttackAttribute() != null ? item.getAttackAttribute().getValue() : 0);
		buffer.writeShort(item.getDefenceAttribute(AttributeType.FIRE));
		buffer.writeShort(item.getDefenceAttribute(AttributeType.WATER));
		buffer.writeShort(item.getDefenceAttribute(AttributeType.WIND));
		buffer.writeShort(item.getDefenceAttribute(AttributeType.EARTH));
		buffer.writeShort(item.getDefenceAttribute(AttributeType.HOLY));
		buffer.writeShort(item.getDefenceAttribute(AttributeType.DARK));
		buffer.writeInt(item.getVisualId());
		
		final List<EnsoulOption> soul = (List<EnsoulOption>) holder.getItemInfo().getSoulCrystalOptions();
		try
		{
			buffer.writeInt(soul != null ? soul.get(0).getId() : 0);
		}
		catch (IndexOutOfBoundsException ignored)
		{
			buffer.writeInt(0);
		}
		
		try
		{
			buffer.writeInt(soul != null ? soul.get(1).getId() : 0);
		}
		catch (IndexOutOfBoundsException ignored)
		{
			buffer.writeInt(0);
		}
		
		final List<EnsoulOption> specialSoul = (List<EnsoulOption>) holder.getItemInfo().getSoulCrystalSpecialOptions();
		try
		{
			buffer.writeInt(specialSoul != null ? specialSoul.get(0).getId() : 0);
		}
		catch (IndexOutOfBoundsException ignored)
		{
			buffer.writeInt(0);
		}
		
		buffer.writeShort(item.isBlessed());
	}
}
