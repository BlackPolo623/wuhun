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
package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Geremy
 */
public class PetExtractionHolder
{
	private final int _petId;
	private final int _petLevel;
	private final long _extractExp;
	private final int _extractItem;
	private final ItemHolder _defaultCost;
	private final ItemHolder _extractCost;
	
	public PetExtractionHolder(int petId, int petLevel, long extractExp, int extractItem, ItemHolder defaultCost, ItemHolder extractCost)
	{
		_petId = petId;
		_petLevel = petLevel;
		_extractExp = extractExp;
		_extractItem = extractItem;
		_defaultCost = defaultCost;
		_extractCost = extractCost;
	}
	
	public int getPetId()
	{
		return _petId;
	}
	
	public int getPetLevel()
	{
		return _petLevel;
	}
	
	public long getExtractExp()
	{
		return _extractExp;
	}
	
	public int getExtractItem()
	{
		return _extractItem;
	}
	
	public ItemHolder getDefaultCost()
	{
		return _defaultCost;
	}
	
	public ItemHolder getExtractCost()
	{
		return _extractCost;
	}
}
