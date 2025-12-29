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
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.model.item.enums.ItemGrade;
import org.l2jmobius.gameserver.model.item.enums.ShotType;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.stats.TraitType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
//todo 檢查每個獲取公式
public class ExUserViewInfoParameter extends ServerPacket
{
	private final Player _player;
	
	public ExUserViewInfoParameter(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_USER_VIEW_INFO_PARAMETER.writeId(this, buffer);
		
		final PlayerStat stat = _player.getStat();
		int index = 0;
		
		// Number of parameters.
		buffer.writeInt(185);
		
		// ################################## ATTACK ##############################
		// P. Atk. (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Atk. (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getPAtk());
		
		// M. Atk. (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Atk. (num)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getMAtk());
		
		// Soulshot Damage - Activation
		buffer.writeShort(index++);
		buffer.writeInt((_player.isChargedShot(ShotType.BLESSED_SOULSHOTS) || _player.isChargedShot(ShotType.SOULSHOTS)) ? (10000 + (_player.getActiveRubyJewel() != null ? (int) _player.getActiveRubyJewel().getBonus() * 1000 : 0)) : 0);
		
		// Spiritshot Damage - Activation
		buffer.writeShort(index++);
		buffer.writeInt((_player.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) || _player.isChargedShot(ShotType.SPIRITSHOTS)) ? (30000 + (_player.getActiveShappireJewel() != null ? (int) _player.getActiveShappireJewel().getBonus() * 1000 : 0)) : 0);
		
