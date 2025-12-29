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

/**
 * @author Brado
 */
public class RelicEnchantHolder
{
	
	private final int _enchantLevel;
	private final int _skillId;
	private final int _skillLevel;
	private final int _combatPower;
	
	/**
	 * @param enchantLevel
	 * @param skillId
	 * @param skillLevel
	 * @param combatPower
	 */
	public RelicEnchantHolder(int enchantLevel, int skillId, int skillLevel, int combatPower)
	{
		_enchantLevel = enchantLevel;
		_skillId = skillId;
		_skillLevel = skillLevel;
		_combatPower = combatPower;
	}
	
	public int getSkillId()
	{
		return _skillId;
	}
	
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	public int getSkillLevel()
	{
		return _skillLevel;
	}
	
	public int getCombatPower()
	{
		return _combatPower;
	}
}
