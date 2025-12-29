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
package org.l2jmobius.gameserver.model.actor.holders.player;

/**
 * @author Smoto
 */
public class CrossEventHolder
{
	private final int _cellId;
	private int _horizontal;
	private int _vertical;
	
	public CrossEventHolder(int cellId, int horizontal, int vertical)
	{
		_cellId = cellId;
		_vertical = vertical;
		_horizontal = horizontal;
	}
	
	public CrossEventHolder(int cellId)
	{
		_cellId = cellId;
		switch (cellId)
		{
			case 0:
			{
				_vertical = 0;
				_horizontal = 0;
				break;
			}
			case 1:
			{
				_vertical = 0;
				_horizontal = 1;
				break;
			}
			case 2:
			{
				_vertical = 0;
				_horizontal = 2;
				break;
			}
			case 3:
			{
				_vertical = 0;
				_horizontal = 3;
				break;
			}
			case 4:
			{
				_vertical = 1;
				_horizontal = 0;
				break;
			}
			case 5:
			{
				_vertical = 1;
				_horizontal = 1;
				break;
			}
			case 6:
			{
				_vertical = 1;
				_horizontal = 2;
				break;
			}
			case 7:
			{
				_vertical = 1;
				_horizontal = 3;
				break;
			}
			case 8:
			{
				_vertical = 2;
				_horizontal = 0;
				break;
			}
			case 9:
			{
				_vertical = 2;
				_horizontal = 1;
				break;
			}
			case 10:
			{
				_vertical = 2;
				_horizontal = 2;
				break;
			}
			case 11:
			{
				_vertical = 2;
				_horizontal = 3;
				break;
			}
			case 12:
			{
				_vertical = 3;
				_horizontal = 0;
				break;
			}
			case 13:
			{
				_vertical = 3;
				_horizontal = 1;
				break;
			}
			case 14:
			{
				_vertical = 3;
				_horizontal = 2;
				break;
			}
			case 15:
			{
				_vertical = 3;
				_horizontal = 3;
				break;
			}
		}
	}
	
	public int cellId()
	{
		return _cellId;
	}
	
	public int getVertical()
	{
		return _vertical;
	}
	
	public int getHorizontal()
	{
		return _horizontal;
	}
}
