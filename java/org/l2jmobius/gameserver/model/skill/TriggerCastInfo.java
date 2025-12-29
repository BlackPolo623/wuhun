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
package org.l2jmobius.gameserver.model.skill;

import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * @author Mobius
 */
public class TriggerCastInfo
{
	private final Creature _creature;
	private final WorldObject _target;
	private final Skill _skill;
	private final Item _item;
	private final boolean _ignoreTargetType;
	
	public TriggerCastInfo(Creature creature, WorldObject target, Skill skill, Item item, boolean ignoreTargetType)
	{
		_creature = creature;
		_target = target;
		_skill = skill;
		_item = item;
		_ignoreTargetType = ignoreTargetType;
	}
	
	public Creature getCreature()
	{
		return _creature;
	}
	
	public WorldObject getTarget()
	{
		return _target;
	}
	
	public Skill getSkill()
	{
		return _skill;
	}
	
	public Item getItem()
	{
		return _item;
	}
	
	public boolean isIgnoreTargetType()
	{
		return _ignoreTargetType;
	}
}
