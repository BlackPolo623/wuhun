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
package org.l2jmobius.gameserver.model;

import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.options.Options;

/**
 * Used to store an augmentation and its bonuses.
 * @author durgus, UnAfraid, Pere, Mobius
 */
public class VariationInstance
{
	private final int _mineralId;
	private final Options _option1;
	private final Options _option2;
	private final Options _option3;
	
	public VariationInstance(int mineralId, int option1Id, int option2Id, int option3Id)
	{
		_mineralId = mineralId;
		_option1 = OptionData.getInstance().getOptions(option1Id);
		_option2 = OptionData.getInstance().getOptions(option2Id);
		_option3 = OptionData.getInstance().getOptions(option3Id);
	}
	
	public VariationInstance(int mineralId, Options op1, Options op2, Options op3)
	{
		_mineralId = mineralId;
		_option1 = op1;
		_option2 = op2;
		_option3 = op3;
	}
	
	public int getMineralId()
	{
		return _mineralId;
	}
	
	public int getOption1Id()
	{
		return _option1 == null ? 0 : _option1.getId();
	}
	
	public int getOption2Id()
	{
		return _option2 == null ? 0 : _option2.getId();
	}
	
	public int getOption3Id()
	{
		return _option3 == null ? 0 : _option3.getId();
	}
	
	public void applyBonus(Playable playable)
	{
		if (_option1 != null)
		{
			_option1.apply(playable);
		}
		
		if (_option2 != null)
		{
			_option2.apply(playable);
		}
		
		if (_option3 != null)
		{
			_option3.apply(playable);
		}
	}
	
	public void removeBonus(Playable playable)
	{
		if (_option1 != null)
		{
			_option1.remove(playable);
		}
		
		if (_option2 != null)
		{
			_option2.remove(playable);
		}
		
		if (_option3 != null)
		{
			_option3.remove(playable);
		}
	}
}
