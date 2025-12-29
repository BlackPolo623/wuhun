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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.Location;

/**
 * @author NviX, Index
 */
public class TeleportListHolder
{
	private final int _locId;
	private final List<Location> _locations;
	private final int _price;
	private final boolean _special;
	
	public TeleportListHolder(int locId, int x, int y, int z, int price, boolean special)
	{
		_locId = locId;
		_locations = new ArrayList<>(1);
		_locations.add(new Location(x, y, z));
		_price = price;
		_special = special;
	}
	
	public TeleportListHolder(int locId, List<Location> locations, int price, boolean special)
	{
		_locId = locId;
		_locations = locations;
		_price = price;
		_special = special;
	}
	
	public int getLocId()
	{
		return _locId;
	}
	
	public List<Location> getLocations()
	{
		return _locations;
	}
	
	public int getPrice()
	{
		return _price;
	}
	
	public boolean isSpecial()
	{
		return _special;
	}
	
	public Location getLocation()
	{
		return _locations.get(Rnd.get(_locations.size()));
	}
}
