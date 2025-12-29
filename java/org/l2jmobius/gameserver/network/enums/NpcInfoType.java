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
 * @author UnAfraid, Mobius
 */
public enum NpcInfoType implements IUpdateTypeComponent
{
	// 0
	ID(0x00, 4),
	ATTACKABLE(0x01, 1),
	RELATIONS(0x02, 8),
	NAME(0x03, 2),
	POSITION(0x04, (3 * 4)),
	HEADING(0x05, 4),
	VEHICLE_ID(0x06, 4),
	ATK_CAST_SPEED(0x07, (2 * 4)),
	
	// 1
	SPEED_MULTIPLIER(0x08, (2 * 4)),
	EQUIPPED(0x09, (3 * 4)),
	ALIVE(0x0A, 1),
	RUNNING(0x0B, 1),
	SWIM_OR_FLY(0x0E, 1),
	TEAM(0x0F, 1),
	
	// 2
	ENCHANT(0x10, 4),
	FLYING(0x11, 4),
	CLONE(0x12, 4),
	PET_EVOLUTION_ID(0x13, 4),
	DISPLAY_EFFECT(0x16, 4),
	TRANSFORMATION(0x17, 4),
	
	// 3
	CURRENT_HP(0x18, 8),
	CURRENT_MP(0x19, 4),
	MAX_HP(0x1A, 8),
	MAX_MP(0x1B, 4),
	SUMMONED(0x1C, 1),
	FOLLOW_INFO(0x1D, (2 * 4)),
	TITLE(0x1E, 2),
	NAME_NPCSTRINGID(0x1F, 4),
	
	// 4
	TITLE_NPCSTRINGID(0x20, 4),
	PVP_FLAG(0x21, 1),
	REPUTATION(0x22, 4),
	CLAN(0x23, (5 * 4)),
	ABNORMALS(0x24, 4),
	VISUAL_STATE(0x25, 1),
	SHOW_NAME(0x26, 1);
	
	private final int _mask;
	private final int _blockLength;
	
	private NpcInfoType(int mask, int blockLength)
	{
		_mask = mask;
		_blockLength = blockLength;
	}
	
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
