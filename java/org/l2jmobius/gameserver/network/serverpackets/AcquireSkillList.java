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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class AcquireSkillList extends ServerPacket
{
	private Player _player;
	private Collection<SkillLearn> _learnable;
	
	public AcquireSkillList(Player player)
	{
		if (!player.isSubclassLocked()) // Changing class.
		{
			_player = player;
			
			if (player.isTransformed())
			{
				_learnable = Collections.emptyList();
			}
			else
			{
				_learnable = SkillTreeData.getInstance().getAvailableSkills(player, player.getPlayerClass(), false, false);
				_learnable.addAll(SkillTreeData.getInstance().getNextAvailableSkills(player, player.getPlayerClass(), false, false));
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		if (_player == null)
		{
			return;
		}
		
		ServerPackets.ACQUIRE_SKILL_LIST.writeId(this, buffer);
		buffer.writeShort(_learnable.size());
		for (SkillLearn skill : _learnable)
		{
			final int skillId = _player.getReplacementSkill(skill.getSkillId());
			buffer.writeInt(skillId);
			
			buffer.writeInt(skill.getSkillLevel()); // 414 both Main and Essence writeInt.
			buffer.writeLong(skill.getLevelUpSp());
			buffer.writeByte(skill.getGetLevel());
			buffer.writeByte(0); // Skill dual class level.
			
			buffer.writeByte(_player.getKnownSkill(skillId) == null);
			
			buffer.writeByte(skill.getRequiredItems().size());
			for (List<ItemHolder> item : skill.getRequiredItems())
			{
				buffer.writeInt(item.get(0).getId());
				buffer.writeLong(item.get(0).getCount());
			}
			
			final List<Skill> removeSkills = new LinkedList<>();
			for (int id : skill.getRemoveSkills())
			{
				final Skill removeSkill = _player.getKnownSkill(id);
				if (removeSkill != null)
				{
					removeSkills.add(removeSkill);
				}
			}
			
			buffer.writeByte(removeSkills.size());
			for (Skill removed : removeSkills)
			{
				buffer.writeInt(removed.getId());
				buffer.writeInt(removed.getLevel()); // 414 both Main and Essence writeInt.
			}
		}
	}
}
