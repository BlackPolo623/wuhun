package org.l2jmobius.gameserver.data.holders;

public class PotentialSkillRangeHolder
{
	private final int _slot;
	private final int _minSkillId;
	private final int _maxSkillId;
	private final int _rarity;

	public PotentialSkillRangeHolder(int slot, int minSkillId, int maxSkillId, int rarity)
	{
		_slot = slot;
		_minSkillId = minSkillId;
		_maxSkillId = maxSkillId;
		_rarity = rarity;
	}

	public int getSlot()
	{
		return _slot;
	}

	public int getMinSkillId()
	{
		return _minSkillId;
	}

	public int getMaxSkillId()
	{
		return _maxSkillId;
	}

	public int getRarity()
	{
		return _rarity;
	}

	public boolean isInRange(int skillId)
	{
		return skillId >= _minSkillId && skillId <= _maxSkillId;
	}
}