package org.l2jmobius.gameserver.data.holders;

public class PotentialRarityHolder
{
	private final int _level;
	private final String _color;
	private final String _name;

	public PotentialRarityHolder(int level, String color, String name)
	{
		_level = level;
		_color = color;
		_name = name;
	}

	public int getLevel()
	{
		return _level;
	}

	public String getColor()
	{
		return _color;
	}

	public String getName()
	{
		return _name;
	}
}