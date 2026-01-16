package org.l2jmobius.gameserver.data.holders;

public class PotentialSlotHolder
{
	private final int _slotId;
	private final int _minSkillId;
	private final int _maxSkillId;

	public PotentialSlotHolder(int slotId, int minSkillId, int maxSkillId)
	{
		_slotId = slotId;
		_minSkillId = minSkillId;
		_maxSkillId = maxSkillId;
	}

	public int getSlotId()
	{
		return _slotId;
	}

	public int getMinSkillId()
	{
		return _minSkillId;
	}

	public int getMaxSkillId()
	{
		return _maxSkillId;
	}

	public boolean isInRange(int skillId)
	{
		return skillId >= _minSkillId && skillId <= _maxSkillId;
	}
}