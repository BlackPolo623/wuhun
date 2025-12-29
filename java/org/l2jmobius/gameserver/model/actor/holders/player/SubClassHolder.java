/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.model.actor.holders.player;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;

/**
 * Character Sub-Class Definition<br>
 * Used to store key information about a character's sub-class.
 * @author Tempy
 */
public class SubClassHolder
{
	private static final int MAX_LEVEL = PlayerConfig.MAX_SUBCLASS_LEVEL < ExperienceData.getInstance().getMaxLevel() ? PlayerConfig.MAX_SUBCLASS_LEVEL : ExperienceData.getInstance().getMaxLevel() - 1;
	
	private static final int MAX_VITALITY_POINTS = 3500000;
	private static final int MIN_VITALITY_POINTS = 0;
	
	private PlayerClass _playerClass;
	private long _exp = ExperienceData.getInstance().getExpForLevel(PlayerConfig.BASE_SUBCLASS_LEVEL);
	private long _sp = 0;
	private int _level = PlayerConfig.BASE_SUBCLASS_LEVEL;
	private int _classIndex = 1;
	private int _vitalityPoints = 0;
	private boolean _dualClass = false;
	
	public SubClassHolder()
	{
		// Used for specifying ALL attributes of a sub class directly,
		// using the preset default values.
	}
	
	public PlayerClass getPlayerClass()
	{
		return _playerClass;
	}
	
	public int getId()
	{
		return _playerClass.getId();
	}
	
	public long getExp()
	{
		return _exp;
	}
	
	public long getSp()
	{
		return _sp;
	}
	
	public int getLevel()
	{
		return _level;
	}
	
	public int getVitalityPoints()
	{
		return Math.min(Math.max(_vitalityPoints, MIN_VITALITY_POINTS), MAX_VITALITY_POINTS);
	}
	
	public void setVitalityPoints(int value)
	{
		_vitalityPoints = Math.min(Math.max(value, MIN_VITALITY_POINTS), MAX_VITALITY_POINTS);
	}
	
	/**
	 * First Sub-Class is index 1.
	 * @return int _classIndex
	 */
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	public void setPlayerClass(int id)
	{
		_playerClass = PlayerClass.getPlayerClass(id);
	}
	
	public void setExp(long expValue)
	{
		if (!_dualClass && (expValue > (ExperienceData.getInstance().getExpForLevel(MAX_LEVEL + 1) - 1)))
		{
			_exp = ExperienceData.getInstance().getExpForLevel(MAX_LEVEL + 1) - 1;
			return;
		}
		
		_exp = expValue;
	}
	
	public void setSp(long spValue)
	{
		_sp = spValue;
	}
	
	public void setClassIndex(int classIndex)
	{
		_classIndex = classIndex;
	}
	
	public boolean isDualClass()
	{
		return _dualClass;
	}
	
	public void setDualClassActive(boolean dualClass)
	{
		_dualClass = dualClass;
	}
	
	public void setLevel(int levelValue)
	{
		if (!_dualClass && (levelValue > MAX_LEVEL))
		{
			_level = MAX_LEVEL;
			return;
		}
		else if (levelValue < PlayerConfig.BASE_SUBCLASS_LEVEL)
		{
			_level = PlayerConfig.BASE_SUBCLASS_LEVEL;
			return;
		}
		
		_level = levelValue;
	}
}
