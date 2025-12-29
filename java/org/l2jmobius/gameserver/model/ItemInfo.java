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
package org.l2jmobius.gameserver.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.l2jmobius.gameserver.data.xml.PetDataTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.actor.holders.creature.PetEvolveHolder;
import org.l2jmobius.gameserver.model.buylist.Product;
import org.l2jmobius.gameserver.model.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.WarehouseItem;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * Get all information from Item to generate ItemInfo.
 */
public class ItemInfo
{
	/** Identifier of the Item */
	private int _objectId;
	
	/** The Item template of the Item */
	private ItemTemplate _item;
	
	/** The level of enchant on the Item */
	private int _enchantLevel;
	
	/** The augmentation of the item */
	private VariationInstance _augmentation;
	
	/** The quantity of Item */
	private long _count;
	
	/** The price of the Item */
	private int _price;
	
	/** The custom Item types (used loto, race tickets) */
	private int _type1;
	private int _type2;
	
	/** If True the Item is equipped */
	private int _equipped;
	private boolean _petEquipped;
	
	/** The action to do clientside (1=ADD, 2=MODIFY, 3=REMOVE) */
	private int _change;
	
	/** The mana of this item */
	private int _mana;
	private int _time;
	private boolean _isBlessed = false;
	private boolean _available = true;
	
	private int _location;
	
	private byte _elemAtkType = -2;
	private int _elemAtkPower = 0;
	private final int[] _attributeDefence =
	{
		0,
		0,
		0,
		0,
		0,
		0
	};
	
	private int[] _option;
	private Collection<EnsoulOption> _soulCrystalOptions;
	private Collection<EnsoulOption> _soulCrystalSpecialOptions;
	private int _visualId;
	private long _visualExpiration;
	
	private int _reuseDelay;
	private Player _owner;
	
	private PetEvolveHolder _petData;
	
	/**
	 * Get all information from Item to generate ItemInfo.
	 * @param item
	 */
	public ItemInfo(Item item)
	{
		Objects.requireNonNull(item);
		
		// Get the Identifier of the Item
		_objectId = item.getObjectId();
		
		// Get the Item of the Item
		_item = item.getTemplate();
		
		// Get the enchant level of the Item
		_enchantLevel = item.getEnchantLevel();
		
		// Get the augmentation bonus
		_augmentation = item.getAugmentation();
		
		// Get the quantity of the Item
		_count = item.getCount();
		
		// Get custom item types (used loto, race tickets)
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		
		// Verify if the Item is equipped
		_equipped = item.isEquipped() ? 1 : 0;
		_petEquipped = item.isPetEquipped();
		
		// Get the action to do clientside
		switch (item.getLastChange())
		{
			case Item.ADDED:
			{
				_change = 1;
				break;
			}
			case Item.MODIFIED:
			{
				_change = 2;
				break;
			}
			case Item.REMOVED:
			{
				_change = 3;
				break;
			}
		}
		
		// Get shadow item mana
		_mana = item.getMana();
		_time = item.isTimeLimitedItem() ? (int) (item.getRemainingTime() / 1000) : -9999;
		_available = item.isAvailable();
		_location = item.getLocationSlot();
		_elemAtkType = item.getAttackAttributeType().getClientId();
		_elemAtkPower = item.getAttackAttributePower();
		for (AttributeType type : AttributeType.ATTRIBUTE_TYPES)
		{
			_attributeDefence[type.getClientId()] = item.getDefenceAttribute(type);
		}
		
		_isBlessed = item.isBlessed();
		_option = item.getEnchantOptions();
		_soulCrystalOptions = item.getSpecialAbilities();
		_soulCrystalSpecialOptions = item.getAdditionalSpecialAbilities();
		_visualId = item.getVisualId();
		_visualExpiration = item.getVisualLifeTime() > 0 ? (item.getVisualLifeTime() - System.currentTimeMillis()) / 1000 : 0;
		_reuseDelay = item.getReuseDelay();
		_owner = item.asPlayer();
		_petData = _owner != null ? PetDataTable.getInstance().getPetDataByItemId(item.getId()) != null ? _owner.getPetEvolve(getObjectId()) : null : null;
	}
	
