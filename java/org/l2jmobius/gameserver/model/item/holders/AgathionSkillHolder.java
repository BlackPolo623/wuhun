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
package org.l2jmobius.gameserver.model.item.holders;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author Mobius
 */
public class AgathionSkillHolder
{
	private final Map<Integer, List<Skill>> _mainSkill;
	private final Map<Integer, List<Skill>> _subSkill;
	
	public AgathionSkillHolder(Map<Integer, List<Skill>> mainSkill, Map<Integer, List<Skill>> subSkill)
	{
		_mainSkill = mainSkill;
		_subSkill = subSkill;
	}
	
	public Map<Integer, List<Skill>> getMainSkills()
	{
		return _mainSkill;
	}
	
	public Map<Integer, List<Skill>> getSubSkills()
	{
		return _subSkill;
	}
	
	public List<Skill> getMainSkills(int enchantLevel)
	{
		if (!_mainSkill.containsKey(enchantLevel))
		{
			return Collections.emptyList();
		}
		
		return _mainSkill.get(enchantLevel);
	}
	
	public List<Skill> getSubSkills(int enchantLevel)
	{
		if (!_subSkill.containsKey(enchantLevel))
		{
			return Collections.emptyList();
		}
		
		return _subSkill.get(enchantLevel);
	}
}
