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
package org.l2jmobius.gameserver.network.enums;

public enum ExBrProductReplyType
{
	SUCCESS(1),
	LACK_OF_POINT(-1),
	INVALID_PRODUCT(-2),
	USER_CANCEL(-3),
	INVENTORY_OVERFLOW(-4),
	CLOSED_PRODUCT(-5),
	SERVER_ERROR(-6),
	BEFORE_SALE_DATE(-7),
	AFTER_SALE_DATE(-8),
	INVALID_USER(-9),
	INVALID_ITEM(-10),
	INVALID_USER_STATE(-11),
	NOT_DAY_OF_WEEK(-12),
	NOT_TIME_OF_DAY(-13),
	SOLD_OUT(-14);
	
	private final int _id;
	
	ExBrProductReplyType(int id)
	{
		_id = id;
	}
	
	public int getId()
	{
		return _id;
	}
}
