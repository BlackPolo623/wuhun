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
import org.l2jmobius.gameserver.config.NpcConfig;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.NpcNameLocalisationData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.actor.instance.Doppelganger;
import org.l2jmobius.gameserver.model.actor.instance.Guard;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.NpcInfoType;

/**
 * @author Mobius
 */
public class NpcInfo extends AbstractMaskPacket<NpcInfoType>
{
	private final Npc _npc;
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
	private final Set<AbnormalVisualEffect> _abnormalVisualEffects;
	
	public NpcInfo(Npc npc)
	{
		_npc = npc;
		_abnormalVisualEffects = npc.getEffectList().getCurrentAbnormalVisualEffects();
		addComponentType(NpcInfoType.ATTACKABLE, NpcInfoType.RELATIONS, NpcInfoType.ID, NpcInfoType.POSITION, NpcInfoType.ALIVE, NpcInfoType.RUNNING);
		if (npc.getHeading() > 0)
		{
			addComponentType(NpcInfoType.HEADING);
		}
		
		if ((npc.getStat().getPAtkSpd() > 0) || (npc.getStat().getMAtkSpd() > 0))
		{
			addComponentType(NpcInfoType.ATK_CAST_SPEED);
		}
		
		if (npc.getRunSpeed() > 0)
		{
			addComponentType(NpcInfoType.SPEED_MULTIPLIER);
		}
		
		if ((npc.getLeftHandItem() > 0) || (npc.getRightHandItem() > 0))
		{
			addComponentType(NpcInfoType.EQUIPPED);
		}
		
		if (npc.getTeam() != Team.NONE)
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
		
		if (npc.getDisplayEffect() > 0)
		{
			addComponentType(NpcInfoType.DISPLAY_EFFECT);
		}
		
		if (npc.isInsideZone(ZoneId.WATER) || npc.isFlying())
		{
			addComponentType(NpcInfoType.SWIM_OR_FLY);
		}
		
		if (npc.isFlying())
		{
			addComponentType(NpcInfoType.FLYING);
		}
		
		if (npc.getCloneObjId() > 0)
		{
			addComponentType(NpcInfoType.CLONE);
		}
		
		if (npc.getMaxHp() > 0)
		{
			addComponentType(NpcInfoType.MAX_HP);
		}
		
		if (npc.getMaxMp() > 0)
		{
			addComponentType(NpcInfoType.MAX_MP);
		}
		
		if (npc.getCurrentHp() <= npc.getMaxHp())
		{
			addComponentType(NpcInfoType.CURRENT_HP);
		}
		
		if (npc.getCurrentMp() <= npc.getMaxMp())
		{
			addComponentType(NpcInfoType.CURRENT_MP);
		}
		
		if (npc.getTemplate().isUsingServerSideName())
		{
			addComponentType(NpcInfoType.NAME);
		}
		
		if (npc.getTemplate().isShowName())
		{
			addComponentType(NpcInfoType.SHOW_NAME);
		}
		
		if (npc.getTemplate().isUsingServerSideTitle() || (npc.isMonster() && (NpcConfig.SHOW_NPC_LEVEL || NpcConfig.SHOW_NPC_AGGRESSION)) || npc.isChampion() || npc.isTrap())
		{
			addComponentType(NpcInfoType.TITLE);
		}
		
		if (npc.getNameString() != null)
		{
			addComponentType(NpcInfoType.NAME_NPCSTRINGID);
		}
		
		if (npc.getTitleString() != null)
		{
			addComponentType(NpcInfoType.TITLE_NPCSTRINGID);
		}
		
		if (_npc.getReputation() != 0)
		{
			addComponentType(NpcInfoType.REPUTATION);
		}
		
		if (!_abnormalVisualEffects.isEmpty() || npc.isInvisible())
		{
			addComponentType(NpcInfoType.ABNORMALS);
		}
		
		if (npc.getEnchantEffect() > 0)
		{
			addComponentType(NpcInfoType.ENCHANT);
		}
		
		if (npc.getTransformationDisplayId() > 0)
		{
			addComponentType(NpcInfoType.TRANSFORMATION);
		}
		
		if (npc.isShowSummonAnimation())
		{
			addComponentType(NpcInfoType.SUMMONED);
		}
		
		if (npc.getClanId() > 0)
		{
			final Clan clan = ClanTable.getInstance().getClan(npc.getClanId());
			if ((clan != null) && ((npc instanceof Doppelganger) || (npc.getTemplate().getId() == 34156 /* Clan Stronghold Device */) || (!npc.isMonster() && npc.isInsideZone(ZoneId.PEACE))))
			{
				_clanId = clan.getId();
				_clanCrest = clan.getCrestId();
				_clanLargeCrest = clan.getCrestLargeId();
				_allyCrest = clan.getAllyCrestId();
				_allyId = clan.getAllyId();
				addComponentType(NpcInfoType.CLAN);
			}
		}
		
		addComponentType(NpcInfoType.PET_EVOLUTION_ID);
		if (npc.getPvpFlag() > 0)
		{
			addComponentType(NpcInfoType.PVP_FLAG);
		}
		
		// TODO: Confirm me
		if (npc.isInCombat())
		{
			_statusMask |= 0x01;
		}
		
		if (npc.isDead())
		{
			_statusMask |= 0x02;
		}
		
		if (npc.isTargetable())
		{
			_statusMask |= 0x04;
		}
		
		if (npc.isShowName())
		{
			_statusMask |= 0x08;
		}
		
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
		calcBlockSize(_npc, component);
	}
	
