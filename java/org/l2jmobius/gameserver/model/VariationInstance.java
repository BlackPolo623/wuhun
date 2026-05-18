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
	private final Options _option4;
	private final Options _option5;
	// raw IDs for custom augmentations that bypass OptionData
	private final int _rawOption1Id;
	private final int _rawOption2Id;
	private final int _rawOption3Id;
	private final int _rawOption4Id;
	private final int _rawOption5Id;

	public VariationInstance(int mineralId, int option1Id, int option2Id, int option3Id, int option4Id, int option5Id)
	{
		_mineralId = mineralId;
		_option1 = OptionData.getInstance().getOptions(option1Id);
		_option2 = OptionData.getInstance().getOptions(option2Id);
		_option3 = OptionData.getInstance().getOptions(option3Id);
		_option4 = OptionData.getInstance().getOptions(option4Id);
		_option5 = OptionData.getInstance().getOptions(option5Id);
		_rawOption1Id = option1Id;
		_rawOption2Id = option2Id;
		_rawOption3Id = option3Id;
		_rawOption4Id = option4Id;
		_rawOption5Id = option5Id;
	}

	public VariationInstance(int mineralId, int option1Id, int option2Id, int option3Id, int option4Id)
	{
		this(mineralId, option1Id, option2Id, option3Id, option4Id, 0);
	}

	/** 直接存 raw ID，不查 OptionData，供精煉系統使用。 */
	public static VariationInstance ofRaw(int mineralId, int op1, int op2, int op3, int op4)
	{
		return new VariationInstance(mineralId, op1, op2, op3, op4, 0, true);
	}

	/** 直接存 raw ID（含突破第五條），不查 OptionData，供精煉/突破系統使用。 */
	public static VariationInstance ofRaw(int mineralId, int op1, int op2, int op3, int op4, int op5)
	{
		return new VariationInstance(mineralId, op1, op2, op3, op4, op5, true);
	}

	private VariationInstance(int mineralId, int op1, int op2, int op3, int op4, int op5, boolean raw)
	{
		_mineralId = mineralId;
		_option1 = null;
		_option2 = null;
		_option3 = null;
		_option4 = null;
		_option5 = null;
		_rawOption1Id = op1;
		_rawOption2Id = op2;
		_rawOption3Id = op3;
		_rawOption4Id = op4;
		_rawOption5Id = op5;
	}

	public VariationInstance(int mineralId, int option1Id, int option2Id, int option3Id)
	{
		this(mineralId, option1Id, option2Id, option3Id, 0, 0);
	}

	public VariationInstance(int mineralId, Options op1, Options op2, Options op3, Options op4)
	{
		_mineralId = mineralId;
		_option1 = op1;
		_option2 = op2;
		_option3 = op3;
		_option4 = op4;
		_option5 = null;
		_rawOption1Id = op1 != null ? op1.getId() : 0;
		_rawOption2Id = op2 != null ? op2.getId() : 0;
		_rawOption3Id = op3 != null ? op3.getId() : 0;
		_rawOption4Id = op4 != null ? op4.getId() : 0;
		_rawOption5Id = 0;
	}

	public VariationInstance(int mineralId, Options op1, Options op2, Options op3)
	{
		this(mineralId, op1, op2, op3, null);
	}

	public int getMineralId()
	{
		return _mineralId;
	}

	public int getOption1Id()
	{
		return _option1 != null ? _option1.getId() : _rawOption1Id;
	}

	public int getOption2Id()
	{
		return _option2 != null ? _option2.getId() : _rawOption2Id;
	}

	public int getOption3Id()
	{
		return _option3 != null ? _option3.getId() : _rawOption3Id;
	}

	public int getOption4Id()
	{
		return _option4 != null ? _option4.getId() : _rawOption4Id;
	}

	/** 突破第五條精煉詞條（raw optionId）。0 表示尚未突破。 */
	public int getOption5Id()
	{
		return _option5 != null ? _option5.getId() : _rawOption5Id;
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
		if (_option4 != null)
		{
			_option4.apply(playable);
		}
		if (_option5 != null)
		{
			_option5.apply(playable);
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
		if (_option4 != null)
		{
			_option4.remove(playable);
		}
		if (_option5 != null)
		{
			_option5.remove(playable);
		}
	}
}
