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

import org.l2jmobius.gameserver.data.enums.LampType;
import org.l2jmobius.gameserver.model.StatSet;

/**
 * @author L2CCCP, Serenitty
 */
public class MagicLampDataHolder
{
	private final LampType _type;
	private final long _exp;
	private final long _sp;
	private final double _chance;
	private final int _fromLevel;
	private final int _toLevel;
	
	public MagicLampDataHolder(StatSet params)
	{
		_type = params.getEnum("type", LampType.class);
		_exp = params.getLong("exp");
		_sp = params.getLong("sp");
		_chance = params.getDouble("chance");
		_fromLevel = params.getInt("minLevel");
		_toLevel = params.getInt("maxLevel");
	}
	
	public LampType getType()
	{
		return _type;
	}
	
	public long getExp()
	{
		return _exp;
	}
	
	public long getSp()
	{
		return _sp;
	}
	
	public double getChance()
	{
		return _chance;
	}
	
	public int getFromLevel()
	{
		return _fromLevel;
	}
	
	public int getToLevel()
	{
		return _toLevel;
	}
}
