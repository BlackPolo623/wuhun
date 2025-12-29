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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.EquipmentUpgradeHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Mobius
 */
public class EquipmentUpgradeData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EquipmentUpgradeData.class.getName());
	
	private static final Map<Integer, EquipmentUpgradeHolder> _upgrades = new HashMap<>();
	
	protected EquipmentUpgradeData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_upgrades.clear();
		parseDatapackFile("data/EquipmentUpgradeData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _upgrades.size() + " upgrade equipment data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "upgrade", upgradeNode ->
		{
			final StatSet set = new StatSet(parseAttributes(upgradeNode));
			
			final int id = set.getInt("id");
			final String[] item = set.getString("item").split(",");
			final int requiredItemId = Integer.parseInt(item[0]);
			final int requiredItemEnchant = Integer.parseInt(item[1]);
			
			// Parsing materials.
			final String materials = set.getString("materials", "");
			final List<ItemHolder> materialList = new ArrayList<>();
			if (!materials.isEmpty())
			{
				for (String mat : materials.split(";"))
				{
					final String[] matValues = mat.split(",");
					final int matItemId = Integer.parseInt(matValues[0]);
					final long matItemCount = Long.parseLong(matValues[1]);
					
					if (ItemData.getInstance().getTemplate(matItemId) == null)
					{
						LOGGER.info(getClass().getSimpleName() + ": Material item with id " + matItemId + " does not exist.");
					}
					else
					{
						materialList.add(new ItemHolder(matItemId, matItemCount));
					}
				}
			}
			
			// Additional attributes.
			final long adena = set.getLong("adena", 0);
			final String[] resultItem = set.getString("result").split(",");
			final int resultItemId = Integer.parseInt(resultItem[0]);
			final int resultItemEnchant = Integer.parseInt(resultItem[1]);
			final boolean announce = set.getBoolean("announce", false);
			
			// Validating required item existence.
			if (ItemData.getInstance().getTemplate(requiredItemId) == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Required item with id " + requiredItemId + " does not exist.");
			}
			else // Creating and storing the upgrade entry.
			{
				final EquipmentUpgradeHolder upgrade = new EquipmentUpgradeHolder(id, requiredItemId, requiredItemEnchant, materialList, adena, resultItemId, resultItemEnchant, announce);
				_upgrades.put(id, upgrade);
			}
		}));
	}
	
	/**
	 * Retrieves the {@link EquipmentUpgradeHolder} associated with the specified upgrade ID.
	 * @param id the unique identifier of the upgrade to retrieve
	 * @return the {@link EquipmentUpgradeHolder} for the specified ID, or {@code null} if no upgrade exists with that ID
	 */
	public EquipmentUpgradeHolder getUpgrade(int id)
	{
		return _upgrades.get(id);
	}
	
	public static EquipmentUpgradeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EquipmentUpgradeData INSTANCE = new EquipmentUpgradeData();
	}
}
