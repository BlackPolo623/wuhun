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
package org.l2jmobius.gameserver.model.actor.enums.player;

import org.l2jmobius.gameserver.model.stats.Stat;

/**
 * @author JoeAlisson, Mobius
 */
public enum ElementalSpiritType
{
	NONE,
	FIRE,
	WATER,
	WIND,
	EARTH;
	
	public byte getId()
	{
		return (byte) (ordinal());
	}
	
	public static ElementalSpiritType of(byte elementId)
	{
		return values()[elementId];
	}
	
	public boolean isSuperior(ElementalSpiritType targetType)
	{
		return this == superior(targetType);
	}
	
	public boolean isInferior(ElementalSpiritType targetType)
	{
		return targetType == superior(this);
	}
	
	public ElementalSpiritType getSuperior()
	{
		return superior(this);
	}
	
	public static ElementalSpiritType superior(ElementalSpiritType elementalType)
	{
		switch (elementalType)
		{
			case FIRE:
			{
				return WATER;
			}
			case WATER:
			{
				return WIND;
			}
			case WIND:
			{
				return EARTH;
			}
			case EARTH:
			{
				return FIRE;
			}
			default:
			{
				return NONE;
			}
		}
	}
	
	public Stat getAttackStat()
	{
		switch (this)
		{
			case EARTH:
			{
				return Stat.ELEMENTAL_SPIRIT_EARTH_ATTACK;
			}
			case WIND:
			{
				return Stat.ELEMENTAL_SPIRIT_WIND_ATTACK;
			}
			case FIRE:
			{
				return Stat.ELEMENTAL_SPIRIT_FIRE_ATTACK;
			}
			case WATER:
			{
				return Stat.ELEMENTAL_SPIRIT_WATER_ATTACK;
			}
			default:
			{
				return null;
			}
		}
	}
	
	public Stat getDefenseStat()
	{
		switch (this)
		{
			case EARTH:
			{
				return Stat.ELEMENTAL_SPIRIT_EARTH_DEFENSE;
			}
			case WIND:
			{
				return Stat.ELEMENTAL_SPIRIT_WIND_DEFENSE;
			}
			case FIRE:
			{
				return Stat.ELEMENTAL_SPIRIT_FIRE_DEFENSE;
			}
			case WATER:
			{
				return Stat.ELEMENTAL_SPIRIT_WATER_DEFENSE;
			}
			default:
			{
				return null;
			}
		}
	}
	
	public String getName()
	{
		switch (this)
		{
			case FIRE:
			{
				return "Fire";
			}
			case WATER:
			{
				return "Water";
			}
			case WIND:
			{
				return "Wind";
			}
			case EARTH:
			{
				return "Earth";
			}
			default:
			{
				return "None";
			}
		}
	}
}
