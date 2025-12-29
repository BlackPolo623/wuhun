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
package org.l2jmobius.gameserver.model.item.henna;

import org.l2jmobius.gameserver.data.xml.HennaPatternPotentialData;

/**
 * @author Serenitty
 */
public class HennaPoten
{
	private Henna _henna;
	private int _potenId;
	private int _enchantLevel = 1;
	private int _enchantExp;
	private int _slotPosition;
	private int _unlockSlot = 1;
	
	public HennaPoten()
	{
	}
	
	public void setHenna(Henna henna)
	{
		_henna = henna;
	}
	
	public Henna getHenna()
	{
		return _henna;
	}
	
	public void setPotenId(int val)
	{
		_potenId = val;
	}
	
	public int getSlotPosition()
	{
		return _slotPosition;
	}
	
	public void setSlotPosition(int val)
	{
		_slotPosition = val;
	}
	
	public int getPotenId()
	{
		return _potenId;
	}
	
	public void setEnchantLevel(int val)
	{
		_enchantLevel = val;
	}
	
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	public void setEnchantExp(int val)
	{
		_enchantExp = val;
	}
	
	public int getEnchantExp()
	{
		if (_enchantExp > HennaPatternPotentialData.getInstance().getMaxPotenExp())
		{
			_enchantExp = HennaPatternPotentialData.getInstance().getMaxPotenExp();
			return _enchantExp;
		}
		
		return _enchantExp;
	}
	
	public int getActiveStep()
	{
		if (_enchantExp == HennaPatternPotentialData.getInstance().getMaxPotenExp())
		{
			return Math.min(_enchantLevel, _unlockSlot);
		}
		
		return Math.min(_enchantLevel - 1, _unlockSlot);
	}
	
	public int getUnlockSlot()
	{
		return _unlockSlot;
	}
	
	public void setUnlockSlot(int slot)
	{
		_unlockSlot = slot;
	}
}
