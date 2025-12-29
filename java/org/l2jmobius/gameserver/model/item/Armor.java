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
package org.l2jmobius.gameserver.model.item;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.item.enums.BodyPart;
import org.l2jmobius.gameserver.model.item.type.ArmorType;

/**
 * This class is dedicated to the management of armors.
 */
public class Armor extends ItemTemplate
{
	private ArmorType _type;
	
	/**
	 * Constructor for Armor.
	 * @param set the StatSet designating the set of couples (key,value) characterizing the armor.
	 */
	public Armor(StatSet set)
	{
		super(set);
	}
	
	@Override
	public void set(StatSet set)
	{
		super.set(set);
		_type = set.getEnum("armor_type", ArmorType.class, ArmorType.NONE);
		
		final BodyPart bodyPart = getBodyPart();
		if ((bodyPart == BodyPart.ARTIFACT) || (bodyPart == BodyPart.AGATHION))
		{
			_type1 = ItemTemplate.TYPE1_SHIELD_ARMOR;
			_type2 = ItemTemplate.TYPE2_ACCESSORY;
		}
		else if ((bodyPart == BodyPart.NECK) //
			|| (bodyPart == BodyPart.L_EAR) //
			|| (bodyPart == BodyPart.R_EAR) //
			|| (bodyPart == BodyPart.LR_EAR) //
			|| (bodyPart == BodyPart.L_FINGER) //
			|| (bodyPart == BodyPart.R_FINGER) //
			|| (bodyPart == BodyPart.LR_FINGER) //
			|| (bodyPart == BodyPart.R_BRACELET) //
			|| (bodyPart == BodyPart.L_BRACELET) //
			|| (bodyPart == BodyPart.ARTIFACT_BOOK))
		{
			_type1 = ItemTemplate.TYPE1_WEAPON_RING_EARRING_NECKLACE;
			_type2 = ItemTemplate.TYPE2_ACCESSORY;
		}
		else
		{
			if ((_type == ArmorType.NONE) && (bodyPart == BodyPart.L_HAND))
			{
				_type = ArmorType.SHIELD;
			}
			
			_type1 = ItemTemplate.TYPE1_SHIELD_ARMOR;
			_type2 = ItemTemplate.TYPE2_SHIELD_ARMOR;
		}
	}
	
	/**
	 * @return the type of the armor.
	 */
	@Override
	public ArmorType getItemType()
	{
		return _type;
	}
	
	/**
	 * @return the ID of the item after applying the mask.
	 */
	@Override
	public int getItemMask()
	{
		return _type.mask();
	}
	
	/**
	 * @return {@code true} if the item is an armor, {@code false} otherwise
	 */
	@Override
	public boolean isArmor()
	{
		return true;
	}
}
