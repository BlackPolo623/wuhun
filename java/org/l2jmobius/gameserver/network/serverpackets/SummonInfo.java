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

import java.util.Set;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.NpcInfoType;

/**
 * @author Mobius
 */
public class SummonInfo extends AbstractMaskPacket<NpcInfoType>
{
	private final Summon _summon;
	private final Player _attacker;
	private final long _relation;
	private final int _value;
	private final byte[] _masks =
	{
		(byte) 0x00,
		(byte) 0x0C,
		(byte) 0x0C,
		(byte) 0x00,
		(byte) 0x00
	};
	private int _initSize = 0;
	private int _blockSize = 0;
	private int _clanCrest = 0;
	private int _clanLargeCrest = 0;
	private int _allyCrest = 0;
	private int _allyId = 0;
	private int _clanId = 0;
	private int _statusMask = 0;
	private final String _title;
	private final Set<AbnormalVisualEffect> _abnormalVisualEffects;
	
	public SummonInfo(Summon summon, Player attacker, int value)
	{
		_summon = summon;
		_attacker = attacker;
		
		final Player owner = summon.getOwner();
		_relation = (attacker != null) && (owner != null) ? owner.getRelation(attacker) : 0;
		_title = (owner != null) && owner.isOnline() ? owner.getName() : "";
		
		_value = value;
		_abnormalVisualEffects = summon.getEffectList().getCurrentAbnormalVisualEffects();
		if (summon.getTemplate().getDisplayId() != summon.getTemplate().getId())
		{
			_masks[2] |= 0x10;
			addComponentType(NpcInfoType.NAME);
		}
		
		addComponentType(NpcInfoType.ATTACKABLE, NpcInfoType.RELATIONS, NpcInfoType.TITLE, NpcInfoType.ID, NpcInfoType.POSITION, NpcInfoType.ALIVE, NpcInfoType.RUNNING, NpcInfoType.PVP_FLAG, NpcInfoType.SHOW_NAME);
		if (summon.getHeading() > 0)
		{
			addComponentType(NpcInfoType.HEADING);
		}
		
		if ((summon.getStat().getPAtkSpd() > 0) || (summon.getStat().getMAtkSpd() > 0))
		{
			addComponentType(NpcInfoType.ATK_CAST_SPEED);
		}
		
		if (summon.getRunSpeed() > 0)
		{
			addComponentType(NpcInfoType.SPEED_MULTIPLIER);
		}
		
		if ((summon.getWeapon() > 0) || (summon.getArmor() > 0))
		{
			addComponentType(NpcInfoType.EQUIPPED);
		}
		
		if (summon.getTeam() != Team.NONE)
		{
			if ((GeneralConfig.BLUE_TEAM_ABNORMAL_EFFECT != null) && (GeneralConfig.RED_TEAM_ABNORMAL_EFFECT != null))
			{
				addComponentType(NpcInfoType.ABNORMALS);
			}
			else
			{
				addComponentType(NpcInfoType.TEAM);
			}
		}
		
		if (summon.isInsideZone(ZoneId.WATER) || summon.isFlying())
		{
			addComponentType(NpcInfoType.SWIM_OR_FLY);
		}
		
		if (summon.isFlying())
		{
			addComponentType(NpcInfoType.FLYING);
		}
		
		if (summon.getMaxHp() > 0)
		{
			addComponentType(NpcInfoType.MAX_HP);
		}
		
		if (summon.getMaxMp() > 0)
		{
			addComponentType(NpcInfoType.MAX_MP);
		}
		
		if (summon.getCurrentHp() <= summon.getMaxHp())
		{
			addComponentType(NpcInfoType.CURRENT_HP);
		}
		
		if (summon.getCurrentMp() <= summon.getMaxMp())
		{
			addComponentType(NpcInfoType.CURRENT_MP);
		}
		
		if (!_abnormalVisualEffects.isEmpty())
		{
			addComponentType(NpcInfoType.ABNORMALS);
		}
		
		if (summon.getTemplate().getWeaponEnchant() > 0)
		{
			addComponentType(NpcInfoType.ENCHANT);
		}
		
		if (summon.getTransformationDisplayId() > 0)
		{
			addComponentType(NpcInfoType.TRANSFORMATION);
		}
		
		if (summon.isShowSummonAnimation())
		{
			addComponentType(NpcInfoType.SUMMONED);
		}
		
		if (summon.getReputation() != 0)
		{
			addComponentType(NpcInfoType.REPUTATION);
		}
		
		if ((owner != null) && (owner.getClan() != null))
		{
			final PlayerAppearance appearance = owner.getAppearance();
			_clanId = appearance.getVisibleClanId();
			_clanCrest = appearance.getVisibleClanCrestId();
			_clanLargeCrest = appearance.getVisibleClanLargeCrestId();
			_allyCrest = appearance.getVisibleAllyId();
			_allyId = appearance.getVisibleAllyCrestId();
			addComponentType(NpcInfoType.CLAN);
		}
		
		addComponentType(NpcInfoType.PET_EVOLUTION_ID);
		
		// TODO: Confirm me
		if (summon.isInCombat())
		{
			_statusMask |= 0x01;
		}
		
		if (summon.isDead())
		{
			_statusMask |= 0x02;
		}
		
		if (summon.isTargetable())
		{
			_statusMask |= 0x04;
		}
		
		_statusMask |= 0x08;
		if (_statusMask != 0x00)
		{
			addComponentType(NpcInfoType.VISUAL_STATE);
		}
	}
	
