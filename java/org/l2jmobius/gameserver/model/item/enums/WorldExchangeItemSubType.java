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
package org.l2jmobius.gameserver.model.item.enums;

/**
 * @author Index
 */
public enum WorldExchangeItemSubType
{
	WEAPON(0),
	ARMOR(1),
	ACCESSORY(2),
	ETC(3),
	ARTIFACT_B1(4),
	ARTIFACT_C1(5),
	ARTIFACT_D1(6),
	ARTIFACT_A1(7),
	ENCHANT_SCROLL(8),
	BLESS_ENCHANT_SCROLL(9),
	MULTI_ENCHANT_SCROLL(10),
	ANCIENT_ENCHANT_SCROLL(11),
	SPIRITSHOT(12),
	SOULSHOT(13),
	BUFF(14),
	VARIATION_STONE(15),
	DYE(16),
	SOUL_CRYSTAL(17),
	SKILLBOOK(18),
	ETC_ENCHANT(19),
	POTION_AND_ETC_SCROLL(20),
	TICKET(21),
	CRAFT(22),
	INC_ENCHANT_PROP(23),
	ADENA(24),
	ETC_SUB_TYPE(25);
	
	private final int _id;
	
	private WorldExchangeItemSubType(int id)
	{
		_id = id;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public static WorldExchangeItemSubType getWorldExchangeItemSubType(int id)
	{
		for (WorldExchangeItemSubType type : values())
		{
			if (type.getId() == id)
			{
				return type;
			}
		}
		
		return null;
	}
}
