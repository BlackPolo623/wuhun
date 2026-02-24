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
package org.l2jmobius.gameserver.model.jewel;

/**
 * 玩家寶玉數據類
 * @author YourName
 */
public class PlayerJewelData
{
	private final int _charId;
	private boolean _activated;
	private int _currentValueStage; // 當前數值揭露階段 (0-20)
	private int _currentBonusStage; // 當前加成突破階段 (0-20)
	private final long[] _stageValues; // 每階段揭露的數值

	public PlayerJewelData(int charId)
	{
		_charId = charId;
		_activated = false;
		_currentValueStage = 0;
		_currentBonusStage = 0;
		_stageValues = new long[20];
	}

	public int getCharId()
	{
		return _charId;
	}

	public boolean isActivated()
	{
		return _activated;
	}

	public void setActivated(boolean activated)
	{
		_activated = activated;
	}

	public int getCurrentValueStage()
	{
		return _currentValueStage;
	}

	public void setCurrentValueStage(int stage)
	{
		_currentValueStage = Math.max(0, Math.min(20, stage));
	}

	public int getCurrentBonusStage()
	{
		return _currentBonusStage;
	}

	public void setCurrentBonusStage(int stage)
	{
		_currentBonusStage = Math.max(0, Math.min(20, stage));
	}

	public long getStageValue(int stage)
	{
		if ((stage < 1) || (stage > 20))
		{
			return 0;
		}
		return _stageValues[stage - 1];
	}

	public void setStageValue(int stage, long value)
	{
		if ((stage >= 1) && (stage <= 20))
		{
			_stageValues[stage - 1] = value;
		}
	}

	public long[] getAllStageValues()
	{
		return _stageValues;
	}

	/**
	 * 計算總攻擊加成
	 * @return 總物攻/魔攻加成值
	 */
	public long calculateTotalBonus()
	{
		if (!_activated)
		{
			return 0;
		}

		long totalBonus = 0;
		for (int i = 1; i <= _currentValueStage; i++)
		{
			final long stageValue = _stageValues[i - 1];
			if (stageValue > 0)
			{
				// 檢查該階段是否有加成突破
				final int bonusPercent = getBonusPercentForStage(i);
				totalBonus += (stageValue * bonusPercent) / 100;
			}
		}
		return totalBonus;
	}

	/**
	 * 獲取指定階段的加成百分比
	 * @param stage 階段 (1-20)
	 * @return 加成百分比
	 */
	private int getBonusPercentForStage(int stage)
	{
		// 該階段的加成突破是否已完成
		// 例如：stage=3，需要 _currentBonusStage >= 3 才有加成
		if (_currentBonusStage >= stage)
		{
			return JewelSystemConfig.getBonusPercent(stage);
		}
		else
		{
			return 100; // 基礎100%
		}
	}

	/**
	 * 初始化所有數值 (從頭到當前階段全部清除)
	 */
	public void resetAllValues()
	{
		for (int i = 0; i < 20; i++)
		{
			_stageValues[i] = 0;
		}
		_currentValueStage = 0;
	}

	/**
	 * 獲取當前所在的區間 (1-4)
	 * @return 區間編號
	 */
	public int getCurrentTier()
	{
		if (_currentValueStage <= 5)
		{
			return 1;
		}
		else if (_currentValueStage <= 10)
		{
			return 2;
		}
		else if (_currentValueStage <= 15)
		{
			return 3;
		}
		else
		{
			return 4;
		}
	}

	/**
	 * 檢查是否可以顯示初始化按鈕
	 * @return 是否可以初始化
	 */
	public boolean canReset()
	{
		// 只有在第5、10、15、20階段才能初始化
		return (_currentValueStage == 5) || (_currentValueStage == 10) || (_currentValueStage == 15) || (_currentValueStage == 20);
	}

	/**
	 * 獲取加成突破在當前5階段循環中的位置 (0-4)
	 * @return 循環位置
	 */
	public int getBonusStageInCycle()
	{
		return _currentBonusStage % 5;
	}
}
