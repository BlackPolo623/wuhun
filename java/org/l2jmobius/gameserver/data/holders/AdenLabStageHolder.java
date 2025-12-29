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

import java.util.ArrayList;
import java.util.List;

/**
 * @author SaltyMike
 */
public class AdenLabStageHolder
{
	private int _stageLevel;
	private int _combatPower;
	private float _stageChance;
	private final ArrayList<AdenLabSkillHolder> _skills = new ArrayList<>();
	
	public AdenLabStageHolder()
	{
	}
	
	public int getStageLevel()
	{
		return _stageLevel;
	}
	
	public int getCombatPower()
	{
		return _combatPower;
	}
	
	public float getStageChance()
	{
		return _stageChance;
	}
	
	public List<AdenLabSkillHolder> getSkills()
	{
		return _skills;
	}
	
	public void setStageLevel(int stageLevel)
	{
		_stageLevel = stageLevel;
	}
	
	public void setCombatPower(int combatPower)
	{
		_combatPower = combatPower;
	}
	
	public void setStageChance(float stageChance)
	{
		_stageChance = stageChance;
	}
	
	public void addSkills(List<AdenLabSkillHolder> skills)
	{
		_skills.addAll(skills);
	}
	
	public void addSkill(AdenLabSkillHolder skill)
	{
		_skills.add(skill);
	}
}