	public ItemInfo(Item item, int change)
	{
		this(item);
		_change = change;
		_visualExpiration = item.getVisualLifeTime() > 0 ? (item.getVisualLifeTime() - System.currentTimeMillis()) / 1000 : 0;
	}
	
	public ItemInfo(TradeItem item)
	{
		if (item == null)
		{
			return;
		}
		
		// Get the Identifier of the Item
		_objectId = item.getObjectId();
		
		// Get the Item of the Item
		_item = item.getItem();
		
		// Get the enchant level of the Item
		_enchantLevel = item.getEnchant();
		
		// Get the augmentation bonus
		if ((item.getAugmentationOption1() > 0) || (item.getAugmentationOption2() > 0) || (item.getAugmentationOption3() > 0))
		{
			_augmentation = new VariationInstance(0, item.getAugmentationOption1(), item.getAugmentationOption2(), item.getAugmentationOption3());
		}
		
		// Get the quantity of the Item
		_count = item.getCount();
		
		// Get custom item types (used loto, race tickets)
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		
		// Verify if the Item is equipped
		_equipped = 0;
		_petEquipped = false;
		
		// Get the action to do clientside
		_change = 0;
		
		// Get shadow item mana
		_mana = -1;
		_time = -9999;
		_location = item.getLocationSlot();
		_elemAtkType = item.getAttackElementType();
		_elemAtkPower = item.getAttackElementPower();
		for (byte i = 0; i < 6; i++)
		{
			_attributeDefence[i] = item.getElementDefAttr(i);
		}
		
		_option = item.getEnchantOptions();
		_soulCrystalOptions = item.getSoulCrystalOptions();
		_soulCrystalSpecialOptions = item.getSoulCrystalSpecialOptions();
		_visualId = item.getVisualId();
		_isBlessed = item.isBlessed();
	}
	
	public ItemInfo(Product item)
	{
		if (item == null)
		{
			return;
		}
		
		// Get the Identifier of the Item
		_objectId = 0;
		
		// Get the Item of the Item
		_item = item.getItem();
		
		// Get the enchant level of the Item
		_enchantLevel = 0;
		
		// Get the augmentation bonus
		_augmentation = null;
		
		// Get the quantity of the Item
		_count = item.getCount();
		
		// Get custom item types (used loto, race tickets)
		_type1 = item.getItem().getType1();
		_type2 = item.getItem().getType2();
		
		// Verify if the Item is equipped
		_equipped = 0;
		_petEquipped = false;
		
		// Get the action to do clientside
		_change = 0;
		
		// Get shadow item mana
		_mana = -1;
		_time = -9999;
		_location = 0;
		_soulCrystalOptions = Collections.emptyList();
		_soulCrystalSpecialOptions = Collections.emptyList();
	}
	
	public ItemInfo(WarehouseItem item)
	{
		if (item == null)
		{
			return;
		}
		
		// Get the Identifier of the Item
		_objectId = item.getObjectId();
		
		// Get the Item of the Item
		_item = item.getItem();
		
		// Get the enchant level of the Item
		_enchantLevel = item.getEnchantLevel();
		
		// Get the augmentation bonus
		_augmentation = item.getAugmentation();
		
		// Get the quantity of the Item
		_count = item.getCount();
		
		// Get custom item types (used loto, race tickets)
		_type1 = item.getCustomType1();
		_type2 = item.getCustomType2();
		
		// Verify if the Item is equipped
		_equipped = 0;
		_petEquipped = false;
		
		// Get shadow item mana
		_mana = item.getMana();
		_time = item.getTime();
		_location = item.getLocationSlot();
		_elemAtkType = item.getAttackElementType();
		_elemAtkPower = item.getAttackElementPower();
		for (byte i = 0; i < 6; i++)
		{
			_attributeDefence[i] = item.getElementDefAttr(i);
		}
		
		_option = item.getEnchantOptions();
		_soulCrystalOptions = item.getSoulCrystalOptions();
		_soulCrystalSpecialOptions = item.getSoulCrystalSpecialOptions();
		_isBlessed = item.isBlessed();
	}
	
