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
package org.l2jmobius.gameserver.data.enums;

/**
 * This class defines all category types.
 * @author xban1x, Mobius
 */
public enum CategoryType
{
	FIGHTER_GROUP,
	MAGE_GROUP,
	WIZARD_GROUP,
	CLERIC_GROUP,
	ATTACKER_GROUP,
	FIRST_CLASS_GROUP,
	SECOND_CLASS_GROUP,
	THIRD_CLASS_GROUP,
	FOURTH_CLASS_GROUP,
	BOUNTY_HUNTER_GROUP,
	WARSMITH_GROUP,
	STRIDER,
	WOLF_GROUP,
	WYVERN_GROUP,
	PET_GROUP,
	SUBJOB_GROUP_KNIGHT,
	HUMAN_FALL_CLASS,
	HUMAN_MALL_CLASS,
	HUMAN_CALL_CLASS,
	ELF_FALL_CLASS,
	ELF_MALL_CLASS,
	ELF_CALL_CLASS,
	ORC_FALL_CLASS,
	ORC_MALL_CLASS,
	BEGINNER_MAGE,
	SUB_GROUP_ROGUE,
	SUB_GROUP_KNIGHT,
	SUB_GROUP_HEC,
	SUB_GROUP_HEW,
	SUB_GROUP_HEF,
	SUB_GROUP_ORC,
	SUB_GROUP_WARE,
	SUB_GROUP_BLACK,
	SUB_GROUP_DE,
	DEATH_KNIGHT_ALL_CLASS,
	SYLPH_ALL_CLASS,
	VANGUARD_ALL_CLASS,
	ASSASSIN_ALL_CLASS,
	HIGH_ELF_WEAVER,
	HIGH_ELF_TEMPLAR,
	HIGH_ELF_ALL_CLASS,
	WARG_ALL_CLASS,
	BLOOD_ROSE_ALL_CLASS,
	SAMURAI_ALL_CLASS;
	
	/**
	 * Finds category by it's name
	 * @param categoryName
	 * @return A {@code CategoryType} if category was found, {@code null} if category was not found
	 */
	public static CategoryType findByName(String categoryName)
	{
		for (CategoryType type : values())
		{
			if (type.name().equalsIgnoreCase(categoryName))
			{
				return type;
			}
		}
		
		return null;
	}
}
