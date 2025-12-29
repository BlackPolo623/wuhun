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
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.actor.instance.Decoy;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class CharInfo extends ServerPacket
{
	private static final int[] PAPERDOLL_ORDER =
	{
		Inventory.PAPERDOLL_UNDER,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_LHAND,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_CLOAK,
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_HAIR,
		Inventory.PAPERDOLL_HAIR2
	};
	
	private final Player _player;
	private final Clan _clan;
	private int _objId;
	private int _x;
	private int _y;
	private int _z;
	private int _heading;
	private final int _mAtkSpd;
	private final int _pAtkSpd;
	private final int _runSpd;
	private final int _walkSpd;
	private final int _swimRunSpd;
	private final int _swimWalkSpd;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final double _moveMultiplier;
	private final float _attackSpeedMultiplier;
	private int _enchantLevel = 0;
	private int _armorEnchant = 0;
	private int _vehicleId = 0;
	private final PlayerAppearance _appearance;
	private final Inventory _inventory;
	private final ILocational _baitLocation;
	private final Set<AbnormalVisualEffect> _abnormalVisualEffects;
	private final Team _team;
	private final int _afkAnimation;
	private final int _rank;
	private final boolean _gmSeeInvis;
	
	public CharInfo(Player player, boolean gmSeeInvis)
	{
		_player = player;
		_objId = player.getObjectId();
		_clan = player.getClan();
		if ((player.getVehicle() != null) && (player.getInVehiclePosition() != null))
		{
			_x = player.getInVehiclePosition().getX();
			_y = player.getInVehiclePosition().getY();
			_z = player.getInVehiclePosition().getZ();
			_vehicleId = player.getVehicle().getObjectId();
		}
		else
		{
			_x = player.getX();
			_y = player.getY();
			_z = player.getZ();
		}
		
		_heading = player.getHeading();
		_mAtkSpd = player.getMAtkSpd();
		_pAtkSpd = player.getPAtkSpd();
		_attackSpeedMultiplier = (float) player.getAttackSpeedMultiplier();
		_moveMultiplier = player.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(player.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(player.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(player.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(player.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = player.isFlying() ? _runSpd : 0;
		_flyWalkSpd = player.isFlying() ? _walkSpd : 0;
		_appearance = player.getAppearance();
		_inventory = player.getInventory();
		_enchantLevel = _inventory.getWeaponEnchant();
		_armorEnchant = _inventory.getArmorSetEnchant();
		_baitLocation = player.getFishing().getBaitLocation();
		_abnormalVisualEffects = player.getEffectList().getCurrentAbnormalVisualEffects();
		_team = (GeneralConfig.BLUE_TEAM_ABNORMAL_EFFECT != null) && (GeneralConfig.RED_TEAM_ABNORMAL_EFFECT != null) ? player.getTeam() : Team.NONE;
		_afkAnimation = ((player.getClan() != null) && (CastleManager.getInstance().getCastleByOwner(player.getClan()) != null)) ? (player.isClanLeader() ? 100 : 101) : 0;
		_rank = OlympiadConfig.OLYMPIAD_HIDE_NAMES && player.isInOlympiadMode() ? 0 : RankManager.getInstance().getPlayerGlobalRank(player) == 1 ? 1 : RankManager.getInstance().getPlayerRaceRank(player) == 1 ? 2 : 0;
		_gmSeeInvis = gmSeeInvis;
	}
	
	public CharInfo(Decoy decoy, boolean gmSeeInvis)
	{
		this(decoy.asPlayer(), gmSeeInvis); // init
		_objId = decoy.getObjectId();
		_x = decoy.getX();
		_y = decoy.getY();
		_z = decoy.getZ();
		_heading = decoy.getHeading();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.CHAR_INFO.writeId(this, buffer);
		buffer.writeByte(0); // Grand Crusade
		buffer.writeInt(_x); // Confirmed
		buffer.writeInt(_y); // Confirmed
		buffer.writeInt(_z); // Confirmed
		buffer.writeInt(_vehicleId); // Confirmed
		buffer.writeInt(_objId); // Confirmed
		
		buffer.writeString(_player.isMercenary() ? _player.getMercenaryName() : _appearance.getVisibleName()); // Confirmed
		buffer.writeShort(_player.getRace().ordinal()); // Confirmed
		buffer.writeByte(_appearance.isFemale()); // Confirmed
		buffer.writeInt(_player.getBaseTemplate().getPlayerClass().getRootClass().getId());
		
		for (int slot : getPaperdollOrder())
		{
			buffer.writeInt(_inventory.getPaperdollItemDisplayId(slot)); // Confirmed
		}
		
		for (int slot : getPaperdollOrderAugument())
		{
			final VariationInstance augment = _inventory.getPaperdollAugmentation(slot);
			if (augment != null)
			{
				buffer.writeInt(augment.getOption1Id());
				buffer.writeInt(augment.getOption2Id());
				buffer.writeInt(augment.getOption3Id());
			}
			else
			{
				buffer.writeInt(0);
				buffer.writeInt(0);
				buffer.writeInt(0);
			}
		}
		
		for (int slot : getPaperdollOrderAugument())
		{
			final VariationInstance augment = _inventory.getPaperdollAugmentation(slot);
			if (augment != null)
			{
				buffer.writeInt(augment.getOption1Id());
				buffer.writeInt(augment.getOption2Id());
				buffer.writeInt(augment.getOption3Id());
			}
			else
			{
				buffer.writeInt(0);
				buffer.writeInt(0);
				buffer.writeInt(0);
			}
		}
		
		buffer.writeByte(_armorEnchant);
		
		for (int slot : getPaperdollOrderVisualId())
		{
			buffer.writeInt(_inventory.getPaperdollItemVisualId(slot));
		}
		
		buffer.writeByte(_player.getPvpFlag());
		buffer.writeInt(_player.getReputation());
		buffer.writeInt(_mAtkSpd);
		buffer.writeInt(_pAtkSpd);
		buffer.writeShort(_runSpd);
		buffer.writeShort(_walkSpd);
		buffer.writeShort(_swimRunSpd);
		buffer.writeShort(_swimWalkSpd);
		buffer.writeShort(_flyRunSpd);
		buffer.writeShort(_flyWalkSpd);
		buffer.writeShort(_flyRunSpd);
		buffer.writeShort(_flyWalkSpd);
		buffer.writeDouble(_moveMultiplier);
		buffer.writeDouble(_attackSpeedMultiplier);
		buffer.writeDouble(_player.getCollisionRadius());
		buffer.writeDouble(_player.getCollisionHeight());
		buffer.writeInt(_player.getVisualHair());
		buffer.writeInt(_player.getVisualHairColor());
		buffer.writeInt(_player.getVisualFace());
		buffer.writeString(_gmSeeInvis ? "Invisible" : _player.isMercenary() ? "" : _appearance.getVisibleTitle());
		buffer.writeInt(_appearance.getVisibleClanId());
		buffer.writeInt(_appearance.getVisibleClanCrestId());
		buffer.writeInt(_appearance.getVisibleAllyId());
		buffer.writeInt(_appearance.getVisibleAllyCrestId());
		buffer.writeByte(!_player.isSitting()); // Confirmed
		buffer.writeByte(_player.isRunning()); // Confirmed
		buffer.writeByte(_player.isInCombat()); // Confirmed
		buffer.writeByte(!_player.isInOlympiadMode() && _player.isAlikeDead()); // Confirmed
		buffer.writeByte(_player.isInvisible());
		buffer.writeByte(_player.getMountType().ordinal()); // 1-on Strider, 2-on Wyvern, 3-on Great Wolf, 0-no mount
		buffer.writeByte(_player.getPrivateStoreType().getId()); // Confirmed
		
		buffer.writeShort(_player.getCubics().size()); // Confirmed
		_player.getCubics().keySet().forEach(buffer::writeShort);
		
		buffer.writeByte(_player.isInMatchingRoom()); // Confirmed
		buffer.writeByte(_player.isInsideZone(ZoneId.WATER) ? 1 : _player.isFlyingMounted() ? 2 : 0);
		buffer.writeShort(_player.getRecomHave()); // Confirmed
		buffer.writeInt(_player.getMountNpcId() == 0 ? 0 : _player.getMountNpcId() + 1000000);
		buffer.writeInt(_player.getPlayerClass().getId()); // Confirmed
		buffer.writeInt(0); // TODO: Find me!
		buffer.writeByte(_player.isMounted() ? 0 : _enchantLevel); // Confirmed
		buffer.writeByte(_player.getTeam().getId()); // Confirmed
		buffer.writeInt(_player.getClanCrestLargeId());
		buffer.writeByte(_player.isNoble()); // Confirmed
		buffer.writeByte(_player.isHero() || (_player.isGM() && GeneralConfig.GM_HERO_AURA) ? 2 : 0); // 152 - Value for enabled changed to 2?
		
		buffer.writeByte(_player.isFishing()); // Confirmed
		if (_baitLocation != null)
		{
			buffer.writeInt(_baitLocation.getX()); // Confirmed
			buffer.writeInt(_baitLocation.getY()); // Confirmed
			buffer.writeInt(_baitLocation.getZ()); // Confirmed
		}
		else
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
		
		buffer.writeInt(_appearance.getNameColor()); // Confirmed
		buffer.writeInt(_heading); // Confirmed
		buffer.writeByte(_player.getPledgeClass());
		buffer.writeShort(_player.getPledgeType());
		buffer.writeInt(_appearance.getTitleColor()); // Confirmed
		buffer.writeByte(_player.isCursedWeaponEquipped() ? CursedWeaponsManager.getInstance().getLevel(_player.getCursedWeaponEquippedId()) : 0);
		buffer.writeInt(_clan != null ? _clan.getReputationScore() : 0);
		buffer.writeInt(_player.getTransformationDisplayId()); // Confirmed
		buffer.writeInt(_player.getAgathionId()); // Confirmed
		buffer.writeByte(0); // nPvPRestrainStatus
		buffer.writeInt((int) Math.round(_player.getCurrentCp())); // Confirmed
		buffer.writeInt((int) _player.getMaxHp()); // Confirmed
		buffer.writeInt((int) Math.round(_player.getCurrentHp())); // Confirmed
		buffer.writeInt(_player.getMaxMp()); // Confirmed
		buffer.writeInt((int) Math.round(_player.getCurrentMp())); // Confirmed
		buffer.writeByte(0); // cBRLectureMark
		
		buffer.writeInt(_abnormalVisualEffects.size() + (_gmSeeInvis ? 1 : 0) + (_team != Team.NONE ? 1 : 0)); // Confirmed
		for (AbnormalVisualEffect abnormalVisualEffect : _abnormalVisualEffects)
		{
			buffer.writeShort(abnormalVisualEffect.getClientId()); // Confirmed
		}
		
		if (_gmSeeInvis)
		{
			buffer.writeShort(AbnormalVisualEffect.STEALTH.getClientId());
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
		
		buffer.writeByte(_player.isTrueHero() ? 100 : 0);
		buffer.writeByte(_player.isHairAccessoryEnabled()); // Hair accessory
		buffer.writeByte(_player.getAbilityPointsUsed()); // Used Ability Points
		buffer.writeInt(0); // nCursedWeaponClassId
		
		// AFK animation.
		buffer.writeInt(_afkAnimation);
		
		// Rank.
		buffer.writeInt(_rank);
		buffer.writeShort(0);
		buffer.writeByte(0);
		buffer.writeInt(_player.getPlayerClass().getId());
		buffer.writeByte(0);
		
		switch (_player.getBaseClass())
		{
			case 247: // Warg 0
			case 248: // Warg 1
			case 249: // Warg 2
			case 250: // Warg 3
			case 251: // Blood Rose 0
			case 252: // Blood Rose 1
			case 253: // Blood Rose 2
			case 254: // Blood Rose 3
			{
				buffer.writeInt(_player.getVisualHairColor() - 1);
				break;
			}
			default: // 338 - DK color.
			{
				buffer.writeInt(_player.getVisualHairColor() + 1);
				break;
			}
		}
		
		buffer.writeInt(0);
		buffer.writeByte(_player.getPlayerClass().level() + 1); // 362 - Vanguard mount.
		buffer.writeInt(0); // 520 - Perfect?
	}
	
	@Override
	public int[] getPaperdollOrder()
	{
		return PAPERDOLL_ORDER;
	}
}
