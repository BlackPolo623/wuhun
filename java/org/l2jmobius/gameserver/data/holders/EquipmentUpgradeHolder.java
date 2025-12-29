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

import java.util.List;

import org.l2jmobius.gameserver.model.item.holders.ItemHolder;

/**
 * @author Mobius
 */
public class EquipmentUpgradeHolder
{
	private final int _id;
	private final int _requiredItemId;
	private final int _requiredItemEnchant;
	private final List<ItemHolder> _materials;
	private final long _adena;
	private final int _resultItemId;
	private final int _resultItemEnchant;
	private final boolean _announce;
	
	public EquipmentUpgradeHolder(int id, int requiredItemId, int requiredItemEnchant, List<ItemHolder> materials, long adena, int resultItemId, int resultItemEnchant, boolean announce)
	{
		_id = id;
		_requiredItemId = requiredItemId;
		_requiredItemEnchant = requiredItemEnchant;
		_materials = materials;
		_adena = adena;
		_resultItemId = resultItemId;
		_resultItemEnchant = resultItemEnchant;
		_announce = announce;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getRequiredItemId()
	{
		return _requiredItemId;
	}
	
	public int getRequiredItemEnchant()
	{
		return _requiredItemEnchant;
	}
	
	public List<ItemHolder> getMaterials()
	{
		return _materials;
	}
	
	public long getAdena()
	{
		return _adena;
	}
	
	public int getResultItemId()
	{
		return _resultItemId;
	}
	
	public int getResultItemEnchant()
	{
		return _resultItemEnchant;
	}
	
	public boolean isAnnounce()
	{
		return _announce;
	}
}
