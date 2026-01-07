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

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.model.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.UserInfoType;

/**
 * @author Mobius
 */
public class UserInfo extends AbstractMaskPacket<UserInfoType>
{
	private Player _player;
	private int _relation;
	private int _runSpd;
	private int _walkSpd;
	private int _swimRunSpd;
	private int _swimWalkSpd;
	private final int _flRunSpd = 0;
	private final int _flWalkSpd = 0;
	private int _flyRunSpd;
	private int _flyWalkSpd;
	private double _moveMultiplier;
	private int _enchantLevel;
	private int _armorEnchant;
	private String _title;
	private PlayerAppearance _appearance;
	private Inventory _inventory;
	private PlayerVariables _variables;
	private int _afkAnimation;
	private int _rank;
	private final byte[] _masks =
	{
		(byte) 0x00,
		(byte) 0x00,
		(byte) 0x00,
		(byte) 0x00
	};
	private int _initSize = 5;
	
	public UserInfo(Player player)
	{
		this(player, true);
	}
	
	public UserInfo(Player player, boolean addAll)
	{
		if (!player.isSubclassLocked()) // Changing class.
		{
			_player = player;
			_relation = calculateRelation(player);
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
			_variables = player.getVariables();
			_afkAnimation = ((player.getClan() != null) && (CastleManager.getInstance().getCastleByOwner(player.getClan()) != null)) ? (player.isClanLeader() ? 100 : 101) : 0;
			_rank = RankManager.getInstance().getPlayerGlobalRank(player) == 1 ? 1 : RankManager.getInstance().getPlayerRaceRank(player) == 1 ? 2 : 0;
			_title = player.getTitle();
			
			if (player.isGM() && player.isInvisible())
			{
				_title = "[Invisible]";
			}
			
			if (addAll)
			{
				addComponentType(UserInfoType.values());
			}
		}
	}
	
	@Override
	protected byte[] getMasks()
	{
		return _masks;
	}
	
	@Override
	protected void onNewMaskAdded(UserInfoType component)
	{
		calcBlockSize(component);
	}
	
