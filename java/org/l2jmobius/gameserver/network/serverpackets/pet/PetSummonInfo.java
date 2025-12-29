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
package org.l2jmobius.gameserver.network.serverpackets.pet;

import java.util.Set;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.enums.EvolveLevel;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.enums.creature.Team;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.actor.instance.Servitor;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * 12 - wolf, 13 - buffalo, 14 - tiger, 15-kukkabara, 17 - hawk, 16 - dragon
 */
public class PetSummonInfo extends ServerPacket
{
	private final Summon _summon;
	private final int _value;
	private final int _runSpd;
	private final int _walkSpd;
	private final int _swimRunSpd;
	private final int _swimWalkSpd;
	private final int _flRunSpd = 0;
	private final int _flWalkSpd = 0;
	private final int _flyRunSpd;
	private final int _flyWalkSpd;
	private final double _moveMultiplier;
	private int _maxFed;
	private int _curFed;
	private final Set<AbnormalVisualEffect> _abnormalVisualEffects;
	private final Team _team;
	private int _statusMask = 0;
	
	public PetSummonInfo(Summon summon, int value)
	{
		_summon = summon;
		_moveMultiplier = summon.getMovementSpeedMultiplier();
		_runSpd = (int) Math.round(summon.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) Math.round(summon.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = (int) Math.round(summon.getSwimRunSpeed() / _moveMultiplier);
		_swimWalkSpd = (int) Math.round(summon.getSwimWalkSpeed() / _moveMultiplier);
		_flyRunSpd = summon.isFlying() ? _runSpd : 0;
		_flyWalkSpd = summon.isFlying() ? _walkSpd : 0;
		_value = value;
		if (summon.isPet())
		{
			final Pet pet = _summon.asPet();
			_curFed = pet.getCurrentFed(); // how fed it is
			_maxFed = pet.getMaxFed(); // max fed it can be
		}
		else if (summon.isServitor())
		{
			final Servitor sum = _summon.asServitor();
			_curFed = sum.getLifeTimeRemaining();
			_maxFed = sum.getLifeTime();
		}
		
		_abnormalVisualEffects = summon.getEffectList().getCurrentAbnormalVisualEffects();
		_team = (GeneralConfig.BLUE_TEAM_ABNORMAL_EFFECT != null) && (GeneralConfig.RED_TEAM_ABNORMAL_EFFECT != null) ? _summon.getTeam() : Team.NONE;
		
		if (summon.isBetrayed())
		{
			_statusMask |= 0x01; // Auto attackable status
		}
		
		_statusMask |= 0x02; // can be chatted with
		if (summon.isRunning())
		{
			_statusMask |= 0x04;
		}
		
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(summon))
		{
			_statusMask |= 0x08;
		}
		
		if (summon.isDead())
		{
			_statusMask |= 0x10;
		}
		
		if (summon.isMountable())
		{
			_statusMask |= 0x20;
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.PET_INFO.writeId(this, buffer);
		buffer.writeByte(_summon.getSummonType());
		buffer.writeInt(_summon.getObjectId());
		buffer.writeInt(_summon.getTemplate().getDisplayId() + 1000000);
		buffer.writeInt(_summon.getX());
		buffer.writeInt(_summon.getY());
		buffer.writeInt(_summon.getZ());
		buffer.writeInt(_summon.getHeading());
		buffer.writeInt(_summon.getMAtkSpd());
		buffer.writeInt(_summon.getPAtkSpd());
		buffer.writeShort(_runSpd);
		buffer.writeShort(_walkSpd);
		buffer.writeShort(_swimRunSpd);
		buffer.writeShort(_swimWalkSpd);
		buffer.writeShort(_flRunSpd);
		buffer.writeShort(_flWalkSpd);
		buffer.writeShort(_flyRunSpd);
		buffer.writeShort(_flyWalkSpd);
		buffer.writeDouble(_moveMultiplier);
		buffer.writeDouble(_summon.getAttackSpeedMultiplier()); // attack speed multiplier
		buffer.writeDouble(_summon.getTemplate().getFCollisionRadius());
		buffer.writeDouble(_summon.getTemplate().getFCollisionHeight());
		buffer.writeInt(_summon.getWeapon()); // right hand weapon
		buffer.writeInt(_summon.getArmor()); // body armor
		buffer.writeInt(0); // left hand weapon
		buffer.writeByte(_summon.isDead() ? 0 : _summon.isShowSummonAnimation() ? 2 : _value);
		buffer.writeInt(-1); // High Five NPCString ID
		if (_summon.isPet())
		{
			buffer.writeString(_summon.getName()); // Pet name.
		}
		else
		{
			buffer.writeString(_summon.getTemplate().isUsingServerSideName() ? _summon.getName() : ""); // Summon name.
		}
		
		buffer.writeInt(-1); // High Five NPCString ID
		buffer.writeString(_summon.getTitle()); // owner name
		buffer.writeByte(_summon.getPvpFlag()); // confirmed
		buffer.writeInt(_summon.getReputation()); // confirmed
		buffer.writeInt(_curFed); // how fed it is
		buffer.writeInt(_maxFed); // max fed it can be
		buffer.writeInt((int) _summon.getCurrentHp()); // current hp
		buffer.writeInt((int) _summon.getMaxHp()); // max hp
		buffer.writeInt((int) _summon.getCurrentMp()); // current mp
		buffer.writeInt(_summon.getMaxMp()); // max mp
		buffer.writeLong(_summon.getStat().getSp()); // sp
		buffer.writeShort(_summon.getLevel()); // level
		buffer.writeLong(_summon.getStat().getExp()); // 0% absolute value
		buffer.writeLong(Math.min(_summon.getExpForThisLevel(), _summon.getStat().getExp())); // 0% absolute value
		buffer.writeLong(_summon.getExpForNextLevel()); // 100% absoulte value
		buffer.writeInt(_summon.isPet() ? _summon.getInventory().getTotalWeight() : 0); // weight
		buffer.writeInt(_summon.getMaxLoad()); // max weight it can carry
		buffer.writeInt(_summon.getPAtk()); // patk
		buffer.writeInt(_summon.getPDef()); // pdef
		buffer.writeInt(_summon.getAccuracy()); // accuracy
		buffer.writeInt(_summon.getEvasionRate()); // evasion
		buffer.writeInt(_summon.getCriticalHit()); // critical
		buffer.writeInt(_summon.getMAtk()); // matk
		buffer.writeInt(_summon.getMDef()); // mdef
		buffer.writeInt(_summon.getMagicAccuracy()); // magic accuracy
		buffer.writeInt(_summon.getMagicEvasionRate()); // magic evasion
		buffer.writeInt(_summon.getMCriticalHit()); // mcritical
		buffer.writeInt((int) _summon.getMoveSpeed()); // speed
		buffer.writeInt(_summon.getPAtkSpd()); // atkspeed
		buffer.writeInt(_summon.getMAtkSpd()); // casting speed
		buffer.writeByte(0); // TODO: Check me, might be ride status
		buffer.writeByte(_summon.getTeam().getId()); // Confirmed
		buffer.writeByte(_summon.getSoulShotsPerHit()); // How many soulshots this servitor uses per hit - Confirmed
		buffer.writeByte(_summon.getSpiritShotsPerHit()); // How many spiritshots this servitor uses per hit - - Confirmed
		buffer.writeInt(-1);
		buffer.writeInt(0); // "Transformation ID - Confirmed" - Used to bug Fenrir after 64 level.
		buffer.writeByte(0); // Used Summon Points
		buffer.writeByte(0); // Maximum Summon Points
		buffer.writeShort(_abnormalVisualEffects.size() + (_summon.isInvisible() ? 1 : 0) + (_team != Team.NONE ? 1 : 0)); // Confirmed
		for (AbnormalVisualEffect abnormalVisualEffect : _abnormalVisualEffects)
		{
			buffer.writeShort(abnormalVisualEffect.getClientId()); // Confirmed
		}
		
		if (_summon.isInvisible())
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
		
		buffer.writeByte(_statusMask);
		if (_summon.isPet())
		{
			final Pet pet = _summon.asPet();
			buffer.writeInt(pet.getPetData().getType());
			buffer.writeInt(pet.getEvolveLevel());
			buffer.writeInt(pet.getEvolveLevel() == 0 ? -1 : pet.getId());
		}
		else
		{
			buffer.writeInt(0);
			buffer.writeInt(EvolveLevel.None.ordinal());
			buffer.writeInt(0);
		}
	}
}
