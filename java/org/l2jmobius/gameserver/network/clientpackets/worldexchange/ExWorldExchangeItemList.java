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
package org.l2jmobius.gameserver.network.clientpackets.worldexchange;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.config.WorldExchangeConfig;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.managers.WorldExchangeManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.WorldExchangeItemSubType;
import org.l2jmobius.gameserver.model.item.enums.WorldExchangeSortType;
import org.l2jmobius.gameserver.model.item.holders.WorldExchangeHolder;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.worldexchange.WorldExchangeItemList;

/**
 * @author Index
 */
public class ExWorldExchangeItemList extends ClientPacket
{
	private int _category;
	private int _sortType;
	private int _page;
	private final List<Integer> _itemIdList = new ArrayList<>();
	
	@Override
	protected void readImpl()
	{
		_category = readShort();
		_sortType = readByte();
		_page = readInt();
		int size = readInt();
		for (int i = 0; i < size; i++)
		{
			_itemIdList.add(readInt());
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final String lang = MultilingualSupportConfig.MULTILANG_ENABLE ? player.getLang() != null ? player.getLang() : WorldExchangeConfig.WORLD_EXCHANGE_DEFAULT_LANG : WorldExchangeConfig.WORLD_EXCHANGE_DEFAULT_LANG;
		if (_itemIdList.isEmpty())
		{
			final List<WorldExchangeHolder> holders = WorldExchangeManager.getInstance().getItemBids(player.getObjectId(), WorldExchangeItemSubType.getWorldExchangeItemSubType(_category), WorldExchangeSortType.getWorldExchangeSortType(_sortType), lang);
			player.sendPacket(new WorldExchangeItemList(holders, WorldExchangeItemSubType.getWorldExchangeItemSubType(_category), _page));
		}
		else
		{
			WorldExchangeManager.getInstance().addCategoryType(_itemIdList, _category);
			final List<WorldExchangeHolder> holders = WorldExchangeManager.getInstance().getItemBids(_itemIdList, WorldExchangeSortType.getWorldExchangeSortType(_sortType), lang);
			player.sendPacket(new WorldExchangeItemList(holders, WorldExchangeItemSubType.getWorldExchangeItemSubType(_category), _page));
		}
	}
}
