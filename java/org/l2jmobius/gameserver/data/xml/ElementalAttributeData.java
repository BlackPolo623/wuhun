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
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.AttributeSystemConfig;
import org.l2jmobius.gameserver.data.holders.ElementalItemHolder;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.item.enums.ElementalItemType;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * @author Mobius
 */
public class ElementalAttributeData implements IXmlReader
{
	private static final Map<Integer, ElementalItemHolder> ELEMENTAL_ITEMS = new HashMap<>();
	
	public static final int FIRST_WEAPON_BONUS = 20;
	public static final int NEXT_WEAPON_BONUS = 5;
	public static final int ARMOR_BONUS = 6;
	
	public static final int[] WEAPON_VALUES =
	{
		0, // Level 1
		25, // Level 2
		75, // Level 3
		150, // Level 4
		175, // Level 5
		225, // Level 6
		300, // Level 7
		325, // Level 8
		375, // Level 9
		450, // Level 10
		475, // Level 11
		525, // Level 12
		600, // Level 13
		Integer.MAX_VALUE
		// TODO: Higher stones
	};
	
	public static final int[] ARMOR_VALUES =
	{
		0, // Level 1
		12, // Level 2
		30, // Level 3
		60, // Level 4
		72, // Level 5
		90, // Level 6
		120, // Level 7
		132, // Level 8
		150, // Level 9
		180, // Level 10
		192, // Level 11
		210, // Level 12
		240, // Level 13
		Integer.MAX_VALUE
		// TODO: Higher stones
	};
	
	/* @formatter:off */
	private static final int[][] CHANCE_TABLE =
	{
		{AttributeSystemConfig.S_WEAPON_STONE,		AttributeSystemConfig.S_ARMOR_STONE,		AttributeSystemConfig.S_WEAPON_CRYSTAL,	AttributeSystemConfig.S_ARMOR_CRYSTAL,		AttributeSystemConfig.S_WEAPON_STONE_SUPER,	AttributeSystemConfig.S_ARMOR_STONE_SUPER,		AttributeSystemConfig.S_WEAPON_CRYSTAL_SUPER,		AttributeSystemConfig.S_ARMOR_CRYSTAL_SUPER,		AttributeSystemConfig.S_WEAPON_JEWEL,		AttributeSystemConfig.S_ARMOR_JEWEL},
		{AttributeSystemConfig.S80_WEAPON_STONE,	AttributeSystemConfig.S80_ARMOR_STONE,		AttributeSystemConfig.S80_WEAPON_CRYSTAL,	AttributeSystemConfig.S80_ARMOR_CRYSTAL,	AttributeSystemConfig.S80_WEAPON_STONE_SUPER,	AttributeSystemConfig.S80_ARMOR_STONE_SUPER,	AttributeSystemConfig.S80_WEAPON_CRYSTAL_SUPER,	AttributeSystemConfig.S80_ARMOR_CRYSTAL_SUPER,		AttributeSystemConfig.S80_WEAPON_JEWEL,	AttributeSystemConfig.S80_ARMOR_JEWEL},
		{AttributeSystemConfig.S84_WEAPON_STONE,	AttributeSystemConfig.S84_ARMOR_STONE,		AttributeSystemConfig.S84_WEAPON_CRYSTAL,	AttributeSystemConfig.S84_ARMOR_CRYSTAL,	AttributeSystemConfig.S84_WEAPON_STONE_SUPER,	AttributeSystemConfig.S84_ARMOR_STONE_SUPER,	AttributeSystemConfig.S84_WEAPON_CRYSTAL_SUPER,	AttributeSystemConfig.S84_ARMOR_CRYSTAL_SUPER,		AttributeSystemConfig.S84_WEAPON_JEWEL,	AttributeSystemConfig.S84_ARMOR_JEWEL},
		{AttributeSystemConfig.R_WEAPON_STONE,		AttributeSystemConfig.R_ARMOR_STONE,		AttributeSystemConfig.R_WEAPON_CRYSTAL,	AttributeSystemConfig.R_ARMOR_CRYSTAL,		AttributeSystemConfig.R_WEAPON_STONE_SUPER,	AttributeSystemConfig.R_ARMOR_STONE_SUPER,		AttributeSystemConfig.R_WEAPON_CRYSTAL_SUPER,		AttributeSystemConfig.R_ARMOR_CRYSTAL_SUPER,		AttributeSystemConfig.R_WEAPON_JEWEL,		AttributeSystemConfig.R_ARMOR_JEWEL},
		{AttributeSystemConfig.R95_WEAPON_STONE,	AttributeSystemConfig.R95_ARMOR_STONE,		AttributeSystemConfig.R95_WEAPON_CRYSTAL,	AttributeSystemConfig.R95_ARMOR_CRYSTAL,	AttributeSystemConfig.R95_WEAPON_STONE_SUPER,	AttributeSystemConfig.R95_ARMOR_STONE_SUPER,	AttributeSystemConfig.R95_WEAPON_CRYSTAL_SUPER,	AttributeSystemConfig.R95_ARMOR_CRYSTAL_SUPER,		AttributeSystemConfig.R95_WEAPON_JEWEL,	AttributeSystemConfig.R95_ARMOR_JEWEL},
		{AttributeSystemConfig.R99_WEAPON_STONE,	AttributeSystemConfig.R99_ARMOR_STONE,		AttributeSystemConfig.R99_WEAPON_CRYSTAL,	AttributeSystemConfig.R99_ARMOR_CRYSTAL,	AttributeSystemConfig.R99_WEAPON_STONE_SUPER,	AttributeSystemConfig.R99_ARMOR_STONE_SUPER,	AttributeSystemConfig.R99_WEAPON_CRYSTAL_SUPER,	AttributeSystemConfig.R99_ARMOR_CRYSTAL_SUPER,		AttributeSystemConfig.R99_WEAPON_JEWEL,	AttributeSystemConfig.R99_ARMOR_JEWEL},
	};	
	/* @formatter:on */
	
