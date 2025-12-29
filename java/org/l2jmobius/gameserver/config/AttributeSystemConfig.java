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
package org.l2jmobius.gameserver.config;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the attribute system related configurations.
 * @author Mobius
 */
public class AttributeSystemConfig
{
	// File
	private static final String ATTRIBUTE_SYSTEM_CONFIG_FILE = "./config/AttributeSystem.ini";
	
	// Constants
	public static int S_WEAPON_STONE;
	public static int S80_WEAPON_STONE;
	public static int S84_WEAPON_STONE;
	public static int R_WEAPON_STONE;
	public static int R95_WEAPON_STONE;
	public static int R99_WEAPON_STONE;
	public static int S_ARMOR_STONE;
	public static int S80_ARMOR_STONE;
	public static int S84_ARMOR_STONE;
	public static int R_ARMOR_STONE;
	public static int R95_ARMOR_STONE;
	public static int R99_ARMOR_STONE;
	public static int S_WEAPON_CRYSTAL;
	public static int S80_WEAPON_CRYSTAL;
	public static int S84_WEAPON_CRYSTAL;
	public static int R_WEAPON_CRYSTAL;
	public static int R95_WEAPON_CRYSTAL;
	public static int R99_WEAPON_CRYSTAL;
	public static int S_ARMOR_CRYSTAL;
	public static int S80_ARMOR_CRYSTAL;
	public static int S84_ARMOR_CRYSTAL;
	public static int R_ARMOR_CRYSTAL;
	public static int R95_ARMOR_CRYSTAL;
	public static int R99_ARMOR_CRYSTAL;
	public static int S_WEAPON_STONE_SUPER;
	public static int S80_WEAPON_STONE_SUPER;
	public static int S84_WEAPON_STONE_SUPER;
	public static int R_WEAPON_STONE_SUPER;
	public static int R95_WEAPON_STONE_SUPER;
	public static int R99_WEAPON_STONE_SUPER;
	public static int S_ARMOR_STONE_SUPER;
	public static int S80_ARMOR_STONE_SUPER;
	public static int S84_ARMOR_STONE_SUPER;
	public static int R_ARMOR_STONE_SUPER;
	public static int R95_ARMOR_STONE_SUPER;
	public static int R99_ARMOR_STONE_SUPER;
	public static int S_WEAPON_CRYSTAL_SUPER;
	public static int S80_WEAPON_CRYSTAL_SUPER;
	public static int S84_WEAPON_CRYSTAL_SUPER;
	public static int R_WEAPON_CRYSTAL_SUPER;
	public static int R95_WEAPON_CRYSTAL_SUPER;
	public static int R99_WEAPON_CRYSTAL_SUPER;
	public static int S_ARMOR_CRYSTAL_SUPER;
	public static int S80_ARMOR_CRYSTAL_SUPER;
	public static int S84_ARMOR_CRYSTAL_SUPER;
	public static int R_ARMOR_CRYSTAL_SUPER;
	public static int R95_ARMOR_CRYSTAL_SUPER;
	public static int R99_ARMOR_CRYSTAL_SUPER;
	public static int S_WEAPON_JEWEL;
	public static int S80_WEAPON_JEWEL;
	public static int S84_WEAPON_JEWEL;
	public static int R_WEAPON_JEWEL;
	public static int R95_WEAPON_JEWEL;
	public static int R99_WEAPON_JEWEL;
	public static int S_ARMOR_JEWEL;
	public static int S80_ARMOR_JEWEL;
	public static int S84_ARMOR_JEWEL;
	public static int R_ARMOR_JEWEL;
	public static int R95_ARMOR_JEWEL;
	public static int R99_ARMOR_JEWEL;
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(ATTRIBUTE_SYSTEM_CONFIG_FILE);
		S_WEAPON_STONE = config.getInt("SWeaponStone", 50);
		S80_WEAPON_STONE = config.getInt("S80WeaponStone", 50);
		S84_WEAPON_STONE = config.getInt("S84WeaponStone", 50);
		R_WEAPON_STONE = config.getInt("RWeaponStone", 50);
		R95_WEAPON_STONE = config.getInt("R95WeaponStone", 50);
		R99_WEAPON_STONE = config.getInt("R99WeaponStone", 50);
		
		S_ARMOR_STONE = config.getInt("SArmorStone", 60);
		S80_ARMOR_STONE = config.getInt("S80ArmorStone", 80);
		S84_ARMOR_STONE = config.getInt("S84ArmorStone", 80);
		R_ARMOR_STONE = config.getInt("RArmorStone", 100);
		R95_ARMOR_STONE = config.getInt("R95ArmorStone", 100);
		R99_ARMOR_STONE = config.getInt("R99ArmorStone", 100);
		
