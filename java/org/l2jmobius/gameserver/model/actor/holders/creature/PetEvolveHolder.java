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
package org.l2jmobius.gameserver.model.actor.holders.creature;

import org.l2jmobius.gameserver.data.enums.EvolveLevel;

/**
 * @author Berezkin Nikolay
 */
public class PetEvolveHolder
{
	private final int _index;
	private final int _level;
	private final EvolveLevel _evolve;
	private final long _exp;
	private final String _name;
	
	public PetEvolveHolder(int index, int evolve, String name, int level, long exp)
	{
		_index = index;
		_evolve = EvolveLevel.values()[evolve];
		_level = level;
		_exp = exp;
		_name = name;
	}
	
	public int getIndex()
	{
		return _index;
	}
	
	public EvolveLevel getEvolve()
	{
		return _evolve;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public long getExp()
	{
		return _exp;
	}
	
	public String getName()
	{
		return _name;
	}
}
