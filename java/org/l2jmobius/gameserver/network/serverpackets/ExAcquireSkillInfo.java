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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.LinkedList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class ExAcquireSkillInfo extends ServerPacket
{
	private final Player _player;
	private final int _id;
	private final int _level;
	private final int _dualClassLevel;
	private final long _spCost;
	private final int _minLevel;
	private final List<List<ItemHolder>> _itemReq;
	private final List<Skill> _skillRem = new LinkedList<>();
	
	/**
	 * Special constructor for Alternate Skill Learning system.<br>
	 * Sets a custom amount of SP.
	 * @param player
	 * @param skillLearn the skill learn.
	 */
	public ExAcquireSkillInfo(Player player, SkillLearn skillLearn)
	{
		_player = player;
		_id = skillLearn.getSkillId();
		_level = skillLearn.getSkillLevel();
		_dualClassLevel = skillLearn.getDualClassLevel();
		_spCost = skillLearn.getLevelUpSp();
		_minLevel = skillLearn.getGetLevel();
		_itemReq = skillLearn.getRequiredItems();
		for (int id : skillLearn.getRemoveSkills())
		{
			final Skill removeSkill = player.getKnownSkill(id);
			if (removeSkill != null)
			{
				_skillRem.add(removeSkill);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_ACQUIRE_SKILL_INFO.writeId(this, buffer);
		buffer.writeInt(_player.getReplacementSkill(_id));
		buffer.writeInt(_level);
		buffer.writeLong(_spCost);
		buffer.writeShort(_minLevel);
		buffer.writeShort(_dualClassLevel);
		buffer.writeInt(_itemReq.size());
		for (List<ItemHolder> holder : _itemReq)
		{
			final ItemHolder first = holder.get(0);
			buffer.writeInt(first.getId());
			buffer.writeLong(first.getCount());
		}
		
		buffer.writeInt(_skillRem.size());
		for (Skill skill : _skillRem)
		{
			buffer.writeInt(skill.getId());
			buffer.writeInt(skill.getLevel());
		}
	}
}
