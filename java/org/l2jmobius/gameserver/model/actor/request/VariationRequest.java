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
package org.l2jmobius.gameserver.model.actor.request;

import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * @author Index
 */
public class VariationRequest extends AbstractRequest
{
	private Item _augmented;
	private Item _mineral;
	private VariationInstance _augmentation;
	
	public VariationRequest(Player player)
	{
		super(player);
	}
	
	public synchronized void setAugmentedItem(int objectId)
	{
		_augmented = getPlayer().getInventory().getItemByObjectId(objectId);
	}
	
	public synchronized Item getAugmentedItem()
	{
		return _augmented;
	}
	
	public synchronized void setMineralItem(int objectId)
	{
		_mineral = getPlayer().getInventory().getItemByObjectId(objectId);
	}
	
	public synchronized Item getMineralItem()
	{
		return _mineral;
	}
	
	public synchronized void setAugment(VariationInstance augment)
	{
		_augmentation = augment;
	}
	
	public synchronized VariationInstance getAugment()
	{
		return _augmentation;
	}
	
	@Override
	public boolean isUsing(int objectId)
	{
		return false;
	}
}