		S_WEAPON_CRYSTAL = config.getInt("SWeaponCrystal", 30);
		S80_WEAPON_CRYSTAL = config.getInt("S80WeaponCrystal", 40);
		S84_WEAPON_CRYSTAL = config.getInt("S84WeaponCrystal", 50);
		R_WEAPON_CRYSTAL = config.getInt("RWeaponCrystal", 60);
		R95_WEAPON_CRYSTAL = config.getInt("R95WeaponCrystal", 60);
		R99_WEAPON_CRYSTAL = config.getInt("R99WeaponCrystal", 60);
		
		S_ARMOR_CRYSTAL = config.getInt("SArmorCrystal", 50);
		S80_ARMOR_CRYSTAL = config.getInt("S80ArmorCrystal", 70);
		S84_ARMOR_CRYSTAL = config.getInt("S84ArmorCrystal", 80);
		R_ARMOR_CRYSTAL = config.getInt("RArmorCrystal", 80);
		R95_ARMOR_CRYSTAL = config.getInt("R95ArmorCrystal", 100);
		R99_ARMOR_CRYSTAL = config.getInt("R99ArmorCrystal", 100);
		
		S_WEAPON_STONE_SUPER = config.getInt("SWeaponStoneSuper", 100);
		S80_WEAPON_STONE_SUPER = config.getInt("S80WeaponStoneSuper", 100);
		S84_WEAPON_STONE_SUPER = config.getInt("S84WeaponStoneSuper", 100);
		R_WEAPON_STONE_SUPER = config.getInt("RWeaponStoneSuper", 100);
		R95_WEAPON_STONE_SUPER = config.getInt("R95WeaponStoneSuper", 100);
		R99_WEAPON_STONE_SUPER = config.getInt("R99WeaponStoneSuper", 100);
		
		S_ARMOR_STONE_SUPER = config.getInt("SArmorStoneSuper", 100);
		S80_ARMOR_STONE_SUPER = config.getInt("S80ArmorStoneSuper", 100);
		S84_ARMOR_STONE_SUPER = config.getInt("S84ArmorStoneSuper", 100);
		R_ARMOR_STONE_SUPER = config.getInt("RArmorStoneSuper", 100);
		R95_ARMOR_STONE_SUPER = config.getInt("R95ArmorStoneSuper", 100);
		R99_ARMOR_STONE_SUPER = config.getInt("R99ArmorStoneSuper", 100);
		
		S_WEAPON_CRYSTAL_SUPER = config.getInt("SWeaponCrystalSuper", 80);
		S80_WEAPON_CRYSTAL_SUPER = config.getInt("S80WeaponCrystalSuper", 90);
		S84_WEAPON_CRYSTAL_SUPER = config.getInt("S84WeaponCrystalSuper", 100);
		R_WEAPON_CRYSTAL_SUPER = config.getInt("RWeaponCrystalSuper", 100);
		R95_WEAPON_CRYSTAL_SUPER = config.getInt("R95WeaponCrystalSuper", 100);
		R99_WEAPON_CRYSTAL_SUPER = config.getInt("R99WeaponCrystalSuper", 100);
		
		S_ARMOR_CRYSTAL_SUPER = config.getInt("SArmorCrystalSuper", 100);
		S80_ARMOR_CRYSTAL_SUPER = config.getInt("S80ArmorCrystalSuper", 100);
		S84_ARMOR_CRYSTAL_SUPER = config.getInt("S84ArmorCrystalSuper", 100);
		R_ARMOR_CRYSTAL_SUPER = config.getInt("RArmorCrystalSuper", 100);
		R95_ARMOR_CRYSTAL_SUPER = config.getInt("R95ArmorCrystalSuper", 100);
		R99_ARMOR_CRYSTAL_SUPER = config.getInt("R99ArmorCrystalSuper", 100);
		
		S_WEAPON_JEWEL = config.getInt("SWeaponJewel", 100);
		S80_WEAPON_JEWEL = config.getInt("S80WeaponJewel", 100);
		S84_WEAPON_JEWEL = config.getInt("S84WeaponJewel", 100);
		R_WEAPON_JEWEL = config.getInt("RWeaponJewel", 100);
		R95_WEAPON_JEWEL = config.getInt("R95WeaponJewel", 100);
		R99_WEAPON_JEWEL = config.getInt("R99WeaponJewel", 100);
		
		S_ARMOR_JEWEL = config.getInt("SArmorJewel", 100);
		S80_ARMOR_JEWEL = config.getInt("S80ArmorJewel", 100);
		S84_ARMOR_JEWEL = config.getInt("S84ArmorJewel", 100);
		R_ARMOR_JEWEL = config.getInt("RArmorJewel", 100);
		R95_ARMOR_JEWEL = config.getInt("R95ArmorJewel", 100);
		R99_ARMOR_JEWEL = config.getInt("R99ArmorJewel", 100);
	}
}
