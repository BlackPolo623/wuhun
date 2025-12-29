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
package org.l2jmobius.gameserver.model;

import static java.lang.Math.max;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.data.xml.ElementalSpiritData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.ElementalSpiritType;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnElementalSpiritUpgrade;
import org.l2jmobius.gameserver.model.item.holders.ElementalSpiritAbsorbItemHolder;
import org.l2jmobius.gameserver.model.item.holders.ElementalSpiritDataHolder;
import org.l2jmobius.gameserver.model.item.holders.ElementalSpiritTemplateHolder;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.elementalspirits.ElementalSpiritInfo;
import org.l2jmobius.gameserver.network.serverpackets.elementalspirits.ExElementalSpiritAttackType;
import org.l2jmobius.gameserver.network.serverpackets.elementalspirits.ExElementalSpiritGetExp;

/**
 * @author JoeAlisson
 */
public class ElementalSpirit
{
	private static final String STORE_ELEMENTAL_SPIRIT_QUERY = "REPLACE INTO character_spirits (charId, type, level, stage, experience, attack_points, defense_points, crit_rate_points, crit_damage_points, in_use) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private final Player _owner;
	private ElementalSpiritTemplateHolder _template;
	private final ElementalSpiritDataHolder _data;
	
	public ElementalSpirit(ElementalSpiritType type, Player owner)
	{
		_data = new ElementalSpiritDataHolder(type.getId(), owner.getObjectId());
		_template = ElementalSpiritData.getInstance().getSpirit(type.getId(), _data.getStage());
		_owner = owner;
	}
	
	public ElementalSpirit(ElementalSpiritDataHolder data, Player owner)
	{
		_owner = owner;
		_data = data;
		_template = ElementalSpiritData.getInstance().getSpirit(data.getType(), data.getStage());
	}
	
	public void addExperience(int experience)
	{
		if ((_data.getLevel() == _template.getMaxLevel()) && (_data.getExperience() >= _template.getMaxExperienceAtLevel(_template.getMaxLevel())))
		{
			return;
		}
		
		_data.addExperience(experience);
		_owner.sendPacket(new ExElementalSpiritGetExp(_data.getType(), _data.getExperience()));
		_owner.sendPacket(new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_S2_ATTRIBUTE_XP).addInt(experience).addElementalSpiritName(_data.getType()));
		
