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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.combination.CombinationItemType;
import org.l2jmobius.gameserver.model.item.henna.CombinationHenna;
import org.l2jmobius.gameserver.model.item.henna.CombinationHennaReward;

/**
 * @author Index
 */
public class HennaCombinationData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(HennaCombinationData.class.getName());
	
	private final List<CombinationHenna> _henna = new ArrayList<>();
	
	protected HennaCombinationData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_henna.clear();
		parseDatapackFile("data/stats/hennaCombinations.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _henna.size() + " henna combinations.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "henna", hennaNode ->
			{
				final CombinationHenna henna = new CombinationHenna(new StatSet(parseAttributes(hennaNode)));
				forEach(hennaNode, "reward", rewardNode ->
				{
					final int hennaId = parseInteger(rewardNode.getAttributes(), "dyeId");
					final int id = parseInteger(rewardNode.getAttributes(), "id", -1);
					final int count = parseInteger(rewardNode.getAttributes(), "count", 0);
					final CombinationItemType type = parseEnum(rewardNode.getAttributes(), CombinationItemType.class, "type");
					henna.addReward(new CombinationHennaReward(hennaId, id, count, type));
					if ((id != -1) && (ItemData.getInstance().getTemplate(id) == null))
					{
						LOGGER.info(getClass().getSimpleName() + ": Could not find item with id " + id);
					}
					
					if ((hennaId != 0) && (HennaData.getInstance().getHenna(hennaId) == null))
					{
						LOGGER.info(getClass().getSimpleName() + ": Could not find henna with id " + hennaId);
					}
				});
				_henna.add(henna);
			});
		});
	}
	
	public List<CombinationHenna> getHenna()
	{
		return _henna;
	}
	
	public CombinationHenna getByHenna(int hennaId)
	{
		for (CombinationHenna henna : _henna)
		{
			if (henna.getHenna() == hennaId)
			{
				return henna;
			}
		}
		
		return null;
	}
	// 添加這個新方法
	public CombinationHenna getByHennaAndItem(int hennaId, int itemId)
	{
		for (CombinationHenna henna : _henna)
		{
			if (henna.getHenna() == hennaId)
			{
				// 檢查 itemOne 或 itemTwo 是否匹配
				if ((henna.getItemOne() == itemId) || (henna.getItemTwo() == itemId))
				{
					return henna;
				}
			}
		}
		return null;
	}

	// 添加這個方法：獲取某個染料的所有可能配置
	public List<CombinationHenna> getAllByHenna(int hennaId)
	{
		List<CombinationHenna> result = new ArrayList<>();
		for (CombinationHenna henna : _henna)
		{
			if (henna.getHenna() == hennaId)
			{
				result.add(henna);
			}
		}
		return result;
	}
	
	public static final HennaCombinationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HennaCombinationData INSTANCE = new HennaCombinationData();
	}
}