		// Soulshot Damage - Enchanted Weapons
		buffer.writeShort(index++);
		buffer.writeInt((((_player.getActiveWeaponInstance() != null) && _player.getActiveWeaponInstance().isEnchanted()) ? (int) (_player.getActiveWeaponInstance().getEnchantLevel() * (_player.getActiveWeaponItem().getItemGrade() == ItemGrade.S ? 1.6 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.A ? 1.4 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.B ? 0.7 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.C) ? 0.4 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.D) ? 0.4 : 0) * 100) : 0));
		
		// Spiritshot Damage - Enchanted Weapons
		buffer.writeShort(index++);
		buffer.writeInt((((_player.getActiveWeaponInstance() != null) && _player.getActiveWeaponInstance().isEnchanted()) ? (int) (_player.getActiveWeaponInstance().getEnchantLevel() * (_player.getActiveWeaponItem().getItemGrade() == ItemGrade.S ? 1.6 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.A ? 1.4 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.B ? 0.7 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.C) ? 0.4 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.D) ? 0.4 : 0) * 100) : 0));
		
		// Soulshot Damage - Misc.
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.SHOTS_BONUS, 100) - 100) * 100);
		
		// Spiritshot Damage - Misc.
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.SHOTS_BONUS, 100) - 100) * 100);
		
		// Basic PvP Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVP_PHYSICAL_ATTACK_DAMAGE, 100) - 100) * 100);
		
		// P. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVP_PHYSICAL_SKILL_DAMAGE, 100) - 100) * 100);
		
		// M. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVP_MAGICAL_SKILL_DAMAGE, 100) - 100) * 100);
		
		// Inflicted PvP Damage
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// PvP Damage Decrease Ignore
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Basic PvE Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVE_PHYSICAL_ATTACK_DAMAGE, 100) - 100) * 100);
		
		// P. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVE_PHYSICAL_SKILL_DAMAGE, 100) - 100) * 100);
		
		// M. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVE_MAGICAL_SKILL_DAMAGE, 100) - 100) * 100);
		
		// PvE Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVE_DAMAGE_TAKEN, 100) - 100) * 100);
		
		// PvE Damage Decrease Ignore
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Basic Power
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Skill Power
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PHYSICAL_SKILL_POWER, 100) - 100) * 100);
		
		// M. Skill Power
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.MAGICAL_SKILL_POWER, 100) - 100) * 100);
		
		// AoE Skill Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.AREA_OF_EFFECT_DAMAGE_MODIFY, 100) - 100) * 100);
		
		// Damage Bonus - Sword
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.SWORD) * 100 * 100) - 10000);
		
		// Damage Bonus - Sword Two hand
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.SWORD) * 100 * 100) - 10000);
		
		// Damage Bonus - Magic Sword
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.SWORD) * 100 * 100) - 10000);
		
		// Damage Bonus - Ancient Sword
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.ANCIENTSWORD) * 100 * 100) - 10000);
		
		// Damage Bonus - Dagger
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.DAGGER) * 100 * 100) - 10000);
		
		// Damage Bonus - Rapier
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.RAPIER) * 100 * 100) - 10000);
		
		// Damage Bonus - Blunt Weapon (one hand)
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.BLUNT) * 100 * 100) - 10000);
		
		// Damage Bonus - Blunt Weapon (two hand)
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.BLUNT) * 100 * 100) - 10000);
		
		// Damage Bonus - Magic Blunt Weapon (one hand)
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.BLUNT) * 100 * 100) - 10000);
		
		// Damage Bonus - Magic Blunt Weapon (two hand)
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.BLUNT) * 100 * 100) - 10000);
		
		// Damage Bonus - Spear
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.POLE) * 100 * 100) - 10000);
		
		// Damage Bonus - Fists
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.DUALFIST) * 100 * 100) - 10000);
		
		// Damage Bonus - Dual Swords
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.DUAL) * 100 * 100) - 10000);
		
		// Damage Bonus - Bow
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.BOW) * 100 * 100) - 10000);
		
		// Damage Bonus - Firearms
		buffer.writeShort(index++);
		buffer.writeInt(((int) stat.getAttackTrait(TraitType.PISTOLS) * 100 * 100) - 10000);
		
		// ################################## DEFENCE ##############################
		// P. Def. (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Def. (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getPDef());
		
		// M. Def. (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Def. (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getMDef());
		
		// Soulshot Damage Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.SOULSHOT_RESISTANCE, 100) - 100) * 100);
		
		// Spiritshot Damage Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.SPIRITSHOT_RESISTANCE, 100) - 100) * 100);
		
		// Received basic PvP Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVP_PHYSICAL_ATTACK_DEFENCE, 100) - 100) * 100 * -1);
		
		// Received P. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVP_PHYSICAL_SKILL_DEFENCE, 100) - 100) * 100 * -1);
		
		// Received M. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVP_MAGICAL_SKILL_DEFENCE, 100) - 100) * 100 * -1);
		
		// Received PvP Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVP_DAMAGE_TAKEN, 100) - 100) * 100);
		
		// PvP Damage Decrease
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received basic PvE Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVE_PHYSICAL_ATTACK_DEFENCE, 100) - 100) * 100 * -1);
		
		// Received P. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVE_PHYSICAL_SKILL_DEFENCE, 100) - 100) * 100 * -1);
		
		// Received M. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVE_MAGICAL_SKILL_DEFENCE, 100) - 100) * 100 * -1);
		
		// Received PvE Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PVE_DAMAGE_TAKEN, 100) - 100) * 100);
		
		// PvE Damage Decrease
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received basic damage power
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Skill Power when hit
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Skill Power when hit
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received AoE Skill Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.AREA_OF_EFFECT_DAMAGE_DEFENCE, 100) - 100) * 100);
		
		// Damage Resistance Bonus - One hand Sword
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.SWORD) * 100);
		
		// Damage Resistance Bonus - Two hand Sword
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.SWORD) * 100);
		
		// Damage Resistance Bonus - Magic Sword
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.SWORD) * 100);
		
		// Damage Resistance Bonus - Ancient Sword
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.ANCIENTSWORD) * 100);
		
		// Damage Resistance Bonus - Dagger
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.DAGGER) * 100);
		
		// Damage Resistance Bonus - Rapier
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.RAPIER) * 100);
		
		// Damage Resistance Bonus - Blunt Weapon one hand
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.BLUNT) * 100);
		
		// Damage Resistance Bonus - Blunt Weapon two hand
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.BLUNT) * 100);
		
		// Damage Resistance Bonus - Magic Blunt Weapon (one hand)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.BLUNT) * 100);
		
		// Damage Resistance Bonus - Magic Blunt Weapon (two hand)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.BLUNT) * 100);
		
		// Damage Resistance Bonus - Spear
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.POLE) * 100);
		
		// Damage Resistance Bonus - Fists
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.FIST) * 100);
		
		// Damage Resistance Bonus - Dual Swords
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.DUAL) * 100);
		
		// Damage Resistance Bonus - Bow
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.BOW) * 100);
		
		// Damage Resistance Bonus - Firearms
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.PISTOLS) * 100);
		
		// Shield Defense (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Shield Defence (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getShldDef());
		
		// Shield Defence Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.SHIELD_DEFENCE_RATE, 100) - 100) * 100);
		
		// M. Damage Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.MAGIC_SUCCESS_RES, 100) - 100) * 100);
		
		// M. Damage Resistance (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Damage Reflection (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.VENGEANCE_SKILL_MAGIC_DAMAGE, 100) - 100) * 100);
		
		// M. Damage Reflection Resistance
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Received Fixed Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.REAL_DAMAGE_RESIST, 100) - 100) * 100);
		
		// Casting Interruption Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.ATTACK_CANCEL, 100) - 100) * 100);
		
		// Casting Interruption Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// ################################## ACCURACY ##############################
		// P. Accuracy (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Accuracy (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getAccuracy());
		
		// M. Accuracy (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Accuracy (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getMagicAccuracy());
		
		// Vital Point Attack Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.BLOW_RATE, 100) - 100) * 100);
		
		// Vital Point Attack Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// ################################## EVASION ##############################
		// P. Evasion (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Evasion (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getEvasionRate());
		
		// M. Evasion (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Evasion (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getMagicEvasionRate());
		
		// Received Vital Point Attack Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.BLOW_RATE_DEFENCE, 100) - 100) * 100);
		
		// Received Vital Point Attack Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// P. Skill Evasion (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getSkillEvasionTypeValue(0) * 100);
		
		// M. Skill Evasion (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getSkillEvasionTypeValue(1) * 100);
		
		// ################################## SPEED ##############################
		// Atk. Spd. (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Atk. Spd. (num.)
		buffer.writeShort(index++);
		buffer.writeInt(stat.getPAtkSpd());
		
		// Casting Spd. (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Casting Spd. (num.)
		buffer.writeShort(index++);
		buffer.writeInt(stat.getMAtkSpd());
		
		// Speed (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Speed (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getMoveSpeed());
		
		// ################################## CRITICAL RATE ##############################
		// Basic Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(stat.getCriticalHit() * 10);
		
		// Basic Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(stat.getCriticalHit());
		
		// P. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getPSkillCriticalRate() * 10);
		
		// P. Skill Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getPSkillCriticalRate());
		
		// M. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getMCriticalHit() * 10);
		
		// M. Skill Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getMCriticalHit());
		
		// Received basic Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.DEFENCE_CRITICAL_RATE, 100) - 100) * 100 * -1);
		
		// Received basic Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.DEFENCE_CRITICAL_RATE_ADD));
		
		// Received P. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_RATE, 100) - 100) * 100 * -1);
		
		// Received P. Skill Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_RATE_ADD));
		
		// Received M. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.DEFENCE_MAGIC_CRITICAL_RATE, 100) - 100) * 100 * -1);
		
		// Received M. Skill Critical Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.DEFENCE_MAGIC_CRITICAL_RATE_ADD));
		
		// ################################## CRITICAL DAMAGE ##############################
		// Basic Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.CRITICAL_DAMAGE, 100) - 100) * 100);
		
		// Basic Critical Damage (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getCriticalDmg(0));
		
		// P. Skill Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.PHYSICAL_SKILL_CRITICAL_DAMAGE, 100) - 100) * 100);
		
		// P. Skill Critical Damage (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PHYSICAL_SKILL_CRITICAL_DAMAGE_ADD));
		
		// M. Skill Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.MAGIC_CRITICAL_DAMAGE, 100) - 100) * 100);
		
		// M. Skill Critical Damage (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.MAGIC_CRITICAL_DAMAGE_ADD));
		
		// Received Basic Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.DEFENCE_CRITICAL_DAMAGE, 100) - 100) * 100 * -1);
		
		// Received Basic Critical Damage (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.DEFENCE_CRITICAL_DAMAGE_ADD));
		
		// Received P. Skill Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_DAMAGE, 100) - 100) * 100 * -1);
		
		// Received P. Skill Critical Damage (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_DAMAGE_ADD));
		
		// Received M. Skill Critical Damage (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.DEFENCE_MAGIC_CRITICAL_DAMAGE, 100) - 100) * 100 * -1);
		
		// Received M. Skill Critical Damage (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.DEFENCE_MAGIC_CRITICAL_DAMAGE_ADD));
		
		// ################################## RECOVERY ##############################
		// HP ReCovery Potions' Effect (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.ADDITIONAL_POTION_HP_PER, 100) - 100) * 100);
		
		// HP Recovery Potions' Effect (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.ADDITIONAL_POTION_HP));
		
		// MP Recovery Potions' Effect (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.ADDITIONAL_POTION_MP_PER, 100) - 100) * 100);
		
		// MP Recovery Potions' Effect (num.)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.ADDITIONAL_POTION_MP));
		
		// HP Recovery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// HP Recovery Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(stat.getHpRegen());
		
		// HP Recovery Rate while standing (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// HP Recovery Rate while standing (num.)
		buffer.writeShort(index++);
		buffer.writeInt(!_player.isMoving() ? stat.getHpRegen() : 0);
		
		// HP Recovery Rate while sitting (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// HP Recovery Rate while sitting (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isSitting() ? stat.getHpRegen() : 0);
		
		// HP Recovery Rate while walking (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// HP Recovery Rate while walking (num.)
		buffer.writeShort(index++);
		buffer.writeInt((_player.isMoving() && !_player.isRunning()) ? stat.getHpRegen() : 0);
		
		// HP Recovery Rate while running (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// HP Recovery Rate while running (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isRunning() ? stat.getHpRegen() : 0);
		
		// MP Recovery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// MP Recovery Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(stat.getMpRegen());
		
		// MP Recovery Rate while standing (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// MP Recovery Rate while standing (num.)
		buffer.writeShort(index++);
		buffer.writeInt(!_player.isMoving() ? stat.getMpRegen() : 0);
		
		// MP Recovery Rate while sitting (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// MP Recovery Rate while sitting (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isSitting() ? stat.getMpRegen() : 0);
		
		// MP Recovery Rate while walking (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// MP Recovery Rate while walking (num.)
		buffer.writeShort(index++);
		buffer.writeInt((_player.isMoving() && !_player.isRunning()) ? stat.getMpRegen() : 0);
		
		// MP Recovery Rate while running (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// MP Recovery Rate while running (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isRunning() ? stat.getMpRegen() : 0);
		
		// CP Recovery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// CP Recovery Rate (num.)
		buffer.writeShort(index++);
		buffer.writeInt(stat.getCpRegen());
		
		// CP Recovery Rate while standing (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// CP Recovery Rate while standing (num.)
		buffer.writeShort(index++);
		buffer.writeInt(!_player.isMoving() ? stat.getCpRegen() : 0);
		
		// CP Recovery Rate while sitting (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// CP Recovery Rate while sitting (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isSitting() ? stat.getCpRegen() : 0);
		
		// CP Recovery Rate while walking (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// CP Recovery Rate while walking (num.)
		buffer.writeShort(index++);
		buffer.writeInt((_player.isMoving() && !_player.isRunning()) ? stat.getCpRegen() : 0);
		
		// CP Recovery Rate while running (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// CP Recovery Rate while running (num.)
		buffer.writeShort(index++);
		buffer.writeInt(_player.isRunning() ? stat.getCpRegen() : 0);
		
		// ################################## SKILL COOLDOWN ##############################
		// P. Skill Cooldown (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getReuseTypeValue(0) * 100 * 100);
		
		// M. Skill Cooldown (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getReuseTypeValue(1) * 100 * 100);
		
		// Song/ Dance Cooldown (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getReuseTypeValue(3) * 100 * 100);
		
		// ################################## MP CONSUMPTION ##############################
		// P. Skill MP Consumption Decrease (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getMpConsumeTypeValue(0) * 100 * 100);
		
		// M. Skill MP Consumption Decrease (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getMpConsumeTypeValue(1) * 100 * 100);
		
		// Song/ Dance MP Consumption Decrease (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getMpConsumeTypeValue(3) * 100 * 100);
		
		// P. Skill MP Consumption Decrease (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// M. Skill MP Consumption Decrease (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Song/ Dance MP Consumption Decrease (num.)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// ################################## ANOMALIES ##############################
		// Buff Cancel Resistance Bonus (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.RESIST_DISPEL_BUFF, 100) - 100) * 100);
		
		// Debuff/ Anomaly Resistance Bonus (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) (stat.getValue(Stat.RESIST_ABNORMAL_DEBUFF, 100) - 100) * 100);
		
		// Unequip Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// Paralysis Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAttackTrait(TraitType.PARALYZE) * 100);
		
		// Shock Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAttackTrait(TraitType.SHOCK) * 100);
		
		// Knockback Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAttackTrait(TraitType.KNOCKBACK) * 100);
		
		// Sleep Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAttackTrait(TraitType.SLEEP) * 100);
		
		// Imprisonment Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAttackTrait(TraitType.IMPRISON) * 100);
		
		// Pull Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAttackTrait(TraitType.PULL) * 100);
		
		// Fear Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAttackTrait(TraitType.FEAR) * 100);
		
		// Silence Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAttackTrait(TraitType.SILENCE) * 100);
		
		// Hold Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAttackTrait(TraitType.HOLD) * 100);
		
		// Suppression Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAttackTrait(TraitType.SUPPRESSION) * 100);
		
		// Infection Atk. Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getAttackTrait(TraitType.INFECTION) * 100);
		
		// Paralysis Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.PARALYZE) * 100);
		
		// Shock Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.SHOCK) * 100);
		
		// Knockback Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.KNOCKBACK) * 100);
		
		// Sleep Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.SLEEP) * 100);
		
		// Imprisonment Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.IMPRISON) * 100);
		
		// Pull Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.PULL) * 100);
		
		// Fear Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.FEAR) * 100);
		
		// Silence Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.SLEEP) * 100);
		
		// Hold Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.HOLD) * 100);
		
		// Suppresion Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.SUPPRESSION) * 100);
		
		// Infection Resistance (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getDefenceTrait(TraitType.INFECTION) * 100);
	}
}
