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

import org.l2jmobius.gameserver.model.interfaces.IUpdateTypeComponent;

/**
 * @author Sdw, Mobius
 */
public enum UserInfoType implements IUpdateTypeComponent
{
	RELATION(0x00, 4),
	BASIC_INFO(0x01, 23),
	BASE_STATS(0x02, 18),
	MAX_HPCPMP(0x03, 14),
	CURRENT_HPMPCP_EXP_SP(0x04, 39),
	ENCHANTLEVEL(0x05, 7),
	APPAREANCE(0x06, 19),
	STATUS(0x07, 6),
	
	STATS(0x08, 76),
	ELEMENTALS(0x09, 14),
	POSITION(0x0A, 18),
	SPEED(0x0B, 18),
	MULTIPLIER(0x0C, 18),
	COL_RADIUS_HEIGHT(0x0D, 18),
	ATK_ELEMENTAL(0x0E, 5),
	CLAN(0x0F, 32),
	
	SOCIAL(0x10, 34),
	VITA_FAME(0x11, 19),
	SLOTS(0x12, 16),
	MOVEMENTS(0x13, 4),
	COLOR(0x14, 10),
	INVENTORY_LIMIT(0x15, 17),
	TRUE_HERO(0x16, 9),
	
	ATT_SPIRITS(0x17, 34),
	
	RANKING(0x18, 6),
	
	STAT_POINTS(0x19, 28),
	STAT_ABILITIES(0x1A, 18),
	
	ELIXIR_USED(0x1B, 1),
	
	VANGUARD_MOUNT(0x1C, 1),
	UNK_414(0x1D, 1);
	
	/** Int mask. */
	private final int _mask;
	private final int _blockLength;
	
	private UserInfoType(int mask, int blockLength)
	{
		_mask = mask;
		_blockLength = blockLength;
	}
	
	/**
	 * Gets the int mask.
	 * @return the int mask
	 */
	@Override
	public int getMask()
	{
		return _mask;
	}
	
	public int getBlockLength()
	{
		return _blockLength;
	}
}