	public int getObjectId()
	{
		return _objectId;
	}
	
	public ItemTemplate getItem()
	{
		return _item;
	}
	
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	public VariationInstance getAugmentation()
	{
		return _augmentation;
	}
	
	public long getCount()
	{
		return _count;
	}
	
	public int getPrice()
	{
		return _price;
	}
	
	public int getCustomType1()
	{
		return _type1;
	}
	
	public int getCustomType2()
	{
		return _type2;
	}
	
	public int getEquipped()
	{
		return _equipped;
	}
	
	public boolean isPetEquipped()
	{
		return _petEquipped;
	}
	
	public int getChange()
	{
		return _change;
	}
	
	public int getMana()
	{
		return _mana;
	}
	
	public int getTime()
	{
		return _time > 0 ? _time : _visualExpiration > 0 ? (int) _visualExpiration : -9999;
	}
	
	public boolean isAvailable()
	{
		return _available;
	}
	
	public int getLocation()
	{
		return _location;
	}
	
	public int getAttackElementType()
	{
		return _elemAtkType;
	}
	
	public int getAttackElementPower()
	{
		return _elemAtkPower;
	}
	
	public int getAttributeDefence(AttributeType attribute)
	{
		return _attributeDefence[attribute.getClientId()];
	}
	
	public int[] getEnchantOptions()
	{
		return _option;
	}
	
	public int getVisualId()
	{
		return _visualId;
	}
	
	public Collection<EnsoulOption> getSoulCrystalOptions()
	{
		return _soulCrystalOptions != null ? _soulCrystalOptions : Collections.emptyList();
	}
	
	public boolean soulCrystalOptionsMatch(EnsoulOption[] soulCrystalOptions)
	{
		if ((_soulCrystalOptions == null))
		{
			return false;
		}
		
		for (EnsoulOption soulCrystalOption1 : _soulCrystalOptions)
		{
			boolean found = false;
			SEARCH: for (EnsoulOption soulCrystalOption2 : soulCrystalOptions)
			{
				if (soulCrystalOption1.equals(soulCrystalOption2))
				{
					found = true;
					break SEARCH;
				}
			}
			
			if (!found)
			{
				return false;
			}
		}
		
		return true;
	}
	
	public Collection<EnsoulOption> getSoulCrystalSpecialOptions()
	{
		return _soulCrystalSpecialOptions != null ? _soulCrystalSpecialOptions : Collections.emptyList();
	}
	
	public boolean soulCrystalSpecialOptionsMatch(EnsoulOption[] soulCrystalSpecialOptions)
	{
		if (_soulCrystalSpecialOptions == null)
		{
			return false;
		}
		
		for (EnsoulOption soulCrystalSpecialOption1 : _soulCrystalSpecialOptions)
		{
			boolean found = false;
			SEARCH: for (EnsoulOption soulCrystalSpecialOption2 : soulCrystalSpecialOptions)
			{
				if (soulCrystalSpecialOption1.equals(soulCrystalSpecialOption2))
				{
					found = true;
					break SEARCH;
				}
			}
			
			if (!found)
			{
				return false;
			}
		}
		
		return true;
	}
	
	public long getVisualExpiration()
	{
		return _visualExpiration;
	}
	
	public boolean isBlessed()
	{
		return _isBlessed;
	}
	
	public int getReuseDelay()
	{
		return _reuseDelay;
	}
	
	public Player getOwner()
	{
		return _owner;
	}
	
	public PetEvolveHolder getPetData()
	{
		return _petData;
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(_item) + "[objId: " + _objectId + ", count: " + _count + "]";
	}
}