	private void calcBlockSize(Npc npc, NpcInfoType type)
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
				_initSize += type.getBlockLength() + (npc.getTitle().length() * 2);
				break;
			}
			case NAME:
			{
				_blockSize += type.getBlockLength() + (npc.getName().length() * 2);
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
		if (_npc.isDecayed())
		{
			return;
		}
		
		// Localisation related.
		String[] localisation = null;
		if (MultilingualSupportConfig.MULTILANG_ENABLE)
		{
			final Player player = client.getPlayer();
			if (player != null)
			{
				final String lang = player.getLang();
				if ((lang != null) && !lang.equals("en"))
				{
					localisation = NpcNameLocalisationData.getInstance().getLocalisation(lang, _npc.getId());
					if (localisation != null)
					{
						if (!containsMask(NpcInfoType.NAME))
						{
							addComponentType(NpcInfoType.NAME);
						}
						
						_blockSize -= _npc.getName().length() * 2;
						_blockSize += localisation[0].length() * 2;
						if (!localisation[1].equals(""))
						{
							if (!containsMask(NpcInfoType.TITLE))
							{
								addComponentType(NpcInfoType.TITLE);
							}
							
							final String title = _npc.getTitle();
							_initSize -= title.length() * 2;
							if (title.equals(""))
							{
								_initSize += localisation[1].length() * 2;
							}
							else
							{
								_initSize += title.replace(NpcData.getInstance().getTemplate(_npc.getId()).getTitle(), localisation[1]).length() * 2;
							}
						}
					}
				}
			}
		}
		
		ServerPackets.NPC_INFO.writeId(this, buffer);
		buffer.writeInt(_npc.getObjectId());
		buffer.writeByte(_npc.isShowSummonAnimation() ? 2 : 0); // // 0=teleported 1=default 2=summoned
		buffer.writeShort(40); // 520 - mask_bits_40
		buffer.writeBytes(_masks);
		
		// Block 1
		buffer.writeByte(_initSize);
		if (containsMask(NpcInfoType.ATTACKABLE))
		{
			buffer.writeByte(_npc.isAttackable() && !(_npc instanceof Guard));
		}
		
		if (containsMask(NpcInfoType.RELATIONS))
		{
			buffer.writeLong(0);
		}
		
		if (containsMask(NpcInfoType.TITLE))
		{
			String title = _npc.getTitle();
			
			// Localisation related.
			if ((localisation != null) && !localisation[1].equals(""))
			{
				if (title.equals(""))
				{
					title = localisation[1];
				}
				else
				{
					title = title.replace(NpcData.getInstance().getTemplate(_npc.getId()).getTitle(), localisation[1]);
				}
			}
			
			buffer.writeString(title);
		}
		
		// Block 2
		buffer.writeShort(_blockSize);
		if (containsMask(NpcInfoType.ID))
		{
			buffer.writeInt(_npc.getTemplate().getDisplayId() + 1000000);
		}
		
		if (containsMask(NpcInfoType.POSITION))
		{
			buffer.writeInt(_npc.getX());
			buffer.writeInt(_npc.getY());
			buffer.writeInt(_npc.getZ());
		}
		
		if (containsMask(NpcInfoType.HEADING))
		{
			buffer.writeInt(_npc.getHeading());
		}
		
		if (containsMask(NpcInfoType.VEHICLE_ID))
		{
			buffer.writeInt(0); // Vehicle object id.
		}
		
		if (containsMask(NpcInfoType.ATK_CAST_SPEED))
		{
			buffer.writeInt(_npc.getPAtkSpd());
			buffer.writeInt(_npc.getMAtkSpd());
		}
		
		if (containsMask(NpcInfoType.SPEED_MULTIPLIER))
		{
			buffer.writeFloat((float) _npc.getStat().getMovementSpeedMultiplier());
			buffer.writeFloat((float) _npc.getStat().getAttackSpeedMultiplier());
		}
		
		if (containsMask(NpcInfoType.EQUIPPED))
		{
			buffer.writeInt(_npc.getRightHandItem());
			buffer.writeInt(0); // Armor id?
			buffer.writeInt(_npc.getLeftHandItem());
		}
		
		if (containsMask(NpcInfoType.ALIVE))
		{
			buffer.writeByte(!_npc.isDead());
		}
		
		if (containsMask(NpcInfoType.RUNNING))
		{
			buffer.writeByte(_npc.isRunning());
		}
		
		if (containsMask(NpcInfoType.SWIM_OR_FLY))
		{
			buffer.writeByte(_npc.isInsideZone(ZoneId.WATER) ? 1 : _npc.isFlying() ? 2 : 0);
		}
		
		if (containsMask(NpcInfoType.TEAM))
		{
			buffer.writeByte(_npc.getTeam().getId());
		}
		
		if (containsMask(NpcInfoType.ENCHANT))
		{
			buffer.writeInt(_npc.getEnchantEffect());
		}
		
		if (containsMask(NpcInfoType.FLYING))
		{
			buffer.writeInt(_npc.isFlying());
		}
		
		if (containsMask(NpcInfoType.CLONE))
		{
			buffer.writeInt(_npc.getCloneObjId()); // Player ObjectId with Decoy
		}
		
		if (containsMask(NpcInfoType.PET_EVOLUTION_ID))
		{
			buffer.writeInt(0); // Unknown
		}
		
		if (containsMask(NpcInfoType.DISPLAY_EFFECT))
		{
			buffer.writeInt(_npc.getDisplayEffect());
		}
		
		if (containsMask(NpcInfoType.TRANSFORMATION))
		{
			buffer.writeInt(_npc.getTransformationDisplayId()); // Transformation ID
		}
		
		if (containsMask(NpcInfoType.CURRENT_HP))
		{
			buffer.writeLong((long) _npc.getCurrentHp());
		}
		
		if (containsMask(NpcInfoType.CURRENT_MP))
		{
			buffer.writeInt((int) _npc.getCurrentMp());
		}
		
		if (containsMask(NpcInfoType.MAX_HP))
		{
			buffer.writeLong(_npc.getMaxHp());
		}
		
		if (containsMask(NpcInfoType.MAX_MP))
		{
			buffer.writeInt(_npc.getMaxMp());
		}
		
		if (containsMask(NpcInfoType.SUMMONED))
		{
			buffer.writeByte(0); // 2 - do some animation on spawn
		}
		
		if (containsMask(NpcInfoType.FOLLOW_INFO))
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
		
		if (containsMask(NpcInfoType.NAME))
		{
			buffer.writeString(localisation != null ? localisation[0] : _npc.getName());
		}
		
		if (containsMask(NpcInfoType.NAME_NPCSTRINGID))
		{
			final NpcStringId nameString = _npc.getNameString();
			buffer.writeInt(nameString != null ? nameString.getId() : -1); // NPCStringId for name
		}
		
		if (containsMask(NpcInfoType.TITLE_NPCSTRINGID))
		{
			final NpcStringId titleString = _npc.getTitleString();
			buffer.writeInt(titleString != null ? titleString.getId() : -1); // NPCStringId for title
		}
		
		if (containsMask(NpcInfoType.PVP_FLAG))
		{
			buffer.writeByte(_npc.getPvpFlag()); // PVP flag
		}
		
		if (containsMask(NpcInfoType.REPUTATION))
		{
			buffer.writeInt(_npc.getReputation()); // Reputation
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
			final Team team = (GeneralConfig.BLUE_TEAM_ABNORMAL_EFFECT != null) && (GeneralConfig.RED_TEAM_ABNORMAL_EFFECT != null) ? _npc.getTeam() : Team.NONE;
			buffer.writeShort(_abnormalVisualEffects.size() + (_npc.isInvisible() ? 1 : 0) + (team != Team.NONE ? 1 : 0));
			for (AbnormalVisualEffect abnormalVisualEffect : _abnormalVisualEffects)
			{
				buffer.writeShort(abnormalVisualEffect.getClientId());
			}
			
			if (_npc.isInvisible())
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
