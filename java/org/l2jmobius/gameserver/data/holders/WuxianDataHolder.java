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
package org.l2jmobius.gameserver.data.holders;

/**
 * @author Mobius
 */
public class WuxianDataHolder
{
	private final int _id;
	private int _shuzhi;
	private final String _stat;
	private final int _level;
	
	public WuxianDataHolder(int id, int shuzhi, String stat, int level)
	{
		_id = id;
		_shuzhi = shuzhi;
		_stat = stat;
		_level = level;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getshuzhi()
	{
		return _shuzhi;
	}
	
	public int setshuzhi(int shuzhi)
	{
		if (_shuzhi != shuzhi)
		{
			_shuzhi = shuzhi;
		}
		return _shuzhi;
	}
	
	public String getstat()
	{
		return _stat;
	}
	
	public int getlevel()
	{
		return _level;
	}
	
}
