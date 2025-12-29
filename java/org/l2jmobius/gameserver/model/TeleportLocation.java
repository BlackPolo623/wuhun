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

/**
 * @version $Revision: 1.2.4.1 $ $Date: 2005/03/27 15:29:32 $
 */
public class TeleportLocation
{
	private int _teleId;
	private int _locX;
	private int _locY;
	private int _locZ;
	private int _price;
	private boolean _forNoble;
	private int _itemId;
	
	/**
	 * @param id
	 */
	public void setTeleId(int id)
	{
		_teleId = id;
	}
	
	/**
	 * @param locX
	 */
	public void setLocX(int locX)
	{
		_locX = locX;
	}
	
	/**
	 * @param locY
	 */
	public void setLocY(int locY)
	{
		_locY = locY;
	}
	
	/**
	 * @param locZ
	 */
	public void setLocZ(int locZ)
	{
		_locZ = locZ;
	}
	
	/**
	 * @param price
	 */
	public void setPrice(int price)
	{
		_price = price;
	}
	
	/**
	 * @param value
	 */
	public void setForNoble(boolean value)
	{
		_forNoble = value;
	}
	
	/**
	 * @param value
	 */
	public void setItemId(int value)
	{
		_itemId = value;
	}
	
	/**
	 * @return
	 */
	public int getTeleId()
	{
		return _teleId;
	}
	
	/**
	 * @return
	 */
	public int getLocX()
	{
		return _locX;
	}
	
	/**
	 * @return
	 */
	public int getLocY()
	{
		return _locY;
	}
	
	/**
	 * @return
	 */
	public int getLocZ()
	{
		return _locZ;
	}
	
	/**
	 * @return
	 */
	public int getPrice()
	{
		return _price;
	}
	
	/**
	 * @return
	 */
	public boolean isForNoble()
	{
		return _forNoble;
	}
	
	/**
	 * @return
	 */
	public int getItemId()
	{
		return _itemId;
	}
}