		if (_data.getExperience() > getExperienceToNextLevel())
		{
			levelUp();
			_owner.sendPacket(new SystemMessage(SystemMessageId.S1_ATTRIBUTE_SPIRIT_HAS_REACHED_LV_S2).addElementalSpiritName(_data.getType()).addByte(_data.getLevel()));
			_owner.sendPacket(new ElementalSpiritInfo(_owner, (byte) 0));
			_owner.sendPacket(new ExElementalSpiritAttackType(_owner));
			final UserInfo userInfo = new UserInfo(_owner);
			userInfo.addComponentType(UserInfoType.ATT_SPIRITS);
			_owner.sendPacket(userInfo);
		}
	}
	
	private void levelUp()
	{
		do
		{
			if (_data.getLevel() < _template.getMaxLevel())
			{
				_data.increaseLevel();
			}
			else
			{
				_data.setExperience(getExperienceToNextLevel());
			}
		}
		while (_data.getExperience() > getExperienceToNextLevel());
	}
	
	public void reduceLevel()
	{
		_data.setLevel(Math.max(1, _data.getLevel() - 1));
		_data.setExperience(ElementalSpiritData.getInstance().getSpirit(_data.getType(), _data.getStage()).getMaxExperienceAtLevel(_data.getLevel() - 1));
		resetCharacteristics();
	}
	
	public int getAvailableCharacteristicsPoints()
	{
		final int stage = _data.getStage();
		final int level = _data.getLevel();
		final int points = (stage > 3 ? ((stage - 2) * 20) : (stage - 1) * 10) + (stage > 2 ? (level * 2) : level * 1);
		return max(points - _data.getAttackPoints() - _data.getDefensePoints() - _data.getCritDamagePoints() - _data.getCritRatePoints(), 0);
	}
	
	public ElementalSpiritAbsorbItemHolder getAbsorbItem(int itemId)
	{
		for (ElementalSpiritAbsorbItemHolder absorbItem : getAbsorbItems())
		{
			if (absorbItem.getId() == itemId)
			{
				return absorbItem;
			}
		}
		
		return null;
	}
	
	public int getExtractAmount()
	{
		int amount = Math.round(_data.getExperience() / ElementalSpiritData.FRAGMENT_XP_CONSUME);
		if (getLevel() > 1)
		{
			amount += ElementalSpiritData.getInstance().getSpirit(_data.getType(), _data.getStage()).getMaxExperienceAtLevel(getLevel() - 1) / ElementalSpiritData.FRAGMENT_XP_CONSUME;
		}
		
		return amount;
	}
	
	public void resetStage()
	{
		_data.setLevel(1);
		_data.setExperience(0);
		resetCharacteristics();
	}
	
	public boolean canEvolve()
	{
		return (_data.getStage() < 5) && (_data.getLevel() == 10) && (_data.getExperience() == getExperienceToNextLevel());
	}
	
	public void upgrade()
	{
		_data.increaseStage();
		_data.setLevel(1);
		_data.setExperience(0);
		_template = ElementalSpiritData.getInstance().getSpirit(_data.getType(), _data.getStage());
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_ELEMENTAL_SPIRIT_UPGRADE, _owner))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnElementalSpiritUpgrade(_owner, this), _owner);
		}
	}
	
	public void resetCharacteristics()
	{
		_data.setAttackPoints((byte) 0);
		_data.setDefensePoints((byte) 0);
		_data.setCritRatePoints((byte) 0);
		_data.setCritDamagePoints((byte) 0);
	}
	
	public byte getType()
	{
		return _template.getType();
	}
	
	public byte getStage()
	{
		return _template.getStage();
	}
	
	public int getNpcId()
	{
		return _template.getNpcId();
	}
	
	public long getExperience()
	{
		return _data.getExperience();
	}
	
	public long getExperienceToNextLevel()
	{
		return _template.getMaxExperienceAtLevel(_data.getLevel());
	}
	
	public int getLevel()
	{
		return _data.getLevel();
	}
	
	public int getMaxLevel()
	{
		return _template.getMaxLevel();
	}
	
	public int getAttack()
	{
		return _template.getAttackAtLevel(_data.getLevel()) + (_data.getAttackPoints() * 5);
	}
	
	public int getDefense()
	{
		return _template.getDefenseAtLevel(_data.getLevel()) + (_data.getDefensePoints() * 5);
	}
	
	public int getMaxCharacteristics()
	{
		return _template.getMaxCharacteristics();
	}
	
	public int getAttackPoints()
	{
		return _data.getAttackPoints();
	}
	
	public int getDefensePoints()
	{
		return _data.getDefensePoints();
	}
	
	public int getCriticalRatePoints()
	{
		return _data.getCritRatePoints();
	}
	
	public int getCriticalDamagePoints()
	{
		return _data.getCritDamagePoints();
	}
	
	public List<ItemHolder> getItemsToEvolve()
	{
		return _template.getItemsToEvolve();
	}
	
	public List<ElementalSpiritAbsorbItemHolder> getAbsorbItems()
	{
		return _template.getAbsorbItems();
	}
	
	public int getExtractItem()
	{
		return _template.getExtractItem();
	}
	
	public void save()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(STORE_ELEMENTAL_SPIRIT_QUERY))
		{
			statement.setInt(1, _data.getCharId());
			statement.setInt(2, _data.getType());
			statement.setInt(3, _data.getLevel());
			statement.setInt(4, _data.getStage());
			statement.setLong(5, _data.getExperience());
			statement.setInt(6, _data.getAttackPoints());
			statement.setInt(7, _data.getDefensePoints());
			statement.setInt(8, _data.getCritRatePoints());
			statement.setInt(9, _data.getCritDamagePoints());
			statement.setInt(10, _data.isInUse() ? 1 : 0);
			statement.execute();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void addAttackPoints(byte attackPoints)
	{
		_data.addAttackPoints(attackPoints);
	}
	
	public void addDefensePoints(byte defensePoints)
	{
		_data.addDefensePoints(defensePoints);
	}
	
	public void addCritRatePoints(byte critRatePoints)
	{
		_data.addCritRatePoints(critRatePoints);
	}
	
	public void addCritDamage(byte critDamagePoints)
	{
		_data.addCritDamagePoints(critDamagePoints);
	}
	
	public int getCriticalRate()
	{
		return _template.getCriticalRateAtLevel(_data.getLevel()) + getCriticalRatePoints();
	}
	
	public int getCriticalDamage()
	{
		return _template.getCriticalDamageAtLevel(_data.getLevel()) + getCriticalDamagePoints();
	}
	
	public void setInUse(boolean value)
	{
		_data.setInUse(value);
	}
	
	public boolean isInUse()
	{
		return _data.isInUse();
	}
}