	private void calcBlockSize(UserInfoType type)
	{
		switch (type)
		{
			case BASIC_INFO:
			{
				if (_player.isMercenary())
				{
					_initSize += type.getBlockLength() + (_player.getMercenaryName().length() * 2);
				}
				else
				{
					_initSize += type.getBlockLength() + (_appearance.getVisibleName().length() * 2);
				}
				break;
			}
			case CLAN:
			{
				_initSize += type.getBlockLength() + (_title.length() * 2);
				break;
			}
			default:
			{
				_initSize += type.getBlockLength();
				break;
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
		
		ServerPackets.USER_INFO.writeId(this, buffer);
		buffer.writeInt(_player.getObjectId());
		buffer.writeInt(_initSize);
		buffer.writeShort(30); // 520 - 30
		buffer.writeBytes(_masks);
		if (containsMask(UserInfoType.RELATION))
		{
			buffer.writeInt(_relation);
		}
		
		if (containsMask(UserInfoType.BASIC_INFO))
		{
			if (_player.isMercenary())
			{
				buffer.writeShort(23 + (_player.getMercenaryName().length() * 2));
				buffer.writeSizedString(_player.getMercenaryName());
			}
			else
			{
				buffer.writeShort(23 + (_appearance.getVisibleName().length() * 2));
				buffer.writeSizedString(_player.getName());
			}
			
			buffer.writeByte(_player.isGM());
			int j = _player.getVariables().getInt("外形幻化", 0);
			int z = 0;
			switch (j)
			{
				case 1:
					z = 88;
					break;
				case 2:
					z = 94;
					break;
				case 5:
					z = 113;
					break;
				case 6:
					z = 115;
					break;
				case 9:
					z = 199;
					break;
				case 10:
					z = 203;
					break;
				case 11:
					z = 207;
					break;
				case 12:
					z = 220;
					break;
				case 13:
					z = 224;
					break;
				case 14:
					z = 228;
					break;
				case 16:
					z = 250;
					break;
				case 17:
					z = 254;
					break;
				case 18:
					z = 263;
					break;
			}
			buffer.writeByte(j > 0 ? _player.getRacesa().ordinal() : _player.getRace().ordinal());
			buffer.writeByte(_appearance.isFemale());
			buffer.writeInt(z > 0 ? z : _player.getBaseTemplate().getPlayerClass().getRootClass().getId());
			buffer.writeInt(z > 0 ? z : _player.getPlayerClass().getId());
			buffer.writeInt(_player.getLevel()); // 270
			buffer.writeInt(z > 0 ? z : _player.getPlayerClass().getId()); // 286
		}
		
		if (containsMask(UserInfoType.BASE_STATS))
		{
			buffer.writeShort(18);
			buffer.writeShort(_player.getSTR());
			buffer.writeShort(_player.getDEX());
			buffer.writeShort(_player.getCON());
			buffer.writeShort(_player.getINT());
			buffer.writeShort(_player.getWIT());
			buffer.writeShort(_player.getMEN());
			buffer.writeShort(0);
			buffer.writeShort(0);
		}
		
		if (containsMask(UserInfoType.MAX_HPCPMP))
		{
			buffer.writeShort(14);
			buffer.writeInt((int) _player.getMaxHp());
			buffer.writeInt(_player.getMaxMp());
			buffer.writeInt(_player.getMaxCp());
		}
		
		if (containsMask(UserInfoType.CURRENT_HPMPCP_EXP_SP))
		{
			buffer.writeShort(39);
			buffer.writeInt((int) Math.round(_player.getCurrentHp()));
			buffer.writeInt((int) Math.round(_player.getCurrentMp()));
			buffer.writeInt((int) Math.round(_player.getCurrentCp()));
			buffer.writeLong(_player.getSp());
			buffer.writeLong(_player.getExp());
			buffer.writeDouble((float) (_player.getExp() - ExperienceData.getInstance().getExpForLevel(_player.getLevel())) / (ExperienceData.getInstance().getExpForLevel(_player.getLevel() + 1) - ExperienceData.getInstance().getExpForLevel(_player.getLevel())));
			buffer.writeByte(0); // 430
		}
		
		if (containsMask(UserInfoType.ENCHANTLEVEL))
		{
			buffer.writeShort(7); // 464
			buffer.writeByte(_enchantLevel);
			buffer.writeByte(_armorEnchant);
			buffer.writeByte(0); // 338 - cBackEnchant?
			buffer.writeByte(0); // 464
			buffer.writeByte(0); // 464
		}
		
		if (containsMask(UserInfoType.APPAREANCE))
		{
			buffer.writeShort(19); // 338
			buffer.writeInt(_player.getVisualHair());
			buffer.writeInt(_player.getVisualHairColor());
			buffer.writeInt(_player.getVisualFace());
			buffer.writeByte(_player.isHairAccessoryEnabled());
			
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
		}
		
		if (containsMask(UserInfoType.STATUS))
		{
			buffer.writeShort(6);
			buffer.writeByte(_player.getMountType().ordinal());
			buffer.writeByte(_player.getPrivateStoreType().getId());
			buffer.writeByte(_player.hasDwarvenCraft() || (_player.getSkillLevel(248) > 0));
			buffer.writeByte(0);
		}
		
		if (containsMask(UserInfoType.STATS))
		{
			buffer.writeShort(76); // 520
			buffer.writeShort(_player.getActiveWeaponItem() != null ? 40 : 20);
			buffer.writeLong(_player.getPAtk());
			buffer.writeInt(_player.getPAtkSpd());
			buffer.writeInt(_player.getPDef());
			buffer.writeInt(_player.getEvasionRate());
			buffer.writeInt(_player.getAccuracy());
			buffer.writeInt(_player.getCriticalHit());
			buffer.writeLong(_player.getMAtk());
			buffer.writeInt(_player.getMAtkSpd());
			buffer.writeInt(_player.getPAtkSpd()); // Seems like atk speed - 1
			buffer.writeInt(_player.getMagicEvasionRate());
			buffer.writeInt(_player.getMDef());
			buffer.writeInt(_player.getMagicAccuracy());
			buffer.writeInt(_player.getMCriticalHit());
			buffer.writeInt(_player.getWeaponBonusPAtk()); // 270
			buffer.writeInt(_player.getWeaponBonusMAtk()); // 270
			buffer.writeInt(_player.getPSkillCriticalRate()); // 464
		}
		
		if (containsMask(UserInfoType.ELEMENTALS))
		{
			buffer.writeShort(14);
			buffer.writeShort(0);
			buffer.writeShort(0);
			buffer.writeShort(0);
			buffer.writeShort(0);
			buffer.writeShort(0);
			buffer.writeShort(0);
		}
		
		if (containsMask(UserInfoType.POSITION))
		{
			buffer.writeShort(18);
			buffer.writeInt(_player.getX());
			buffer.writeInt(_player.getY());
			buffer.writeInt(_player.getZ());
			buffer.writeInt(_player.isInVehicle() ? _player.getVehicle().getObjectId() : 0);
		}
		
		if (containsMask(UserInfoType.SPEED))
		{
			buffer.writeShort(18);
			buffer.writeShort(_runSpd);
			buffer.writeShort(_walkSpd);
			buffer.writeShort(_swimRunSpd);
			buffer.writeShort(_swimWalkSpd);
			buffer.writeShort(_flRunSpd);
			buffer.writeShort(_flWalkSpd);
			buffer.writeShort(_flyRunSpd);
			buffer.writeShort(_flyWalkSpd);
		}
		
		if (containsMask(UserInfoType.MULTIPLIER))
		{
			buffer.writeShort(18);
			buffer.writeDouble(_moveMultiplier);
			buffer.writeDouble(_player.getAttackSpeedMultiplier());
		}
		
		if (containsMask(UserInfoType.COL_RADIUS_HEIGHT))
		{
			buffer.writeShort(18);
			buffer.writeDouble(_player.getCollisionRadius());
			buffer.writeDouble(_player.getCollisionHeight());
		}
		
		if (containsMask(UserInfoType.ATK_ELEMENTAL))
		{
			buffer.writeShort(5);
			buffer.writeByte(0);
			buffer.writeShort(0);
		}
		
		if (containsMask(UserInfoType.CLAN))
		{
			buffer.writeShort(32 + (_title.length() * 2));
			buffer.writeSizedString(_title);
			buffer.writeShort(_player.getPledgeType());
			buffer.writeInt(_player.getClanId());
			buffer.writeInt(_player.getClanCrestLargeId());
			buffer.writeInt(_player.getClanCrestId());
			buffer.writeInt(_player.getClanPrivileges().getMask());
			buffer.writeByte(_player.isClanLeader());
			buffer.writeInt(_player.getAllyId());
			buffer.writeInt(_player.getAllyCrestId());
			buffer.writeByte(_player.isInMatchingRoom());
		}
		
		if (containsMask(UserInfoType.SOCIAL))
		{
			buffer.writeShort(34); // 447
			buffer.writeByte(_player.getPvpFlag());
			buffer.writeInt(_player.getReputation()); // Reputation
			buffer.writeByte(_player.isNoble());
			buffer.writeByte(_player.isHero() || (_player.isGM() && GeneralConfig.GM_HERO_AURA) ? 2 : 0); // 152 - Value for enabled changed to 2?
			buffer.writeByte(_player.getPledgeClass());
			buffer.writeInt(_player.getPkKills());
			buffer.writeInt(_player.getPvpKills());
			buffer.writeShort(_player.getRecomLeft());
			buffer.writeShort(_player.getRecomHave());
			buffer.writeInt(_afkAnimation);
			buffer.writeInt(0); // 228
			buffer.writeInt(0); // 447
		}
		
		if (containsMask(UserInfoType.VITA_FAME))
		{
			buffer.writeShort(19); // 196
			buffer.writeInt(_player.getVitalityPoints());
			buffer.writeByte(0); // Vita Bonus
			buffer.writeInt(0); // _player.getFame()
			buffer.writeInt(0); // _player.getRaidbossPoints()
			buffer.writeByte(0); // 196
			buffer.writeShort(0); // Henna Seal Engraving Gauge
			buffer.writeByte(0); // 196
		}
		
		if (containsMask(UserInfoType.SLOTS))
		{
			buffer.writeShort(16); // 520
			buffer.writeByte(_inventory.getTalismanSlots());
			buffer.writeByte(_inventory.getBroochJewelSlots());
			buffer.writeByte(_player.getTeam().getId());
			buffer.writeInt(0);
			if (_inventory.getAgathionSlots() > 0)
			{
				buffer.writeByte(1); // Charm slots
				buffer.writeByte(_inventory.getAgathionSlots() - 1);
			}
			else
			{
				buffer.writeByte(0); // Charm slots
				buffer.writeByte(0);
			}
			
			buffer.writeByte(_inventory.getArtifactSlots()); // Artifact set slots // 152
			buffer.writeInt(0); // 520 - Perfect?
		}
		
		if (containsMask(UserInfoType.MOVEMENTS))
		{
			buffer.writeShort(4);
			buffer.writeByte(_player.isInsideZone(ZoneId.WATER) ? 1 : _player.isFlyingMounted() ? 2 : 0);
			buffer.writeByte(_player.isRunning());
		}
		
		if (containsMask(UserInfoType.COLOR))
		{
			buffer.writeShort(10);
			buffer.writeInt(_appearance.getNameColor());
			buffer.writeInt(_appearance.getTitleColor());
		}
		
		if (containsMask(UserInfoType.INVENTORY_LIMIT))
		{
			buffer.writeShort(17); // 520
			buffer.writeShort(0); // Mount?
			buffer.writeInt(_player.getInventoryLimit());
			buffer.writeByte(_player.isCursedWeaponEquipped() ? CursedWeaponsManager.getInstance().getLevel(_player.getCursedWeaponEquippedId()) : 0);
			buffer.writeInt(0); // 196
			buffer.writeInt(0); // 196
		}
		
		if (containsMask(UserInfoType.TRUE_HERO))
		{
			buffer.writeShort(9);
			buffer.writeInt(0);
			buffer.writeShort(0);
			buffer.writeByte(_player.isTrueHero() ? 100 : 0);
		}
		
		if (containsMask(UserInfoType.ATT_SPIRITS)) // 152
		{
			buffer.writeShort(34);
			buffer.writeInt((int) _player.getFireSpiritAttack());
			buffer.writeInt((int) _player.getWaterSpiritAttack());
			buffer.writeInt((int) _player.getWindSpiritAttack());
			buffer.writeInt((int) _player.getEarthSpiritAttack());
			buffer.writeInt((int) _player.getFireSpiritDefense());
			buffer.writeInt((int) _player.getWaterSpiritDefense());
			buffer.writeInt((int) _player.getWindSpiritDefense());
			buffer.writeInt((int) _player.getEarthSpiritDefense());
		}
		
		if (containsMask(UserInfoType.RANKING)) // 196
		{
			buffer.writeShort(6);
			buffer.writeInt(_rank);
		}
		
		if (containsMask(UserInfoType.STAT_POINTS)) // 235
		{
			buffer.writeShort(28);
			buffer.writeShort(_player.getLevel() < 76 ? 0 : (_player.getLevel() - 75) + _variables.getInt(PlayerVariables.ELIXIRS_AVAILABLE, 0) + (int) _player.getStat().getValue(Stat.ELIXIR_USAGE_LIMIT, 0)); // Usable points
			
			buffer.writeShort(_variables.getInt(PlayerVariables.STAT_STR, 0)); // 493
			buffer.writeShort(_variables.getInt(PlayerVariables.STAT_DEX, 0)); // 493
			buffer.writeShort(_variables.getInt(PlayerVariables.STAT_CON, 0)); // 493
			buffer.writeShort(_variables.getInt(PlayerVariables.STAT_INT, 0)); // 493
			buffer.writeShort(_variables.getInt(PlayerVariables.STAT_WIT, 0)); // 493
			buffer.writeShort(_variables.getInt(PlayerVariables.STAT_MEN, 0)); // 493
			
			buffer.writeShort(_variables.getInt(PlayerVariables.STAT_STR, 0));
			buffer.writeShort(_variables.getInt(PlayerVariables.STAT_DEX, 0));
			buffer.writeShort(_variables.getInt(PlayerVariables.STAT_CON, 0));
			buffer.writeShort(_variables.getInt(PlayerVariables.STAT_INT, 0));
			buffer.writeShort(_variables.getInt(PlayerVariables.STAT_WIT, 0));
			buffer.writeShort(_variables.getInt(PlayerVariables.STAT_MEN, 0));
		}
		
		if (containsMask(UserInfoType.STAT_ABILITIES)) // 235
		{
			buffer.writeShort(18);
			final PlayerTemplate template = _player.getTemplate();
			buffer.writeShort(_player.getSTR() - template.getBaseSTR() - _variables.getInt(PlayerVariables.STAT_STR, 0)); // additional STR
			buffer.writeShort(_player.getDEX() - template.getBaseDEX() - _variables.getInt(PlayerVariables.STAT_DEX, 0)); // additional DEX
			buffer.writeShort(_player.getCON() - template.getBaseCON() - _variables.getInt(PlayerVariables.STAT_CON, 0)); // additional CON
			buffer.writeShort(_player.getINT() - template.getBaseINT() - _variables.getInt(PlayerVariables.STAT_INT, 0)); // additional INT
			buffer.writeShort(_player.getWIT() - template.getBaseWIT() - _variables.getInt(PlayerVariables.STAT_WIT, 0)); // additional WIT
			buffer.writeShort(_player.getMEN() - template.getBaseMEN() - _variables.getInt(PlayerVariables.STAT_MEN, 0)); // additional MEN
			buffer.writeShort(0);
			buffer.writeShort(0);
		}
		
		if (containsMask(UserInfoType.ELIXIR_USED)) // 286
		{
			buffer.writeShort(_variables.getInt(PlayerVariables.ELIXIRS_AVAILABLE, 0)); // count
			buffer.writeShort(0);
		}
		
		if (containsMask(UserInfoType.VANGUARD_MOUNT)) // 362
		{
			buffer.writeByte(_player.getPlayerClass().level() + 1); // 362 - Vanguard mount.
		}
	}
	
	@Override
	public void runImpl(Player player)
	{
		if (_player == null)
		{
			return;
		}
		
		// Send exp bonus change.
		if (containsMask(UserInfoType.VITA_FAME))
		{
			_player.sendUserBoostStat();
		}
	}
	
	private int calculateRelation(Player player)
	{
		int relation = 0;
		final Party party = player.getParty();
		final Clan clan = player.getClan();
		if (party != null)
		{
			relation |= 8; // Party member
			if (party.getLeader() == _player)
			{
				relation |= 16; // Party leader
			}
		}
		
		if (clan != null)
		{
			if (player.getSiegeState() == 1)
			{
				relation |= 256; // Clan member
			}
			else if (player.getSiegeState() == 2)
			{
				relation |= 32; // Clan member
			}
			
			if (clan.getLeaderId() == player.getObjectId())
			{
				relation |= 64; // Clan leader
			}
		}
		
		if (player.getSiegeState() != 0)
		{
			relation |= 128; // In siege
		}
		
		return relation;
	}
}