	@Override
	protected byte[] getMasks()
	{
		return _masks;
	}
	
	@Override
	protected void onNewMaskAdded(NpcInfoType component)
	{
		calcBlockSize(_summon, component);
	}
	
	private void calcBlockSize(Summon summon, NpcInfoType type)
	{
		switch (type)
		{
			case ATTACKABLE:
			case RELATIONS:
			{
				_initSize += type.getBlockLength();
				break;
			}
			case TITLE:
			{
				_initSize += type.getBlockLength() + (_title.length() * 2);
				break;
			}
			case NAME:
			{
				_blockSize += type.getBlockLength() + (summon.getName().length() * 2);
				break;
			}
			default:
			{
				_blockSize += type.getBlockLength();
				break;
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.SUMMON_INFO.writeId(this, buffer);
		buffer.writeInt(_summon.getObjectId());
		buffer.writeByte(_value); // 0=teleported 1=default 2=summoned
		buffer.writeShort(40); // 520 - mask_bits_40
		buffer.writeBytes(_masks);
		
		// Block 1
		buffer.writeByte(_initSize);
		if (containsMask(NpcInfoType.ATTACKABLE))
		{
			buffer.writeByte(_summon.isAutoAttackable(_attacker));
		}
		
		if (containsMask(NpcInfoType.RELATIONS))
		{
			buffer.writeLong(_relation);
		}
		
		if (containsMask(NpcInfoType.TITLE))
		{
			buffer.writeString(_title);
		}
		
		// Block 2
		buffer.writeShort(_blockSize);
		if (containsMask(NpcInfoType.ID))
		{
			buffer.writeInt(_summon.getTemplate().getDisplayId() + 1000000);
		}
		
		if (containsMask(NpcInfoType.POSITION))
		{
			buffer.writeInt(_summon.getX());
			buffer.writeInt(_summon.getY());
			buffer.writeInt(_summon.getZ());
		}
		
		if (containsMask(NpcInfoType.HEADING))
		{
			buffer.writeInt(_summon.getHeading());
		}
		
		if (containsMask(NpcInfoType.VEHICLE_ID))
		{
			buffer.writeInt(0); // Vehicle object id.
		}
		
		if (containsMask(NpcInfoType.ATK_CAST_SPEED))
		{
			buffer.writeInt(_summon.getPAtkSpd());
			buffer.writeInt(_summon.getMAtkSpd());
		}
		
		if (containsMask(NpcInfoType.SPEED_MULTIPLIER))
		{
			buffer.writeFloat((float) _summon.getMovementSpeedMultiplier());
			buffer.writeFloat((float) _summon.getAttackSpeedMultiplier());
		}
		
		if (containsMask(NpcInfoType.EQUIPPED))
		{
			buffer.writeInt(_summon.getWeapon());
			buffer.writeInt(_summon.getArmor()); // Armor id?
			buffer.writeInt(0);
		}
		
		if (containsMask(NpcInfoType.ALIVE))
		{
			buffer.writeByte(!_summon.isDead());
		}
		
		if (containsMask(NpcInfoType.RUNNING))
		{
			buffer.writeByte(_summon.isRunning());
		}
		
		if (containsMask(NpcInfoType.SWIM_OR_FLY))
		{
			buffer.writeByte(_summon.isInsideZone(ZoneId.WATER) ? 1 : _summon.isFlying() ? 2 : 0);
		}
		
		if (containsMask(NpcInfoType.TEAM))
		{
			buffer.writeByte(_summon.getTeam().getId());
		}
		
		if (containsMask(NpcInfoType.ENCHANT))
		{
			buffer.writeInt(_summon.getTemplate().getWeaponEnchant());
		}
		
		if (containsMask(NpcInfoType.FLYING))
		{
			buffer.writeInt(_summon.isFlying());
		}
		
		if (containsMask(NpcInfoType.CLONE))
		{
			buffer.writeInt(0); // Player ObjectId with Decoy
		}
		
		if (containsMask(NpcInfoType.PET_EVOLUTION_ID))
		{
			buffer.writeInt(0); // Unknown
		}
		
		if (containsMask(NpcInfoType.DISPLAY_EFFECT))
		{
			buffer.writeInt(0);
		}
		
		if (containsMask(NpcInfoType.TRANSFORMATION))
		{
			buffer.writeInt(_summon.getTransformationDisplayId()); // Transformation ID
		}
		
		if (containsMask(NpcInfoType.CURRENT_HP))
		{
			buffer.writeLong((long) _summon.getCurrentHp());
		}
		
		if (containsMask(NpcInfoType.CURRENT_MP))
		{
			buffer.writeInt((int) _summon.getCurrentMp());
		}
		
		if (containsMask(NpcInfoType.MAX_HP))
		{
			buffer.writeLong(_summon.getMaxHp());
		}
		
		if (containsMask(NpcInfoType.MAX_MP))
		{
			buffer.writeInt(_summon.getMaxMp());
		}
		
		if (containsMask(NpcInfoType.SUMMONED))
		{
			buffer.writeByte(_summon.isShowSummonAnimation() ? 2 : 0); // 2 - do some animation on spawn
		}
		
		if (containsMask(NpcInfoType.FOLLOW_INFO))
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
		
		if (containsMask(NpcInfoType.NAME))
		{
			buffer.writeString(_summon.getName());
		}
		
		if (containsMask(NpcInfoType.NAME_NPCSTRINGID))
		{
			buffer.writeInt(-1); // NPCStringId for name
		}
		
		if (containsMask(NpcInfoType.TITLE_NPCSTRINGID))
		{
			buffer.writeInt(-1); // NPCStringId for title
		}
		
		if (containsMask(NpcInfoType.PVP_FLAG))
		{
			buffer.writeByte(_summon.getPvpFlag()); // PVP flag
		}
		
		if (containsMask(NpcInfoType.REPUTATION))
		{
			buffer.writeInt(_summon.getReputation()); // Name color
		}
		
		if (containsMask(NpcInfoType.CLAN))
		{
			buffer.writeInt(_clanId);
			buffer.writeInt(_clanCrest);
			buffer.writeInt(_clanLargeCrest);
			buffer.writeInt(_allyId);
			buffer.writeInt(_allyCrest);
		}
		
		if (containsMask(NpcInfoType.VISUAL_STATE))
		{
			buffer.writeByte(_statusMask);
		}
		
		if (containsMask(NpcInfoType.SHOW_NAME))
		{
			buffer.writeByte(1);
		}
		
		if (containsMask(NpcInfoType.ABNORMALS))
		{
			buffer.writeInt(0); // 493
			final Team team = (GeneralConfig.BLUE_TEAM_ABNORMAL_EFFECT != null) && (GeneralConfig.RED_TEAM_ABNORMAL_EFFECT != null) ? _summon.getTeam() : Team.NONE;
			buffer.writeShort(_abnormalVisualEffects.size() + (_summon.isInvisible() ? 1 : 0) + (team != Team.NONE ? 1 : 0));
			for (AbnormalVisualEffect abnormalVisualEffect : _abnormalVisualEffects)
			{
				buffer.writeShort(abnormalVisualEffect.getClientId());
			}
			
			if (_summon.isInvisible())
			{
				buffer.writeShort(AbnormalVisualEffect.STEALTH.getClientId());
			}
			
			if (team == Team.BLUE)
			{
				if (GeneralConfig.BLUE_TEAM_ABNORMAL_EFFECT != null)
				{
					buffer.writeShort(GeneralConfig.BLUE_TEAM_ABNORMAL_EFFECT.getClientId());
				}
			}
			else if ((team == Team.RED) && (GeneralConfig.RED_TEAM_ABNORMAL_EFFECT != null))
			{
				buffer.writeShort(GeneralConfig.RED_TEAM_ABNORMAL_EFFECT.getClientId());
			}
		}
	}
}
