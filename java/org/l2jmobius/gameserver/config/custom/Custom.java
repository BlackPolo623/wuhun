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
package org.l2jmobius.gameserver.config.custom;

import java.util.Set;
import java.util.TreeSet;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all the custom wedding related configurations.
 * @author Mobius
 */
public class Custom
{
	// File
	private static final String Custom_CONFIG_FILE = "./config/Custom.ini";
	
	// Constants
	public static int WUXIANXIANDING;// 無限成長最高值
	public static int wuxianjiacheng;// 無限成長加成數值默認為1
	public static int zhuangbeizszdz;// 裝備轉生最大值
	public static int zhuangbeizsjczd;// 裝備轉生加成最大值
	public static int zhuangbeijcz;// 裝備轉生加成值
	
	public static int enchantmin;// 隨機强化最小值
	public static int enchantmax;// 隨機强化最大值
	public static final Set<Integer> SKILL_ACTIVE_IDS = new TreeSet<>();
	public static final Set<Integer> ADD_HP_SKILLS = new TreeSet<>();
	public static final Set<Integer> ADD_HP_PARTY_SKILLS = new TreeSet<>();
	public static final Set<Integer> ADD_HP_GROUP_SKILLS = new TreeSet<>();
	public static final Set<Integer> ADD_HP_BALANCE_SKILLS = new TreeSet<>();
	public static final Set<Integer> ADD_MP_SKILLS = new TreeSet<>();
	public static final Set<Integer> ADD_MP_PARTY_SKILLS = new TreeSet<>();
	public static final Set<Integer> ADD_MP_GROUP_SKILLS = new TreeSet<>();
	public static final Set<Integer> BUFF_ITEM_IDS = new TreeSet<>();
	public static int paoshangitemid;
	public static int MinSoulRingForPVP;
	public static int BUffTIME;
	public static int checkskillmin;
	public static int checkskillmax;
	public static final Set<Integer> SKILL_WHITELIST = new TreeSet<>();
	
	public static void load()
	{
		final ConfigReader config = new ConfigReader(Custom_CONFIG_FILE);
		WUXIANXIANDING = config.getInt("wuxianxianding", 100);
		wuxianjiacheng = config.getInt("wuxianjiacheng", 1);
		zhuangbeizszdz = config.getInt("zhuangbeizszdz", 100);
		zhuangbeizsjczd = config.getInt("zhuangbeizsjczd", 100);
		zhuangbeijcz = config.getInt("zhuangbeijcz", 1);
		enchantmin = config.getInt("enchantmin", 1);
		enchantmax = config.getInt("enchantmax", 10);
		
		String[] skillactiveids = config.getString("SkillActiveIds", "1").split(",");
		for (String s : skillactiveids)
		{
			SKILL_ACTIVE_IDS.add(Integer.parseInt(s.trim()));
		}
		String[] addhpskill = config.getString("自身HP技能", "1").split(",");
		for (String s : addhpskill)
		{
			ADD_HP_SKILLS.add(Integer.parseInt(s.trim()));
		}
		
		String[] addhppatryskills = config.getString("队伍HP技能", "1").split(",");
		for (String s : addhppatryskills)
		{
			ADD_HP_PARTY_SKILLS.add(Integer.parseInt(s.trim()));
		}
		String[] addhpgroupskillss = config.getString("群HP技能", "1").split(",");
		for (String s : addhpgroupskillss)
		{
			ADD_HP_GROUP_SKILLS.add(Integer.parseInt(s.trim()));
		}
		String[] addhpbalanceskills = config.getString("HP均衡技能", "1").split(",");
		for (String s : addhpbalanceskills)
		{
			ADD_HP_BALANCE_SKILLS.add(Integer.parseInt(s.trim()));
		}
		String[] addmpskills = config.getString("自身MP技能", "1").split(",");
		for (String s : addmpskills)
		{
			ADD_MP_SKILLS.add(Integer.parseInt(s.trim()));
		}
		String[] addmppartyskills = config.getString("队伍MP技能", "1").split(",");
		for (String s : addmppartyskills)
		{
			ADD_MP_PARTY_SKILLS.add(Integer.parseInt(s.trim()));
		}
		String[] addmpgroupskills = config.getString("群MP技能", "1").split(",");
		for (String s : addmpgroupskills)
		{
			ADD_MP_GROUP_SKILLS.add(Integer.parseInt(s.trim()));
		}
		String[] buffitemids = config.getString("BuffItemIds", "1").split(",");
		for (String s : buffitemids)
		{
			BUFF_ITEM_IDS.add(Integer.parseInt(s.trim()));
		}
		paoshangitemid = config.getInt("paoshangitemid", 57);
		MinSoulRingForPVP = config.getInt("minsoulringforPVP", 999);
		BUffTIME = config.getInt("bufftime", 14400);
		String[] checksikllid = config.getString("CheckSkills", "1").split(",");
		for (String s : checksikllid)
		{
			SKILL_WHITELIST.add(Integer.parseInt(s.trim()));
		}
		checkskillmin = config.getInt("checkskillmin", 106030);
		checkskillmax = config.getInt("checkskillmax", 106031);
		
	}
	
}