	protected ElementalAttributeData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		ELEMENTAL_ITEMS.clear();
		parseDatapackFile("data/ElementalAttributeData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + ELEMENTAL_ITEMS.size() + " elemental attribute items.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "item", itemNode ->
		{
			final StatSet set = new StatSet(parseAttributes(itemNode));
			
			final int id = set.getInt("id");
			if (ItemData.getInstance().getTemplate(id) == null)
			{
				LOGGER.info(getClass().getSimpleName() + ": Could not find item with id " + id + ".");
				return;
			}
			
			ELEMENTAL_ITEMS.put(id, new ElementalItemHolder(id, set.getEnum("elemental", AttributeType.class), set.getEnum("type", ElementalItemType.class), set.getInt("power", 0)));
		}));
	}
	
	public AttributeType getItemElement(int itemId)
	{
		final ElementalItemHolder item = ELEMENTAL_ITEMS.get(itemId);
		if (item != null)
		{
			return item.getElement();
		}
		
		return AttributeType.NONE;
	}
	
	public ElementalItemHolder getItemElemental(int itemId)
	{
		return ELEMENTAL_ITEMS.get(itemId);
	}
	
	public int getMaxElementLevel(int itemId)
	{
		final ElementalItemHolder item = ELEMENTAL_ITEMS.get(itemId);
		if (item != null)
		{
			return item.getType().getMaxLevel();
		}
		
		return -1;
	}
	
	public boolean isSuccess(Item item, int stoneId)
	{
		int row = -1;
		int column = -1;
		switch (item.getTemplate().getCrystalType())
		{
			case S:
			{
				row = 0;
				break;
			}
			case S80:
			{
				row = 1;
				break;
			}
			case S84:
			{
				row = 2;
				break;
			}
			case R:
			{
				row = 3;
				break;
			}
			case R95:
			{
				row = 4;
				break;
			}
			case R99:
			{
				row = 5;
				break;
			}
		}
		
		switch (ELEMENTAL_ITEMS.get(stoneId).getType())
		{
			case STONE:
			{
				column = item.isWeapon() ? 0 : 1;
				break;
			}
			case CRYSTAL:
			{
				column = item.isWeapon() ? 2 : 3;
				break;
			}
			case STONE_SUPER:
			{
				column = item.isWeapon() ? 4 : 5;
				break;
			}
			case CRYSTAL_SUPER:
			{
				column = item.isWeapon() ? 6 : 7;
				break;
			}
			case JEWEL:
			{
				column = item.isWeapon() ? 8 : 9;
				break;
			}
		}
		
		if ((row != -1) && (column != -1))
		{
			return Rnd.get(100) < CHANCE_TABLE[row][column];
		}
		
		return true;
	}
	
	public static ElementalAttributeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ElementalAttributeData INSTANCE = new ElementalAttributeData();
	}
}
