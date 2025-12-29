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
package org.l2jmobius.gameserver.model.item.holders;

import java.util.List;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;

/**
 * A DTO for items; contains item ID, count and chance.
 * @author xban1x, CostyKiller
 */
public class ItemChanceHolder extends ItemHolder
{
	private final double _chance;
	private final byte _enchantmentLevel;
	private final boolean _maintainIngredient;
	private Set<Integer> _targetIds = null;
	
	public ItemChanceHolder(int id, double chance)
	{
		this(id, chance, 1);
	}
	
	public ItemChanceHolder(int id, double chance, long count)
	{
		super(id, count);
		_chance = chance;
		_enchantmentLevel = 0;
		_maintainIngredient = false;
		_targetIds = null;
	}
	
	public ItemChanceHolder(int id, double chance, long count, byte enchantmentLevel)
	{
		super(id, count);
		_chance = chance;
		_enchantmentLevel = enchantmentLevel;
		_maintainIngredient = false;
		_targetIds = null;
	}
	
	public ItemChanceHolder(int id, double chance, long count, byte enchantmentLevel, boolean maintainIngredient)
	{
		super(id, count);
		_chance = chance;
		_enchantmentLevel = enchantmentLevel;
		_maintainIngredient = maintainIngredient;
		_targetIds = null;
	}
	
	public ItemChanceHolder(int id, long count, double chance, String[] targetIds)
	{
		super(id, count);
		_chance = chance;
		_enchantmentLevel = 0;
		_maintainIngredient = false;
		
		if (targetIds != null)
		{
			for (String s : targetIds)
			{
				try
				{
					_targetIds.add(Integer.parseInt(s.trim()));
				}
				catch (NumberFormatException e)
				{
					// Ignore invalid.
				}
			}
		}
	}
	
	/**
	 * Gets the chance.
	 * @return the drop chance of the item contained in this object
	 */
	public double getChance()
	{
		return _chance;
	}
	
	/**
	 * Gets the enchant level.
	 * @return the enchant level of the item contained in this object
	 */
	public byte getEnchantmentLevel()
	{
		return _enchantmentLevel;
	}
	
	public boolean isMaintainIngredient()
	{
		return _maintainIngredient;
	}
	
	/**
	 * Checks if this reward applies to the given item ID
	 * @param destroyedItemId
	 * @return
	 */
	public boolean matches(int destroyedItemId)
	{
		if (_targetIds.isEmpty())
		{
			return true; // No targetIds means match all.
		}
		
		return _targetIds.contains(destroyedItemId);
	}
	
	/**
	 * Calculates a cumulative chance of all given holders. If all holders' chance sum up to 100% or above, there is 100% guarantee a holder will be selected.
	 * @param holders list of holders to calculate chance from.
	 * @return {@code ItemChanceHolder} of the successful random roll or {@code null} if there was no lucky holder selected.
	 */
	public static ItemChanceHolder getRandomHolder(List<ItemChanceHolder> holders)
	{
		double itemRandom = 100 * Rnd.nextDouble();
		for (ItemChanceHolder holder : holders)
		{
			// Any mathmatical expression including NaN will result in either NaN or 0 of converted to something other than double.
			// We would usually want to skip calculating any holders that include NaN as a chance, because that ruins the overall process.
			if (!Double.isNaN(holder.getChance()))
			{
				// Calculate chance
				if (holder.getChance() > itemRandom)
				{
					return holder;
				}
				
				itemRandom -= holder.getChance();
			}
		}
		
		return null;
	}
	
	@Override
	public String toString()
	{
		return "[" + getClass().getSimpleName() + "] ID: " + getId() + ", count: " + getCount() + ", chance: " + _chance;
	}
}
