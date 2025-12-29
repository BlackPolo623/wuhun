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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Set;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Sdw
 */
public class NpcInfoAbnormalVisualEffect extends ServerPacket
{
	private final Npc _npc;
	private final Set<AbnormalVisualEffect> _abnormalVisualEffects;
	private final Team _team;
	
	public NpcInfoAbnormalVisualEffect(Npc npc)
	{
		_npc = npc;
		_abnormalVisualEffects = _npc.getEffectList().getCurrentAbnormalVisualEffects();
		_team = (GeneralConfig.BLUE_TEAM_ABNORMAL_EFFECT != null) && (GeneralConfig.RED_TEAM_ABNORMAL_EFFECT != null) ? _npc.getTeam() : Team.NONE;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.NPC_INFO_ABNORMAL_VISUAL_EFFECT.writeId(this, buffer);
		buffer.writeInt(_npc.getObjectId());
		buffer.writeInt(_npc.getTransformationDisplayId());
		buffer.writeShort(_abnormalVisualEffects.size() + (_team != Team.NONE ? 1 : 0));
		for (AbnormalVisualEffect abnormalVisualEffect : _abnormalVisualEffects)
		{
			buffer.writeShort(abnormalVisualEffect.getClientId());
		}
		
		if (_team == Team.BLUE)
		{
			if (GeneralConfig.BLUE_TEAM_ABNORMAL_EFFECT != null)
			{
				buffer.writeShort(GeneralConfig.BLUE_TEAM_ABNORMAL_EFFECT.getClientId());
			}
		}
		else if ((_team == Team.RED) && (GeneralConfig.RED_TEAM_ABNORMAL_EFFECT != null))
		{
			buffer.writeShort(GeneralConfig.RED_TEAM_ABNORMAL_EFFECT.getClientId());
		}
	}
}
