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
package org.l2jmobius.gameserver.model.item.henna;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author Serenitty
 */
public class DyePotential
{
	private final int _id;
	private final int _slotId;
	private final int _skillId;
	private final Skill[] _skills;
	private final int _maxSkillLevel;
	
	public DyePotential(int id, int slotId, int skillId, int maxSkillLevel)
	{
		_id = id;
		_slotId = slotId;
		_skillId = skillId;
		_skills = new Skill[maxSkillLevel];
		for (int i = 1; i <= maxSkillLevel; i++)
		{
			_skills[i - 1] = SkillData.getInstance().getSkill(skillId, i);
		}
		
		_maxSkillLevel = maxSkillLevel;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getSlotId()
	{
		return _slotId;
	}
	
	public int getSkillId()
	{
		return _skillId;
	}
	
	public Skill getSkill(int level)
	{
		return _skills[level - 1];
	}
	
	public int getMaxSkillLevel()
	{
		return _maxSkillLevel;
	}
}
