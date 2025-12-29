/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.LuckyGameDataHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemPointHolder;

/**
 * @author Sdw
 */
public class LuckyGameData implements IXmlReader
{
	private final Map<Integer, LuckyGameDataHolder> _luckyGame = new HashMap<>();
	private final AtomicInteger _serverPlay = new AtomicInteger();
	
	protected LuckyGameData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_luckyGame.clear();
		parseDatapackFile("data/LuckyGameData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _luckyGame.size() + " lucky game data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "luckygame", rewardNode ->
		{
			final LuckyGameDataHolder holder = new LuckyGameDataHolder(new StatSet(parseAttributes(rewardNode)));
			
			// Parse common_reward items.
			forEach(rewardNode, "common_reward", commonRewardNode -> forEach(commonRewardNode, "item", itemNode ->
			{
				final StatSet stats = new StatSet(parseAttributes(itemNode));
				holder.addCommonReward(new ItemChanceHolder(stats.getInt("id"), stats.getDouble("chance"), stats.getLong("count")));
			}));
			
			// Parse unique_reward items.
			forEach(rewardNode, "unique_reward", uniqueRewardNode -> forEach(uniqueRewardNode, "item", itemNode ->
			{
				holder.addUniqueReward(new ItemPointHolder(new StatSet(parseAttributes(itemNode))));
			}));
			
			// Parse modify_reward items.
			forEach(rewardNode, "modify_reward", uniqueRewardNode ->
			{
				holder.setMinModifyRewardGame(parseInteger(uniqueRewardNode.getAttributes(), "min_game"));
				holder.setMaxModifyRewardGame(parseInteger(uniqueRewardNode.getAttributes(), "max_game"));
				forEach(uniqueRewardNode, "item", itemNode ->
				{
					final StatSet stats = new StatSet(parseAttributes(itemNode));
					holder.addModifyReward(new ItemChanceHolder(stats.getInt("id"), stats.getDouble("chance"), stats.getLong("count")));
				});
			});
			
			// Store holder in _luckyGame map.
			_luckyGame.put(parseInteger(rewardNode.getAttributes(), "index"), holder);
		}));
	}
	
	/**
	 * Returns the count of lucky games available.
	 * @return the number of lucky game entries in the collection.
	 */
	public int getLuckyGameCount()
	{
		return _luckyGame.size();
	}
	
	/**
	 * Retrieves the lucky game data associated with the specified index.
	 * @param index the index of the lucky game to retrieve.
	 * @return the {@code LuckyGameDataHolder} instance associated with the given index, or {@code null} if no entry exists for the specified index.
	 */
	public LuckyGameDataHolder getLuckyGameDataByIndex(int index)
	{
		return _luckyGame.get(index);
	}
	
	/**
	 * Increments the server play counter by one and returns the updated count.
	 * @return the new value of the server play counter after incrementing.
	 */
	public int increaseGame()
	{
		return _serverPlay.incrementAndGet();
	}
	
	/**
	 * Returns the current count of server plays.
	 * @return the current value of the server play counter.
	 */
	public int getServerPlay()
	{
		return _serverPlay.get();
	}
	
	public static LuckyGameData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final LuckyGameData INSTANCE = new LuckyGameData();
	}
}
